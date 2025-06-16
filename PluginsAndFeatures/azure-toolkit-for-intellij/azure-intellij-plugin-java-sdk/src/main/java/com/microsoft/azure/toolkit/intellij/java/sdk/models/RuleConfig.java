// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.models;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/**
 * This class contains configuration options for code style rules. It contains the methods to check, the client name, and
 * the antipattern message.
 */
@Getter
public class RuleConfig {
    private final List<String> usagesToCheck;
    private final List<String> scopeToCheck;
    private final String antiPatternMessage;
    private final Map<String, String> regexPatternsToCheck;
    private final boolean skipRuleCheck;

    /**
     * Constructor for RuleConfig.
     *
     * @param usagesToCheck List of methods to check. Defaults to "all" if null or empty.
     * @param scopeToCheck List of clients to check. Defaults to "all" if null or empty.
     * @param antiPatternMessage Antipattern messages to display.
     * @param regexPatternsToCheck Map of regex patterns to check.
     * @param skipRuleCheck Whether to skip the rule check.
     */
    public RuleConfig(List<String> usagesToCheck, List<String> scopeToCheck, String antiPatternMessage,
        Map<String, String> regexPatternsToCheck, boolean skipRuleCheck) {
        this.usagesToCheck = usagesToCheck == null || usagesToCheck.isEmpty()
            ? Collections.emptyList()
            : Collections.unmodifiableList(usagesToCheck);
        this.scopeToCheck = scopeToCheck == null || scopeToCheck.isEmpty()
            ? Collections.emptyList()
            : Collections.unmodifiableList(scopeToCheck);
        this.antiPatternMessage = antiPatternMessage;
        this.regexPatternsToCheck = regexPatternsToCheck == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(regexPatternsToCheck);
        this.skipRuleCheck = skipRuleCheck;
    }
}
