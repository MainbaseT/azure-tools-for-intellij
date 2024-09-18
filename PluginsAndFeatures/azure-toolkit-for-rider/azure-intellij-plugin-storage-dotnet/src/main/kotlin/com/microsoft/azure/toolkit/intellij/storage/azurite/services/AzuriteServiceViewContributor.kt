/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage.azurite.services

import com.intellij.execution.services.ServiceViewContributor
import com.intellij.execution.services.SimpleServiceViewDescriptor
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import com.microsoft.azure.toolkit.intellij.storage.azurite.settings.AzuriteSettings

class AzuriteServiceViewContributor : ServiceViewContributor<AzuriteSession> {
    override fun getServices(project: Project): MutableList<AzuriteSession> {
        val settings = AzuriteSettings.getInstance(project)
        if (!settings.showAzuriteService) return mutableListOf()

        return mutableListOf(AzuriteService.getInstance().session)
    }

    override fun getViewDescriptor(project: Project) =
        SimpleServiceViewDescriptor("Azurite Emulator", IntelliJAzureIcons.getIcon(AzureIcons.StorageAccount.AZURITE))

    override fun getServiceDescriptor(project: Project, session: AzuriteSession) =
        when (session) {
            is AzuriteStartedSession -> AzuriteSessionDescriptor(project)
            else -> AzuriteNotStartedSessionDescriptor()
        }
}