// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.HelperUtils;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.RuleConfigLoader;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * This class extends the LocalInspectionTool and is used to inspect the usage of Azure SDK ServiceBusReceiver &
 * ServiceBusProcessor clients in the code. It checks if the auto-complete feature is disabled for the clients. If the
 * auto-complete feature is not disabled, a problem is registered with the ProblemsHolder.
 */
public class DisableAutoCompleteCheck extends LocalInspectionTool {

    /**
     * Build the visitor for the inspection. This visitor will be used to traverse the PSI tree.
     *
     * @param holder The holder for the problems found
     *
     * @return The visitor for the inspection. This is not used anywhere else in the code.
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new DisableAutoCompleteVisitor(holder, RuleConfigLoader.getInstance().getRuleConfigs());
    }
    
    /**
     * This class extends the JavaElementVisitor and is used to visit the Java elements in the code. It checks for the
     * usage of Azure SDK ServiceBusReceiver & ServiceBusProcessor clients and whether the auto-complete feature is
     * disabled. If the auto-complete feature is not disabled, a problem is registered with the ProblemsHolder.
     */
    static class DisableAutoCompleteVisitor extends JavaElementVisitor {

        private static RuleConfig RULE_CONFIG;
        private static boolean SKIP_WHOLE_RULE;
        private final ProblemsHolder holder;

    DisableAutoCompleteVisitor(ProblemsHolder holder, Map<String, RuleConfig> ruleConfigs) {
        this.holder = holder;
        initializeRuleConfig(ruleConfigs);
    }

    private void initializeRuleConfig(Map<String, RuleConfig> ruleConfigs) {
        if (RULE_CONFIG == null) {
            final String ruleName = "DisableAutoCompleteCheck";
            RULE_CONFIG = ruleConfigs.get(ruleName);
            SKIP_WHOLE_RULE = RULE_CONFIG.isSkipRuleCheck();
        }
    }
        /**
         * This method is used to visit the declaration statements in the code. It checks for the declaration of Azure
         * SDK ServiceBusReceiver & ServiceBusProcessor clients and whether the auto-complete feature is disabled. If
         * the auto-complete feature is not disabled, a problem is registered with the ProblemsHolder.
         *
         * @param statement The declaration statement to visit
         */
        @Override
        public void visitDeclarationStatement(PsiDeclarationStatement statement) {

            if (SKIP_WHOLE_RULE) {
                return;
            }
            super.visitDeclarationStatement(statement);

            // Get the declared elements
            PsiElement[] elements = statement.getDeclaredElements();

            // Get the variable declaration
            if (elements.length > 0 && elements[0] instanceof PsiVariable) {
                PsiVariable variable = (PsiVariable) elements[0];

                // Process the variable declaration
                processVariableDeclaration(variable);
            }
        }

        /**
         * This method is used to process the variable declaration. It checks for the declaration of Azure SDK
         * ServiceBusReceiver & ServiceBusProcessor clients and whether the auto-complete feature is disabled. If the
         * auto-complete feature is not disabled, a problem is registered with the ProblemsHolder.
         *
         * @param variable The variable to process
         */
        private void processVariableDeclaration(PsiVariable variable) {

            // Retrieve the client name (left side of the declaration)
            PsiType clientType = variable.getType();

            // Check the assignment part (right side)
            PsiExpression initializer = variable.getInitializer();

            // Check if the client type is an Azure SDK client
            if (HelperUtils.isAzurePackage(clientType.getCanonicalText())) {
                if (HelperUtils.checkIfInScope(RULE_CONFIG.getScopeToCheck(), clientType.getPresentableText())) {
                    if (initializer instanceof PsiMethodCallExpression) {
                        if (!isAutoCompleteDisabled((PsiMethodCallExpression) initializer)) {
                            holder.registerProblem(initializer, RULE_CONFIG.getAntiPatternMessage());
                        }
                    }
                }
            }
        }

        /**
         * This method is used to check if the auto-complete feature is disabled. It iterates up the chain of method
         * calls to check if the auto-complete feature is disabled.
         *
         * @param methodCallExpression The method call expression to check
         *
         * @return true if the auto-complete feature is disabled, false otherwise
         */
        private static boolean isAutoCompleteDisabled(PsiMethodCallExpression methodCallExpression) {

            // Iterating up the chain of method calls
            PsiExpression qualifier = methodCallExpression.getMethodExpression().getQualifierExpression();

            // Check if the method call chain has the method to check
            while (qualifier instanceof PsiMethodCallExpression) {

                if (qualifier instanceof PsiMethodCallExpression) {

                    // Get the method expression
                    PsiReferenceExpression methodExpression =
                        ((PsiMethodCallExpression) qualifier).getMethodExpression();

                    // Get the method name
                    String methodName = methodExpression.getReferenceName();

                    // Check if the method name is the method to check
                    if (RULE_CONFIG.getUsagesToCheck().contains(methodName)) {
                        return true;
                    }
                }
                qualifier = ((PsiMethodCallExpression) qualifier).getMethodExpression().getQualifierExpression();
            }
            // When the chain has been traversed and the method to check is not found
            return false;
        }
    }
}
