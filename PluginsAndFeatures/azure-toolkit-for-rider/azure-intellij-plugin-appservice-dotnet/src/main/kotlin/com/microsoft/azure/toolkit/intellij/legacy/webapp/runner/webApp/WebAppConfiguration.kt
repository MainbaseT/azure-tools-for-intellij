/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import com.microsoft.azure.toolkit.intellij.AppServiceProjectService
import com.microsoft.azure.toolkit.intellij.legacy.utils.*
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase

class WebAppConfiguration(private val project: Project, factory: ConfigurationFactory, name: String?) :
    LocatableConfigurationBase<WebAppConfigurationOptions>(project, factory, name) {

    var publishableProjectPath: String?
        get() = getState()?.publishableProjectPath
        set(value) {
            getState()?.publishableProjectPath = value
        }

    override fun suggestedName() = "Publish Web App"

    override fun getState() = options as? WebAppConfigurationOptions

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment) =
        WebAppDeploymentState(
            project,
            AppServiceProjectService.getInstance(project).scope.childScope("WebAppRunState"),
            this
        )

    override fun getConfigurationEditor() = WebAppSettingEditor(project)

    override fun checkConfiguration() {
        val options = getState() ?: return
        with(options) {
            isAccountSignedIn()

            if (webAppName.isNullOrEmpty()) throw RuntimeConfigurationError("Web App name is not provided")
            if (subscriptionId.isNullOrEmpty()) throw RuntimeConfigurationError("Subscription is not provided")
            if (resourceGroupName.isNullOrEmpty()) throw RuntimeConfigurationError("Resource group is not provided")
            if (region.isNullOrEmpty()) throw RuntimeConfigurationError("Region is not provided")
            if (appServicePlanName.isNullOrEmpty()) throw RuntimeConfigurationError("App Service plan name is not provided")
            if (appServicePlanResourceGroupName.isNullOrEmpty()) throw RuntimeConfigurationError("App Service plan resource group is not provided")
            if (pricingTier.isNullOrEmpty()) throw RuntimeConfigurationError("Pricing tier is not provided")
            if (pricingSize.isNullOrEmpty()) throw RuntimeConfigurationError("Pricing size is not provided")
            if (operatingSystem.isNullOrEmpty()) throw RuntimeConfigurationError("Operating system is not provided")
            if (publishableProjectPath.isNullOrEmpty()) throw RuntimeConfigurationError("Choose a project to deploy")

            val webApp = Azure.az(AzureWebApp::class.java)
                .webApps(requireNotNull(subscriptionId))
                .get(requireNotNull(webAppName), requireNotNull(resourceGroupName))
            if (webApp == null) {
                //Validate names only for the new Web Apps
                if (!isValidApplicationName(webAppName)) throw RuntimeConfigurationError(APPLICATION_VALIDATION_MESSAGE)
                if (!isValidResourceGroupName(resourceGroupName)) throw RuntimeConfigurationError(RESOURCE_GROUP_VALIDATION_MESSAGE)
                if (!isValidApplicationName(appServicePlanName)) throw RuntimeConfigurationError("App Service plan names only allow alphanumeric characters and hyphens, cannot start or end in a hyphen, and must be less than 60 chars")
                if (!isValidResourceGroupName(appServicePlanResourceGroupName)) throw RuntimeConfigurationError(RESOURCE_GROUP_VALIDATION_MESSAGE)
            }

            if (isDeployToSlot) {
                if (slotName.isNullOrEmpty()) throw RuntimeConfigurationError("Deployment slot name is not provided")

                if (webApp != null) {
                    val slot = webApp.slots().get(requireNotNull(slotName), requireNotNull(resourceGroupName))
                    if (slot == null) {
                        //Validate slot name only for the new Deployment Slots
                        if (!isValidApplicationSlotName(slotName)) throw RuntimeConfigurationError(APPLICATION_SLOT_VALIDATION_MESSAGE)
                    }
                }
            }
        }
    }

    fun setWebApp(webApp: WebAppBase<*,*,*>) {
        getState()?.apply {
            webAppName = webApp.name
            subscriptionId = webApp.subscriptionId
            resourceGroupName = webApp.resourceGroupName
            region = webApp.region.toString()
            appServicePlanName = webApp.appServicePlan?.name
            appServicePlanResourceGroupName = webApp.appServicePlan?.resourceGroupName
            pricingTier = webApp.appServicePlan?.pricingTier?.tier
            pricingSize = webApp.appServicePlan?.pricingTier?.size
            operatingSystem = webApp.runtime?.operatingSystem?.toString()
            isDeployToSlot = false
            slotName = null
            slotConfigurationSource = null
        }
    }
}