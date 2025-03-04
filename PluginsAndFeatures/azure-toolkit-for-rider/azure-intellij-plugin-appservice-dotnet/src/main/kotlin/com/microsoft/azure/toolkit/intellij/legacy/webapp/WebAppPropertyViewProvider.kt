/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.microsoft.azure.toolkit.intellij.appservice.actions.OpenAppServicePropertyViewAction

class WebAppPropertyViewProvider : WebAppBasePropertyViewProvider() {
    companion object {
        const val TYPE = "WEB_APP_PROPERTY"
    }

    override fun getType() = TYPE

    override fun createEditor(
        project: Project,
        virtualFile: VirtualFile
    ): FileEditor {
        val sid = virtualFile.getUserData(OpenAppServicePropertyViewAction.SUBSCRIPTION_ID)
            ?: error("Unable to determine subscription id")
        val id = virtualFile.getUserData(OpenAppServicePropertyViewAction.RESOURCE_ID)
            ?: error("Unable to determine resource id")
        return WebAppPropertyView.create(sid, id, virtualFile)
    }
}