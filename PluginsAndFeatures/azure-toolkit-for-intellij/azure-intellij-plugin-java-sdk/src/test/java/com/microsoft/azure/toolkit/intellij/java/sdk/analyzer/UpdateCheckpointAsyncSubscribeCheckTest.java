// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.RuleConfigLoader;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link UpdateCheckpointAsyncSubscribeCheck} and derived classes.
 */
public class UpdateCheckpointAsyncSubscribeCheckTest {
    private static final String SUGGESTION_MESSAGE = "Consider replacing `subscribe()` with `block()` or `block(timeout)`, or use the synchronous version `updateCheckpoint()` for better reliability.";

    @Mock
    private ProblemsHolder mockHolder;

    @Mock
    private PsiMethodCallExpression mockMethodCallExpression;
    @Mock
    private PsiElement mockPsiElement;

    @Mock private RuleConfigLoader mockRuleConfigLoader;
    @Mock private RuleConfig mockRuleConfig;
    @Mock
    private JavaElementVisitor mockVisitor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockHolder = mock(ProblemsHolder.class);
        // Set up mock rule config
        when(mockRuleConfig.isSkipRuleCheck()).thenReturn(false);
        when(mockRuleConfig.getUsagesToCheck()).thenReturn(Collections.singletonList("updateCheckpointAsync"));
        when(mockRuleConfig.getAntiPatternMessage()).thenReturn(SUGGESTION_MESSAGE);
        when(mockRuleConfig.getScopeToCheck()).thenReturn(Collections.singletonList("EventBatchContext"));
        mockMethodCallExpression = mock(PsiMethodCallExpression.class);
        mockPsiElement = mock(PsiElement.class);

        Map<String, RuleConfig> mockRules = Map.of("UpdateCheckpointAsyncSubscribeCheck", mockRuleConfig);
        mockVisitor = new UpdateCheckpointAsyncSubscribeCheck.UpdateCheckpointAsyncVisitor(mockHolder,
            mockRules);
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    public void testWithParameterizedCases(String packageName, String mainMethodFound,
        int numOfInvocations, String followingMethod, String objectType) {
        setupMockMethodCall(packageName, mainMethodFound, numOfInvocations, followingMethod,
            objectType,
            SUGGESTION_MESSAGE);
        mockVisitor.visitMethodCallExpression(mockMethodCallExpression);
        verify(mockHolder, times(numOfInvocations)).registerProblem(eq(mockPsiElement),
            eq(SUGGESTION_MESSAGE));
    }

    private static Stream<Object[]> provideTestCases() {
        return Stream.of(
            new Object[] {"com.azure.", "updateCheckpointAsync", 1,
                "subscribe", "EventBatchContext"},
            new Object[] {"com.azure.", "updateCheckpointAsync", 0, "notSubscribe", "EventBatchContext"}
        );
    }

    private void setupMockMethodCall(String packageName, String mainMethodFound,
        int numOfInvocations, String followingMethod, String objectType, String expectedMessage) {
        PsiReferenceExpression mockReferenceExpression = mock(PsiReferenceExpression.class);
        PsiReferenceExpression parentReferenceExpression = mock(PsiReferenceExpression.class);
        PsiMethodCallExpression grandParentMethodCalLExpression = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression mockQualifier = mock(PsiReferenceExpression.class);
        PsiParameter mockParameter = mock(PsiParameter.class);
        PsiClassType parameterType = mock(PsiClassType.class);
        PsiClass psiClass = mock(PsiClass.class);
        PsiMethod resolvedMethod = mock(PsiMethod.class);
        PsiClass containingClass = mock(PsiClass.class);
        PsiReferenceExpression followingMethodRefExpression = mock(PsiReferenceExpression.class);

        when(mockMethodCallExpression.getMethodExpression()).thenReturn(mockReferenceExpression);
        when(mockReferenceExpression.getReferenceName()).thenReturn(mainMethodFound);
        when(mockReferenceExpression.resolve()).thenReturn(resolvedMethod);
        when(resolvedMethod.getContainingClass()).thenReturn(containingClass);
        when(resolvedMethod.getName()).thenReturn(mainMethodFound);
        when(containingClass.getQualifiedName()).thenReturn(packageName + objectType);

        when(mockMethodCallExpression.getParent()).thenReturn(followingMethodRefExpression);
        when(followingMethodRefExpression.getParent()).thenReturn(grandParentMethodCalLExpression);
        when(grandParentMethodCalLExpression.getMethodExpression()).thenReturn(parentReferenceExpression);
        when(followingMethodRefExpression.getReferenceName()).thenReturn(followingMethod);
        when(mockReferenceExpression.getQualifierExpression()).thenReturn(mockQualifier);
        when(mockQualifier.resolve()).thenReturn(mockParameter);
        when(mockParameter.getType()).thenReturn(parameterType);
        when(parameterType.getCanonicalText()).thenReturn(objectType);
        when(parameterType.resolve()).thenReturn(psiClass);
        when(psiClass.getQualifiedName()).thenReturn(packageName + objectType);
        when(mockReferenceExpression.getReferenceNameElement()).thenReturn(mockPsiElement);
    }
}