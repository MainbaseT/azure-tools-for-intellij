// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.microsoft.azure.toolkit.intellij.java.sdk.analyzer.DisableAutoCompleteCheck.DisableAutoCompleteVisitor;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.RuleConfigLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This class is used to test the DisableAutoCompleteCheck class. It tests the visitDeclarationStatement method of the
 * DisableAutoCompleteVisitor class. Use of AC refers to the auto-complete feature.
 */
public class DisableAutoCompleteCheckTest {

    private static final String SUGGESTION_MESSAGE =
        "Auto-completion enabled by default. Consider using the `disableAutoComplete()` API call to prevent automatic message completion.";
    @Mock
    private ProblemsHolder mockHolder;

    @Mock
    private JavaElementVisitor mockVisitor;

    @Mock
    private PsiDeclarationStatement mockDeclarationStatement;

    @Mock
    private PsiMethodCallExpression initializer;
    @Mock private RuleConfigLoader mockRuleConfigLoader;
    @Mock private RuleConfig mockRuleConfig;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockHolder = mock(ProblemsHolder.class);
        // Set up mock rule config
        when(mockRuleConfig.isSkipRuleCheck()).thenReturn(false);
        when(mockRuleConfig.getUsagesToCheck()).thenReturn(Collections.singletonList("disableAutoComplete"));
        when(mockRuleConfig.getAntiPatternMessage()).thenReturn(SUGGESTION_MESSAGE);
        when(mockRuleConfig.getScopeToCheck()).thenReturn(Arrays.asList( "ServiceBusReceiverClient",
                "ServiceBusReceiverAsyncClient",
                "ServiceBusProcessorClient"));
        mockDeclarationStatement = mock(PsiDeclarationStatement.class);
        Map<String, RuleConfig> mockRules = Map.of("DisableAutoCompleteCheck", mockRuleConfig);
        mockVisitor = new DisableAutoCompleteVisitor(mockHolder, mockRules);
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    public void testDisableAutoCompleteCheck(TestCase testCase) {
        setupMockMethodCall(testCase.methodFound, testCase.numOfInvocations, testCase.packageName, testCase.className);
        mockVisitor.visitDeclarationStatement(mockDeclarationStatement);
        verify(mockHolder, times(testCase.numOfInvocations)).registerProblem(eq(initializer), eq(SUGGESTION_MESSAGE));
    }

    private void setupMockMethodCall(String methodToCheck, int numOfInvocations, String packageName, String className) {
        PsiVariable declaredElement = mock(PsiVariable.class);
        PsiElement[] declaredElements = new PsiElement[] {declaredElement};

        PsiType clientType = mock(PsiType.class);
        initializer = mock(PsiMethodCallExpression.class);

        PsiReferenceExpression expression = mock(PsiReferenceExpression.class);
        PsiMethodCallExpression qualifier = mock(PsiMethodCallExpression.class);
        PsiMethodCallExpression finalExpression = mock(PsiMethodCallExpression.class);

        when(mockDeclarationStatement.getDeclaredElements()).thenReturn(declaredElements);
        when(declaredElement.getType()).thenReturn(clientType);
        when(declaredElement.getInitializer()).thenReturn(initializer);
        when(clientType.getCanonicalText()).thenReturn(packageName);
        when(clientType.getPresentableText()).thenReturn(packageName + className);

        PsiReferenceExpression methodExpression = mock(PsiReferenceExpression.class);
        PsiMethod resolvedMethod = mock(PsiMethod.class);
        PsiClass containingClass = mock(PsiClass.class);

        when(initializer.getMethodExpression()).thenReturn(methodExpression);
        when(methodExpression.resolve()).thenReturn(resolvedMethod);
        when(resolvedMethod.getContainingClass()).thenReturn(containingClass);
        when(resolvedMethod.getName()).thenReturn(methodToCheck);
        when(containingClass.getQualifiedName()).thenReturn(packageName);

        // Ensure the method call chain is properly set up
        when(methodExpression.getQualifierExpression()).thenReturn(qualifier);
        when(qualifier.getMethodExpression()).thenReturn(expression);
        when(expression.getQualifierExpression()).thenReturn(finalExpression);
        when(finalExpression.getMethodExpression()).thenReturn(expression);
        when(expression.getReferenceName()).thenReturn(methodToCheck);

        if (!"disableAutoComplete".equals(methodToCheck)) {
            when(expression.getQualifierExpression()).thenReturn(null);
        }
    }

    private static Stream<TestCase> provideTestCases() {
        return Stream.of(
            new TestCase("com.azure.", "ServiceBusReceiverClient", 1, "notDisableAutoComplete"),
            new TestCase("com.azure.", "ServiceBusProcessorClient", 1, "notDisableAutoComplete"),
            new TestCase("com.azure.", "ServiceBusReceiverAsyncClient", 1, "notDisableAutoComplete"),
            new TestCase("com.azure.", "ServiceBusRuleManagerClient", 0, "notDisableAutoComplete"),
            new TestCase("com.azure.", "ServiceBusReceiverClient", 0, "disableAutoComplete"),
            new TestCase("com.microsoft.azure.", "ServiceBusReceiverClient", 0, "disableAutoComplete")
        );
    }

    private static class TestCase {
        String packageName;
        String className;
        int numOfInvocations;
        String methodFound;

        TestCase(String packageName, String className, int numOfInvocations, String methodFound) {
            this.packageName = packageName;
            this.className = className;
            this.numOfInvocations = numOfInvocations;
            this.methodFound = methodFound;
        }
    }
}