/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper
import com.microsoft.azure.toolkit.intellij.common.*
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBox
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.ui.components.DeploymentSlotComboBox
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.DeploymentSlotConfig
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.common.model.Region
import java.awt.event.ItemEvent
import javax.swing.JPanel

class FunctionDeploymentSettingsEditor(private val project: Project) :
    SettingsEditor<FunctionDeploymentConfiguration>() {

    private val panel: JPanel
    private lateinit var functionAppComboBox: Cell<FunctionAppComboBox>
    private lateinit var deployToSlotCheckBox: Cell<JBCheckBox>
    private lateinit var deploymentSlotComboBox: Cell<DeploymentSlotComboBox>
    private lateinit var dotnetProjectComboBox: Cell<AzureDotnetProjectComboBox>
    private lateinit var configurationAndPlatformComboBox: Cell<LabeledComponent<ComboBox<PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform?>>>
    private lateinit var openBrowserCheckBox: Cell<JBCheckBox>

    private var isLoading: Boolean = false

    init {
        panel = panel {
            row("Project:") {
                dotnetProjectComboBox = dotnetProjectComboBox(project) { it.isAzureFunction }
                    .align(Align.FILL)
            }
            row("Configuration:") {
                configurationAndPlatformComboBox = configurationAndPlatformComboBox(project)
                    .align(Align.FILL)
            }
            row("Function:") {
                functionAppComboBox = functionAppComboBox(project)
                    .align(Align.FILL)
                    .resizableColumn()
                Disposer.register(this@FunctionDeploymentSettingsEditor, functionAppComboBox.component)
            }
            row {
                deployToSlotCheckBox = checkBox("Deploy to Slot:")
                deploymentSlotComboBox = cell(DeploymentSlotComboBox(project))
                    .enabledIf(deployToSlotCheckBox.selected)
                    .align(Align.FILL)
            }.bottomGap(BottomGap.MEDIUM)
            row {
                openBrowserCheckBox = checkBox("Open browser after deployment")
            }
        }

        dotnetProjectComboBox.component.apply {
            reloadItems()
            addValueChangedListener(::onSelectProject)
        }
        functionAppComboBox.component.apply {
            addValueChangedListener(::onSelectFunctionApp)
        }
        deployToSlotCheckBox.component.apply {
            addItemListener(::onSlotCheckBoxChanged)
        }
    }

    private fun onSelectProject(value: PublishableProjectModel) {
        if (isLoading) return

        val onNetFramework = !value.isDotNetCore || value.projectOutputs.all { it.tfmInMsbuildFormat == "net48" }
        functionAppComboBox.component.targetProjectOnNetFramework = onNetFramework
    }

    private fun onSelectFunctionApp(value: FunctionAppConfig?) {
        if (isLoading) return

        if (value == null) {
            deployToSlotCheckBox.enabled(false)
            deployToSlotCheckBox.component.isSelected = false
            return
        }

        val resource = getResource(value, null)
        val isDraftResource = resource == null || !resource.exists()

        deployToSlotCheckBox.enabled(!isDraftResource)

        if (isDraftResource) {
            deployToSlotCheckBox.component.isSelected = false
        }
        else if (resource is FunctionApp) {
            val hasDeploymentSlots = resource.slots().list().isNotEmpty()
            deployToSlotCheckBox.component.isSelected = hasDeploymentSlots
        }

        deploymentSlotComboBox.component.setAppService(resource?.id)
    }

    private fun onSlotCheckBoxChanged(event: ItemEvent) {
        if (isLoading) return

        if (deployToSlotCheckBox.component.isSelected) {
            deploymentSlotComboBox.component.reloadItems()
        } else if (!deployToSlotCheckBox.component.isSelected) {
            deploymentSlotComboBox.component.clear()
        }
    }

    override fun resetEditorFrom(configuration: FunctionDeploymentConfiguration) {
        isLoading = true

        val state = configuration.state ?: return

        val publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
            .firstOrNull { p -> p.projectFilePath == state.publishableProjectPath }
        if (publishableProject != null) {
            dotnetProjectComboBox.component.setProject(publishableProject)
        }
        configurationAndPlatformComboBox.component.component.setPublishConfiguration(
            state.projectConfiguration ?: "",
            state.projectPlatform ?: ""
        )

        val region = if (state.region.isNullOrEmpty()) null else Region.fromName(requireNotNull(state.region))
        val pricingTier = PricingTier(state.pricingTier, state.pricingSize)
        val operatingSystem = OperatingSystem.fromString(state.operatingSystem)

        val functionAppConfig = FunctionAppConfig
            .builder()
            .appName(state.functionAppName)
            .subscriptionId(state.subscriptionId)
            .resourceGroup(state.resourceGroupName)
            .region(region)
            .servicePlanName(state.appServicePlanName)
            .servicePlanResourceGroup(state.appServicePlanResourceGroupName)
            .pricingTier(pricingTier)
            .runtime(RuntimeConfig().apply { os = operatingSystem })
            .storageAccountName(state.storageAccountName)
            .storageAccountResourceGroup(state.storageAccountResourceGroup)
            .build()
        functionAppComboBox.component.setConfigModel(functionAppConfig)
        functionAppComboBox.component.setValue { AppServiceComboBox.isSameApp(it, functionAppConfig) }

        if (state.isDeployToSlot) {
            deployToSlotCheckBox.selected(true)
            val slotConfig = DeploymentSlotConfig
                .builder()
                .name(state.slotName)
                .configurationSource(state.slotConfigurationSource)
                .build()
            deploymentSlotComboBox.component.value = slotConfig
        } else {
            deployToSlotCheckBox.selected(false)
            deploymentSlotComboBox.component.clear()
        }

        openBrowserCheckBox.component.isSelected = state.openBrowser

        isLoading = false

        functionAppComboBox.component.reloadItems()
    }

    override fun applyEditorTo(configuration: FunctionDeploymentConfiguration) {
        val state = configuration.state ?: return

        val functionConfig = functionAppComboBox.component.value
        val deployToSlot = deployToSlotCheckBox.component.isSelected
        val slotConfig = deploymentSlotComboBox.component.value

        state.apply {
            functionAppName = functionConfig?.appName
            subscriptionId = functionConfig?.subscriptionId
            resourceGroupName = functionConfig?.resourceGroup
            region = functionConfig?.region?.toString()
            appServicePlanName = functionConfig?.servicePlanName
            appServicePlanResourceGroupName = functionConfig?.servicePlanResourceGroup
            pricingTier = functionConfig?.pricingTier?.tier
            pricingSize = functionConfig?.pricingTier?.size
            operatingSystem = functionConfig?.runtime?.os?.toString()
            isDeployToSlot = deployToSlot
            if (!deployToSlot || slotConfig == null) {
                slotName = null
                slotConfigurationSource = null
            } else {
                slotName = slotConfig.name
                slotConfigurationSource = slotConfig.configurationSource
            }
            storageAccountName = functionConfig?.storageAccountName
            storageAccountResourceGroup = functionConfig?.storageAccountResourceGroup
            publishableProjectPath = dotnetProjectComboBox.component.value?.projectFilePath
            val (config, platform) = configurationAndPlatformComboBox.component.component.getPublishConfiguration()
            projectConfiguration = config
            projectPlatform = platform
            openBrowser = openBrowserCheckBox.component.isSelected
        }
    }

    override fun createEditor() = panel

    private fun getResource(config: FunctionAppConfig, slot: String?): FunctionAppBase<*, *, *>? {
        if (config.appName.isNullOrEmpty()) return null

        val functionApp = Azure.az(AzureFunctions::class.java)
            .functionApps(config.subscriptionId)
            .get(config.appName, config.resourceGroup)

        return if (slot.isNullOrEmpty() || functionApp == null) functionApp
        else functionApp.slots().get(slot, config.resourceGroup)
    }
}