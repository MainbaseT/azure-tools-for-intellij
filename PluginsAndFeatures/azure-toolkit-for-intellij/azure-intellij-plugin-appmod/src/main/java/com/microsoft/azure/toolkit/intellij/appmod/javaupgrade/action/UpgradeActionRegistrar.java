/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.action;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.microsoft.azure.toolkit.intellij.appmod.common.AppModPluginInstaller;

import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModUtils;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registers the Upgrade action into the GitHub Copilot context menu at runtime.
 * This is needed because the Copilot plugin creates its context menu groups dynamically.
 */
@Slf4j
public class UpgradeActionRegistrar implements ProjectActivity {

    private static final String UPGRADE_ACTION_ID = "AzureToolkit.JavaUpgradeContextMenu";
    private static final String PROJECT_VIEW_POPUP_MENU = "ProjectViewPopupMenu";

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        try{
            discoverAndRegisterAction();
        } catch (Throwable e) {
            log.error("Failed to register Upgrade action in Copilot context menu.", e);
        }
        return Unit.INSTANCE;
    }

    private void discoverAndRegisterAction() {
        // Only proceed if Copilot plugin is installed
        if (!AppModPluginInstaller.isCopilotInstalled()) {
            log.info("GitHub Copilot plugin not installed; skipping UpgradeActionRegistrar.");
            return;
        }

        ActionManager actionManager = ActionManager.getInstance();
        
        // Get the ProjectViewPopupMenu group
        AnAction projectViewPopup = actionManager.getAction(PROJECT_VIEW_POPUP_MENU);
        if (projectViewPopup instanceof DefaultActionGroup) {
            DefaultActionGroup popupGroup = (DefaultActionGroup) projectViewPopup;
            
            // Search for the GitHub Copilot submenu within ProjectViewPopupMenu
            DefaultActionGroup copilotGroup = findCopilotSubmenu(popupGroup, actionManager);
            
            if (copilotGroup != null) {
                tryAddToGroup(actionManager, copilotGroup, "GitHub Copilot submenu");
            }
        }
    }

    /**
     * Search for the GitHub Copilot submenu within a parent group.
     * The Copilot plugin creates this dynamically, so we search by exact presentation text.
     */
    private DefaultActionGroup findCopilotSubmenu(DefaultActionGroup parentGroup, ActionManager actionManager) {
        for (AnAction child : parentGroup.getChildActionsOrStubs()) {
            if (child instanceof DefaultActionGroup) {
                DefaultActionGroup childGroup = (DefaultActionGroup) child;
                Presentation presentation = childGroup.getTemplatePresentation();
                String text = presentation.getText();
                String actionId = actionManager.getId(child);
                
                // Match exactly "GitHub Copilot" to avoid false positives
                if ("GitHub Copilot".equals(text)) {
                    return childGroup;
                }
            }
        }
        return null;
    }

    private void tryAddToGroup(ActionManager actionManager, DefaultActionGroup group, String groupId) {
        AnAction upgradeAction = actionManager.getAction(UPGRADE_ACTION_ID);
        if (upgradeAction == null) {
            return;
        }
        
        // Check if action is not already added
        if (!containsAction(group, UPGRADE_ACTION_ID, actionManager)) {
            // Add a separator before the upgrade action to visually group it
            group.add(Separator.create());
            AppModUtils.logTelemetryEvent("java-upgrade.contextmenu.action.registered");
            group.add(upgradeAction);
            log.info("Registered Upgrade action into {}.", groupId);
        }
    }

    private boolean containsAction(DefaultActionGroup group, String actionId, ActionManager actionManager) {
        for (AnAction action : group.getChildActionsOrStubs()) {
            if (action != null && actionId.equals(actionManager.getId(action))) {
                return true;
            }
        }
        return false;
    }
}
