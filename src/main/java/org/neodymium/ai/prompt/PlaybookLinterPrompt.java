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
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.playbook.linter.LinterCategory;
import org.neodymium.ai.playbook.linter.LinterSeverity;
import org.neodymium.ai.playbook.linter.PlaybookLinterFinding;

/**
 * AI prompt implementation for the upfront playbook pre-flight linter.
 * Formats scenario steps and optional description into a single batch request
 * and parses structured linguistic/semantic advisory findings.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookLinterPrompt implements AiPrompt<List<PlaybookLinterFinding>>
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String description;
    private final List<StepInfo> steps;

    /**
     * DTO capturing step information for prompt compilation and finding reconciliation.
     *
     * @param stepIndex 1-based index
     * @param lineNumber source line number
     * @param sourceFile source file path
     * @param rawInstruction unresolved template instruction
     * @param resolvedInstruction data-substituted instruction
     */
    public record StepInfo(
        int stepIndex,
        int lineNumber,
        String sourceFile,
        String rawInstruction,
        String resolvedInstruction
    )
    {
    }

    /**
     * Constructs a PlaybookLinterPrompt from a list of PlaybookStep objects.
     *
     * @param description optional high-level scenario description
     * @param playbookSteps scenario steps
     * @param context active execution context for variable resolution
     */
    public PlaybookLinterPrompt(final String description, final List<PlaybookStep> playbookSteps, final ExecutionContext context)
    {
        this.description = description != null && !description.isBlank() ? description.trim() : null;
        this.steps = new ArrayList<>();

        if (playbookSteps != null)
        {
            for (int i = 0; i < playbookSteps.size(); i++)
            {
                final PlaybookStep pbStep = playbookSteps.get(i);
                final String raw = pbStep.getInstruction() != null ? pbStep.getInstruction() : "";
                String resolved = raw;

                if (context != null && context.getSessionData() != null && !raw.isBlank())
                {
                    try
                    {
                        resolved = context.getSessionData().resolveVariables(raw);
                    }
                    catch (final Exception ignored)
                    {
                    }
                }

                this.steps.add(new StepInfo(
                    i + 1,
                    pbStep.getLineNumber(),
                    pbStep.getSourceFile(),
                    raw,
                    resolved
                ));
            }
        }
    }

    /**
     * Constructs a PlaybookLinterPrompt with explicit StepInfo items.
     *
     * @param description optional high-level scenario description
     * @param steps list of StepInfo
     */
    public PlaybookLinterPrompt(final String description, final List<StepInfo> steps)
    {
        this.description = description != null && !description.isBlank() ? description.trim() : null;
        this.steps = steps != null ? List.copyOf(steps) : Collections.emptyList();
    }

    @Override
    public ResponseSchema getResponseSchema()
    {
        return ResponseSchema.LINTER;
    }

    @Override
    public String compileSystemMessage(final ExecutionContext context)
    {
        return SystemPromptAddonHelper.appendAddon(AiAgentPrompts.getPlaybookLinterPrompt(), "linter", context);
    }

    @Override
    public String compileUserMessage(final ExecutionContext context)
    {
        final StringBuilder sb = new StringBuilder();

        if (this.description != null && !this.description.isBlank())
        {
            sb.append("## Scenario Context\nGoal: ").append(this.description).append("\n\n");
        }

        sb.append("## Playbook Scenario Instructions\n");
        if (this.steps.isEmpty())
        {
            sb.append("(No instructions in scenario)\n");
        }
        else
        {
            for (final StepInfo step : this.steps)
            {
                sb.append(step.stepIndex()).append(". ").append(step.rawInstruction()).append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public List<PlaybookLinterFinding> parseResponse(final String rawContent, final ExecutionContext context)
    {
        final List<PlaybookLinterFinding> findings = new ArrayList<>();
        final String jsonContent = LlmResponseSanitizer.extractJson(rawContent);

        if (jsonContent.isBlank())
        {
            return findings;
        }

        try
        {
            final JsonNode root = MAPPER.readTree(jsonContent);
            final JsonNode findingsNode = root.path("findings");

            if (findingsNode.isArray())
            {
                for (final JsonNode node : findingsNode)
                {
                    final int stepIndex = node.path("stepIndex").asInt(0);
                    final String categoryStr = node.path("category").asText(null);
                    final String severityStr = node.path("severity").asText("WARNING");
                    final String message = node.path("message").asText("");
                    final String suggestedRewrite = node.path("suggestedRewrite").asText("");
                    final String scope = node.hasNonNull("scope") ? node.path("scope").asText(null) : null;

                    final LinterCategory category = LinterCategory.fromCode(categoryStr);
                    final LinterSeverity severity = LinterSeverity.fromCode(severityStr);

                    // Find matching StepInfo to extract line number, source file, raw and resolved instruction
                    StepInfo matchedStep = null;
                    for (final StepInfo info : this.steps)
                    {
                        if (info.stepIndex() == stepIndex)
                        {
                            matchedStep = info;
                            break;
                        }
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
            // Graceful fallback on non-JSON or malformed responses
        }

        return findings;
    }
}
