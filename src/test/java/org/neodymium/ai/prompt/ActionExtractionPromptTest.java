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
package org.neodymium.ai.prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.executor.rest.RestTargetExecutor;
import org.neodymium.ai.executor.selenide.SelenideTargetExecutor;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.model.DomFeatureVector;
import org.neodymium.ai.pipeline.DivergenceException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.ToLevelEscalationException;

/**
 * Unit tests validating user prompt message compilation, action extraction on FAILED responses,
 * and escalation level progression in ActionExtractionPrompt.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class ActionExtractionPromptTest
{
    /**
     * Default constructor.
     */
    public ActionExtractionPromptTest()
    {
    }

    /**
     * Verifies that compileUserMessage includes the current context level and next escalation target level.
     */
    @Test
    public void testCompileUserMessageIncludesContextLevel()
    {
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Verify free gift item");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.RICH);

        final String userMessage = prompt.compileUserMessage(context);

        assertNotNull(userMessage);
        assertTrue(userMessage.contains("## Execution Context"));
        assertTrue(userMessage.contains("[INSTRUCTION]      Verify free gift item"));
        assertTrue(userMessage.contains("[CURRENT_LEVEL]    RICH"));
        assertTrue(userMessage.contains("[NEXT_ESCALATION]  VISUAL_RICH"));
    }

    /**
     * Verifies that parseResponse returns extracted ASSERT actions even when status is FAILED.
     */
    @Test
    public void testParseResponseFailedStatusWithActions() throws Exception
    {
        final String rawJson = """
            {
              "status": "FAILED",
              "targetContextLevel": "STANDARD",
              "reasoning": "The cart table currently only contains 'Premium Off-White shirts' and does not include any free bonus gift item.",
              "actions": [ {
                "action": "ASSERT",
                "locator": ".cart-table-wrapper",
                "value": "Free",
                "reasoning": "Verify that the cart table includes a free bonus gift item."
              } ]
            }
            """;

        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        final List<Action> actions = prompt.parseResponse(rawJson, context);

        assertNotNull(actions);
        assertEquals(1, actions.size());
        final Action action = actions.get(0);
        assertEquals("ASSERT", action.getType());
        assertEquals(".cart-table-wrapper", action.getTarget());
        assertEquals("Free", action.getValue());
    }

    /**
     * Verifies that parseResponse throws DivergenceException when status is FAILED and actions array is empty.
     */
    @Test
    public void testParseResponseFailedStatusWithoutActions()
    {
        final String rawJson = """
            {
              "status": "FAILED",
              "reasoning": "Visual element missing.",
              "actions": []
            }
            """;

        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);

        final DivergenceException ex = assertThrows(DivergenceException.class, () -> {
            prompt.parseResponse(rawJson, context);
        });

        assertTrue(ex.getMessage().contains("Visual element missing."));
    }

    /**
     * Verifies that parseResponse auto-corrects a retrograde escalation level when requested target is lower or equal.
     */
    @Test
    public void testParseResponseEscalateRetrogradeCorrection()
    {
        final String rawJson = """
            {
              "status": "ESCALATE",
              "targetContextLevel": "STANDARD",
              "reasoning": "Need higher context."
            }
            """;

        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.RICH);

        final ToLevelEscalationException ex = assertThrows(ToLevelEscalationException.class, () -> {
            prompt.parseResponse(rawJson, context);
        });

        // Current level is RICH, so requesting STANDARD should auto-correct to VISUAL_RICH
        assertEquals("VISUAL_RICH", ex.getTargetLevel());
    }

    /**
     * Verifies that parseResponse escalates context level when status is FAILED, actions are empty,
     * and targetContextLevel specifies a level higher than the active context level.
     */
    @Test
    public void testParseResponseFailedStatusEscalatesWhenTargetContextLevelHigher()
    {
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.VISUAL_LEAN);

        final String rawJson = """
            {
              "status": "FAILED",
              "targetContextLevel": "VISUAL_RICH",
              "reasoning": "The green checkmark is missing in the current text DOM.",
              "actions": []
            }
            """;

        final ToLevelEscalationException ex = assertThrows(ToLevelEscalationException.class, () -> prompt.parseResponse(rawJson, context));
        assertEquals("VISUAL_RICH", ex.getTargetLevel());
    }

    /**
     * Verifies that parseResponse auto-escalates context to LEAN when status is FAILED, actions are empty,
     * and current context level is MINIMAL (where static text elements are pruned from DOM).
     */
    @Test
    public void testParseResponseFailedStatusAutoEscalatesAtMinimalContext()
    {
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.MINIMAL);

        final String rawJson = """
            {
              "status": "FAILED",
              "targetContextLevel": "MINIMAL",
              "reasoning": "Looking at the DOM, there is no element displaying the count of search results.",
              "actions": []
            }
            """;

        final ToLevelEscalationException ex = assertThrows(ToLevelEscalationException.class, () -> prompt.parseResponse(rawJson, context));
        assertEquals("LEAN", ex.getTargetLevel());
    }

    /**
     * Verifies system prompt compilation.
     */
    @Test
    public void testCompileSystemMessage()
    {
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);

        final String systemMsg = prompt.compileSystemMessage(context);
        assertNotNull(systemMsg);
        assertTrue(systemMsg.contains("Analyze current DOM and visual state"), "System prompt must contain core instruction.");
    }

    /**
     * Verifies that compileSystemMessage includes the Selenide W3C locator rule in Selenide mode,
     * but excludes it when executing under non-Selenide mode (e.g. RestTargetExecutor).
     */
    @Test
    public void testCompileSystemMessageSelenideModeLocatorRule()
    {
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();

        // 1. Selenide mode (SelenideTargetExecutor present in transient data) -> includes rule
        final ExecutionContext selenideContext = new ExecutionContext(null);
        selenideContext.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, new SelenideTargetExecutor());
        final String selenideSystemMsg = prompt.compileSystemMessage(selenideContext);
        assertNotNull(selenideSystemMsg);
        assertTrue(selenideSystemMsg.contains("## Selenide/Selenium Engine Locators"), "Selenide system prompt must include Selenide engine locator rule.");
        assertTrue(selenideSystemMsg.contains("FORBIDDEN: Playwright pseudo-selectors"), "Selenide system prompt must forbid Playwright pseudo-selectors.");

        // 2. Default/null context -> defaults to Selenide mode for backward compatibility
        final String defaultSystemMsg = prompt.compileSystemMessage(null);
        assertNotNull(defaultSystemMsg);
        assertTrue(defaultSystemMsg.contains("## Selenide/Selenium Engine Locators"), "Default system prompt must include Selenide engine locator rule.");

        // 3. REST mode (RestTargetExecutor present in transient data) -> excludes Selenide rule
        final ExecutionContext restContext = new ExecutionContext(null);
        restContext.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, new RestTargetExecutor());
        final String restSystemMsg = prompt.compileSystemMessage(restContext);
        assertNotNull(restSystemMsg);
        assertFalse(restSystemMsg.contains("## Selenide/Selenium Engine Locators"), "REST system prompt must not include Selenide engine locator rule.");
    }

    /**
     * Verifies that compileSystemMessage includes the Candidate Locators rule when Quality Judge is enabled.
     */
    @Test
    public void testCompileSystemMessageCandidateLocatorsRule()
    {
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final String systemMsg = prompt.compileSystemMessage(null);
        assertNotNull(systemMsg);

        final boolean isJudgeEnabled = org.neodymium.ai.config.AiConfiguration.getInstance().isJudgeEnabled();
        if (isJudgeEnabled)
        {
            assertTrue(systemMsg.contains("## Candidate Locators & Ambiguity Evaluation"), "System prompt must include candidate locators rule when judge is enabled.");
        }
        else
        {
            assertFalse(systemMsg.contains("## Candidate Locators & Ambiguity Evaluation"), "System prompt must omit candidate locators rule when judge is disabled.");
        }
    }

    /**
     * Verifies that parseResponse deserializes domFeatureVector if present in action JSON.
     */
    @Test
    public void testParseResponseWithDomFeatureVector() throws Exception
    {
        final String rawJson = """
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#apply-btn",
                  "value": "",
                  "reasoning": "Click Apply button",
                  "domFeatureVector": {
                    "tag": "button",
                    "text": "Apply",
                    "classes": ["btn", "btn-primary"],
                    "attributes": {"id": "apply-btn", "type": "submit"},
                    "role": "button",
                    "accessibleName": "Apply",
                    "parentTag": "form",
                    "siblingIndex": 3,
                    "x": 100,
                    "y": 200,
                    "width": 80,
                    "height": 30
                  }
                }
              ]
            }
            """;

        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        final List<Action> actions = prompt.parseResponse(rawJson, context);

        assertNotNull(actions);
        assertEquals(1, actions.size());
        final Action action = actions.get(0);
        assertEquals("CLICK", action.getType());
        assertEquals("#apply-btn", action.getTarget());

        final DomFeatureVector vector = action.getDomFeatureVector();
        assertNotNull(vector, "DomFeatureVector must be deserialized into action.");
        assertEquals("button", vector.getTag());
        assertEquals("Apply", vector.getText());
        assertEquals("button", vector.getRole());
        assertEquals("Apply", vector.getAccessibleName());
        assertEquals("form", vector.getParentTag());
        assertEquals(3, vector.getSiblingIndex());
        assertEquals(100, vector.getX());
        assertEquals(200, vector.getY());
        assertEquals(80, vector.getWidth());
        assertEquals(30, vector.getHeight());
        assertTrue(vector.getClasses().contains("btn-primary"));
        assertEquals("submit", vector.getAttributes().get("type"));
        assertTrue(vector.toDetailString().contains("tag=<button>"));
    }

    /**
     * Goal: Verifies that when status is SUCCESS at ContextLevel.VISUAL, any synthetic DOM ASSERT actions
     * generated without DOM context are discarded by the visual guard, preserving the visual reasoning.
     */
    @Test
    public void testVisualGuardDiscardsSyntheticActionsAtVisualLevel() throws Exception
    {
        final String rawJson = """
            {
              "status": "SUCCESS",
              "targetContextLevel": "VISUAL",
              "reasoning": "The form is on the left and order summary is on the right.",
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "form",
                  "value": "Contact Information",
                  "isRegex": false,
                  "reasoning": "Assert data input forms are present on the left"
                },
                {
                  "action": "ASSERT",
                  "locator": ".order-summary",
                  "value": "Order Summary",
                  "isRegex": false,
                  "reasoning": "Assert order summary block is present on the right"
                }
              ]
            }
            """;

        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.VISUAL);
        final org.neodymium.ai.model.PlaybookStep step = new org.neodymium.ai.model.PlaybookStep(
            "There are data input forms on the left and and order summary block on the right (visual)."
        );
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);

        final List<Action> actions = prompt.parseResponse(rawJson, context);

        assertNotNull(actions);
        assertTrue(actions.isEmpty(), "Synthetic DOM ASSERT actions must be discarded at ContextLevel.VISUAL with status SUCCESS");
        assertTrue(step.getActions().isEmpty(), "Step actions list must be empty after visual guard execution");
        assertEquals("The form is on the left and order summary is on the right.", step.getReasoning());
    }

    /**
     * Goal: Verifies that when status is SUCCESS at ContextLevel.STANDARD, valid DOM ASSERT actions
     * are preserved normally.
     */
    @Test
    public void testStandardLevelPreservesAssertActions() throws Exception
    {
        final String rawJson = """
            {
              "status": "SUCCESS",
              "targetContextLevel": "STANDARD",
              "reasoning": "The headline says Checkout",
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#checkout-form-container h1",
                  "value": "Checkout",
                  "isRegex": false,
                  "reasoning": "Verify headline"
                }
              ]
            }
            """;
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.STANDARD);

        final List<Action> actions = prompt.parseResponse(rawJson, context);

        assertNotNull(actions);
        assertEquals(1, actions.size());
        assertEquals("ASSERT", actions.get(0).getType());
        assertEquals("#checkout-form-container h1", actions.get(0).getTarget());
        assertEquals("Checkout", actions.get(0).getValue());
    }

    @Test
    public void testVisualLevelPreservesNavigateActionsOnNavigationStep() throws Exception
    {
        final String rawJson = """
            {
              "status": "SUCCESS",
              "targetContextLevel": "VISUAL",
              "reasoning": "Open the specified URL.",
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "https://localhost:8543/verla-perfect/index.html",
                  "value": "",
                  "isRegex": false,
                  "reasoning": "Navigate to the given URL."
                }
              ]
            }
            """;
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.VISUAL);
        final org.neodymium.ai.model.PlaybookStep step = new org.neodymium.ai.model.PlaybookStep(
            "Open https://localhost:8543/verla-perfect/index.html"
        );
        step.setLineNumber(2);
        step.setSourceFile("RegisterTest.yaml");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);

        final List<Action> actions = prompt.parseResponse(rawJson, context);

        assertNotNull(actions);
        assertEquals(1, actions.size());
        assertEquals("NAVIGATE", actions.get(0).getType());
        assertEquals("https://localhost:8543/verla-perfect/index.html", actions.get(0).getTarget());
        assertEquals(1, step.getActions().size());
        assertEquals("NAVIGATE", step.getActions().get(0).getType());
    }

    @Test
    public void testCompileSystemMessageContainsDeclarativeVsImperativeRule()
    {
        AiAgentPrompts.clearCache();
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final String systemMsg = prompt.compileSystemMessage(null);
        assertNotNull(systemMsg);
        assertTrue(systemMsg.contains("Declarative vs. Imperative Instructions"), "System prompt must contain declarative vs imperative rule.");
        assertTrue(systemMsg.contains("NOT an imperative command to execute"), "System prompt must clarify that descriptive affordances are not commands to execute.");
    }

    /**
     * Verifies that the compiled system message contains the strict data fidelity guideline
     * and the dedicated TYPE action rule to prevent LLM value hallucination.
     */
    @Test
    public void testCompileSystemMessageContainsDataFidelityAndTypeRule()
    {
        AiAgentPrompts.clearCache();
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final String systemMsg = prompt.compileSystemMessage(null);
        assertNotNull(systemMsg);
        assertTrue(systemMsg.contains("Input & Assertion Data Fidelity"), "System prompt must contain data fidelity guideline.");
        assertTrue(systemMsg.contains("NEVER invent, hallucinate, or substitute synthetic sample data"), "System prompt must forbid synthetic sample data.");
        assertTrue(systemMsg.contains("- **TYPE**: Target `input`, `textarea`, or `contenteditable`"), "System prompt must contain explicit TYPE action rule.");
    }

    /**
     * Verifies that the lite model add-on includes the value fidelity directive for gemini-3.5-flash-lite.
     */
    @Test
    public void testLiteModelAddonContainsValueFidelityDirective()
    {
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_ACTIVE_MODEL, "gemini-3.5-flash-lite");

        final String addon = SystemPromptAddonHelper.getAddon("general", context);
        assertNotNull(addon, "Addon for gemini-3.5-flash-lite should not be null.");
        assertTrue(addon.contains("Values"), "Lite model addon must contain values directive.");
        assertTrue(addon.contains("Copy the exact literal text from the instruction into 'value'"), "Lite model addon must mandate copying literal text.");
        assertTrue(addon.contains("Locators"), "Lite model addon must contain locators directive.");
    }
}

