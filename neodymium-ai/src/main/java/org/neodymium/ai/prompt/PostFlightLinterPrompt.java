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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.playbook.linter.LinterCategory;
import org.neodymium.ai.playbook.linter.LinterSeverity;
import org.neodymium.ai.playbook.linter.PlaybookLinterFinding;

/**
 * AI prompt implementation for the empirical post-flight playbook linter.
 * Formats scenario steps that experienced runtime friction together with concrete
 * execution telemetry (turns, executed actions, DOM elements) to synthesize high-precision,
 * language-preserving rewrites.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class PostFlightLinterPrompt implements AiPrompt<List<PlaybookLinterFinding>>
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String description;
    private final List<StepTelemetryInfo> frictionSteps;

    /**
     * DTO capturing execution telemetry and friction signals for prompt compilation.
     *
     * @param stepIndex 1-based step index
     * @param lineNumber source line number
     * @param sourceFile source file path
     * @param rawInstruction original raw instruction
     * @param resolvedInstruction data-substituted instruction
     * @param status outcome status (PASSED/FAILED)
     * @param agentTurns number of agent turns
     * @param durationMs execution duration in milliseconds
     * @param executedActions summary of executed actions
     * @param observedDomText observed DOM text or accessible name
     * @param empiricalCategory primary detected empirical category
     * @param frictionReason explanation of observed friction
     */
    public record StepTelemetryInfo(
        int stepIndex,
        int lineNumber,
        String sourceFile,
        String rawInstruction,
        String resolvedInstruction,
        String status,
        int agentTurns,
        long durationMs,
        List<String> executedActions,
        String observedDomText,
        LinterCategory empiricalCategory,
        String frictionReason
    )
    {
    }

    /**
     * Constructs a PostFlightLinterPrompt with friction steps to analyze.
     *
     * @param description optional high-level scenario description
     * @param frictionSteps list of steps that exhibited execution friction
     */
    public PostFlightLinterPrompt(final String description, final List<StepTelemetryInfo> frictionSteps)
    {
        this.description = description != null && !description.isBlank() ? description.trim() : null;
        this.frictionSteps = frictionSteps != null ? List.copyOf(frictionSteps) : Collections.emptyList();
    }

    @Override
    public ResponseSchema getResponseSchema()
    {
        return ResponseSchema.LINTER;
    }

    @Override
    public String compileSystemMessage(final ExecutionContext context)
    {
        return SystemPromptAddonHelper.appendAddon(AiAgentPrompts.getPostFlightLinterPrompt(), "linter", context);
    }

    @Override
    public String compileUserMessage(final ExecutionContext context)
    {
        final StringBuilder sb = new StringBuilder();

        if (this.description != null)
        {
            sb.append("## Scenario Context\n")
              .append(this.description)
              .append("\n\n");
        }

        sb.append("## Steps Exhibiting Runtime Friction\n\n");
        sb.append("The following scenario steps completed with runtime friction, multi-action fallout, or locator divergences.\n");
        sb.append("Analyze each step's telemetry and provide grounded advisory findings and language-preserving rewrites.\n\n");

        for (final StepTelemetryInfo info : this.frictionSteps)
        {
            sb.append("### Step #").append(info.stepIndex());
            if (info.lineNumber() > 0)
            {
                sb.append(" (line ").append(info.lineNumber()).append(")");
            }
            sb.append("\n");

            sb.append("- **Raw Template Step:** `").append(info.rawInstruction()).append("`\n");
            if (info.resolvedInstruction() != null && !info.resolvedInstruction().equals(info.rawInstruction()))
            {
                sb.append("- **Resolved Step:** `").append(info.resolvedInstruction()).append("`\n");
            }
            sb.append("- **Status:** ").append(info.status()).append("\n");
            sb.append("- **Agent Turns:** ").append(info.agentTurns()).append("\n");
            sb.append("- **Duration:** ").append(info.durationMs()).append(" ms\n");
            sb.append("- **Detected Friction:** `").append(info.empiricalCategory()).append("` — ").append(info.frictionReason()).append("\n");

            if (info.observedDomText() != null && !info.observedDomText().isBlank())
            {
                sb.append("- **Interacted DOM Element Text:** \"").append(info.observedDomText()).append("\"\n");
            }

            if (info.executedActions() != null && !info.executedActions().isEmpty())
            {
                sb.append("- **Executed Actions:**\n");
                for (final String act : info.executedActions())
                {
                    sb.append("    * ").append(act).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("Generate a valid JSON object with the `findings` array evaluating each step above.");
        return sb.toString();
    }

    @Override
    public List<PlaybookLinterFinding> parseResponse(final String response, final ExecutionContext context)
    {
        if (response == null || response.isBlank())
        {
            return Collections.emptyList();
        }

        final List<PlaybookLinterFinding> findings = new ArrayList<>();

        try
        {
            String json = response.trim();
            if (json.startsWith("```json"))
            {
                json = json.substring(7);
            }
            else if (json.startsWith("```"))
            {
                json = json.substring(3);
            }
            if (json.endsWith("```"))
            {
                json = json.substring(0, json.length() - 3);
            }
            json = json.trim();

            final JsonNode root = MAPPER.readTree(json);
            final JsonNode findingsNode = root.path("findings");

            if (findingsNode.isArray())
            {
                for (final JsonNode node : findingsNode)
                {
                    final int stepIndex = node.path("stepIndex").asInt(1);
                    final String categoryStr = node.path("category").asText(null);
                    final String severityStr = node.path("severity").asText("WARNING");
                    final String message = node.path("message").asText("");
                    final String suggestedRewrite = node.path("suggestedRewrite").asText("");
                    final String scope = node.hasNonNull("scope") ? node.path("scope").asText(null) : null;

                    LinterCategory category = LinterCategory.fromCode(categoryStr);
                    final LinterSeverity severity = LinterSeverity.fromCode(severityStr);

                    StepTelemetryInfo matchedStep = null;
                    for (final StepTelemetryInfo info : this.frictionSteps)
                    {
                        if (info.stepIndex() == stepIndex)
                        {
                            matchedStep = info;
                            break;
                        }
                    }

                    if (category == null && matchedStep != null)
                    {
                        category = matchedStep.empiricalCategory();
                    }

                    final int lineNumber = matchedStep != null ? matchedStep.lineNumber() : -1;
                    final String sourceFile = matchedStep != null ? matchedStep.sourceFile() : null;
                    final String rawInstruction = matchedStep != null ? matchedStep.rawInstruction() : "";
                    final String resolvedInstruction = matchedStep != null ? matchedStep.resolvedInstruction() : rawInstruction;

                    if (category != null)
                    {
                        findings.add(new PlaybookLinterFinding(
                            stepIndex,
                            lineNumber,
                            sourceFile,
                            rawInstruction,
                            resolvedInstruction,
                            category,
                            severity,
                            message,
                            suggestedRewrite,
                            scope
                        ));
                    }
                }
            }
        }
        catch (final Exception ignored)
        {
            // Graceful degradation on JSON parsing errors
        }

        return findings;
    }
}
