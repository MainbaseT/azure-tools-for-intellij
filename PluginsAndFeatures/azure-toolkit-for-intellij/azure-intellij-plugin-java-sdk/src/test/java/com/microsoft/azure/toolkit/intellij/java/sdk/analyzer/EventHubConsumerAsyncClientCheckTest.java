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
 * This class is used to check if the EventHubConsumerAsyncClient is being used in the code.
 */
public class EventHubConsumerAsyncClientCheckTest {

    private static final String SUGGESTION_MESSAGE = "Use of `EventHubConsumerAsyncClient` detected. Consider using `EventProcessorClient` as it simplifies event processing and provides a higher-level abstraction.";
    @Mock
    private ProblemsHolder mockHolder;

    @Mock
    private JavaElementVisitor mockVisitor;

    @Mock
    private PsiTypeElement mockTypeElement;

    @Mock
    private RuleConfigLoader mockRuleConfigLoader;
    @Mock private RuleConfig mockRuleConfig;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockHolder = mock(ProblemsHolder.class);
        // Set up mock rule config
        when(mockRuleConfig.isSkipRuleCheck()).thenReturn(false);
        when(mockRuleConfig.getUsagesToCheck()).thenReturn(Collections.singletonList("EventHubConsumerAsyncClient"));
        when(mockRuleConfig.getAntiPatternMessage()).thenReturn(SUGGESTION_MESSAGE);
        when(mockRuleConfig.getScopeToCheck()).thenReturn(Collections.emptyList());
        Map<String, RuleConfig> mockRules = Map.of("EventHubConsumerAsyncClientCheck", mockRuleConfig);
        mockVisitor =
            new EventHubConsumerAsyncClientCheck.EventHubConsumerAsyncClientVisitor(mockHolder, mockRules);

        mockTypeElement = mock(PsiTypeElement.class);
    }

    @ParameterizedTest
    @MethodSource("provideEventHubConsumerAsyncClientTestCases")
    void detectsEventHubConsumerAsyncClientUsage(TestCase testCase) {
        setupMockElement(mockTypeElement, testCase.numOfInvocations, testCase.usagesToCheck);
        mockVisitor.visitTypeElement(mockTypeElement);
        verify(mockHolder, times(testCase.numOfInvocations)).registerProblem(eq(mockTypeElement),
            eq(SUGGESTION_MESSAGE));
    }

    private static Stream<TestCase> provideEventHubConsumerAsyncClientTestCases() {
        return Stream.of(
            new TestCase("EventHubConsumerAsyncClient", 1),
            new TestCase("EventProcessorClient", 0),
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
