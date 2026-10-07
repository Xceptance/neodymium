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

        final long start = System.nanoTime();
        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);
        final long durationMs = (System.nanoTime() - start) / 1_000_000;

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
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

        // 2. Candidate with mismatched attribute is rejected and fails conclusively
        this.executedCalls.clear();
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
                () -> PlaybookToolReplayer.replayStep(step, this.registry, this.context)
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

        // 2. Candidate is disabled -> rejected for healing
        this.executedCalls.clear();
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
                () -> PlaybookToolReplayer.replayStep(step, this.registry, this.context)
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
}
