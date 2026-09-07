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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.LocatorCandidate;
import org.neodymium.ai.model.SemanticIntent;
import org.neodymium.ai.tool.SimpleToolContext;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.neodymium.ai.tool.ToolRegistry;

import java.util.List;

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
}
