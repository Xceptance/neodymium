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
 * @author AI-generated: Gemini 2.5 Flash
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
    public static final String SYSTEM_EXPLORATION_PROMPT = loadPrompt("system-exploration-prompt.md");

    /**
     * Template for standard exploration steps.
     */
    public static final String EXPLORATION_PROMPT_TEMPLATE = loadPrompt("exploration-prompt-template.md");

    /**
     * Template for v2 exploration steps.
     */
    public static final String V2_EXPLORATION_PROMPT_TEMPLATE = loadPrompt("v2-exploration-prompt-template.md");

    /**
     * System prompt for v2 exploration.
     */
    public static final String V2_SYSTEM_EXPLORATION_PROMPT = loadPrompt("v2-system-exploration-prompt.md");

    /**
     * System prompt for v2 extraction.
     */
    public static final String V2_EXTRACTION_PROMPT = loadPrompt("v2-extraction-prompt.md");

    /**
     * System prompt for v2 extraction retry.
     */
    public static final String V2_EXTRACTION_RETRY_PROMPT = loadPrompt("v2-extraction-retry-prompt.md");

    /**
     * Snippets for modular system prompt construction.
     */
    public static final String SNIPPET_ROLE = loadPrompt("snippet-role.md");
    public static final String SNIPPET_CAPABILITIES = loadPrompt("snippet-capabilities.md");
    public static final String SNIPPET_RESPONSE_FORMAT = loadPrompt("snippet-response-format.md");
    public static final String SNIPPET_RULES_SUCCESS_FAILURE = loadPrompt("snippet-rules-success-failure.md");
    public static final String SNIPPET_RULES_GENERAL = loadPrompt("snippet-rules-general.md");
    public static final String SNIPPET_RULES_DOM_ANALYSIS = loadPrompt("snippet-rules-dom-analysis.md");
    public static final String SNIPPET_RULES_ELEMENT_SELECTION = loadPrompt("snippet-rules-element-selection.md");
    public static final String SNIPPET_RULES_VISUAL = loadPrompt("snippet-rules-visual.md");
    public static final String SYSTEM_HEALING_INSTRUCTION = loadPrompt("system-healing-instruction.md");

    /**
     * Base system prompt.
     */
    public static final String SYSTEM_PROMPT = SNIPPET_ROLE + "\n\n"
            + SNIPPET_CAPABILITIES + "\n\n"
            + SNIPPET_RESPONSE_FORMAT + "\n\n"
            + SNIPPET_RULES_SUCCESS_FAILURE + "\n\n"
            + SNIPPET_RULES_GENERAL + "\n\n"
            + SNIPPET_RULES_DOM_ANALYSIS + "\n\n"
            + SNIPPET_RULES_ELEMENT_SELECTION + "\n\n"
            + SNIPPET_RULES_VISUAL;

    /**
     * Standard user prompt template.
     */
    public static final String USER_PROMPT_TEMPLATE = loadPrompt("user-prompt-template.md");

    /**
     * Prompt template for when an action fails and needs a retry.
     */
    public static final String RETRY_PROMPT_TEMPLATE = loadPrompt("retry-prompt-template.md");

    /**
     * Prompt template for when no actions were generated and a retry is needed.
     */
    public static final String NO_ACTIONS_RETRY_PROMPT_TEMPLATE = loadPrompt("no-actions-retry-prompt-template.md");

    /**
     * System prompt for playbook healing.
     */
    public static final String SYSTEM_HEALING_PROMPT = SNIPPET_ROLE + "\n\n"
            + SNIPPET_CAPABILITIES + "\n\n"
            + SNIPPET_RESPONSE_FORMAT + "\n\n"
            + SNIPPET_RULES_SUCCESS_FAILURE + "\n\n"
            + SNIPPET_RULES_GENERAL + "\n\n"
            + SNIPPET_RULES_DOM_ANALYSIS + "\n\n"
            + SNIPPET_RULES_ELEMENT_SELECTION + "\n\n"
            + SYSTEM_HEALING_INSTRUCTION;

    /**
     * System prompt for PESAP (Pre-Execution Static Analysis Phase) classification.
     */
    public static final String PESAP_SYSTEM_PROMPT = loadPrompt("pesap-system-prompt.md");

    /**
     * System prompt for PESAP (Pre-Execution Static Analysis Phase) classification phase only.
     */
    public static final String PESAP_CLASSIFY_PROMPT = loadPrompt("pesap-classify-prompt.md");

    /**
     * System prompt for PESAP (Pre-Execution Static Analysis Phase) semantic linter phase only.
     */
    public static final String PESAP_LINTER_PROMPT = loadPrompt("pesap-linter-prompt.md");

    /**
     * Gets the system linter prompt, dynamically appending custom rules if present.
     *
     * @param customRules the custom rules to append (can be null/empty)
     * @return the combined prompt
     */
    public static String getPesapLinterPrompt(final String customRules)
    {
        if (customRules == null || customRules.trim().isEmpty())
        {
            return PESAP_LINTER_PROMPT;
        }
        return PESAP_LINTER_PROMPT + "\n\n### Custom Semantic Linting Rules\n\nAdditional custom linting rules defined for this project/environment:\n\n" + customRules.trim();
    }

    /**
     * Prompt template for playbook healing.
     */
    public static final String HEALING_PROMPT_TEMPLATE = loadPrompt("healing-prompt-template.md");

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
            .replace("{sutContextBlock}", sutContextBlock == null || sutContextBlock.trim().isEmpty() ? "None" : sutContextBlock)
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
        return injectPluginMetadata(promptTemplate, ContextLevel.STANDARD);
    }

    /**
     * Injects the dynamic plugin metadata (available actions and their descriptions) into a prompt template,
     * tailoring the output granularity to the specified context level.
     *
     * @param promptTemplate the raw template containing {actionTypes} and {actionDescriptions} placeholders
     * @param level          the context level determining descriptions granularity
     * @return the final prompt with plugins injected
     */
    public static String injectPluginMetadata(final String promptTemplate, final ContextLevel level)
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
            if (level != ContextLevel.HINT)
            {
                final String desc = plugin.getPromptInstructions();
                if (desc != null && !desc.isBlank())
                {
                    descriptions.append("- ").append(desc).append("\n");
                }
            }
        }

        final String typesStr = String.join(" | ", typeNames);

        if (level == ContextLevel.HINT)
        {
            descriptions.append("- ").append(String.join(", ", typeNames)).append("\n");
        }

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
     *
     * @author AI-generated: Gemini 2.5 Pro
     */
    public static String getSystemPrompt(final ContextLevel level)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(SNIPPET_ROLE).append("\n\n");

        if (level == ContextLevel.HINT)
        {
            sb.append("## Your Capabilities\n")
              .append("You can perform these action types targeting the element: CLICK | TYPE | CLEAR | SELECT | HOVER | ASSERT | CHECK | SCROLL | KEY_PRESS\n\n");

            final String minResponseFormat = """
                ## Response Format
                Return raw minified single-line JSON without markdown code blocks:
                {
                  "s": true/false,
                  "a": [
                    {
                      "t": "ACTION_TYPE",
                      "tg": "locator string from hint",
                      "v": "value if required",
                      "d": "brief action description",
                      "ed": "short description of target element"
                    }
                  ],
                  "d": true/false,
                  "e": "error message if s is false",
                  "r": "brief explanation"
                }""";
            sb.append(minResponseFormat).append("\n\n");

            sb.append("## Rules\n")
              .append("1. Set \"d\" to true when all instructions for this step have been addressed.\n")
              .append("2. Keep descriptions (\"d\") concise but descriptive.\n")
              .append("3. When the instruction says \"click\" or \"press\" an element (except dropdowns/keys), use CLICK.\n")
              .append("4. When the instruction says \"type\" or \"enter\", use TYPE (which auto-clears first).\n")
              .append("5. When the instruction says \"clear\", use CLEAR.\n")
              .append("6. When selecting a dropdown option, use SELECT.\n")
              .append("7. When the instruction says \"hover\" or \"move mouse to\", use HOVER.\n")
              .append("8. When the instruction says \"verify\", \"check\" (meaning assert), or \"assert\", use ASSERT.\n")
              .append("9. When checking/unchecking a checkbox or radio button, use CHECK.\n")
              .append("10. When the instruction says \"scroll\", use SCROLL.\n")
              .append("11. To press keyboard keys (e.g., \"Enter\", \"Tab\"), use KEY_PRESS.\n\n");
        }
        else
        {
            sb.append(injectPluginMetadata(SNIPPET_CAPABILITIES, level)).append("\n\n");
            sb.append(SNIPPET_RESPONSE_FORMAT).append("\n\n");
            sb.append(SNIPPET_RULES_GENERAL).append("\n\n");
        }

        switch (level)
        {
            case HINT:
            {
                break;
            }

            case AXTREE:
            {
                sb.append(SNIPPET_RULES_SUCCESS_FAILURE).append("\n\n");
                sb.append(SNIPPET_RULES_DOM_ANALYSIS).append("\n\n");
                sb.append(SNIPPET_RULES_ELEMENT_SELECTION).append("\n\n");
                break;
            }

            case LEAN:
            case STANDARD:
            {
                sb.append(SNIPPET_RULES_SUCCESS_FAILURE).append("\n\n");
                sb.append(SNIPPET_RULES_DOM_ANALYSIS).append("\n\n");
                sb.append(SNIPPET_RULES_ELEMENT_SELECTION).append("\n\n");
                break;
            }

            case VISUAL_LEAN:
            case VISUAL:
            {
                sb.append(SNIPPET_RULES_SUCCESS_FAILURE).append("\n\n");
                sb.append(SNIPPET_RULES_DOM_ANALYSIS).append("\n\n");
                sb.append(SNIPPET_RULES_ELEMENT_SELECTION).append("\n\n");
                sb.append(SNIPPET_RULES_VISUAL).append("\n\n");
                break;
            }

            default:
            {
                break;
            }
        }

        sb.append(getContextLevelGuidance(level));
        return sb.toString();
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
                No DOM is provided. Translate the hint to the action JSON. If you cannot fulfill the instruction from the hint alone, respond with:
                {"s": false, "st": "ESCALATE", "tc": "AXTREE", "r": "I need the DOM", "a": []}
                """;
            case AXTREE -> """

                ## Context Level: AXTREE
                You are receiving a native Accessibility Tree (AXTree) context representing the semantic, interactive structure of the page.
                This contains links, buttons, inputs, headings, forms, and landmark sections with their resolved accessibility names and states.

                CRITICAL: If the instruction requires visual analysis (e.g., verifying colors, visual design, layout positioning, images, icons, or logos), or if you cannot find the requested element, or if you need full plain text content (like paragraph bodies, table cells, or static list item texts) to disambiguate between multiple elements, you MUST immediately respond with:
                {"s": false, "st": "ESCALATE", "tc": "STANDARD", "r": "This step requires standard text context which is not available in AXTREE context", "a": []}
                (Set tc to LEAN, STANDARD, VISUAL_LEAN, or VISUAL depending on what context you need).

                Do NOT guess. Do NOT pick an arbitrary element when multiple matches exist. Request escalation instead.
                """;
            case LEAN -> """

                ## Context Level: LEAN
                You are receiving a LEAN context that only includes interactive elements \
                (buttons, links, inputs, selects, textareas), clickable elements, and headings. \
                Text content like paragraphs, spans, table cells, and list items is NOT included.

                CRITICAL: If the instruction requires visual analysis (e.g., verifying colors, visual design, layout positioning, images, icons, or logos), or if you cannot find the requested element, or if you need text content \
                to disambiguate between multiple similar elements (e.g. multiple 'View Details' links), \
                or if the instruction requires reading text that is not shown, you MUST immediately respond with:
                {"s": false, "st": "ESCALATE", "tc": "STANDARD", "r": "This step requires visual validation or additional text context which is not available in LEAN context", "a": []}
                (Set tc to STANDARD, VISUAL_LEAN, or VISUAL depending on what context you need).

                Do NOT guess. Do NOT pick an arbitrary element when multiple matches exist. \
                Request escalation instead.
                """;

            case STANDARD -> """

                ## Context Level: STANDARD
                You are receiving a STANDARD context that includes all interactive elements \
                AND all visible text content (paragraphs, spans, list items, table cells, divs).

                CRITICAL: If the instruction requires visual analysis (e.g., verifying colors, visual design, layout positioning, images, icons, or logos), or if you still cannot find the target element or fulfill the instruction, \
                you MUST immediately respond with:
                {"s": false, "st": "ESCALATE", "tc": "VISUAL", "r": "This step requires visual validation which is not available in STANDARD context", "a": []}
                to request visual context (a screenshot will be provided on the next attempt).

                Do NOT guess if you are uncertain about which element to target.
                """;

            case VISUAL_LEAN -> """

                ## Context Level: VISUAL_LEAN
                You are receiving a LEAN text context (interactive elements and headings only, NO paragraphs or list items) PLUS a screenshot of the current page. \
                Use the screenshot to visually identify target elements, then map them to the \
                closest element in the text context using their `data-neo-ref` identifier.

                CRITICAL - VISUAL-ONLY VALIDATION:
                If the instruction is a visual-only check (e.g. verifying colors, layout, styling, font, visual design, logo details, alignment, or image content), you MUST act as the validator yourself by inspecting the screenshot.
                Since no browser/driver interaction is required to verify visual attributes, you do NOT need to return any browser actions. Instead, return:
                - {"s": true, "d": true, "a": [], "r": "Detailed visual analysis explaining how the screenshot matches the instruction (e.g. explaining the colors, layouts, or visual elements you see)"} if the visual condition is met.
                - {"s": false, "d": true, "a": [], "e": "Visual verification failed: <explanation of what did not match>", "r": "Detailed analysis of why the screenshot does not match the instruction"} if the visual condition is not met.

                This is the maximum available context for this initial tagged step. If you cannot fulfill the instruction (for example, if you need full text context of paragraphs or list items to complete the validation), \
                respond with {"s": false, "st": "ESCALATE", "tc": "VISUAL", "r": "This step requires full text context which is not available in VISUAL_LEAN", "a": []} to escalate to VISUAL.
                """;

            case VISUAL -> """

                ## Context Level: VISUAL
                You are receiving the FULL standard text context (interactive elements, headings, paragraphs, list items, tables, spans, divs, etc.) PLUS a page screenshot. \
                Use the screenshot to visually identify target elements, and leverage the full text content to understand their meaning and verify copy. Map them using their `data-neo-ref` identifier.

                CRITICAL - VISUAL-ONLY VALIDATION:
                If the instruction is a visual-only check (e.g. verifying colors, layout, styling, font, visual design, logo details, alignment, or image content), you MUST act as the validator yourself by inspecting the screenshot.
                Since no browser/driver interaction is required to verify visual attributes, you do NOT need to return any browser actions. Instead, return:
                - {"s": true, "d": true, "a": [], "r": "Detailed visual analysis explaining how the screenshot matches the instruction (e.g. explaining the colors, layouts, or visual elements you see)"} if the visual condition is met.
                - {"s": false, "d": true, "a": [], "e": "Visual verification failed: <explanation of what did not match>", "r": "Detailed analysis of why the screenshot does not match the instruction"} if the visual condition is not met.

                This is the maximum available context. If you cannot fulfill the instruction, \
                respond with {"s": false, "e": "explain what failed", "a": []}. \
                Do not request escalation — there is no higher level.
                """;
        };
    }
}
