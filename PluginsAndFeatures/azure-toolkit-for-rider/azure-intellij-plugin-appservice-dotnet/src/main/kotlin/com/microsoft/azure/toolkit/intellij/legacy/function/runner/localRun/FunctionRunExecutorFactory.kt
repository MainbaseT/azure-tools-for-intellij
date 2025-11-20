/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("DialogTitleCapitalization")

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.CantRunException
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.ide.browsers.BrowserStarter
import com.intellij.ide.browsers.StartBrowserSettings
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.AsyncExecutorFactory
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsJsonService
import com.jetbrains.rider.run.configurations.project.DotNetStartBrowserParameters
import com.jetbrains.rider.run.environment.ExecutableParameterProcessor
import com.jetbrains.rider.run.environment.ExecutableRunParameters
import com.jetbrains.rider.run.environment.ProjectProcessOptions
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.msNet.MsNetRuntime
import com.microsoft.azure.toolkit.intellij.legacy.function.daemon.AzureRunnableProjectKinds
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.*
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionLocalSettings
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class FunctionRunExecutorFactory(
    private val project: Project,
    private val parameters: FunctionRunConfigurationParameters
) : AsyncExecutorFactory {
    companion object {
        private val LOG = logger<FunctionRunExecutorFactory>()
    }

    override suspend fun create(
        executorId: String,
        environment: ExecutionEnvironment,
        lifetime: Lifetime
    ): RunProfileState {
        val projectFilePath = Path(parameters.projectFilePath)

        val coreToolsExecutable = FunctionCoreToolsExecutableService.getInstance(project)
            .getCoreToolsExecutable(projectFilePath, parameters.projectTfm)
            ?: throw CantRunException("Can't run Azure Functions host. Unable to find locally or download Function core tools")

        val dotNetExecutable = getDotNetExecutable(
            coreToolsExecutable.executablePath,
            coreToolsExecutable.localSettings
        ) ?: throw CantRunException("Can't run Azure Functions host. Unable to create .NET executable")

        LOG.debug("Patching host.json file to reflect run configuration parameters")
        HostJsonPatcher
            .getInstance()
            .tryPatchHostJsonFile(dotNetExecutable.workingDirectory, parameters.functionNames)

        val runtimeToExecute = if (coreToolsExecutable.functionsRuntimeVersion.equals("v1", ignoreCase = true)) {
            MsNetRuntime()
        } else {
            FunctionNetCoreRuntime(coreToolsExecutable.executablePath, coreToolsExecutable.functionRuntime, lifetime)
        }

        LOG.debug { "Configuration will be executed on ${runtimeToExecute.javaClass.name}" }
        return when (executorId) {
            DefaultRunExecutor.EXECUTOR_ID -> runtimeToExecute.createRunState(dotNetExecutable, environment)
            DefaultDebugExecutor.EXECUTOR_ID -> runtimeToExecute.createDebugState(dotNetExecutable, environment)
            else -> throw CantRunException("Unsupported executor $executorId")
        }
    }

    private suspend fun getDotNetExecutable(
        functionCoreToolsExecutablePath: Path,
        functionLocalSettings: FunctionLocalSettings?
    ): DotNetExecutable? {
        val runnableProjects = project.solution.runnableProjectsModel.projects.valueOrNull
        if (runnableProjects.isNullOrEmpty()) return null

        val runnableProject = runnableProjects.singleOrNull {
            it.projectFilePath == parameters.projectFilePath && it.kind == AzureRunnableProjectKinds.AzureFunctions
        }
        if (runnableProject == null) {
            LOG.warn("Unable to get the runnable project with path ${parameters.projectFilePath}")
            return null
        }

        val coreToolsExecutablePath = functionCoreToolsExecutablePath.absolutePathString()

        val projectOutput = runnableProject
            .projectOutputs
            .singleOrNull { it.tfm?.presentableName == parameters.projectTfm }
            ?: runnableProject.projectOutputs.firstOrNull()

        val launchProfile = LaunchSettingsJsonService
            .getInstance(project)
            .getProjectLaunchProfileByName(runnableProject, parameters.profileName)

        val effectiveArguments =
            if (parameters.trackArguments) getArguments(launchProfile?.content, projectOutput)
            else parameters.arguments

        val effectiveWorkingDirectory =
            if (parameters.trackWorkingDirectory) getWorkingDirectory(launchProfile?.content, projectOutput)
            else parameters.workingDirectory

        val effectiveEnvs =
            if (parameters.trackEnvs) getEnvironmentVariables(launchProfile?.content)
            else parameters.envs

        val effectiveUrl =
            if (parameters.trackUrl) getApplicationUrl(launchProfile?.content, effectiveArguments, functionLocalSettings)
            else parameters.startBrowserParameters.url

        val effectiveLaunchBrowser =
            if (parameters.trackBrowserLaunch) getLaunchBrowserFlag(launchProfile?.content)
            else parameters.startBrowserParameters.startAfterLaunch

        val projectProcessOptions = ProjectProcessOptions(
            Path(runnableProject.projectFilePath),
            Path(effectiveWorkingDirectory)
        )

        val runParameters = ExecutableRunParameters(
            coreToolsExecutablePath,
            effectiveWorkingDirectory,
            effectiveArguments,
            effectiveEnvs,
            true,
            projectOutput?.tfm
        )

        val executableParameters = ExecutableParameterProcessor
            .getInstance(project)
            .processEnvironment(runParameters, projectProcessOptions)

        LOG.trace { "Function executable: $executableParameters" }

        return DotNetExecutable(
            executableParameters.executablePath ?: coreToolsExecutablePath,
            executableParameters.tfm ?: projectOutput?.tfm,
            executableParameters.workingDirectoryPath ?: effectiveWorkingDirectory,
            executableParameters.commandLineArgumentString ?: effectiveArguments,
            false,
            parameters.useExternalConsole,
            executableParameters.environmentVariables,
            true,
            getStartBrowserAction(effectiveUrl, effectiveLaunchBrowser, parameters.startBrowserParameters),
            coreToolsExecutablePath,
            "",
            true
        )
    }

    private fun getStartBrowserAction(
        browserUrl: String,
        launchBrowser: Boolean,
        params: DotNetStartBrowserParameters
    ): (ExecutionEnvironment, RunProfile, ProcessHandler) -> Unit =
        { _, runProfile, processHandler ->
            if (launchBrowser && runProfile is RunConfiguration) {
                val startBrowserSettings = StartBrowserSettings().apply {
                    isSelected = true
                    url = browserUrl
                    browser = params.browser
                    isStartJavaScriptDebugger = params.withJavaScriptDebugger
                }
                BrowserStarter(runProfile, startBrowserSettings, processHandler).start()
            }
        }
}