// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.RuleConfigLoader;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This class is used to test the UseOfBlockOnAsyncClientsCheck class. The UseOfBlockOnAsyncClientsCheck class is an
 * inspection tool that checks for the use of blocking method on async clients in Azure SDK. This inspection will check
 * for the use of blocking method on reactive types like Mono, Flux, etc. This is an example of what should be flagged:
 * <p>
 * private ServiceBusReceiverAsyncClient receiver; receiver.complete(received).block(Duration.ofSeconds(15));
 * <p>
 * private final ServiceBusReceiverAsyncClient client; try { if (isComplete) { client.complete(message)
 * .doOnSuccess(success -> System.out.println("Message completed successfully")) .doOnError(error ->
 * System.err.println("Error completing message: " + error.getMessage())) .log() .timeout(Duration.ofSeconds(30))
 * .retry(3) .block();
 * <p>
 * } else { client.abandon(message).block(); }
 */
public class UseOfBlockOnAsyncClientsCheckTest {
    private static final String SUGGESTION_MESSAGE = "Blocking calls detected on asynchronous clients. Consider using" +
        " fully synchronous APIs instead.";

    @Mock
    private ProblemsHolder mockHolder;

    @Mock
    private JavaElementVisitor mockVisitor;

    @Mock
    private PsiMethodCallExpression mockElement;

    @Mock
    private PsiElement problemElement;

    @Mock private RuleConfigLoader mockRuleConfigLoader;
    @Mock private RuleConfig mockRuleConfig;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockHolder = mock(ProblemsHolder.class);
        // Set up mock rule config
        when(mockRuleConfig.isSkipRuleCheck()).thenReturn(false);
        when(mockRuleConfig.getUsagesToCheck()).thenReturn(Arrays.asList("block",
            "blockOptional",
            "blockFirst",
            "blockLast",
            "toIterable",
            "toStream",
            "toFuture",
            "blockFirstOptional",
            "blockLastOptional"));
        when(mockRuleConfig.getAntiPatternMessage()).thenReturn(SUGGESTION_MESSAGE);
        when(mockRuleConfig.getScopeToCheck()).thenReturn(Arrays.asList("reactor.core.publisher.Flux",
            "reactor.core.publisher.Mono"));
        mockElement = mock(PsiMethodCallExpression.class);
        problemElement = mock(PsiElement.class);
        Map<String, RuleConfig> mockRules = Map.of("UseOfBlockOnAsyncClientsCheck", mockRuleConfig);
        mockVisitor = new UseOfBlockOnAsyncClientsCheck.UseOfBlockOnAsyncClientsVisitor(mockHolder, mockRules);
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    public void testUseOfBlockOnAsyncClient(TestCase testCase) {
        setupMockMethodCallExpression(testCase.methodName, testCase.clientPackageName, testCase.numberOfInvocations,
            testCase.reactivePackageName);
        mockVisitor.visitMethodCallExpression(mockElement);

        verify(mockHolder, times(testCase.numberOfInvocations)).registerProblem(Mockito.eq(problemElement),
            Mockito.eq(SUGGESTION_MESSAGE));
    }

    private void setupMockMethodCallExpression(String methodName, String clientPackageName, int numberOfInvocations,
        String reactivePackageName) {
        PsiReferenceExpression referenceExpression = mock(PsiReferenceExpression.class);
        PsiMethodCallExpression expression = mock(PsiMethodCallExpression.class);
        PsiClassType type = mock(PsiClassType.class);
        PsiClass qualifierReturnTypeClass = mock(PsiClass.class);

        PsiReferenceExpression clientReferenceExpression = mock(PsiReferenceExpression.class);
        PsiReferenceExpression clientQualifierExpression = mock(PsiReferenceExpression.class);
        PsiClassType clientType = mock(PsiClassType.class);
        PsiClass clientReturnTypeClass = mock(PsiClass.class);

        when(mockElement.getMethodExpression()).thenReturn(referenceExpression);
        when(referenceExpression.getReferenceName()).thenReturn(methodName);

        when(referenceExpression.getQualifierExpression()).thenReturn(expression);
        when(expression.getType()).thenReturn(type);
        when(type.resolve()).thenReturn(qualifierReturnTypeClass);

        when(qualifierReturnTypeClass.getQualifiedName()).thenReturn(reactivePackageName);

        when(expression.getMethodExpression()).thenReturn(clientReferenceExpression);
        when(clientReferenceExpression.getQualifierExpression()).thenReturn(clientQualifierExpression);
        when(clientQualifierExpression.getType()).thenReturn(clientType);
        when(clientType.resolve()).thenReturn(clientReturnTypeClass);
        when(clientReturnTypeClass.getQualifiedName()).thenReturn(clientPackageName);
        when(referenceExpression.getReferenceNameElement()).thenReturn(problemElement);
    }

    private static Stream<TestCase> provideTestCases() {
        return Stream.of(
            new TestCase("block", "com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient", 1,
                "reactor.core.publisher.Flux"),
            new TestCase("blockOptional", "com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient", 1,
                "reactor.core.publisher.Mono"),
            new TestCase("blockFirst", "com.notAzure.", 0, "reactor.core.publisher.Flux"),
            new TestCase("nonBlockingMethod", "com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient", 0,
                "reactor.core.publisher.Flux"),
            new TestCase("block", "com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient", 0, "java.util.List"),
            new TestCase("block", "com.azure.messaging.servicebus.ServiceBusReceiverClient", 0,
                "reactor.core.publisher.Mono")
        );
    }

    private static class TestCase {
        String methodName;
        String clientPackageName;
        int numberOfInvocations;
        String reactivePackageName;

        TestCase(String methodName, String clientPackageName, int numberOfInvocations, String reactivePackageName) {
            this.methodName = methodName;
            this.clientPackageName = clientPackageName;
            this.numberOfInvocations = numberOfInvocations;
            this.reactivePackageName = reactivePackageName;
        }
    }
}