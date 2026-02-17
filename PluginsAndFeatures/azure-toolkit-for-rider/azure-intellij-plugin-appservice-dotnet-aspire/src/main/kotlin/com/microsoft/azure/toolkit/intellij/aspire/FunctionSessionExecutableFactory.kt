/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.aspire

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.ide.browsers.StartBrowserSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import com.jetbrains.aspire.rider.launchProfiles.getWorkingDirectory
import com.jetbrains.aspire.rider.run.host.AspireHostConfiguration
import com.jetbrains.aspire.rider.sessions.findBySessionProject
import com.jetbrains.aspire.rider.sessions.getLaunchProfile
import com.jetbrains.aspire.rider.sessions.mergeArguments
import com.jetbrains.aspire.rider.sessions.mergeEnvironmentVariables
import com.jetbrains.aspire.sessions.DotNetSessionLaunchConfiguration
import com.jetbrains.aspire.settings.AspireSettings
import com.jetbrains.aspire.util.MSBuildPropertyService
import com.jetbrains.aspire.util.getStartBrowserAction
import com.jetbrains.rider.model.RdTargetFrameworkId
import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsJson
import com.jetbrains.rider.run.environment.ExecutableParameterProcessingResult
import com.jetbrains.rider.run.environment.ExecutableParameterProcessor
import com.jetbrains.rider.run.environment.ExecutableRunParameters
import com.jetbrains.rider.run.environment.ProjectProcessOptions
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.dotNetCore.DotNetCoreRuntimeType
import com.microsoft.azure.toolkit.intellij.legacy.function.daemon.AzureRunnableProjectKinds
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.getApplicationUrl
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionLocalSettings
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.FunctionCoreToolsExecutableService
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

/**
 * Factory class for creating instances of [DotNetExecutable] from a .NET Azure Function project.
 */
@Service(Service.Level.PROJECT)
class FunctionSessionExecutableFactory(private val project: Project) {
    companion object {
        fun getInstance(project: Project): FunctionSessionExecutableFactory = project.service()
        private val LOG = logger<FunctionSessionExecutableFactory>()
    }

    suspend fun createExecutable(
        launchConfiguration: DotNetSessionLaunchConfiguration,
        hostRunConfiguration: AspireHostConfiguration?
    ): DotNetExecutable? {
        val sessionProjectPath = launchConfiguration.projectPath
        val runnableProject = project.solution.runnableProjectsModel.findBySessionProject(sessionProjectPath) {
            it.kind == AzureRunnableProjectKinds.AzureFunctions
        }
        return if (runnableProject != null) {
            getExecutableForRunnableProject(
                sessionProjectPath,
                runnableProject,
                launchConfiguration,
                hostRunConfiguration
            )
        } else {
            getExecutableForExternalProject(
                sessionProjectPath,
                launchConfiguration,
                hostRunConfiguration
            )
        }
    }

    private suspend fun getExecutableForRunnableProject(
        sessionProjectPath: Path,
        runnableProject: RunnableProject,
        launchConfiguration: DotNetSessionLaunchConfiguration,
        hostRunConfiguration: AspireHostConfiguration?
    ): DotNetExecutable? {
        val output = runnableProject.projectOutputs.firstOrNull()
        if (output == null) {
            LOG.warn("Unable to find output for runnable project $sessionProjectPath")
            return null
        }

        val coreToolsExecutable = FunctionCoreToolsExecutableService.getInstance(project)
            .getCoreToolsExecutable(sessionProjectPath, output.tfm?.presentableName)
        if (coreToolsExecutable == null) {
            LOG.warn("Unable to find Function core tools executable for runnable project $sessionProjectPath")
            return null
        }

        val launchProfile = getLaunchProfile(launchConfiguration, runnableProject, project)
        val coreToolsExecutablePath = coreToolsExecutable.executablePath.absolutePathString()
        val workingDirectory = getWorkingDirectory(launchProfile, output)
        val arguments = mergeArguments(launchConfiguration.args, output.defaultArguments, launchProfile?.commandLineArgs)
        val envs = mergeEnvironmentVariables(launchConfiguration.envs, launchProfile?.environmentVariables)

        val executableParams = getExecutableParams(
            sessionProjectPath,
            coreToolsExecutablePath,
            workingDirectory,
            arguments,
            envs,
            output.tfm
        )

        val (browserSettings, browserAction) = getBrowserAction(
            launchProfile,
            arguments,
            coreToolsExecutable,
            hostRunConfiguration
        )

        LOG.trace { "Executable parameters for runnable project (${runnableProject.projectFilePath}): $executableParams" }
        LOG.trace { "Browser settings for runnable project (${runnableProject.projectFilePath}): $browserSettings" }

        return DotNetExecutable(
            executableParams.executablePath ?: coreToolsExecutablePath,
            executableParams.tfm ?: output.tfm,
            executableParams.workingDirectoryPath ?: workingDirectory,
            executableParams.commandLineArgumentString ?: arguments,
            useMonoRuntime = false,
            useExternalConsole = false,
            executableParams.environmentVariables,
            true,
            browserAction,
            coreToolsExecutablePath,
            "",
            true,
            DotNetCoreRuntimeType,
            usePty = false
        )
    }

    private suspend fun getExecutableForExternalProject(
        sessionProjectPath: Path,
        launchConfiguration: DotNetSessionLaunchConfiguration,
        hostRunConfiguration: AspireHostConfiguration?
    ): DotNetExecutable? {
        val propertyService = MSBuildPropertyService.getInstance(project)
        val properties = propertyService.getProjectRunProperties(sessionProjectPath)
        if (properties == null) {
            LOG.warn("Unable to get MSBuild properties for project $sessionProjectPath")
            return null
        }

        val coreToolsExecutable = FunctionCoreToolsExecutableService.getInstance(project)
            .getCoreToolsExecutable(sessionProjectPath, properties.targetFramework.presentableName)
        if (coreToolsExecutable == null) {
            LOG.warn("Unable to find Function core tools executable for external project $sessionProjectPath")
            return null
        }

        val launchProfile = getLaunchProfile(launchConfiguration, sessionProjectPath, project)
        val coreToolsExecutablePath = coreToolsExecutable.executablePath.absolutePathString()
        val workingDirectory = getWorkingDirectory(launchProfile, properties)
        val arguments = mergeArguments(launchConfiguration.args, properties.arguments, launchProfile?.commandLineArgs)
        val envs = mergeEnvironmentVariables(launchConfiguration.envs, launchProfile?.environmentVariables)

        val executableParams = getExecutableParams(
            sessionProjectPath,
            coreToolsExecutablePath,
            workingDirectory,
            arguments,
            envs,
            properties.targetFramework
        )

        val (browserSettings, browserAction) = getBrowserAction(
            launchProfile,
            arguments,
            coreToolsExecutable,
            hostRunConfiguration
        )

        LOG.trace { "Executable parameters for external project (${sessionProjectPath.absolutePathString()}): $executableParams" }
        LOG.trace { "Browser settings for external project (${sessionProjectPath.absolutePathString()}): $browserSettings" }

        return DotNetExecutable(
            executableParams.executablePath ?: coreToolsExecutablePath,
            executableParams.tfm ?: properties.targetFramework,
            executableParams.workingDirectoryPath ?: workingDirectory,
            executableParams.commandLineArgumentString ?: arguments,
            useMonoRuntime = false,
            useExternalConsole = false,
            executableParams.environmentVariables,
            true,
            browserAction,
            coreToolsExecutablePath,
            "",
            true,
            DotNetCoreRuntimeType
        )
    }

    private suspend fun getExecutableParams(
        sessionProjectPath: Path,
        coreToolsExecutablePath: String,
        workingDirectory: String,
        arguments: String,
        envs: Map<String, String>,
        targetFramework: RdTargetFrameworkId?
    ): ExecutableParameterProcessingResult {
        val processOptions = ProjectProcessOptions(
            sessionProjectPath,
            Path(workingDirectory)
        )
        val runParameters = ExecutableRunParameters(
            coreToolsExecutablePath,
            workingDirectory,
            arguments,
            envs,
            true,
            targetFramework
        )

        return ExecutableParameterProcessor
            .getInstance(project)
            .processEnvironment(runParameters, processOptions)
    }

    private fun getBrowserAction(
        launchProfile: LaunchSettingsJson.Profile?,
        arguments: String,
        coreToolsExecutable: FunctionCoreToolsExecutableService.FunctionCoreToolsExecutable,
        hostRunConfiguration: AspireHostConfiguration?
    ): Pair<StartBrowserSettings, (ExecutionEnvironment, RunProfile, ProcessHandler) -> Unit> {
        val browserSettings =
            getStartBrowserSettings(launchProfile, arguments, coreToolsExecutable.localSettings, hostRunConfiguration)
        val launchBrowser = AspireSettings.getInstance().doNotLaunchBrowserForProjects.not()
        val browserAction =
            if (launchBrowser && hostRunConfiguration != null) {
                getStartBrowserAction(hostRunConfiguration, browserSettings)
            } else {
                { _, _, _ -> }
            }
        return Pair(browserSettings, browserAction)
    }

    private fun getStartBrowserSettings(
        launchProfile: LaunchSettingsJson.Profile?,
        arguments: String,
        functionLocalSettings: FunctionLocalSettings?,
        hostRunConfiguration: AspireHostConfiguration?
    ): StartBrowserSettings {
        val applicationUrl = getApplicationUrl(launchProfile, arguments, functionLocalSettings)
        val webBrowser = hostRunConfiguration?.parameters?.startBrowserParameters?.browser
        val withJavaScriptDebugger =
            hostRunConfiguration?.parameters?.startBrowserParameters?.withJavaScriptDebugger == true

        return StartBrowserSettings().apply {
            browser = webBrowser
            isSelected = launchProfile?.launchBrowser == true
            url = applicationUrl
            isStartJavaScriptDebugger = withJavaScriptDebugger
        }
    }
}