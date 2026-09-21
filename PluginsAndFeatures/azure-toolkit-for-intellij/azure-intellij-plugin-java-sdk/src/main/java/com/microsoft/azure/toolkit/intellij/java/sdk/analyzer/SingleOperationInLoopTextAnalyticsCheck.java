// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiBlockStatement;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiDoWhileStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiForStatement;
import com.intellij.psi.PsiForeachStatement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.PsiWhileStatement;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.RuleConfigLoader;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection to check if there is a Text Analytics client operation inside a loop. If a Text Analytics client operation
 * is found inside a loop, and the API has a batch alternative, a problem will be registered.
 * <p>
 * This is an example of a situation where the inspection should register a problem:
 * <p>
 * // Loop through the list of texts and detect the language for each text 1. for (String text : texts) {
 * DetectedLanguage detectedLanguage = textAnalyticsClient.detectLanguage(text); System.out.println("Text: " + text + "
 * | Detected Language: " + detectedLanguage.getName() + " | Confidence Score: " +
 * detectedLanguage.getConfidenceScore()); }
 * <p>
 * // Traditional for-loop to recognize entities for each text for (int i = 0; i < texts.size(); i++) { String text =
 * texts.get(i); textAnalyticsClient.recognizeEntities(text); // Process recognized entities if needed }
 */
public class SingleOperationInLoopTextAnalyticsCheck extends LocalInspectionTool {

    /**
     * Build the visitor for the inspection. This visitor will be used to traverse the PSI tree.
     *
     * @param holder The holder for the problems found
     *
     * @return The visitor for the inspection
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new SingleOperationInLoopVisitor(holder, RuleConfigLoader.getInstance().getRuleConfigs());
    }

    /**
     * Visitor class to traverse the PSI tree and check for single Azure client operation inside a loop. The visitor
     * will check for loops of type for, foreach, while, and do-while. The visitor will check for a Text Analytics
     * client operation inside the loop. If a Text Analytics client operation is found inside the loop, and the API has
     * a batch alternative, a problem will be registered.
     */
    static class SingleOperationInLoopVisitor extends JavaElementVisitor {
        private final ProblemsHolder holder;
        private static RuleConfig RULE_CONFIG;
        private static boolean SKIP_WHOLE_RULE;

        public SingleOperationInLoopVisitor(ProblemsHolder holder, Map<String, RuleConfig> ruleConfigs) {
            this.holder = holder;
            initializeRuleConfig(ruleConfigs);
        }

        private void initializeRuleConfig(Map<String, RuleConfig> ruleConfigs) {
            if (RULE_CONFIG == null) {
                final String ruleName = "SingleOperationInLoopTextAnalyticsCheck";
                RULE_CONFIG = ruleConfigs.getOrDefault(ruleName, RuleConfig.EMPTY_CONFIG);
                SKIP_WHOLE_RULE = RULE_CONFIG.isSkipRuleCheck();
            }
        }

        @Override
        public void visitForStatement(@NotNull PsiForStatement statement) {
            if (SKIP_WHOLE_RULE) return;
            checkLoopStatement(statement);
        }

        @Override
        public void visitForeachStatement(@NotNull PsiForeachStatement statement) {
            if (SKIP_WHOLE_RULE) return;
            checkLoopStatement(statement);
        }

        @Override
        public void visitWhileStatement(@NotNull PsiWhileStatement statement) {
            if (SKIP_WHOLE_RULE) return;
            checkLoopStatement(statement);
        }

        @Override
        public void visitDoWhileStatement(@NotNull PsiDoWhileStatement statement) {
            if (SKIP_WHOLE_RULE) return;
            checkLoopStatement(statement);
        }

        private void checkLoopStatement(PsiStatement loopStatement) {
            PsiStatement loopBody = getLoopBody(loopStatement);
            if (!(loopBody instanceof PsiBlockStatement)) return;

            PsiCodeBlock codeBlock = ((PsiBlockStatement) loopBody).getCodeBlock();
            for (PsiStatement statement : codeBlock.getStatements()) {
                if (statement instanceof PsiExpressionStatement) {
                    checkExpressionStatement((PsiExpressionStatement) statement);
                } else if (statement instanceof PsiDeclarationStatement) {
                    checkDeclarationStatement((PsiDeclarationStatement) statement);
                }
            }
        }

        private void checkExpressionStatement(PsiExpressionStatement statement) {
            PsiExpression expression = statement.getExpression();
            if (expression instanceof PsiMethodCallExpression) {
                checkMethodCall((PsiMethodCallExpression) expression);
            }
        }

        private void checkDeclarationStatement(PsiDeclarationStatement statement) {
            for (PsiElement element : statement.getDeclaredElements()) {
                if (element instanceof PsiVariable) {
                    PsiExpression initializer = ((PsiVariable) element).getInitializer();
                    if (initializer instanceof PsiMethodCallExpression) {
                        checkMethodCall((PsiMethodCallExpression) initializer);
                    }
                }
            }
        }

        private void checkMethodCall(PsiMethodCallExpression methodCall) {
            if (isAzureTextAnalyticsClientOperation(methodCall)) {
                String methodName = methodCall.getMethodExpression().getReferenceName();
                holder.registerProblem(
                    methodCall,
                    RULE_CONFIG.getAntiPatternMessage() + " Consider using " + methodName + "Batch instead."
                );
            }
        }

        private static PsiStatement getLoopBody(PsiStatement loopStatement) {
            if (loopStatement instanceof PsiForStatement) {
                return ((PsiForStatement) loopStatement).getBody();
            }
            if (loopStatement instanceof PsiForeachStatement) {
                return ((PsiForeachStatement) loopStatement).getBody();
            }
            if (loopStatement instanceof PsiWhileStatement) {
                return ((PsiWhileStatement) loopStatement).getBody();
            }
            if (loopStatement instanceof PsiDoWhileStatement) {
                return ((PsiDoWhileStatement) loopStatement).getBody();
            }
            return null;
        }

        private static boolean isAzureTextAnalyticsClientOperation(PsiMethodCallExpression methodCall) {
            // Check the qualifier expression (e.g., the object or variable the method is called on)
            PsiExpression qualifierExpression = methodCall.getMethodExpression().getQualifierExpression();

            if (qualifierExpression != null) {
                PsiType qualifierType = qualifierExpression.getType();
                if (qualifierType != null) {
                    String qualifiedTypeName = qualifierType.getCanonicalText();
                    if (qualifiedTypeName != null && RULE_CONFIG.getScopeToCheck().stream().anyMatch(qualifiedTypeName::startsWith)) {
                        String methodName = methodCall.getMethodExpression().getReferenceName();
                        return RULE_CONFIG.getUsagesToCheck().contains(methodName + "Batch");
                    }
                }
            }
            return false;
        }
    }
}
