/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.rider.run.ConsoleKind
import com.jetbrains.rider.run.TerminalProcessHandler
import com.jetbrains.rider.run.createConsole
import com.jetbrains.rider.run.createRunCommandLine
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.DotNetRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Service(Service.Level.PROJECT)
class FunctionHostDebugLauncher(private val project: Project) {
    companion object {
        fun getInstance(project: Project): FunctionHostDebugLauncher = project.service()

        private val LOG = logger<FunctionHostDebugLauncher>()

        private val waitDuration = 1.minutes

        //See: https://learn.microsoft.com/en-us/azure/azure-functions/functions-core-tools-reference?tabs=v2#func-start
        //When set to true, pauses the dotnet worker process until a debugger is attached from the dotnet isolated project being debugged.
        private const val DOTNET_ISOLATED_DEBUG_ARGUMENT = "--dotnet-isolated-debug"

        //Emits console logs as JSON, when possible.
        private const val ENABLE_JSON_OUTPUT_ARGUMENT = "--enable-json-output"

        //File path for the JSON output.
        private const val JSON_OUTPUT_FILE_ARGUMENT = "--json-output-file"

        private const val COMPLUS_FORCEENC = "COMPLUS_FORCEENC"
        private const val DOTNET_MODIFIABLE_ASSEMBLIES = "DOTNET_MODIFIABLE_ASSEMBLIES"
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    /**
     * Starts a function host process in a dotnet-isolated debug mode and waits for the pid output.
     *
     * @param executable The [DotNetExecutable] object representing the function host executable to run.
     * @param dotNetRuntime The [DotNetRuntime] object representing the .NET runtime.
     * @return A Pair containing the [ExecutionResult] and the process id, if obtained.
     */
    suspend fun startProcessWaitingForDebugger(
        executable: DotNetExecutable,
        dotNetRuntime: DotNetRuntime
    ): Pair<ExecutionResult, Int?> {
        val temporaryPidFile = withContext(Dispatchers.IO) { createTemporaryPidFile() }
        val programParameters = modifyProgramParameters(executable.programParameterString, temporaryPidFile)
        LOG.debug { "Program parameters: $programParameters" }
        val environmentVariables = modifyEnvironmentVariables(executable.environmentVariables)
        LOG.debug { "Environment variables: ${environmentVariables.entries.joinToString()}" }

        val processExecutable = executable.copy(
            programParameterString = ParametersListUtil.join(programParameters),
            environmentVariables = environmentVariables
        )

        val commandLine = processExecutable.createRunCommandLine(dotNetRuntime)
        LOG.debug { "Prepared commandLine: ${commandLine.commandLineString}" }
        val handler = TerminalProcessHandler(project, commandLine, commandLine.commandLineString)
        val console = createConsole(ConsoleKind.Normal, handler, project)
        val executionResult = DefaultExecutionResult(console, handler)

        withContext(Dispatchers.EDT) {
            handler.startNotify()
        }

        val pid = waitForThePrintedPid(temporaryPidFile)
        if (pid == null) {
            LOG.warn("Unable to obtain process id")
        }

        return executionResult to pid
    }

    private fun modifyProgramParameters(programParameterString: String, temporaryPidFile: File): List<String> {
        val programParameters = ParametersListUtil.parse(programParameterString)

        if (!programParameters.contains(DOTNET_ISOLATED_DEBUG_ARGUMENT)) {
            programParameters.add(DOTNET_ISOLATED_DEBUG_ARGUMENT)
        }

        if (!programParameters.contains(ENABLE_JSON_OUTPUT_ARGUMENT)) {
            programParameters.add(ENABLE_JSON_OUTPUT_ARGUMENT)
        }

        if (!programParameters.contains(JSON_OUTPUT_FILE_ARGUMENT)) {
            programParameters.add(JSON_OUTPUT_FILE_ARGUMENT)
            programParameters.add(temporaryPidFile.absolutePath)
        } else {
            val argumentIndex = programParameters.indexOf(JSON_OUTPUT_FILE_ARGUMENT)
            if (argumentIndex < programParameters.size - 1) {
                programParameters[argumentIndex + 1] = temporaryPidFile.absolutePath
            } else {
                programParameters.add(temporaryPidFile.absolutePath)
            }
        }

        return programParameters
    }

    private fun modifyEnvironmentVariables(envs: Map<String, String>): Map<String, String> {
        return envs + mapOf(
            COMPLUS_FORCEENC to "1",
            DOTNET_MODIFIABLE_ASSEMBLIES to "debug"
        )
    }

    private fun createTemporaryPidFile(): File {
        // We will need to read the worker process PID, so the debugger can later attach to it.
        // We are using this file to read the PID,
        // (see https://github.com/Azure/azure-functions-dotnet-worker/issues/900).
        // Example contents: { "name":"dotnet-worker-startup", "workerProcessId":28460 }
        return FileUtil.createTempFile(
            File(FileUtil.getTempDirectory()),
            "Rider-AzureFunctions-IsolatedWorker-",
            "json.pid",
            true,
            true
        )
    }

    private suspend fun waitForThePrintedPid(temporaryPidFile: File): Int? {
        var timeout = 0.milliseconds
        while (timeout <= waitDuration) {
            val pidFromJsonOutput = readPidFromJsonOutput(temporaryPidFile)
            if (pidFromJsonOutput != null) {
                LOG.debug("Got functions isolated worker process id from JSON output")
                LOG.debug { "Functions isolated worker process id: $pidFromJsonOutput" }
                return pidFromJsonOutput
            }

            delay(500)
            timeout += 500.milliseconds
        }

        val pidFromJsonOutput = readPidFromJsonOutput(temporaryPidFile)
        return pidFromJsonOutput
    }

    private suspend fun readPidFromJsonOutput(pidFile: File): Int? {
        if (!pidFile.exists()) return null
        val jsonText = withContext(Dispatchers.IO) { pidFile.readText() }
        if (jsonText.isEmpty()) return null
        val content = json.decodeFromString<JsonOutputFile>(jsonText)
        return content.workerProcessId
    }

    @Serializable
    private data class JsonOutputFile(
        val name: String,
        val workerProcessId: Int
    )
}