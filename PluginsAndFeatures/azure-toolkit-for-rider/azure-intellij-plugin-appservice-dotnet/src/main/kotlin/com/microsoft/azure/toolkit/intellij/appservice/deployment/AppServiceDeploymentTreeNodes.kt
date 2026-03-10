/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.deployment

import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentModel.RemoteAppServiceModel

internal data class GroupNode(val name: String)
internal data class ResourceGroupNode(val name: String)
internal data class AppServiceNode(val appServiceModel: AppServiceDeploymentModel)
internal data class DeploymentSlotNode(val slotName: String, val appServiceModel: RemoteAppServiceModel)