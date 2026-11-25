/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.replay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.model.DomFeatureVector;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.SimpleToolContext;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.neodymium.ai.tool.ToolDefinition;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;
import org.neodymium.ai.tool.browser.BrowserToolProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests validating {@link PlaybookToolReplayer}, PlaybookStep tool call serialization,
 * offline replay execution without LLM calls, and Tier 2 sub-millisecond similarity healing.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class PlaybookToolReplayTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolRegistry registry;
    private SimpleToolContext context;
    private List<ToolCall> executedCalls;

    @BeforeEach
    public void setUp()
    {
        this.registry = new ToolRegistry();
        this.context = new SimpleToolContext(this.registry);
        this.executedCalls = new ArrayList<>();

        // Register mock browser tools
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("browser_click", "Clicks target", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                return ToolResult.success(call.callId(), "Clicked " + call.arguments().path("target").asText());
            }
        });

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("browser_type", "Types text", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                return ToolResult.success(call.callId(), "Typed into " + call.arguments().path("target").asText());
            }
        });
    }

    @Test
    public void testPlaybookStepSerializationWithToolCalls() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Click checkout and enter email");
        final ObjectNode clickArgs = MAPPER.createObjectNode().put("target", "#checkout-btn");
        final ObjectNode typeArgs = MAPPER.createObjectNode().put("target", "#email-input").put("text", "test@example.com");

        step.addToolCall(new ToolCall("call-1", "browser_click", clickArgs));
        step.addToolCall(new ToolCall("call-2", "browser_type", typeArgs));

        final String json = MAPPER.writeValueAsString(step);
        Assertions.assertTrue(json.contains("\"toolCalls\""));
        Assertions.assertTrue(json.contains("browser_click"));
        Assertions.assertTrue(json.contains("#checkout-btn"));

        final PlaybookStep deserialized = MAPPER.readValue(json, PlaybookStep.class);
        Assertions.assertEquals(2, deserialized.getToolCalls().size());
        Assertions.assertEquals("browser_click", deserialized.getToolCalls().get(0).toolName());
        Assertions.assertEquals("#checkout-btn", deserialized.getToolCalls().get(0).arguments().path("target").asText());
        Assertions.assertEquals("browser_type", deserialized.getToolCalls().get(1).toolName());
    }

    @Test
    public void testTransparentLegacyActionsDeserialization() throws Exception
    {
        final String legacyJson = """
                {
                  "instruction": "Click old button",
                  "actions": [
                    {
                      "type": "CLICK",
                      "target": "#old-button"
                    },
                    {
                      "type": "TYPE",
                      "target": "#query",
                      "value": ["search term"]
                    }
                  ]
                }
                """;

        final PlaybookStep step = MAPPER.readValue(legacyJson, PlaybookStep.class);
        Assertions.assertEquals(2, step.getActions().size());

        // Transparent synthesis into ToolCalls
        final List<ToolCall> synthesized = step.getToolCalls();
        Assertions.assertEquals(2, synthesized.size());
        Assertions.assertEquals("click", synthesized.get(0).toolName());
        Assertions.assertEquals("#old-button", synthesized.get(0).arguments().path("target").asText());
        Assertions.assertEquals("fill", synthesized.get(1).toolName());
        Assertions.assertEquals("#query", synthesized.get(1).arguments().path("target").asText());
        Assertions.assertEquals("search term", synthesized.get(1).arguments().path("text").asText());
    }

    @Test
    public void testOfflineReplayWithoutLlmCalls() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Click checkout");
        final ObjectNode clickArgs = MAPPER.createObjectNode().put("target", "#checkout-btn");
        step.addToolCall(new ToolCall("call-10", "browser_click", clickArgs));

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.SUCCESS, step.getStatus());
        Assertions.assertEquals(1, this.executedCalls.size());
        Assertions.assertEquals("browser_click", this.executedCalls.get(0).toolName());
        Assertions.assertEquals("#checkout-btn", this.executedCalls.get(0).arguments().path("target").asText());
    }

    @Test
    public void testOfflineReplaySimilarityHealingWithDomFeatureVector() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Click submit");

        // Recorded with old selector: button.old-class
        final ObjectNode clickArgs = MAPPER.createObjectNode().put("target", "button.old-class");
        step.addToolCall(new ToolCall("call-20", "browser_click", clickArgs));

        // Companion recorded Action with DomFeatureVector
        final DomFeatureVector recorded = new DomFeatureVector(
                "button",
                "Submit Order",
                Set.of("old-class", "btn"),
                Map.of("type", "submit", "name", "order"),
                "button",
                "Submit Order",
                "form",
                0
        );
        final Action act = new Action("CLICK", "button.old-class", List.of(), "Submit", "");
        act.setDomFeatureVector(recorded);
        step.setActions(List.of(act));

        // Live page candidates: selector shifted to button#submit-order (different class and ID, but same tag and text)
        final DomFeatureVector liveCandidate = new DomFeatureVector(
                "button",
                "Submit Order",
                Set.of("new-redesigned-btn"),
                Map.of("id", "submit-order", "type", "submit"),
                "button",
                "Submit Order",
                "form",
                0
        );

        this.context.setVariable("liveCandidates", List.of(liveCandidate));

        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final List<ActionExecutedEvent> actionEvents = new ArrayList<>();
        eventBus.registerListener(event ->
        {
            if (event instanceof ActionExecutedEvent aee)
            {
                actionEvents.add(aee);
            }
        });
        final AiSession session = AiSession.mock(
                ExecutionMode.REPLAY_WITH_HEALING,
                new SessionData(Map.of()),
                new LlmRegistry(),
                eventBus,
                (TargetExecutor) null);
        this.context.setVariable("neodymium.session", session);

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        Assertions.assertEquals(1, this.executedCalls.size());
        // Target was healed to the live candidate selector #submit-order!
        Assertions.assertEquals("#submit-order", this.executedCalls.get(0).arguments().path("target").asText());

        Assertions.assertEquals(1, actionEvents.size(), "ActionExecutedEvent must be dispatched");
        final ActionExecutedEvent aee = actionEvents.get(0);
        Assertions.assertTrue(aee.isHealed(), "ActionExecutedEvent must be marked as healed");
        Assertions.assertEquals("button.old-class", aee.getAction().getTarget(), "Canonical target must be recorded selector");
        Assertions.assertEquals("#submit-order", aee.getResolvedAction().getTarget(), "Resolved target must be healed selector");
        Assertions.assertEquals("button.old-class", aee.getResolvedAction().getExpectedTarget(), "Expected target must be recorded selector");
    }

    @Test
    public void testTier2OfflineVisualDHashMatchingForIconOnlyElements() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Click favorite heart icon");

        // Recorded icon-only element with visual dHash and no text
        final ObjectNode clickArgs = MAPPER.createObjectNode().put("target", ".icon-favorite-old");
        step.addToolCall(new ToolCall("call-30", "browser_click", clickArgs));

        final DomFeatureVector recordedIcon = new DomFeatureVector(
                "button",
                "",
                Set.of("icon-btn", "heart-icon"),
                Map.of("type", "button"),
                "button",
                "",
                "div",
                1,
                300,
                50,
                32,
                32,
                "0011223344556677",
                ""
        );
        final Action act = new Action("CLICK", ".icon-favorite-old", List.of(), "Favorite", "");
        act.setDomFeatureVector(recordedIcon);
        step.setActions(List.of(act));

        // Live candidate: refactored markup, matching visual dHash and aspect ratio
        final DomFeatureVector liveIcon = new DomFeatureVector(
                "button",
                "",
                Set.of("favorite-btn"),
                Map.of("id", "favorite-heart", "type", "button"),
                "button",
                "",
                "div",
                1,
                305,
                52,
                32,
                32,
                "0011223344556677",
                ""
        );

        this.context.setVariable("liveCandidates", List.of(liveIcon));

        // Warm-up call to eliminate JVM cold class-loading jitter from micro-benchmark
        PlaybookToolReplayer.replayStep(step, this.registry, this.context);
        this.executedCalls.clear();

        // Use a fresh unhealed step for the timed benchmark so offline healing actually executes
        final PlaybookStep timedStep = new PlaybookStep("Click favorite heart icon");
        timedStep.addToolCall(new ToolCall("call-30", "browser_click", MAPPER.createObjectNode().put("target", ".icon-favorite-old")));
        final Action timedAct = new Action("CLICK", ".icon-favorite-old", List.of(), "Favorite", "");
        timedAct.setDomFeatureVector(recordedIcon);
        timedStep.setActions(List.of(timedAct));

        final long start = System.nanoTime();
        final ToolResult result = PlaybookToolReplayer.replayStep(timedStep, this.registry, this.context);
        final long durationMs = (System.nanoTime() - start) / 1_000_000;

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, timedStep.getStatus());
        Assertions.assertEquals(1, this.executedCalls.size());
        Assertions.assertEquals("#favorite-heart", this.executedCalls.get(0).arguments().path("target").asText());
        // Sub-millisecond CPU execution
        Assertions.assertTrue(durationMs < 50, "Tier 2 offline matching should execute in milliseconds, took: " + durationMs + "ms");
    }

    @Test
    public void testOfflineReplayPreservesTextQualificationWhenCandidateHasTextAndNoId() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Click country item");

        // Recorded action with old class selector but containing text qualification
        final ObjectNode clickArgs = MAPPER.createObjectNode().put("target", "li.old-country-item:has-text(\"Germany\")");
        step.addToolCall(new ToolCall("call-40", "browser_click", clickArgs));

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "li",
                "Germany",
                Set.of("old-country-item"),
                Map.of("role", "option"),
                "li",
                "Germany",
                "ul",
                0
        );
        final Action act = new Action("CLICK", "li.old-country-item:has-text(\"Germany\")", List.of(), "Germany", "");
        act.setDomFeatureVector(recordedVector);
        step.setActions(List.of(act));

        // Live page candidate: class shifted to country-item, but still has text "Germany" and no ID
        final DomFeatureVector liveCandidate = new DomFeatureVector(
                "li",
                "Germany",
                Set.of("country-item"),
                Map.of("role", "option"),
                "li",
                "Germany",
                "ul",
                0
        );

        this.context.setVariable("liveCandidates", List.of(liveCandidate));

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        Assertions.assertEquals(1, this.executedCalls.size());
        // Target was healed to the text-qualified selector li.country-item:has-text("Germany")!
        Assertions.assertEquals("li.country-item:has-text(\"Germany\")", this.executedCalls.get(0).arguments().path("target").asText());
    }

    @Test
    public void testOfflineReplayDoesNotTriggerHealingWhenSelectorMatchesCandidate() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Click Germany");

        final ObjectNode clickArgs = MAPPER.createObjectNode().put("target", "li.country-item:has-text(\"Germany\")");
        step.addToolCall(new ToolCall("call-41", "browser_click", clickArgs));

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "li",
                "Germany",
                Set.of("country-item"),
                Map.of(),
                "li",
                "Germany",
                "ul",
                0
        );
        final Action act = new Action("CLICK", "li.country-item:has-text(\"Germany\")", List.of(), "Germany", "");
        act.setDomFeatureVector(recordedVector);
        step.setActions(List.of(act));

        final DomFeatureVector liveCandidate = new DomFeatureVector(
                "li",
                "Germany",
                Set.of("country-item"),
                Map.of(),
                "li",
                "Germany",
                "ul",
                0
        );

        this.context.setVariable("liveCandidates", List.of(liveCandidate));

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.SUCCESS, step.getStatus());
        Assertions.assertEquals(1, this.executedCalls.size());
        Assertions.assertEquals("li.country-item:has-text(\"Germany\")", this.executedCalls.get(0).arguments().path("target").asText());
    }

    @Test
    public void testReadOnlyQueryDomToolBypassesSelfHealing() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Search for Poland");
        final ObjectNode queryArgs = MAPPER.createObjectNode();
        queryArgs.put("selector", "#country-list-container *");
        queryArgs.put("text", "Poland");
        step.addToolCall(new ToolCall("call-query", "query_dom", queryArgs));

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("query_dom", "Searches DOM", queryArgs);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                return ToolResult.success(call.callId(), "[]");
            }
        });

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(1, this.executedCalls.size());
        Assertions.assertEquals("query_dom", this.executedCalls.get(0).toolName());
        Assertions.assertEquals(PlaybookStepStatus.SUCCESS, step.getStatus());
    }

    @Test
    public void testReplayStrictBypassesHealingEvenWithLiveCandidatesAndFeatureVectors() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Click submit");

        // Recorded with old selector: button.old-class
        final ObjectNode clickArgs = MAPPER.createObjectNode().put("target", "button.old-class");
        step.addToolCall(new ToolCall("call-20-strict", "browser_click", clickArgs));

        // Companion recorded Action with DomFeatureVector
        final DomFeatureVector recorded = new DomFeatureVector(
                "button",
                "Submit Order",
                Set.of("old-class", "btn"),
                Map.of("type", "submit", "name", "order"),
                "button",
                "Submit Order",
                "form",
                0
        );
        final Action act = new Action("CLICK", "button.old-class", List.of(), "Submit", "");
        act.setDomFeatureVector(recorded);
        step.setActions(List.of(act));

        // Live page candidates: selector shifted to button#submit-order
        final DomFeatureVector liveCandidate = new DomFeatureVector(
                "button",
                "Submit Order",
                Set.of("new-redesigned-btn"),
                Map.of("id", "submit-order", "type", "submit"),
                "button",
                "Submit Order",
                "form",
                0
        );

        this.context.setVariable("liveCandidates", List.of(liveCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_STRICT);

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.SUCCESS, step.getStatus());
        Assertions.assertEquals(1, this.executedCalls.size());
        // In REPLAY_STRICT mode, healing MUST be bypassed completely; selector remains button.old-class!
        Assertions.assertEquals("button.old-class", this.executedCalls.get(0).arguments().path("target").asText());
    }

    @Test
    public void testAssertionLocatorHealsWhenElementTextMatchesExpected() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("assert_text", "Asserts text", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                final String target = call.arguments().hasNonNull("selector")
                        ? call.arguments().path("selector").asText()
                        : call.arguments().path("target").asText();
                if ("#subtotal".equals(target))
                {
                    throw new AssertionError("Element #subtotal not found on page");
                }
                return ToolResult.success(call.callId(), "Subtotal verified: " + target);
            }
        });

        final PlaybookStep step = new PlaybookStep("Assert subtotal is 35");
        final ObjectNode assertArgs = MAPPER.createObjectNode()
                .put("target", "#subtotal")
                .put("expectedText", "35");
        step.addToolCall(new ToolCall("call-assert-1", "assert_text", assertArgs));

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "span",
                "35",
                Set.of("subtotal", "amount"),
                Map.of("id", "subtotal", "data-test", "subtotal"),
                "text",
                "Subtotal: 35",
                "div",
                1
        );
        final Action recordedAction = new Action("ASSERT_TEXT", "#subtotal", List.of("35"), "Verify subtotal", "");
        recordedAction.setDomFeatureVector(recordedVector);
        step.setActions(List.of(recordedAction));

        // Candidate on live page moved to #cart-subtotal and still has text "35"
        final DomFeatureVector liveCandidate = new DomFeatureVector(
                "span",
                "35",
                Set.of("cart-subtotal", "amount"),
                Map.of("id", "cart-subtotal", "data-test", "subtotal"),
                "text",
                "Subtotal: 35",
                "div",
                1
        );

        this.context.setVariable("liveCandidates", List.of(liveCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        Assertions.assertEquals(1, this.executedCalls.size());
        Assertions.assertEquals("#cart-subtotal", this.executedCalls.get(0).arguments().path("target").asText());
    }

    @Test
    public void testAssertionLocatorHealingRejectedWhenElementTextDoesNotMatchExpected()
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("assert_text", "Asserts text", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                final String target = call.arguments().hasNonNull("selector")
                        ? call.arguments().path("selector").asText()
                        : call.arguments().path("target").asText();
                if ("#subtotal".equals(target))
                {
                    throw new AssertionError("Element #subtotal not found on page");
                }
                return ToolResult.success(call.callId(), "Subtotal verified: " + target);
            }
        });

        final PlaybookStep step = new PlaybookStep("Assert subtotal is 35");
        final ObjectNode assertArgs = MAPPER.createObjectNode()
                .put("target", "#subtotal")
                .put("expectedText", "35");
        step.addToolCall(new ToolCall("call-assert-2", "assert_text", assertArgs));

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "span",
                "35",
                Set.of("subtotal", "amount"),
                Map.of("id", "subtotal", "data-test", "subtotal"),
                "text",
                "Subtotal: 35",
                "div",
                1
        );
        final Action recordedAction = new Action("ASSERT_TEXT", "#subtotal", List.of("35"), "Verify subtotal", "");
        recordedAction.setDomFeatureVector(recordedVector);
        step.setActions(List.of(recordedAction));

        // Candidate on live page has changed value "40" (data regression / price mismatch!)
        final DomFeatureVector liveCandidate = new DomFeatureVector(
                "span",
                "40",
                Set.of("cart-subtotal", "amount"),
                Map.of("id", "cart-subtotal", "data-test", "subtotal"),
                "text",
                "Subtotal: 40",
                "div",
                1
        );

        this.context.setVariable("liveCandidates", List.of(liveCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        // Healing must be rejected because candidate text "40" does not match expected "35".
        // As a result, the tool runs with unhealed "#subtotal" and fails conclusively.
        final ConclusiveFailureException ex = Assertions.assertThrows(
                ConclusiveFailureException.class,
                () -> PlaybookToolReplayer.replayStep(step, this.registry, this.context)
        );

        Assertions.assertTrue(ex.getMessage().contains("Assertion failed during replay"));
        Assertions.assertEquals(PlaybookStepStatus.FAILED, step.getStatus());
        Assertions.assertEquals(1, this.executedCalls.size());
        Assertions.assertEquals("#subtotal", this.executedCalls.get(0).arguments().path("target").asText());
    }

    @Test
    public void testPageLevelAssertionBypassesLocatorHealing() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("assert_count", "Asserts count", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                return ToolResult.success(call.callId(), "Count verified");
            }
        });

        final PlaybookStep step = new PlaybookStep("Assert count of items is 2");
        final ObjectNode countArgs = MAPPER.createObjectNode()
                .put("selector", ".cart-item")
                .put("count", 2);
        step.addToolCall(new ToolCall("call-count-1", "assert_count", countArgs));

        final DomFeatureVector liveCandidate = new DomFeatureVector(
                "div",
                "Item",
                Set.of("new-cart-item"),
                Map.of("class", "new-cart-item"),
                "item",
                "Item",
                "div",
                1
        );

        this.context.setVariable("liveCandidates", List.of(liveCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.SUCCESS, step.getStatus());
        Assertions.assertEquals(1, this.executedCalls.size());
        // Page-level assert_count must not heal locator via single DomFeatureVector
        Assertions.assertEquals(".cart-item", this.executedCalls.get(0).arguments().path("selector").asText());
    }

    @Test
    public void testAssertionLocatorHealsInputFieldMatchingValueAttribute() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("assert_text", "Asserts text", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                return ToolResult.success(call.callId(), "Value verified");
            }
        });

        final PlaybookStep step = new PlaybookStep("Assert coupon code input has value SAVE20");
        final ObjectNode assertArgs = MAPPER.createObjectNode()
                .put("target", "#coupon")
                .put("expectedText", "SAVE20");
        step.addToolCall(new ToolCall("call-assert-val", "assert_text", assertArgs));

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "input",
                "",
                Set.of("coupon-field", "coupon-input"),
                Map.of("id", "coupon", "value", "SAVE20"),
                "input",
                "Coupon",
                "form",
                1
        );
        final Action recordedAction = new Action("ASSERT_TEXT", "#coupon", List.of("SAVE20"), "Verify coupon", "");
        recordedAction.setDomFeatureVector(recordedVector);
        step.setActions(List.of(recordedAction));

        // Candidate has empty text but matches via value attribute
        final DomFeatureVector liveCandidate = new DomFeatureVector(
                "input",
                "",
                Set.of("coupon-field-dyn", "coupon-input"),
                Map.of("id", "coupon-dyn", "value", "SAVE20"),
                "input",
                "Coupon",
                "form",
                1
        );

        this.context.setVariable("liveCandidates", List.of(liveCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        Assertions.assertEquals(1, this.executedCalls.size());
        Assertions.assertEquals("#coupon-dyn", this.executedCalls.get(0).arguments().path("target").asText());
    }

    @Test
    public void testAssertionLocatorHealingNormalizesWhitespaceDifferences() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("assert_text", "Asserts text", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                return ToolResult.success(call.callId(), "Text verified");
            }
        });

        final PlaybookStep step = new PlaybookStep("Assert item status");
        final ObjectNode assertArgs = MAPPER.createObjectNode()
                .put("target", "#status")
                .put("expectedText", "Order status: confirmed");
        step.addToolCall(new ToolCall("call-ws-1", "assert_text", assertArgs));

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "div",
                "Order status: confirmed",
                Set.of("order-status", "order-status-box"),
                Map.of("id", "status", "data-box", "status"),
                "text",
                "status",
                "main",
                1
        );
        final Action recordedAction = new Action("ASSERT_TEXT", "#status", List.of("Order status: confirmed"), "Verify status", "");
        recordedAction.setDomFeatureVector(recordedVector);
        step.setActions(List.of(recordedAction));

        // Candidate on live page contains multiline and multiple whitespace spacing
        final DomFeatureVector liveCandidate = new DomFeatureVector(
                "div",
                "Order   status:\n   confirmed",
                Set.of("order-status-dyn", "order-status-box"),
                Map.of("id", "status-dyn", "data-box", "status"),
                "text",
                "status",
                "main",
                1
        );

        this.context.setVariable("liveCandidates", List.of(liveCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        Assertions.assertEquals(1, this.executedCalls.size());
        Assertions.assertEquals("#status-dyn", this.executedCalls.get(0).arguments().path("target").asText());
    }

    @Test
    public void testAssertionLocatorHealsRegexTextMatchAndFallsBackOnInvalidRegex() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("assert_text", "Asserts text", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                return ToolResult.success(call.callId(), "Regex verified");
            }
        });

        final PlaybookStep step = new PlaybookStep("Assert order code format");
        final ObjectNode assertArgs = MAPPER.createObjectNode()
                .put("target", "#order-code")
                .put("expectedText", "[A-Z]{3}-\\d{4}")
                .put("regex", true);
        step.addToolCall(new ToolCall("call-reg-1", "assert_text", assertArgs));

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "span",
                "ABC-1234",
                Set.of("code-badge", "code-badge-common"),
                Map.of("id", "order-code", "data-code", "order"),
                "code",
                "code",
                "div",
                1
        );
        final Action recordedAction = new Action("ASSERT_TEXT", "#order-code", List.of("[A-Z]{3}-\\d{4}"), "Verify code", "");
        recordedAction.setDomFeatureVector(recordedVector);
        step.setActions(List.of(recordedAction));

        // Candidate matches regex [A-Z]{3}-\\d{4}
        final DomFeatureVector liveCandidate = new DomFeatureVector(
                "span",
                "XYZ-9876",
                Set.of("code-badge-new", "code-badge-common"),
                Map.of("id", "order-code-new", "data-code", "order"),
                "code",
                "code",
                "div",
                1
        );

        this.context.setVariable("liveCandidates", List.of(liveCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        Assertions.assertEquals(1, this.executedCalls.size());
        Assertions.assertEquals("#order-code-new", this.executedCalls.get(0).arguments().path("target").asText());
    }

    @Test
    public void testAssertionLocatorHealingNegatedTextExcludesMatchingCandidates() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("assert_text", "Asserts text", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                final String target = call.arguments().path("target").asText();
                if ("#badge".equals(target))
                {
                    throw new AssertionError("Element #badge not found");
                }
                return ToolResult.success(call.callId(), "Negated text verified: " + target);
            }
        });

        final PlaybookStep step = new PlaybookStep("Assert out of stock is not displayed");
        final ObjectNode assertArgs = MAPPER.createObjectNode()
                .put("target", "#badge")
                .put("expectedText", "Out of Stock")
                .put("negated", true);
        step.addToolCall(new ToolCall("call-neg-1", "assert_text", assertArgs));

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "span",
                "In Stock",
                Set.of("badge", "status-badge"),
                Map.of("id", "badge", "data-badge", "true"),
                "badge",
                "badge",
                "div",
                1
        );
        final Action recordedAction = new Action("ASSERT_TEXT", "#badge", List.of("Out of Stock"), "Verify badge", "");
        recordedAction.setDomFeatureVector(recordedVector);
        step.setActions(List.of(recordedAction));

        // Candidate on live page contains the forbidden text "Out of Stock"
        final DomFeatureVector liveCandidate = new DomFeatureVector(
                "span",
                "Out of Stock",
                Set.of("badge-dyn", "status-badge"),
                Map.of("id", "badge-dyn", "data-badge", "true"),
                "badge",
                "badge",
                "div",
                1
        );

        this.context.setVariable("liveCandidates", List.of(liveCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        // Healing must be rejected because candidate text matches the forbidden negated text
        final ConclusiveFailureException ex = Assertions.assertThrows(
                ConclusiveFailureException.class,
                () -> PlaybookToolReplayer.replayStep(step, this.registry, this.context)
        );

        Assertions.assertTrue(ex.getMessage().contains("Assertion failed during replay"));
        Assertions.assertEquals(PlaybookStepStatus.FAILED, step.getStatus());
        Assertions.assertEquals("#badge", this.executedCalls.get(0).arguments().path("target").asText());
    }

    @Test
    public void testAssertionAttributeHealingAcceptsMatchingAndRejectsMismatchedAttribute() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("assert_attribute", "Asserts attribute", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                final String target = call.arguments().path("target").asText();
                if ("#account-status".equals(target))
                {
                    throw new AssertionError("Element #account-status not found");
                }
                return ToolResult.success(call.callId(), "Attribute verified");
            }
        });

        final PlaybookStep step = new PlaybookStep("Assert account status is active");
        final ObjectNode assertArgs = MAPPER.createObjectNode()
                .put("target", "#account-status")
                .put("attribute", "data-status")
                .put("expectedValue", "active");
        step.addToolCall(new ToolCall("call-attr-1", "assert_attribute", assertArgs));

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "div",
                "Active User",
                Set.of("user-status", "status-card"),
                Map.of("id", "account-status", "data-status", "active"),
                "status",
                "status",
                "div",
                1
        );
        final Action recordedAction = new Action("ASSERT_ATTRIBUTE", "#account-status", List.of("data-status", "active"), "Verify status", "");
        recordedAction.setDomFeatureVector(recordedVector);
        step.setActions(List.of(recordedAction));

        // 1. Candidate with matching attribute heals
        final DomFeatureVector matchingCandidate = new DomFeatureVector(
                "div",
                "Active User",
                Set.of("user-status-new", "status-card"),
                Map.of("id", "account-status-new", "data-status", "active"),
                "status",
                "status",
                "div",
                1
        );

        this.context.setVariable("liveCandidates", List.of(matchingCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        Assertions.assertEquals("#account-status-new", this.executedCalls.get(0).arguments().path("target").asText());

        // 2. Candidate with mismatched attribute is rejected and fails conclusively (on a fresh unhealed step)
        this.executedCalls.clear();
        final PlaybookStep mismatchStep = new PlaybookStep("Assert account status is active");
        mismatchStep.addToolCall(new ToolCall("call-attr-2", "assert_attribute", assertArgs.deepCopy()));
        final Action mismatchAction = new Action("ASSERT_ATTRIBUTE", "#account-status", List.of("data-status", "active"), "Verify status", "");
        mismatchAction.setDomFeatureVector(recordedVector);
        mismatchStep.setActions(List.of(mismatchAction));

        final DomFeatureVector mismatchedCandidate = new DomFeatureVector(
                "div",
                "Suspended User",
                Set.of("user-status-new", "status-card"),
                Map.of("id", "account-status-new", "data-status", "suspended"),
                "status",
                "status",
                "div",
                1
        );
        this.context.setVariable("liveCandidates", List.of(mismatchedCandidate));

        final ConclusiveFailureException ex = Assertions.assertThrows(
                ConclusiveFailureException.class,
                () -> PlaybookToolReplayer.replayStep(mismatchStep, this.registry, this.context)
        );
        Assertions.assertTrue(ex.getMessage().contains("Assertion failed during replay"));
        Assertions.assertEquals("#account-status", this.executedCalls.get(0).arguments().path("target").asText());
    }

    @Test
    public void testAssertionAttributePresenceOnlyHealing() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("assert_attribute", "Asserts attribute presence", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                return ToolResult.success(call.callId(), "Attribute presence verified");
            }
        });

        final PlaybookStep step = new PlaybookStep("Assert aria-expanded exists on toggle");
        final ObjectNode assertArgs = MAPPER.createObjectNode()
                .put("target", "#menu-toggle")
                .put("attribute", "aria-expanded");
        step.addToolCall(new ToolCall("call-attr-pres", "assert_attribute", assertArgs));

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "button",
                "Menu",
                Set.of("toggle-btn", "nav-item", "btn"),
                Map.of("id", "menu-toggle", "aria-expanded", "true", "type", "button"),
                "button",
                "Menu",
                "nav",
                1
        );
        final Action recordedAction = new Action("ASSERT_ATTRIBUTE", "#menu-toggle", List.of("aria-expanded"), "Verify toggle", "");
        recordedAction.setDomFeatureVector(recordedVector);
        step.setActions(List.of(recordedAction));

        final DomFeatureVector liveCandidate = new DomFeatureVector(
                "button",
                "Menu",
                Set.of("toggle-btn-dyn", "nav-item", "btn"),
                Map.of("id", "menu-toggle-dyn", "aria-expanded", "false", "type", "button"),
                "button",
                "Menu",
                "nav",
                1
        );

        this.context.setVariable("liveCandidates", List.of(liveCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        Assertions.assertEquals("#menu-toggle-dyn", this.executedCalls.get(0).arguments().path("target").asText());
    }

    @Test
    public void testAssertionElementStateHealingValidatesMultipleStates() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("assert_element_state", "Asserts state", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                final String target = call.arguments().path("target").asText();
                if ("#submit-order".equals(target))
                {
                    throw new AssertionError("Element #submit-order not found");
                }
                return ToolResult.success(call.callId(), "States verified");
            }
        });

        final PlaybookStep step = new PlaybookStep("Assert submit button is visible and enabled");
        final ObjectNode assertArgs = MAPPER.createObjectNode()
                .put("target", "#submit-order");
        assertArgs.putArray("states").add("visible").add("enabled");
        step.addToolCall(new ToolCall("call-state-1", "assert_element_state", assertArgs));

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "button",
                "Submit",
                Set.of("btn-primary", "btn-order"),
                Map.of("id", "submit-order", "data-type", "submit"),
                "button",
                "Submit",
                "form",
                1,
                10, 20, 100, 40
        );
        final Action recordedAction = new Action("ASSERT_ELEMENT_STATE", "#submit-order", List.of("visible", "enabled"), "Verify button", "");
        recordedAction.setDomFeatureVector(recordedVector);
        step.setActions(List.of(recordedAction));

        // 1. Candidate is visible and enabled -> heals
        final DomFeatureVector enabledCandidate = new DomFeatureVector(
                "button",
                "Submit",
                Set.of("btn-primary-dyn", "btn-order"),
                Map.of("id", "submit-order-dyn", "data-type", "submit"),
                "button",
                "Submit",
                "form",
                1,
                10, 20, 100, 40
        );

        this.context.setVariable("liveCandidates", List.of(enabledCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        Assertions.assertEquals("#submit-order-dyn", this.executedCalls.get(0).arguments().path("target").asText());

        // 2. Candidate is disabled -> rejected for healing (on a fresh unhealed step)
        this.executedCalls.clear();
        final PlaybookStep disabledStep = new PlaybookStep("Assert submit button is visible and enabled");
        disabledStep.addToolCall(new ToolCall("call-state-2", "assert_element_state", assertArgs.deepCopy()));
        final Action disabledAction = new Action("ASSERT_ELEMENT_STATE", "#submit-order", List.of("visible", "enabled"), "Verify button", "");
        disabledAction.setDomFeatureVector(recordedVector);
        disabledStep.setActions(List.of(disabledAction));

        final DomFeatureVector disabledCandidate = new DomFeatureVector(
                "button",
                "Submit",
                Set.of("btn-primary-dyn", "btn-order"),
                Map.of("id", "submit-order-dyn", "disabled", "true", "data-type", "submit"),
                "button",
                "Submit",
                "form",
                1,
                10, 20, 100, 40
        );
        this.context.setVariable("liveCandidates", List.of(disabledCandidate));

        final ConclusiveFailureException ex = Assertions.assertThrows(
                ConclusiveFailureException.class,
                () -> PlaybookToolReplayer.replayStep(disabledStep, this.registry, this.context)
        );
        Assertions.assertTrue(ex.getMessage().contains("Assertion failed during replay"));
        Assertions.assertEquals("#submit-order", this.executedCalls.get(0).arguments().path("target").asText());
    }

    @Test
    public void testAssertionElementStateHealingRejectsZeroDimensionCandidateWhenExpectingVisible() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("assert_element_state", "Asserts state", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                throw new AssertionError("Element #notification not found");
            }
        });

        final PlaybookStep step = new PlaybookStep("Assert notification is visible");
        final ObjectNode assertArgs = MAPPER.createObjectNode()
                .put("target", "#notification")
                .put("state", "visible");
        step.addToolCall(new ToolCall("call-vis-1", "assert_element_state", assertArgs));

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "div",
                "Notification",
                Set.of("alert-box", "banner-box"),
                Map.of("id", "notification", "data-box", "alert"),
                "alert",
                "alert",
                "div",
                1,
                10, 20, 200, 50
        );
        final Action recordedAction = new Action("ASSERT_ELEMENT_STATE", "#notification", List.of("visible"), "Verify alert", "");
        recordedAction.setDomFeatureVector(recordedVector);
        step.setActions(List.of(recordedAction));

        // Candidate has 0 width and 0 height (hidden element)
        final DomFeatureVector hiddenCandidate = new DomFeatureVector(
                "div",
                "Notification",
                Set.of("alert-box-dyn", "banner-box"),
                Map.of("id", "notification-dyn", "data-box", "alert"),
                "alert",
                "alert",
                "div",
                1,
                0, 0, 0, 0
        );

        this.context.setVariable("liveCandidates", List.of(hiddenCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final ConclusiveFailureException ex = Assertions.assertThrows(
                ConclusiveFailureException.class,
                () -> PlaybookToolReplayer.replayStep(step, this.registry, this.context)
        );
        Assertions.assertTrue(ex.getMessage().contains("Assertion failed during replay"));
        Assertions.assertEquals("#notification", this.executedCalls.get(0).arguments().path("target").asText());
    }

    @Test
    public void testPageLevelAssertUrlAndTitleBypassLocatorHealing() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition defUrl = new ToolDefinition("assert_url", "Asserts url", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.defUrl;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                return ToolResult.success(call.callId(), "URL verified");
            }
        });
        this.registry.register(new AiTool()
        {
            private final ToolDefinition defTitle = new ToolDefinition("assert_title", "Asserts title", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.defTitle;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                return ToolResult.success(call.callId(), "Title verified");
            }
        });

        final PlaybookStep urlStep = new PlaybookStep("Assert current URL contains cart");
        urlStep.addToolCall(new ToolCall("call-url", "assert_url", MAPPER.createObjectNode().put("expectedUrl", "/cart")));

        final PlaybookStep titleStep = new PlaybookStep("Assert page title is Shopping Cart");
        titleStep.addToolCall(new ToolCall("call-title", "assert_title", MAPPER.createObjectNode().put("expectedTitle", "Shopping Cart")));

        final DomFeatureVector liveCandidate = new DomFeatureVector(
                "div",
                "/cart",
                Set.of("cart-link"),
                Map.of("id", "cart-link"),
                "cart",
                "cart",
                "nav",
                1
        );

        this.context.setVariable("liveCandidates", List.of(liveCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final ToolResult urlResult = PlaybookToolReplayer.replayStep(urlStep, this.registry, this.context);
        final ToolResult titleResult = PlaybookToolReplayer.replayStep(titleStep, this.registry, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, urlResult.status());
        Assertions.assertEquals(ToolResult.Status.SUCCESS, titleResult.status());
        Assertions.assertEquals(PlaybookStepStatus.SUCCESS, urlStep.getStatus());
        Assertions.assertEquals(PlaybookStepStatus.SUCCESS, titleStep.getStatus());
        Assertions.assertEquals(2, this.executedCalls.size());
    }

    @Test
    public void testAssertionLocatorHealingResolvesVariablesBeforeValidatingCandidate() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("assert_text", "Asserts text", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                return ToolResult.success(call.callId(), "Resolved text verified: " + call.arguments().path("expectedText").asText());
            }
        });

        final PlaybookStep step = new PlaybookStep("Assert total matches expected variable");
        final ObjectNode assertArgs = MAPPER.createObjectNode()
                .put("target", "#total")
                .put("expectedText", "${expectedAmount}");
        step.addToolCall(new ToolCall("call-var-1", "assert_text", assertArgs));

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "span",
                "100.00",
                Set.of("total-box", "price-text"),
                Map.of("id", "total", "data-type", "amount"),
                "total",
                "total",
                "div",
                1
        );
        final Action recordedAction = new Action("ASSERT_TEXT", "#total", List.of("100.00"), "Verify total", "");
        recordedAction.setDomFeatureVector(recordedVector);
        step.setActions(List.of(recordedAction));

        // Live candidate has text "100.00"
        final DomFeatureVector liveCandidate = new DomFeatureVector(
                "span",
                "100.00",
                Set.of("total-box-dyn", "price-text"),
                Map.of("id", "total-dyn", "data-type", "amount"),
                "total",
                "total",
                "div",
                1
        );

        final SessionData sessionData = new SessionData();
        sessionData.set("expectedAmount", "100.00");

        this.context.setVariable("liveCandidates", List.of(liveCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context, sessionData, null);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        Assertions.assertEquals("#total-dyn", this.executedCalls.get(0).arguments().path("target").asText());
        Assertions.assertEquals("100.00", this.executedCalls.get(0).arguments().path("expectedText").asText());
    }

    @Test
    public void testAssertionToolErrorResultThrowsConclusiveFailureException() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("assert_text", "Asserts text", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.error(call.callId(), "Text mismatch: expected 'OK' but got 'ERROR'");
            }
        });

        final PlaybookStep step = new PlaybookStep("Assert status is OK");
        final ObjectNode assertArgs = MAPPER.createObjectNode()
                .put("target", "#status")
                .put("expectedText", "OK");
        step.addToolCall(new ToolCall("call-err-1", "assert_text", assertArgs));

        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final ConclusiveFailureException ex = Assertions.assertThrows(
                ConclusiveFailureException.class,
                () -> PlaybookToolReplayer.replayStep(step, this.registry, this.context)
        );

        Assertions.assertTrue(ex.getMessage().contains("Assertion failed during replay"));
        Assertions.assertTrue(ex.getMessage().contains("Text mismatch"));
        Assertions.assertEquals(PlaybookStepStatus.FAILED, step.getStatus());
    }

    @Test
    public void testIntrospectionToolsReplaySuccessfullyInReplayStrict() throws Exception
    {
        BrowserToolProvider.registerStandardTools(this.registry);

        final PlaybookStep step = new PlaybookStep("Troubleshoot network and console state");
        final ObjectNode netArgs = MAPPER.createObjectNode()
                .put("status_filter", "all_failures")
                .put("clear_buffer", false);
        final ObjectNode consoleArgs = MAPPER.createObjectNode()
                .put("min_level", "SEVERE")
                .put("clear_buffer", false);

        step.addToolCall(new ToolCall("call-net-1", "browser_get_network_failures", netArgs));
        step.addToolCall(new ToolCall("call-con-1", "browser_get_console_errors", consoleArgs));

        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_STRICT);

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.SUCCESS, step.getStatus());
    }

    @Test
    public void testOfflineDomHealingUpdatesToolCallsWhilePreservingVariablePlaceholders() throws Exception
    {
        registerFillTool();
        final PlaybookStep step = createOfflineFillStep();
        final SessionData sessionData = createSearchSessionData();
        this.context.setVariable("liveCandidates", List.of(createSearchInputCandidate()));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context, sessionData, null);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        Assertions.assertEquals("#searchInput", this.executedCalls.get(0).arguments().path("selector").asText());
        Assertions.assertEquals("Neodymium", this.executedCalls.get(0).arguments().path("text").asText());

        final ToolCall persistedCall = step.getToolCalls().get(0);
        Assertions.assertEquals("#searchInput", persistedCall.arguments().path("selector").asText(),
                "Persisted ToolCall selector must update to healed #searchInput");
        Assertions.assertEquals("${searchPhrase}", persistedCall.arguments().path("text").asText(),
                "Persisted ToolCall must preserve parameterized ${searchPhrase} placeholder");
        Assertions.assertEquals("searchInput", persistedCall.arguments().path("domFeatureVector").path("attributes").path("id").asText(),
                "Persisted ToolCall domFeatureVector must update to the healed candidate identity");

        final Action persistedAction = step.getActions().get(0);
        Assertions.assertEquals("#searchInput", persistedAction.getTarget());
        Assertions.assertTrue(persistedAction.isHealed());
        Assertions.assertEquals("#searchField", persistedAction.getExpectedTarget());
    }

    @Test
    public void testOfflineHealedStepConvergesToCleanSuccessOnSubsequentReplay() throws Exception
    {
        registerFillTool();
        final PlaybookStep step = createOfflineFillStep();
        final SessionData sessionData = createSearchSessionData();
        this.context.setVariable("liveCandidates", List.of(createSearchInputCandidate()));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        // Run 1: offline DOM healing heals #searchField -> #searchInput and persists it to toolCalls
        final ToolResult run1Result = PlaybookToolReplayer.replayStep(step, this.registry, this.context, sessionData, null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, run1Result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());

        // Run 2: clear transient liveCandidates (simulating direct replay where #searchInput works cleanly)
        this.executedCalls.clear();
        final SimpleToolContext cleanRunContext = new SimpleToolContext(this.registry);
        cleanRunContext.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);
        step.setStatus(PlaybookStepStatus.RUNNING);

        final ToolResult run2Result = PlaybookToolReplayer.replayStep(step, this.registry, cleanRunContext, sessionData, null);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, run2Result.status());
        Assertions.assertEquals(PlaybookStepStatus.SUCCESS, step.getStatus(),
                "Subsequent replay of offline-healed playbook must converge to clean SUCCESS");
        Assertions.assertEquals("#searchInput", this.executedCalls.get(0).arguments().path("selector").asText());
        Assertions.assertEquals("Neodymium", this.executedCalls.get(0).arguments().path("text").asText());

        final Action convergedAction = step.getActions().get(0);
        Assertions.assertFalse(convergedAction.isHealed(), "Clean replay must clear stale healed flag");
        Assertions.assertNull(convergedAction.getExpectedTarget(), "Clean replay must clear stale expectedTarget");
        Assertions.assertNull(convergedAction.getOriginalTarget(), "Clean replay must clear stale originalTarget");
        Assertions.assertNull(convergedAction.getHealedFrom(), "Clean replay must clear stale healedFrom");
        Assertions.assertEquals("#searchInput", step.getToolCalls().get(0).arguments().path("selector").asText());
        Assertions.assertEquals("${searchPhrase}", step.getToolCalls().get(0).arguments().path("text").asText());
    }

    @Test
    public void testAssertElementStateHiddenSkipsProactiveHealing() throws Exception
    {
        registerMockAssertElementStateTool("assert_element_state", false);
        registerMockAssertElementStateTool("browser_assert_element_state", false);

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "button",
                "",
                Set.of("secret-btn"),
                Map.of("id", "hidden-btn"),
                "button",
                "Secret Button",
                "body",
                0,
                0, 0, 0, 0
        );
        final DomFeatureVector matchingHiddenCandidate = new DomFeatureVector(
                "button",
                "",
                Set.of("secret-btn"),
                Map.of("id", "hidden-btn-alt"),
                "button",
                "Secret Button",
                "body",
                0,
                0, 0, 0, 0
        );
        this.context.setVariable("liveCandidates", List.of(matchingHiddenCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final List<ObjectNode> variants = List.of(
                MAPPER.createObjectNode().put("selector", "button#hidden-btn").put("state", "hidden"),
                MAPPER.createObjectNode().put("selector", "button#hidden-btn").put("state", "invisible"),
                MAPPER.createObjectNode().put("selector", "button#hidden-btn").put("state", "visible").put("negated", true)
        );

        for (final ObjectNode args : variants)
        {
            this.executedCalls.clear();
            final PlaybookStep step = new PlaybookStep("Assert secret button is hidden");
            step.addToolCall(new ToolCall("call-hidden", "assert_element_state", args));
            final Action recordedAction = new Action("ASSERT_HIDDEN", "button#hidden-btn", List.of("hidden"), "Verify hidden", "");
            recordedAction.setDomFeatureVector(recordedVector);
            step.setActions(List.of(recordedAction));

            final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
            Assertions.assertEquals(PlaybookStepStatus.SUCCESS, step.getStatus(), "Hidden assertion must not trigger healing for args: " + args);
            Assertions.assertEquals(1, this.executedCalls.size());
            Assertions.assertEquals("button#hidden-btn", this.executedCalls.get(0).arguments().path("selector").asText());
        }

        // Also verify browser_-prefixed tool name skips proactive healing
        this.executedCalls.clear();
        final PlaybookStep prefixedStep = new PlaybookStep("Assert secret button is hidden (prefixed)");
        final ObjectNode prefixedArgs = MAPPER.createObjectNode().put("selector", "button#hidden-btn").put("state", "hidden");
        prefixedStep.addToolCall(new ToolCall("call-hidden-prefixed", "browser_assert_element_state", prefixedArgs));
        final Action prefixedAction = new Action("ASSERT_HIDDEN", "button#hidden-btn", List.of("hidden"), "Verify hidden", "");
        prefixedAction.setDomFeatureVector(recordedVector);
        prefixedStep.setActions(List.of(prefixedAction));

        final ToolResult prefixedResult = PlaybookToolReplayer.replayStep(prefixedStep, this.registry, this.context);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, prefixedResult.status());
        Assertions.assertEquals(PlaybookStepStatus.SUCCESS, prefixedStep.getStatus());
        Assertions.assertEquals("button#hidden-btn", this.executedCalls.get(0).arguments().path("selector").asText());
    }

    @Test
    public void testAssertElementStateAbsentSkipsProactiveHealing() throws Exception
    {
        registerMockAssertElementStateTool("assert_element_state", false);

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "div",
                "Banner",
                Set.of("promo-banner"),
                Map.of("id", "promo-banner"),
                "region",
                "Banner",
                "body",
                0,
                0, 0, 100, 40
        );
        final DomFeatureVector candidate = new DomFeatureVector(
                "div",
                "Banner",
                Set.of("promo-banner"),
                Map.of("id", "promo-banner-alt"),
                "region",
                "Banner",
                "body",
                0,
                0, 0, 100, 40
        );
        this.context.setVariable("liveCandidates", List.of(candidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final List<ObjectNode> variants = List.of(
                MAPPER.createObjectNode().put("selector", "#promo-banner").put("state", "absent"),
                MAPPER.createObjectNode().put("selector", "#promo-banner").put("state", "not_exists"),
                MAPPER.createObjectNode().put("selector", "#promo-banner").put("state", "exists").put("negated", true)
        );

        for (final ObjectNode args : variants)
        {
            this.executedCalls.clear();
            final PlaybookStep step = new PlaybookStep("Assert banner is absent");
            step.addToolCall(new ToolCall("call-absent", "assert_element_state", args));
            final Action recordedAction = new Action("ASSERT_ABSENT", "#promo-banner", List.of("absent"), "Verify absent", "");
            recordedAction.setDomFeatureVector(recordedVector);
            step.setActions(List.of(recordedAction));

            final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
            Assertions.assertEquals(PlaybookStepStatus.SUCCESS, step.getStatus(), "Absent assertion must not trigger healing for args: " + args);
            Assertions.assertEquals("#promo-banner", this.executedCalls.get(0).arguments().path("selector").asText());
        }
    }

    @Test
    public void testAssertElementStateNegatedHiddenStillAllowsVisibleHealing() throws Exception
    {
        registerMockAssertElementStateTool("assert_element_state", true);

        final PlaybookStep step = new PlaybookStep("Assert banner is not hidden");
        final ObjectNode assertArgs = MAPPER.createObjectNode()
                .put("selector", "#old-banner")
                .put("state", "hidden")
                .put("negated", true);
        step.addToolCall(new ToolCall("call-neg-hidden", "assert_element_state", assertArgs));

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "div",
                "Promo Banner",
                Set.of("banner-card"),
                Map.of("id", "old-banner", "data-test", "banner"),
                "region",
                "Promo Banner",
                "main",
                0,
                10, 20, 200, 50
        );
        final Action recordedAction = new Action("ASSERT_ELEMENT_STATE", "#old-banner", List.of("hidden"), "Verify visible", "");
        recordedAction.setDomFeatureVector(recordedVector);
        step.setActions(List.of(recordedAction));

        final DomFeatureVector visibleCandidate = new DomFeatureVector(
                "div",
                "Promo Banner",
                Set.of("banner-card"),
                Map.of("id", "new-banner", "data-test", "banner"),
                "region",
                "Promo Banner",
                "main",
                0,
                10, 20, 200, 50
        );
        this.context.setVariable("liveCandidates", List.of(visibleCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        Assertions.assertEquals("#new-banner", this.executedCalls.get(0).arguments().path("selector").asText());
    }

    @Test
    public void testAssertElementStateExistsAndCompoundHiddenDisabledStillHealWhenMissing() throws Exception
    {
        registerMockAssertElementStateTool("assert_element_state", true);

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "button",
                "",
                Set.of("hidden-control", "btn"),
                Map.of("id", "old-hidden", "disabled", "true", "data-role", "secret"),
                "button",
                "Secret",
                "form",
                0,
                0, 0, 0, 0
        );
        final DomFeatureVector matchingCandidate = new DomFeatureVector(
                "button",
                "",
                Set.of("hidden-control", "btn"),
                Map.of("id", "new-hidden", "disabled", "true", "data-role", "secret"),
                "button",
                "Secret",
                "form",
                0,
                0, 0, 0, 0
        );
        this.context.setVariable("liveCandidates", List.of(matchingCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        // 1. 'exists' heals when missing
        final PlaybookStep existsStep = new PlaybookStep("Assert hidden element exists");
        existsStep.addToolCall(new ToolCall("call-exists", "assert_element_state",
                MAPPER.createObjectNode().put("selector", "#old-hidden").put("state", "exists")));
        final Action existsAction = new Action("ASSERT_EXISTS", "#old-hidden", List.of("exists"), "Verify exists", "");
        existsAction.setDomFeatureVector(recordedVector);
        existsStep.setActions(List.of(existsAction));

        final ToolResult existsResult = PlaybookToolReplayer.replayStep(existsStep, this.registry, this.context);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, existsResult.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, existsStep.getStatus());
        Assertions.assertEquals("#new-hidden", this.executedCalls.get(0).arguments().path("selector").asText());

        // 2. 'absent' + negated=true resolves to 'exists' and heals when missing
        this.executedCalls.clear();
        final PlaybookStep negatedAbsentStep = new PlaybookStep("Assert element is not absent");
        negatedAbsentStep.addToolCall(new ToolCall("call-neg-absent", "assert_element_state",
                MAPPER.createObjectNode().put("selector", "#old-hidden").put("state", "absent").put("negated", true)));
        negatedAbsentStep.setActions(List.of(existsAction));

        final ToolResult negatedAbsentResult = PlaybookToolReplayer.replayStep(negatedAbsentStep, this.registry, this.context);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, negatedAbsentResult.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, negatedAbsentStep.getStatus());
        Assertions.assertEquals("#new-hidden", this.executedCalls.get(0).arguments().path("selector").asText());

        // 3. Compound ['hidden', 'disabled'] heals onto hidden+disabled candidate
        this.executedCalls.clear();
        final PlaybookStep compoundStep = new PlaybookStep("Assert hidden control is hidden and disabled");
        final ObjectNode compoundArgs = MAPPER.createObjectNode().put("selector", "#old-hidden");
        compoundArgs.putArray("states").add("hidden").add("disabled");
        compoundStep.addToolCall(new ToolCall("call-compound-hd", "assert_element_state", compoundArgs));
        final Action compoundAction = new Action("ASSERT_ELEMENT_STATE", "#old-hidden", List.of("hidden", "disabled"), "Verify hidden disabled", "");
        compoundAction.setDomFeatureVector(recordedVector);
        compoundStep.setActions(List.of(compoundAction));

        final ToolResult compoundResult = PlaybookToolReplayer.replayStep(compoundStep, this.registry, this.context);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, compoundResult.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, compoundStep.getStatus());
        Assertions.assertEquals("#new-hidden", this.executedCalls.get(0).arguments().path("selector").asText());

        // 4. Compound ['hidden', 'disabled'] rejects enabled hidden candidate and fails conclusively
        this.executedCalls.clear();
        final DomFeatureVector enabledHiddenCandidate = new DomFeatureVector(
                "button",
                "",
                Set.of("hidden-control", "btn"),
                Map.of("id", "new-hidden", "data-role", "secret"),
                "button",
                "Secret",
                "form",
                0,
                0, 0, 0, 0
        );
        this.context.setVariable("liveCandidates", List.of(enabledHiddenCandidate));

        final PlaybookStep rejectedStep = new PlaybookStep("Assert hidden control is hidden and disabled");
        rejectedStep.addToolCall(new ToolCall("call-compound-reject", "assert_element_state", compoundArgs));
        rejectedStep.setActions(List.of(compoundAction));

        final ConclusiveFailureException ex = Assertions.assertThrows(
                ConclusiveFailureException.class,
                () -> PlaybookToolReplayer.replayStep(rejectedStep, this.registry, this.context)
        );
        Assertions.assertTrue(ex.getMessage().contains("Assertion failed during replay"));
        Assertions.assertEquals(PlaybookStepStatus.FAILED, rejectedStep.getStatus());
        Assertions.assertEquals("#old-hidden", this.executedCalls.get(0).arguments().path("selector").asText());
    }

    @Test
    public void testResolveAssertElementStatesNormalizationAndNegation()
    {
        Assertions.assertEquals(List.of(), PlaybookToolReplayer.resolveAssertElementStates(null));
        Assertions.assertEquals(List.of(), PlaybookToolReplayer.resolveAssertElementStates(MAPPER.createObjectNode()));

        final ObjectNode arrayArgs = MAPPER.createObjectNode();
        arrayArgs.putArray("states").add("invisible").add("disabled");
        Assertions.assertEquals(List.of("hidden", "disabled"), PlaybookToolReplayer.resolveAssertElementStates(arrayArgs));

        final ObjectNode splitArgs = MAPPER.createObjectNode()
                .put("state", "displayed, not readonly and non-existent");
        Assertions.assertEquals(List.of("visible", "not_readonly", "absent"), PlaybookToolReplayer.resolveAssertElementStates(splitArgs));

        Assertions.assertEquals(List.of("hidden"),
                PlaybookToolReplayer.resolveAssertElementStates(MAPPER.createObjectNode().put("state", "visible").put("negated", true)));
        Assertions.assertEquals(List.of("visible"),
                PlaybookToolReplayer.resolveAssertElementStates(MAPPER.createObjectNode().put("state", "invisible").put("not", true)));
        Assertions.assertEquals(List.of("absent"),
                PlaybookToolReplayer.resolveAssertElementStates(MAPPER.createObjectNode().put("state", "present").put("negated", true)));
        Assertions.assertEquals(List.of("exists"),
                PlaybookToolReplayer.resolveAssertElementStates(MAPPER.createObjectNode().put("state", "not_exists").put("negated", true)));
        Assertions.assertEquals(List.of("not_editable"),
                PlaybookToolReplayer.resolveAssertElementStates(MAPPER.createObjectNode().put("state", "editable").put("negated", true)));
        Assertions.assertEquals(List.of("not_readonly"),
                PlaybookToolReplayer.resolveAssertElementStates(MAPPER.createObjectNode().put("state", "readonly").put("negated", true)));
        Assertions.assertEquals(List.of("not_empty"),
                PlaybookToolReplayer.resolveAssertElementStates(MAPPER.createObjectNode().put("state", "empty").put("negated", true)));
    }

    private void registerFillTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("fill", "Fills input field", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                return ToolResult.success(call.callId(), "Filled " + call.arguments().path("selector").asText());
            }
        });
    }

    private PlaybookStep createOfflineFillStep()
    {
        final PlaybookStep step = new PlaybookStep("Type '${searchPhrase}' in the Search input field");
        final DomFeatureVector recorded = new DomFeatureVector(
                "input",
                "Search Wikipedia",
                Set.of("mw-searchField"),
                Map.of("id", "searchField", "name", "search", "type", "search", "placeholder", "Search Wikipedia"),
                "textbox",
                "Search Wikipedia",
                "div",
                0
        );
        final ObjectNode fillArgs = MAPPER.createObjectNode()
                .put("selector", "#searchField")
                .put("text", "${searchPhrase}");
        fillArgs.set("domFeatureVector", MAPPER.valueToTree(recorded));
        step.addToolCall(new ToolCall("call-fill-1", "fill", fillArgs));

        final Action act = new Action("TYPE", "#searchField", List.of("${searchPhrase}"), "Type '${searchPhrase}' into #searchField", "");
        act.setDomFeatureVector(recorded);
        step.setActions(List.of(act));
        return step;
    }

    private SessionData createSearchSessionData()
    {
        final SessionData sessionData = new SessionData();
        sessionData.set("searchPhrase", "Neodymium");
        return sessionData;
    }

    private DomFeatureVector createSearchInputCandidate()
    {
        return new DomFeatureVector(
                "input",
                "Search Wikipedia",
                Set.of("search-input"),
                Map.of("id", "searchInput", "name", "search", "type", "search", "placeholder", "Search Wikipedia"),
                "textbox",
                "Search Wikipedia",
                "div",
                0
        );
    }

    private void registerMockAssertElementStateTool(final String toolName, final boolean failOnOldSelector)
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition(toolName, "Asserts element state", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                final String target = call.arguments().hasNonNull("selector")
                        ? call.arguments().path("selector").asText()
                        : call.arguments().path("target").asText();
                if (failOnOldSelector && target.contains("old"))
                {
                    throw new AssertionError("Element " + target + " not found");
                }
                return ToolResult.success(call.callId(), "State verified for " + target);
            }
        });
    }

    @Test
    public void testMultiCallOfflineHealingDoesNotOverwriteStepDomFeatureVectorFromSubsequentCalls() throws Exception
    {
        final ObjectNode waitSchema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("browser_wait", "Waits ms", waitSchema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                return ToolResult.success(call.callId(), "Waited");
            }
        });

        final PlaybookStep step = new PlaybookStep("Wait then click confirm button");
        final DomFeatureVector firstStepVector = new DomFeatureVector(
                "a",
                "Home",
                Set.of("nav-home"),
                Map.of("id", "btn0"),
                "link",
                "Home",
                "header",
                0
        );
        final DomFeatureVector secondOldVector = new DomFeatureVector(
                "button",
                "Confirm",
                Set.of("confirm-btn"),
                Map.of("id", "btn1Old"),
                "button",
                "Confirm",
                "form",
                0
        );
        final DomFeatureVector secondHealedCandidate = new DomFeatureVector(
                "button",
                "Confirm",
                Set.of("confirm-btn"),
                Map.of("id", "btn1New"),
                "button",
                "Confirm",
                "form",
                0
        );

        step.setDomFeatureVector(firstStepVector);

        final ObjectNode waitArgs = MAPPER.createObjectNode().put("ms", 5);
        final ObjectNode clickArgs = MAPPER.createObjectNode().put("target", "#btn1Old");
        clickArgs.set("domFeatureVector", MAPPER.valueToTree(secondOldVector));

        step.addToolCall(new ToolCall("call-wait-0", "browser_wait", waitArgs));
        step.addToolCall(new ToolCall("call-click-1", "browser_click", clickArgs));

        final Action act0 = new Action("WAIT", "", List.of("5"), "Wait briefly", "");
        final Action act1 = new Action("CLICK", "#btn1Old", List.of(), "Click confirm", "");
        act1.setDomFeatureVector(secondOldVector);
        step.setActions(new ArrayList<>(List.of(act0, act1)));

        this.context.setVariable("liveCandidates", List.of(secondHealedCandidate));
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        Assertions.assertEquals("#btn1New", step.getActions().get(1).getTarget());
        Assertions.assertNotNull(step.getDomFeatureVector());
        Assertions.assertEquals("btn0", step.getDomFeatureVector().getAttributes().get("id"),
                "Healing callIndex=1 must NOT overwrite callIndex=0 step-level domFeatureVector");
    }

    @Test
    public void testReplayStepRetainsSensitivePlaceholderInActionExecutedEventWhileExecutingSecret() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("fill", "Fills input", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                return ToolResult.success(call.callId(), "Filled");
            }
        });

        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("user.name", "alice", false);
        sessionData.putDynamic("user.password", "secretPassword123", true);

        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final List<ActionExecutedEvent> actionEvents = new ArrayList<>();
        eventBus.registerListener(event ->
        {
            if (event instanceof final ActionExecutedEvent aee)
            {
                actionEvents.add(aee);
            }
        });
        final AiSession session = AiSession.mock(
                ExecutionMode.REPLAY_WITH_HEALING,
                sessionData,
                new LlmRegistry(),
                eventBus,
                (TargetExecutor) null);
        this.context.setVariable("neodymium.session", session);
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

        final PlaybookStep step = new PlaybookStep("Fill credentials");
        final ObjectNode fillArgs = MAPPER.createObjectNode()
                .put("selector", "#pwd")
                .put("text", "${user.name}:${user.password}");
        step.addToolCall(new ToolCall("call-fill-sec", "fill", fillArgs));

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context, sessionData, null);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(1, this.executedCalls.size());
        Assertions.assertEquals("alice:secretPassword123", this.executedCalls.get(0).arguments().path("text").asText(),
                "Replay execution must receive resolved secret value");

        Assertions.assertEquals(1, actionEvents.size());
        final ActionExecutedEvent event = actionEvents.get(0);
        Assertions.assertEquals("${user.name}:${user.password}", event.getAction().getValue());
        Assertions.assertEquals("alice:${user.password}", event.getResolvedAction().getValue(),
                "Replay ActionExecutedEvent.resolvedAction must retain sensitive placeholder");
    }

    @Test
    public void testHealedReplayStepRetainsSensitivePlaceholderInActionExecutedEventWhileUpdatingTarget() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode().put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("fill", "Fills input", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                PlaybookToolReplayTest.this.executedCalls.add(call);
                return ToolResult.success(call.callId(), "Filled");
            }
        });

        final DomFeatureVector recordedVector = new DomFeatureVector(
                "input", "", Set.of("pwd-old"), Map.of("id", "oldPwd", "type", "password"), "textbox", "Password", "form", 0);
        final DomFeatureVector liveCandidate = new DomFeatureVector(
                "input", "", Set.of("pwd-new"), Map.of("id", "newPwd", "type", "password"), "textbox", "Password", "form", 0);

        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("user.name", "alice", false);
        sessionData.putDynamic("user.password", "secretPassword123", true);

        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final List<ActionExecutedEvent> actionEvents = new ArrayList<>();
        eventBus.registerListener(event ->
        {
            if (event instanceof final ActionExecutedEvent aee)
            {
                actionEvents.add(aee);
            }
        });
        final AiSession session = AiSession.mock(
                ExecutionMode.REPLAY_WITH_HEALING,
                sessionData,
                new LlmRegistry(),
                eventBus,
                (TargetExecutor) null);
        this.context.setVariable("neodymium.session", session);
        this.context.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);
        this.context.setVariable("liveCandidates", List.of(liveCandidate));

        final PlaybookStep step = new PlaybookStep("Fill healed password");
        final ObjectNode fillArgs = MAPPER.createObjectNode()
                .put("selector", "#oldPwd")
                .put("text", "${user.name}:${user.password}");
        fillArgs.set("domFeatureVector", MAPPER.valueToTree(recordedVector));
        step.addToolCall(new ToolCall("call-fill-heal", "fill", fillArgs));
        final Action recordedAct = new Action("TYPE", "#oldPwd", List.of("${user.name}:${user.password}"), "Fill password", "");
        recordedAct.setDomFeatureVector(recordedVector);
        step.setActions(new ArrayList<>(List.of(recordedAct)));

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context, sessionData, null);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        Assertions.assertEquals("#newPwd", this.executedCalls.get(0).arguments().path("selector").asText());
        Assertions.assertEquals("alice:secretPassword123", this.executedCalls.get(0).arguments().path("text").asText());

        Assertions.assertEquals(1, actionEvents.size());
        final ActionExecutedEvent event = actionEvents.get(0);
        Assertions.assertTrue(event.isHealed());
        Assertions.assertEquals("#oldPwd", event.getAction().getTarget());
        Assertions.assertEquals("#newPwd", event.getResolvedAction().getTarget());
        Assertions.assertEquals("#oldPwd", event.getResolvedAction().getExpectedTarget());
        Assertions.assertEquals("${user.name}:${user.password}", event.getAction().getValue());
        Assertions.assertEquals("alice:${user.password}", event.getResolvedAction().getValue(),
                "Healed replay ActionExecutedEvent.resolvedAction must retain sensitive placeholder");
    }
}
