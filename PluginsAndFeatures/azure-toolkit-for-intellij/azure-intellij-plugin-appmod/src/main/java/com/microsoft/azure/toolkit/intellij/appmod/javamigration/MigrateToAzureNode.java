/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javamigration;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModPanelHelper;
import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModUtils;
import com.microsoft.azure.toolkit.intellij.appmod.utils.Constants;
import com.microsoft.azure.toolkit.intellij.appmod.common.AppModPluginInstaller;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service Explorer node for "Migrate to Azure" functionality.
 * This node extends the azure-toolkit-ide-common-lib Node class to integrate with the Service Explorer tree.
 * 
 * State is computed on initialization and can be refreshed via refresh() method.
 */
@Slf4j
public final class MigrateToAzureNode extends Node<String> {
    private static final ExtensionPointName<IMigrateOptionProvider> childProviders =
        ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.migrateOptionProvider");
    
    private final Project project;

    private static final AzureIcon APP_MOD_ICON = AzureIcon.builder().iconPath(Constants.ICON_APPMOD_PATH).build();
    private static final AzureIcon CHANGELIST_ICON = AzureIcon.builder().iconPath("/icons/changelist").build();

    public MigrateToAzureNode(Project project) {
        super("Migrate to Azure");
        this.project = project;
        log.debug("[MigrateToAzureNode] Creating node for project: {}", project.getName());
        withIcon(APP_MOD_ICON);
        
        // Add refresh action to context menu (only once in constructor)
        withActions(new ActionGroup(
            new Action<>(Action.Id.<String>of("user/appmod.refresh_migrate_node"))
                .withLabel("Refresh")
                .withIcon(AzureIcons.Action.REFRESH.getIconPath())
                .withHandler((v, e) -> this.refresh())
                .withAuthRequired(false)
        ));
        
        // Use addChildren with a function so it rebuilds on refresh
        addChildren(data -> buildChildNodes());
        
        initializeNode();
    }

    public void initializeNode() {
        log.debug("[MigrateToAzureNode] initializeNode - appModInstalled: {}", AppModPluginInstaller.isAppModPluginInstalled());
        // Clear previous state
        clearClickHandlers();
        withDescription("");
        
        if (!AppModPluginInstaller.isAppModPluginInstalled()) {
            showNotInstalled();
        }
        // Don't call showMigrationOptions() here - let buildChildNodes() handle it
        // This avoids double loading of extension point data
    }
    
    /**
     * Refreshes the node by re-computing migration options.
     * Called by RefreshMigrateToAzureAction from context menu.
     */
    public void refresh() {
        log.debug("[MigrateToAzureNode] refresh called");
        AppModUtils.logTelemetryEvent("node.refresh");
        refreshChildren();  // This rebuilds children from addChildren function
    }

    public Project getProject() {
        return project;
    }

    private void showNotInstalled() {
        final boolean copilotInstalled = AppModPluginInstaller.isCopilotInstalled();
        log.debug("[MigrateToAzureNode] showNotInstalled - copilotInstalled: {}", copilotInstalled);
        
        // Dynamic description based on what needs to be installed
        final String description = copilotInstalled 
            ? "Install Github Copilot app modernization"
            : "Install GitHub Copilot and app modernization";
        withDescription(description);
        
        onClicked(e -> {
            log.info("[MigrateToAzureNode] Install click triggered");
            AppModUtils.logTelemetryEvent("node.click-install");
            AppModPluginInstaller.showInstallConfirmation(project, false, () -> AppModPluginInstaller.installPlugin(project, false));
        });
    }
    
    /**
     * Load migration options from extension points.
     */
    private List<MigrateNodeData> loadMigrationNodeData() {
        log.debug("[MigrateToAzureNode] loadMigrationNodeData - loading extension points");
        try {
            final List<MigrateNodeData> nodes = childProviders.getExtensionList().stream()
                .filter(provider -> provider.isApplicable(project))
                .sorted(Comparator.comparingInt(IMigrateOptionProvider::getPriority))
                .flatMap(provider -> provider.createNodeData(project).stream())
                .filter(MigrateNodeData::isVisible)
                .collect(Collectors.toList());
            if (nodes.isEmpty()) {
                AppModUtils.logTelemetryEvent("node.no-tasks");
            }
            return nodes;
        } catch (Exception e) {
            log.error("[MigrateToAzureNode] Failed to load migration node data", e);
            return List.of();
        }
    }
    
    /**
     * Build child nodes - called by Node framework on refresh.
     * Also updates description and click handler based on data.
     */
    private List<Node<?>> buildChildNodes() {
        log.debug("[MigrateToAzureNode] buildChildNodes - appModInstalled: {}", AppModPluginInstaller.isAppModPluginInstalled());
        try {
            if (!AppModPluginInstaller.isAppModPluginInstalled()) {
                log.debug("[MigrateToAzureNode] buildChildNodes - returning empty (plugin not installed)");
                return List.of();
            }
            
            final List<MigrateNodeData> nodeDataList = loadMigrationNodeData();
            log.debug("[MigrateToAzureNode] buildChildNodes - loaded {} nodes", nodeDataList.size());
            
            // Update description and click handler based on data
            clearClickHandlers();
            if (nodeDataList.isEmpty()) {
                log.debug("[MigrateToAzureNode] buildChildNodes - no migration options, setting click to open panel");
                withDescription("Open GitHub Copilot app modernization");
                onClicked(e -> {
                    log.info("[MigrateToAzureNode] Opening AppMod panel (no options)");
                    AppModPanelHelper.openAppModPanel(project, "node");
                });
            } else {
                withDescription("");
            }
            
            return nodeDataList.stream()
                .map(this::convertToNode)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("[MigrateToAzureNode] Failed to build child nodes", e);
            return List.of();
        }
    }
    
    /**
     * Converts MigrateNodeData to Node for Service Explorer compatibility.
     */
    private Node<?> convertToNode(MigrateNodeData data) {
        Node<MigrateNodeData> node = new Node<>(data);
        
        // Set basic properties
        node.withLabel(d -> d.getLabel());
        // Use Changelist icon for child nodes
        node.withIcon(CHANGELIST_ICON);
        if (data.getDescription() != null) {
            node.withTips(d -> d.getDescription());
        }
        
        // Set click handler
        if (data.hasClickHandler()) {
            node.onClicked(d -> {
                AppModUtils.logTelemetryEvent("node.click-task", Map.of("label", data.getLabel()));
                data.click(null);
            });
        }
        
        // Handle children - lazy or static
        if (data.isLazyLoading()) {
            // Lazy loading: use Node's native lazy loading mechanism
            node.withChildrenLoadLazily(true);
            node.addChildren(
                d -> d.getChildrenLoader().get(),
                (childData, parent) -> convertToNode(childData)
            );
        } else if (data.hasChildren()) {
            // Static children: add directly
            for (MigrateNodeData childData : data.getChildren()) {
                node.addChild(convertToNode(childData));
            }
        }
        
        return node;
    }

    @Override
    public synchronized void refreshView() {
        super.refreshView();
        refreshChildrenLater(false);
    }

    public static boolean isPluginInstalled() {
        return AppModPluginInstaller.isAppModPluginInstalled();
    }
}
