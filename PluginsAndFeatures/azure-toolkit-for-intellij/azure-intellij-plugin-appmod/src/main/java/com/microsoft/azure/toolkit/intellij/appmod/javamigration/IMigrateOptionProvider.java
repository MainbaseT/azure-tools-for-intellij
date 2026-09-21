/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javamigration;

import com.intellij.openapi.project.Project;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Extension point interface for providing migration nodes.
 * 
 * Implementations of this interface will be discovered via IntelliJ's extension point mechanism
 * and used to dynamically construct the migration options tree in:
 * - Service Explorer (MigrateToAzureNode)
 * - Context Menu (MigrateToAzureAction)
 * - Project Explorer (MigrateToAzureFacetNode)
 * 
 * Example implementation:
 * <pre>
 * public class MyMigrationProvider implements IMigrateOptionProvider {
 *     @Override
 *     public List&lt;MigrateNodeData&gt; createNodeData(@Nonnull Project project) {
 *         return List.of(
 *             MigrateNodeData.builder("My Migration Option")
 *                 .onClick(() -> performMigration(project))
 *                 .build()
 *         );
 *     }
 * }
 * </pre>
 * 
 * Registration in plugin.xml:
 * <pre>
 * &lt;extensions defaultExtensionNs="com.microsoft.tooling.msservices.intellij.azure"&gt;
 *     &lt;migrateOptionProvider implementation="your.package.MyMigrationProvider"/&gt;
 * &lt;/extensions&gt;
 * </pre>
 */
public interface IMigrateOptionProvider {
    
    /**
     * Creates migration node data for the Migrate to Azure section.
     * 
     * This method is called each time the migration menu/tree is constructed.
     * The returned list can contain multiple MigrateNodeData instances,
     * each representing a single action or a group of options.
     *
     * @param project The current IntelliJ project
     * @return A list of MigrateNodeData instances representing the migration option(s)
     */
    @Nonnull
    List<MigrateNodeData> createNodeData(@Nonnull Project project);
    
    /**
     * Returns the priority/order of this node provider.
     * Nodes will be displayed in ascending order of priority.
     * Lower numbers appear first.
     *
     * @return Priority value (default: 100)
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * Determines whether this provider should create a node.
     * Can be used to conditionally show/hide migration options based on context.
     *
     * @param project The current IntelliJ project
     * @return true if this provider should contribute a node, false otherwise
     */
    default boolean isApplicable(@Nonnull Project project) {
        return true;
    }
}
