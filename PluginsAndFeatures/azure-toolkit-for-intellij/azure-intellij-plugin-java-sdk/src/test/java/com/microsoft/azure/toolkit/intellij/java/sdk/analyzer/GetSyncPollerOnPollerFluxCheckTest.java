// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GetSyncPollerOnPollerFluxCheck.
 */
public class GetSyncPollerOnPollerFluxCheckTest {
    private static final String SUGGESTION_MESSAGE =
        "`getSyncPoller()` API usage on a `PollerFlux` detected. Consider using a `SyncPoller` directly to handle synchronous polling tasks.";

    @Mock
    private ProblemsHolder mockHolder;

    @Mock
    private JavaElementVisitor mockVisitor;

    @Mock
    private PsiMethodCallExpression mockMethodCallExpression;

    @Mock
    private PsiElement mockElement;
    @Mock private RuleConfigLoader mockRuleConfigLoader;
    @Mock private RuleConfig mockRuleConfig;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockHolder = mock(ProblemsHolder.class);
        // Set up mock rule config
        when(mockRuleConfig.isSkipRuleCheck()).thenReturn(false);
        when(mockRuleConfig.getUsagesToCheck()).thenReturn(Collections.singletonList("getSyncPoller"));
        when(mockRuleConfig.getAntiPatternMessage()).thenReturn(SUGGESTION_MESSAGE);
        when(mockRuleConfig.getScopeToCheck()).thenReturn(Collections.emptyList());
        mockMethodCallExpression = mock(PsiMethodCallExpression.class);
        mockElement = mock(PsiElement.class);
        Map<String, RuleConfig> mockRules = Map.of("GetSyncPollerOnPollerFluxCheck", mockRuleConfig);
        mockVisitor = new GetSyncPollerOnPollerFluxCheck().new GetSyncPollerOnPollerFluxVisitor(mockHolder, mockRules);
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testGetSyncPollerOnPollerFluxCheck(TestCase testCase) {
        mockMethodExpression(testCase.methodName, testCase.className, testCase.numberOfInvocations);
        mockVisitor.visitMethodCallExpression(mockMethodCallExpression);
        verify(mockHolder, times(testCase.numberOfInvocations)).registerProblem(mockElement, SUGGESTION_MESSAGE);
    }

    private static Stream<TestCase> testCases() {
        return Stream.of(
            new TestCase("getSyncPoller", "com.azure.core.util.polling.PollerFlux", 1),
            new TestCase("getAnotherMethod", "com.azure.core.util.polling.PollerFlux", 0),
            new TestCase("getSyncPoller", "com.not.azure..util.polling.DifferentClassName", 0),
            new TestCase("getSyncPoller", null, 0)
        );
    }

    private void mockMethodExpression(String methodName, String className, int numberOfInvocations) {
        PsiReferenceExpression referenceExpression = mock(PsiReferenceExpression.class);
        PsiExpression expression = mock(PsiExpression.class);
        PsiType type = mock(PsiType.class);
        PsiClass containingClass = mock(PsiClass.class);
        PsiMethod method = mock(PsiMethod.class);

        when(mockMethodCallExpression.resolveMethod()).thenReturn(method);
        when(method.getContainingClass()).thenReturn(containingClass);
        when(containingClass.getQualifiedName()).thenReturn(className);
        when(mockMethodCallExpression.getMethodExpression()).thenReturn(referenceExpression);
        when(referenceExpression.getReferenceName()).thenReturn(methodName);
        when(referenceExpression.getReferenceNameElement()).thenReturn(mockElement);
    }

    private static class TestCase {
        String methodName;
        String className;
        int numberOfInvocations;

        TestCase(String methodName, String className, int numberOfInvocations) {
            this.methodName = methodName;
            this.className = className;
            this.numberOfInvocations = numberOfInvocations;
        }
    }
}