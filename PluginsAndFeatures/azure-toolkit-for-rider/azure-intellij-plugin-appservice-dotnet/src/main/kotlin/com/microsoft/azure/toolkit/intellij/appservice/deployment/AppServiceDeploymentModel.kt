/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.deployment

import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig

interface AppServiceDeploymentModel {
    val config: AppServiceConfig

    class DraftAppServiceModel(override val config: AppServiceConfig) : AppServiceDeploymentModel

    class RemoteAppServiceModel(
        val resourceGroup: String,
        override val config: AppServiceConfig,
        val deploymentSlots: List<String>
    ) : AppServiceDeploymentModel
}

sealed interface AppServiceLoadState {
    data object Loading : AppServiceLoadState
    data class Loaded(val items: List<AppServiceDeploymentModel.RemoteAppServiceModel>) : AppServiceLoadState
    data class Error(val message: String) : AppServiceLoadState
}