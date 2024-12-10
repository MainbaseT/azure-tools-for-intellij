/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.functionapp

import com.microsoft.azure.toolkit.intellij.appservice.DotNetRuntime
import com.microsoft.azure.toolkit.intellij.appservice.getDotNetRuntime
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppModule
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.model.AzResource
import com.microsoft.azure.toolkit.lib.storage.StorageAccount

class DotNetFunctionAppDraft : FunctionApp,
    AzResource.Draft<FunctionApp, com.azure.resourcemanager.appservice.models.FunctionApp> {

    constructor(name: String, resourceGroupName: String, module: FunctionAppModule) :
            super(name, resourceGroupName, module) {
        origin = null
    }

    constructor(origin: FunctionApp) : super(origin) {
        this.origin = origin
    }

    private val origin: FunctionApp?

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

    override fun isModified() = false

    override fun createResourceInAzure(): com.azure.resourcemanager.appservice.models.FunctionApp {
        val newRuntime = checkNotNull(dotNetRuntime) { "'runtime' is required to create a Function App" }
        val newPlan = checkNotNull(appServicePlan) { "'service plan' is required to create a Function App" }
        val os = newRuntime.operatingSystem
        if (os != newPlan.operatingSystem) {
            throw AzureToolkitRuntimeException("Could not create $os app service in ${newPlan.operatingSystem} service plan")
        }
        val newAppSettings = appSettings
        val newDiagnosticConfig = diagnosticConfig
        val newFlexConsumptionConfiguration = flexConsumptionConfiguration
        val newStorageAccount = storageAccount

        val manager = checkNotNull(parent.remote)
        val blank = manager.functionApps().define(name)
        val withCreate = createFunctionApp(blank, os, newPlan, newRuntime)

        if (!newAppSettings.isNullOrEmpty())
            withCreate.withAppSettings(newAppSettings)
        if (newStorageAccount != null)
            withCreate.withExistingStorageAccount(newStorageAccount.remote)
        if (newDiagnosticConfig != null)
            AppServiceUtils.defineDiagnosticConfigurationForWebAppBase(withCreate, newDiagnosticConfig)

        val updateFlexConsumptionConfiguration =
            newFlexConsumptionConfiguration != null && newPlan.pricingTier.isFlexConsumption
        if (updateFlexConsumptionConfiguration) {
            withCreate.withContainerSize(newFlexConsumptionConfiguration.instanceSize)
            withCreate.withWebAppAlwaysOn(false)
        }

        val messager = AzureMessager.getMessager()
        messager.info(AzureString.format("Start creating Function App ({0})...", name))

        val functionApp = withCreate.create()
        if (updateFlexConsumptionConfiguration) {
            updateFlexConsumptionConfiguration(functionApp, newFlexConsumptionConfiguration)
        }

        messager.success(AzureString.format("Function App ({0}) is successfully created", name))

        return functionApp
    }

    private fun createFunctionApp(
        blank: com.azure.resourcemanager.appservice.models.FunctionApp.DefinitionStages.Blank,
        os: OperatingSystem,
        plan: AppServicePlan,
        runtime: DotNetRuntime
    ) = when (os) {
        OperatingSystem.LINUX -> {
            val functionStack = requireNotNull(runtime.functionStack) { "Unable to configure function runtime" }
            blank
                .withExistingLinuxAppServicePlan(plan.remote)
                .withExistingResourceGroup(resourceGroupName)
                .withBuiltInImage(functionStack)
        }

        OperatingSystem.WINDOWS -> {
            val functionStack = requireNotNull(runtime.functionStack) { "Unable to configure function runtime" }
            blank
                .withExistingAppServicePlan(plan.remote)
                .withExistingResourceGroup(resourceGroupName)
                .withRuntime(functionStack.runtime())
                .withRuntimeVersion(functionStack.version())
        }

        OperatingSystem.DOCKER -> throw AzureToolkitRuntimeException("Unsupported operating system $os")
    }

    override fun updateResourceInAzure(remote: com.azure.resourcemanager.appservice.models.FunctionApp): com.azure.resourcemanager.appservice.models.FunctionApp {
        throw AzureToolkitRuntimeException("Updating function app is not supported")
    }

    private fun updateFlexConsumptionConfiguration(
        app: com.azure.resourcemanager.appservice.models.FunctionApp,
        flexConfiguration: FlexConsumptionConfiguration
    ) {
        val webApps = app.manager().serviceClient().webApps
        if (flexConfiguration.maximumInstances != null || flexConfiguration.alwaysReadyInstances != null) {
            val configuration = webApps.getConfiguration(app.resourceGroupName(), app.name())
            if (flexConfiguration.maximumInstances != configuration.functionAppScaleLimit() ||
                flexConfiguration.alwaysReadyInstances.size != configuration.minimumElasticInstanceCount()
            ) {
                configuration.withFunctionAppScaleLimit(flexConfiguration.maximumInstances)
                webApps.updateConfiguration(app.resourceGroupName(), app.name(), configuration)
            }
        }
    }

    var dotNetRuntime: DotNetRuntime?
        get() = config?.runtime ?: remote?.getDotNetRuntime()
        set(value) {
            ensureConfig().runtime = value
        }
    var storageAccount: StorageAccount?
        get() = config?.storageAccount
        set(value) {
            ensureConfig().storageAccount = value
        }
    var dockerConfiguration: DockerConfiguration?
        get() = config?.dockerConfiguration
        set(value) {
            ensureConfig().dockerConfiguration = value
        }

    override fun getAppServicePlan() = config?.plan ?: super.getAppServicePlan()
    fun setAppServicePlan(value: AppServicePlan?) {
        ensureConfig().plan = value
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
        var plan: AppServicePlan? = null,
        var storageAccount: StorageAccount? = null,
        var enableDistributedTracing: Boolean? = null,
        var dockerConfiguration: DockerConfiguration? = null,
        var diagnosticConfig: DiagnosticConfig? = null,
        var flexConsumptionConfiguration: FlexConsumptionConfiguration? = null,
        var appSettings: Map<String, String>? = null
    )
}