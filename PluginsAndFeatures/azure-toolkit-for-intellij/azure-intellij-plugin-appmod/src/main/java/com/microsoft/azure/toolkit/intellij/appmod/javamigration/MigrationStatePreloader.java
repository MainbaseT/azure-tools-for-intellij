/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javamigration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.microsoft.azure.toolkit.intellij.appmod.common.AppModPluginInstaller;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Preloads migration state when project opens.
 * This ensures that when user opens the context menu, the data is already available.
 */
@Slf4j
public class MigrationStatePreloader implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // Only preload if AppMod plugin is installed
        if (!AppModPluginInstaller.isAppModPluginInstalled()) {
            log.info("[MigrationStatePreloader] AppMod plugin not installed, skipping preload");
            return Unit.INSTANCE;
        }
        
        // Check if already loading or loaded
        if (Boolean.TRUE.equals(project.getUserData(MigrateToAzureAction.LOADING_KEY)) ||
            project.getUserData(MigrateToAzureAction.STATE_KEY) != null) {
            log.debug("[MigrationStatePreloader] Already loading or loaded, skipping");
            return Unit.INSTANCE;
        }
        
        // Mark as loading to prevent duplicate loading from MigrateToAzureAction
        project.putUserData(MigrateToAzureAction.LOADING_KEY, Boolean.TRUE);
        log.info("[MigrationStatePreloader] Starting preload for project: {}", project.getName());
        
        // Load in background thread
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final long startTime = System.currentTimeMillis();
                final MigrateToAzureAction.MigrationState state = MigrateToAzureAction.computeState(project);
                // Only cache if computation succeeded (state != null)
                // If failed (e.g., MCP server error), leave cache empty so next access will retry
                if (state != null) {
                    project.putUserData(MigrateToAzureAction.STATE_KEY, state);
                    log.info("[MigrationStatePreloader] Preload completed for project: {}, state: {}, took {}ms", 
                        project.getName(), state.state, System.currentTimeMillis() - startTime);
                } else {
                    log.warn("[MigrationStatePreloader] Preload failed for project: {}, will retry on next access", 
                        project.getName());
                }
            } catch (Exception e) {
                log.error("[MigrationStatePreloader] Preload failed for project: {}", project.getName(), e);
            } finally {
                project.putUserData(MigrateToAzureAction.LOADING_KEY, Boolean.FALSE);
            }
        });
        
        return Unit.INSTANCE;
    }
}
