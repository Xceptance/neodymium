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
package org.neodymium.ai.tool.guard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.LocatorCandidate;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.executor.probe.LocatorProbeResult;
import org.neodymium.ai.executor.probe.ProbeBoundingRect;
import org.neodymium.ai.executor.probe.ProbeElementSummary;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SemanticIntent;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.tool.SimpleToolContext;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.neodymium.ai.tool.ToolRegistry;

/**
 * Unit tests verifying {@link QualityJudgeToolInterceptor} Journey Fidelity policies
 * and candidate locator confidence scoring.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class QualityJudgeToolInterceptorTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private QualityJudgeToolInterceptor interceptor;

    private ToolContext context;

    @BeforeEach
    public void setUp()
    {
        this.interceptor = new QualityJudgeToolInterceptor();
        this.context = new SimpleToolContext(new ToolRegistry());
    }

    @Test
    public void testJourneyFidelityRejectsDirectNavigateOnInteractiveIntent()
    {
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("url", "https://example.com/checkout");
        final ToolCall navigateCall = new ToolCall("call-1", "browser_navigate", args);

        final InterceptionVerdict verdict = this.interceptor.intercept(navigateCall, this.context, SemanticIntent.CLICK);

        Assertions.assertFalse(verdict.isAllowed());
        Assertions.assertEquals(InterceptionVerdict.Decision.REJECT, verdict.decision());
        Assertions.assertEquals(QualityJudgeToolInterceptor.JOURNEY_FIDELITY_NAVIGATE_VIOLATION, verdict.reason());
        Assertions.assertNotNull(verdict.rejectionResult());
        Assertions.assertTrue(verdict.rejectionResult().isError());
    }

    @Test
    public void testJourneyFidelityAllowsDirectNavigateOnNavigateIntent()
    {
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("url", "https://example.com/home");
        final ToolCall navigateCall = new ToolCall("call-2", "browser_navigate", args);

        final InterceptionVerdict verdict = this.interceptor.intercept(navigateCall, this.context, SemanticIntent.NAVIGATE);

        Assertions.assertTrue(verdict.isAllowed());
        Assertions.assertEquals(InterceptionVerdict.Decision.ALLOW, verdict.decision());
    }

    @Test
    public void testJourneyFidelityRejectsScriptUrlMutationOnInteractiveIntent()
    {
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("script", "window.location.href = '/cart';");
        final ToolCall scriptCall = new ToolCall("call-3", "browser_execute_script", args);

        final InterceptionVerdict verdict = this.interceptor.intercept(scriptCall, this.context, SemanticIntent.TYPE);

        Assertions.assertFalse(verdict.isAllowed());
        Assertions.assertEquals(InterceptionVerdict.Decision.REJECT, verdict.decision());
        Assertions.assertEquals(QualityJudgeToolInterceptor.JOURNEY_FIDELITY_SCRIPT_VIOLATION, verdict.reason());
    }

    @Test
    public void testJourneyFidelityAllowsBenignScriptOnInteractiveIntent()
    {
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("script", "return document.title;");
        final ToolCall scriptCall = new ToolCall("call-4", "browser_execute_script", args);

        final InterceptionVerdict verdict = this.interceptor.intercept(scriptCall, this.context, SemanticIntent.CLICK);

        Assertions.assertTrue(verdict.isAllowed());
        Assertions.assertEquals(InterceptionVerdict.Decision.ALLOW, verdict.decision());
    }

    @Test
    public void testJourneyFidelityRejectsMutationOnPureAssertionStep()
    {
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("selector", "#couponCode");
        final ToolCall clickCall = new ToolCall("call-assert-mut", "browser_click", args);

        final InterceptionVerdict verdict = this.interceptor.intercept(clickCall, this.context, SemanticIntent.ASSERT);

        Assertions.assertFalse(verdict.isAllowed());
        Assertions.assertEquals(InterceptionVerdict.Decision.REJECT, verdict.decision());
        Assertions.assertEquals(QualityJudgeToolInterceptor.ASSERTION_MUTATION_VIOLATION, verdict.reason());
    }

    @Test
    public void testJourneyFidelityAllowsMutationOnCompoundStepWithInteractiveMilestones()
    {
        final ExecutionContext execCtx = new ExecutionContext(null);
        final PlaybookStep parent = new PlaybookStep("Locate promo code:");
        parent.getSubSteps().add(new PlaybookStep("clear its content"));
        parent.getSubSteps().add(new PlaybookStep("type 'FREEGIFT'"));
        execCtx.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, parent);
        ExecutionContext.setActiveContext(execCtx);

        try
        {
            final ObjectNode args = MAPPER.createObjectNode();
            args.put("selector", "#couponCode");
            final ToolCall clickCall = new ToolCall("call-compound", "browser_click", args);

            final InterceptionVerdict verdict = this.interceptor.intercept(clickCall, this.context, SemanticIntent.ASSERT);

            Assertions.assertTrue(verdict.isAllowed());
            Assertions.assertEquals(InterceptionVerdict.Decision.ALLOW, verdict.decision());
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
        }
    }

    @Test
    public void testHighConfidenceCandidatePassesImmediately()
    {
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("selector", "#submit-button");
        final ArrayNode candidates = args.putArray("candidates");

        final ObjectNode c1 = candidates.addObject();
        c1.put("locator", "#submit-button");
        c1.put("strategy", "ID");
        c1.put("score", 0.98);

        final ToolCall call = new ToolCall("call-5", "browser_click", args);
        final InterceptionVerdict verdict = this.interceptor.intercept(call, this.context, SemanticIntent.CLICK);

        Assertions.assertTrue(verdict.isAllowed());
        Assertions.assertEquals(InterceptionVerdict.Decision.ALLOW, verdict.decision());
        Assertions.assertTrue(verdict.reason().contains("0.98"));
    }

    @Test
    public void testLowConfidenceCandidateTriggersDeliberation()
    {
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("selector", "div.btn > span");
        final ArrayNode candidates = args.putArray("candidates");

        final ObjectNode c1 = candidates.addObject();
        c1.put("locator", "div.btn > span");
        c1.put("strategy", "CLASS");
        c1.put("score", 0.72);

        final ObjectNode c2 = candidates.addObject();
        c2.put("locator", "[data-ai='submit-btn']");
        c2.put("strategy", "DATA_AI");
        c2.put("score", 0.80);

        final ToolCall call = new ToolCall("call-6", "browser_click", args);
        final InterceptionVerdict verdict = this.interceptor.intercept(call, this.context, SemanticIntent.CLICK);

        Assertions.assertTrue(verdict.isAllowed());
        Assertions.assertEquals(InterceptionVerdict.Decision.DELIBERATED, verdict.decision());
        Assertions.assertNotNull(verdict.adjustedCall());
        Assertions.assertEquals("[data-ai='submit-btn']", verdict.adjustedCall().arguments().path("selector").asText());
    }

    @Test
    public void testAmbiguousCandidatesCloseScoresTriggerDeliberation()
    {
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("selector", ".primary-button");
        final ArrayNode candidates = args.putArray("candidates");

        final ObjectNode c1 = candidates.addObject();
        c1.put("locator", ".primary-button");
        c1.put("strategy", "CLASS");
        c1.put("score", 0.88);

        final ObjectNode c2 = candidates.addObject();
        c2.put("locator", "#btn-checkout");
        c2.put("strategy", "ID");
        c2.put("score", 0.86); // diff is 0.02 < 0.15

        final ToolCall call = new ToolCall("call-7", "browser_click", args);
        final InterceptionVerdict verdict = this.interceptor.intercept(call, this.context, SemanticIntent.CLICK);

        Assertions.assertTrue(verdict.isAllowed());
        Assertions.assertEquals(InterceptionVerdict.Decision.DELIBERATED, verdict.decision());
        Assertions.assertNotNull(verdict.adjustedCall());
        // ID strategy takes precedence over CLASS during deliberation
        Assertions.assertEquals("#btn-checkout", verdict.adjustedCall().arguments().path("selector").asText());
    }

    @Test
    public void testContextCandidateLocatorsScored()
    {
        final List<LocatorCandidate> candidates = List.of(
                new LocatorCandidate("#quick-add", "ID", 0.96, "Unique ID")
        );
        this.context.setVariable("candidateLocators", candidates);

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("selector", "#quick-add");
        final ToolCall call = new ToolCall("call-8", "browser_click", args);

        final InterceptionVerdict verdict = this.interceptor.intercept(call, this.context, SemanticIntent.CLICK);

        Assertions.assertTrue(verdict.isAllowed());
        Assertions.assertEquals(InterceptionVerdict.Decision.ALLOW, verdict.decision());
    }

    @Test
    public void testLlmJudgeDeliberationRewritesSelector()
    {
        System.setProperty("neodymium.ai.judge.enabled", "true");
        System.setProperty("neodymium.ai.judge.mode", "ON_AMBIGUITY");
        try
        {
            final MockLlmProvider mockLlm = new MockLlmProvider();
            final String judgeResponse = """
                {
                  "judgment": "REFINED",
                  "chosenLocator": "#refined-button",
                  "confidence": 0.96,
                  "reasoning": "Unique stable ID attribute"
                }
                """;
            mockLlm.addResponse(new LlmResponse(judgeResponse, new TokenUsage(25, 15, 40), "mock-judge"));

            final LlmRegistry registry = new LlmRegistry();
            registry.setDefaultProvider(mockLlm);
            registry.registerProvider(mockLlm);

            final SessionData sessionData = new SessionData(new HashMap<>());
            final ExecutionEventBus eventBus = new ExecutionEventBus();
            final MockTargetExecutor targetExecutor = new MockTargetExecutor();
            final AiSession session = AiSession.mock(sessionData, registry, eventBus, targetExecutor);

            final ExecutionContext execContext = session.getExecutionContext();
            execContext.getTransientData().put(ExecutionContext.KEY_SESSION, session);
            execContext.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Click submit button");

            ExecutionContext.setActiveContext(execContext);

            final ObjectNode args = MAPPER.createObjectNode();
            args.put("selector", ".brittle-button");
            final ArrayNode candidates = args.putArray("candidates");
            final ObjectNode c1 = candidates.addObject();
            c1.put("locator", ".brittle-button");
            c1.put("strategy", "CLASS");
            c1.put("score", 0.60);

            final ToolCall call = new ToolCall("call-9", "browser_click", args);
            final InterceptionVerdict verdict = this.interceptor.intercept(call, this.context, SemanticIntent.CLICK);

            Assertions.assertTrue(verdict.isAllowed());
            Assertions.assertEquals(InterceptionVerdict.Decision.DELIBERATED, verdict.decision());
            Assertions.assertNotNull(verdict.adjustedCall());
            Assertions.assertEquals("#refined-button", verdict.adjustedCall().arguments().path("selector").asText());

            final Integer judgeCalls = (Integer) execContext.getTransientData().get(ExecutionContext.KEY_JUDGE_CALL_COUNT);
            Assertions.assertEquals(1, judgeCalls);
            final TokenUsage tokenUsage = (TokenUsage) execContext.getTransientData().get(ExecutionContext.KEY_JUDGE_TOKEN_USAGE);
            Assertions.assertNotNull(tokenUsage);
            Assertions.assertEquals(40, tokenUsage.totalTokenCount());
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
            System.clearProperty("neodymium.ai.judge.enabled");
            System.clearProperty("neodymium.ai.judge.mode");
        }
    }

    @Test
    public void testDiscussionModeFastPathPassesUniqueVisibleCandidateWithoutLlmCalls()
    {
        System.setProperty("neodymium.ai.judge.enabled", "true");
        System.setProperty("neodymium.ai.judge.mode", "DISCUSSION");
        System.setProperty("neodymium.ai.judge.discussion.fastPath", "true");
        try
        {
            final MockLlmProvider mockLlm = new MockLlmProvider();
            final LlmRegistry registry = new LlmRegistry();
            registry.setDefaultProvider(mockLlm);
            registry.registerProvider(mockLlm);

            final SessionData sessionData = new SessionData(new HashMap<>());
            final ExecutionEventBus eventBus = new ExecutionEventBus();
            final MockTargetExecutor targetExecutor = new MockTargetExecutor();
            final ProbeElementSummary summary = new ProbeElementSummary(
                    0,
                    "button",
                    "Submit",
                    true,
                    true,
                    new ProbeBoundingRect(10.0, 20.0, 100.0, 40.0)
            );
            targetExecutor.registerProbeResult("#btn-checkout",
                    LocatorProbeResult.supported("#btn-checkout", 1, List.of(summary)));

            final AiSession session = AiSession.mock(sessionData, registry, eventBus, targetExecutor);
            final ExecutionContext execContext = session.getExecutionContext();
            execContext.getTransientData().put(ExecutionContext.KEY_SESSION, session);
            execContext.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Click checkout");
            ExecutionContext.setActiveContext(execContext);

            final ObjectNode args = MAPPER.createObjectNode();
            args.put("selector", "#btn-checkout");
            final ArrayNode candidates = args.putArray("candidates");

            final ObjectNode c1 = candidates.addObject();
            c1.put("locator", "#btn-checkout");
            c1.put("strategy", "ID");
            c1.put("score", 0.88);

            final ObjectNode c2 = candidates.addObject();
            c2.put("locator", ".btn-primary");
            c2.put("strategy", "CLASS");
            c2.put("score", 0.86);

            final ToolCall call = new ToolCall("call-fastpath", "browser_click", args);
            final InterceptionVerdict verdict = this.interceptor.intercept(call, this.context, SemanticIntent.CLICK);

            Assertions.assertTrue(verdict.isAllowed());
            Assertions.assertEquals(InterceptionVerdict.Decision.ALLOW, verdict.decision());
            Assertions.assertTrue(verdict.reason().contains("Fast-path auto-approved unique locator: #btn-checkout"));
            Assertions.assertNull(execContext.getTransientData().get(ExecutionContext.KEY_JUDGE_CALL_COUNT));
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
            System.clearProperty("neodymium.ai.judge.enabled");
            System.clearProperty("neodymium.ai.judge.mode");
            System.clearProperty("neodymium.ai.judge.discussion.fastPath");
        }
    }

    @Test
    public void testDiscussionModeMultiTurnDeliberationReachesConsensus()
    {
        System.setProperty("neodymium.ai.judge.enabled", "true");
        System.setProperty("neodymium.ai.judge.mode", "DISCUSSION");
        System.setProperty("neodymium.ai.judge.discussion.maxTurns", "3");
        try
        {
            final MockLlmProvider mockLlm = new MockLlmProvider();
            final String turn1Response = """
                {
                  "status": "NEED_REFINEMENT",
                  "refinedProposal": "button.size-btn[data-ai='s']",
                  "reasoning": "Candidate is ambiguous; refine to semantic size button"
                }
                """;
            final String turn2Response = """
                {
                  "status": "APPROVED",
                  "chosenLocator": "button.size-btn[data-ai='s']",
                  "confidence": 0.98,
                  "reasoning": "Probe confirmed unique visible match for size S"
                }
                """;
            mockLlm.addResponse(new LlmResponse(turn1Response, new TokenUsage(30, 20, 50), "mock-judge"));
            mockLlm.addResponse(new LlmResponse(turn2Response, new TokenUsage(35, 15, 50), "mock-judge"));

            final LlmRegistry registry = new LlmRegistry();
            registry.setDefaultProvider(mockLlm);
            registry.registerProvider(mockLlm);

            final SessionData sessionData = new SessionData(new HashMap<>());
            final ExecutionEventBus eventBus = new ExecutionEventBus();
            final MockTargetExecutor targetExecutor = new MockTargetExecutor();
            final ProbeElementSummary summary = new ProbeElementSummary(
                    0,
                    "button",
                    "S",
                    true,
                    true,
                    new ProbeBoundingRect(50.0, 100.0, 30.0, 30.0)
            );
            targetExecutor.registerProbeResult("button.size-btn[data-ai='s']",
                    LocatorProbeResult.supported("button.size-btn[data-ai='s']", 1, List.of(summary)));

            final AiSession session = AiSession.mock(sessionData, registry, eventBus, targetExecutor);
            final ExecutionContext execContext = session.getExecutionContext();
            execContext.getTransientData().put(ExecutionContext.KEY_SESSION, session);
            execContext.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Click size S");
            ExecutionContext.setActiveContext(execContext);

            final ObjectNode args = MAPPER.createObjectNode();
            args.put("selector", ".size-option");
            final ArrayNode candidates = args.putArray("candidates");
            final ObjectNode c1 = candidates.addObject();
            c1.put("locator", ".size-option");
            c1.put("strategy", "CLASS");
            c1.put("score", 0.60);

            final ToolCall call = new ToolCall("call-multiturn", "browser_click", args);
            final InterceptionVerdict verdict = this.interceptor.intercept(call, this.context, SemanticIntent.CLICK);

            Assertions.assertTrue(verdict.isAllowed());
            Assertions.assertEquals(InterceptionVerdict.Decision.DELIBERATED, verdict.decision());
            Assertions.assertNotNull(verdict.adjustedCall());
            Assertions.assertEquals("button.size-btn[data-ai='s']", verdict.adjustedCall().arguments().path("selector").asText());

            final Integer judgeCalls = (Integer) execContext.getTransientData().get(ExecutionContext.KEY_JUDGE_CALL_COUNT);
            Assertions.assertEquals(2, judgeCalls);
            final TokenUsage tokenUsage = (TokenUsage) execContext.getTransientData().get(ExecutionContext.KEY_JUDGE_TOKEN_USAGE);
            Assertions.assertNotNull(tokenUsage);
            Assertions.assertEquals(100, tokenUsage.totalTokenCount());
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
            System.clearProperty("neodymium.ai.judge.enabled");
            System.clearProperty("neodymium.ai.judge.mode");
            System.clearProperty("neodymium.ai.judge.discussion.maxTurns");
        }
    }

    @Test
    public void testDiscussionModeRefinedToExistingVerifiedCandidate()
    {
        System.setProperty("neodymium.ai.judge.enabled", "true");
        System.setProperty("neodymium.ai.judge.mode", "DISCUSSION");
        try
        {
            final MockLlmProvider mockLlm = new MockLlmProvider();
            final String judgeResponse = """
                {
                  "status": "REFINED",
                  "chosenLocator": "[data-ai='submit-btn']",
                  "confidence": 0.95,
                  "reasoning": "Unique data-ai attribute candidate verified"
                }
                """;
            mockLlm.addResponse(new LlmResponse(judgeResponse, new TokenUsage(25, 15, 40), "mock-judge"));

            final LlmRegistry registry = new LlmRegistry();
            registry.setDefaultProvider(mockLlm);
            registry.registerProvider(mockLlm);

            final SessionData sessionData = new SessionData(new HashMap<>());
            final ExecutionEventBus eventBus = new ExecutionEventBus();
            final MockTargetExecutor targetExecutor = new MockTargetExecutor();
            final ProbeElementSummary summary = new ProbeElementSummary(
                    0,
                    "button",
                    "Submit",
                    true,
                    true,
                    new ProbeBoundingRect(10.0, 10.0, 80.0, 30.0)
            );
            targetExecutor.registerProbeResult("[data-ai='submit-btn']",
                    LocatorProbeResult.supported("[data-ai='submit-btn']", 1, List.of(summary)));

            final AiSession session = AiSession.mock(sessionData, registry, eventBus, targetExecutor);
            final ExecutionContext execContext = session.getExecutionContext();
            execContext.getTransientData().put(ExecutionContext.KEY_SESSION, session);
            execContext.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Submit form");
            ExecutionContext.setActiveContext(execContext);

            final ObjectNode args = MAPPER.createObjectNode();
            args.put("selector", "div.btn > span");
            final ArrayNode candidates = args.putArray("candidates");
            final ObjectNode c1 = candidates.addObject();
            c1.put("locator", "div.btn > span");
            c1.put("strategy", "CLASS");
            c1.put("score", 0.70);

            final ObjectNode c2 = candidates.addObject();
            c2.put("locator", "[data-ai='submit-btn']");
            c2.put("strategy", "DATA_AI");
            c2.put("score", 0.80);

            final ToolCall call = new ToolCall("call-refined", "browser_click", args);
            final InterceptionVerdict verdict = this.interceptor.intercept(call, this.context, SemanticIntent.CLICK);

            Assertions.assertTrue(verdict.isAllowed());
            Assertions.assertEquals(InterceptionVerdict.Decision.DELIBERATED, verdict.decision());
            Assertions.assertNotNull(verdict.adjustedCall());
            Assertions.assertEquals("[data-ai='submit-btn']", verdict.adjustedCall().arguments().path("selector").asText());
            Assertions.assertEquals(1, (Integer) execContext.getTransientData().get(ExecutionContext.KEY_JUDGE_CALL_COUNT));
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
            System.clearProperty("neodymium.ai.judge.enabled");
            System.clearProperty("neodymium.ai.judge.mode");
        }
    }

    @Test
    public void testDiscussionModeTurnExhaustionFallsBackToLastProposal()
    {
        System.setProperty("neodymium.ai.judge.enabled", "true");
        System.setProperty("neodymium.ai.judge.mode", "DISCUSSION");
        System.setProperty("neodymium.ai.judge.discussion.maxTurns", "2");
        try
        {
            final MockLlmProvider mockLlm = new MockLlmProvider();
            final String turn1Response = """
                {
                  "status": "NEED_REFINEMENT",
                  "refinedProposal": "div.attempt-one",
                  "reasoning": "Try attempt one"
                }
                """;
            final String turn2Response = """
                {
                  "status": "NEED_REFINEMENT",
                  "refinedProposal": "div.attempt-two",
                  "reasoning": "Try attempt two"
                }
                """;
            mockLlm.addResponse(new LlmResponse(turn1Response, new TokenUsage(20, 10, 30), "mock-judge"));
            mockLlm.addResponse(new LlmResponse(turn2Response, new TokenUsage(25, 10, 35), "mock-judge"));

            final LlmRegistry registry = new LlmRegistry();
            registry.setDefaultProvider(mockLlm);
            registry.registerProvider(mockLlm);

            final SessionData sessionData = new SessionData(new HashMap<>());
            final ExecutionEventBus eventBus = new ExecutionEventBus();
            final MockTargetExecutor targetExecutor = new MockTargetExecutor();
            final AiSession session = AiSession.mock(sessionData, registry, eventBus, targetExecutor);

            final ExecutionContext execContext = session.getExecutionContext();
            execContext.getTransientData().put(ExecutionContext.KEY_SESSION, session);
            execContext.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Click something");
            ExecutionContext.setActiveContext(execContext);

            final ObjectNode args = MAPPER.createObjectNode();
            args.put("selector", ".initial-selector");
            final ArrayNode candidates = args.putArray("candidates");
            final ObjectNode c1 = candidates.addObject();
            c1.put("locator", ".initial-selector");
            c1.put("strategy", "CLASS");
            c1.put("score", 0.50);

            final ToolCall call = new ToolCall("call-exhaust", "browser_click", args);
            final InterceptionVerdict verdict = this.interceptor.intercept(call, this.context, SemanticIntent.CLICK);

            Assertions.assertTrue(verdict.isAllowed());
            Assertions.assertEquals(InterceptionVerdict.Decision.DELIBERATED, verdict.decision());
            Assertions.assertNotNull(verdict.adjustedCall());
            Assertions.assertEquals("div.attempt-two", verdict.adjustedCall().arguments().path("selector").asText());
            Assertions.assertTrue(verdict.reason().contains("Max discussion turns (2) reached; proceeded with last proposal: div.attempt-two"));

            final Integer judgeCalls = (Integer) execContext.getTransientData().get(ExecutionContext.KEY_JUDGE_CALL_COUNT);
            Assertions.assertEquals(2, judgeCalls);
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
            System.clearProperty("neodymium.ai.judge.enabled");
            System.clearProperty("neodymium.ai.judge.mode");
            System.clearProperty("neodymium.ai.judge.discussion.maxTurns");
        }
    }
}
