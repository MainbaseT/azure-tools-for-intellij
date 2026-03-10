/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("DuplicatedCode", "UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionApp

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
import com.microsoft.azure.toolkit.intellij.appservice.utils.bindSelected
import com.microsoft.azure.toolkit.intellij.appservice.utils.bindSelectedItemIn
import com.microsoft.azure.toolkit.intellij.appservice.utils.toComboBoxModelIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.swing.JPanel

class FunctionDeploymentSettingsEditor(
    project: Project,
    parentCs: CoroutineScope,
    private val viewModel: FunctionDeploymentSettingsEditorViewModel
) : SettingsEditor<FunctionDeploymentConfiguration>() {

    private val cs: CoroutineScope =
        parentCs.childScope(
            "FunctionDeploymentSettingsEditor",
            Dispatchers.UI + ModalityState.current().asContextElement()
        ).also {
            Disposer.register(this) {
                it.cancel("FunctionDeploymentSettingsEditor disposal")
            }
        }

    init {
        cs.launch {
            viewModel.selectedAppService.collect {
                fireEditorStateChanged()
            }
        }
    }

    private val functionAppTreePanel = FunctionAppDeploymentTreePanel(project, viewModel).also {
        Disposer.register(this, it)
    }

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
            comboBox(
                viewModel.configurationAndPlatforms.toComboBoxModelIn(cs),
                renderer = SimpleListCellRenderer.create("") { "${it.configuration} | ${it.platform}" }
            )
                .bindSelectedItemIn(cs, viewModel.selectedConfigurationAndPlatform)
                .align(Align.FILL)
        }
        row {
            cell(functionAppTreePanel.component)
                .align(Align.FILL)
                .resizableColumn()
        }.resizableRow()
        row {
            checkBox("Open browser after deployment")
                .bindSelected(viewModel.openBrowserAfterDeployment) { viewModel.setOpenBrowserFlag(it) }
        }
    }

    override fun resetEditorFrom(configuration: FunctionDeploymentConfiguration) {
        val configurationOptions = configuration.state ?: return
        viewModel.setConfigFromOptions(configurationOptions)
    }

    override fun applyEditorTo(configuration: FunctionDeploymentConfiguration) {
        val state = configuration.state ?: return
        viewModel.applySelectedConfigToOptions(state)
    }

    override fun createEditor() = panel
}