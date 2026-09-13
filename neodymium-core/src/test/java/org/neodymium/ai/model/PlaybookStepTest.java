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

package org.neodymium.ai.model;

import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.playbook.YamlPlaybookParser;
import org.neodymium.ai.resources.InMemoryResourceManager;
import org.neodymium.ai.runner.StateMachineRunner;
import org.neodymium.ai.session.AiSession;

/**
 * Unit tests for {@link PlaybookStep} visual and full-page instruction detection methods.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
public class PlaybookStepTest
{
    @Test
    public void testIsVisualStep()
    {
        final PlaybookStep standardStep = new PlaybookStep();
        standardStep.setInstruction("Click on the login button");
        Assertions.assertFalse(standardStep.isVisualStep());

        final PlaybookStep visualStep = new PlaybookStep();
        visualStep.setInstruction("Verify header logo position (visual)");
        Assertions.assertTrue(visualStep.isVisualStep());

        final PlaybookStep layoutStep = new PlaybookStep();
        layoutStep.setInstruction("Check sidebar layout (layout)");
        Assertions.assertTrue(layoutStep.isVisualStep());
    }

    @Test
    public void testIsHintStep()
    {
        final PlaybookStep standardStep = new PlaybookStep();
        standardStep.setInstruction("Click on the login button");
        Assertions.assertFalse(standardStep.isHintStep());

        final PlaybookStep standardHintStep = new PlaybookStep();
        standardHintStep.setInstruction("Click submit (hint: #submit-order)");
        Assertions.assertTrue(standardHintStep.isHintStep());

        final PlaybookStep noSpaceHintStep = new PlaybookStep();
        noSpaceHintStep.setInstruction("Click search (hint:#search)");
        Assertions.assertTrue(noSpaceHintStep.isHintStep());

        final PlaybookStep spaceTolerantHintStep = new PlaybookStep();
        spaceTolerantHintStep.setInstruction("Click cart ( hint : button.cart-btn )");
        Assertions.assertTrue(spaceTolerantHintStep.isHintStep());
    }

    @Test
    public void testIsFullPageVisualStep()
    {
        final PlaybookStep standardVisualStep = new PlaybookStep();
        standardVisualStep.setInstruction("Verify footer links (visual)");
        Assertions.assertTrue(standardVisualStep.isVisualStep());
        Assertions.assertFalse(standardVisualStep.isFullPageVisualStep());

        final PlaybookStep visualFullNoSpace = new PlaybookStep();
        visualFullNoSpace.setInstruction("Inspect footer (visual:full)");
        Assertions.assertTrue(visualFullNoSpace.isVisualStep());
        Assertions.assertTrue(visualFullNoSpace.isFullPageVisualStep());

        final PlaybookStep visualFullSpaces = new PlaybookStep();
        visualFullSpaces.setInstruction("Inspect full page overview ( visual : full )");
        Assertions.assertTrue(visualFullSpaces.isVisualStep());
        Assertions.assertTrue(visualFullSpaces.isFullPageVisualStep());
    }

    @Test
    public void testDurationAndDelayFields() throws Exception
    {
        final PlaybookStep step = new PlaybookStep();
        step.setInstruction("Click submit");
        step.setDurationMs(350L);
        step.setDelayMs(700L);

        Assertions.assertEquals(350L, step.getDurationMs());
        Assertions.assertEquals(700L, step.getDelayMs());

        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(step);
        Assertions.assertTrue(json.contains("\"durationMs\":350") || json.contains("\"durationMs\" : 350"));
        Assertions.assertTrue(json.contains("\"delayMs\":700") || json.contains("\"delayMs\" : 700"));

        final PlaybookStep deserialized = mapper.readValue(json, PlaybookStep.class);
        Assertions.assertEquals(350L, deserialized.getDurationMs());
        Assertions.assertEquals(700L, deserialized.getDelayMs());
    }

    @Test
    public void testTargetFrameworkAndDomFeatureVectorSerialization() throws Exception
    {
        final PlaybookStep step = new PlaybookStep();
        step.setInstruction("Click submit button");
        step.setTargetFramework("SELENIUM_SELENIDE");
        step.setSemanticContext("Primary checkout purchase button");
        step.setDomFeatureVector(new DomFeatureVector(
            "button",
            "Place Order",
            Set.of("btn", "btn-primary"),
            Map.of("type", "submit", "id", "btn-order"),
            "button",
            "Place Order",
            "form",
            3
        ));

        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(step);

        Assertions.assertTrue(json.contains("\"targetFramework\" : \"SELENIUM_SELENIDE\"") || json.contains("\"targetFramework\":\"SELENIUM_SELENIDE\""));
        Assertions.assertTrue(json.contains("\"schemaVersion\" : \"" + PlaybookStep.CURRENT_SCHEMA_VERSION + "\"") || json.contains("\"schemaVersion\":\"" + PlaybookStep.CURRENT_SCHEMA_VERSION + "\""));
        Assertions.assertTrue(json.contains("\"semanticContext\" : \"Primary checkout purchase button\"") || json.contains("\"semanticContext\":\"Primary checkout purchase button\""));
        Assertions.assertTrue(json.contains("\"domFeatureVector\""));

        final PlaybookStep deserialized = mapper.readValue(json, PlaybookStep.class);
        Assertions.assertEquals("SELENIUM_SELENIDE", deserialized.getTargetFramework());
        Assertions.assertEquals(PlaybookStep.CURRENT_SCHEMA_VERSION, deserialized.getSchemaVersion());
        Assertions.assertEquals("Primary checkout purchase button", deserialized.getSemanticContext());
        Assertions.assertNotNull(deserialized.getDomFeatureVector());
        Assertions.assertEquals("button", deserialized.getDomFeatureVector().getTag());
        Assertions.assertEquals("Place Order", deserialized.getDomFeatureVector().getText());
    }

    @Test
    public void testIncompatibleFrameworkValidationOnExecution() throws Exception
    {
        final String json = """
            [
              {
                "instruction": "Click on cart",
                "targetFramework": "PLAYWRIGHT",
                "schemaVersion": "3.0"
              }
            ]
            """;
        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("mock_playbook.json", json);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("mock_playbook.json", manager);
        Assertions.assertNotNull(playbook);
        Assertions.assertEquals(1, playbook.getSteps().size());
        Assertions.assertEquals("PLAYWRIGHT", playbook.getSteps().get(0).getTargetFramework());
        final AiSession session = AiSession.mock(new SessionData(), new LlmRegistry(), new ExecutionEventBus(), new MockTargetExecutor());
        session.getExecutionContext().getTransientData().put("playbook.steps", playbook.getSteps());
        final StateMachineRunner runner = new StateMachineRunner(session);
        Assertions.assertThrows(IncompatibleFrameworkException.class, () -> runner.run());
    }

    @Test
    public void testPlaybookStepWithRegexActionSerialization() throws Exception
    {
        final PlaybookStep step = new PlaybookStep();
        step.setInstruction("An order number is shown in the form 'V-[0-9]+-US'.");
        final Action action = new Action("ASSERT", "[data-ai='xcuulzml']", List.of("V-[0-9]+-US"),
                "Extracted ASSERT action", "Matching dynamic order number pattern", true);
        step.getActions().add(action);

        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(step);
        Assertions.assertTrue(json.contains("\"isRegex\":true") || json.contains("\"isRegex\" : true"));

        final PlaybookStep deserialized = mapper.readValue(json, PlaybookStep.class);
        Assertions.assertEquals(1, deserialized.getActions().size());
        final Action deserializedAction = deserialized.getActions().get(0);
        Assertions.assertEquals("ASSERT", deserializedAction.getType());
        Assertions.assertEquals("[data-ai='xcuulzml']", deserializedAction.getTarget());
        Assertions.assertEquals("V-[0-9]+-US", deserializedAction.getValue());
        Assertions.assertTrue(deserializedAction.isRegex());
    }

    @Test
    public void testTimeoutParsingAndSerialization() throws Exception
    {
        final PlaybookStep stepMs = new PlaybookStep();
        stepMs.setInstruction("Wait for loader to vanish (timeout: 5000ms)");
        Assertions.assertEquals("Wait for loader to vanish", stepMs.getInstruction());
        Assertions.assertEquals(5000L, stepMs.getTimeoutMs());

        final PlaybookStep stepSec = new PlaybookStep();
        stepSec.setInstruction("Wait for response (timeout: 10s)");
        Assertions.assertEquals("Wait for response", stepSec.getInstruction());
        Assertions.assertEquals(10000L, stepSec.getTimeoutMs());

        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(stepSec);
        Assertions.assertTrue(json.contains("\"timeoutMs\":10000") || json.contains("\"timeoutMs\" : 10000"));

        final PlaybookStep deserialized = mapper.readValue(json, PlaybookStep.class);
        Assertions.assertEquals(10000L, deserialized.getTimeoutMs());
    }

    @Test
    public void testGetFullInstructionAndInteractiveSubSteps()
    {
        final PlaybookStep parent = new PlaybookStep("Locate the promo code input field:");
        Assertions.assertFalse(parent.hasInteractiveSubSteps());
        Assertions.assertEquals("Locate the promo code input field:", parent.getFullInstruction());

        final PlaybookStep sub1 = new PlaybookStep("clear its content");
        final PlaybookStep sub2 = new PlaybookStep("type 'FREEGIFT' into it");
        final PlaybookStep sub3 = new PlaybookStep("Submit the promo code form.");
        final PlaybookStep sub4 = new PlaybookStep("Assert that the cart items table contains a line item for 'Free Bonus Gift'.");

        parent.getSubSteps().addAll(List.of(sub1, sub2, sub3, sub4));
        sub1.setParent(parent);
        sub2.setParent(parent);
        sub3.setParent(parent);
        sub4.setParent(parent);

        Assertions.assertTrue(parent.hasInteractiveSubSteps());
        final String full = parent.getFullInstruction();
        Assertions.assertTrue(full.contains("Locate the promo code input field:"));
        Assertions.assertTrue(full.contains("  - clear its content"));
        Assertions.assertTrue(full.contains("  - type 'FREEGIFT' into it"));
        Assertions.assertTrue(full.contains("  - Submit the promo code form."));
        Assertions.assertTrue(full.contains("  - Assert that the cart items table contains a line item for 'Free Bonus Gift'."));

        final PlaybookStep pureAssertParent = new PlaybookStep("Verify cart details:");
        pureAssertParent.getSubSteps().add(new PlaybookStep("Verify total is $10.00"));
        Assertions.assertFalse(pureAssertParent.hasInteractiveSubSteps());
    }

    @Test
    public void testSubStepModifierInheritance()
    {
        final PlaybookStep parent = new PlaybookStep("Locate promo field:");
        final PlaybookStep subAction = new PlaybookStep("type 'DISCOUNT' into field");
        final PlaybookStep subBug = new PlaybookStep("Assert item 'Free Gift' is present (bug: PROMO-101)");
        final PlaybookStep subOptional = new PlaybookStep("Check badge (optional)");
        final PlaybookStep subContinue = new PlaybookStep("Check tooltip (continue-on-error)");
        final PlaybookStep subNoHealing = new PlaybookStep("Click legacy link (no-healing)");
        final PlaybookStep subNoReplay = new PlaybookStep("Generate OTP token (no-replay)");

        parent.getSubSteps().addAll(List.of(subAction, subBug, subOptional, subContinue, subNoHealing, subNoReplay));

        Assertions.assertTrue(parent.isBug());
        Assertions.assertEquals("PROMO-101", parent.getBugDetails());
        Assertions.assertTrue(parent.isOptional());
        Assertions.assertTrue(parent.isContinueOnError());
        Assertions.assertTrue(parent.isNoHealing());
        Assertions.assertTrue(parent.isNoReplay());

        final PlaybookStep cleanParent = new PlaybookStep("Standard step:");
        cleanParent.getSubSteps().add(new PlaybookStep("click search button"));
        Assertions.assertFalse(cleanParent.isBug());
        Assertions.assertNull(cleanParent.getBugDetails());
        Assertions.assertFalse(cleanParent.isOptional());
        Assertions.assertFalse(cleanParent.isContinueOnError());
        Assertions.assertFalse(cleanParent.isNoHealing());
        Assertions.assertFalse(cleanParent.isNoReplay());
    }

    @Test
    public void testCompositeStepModifierSerializationRoundTrip() throws Exception
    {
        final PlaybookStep parent = new PlaybookStep("Locate promo field:");
        final PlaybookStep subAction = new PlaybookStep("type 'DISCOUNT' into field");
        final PlaybookStep subBug = new PlaybookStep("Assert item 'Free Gift' is present (bug: PROMO-101)");
        final PlaybookStep subOptional = new PlaybookStep("Check badge (optional)");
        final PlaybookStep subNoReplay = new PlaybookStep("Generate OTP token (no-replay)");

        parent.getSubSteps().addAll(List.of(subAction, subBug, subOptional, subNoReplay));
        subAction.setParent(parent);
        subBug.setParent(parent);
        subOptional.setParent(parent);
        subNoReplay.setParent(parent);

        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(parent);

        Assertions.assertTrue(json.contains("\"bug\":false") || json.contains("\"bug\" : false"));
        Assertions.assertTrue(json.contains("\"optional\":false") || json.contains("\"optional\" : false"));
        Assertions.assertTrue(json.contains("\"noReplay\":false") || json.contains("\"noReplay\" : false"));

        final PlaybookStep deserialized = mapper.readValue(json, PlaybookStep.class);
        for (final PlaybookStep child : deserialized.getSubSteps())
        {
            child.setParent(deserialized);
        }

        Assertions.assertTrue(deserialized.isBug());
        Assertions.assertEquals("PROMO-101", deserialized.getBugDetails());
        Assertions.assertTrue(deserialized.isOptional());
        Assertions.assertTrue(deserialized.isNoReplay());

        final PlaybookStep deserializedSubAction = deserialized.getSubSteps().get(0);
        Assertions.assertFalse(deserializedSubAction.isBug(), "subAction must not inherit bug from sibling");
        Assertions.assertFalse(deserializedSubAction.isOptional(), "subAction must not inherit optional from sibling");
        Assertions.assertFalse(deserializedSubAction.isNoReplay(), "subAction must not inherit noReplay from sibling");

        final PlaybookStep deserializedSubBug = deserialized.getSubSteps().get(1);
        Assertions.assertTrue(deserializedSubBug.isBug());
        Assertions.assertEquals("PROMO-101", deserializedSubBug.getBugDetails());
    }
}
