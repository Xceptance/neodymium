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

import java.util.ArrayList;
import java.util.List;

import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.SutAttachment;

import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.prompt.VisualRcaPrompt;
import org.neodymium.ai.session.AiSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pipeline step executing Visual Root Cause Analysis (RCA) on test failure or breakpoint.
 * Captures page screenshots/DOM and queries LLM to diagnose visual/functional failure reasons.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class VisualRcaStep implements PipelineStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VisualRcaStep.class);

    /** Specific error message or exception context triggering the RCA analysis */
    private final String errorMessage;

    /**
     * Constructs a VisualRcaStep with explicit error message context.
     *
     * @param errorMessage the error message or exception summary
     */
    public VisualRcaStep(final String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    /**
     * Constructs a VisualRcaStep without explicit error message.
     */
    public VisualRcaStep()
    {
        this(null);
    }

    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        // 1. Retrieve the active session instance
        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
        if (session == null)
        {
            LOGGER.debug("No active session found in context. Skipping Visual RCA analysis.");
            return;
        }

        // 2. Extract current playbook step and current SUT state from transient context
        final PlaybookStep currentStep = (PlaybookStep) context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
        final SutState lastState = (SutState) context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);

        final String instruction = currentStep != null ? currentStep.getInstruction() : "Unknown step";
        final String err = this.errorMessage != null ? this.errorMessage : "Execution error occurred";

        // 3. Construct VisualRcaPrompt template with step instruction and error context
        final VisualRcaPrompt rcaPrompt = new VisualRcaPrompt(instruction, err);
        final String system = rcaPrompt.compileSystemMessage(context);
        final String user = rcaPrompt.compileUserMessage(context);

        // 4. Extract image attachments (screenshots) for multimodal vision analysis
        final List<SutAttachment> imageAttachments = new ArrayList<>();
        if (lastState != null && lastState.getAttachments() != null)
        {
            for (final SutAttachment attachment : lastState.getAttachments())
            {
                if (attachment.mediaType() != null && attachment.mediaType().startsWith("image/"))
                {
                    imageAttachments.add(attachment);
                }
            }
        }

        // 5. Select VISION capability if images present, otherwise fall back to TEXT_ONLY
        final LlmCapability requiredCap = imageAttachments.isEmpty() ? LlmCapability.TEXT_ONLY : LlmCapability.VISION;
        final LlmProvider provider = session.getLlmRegistry().getProvider(requiredCap);

        // 6. Build LLM chat request for diagnosis
        final LlmRequest request = new LlmRequest(
            system,
            user,
            imageAttachments,
            rcaPrompt.getResponseSchema(),
            0.0,
            60
        );

        try
        {
            // 7. Dispatch diagnosis call and parse plain-English RCA diagnosis summary
            LOGGER.debug("Executing Visual RCA via LLM provider '{}'", provider.getClass().getSimpleName());
            final LlmResponse response = provider.chat(request);
            final String diagnosis = rcaPrompt.parseResponse(response.content(), context);

            // 8. Log diagnosis warning and dispatch DiagnosticErrorEvent to event bus
            LOGGER.warn("🚨 [Visual RCA Diagnosis]: {}", diagnosis);
            context.getTransientData().put(ExecutionContext.KEY_VISUAL_RCA_SUMMARY, diagnosis);
            session.getEventBus().dispatch(new org.neodymium.ai.event.diagnostic.DiagnosticErrorEvent("Visual RCA Diagnosis: " + diagnosis, null));
        }
        catch (final Exception e)
        {
            // 9. Graceful fallback on analysis error
            LOGGER.warn("Failed to perform Visual RCA analysis: {}", e.getMessage(), e);
            context.getTransientData().put(ExecutionContext.KEY_VISUAL_RCA_SUMMARY, "RCA analysis unavailable: " + e.getMessage());
        }
    }
}
