/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class WebAppTreePanel(
    cs: CoroutineScope,
    private val vm: WebAppSettingEditorViewModel
) : Disposable {

    private data class GroupNode(val name: String)
    private interface WebAppNode {
        val item: AppServiceConfig
    }

    private data class WebAppRemoteLeafNode(override val item: AppServiceConfig) : WebAppNode
    private data class WebAppDraftLeafNode(override val item: AppServiceConfig) : WebAppNode

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

        cs.launch {
            vm.webAppsLoading.collect { loading ->
                withContext(Dispatchers.EDT) {
                    if (loading) loadingPanel.startLoading() else loadingPanel.stopLoading()
                }
            }
        }

        cs.launch {
            combine(vm.webAppItems, vm.draftWebApps, vm.searchQuery) { remoteApps, draftApps, searchQuery ->
                Triple(remoteApps, draftApps, searchQuery)
            }.collect { (remoteApps, draftApps, query) ->
                withContext(Dispatchers.EDT) {
                    rebuildTreeModel(remoteApps, draftApps, query)
                }
            }
        }

        cs.launch {
            vm.selectedWebApp.collect { selected ->
                withContext(Dispatchers.EDT) {
                    if (isUpdatingSelection) return@withContext
                    selectNodeForConfig(selected)
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
        TreeSpeedSearch.installOn(tree)

        tree.addTreeSelectionListener {
            if (isUpdatingSelection) return@addTreeSelectionListener
            val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return@addTreeSelectionListener
            val leaf = node.userObject as? WebAppRemoteLeafNode ?: return@addTreeSelectionListener
            isUpdatingSelection = true
            try {
                vm.selectWebApp(leaf.item)
            } finally {
                isUpdatingSelection = false
            }
        }
    }

    private fun setupLayout() {
        val actionGroup = DefaultActionGroup(
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

    private fun rebuildTreeModel(remoteApps: List<AppServiceConfig>, draftApps: List<AppServiceConfig>, query: String) {
        val filteredRemoteApps = if (query.isEmpty()) remoteApps else remoteApps.filter { matchesQuery(it, query) }
        val filteredDraftApps = if (query.isEmpty()) draftApps else draftApps.filter { matchesQuery(it, query) }

        val root = DefaultMutableTreeNode()

        if (filteredRemoteApps.isNotEmpty()) {
            val webAppsGroup = DefaultMutableTreeNode(GroupNode("Web Apps"))
            filteredRemoteApps
                .sortedBy { it.appName }
                .forEach { webAppsGroup.add(DefaultMutableTreeNode(WebAppRemoteLeafNode(it))) }
            root.add(webAppsGroup)
        }

        if (filteredDraftApps.isNotEmpty()) {
            val draftsGroup = DefaultMutableTreeNode(GroupNode("Drafts"))
            filteredDraftApps
                .sortedBy { it.appName }
                .forEach { draftsGroup.add(DefaultMutableTreeNode(WebAppDraftLeafNode(it))) }
            root.add(draftsGroup)
        }

        treeModel.setRoot(root)
        treeModel.reload()

        for (i in 0 until root.childCount) {
            val groupNode = root.getChildAt(i) as DefaultMutableTreeNode
            tree.expandPath(TreePath(groupNode.path))
        }

        selectNodeForConfig(vm.selectedWebApp.value)
    }

    private fun selectNodeForConfig(config: AppServiceConfig?) {
        if (config == null) {
            tree.clearSelection()
            return
        }
        val root = treeModel.root as? DefaultMutableTreeNode ?: return
        for (i in 0 until root.childCount) {
            val group = root.getChildAt(i) as DefaultMutableTreeNode
            for (j in 0 until group.childCount) {
                val leaf = group.getChildAt(j) as DefaultMutableTreeNode
                val nodeObj = leaf.userObject as? WebAppNode ?: continue
                if (WebAppSettingEditorViewModel.isSameApp(nodeObj.item, config)) {
                    isUpdatingSelection = true
                    try {
                        tree.selectionPath = TreePath(leaf.path)
                    } finally {
                        isUpdatingSelection = false
                    }
                    return
                }
            }
        }
    }

    private fun matchesQuery(config: AppServiceConfig, query: String): Boolean {
        val lowerQuery = query.lowercase()
        return config.appName?.lowercase()?.contains(lowerQuery) == true ||
                config.resourceGroup?.lowercase()?.contains(lowerQuery) == true
    }

    override fun dispose() {}

    private class WebAppTreeCellRenderer : ColoredTreeCellRenderer() {
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
                    icon = AllIcons.Nodes.Module
                    append(userObject.name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                }

                is WebAppRemoteLeafNode -> {
                    icon = AllIcons.Nodes.Deploy
                    val config = userObject.item
                    append(config.appName ?: "Unknown")
                    val os = config.runtime?.os
                    if (os != null) {
                        append("  $os", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    }
                    val resourceGroup = config.resourceGroup
                    if (!resourceGroup.isNullOrEmpty()) {
                        append("  $resourceGroup", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    }
                }

                is WebAppDraftLeafNode -> {
                    icon = AllIcons.Nodes.Deploy
                    val config = userObject.item
                    append("(New) ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    append(config.appName ?: "Unknown")
                    val os = config.runtime?.os
                    if (os != null) {
                        append("  $os", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    }
                    val resourceGroup = config.resourceGroup
                    if (!resourceGroup.isNullOrEmpty()) {
                        append("  $resourceGroup", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    }
                }
            }
        }
    }
}
