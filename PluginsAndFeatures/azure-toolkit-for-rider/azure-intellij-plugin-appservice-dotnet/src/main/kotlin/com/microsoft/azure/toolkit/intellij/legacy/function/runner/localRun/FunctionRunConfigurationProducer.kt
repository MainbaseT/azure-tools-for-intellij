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
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsJsonService
import com.microsoft.azure.toolkit.intellij.legacy.function.daemon.AzureRunnableProjectKinds
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.*

class FunctionRunConfigurationProducer : LazyRunConfigurationProducer<FunctionRunConfiguration>() {
    override fun getConfigurationFactory() =
        ConfigurationTypeUtil
            .findConfigurationType(FunctionRunConfigurationType::class.java)
            .factory

    override fun isConfigurationFromContext(
        configuration: FunctionRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val selectedProjectFilePath = context.getSelectedProject()?.getFile()?.systemIndependentPath ?: return false

        val projects = context.project.solution.runnableProjectsModel.projects.valueOrNull ?: return false
        val configurationProjectFilePath =  FileUtil.toSystemIndependentName(configuration.parameters.projectFilePath)
        val runnableProject = projects.firstOrNull {
            it.kind == AzureRunnableProjectKinds.AzureFunctions &&
                    FileUtil.toSystemIndependentName(it.projectFilePath) == selectedProjectFilePath &&
                    configurationProjectFilePath == selectedProjectFilePath
        }

        return runnableProject != null
    }

    override fun setupConfigurationFromContext(
        configuration: FunctionRunConfiguration,
        context: ConfigurationContext,
        ref: Ref<PsiElement>
    ): Boolean {
        val selectedProjectFilePath = context.getSelectedProject()?.getFile()?.systemIndependentPath
            ?: return false

        val projects = context.project.solution.runnableProjectsModel.projects.valueOrNull ?: return false
        val runnableProject = projects.firstOrNull {
            it.kind == AzureRunnableProjectKinds.AzureFunctions &&
                    FileUtil.toSystemIndependentName(it.projectFilePath) == selectedProjectFilePath
        } ?: return false

        if (configuration.name.isEmpty()) {
            configuration.name = runnableProject.name
        }

        val projectOutput = runnableProject
            .projectOutputs
            .firstOrNull()
        val profile = LaunchSettingsJsonService
            .getInstance(context.project)
            .getFirstOrNullLaunchProfileProfile(runnableProject)

        configuration.parameters.setUpFromRunnableProject(
            runnableProject,
            projectOutput,
            profile
        )

        return true
    }

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        return !other.isProducedBy(FunctionRunConfigurationProducer::class.java)
    }
}