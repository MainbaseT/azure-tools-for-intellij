/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for parsing and extracting information from pom.xml files.
 */
public final class PomXmlUtils {

    private static final int MAX_SEARCH_OFFSET = 500;

    private PomXmlUtils() {
        // Utility class, no instantiation
    }

    /**
     * Finds the start of the dependency block containing the given offset.
     *
     * @param text   the full text of the pom.xml file
     * @param offset the current cursor offset
     * @return the start index of the dependency block, or -1 if not found
     */
    public static int findDependencyStart(@NotNull String text, int offset) {
        // Look for <dependency> tag before the offset
        int searchStart = Math.max(0, offset - MAX_SEARCH_OFFSET);
        String searchArea = text.substring(searchStart, offset);
        int lastDependency = searchArea.lastIndexOf("<dependency>");
        if (lastDependency >= 0) {
            return searchStart + lastDependency;
        }
        return -1;
    }

    /**
     * Finds the end of the dependency block containing the given offset.
     *
     * @param text   the full text of the pom.xml file
     * @param offset the current cursor offset
     * @return the end index of the dependency block (after closing tag), or -1 if not found
     */
    public static int findDependencyEnd(@NotNull String text, int offset) {
        // Look for </dependency> tag after the offset
        int searchEnd = Math.min(text.length(), offset + MAX_SEARCH_OFFSET);
        String searchArea = text.substring(offset, searchEnd);
        int endDependency = searchArea.indexOf("</dependency>");
        if (endDependency >= 0) {
            return offset + endDependency + "</dependency>".length();
        }
        return -1;
    }

    /**
     * Extracts a value from an XML tag.
     *
     * @param xml     the XML string to search in
     * @param tagName the name of the tag to extract the value from
     * @return the value inside the tag, or null if not found
     */
    @Nullable
    public static String extractXmlValue(@NotNull String xml, @NotNull String tagName) {
        final String startTag = "<" + tagName + ">";
        final String endTag = "</" + tagName + ">";

        int start = xml.indexOf(startTag);
        if (start < 0) {
            return null;
        }
        start += startTag.length();

        int end = xml.indexOf(endTag, start);
        if (end < 0) {
            return null;
        }

        return xml.substring(start, end).trim();
    }
}
