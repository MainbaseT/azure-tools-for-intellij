/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.CantRunException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.process.impl.ProcessListUtil
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.system.CpuArch
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.debugger.DebuggerHelperHost
import com.jetbrains.rider.debugger.DebuggerWorkerProcessHandler
import com.jetbrains.rider.model.DesktopClrRuntime
import com.jetbrains.rider.model.debuggerHelper.PlatformArchitecture
import com.jetbrains.rider.run.AttachDebugProcessAwareProfileStateBase
import com.jetbrains.rider.run.ConsoleKind
import com.jetbrains.rider.run.IDotNetDebugProfileState
import com.jetbrains.rider.run.configurations.RequiresPreparationRunProfileState
import com.jetbrains.rider.run.dotNetCore.DotNetCoreAttachProfileState
import com.jetbrains.rider.run.dotNetCore.toCPUKind
import com.jetbrains.rider.run.msNet.MsNetAttachProfileState
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.DotNetRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FunctionIsolatedDebugProfileState(
    private val dotNetExecutable: DotNetExecutable,
    private val dotNetRuntime: DotNetRuntime,
    private val lifetime: Lifetime,
) : IDotNetDebugProfileState, RequiresPreparationRunProfileState {
    companion object {
        private val LOG = logger<FunctionIsolatedDebugProfileState>()
    }

    private lateinit var functionHostExecutionResult: ExecutionResult
    private lateinit var wrappedState: AttachDebugProcessAwareProfileStateBase

    override val consoleKind: ConsoleKind =
        if (dotNetExecutable.useExternalConsole) ConsoleKind.ExternalConsole
        else ConsoleKind.Normal

    override val attached: Boolean = false

    override suspend fun checkBeforeExecution() {
        dotNetExecutable.validate()
    }

    override suspend fun prepareExecution(environment: ExecutionEnvironment) {
        val (executionResult, pid) = launchFunctionHostWaitingForDebugger(environment)
        functionHostExecutionResult = executionResult

        val targetProcess = withContext(Dispatchers.IO) {
            ProcessListUtil.getProcessList().firstOrNull { it.pid == pid }
        } ?: throw CantRunException("Unable to find process with ID $pid")

        val processExecutablePath = ParametersListUtil.parse(targetProcess.commandLine).firstOrNull()
        val processArchitecture = getPlatformArchitecture(lifetime, pid, environment.project)
        val processTargetFramework = processExecutablePath?.let {
            DebuggerHelperHost
                .getInstance(environment.project)
                .getAssemblyTargetFramework(it, lifetime)
        }

        val isNetFrameworkProcess =
            processExecutablePath?.endsWith("dotnet.exe") == false && (processTargetFramework?.isNetFramework ?: false)

        wrappedState =
            if (isNetFrameworkProcess) {
                MsNetAttachProfileState(
                    targetProcess,
                    processArchitecture.toCPUKind(),
                    DesktopClrRuntime(""),
                    environment
                )
            } else {
                DotNetCoreAttachProfileState(
                    targetProcess,
                    environment,
                    processArchitecture
                )
            }
    }

    private suspend fun launchFunctionHostWaitingForDebugger(environment: ExecutionEnvironment): Pair<ExecutionResult, Int> {
        val launcher = FunctionHostDebugLauncher.getInstance(environment.project)
        val (executionResult, pid) =
            withBackgroundProgress(environment.project, "Waiting for Azure Functions host to start...") {
                withContext(Dispatchers.Default) {
                    launcher.startProcessWaitingForDebugger(dotNetExecutable, dotNetRuntime)
                }
            }

        if (executionResult.processHandler.isProcessTerminating || executionResult.processHandler.isProcessTerminated) {
            LOG.warn("Azure Functions host process terminated before the debugger could attach")

            Notification(
                "Azure AppServices",
                "Azure Functions - debug",
                "Azure Functions host process terminated before the debugger could attach.",
                NotificationType.ERROR
            )
                .notify(environment.project)

            throw CantRunException("Azure Functions host process terminated before the debugger could attach")
        }

        if (pid == 0) {
            LOG.warn("Azure Functions host did not return isolated worker process id")

            Notification(
                "Azure AppServices",
                "Azure Functions - debug",
                "Azure Functions host did not return isolated worker process id. Could not attach the debugger. Check the process output for more information.",
                NotificationType.ERROR
            )
                .notify(environment.project)

            throw CantRunException("Azure Functions host did not return isolated worker process id")
        }

        return executionResult to requireNotNull(pid)
    }

    override suspend fun createWorkerRunInfo(lifetime: Lifetime, helper: DebuggerHelperHost, port: Int) =
        wrappedState.createWorkerRunInfo(lifetime, helper, port)

    override suspend fun getLauncherInfo(lifetime: Lifetime, helper: DebuggerHelperHost) =
        wrappedState.getLauncherInfo(lifetime, helper)

    override suspend fun createModelStartInfo(lifetime: Lifetime) =
        wrappedState.createModelStartInfo(lifetime)

    override fun execute(executor: Executor?, runner: ProgramRunner<*>) =
        functionHostExecutionResult

    override suspend fun execute(
        executor: Executor,
        runner: ProgramRunner<*>,
        workerProcessHandler: DebuggerWorkerProcessHandler
    ) = functionHostExecutionResult

    override suspend fun execute(
        executor: Executor,
        runner: ProgramRunner<*>,
        workerProcessHandler: DebuggerWorkerProcessHandler,
        lifetime: Lifetime
    ) = functionHostExecutionResult

    private suspend fun getPlatformArchitecture(lifetime: Lifetime, pid: Int, project: Project): PlatformArchitecture {
        if (SystemInfo.isWindows) {
            return DebuggerHelperHost
                .getInstance(project)
                .getProcessArchitecture(lifetime, pid)
        }

        return when (CpuArch.CURRENT) {
            CpuArch.X86 -> PlatformArchitecture.X86
            CpuArch.X86_64 -> PlatformArchitecture.X64
            CpuArch.ARM64 -> PlatformArchitecture.Arm64
            else -> PlatformArchitecture.Unknown
        }
    }
}