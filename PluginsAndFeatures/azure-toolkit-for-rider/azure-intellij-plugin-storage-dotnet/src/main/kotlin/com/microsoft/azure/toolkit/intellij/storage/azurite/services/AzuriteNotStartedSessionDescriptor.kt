/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage.azurite.services

import com.intellij.execution.services.SimpleServiceViewDescriptor
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons

class AzuriteNotStartedSessionDescriptor : SimpleServiceViewDescriptor(
    "Azurite Emulator", IntelliJAzureIcons.getIcon(AzureIcons.StorageAccount.AZURITE)
) {
    companion object {
        private val defaultToolbarActions = DefaultActionGroup(
            ActionManager.getInstance().getAction("AzureToolkit.Azurite.Start"),
            ActionManager.getInstance().getAction("AzureToolkit.Azurite.Stop"),
            ActionManager.getInstance().getAction("AzureToolkit.Azurite.Clean"),
            ActionManager.getInstance().getAction("AzureToolkit.Azurite.ShowSettings")
        )
    }

    override fun getPresentation() = PresentationData().apply {
        setIcon(IntelliJAzureIcons.getIcon(AzureIcons.StorageAccount.AZURITE))
        addText("Azurite emulator", SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }

    override fun getContentComponent() =
        JBPanelWithEmptyText().withEmptyText("Azurite emulator has not been started.")

    override fun getToolbarActions() = defaultToolbarActions
}