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
import java.util.ArrayList;
import java.util.List;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.model.SemanticIntent;
import org.neodymium.ai.pipeline.ExecutionContext;

/**
 * AI prompt implementation for the pre-step PESAP preparation phase.
 * Analyzes the active step instruction to predict minimal context level,
 * check if custom Java methods are required, classify semantic intent, and identify step splits.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class PesapPrompt implements AiPrompt<PesapPrompt.PesapResult>
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
         * Backwards-compatible constructor without explicit semantic intent.
         *
         * @param contextLevel predicted SUT context level
         * @param requiresJavaMethods whether custom Java reflection methods are required
         * @param splitSteps the split step instructions, or empty if no split
         */
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

    @Override
    public PesapResult parseResponse(final String rawContent, final ExecutionContext context) throws Exception
    {
        final String jsonContent = LlmResponseSanitizer.extractJson(rawContent);
        if (jsonContent.isEmpty())
        {
            return new PesapResult("LEAN", false, List.of(), null);
        }

        final JsonNode root = MAPPER.readTree(jsonContent);
        
        final String contextLevel = root.hasNonNull("c") ? root.path("c").asText("LEAN").toUpperCase().trim() : "LEAN";
        final boolean requiresJavaMethods = root.hasNonNull("jm") && root.path("jm").asBoolean();
        
        final List<String> splitSteps = new ArrayList<>();
        final JsonNode splitNode = root.path("sp");
        if (splitNode.isArray())
        {
            for (final JsonNode node : splitNode)
            {
                splitSteps.add(node.asText());
            }
        }

        final String rawIntent = root.hasNonNull("i") ? root.path("i").asText(null) : null;
        final SemanticIntent intent = SemanticIntent.fromCode(rawIntent);

        return new PesapResult(contextLevel, requiresJavaMethods, splitSteps, intent);
    }
}
