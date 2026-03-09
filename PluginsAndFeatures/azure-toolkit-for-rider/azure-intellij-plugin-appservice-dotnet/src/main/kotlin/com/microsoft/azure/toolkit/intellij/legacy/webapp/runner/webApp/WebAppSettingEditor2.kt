/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.UI
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import javax.swing.JPanel

class WebAppSettingEditor2(
    project: Project,
    parentCs: CoroutineScope,
    private val viewModel: WebAppSettingEditorViewModel
) : SettingsEditor<WebAppConfiguration>() {

    private val cs: CoroutineScope =
        parentCs.childScope("WebAppSettingEditor2", Dispatchers.UI + ModalityState.current().asContextElement()).also {
            Disposer.register(this) {
                it.cancel("WebAppSettingEditor2 disposal")
            }
        }

    private val webAppTreePanel = WebAppTreePanel(project, viewModel).also {
        Disposer.register(this, it)
    }

    private val configAndPlatformComboBox =
        PublishRuntimeSettingsCoreHelper.createConfigurationAndPlatformComboBox(project).component

    private val panel: JPanel = panel {
        row("Project:") {
            comboBox(
                viewModel.publishableProjects.toComboBoxModelIn(cs),
                renderer = SimpleListCellRenderer.create("") { it.projectName }
            )
                .bindSelectedItemIn(cs, viewModel.selectedProject)
                .align(Align.FILL)
        }
        row("Configuration:") {
            cell(configAndPlatformComboBox)
                .bindSelectedNullableItemIn(cs, viewModel.selectedConfigurationAndPlatform)
                .align(Align.FILL)
        }
        row {
            cell(webAppTreePanel.component)
                .align(Align.FILL)
                .resizableColumn()
        }.resizableRow()
        row {
            checkBox("Open browser after deployment")
                .bindSelected(viewModel.openBrowserAfterDeployment) { viewModel.setOpenBrowserFlag(it) }
        }
    }

    override fun resetEditorFrom(configuration: WebAppConfiguration) {
        val configurationOptions = configuration.state ?: return
        viewModel.setConfigFromOptions(configurationOptions)
    }

    override fun applyEditorTo(configuration: WebAppConfiguration) {
        val state = configuration.state ?: return
        viewModel.applySelectedConfigToOptions(state)
    }

    override fun createEditor() = panel
}