/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp

import com.intellij.execution.ExecutionException
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.progress.checkCanceled
import com.intellij.openapi.project.Project
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.toolkit.intellij.appservice.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.intellij.appservice.functionapp.DotNetFunctionAppDeploymentSlotDraft
import com.microsoft.azure.toolkit.intellij.appservice.webapp.CreateDotNetWebAppTask
import com.microsoft.azure.toolkit.intellij.appservice.webapp.DotNetAppServiceConfig
import com.microsoft.azure.toolkit.intellij.appservice.DotNetAppServiceDeployer
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureDeploymentState
import com.microsoft.azure.toolkit.intellij.legacy.getStackAndVersion
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot
import com.microsoft.azure.toolkit.lib.common.model.AzResource
import com.microsoft.azure.toolkit.lib.common.model.Region
import kotlinx.coroutines.CoroutineScope

class WebAppDeploymentState(
    project: Project,
    scope: CoroutineScope,
    private val webAppConfiguration: WebAppConfiguration
) : AzureDeploymentState<WebAppBase<*, *, *>>(project, scope) {

    override suspend fun executeSteps(processHandler: RunProcessHandler): WebAppBase<*, *, *> {
        processHandlerMessenger?.info("Start Web App deployment...")

        val options = requireNotNull(webAppConfiguration.state)
        val publishableProjectPath = options.publishableProjectPath
            ?: throw ExecutionException("Project is not defined")
        val publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
            .firstOrNull { it.projectFilePath == publishableProjectPath }
            ?: throw ExecutionException("Project is not defined")

        checkCanceled()

        val config = createDotNetAppServiceConfig(publishableProject, options)
        val createTask = CreateDotNetWebAppTask(config, processHandlerMessenger)
        val deployTarget = createTask.execute()

        if (deployTarget is AzResource.Draft<*, *>) {
            deployTarget.reset()
        }

        checkCanceled()

        DotNetAppServiceDeployer
            .getInstance(project)
            .deploy(
                deployTarget,
                publishableProject,
                options.projectConfiguration,
                options.projectPlatform,
            ) { processHandlerMessenger?.info(it) }
            .getOrThrow()

        return deployTarget
    }

    private fun createDotNetAppServiceConfig(
        publishableProject: PublishableProjectModel,
        options: WebAppConfigurationOptions
    ) = DotNetAppServiceConfig().apply {
        subscriptionId(options.subscriptionId)
        resourceGroup(options.resourceGroupName)
        region(Region.fromName(requireNotNull(options.region)))
        servicePlanName(options.appServicePlanName)
        servicePlanResourceGroup(options.appServicePlanResourceGroupName)
        val pricingTier = PricingTier(options.pricingTier, options.pricingSize)
        pricingTier(pricingTier)
        appName(options.webAppName)
        deploymentSlotName(options.slotName)
        val configurationSource = when (options.slotConfigurationSource) {
            "Do not clone settings" -> DotNetFunctionAppDeploymentSlotDraft.CONFIGURATION_SOURCE_NEW
            "parent" -> DotNetFunctionAppDeploymentSlotDraft.CONFIGURATION_SOURCE_PARENT
            null -> null
            else -> options.slotConfigurationSource
        }
        deploymentSlotConfigurationSource(configurationSource)
        val os = OperatingSystem.fromString(options.operatingSystem)
        runtime = createRuntimeConfig(os)
        dotnetRuntime = createDotNetRuntimeConfig(publishableProject, os)
    }

    private fun createRuntimeConfig(os: OperatingSystem) =
        RuntimeConfig().apply {
            this.os = os
        }

    private fun createDotNetRuntimeConfig(publishableProject: PublishableProjectModel, os: OperatingSystem) =
        DotNetRuntimeConfig().apply {
            os(os)
            isDocker = false
            val stackAndVersion = publishableProject.getStackAndVersion(project, os, false)
            stack = stackAndVersion?.first
            frameworkVersion = stackAndVersion?.second
        }

    override fun onSuccess(result: WebAppBase<*, *, *>, processHandler: RunProcessHandler) {
        val options = requireNotNull(webAppConfiguration.state)

        updateConfigurationDataModel(result)
        processHandlerMessenger?.info("Deployment was successful, but the app may still be starting.")

        val url = "https://${result.hostName}"
        processHandlerMessenger?.info("URL: $url")
        if (options.openBrowser) {
            BrowserUtil.open(url)
        }

        processHandler.notifyComplete()
    }

    private fun updateConfigurationDataModel(app: WebAppBase<*, *, *>) {
        webAppConfiguration.state?.apply {
            if (app is WebAppDeploymentSlot) {
                slotName = app.name
                slotConfigurationSource = null
            }
        }
    }
}