/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp

import com.intellij.openapi.diagnostic.logger
import com.intellij.platform.util.coroutines.childScope
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp.WebAppModel.DraftWebAppModel
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp.WebAppModel.RemoteWebAppModel
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
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlin.coroutines.cancellation.CancellationException

sealed interface WebAppModel {
    val config: AppServiceConfig

    class DraftWebAppModel(override val config: AppServiceConfig) : WebAppModel

    class RemoteWebAppModel(
        val resourceGroup: String,
        override val config: AppServiceConfig,
        val deploymentSlots: List<String>
    ) : WebAppModel
}

sealed interface WebAppsLoadState {
    data object Loading : WebAppsLoadState
    data class Loaded(val items: List<RemoteWebAppModel>) : WebAppsLoadState
    data class Error(val message: String) : WebAppsLoadState
}

class WebAppSettingEditorViewModel(parentCs: CoroutineScope) {
    companion object {
        private val LOG = logger<WebAppSettingEditorViewModel>()

        internal fun isSameApp(first: AppServiceConfig?, second: AppServiceConfig?): Boolean {
            if (first == null || second == null) return first === second
            return first.appName.equals(second.appName, ignoreCase = true) &&
                    first.resourceGroup.equals(second.resourceGroup, ignoreCase = true) &&
                    first.subscriptionId.equals(second.subscriptionId, ignoreCase = true)
        }
    }

    private val cs = parentCs.childScope("WebAppSettingEditorViewModel", Dispatchers.Default)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _draftWebApps = MutableStateFlow<List<DraftWebAppModel>>(emptyList())
    val draftWebApps: StateFlow<List<DraftWebAppModel>> = _draftWebApps.asStateFlow()

    private val _webAppsState = MutableStateFlow<WebAppsLoadState>(WebAppsLoadState.Loading)
    val webAppsState: StateFlow<WebAppsLoadState> = _webAppsState.asStateFlow()

    private val _selectedWebApp = MutableStateFlow<Pair<AppServiceConfig, String?>?>(null)
    val selectedWebApp: StateFlow<Pair<AppServiceConfig, String?>?> = _selectedWebApp.asStateFlow()

    private val reloadTrigger = MutableSharedFlow<Boolean>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        reloadTrigger.tryEmit(false)

        cs.launch {
            reloadTrigger
                .collectLatest { refresh ->
                    _webAppsState.value = WebAppsLoadState.Loading

                    if (refresh) invalidateWebAppCache()
                    try {
                        val configs = loadListOfWebApps()
                        _webAppsState.value = WebAppsLoadState.Loaded(configs)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        LOG.warn("Error while trying to load Azure web apps", e)
                        _webAppsState.value = WebAppsLoadState.Error(e.message ?: "Unknown error")
                    }
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectWebApp(webAppModel: WebAppModel, deploymentSlotName: String?) {
        _selectedWebApp.value = webAppModel.config to deploymentSlotName
    }

    fun refreshWebApps() {
        reloadTrigger.tryEmit(true)
    }

    fun addDraftWebApp(config: AppServiceConfig) {
        val model = DraftWebAppModel(config)
        _draftWebApps.update { current ->
            listOf(model) + current.filter { !isSameApp(it.config, config) }
        }
        _selectedWebApp.value = model.config to null
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
        _selectedWebApp.value = webAppConfig to deploymentSlotName
    }

    fun applySelectedConfigToOptions(state: WebAppConfigurationOptions) {
        val webAppValue = selectedWebApp.value ?: return
        val webAppConfig = webAppValue.first
        val slotNameConfig = webAppValue.second

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
        }
    }

    private suspend fun loadListOfWebApps(): List<RemoteWebAppModel> {
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
                RemoteWebAppModel(
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

    private fun getResource(config: AppServiceConfig): com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase<*, *, *>? {
        if (config.appName.isNullOrEmpty()) return null
        return Azure.az(AzureWebApp::class.java)
            .webApps(config.subscriptionId)
            .get(config.appName, config.resourceGroup)
    }
}
