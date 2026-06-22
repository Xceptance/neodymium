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
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.xceptance.neodymium.ai.action.ActionRegistry;
import com.xceptance.neodymium.ai.action.AiActionPlugin;
import com.xceptance.neodymium.ai.action.plugins.JavaMethodAction;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookStep;

/**
 * Contains the system prompt templates for the AI agent.
 * Separated into its own class for easy tuning and experimentation.
 * Prompts are loaded dynamically from the classpath at 'ai-prompts/' and cached
 * in a ConcurrentHashMap to allow runtime reloading.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class AiAgentPrompts
{
    private static final ConcurrentHashMap<String, String> PROMPT_CACHE = new ConcurrentHashMap<>();

    private AiAgentPrompts()
    {
        // utility class
    }

    /**
     * Clears the cached prompt contents, forcing them to be reloaded on the next access.
     */
    public static void clearCache()
    {
        PROMPT_CACHE.clear();
    }

    private static String getPrompt(final String filename)
    {
        return PROMPT_CACHE.computeIfAbsent(filename, AiAgentPrompts::loadPrompt);
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
     * 
     * @param includeValidations whether to include assertion rules
     * @return the formatted prompt
     */
    public static String getSystemExplorationPrompt(final boolean includeValidations)
    {
        final String systemExplorationPrompt = getPrompt("system-exploration-prompt.md");
        if (includeValidations)
        {
            final String assertionsBlock = getPrompt("assertions-instruction-v1.md");
            return systemExplorationPrompt.replace("{assertionsInstruction}", assertionsBlock);
        }
        return systemExplorationPrompt.replace("{assertionsInstruction}", "");
    }

    /**
     * Template for standard exploration steps.
     * 
     * @return the prompt template
     */
    public static String getExplorationPromptTemplate()
    {
        return getPrompt("exploration-prompt-template.md");
    }

    /**
     * Template for v2 exploration steps.
     * 
     * @return the prompt template
     */
    public static String getV2ExplorationPromptTemplate()
    {
        return getPrompt("v2-exploration-prompt-template.md");
    }

    /**
     * System prompt for v2 exploration.
     * 
     * @param includeValidations whether to include assertion rules
     * @return the formatted prompt
     */
    public static String getV2SystemExplorationPrompt(final boolean includeValidations)
    {
        final String v2SystemExplorationPrompt = getPrompt("v2-system-exploration-prompt.md");
        if (includeValidations)
        {
            final String assertionsBlock = getPrompt("assertions-instruction-v2.md");
            return v2SystemExplorationPrompt.replace("{assertionsInstruction}", assertionsBlock);
        }
        return v2SystemExplorationPrompt.replace("{assertionsInstruction}", "");
    }

    /**
     * System prompt for v2 extraction.
     * 
     * @return the prompt
     */
    public static String getV2ExtractionPrompt()
    {
        return getPrompt("v2-extraction-prompt.md");
    }

    /**
     * System prompt for v2 extraction retry.
     * 
     * @return the prompt
     */
    public static String getV2ExtractionRetryPrompt()
    {
        return getPrompt("v2-extraction-retry-prompt.md");
    }

    /**
     * Standard user prompt template.
     * 
     * @return the prompt template
     */
    public static String getUserPromptTemplate()
    {
        return getPrompt("user-prompt-template.md");
    }

    /**
     * Prompt template for when an action fails and needs a retry.
     * 
     * @return the prompt template
     */
    public static String getRetryPromptTemplate()
    {
        return getPrompt("retry-prompt-template.md");
    }

    /**
     * Prompt template for when no actions were generated and a retry is needed.
     * 
     * @return the prompt template
     */
    public static String getNoActionsRetryPromptTemplate()
    {
        return getPrompt("no-actions-retry-prompt-template.md");
    }

    /**
     * Prompt template for playbook healing.
     * 
     * @return the prompt template
     */
    public static String getHealingPromptTemplate()
    {
        return getPrompt("healing-prompt-template.md");
    }

    /**
     * Loads the JIT pre-step PESAP prompt template.
     * The returned prompt is ready to be sent as a system prompt with the flow context
     * injected as the user prompt.
     *
     * @return the fully prepared pre-step PESAP system prompt
     */
    public static String getPesapPreStepPrompt()
    {
        return getPrompt("pesap-pre-step-prompt.md");
    }

    /**
     * System prompt for PESAP semantic linter phase only.
     * 
     * @return the prompt
     */
    public static String getPesapLinterPrompt()
    {
        return getPrompt("pesap-linter-prompt.md");
    }

    /**
     * Gets the system linter prompt, dynamically appending custom rules if present.
     *
     * @param customRules the custom rules to append (can be null/empty)
     * @return the combined prompt
     */
    public static String getPesapLinterPrompt(final String customRules)
    {
        final String linterPrompt = getPesapLinterPrompt();
        if (customRules == null || customRules.trim().isEmpty())
        {
            return linterPrompt;
        }
        return linterPrompt + "\n\n### Custom Semantic Linting Rules\n\nAdditional custom linting rules defined for this project/environment:\n\n" + customRules.trim();
    }

    /**
     * Base system prompt.
     * 
     * @return the base prompt
     */
    public static String getSystemPromptBase()
    {
        final String snippetRole = getPrompt("snippet-role.md");
        final String snippetCapabilities = getPrompt("snippet-capabilities.md");
        final String snippetResponseFormat = getPrompt("snippet-response-format.md");
        final String systemPromptRules = getPrompt("system-prompt-rules.md");
        final String rulesVisual = getPrompt("rules-visual.md");

        return snippetRole + "\n\n"
                + snippetCapabilities + "\n\n"
                + snippetResponseFormat + "\n\n"
                + systemPromptRules + "\n\n"
                + rulesVisual;
    }

    /**
     * System prompt for playbook healing.
     * 
     * @return the healing system prompt
     */
    public static String getSystemHealingPrompt()
    {
        final String snippetRole = getPrompt("snippet-role.md");
        final String snippetCapabilities = getPrompt("snippet-capabilities.md");
        final String snippetResponseFormat = getPrompt("snippet-response-format.md");
        final String systemPromptRules = getPrompt("system-prompt-rules.md");
        final String systemHealingInstruction = getPrompt("system-healing-instruction.md");

        return snippetRole + "\n\n"
                + snippetCapabilities + "\n\n"
                + snippetResponseFormat + "\n\n"
                + systemPromptRules + "\n\n"
                + systemHealingInstruction;
    }

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
        
        return getExplorationPromptTemplate()
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
        
        return getV2ExplorationPromptTemplate()
            .replace("{intent}", intent)
            .replace("{sutContextBlock}", sutContextBlock)
            .replace("{subgoal}", subgoal != null && !subgoal.isEmpty() ? subgoal : "None (Starting First Phase)")
            .replace("{knownBindingsBlock}", knownBindingsBlock)
            .replace("{history}", history != null && !history.trim().isEmpty() ? history : "None (Initial Step)")
            .replace("{domContext}", domContext)
            .replace("{previousAction}", previousActionStr != null ? previousActionStr : "None (Initial Step)");
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
        final String snippetRole = getPrompt("snippet-role.md");
        final StringBuilder sb = new StringBuilder();
        sb.append(snippetRole).append("\n\n");

        if (level == ContextLevel.HINT)
        {
            final Collection<AiActionPlugin> plugins = ActionRegistry.getAllPlugins();
            final StringBuilder descriptions = new StringBuilder();
            for (final AiActionPlugin plugin : plugins)
            {
                final String desc = plugin.getPromptInstructions();
                if (desc != null && !desc.isBlank())
                {
                    descriptions.append("- ").append(desc).append("\n");
                }
            }

            sb.append("## Your Capabilities\n")
              .append("You can perform these action types targeting the element:\n")
              .append(descriptions).append("\n");

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
                      "desc": "brief action description",
                      "ed": "short description of target element",
                      "c": [ "... nested actions for BRANCH type (optional) ..." ],
                      "th": [ "... nested actions for BRANCH type (optional) ..." ],
                      "el": [ "... nested actions for BRANCH type (optional) ..." ]
                    }
                  ],
                  "d": true/false,
                  "e": "error message if s is false",
                  "r": "brief explanation"
                }""";
            sb.append(minResponseFormat).append("\n\n");

            sb.append("## Rules\n")
              .append("1. Set \"d\" to true when all instructions for this step are complete.\n")
              .append("2. Keep descriptions (\"desc\") concise.\n")
              .append("3. LOCATOR HINTS: If an inline hint is provided (e.g., \"(hint: selector)\"), you MUST extract the selector value and set it as the \"tg\" field of the action (including for KEY_PRESS).\n")
              .append("4. For single character keyboard inputs (e.g. 'a', 'b', 'c'), use KEY_PRESS instead of CLICK or TYPE when instructing to press/type a single letter key.\n\n");
        }
        else
        {
            final String snippetCapabilities = getPrompt("snippet-capabilities.md");
            sb.append(injectPluginMetadata(snippetCapabilities, level)).append("\n\n");

            final String snippetResponseFormat = getPrompt("snippet-response-format.md");
            sb.append(snippetResponseFormat).append("\n\n");

            final String systemPromptRules = getPrompt("system-prompt-rules.md");
            sb.append(systemPromptRules).append("\n\n");
        }

        switch (level)
        {
            case HINT:
            {
                break;
            }

            case AXTREE:
            case LEAN:
            case STANDARD:
            {
                break;
            }

            case VISUAL_LEAN:
            case VISUAL:
            {
                final String rulesVisual = getPrompt("rules-visual.md");
                sb.append(rulesVisual).append("\n\n");
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
     *
     * @param level the current context level
     * @return the context guidance text to append
     */
    private static String getContextLevelGuidance(final ContextLevel level)
    {
        return switch (level)
        {
            case HINT -> getPrompt("guidance-hint.md");
            case AXTREE -> getPrompt("guidance-axtree.md");
            case LEAN -> getPrompt("guidance-lean.md");
            case STANDARD -> getPrompt("guidance-standard.md");
            case VISUAL_LEAN -> getPrompt("guidance-visual-lean.md");
            case VISUAL -> getPrompt("guidance-visual.md");
        };
    }

    /**
     * Builds the user prompt with the instruction and DOM context.
     * No step history is included.
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
        return buildUserPrompt(instruction, sutContext, domContext, historyBlock, "");
    }

    /**
     * Builds the user prompt with the instruction, DOM context, optional step history, and optional Java methods block.
     *
     * @param instruction      the task instruction
     * @param sutContext       application specific instructions
     * @param domContext       current DOM representation
     * @param historyBlock     pre-formatted step history block, or empty string if none
     * @param javaMethodsBlock pre-formatted custom Java methods block, or empty string if none
     * @return the formatted user prompt
     */
    public static String buildUserPrompt(final String instruction, final String sutContext, final String domContext,
        final String historyBlock, final String javaMethodsBlock)
    {
        String sutContextBlock = "";
        if (sutContext != null && !sutContext.trim().isEmpty())
        {
            sutContextBlock = "\n### SUT Specific Instructions (Application Context)\n" + sutContext + "\n";
        }
        return getUserPromptTemplate()
            .replace("{instruction}", instruction)
            .replace("{sutContextBlock}", sutContextBlock)
            .replace("{javaMethodsBlock}", javaMethodsBlock != null ? javaMethodsBlock : "")
            .replace("{historyBlock}", historyBlock != null ? historyBlock : "")
            .replace("{domContext}", domContext);
    }

    /**
     * Builds a retry prompt with error context.
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
        return buildRetryPrompt(instruction, sutContext, domContext, error, historyBlock, "");
    }

    /**
     * Builds a retry prompt with error context, optional step history, and optional Java methods block.
     *
     * @param instruction      the task instruction
     * @param sutContext       application specific instructions
     * @param domContext       current DOM representation
     * @param error            the error that caused the previous failure
     * @param historyBlock     pre-formatted step history block, or empty string if none
     * @param javaMethodsBlock pre-formatted custom Java methods block, or empty string if none
     * @return the formatted retry prompt
     */
    public static String buildRetryPrompt(final String instruction, final String sutContext, final String domContext,
        final String error, final String historyBlock, final String javaMethodsBlock)
    {
        String sutContextBlock = "";
        if (sutContext != null && !sutContext.trim().isEmpty())
        {
            sutContextBlock = "\n### SUT Specific Instructions (Application Context)\n" + sutContext + "\n";
        }
        return getRetryPromptTemplate()
            .replace("{instruction}", instruction)
            .replace("{sutContextBlock}", sutContextBlock)
            .replace("{javaMethodsBlock}", javaMethodsBlock != null ? javaMethodsBlock : "")
            .replace("{historyBlock}", historyBlock != null ? historyBlock : "")
            .replace("{domContext}", domContext)
            .replace("{error}", error);
    }

    /**
     * Builds a retry prompt for when no actions were returned.
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
        return buildNoActionsRetryPrompt(instruction, sutContext, domContext, historyBlock, "");
    }

    /**
     * Builds a retry prompt for when no actions were returned, with optional step history and optional Java methods block.
     *
     * @param instruction      the task instruction
     * @param sutContext       application specific instructions
     * @param domContext       current DOM representation
     * @param historyBlock     pre-formatted step history block, or empty string if none
     * @param javaMethodsBlock pre-formatted custom Java methods block, or empty string if none
     * @return the formatted prompt
     */
    public static String buildNoActionsRetryPrompt(final String instruction, final String sutContext,
        final String domContext, final String historyBlock, final String javaMethodsBlock)
    {
        String sutContextBlock = "";
        if (sutContext != null && !sutContext.trim().isEmpty())
        {
            sutContextBlock = "\n### SUT Specific Instructions (Application Context)\n" + sutContext + "\n";
        }
        return getNoActionsRetryPromptTemplate()
            .replace("{instruction}", instruction)
            .replace("{sutContextBlock}", sutContextBlock)
            .replace("{javaMethodsBlock}", javaMethodsBlock != null ? javaMethodsBlock : "")
            .replace("{historyBlock}", historyBlock != null ? historyBlock : "")
            .replace("{domContext}", domContext);
    }

    /**
     * Builds a compact step history block from completed playbook steps.
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

        return getHealingPromptTemplate()
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
        return injectPluginMetadata(promptTemplate, level, true, null, null);
    }

    /**
     * Injects the dynamic plugin metadata with targeted Java method support.
     * When {@code includeJavaMethod} is {@code true} and {@code targetedMethods} is non-empty,
     * only the predicted methods are included in the JAVA_METHOD plugin description.
     *
     * @param promptTemplate  the raw template containing {actionTypes} and {actionDescriptions} placeholders
     * @param level           the context level determining descriptions granularity
     * @param includeJavaMethod whether to include JAVA_METHOD plugin instructions at all
     * @param testClass       the active test class for method scanning, or {@code null}
     * @param targetedMethods the set of method names to include, or {@code null}/empty for all
     * @return the final prompt with plugins injected
     */
    public static String injectPluginMetadata(final String promptTemplate, final ContextLevel level,
            final boolean includeJavaMethod, final Class<?> testClass, final Set<String> targetedMethods)
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
                // For JAVA_METHOD plugin, use targeted instructions when available
                if (plugin instanceof JavaMethodAction && includeJavaMethod)
                {
                    final JavaMethodAction jma = (JavaMethodAction) plugin;
                    final String desc = jma.getPromptInstructions(testClass, targetedMethods);
                    if (desc != null && !desc.isBlank())
                    {
                        descriptions.append("- ").append(desc).append("\n");
                    }
                }
                else if (plugin instanceof JavaMethodAction && !includeJavaMethod)
                {
                    // Skip JAVA_METHOD entirely when not needed for this step
                }
                else
                {
                    final String desc = plugin.getPromptInstructions();
                    if (desc != null && !desc.isBlank())
                    {
                        descriptions.append("- ").append(desc).append("\n");
                    }
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
}
