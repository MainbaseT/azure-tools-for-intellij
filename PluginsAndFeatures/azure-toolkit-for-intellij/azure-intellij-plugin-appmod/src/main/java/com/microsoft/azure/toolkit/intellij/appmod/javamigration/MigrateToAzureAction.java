/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javamigration;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModPanelHelper;
import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModUtils;
import com.microsoft.azure.toolkit.intellij.appmod.common.AppModPluginInstaller;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Unified ActionGroup for "Migrate to Azure" functionality.
 * Handles all three states:
 * 1. Plugin NOT installed - direct click triggers installation
 * 2. Plugin installed but no migration options - shows "Open App Mod Panel" action
 * 3. Plugin installed with migration options - sub-menu shows migration options
 * 
 * Data is preloaded by MigrationStatePreloader when project opens.
 * If data is not ready yet, shows "Open App Mod Panel" as fallback.
 */
@Slf4j
public class MigrateToAzureAction extends ActionGroup {
    private static final ExtensionPointName<IMigrateOptionProvider> MIGRATION_PROVIDERS =
        ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.migrateOptionProvider");
    static final Key<MigrationState> STATE_KEY = Key.create("azure.migrate.action.state");
    static final Key<Boolean> LOADING_KEY = Key.create("azure.migrate.action.loading");
    
    enum State { NOT_INSTALLED, LOADING, NO_OPTIONS, HAS_OPTIONS }
    
    static class MigrationState {
        final State state;
        final List<MigrateNodeData> nodes;
        
        MigrationState(State state, List<MigrateNodeData> nodes) {
            this.state = state;
            this.nodes = nodes;
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    
    /**
     * Gets migration state for the project.
     * State is preloaded by MigrationStatePreloader on project open.
     * Returns LOADING state if data is not ready yet, and triggers async loading.
     */
    private MigrationState getOrComputeState(Project project) {
        // Fast path: if plugin not installed, return immediately
        if (!AppModPluginInstaller.isAppModPluginInstalled()) {
            log.debug("[MigrateToAzureAction] Plugin not installed, returning NOT_INSTALLED");
            return new MigrationState(State.NOT_INSTALLED, List.of());
        }
        
        // Check if we have preloaded state
        MigrationState state = project.getUserData(STATE_KEY);
        if (state != null) {
            return state;
        }
        
        // State not ready yet - trigger async loading if not already loading
        Boolean isLoading = project.getUserData(LOADING_KEY);
        if (!Boolean.TRUE.equals(isLoading)) {
            project.putUserData(LOADING_KEY, Boolean.TRUE);
            log.debug("[MigrateToAzureAction] State not ready, triggering async load");
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    final MigrationState computedState = computeState(project);
                    if (computedState != null) {
                        project.putUserData(STATE_KEY, computedState);
                        log.debug("[MigrateToAzureAction] Async load completed, state: {}", computedState.state);
                    }
                } finally {
                    project.putUserData(LOADING_KEY, Boolean.FALSE);
                }
            });
        }
        
        log.debug("[MigrateToAzureAction] State not ready yet, returning LOADING");
        return new MigrationState(State.LOADING, List.of());
    }
    
    /**
     * Computes migration state by calling providers.
     * Called by MigrationStatePreloader during project startup.
     */
    static MigrationState computeState(Project project) {
        final long startTime = System.currentTimeMillis();
        log.debug("[MigrateToAzureAction] computeState - start");
        try {
            final long providerStartTime = System.currentTimeMillis();
            final List<MigrateNodeData> nodes = MIGRATION_PROVIDERS.getExtensionList().stream()
                .filter(provider -> provider.isApplicable(project))
                .sorted(Comparator.comparingInt(IMigrateOptionProvider::getPriority))
                .flatMap(provider -> provider.createNodeData(project).stream())
                .filter(MigrateNodeData::isVisible)
                .collect(Collectors.toList());
            
            log.debug("[MigrateToAzureAction] computeState - loaded {} nodes, provider call took {}ms, total {}ms", 
                nodes.size(), System.currentTimeMillis() - providerStartTime, System.currentTimeMillis() - startTime);
            
            if (nodes.isEmpty()) {
                AppModUtils.logTelemetryEvent("action.no-tasks");
            }
            
            return new MigrationState(
                nodes.isEmpty() ? State.NO_OPTIONS : State.HAS_OPTIONS,
                nodes
            );
        } catch (Exception e) {
            log.error("[MigrateToAzureAction] Failed to compute migration state, took {}ms", System.currentTimeMillis() - startTime, e);
            // Return null to indicate failure - caller should not cache this result
            return null;
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final long startTime = System.currentTimeMillis();
        final Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        
        final MigrationState migrationState = getOrComputeState(project);
        log.debug("[MigrateToAzureAction] update - state: {}, took {}ms", migrationState.state, System.currentTimeMillis() - startTime);
        
        // Common settings for all states
        e.getPresentation().setPopupGroup(true);
        e.getPresentation().putClientProperty(ActionUtil.ALWAYS_VISIBLE_GROUP, true);
        
        switch (migrationState.state) {
            case NOT_INSTALLED:
                final boolean copilotInstalled = AppModPluginInstaller.isCopilotInstalled();
                e.getPresentation().setText(copilotInstalled 
                    ? "Migrate to Azure (Install Github Copilot app modernization)"
                    : "Migrate to Azure (Install GitHub Copilot and app modernization)");
                e.getPresentation().setPerformGroup(true);
                e.getPresentation().putClientProperty(ActionUtil.SUPPRESS_SUBMENU, true);
                break;
            case LOADING:
            case NO_OPTIONS:
                e.getPresentation().setText("Migrate to Azure (Open GitHub Copilot app modernization)");
                e.getPresentation().setPerformGroup(true);
                e.getPresentation().putClientProperty(ActionUtil.SUPPRESS_SUBMENU, true);
                break;
            case HAS_OPTIONS:
                e.getPresentation().setText("Migrate to Azure");
                e.getPresentation().setPerformGroup(false);
                e.getPresentation().putClientProperty(ActionUtil.SUPPRESS_SUBMENU, false);
                break;
        }
        e.getPresentation().setEnabledAndVisible(true);
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        final MigrationState migrationState = getOrComputeState(project);
        log.debug("[MigrateToAzureAction] actionPerformed - state: {}", migrationState.state);
        
        switch (migrationState.state) {
            case NOT_INSTALLED:
                log.info("[MigrateToAzureAction] Install click triggered");
                AppModUtils.logTelemetryEvent("action.click-install");
                AppModPluginInstaller.showInstallConfirmation(project, false,
                    () -> AppModPluginInstaller.installPlugin(project, false));
                break;
            case LOADING:
            case NO_OPTIONS:
                log.info("[MigrateToAzureAction] Opening AppMod panel (state: {})", migrationState.state);
                AppModPanelHelper.openAppModPanel(project, "action");
                break;
            case HAS_OPTIONS:
                // Handled by popup menu
                break;
        }
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        final long startTime = System.currentTimeMillis();
        try {
            if (e == null) {
                return AnAction.EMPTY_ARRAY;
            }
            
            final Project project = e.getProject();
            if (project == null) {
                return AnAction.EMPTY_ARRAY;
            }
            
            final MigrationState migrationState = getOrComputeState(project);
            
            if (migrationState.state == State.HAS_OPTIONS) {
                final AnAction[] result = migrationState.nodes.stream()
                    .map(this::convertNodeToAction)
                    .toArray(AnAction[]::new);
                log.debug("[MigrateToAzureAction] getChildren - returned {} actions, took {}ms", result.length, System.currentTimeMillis() - startTime);
                return result;
            }
            
            log.debug("[MigrateToAzureAction] getChildren - no options, took {}ms", System.currentTimeMillis() - startTime);
            return AnAction.EMPTY_ARRAY;
        } catch (Exception ex) {
            log.error("[MigrateToAzureAction] Failed to get children, took {}ms", System.currentTimeMillis() - startTime, ex);
            return AnAction.EMPTY_ARRAY;
        }
    }
    
    private AnAction convertNodeToAction(MigrateNodeData nodeData) {
        if (nodeData.hasChildren()) {
            final DefaultActionGroup subgroup = new DefaultActionGroup(nodeData.getLabel(), true);
            subgroup.getTemplatePresentation().setIcon(AllIcons.Vcs.Changelist);

            try {
                final long loadStartTime = System.currentTimeMillis();
                final List<MigrateNodeData> children = nodeData.isLazyLoading() 
                    ? nodeData.getChildrenLoader().get() 
                    : nodeData.getChildren();
                log.debug("[MigrateToAzureAction] convertNodeToAction - loaded {} children for '{}', lazy={}, took {}ms", 
                    children.size(), nodeData.getLabel(), nodeData.isLazyLoading(), System.currentTimeMillis() - loadStartTime);
            
                for (MigrateNodeData child : children) {
                    if (child.isVisible()) {
                        subgroup.add(convertNodeToAction(child));
                    }
                }
            } catch (Exception e) {
                log.error("[MigrateToAzureAction] Failed to load children for node: {}", nodeData.getLabel(), e);
            }
            return subgroup;
        } else {
            return new AnAction(nodeData.getLabel(), nodeData.getDescription(), AllIcons.Vcs.Changelist) {
                @Override
                public void update(@NotNull AnActionEvent e) {
                    e.getPresentation().setEnabled(nodeData.isEnabled());
                }
                
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    AppModUtils.logTelemetryEvent("action.click-task", Map.of("label", nodeData.getLabel()));
                    nodeData.click(e);
                }
            };
        }
    }
}
