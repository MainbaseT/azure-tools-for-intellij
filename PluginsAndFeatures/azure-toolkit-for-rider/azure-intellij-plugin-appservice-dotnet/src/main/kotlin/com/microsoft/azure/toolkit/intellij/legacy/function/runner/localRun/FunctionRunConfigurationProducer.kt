/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.systemIndependentPath
import com.intellij.psi.PsiElement
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.workspace.getFile
import com.jetbrains.rider.run.configurations.getSelectedProject
import com.microsoft.azure.toolkit.intellij.legacy.function.daemon.AzureRunnableProjectKinds
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.*
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.getApplicationUrl
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.getArguments
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.getEnvironmentVariables
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.getWorkingDirectory

class FunctionRunConfigurationProducer : LazyRunConfigurationProducer<FunctionRunConfiguration>() {
    override fun getConfigurationFactory() =
        ConfigurationTypeUtil
            .findConfigurationType(FunctionRunConfigurationType::class.java)
            .factory

    override fun isConfigurationFromContext(
        configuration: FunctionRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val selectedProjectFilePathInvariant =
            context.getSelectedProject()?.getFile()?.systemIndependentPath ?: return false

        val projects = context.project.solution.runnableProjectsModel.projects.valueOrNull ?: return false
        val runnableProject = projects.firstOrNull {
            it.kind == AzureRunnableProjectKinds.AzureFunctions &&
                    FileUtil.toSystemIndependentName(it.projectFilePath) == selectedProjectFilePathInvariant &&
                    FileUtil.toSystemIndependentName(configuration.parameters.projectFilePath) == selectedProjectFilePathInvariant
        }

        return runnableProject != null
    }

    override fun setupConfigurationFromContext(
        configuration: FunctionRunConfiguration,
        context: ConfigurationContext,
        ref: Ref<PsiElement>
    ): Boolean {
        val selectedProjectFilePathInvariant = context.getSelectedProject()?.getFile()?.systemIndependentPath
            ?: return false

        val projects = context.project.solution.runnableProjectsModel.projects.valueOrNull ?: return false
        val runnableProject = projects.firstOrNull {
            it.kind == AzureRunnableProjectKinds.AzureFunctions &&
                    FileUtil.toSystemIndependentName(it.projectFilePath) == selectedProjectFilePathInvariant
        } ?: return false

        if (configuration.name.isEmpty()) {
            configuration.name = runnableProject.name
        }

        val projectOutput = runnableProject.projectOutputs.firstOrNull()
        val launchProfile = FunctionLaunchProfilesService
            .getInstance(context.project)
            .getLaunchProfiles(runnableProject)
            .firstOrNull()

        configuration.parameters.apply {
            projectFilePath = selectedProjectFilePathInvariant
            projectTfm = projectOutput?.tfm?.presentableName ?: ""
            profileName = launchProfile?.name ?: ""
            functionNames = ""
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
                startAfterLaunch = launchProfile?.content?.launchBrowser == true
            }
        }

        return true
    }

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        return !other.isProducedBy(FunctionRunConfigurationProducer::class.java)
    }
}