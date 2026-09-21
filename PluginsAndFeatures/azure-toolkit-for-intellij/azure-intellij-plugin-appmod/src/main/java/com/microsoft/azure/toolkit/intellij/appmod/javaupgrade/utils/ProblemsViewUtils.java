/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for extracting information from the Problems View in IntelliJ IDEA.
 * Provides cross-version compatibility for IntelliJ 2025.2 and newer versions.
 */
public final class ProblemsViewUtils {

    private ProblemsViewUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Extracts the problem description from the action event context.
     * Uses multiple approaches for compatibility across different IntelliJ versions.
     *
     * @param e the action event
     * @return the problem description, or null if not found
     */
    @Nullable
    public static String extractProblemDescription(@NotNull AnActionEvent e) {
        // Approach 1: Try to get selected items from the tree (works in IntelliJ 2025.3+)
        try {
            final Object[] selectedItems = e.getData(PlatformDataKeys.SELECTED_ITEMS);
            if (selectedItems != null && selectedItems.length > 0) {
                // Concatenate all selected items' string representations
                final StringBuilder sb = new StringBuilder();
                for (Object item : selectedItems) {
                    if (item != null) {
                        sb.append(item.toString()).append(" ");
                    }
                }
                final String result = sb.toString().trim();
                if (!result.isEmpty()) {
                    return result;
                }
            }
        } catch (Exception ignored) {
        }

        // Approach 2: Try to get context component (JTree) and extract text from tree nodes
        // This works for IntelliJ 2025.2 and older versions
        try {
            final java.awt.Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
            if (component instanceof javax.swing.JTree) {
                final javax.swing.JTree tree = (javax.swing.JTree) component;
                final javax.swing.tree.TreePath[] selectionPaths = tree.getSelectionPaths();
                if (selectionPaths != null && selectionPaths.length > 0) {
                    final StringBuilder sb = new StringBuilder();
                    for (javax.swing.tree.TreePath path : selectionPaths) {
                        final Object lastComponent = path.getLastPathComponent();
                        if (lastComponent != null) {
                            final String text = extractTextFromTreeNode(lastComponent, tree);
                            if (text != null && !text.isEmpty()) {
                                sb.append(text).append(" ");
                            }
                        }
                    }
                    final String result = sb.toString().trim();
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Extracts text from a tree node using multiple strategies.
     *
     * @param node the tree node
     * @param tree the JTree containing the node
     * @return the extracted text, or null if not found
     */
    @Nullable
    private static String extractTextFromTreeNode(@NotNull Object node, @NotNull javax.swing.JTree tree) {
        // Strategy 1: Try to get text via reflection from common methods
        try {
            // Try getText() method (common in many tree node implementations)
            java.lang.reflect.Method getTextMethod = node.getClass().getMethod("getText");
            Object result = getTextMethod.invoke(node);
            if (result instanceof String && !((String) result).isEmpty()) {
                return (String) result;
            }
        } catch (Exception ignored) {
        }

        // Strategy 2: Try getPresentation().getText() (IntelliJ tree nodes often have this)
        try {
            java.lang.reflect.Method getPresentationMethod = node.getClass().getMethod("getPresentation");
            Object presentation = getPresentationMethod.invoke(node);
            if (presentation != null) {
                java.lang.reflect.Method presentationGetTextMethod = presentation.getClass().getMethod("getPresentableText");
                Object result = presentationGetTextMethod.invoke(presentation);
                if (result instanceof String && !((String) result).isEmpty()) {
                    return (String) result;
                }
            }
        } catch (Exception ignored) {
        }

        // Strategy 3: Try getName() method
        try {
            java.lang.reflect.Method getNameMethod = node.getClass().getMethod("getName");
            Object result = getNameMethod.invoke(node);
            if (result instanceof String && !((String) result).isEmpty()) {
                return (String) result;
            }
        } catch (Exception ignored) {
        }

        // Strategy 4: Try to get the cell renderer component and extract text from JLabel
        try {
            javax.swing.tree.TreeCellRenderer renderer = tree.getCellRenderer();
            if (renderer != null) {
                java.awt.Component rendererComponent = renderer.getTreeCellRendererComponent(
                        tree, node, true, false, true, 0, true);
                if (rendererComponent instanceof javax.swing.JLabel) {
                    String text = ((javax.swing.JLabel) rendererComponent).getText();
                    if (text != null && !text.isEmpty()) {
                        // Strip HTML tags if present
                        return text.replaceAll("<[^>]*>", "").trim();
                    }
                }
                // Try to find JLabel in component hierarchy
                String labelText = findLabelTextInComponent(rendererComponent);
                if (labelText != null && !labelText.isEmpty()) {
                    return labelText;
                }
            }
        } catch (Exception ignored) {
        }

        // Strategy 5: Try getUserObject() for DefaultMutableTreeNode
        try {
            java.lang.reflect.Method getUserObjectMethod = node.getClass().getMethod("getUserObject");
            Object userObject = getUserObjectMethod.invoke(node);
            if (userObject != null && userObject != node) {
                // Recursively try to extract text from userObject
                String text = extractTextFromObject(userObject);
                if (text != null && !text.isEmpty()) {
                    return text;
                }
            }
        } catch (Exception ignored) {
        }

        // Strategy 6: Fallback to toString() but filter out class names with @ (like OpenFileDescriptor@xxx)
        String nodeString = node.toString();
        if (nodeString != null && !nodeString.isEmpty() && !nodeString.contains("@")) {
            return nodeString;
        }

        return null;
    }

    /**
     * Recursively finds JLabel text in a component hierarchy.
     *
     * @param component the root component to search
     * @return the label text, or null if not found
     */
    @Nullable
    private static String findLabelTextInComponent(java.awt.Component component) {
        if (component instanceof javax.swing.JLabel) {
            String text = ((javax.swing.JLabel) component).getText();
            if (text != null && !text.isEmpty()) {
                return text.replaceAll("<[^>]*>", "").trim();
            }
        }
        if (component instanceof java.awt.Container) {
            for (java.awt.Component child : ((java.awt.Container) component).getComponents()) {
                String text = findLabelTextInComponent(child);
                if (text != null && !text.isEmpty()) {
                    return text;
                }
            }
        }
        return null;
    }

    /**
     * Extracts text from an arbitrary object using reflection.
     *
     * @param obj the object to extract text from
     * @return the extracted text, or null if not found
     */
    @Nullable
    private static String extractTextFromObject(@NotNull Object obj) {
        // Try common text getter methods
        String[] methodNames = {"getText", "getPresentableText", "getName", "getDescription", "getMessage"};
        for (String methodName : methodNames) {
            try {
                java.lang.reflect.Method method = obj.getClass().getMethod(methodName);
                Object result = method.invoke(obj);
                if (result instanceof String && !((String) result).isEmpty()) {
                    return (String) result;
                }
            } catch (Exception ignored) {
            }
        }
        // Fallback to toString() if it doesn't look like an object reference
        String str = obj.toString();
        if (str != null && !str.isEmpty() && !str.contains("@")) {
            return str;
        }
        return null;
    }
}
