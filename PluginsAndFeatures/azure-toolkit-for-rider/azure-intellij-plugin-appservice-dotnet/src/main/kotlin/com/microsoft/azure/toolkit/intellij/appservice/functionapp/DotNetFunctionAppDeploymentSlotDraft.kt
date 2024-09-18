/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.functionapp

import com.azure.core.management.exception.ManagementException
import com.azure.resourcemanager.appservice.fluent.models.SitePatchResourceInner
import com.azure.resourcemanager.appservice.models.DeploymentSlotBase
import com.azure.resourcemanager.appservice.models.FunctionApp
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlot
import com.azure.resourcemanager.appservice.models.WebAppBase
import com.microsoft.azure.toolkit.intellij.appservice.DotNetRuntime
import com.microsoft.azure.toolkit.intellij.appservice.getDotNetRuntime
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlot
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlotModule
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.model.AzResource

class DotNetFunctionAppDeploymentSlotDraft : FunctionAppDeploymentSlot,
    AzResource.Draft<FunctionAppDeploymentSlot, FunctionDeploymentSlot> {
    companion object {
        const val CONFIGURATION_SOURCE_PARENT = "parent"
        const val CONFIGURATION_SOURCE_NEW = "new"
    }

    constructor(name: String, module: FunctionAppDeploymentSlotModule) :
            super(name, module) {
        origin = null
    }

    constructor(origin: FunctionAppDeploymentSlot) : super(origin) {
        this.origin = origin
    }

    private val origin: FunctionAppDeploymentSlot?

    private var config: Config? = null

    private val lock = Any()

    override fun getOrigin() = origin

    override fun reset() {
        config = null
    }

    private fun ensureConfig(): Config {
        synchronized(lock) {
            val localConfig = config ?: Config()
            config = localConfig
            return localConfig
        }
    }

    override fun isModified(): Boolean {
        val localConfig = config
        val notModified = localConfig == null ||
                ((localConfig.runtime == null || localConfig.runtime == remote?.getDotNetRuntime()) &&
                        localConfig.dockerConfiguration == null &&
                        (localConfig.diagnosticConfig == null || localConfig.diagnosticConfig == super.getDiagnosticConfig()) &&
                        localConfig.configurationSource.isNullOrEmpty())

        return !notModified
    }

    override fun createResourceInAzure(): FunctionDeploymentSlot {
        val functionApp = requireNotNull(parent.remote)

        val newAppSettings = appSettings
        val newDiagnosticConfig = diagnosticConfig
        val newConfigurationSource = configurationSource
        val newFlexConsumptionConfiguration = flexConsumptionConfiguration
        val source =
            if (newConfigurationSource.isNullOrEmpty()) CONFIGURATION_SOURCE_PARENT else newConfigurationSource.lowercase()

        val blank = functionApp.deploymentSlots().define(name)
        val withCreate =
            when (source) {
                CONFIGURATION_SOURCE_NEW -> blank.withBrandNewConfiguration()
                CONFIGURATION_SOURCE_PARENT -> blank.withConfigurationFromParent()
                else -> {
                    try {
                        val sourceSlot = functionApp.deploymentSlots().getByName(newConfigurationSource)
                        requireNotNull(sourceSlot) { "Target slot configuration source does not exists in current app" }
                        blank.withConfigurationFromDeploymentSlot(sourceSlot)
                    } catch (e: ManagementException) {
                        throw AzureToolkitRuntimeException("Failed to get configuration source slot", e)
                    }
                }
            }

        if (!newAppSettings.isNullOrEmpty())
            withCreate.withAppSettings(newAppSettings)
        if (newDiagnosticConfig != null)
            AppServiceUtils.defineDiagnosticConfigurationForWebAppBase(withCreate, newDiagnosticConfig)
        val updateFlexConsumptionConfiguration =
            newFlexConsumptionConfiguration != null &&
                    parent.appServicePlan?.pricingTier?.isFlexConsumption == true
        if (updateFlexConsumptionConfiguration) {
            (withCreate as? FunctionApp)?.innerModel()
                ?.withContainerSize(requireNotNull(newFlexConsumptionConfiguration).instanceSize)
        }

        val messager = AzureMessager.getMessager()
        messager.info("Start creating Function App deployment slot ($name)...")

        var slot = withCreate.create()
        if (updateFlexConsumptionConfiguration) {
            updateFlexConsumptionConfiguration(slot, requireNotNull(newFlexConsumptionConfiguration))
        }
        ensureConfig().appSettings = null
        ensureConfig().diagnosticConfig = null
        slot = updateRuntime(slot)

        messager.success("Function App deployment slot ($name) is successfully created")

        return slot
    }

    private fun updateRuntime(slot: FunctionDeploymentSlot): FunctionDeploymentSlot {
        val dotnetRuntime = dotNetRuntime
        val isRuntimeModified =
            (dotnetRuntime != null && dotnetRuntime != parent.getDotNetRuntime()) || dockerConfiguration != null
        return if (isRuntimeModified)
            updateResourceInAzure(slot)
        else
            slot
    }

    override fun updateResourceInAzure(remote: FunctionDeploymentSlot): FunctionDeploymentSlot {
        val newRuntime = ensureConfig().runtime
        val newDockerConfig = ensureConfig().dockerConfiguration
        val newDiagnosticConfig = ensureConfig().diagnosticConfig
        val newFlexConsumptionConfiguration = ensureConfig().flexConsumptionConfiguration

        val oldRuntime = remote.getDotNetRuntime()
        val oldDiagnosticConfig = super.getDiagnosticConfig()
        val oldFlexConsumptionConfiguration = super.getFlexConsumptionConfiguration()

        val runtimeModified = !oldRuntime.isDocker && newRuntime != null && newRuntime != oldRuntime
        val dockerModified = oldRuntime.isDocker && newDockerConfig != null
        val diagnosticModified = newDiagnosticConfig != null && newDiagnosticConfig != oldDiagnosticConfig
        val flexConsumptionModified = parent.appServicePlan?.pricingTier?.isFlexConsumption == true &&
                newFlexConsumptionConfiguration != null &&
                newFlexConsumptionConfiguration != oldFlexConsumptionConfiguration
        val isModified = runtimeModified || dockerModified || diagnosticModified || flexConsumptionModified

        var result = remote
        if (isModified) {
            val update = remote.update()

            if (runtimeModified) newRuntime?.let { updateRuntime(update, it) }
            if (dockerModified) newDockerConfig?.let { updateDockerConfiguration(update, it) }
            if (diagnosticModified) newDiagnosticConfig?.let {
                AppServiceUtils.updateDiagnosticConfigurationForWebAppBase(update, it)
            }

            val messager = AzureMessager.getMessager()
            messager.info("Start updating Function App deployment slot (${remote.name()})...")

            result = update.apply()

            messager.success("Function app deployment slot (${remote.name()}) is successfully updated")
        }

        return result
    }

    private fun updateRuntime(update: DeploymentSlotBase.Update<*>, newRuntime: DotNetRuntime) {
        val oldRuntime = (update as WebAppBase).getDotNetRuntime()
        if (newRuntime.operatingSystem != oldRuntime.operatingSystem) {
            throw AzureToolkitRuntimeException("Can not update the operation system for existing app service")
        }

        when (oldRuntime.operatingSystem) {
            OperatingSystem.LINUX -> {
                AzureMessager.getMessager().warning("Update runtime is not supported for Linux app service")
            }

            OperatingSystem.WINDOWS -> {
                val functionStack = requireNotNull(newRuntime.functionStack) { "Unable to configure function runtime" }
                update.withRuntime(functionStack.runtime())
                    .withRuntimeVersion(functionStack.version())
            }

            OperatingSystem.DOCKER -> return
        }
    }

    private fun updateDockerConfiguration(update: DeploymentSlotBase.Update<*>, newConfig: DockerConfiguration) {
        val draft =
            if (newConfig.userName.isNullOrEmpty() && newConfig.password.isNullOrEmpty()) {
                update.withPublicDockerHubImage(newConfig.image)
            } else if (newConfig.registryUrl.isNullOrEmpty()) {
                update.withPrivateDockerHubImage(newConfig.image)
                    .withCredentials(newConfig.userName, newConfig.password)
            } else {
                update.withPrivateRegistryImage(newConfig.image, newConfig.registryUrl)
                    .withCredentials(newConfig.userName, newConfig.password)
            }
        draft.withStartUpCommand(newConfig.startUpCommand)
    }

    private fun updateFlexConsumptionConfiguration(
        slot: FunctionDeploymentSlot,
        flexConfiguration: FlexConsumptionConfiguration
    ) {
        val name = "${slot.parent().name()}/slots/${slot.name()}"
        val webApps = slot.manager().serviceClient().webApps
        if (flexConfiguration.maximumInstances != null || flexConfiguration.alwaysReadyInstances != null) {
            val configuration = webApps.getConfiguration(slot.resourceGroupName(), name)
            if (flexConfiguration.maximumInstances != configuration.functionAppScaleLimit() ||
                flexConfiguration.alwaysReadyInstances.size != configuration.minimumElasticInstanceCount()
            ) {
                configuration.withFunctionAppScaleLimit(flexConfiguration.maximumInstances)
                webApps.updateConfiguration(slot.resourceGroupName(), name, configuration)
            }
        }
        if (slot.innerModel().containerSize() != flexConfiguration.instanceSize) {
            val patch = SitePatchResourceInner()
                .withContainerSize(flexConfiguration.instanceSize)
            webApps
                .updateWithResponseAsync(slot.resourceGroupName(), name, patch)
                .block()
        }
    }

    var dotNetRuntime: DotNetRuntime?
        get() = config?.runtime ?: remote?.getDotNetRuntime()
        set(value) {
            ensureConfig().runtime = value
        }
    var dockerConfiguration: DockerConfiguration?
        get() = config?.dockerConfiguration
        set(value) {
            ensureConfig().dockerConfiguration = value
        }
    var configurationSource: String?
        get() = config?.configurationSource
        set(value) {
            ensureConfig().configurationSource = value
        }

    override fun getAppSettings() = config?.appSettings ?: super.getAppSettings()
    fun setAppSettings(value: Map<String, String>?) {
        ensureConfig().appSettings = value
    }

    override fun getDiagnosticConfig() = config?.diagnosticConfig ?: super.getDiagnosticConfig()
    fun setDiagnosticConfig(value: DiagnosticConfig?) {
        ensureConfig().diagnosticConfig = value
    }

    override fun getFlexConsumptionConfiguration() =
        config?.flexConsumptionConfiguration ?: super.getFlexConsumptionConfiguration()

    fun setFlexConsumptionConfiguration(value: FlexConsumptionConfiguration?) {
        ensureConfig().flexConsumptionConfiguration = value
    }

    data class Config(
        var runtime: DotNetRuntime? = null,
        var dockerConfiguration: DockerConfiguration? = null,
        var diagnosticConfig: DiagnosticConfig? = null,
        var configurationSource: String? = null,
        var flexConsumptionConfiguration: FlexConsumptionConfiguration? = null,
        var appSettings: Map<String, String>? = null
    )
}