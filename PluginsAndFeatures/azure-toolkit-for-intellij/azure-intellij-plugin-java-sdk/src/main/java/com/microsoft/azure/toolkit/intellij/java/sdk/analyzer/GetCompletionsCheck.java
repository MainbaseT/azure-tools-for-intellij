// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.HelperUtils;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.RuleConfigLoader;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection tool to check discouraged GetCompletions API usage in openai package context.
 */
public class GetCompletionsCheck extends LocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new GetCompletionsCheck.GetCompletionsVisitor(holder, RuleConfigLoader.getInstance().getRuleConfigs());
    }

    static class GetCompletionsVisitor extends JavaElementVisitor {
        private final ProblemsHolder holder;
        private static RuleConfig RULE_CONFIG;
        private static boolean SKIP_WHOLE_RULE;

        GetCompletionsVisitor(ProblemsHolder holder, Map<String, RuleConfig> ruleConfigs) {
            this.holder = holder;
            initializeRuleConfig(ruleConfigs);
        }

        private void initializeRuleConfig(Map<String, RuleConfig> ruleConfigs) {
            if (RULE_CONFIG == null) {
                final String ruleName = "GetCompletionsCheck";
                RULE_CONFIG = ruleConfigs.get(ruleName);
                SKIP_WHOLE_RULE = RULE_CONFIG.isSkipRuleCheck();
            }
        }

        // visit all methodcalls
        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);
            // Check if the rule should be skipped
            if (SKIP_WHOLE_RULE) {
                return;
            }
            PsiMethod method = HelperUtils.getResolvedMethod(expression);
            if (HelperUtils.isItDiscouragedAPI(method, RULE_CONFIG.getUsagesToCheck(),
                RULE_CONFIG.getScopeToCheck())) {

                PsiElement problemElement = expression.getMethodExpression().getReferenceNameElement();
                if (problemElement != null) {
                    holder.registerProblem(problemElement, RULE_CONFIG.getAntiPatternMessage());
                }
            }
        }
    }
}