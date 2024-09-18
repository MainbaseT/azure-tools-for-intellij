/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage.azurite.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.microsoft.azure.toolkit.intellij.storage.azurite.settings.AzuriteConfigurable

class ShowAzuriteSettingsNotificationAction: NotificationAction("Show settings") {
    override fun actionPerformed(e: AnActionEvent, n: Notification) {
        val project = e.project ?: return
        ShowSettingsUtil.getInstance().showSettingsDialog(project, AzuriteConfigurable::class.java)
    }
}