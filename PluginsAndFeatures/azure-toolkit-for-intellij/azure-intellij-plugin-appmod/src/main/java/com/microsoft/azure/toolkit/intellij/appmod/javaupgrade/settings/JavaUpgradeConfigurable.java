/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaVersionNotificationService;

import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Settings page for Java Upgrade feature.
 * Allows users to enable/disable notifications and reset deferral settings.
 * 
 * Accessible via: Settings → Tools → Azure Toolkit → Java Upgrade
 */
public class JavaUpgradeConfigurable implements Configurable {

    private JPanel mainPanel;
    private JCheckBox enableNotificationsCheckBox;
    private JButton resetDeferralButton;
    private JLabel deferralStatusLabel;

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Java Upgrade";
    }

    @Override
    public @Nullable JComponent createComponent() {
        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(2, 0, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Title label
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel titleLabel = new JLabel("<html><b>Java Upgrade Notifications</b></html>");
        mainPanel.add(titleLabel, gbc);

        // Description
        gbc.gridy = 1;
        JLabel descriptionLabel = new JLabel("<html>Configure notifications for outdated JDK versions, " +
                "framework versions, and security vulnerabilities.</html>");
        mainPanel.add(descriptionLabel, gbc);

        // Enable notifications checkbox
        gbc.gridy = 2;
        enableNotificationsCheckBox = new JCheckBox("Show notifications for Java upgrade issues");
        enableNotificationsCheckBox.setToolTipText(
                "When enabled, balloon notifications will appear when outdated JDK or framework versions are detected.");
        mainPanel.add(enableNotificationsCheckBox, gbc);

        // Deferral section subtitle (indented)
        gbc.gridy = 3;
        gbc.insets = new Insets(2, 20, 2, 5);
        JLabel deferralTitle = new JLabel("Notification Deferral");
        mainPanel.add(deferralTitle, gbc);

        // Deferral status (indented)
        gbc.gridy = 4;
        deferralStatusLabel = new JLabel();
        updateDeferralStatusLabel();
        mainPanel.add(deferralStatusLabel, gbc);

        // Reset deferral button (indented)
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        resetDeferralButton = new JButton("Reset Deferral");
        resetDeferralButton.setToolTipText("Clear the \"Not Now\" deferral and allow notifications to show immediately.");
        resetDeferralButton.addActionListener(e -> {
            JavaVersionNotificationService.getInstance().clearDeferral();
            updateDeferralStatusLabel();
        });
        mainPanel.add(resetDeferralButton, gbc);

        // Spacer to push content to the top
        gbc.gridy = 6;
        gbc.insets = new Insets(2, 0, 2, 5);
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(new JPanel(), gbc);

        return mainPanel;
    }

    private void updateDeferralStatusLabel() {
        if (deferralStatusLabel == null || resetDeferralButton == null) {
            return;
        }
        final JavaVersionNotificationService service = JavaVersionNotificationService.getInstance();
        final long deferredUntil = service.getDeferredUntil();
        final long now = System.currentTimeMillis();

        if (deferredUntil > now) {
            // Notifications are deferred
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");
            String dateStr = dateFormat.format(new Date(deferredUntil));
            deferralStatusLabel.setText("<html>Notifications deferred until: <b>" + dateStr + "</b></html>");
            resetDeferralButton.setEnabled(true);
        } else {
            deferralStatusLabel.setText("Notifications are not deferred.");
            resetDeferralButton.setEnabled(false);
        }
    }

    @Override
    public boolean isModified() {
        if (enableNotificationsCheckBox == null) {
            return false;
        }
        final JavaVersionNotificationService service = JavaVersionNotificationService.getInstance();
        return enableNotificationsCheckBox.isSelected() != service.isNotificationsEnabled();
    }

    @Override
    public void apply() throws ConfigurationException {
        if (enableNotificationsCheckBox == null) {
            return;
        }
        final JavaVersionNotificationService service = JavaVersionNotificationService.getInstance();
        service.setNotificationsEnabled(enableNotificationsCheckBox.isSelected());
        AppModUtils.logTelemetryEvent("applyJavaUpgradeNotificationSettings", Map.of("notificationsEnabled", String.valueOf(enableNotificationsCheckBox.isSelected())));
    }

    @Override
    public void reset() {
        if (enableNotificationsCheckBox == null) {
            return;
        }
        final JavaVersionNotificationService service = JavaVersionNotificationService.getInstance();
        enableNotificationsCheckBox.setSelected(service.isNotificationsEnabled());
        updateDeferralStatusLabel();
        AppModUtils.logTelemetryEvent("resetJavaUpgradeNotificationDeferralSettings");
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
        enableNotificationsCheckBox = null;
        resetDeferralButton = null;
        deferralStatusLabel = null;
    }
}
