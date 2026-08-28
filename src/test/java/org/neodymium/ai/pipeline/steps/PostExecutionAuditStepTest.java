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
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;

/**
 * Unit test suite for {@link PostExecutionAuditStep}.
 * Verifies post-session audit delegation to {@link org.neodymium.ai.audit.ExecutionAuditor}.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class PostExecutionAuditStepTest
{
    private ExecutionContext context;
    private AiSession session;

    @BeforeEach
    public void setUp()
    {
        final SessionData sessionData = new SessionData(new HashMap<>());
        final MockLlmProvider mockLlmProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockLlmProvider);
        final ExecutionEventBus eventBus = new ExecutionEventBus();

        session = AiSession.mock(sessionData, registry, eventBus, new MockTargetExecutor());
        context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
    }

    /**
     * Goal: Verifies that {@link PostExecutionAuditStep#execute} delegates auditing to
     * {@link org.neodymium.ai.audit.ExecutionAuditor} and records `auditPassed` and `auditFindings` keys into transient context.
     */
    @Test
    public void testPostExecutionAuditPopulatesTransientData() throws Exception
    {
        context.getTransientData().put(ExecutionContext.KEY_VISUAL_RCA_SUMMARY, "Visual anomaly on banner");

        final PostExecutionAuditStep auditStep = new PostExecutionAuditStep();
        auditStep.execute(context);

        final Boolean auditPassed = (Boolean) context.getTransientData().get("auditPassed");
        @SuppressWarnings("unchecked")
        final List<String> findings = (List<String>) context.getTransientData().get("auditFindings");

        // Assert auditor findings and pass/fail flags are recorded into transient map
        Assertions.assertNotNull(auditPassed);
        Assertions.assertFalse(auditPassed);
        Assertions.assertNotNull(findings);
        Assertions.assertEquals(1, findings.size());
    }
}
