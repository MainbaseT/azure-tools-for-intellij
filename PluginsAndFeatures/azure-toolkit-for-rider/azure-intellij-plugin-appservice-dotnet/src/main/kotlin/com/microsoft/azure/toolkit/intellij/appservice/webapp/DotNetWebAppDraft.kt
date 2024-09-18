/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.webapp

import com.azure.resourcemanager.appservice.models.WebApp.DefinitionStages
import com.azure.resourcemanager.appservice.models.WebAppRuntimeStack
import com.microsoft.azure.toolkit.intellij.appservice.DotNetRuntime
import com.microsoft.azure.toolkit.intellij.appservice.getDotNetRuntime
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppModule
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.model.AzResource

class DotNetWebAppDraft : WebApp, AzResource.Draft<WebApp, com.azure.resourcemanager.appservice.models.WebApp> {
    constructor(name: String, resourceGroupName: String, module: WebAppModule) :
            super(name, resourceGroupName, module) {
        origin = null
    }

    constructor(origin: WebApp) : super(origin) {
        this.origin = origin
    }

    private val origin: WebApp?

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
                        (localConfig.plan == null || localConfig.plan == super.getAppServicePlan()) &&
                        localConfig.dockerConfiguration == null &&
                        localConfig.diagnosticConfig == null)

        return !notModified
    }

    override fun createResourceInAzure(): com.azure.resourcemanager.appservice.models.WebApp {
        val newRuntime = requireNotNull(dotNetRuntime) { "'runtime' is required to create Azure Web App" }
        val newPlan = requireNotNull(appServicePlan) { "'service plan' is required to create Azure Web App" }
        val os = newRuntime.operatingSystem
        if (os != newPlan.operatingSystem) {
            throw AzureToolkitRuntimeException("Could not create $os app service in ${newPlan.operatingSystem} service plan")
        }
        val newAppSettings = appSettings
        val newDiagnosticConfig = diagnosticConfig

        val manager = requireNotNull(parent.remote)
        val blank = manager.webApps().define(name)
        val withCreate =
            if (!newRuntime.isDocker) createWebApp(blank, os, newPlan, newRuntime)
            else createDockerWebApp(blank, os, newPlan)

        if (!newAppSettings.isNullOrEmpty())
            withCreate.withAppSettings(newAppSettings)
        if (newDiagnosticConfig != null)
            AppServiceUtils.defineDiagnosticConfigurationForWebAppBase(withCreate, newDiagnosticConfig)

        val messager = AzureMessager.getMessager()
        messager.info(AzureString.format("Start creating Web App ({0})...", name))

        val webApp = withCreate.create()

        val open = AzureActionManager.getInstance().getAction(OPEN_IN_BROWSER)?.bind(this)
        messager.success(AzureString.format("Web App ({0}) is successfully created", name), open)

        return webApp
    }

    private fun createWebApp(
        blank: DefinitionStages.Blank,
        os: OperatingSystem,
        plan: AppServicePlan,
        runtime: DotNetRuntime
    ) = when (os) {
        OperatingSystem.LINUX -> {
            val stack = requireNotNull(runtime.stack) { "Unable to configure web app runtime" }
            blank
                .withExistingLinuxPlan(plan.remote)
                .withExistingResourceGroup(resourceGroupName)
                .withBuiltInImage(stack)
        }

        OperatingSystem.WINDOWS -> {
            val frameworkVersion = requireNotNull(runtime.frameworkVersion) { "Unable to configure web app runtime" }
            blank
                .withExistingWindowsPlan(plan.remote)
                .withExistingResourceGroup(resourceGroupName)
                .withRuntimeStack(WebAppRuntimeStack.NET)
                .withNetFrameworkVersion(frameworkVersion)
        }

        OperatingSystem.DOCKER -> throw AzureToolkitRuntimeException("Unsupported operating system $os")
    }

    private fun createDockerWebApp(
        blank: DefinitionStages.Blank,
        os: OperatingSystem,
        plan: AppServicePlan
    ): DefinitionStages.WithCreate {
        val dockerConfig =
            requireNotNull(dockerConfiguration) { "Docker configuration is required to create a docker based Azure Web App" }
        val withFramework = when (os) {
            OperatingSystem.LINUX -> {
                blank
                    .withExistingLinuxPlan(plan.remote)
                    .withExistingResourceGroup(resourceGroupName)
            }

            OperatingSystem.WINDOWS -> {
                blank
                    .withExistingWindowsPlan(plan.remote)
                    .withExistingResourceGroup(resourceGroupName)
            }

            OperatingSystem.DOCKER -> throw AzureToolkitRuntimeException("Unsupported operating system $os")
        }

        val draft =
            if (dockerConfig.userName.isNullOrEmpty() && dockerConfig.password.isNullOrEmpty())
                withFramework.withPublicDockerHubImage(dockerConfig.image)
            else if (dockerConfig.registryUrl.isNullOrEmpty())
                withFramework.withPrivateDockerHubImage(dockerConfig.image)
                    .withCredentials(dockerConfig.userName, dockerConfig.password)
            else
                withFramework.withPrivateRegistryImage(dockerConfig.image, dockerConfig.registryUrl)
                    .withCredentials(dockerConfig.userName, dockerConfig.password)

        return draft.withStartUpCommand(dockerConfig.startUpCommand)
    }

    override fun updateResourceInAzure(remote: com.azure.resourcemanager.appservice.models.WebApp): com.azure.resourcemanager.appservice.models.WebApp {
        if (origin == null)
            throw AzureToolkitRuntimeException("Updating target is not specified")

        val newPlan = ensureConfig().plan
        val newRuntime = ensureConfig().runtime
        val newDockerConfig = ensureConfig().dockerConfiguration
        val newDiagnosticConfig = ensureConfig().diagnosticConfig

        val oldPlan = origin.appServicePlan
        val oldRuntime = requireNotNull(origin.getDotNetRuntime())

        val planModified = newPlan != null && newPlan != oldPlan
        val runtimeModified = !oldRuntime.isDocker && newRuntime != null && newRuntime != oldRuntime
        val dockerModified = oldRuntime.isDocker && newDockerConfig != null
        val diagnosticModified = newDiagnosticConfig != null
        val isModified = planModified || runtimeModified || dockerModified || diagnosticModified

        var result = remote
        if (isModified) {
            val update = remote.update()

            if (planModified) newPlan?.let { updateAppServicePlan(update, it) }
            if (runtimeModified) newRuntime?.let { updateRuntime(update, it) }
            if (dockerModified) newDockerConfig?.let { updateDockerConfiguration(update, it) }
            if (diagnosticModified) newDiagnosticConfig?.let {
                AppServiceUtils.updateDiagnosticConfigurationForWebAppBase(update, it)
            }

            val messager = AzureMessager.getMessager()
            messager.info(AzureString.format("Start updating Web App ({0})...", remote.name()))

            result = update.apply()

            val open = AzureActionManager.getInstance().getAction(AppServiceAppBase.OPEN_IN_BROWSER)?.bind(this)
            messager.success(AzureString.format("Web App ({0}) is successfully updated", result.name()), open)
        }

        return result
    }

    private fun updateAppServicePlan(
        update: com.azure.resourcemanager.appservice.models.WebApp.Update,
        newPlan: AppServicePlan
    ) {
        val plan = requireNotNull(newPlan.remote) { "Target app service plan doesn't exist" }
        val runtime = requireNotNull(dotNetRuntime) { "Unable to find web app runtime" }
        if (runtime.operatingSystem != newPlan.operatingSystem) {
            throw AzureToolkitRuntimeException("Could not migrate ${runtime.operatingSystem} app service to ${newPlan.operatingSystem} service plan")
        }
        update.withExistingAppServicePlan(plan)
    }

    private fun updateRuntime(
        update: com.azure.resourcemanager.appservice.models.WebApp.Update,
        newRuntime: DotNetRuntime
    ) {
        val oldRuntime = requireNotNull(origin?.getDotNetRuntime())
        if (newRuntime.operatingSystem != oldRuntime.operatingSystem) {
            throw AzureToolkitRuntimeException("Can not update the operation system for existing app service")
        }

        when (oldRuntime.operatingSystem) {
            OperatingSystem.LINUX -> {
                val stack = requireNotNull(newRuntime.stack) { "Unable to configure web app runtime" }
                update.withBuiltInImage(stack)
            }

            OperatingSystem.WINDOWS -> {
                val frameworkVersion =
                    requireNotNull(newRuntime.frameworkVersion) { "Unable to configure web app runtime" }
                update.withRuntimeStack(WebAppRuntimeStack.NET)
                    .withNetFrameworkVersion(frameworkVersion)
            }

            OperatingSystem.DOCKER -> return
        }
    }

    private fun updateDockerConfiguration(
        update: com.azure.resourcemanager.appservice.models.WebApp.Update,
        newConfig: DockerConfiguration
    ) {
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

    data class Config(
        var runtime: DotNetRuntime? = null,
        var plan: AppServicePlan? = null,
        var dockerConfiguration: DockerConfiguration? = null,
        var diagnosticConfig: DiagnosticConfig? = null,
        var appSettings: Map<String, String>? = null
    )
}