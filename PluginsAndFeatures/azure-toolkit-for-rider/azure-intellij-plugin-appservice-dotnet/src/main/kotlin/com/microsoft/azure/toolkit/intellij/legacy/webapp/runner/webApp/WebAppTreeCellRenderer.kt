/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp

import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.microsoft.azure.toolkit.intellij.AppServiceIcons
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

internal class WebAppTreeCellRenderer : ColoredTreeCellRenderer() {
    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        val node = value as? DefaultMutableTreeNode ?: return
        when (val userObject = node.userObject) {
            is GroupNode -> {
                icon = AppServiceIcons.ResourceGroup
                append(userObject.name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            }

            is WebAppNode -> {
                val webAppModel = userObject.webAppModel
                val os = webAppModel.config.runtime?.os

                icon = if (os == OperatingSystem.LINUX) AppServiceIcons.LinuxWebApp else AppServiceIcons.WebApp

                append(webAppModel.config.appName ?: "Unknown")
                if (webAppModel is WebAppModel.DraftWebAppModel) {
                    append(" (New) ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }

                val resourceGroup = webAppModel.config.resourceGroup
                if (!resourceGroup.isNullOrEmpty()) {
                    append("  $resourceGroup", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
            }
        }
    }
}