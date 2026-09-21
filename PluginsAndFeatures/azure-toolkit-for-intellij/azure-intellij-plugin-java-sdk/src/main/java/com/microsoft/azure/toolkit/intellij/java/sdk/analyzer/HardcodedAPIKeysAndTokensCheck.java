// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiBinaryExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.HelperUtils;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.RuleConfigLoader;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

/**
 * Custom inspection class to detect hardcoded API keys and tokens in Java code. The inspection tool will flag and
 * suggest to use environment variables or DefaultAzureCredential instead other credential types.
 */
public class HardcodedAPIKeysAndTokensCheck extends LocalInspectionTool {
    private static final Pattern pattern = Pattern.compile("clientId|tenantId|clientSecret|username|password",
        Pattern.CASE_INSENSITIVE);
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new APIKeysAndTokensVisitor(holder, RuleConfigLoader.getInstance().getRuleConfigs());
    }

    /**
     * Visitor to inspect Java elements for hardcoded API keys and tokens.
     */
    static class APIKeysAndTokensVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private static RuleConfig RULE_CONFIG;
        private static boolean SKIP_WHOLE_RULE;

        APIKeysAndTokensVisitor(ProblemsHolder holder, Map<String, RuleConfig> ruleConfigs) {
            this.holder = holder;
            initializeRuleConfig(ruleConfigs);
        }

        private void initializeRuleConfig(Map<String, RuleConfig> ruleConfigs) {
            if (RULE_CONFIG == null) {
                final String ruleName = "HardcodedAPIKeysAndTokensCheck";
                RULE_CONFIG = ruleConfigs.getOrDefault(ruleName, RuleConfig.EMPTY_CONFIG);
                SKIP_WHOLE_RULE = RULE_CONFIG.isSkipRuleCheck();
            }
        }
        @Override
        public void visitNewExpression(@NotNull PsiNewExpression newExpression) {
            PsiJavaCodeReferenceElement classReference = newExpression.getClassReference();
            if (classReference == null) {
                return;
            }

            String qualifiedName = classReference.getQualifiedName();
            String referenceName = classReference.getReferenceName();

            if (HelperUtils.isAzurePackage(qualifiedName) && HelperUtils.checkIfInUsages(RULE_CONFIG.getUsagesToCheck(), referenceName)){
                checkForHardcodedStrings(newExpression);
            }
        }

        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression methodCall) {
            // Check all arguments passed to the method call and if the method call is from the rule config
            PsiType qualifierType = methodCall.getType();
            if (qualifierType != null && RULE_CONFIG.getUsagesToCheck().contains(qualifierType.getCanonicalText())) {
                for (PsiExpression argument : methodCall.getArgumentList().getExpressions()) {
                    if (isProblematicString(argument)) {
                        holder.registerProblem(
                            argument,
                            RULE_CONFIG.getAntiPatternMessage()
                        );
                    }
                }
            }

            // check for chained method calls
            PsiExpression qualifier = methodCall.getMethodExpression().getQualifierExpression();
            if (qualifier != null) {
                if (isProblematicChainedMethodCall(methodCall)) {
                    holder.registerProblem(
                        methodCall,
                        RULE_CONFIG.getAntiPatternMessage()
                    );
                }
            }
        }

        /**
         * Checks for problematic chained method calls.
         */
        private boolean isProblematicChainedMethodCall(@NotNull PsiMethodCallExpression methodCall) {
            PsiExpression qualifier = methodCall.getMethodExpression().getQualifierExpression();
            if (!(qualifier != null && qualifier.getType() instanceof PsiClassType)) {
                return false;
            }

            PsiClass containingClass = ((PsiClassType) qualifier.getType()).resolve();
            if (containingClass == null || containingClass.getQualifiedName() == null) {
                return false;
            }

            if (RULE_CONFIG.getUsagesToCheck().stream()
                .anyMatch(scope -> containingClass.getName().startsWith(scope))) {
                // Check if the method name is "clientId"
                String methodName = methodCall.getMethodExpression().getReferenceName();
                if (checkForPattern(methodCall, methodName)) {
                    return true;
                }
            }
            return false;
        }

        private boolean checkForPattern(@Nonnull PsiMethodCallExpression methodCall, String methodName) {
            if (pattern.matcher(methodName).find()) {
                // Check if the argument to clientId() is a problematic string
                PsiExpression[] arguments = methodCall.getArgumentList().getExpressions();
                if (arguments.length == 1 && isProblematicString(arguments[0])) {
                    return true;
                }
            }
            return false;
        }

        private void checkForHardcodedStrings(@NotNull PsiNewExpression newExpression) {
            for (PsiElement child : newExpression.getChildren()) {
                if (child instanceof PsiExpressionList expression) {
                    for (PsiExpression argument : expression.getExpressions()) {
                        if (isProblematicString(argument)) {
                            holder.registerProblem(newExpression, RULE_CONFIG.getAntiPatternMessage());
                        }
                    }
                }
            }
        }

        /**
         * Recursively checks if an expression is a hardcoded string or derived from one.
         */
        private boolean isProblematicString(@NotNull PsiExpression expression) {
            if (expression instanceof PsiLiteralExpression literal && literal.getValue() instanceof String value) {
                // Direct string literal.
                return isASCII(value);
            } else if (expression instanceof PsiReferenceExpression reference) {
                // Check if the reference points to a string variable initialized with a literal.
                PsiElement resolved = reference.resolve();
                if (resolved instanceof PsiVariable variable) {
                    PsiExpression initializer = variable.getInitializer();
                    return initializer != null && isProblematicString(initializer);
                }
            } else if (expression instanceof PsiBinaryExpression binaryExpression) {
                // Check if concatenated strings involve literals.
                PsiExpression left = binaryExpression.getLOperand();
                PsiExpression right = binaryExpression.getROperand();
                return isProblematicString(left) || (right != null && isProblematicString(right));
            }
            return false;
        }

        private static boolean isASCII(String text) {
            return text.length() == 32 && text.chars().allMatch(ch -> (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f'));
        }
    }
}

