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
import org.neodymium.ai.model.SemanticIntent;
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
     * Verifies that compileSystemMessage uses the dedicated lightweight visual prompt at ContextLevel.VISUAL
     * and excludes DOM locator rules and candidate locator rules.
     */
    @Test
    public void testCompileSystemMessageVisualLevelUsesVisualOnlyPrompt()
    {
        AiAgentPrompts.clearCache();
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.VISUAL);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, new SelenideTargetExecutor());

        final String systemMsg = prompt.compileSystemMessage(context);
        assertNotNull(systemMsg);
        assertTrue(systemMsg.contains("Analyze the visual screenshot to fulfill the active test instruction."), "Visual system prompt must contain visual instruction.");
        assertTrue(systemMsg.contains("## Execution Guidelines"), "Visual system prompt must contain execution guidelines.");
        assertFalse(systemMsg.contains("## Selenide/Selenium Engine Locators"), "Visual system prompt must not include Selenide locator rule.");
        assertFalse(systemMsg.contains("## Candidate Locators & Ambiguity Evaluation"), "Visual system prompt must not include candidate locators rule.");
        assertFalse(systemMsg.contains("## Action Rules"), "Visual system prompt must not include DOM action rules.");
    }

    /**
     * Verifies that compileSystemMessage uses the standard action extraction prompt at non-VISUAL levels (e.g. VISUAL_LEAN, VISUAL_RICH, STANDARD).
     */
    @Test
    public void testCompileSystemMessageNonVisualLevelsUseStandardPrompt()
    {
        AiAgentPrompts.clearCache();
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();

        for (final ContextLevel level : new ContextLevel[] { ContextLevel.MINIMAL, ContextLevel.LEAN, ContextLevel.STANDARD, ContextLevel.RICH, ContextLevel.VISUAL_LEAN, ContextLevel.VISUAL_RICH })
        {
            final ExecutionContext context = new ExecutionContext(null);
            context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, level);

            final String systemMsg = prompt.compileSystemMessage(context);
            assertNotNull(systemMsg);
            assertTrue(systemMsg.contains("Analyze current DOM and visual state"), "Level " + level + " must use standard action extraction prompt.");
        }
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

    /**
     * Verifies that compileUserMessage adds ceiling level notice and omits next escalation when at VISUAL_RICH.
     */
    @Test
    public void testCompileUserMessageCeilingLevelNotice()
    {
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Verify bonus gift");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.VISUAL_RICH);

        final String userMessage = prompt.compileUserMessage(context);

        assertNotNull(userMessage);
        assertTrue(userMessage.contains("[CURRENT_LEVEL]    VISUAL_RICH"));
        assertFalse(userMessage.contains("[NEXT_ESCALATION]"));
        assertTrue(userMessage.contains("MAXIMUM CONTEXT LEVEL REACHED"));
        assertTrue(userMessage.contains("Do NOT emit speculative actions"));
    }

    /**
     * Verifies that compileUserMessage does not output previous attempt failure when error is ToLevelEscalationException.
     */
    @Test
    public void testCompileUserMessageSuppressesToLevelEscalationException()
    {
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Verify bonus gift");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.VISUAL_RICH);
        context.getTransientData().put(
            ExecutionContext.KEY_LAST_EXECUTION_ERROR,
            new ToLevelEscalationException("Escalating context", "VISUAL_RICH")
        );

        final String userMessage = prompt.compileUserMessage(context);

        assertNotNull(userMessage);
        assertFalse(userMessage.contains("⚠️ PREVIOUS ATTEMPT FAILURE"));
    }

    /**
     * Verifies that parseResponse throws DivergenceException when escalation is requested at the ceiling (VISUAL_RICH).
     */
    @Test
    public void testParseResponseEscalateAtCeilingThrowsDivergenceException()
    {
        final String rawJson = """
            {
              "status": "ESCALATE",
              "targetContextLevel": "VISUAL_RICH",
              "reasoning": "The cart table does not show 'Free Bonus Gift'."
            }
            """;

        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.VISUAL_RICH);

        final DivergenceException ex = assertThrows(DivergenceException.class, () -> {
            prompt.parseResponse(rawJson, context);
        });

        assertTrue(ex.getMessage().contains("The cart table does not show 'Free Bonus Gift'."));
    }

    /**
     * Verifies that parseResponse throws DivergenceException when assertionSatisfied is false despite status SUCCESS.
     */
    @Test
    public void testParseResponseStructuralInconsistencyThrowsDivergenceException()
    {
        final String rawJson = """
            {
              "reasoning": "Upon inspecting the visual state, there is no green checkmark at the center of the screen.",
              "assertionSatisfied": false,
              "status": "SUCCESS",
              "targetContextLevel": "VISUAL",
              "actions": []
            }
            """;

        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.VISUAL);

        final DivergenceException ex = assertThrows(DivergenceException.class, () -> {
            prompt.parseResponse(rawJson, context);
        });

        assertTrue(ex.getMessage().contains("Upon inspecting the visual state, there is no green checkmark"));
    }

    /**
     * Verifies that parseResponse discards mutating interactive actions (e.g. CLICK) on visual verification steps.
     */
    @Test
    public void testParseResponseVisualStepDiscardsMutatingActions() throws Exception
    {
        final String rawJson = """
            {
              "reasoning": "There are data input forms on the left and order summary on the right.",
              "assertionSatisfied": true,
              "status": "SUCCESS",
              "targetContextLevel": "VISUAL",
              "actions": [ {
                "action": "CLICK",
                "locator": "#purchase-btn",
                "reasoning": "Speculative click"
              } ]
            }
            """;

        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.VISUAL);

        final org.neodymium.ai.model.PlaybookStep step = new org.neodymium.ai.model.PlaybookStep(
            "There are data input forms on the left and an order summary on the right (visual)."
        );
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);

        final List<Action> actions = prompt.parseResponse(rawJson, context);
        assertNotNull(actions);
        assertTrue(actions.isEmpty(), "Mutating actions should be discarded on visual steps");
    }

    /**
     * Verifies that parseResponse allows executable actions (e.g. NAVIGATE) when status is SUCCESS even if assertionSatisfied is false.
     */
    @Test
    public void testParseResponseActionStepAllowsAssertionSatisfiedFalseWithExecutableActions() throws Exception
    {
        final String rawJson = """
            {
              "reasoning": "The current page is empty. Navigating to the URL.",
              "assertionSatisfied": false,
              "status": "SUCCESS",
              "targetContextLevel": "LEAN",
              "actions": [ {
                "action": "NAVIGATE",
                "locator": "",
                "value": "https://localhost:8543/verla-perfect/index.html",
                "reasoning": "Navigate to the specified URL"
              } ]
            }
            """;

        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.LEAN);

        final List<Action> actions = prompt.parseResponse(rawJson, context);
        assertNotNull(actions);
        assertEquals(1, actions.size());
        assertEquals("NAVIGATE", actions.get(0).getType());
        assertEquals("https://localhost:8543/verla-perfect/index.html", actions.get(0).getValue());
    }

    /**
     * Verifies that compileUserMessage includes the classified semantic intent header.
     */
    @Test
    public void testCompileUserMessageIncludesSemanticIntent()
    {
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Verify checkout total is 99.00");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.LEAN);
        context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.ASSERT);

        final String userMessage = prompt.compileUserMessage(context);

        assertNotNull(userMessage);
        assertTrue(userMessage.contains("[INSTRUCTION]      Verify checkout total is 99.00"));
        assertTrue(userMessage.contains("[SEMANTIC_INTENT]  ASSERT"));
        assertTrue(userMessage.contains("[CURRENT_LEVEL]    LEAN"));
    }

    /**
     * Verifies that parseResponse discards mutating actions when step has assertion intent.
     */
    @Test
    public void testParseResponseDiscardsMutatingActionsOnAssertionIntent() throws Exception
    {
        final String rawJson = """
            {
              "status": "SUCCESS",
              "targetContextLevel": "LEAN",
              "reasoning": "Wait for confirmation message to appear",
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#purchase-btn",
                  "reasoning": "Speculative click attempt"
                },
                {
                  "action": "ASSERT",
                  "locator": "#order-confirmation",
                  "value": "Thank you",
                  "reasoning": "Verify order confirmation text"
                }
              ]
            }
            """;

        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.ASSERT);

        final List<Action> actions = prompt.parseResponse(rawJson, context);

        assertNotNull(actions);
        assertEquals(1, actions.size(), "Mutating action (CLICK) should be discarded on ASSERT intent");
        assertEquals("ASSERT", actions.get(0).getType());
        assertEquals("#order-confirmation", actions.get(0).getTarget());
    }

    /**
     * Verifies that parseResponse does not set hasElse to true when else is an empty array,
     * but sets hasElse to true when else contains actions or when hasElse is explicitly true.
     */
    @Test
    public void testParseResponseBranchActionHasElseBehavior() throws Exception
    {
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);

        // Case 1: 1-way if branch with empty "else": [] should NOT set hasElse to true
        final String jsonEmptyElse = """
            {
              "status": "SUCCESS",
              "actions": [
                {
                  "action": "BRANCH",
                  "condition": [
                    { "action": "ASSERT", "locator": "#state", "value": "present" }
                  ],
                  "then": [
                    { "action": "TYPE", "locator": "#state", "value": "CA" }
                  ],
                  "else": []
                }
              ]
            }
            """;

        final List<Action> actionsEmpty = prompt.parseResponse(jsonEmptyElse, context);
        assertNotNull(actionsEmpty);
        assertEquals(1, actionsEmpty.size());
        final Action actionEmpty = actionsEmpty.get(0);
        assertEquals("BRANCH", actionEmpty.getType());
        assertFalse(actionEmpty.hasElse(), "Empty 'else' array must not mark hasElse as true");

        // Case 2: Populated "else" array should set hasElse to true
        final String jsonPopulatedElse = """
            {
              "status": "SUCCESS",
              "actions": [
                {
                  "action": "BRANCH",
                  "condition": [
                    { "action": "ASSERT", "locator": "#state", "value": "present" }
                  ],
                  "then": [
                    { "action": "TYPE", "locator": "#state", "value": "CA" }
                  ],
                  "else": [
                    { "action": "CLICK", "locator": "#skip-btn" }
                  ]
                }
              ]
            }
            """;

        final List<Action> actionsPopulated = prompt.parseResponse(jsonPopulatedElse, context);
        assertNotNull(actionsPopulated);
        assertEquals(1, actionsPopulated.size());
        final Action actionPopulated = actionsPopulated.get(0);
        assertTrue(actionPopulated.hasElse(), "Populated 'else' array must mark hasElse as true");

        // Case 3: Explicit "hasElse": true with empty else array
        final String jsonExplicitHasElse = """
            {
              "status": "SUCCESS",
              "actions": [
                {
                  "action": "BRANCH",
                  "hasElse": true,
                  "condition": [
                    { "action": "ASSERT", "locator": "#state", "value": "present" }
                  ],
                  "then": [
                    { "action": "TYPE", "locator": "#state", "value": "CA" }
                  ],
                  "else": []
                }
              ]
            }
            """;

        final List<Action> actionsExplicit = prompt.parseResponse(jsonExplicitHasElse, context);
        assertNotNull(actionsExplicit);
        assertEquals(1, actionsExplicit.size());
        final Action actionExplicit = actionsExplicit.get(0);
        assertTrue(actionExplicit.hasElse(), "Explicit hasElse=true must be preserved");
    }

    @Test
    public void testParseDistinctAssertActions() throws Exception
    {
        final String rawJson = """
            {
              "status": "SUCCESS",
              "reasoning": "Parsed various assertion actions",
              "actions": [
                {
                  "action": "ASSERT_EXISTS",
                  "locator": "#prefecture",
                  "reasoning": "Verify prefecture field is present"
                },
                {
                  "action": "ASSERT_ABSENT",
                  "locator": "#cookie-modal",
                  "reasoning": "Verify cookie modal is closed"
                },
                {
                  "action": "ASSERT_TEXT",
                  "locator": "#headline",
                  "value": "Checkout",
                  "reasoning": "Verify headline text"
                },
                {
                  "action": "ASSERT_VALUE",
                  "locator": "#first-name",
                  "value": "Alice",
                  "reasoning": "Verify input value"
                },
                {
                  "action": "ASSERT_CHECKED",
                  "locator": "#newsletter",
                  "reasoning": "Verify checkbox is checked"
                },
                {
                  "action": "ASSERT_DISABLED",
                  "locator": "#submit-btn",
                  "reasoning": "Verify button is disabled"
                },
                {
                  "action": "ASSERT_URL",
                  "locator": "url",
                  "value": "https://example.com/checkout",
                  "reasoning": "Verify page URL"
                },
                {
                  "action": "ASSERT_ATTRIBUTE",
                  "locator": "#username",
                  "value": "placeholder=Enter username",
                  "reasoning": "Verify placeholder attribute"
                },
                {
                  "action": "ASSERT_COUNT",
                  "locator": ".cart-item",
                  "value": "3",
                  "reasoning": "Verify 3 cart items"
                }
              ]
            }
            """;
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);

        final List<Action> actions = prompt.parseResponse(rawJson, context);
        assertNotNull(actions);
        assertEquals(9, actions.size());

        assertEquals("ASSERT_EXISTS", actions.get(0).getType());
        assertEquals("#prefecture", actions.get(0).getTarget());

        assertEquals("ASSERT_ABSENT", actions.get(1).getType());
        assertEquals("#cookie-modal", actions.get(1).getTarget());

        assertEquals("ASSERT_TEXT", actions.get(2).getType());
        assertEquals("#headline", actions.get(2).getTarget());
        assertEquals("Checkout", actions.get(2).getValue());

        assertEquals("ASSERT_VALUE", actions.get(3).getType());
        assertEquals("#first-name", actions.get(3).getTarget());
        assertEquals("Alice", actions.get(3).getValue());

        assertEquals("ASSERT_CHECKED", actions.get(4).getType());
        assertEquals("#newsletter", actions.get(4).getTarget());

        assertEquals("ASSERT_DISABLED", actions.get(5).getType());
        assertEquals("#submit-btn", actions.get(5).getTarget());

        assertEquals("ASSERT_URL", actions.get(6).getType());
        assertEquals("url", actions.get(6).getTarget());
        assertEquals("https://example.com/checkout", actions.get(6).getValue());

        assertEquals("ASSERT_ATTRIBUTE", actions.get(7).getType());
        assertEquals("#username", actions.get(7).getTarget());
        assertEquals("placeholder=Enter username", actions.get(7).getValue());

        assertEquals("ASSERT_COUNT", actions.get(8).getType());
        assertEquals(".cart-item", actions.get(8).getTarget());
        assertEquals("3", actions.get(8).getValue());
    }

    @Test
    public void testParseBranchWithAssertExistsCondition() throws Exception
    {
        final String rawJson = """
            {
              "status": "SUCCESS",
              "reasoning": "Conditional branch based on prefecture existence",
              "actions": [
                {
                  "action": "BRANCH",
                  "condition": [
                    { "action": "ASSERT_EXISTS", "locator": "input[name='prefecture'], #prefecture" }
                  ],
                  "then": [
                    { "action": "TYPE", "locator": "input[name='prefecture'], #prefecture", "value": "${prefecture}" }
                  ]
                }
              ]
            }
            """;
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);

        final List<Action> actions = prompt.parseResponse(rawJson, context);
        assertNotNull(actions);
        assertEquals(1, actions.size());

        final Action branch = actions.get(0);
        assertEquals("BRANCH", branch.getType());
        assertNotNull(branch.getCondition());
        assertEquals(1, branch.getCondition().size());
        assertEquals("ASSERT_EXISTS", branch.getCondition().get(0).getType());
        assertEquals("input[name='prefecture'], #prefecture", branch.getCondition().get(0).getTarget());

        assertNotNull(branch.getThen());
        assertEquals(1, branch.getThen().size());
        assertEquals("TYPE", branch.getThen().get(0).getType());
        assertEquals("${prefecture}", branch.getThen().get(0).getValue());
    }
}


