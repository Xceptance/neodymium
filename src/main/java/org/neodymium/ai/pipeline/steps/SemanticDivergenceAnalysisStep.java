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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pipeline step executing the first stage of self-healing: Semantic Divergence Diffing.
 * Compares the baseline state of the failed step with the current page state to identify
 * layout, ID, or element changes, storing the natural language summary in context.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class SemanticDivergenceAnalysisStep implements PipelineStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticDivergenceAnalysisStep.class);

    /**
     * Constructs a SemanticDivergenceAnalysisStep.
     */
    public SemanticDivergenceAnalysisStep()
    {
    }

    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        final ExecutionContext previousContext = ExecutionContext.getActiveContext();
        try
        {
            ExecutionContext.setActiveContext(context);
            executeInternal(context);
        }
        finally
        {
            ExecutionContext.setActiveContext(previousContext);
        }
    }

    private void executeInternal(final ExecutionContext context) throws PipelineException
    {
        // 1. Retrieve the active session instance
        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
        if (session == null)
        {
            LOGGER.debug("No active session found in context. Skipping semantic divergence analysis.");
            return;
        }

        // 2. Extract current playbook step and current SUT page state
        final PlaybookStep currentStep = (PlaybookStep) context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
        final SutState currentState = (SutState) context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);

        // 3. Extract baseline DOM representation from playbook step if present
        String baselineState = null;
        if (currentStep != null)
        {
            baselineState = currentStep.getBaselineState();
        }

        // 4. Extract current DOM text content from captured SUT state
        String currentStateText = null;
        if (currentState != null)
        {
            currentStateText = currentState.getTextContent();
        }

        // 5. Compare baseline and current states to determine if structural diffing is required
        if (baselineState != null && currentStateText != null && !baselineState.equals(currentStateText))
        {
            LOGGER.debug("DOM state divergence detected. Compiling SemanticDivergencePrompt...");

            // 6. Build prompt comparing baseline DOM text vs current DOM text
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
                // 7. Call text-capable LLM provider to compute semantic diff
                final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
                LOGGER.debug("Calling LLM provider '{}' via capability: TEXT_ONLY", provider.getClass().getSimpleName());
                final long startTime = System.currentTimeMillis();
                final LlmResponse response = provider.chat(request);
                final long durationMs = System.currentTimeMillis() - startTime;
                LOGGER.debug("LLM response received. Length: {} chars (duration: {} ms)", response.content() != null ? response.content().length() : 0, durationMs);
                
                // 8. Parse natural language diff summary and store in transient context map
                final String diffSummary = diffPrompt.parseResponse(response.content(), context);
                context.getTransientData().put(ExecutionContext.KEY_SEMANTIC_DIFF_SUMMARY, diffSummary);
            }
            catch (final IOException e)
            {
                // 9. Soft fallback on IO error to keep execution resilient
                LOGGER.warn("Failed to execute semantic divergence diff request: {}", e.getMessage(), e);
                context.getTransientData().put(ExecutionContext.KEY_SEMANTIC_DIFF_SUMMARY, "Failed to analyze semantic divergence.");
            }
        }
        else
        {
            // 10. Record match when baseline and current DOM text are structurally identical
            LOGGER.debug("No layout changes detected (DOM states match or baseline unavailable).");
            context.getTransientData().put(ExecutionContext.KEY_SEMANTIC_DIFF_SUMMARY, "No layout changes detected (states match or baseline unavailable).");
        }
    }
}
