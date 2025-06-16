// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.HelperUtils;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.RuleConfigLoader;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Inspection tool to check for the use of blocking method calls on async clients in Azure SDK.
 */
public class UseOfBlockOnAsyncClientsCheck extends LocalInspectionTool {

    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new UseOfBlockOnAsyncClientsVisitor(holder, RuleConfigLoader.getInstance().getRuleConfigs());
    }

    /**
     * Visitor to check for the use of blocking methods on async clients in Azure SDK.
     */
    static class UseOfBlockOnAsyncClientsVisitor extends JavaElementVisitor {
        private final ProblemsHolder holder;
        private static RuleConfig RULE_CONFIG;
        private static boolean SKIP_WHOLE_RULE;

        UseOfBlockOnAsyncClientsVisitor(ProblemsHolder holder, Map<String, RuleConfig> ruleConfigs) {
            this.holder = holder;
            initializeRuleConfig(ruleConfigs);
        }

        private void initializeRuleConfig(Map<String, RuleConfig> ruleConfigs) {
            if (RULE_CONFIG == null) {
                final String ruleName = "UseOfBlockOnAsyncClientsCheck";
                RULE_CONFIG = ruleConfigs.get(ruleName);
                SKIP_WHOLE_RULE = RULE_CONFIG.isSkipRuleCheck();
            }
        }

        /**
         * This method is used to visit the method call expression and check for blocking calls on async clients.
         *
         * @param expression PsiMethodCallExpression - the method call expression to visit
         */
        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            if (SKIP_WHOLE_RULE) {
                return;
            }

            if (!isBlockingMethodCall(expression)) {
                return;
            }

            if (isAsyncClientBlockingCall(expression)) {
                PsiElement problemElement = expression.getMethodExpression().getReferenceNameElement();
                holder.registerProblem(problemElement, RULE_CONFIG.getAntiPatternMessage());
            }
        }

        /**
         * Checks if the method call is a blocking call on an async client.
         *
         * @param expression PsiMethodCallExpression - the method call expression
         *
         * @return true if it's a blocking call on an async client, false otherwise
         */
        private boolean isAsyncClientBlockingCall(@NotNull PsiMethodCallExpression expression) {
            PsiExpression qualifierExpression = expression.getMethodExpression().getQualifierExpression();
            if (qualifierExpression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression qualifierMethodCall = (PsiMethodCallExpression) qualifierExpression;
                PsiType qualifierReturnType = qualifierMethodCall.getType();

                if (qualifierReturnType instanceof PsiClassType) {
                    PsiClass qualifierReturnTypeClass = ((PsiClassType) qualifierReturnType).resolve();
                    if (qualifierReturnTypeClass != null && isReactiveType(qualifierReturnTypeClass)) {
                        return isAzureAsyncClient(qualifierMethodCall);
                    }
                }
            }
            return false;
        }

        /**
         * Checks if the method call is a blocking method call on a reactive type.
         *
         * @param expression PsiMethodCallExpression - the method call expression
         *
         * @return true if it's a blocking method call on a reactive type, false otherwise
         */
        private boolean isBlockingMethodCall(@NotNull PsiMethodCallExpression expression) {
            return RULE_CONFIG.getUsagesToCheck().stream().anyMatch(expression.getMethodExpression().getReferenceName()::equals);
        }

        /**
         * Checks if the class is an async client in Azure SDK.
         *
         * @param qualifierMethodCall PsiMethodCallExpression - the method call expression
         *
         * @return true if the class is an async client in Azure SDK, false otherwise
         */
        private boolean isAzureAsyncClient(PsiMethodCallExpression qualifierMethodCall) {
            PsiExpression clientExpression = getClientExpression(qualifierMethodCall);
            if (clientExpression instanceof PsiReferenceExpression) {
                PsiType clientType = clientExpression.getType();
                if (clientType instanceof PsiClassType) {
                    PsiClass clientClass = ((PsiClassType) clientType).resolve();
                    String qualifiedName = clientClass.getQualifiedName();
                    return qualifiedName != null && HelperUtils.isAzurePackage(clientClass.getQualifiedName()) &&
                        clientClass.getQualifiedName().endsWith("AsyncClient");
                }
            }
            return false;
        }

        /**
         * Extracts the client expression by traveling up the method call chain.
         *
         * @param methodCall PsiMethodCallExpression - the method call expression
         *
         * @return the client expression at the end of the chain
         */
        private PsiExpression getClientExpression(PsiMethodCallExpression methodCall) {
            PsiExpression clientExpression = methodCall.getMethodExpression().getQualifierExpression();
            while (clientExpression instanceof PsiMethodCallExpression) {
                clientExpression =
                    ((PsiMethodCallExpression) clientExpression).getMethodExpression().getQualifierExpression();
            }
            return clientExpression;
        }

        /**
         * Checks if the class is a reactive type (Mono, Flux, etc.).
         *
         * @param psiClass PsiClass - the class to check
         *
         * @return true if the class is reactive, false otherwise
         */
        private boolean isReactiveType(PsiClass psiClass) {
            return RULE_CONFIG.getScopeToCheck().contains(psiClass.getQualifiedName());
        }
    }
}
