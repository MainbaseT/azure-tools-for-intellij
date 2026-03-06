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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.cancellation.CancellationException

sealed interface WebAppModel {
    val config: AppServiceConfig

    class DraftWebAppModel(override val config: AppServiceConfig) : WebAppModel
    class RemoteWebAppModel(override val config: AppServiceConfig, val deploymentSlots: List<String>) : WebAppModel
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

    private val _webAppItems = MutableStateFlow<List<RemoteWebAppModel>>(emptyList())
    val webAppItems: StateFlow<List<RemoteWebAppModel>> = _webAppItems.asStateFlow()

    private val _webAppsLoading = MutableStateFlow(true)
    val webAppsLoading: StateFlow<Boolean> = _webAppsLoading.asStateFlow()

    private val _selectedWebApp = MutableStateFlow<AppServiceConfig?>(null)
    val selectedWebApp: StateFlow<AppServiceConfig?> = _selectedWebApp.asStateFlow()

    private var loadWebAppsJob: Job? = null

    init {
        loadWebAppsJob = cs.launch {
            _webAppsLoading.value = true
            try {
                val configs = loadListOfWebApps()
                _webAppItems.update { configs }
            } finally {
                _webAppsLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectWebApp(webAppModel: WebAppModel) {
        _selectedWebApp.value = webAppModel.config
    }

    fun refreshWebApps() {
        loadWebAppsJob?.cancel()
        loadWebAppsJob = cs.launch {
            _webAppsLoading.value = true
            _webAppItems.update { emptyList() }
            try {
                invalidateWebAppCache()
                val configs = loadListOfWebApps()
                _webAppItems.update { configs }
            } finally {
                _webAppsLoading.value = false
            }
        }
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

        _selectedWebApp.value = webAppConfig
    }

    private suspend fun loadListOfWebApps(): List<RemoteWebAppModel> {
        try {
            val account = Azure.az(AzureAccount::class.java).account()
            if (!account.isLoggedIn) {
                LOG.trace("User is not logged in, skipping web app loading")
                return emptyList()
            }

            loadRemoteResources()

            val webApps = Azure.az(AzureWebApp::class.java).webApps()

            val loadedApps = webApps.sortedBy { it.name }.map { webApp ->
                convertAppServiceToConfig(webApp)
            }.map { config ->
                RemoteWebAppModel(config, emptyList())
            }

            return loadedApps
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            LOG.warn("Error while trying to load Azure web apps", e)
            return emptyList()
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

    fun addDraftWebApp(config: AppServiceConfig) {
        val model = DraftWebAppModel(config)
        _draftWebApps.update { current ->
            listOf(model) + current.filter { !isSameApp(it.config, config) }
        }
        _selectedWebApp.value = model.config
    }

    fun applySelectedConfigToOptions(state: WebAppConfigurationOptions) {
        val webAppConfig = selectedWebApp.value ?: return

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
