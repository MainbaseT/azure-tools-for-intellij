/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;

import javax.annotation.Nonnull;

/**
 * Helper class for opening the App Modernization Panel.
 */
public final class AppModPanelHelper {
    
    public static final String TOOL_WINDOW_ID = "GitHub Copilot app modernization";
    
    private AppModPanelHelper() {
        // Utility class
    }
    
    /**
     * Opens the App Modernization Panel tool window.
     * 
     * @param project the current project
     * @param source the source of the open request (e.g., "node", "action", "facet")
     */
    public static void openAppModPanel(@Nonnull Project project, @Nonnull String source) {
        final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        final ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
        
        if (toolWindow != null) {
            AppModUtils.logTelemetryEvent(source + ".open-panel");
            toolWindow.show();
        } else {
            AppModUtils.logTelemetryEvent(source + ".open-panel-failed");
            AzureMessager.getMessager().warning("App Modernization Panel is not available. Please ensure the GitHub Copilot App Modernization plugin is installed and enabled.");
        }
    }
}
