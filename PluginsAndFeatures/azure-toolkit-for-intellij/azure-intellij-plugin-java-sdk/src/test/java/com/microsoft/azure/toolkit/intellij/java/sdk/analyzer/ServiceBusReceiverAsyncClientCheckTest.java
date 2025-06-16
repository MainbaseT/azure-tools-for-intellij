// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiTypeElement;
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
 * This class tests the ServiceBusReceiverAsyncClientCheck class by mocking the ProblemsHolder and PsiElementVisitor
 * and verifying that a problem is registered when the ServiceBusReceiverAsyncClient is used.
 * The test also verifies that a problem is not registered when the PsiElement is null.
 *
 * Here are some examples of test data where registerProblem should be called:
 * 1. ServiceBusReceiverAsyncClient client = new ServiceBusReceiverAsyncClient();
 * 2. private ServiceBusReceiverAsyncClient receiver;
 * 3. final ServiceBusReceiverAsyncClient autoCompleteReceiver =
 *    toClose(getReceiverBuilder(false, entityType, index, false)
 *    .buildAsyncClient());
 */
public class ServiceBusReceiverAsyncClientCheckTest {

    private static final String SUGGESTION_MESSAGE = "Use of `ServiceBusReceiverAsyncClient` detected. Consider using `ServiceBusProcessorClient` instead.";
    @Mock
    private ProblemsHolder mockHolder;

    @Mock
    private JavaElementVisitor mockVisitor;

    @Mock
    private PsiTypeElement mockTypeElement;

    @Mock private RuleConfigLoader mockRuleConfigLoader;
    @Mock private RuleConfig mockRuleConfig;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockHolder = mock(ProblemsHolder.class);
        // Set up mock rule config
        when(mockRuleConfig.isSkipRuleCheck()).thenReturn(false);
        when(mockRuleConfig.getUsagesToCheck()).thenReturn(Collections.singletonList("ServiceBusReceiverAsyncClient"));
        when(mockRuleConfig.getScopeToCheck()).thenReturn(Collections.emptyList());
        when(mockRuleConfig.getAntiPatternMessage()).thenReturn(SUGGESTION_MESSAGE);
        mockTypeElement = mock(PsiTypeElement.class);
        Map<String, RuleConfig> mockRules = Map.of("ServiceBusReceiverAsyncClientCheck", mockRuleConfig);
        mockVisitor = new ServiceBusReceiverAsyncClientCheck.ServiceBusReceiverAsyncClientCheckVisitor(mockHolder, mockRules);
    }

    @ParameterizedTest
    @MethodSource("provideServiceBusReceiverAsyncClientTestCases")
    void detectsServiceBusReceiverAsyncClientUsage(TestCase testCase) {
        setupMockElement(mockTypeElement, testCase.numOfInvocations, testCase.usagesToCheck);
        mockVisitor.visitTypeElement(mockTypeElement);
        verify(mockHolder, times(testCase.numOfInvocations)).registerProblem(eq(mockTypeElement), eq(SUGGESTION_MESSAGE));
    }

    private static Stream<TestCase> provideServiceBusReceiverAsyncClientTestCases() {
        return Stream.of(
            new TestCase("ServiceBusReceiverAsyncClient",  1),
            new TestCase("ServiceBusProcessorClient",  0),
            new TestCase("", 0)
        );
    }

    private void setupMockElement(PsiTypeElement typeElement, int numberOfInvocations, String clientToCheck) {
        PsiClassType mockType = mock(PsiClassType.class);
        PsiClass mockClass = mock(PsiClass.class);
        when(typeElement.getType()).thenReturn(mockType);
        when(mockType.resolve()).thenReturn(mockClass);
        when(mockClass.getQualifiedName()).thenReturn("com.azure.");
        when(mockClass.getName()).thenReturn(clientToCheck);
    }

    private static class TestCase {
        String usagesToCheck;
        int numOfInvocations;

        TestCase(String usagesToCheck, int numOfInvocations) {
            this.usagesToCheck = usagesToCheck;
            this.numOfInvocations = numOfInvocations;
        }
    }
}
