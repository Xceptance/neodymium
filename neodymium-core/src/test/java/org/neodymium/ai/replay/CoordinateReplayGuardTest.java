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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.neodymium.ai.tool.browser.ReanchoringBridge;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Unit tests validating {@link PlaybookToolReplayer} coordinate click guards,
 * {@link DomFeatureVector} identity validation, and coordinate-to-selector self-healing.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class CoordinateReplayGuardTest
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
                CoordinateReplayGuardTest.this.executedCalls.add(call);
                return ToolResult.success(call.callId(), "Clicked " + call.arguments().path("target").asText());
            }
        });
    }

    @Test
    public void testCoordinateClickHealingWithDomFeatureVectorToSelector() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Click submit order button via visual coordinate");

        final DomFeatureVector recordedVector = new DomFeatureVector(
            "button",
            "Submit Order",
            Set.of("btn", "btn-primary"),
            Map.of("type", "submit", "name", "order"),
            "button",
            "Submit Order",
            "form",
            0,
            150,
            250,
            120,
            40
        );

        final ObjectNode clickArgs = MAPPER.createObjectNode();
        clickArgs.put("target", "coord: 210,270");
        clickArgs.put("x", 210);
        clickArgs.put("y", 270);
        clickArgs.set("domFeatureVector", MAPPER.valueToTree(recordedVector));

        step.addToolCall(new ToolCall("call-coord-1", "browser_click", clickArgs));

        final DomFeatureVector liveCandidate = new DomFeatureVector(
            "button",
            "Submit Order",
            Set.of("redesigned-btn"),
            Map.of("id", "submit-order", "type", "submit"),
            "button",
            "Submit Order",
            "form",
            0,
            300,
            450,
            140,
            44
        );

        this.context.setVariable("liveCandidates", List.of(liveCandidate));

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);

        assertEquals(ToolResult.Status.SUCCESS, result.status());
        assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        assertEquals(1, this.executedCalls.size());

        final ToolCall executed = this.executedCalls.get(0);
        assertEquals("#submit-order", executed.arguments().path("target").asText());
        assertEquals("#submit-order", executed.arguments().path("selector").asText());
        assertFalse(executed.arguments().hasNonNull("x"), "Coordinates must be removed when healed to a selector");
        assertFalse(executed.arguments().hasNonNull("y"), "Coordinates must be removed when healed to a selector");
    }

    @Test
    public void testCoordinateClickHealingRelocatesCoordinatesWhenNoSelectorDerivable() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Click canvas tool icon via visual coordinate");

        final DomFeatureVector recordedVector = new DomFeatureVector(
            "div",
            "",
            Set.of("w-1/2", "hover:opacity-80"),
            Map.of("data-canvas-item", "brush-1"),
            "",
            "",
            "div",
            0,
            100,
            200,
            40,
            40
        );

        final ObjectNode clickArgs = MAPPER.createObjectNode();
        clickArgs.put("target", "coord: 120,220");
        clickArgs.put("x", 120);
        clickArgs.put("y", 220);
        clickArgs.set("domFeatureVector", MAPPER.valueToTree(recordedVector));

        step.addToolCall(new ToolCall("call-coord-2", "browser_click", clickArgs));

        final DomFeatureVector liveCandidate = new DomFeatureVector(
            "div",
            "",
            Set.of("w-1/2", "hover:opacity-80"),
            Map.of("data-canvas-item", "brush-1"),
            "",
            "",
            "div",
            0,
            400,
            500,
            60,
            40
        );

        this.context.setVariable("liveCandidates", List.of(liveCandidate));

        final ToolResult result = PlaybookToolReplayer.replayStep(step, this.registry, this.context);

        assertEquals(ToolResult.Status.SUCCESS, result.status());
        assertEquals(PlaybookStepStatus.HEALED, step.getStatus());
        assertEquals(1, this.executedCalls.size());

        final ToolCall executed = this.executedCalls.get(0);
        assertEquals("coord: 430,520", executed.arguments().path("target").asText());
        assertEquals(430, executed.arguments().path("x").asInt());
        assertEquals(520, executed.arguments().path("y").asInt());
    }

    @Test
    public void testMatchesRecordedIdentityLogic()
    {
        final DomFeatureVector recorded = new DomFeatureVector(
            "button",
            "Checkout Now",
            Set.of("btn"),
            Map.of("id", "co"),
            "button",
            "Checkout Now",
            "div",
            0
        );

        final ReanchoringBridge.ReanchoredElement matching = new ReanchoringBridge.ReanchoredElement(
            "#co",
            "button",
            "Checkout Now",
            10,
            10,
            Map.of()
        );
        assertTrue(PlaybookToolReplayer.matchesRecordedIdentity(matching, recorded));

        final ReanchoringBridge.ReanchoredElement partialTextMatch = new ReanchoringBridge.ReanchoredElement(
            "#co",
            "button",
            "Checkout",
            10,
            10,
            Map.of()
        );
        assertTrue(PlaybookToolReplayer.matchesRecordedIdentity(partialTextMatch, recorded));

        final ReanchoringBridge.ReanchoredElement tagMismatch = new ReanchoringBridge.ReanchoredElement(
            "div.container",
            "div",
            "Checkout Now",
            10,
            10,
            Map.of()
        );
        assertFalse(PlaybookToolReplayer.matchesRecordedIdentity(tagMismatch, recorded));

        final ReanchoringBridge.ReanchoredElement textMismatch = new ReanchoringBridge.ReanchoredElement(
            "#cancel",
            "button",
            "Cancel Order",
            10,
            10,
            Map.of()
        );
        assertFalse(PlaybookToolReplayer.matchesRecordedIdentity(textMismatch, recorded));

        assertFalse(PlaybookToolReplayer.matchesRecordedIdentity(null, recorded));
    }

    @Test
    public void testReanchoringBridgePopulatesTagClassesAttributes()
    {
        final Map<String, Object> scriptResult = new LinkedHashMap<>();
        scriptResult.put("selector", "#submit");
        scriptResult.put("tagName", "button");
        scriptResult.put("text", "Submit");
        scriptResult.put("classes", List.of("btn", "btn-primary"));
        scriptResult.put("attributes", Map.of("id", "submit", "type", "submit"));
        scriptResult.put("relX", 15);
        scriptResult.put("relY", 10);
        scriptResult.put("x", 100);
        scriptResult.put("y", 200);
        scriptResult.put("width", 80);
        scriptResult.put("height", 35);

        final WebDriver mockDriver = (WebDriver) Proxy.newProxyInstance(
            CoordinateReplayGuardTest.class.getClassLoader(),
            new Class<?>[] { WebDriver.class, JavascriptExecutor.class },
            (proxy, method, args) -> {
                if ("executeScript".equals(method.getName()))
                {
                    return scriptResult;
                }
                return null;
            }
        );

        final ReanchoringBridge.ReanchoredElement reanchored = ReanchoringBridge.resolveElementAtPoint(mockDriver, 115, 210);

        assertNotNull(reanchored);
        assertEquals("button", reanchored.tagName());
        assertEquals("Submit", reanchored.text());

        final Map<String, Object> vectorMap = reanchored.domFeatureVector();
        assertEquals("button", vectorMap.get("tag"));
        assertEquals("button", vectorMap.get("tagName"));
        assertNotNull(vectorMap.get("classes"));
        assertNotNull(vectorMap.get("attributes"));

        final DomFeatureVector deserialized = MAPPER.convertValue(vectorMap, DomFeatureVector.class);
        assertNotNull(deserialized);
        assertEquals("button", deserialized.getTag());
        assertEquals("Submit", deserialized.getText());
        assertTrue(deserialized.getClasses().contains("btn"));
        assertEquals("submit", deserialized.getAttributes().get("id"));
    }

    @Test
    public void testExtractCoordinateAndVectorHelpers()
    {
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("target", "coord: 240,360");
        args.put("x", 240);
        args.put("y", 360);

        assertEquals(240, PlaybookToolReplayer.extractCoordinate(args, "x", "coord: 240,360", 0));
        assertEquals(360, PlaybookToolReplayer.extractCoordinate(args, "y", "coord: 240,360", 1));

        final ObjectNode argsWithoutXY = MAPPER.createObjectNode();
        argsWithoutXY.put("target", "coord: 125,450");
        assertEquals(125, PlaybookToolReplayer.extractCoordinate(argsWithoutXY, "x", "coord: 125,450", 0));
        assertEquals(450, PlaybookToolReplayer.extractCoordinate(argsWithoutXY, "y", "coord: 125,450", 1));

        final DomFeatureVector vector = new DomFeatureVector("a", "Link", Set.of(), Map.of(), "link", "Link", "div", 0);
        final PlaybookStep step = new PlaybookStep("Step");
        final Action action = new Action("CLICK", "a.link", List.of(), "Link", "");
        action.setDomFeatureVector(vector);
        step.setActions(List.of(action));

        final DomFeatureVector extracted = PlaybookToolReplayer.extractRecordedVector(args, step, 0);
        assertNotNull(extracted);
        assertEquals("a", extracted.getTag());
        assertEquals("Link", extracted.getText());
    }
}
