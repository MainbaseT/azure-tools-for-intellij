/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.platform.util.coroutines.childScope
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentModel
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentModel.DraftAppServiceModel
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentModel.RemoteAppServiceModel
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentViewModel
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceLoadState
import com.microsoft.azure.toolkit.intellij.appservice.utils.isSameApp
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.model.Region
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException


class WebAppSettingEditorViewModel(project: Project, parentCs: CoroutineScope): AppServiceDeploymentViewModel {
    companion object {
        private val LOG = logger<WebAppSettingEditorViewModel>()
    }

    private val cs = parentCs.childScope("WebAppSettingEditorViewModel", Dispatchers.Default)

    private val _draftAppServiceState = MutableStateFlow<List<DraftAppServiceModel>>(emptyList())
    override val draftAppServiceState: StateFlow<List<DraftAppServiceModel>> = _draftAppServiceState.asStateFlow()

    private val _remoteAppServiceState = MutableStateFlow<AppServiceLoadState>(AppServiceLoadState.Loading)
    override val remoteAppServiceState: StateFlow<AppServiceLoadState> = _remoteAppServiceState.asStateFlow()

    private val _selectedAppService = MutableStateFlow<Pair<AppServiceConfig, String?>?>(null)
    override val selectedAppService: StateFlow<Pair<AppServiceConfig, String?>?> = _selectedAppService.asStateFlow()

    private val _openBrowserAfterDeployment = MutableStateFlow(false)
    val openBrowserAfterDeployment: StateFlow<Boolean> = _openBrowserAfterDeployment.asStateFlow()

    private val _publishableProjects = MutableStateFlow<List<PublishableProjectModel>>(emptyList())
    val publishableProjects: StateFlow<List<PublishableProjectModel>> = _publishableProjects.asStateFlow()

    val selectedProject = MutableStateFlow<PublishableProjectModel?>(null)

    val selectedConfigurationAndPlatform = MutableStateFlow<ConfigurationAndPlatform?>(null)

    private val reloadTrigger = MutableSharedFlow<Boolean>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        _publishableProjects.value = project.solution.publishableProjectsModel.publishableProjects.values
            .filter { it.isWeb && (it.isDotNetCore || SystemInfo.isWindows) }

        reloadTrigger.tryEmit(false)

        cs.launch {
            combine(remoteAppServiceState, selectedAppService) { state, selected ->
                state to selected
            }.collect { (state, selected) ->
                if (state is AppServiceLoadState.Loaded) {
                    val remote = state.items
                    _draftAppServiceState.update { drafts ->
                        //Remove from drafts all the existing remote apps
                        val filteredDrafts =
                            drafts.filter { draft -> remote.none { isSameApp(it.config, draft.config) } }

                        if (selected == null ||
                            remote.any { isSameApp(it.config, selected.first) } ||
                            drafts.any { isSameApp(it.config, selected.first) }
                        ) {
                            filteredDrafts
                        } else {
                            //If we cannot find selected app among remote apps and draft apps, add it to the drafts
                            listOf(DraftAppServiceModel(selected.first)) + filteredDrafts
                        }
                    }
                }
            }
        }

        cs.launch {
            reloadTrigger
                .collectLatest { refresh ->
                    _remoteAppServiceState.value = AppServiceLoadState.Loading

                    if (refresh) invalidateWebAppCache()
                    try {
                        val configs = loadListOfWebApps()
                        _remoteAppServiceState.value = AppServiceLoadState.Loaded(configs)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        LOG.warn("Error while trying to load Azure web apps", e)
                        _remoteAppServiceState.value = AppServiceLoadState.Error(e.message ?: "Unknown error")
                    }
                }
        }
    }

    override fun selectAppService(appService: AppServiceDeploymentModel, deploymentSlotName: String?) {
        _selectedAppService.value = appService.config to deploymentSlotName
    }

    override fun refreshAppServices() {
        reloadTrigger.tryEmit(true)
    }

    override fun addDraftAppService(config: AppServiceConfig) {
        val model = DraftAppServiceModel(config)
        _draftAppServiceState.update { current ->
            listOf(model) + current.filter { !isSameApp(it.config, model.config) }
        }
        _selectedAppService.value = model.config to null
    }

    fun setOpenBrowserFlag(enabled: Boolean) {
        _openBrowserAfterDeployment.value = enabled
    }

    fun setConfigFromOptions(state: WebAppConfigurationOptions) {
        val region = if (state.region.isNullOrEmpty()) null else Region.fromName(requireNotNull(state.region))
        val pricingTier = PricingTier(state.pricingTier, state.pricingSize)
        val operatingSystem = OperatingSystem.fromString(state.operatingSystem)

        val webAppConfig = AppServiceConfig
            .builder()
            .appName(state.webAppName)
            .subscriptionId(state.subscriptionId)
            .resourceGroup(state.resourceGroupName)
            .region(region)
            .servicePlanName(state.appServicePlanName)
            .servicePlanResourceGroup(state.appServicePlanResourceGroupName)
            .pricingTier(pricingTier)
            .runtime(RuntimeConfig().apply { os = operatingSystem })
            .build()
        val deploymentSlotName = if (state.isDeployToSlot) state.slotName else null
        _selectedAppService.value = webAppConfig to deploymentSlotName

        selectedProject.value = _publishableProjects.value
            .firstOrNull { it.projectFilePath == state.publishableProjectPath }

        val config = state.projectConfiguration
        val platform = state.projectPlatform
        selectedConfigurationAndPlatform.value =
            if (config != null && platform != null) ConfigurationAndPlatform(config, platform)
            else null

        _openBrowserAfterDeployment.value = state.openBrowser
    }

    fun applySelectedConfigToOptions(state: WebAppConfigurationOptions) {
        val webAppValue = selectedAppService.value ?: return
        val webAppConfig = webAppValue.first
        val slotNameConfig = webAppValue.second
        val openBrowserValue = openBrowserAfterDeployment.value

        state.apply {
            webAppName = webAppConfig.appName
            subscriptionId = webAppConfig.subscriptionId
            resourceGroupName = webAppConfig.resourceGroup
            region = webAppConfig.region?.toString()
            appServicePlanName = webAppConfig.servicePlanName
            appServicePlanResourceGroupName = webAppConfig.servicePlanResourceGroup
            pricingTier = webAppConfig.pricingTier?.tier
            pricingSize = webAppConfig.pricingTier?.size
            operatingSystem = webAppConfig.runtime?.os?.toString()
            if (slotNameConfig != null) {
                isDeployToSlot = true
                slotName = slotNameConfig
            } else {
                isDeployToSlot = false
                slotName = null
            }

            publishableProjectPath = this@WebAppSettingEditorViewModel.selectedProject.value?.projectFilePath
            val cap = this@WebAppSettingEditorViewModel.selectedConfigurationAndPlatform.value
            projectConfiguration = cap?.configuration
            projectPlatform = cap?.platform

            openBrowser = openBrowserValue
        }
    }

    private suspend fun loadListOfWebApps(): List<RemoteAppServiceModel> {
        val account = Azure.az(AzureAccount::class.java).account()
        if (!account.isLoggedIn) {
            LOG.trace("User is not logged in, skipping web app loading")
            return emptyList()
        }

        loadRemoteResources()

        val webApps = Azure.az(AzureWebApp::class.java).webApps()

        return webApps
            .sortedBy { it.name }
            .map { webApp ->
                val config = convertAppServiceToConfig(webApp)
                val deploymentSlots = webApp.slots().list().map { it.name }
                RemoteAppServiceModel(
                    webApp.resourceGroupName,
                    config,
                    deploymentSlots
                )
            }
    }

    /**
     * This method loads remote web apps and app service plans in parallel.
     * The loaded web apps will be saved in the cache, so the further calls won't load them from Azure again.
     */
    private suspend fun loadRemoteResources() {
        LOG.trace("Loading web apps and app service plans from Azure")

        coroutineScope {
            launch { loadAppServicePlans() }
            launch { loadWebApps() }
        }
    }

    private suspend fun loadAppServicePlans() {
        val appServicePlans = Azure.az(AzureAppService::class.java).plans()
        coroutineScope {
            appServicePlans.forEach {
                launch {
                    it.remote
                }
            }
        }
    }

    private suspend fun loadWebApps() {
        val webApps = Azure.az(AzureWebApp::class.java).webApps()
        coroutineScope {
            webApps.forEach {
                launch {
                    it.remote
                }
                launch {
                    it.slots().list()
                }
            }
        }
    }

    /**
     * This method invalidates the cache and reloads web apps from Azure.
     */
    private fun invalidateWebAppCache() {
        try {
            Azure.az(AzureWebApp::class.java).refresh()
        } catch (e: Exception) {
            LOG.warn("Error while invalidating web app cache", e)
        }
    }

    private fun convertAppServiceToConfig(appService: AppServiceAppBase<*, *, *>): AppServiceConfig {
        return AppServiceConfig().apply {
            subscriptionId = appService.subscriptionId
            resourceGroup = appService.resourceGroupName
            appName = appService.name
            region = appService.region
            runtime = RuntimeConfig().apply {
                os = OperatingSystem.fromString(appService.remote?.operatingSystem()?.name)
            }
            val servicePlan = appService.appServicePlan
            servicePlan?.also {
                pricingTier = it.pricingTier
                servicePlanName = it.name
                servicePlanResourceGroup = it.resourceGroupName
            }
        }
    }
}
