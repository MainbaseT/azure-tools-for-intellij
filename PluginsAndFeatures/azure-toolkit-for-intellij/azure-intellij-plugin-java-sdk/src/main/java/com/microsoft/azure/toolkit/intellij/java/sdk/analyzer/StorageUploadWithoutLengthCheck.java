// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.RuleConfigLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection tool to enforce usage of Azure Storage upload APIs with a 'length' parameter.
 */
public class StorageUploadWithoutLengthCheck extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new StorageUploadVisitor(holder, RuleConfigLoader.getInstance().getRuleConfigs());
    }

    /**
     * Visitor class to check for Azure Storage upload APIs without a 'length' parameter.
     */
    static class StorageUploadVisitor extends JavaRecursiveElementWalkingVisitor {
        private static final String LENGTH_TYPE = "long";
        private final ProblemsHolder holder;
        private static RuleConfig RULE_CONFIG;
        private static boolean SKIP_WHOLE_RULE;


        StorageUploadVisitor(ProblemsHolder holder, Map<String, RuleConfig> ruleConfigs) {
            this.holder = holder;
            initializeRuleConfig(ruleConfigs);
        }

        private void initializeRuleConfig(Map<String, RuleConfig> ruleConfigs) {
            if (RULE_CONFIG == null) {
                final String ruleName = "StorageUploadWithoutLengthCheck";
                RULE_CONFIG = ruleConfigs.get(ruleName);
                SKIP_WHOLE_RULE = RULE_CONFIG.isSkipRuleCheck();
            }
        }
        @Override
        public void visitMethodCallExpression(PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            if (SKIP_WHOLE_RULE || !shouldAnalyze(expression)) {
                return;
            }

            if (!hasLengthArgument(expression)) {
                holder.registerProblem(expression, RULE_CONFIG.getAntiPatternMessage());
            }
        }

        private boolean shouldAnalyze(PsiMethodCallExpression expression) {
            String methodName = expression.getMethodExpression().getReferenceName();
            return RULE_CONFIG.getUsagesToCheck().contains(methodName) && isInScope(expression);
        }

        private boolean isInScope(PsiMethodCallExpression expression) {
            PsiExpression qualifier = expression.getMethodExpression().getQualifierExpression();
            if (!(qualifier != null && qualifier.getType() instanceof PsiClassType)) {
                return false;
            }

            PsiClass containingClass = ((PsiClassType) qualifier.getType()).resolve();
            if (containingClass == null || containingClass.getQualifiedName() == null) {
                return false;
            }

            return RULE_CONFIG.getScopeToCheck().stream()
                .anyMatch(scope -> containingClass.getQualifiedName().startsWith(scope));
        }

        /**
         * Checks if the given method call expression has a 'length' argument.
         *
         * @param expression the method call expression to check
         *
         * @return true if the method call expression has a 'length' argument, false otherwise
         */
        private boolean hasLengthArgument(PsiMethodCallExpression expression) {
            return Arrays.stream(expression.getArgumentList().getExpressions())
                .anyMatch(arg -> isLongType(arg) || isLengthMethodCall(arg) || isLongTypeInCallChain(arg)
                    || (arg instanceof PsiNewExpression && hasLongConstructorArgument((PsiNewExpression) arg)));
        }

        /**
         * Checks if the given expression is a method call that returns a long value.
         *
         * @param expression the expression to check
         *
         * @return true if the expression is a method call that returns a long value, false otherwise
         */
        private boolean isLongTypeInCallChain(PsiExpression expression) {
            while (expression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression methodCall = (PsiMethodCallExpression) expression;
                PsiExpression qualifier = methodCall.getMethodExpression().getQualifierExpression();

                if (qualifier instanceof PsiReferenceExpression &&
                    resolvesToVariableWithLength((PsiReferenceExpression) qualifier)) {
                    return false;
                }

                if (qualifier instanceof PsiNewExpression &&
                    hasLengthArgumentInConstructor(((PsiNewExpression) qualifier).resolveConstructor())) {
                    return false;
                }

                expression = qualifier;
            }
            return false;
        }

        /**
         * Checks if the given qualifier resolves to a variable with a 'length' field or method.
         *
         * @param qualifier the qualifier to check
         *
         * @return true if the qualifier resolves to a variable with a 'length' field or method, false otherwise
         */
        private boolean resolvesToVariableWithLength(PsiReferenceExpression qualifier) {
            PsiElement resolved = qualifier.resolve();
            if (resolved instanceof PsiVariable) {
                PsiVariable variable = (PsiVariable) resolved;
                return hasLengthFieldOrMethod(variable.getType());
            }
            return false;
        }

        private boolean isLongType(PsiExpression expression) {
            return expression.getType() != null && LENGTH_TYPE.equals(expression.getType().getCanonicalText());
        }

        private boolean hasLongConstructorArgument(PsiNewExpression newExpression) {
            return Arrays.stream(newExpression.getArgumentList().getExpressions())
                .anyMatch(this::isLongType)
                || isLengthRelatedConstructor(newExpression.resolveConstructor());
        }

        private boolean hasLengthArgumentInConstructor(PsiMethod constructor) {
            if (constructor == null) {
                return false;
            }
            return Arrays.stream(constructor.getParameterList().getParameters())
                .anyMatch(param -> LENGTH_TYPE.equals(param.getType().getCanonicalText()));
        }

        private boolean hasLengthFieldOrMethod(PsiType type) {
            if (!(type instanceof PsiClassType)) {
                return false;
            }
            PsiClass resolvedClass = ((PsiClassType) type).resolve();
            if (resolvedClass == null) {
                return false;
            }

            return Arrays.stream(resolvedClass.getAllMethods())
                .anyMatch(method -> "length".equals(method.getName())
                    && method.getParameterList().getParametersCount() == 0
                    && LENGTH_TYPE.equals(method.getReturnType().getCanonicalText()))
                || Arrays.stream(resolvedClass.getAllFields())
                .anyMatch(field -> "length".equals(field.getName())
                    && LENGTH_TYPE.equals(field.getType().getCanonicalText()));
        }

        private boolean isLengthMethodCall(PsiExpression expression) {
            if (!(expression instanceof PsiMethodCallExpression)) {
                return false;
            }

            PsiMethodCallExpression methodCall = (PsiMethodCallExpression) expression;
            String methodName = methodCall.getMethodExpression().getReferenceName();
            if (!"length".equals(methodName)) {
                return false;
            }

            PsiExpression qualifier = methodCall.getMethodExpression().getQualifierExpression();
            if (!(qualifier instanceof PsiReferenceExpression)) {
                return false;
            }

            PsiElement resolved = ((PsiReferenceExpression) qualifier).resolve();
            return resolved instanceof PsiVariable
                && "java.lang.String".equals(((PsiVariable) resolved).getType().getCanonicalText());
        }

        private boolean isLengthRelatedConstructor(PsiMethod constructor) {
            if (constructor == null || constructor.getContainingClass() == null) {
                return false;
            }

            String className = constructor.getContainingClass().getQualifiedName();
            if (className == null || !className.endsWith("UploadOptions")) {
                return false;
            }

            return Arrays.stream(constructor.getParameterList().getParameters())
                .anyMatch(param -> LENGTH_TYPE.equals(param.getType().getCanonicalText()));
        }
    }
}


