/*
 * Apache License 2.0
 *
 * Copyright (c) 2026 Xceptance
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neodymium.ai.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.model.SemanticIntent;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolDefinition;

/**
 * AI prompt implementation for the pre-step PESAP preparation phase.
 * Analyzes the active step instruction to predict minimal context level,
 * check if custom Java methods are required, classify semantic intent, and identify step splits.
 * Supports native structured tool calling via the {@code classify_step} tool definition.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class PesapPrompt implements AiPrompt<PesapPrompt.PesapResult>
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final ToolDefinition CLASSIFY_STEP_TOOL = createClassifyStepTool();

    private final String currentInstruction;

    /**
     * Represents the parsed result of the PESAP analysis.
     *
     * @param contextLevel predicted SUT context level
     * @param requiresJavaMethods whether custom Java reflection methods are required
     * @param splitSteps the split step instructions, or empty if no split
     * @param intent the predicted semantic intent, or null if unclassified
     */
    public record PesapResult(String contextLevel, boolean requiresJavaMethods, List<String> splitSteps, SemanticIntent intent)
    {
        /**
         * Modern constructor without requiresJavaMethods.
         *
         * @param contextLevel predicted SUT context level
         * @param splitSteps the split step instructions, or empty if no split
         * @param intent the predicted semantic intent, or null if unclassified
         */
        public PesapResult(final String contextLevel, final List<String> splitSteps, final SemanticIntent intent)
        {
            this(contextLevel, false, splitSteps, intent);
        }

        /**
         * Backwards-compatible constructor without explicit semantic intent.
         *
         * @param contextLevel predicted SUT context level
         * @param requiresJavaMethods whether custom Java reflection methods are required
         * @param splitSteps the split step instructions, or empty if no split
         * @deprecated Use {@link #PesapResult(String, List, SemanticIntent)} instead.
         */
        @Deprecated
        public PesapResult(final String contextLevel, final boolean requiresJavaMethods, final List<String> splitSteps)
        {
            this(contextLevel, requiresJavaMethods, splitSteps, null);
        }
    }

    /**
     * Constructs a PesapPrompt for the active step instruction.
     *
     * @param currentInstruction the current step instruction
     */
    public PesapPrompt(final String currentInstruction)
    {
        this.currentInstruction = currentInstruction;
    }

    /**
     * Constructs a PesapPrompt with legacy context parameters for backwards compatibility.
     *
     * @param currentInstruction the current step instruction
     * @param previousInstruction the previous step instruction (unused)
     * @param nextInstructions the next step instructions (unused)
     */
    public PesapPrompt(final String currentInstruction, final String previousInstruction, final List<String> nextInstructions)
    {
        this(currentInstruction);
    }

    /**
     * Returns the structured tool definitions for pre-step PESAP classification.
     *
     * @return list containing the {@code classify_step} tool definition
     */
    public List<ToolDefinition> getTools()
    {
        return List.of(CLASSIFY_STEP_TOOL);
    }

    @Override
    public ResponseSchema getResponseSchema()
    {
        return ResponseSchema.STEP_SPLITS;
    }

    @Override
    public String compileSystemMessage(final ExecutionContext context)
    {
        return SystemPromptAddonHelper.appendAddon(AiAgentPrompts.getPesapPreStepPrompt(), "pesap", context);
    }

    @Override
    public String compileUserMessage(final ExecutionContext context)
    {
        return "## Active Instruction\n" + (this.currentInstruction != null ? truncateInstruction(this.currentInstruction) : "");
    }

    private static String truncateInstruction(final String instruction)
    {
        if (instruction == null)
        {
            return "";
        }
        final String trimmed = instruction.trim();
        if (trimmed.length() > 500)
        {
            return trimmed.substring(0, 500) + "... [TRUNCATED]";
        }
        return trimmed;
    }

    /**
     * Parses the LLM response, prioritizing native tool calls and falling back to raw content text parsing.
     *
     * @param response the LLM response
     * @param context the execution context
     * @return the parsed PESAP result
     * @throws Exception if parsing fails
     */
    public PesapResult parseResponse(final LlmResponse response, final ExecutionContext context) throws Exception
    {
        if (response != null && response.toolCalls() != null && !response.toolCalls().isEmpty())
        {
            for (final ToolCall call : response.toolCalls())
            {
                if ("classify_step".equalsIgnoreCase(call.toolName()) || call.arguments() != null)
                {
                    return parseArgumentsNode(call.arguments());
                }
            }
        }

        return parseResponse(response != null ? response.content() : null, context);
    }

    @Override
    public PesapResult parseResponse(final String rawContent, final ExecutionContext context) throws Exception
    {
        if (rawContent == null || rawContent.isBlank())
        {
            return new PesapResult("LEAN", false, List.of(), null);
        }

        final String jsonContent = LlmResponseSanitizer.extractJson(rawContent);
        if (jsonContent.isEmpty())
        {
            return new PesapResult("LEAN", false, List.of(), null);
        }

        final JsonNode root = MAPPER.readTree(jsonContent);
        return parseArgumentsNode(root);
    }

    private static PesapResult parseArgumentsNode(final JsonNode root)
    {
        if (root == null || root.isEmpty())
        {
            return new PesapResult("LEAN", false, List.of(), null);
        }

        final String rawContextLevel;
        if (root.hasNonNull("contextLevel"))
        {
            rawContextLevel = root.path("contextLevel").asText("LEAN").toUpperCase().trim();
        }
        else if (root.hasNonNull("c"))
        {
            rawContextLevel = root.path("c").asText("LEAN").toUpperCase().trim();
        }
        else
        {
            rawContextLevel = "LEAN";
        }

        final boolean requiresJavaMethods = root.hasNonNull("jm") && root.path("jm").asBoolean();

        final List<String> splitSteps = new ArrayList<>();
        JsonNode splitNode = root.path("milestones");
        if (!splitNode.isArray() || splitNode.isEmpty())
        {
            splitNode = root.path("sp");
        }
        if (splitNode.isArray())
        {
            for (final JsonNode node : splitNode)
            {
                splitSteps.add(node.asText());
            }
        }

        final String rawIntent;
        if (root.hasNonNull("intent"))
        {
            rawIntent = root.path("intent").asText(null);
        }
        else if (root.hasNonNull("i"))
        {
            rawIntent = root.path("i").asText(null);
        }
        else
        {
            rawIntent = null;
        }

        final SemanticIntent intent = SemanticIntent.fromCode(rawIntent);
        final ContextLevel cleanedLevel = ContextLevel.clean(rawContextLevel, intent, ContextLevel.LEAN);

        return new PesapResult(cleanedLevel.name(), requiresJavaMethods, splitSteps, intent);
    }

    private static ToolDefinition createClassifyStepTool()
    {
        final ObjectNode properties = MAPPER.createObjectNode();

        final ObjectNode intentProp = MAPPER.createObjectNode();
        intentProp.put("type", "string");
        intentProp.put("description", "The primary operational or verification objective of the instruction");
        final ArrayNode intentEnum = MAPPER.createArrayNode();
        for (final SemanticIntent si : SemanticIntent.values())
        {
            intentEnum.add(si.name());
        }
        intentProp.set("enum", intentEnum);
        properties.set("intent", intentProp);

        final ObjectNode milestonesProp = MAPPER.createObjectNode();
        milestonesProp.put("type", "array");
        milestonesProp.put("description", "Ordered list of standalone atomic sub-step instructions if this is a compound goal, or empty if atomic");
        final ObjectNode itemsProp = MAPPER.createObjectNode();
        itemsProp.put("type", "string");
        milestonesProp.set("items", itemsProp);
        properties.set("milestones", milestonesProp);

        final ObjectNode contextLevelProp = MAPPER.createObjectNode();
        contextLevelProp.put("type", "string");
        contextLevelProp.put("description", "Minimal required SUT context level for Turn 1 execution (e.g. MINIMAL, LEAN, STANDARD, RICH, VISUAL)");
        final ArrayNode contextEnum = MAPPER.createArrayNode();
        for (final ContextLevel cl : ContextLevel.values())
        {
            contextEnum.add(cl.name());
        }
        contextLevelProp.set("enum", contextEnum);
        properties.set("contextLevel", contextLevelProp);

        final ArrayNode required = MAPPER.createArrayNode();
        required.add("intent");

        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", properties);
        schema.set("required", required);

        return new ToolDefinition(
            "classify_step",
            "Classify the operational intent, decompose compound milestones, and determine the minimal required SUT context level for a test instruction.",
            schema
        );
    }
}
