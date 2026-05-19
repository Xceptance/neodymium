/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.ai.core;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.xceptance.neodymium.ai.action.ActionRegistry;
import com.xceptance.neodymium.ai.action.AiActionPlugin;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookStep;

/**
 * Contains the system prompt templates for the AI agent.
 * Separated into its own class for easy tuning and experimentation.
 * Prompts are loaded from the classpath at 'ai-prompts/' to allow 
 * external projects to easily override them.
 *
 * // AI-generated: Gemini 2.0 Flash
 */
public final class AiAgentPrompts
{
    private AiAgentPrompts()
    {
        // utility class
    }

    private static String loadPrompt(final String filename)
    {
        final String resourcePath = "ai-prompts/" + filename;
        try (final InputStream is = AiAgentPrompts.class.getClassLoader().getResourceAsStream(resourcePath))
        {
            if (is != null)
            {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            else
            {
                throw new RuntimeException("Could not find prompt file on classpath: " + resourcePath);
            }
        }
        catch (final Exception e)
        {
            throw new RuntimeException("Failed to load prompt file: " + resourcePath, e);
        }
    }

    /**
     * System prompt for standard exploration.
     */
    public static final String SYSTEM_EXPLORATION_PROMPT = loadPrompt("system-exploration-prompt.txt");

    /**
     * Template for standard exploration steps.
     */
    public static final String EXPLORATION_PROMPT_TEMPLATE = loadPrompt("exploration-prompt-template.txt");

    /**
     * Template for v2 exploration steps.
     */
    public static final String V2_EXPLORATION_PROMPT_TEMPLATE = loadPrompt("v2-exploration-prompt-template.txt");

    /**
     * System prompt for v2 exploration.
     */
    public static final String V2_SYSTEM_EXPLORATION_PROMPT = loadPrompt("v2-system-exploration-prompt.txt");

    /**
     * System prompt for v2 extraction.
     */
    public static final String V2_EXTRACTION_PROMPT = loadPrompt("v2-extraction-prompt.txt");

    /**
     * System prompt for v2 extraction retry.
     */
    public static final String V2_EXTRACTION_RETRY_PROMPT = loadPrompt("v2-extraction-retry-prompt.txt");

    /**
     * Base system prompt.
     */
    public static final String SYSTEM_PROMPT = loadPrompt("system-prompt.txt");

    /**
     * Standard user prompt template.
     */
    public static final String USER_PROMPT_TEMPLATE = loadPrompt("user-prompt-template.txt");

    /**
     * Prompt template for when an action fails and needs a retry.
     */
    public static final String RETRY_PROMPT_TEMPLATE = loadPrompt("retry-prompt-template.txt");

    /**
     * Prompt template for when no actions were generated and a retry is needed.
     */
    public static final String NO_ACTIONS_RETRY_PROMPT_TEMPLATE = loadPrompt("no-actions-retry-prompt-template.txt");

    /**
     * System prompt for playbook healing.
     */
    public static final String SYSTEM_HEALING_PROMPT = loadPrompt("system-healing-prompt.txt");

    /**
     * Prompt template for playbook healing.
     */
    public static final String HEALING_PROMPT_TEMPLATE = loadPrompt("healing-prompt-template.txt");

    /**
     * Builds the exploration prompt.
     *
     * @param intent            the overarching goal
     * @param sutContext        application specific instructions
     * @param subgoal           current subgoal
     * @param history           action history
     * @param domContext        current DOM representation
     * @param previousActionStr the previously executed action
     * @param knownBindings     previously extracted or known variables
     * @return the fully formatted prompt
     */
    public static String buildExplorationPrompt(final String intent, final String sutContext, final String subgoal, final String history,
        final String domContext, final String previousActionStr, final Map<String, String> knownBindings)
    {
        String sutContextBlock = "";
        if (sutContext != null && !sutContext.trim().isEmpty())
        {
            sutContextBlock = "\n### SUT Specific Instructions (Application Context)\n" + sutContext + "\n";
        }
        
        String knownBindingsBlock = "";
        if (knownBindings != null && !knownBindings.isEmpty())
        {
            final StringBuilder sb = new StringBuilder();
            for (final Entry<String, String> entry : knownBindings.entrySet())
            {
                sb.append("  ${").append(entry.getKey()).append("} = '").append(entry.getValue()).append("'\n");
            }
            knownBindingsBlock = "\n## Known Data Bindings\n" + sb.toString();
        }
        
        return EXPLORATION_PROMPT_TEMPLATE
            .replace("{intent}", intent)
            .replace("{sutContextBlock}", sutContextBlock)
            .replace("{subgoal}", subgoal != null && !subgoal.isEmpty() ? subgoal : "None (Starting First Phase)")
            .replace("{knownBindingsBlock}", knownBindingsBlock)
            .replace("{history}", history != null && !history.trim().isEmpty() ? history : "None (Initial Step)")
            .replace("{domContext}", domContext)
            .replace("{previousAction}", previousActionStr != null ? previousActionStr : "None (Initial Step)");
    }

    /**
     * Builds the V2 exploration prompt.
     *
     * @param intent            the overarching goal
     * @param sutContext        application specific instructions
     * @param subgoal           current subgoal
     * @param history           action history
     * @param domContext        current DOM representation
     * @param previousActionStr the previously executed action
     * @param knownBindings     previously extracted or known variables
     * @return the fully formatted prompt
     */
    public static String buildV2ExplorationPrompt(final String intent, final String sutContext, final String subgoal, final String history,
        final String domContext, final String previousActionStr, final Map<String, String> knownBindings)
    {
        String sutContextBlock = "";
        if (sutContext != null && !sutContext.trim().isEmpty())
        {
            sutContextBlock = "\n### SUT Specific Instructions (Application Context)\n" + sutContext + "\n";
        }
        
        String knownBindingsBlock = "";
        if (knownBindings != null && !knownBindings.isEmpty())
        {
            final StringBuilder sb = new StringBuilder();
            for (final Entry<String, String> entry : knownBindings.entrySet())
            {
                sb.append("  ${").append(entry.getKey()).append("} = '").append(entry.getValue()).append("'\n");
            }
            knownBindingsBlock = "\n## Known Data Bindings\n" + sb.toString();
        }
        
        return V2_EXPLORATION_PROMPT_TEMPLATE
            .replace("{intent}", intent)
            .replace("{sutContextBlock}", sutContextBlock)
            .replace("{subgoal}", subgoal != null && !subgoal.isEmpty() ? subgoal : "None (Starting First Phase)")
            .replace("{knownBindingsBlock}", knownBindingsBlock)
            .replace("{history}", history != null && !history.trim().isEmpty() ? history : "None (Initial Step)")
            .replace("{domContext}", domContext)
            .replace("{previousAction}", previousActionStr != null ? previousActionStr : "None (Initial Step)");
    }

    /**
     * Gets the system exploration prompt, optionally injecting the assertion instructions.
     *
     * @param includeValidations whether to include the validation instructions
     * @return the formatted system exploration prompt
     */
    public static String getSystemExplorationPrompt(final boolean includeValidations)
    {
        if (includeValidations)
        {
            final String assertionsBlock = """
          CRITICAL INSTRUCTION FOR Assertions:
          You MUST systematically inject ASSERT actions. Target elements that are functionally and visually interactable to the user (e.g., "Validate the Login button is visible") or structurally important text on the page.
          Whenever you land on a new page or new modal, your FIRST actions in your array MUST be multiple `ASSERT` actions to validate the new state.
          Make sure to check for IMPORTANT information that matches the page's purpose (e.g. check if the expected text matches the page context). We don't need to check that text is character-perfect, so DO NOT include a "value" field for `ASSERT` actions unless absolutely necessary. Simply providing the `target` and a `description` will check if the element is visible on the page, which is sufficient for structural validation.
          - NEVER use structural terms like "heading" or "page headline" in your assertion descriptions. Just refer to the text itself (e.g., use "Validate the text North Boston is visible" instead of "Validate the 'North Boston' heading is visible").
    """;
            return SYSTEM_EXPLORATION_PROMPT.replace("{assertionsInstruction}", assertionsBlock);
        }
        return SYSTEM_EXPLORATION_PROMPT.replace("{assertionsInstruction}", "");
    }

    /**
     * Gets the V2 system exploration prompt, optionally injecting the assertion instructions.
     *
     * @param includeValidations whether to include the validation instructions
     * @return the formatted V2 system exploration prompt
     */
    public static String getV2SystemExplorationPrompt(final boolean includeValidations)
    {
        if (includeValidations)
        {
            final String assertionsBlock = """
          CRITICAL INSTRUCTION FOR Assertions:
          You MUST systematically inject ASSERT actions. Target elements that are functionally and visually interactable to the user (e.g., "Validate the Login button is visible") or structurally important text on the page...
    """;
            return V2_SYSTEM_EXPLORATION_PROMPT.replace("{assertionsInstruction}", assertionsBlock);
        }
        return V2_SYSTEM_EXPLORATION_PROMPT.replace("{assertionsInstruction}", "");
    }

    /**
     * Builds the user prompt with the instruction and DOM context.
     * No step history is included (used for first-attempt happy path).
     *
     * @param instruction the task instruction
     * @param sutContext  application specific instructions
     * @param domContext  current DOM representation
     * @return the formatted user prompt
     */
    public static String buildUserPrompt(final String instruction, final String sutContext, final String domContext)
    {
        return buildUserPrompt(instruction, sutContext, domContext, "");
    }

    /**
     * Builds the user prompt with the instruction, DOM context, and optional step history.
     * Step history provides the LLM with context about previously completed steps,
     * which is useful during context escalation to help disambiguate elements.
     *
     * @param instruction  the task instruction
     * @param sutContext   application specific instructions
     * @param domContext   current DOM representation
     * @param historyBlock pre-formatted step history block, or empty string if none
     * @return the formatted user prompt
     */
    public static String buildUserPrompt(final String instruction, final String sutContext, final String domContext,
        final String historyBlock)
    {
        String sutContextBlock = "";
        if (sutContext != null && !sutContext.trim().isEmpty())
        {
            sutContextBlock = "\n### SUT Specific Instructions (Application Context)\n" + sutContext + "\n";
        }
        return USER_PROMPT_TEMPLATE
            .replace("{instruction}", instruction)
            .replace("{sutContextBlock}", sutContextBlock)
            .replace("{historyBlock}", historyBlock != null ? historyBlock : "")
            .replace("{domContext}", domContext);
    }

    /**
     * Builds a retry prompt with error context.
     * No step history is included.
     *
     * @param instruction the task instruction
     * @param sutContext  application specific instructions
     * @param domContext  current DOM representation
     * @param error       the error that caused the previous failure
     * @return the formatted retry prompt
     */
    public static String buildRetryPrompt(final String instruction, final String sutContext, final String domContext,
        final String error)
    {
        return buildRetryPrompt(instruction, sutContext, domContext, error, "");
    }

    /**
     * Builds a retry prompt with error context and optional step history.
     * Step history helps the LLM reason about expected page state when recovering from errors.
     *
     * @param instruction  the task instruction
     * @param sutContext   application specific instructions
     * @param domContext   current DOM representation
     * @param error        the error that caused the previous failure
     * @param historyBlock pre-formatted step history block, or empty string if none
     * @return the formatted retry prompt
     */
    public static String buildRetryPrompt(final String instruction, final String sutContext, final String domContext,
        final String error, final String historyBlock)
    {
        String sutContextBlock = "";
        if (sutContext != null && !sutContext.trim().isEmpty())
        {
            sutContextBlock = "\n### SUT Specific Instructions (Application Context)\n" + sutContext + "\n";
        }
        return RETRY_PROMPT_TEMPLATE
            .replace("{instruction}", instruction)
            .replace("{sutContextBlock}", sutContextBlock)
            .replace("{historyBlock}", historyBlock != null ? historyBlock : "")
            .replace("{domContext}", domContext)
            .replace("{error}", error);
    }

    /**
     * Builds a retry prompt for when no actions were returned.
     * No step history is included.
     *
     * @param instruction the task instruction
     * @param sutContext  application specific instructions
     * @param domContext  current DOM representation
     * @return the formatted prompt
     */
    public static String buildNoActionsRetryPrompt(final String instruction, final String sutContext, final String domContext)
    {
        return buildNoActionsRetryPrompt(instruction, sutContext, domContext, "");
    }

    /**
     * Builds a retry prompt for when no actions were returned, with optional step history.
     * Step history helps the LLM understand the broader test flow context when it
     * failed to produce actions on a previous attempt.
     *
     * @param instruction  the task instruction
     * @param sutContext   application specific instructions
     * @param domContext   current DOM representation
     * @param historyBlock pre-formatted step history block, or empty string if none
     * @return the formatted prompt
     */
    public static String buildNoActionsRetryPrompt(final String instruction, final String sutContext,
        final String domContext, final String historyBlock)
    {
        String sutContextBlock = "";
        if (sutContext != null && !sutContext.trim().isEmpty())
        {
            sutContextBlock = "\n### SUT Specific Instructions (Application Context)\n" + sutContext + "\n";
        }
        return NO_ACTIONS_RETRY_PROMPT_TEMPLATE
            .replace("{instruction}", instruction)
            .replace("{sutContextBlock}", sutContextBlock)
            .replace("{historyBlock}", historyBlock != null ? historyBlock : "")
            .replace("{domContext}", domContext);
    }

    /**
     * Builds a compact step history block from completed playbook steps.
     * Returns a formatted markdown section listing all steps that have been
     * successfully executed before the current one, giving the LLM context
     * about the test flow leading to this point.
     * <p>
     * Only includes steps with a non-null, non-blank {@code promptLine}.
     * Returns an empty string if no completed steps exist, ensuring the
     * {@code {historyBlock}} placeholder is cleanly removed from the template.
     *
     * @param playbook the current playbook (may be {@code null})
     * @return the formatted history block, or empty string if no history
     */
    public static String buildStepHistory(final Playbook playbook)
    {
        if (playbook == null)
        {
            return "";
        }

        final int cursor = playbook.getCursor();
        final List<PlaybookStep> steps = playbook.getSteps();

        if (cursor <= 0 || steps == null || steps.isEmpty())
        {
            return "";
        }

        final int limit = Math.min(cursor, steps.size());
        final StringBuilder sb = new StringBuilder();
        sb.append("\n### Completed Steps (for context)\n");

        int lineNum = 1;
        for (int i = 0; i < limit; i++)
        {
            final PlaybookStep step = steps.get(i);
            final String promptLine = step.getPromptLine();
            if (promptLine != null && !promptLine.isBlank())
            {
                sb.append(lineNum++).append(". ").append(promptLine).append("\n");
            }
        }

        // If all steps had null/blank promptLines, return empty
        if (lineNum == 1)
        {
            return "";
        }

        sb.append("[CURRENT] → see Instruction above\n");
        return sb.toString();
    }

    /**
     * Builds a prompt for healing an invalid or broken playbook step.
     *
     * @param instruction       the original task instruction
     * @param originalReasoning the AI's reasoning from the broken step
     * @param domContext        the current page DOM
     * @param error             the execution error that occurred
     * @param step              the broken playbook step
     * @return the formatted healing prompt
     */
    public static String buildHealingPrompt(final String instruction, final String originalReasoning,
        final String domContext, final String error, final PlaybookStep step)
    {
        String elemCtx = "None";
        if (step != null && step.getActions() != null && !step.getActions().isEmpty())
        {
            final Map<String, String> ctx = step.getActions().get(0).getElementContext();
            if (ctx != null)
            {
                elemCtx = ctx.toString();
            }
        }

        return HEALING_PROMPT_TEMPLATE
            .replace("{instruction}", instruction)
            .replace("{originalReasoning}", originalReasoning != null ? originalReasoning : "None")
            .replace("{elementContext}", elemCtx)
            .replace("{error}", error != null ? error : "Unknown error")
            .replace("{domContext}", domContext);
    }

    /**
     * Injects the dynamic plugin metadata (available actions and their descriptions) into a prompt template.
     *
     * @param promptTemplate the raw template containing {actionTypes} and {actionDescriptions} placeholders
     * @return the final prompt with plugins injected
     */
    public static String injectPluginMetadata(final String promptTemplate)
    {
        if (promptTemplate == null)
        {
            return null;
        }

        final Collection<AiActionPlugin> plugins = ActionRegistry.getAllPlugins();

        final List<String> typeNames = new ArrayList<>();
        final StringBuilder descriptions = new StringBuilder();

        for (final AiActionPlugin plugin : plugins)
        {
            typeNames.add(plugin.getActionName());
            final String desc = plugin.getPromptInstructions();
            if (desc != null && !desc.isBlank())
            {
                descriptions.append("- ").append(desc).append("\n");
            }
        }

        final String typesStr = String.join(" | ", typeNames);

        return promptTemplate.replace("{actionTypes}", typesStr)
            .replace("{actionDescriptions}", descriptions.toString());
    }

    /**
     * Retrieves the base system prompt with all plugins dynamically injected.
     * Uses {@link ContextLevel#STANDARD} for backward compatibility.
     *
     * @return the fully prepared system prompt
     */
    public static String getSystemPrompt()
    {
        return getSystemPrompt(ContextLevel.STANDARD);
    }

    /**
     * Retrieves the system prompt tailored to the given context level.
     * Injects plugin metadata and appends context-level-specific instructions
     * that tell the LLM what data it is receiving and when to request escalation.
     *
     * @param level the current context level
     * @return the fully prepared system prompt with context-level guidance
     */
    public static String getSystemPrompt(final ContextLevel level)
    {
        final String base = injectPluginMetadata(SYSTEM_PROMPT);
        final String contextGuidance = getContextLevelGuidance(level);
        return base + "\n" + contextGuidance;
    }

    /**
     * Returns context-level-specific instructions to append to the system prompt.
     * These instructions make the LLM a conscious participant in the escalation
     * protocol by telling it exactly what data it is receiving and when to
     * respond with {@code "status": "ESCALATE"} instead of guessing.
     *
     * @param level the current context level
     * @return the context guidance text to append
     */
    private static String getContextLevelGuidance(final ContextLevel level)
    {
        return switch (level)
        {
            case HINT -> """

                ## Context Level: HINT
                You are receiving MINIMAL context (no DOM elements). This is because the user provided an explicit inline locator hint (e.g., "(hint: #myId)") in the instruction.

                CRITICAL: Use the provided hint to generate the requested action JSON immediately. Do not attempt to verify the element's existence in the DOM (since no DOM is provided). If you cannot fulfill the instruction based on the hint alone, respond with:
                {"success": false, "status": "ESCALATE", "reasoning": "I need the actual DOM to determine the action", "actions": []}
                """;
            case LEAN -> """

                ## Context Level: LEAN
                You are receiving a LEAN context that only includes interactive elements \
                (buttons, links, inputs, selects, textareas), clickable elements, and headings. \
                Text content like paragraphs, spans, table cells, and list items is NOT included.

                CRITICAL: If you cannot find the requested element, or if you need text content \
                to disambiguate between multiple similar elements (e.g. multiple 'View Details' links), \
                or if the instruction requires reading text that is not shown, you MUST respond with:
                {"success": false, "status": "ESCALATE", "reasoning": "explain what additional data you need", "actions": []}

                Do NOT guess. Do NOT pick an arbitrary element when multiple matches exist. \
                Request escalation instead.
                """;

            case STANDARD -> """

                ## Context Level: STANDARD
                You are receiving a STANDARD context that includes all interactive elements \
                AND all visible text content (paragraphs, spans, list items, table cells, divs).

                If you still cannot find the target element or fulfill the instruction, \
                you may respond with:
                {"success": false, "status": "ESCALATE", "reasoning": "explain what you need", "actions": []}
                to request visual context (a screenshot will be provided on the next attempt).

                Do NOT guess if you are uncertain about which element to target.
                """;

            case VISUAL -> """

                ## Context Level: VISUAL
                You are receiving the STANDARD text context PLUS a screenshot of the current page. \
                Use the screenshot to visually identify the target element, then map it to the \
                closest element in the text context using its `data-neo-ref` identifier.

                This is the maximum available context. If you cannot fulfill the instruction, \
                respond with {"success": false, "error": "explain what failed", "actions": []}. \
                Do not request escalation — there is no higher level.
                """;
        };
    }
}
