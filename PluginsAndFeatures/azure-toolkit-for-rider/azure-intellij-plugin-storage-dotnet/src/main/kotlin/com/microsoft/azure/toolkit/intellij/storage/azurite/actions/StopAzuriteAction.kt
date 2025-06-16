/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage.azurite.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.microsoft.azure.toolkit.intellij.storage.azurite.services.AzuriteService

class StopAzuriteAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val service = AzuriteService.getInstance()
        if (service.isRunning) {
            service.stop()
        }
    }

    override fun update(e: AnActionEvent) {
        val service = AzuriteService.getInstance()
        e.presentation.isEnabled = service.isRunning
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}