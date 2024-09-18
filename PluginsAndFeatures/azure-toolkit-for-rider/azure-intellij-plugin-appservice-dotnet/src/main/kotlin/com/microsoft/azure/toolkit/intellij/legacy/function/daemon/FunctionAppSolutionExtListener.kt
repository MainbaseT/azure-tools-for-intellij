/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.daemon

import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.client.ClientProjectSession
import com.intellij.openapi.project.Project
import com.jetbrains.rd.protocol.SolutionExtListener
import com.jetbrains.rd.ui.bedsl.extensions.valueOrEmpty
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.azure.model.FunctionAppDaemonModel
import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.toolkit.intellij.legacy.function.actions.TriggerAzureFunctionAction
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.*
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.getApplicationUrl
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.getArguments
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.getEnvironmentVariables
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.getWorkingDirectory
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.*

class FunctionAppSolutionExtListener : SolutionExtListener<FunctionAppDaemonModel> {
    override fun extensionCreated(lifetime: Lifetime, session: ClientProjectSession, model: FunctionAppDaemonModel) {
        model.runFunctionApp.advise(lifetime) {
            runConfiguration(
                functionName = it.functionName,
                runnableProject = getRunnableProject(session.project, it.projectFilePath),
                executor = DefaultRunExecutor.getRunExecutorInstance(),
                project = session.project
            )
        }
        model.debugFunctionApp.advise(lifetime) {
            runConfiguration(
                functionName = it.functionName,
                runnableProject = getRunnableProject(session.project, it.projectFilePath),
                executor = DefaultDebugExecutor.getDebugExecutorInstance(),
                project = session.project
            )
        }
        model.triggerFunctionApp.advise(lifetime) {
            val triggerAction = TriggerAzureFunctionAction(
                functionName = it.functionName,
                triggerType = it.triggerType,
                httpTriggerAttribute = it.httpTriggerAttribute
            )
            ActionUtil.invokeAction(
                triggerAction,
                SimpleDataContext.getProjectContext(session.project),
                ActionPlaces.EDITOR_GUTTER_POPUP,
                null,
                null
            )
        }
    }

    private fun getRunnableProject(project: Project, expectedProjectPath: String): RunnableProject {
        val runnableProjects = project.solution.runnableProjectsModel.projects.valueOrEmpty()
        return runnableProjects.find { runnableProject ->
            runnableProject.projectFilePath == expectedProjectPath && runnableProject.kind == AzureRunnableProjectKinds.AzureFunctions
        }
            ?: throw IllegalStateException(
                "Unable to find a project to run with path: '$expectedProjectPath', available project paths: " +
                        runnableProjects.joinToString(", ", "'", "'") { it.projectFilePath }
            )
    }

    private fun runConfiguration(
        functionName: String?,
        runnableProject: RunnableProject,
        executor: Executor,
        project: Project
    ) {
        val runManager = RunManager.getInstance(project)
        val existingSettings = findExistingConfigurationSettings(functionName, runnableProject.projectFilePath, project)

        val settings = existingSettings ?: let {
            val configuration = createFunctionAppRunConfiguration(project, functionName, runnableProject)
            val newSettings =
                runManager.createConfiguration(configuration, configuration.factory as FunctionRunConfigurationFactory)

            runManager.setTemporaryConfiguration(newSettings)
            runManager.addConfiguration(newSettings)

            newSettings
        }

        runManager.selectedConfiguration = settings
        ProgramRunnerUtil.executeConfiguration(settings, executor)
    }

    private fun findExistingConfigurationSettings(
        functionName: String?,
        projectFilePath: String,
        project: Project
    ): RunnerAndConfigurationSettings? {
        val runManager = RunManager.getInstance(project)

        val configurationType = ConfigurationTypeUtil.findConfigurationType(FunctionRunConfigurationType::class.java)
        val runConfigurations = runManager.getConfigurationsList(configurationType)

        return runConfigurations.filterIsInstance<FunctionRunConfiguration>().firstOrNull { configuration ->
            configuration.parameters.projectFilePath == projectFilePath &&
                    ((functionName.isNullOrEmpty() && configuration.parameters.functionNames.isEmpty()) ||
                            configuration.parameters.functionNames == functionName)
        }?.let { configuration ->
            runManager.findSettings(configuration)
        }
    }

    private fun createFunctionAppRunConfiguration(
        project: Project,
        functionName: String?,
        runnableProject: RunnableProject
    ): FunctionRunConfiguration {
        val runManager = RunManager.getInstance(project)
        val configurationType = ConfigurationTypeUtil.findConfigurationType(FunctionRunConfigurationType::class.java)

        val factory = configurationType.factory
        val configurationName =
            if (functionName.isNullOrEmpty()) runnableProject.name
            else "$functionName (${runnableProject.fullName})"
        val settings = runManager.createConfiguration(configurationName, factory).apply {
            isTemporary = true
        }

        val configuration = settings.configuration as FunctionRunConfiguration
        patchConfigurationParameters(project, configuration, runnableProject, functionName)

        return configuration
    }

    private fun patchConfigurationParameters(
        project: Project,
        configuration: FunctionRunConfiguration,
        runnableProject: RunnableProject,
        functionName: String?
    ) {
        val projectOutput = runnableProject.projectOutputs.firstOrNull()
        val launchProfile = FunctionLaunchProfilesService
            .getInstance(project)
            .getLaunchProfiles(runnableProject)
            .firstOrNull()

        configuration.parameters.apply {
            projectFilePath = runnableProject.projectFilePath
            projectTfm = projectOutput?.tfm?.presentableName ?: ""
            profileName = launchProfile?.name ?: ""
            functionNames =  if (functionName.isNullOrEmpty()) "" else functionName
            trackArguments = true
            arguments = getArguments(launchProfile?.content, projectOutput)
            trackWorkingDirectory = true
            workingDirectory = getWorkingDirectory(launchProfile?.content, projectOutput)
            trackEnvs = true
            envs = getEnvironmentVariables(launchProfile?.content)
            useExternalConsole = false
            trackUrl = true
            startBrowserParameters.apply {
                url = getApplicationUrl(launchProfile?.content, projectOutput, null)
                startAfterLaunch = launchProfile?.content?.launchBrowser ?: false
            }
        }
    }
}