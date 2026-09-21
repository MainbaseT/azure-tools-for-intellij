// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiBlockStatement;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiForStatement;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiStatement;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.HelperUtils;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.RuleConfigLoader;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection to detect dynamic client creation in loops.
 */
public class DynamicClientCreationCheck extends LocalInspectionTool {

    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new DynamicClientCreationVisitor(holder, RuleConfigLoader.getInstance().getRuleConfigs());
    }

    static class DynamicClientCreationVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private static RuleConfig RULE_CONFIG;
        private static boolean SKIP_WHOLE_RULE;

        public DynamicClientCreationVisitor(ProblemsHolder holder, Map<String, RuleConfig> ruleConfigs) {
            this.holder = holder;
            initializeRuleConfig(ruleConfigs);
        }

        private void initializeRuleConfig(Map<String, RuleConfig> ruleConfigs) {
            if (RULE_CONFIG == null) {
                final String ruleName = "DynamicClientCreationCheck";
                RULE_CONFIG = ruleConfigs.getOrDefault(ruleName, RuleConfig.EMPTY_CONFIG);
                SKIP_WHOLE_RULE = RULE_CONFIG.isSkipRuleCheck();
            }
        }

        @Override
        public void visitForStatement(@NotNull PsiForStatement statement) {
            if (SKIP_WHOLE_RULE) return;

            PsiStatement body = statement.getBody();
            if (!(body instanceof PsiBlockStatement)) return;

            PsiCodeBlock codeBlock = ((PsiBlockStatement) body).getCodeBlock();
            for (PsiStatement blockChild : codeBlock.getStatements()) {
                if (blockChild instanceof PsiExpressionStatement) {
                    checkExpression(((PsiExpressionStatement) blockChild).getExpression());
                } else if (blockChild instanceof PsiDeclarationStatement) {
                    checkDeclaration((PsiDeclarationStatement) blockChild);
                }
            }
        }

        private void checkExpression(PsiExpression expression) {
            if (expression instanceof PsiAssignmentExpression) {
                PsiExpression rhs = ((PsiAssignmentExpression) expression).getRExpression();
                if (rhs instanceof PsiMethodCallExpression && isClientCreationMethod((PsiMethodCallExpression) rhs)) {
                    holder.registerProblem(rhs, RULE_CONFIG.getAntiPatternMessage());
                }
            }
        }

        private void checkDeclaration(PsiDeclarationStatement declaration) {
            for (PsiElement declaredElement : declaration.getDeclaredElements()) {
                if (declaredElement instanceof PsiLocalVariable) {
                    PsiExpression initializer = ((PsiLocalVariable) declaredElement).getInitializer();
                    if (initializer instanceof PsiMethodCallExpression && isClientCreationMethod((PsiMethodCallExpression) initializer)) {
                        holder.registerProblem(initializer, RULE_CONFIG.getAntiPatternMessage());
                    }
                }
            }
        }

        private boolean isClientCreationMethod(PsiMethodCallExpression methodCallExpression) {
            PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();
            String methodName = methodExpression.getReferenceName();

            if (methodName == null || !RULE_CONFIG.getUsagesToCheck().contains(methodName)) {
                return false;
            }

            PsiExpression qualifier = methodExpression.getQualifierExpression();
            if (qualifier == null || qualifier.getType() == null) {
                return false;
            }

            return HelperUtils.isAzurePackage(qualifier.getType().getCanonicalText());
        }
    }
}
