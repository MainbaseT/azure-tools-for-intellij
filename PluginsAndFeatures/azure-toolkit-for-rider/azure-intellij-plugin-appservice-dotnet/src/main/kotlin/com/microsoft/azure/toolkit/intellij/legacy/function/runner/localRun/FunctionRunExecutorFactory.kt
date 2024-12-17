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
import com.intellij.ide.actions.OpenFileAction
import com.intellij.ide.browsers.BrowserStarter
import com.intellij.ide.browsers.StartBrowserSettings
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.azure.model.AzureFunctionWorkerModel
import com.jetbrains.rider.azure.model.AzureFunctionWorkerModelRequest
import com.jetbrains.rider.azure.model.functionAppDaemonModel
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.AsyncExecutorFactory
import com.jetbrains.rider.run.configurations.project.DotNetStartBrowserParameters
import com.jetbrains.rider.run.environment.ExecutableParameterProcessor
import com.jetbrains.rider.run.environment.ExecutableRunParameters
import com.jetbrains.rider.run.environment.ProjectProcessOptions
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.msNet.MsNetRuntime
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsManager
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionsVersionMsBuildService
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionsVersionMsBuildService.Companion.PROPERTY_AZURE_FUNCTIONS_VERSION
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.resolveFunctionCoreToolsExecutable
import com.microsoft.azure.toolkit.intellij.legacy.function.daemon.AzureRunnableProjectKinds
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.*
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionLocalSettings
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionLocalSettingsService
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionWorkerRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
        val msBuildVersionProperty = FunctionsVersionMsBuildService
            .getInstance(project)
            .requestAzureFunctionsVersion(parameters.projectFilePath)
        if (msBuildVersionProperty == null) {
            LOG.warn("Could not determine project MSBuild property '${PROPERTY_AZURE_FUNCTIONS_VERSION}'")
            throw CantRunException("Can't run Azure Functions host. Could not determine project MSBuild property '${PROPERTY_AZURE_FUNCTIONS_VERSION}'")
        }
        LOG.debug { "Function version project property: $msBuildVersionProperty"}

        val projectFilePath = Path(parameters.projectFilePath)
        val functionLocalSettings = withContext(Dispatchers.Default) {
            FunctionLocalSettingsService
                .getInstance(project)
                .getFunctionLocalSettings(projectFilePath)
        }
        val workerRuntime = if (functionLocalSettings?.values?.workerRuntime == null) {
            getFunctionWorkerRuntimeFromBackendOrDefault(projectFilePath)
        } else {
            functionLocalSettings.values.workerRuntime
        }
        LOG.debug { "Worker runtime: $workerRuntime" }

        val functionsRuntimeVersion = calculateFunctionsRuntimeVersion(msBuildVersionProperty, workerRuntime)
        LOG.debug { "Functions runtime version: $functionsRuntimeVersion" }

        val functionsTfm = if (workerRuntime == FunctionWorkerRuntime.DOTNET) parameters.projectTfm else null
        LOG.debug { "Functions target framework: $functionsTfm" }

        val functionCoreToolsPath =  withContext(Dispatchers.Default) {
            withBackgroundProgress(project, "Getting Azure Functions core tools") {
                FunctionCoreToolsManager
                    .getInstance()
                    .getFunctionCoreToolsPathOrDownloadForVersion(functionsRuntimeVersion, functionsTfm)
            }
        }
        if (functionCoreToolsPath == null) {
            LOG.warn("Unable to find or download Function core tools for the project '${parameters.projectFilePath}'")
            throw CantRunException("Can't run Azure Functions host. Unable to find locally or download Function core tools")
        }

        val functionCoreToolsExecutablePath = functionCoreToolsPath.resolveFunctionCoreToolsExecutable()
        LOG.trace { "Function core tools executable path: $functionCoreToolsExecutablePath" }

        val dotNetExecutable = getDotNetExecutable(functionCoreToolsExecutablePath, functionLocalSettings)
            ?: throw CantRunException("Can't run Azure Functions host. Unable to create .NET executable")

        LOG.debug("Patching host.json file to reflect run configuration parameters")
        HostJsonPatcher
            .getInstance()
            .tryPatchHostJsonFile(dotNetExecutable.workingDirectory, parameters.functionNames)

        val runtimeToExecute = if (functionsRuntimeVersion.equals("v1", ignoreCase = true)) {
            MsNetRuntime()
        } else {
            FunctionNetCoreRuntime(functionCoreToolsExecutablePath, workerRuntime, lifetime)
        }

        LOG.debug { "Configuration will be executed on ${runtimeToExecute.javaClass.name}" }
        return when (executorId) {
            DefaultRunExecutor.EXECUTOR_ID -> runtimeToExecute.createRunState(dotNetExecutable, environment)
            DefaultDebugExecutor.EXECUTOR_ID -> runtimeToExecute.createDebugState(dotNetExecutable, environment)
            else -> throw CantRunException("Unsupported executor $executorId")
        }
    }

    private fun calculateFunctionsRuntimeVersion(msBuildVersionProperty: String, workerRuntime: FunctionWorkerRuntime) =
        if (msBuildVersionProperty.equals("v0", ignoreCase = true)) msBuildVersionProperty
        else if (msBuildVersionProperty.equals("v1", ignoreCase = true)) msBuildVersionProperty
        else if (msBuildVersionProperty.equals("v2", ignoreCase = true)) msBuildVersionProperty
        else if (msBuildVersionProperty.equals("v3", ignoreCase = true)) msBuildVersionProperty
        else if (msBuildVersionProperty.equals("v4", ignoreCase = true)) {
            when (workerRuntime) {
                FunctionWorkerRuntime.DOTNET -> "v0"
                FunctionWorkerRuntime.DOTNET_ISOLATED -> "v4"
            }
        } else {
            error("Function runtime version not supported: $workerRuntime")
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

        val launchProfile = FunctionLaunchProfilesService
            .getInstance(project)
            .getLaunchProfileByName(runnableProject, parameters.profileName)

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
            if (parameters.trackUrl) getApplicationUrl(launchProfile?.content, projectOutput, functionLocalSettings)
            else parameters.startBrowserParameters.url

        val projectProcessOptions = ProjectProcessOptions(
            File(runnableProject.projectFilePath),
            File(effectiveWorkingDirectory)
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
            getStartBrowserAction(effectiveUrl, parameters.startBrowserParameters),
            coreToolsExecutablePath,
            "",
            true
        )
    }

    private suspend fun getFunctionWorkerRuntimeFromBackendOrDefault(projectFilePath: Path): FunctionWorkerRuntime {
        val functionWorkerModel = project.solution
            .functionAppDaemonModel
            .getAzureFunctionWorkerModel
            .startSuspending(AzureFunctionWorkerModelRequest(projectFilePath.absolutePathString()))

        return when(functionWorkerModel) {
            AzureFunctionWorkerModel.Default -> FunctionWorkerRuntime.DOTNET
            AzureFunctionWorkerModel.Isolated -> FunctionWorkerRuntime.DOTNET_ISOLATED
            AzureFunctionWorkerModel.Unknown -> {
                showNotificationAboutDefaultRuntime(projectFilePath)
                FunctionWorkerRuntime.DOTNET
            }
        }
    }

    private fun showNotificationAboutDefaultRuntime(projectPath: Path) {
        val settingsFilePath = FunctionLocalSettingsService.getInstance(project).getLocalSettingFilePath(projectPath)

        Notification(
            "Azure AppServices",
            "Unable to find the Function worker runtime",
            "Unable to find the `FUNCTIONS_WORKER_RUNTIME` variable in the `local.settings.json` file. The Default worker model will be applied",
            NotificationType.WARNING
        )
            .addAction(NotificationAction.createSimple("Open local.settings.json") {
                OpenFileAction.openFile(settingsFilePath.absolutePathString(), project)
            })
            .notify(project)
    }

    private fun getStartBrowserAction(
        browserUrl: String,
        params: DotNetStartBrowserParameters
    ): (ExecutionEnvironment, RunProfile, ProcessHandler) -> Unit =
        { _, runProfile, processHandler ->
            if (params.startAfterLaunch && runProfile is RunConfiguration) {
                val startBrowserSettings = StartBrowserSettings().apply {
                    isSelected = params.startAfterLaunch
                    url = browserUrl
                    browser = params.browser
                    isStartJavaScriptDebugger = params.withJavaScriptDebugger
                }
                BrowserStarter(runProfile, startBrowserSettings, processHandler).start()
            }
        }
}