package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiTypeElement;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.HelperUtils;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.RuleConfigLoader;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * This class is used to check if the EventHubConsumerAsyncClient is being used in the code.
 */
public class EventHubConsumerAsyncClientCheck extends LocalInspectionTool {
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
        return new EventHubConsumerAsyncClientCheck.EventHubConsumerAsyncClientVisitor(holder,
            RuleConfigLoader.getInstance().getRuleConfigs());
    }

    /**
     * Visitor class to visit the method call expressions and check for the use of getSyncPoller() on a PollerFlux. The
     * visitor will check if the method call is on a PollerFlux and if the method call is on an Azure SDK client.
     */
    static class EventHubConsumerAsyncClientVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private static RuleConfig RULE_CONFIG;
        private static boolean SKIP_WHOLE_RULE;

        /**
         * Constructor to initialize the visitor with the holder and isOnTheFly flag.
         *
         * @param holder Holder for the problems found by the inspection
         * @param ruleConfigs Rule configurations for the inspection
         */
        public EventHubConsumerAsyncClientVisitor(ProblemsHolder holder, Map<String, RuleConfig> ruleConfigs) {
            this.holder = holder;
            initializeRuleConfig(ruleConfigs);
        }

        private void initializeRuleConfig(Map<String, RuleConfig> ruleConfigs) {
            if (RULE_CONFIG == null) {
                final String ruleName = "EventHubConsumerAsyncClientCheck";
                RULE_CONFIG = ruleConfigs.getOrDefault(ruleName, RuleConfig.EMPTY_CONFIG);
                SKIP_WHOLE_RULE = RULE_CONFIG.isSkipRuleCheck();
            }
        }

        @Override
        public void visitTypeElement(PsiTypeElement element) {
            super.visitTypeElement(element);
            if (SKIP_WHOLE_RULE) {
                return;
            }
            if (HelperUtils.isItDiscouragedClient(element, RULE_CONFIG.getUsagesToCheck())) {
                holder.registerProblem(element, RULE_CONFIG.getAntiPatternMessage());
            }
        }

    }
}