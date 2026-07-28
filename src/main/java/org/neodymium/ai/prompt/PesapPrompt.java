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
import org.neodymium.ai.pipeline.ExecutionContext;

/**
 * AI prompt implementation for the pre-step PESAP preparation phase.
 * Analyzes the flow context upfront to predict minimal context level,
 * check if custom Java methods are required, and identify step splits.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class PesapPrompt implements AiPrompt<PesapPrompt.PesapResult>
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String currentInstruction;
    private final String previousInstruction;
    private final List<String> nextInstructions;

    /**
     * Represents the parsed result of the PESAP analysis.
     *
     * @param contextLevel predicted SUT context level
     * @param requiresJavaMethods whether custom Java reflection methods are required
     * @param splitSteps the split step instructions, or empty if no split
     */
    public record PesapResult(String contextLevel, boolean requiresJavaMethods, List<String> splitSteps)
    {
    }

    /**
     * Constructs a PesapPrompt with flow context details.
     *
     * @param currentInstruction the current step instruction
     * @param previousInstruction the previous step instruction (can be null)
     * @param nextInstructions the next step instructions in the scenario flow (can be null/empty)
     */
    public PesapPrompt(final String currentInstruction, final String previousInstruction, final List<String> nextInstructions)
    {
        this.currentInstruction = currentInstruction;
        this.previousInstruction = previousInstruction;
        this.nextInstructions = nextInstructions != null ? nextInstructions : List.of();
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
        final StringBuilder sb = new StringBuilder();
        if (this.previousInstruction != null && !this.previousInstruction.isBlank())
        {
            sb.append("[PREVIOUS] Step: ").append(this.previousInstruction).append("\n");
        }
        sb.append("[CURRENT]  Step: ").append(this.currentInstruction).append("\n");
        int index = 1;
        for (final String next : this.nextInstructions)
        {
            if (next != null && !next.isBlank())
            {
                sb.append("[NEXT]     Step: ").append(next).append("\n");
                if (++index > 2)
                {
                    break; // Max 2 next steps as context
                }
            }
        }
        return "## Flow Context\n" + sb.toString();
    }

    @Override
    public PesapResult parseResponse(final String rawContent, final ExecutionContext context) throws Exception
    {
        final String jsonContent = LlmResponseSanitizer.extractJson(rawContent);
        if (jsonContent.isEmpty())
        {
            return new PesapResult("AXTREE", false, List.of());
        }

        final JsonNode root = MAPPER.readTree(jsonContent);
        
        final String contextLevel = root.hasNonNull("c") ? root.path("c").asText("AXTREE").toUpperCase().trim() : "AXTREE";
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

        return new PesapResult(contextLevel, requiresJavaMethods, splitSteps);
    }
}
