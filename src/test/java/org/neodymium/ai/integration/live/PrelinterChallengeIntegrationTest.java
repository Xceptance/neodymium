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
package org.neodymium.ai.integration.live;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestMethodOrder;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiLinter;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.playbook.linter.LinterCategory;
import org.neodymium.ai.playbook.linter.PlaybookLinterFinding;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Integration test exercising the upfront Playbook Pre-Flight Linter against a dedicated
 * Aura test fixture that challenges all 8 universal quality rules.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook(value = "playbooks/integration/PrelinterChallengeTest.yaml", recordingDirectory = "target/playbooks/integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PrelinterChallengeIntegrationTest extends BaseAiTest
{
    /**
     * Constructs a default PrelinterChallengeIntegrationTest instance.
     */
    public PrelinterChallengeIntegrationTest()
    {
    }

    /**
     * Sets up dynamic target page URL before each test execution.
     */
    @BeforeEach
    public void setup()
    {
        Neodymium.getData().put("prelinter.test.url", String.format("http://localhost:%d/PrelinterChallengeTest/index.html", server.getPort()));
    }

    /**
     * Validates that the pre-flight linter runs upfront when enabled, detects findings across
     * universal rules, and records token usage metrics.
     *
     * @param session the active AI session
     */
    @Order(1)
    @AiLinter(true)
    @AiMode(ExecutionMode.LLM_ONLY)
    @AiDataSet("prelinter-all-rules")
    @AiPlaybook
    public void testPrelinterEnabledChallengesAllRules(final AiSession session)
    {
        final ExecutionContext context = session.getExecutionContext();
        assertNotNull(context);

        @SuppressWarnings("unchecked")
        final List<PlaybookLinterFinding> findings = (List<PlaybookLinterFinding>) context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK_LINTER_FINDINGS);
        assertNotNull(findings, "Expected linter findings to be stored in execution context");
        assertFalse(findings.isEmpty(), "Expected linter findings to be detected for challenged playbook");

        // Verify categories are detected
        final Set<LinterCategory> detectedCategories = findings.stream()
            .map(PlaybookLinterFinding::category)
            .collect(Collectors.toSet());

        // Verify all 8 universal rule categories are challenged and detected
        assertTrue(detectedCategories.contains(LinterCategory.STEP_SPLITTING_CANDIDATE), "Expected STEP_SPLITTING_CANDIDATE finding");
        assertTrue(detectedCategories.contains(LinterCategory.MISSING_VISUAL_TAG), "Expected MISSING_VISUAL_TAG finding");
        assertTrue(detectedCategories.contains(LinterCategory.AMBIGUOUS_AFFORDANCE), "Expected AMBIGUOUS_AFFORDANCE finding");
        assertTrue(detectedCategories.contains(LinterCategory.VAGUE_TARGET), "Expected VAGUE_TARGET finding");
        assertTrue(detectedCategories.contains(LinterCategory.VAGUE_VERIFICATION), "Expected VAGUE_VERIFICATION finding");
        assertTrue(detectedCategories.contains(LinterCategory.DANGLING_ANAPHORA), "Expected DANGLING_ANAPHORA finding");
        assertTrue(detectedCategories.contains(LinterCategory.TEMPORAL_FLOW_ANOMALY), "Expected TEMPORAL_FLOW_ANOMALY finding");
        assertTrue(detectedCategories.contains(LinterCategory.HARDCODED_VOLATILE_DATA), "Expected HARDCODED_VOLATILE_DATA finding");
        assertTrue(detectedCategories.size() >= 8, "Expected at least 8 distinct linter categories detected");

        // Verify token accounting
        final TokenUsage usage = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_LINTER_TOKEN_USAGE);
        if (usage != null)
        {
            assertTrue(usage.totalTokenCount() > 0);
        }

        final Integer calls = (Integer) context.getTransientData().get(ExecutionContext.KEY_LINTER_CALL_COUNT);
        if (calls != null)
        {
            assertTrue(calls >= 1);
        }
    }

    /**
     * Validates that the pre-flight linter is bypassed when annotated with {@code @AiLinter(false)}.
     *
     * @param session the active AI session
     */
    @Order(2)
    @AiLinter(false)
    @AiMode(ExecutionMode.LLM_ONLY)
    @AiDataSet("prelinter-all-rules")
    @AiPlaybook
    public void testPrelinterDisabledBypassesExecution(final AiSession session)
    {
        final ExecutionContext context = session.getExecutionContext();
        assertNotNull(context);

        @SuppressWarnings("unchecked")
        final List<PlaybookLinterFinding> findings = (List<PlaybookLinterFinding>) context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK_LINTER_FINDINGS);
        assertTrue(findings == null || findings.isEmpty());

        final Integer calls = (Integer) context.getTransientData().get(ExecutionContext.KEY_LINTER_CALL_COUNT);
        assertNull(calls);
    }

    /**
     * Validates that the pre-flight linter is bypassed during strict replay execution.
     *
     * @param session the active AI session
     */
    @Order(3)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("prelinter-all-rules")
    @AiPlaybook(recordingMethod = "testPrelinterEnabledChallengesAllRules")
    public void testPrelinterBypassedInReplayStrict(final AiSession session)
    {
        final ExecutionContext context = session.getExecutionContext();
        assertNotNull(context);

        @SuppressWarnings("unchecked")
        final List<PlaybookLinterFinding> findings = (List<PlaybookLinterFinding>) context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK_LINTER_FINDINGS);
        assertTrue(findings == null || findings.isEmpty(), "Expected no linter findings in replay mode");

        final Integer calls = (Integer) context.getTransientData().get(ExecutionContext.KEY_LINTER_CALL_COUNT);
        assertNull(calls, "Expected zero linter LLM calls in replay mode");
    }
}
