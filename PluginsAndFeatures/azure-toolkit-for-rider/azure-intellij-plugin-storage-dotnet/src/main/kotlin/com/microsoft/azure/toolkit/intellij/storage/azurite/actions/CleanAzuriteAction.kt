/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage.azurite.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.VcsShowConfirmationOption
import com.intellij.util.ui.ConfirmationDialog
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.storage.azurite.services.AzuriteService
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons

class CleanAzuriteAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val shouldPerformClean = ConfirmationDialog.requestForConfirmation(
            VcsShowConfirmationOption.STATIC_SHOW_CONFIRMATION,
            project,
            "This will remove all existing data in the Azurite workspace.\n\nAre you sure you want to clean data in Azurite Emulator?",
            "Clean Data in Azurite Emulator",
            IntelliJAzureIcons.getIcon(AzureIcons.StorageAccount.AZURITE),
            "Yes, Remove Existing Data",
            "Cancel"
        )

        if (shouldPerformClean) {
            AzuriteService.getInstance().clean(project)
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        val service = AzuriteService.getInstance()
        e.presentation.isEnabled = !service.isRunning
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}