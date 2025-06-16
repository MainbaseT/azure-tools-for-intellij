// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.HelperUtils;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.RuleConfigLoader;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection tool to detect the use of getSyncPoller() on a PollerFlux. The inspection will check if the method call is
 * on a PollerFlux and if the method call is on an Azure SDK client. If both conditions are met, the inspection will
 * register a problem with the suggestion to use SyncPoller instead.
 *
 * This is an example of an anti-pattern that would be detected by the inspection tool.
 * public void exampleUsage() {
 *     PollerFlux<String> pollerFlux = createPollerFlux();
 *
 *     // Anti-pattern: Using getSyncPoller() on PollerFlux
 *     SyncPoller<String, Void> syncPoller = pollerFlux.getSyncPoller();
 * }
 */
public class GetSyncPollerOnPollerFluxCheck extends LocalInspectionTool {

    /**
     * Method to build the visitor for the inspection tool.
     *
     * @param holder Holder for the problems found by the inspection
     *
     * @return JavaElementVisitor a visitor to visit the method call expressions
     */
    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new GetSyncPollerOnPollerFluxVisitor(holder, RuleConfigLoader.getInstance().getRuleConfigs());
    }

    /**
     * Visitor class to visit the method call expressions and check for the use of getSyncPoller() on a PollerFlux. The
     * visitor will check if the method call is on a PollerFlux and if the method call is on an Azure SDK client.
     */
    class GetSyncPollerOnPollerFluxVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private static RuleConfig RULE_CONFIG;
        private static boolean SKIP_WHOLE_RULE;

        /**
         * Constructor to initialize the visitor with the holder and isOnTheFly flag.
         *
         * @param holder Holder for the problems found by the inspection
         * @param ruleConfigs Rule configurations for the inspection
         */
        public GetSyncPollerOnPollerFluxVisitor(ProblemsHolder holder, Map<String, RuleConfig> ruleConfigs) {
            this.holder = holder;
            initializeRuleConfig(ruleConfigs);
        }

        private void initializeRuleConfig(Map<String, RuleConfig> ruleConfigs) {
            if (RULE_CONFIG == null) {
                final String ruleName = "GetSyncPollerOnPollerFluxCheck";
                RULE_CONFIG = ruleConfigs.get(ruleName);
                SKIP_WHOLE_RULE = RULE_CONFIG.isSkipRuleCheck();
            }
        }

        /**
         * Method to visit the method call expressions and check for the use of getSyncPoller() on a PollerFlux.
         *
         * @param expression Method call expression to visit
         */
        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            // Check if the whole rule should be skipped
            if (SKIP_WHOLE_RULE) {
                return;
            }

            PsiMethod method = expression.resolveMethod();
            if (method != null) {
                PsiClass containingClass = method.getContainingClass();
                if (containingClass != null) {
                    String qualifiedName = containingClass.getQualifiedName();
                    if (qualifiedName != null && HelperUtils.isAzurePackage(qualifiedName)) {
                        if (isGetSyncPollerCall(expression)) {
                            holder.registerProblem(expression.getMethodExpression().getReferenceNameElement(),
                                RULE_CONFIG.getAntiPatternMessage());
                        }
                    }
                }
            }
        }

        /**
         * Helper method to check if the method call is on a PollerFlux type.
         *
         * @param expression Method call expression to check
         *
         * @return true if the method call is a getSyncPoller() call, false otherwise
         */
        private boolean isGetSyncPollerCall(@NotNull PsiMethodCallExpression expression) {
            for (String usage : RULE_CONFIG.getUsagesToCheck()) {
                if (expression.getMethodExpression().getReferenceName().startsWith(usage)) {
                    return true;
                }
            }
            return false;
        }
    }
}
