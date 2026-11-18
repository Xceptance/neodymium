/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.TokenBudgetExceededException;
import org.neodymium.ai.playbook.YamlPlaybookParser;
import org.neodymium.ai.telemetry.TokenBudgetGuard;
import org.neodymium.util.Neodymium;

/**
 * Comprehensive test suite verifying the configuration property precedence order across all layers:
 * Base defaults &lt; Config files &lt; System properties &lt; YAML Playbook _properties &lt; @AiContext runner annotations.
 * Also verifies thread-local cleanup isolation and nested YAML flattening.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class PropertyPrecedenceOrderTest
{
    @BeforeEach
    @AfterEach
    public void cleanup()
    {
        Neodymium.clearThreadContext();
        System.clearProperty("neodymium.ai.tokenBudget.input");
        System.clearProperty("neodymium.ai.tokenBudget.output");
        System.clearProperty("neodymium.ai.step.maxTokens");
        System.clearProperty("neodymium.ai.tokenBudget.step");
        System.clearProperty("neodymium.ai.step.tokenBudget");
        System.clearProperty("tokenBudget.step");
        System.clearProperty("neodymium.ai.step.maxTurns");
        System.clearProperty("neodymium.ai.testProp");
        System.clearProperty("MY_TEST_BUDGET");
        AiConfiguration.resetInstance();
    }

    @Test
    @DisplayName("Full precedence ladder: Base default < System Property < Neodymium.getData() < clearThreadContext revert")
    public void testFullConfigurationPrecedenceLadder()
    {
        // 1. Base Default
        final AiConfiguration defaultConfig = AiConfiguration.getInstance();
        assertEquals(500_000, defaultConfig.getTokenBudgetInput(),
            "Default input token budget must be 500,000 tokens.");

        // 2. System Property overrides Base Default
        System.setProperty("neodymium.ai.tokenBudget.input", "750000");
        AiConfiguration.resetInstance();
        assertEquals(750_000, AiConfiguration.getInstance().getTokenBudgetInput(),
            "System property must override base configuration default.");

        // 3. Neodymium.getData() thread-local context overrides System Property
        Neodymium.getData().put("neodymium.ai.tokenBudget.input", "1000000");
        assertEquals(1_000_000, AiConfiguration.getInstance().getTokenBudgetInput(),
            "Neodymium.getData() thread-local override must take precedence over System property.");

        // 4. clearThreadContext() reverts Neodymium.getData() back to System Property
        Neodymium.clearThreadContext();
        assertEquals(750_000, AiConfiguration.getInstance().getTokenBudgetInput(),
            "Clearing thread context must revert resolution back to active System property.");

        // 5. Clearing System Property reverts back to Base Default
        System.clearProperty("neodymium.ai.tokenBudget.input");
        AiConfiguration.resetInstance();
        assertEquals(500_000, AiConfiguration.getInstance().getTokenBudgetInput(),
            "Clearing System property must revert resolution back to base default.");
    }

    @Test
    @DisplayName("YAML playbook _properties inheritance and row-level override precedence")
    public void testYamlPropertiesPrecedenceAndInheritance() throws IOException
    {
        final String yamlContent = """
            _properties:
              neodymium.ai.tokenBudget.input: 1200000
              neodymium.ai.step.maxTurns: 20
            data:
              - user: "Alice"
              - user: "Bob"
                _properties:
                  neodymium.ai.tokenBudget.input: 1800000
            steps:
              - "Navigate to homepage"
            """;

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parseString(yamlContent);

        final List<Map<String, SessionData.DataEntry>> datasets = playbook.getDataSets();
        assertEquals(2, datasets.size(), "Playbook must have 2 dataset rows.");

        // Validate Row 0 (Alice): inherits root properties
        final Map<String, SessionData.DataEntry> row0 = datasets.get(0);
        assertEquals("Alice", row0.get("user").value());
        assertEquals("1200000", row0.get("neodymium.ai.tokenBudget.input").value());
        assertEquals("20", row0.get("neodymium.ai.step.maxTurns").value());

        // Validate Row 1 (Bob): overrides input token budget, inherits maxTurns
        final Map<String, SessionData.DataEntry> row1 = datasets.get(1);
        assertEquals("Bob", row1.get("user").value());
        assertEquals("1800000", row1.get("neodymium.ai.tokenBudget.input").value());
        assertEquals("20", row1.get("neodymium.ai.step.maxTurns").value());

        // Simulate Test Execution for Row 0: NeodymiumAiRunner copies dataset to Neodymium.getData()
        Neodymium.clearThreadContext();
        row0.forEach((k, v) -> Neodymium.getData().put(k, String.valueOf(v.value())));
        assertEquals(1_200_000, AiConfiguration.getInstance().getTokenBudgetInput(),
            "Row 0 must resolve inherited root token budget.");
        assertEquals(20, AiConfiguration.getInstance().getStepMaxTurns(),
            "Row 0 must resolve inherited maxTurns.");

        // Simulate Test Execution for Row 1: runner resets context, copies row 1
        Neodymium.clearThreadContext();
        row1.forEach((k, v) -> Neodymium.getData().put(k, String.valueOf(v.value())));
        assertEquals(1_800_000, AiConfiguration.getInstance().getTokenBudgetInput(),
            "Row 1 must resolve overridden token budget.");
        assertEquals(20, AiConfiguration.getInstance().getStepMaxTurns(),
            "Row 1 must resolve inherited maxTurns.");

        // After test execution lifecycle
        Neodymium.clearThreadContext();
        assertEquals(500_000, AiConfiguration.getInstance().getTokenBudgetInput(),
            "Context reset must return to base default.");
        assertEquals(15, AiConfiguration.getInstance().getStepMaxTurns(),
            "Context reset must return to base default.");
    }

    @Test
    @DisplayName("Annotation hierarchy precedence: Method @AiContext > Class @AiContext > YAML Playbook dataset _properties")
    public void testRunnerAnnotationPrecedenceOverYamlProperties()
    {
        // 1. YAML playbook populates Neodymium.getData()
        Neodymium.getData().put("neodymium.ai.tokenBudget.input", "1500000");
        assertEquals(1_500_000, AiConfiguration.getInstance().getTokenBudgetInput(),
            "Neodymium.getData() from YAML playbook resolves initially.");

        // 2. Class-level @AiContext(tokenBudgetInput = 2000000) overrides YAML playbook property
        final int classLevelBudget = 2_000_000;
        Neodymium.getData().put("neodymium.ai.tokenBudget.input", String.valueOf(classLevelBudget));
        assertEquals(2_000_000, AiConfiguration.getInstance().getTokenBudgetInput(),
            "Class-level @AiContext must override YAML playbook property.");

        // 3. Method-level @AiContext(tokenBudgetInput = 2500000) overrides Class-level annotation
        final int methodLevelBudget = 2_500_000;
        Neodymium.getData().put("neodymium.ai.tokenBudget.input", String.valueOf(methodLevelBudget));
        assertEquals(2_500_000, AiConfiguration.getInstance().getTokenBudgetInput(),
            "Method-level @AiContext must override class-level annotation.");

        // 4. Thread isolation verification
        Neodymium.clearThreadContext();
        assertEquals(500_000, AiConfiguration.getInstance().getTokenBudgetInput(),
            "Thread context clear must isolate test executions and revert to default.");
    }

    @Test
    @DisplayName("Nested YAML _properties hierarchy is flattened and resolved correctly")
    public void testNestedYamlPropertiesFlattening() throws IOException
    {
        final String yamlContent = """
            _properties:
              neodymium:
                ai:
                  tokenBudget:
                    input: 1600000
                    output: 80000
                  step:
                    maxTurns: 25
            steps:
              - "Verify homepage title"
            """;

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parseString(yamlContent);

        final List<Map<String, SessionData.DataEntry>> datasets = playbook.getDataSets();
        assertEquals(1, datasets.size(), "Playbook must generate default dataset with root properties.");

        final Map<String, SessionData.DataEntry> row = datasets.get(0);
        assertEquals("1600000", row.get("neodymium.ai.tokenBudget.input").value());
        assertEquals("80000", row.get("neodymium.ai.tokenBudget.output").value());
        assertEquals("25", row.get("neodymium.ai.step.maxTurns").value());

        // Copy to Neodymium.getData()
        row.forEach((k, v) -> Neodymium.getData().put(k, String.valueOf(v.value())));

        final AiConfiguration config = AiConfiguration.getInstance();
        assertEquals(1_600_000, config.getTokenBudgetInput(), "Flattened input budget must resolve.");
        assertEquals(80_000, config.getTokenBudgetOutput(), "Flattened output budget must resolve.");
        assertEquals(25, config.getStepMaxTurns(), "Flattened maxTurns must resolve.");
    }

    @Test
    @DisplayName("Dynamic placeholder substitution resolves System properties within Neodymium.getData()")
    public void testPlaceholderSubstitutionInNeodymiumData()
    {
        System.setProperty("MY_TEST_BUDGET", "1750000");
        Neodymium.getData().put("neodymium.ai.tokenBudget.input", "${MY_TEST_BUDGET}");

        assertEquals(1_750_000, AiConfiguration.getInstance().getTokenBudgetInput(),
            "Placeholders within Neodymium.getData() properties must dynamically resolve System properties.");
    }

    @Test
    @DisplayName("TokenBudgetGuard integrates with precedence order and enforces active limits")
    public void testTokenBudgetGuardIntegrationWithPrecedence()
    {
        final ExecutionEventBus eventBus = new ExecutionEventBus();

        // Step 1: Set YAML / NeodymiumData budget of 1,200,000 tokens
        Neodymium.getData().put("neodymium.ai.tokenBudget.input", "1200000");

        final TokenBudgetGuard guard = new TokenBudgetGuard();
        eventBus.registerListener(guard);

        // Usage of 800,000 tokens should PASS under 1,200,000 budget
        final LlmRequest request1 = new LlmRequest("System prompt", "User prompt", null, null, 0.0, 30);
        final LlmResponse response1 = new LlmResponse("Result", new TokenUsage(800_000, 1_000, 801_000), "mock-model");
        final LlmResponseReceivedEvent event1 = new LlmResponseReceivedEvent(request1, response1, 100, "EXECUTION");

        assertDoesNotThrow(() -> eventBus.dispatch(event1),
            "Usage under custom 1,200,000 budget should not throw exception.");

        // Step 2: Reset thread context (reverting to default 500,000 limit)
        Neodymium.clearThreadContext();

        final TokenBudgetGuard defaultGuard = new TokenBudgetGuard();
        final ExecutionEventBus defaultEventBus = new ExecutionEventBus();
        defaultEventBus.registerListener(defaultGuard);

        // Usage of 600,000 tokens should FAIL under default 500,000 budget
        final LlmRequest request2 = new LlmRequest("System prompt", "User prompt", null, null, 0.0, 30);
        final LlmResponse response2 = new LlmResponse("Result", new TokenUsage(600_000, 1_000, 601_000), "mock-model");
        final LlmResponseReceivedEvent event2 = new LlmResponseReceivedEvent(request2, response2, 100, "EXECUTION");

        final TokenBudgetExceededException exception = assertThrows(
            TokenBudgetExceededException.class,
            () -> defaultEventBus.dispatch(event2),
            "Usage over default 500,000 budget must throw TokenBudgetExceededException."
        );

        assertTrue(exception.isInputBudget());
        assertEquals(600_000, exception.getConsumedTokens());
        assertEquals(500_000, exception.getBudgetLimit());
    }

    @Test
    @DisplayName("Step token budget supports aliases, System properties, and Neodymium.getData() overrides")
    public void testStepTokenBudgetAliasesAndOverrides()
    {
        // 1. Default fallback
        assertEquals(100_000, AiConfiguration.getInstance().getStepTokenBudget(),
            "Default step token budget must be 100,000.");

        // 2. Alias: tokenBudget.step
        System.setProperty("tokenBudget.step", "150000");
        AiConfiguration.resetInstance();
        assertEquals(150_000, AiConfiguration.getInstance().getStepTokenBudget(),
            "Should resolve from alias tokenBudget.step");

        // 3. Alias: neodymium.ai.step.tokenBudget
        System.setProperty("neodymium.ai.step.tokenBudget", "175000");
        AiConfiguration.resetInstance();
        assertEquals(175_000, AiConfiguration.getInstance().getStepTokenBudget(),
            "Should resolve from alias neodymium.ai.step.tokenBudget");

        // 4. Alias: neodymium.ai.tokenBudget.step
        System.setProperty("neodymium.ai.tokenBudget.step", "200000");
        AiConfiguration.resetInstance();
        assertEquals(200_000, AiConfiguration.getInstance().getStepTokenBudget(),
            "Should resolve from alias neodymium.ai.tokenBudget.step");

        // 5. Canonical: neodymium.ai.step.maxTokens
        System.setProperty("neodymium.ai.step.maxTokens", "250000");
        AiConfiguration.resetInstance();
        assertEquals(250_000, AiConfiguration.getInstance().getStepTokenBudget(),
            "Should resolve from canonical property neodymium.ai.step.maxTokens");

        // 6. Thread-local Neodymium.getData() override
        Neodymium.getData().put("neodymium.ai.step.maxTokens", "300000");
        assertEquals(300_000, AiConfiguration.getInstance().getStepTokenBudget(),
            "Neodymium.getData() must override System property for step token budget.");

        // 7. Revert on clearThreadContext()
        Neodymium.clearThreadContext();
        assertEquals(250_000, AiConfiguration.getInstance().getStepTokenBudget(),
            "Clearing thread context must revert to System property.");
    }

    @Test
    @DisplayName("TokenBudgetExceededException distinguishes between test-level and step-level failures in error message")
    public void testTokenBudgetExceededExceptionStepMessageFormatting()
    {
        final TokenBudgetExceededException stepException = new TokenBudgetExceededException(
            TokenBudgetExceededException.BudgetType.TOTAL,
            100_124,
            100_000
        );
        assertTrue(stepException.getMessage().startsWith("Token budget exceeded for step:"),
            "Step budget exception must clearly state 'Token budget exceeded for step:'.");
        assertTrue(stepException.getMessage().contains("Total tokens consumed (100124) exceeded configured step token budget (100000). Step aborted."),
            "Step budget exception message must contain precise consumption and limit numbers.");

        final TokenBudgetExceededException testException = new TokenBudgetExceededException(
            TokenBudgetExceededException.BudgetType.INPUT,
            500_050,
            500_000
        );
        assertTrue(testException.getMessage().startsWith("Token budget exceeded for test run:"),
            "Test run budget exception must state 'Token budget exceeded for test run:'.");
    }
}
