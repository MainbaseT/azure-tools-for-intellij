/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.deployment

import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentModel.DraftAppServiceModel
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import kotlinx.coroutines.flow.StateFlow

interface AppServiceDeploymentViewModel {
    val draftAppServiceState: StateFlow<List<DraftAppServiceModel>>
    val remoteAppServiceState: StateFlow<AppServiceLoadState>
    val selectedAppService: StateFlow<Pair<AppServiceConfig, String?>?>

    fun selectAppService(appService: AppServiceDeploymentModel,  deploymentSlotName: String?)
    fun addDraftAppService(config: AppServiceConfig)
    fun refreshAppServices()
}