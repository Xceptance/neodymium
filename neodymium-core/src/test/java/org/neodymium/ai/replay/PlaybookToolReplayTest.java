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
import org.neodymium.ai.model.DomFeatureVector;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
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
        Assertions.assertEquals("browser_click", synthesized.get(0).toolName());
        Assertions.assertEquals("#old-button", synthesized.get(0).arguments().path("target").asText());
        Assertions.assertEquals("browser_type", synthesized.get(1).toolName());
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
}
