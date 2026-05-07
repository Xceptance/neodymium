package com.xceptance.neodymium.ai.core;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Contains the system prompt templates for the AI agent.
 * Separated into its own class for easy tuning and experimentation.
 * Prompts are loaded from the classpath at 'ai-prompts/' to allow 
 * external projects to easily override them.
 */
public final class AiAgentPrompts {
  private AiAgentPrompts() {
    // utility class
  }

  private static String loadPrompt(String filename) {
    String resourcePath = "ai-prompts/" + filename;
    try (InputStream is = AiAgentPrompts.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (is != null) {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
      } else {
        throw new RuntimeException("Could not find prompt file on classpath: " + resourcePath);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to load prompt file: " + resourcePath, e);
    }
  }

  public static final String SYSTEM_EXPLORATION_PROMPT = loadPrompt("system-exploration-prompt.txt");
  public static final String EXPLORATION_PROMPT_TEMPLATE = loadPrompt("exploration-prompt-template.txt");
  public static final String V2_EXPLORATION_PROMPT_TEMPLATE = loadPrompt("v2-exploration-prompt-template.txt");
  public static final String V2_SYSTEM_EXPLORATION_PROMPT = loadPrompt("v2-system-exploration-prompt.txt");
  public static final String V2_EXTRACTION_PROMPT = loadPrompt("v2-extraction-prompt.txt");
  public static final String V2_EXTRACTION_RETRY_PROMPT = loadPrompt("v2-extraction-retry-prompt.txt");
  public static final String SYSTEM_PROMPT = loadPrompt("system-prompt.txt");
  public static final String USER_PROMPT_TEMPLATE = loadPrompt("user-prompt-template.txt");
  public static final String RETRY_PROMPT_TEMPLATE = loadPrompt("retry-prompt-template.txt");
  public static final String NO_ACTIONS_RETRY_PROMPT_TEMPLATE = loadPrompt("no-actions-retry-prompt-template.txt");
  public static final String SYSTEM_HEALING_PROMPT = loadPrompt("system-healing-prompt.txt");
  public static final String HEALING_PROMPT_TEMPLATE = loadPrompt("healing-prompt-template.txt");

  /**
   * Builds the exploration prompt.
   */
  public static String buildExplorationPrompt(final String intent, final String sutContext, final String subgoal, final String history,
      final String domContext, final String previousActionStr, final java.util.Map<String, String> knownBindings) {
    String sutContextBlock = "";
    if (sutContext != null && !sutContext.trim().isEmpty()) {
      sutContextBlock = "\n      ## SUT Specific Instructions (Application Context)\n      " + sutContext + "\n";
    }

    String knownBindingsBlock = "";
    if (knownBindings != null && !knownBindings.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (java.util.Map.Entry<String, String> entry : knownBindings.entrySet()) {
        sb.append("      ${").append(entry.getKey()).append("} = '").append(entry.getValue()).append("'\n");
      }
      knownBindingsBlock = "\n      ## Known Data Bindings\n" + sb.toString();
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

  public static String buildV2ExplorationPrompt(final String intent, final String sutContext, final String subgoal, final String history,
      final String domContext, final String previousActionStr, final java.util.Map<String, String> knownBindings) {
    String sutContextBlock = "";
    if (sutContext != null && !sutContext.trim().isEmpty()) {
      sutContextBlock = "\n      ## SUT Specific Instructions (Application Context)\n      " + sutContext + "\n";
    }

    String knownBindingsBlock = "";
    if (knownBindings != null && !knownBindings.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (java.util.Map.Entry<String, String> entry : knownBindings.entrySet()) {
        sb.append("      ${").append(entry.getKey()).append("} = '").append(entry.getValue()).append("'\n");
      }
      knownBindingsBlock = "\n      ## Known Data Bindings\n" + sb.toString();
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

  public static String getSystemExplorationPrompt(boolean includeValidations) {
    if (includeValidations) {
        String assertionsBlock = """
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

  public static String getV2SystemExplorationPrompt(boolean includeValidations) {
    if (includeValidations) {
        String assertionsBlock = """
      CRITICAL INSTRUCTION FOR Assertions:
      You MUST systematically inject ASSERT actions. Target elements that are functionally and visually interactable to the user (e.g., "Validate the Login button is visible") or structurally important text on the page...
""";
        return V2_SYSTEM_EXPLORATION_PROMPT.replace("{assertionsInstruction}", assertionsBlock);
    }
    return V2_SYSTEM_EXPLORATION_PROMPT.replace("{assertionsInstruction}", "");
  }

  /**
   * Builds the user prompt with the instruction and DOM context.
   */
  public static String buildUserPrompt(final String instruction, final String sutContext, final String domContext) {
    String sutContextBlock = "";
    if (sutContext != null && !sutContext.trim().isEmpty()) {
      sutContextBlock = "\n      ## SUT Specific Instructions (Application Context)\n      " + sutContext + "\n";
    }
    return USER_PROMPT_TEMPLATE
        .replace("{instruction}", instruction)
        .replace("{sutContextBlock}", sutContextBlock)
        .replace("{domContext}", domContext);
  }

  /**
   * Builds a retry prompt with error context.
   */
  public static String buildRetryPrompt(final String instruction, final String sutContext, final String domContext,
      final String error) {
    String sutContextBlock = "";
    if (sutContext != null && !sutContext.trim().isEmpty()) {
      sutContextBlock = "\n      ## SUT Specific Instructions (Application Context)\n      " + sutContext + "\n";
    }
    return RETRY_PROMPT_TEMPLATE
        .replace("{instruction}", instruction)
        .replace("{sutContextBlock}", sutContextBlock)
        .replace("{domContext}", domContext)
        .replace("{error}", error);
  }

  /**
   * Builds a retry prompt for when no actions were returned.
   */
  public static String buildNoActionsRetryPrompt(final String instruction, final String sutContext, final String domContext) {
    String sutContextBlock = "";
    if (sutContext != null && !sutContext.trim().isEmpty()) {
      sutContextBlock = "\n      ## SUT Specific Instructions (Application Context)\n      " + sutContext + "\n";
    }
    return NO_ACTIONS_RETRY_PROMPT_TEMPLATE
        .replace("{instruction}", instruction)
        .replace("{sutContextBlock}", sutContextBlock)
        .replace("{domContext}", domContext);
  }

  public static String buildHealingPrompt(final String instruction, final String originalReasoning,
      final String domContext, final String error, final com.xceptance.neodymium.ai.playbook.PlaybookStep step) {

    String elemCtx = "None";
    if (step != null && step.getActions() != null && !step.getActions().isEmpty()) {
      java.util.Map<String, String> ctx = step.getActions().get(0).getElementContext();
      if (ctx != null) {
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

  public static String injectPluginMetadata(String promptTemplate) {
      if (promptTemplate == null) {
          return null;
      }

      java.util.Collection<com.xceptance.neodymium.ai.action.AiActionPlugin> plugins = com.xceptance.neodymium.ai.action.ActionRegistry
              .getAllPlugins();

      java.util.List<String> typeNames = new java.util.ArrayList<>();
      StringBuilder descriptions = new StringBuilder();

      for (com.xceptance.neodymium.ai.action.AiActionPlugin plugin : plugins) {
          typeNames.add(plugin.getActionName());
          String desc = plugin.getPromptInstructions();
          if (desc != null && !desc.isBlank()) {
              descriptions.append("- ").append(desc).append("\n");
          }
      }

      String typesStr = String.join(" | ", typeNames);

      return promptTemplate.replace("{actionTypes}", typesStr)
              .replace("{actionDescriptions}", descriptions.toString());
  }

  public static String getSystemPrompt() {
      return injectPluginMetadata(SYSTEM_PROMPT);
  }
}
