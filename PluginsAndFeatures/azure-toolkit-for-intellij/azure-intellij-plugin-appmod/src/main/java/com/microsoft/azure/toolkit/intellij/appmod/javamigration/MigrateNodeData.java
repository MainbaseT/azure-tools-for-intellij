/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javamigration;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Unified data structure for migration nodes.
 * This class is used by MigrateToAzureNode, MigrateToAzureAction, and MigrateToAzureFacetNode.
 * 
 * Features:
 * - Basic properties: label, description (used as menu description and tooltip)
 * - Click handling for both leaf and parent nodes
 * - Static children or lazy-loaded children via childrenLoader
 * 
 * Lazy Loading:
 * Provider can declare lazy loading intent via childrenLoader. Each consumer handles it based on capability:
 * - Service Explorer: Supports lazy loading → uses Node.withChildrenLoadLazily(true)
 * - Project Explorer: Native lazy loading → buildChildren() calls loader
 * - Action menus: No lazy loading → calls loader.get() immediately
 * 
 * Usage examples:
 * 
 * 1. Simple leaf node with click action:
 * <pre>
 * MigrateNodeData.builder("Option A")
 *     .onClick(e -> doSomething())
 *     .build();
 * </pre>
 * 
 * 2. Parent node with static children:
 * <pre>
 * MigrateNodeData.builder("Parent")
 *     .addChild(childNode1)
 *     .addChild(childNode2)
 *     .build();
 * </pre>
 * 
 * 3. Parent node with lazy-loaded children:
 * <pre>
 * MigrateNodeData.builder("Lazy Parent")
 *     .childrenLoader(() -> loadChildrenFromServer())
 *     .build();
 * </pre>
 */
@Getter
public class MigrateNodeData {
    
    // ==================== Basic Properties ====================
    
    /**
     * Display label for the node.
     */
    @Nonnull
    private final String label;
    
    /**
     * Description text. Used as:
     * - Menu item description in MigrateToAzureAction
     * - Tooltip in MigrateToAzureNode and MigrateToAzureFacetNode
     */
    @Nullable
    private String description;
    
    // ==================== State ====================
    
    /**
     * Whether the node is enabled (clickable).
     */
    @Setter
    private boolean enabled = true;
    
    /**
     * Whether the node is visible.
     */
    @Setter
    private boolean visible = true;
    
    // ==================== Click Handling ====================
    
    /**
     * Click handler. Can be set on any node (leaf or parent).
     * For parent nodes in menus, this may be triggered by a specific action.
     */
    @Nullable
    private Consumer<Object> clickHandler;
    
    /**
     * Double-click handler. Useful for tree views where single-click selects
     * and double-click performs action.
     */
    @Nullable
    private Consumer<Object> doubleClickHandler;
    
    // ==================== Children ====================
    
    /**
     * Static list of child nodes.
     */
    @Nonnull
    private final List<MigrateNodeData> children = new ArrayList<>();
    
    /**
     * Lazy loader for children. If set, consumers that support lazy loading
     * will use this to load children on demand. Consumers without lazy loading
     * support will call this immediately.
     * 
     * Note: This is just a declaration of intent. MigrateNodeData does NOT
     * manage loading state - each consumer handles that according to its capability.
     */
    @Nullable
    private Supplier<List<MigrateNodeData>> childrenLoader;
    
    // ==================== Constructor ====================
    
    private MigrateNodeData(@Nonnull String label) {
        this.label = label;
    }
    
    // ==================== Builder ====================
    
    /**
     * Creates a new builder for MigrateNodeData.
     * 
     * @param label The display label for the node
     * @return A new builder instance
     */
    public static Builder builder(@Nonnull String label) {
        return new Builder(label);
    }
    
    // ==================== Methods ====================
    
    /**
     * Checks if this node has children (static or via loader).
     */
    public boolean hasChildren() {
        return !children.isEmpty() || childrenLoader != null;
    }
    
    /**
     * Gets the static children list.
     * For lazy-loaded children, use getChildrenLoader() instead.
     */
    @Nonnull
    public List<MigrateNodeData> getChildren() {
        return children;
    }
    
    /**
     * Gets the children loader for lazy loading.
     * Consumers should check this and handle according to their capability.
     */
    @Nullable
    public Supplier<List<MigrateNodeData>> getChildrenLoader() {
        return childrenLoader;
    }
    
    /**
     * Checks if this node uses lazy loading for children.
     */
    public boolean isLazyLoading() {
        return childrenLoader != null;
    }
    
    /**
     * Triggers the click handler.
     * 
     * @param event The event object (can be AnActionEvent, MouseEvent, or null)
     */
    public void click(@Nullable Object event) {
        if (clickHandler != null && enabled) {
            clickHandler.accept(event);
        }
    }
    
    /**
     * Triggers the double-click handler.
     * Falls back to click handler if double-click is not set.
     * 
     * @param event The event object
     */
    public void doubleClick(@Nullable Object event) {
        if (doubleClickHandler != null && enabled) {
            doubleClickHandler.accept(event);
        } else {
            click(event);
        }
    }
    
    /**
     * Checks if this node has a click handler.
     */
    public boolean hasClickHandler() {
        return clickHandler != null;
    }
    
    /**
     * Adds a child node dynamically.
     */
    public void addChild(@Nonnull MigrateNodeData child) {
        children.add(child);
    }
    
    /**
     * Removes a child node.
     */
    public void removeChild(@Nonnull MigrateNodeData child) {
        children.remove(child);
    }
    
    // ==================== Builder Class ====================
    
    public static class Builder {
        private final MigrateNodeData data;
        
        private Builder(@Nonnull String label) {
            this.data = new MigrateNodeData(label);
        }
        
        /**
         * Sets the description (used as menu description and tooltip).
         */
        public Builder description(@Nullable String description) {
            data.description = description;
            return this;
        }
        
        /**
         * Sets the enabled state.
         */
        public Builder enabled(boolean enabled) {
            data.enabled = enabled;
            return this;
        }
        
        /**
         * Sets the visible state.
         */
        public Builder visible(boolean visible) {
            data.visible = visible;
            return this;
        }
        
        /**
         * Sets the click handler.
         */
        public Builder onClick(@Nullable Consumer<Object> handler) {
            data.clickHandler = handler;
            return this;
        }
        
        /**
         * Sets the click handler (no-arg version).
         */
        public Builder onClick(@Nullable Runnable handler) {
            if (handler != null) {
                data.clickHandler = e -> handler.run();
            }
            return this;
        }
        
        /**
         * Sets the double-click handler.
         */
        public Builder onDoubleClick(@Nullable Consumer<Object> handler) {
            data.doubleClickHandler = handler;
            return this;
        }
        
        /**
         * Sets the double-click handler (no-arg version).
         */
        public Builder onDoubleClick(@Nullable Runnable handler) {
            if (handler != null) {
                data.doubleClickHandler = e -> handler.run();
            }
            return this;
        }
        
        /**
         * Adds a child node.
         */
        public Builder addChild(@Nonnull MigrateNodeData child) {
            data.children.add(child);
            return this;
        }
        
        /**
         * Adds multiple child nodes.
         */
        public Builder addChildren(@Nonnull List<MigrateNodeData> children) {
            data.children.addAll(children);
            return this;
        }
        
        /**
         * Sets the children loader for lazy loading.
         * Consumers that support lazy loading will use this to load children on demand.
         * Consumers without lazy loading support will call this immediately.
         * 
         * @param loader Supplier that returns the list of child nodes
         */
        public Builder childrenLoader(@Nullable Supplier<List<MigrateNodeData>> loader) {
            data.childrenLoader = loader;
            return this;
        }
        
        /**
         * Builds the MigrateNodeData instance.
         */
        public MigrateNodeData build() {
            return data;
        }
    }
}
