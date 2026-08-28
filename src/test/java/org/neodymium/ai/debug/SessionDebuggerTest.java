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
package org.neodymium.ai.debug;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;

/**
 * Unit test suite for {@link SessionDebugger}.
 * Verifies retrieval of RCA diagnoses, semantic diff summaries, token usage metrics breakdown,
 * and debug snapshot report formatting.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class SessionDebuggerTest
{
    private AiSession session;
    private SessionDebugger debugger;

    @BeforeEach
    public void setUp()
    {
        final SessionData sessionData = new SessionData(new HashMap<>());
        final MockLlmProvider mockLlmProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockLlmProvider);
        final ExecutionEventBus eventBus = new ExecutionEventBus();

        session = AiSession.mock(sessionData, registry, eventBus, new MockTargetExecutor());
        debugger = new SessionDebugger(session);
    }

    /**
     * Goal: Verifies that {@link SessionDebugger#getLastRcaSummary()} and
     * {@link SessionDebugger#getLastSemanticDiffSummary()} correctly extract non-empty summaries from context.
     */
    @Test
    public void testDebuggerRetrievesRcaAndDiffSummaries()
    {
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_VISUAL_RCA_SUMMARY, "Cookie consent overlay active");
        context.getTransientData().put(ExecutionContext.KEY_SEMANTIC_DIFF_SUMMARY, "Button selector changed");

        // Assert summaries match stored transient context entries
        Assertions.assertEquals("Cookie consent overlay active", debugger.getLastRcaSummary());
        Assertions.assertEquals("Button selector changed", debugger.getLastSemanticDiffSummary());
    }

    /**
     * Goal: Verifies that {@link SessionDebugger#getTokenUsageBreakdown()} aggregates standard
     * and verification token consumption metrics into a structured category map.
     */
    @Test
    public void testDebuggerExtractsTokenUsageBreakdown()
    {
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_STANDARD_TOKEN_USAGE, new TokenUsage(100, 50, 150));
        context.getTransientData().put(ExecutionContext.KEY_VERIFICATION_TOKEN_USAGE, new TokenUsage(200, 100, 300));
        context.getTransientData().put(ExecutionContext.KEY_RCA_TOKEN_USAGE, new TokenUsage(400, 100, 500));

        final Map<String, TokenUsage> breakdown = debugger.getTokenUsageBreakdown();
        // Assert token map entries and total token metrics
        Assertions.assertEquals(3, breakdown.size());
        Assertions.assertEquals(150, breakdown.get("standard").totalTokenCount());
        Assertions.assertEquals(300, breakdown.get("verification").totalTokenCount());
        Assertions.assertEquals(500, breakdown.get("rca").totalTokenCount());
    }

    /**
     * Goal: Verifies that {@link SessionDebugger#dumpSessionReport()} creates a formatted human-readable diagnostic report string.
     */
    @Test
    public void testDumpSessionReportContainsReportHeaders()
    {
        final String report = debugger.dumpSessionReport();
        // Assert report is non-null and contains section header
        Assertions.assertNotNull(report);
        Assertions.assertTrue(report.contains("AI Session Debug Report"));
    }
}
