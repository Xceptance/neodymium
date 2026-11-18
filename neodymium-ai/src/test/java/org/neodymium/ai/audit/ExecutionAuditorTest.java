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
package org.neodymium.ai.audit;

import java.util.HashMap;
import java.util.List;

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
 * Unit test suite for {@link ExecutionAuditor}.
 * Verifies clean pass audits, visual RCA finding flags, token consumption threshold warnings,
 * and verification warning detection.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class ExecutionAuditorTest
{
    private AiSession session;
    private ExecutionAuditor auditor;

    @BeforeEach
    public void setUp()
    {
        final SessionData sessionData = new SessionData(new HashMap<>());
        final MockLlmProvider mockLlmProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockLlmProvider);
        final ExecutionEventBus eventBus = new ExecutionEventBus();

        session = AiSession.mock(sessionData, registry, eventBus, new MockTargetExecutor());
        auditor = new ExecutionAuditor();
    }

    /**
     * Goal: Verifies that an AI session with no recorded RCA findings, low token consumption,
     * and zero verification warnings passes the health audit cleanly.
     */
    @Test
    public void testCleanSessionPassesAudit()
    {
        final boolean passed = auditor.audit(session);
        // Assert clean session passes audit without findings
        Assertions.assertTrue(passed);
        Assertions.assertTrue(auditor.getAuditFindings().isEmpty());
    }

    /**
     * Goal: Verifies that an AI session containing visual RCA failure diagnostics
     * fails the overall audit check and records the finding description.
     */
    @Test
    public void testSessionWithRcaFindingsFailsAudit()
    {
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_VISUAL_RCA_SUMMARY, "Element click intercepted by pop-up");

        final boolean passed = auditor.audit(session);
        // Assert session with RCA findings fails audit and includes finding message
        Assertions.assertFalse(passed);
        Assertions.assertEquals(1, auditor.getAuditFindings().size());
        Assertions.assertTrue(auditor.getAuditFindings().get(0).contains("Element click intercepted"));
    }

    /**
     * Goal: Verifies that token usage exceeding 100,000 tokens records a soft warning finding
     * without failing the overall execution state.
     */
    @Test
    public void testSessionWithHighTokenUsageRecordsWarning()
    {
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_STANDARD_TOKEN_USAGE, new TokenUsage(80000, 30000, 110000));

        final boolean passed = auditor.audit(session);
        // Token warning recorded without failing overall test state
        Assertions.assertTrue(passed);
        Assertions.assertEquals(1, auditor.getAuditFindings().size());
        Assertions.assertTrue(auditor.getAuditFindings().get(0).contains("High token consumption"));
    }

    /**
     * Goal: Verifies that an AI session containing recorded verification warnings
     * fails the audit check and logs warning finding count.
     */
    @Test
    public void testSessionWithVerificationWarningsFailsAudit()
    {
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put("verificationWarnings", List.of("Warning: expected page title match failed"));

        final boolean passed = auditor.audit(session);
        // Assert audit fails when verification warnings are present
        Assertions.assertFalse(passed);
        Assertions.assertEquals(1, auditor.getAuditFindings().size());
        Assertions.assertTrue(auditor.getAuditFindings().get(0).contains("Verification warnings present"));
    }
}
