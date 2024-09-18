/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.intellij.ide.actions.ShowSettingsUtilImpl
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.registry.Registry
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBInsets
import com.jetbrains.rider.ui.components.base.Viewable
import com.microsoft.azure.toolkit.intellij.legacy.function.FUNCTIONS_CORE_TOOLS_LATEST_SUPPORTED_VERSION
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsInfoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JComponent
import javax.swing.JPanel

class InstallFunctionToolComponent(reloadTemplates: Runnable) : Viewable<JComponent> {

    private val panel: JPanel
    private val isCoreToolsFeedEnabled = Registry.`is`("azure.function_app.core_tools.feed.enabled")

    init {
        panel = panel {
            row {
                text("Azure Functions Project Templates")
            }
            row {
                text("Rider requires the Azure Functions Core Tools to be installed and configured to create new Azure Functions projects.")
            }
            row {
                link("Download Azure Functions Core Tools... (recommended)") {
                    val project = ProjectManager.getInstance().defaultProject
                    runWithModalProgressBlocking(project, "Downloading Azure Functions Core Tools...") {
                        withContext(Dispatchers.Default) {
                            FunctionCoreToolsInfoProvider.getInstance()
                                .retrieveForVersion(
                                    FUNCTIONS_CORE_TOOLS_LATEST_SUPPORTED_VERSION,
                                    true
                                )
                        }
                    }
                    reloadTemplates.run()
                }
            }.visible(isCoreToolsFeedEnabled)
            row {
                link("Configure Azure Functions Core Tools...") {
                    val project = ProjectManager.getInstance().defaultProject
                    ShowSettingsUtilImpl.showSettingsDialog(
                        project,
                        "com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionConfigurable",
                        ""
                    )
                    reloadTemplates.run()
                }
            }
        }.apply { border = IdeBorderFactory.createEmptyBorder(JBInsets(10, 20, 10, 20)) }
    }

    override fun getView() = panel
}