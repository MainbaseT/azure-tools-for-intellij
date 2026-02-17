/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage.azurite.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.microsoft.azure.toolkit.intellij.storage.azurite.settings.AzuriteConfigurable

class ShowAzuriteSettingsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ShowSettingsUtil.getInstance().showSettingsDialog(project, AzuriteConfigurable::class.java)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}