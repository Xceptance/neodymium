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

import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.structural.StateCapturedEvent;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.executor.selenide.plugins.ClickAction;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.pipeline.DivergenceException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.HealingRequiredException;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.util.ScreenshotHasher;
import org.neodymium.ai.util.VisualStabilityDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone pipeline step evaluating replay visual stability and SSIM gating thresholds.
 * Compares live browser state against recorded SSIM matrices before replaying interactive actions.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class VisualBaselineGateStep implements PipelineStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VisualBaselineGateStep.class);

    private final PlaybookStep step;
    private final AiSession session;

    /**
     * Constructs a VisualBaselineGateStep.
     *
     * @param step the playbook step
     * @param session the active AI session
     */
    public VisualBaselineGateStep(final PlaybookStep step, final AiSession session)
    {
        this.step = step;
        this.session = session;
    }

    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        executeGate(context);
    }

    /**
     * Executes the visual baseline gating check.
     *
     * @param context the execution context
     * @return true if the step matched visually and has no further actions (short-circuiting subsequent steps)
     * @throws PipelineException if visual baseline comparison fails in strict or healing mode
     */
    public boolean executeGate(final ExecutionContext context) throws PipelineException
    {
        if (this.step == null || context == null)
        {
            return false;
        }

        final String resolvedInstruction = context.getSessionData().resolveVariables(this.step.getInstruction());
        final ExecutionMode mode = (ExecutionMode) context.getTransientData()
            .computeIfAbsent(ExecutionContext.KEY_EXECUTION_MODE, k -> AiConfiguration.getInstance().getExecutionMode());

        if (mode.isReplay() && !this.step.isNoReplay() && this.step.getScreenshotHash() != null && !this.step.getScreenshotHash().isEmpty())
        {
            final TargetExecutor executor = (TargetExecutor) context.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
            if (executor != null)
            {
                try
                {
                    if (AiConfiguration.getInstance().isUseRecordedDelays())
                    {
                        final Long recStepDelay = this.step.getDelayMs();
                        if (recStepDelay != null && recStepDelay > 0)
                        {
                            final double scale = AiConfiguration.getInstance().getReplayDelayScale();
                            final long sleepTime = Math.max(0L, (long) (recStepDelay * scale));
                            if (sleepTime > 0)
                            {
                                try
                                {
                                    Thread.sleep(sleepTime);
                                }
                                catch (final InterruptedException e)
                                {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                    }

                    ClickAction.CoordinateTarget coordinateTarget = null;
                    if (this.step.getActions() != null)
                    {
                        for (final Action act : this.step.getActions())
                        {
                            if (act != null && act.getTarget() != null)
                            {
                                final ClickAction.CoordinateTarget parsed = ClickAction.parseCoordinateTarget(act.getTarget());
                                if (parsed != null)
                                {
                                    coordinateTarget = parsed;
                                    break;
                                }
                            }
                        }
                    }

                    final String recordedHash = this.step.getScreenshotHash();
                    final double minScore = coordinateTarget != null ? 0.95 : AiConfiguration.getInstance().getVisualSsimMinScore();

                    final boolean isFullPageReq = Boolean.TRUE.equals(context.getTransientData().get("KEY_IS_FULL_PAGE_SCREENSHOT"))
                        || (this.step != null && this.step.isFullPageVisualStep());
                    final SutState currentState = VisualStabilityDetector.captureSettledState(
                        executor,
                        isFullPageReq,
                        coordinateTarget == null ? recordedHash : null,
                        minScore);
                    context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, currentState);
                    if (this.session != null && this.session.getEventBus() != null && currentState != null)
                    {
                        this.session.getEventBus().dispatch(new StateCapturedEvent(currentState));
                    }

                    String currentSsimMatrix = null;
                    if (coordinateTarget != null)
                    {
                        // Micro-crop 64x64 luminance tile around centroid (x, y) with radius 32
                        if (currentState.getAttachments() != null)
                        {
                            for (final SutAttachment attachment : currentState.getAttachments())
                            {
                                if (attachment.mediaType() != null && attachment.mediaType().startsWith("image/") && attachment.base64Data() != null)
                                {
                                    currentSsimMatrix = ScreenshotHasher.computeTileSsimMatrix(attachment.base64Data(), coordinateTarget.x(), coordinateTarget.y(), 32);
                                    break;
                                }
                            }
                        }
                    }
                    else
                    {
                        currentSsimMatrix = VisualStabilityDetector.extractSsimMatrix(currentState);
                    }

                    if (this.step.getScreenshotHash() != null)
                    {
                        boolean isVisualMatch = false;
                        Double currentSsimScore = null;

                        if (currentSsimMatrix != null)
                        {
                            final double ssimScore = ScreenshotHasher.calculateSsim(recordedHash, currentSsimMatrix);
                            currentSsimScore = ssimScore;
                            this.step.setSsimScore(ssimScore);
                            this.step.setSsimMinScore(minScore);
                            this.step.setBaselineMatrixPng(ScreenshotHasher.matrixToDataUri(recordedHash));
                            this.step.setReplayMatrixPng(ScreenshotHasher.matrixToDataUri(currentSsimMatrix));
                            this.step.setScreenshotHashDim(coordinateTarget != null ? ScreenshotHasher.TILE_SSIM_MATRIX_DIM : (this.step.getScreenshotHashDim() != null ? this.step.getScreenshotHashDim() : ScreenshotHasher.DEFAULT_SSIM_MATRIX_DIM));

                            LOGGER.debug("   🖼️ [Visual SSIM Check] Instruction: \"{}\" | SSIM Score: {} | Required Min Score: {}",
                                resolvedInstruction, String.format("%.4f", ssimScore), minScore);

                            if (ssimScore >= minScore)
                            {
                                isVisualMatch = true;
                                LOGGER.info("   ✅ Visual SSIM match (score: {} >= {}) for instruction: \"{}\". Bypassing LLM call/actions.",
                                    String.format("%.4f", ssimScore), minScore, resolvedInstruction);
                            }
                            else
                            {
                                LOGGER.debug("   ⚠️ Visual SSIM score below threshold ({} < {}) for instruction: \"{}\"",
                                    String.format("%.4f", ssimScore), minScore, resolvedInstruction);
                            }
                        }

                        final boolean hasActualActions = this.step.getActions() != null && !this.step.getActions().isEmpty()
                            && this.step.getActions().stream().anyMatch(a -> !"NONE".equalsIgnoreCase(a.getType()));

                        if (isVisualMatch && !hasActualActions)
                        {
                            return true;
                        }
                        else
                        {
                            if (hasActualActions)
                            {
                                if (coordinateTarget != null && !isVisualMatch)
                                {
                                    final String msg = String.format("Coordinate SSIM tile mismatch (score: %s < %.2f) for instruction: \"%s\". Aborting coordinate click to prevent blind misclick.",
                                        currentSsimScore != null ? String.format("%.4f", currentSsimScore) : "N/A", minScore, resolvedInstruction);
                                    LOGGER.warn("   ❌ " + msg);

                                    if (mode.supportsHealing())
                                    {
                                        throw new HealingRequiredException(msg);
                                    }
                                    else
                                    {
                                        throw new DivergenceException(msg);
                                    }
                                }

                                if (isVisualMatch)
                                {
                                    LOGGER.info("   Visual match for interactive instruction: \"{}\". Executing actions anyway to guarantee state.",
                                        resolvedInstruction);
                                }
                                else
                                {
                                    LOGGER.debug("   Visual mismatch for interactive instruction: \"{}\". Proceeding to execute actions.",
                                        resolvedInstruction);
                                }
                            }
                            else
                            {
                                final String msg = String.format("Visual mismatch for instruction: \"%s\".", resolvedInstruction);
                                LOGGER.warn("   ❌ " + msg);

                                if (mode.supportsHealing())
                                {
                                    throw new HealingRequiredException(msg);
                                }
                                else
                                {
                                    throw new DivergenceException(msg);
                                }
                            }
                        }
                    }
                }
                catch (final PipelineException e)
                {
                    throw e;
                }
                catch (final Exception e)
                {
                    LOGGER.warn("   ⚠️ Failed to capture state or compare visual dHash: {}", e.getMessage());
                }
            }
        }
        return false;
    }
}
