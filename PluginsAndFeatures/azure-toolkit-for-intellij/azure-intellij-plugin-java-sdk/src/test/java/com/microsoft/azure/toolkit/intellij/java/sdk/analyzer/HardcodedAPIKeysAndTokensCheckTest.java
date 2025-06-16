// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.analyzer;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiNewExpression;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.RuleConfigLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * Test the HardcodedAPIKeysAndTokensCheck class for hardcoded API keys and tokens.
 * When a client is authenticated with AzurekeyCredentials and AccessToken, a problem is registered.
 * These are some instances that a flag would be raised.
 * 1. TextAnalyticsClient client = new TextAnalyticsClientBuilder()
 * .endpoint(endpoint)
 * .credential(new AzureKeyCredential(apiKey))
 * .buildClient();
 * <p>
 * 2. TokenCredential credential = request -> {
 * AccessToken token = new AccessToken("<your-hardcoded-token>", OffsetDateTime.now().plusHours(1));
 * }
 */
public class HardcodedAPIKeysAndTokensCheckTest {

    private static final String SUGGESTION_MESSAGE =
        "`DefaultAzureCredential` is recommended for authentication if the service client supports Token Credential (Entra ID Authentication). If not, then use environment variables when using key based authentication.";
    @Mock
    private ProblemsHolder mockHolder;

    @Mock
    private JavaElementVisitor mockVisitor;

    @Mock
    private RuleConfig mockRuleConfig;
    private PsiElement problemElement;
    @Mock
    private RuleConfigLoader mockRuleConfigLoader;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockHolder = mock(ProblemsHolder.class);
        // Set up mock rule config
        when(mockRuleConfig.isSkipRuleCheck()).thenReturn(false);
        when(mockRuleConfig.getUsagesToCheck()).thenReturn(Arrays.asList("AzureKeyCredential",
            "AccessToken",
            "KeyCredential",
            "AzureNamedKeyCredential",
            "AzureSasCredential",
            "AzureNamedKey",
            "ClientSecretCredentialBuilder",
            "UsernamePasswordCredentialBuilder",
            "BasicAuthenticationCredential"));
        when(mockRuleConfig.getAntiPatternMessage()).thenReturn(SUGGESTION_MESSAGE);
        when(mockRuleConfig.getScopeToCheck()).thenReturn(Collections.emptyList());
        Map<String, RuleConfig> mockRules = Map.of("HardcodedAPIKeysAndTokensCheck", mockRuleConfig);

        mockVisitor = new HardcodedAPIKeysAndTokensCheck.APIKeysAndTokensVisitor(mockHolder, mockRules);
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testHardcodedAPIKeysAndTokensCheck(TestCase testCase) {
        PsiNewExpression mockExpression = mockMethodExpression(testCase.apiName, testCase.credentialString,
            testCase.numOfInvocations);
        mockVisitor.visitNewExpression(mockExpression);

        verify(mockHolder, times(testCase.numOfInvocations))
            .registerProblem(eq(mockExpression),eq(SUGGESTION_MESSAGE));
    }

    @Test
    public void testNoFlagIfNoHardcodedAPIKeysAndTokens() {
        PsiNewExpression newExpression = mock(PsiNewExpression.class);
        PsiJavaCodeReferenceElement javaCodeReferenceElement = mock(PsiJavaCodeReferenceElement.class);
        PsiLiteralExpression literalExpression = mock(PsiLiteralExpression.class);

        when(newExpression.getClassReference()).thenReturn(javaCodeReferenceElement);
        when(javaCodeReferenceElement.getReferenceName()).thenReturn("AzureKeyCredential");
        when(javaCodeReferenceElement.getQualifiedName()).thenReturn("com.azure.");
        when(newExpression.getChildren()).thenReturn(new PsiElement[]{literalExpression});
        when(literalExpression.getValue()).thenReturn(System.getenv());

        mockVisitor.visitNewExpression(newExpression);

        verify(mockHolder, times(0)).registerProblem(eq(newExpression), eq(SUGGESTION_MESSAGE));
    }

    private static Stream<TestCase> testCases() {
        return Stream.of(
            new TestCase("AzureKeyCredential", "340c6ea27d214f88b7a759fee63cbfa1", 1),
            new TestCase("AccessToken", "access-token", 0),
            new TestCase("KeyCredential", "340c6ea27d214f88b7a759fee63cbfa1", 1),
            new TestCase("AzureNamedKeyCredential", "340c6ea27d214f88b7a759fee63cbfa1", 1),
            new TestCase("AzureSasCredential", "", 0),
            new TestCase("AzureNamedKey", "", 0),
            new TestCase("SomeOtherClient", "340c6ea27d214f88b7a759fee63cbfa1", 0),
            new TestCase("", "340c6ea27d214f88b7a759fee63cbfa1", 0)
        );
    }

    private PsiNewExpression mockMethodExpression(String authServiceToCheck, String credentialString, int numOfInvocations) {
        PsiNewExpression newExpression = mock(PsiNewExpression.class);
        PsiJavaCodeReferenceElement javaCodeReferenceElement = mock(PsiJavaCodeReferenceElement.class);
        PsiLiteralExpression literalExpression = mock(PsiLiteralExpression.class);
        PsiExpressionList expressionList = mock(PsiExpressionList.class);

        when(newExpression.getClassReference()).thenReturn(javaCodeReferenceElement);
        when(javaCodeReferenceElement.getReferenceName()).thenReturn(authServiceToCheck);
        when(javaCodeReferenceElement.getQualifiedName()).thenReturn("com.azure.");
        when(newExpression.getChildren()).thenReturn(new PsiElement[]{expressionList});
        when(expressionList.getExpressions()).thenReturn(new PsiExpression[]{literalExpression});
        when(literalExpression.getValue()).thenReturn(credentialString);

        return newExpression;
    }

    static class TestCase {
        String apiName;
        String credentialString;
        int numOfInvocations;

        TestCase(String apiName, String credentialString, int numOfInvocations) {
            this.apiName = apiName;
            this.credentialString = credentialString;
            this.numOfInvocations = numOfInvocations;
        }
    }
}
