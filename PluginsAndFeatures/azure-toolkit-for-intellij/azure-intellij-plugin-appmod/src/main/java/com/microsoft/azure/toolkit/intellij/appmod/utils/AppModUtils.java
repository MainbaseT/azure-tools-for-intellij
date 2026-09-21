/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Utility class for App Modernization (Migrate to Azure) telemetry and common operations.
 */
public final class AppModUtils {

    private static final Logger LOG = Logger.getInstance(AppModUtils.class);
    private static final String SERVICE_NAME = "appmod";
    // Set to true to enable telemetry debug logging (for development only)
    private static final boolean DEBUG_TELEMETRY = false;

    private AppModUtils() {
        // Utility class, no instantiation allowed
    }

    /**
     * Logs a telemetry event for App Modernization features.
     * 
     * @param eventName The name of the event to log (e.g., "show-node", "click-option", "refresh")
     */
    public static void logTelemetryEvent(@Nonnull String eventName) {
        if (DEBUG_TELEMETRY) {
            LOG.info("[AppMod Telemetry] " + eventName);
        }
        final Map<String, String> properties = Map.of(
                AzureTelemeter.OP_NAME, eventName,
                AzureTelemeter.OP_PARENT_ID, SERVICE_NAME,
                AzureTelemeter.OPERATION_NAME, eventName,
                AzureTelemeter.SERVICE_NAME, SERVICE_NAME
        );
        AzureTaskManager.getInstance().runLater(() -> {
            AzureTelemeter.log(AzureTelemetry.Type.INFO, properties);
        });
    }

    /**
     * Logs a telemetry event with additional properties.
     * 
     * @param eventName The name of the event to log
     * @param additionalProperties Additional properties to include in the telemetry
     */
    public static void logTelemetryEvent(@Nonnull String eventName, @Nonnull Map<String, String> additionalProperties) {
        if (DEBUG_TELEMETRY) {
            LOG.info("[AppMod Telemetry] " + eventName + " " + additionalProperties);
        }
        final Map<String, String> properties = new java.util.HashMap<>(Map.of(
                AzureTelemeter.OP_NAME, eventName,
                AzureTelemeter.OP_PARENT_ID, SERVICE_NAME,
                AzureTelemeter.OPERATION_NAME, eventName,
                AzureTelemeter.SERVICE_NAME, SERVICE_NAME
        ));
        properties.putAll(additionalProperties);
        AzureTaskManager.getInstance().runLater(() -> {
            AzureTelemeter.log(AzureTelemetry.Type.INFO, properties);
        });
    }
}
