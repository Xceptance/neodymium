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
package org.neodymium.ai.pipeline.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.action.LocatorCandidate;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockSutState;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.session.AiSession;
import org.neodymium.util.Neodymium;

/**
 * Unit tests validating {@link QualityJudgeStep} execution triggers, ambiguity thresholding,
 * and decision overrides.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class QualityJudgeStepTest
{
    private MockTargetExecutor executor;
    private MockLlmProvider mockLlm;
    private LlmRegistry registry;
    private ExecutionEventBus eventBus;

    /**
     * Constructs a default QualityJudgeStepTest instance.
     */
    public QualityJudgeStepTest()
    {
    }

    /**
     * Setup test fixtures before each test.
     */
    @BeforeEach
    public void setup()
    {
        executor = new MockTargetExecutor();
        mockLlm = new MockLlmProvider();
        registry = new LlmRegistry();
        registry.registerProvider(LlmCapability.TEXT_ONLY, mockLlm);
        registry.setDefaultProvider(mockLlm);
        eventBus = new ExecutionEventBus();

        Neodymium.getData().put("neodymium.ai.judge.enabled", "true");
        Neodymium.getData().put("neodymium.ai.judge.mode", "ON_AMBIGUITY");
        AiConfiguration.resetInstance();
    }

    /**
     * Tear down test fixtures.
     */
    @AfterEach
    public void tearDown()
    {
        Neodymium.getData().remove("neodymium.ai.judge.enabled");
        Neodymium.getData().remove("neodymium.ai.judge.mode");
        AiConfiguration.resetInstance();
        ExecutionContext.setActiveContext(null);
    }

    /**
     * Verifies that when Candidate 1 has high score (>= 0.95), the judge is skipped.
     *
     * @throws PipelineException on unexpected error
     */
    @Test
    public void testOnAmbiguitySkipsHighConfidenceTopCandidate() throws PipelineException
    {
        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        final Action action = new Action("TYPE", "#firstName", Collections.emptyList(), "Mario", "Enter first name");
        action.setCandidateLocators(List.of(
            new LocatorCandidate("#firstName", "ID", 1.00, "Unique ID"),
            new LocatorCandidate("input[name='firstName']", "ATTRIBUTE", 0.90, "Name attribute")
        ));

        context.getTransientData().put(ExecutionContext.KEY_LAST_LLM_RESULT, List.of(action));
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Enter 'Mario' as first name");
        context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, new MockSutState("=== DOM ===", "hash123"));
        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);

        final QualityJudgeStep step = new QualityJudgeStep();
        step.execute(context);

        // Mock LLM should have received 0 requests because the judge was bypassed
        assertNull(mockLlm.getLastRequest());
        assertEquals("#firstName", action.getTarget());
    }

    /**
     * Verifies that when Candidate 1 has low score (< 0.85), the judge is triggered.
     *
     * @throws PipelineException on unexpected error
     */
    @Test
    public void testOnAmbiguityTriggersOnLowScoreTopCandidate() throws PipelineException
    {
        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        final Action action = new Action("CLICK", ".btn-12345", Collections.emptyList(), "", "Click submit");
        action.setCandidateLocators(List.of(
            new LocatorCandidate(".btn-12345", "CLASS", 0.60, "Fragile dynamic class")
        ));

        context.getTransientData().put(ExecutionContext.KEY_LAST_LLM_RESULT, List.of(action));
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Click submit");
        context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, new MockSutState("=== DOM ===", "hash123"));
        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);

        mockLlm.addResponse(new LlmResponse("""
            {
              "judgment": "APPROVED",
              "chosenLocator": ".btn-12345",
              "confidence": 0.90,
              "reasoning": "Keep candidate"
            }
            """, null, "mock-model"));

        final QualityJudgeStep step = new QualityJudgeStep();
        step.execute(context);

        assertNotNull(mockLlm.getLastRequest());
    }

    /**
     * Verifies that when Candidate 1 is mid-tier (0.85 <= score < 0.95) and diff < 0.15,
     * the judge is triggered to resolve the ambiguity.
     *
     * @throws PipelineException on unexpected error
     */
    @Test
    public void testOnAmbiguityTriggersOnCloseMidTierContest() throws PipelineException
    {
        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        final Action action = new Action("CLICK", ".products-grid .product-quick-add", Collections.emptyList(), "", "Add to cart");
        action.setCandidateLocators(List.of(
            new LocatorCandidate(".products-grid .product-quick-add", "CLASS", 0.85, "Scoped class"),
            new LocatorCandidate("button.product-quick-add", "CLASS", 0.80, "Class name")
        ));

        context.getTransientData().put(ExecutionContext.KEY_LAST_LLM_RESULT, List.of(action));
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Add to cart");
        context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, new MockSutState("=== DOM ===", "hash123"));
        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);

        mockLlm.addResponse(new LlmResponse("""
            {
              "judgment": "REFINED",
              "chosenLocator": ".products-grid .product-quick-add",
              "confidence": 0.95,
              "reasoning": "Scoped selector is more specific"
            }
            """, null, "mock-model"));

        final QualityJudgeStep step = new QualityJudgeStep();
        step.execute(context);

        assertNotNull(mockLlm.getLastRequest());
        assertEquals(".products-grid .product-quick-add", action.getTarget());
    }

    /**
     * Verifies that when Candidate 1 is mid-tier (0.85 <= score < 0.95) and diff >= 0.15,
     * the judge is skipped because there is a clear winner.
     *
     * @throws PipelineException on unexpected error
     */
    @Test
    public void testOnAmbiguitySkipsClearMidTierWinner() throws PipelineException
    {
        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        final Action action = new Action("CLICK", ".btn-primary", Collections.emptyList(), "", "Click submit");
        action.setCandidateLocators(List.of(
            new LocatorCandidate(".btn-primary", "CLASS", 0.85, "Primary button class"),
            new LocatorCandidate("[data-ai='xc123']", "DATA_AI", 0.60, "Automation fallback")
        ));

        context.getTransientData().put(ExecutionContext.KEY_LAST_LLM_RESULT, List.of(action));
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Click submit");
        context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, new MockSutState("=== DOM ===", "hash123"));
        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);

        final QualityJudgeStep step = new QualityJudgeStep();
        step.execute(context);

        assertNull(mockLlm.getLastRequest());
        assertEquals(".btn-primary", action.getTarget());
    }
}
