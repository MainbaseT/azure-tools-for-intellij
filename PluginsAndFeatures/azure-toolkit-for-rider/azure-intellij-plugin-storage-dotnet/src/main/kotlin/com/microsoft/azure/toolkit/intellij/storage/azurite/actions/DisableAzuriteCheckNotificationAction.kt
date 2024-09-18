/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage.azurite.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.microsoft.azure.toolkit.intellij.storage.azurite.settings.AzuriteSettings

class DisableAzuriteCheckNotificationAction : NotificationAction("Do not show again") {
    override fun actionPerformed(event: AnActionEvent, notification: Notification) {
        val project = event.project ?: return
        val settings = AzuriteSettings.getInstance(project)
        settings.checkAzuriteExecutable = false
        notification.expire()
    }
}