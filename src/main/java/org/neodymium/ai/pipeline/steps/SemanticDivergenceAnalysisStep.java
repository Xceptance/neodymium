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
package org.neodymium.ai.pipeline.steps;

import java.io.IOException;
import java.util.Collections;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.prompt.SemanticDivergencePrompt;
import org.neodymium.ai.session.AiSession;

/**
 * Pipeline step executing the first stage of self-healing: Semantic Divergence Diffing.
 * Compares the baseline state of the failed step with the current page state to identify
 * layout, ID, or element changes, storing the plain-English summary in context.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class SemanticDivergenceAnalysisStep implements PipelineStep
{
    /**
     * Constructs a SemanticDivergenceAnalysisStep.
     */
    public SemanticDivergenceAnalysisStep()
    {
    }

    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
        if (session == null)
        {
            return;
        }

        final PlaybookStep currentStep = (PlaybookStep) context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
        final SutState currentState = (SutState) context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);

        String baselineState = null;
        if (currentStep != null)
        {
            baselineState = currentStep.getBaselineState();
        }

        String currentStateText = null;
        if (currentState != null)
        {
            currentStateText = currentState.getTextContent();
        }

        // If both baseline and current state are available and they differ:
        if (baselineState != null && currentStateText != null && !baselineState.equals(currentStateText))
        {
            final SemanticDivergencePrompt diffPrompt = new SemanticDivergencePrompt(baselineState, currentStateText);
            final String system = diffPrompt.compileSystemMessage(context);
            final String user = diffPrompt.compileUserMessage(context);

            final LlmRequest request = new LlmRequest(
                system,
                user,
                Collections.emptyList(),
                ResponseSchema.TEXT,
                0.0,
                60
            );

            try
            {
                final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
                final LlmResponse response = provider.chat(request);
                final String diffSummary = diffPrompt.parseResponse(response.content(), context);
                context.getTransientData().put(ExecutionContext.KEY_SEMANTIC_DIFF_SUMMARY, diffSummary);
            }
            catch (final IOException e)
            {
                // Soft fallback: log and set placeholder, do not crash self-healing
                context.getTransientData().put(ExecutionContext.KEY_SEMANTIC_DIFF_SUMMARY, "Failed to analyze semantic divergence.");
            }
        }
        else
        {
            context.getTransientData().put(ExecutionContext.KEY_SEMANTIC_DIFF_SUMMARY, "No layout changes detected (states match or baseline unavailable).");
        }
    }
}
