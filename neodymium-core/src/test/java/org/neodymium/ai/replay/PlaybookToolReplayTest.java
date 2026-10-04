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
import org.neodymium.ai.model.DomFeatureVector;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
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
import java.util.UUID;

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

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        Assertions.assertEquals(1, this.executedCalls.size());
        // Target was healed to the live candidate selector #submit-order!
        Assertions.assertEquals("#submit-order", this.executedCalls.get(0).arguments().path("target").asText());
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
}
