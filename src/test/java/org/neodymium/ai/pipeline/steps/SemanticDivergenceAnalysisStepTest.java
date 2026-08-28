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

import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockSutState;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;

/**
 * Unit test suite for {@link SemanticDivergenceAnalysisStep}.
 * Verifies matching baseline evaluation and LLM-based DOM diff generation when states diverge.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class SemanticDivergenceAnalysisStepTest
{
    private ExecutionContext context;
    private MockLlmProvider mockLlmProvider;
    private AiSession session;

    @BeforeEach
    public void setUp()
    {
        final SessionData sessionData = new SessionData(new HashMap<>());
        mockLlmProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockLlmProvider);
        registry.registerProvider(mockLlmProvider);
        final ExecutionEventBus eventBus = new ExecutionEventBus();

        session = AiSession.mock(sessionData, registry, eventBus, new MockTargetExecutor());
        context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
    }

    /**
     * Goal: Verifies that when the baseline DOM text matches current SUT DOM text,
     * no LLM prompt is issued and a match summary is stored directly in transient context.
     */
    @Test
    public void testNoDivergenceWhenStatesMatch() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Click Login");
        step.setBaselineState("<html>Login Page</html>");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);

        final MockSutState currentState = new MockSutState("<html>Login Page</html>", "hash1");
        context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, currentState);

        final SemanticDivergenceAnalysisStep stepImpl = new SemanticDivergenceAnalysisStep();
        stepImpl.execute(context);

        final String diffSummary = (String) context.getTransientData().get(ExecutionContext.KEY_SEMANTIC_DIFF_SUMMARY);
        // Assert no layout changes summary recorded and no LLM call made
        Assertions.assertNotNull(diffSummary);
        Assertions.assertTrue(diffSummary.contains("No layout changes detected"));
    }

    /**
     * Goal: Verifies that when baseline DOM text differs from current SUT DOM text,
     * the step invokes the LLM to generate a plain-English diff summary and stores it in context.
     */
    @Test
    public void testDivergenceAnalyzedWhenStatesDiffer() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Click Login");
        step.setBaselineState("<html>Login Page Old Selector #btn-login</html>");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);

        final MockSutState currentState = new MockSutState("<html>Login Page New Selector #submit-login</html>", "hash2");
        context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, currentState);

        mockLlmProvider.addResponse(new LlmResponse("Login button ID changed from #btn-login to #submit-login.", new TokenUsage(10, 10, 20), "mock-model"));

        final SemanticDivergenceAnalysisStep stepImpl = new SemanticDivergenceAnalysisStep();
        stepImpl.execute(context);

        final String diffSummary = (String) context.getTransientData().get(ExecutionContext.KEY_SEMANTIC_DIFF_SUMMARY);
        // Assert expected LLM diff output is retrieved and saved in transient context
        Assertions.assertNotNull(diffSummary);
        Assertions.assertEquals("Login button ID changed from #btn-login to #submit-login.", diffSummary);
    }
}
