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
package org.neodymium.ai.playbook.linter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;

/**
 * Unit tests for {@link PlaybookLinter}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookLinterTest
{
    private String originalEnabled;

    @BeforeEach
    public void setUp()
    {
        this.originalEnabled = System.getProperty("neodymium.ai.linter.enabled");
    }

    @AfterEach
    public void tearDown()
    {
        if (this.originalEnabled != null)
        {
            System.setProperty("neodymium.ai.linter.enabled", this.originalEnabled);
        }
        else
        {
            System.clearProperty("neodymium.ai.linter.enabled");
        }
        AiConfiguration.resetInstance();
        ExecutionContext.setActiveContext(null);
    }

    @Test
    @DisplayName("Verify pre-flight linter is enabled by default without explicit property")
    public void testLinterEnabledByDefault()
    {
        System.clearProperty("neodymium.ai.linter.enabled");
        System.clearProperty("neodymium.ai.prelinter.enabled");
        System.clearProperty("neodymium.ai.prelint.enabled");
        AiConfiguration.resetInstance();

        assertTrue(AiConfiguration.getInstance().isLinterEnabled());
    }

    @Test
    @DisplayName("Verify pre-flight linter can be disabled via neodymium.ai.linter.enabled=false and aliases")
    public void testLinterCanBeDisabledViaProperty()
    {
        System.setProperty("neodymium.ai.linter.enabled", "false");
        AiConfiguration.resetInstance();
        assertFalse(AiConfiguration.getInstance().isLinterEnabled());

        System.setProperty("neodymium.ai.prelinter.enabled", "false");
        System.clearProperty("neodymium.ai.linter.enabled");
        AiConfiguration.resetInstance();
        assertFalse(AiConfiguration.getInstance().isLinterEnabled());

        System.setProperty("neodymium.ai.prelint.enabled", "false");
        System.clearProperty("neodymium.ai.prelinter.enabled");
        AiConfiguration.resetInstance();
        assertFalse(AiConfiguration.getInstance().isLinterEnabled());
    }

    @Test
    @DisplayName("Verify pre-flight linter is bypassed when disabled")
    public void testLinterDisabledBypassesExecution()
    {
        System.setProperty("neodymium.ai.linter.enabled", "false");
        AiConfiguration.resetInstance();

        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.registerProvider(LlmCapability.LINTER, mockProvider);

        final AiSession session = AiSession.mock(new SessionData(), registry, new ExecutionEventBus(), null);
        final ExecutionContext context = session.getExecutionContext();
        ExecutionContext.setActiveContext(context);

        final PlaybookLinter linter = new PlaybookLinter(session);

        final List<PlaybookStep> steps = List.of(new PlaybookStep("Click Login"));
        final List<PlaybookLinterFinding> findings = linter.lint(steps, "Test Scenario");

        assertTrue(findings.isEmpty());
        assertNull(context.getTransientData().get(ExecutionContext.KEY_LINTER_CALL_COUNT));
    }

    @Test
    @DisplayName("Verify pre-flight linter executes when enabled and records token metrics")
    public void testLinterEnabledExecutesAndTracksTokens()
    {
        System.setProperty("neodymium.ai.linter.enabled", "true");
        AiConfiguration.resetInstance();

        final String cannedJson = """
            {
              "findings": [
                {
                  "stepIndex": 1,
                  "category": "STEP_SPLITTING_CANDIDATE",
                  "severity": "WARNING",
                  "message": "Compound action instruction.",
                  "suggestedRewrite": "1. Step A\\n2. Step B",
                  "scope": null
                }
              ]
            }
            """;

        final MockLlmProvider mockProvider = new MockLlmProvider();
        mockProvider.addResponse(new LlmResponse(cannedJson, new TokenUsage(100, 50, 150, 20), "mock-linter-model"));

        final LlmRegistry registry = new LlmRegistry();
        registry.registerProvider(LlmCapability.LINTER, mockProvider);

        final AiSession session = AiSession.mock(new SessionData(), registry, new ExecutionEventBus(), null);
        final ExecutionContext context = session.getExecutionContext();
        ExecutionContext.setActiveContext(context);

        final PlaybookLinter linter = new PlaybookLinter(session);

        final PlaybookStep s1 = new PlaybookStep("Open menu and click Settings");
        s1.setLineNumber(5);
        s1.setSourceFile("menu.yaml");

        final List<PlaybookLinterFinding> findings = linter.lint(List.of(s1), "Navigation Check");

        assertEquals(1, findings.size());
        final PlaybookLinterFinding f = findings.get(0);
        assertEquals(1, f.stepIndex());
        assertEquals(5, f.lineNumber());
        assertEquals("menu.yaml", f.sourceFile());
        assertEquals(LinterCategory.STEP_SPLITTING_CANDIDATE, f.category());

        // Verify token accounting
        final TokenUsage recordedUsage = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_LINTER_TOKEN_USAGE);
        assertNotNull(recordedUsage);
        assertEquals(100, recordedUsage.inputTokenCount());
        assertEquals(50, recordedUsage.outputTokenCount());
        assertEquals(20, recordedUsage.cachedTokenCount());

        final Integer calls = (Integer) context.getTransientData().get(ExecutionContext.KEY_LINTER_CALL_COUNT);
        assertNotNull(calls);
        assertEquals(1, calls.intValue());
    }

    @Test
    @DisplayName("Verify linter degrades gracefully on provider errors")
    public void testLinterHandlesProviderExceptionGracefully()
    {
        System.setProperty("neodymium.ai.linter.enabled", "true");
        AiConfiguration.resetInstance();

        final LlmProvider failingProvider = new LlmProvider()
        {
            @Override
            public LlmResponse chat(final LlmRequest request)
            {
                throw new RuntimeException("Simulated LLM network timeout");
            }

            @Override
            public Set<LlmCapability> getCapabilities()
            {
                return Set.of(LlmCapability.LINTER);
            }
        };

        final LlmRegistry registry = new LlmRegistry();
        registry.registerProvider(LlmCapability.LINTER, failingProvider);

        final ExecutionContext context = new ExecutionContext(new SessionData());
        ExecutionContext.setActiveContext(context);

        final AiSession session = AiSession.mock(new SessionData(), registry, new ExecutionEventBus(), null);
        final PlaybookLinter linter = new PlaybookLinter(session);

        final List<PlaybookStep> steps = List.of(new PlaybookStep("Click Checkout"));
        final List<PlaybookLinterFinding> findings = linter.lint(steps, "Resilience Test");

        // Graceful return of empty findings
        assertTrue(findings.isEmpty());
    }
}
