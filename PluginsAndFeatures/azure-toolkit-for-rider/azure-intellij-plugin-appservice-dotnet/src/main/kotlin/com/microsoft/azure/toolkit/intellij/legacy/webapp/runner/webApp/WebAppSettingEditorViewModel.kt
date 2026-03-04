/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp

import com.intellij.openapi.diagnostic.logger
import com.intellij.platform.util.coroutines.childScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.java

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

    private val _draftWebApps = MutableStateFlow<List<AppServiceConfig>>(emptyList())
    val draftWebApps: StateFlow<List<AppServiceConfig>> = _draftWebApps.asStateFlow()

    private val _webAppItems = MutableStateFlow<List<AppServiceConfig>>(emptyList())
    val webAppItems: StateFlow<List<AppServiceConfig>> = _webAppItems.asStateFlow()

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

    fun selectWebApp(appServiceConfig: AppServiceConfig) {
        _selectedWebApp.value = appServiceConfig
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

    private suspend fun loadListOfWebApps(): List<AppServiceConfig> {
        try {
            val account = Azure.az(AzureAccount::class.java).account()
            if (!account.isLoggedIn) {
                LOG.trace("User is not logged in, skipping web app loading")
                return emptyList()
            }

            loadRemoteWebApps()

            val webApps = Azure.az(AzureWebApp::class.java).webApps()

            val loadedApps = webApps.sortedBy { it.name }.map { webApp ->
                convertAppServiceToConfig(webApp)
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
     * This method loads remote web apps in parallel.
     * The loaded web apps will be saved in the cache, so the further calls won't load them from Azure again.
     */
    private suspend fun loadRemoteWebApps() {
        LOG.trace("Loading web apps from Azure")

        val appServicePlans = Azure.az(AzureAppService::class.java).plans()
        coroutineScope {
            appServicePlans.forEach {
                launch {
                    it.remote
                }
            }
        }

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
        _draftWebApps.update { current ->
            listOf(config) + current.filter { !isSameApp(it, config) }
        }
        _selectedWebApp.value = config
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
