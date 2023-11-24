/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.impl.ProcessListUtil
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.rd.util.withBackgroundContext
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.system.CpuArch
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.debugger.DebuggerHelperHost
import com.jetbrains.rider.debugger.DebuggerWorkerPlatform
import com.jetbrains.rider.debugger.DebuggerWorkerProcessHandler
import com.jetbrains.rider.debugger.RiderDebuggerBundle
import com.jetbrains.rider.model.DesktopClrRuntime
import com.jetbrains.rider.model.debuggerHelper.PlatformArchitecture
import com.jetbrains.rider.model.debuggerWorker.DebuggerStartInfoBase
import com.jetbrains.rider.model.debuggerWorker.DotNetClrAttachStartInfo
import com.jetbrains.rider.model.debuggerWorker.DotNetCoreAttachStartInfo
import com.jetbrains.rider.run.*
import com.jetbrains.rider.run.dotNetCore.DotNetCoreAttachProfileState
import com.jetbrains.rider.run.dotNetCore.getWorkerPlatform
import com.jetbrains.rider.run.msNet.MsNetAttachProfileState
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.DotNetRuntime
import com.jetbrains.rider.runtime.apply
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils
import kotlinx.coroutines.delay
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class FunctionIsolatedDebugProfile(
    private val dotNetExecutable: DotNetExecutable,
    private val dotNetRuntime: DotNetRuntime,
    executionEnvironment: ExecutionEnvironment
) : DebugProfileStateBase(executionEnvironment) {
    companion object {
        private val LOG = logger<FunctionIsolatedDebugProfile>()
        private val waitDuration = 1.minutes
        private const val DOTNET_ISOLATED_DEBUG_ARGUMENT = "--dotnet-isolated-debug"
        private const val DOTNET_ENABLE_JSON_OUTPUT_ARGUMENT = "--enable-json-output"
        private const val DOTNET_JSON_OUTPUT_FILE_ARGUMENT = "--json-output-file"
    }

    private var processId = 0
    private var isNetFrameworkProcess = false
    private lateinit var targetProcessHandler: ProcessHandler
    private lateinit var console: ConsoleView

    override suspend fun createWorkerRunInfo(lifetime: Lifetime, helper: DebuggerHelperHost, port: Int): WorkerRunInfo {
        processId = withBackgroundProgress(
            executionEnvironment.project, "Waiting for Azure Functions host to start..."
        ) {
            withBackgroundContext {
                launchAzureFunctionsHost()
            }
        } ?: 0

        if (targetProcessHandler.isProcessTerminated) {
            LOG.warn("Azure Functions host process terminated before the debugger could attach.")

            PluginUtil.showErrorNotificationProject(
                executionEnvironment.project,
                RiderAzureBundle.message("run_config.run_function_app.debug.notification.title"),
                RiderAzureBundle.message("run_config.run_function_app.debug.notification.isolated_worker_process_terminated")
            )
            return createWorkerRunInfoFor(port, DebuggerWorkerPlatform.AnyCpu)
        }

        if (processId == 0) {
            LOG.warn("Azure Functions host did not return isolated worker process id.")

            PluginUtil.showErrorNotificationProject(
                executionEnvironment.project,
                RiderAzureBundle.message("run_config.run_function_app.debug.notification.title"),
                RiderAzureBundle.message("run_config.run_function_app.debug.notification.isolated_worker_pid_unspecified")
            )
            return createWorkerRunInfoFor(port, DebuggerWorkerPlatform.AnyCpu)
        }

        val targetProcess = withBackgroundContext {
            ProcessListUtil.getProcessList().firstOrNull { it.pid == processId }
        }

        if (targetProcess == null) {
            LOG.warn("Unable to find target process with pid $processId")
            return createWorkerRunInfoFor(port, DebuggerWorkerPlatform.AnyCpu)
        }

        val processExecutablePath = ParametersListUtil.parse(targetProcess.commandLine).firstOrNull()
        val processArchitecture = getPlatformArchitecture(lifetime, processId)
        val processTargetFramework = processExecutablePath?.let {
            DebuggerHelperHost.getInstance(executionEnvironment.project)
                .getAssemblyTargetFramework(it, lifetime)
        }

        isNetFrameworkProcess = processExecutablePath?.endsWith("dotnet.exe") == false
                && (processTargetFramework?.isNetFramework ?: false)

        return if (!isNetFrameworkProcess) {
            DotNetCoreAttachProfileState(targetProcess, executionEnvironment, processArchitecture)
                .createWorkerRunInfo(lifetime, helper, port)
        } else {
            val clrRuntime = DesktopClrRuntime("")
            MsNetAttachProfileState(
                targetProcess,
                processArchitecture.getWorkerPlatform(),
                clrRuntime,
                executionEnvironment,
                RiderDebuggerBundle.message("MsNetAttachProvider.display.name", clrRuntime.version)
            )
                .createWorkerRunInfo(lifetime, helper, port)
        }
    }

    private suspend fun getPlatformArchitecture(lifetime: Lifetime, pid: Int): PlatformArchitecture {
        if (SystemInfo.isWindows) {
            return DebuggerHelperHost.getInstance(executionEnvironment.project)
                .getProcessArchitecture(lifetime, pid)
        }

        return when (CpuArch.CURRENT) {
            CpuArch.X86 -> PlatformArchitecture.X86
            CpuArch.X86_64 -> PlatformArchitecture.X64
            CpuArch.ARM64 -> PlatformArchitecture.Arm64
            else -> PlatformArchitecture.Unknown
        }
    }

    private suspend fun launchAzureFunctionsHost(): Int? {
        ThreadingAssertions.assertBackgroundThread()

        val programParameters = ParametersListUtil.parse(dotNetExecutable.programParameterString)

        if (!programParameters.contains(DOTNET_ISOLATED_DEBUG_ARGUMENT)) {
            programParameters.add(DOTNET_ISOLATED_DEBUG_ARGUMENT)
        }

        // We will need to read the worker process PID, so the debugger can later attach to it.
        //
        // We are using this file to read the PID,
        // (see https://github.com/Azure/azure-functions-dotnet-worker/issues/900).
        //
        // Example contents: { "name":"dotnet-worker-startup", "workerProcessId" : 28460 }
        val tempPidFile = FileUtil.createTempFile(
            File(FileUtil.getTempDirectory()),
            "Rider-AzureFunctions-IsolatedWorker-",
            "json.pid", true, true
        )

        if (!programParameters.contains(DOTNET_ENABLE_JSON_OUTPUT_ARGUMENT)) {
            programParameters.add(DOTNET_ENABLE_JSON_OUTPUT_ARGUMENT)
        }
        if (!programParameters.contains(DOTNET_JSON_OUTPUT_FILE_ARGUMENT)) {
            programParameters.add(DOTNET_JSON_OUTPUT_FILE_ARGUMENT)
            programParameters.add(tempPidFile.path)
        }

        // Start the Azure Functions host
        val commandLine = dotNetExecutable
            .copy(
                useExternalConsole = false,
                programParameterString = ParametersListUtil.join(programParameters)
            )
            .createRunCommandLine(dotNetRuntime)
            .apply(dotNetRuntime, ParametersListUtil.parse(dotNetExecutable.runtimeArguments))

        val processListeners = PatchCommandLineExtension.EP_NAME.getExtensions(executionEnvironment.project)
            .map { it.patchRunCommandLine(commandLine, dotNetRuntime, executionEnvironment.project) }

        val commandLineString = commandLine.commandLineString

        targetProcessHandler = TerminalProcessHandler(executionEnvironment.project, commandLine)

        LOG.info("Starting functions host process with command line: $commandLineString")

        processListeners.filterNotNull().forEach { targetProcessHandler.addProcessListener(it) }

        console = createConsole(
            consoleKind = ConsoleKind.Normal,
            processHandler = targetProcessHandler,
            project = executionEnvironment.project
        )

        if (dotNetExecutable.useExternalConsole) {
            LOG.debug("Ignoring for isolated worker: dotNetExecutable.useExternalConsole=true")
            console.print(
                "The 'Use external console' option was ignored. Process Input/Output redirection is required to debug Azure Functions isolated workers." + System.lineSeparator(),
                ConsoleViewContentType.SYSTEM_OUTPUT
            )
        }

        console.attachToProcess(targetProcessHandler)

        targetProcessHandler.startNotify()

        var timeout = 0.milliseconds
        while (timeout <= waitDuration) {
            val pidFileJson = JsonUtils.readJsonFile(tempPidFile)
            if (pidFileJson != null) {
                if (pidFileJson.has("workerProcessId")) {
                    val pidFromJson = pidFileJson.get("workerProcessId").asInt

                    LOG.info("Got functions isolated worker process id from JSON output.")
                    LOG.info("Functions isolated worker process id: $pidFromJson")
                    return pidFromJson
                }
            }
            delay(500)
            timeout += 500.milliseconds
        }

        return null
    }

    override fun execute(
        executor: Executor,
        runner: ProgramRunner<*>,
        workerProcessHandler: DebuggerWorkerProcessHandler
    ): ExecutionResult {
        throw UnsupportedOperationException("Use overload with lifetime")
    }

    override fun execute(
        executor: Executor,
        runner: ProgramRunner<*>,
        workerProcessHandler: DebuggerWorkerProcessHandler,
        lifetime: Lifetime
    ): ExecutionResult {
        if (processId == 0) {
            if (!targetProcessHandler.isProcessTerminating && !targetProcessHandler.isProcessTerminated) {
                LOG.debug("Destroying Azure Functions host process.")
                targetProcessHandler.destroyProcess()
            }

            return DefaultExecutionResult(console, targetProcessHandler)
        }

        workerProcessHandler.attachTargetProcess(targetProcessHandler)
        return DefaultExecutionResult(console, workerProcessHandler)
    }

    override val consoleKind: ConsoleKind =
        if (dotNetExecutable.useExternalConsole) ConsoleKind.ExternalConsole
        else ConsoleKind.Normal

    override val attached: Boolean = false

    override suspend fun checkBeforeExecution() {
        dotNetExecutable.validate()
    }

    override suspend fun createModelStartInfo(lifetime: Lifetime): DebuggerStartInfoBase = if (!isNetFrameworkProcess) {
        DotNetCoreAttachStartInfo(processId)
    } else {
        DotNetClrAttachStartInfo("", processId)
    }
}