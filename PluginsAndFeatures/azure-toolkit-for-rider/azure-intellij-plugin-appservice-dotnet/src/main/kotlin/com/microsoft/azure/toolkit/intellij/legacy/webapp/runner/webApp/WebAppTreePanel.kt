/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.launchOnShow
import com.intellij.util.ui.tree.TreeUtil
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp.WebAppModel.DraftWebAppModel
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp.WebAppModel.RemoteWebAppModel
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.common.action.Action
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel
import kotlin.time.Duration.Companion.milliseconds

internal data class GroupNode(val name: String)
internal data class WebAppNode(val webAppModel: WebAppModel)
internal data class ResourceGroupNode(val name: String)
internal data class DeploymentSlotsGroupNode(val webAppModel: RemoteWebAppModel)
internal data class DeploymentSlotNode(val slotName: String, val webAppModel: RemoteWebAppModel)

class WebAppTreePanel(
    private val project: Project,
    private val vm: WebAppSettingEditorViewModel
) : Disposable {
    private val searchTextField = SearchTextField(false)

    private val treeModel = DefaultTreeModel(DefaultMutableTreeNode())
    private val tree = Tree(treeModel)
    private val loadingPanel = JBLoadingPanel(BorderLayout(), this).apply {
        border = JBUI.Borders.customLine(JBColor.border(), 1)
    }

    private var isUpdatingSelection = false

    val component: JComponent
        get() = loadingPanel

    init {
        setupSearchField()
        setupTree()
        setupLayout()

        tree.launchOnShow("${WebAppTreePanel::class.java.name}.tree.rebuild") {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            combine(
                vm.webAppsState,
                vm.draftWebApps,
                vm.searchQuery.debounce(150.milliseconds)
            ) { state, draftApps, searchQuery ->
                Triple(state, draftApps, searchQuery)
            }.collectLatest { (state, draftApps, query) ->
                withContext(Dispatchers.EDT) {
                    when (state) {
                        is WebAppsLoadState.Loading -> {
                            loadingPanel.startLoading()
                            rebuildTreeModel(emptyList(), emptyList(), "")
                        }

                        is WebAppsLoadState.Loaded -> {
                            loadingPanel.stopLoading()
                            rebuildTreeModel(state.items, draftApps, query)
                        }

                        is WebAppsLoadState.Error -> {
                            loadingPanel.stopLoading()
                            rebuildTreeModel(emptyList(), draftApps, query)
                        }
                    }
                }
            }
        }

        tree.launchOnShow("${WebAppTreePanel::class.java.name}.tree.select") {
            vm.selectedWebApp.collect { pair ->
                withContext(Dispatchers.EDT) {
                    selectNodeForConfig(pair?.first, pair?.second)
                }
            }
        }
    }

    private fun setupSearchField() {
        searchTextField.textEditor.emptyText.text = "Search web apps..."
        searchTextField.textEditor.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateQuery()
            override fun removeUpdate(e: DocumentEvent?) = updateQuery()
            override fun changedUpdate(e: DocumentEvent?) = updateQuery()
            private fun updateQuery() {
                vm.setSearchQuery(searchTextField.text.trim())
            }
        })
    }

    private fun setupTree() {
        tree.isRootVisible = false
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.cellRenderer = WebAppTreeCellRenderer()
        tree.emptyText.text = "No web apps found"
        TreeSpeedSearch.installOn(tree)

        tree.addTreeSelectionListener {
            val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return@addTreeSelectionListener
            withSelectionGuard {
                when (val userObject = node.userObject) {
                    is WebAppNode -> vm.selectWebApp(userObject.webAppModel, null)
                    is DeploymentSlotNode -> vm.selectWebApp(userObject.webAppModel, userObject.slotName)
                    is DeploymentSlotsGroupNode -> vm.selectWebApp(userObject.webAppModel, null)
                }
            }
        }
    }

    private fun setupLayout() {
        val actionGroup = DefaultActionGroup(
            DumbAwareAction.create("Create New", AllIcons.General.Add) {
                val dialog = WebAppCreationDialog(project, false)
                Disposer.register(this, dialog)
                dialog.setOkAction(
                    Action<AppServiceConfig>(Action.Id.of("user/webapp.create_app.app"))
                        .withLabel("Create")
                        .withIdParam(AppServiceConfig::appName)
                        .withSource { it }
                        .withAuthRequired(false)
                        .withHandler { config -> vm.addDraftWebApp(config) }
                )
                dialog.show()
            },
            DumbAwareAction.create("Refresh", AllIcons.Actions.Refresh) { vm.refreshWebApps() }
        )
        val toolbar = ActionUtil.createToolbarComponent(tree, "WebAppTreePanel", actionGroup, true)

        val topPanel = panel {
            row {
                cell(searchTextField).align(AlignX.FILL).resizableColumn()
                cell(toolbar).align(AlignX.RIGHT)
            }
        }.apply {
            border = JBUI.Borders.empty(UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP)
        }

        val scrollPane = JBScrollPane(tree).apply {
            border = JBUI.Borders.empty()
        }

        loadingPanel.add(topPanel, BorderLayout.NORTH)
        loadingPanel.add(scrollPane, BorderLayout.CENTER)
    }

    private fun rebuildTreeModel(
        remoteApps: List<RemoteWebAppModel>,
        draftApps: List<DraftWebAppModel>,
        query: String
    ) {
        val expandedPaths = TreeUtil.collectExpandedPaths(tree)

        val filteredDraftApps =
            if (query.isEmpty()) draftApps
            else draftApps.filter { matchesQuery(it.config, query) }

        val root = treeModel.root as DefaultMutableTreeNode
        root.removeAllChildren()

        if (filteredDraftApps.isNotEmpty()) {
            val draftsGroup = DefaultMutableTreeNode(GroupNode("Drafts"))
            filteredDraftApps
                .sortedBy { it.config.appName }
                .forEach { draftsGroup.add(DefaultMutableTreeNode(WebAppNode(it))) }
            root.add(draftsGroup)
        }

        val lowerQuery = query.lowercase()
        val appsByResourceGroup = remoteApps.groupBy { it.resourceGroup }
        for ((resourceGroup, apps) in appsByResourceGroup.entries.sortedBy { it.key.lowercase() }) {
            val filteredApps = if (query.isEmpty()) {
                apps.map { it to it.deploymentSlots }
            } else {
                val resourceGroupMatches = resourceGroup.lowercase().contains(lowerQuery)
                apps.mapNotNull { app ->
                    val appNameMatches = app.config.appName?.lowercase()?.contains(lowerQuery) == true
                    val matchingSlots = app.deploymentSlots.filter { it.lowercase().contains(lowerQuery) }

                    when {
                        resourceGroupMatches || appNameMatches -> app to app.deploymentSlots
                        matchingSlots.isNotEmpty() -> app to matchingSlots
                        else -> null
                    }
                }
            }

            if (filteredApps.isEmpty()) continue

            val rgNode = DefaultMutableTreeNode(ResourceGroupNode(resourceGroup))
            for ((app, slots) in filteredApps.sortedBy { it.first.config.appName?.lowercase() }) {
                val appNode = DefaultMutableTreeNode(WebAppNode(app))
                if (slots.isNotEmpty()) {
                    val slotsGroup = DefaultMutableTreeNode(DeploymentSlotsGroupNode(app))
                    slots.sorted().forEach { slotName ->
                        slotsGroup.add(DefaultMutableTreeNode(DeploymentSlotNode(slotName, app)))
                    }
                    appNode.add(slotsGroup)
                }
                rgNode.add(appNode)
            }
            root.add(rgNode)
        }

        treeModel.reload()

        when {
            // Filter active: expand entire tree to reveal all matches (platform convention)
            query.isNotEmpty() -> TreeUtil.expandAll(tree)
            // No filter, but have prior state: restore it
            expandedPaths.isNotEmpty() -> TreeUtil.restoreExpandedPaths(tree, expandedPaths)
        }

        val selectedWebApp = vm.selectedWebApp.value
        selectNodeForConfig(selectedWebApp?.first, selectedWebApp?.second)
    }

    private fun selectNodeForConfig(config: AppServiceConfig?, slotName: String?) {
        if (config == null) {
            tree.clearSelection()
            return
        }

        val root = treeModel.root as? DefaultMutableTreeNode ?: return

        val targetNode = TreeUtil.findNode(root) { node ->
            when (val obj = node.userObject) {
                is WebAppNode ->
                    slotName == null &&
                            WebAppSettingEditorViewModel.isSameApp(obj.webAppModel.config, config)

                is DeploymentSlotNode ->
                    slotName != null &&
                            obj.slotName == slotName &&
                            WebAppSettingEditorViewModel.isSameApp(obj.webAppModel.config, config)

                else -> false
            }
        } ?: return

        withSelectionGuard { TreeUtil.selectNode(tree, targetNode) }
    }

    private inline fun withSelectionGuard(action: () -> Unit) {
        if (isUpdatingSelection) return
        isUpdatingSelection = true
        try {
            action()
        } finally {
            isUpdatingSelection = false
        }
    }

    private fun matchesQuery(config: AppServiceConfig, query: String): Boolean {
        val lowerQuery = query.lowercase()
        return config.appName?.lowercase()?.contains(lowerQuery) == true ||
                config.resourceGroup?.lowercase()?.contains(lowerQuery) == true
    }

    override fun dispose() {}
}
