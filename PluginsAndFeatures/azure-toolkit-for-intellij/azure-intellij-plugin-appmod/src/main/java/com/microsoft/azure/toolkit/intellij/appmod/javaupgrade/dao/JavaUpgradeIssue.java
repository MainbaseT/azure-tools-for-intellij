/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao;

import lombok.Builder;
import lombok.Data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a detected upgrade issue in a Java project.
 * This class is aligned with the TypeScript implementation in vscode-java-dependency.
 * 
 * @see <a href="https://github.com/microsoft/vscode-java-dependency/blob/main/src/upgrade/type.ts">type.ts</a>
 */
@Data
@Builder
public class JavaUpgradeIssue {
    
    /**
     * The reason why an upgrade is recommended.
     * Aligned with UpgradeReason enum from vscode-java-dependency.
     */
    public enum UpgradeReason {
        /**
         * The version has reached end of life and is no longer maintained.
         */
        END_OF_LIFE,
        /**
         * The version is deprecated and should be upgraded.
         */
        DEPRECATED,
        /**
         * The version has known security vulnerabilities (CVEs).
         */
        CVE,
        /**
         * The JRE/JDK version is too old compared to the mature LTS version.
         */
        JRE_TOO_OLD
    }
    
    /**
     * Severity level of the issue.
     */
    public enum Severity {
        /**
         * The version is very old and may have security vulnerabilities or be unsupported.
         */
        CRITICAL,
        /**
         * The version is outdated and should be upgraded.
         */
        WARNING,
        /**
         * The version is slightly behind the latest, informational only.
         */
        INFO
    }
    
    /**
     * The package identifier (groupId:artifactId or "jdk" for JDK issues).
     */
    @Nonnull
    private String packageId;
    
    /**
     * The display name of the package (e.g., "Spring Boot", "JDK").
     */
    @Nonnull
    private String packageDisplayName;
    
    /**
     * The reason why upgrade is recommended.
     */
    @Nonnull
    private UpgradeReason upgradeReason;
    
    /**
     * Severity level of this issue.
     */
    @Nonnull
    private Severity severity;
    
    /**
     * The current version detected in the project.
     */
    @Nullable
    private String currentVersion;
    
    /**
     * The supported version range (e.g., "2.7.x || >=3.2").
     */
    @Nullable
    private String supportedVersion;
    
    /**
     * The suggested version to upgrade to.
     */
    @Nullable
    private String suggestedVersion;
    
    /**
     * Detailed message describing the issue.
     */
    @Nonnull
    private String message;
    
    /**
     * URL for more information about the issue.
     */
    @Nullable
    private String learnMoreUrl;
    
    /**
     * CVE identifier if this is a CVE issue.
     */
    @Nullable
    private String cveId;

    @Nullable
    private String eofDate;
    
    /**
     * Gets a formatted title for the notification.
     */
    public String getTitle() {
        return switch (upgradeReason) {
            case JRE_TOO_OLD -> "Outdated JDK Detected";
            case END_OF_LIFE -> packageDisplayName + " End of Life";
            case DEPRECATED -> packageDisplayName + " Deprecated";
            case CVE -> "Security Vulnerability in " + packageDisplayName;
        };
    }
}
