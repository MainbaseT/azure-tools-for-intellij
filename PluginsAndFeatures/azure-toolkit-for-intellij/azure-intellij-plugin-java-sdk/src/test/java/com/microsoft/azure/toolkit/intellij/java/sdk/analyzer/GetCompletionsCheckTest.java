// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
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

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GetCompletionsCheck}.
 */
public class GetCompletionsCheckTest {
    private static final String SUGGESTION_MESSAGE =
        "`getCompletions` API usage detected. Consider using the `getChatCompletions` API instead.";
    @Mock
    private ProblemsHolder mockHolder;

    @Mock
    private JavaElementVisitor mockVisitor;

    @Mock
    private PsiMethodCallExpression methodCallExpression;

    @Mock
    private PsiElement problemElement;
    @Mock private RuleConfigLoader mockRuleConfigLoader;
    @Mock private RuleConfig mockRuleConfig;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockHolder = mock(ProblemsHolder.class);
        methodCallExpression = mock(PsiMethodCallExpression.class);
        // Set up mock rule config
        when(mockRuleConfig.isSkipRuleCheck()).thenReturn(false);
        when(mockRuleConfig.getUsagesToCheck()).thenReturn(Collections.singletonList("getCompletions"));
        when(mockRuleConfig.getAntiPatternMessage()).thenReturn(SUGGESTION_MESSAGE);
        Map<String, RuleConfig> mockRules = Map.of("GetCompletionsCheck", mockRuleConfig);
        mockVisitor = new GetCompletionsCheck.GetCompletionsVisitor(mockHolder, mockRules);
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    public void detectsDiscouragedAPIUsage(TestCase testCase) {
        setupMockAPI(testCase.methodToCheck, testCase.numOfInvocations, testCase.packageName);
        mockVisitor.visitMethodCallExpression(methodCallExpression);
        verify(mockHolder, times(testCase.numOfInvocations)).registerProblem(eq(problemElement),
            eq(SUGGESTION_MESSAGE));
    }

    private void setupMockAPI(String methodToCheck, int numOfInvocations, String packageName) {
        methodCallExpression = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression methodExpression = mock(PsiReferenceExpression.class);
        PsiMethod resolvedMethod = mock(PsiMethod.class);
        PsiClass containingClass = mock(PsiClass.class);
        problemElement = mock(PsiElement.class);
        when(mockRuleConfig.getScopeToCheck()).thenReturn(Arrays.asList(packageName));

        when(methodCallExpression.getMethodExpression()).thenReturn(methodExpression);
        when(methodExpression.resolve()).thenReturn(resolvedMethod);
        when(resolvedMethod.getContainingClass()).thenReturn(containingClass);
        when(resolvedMethod.getName()).thenReturn(methodToCheck);
        when(containingClass.getQualifiedName()).thenReturn(packageName);
        when(methodExpression.getReferenceNameElement()).thenReturn(problemElement);
    }


    private static Stream<TestCase> provideTestCases() {
        return Stream.of(
            new TestCase("getCompletions", "com.azure.ai.openai",  1),
           new TestCase("getCompletions", "com.azure.other",  0)
        );
    }

    private static class TestCase {
        String methodToCheck;
        String packageName;
        int numOfInvocations;

        TestCase(String methodToCheck, String packageName, int numOfInvocations) {
            this.methodToCheck = methodToCheck;
            this.packageName = packageName;
            this.numOfInvocations = numOfInvocations;
        }
    }
}