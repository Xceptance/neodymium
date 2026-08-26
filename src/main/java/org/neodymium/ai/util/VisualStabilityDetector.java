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
package org.neodymium.ai.util;

import java.io.IOException;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.model.ContextLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for detecting temporal inter-frame visual stability of the System Under Test (SUT).
 * <p>
 * Evaluates consecutive visual state captures separated by at least 1-second intervals,
 * comparing Frame {@code t} against Frame {@code t-1} using Mean Structural Similarity (SSIM).
 * The SUT is considered visually settled/quiescent once inter-frame stability reaches the
 * configured threshold (default: {@code 0.999}), or when the maximum attempts limit (default: 5) is reached.
 * </p>
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class VisualStabilityDetector
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VisualStabilityDetector.class);

    /**
     * Private constructor to prevent instantiation.
     */
    private VisualStabilityDetector()
    {
    }

    /**
     * Captures a visually settled SUT state using default configuration parameters from {@link AiConfiguration}.
     *
     * @param executor the target SUT executor
     * @param isFullPage true to force full-page screenshot capture
     * @return the captured, visually settled {@link SutState}
     * @throws IOException if state capture fails
     */
    public static SutState captureSettledState(final TargetExecutor executor, final boolean isFullPage) throws IOException
    {
        final AiConfiguration config = AiConfiguration.getInstance();
        return captureSettledState(
            executor,
            isFullPage,
            null,
            0.0,
            config.getVisualStabilityIntervalMs(),
            config.getVisualStabilityMaxAttempts(),
            config.getVisualStabilityThreshold());
    }

    /**
     * Captures a visually settled SUT state comparing against an expected baseline matrix.
     *
     * @param executor the target SUT executor
     * @param isFullPage true to force full-page screenshot capture
     * @param expectedBaselineMatrix optional recorded SSIM baseline matrix
     * @param targetMinScore required minimum SSIM score for baseline match
     * @return the captured, visually settled {@link SutState}
     * @throws IOException if state capture fails
     */
    public static SutState captureSettledState(
        final TargetExecutor executor,
        final boolean isFullPage,
        final String expectedBaselineMatrix,
        final double targetMinScore) throws IOException
    {
        final AiConfiguration config = AiConfiguration.getInstance();
        return captureSettledState(
            executor,
            isFullPage,
            expectedBaselineMatrix,
            targetMinScore,
            config.getVisualStabilityIntervalMs(),
            config.getVisualStabilityMaxAttempts(),
            config.getVisualStabilityThreshold());
    }

    /**
     * Captures a visually settled SUT state by polling consecutive frames until inter-frame stability is achieved.
     *
     * @param executor the target SUT executor
     * @param isFullPage true to force full-page screenshot capture
     * @param intervalMs polling interval between consecutive frame captures in milliseconds (minimum: 1000ms)
     * @param maxAttempts maximum number of settling attempts before proceeding (default: 5)
     * @param stabilityThreshold minimum inter-frame SSIM score to consider state settled (default: 0.999)
     * @return the captured, visually settled {@link SutState}
     * @throws IOException if state capture fails
     */
    public static SutState captureSettledState(
        final TargetExecutor executor,
        final boolean isFullPage,
        final long intervalMs,
        final int maxAttempts,
        final double stabilityThreshold) throws IOException
    {
        return captureSettledState(
            executor,
            isFullPage,
            null,
            0.0,
            intervalMs,
            maxAttempts,
            stabilityThreshold);
    }

    /**
     * Captures a visually settled SUT state by polling consecutive frames until the expected baseline is matched
     * or inter-frame visual quiescence is achieved.
     *
     * @param executor the target SUT executor
     * @param isFullPage true to force full-page screenshot capture
     * @param expectedBaselineMatrix optional recorded SSIM baseline matrix to match against
     * @param targetMinScore required minimum SSIM score for baseline match
     * @param intervalMs polling interval between consecutive frame captures in milliseconds (minimum: 1000ms)
     * @param maxAttempts maximum number of settling attempts before proceeding (default: 5)
     * @param stabilityThreshold minimum inter-frame SSIM score to consider state settled (default: 0.999)
     * @return the captured, visually settled {@link SutState}
     * @throws IOException if state capture fails
     */
    public static SutState captureSettledState(
        final TargetExecutor executor,
        final boolean isFullPage,
        final String expectedBaselineMatrix,
        final double targetMinScore,
        final long intervalMs,
        final int maxAttempts,
        final double stabilityThreshold) throws IOException
    {
        if (executor == null)
        {
            throw new IllegalArgumentException("TargetExecutor cannot be null");
        }

        final long effectiveInterval = Math.max(1000L, intervalMs);
        final int effectiveMaxAttempts = Math.max(1, maxAttempts);

        // 1. Capture initial Frame 0
        final ContextLevel captureLevel = isFullPage ? ContextLevel.VISUAL_LEAN : ContextLevel.VISUAL;
        SutState previousState = executor.captureState(captureLevel, isFullPage);
        String previousMatrix = extractSsimMatrix(previousState);

        if (previousMatrix == null)
        {
            LOGGER.warn("   ⚠️ Initial visual capture produced no image attachment; returning initial state without temporal polling");
            return previousState;
        }

        if (expectedBaselineMatrix != null && !expectedBaselineMatrix.isBlank())
        {
            final double initialBaselineScore = ScreenshotHasher.calculateSsim(expectedBaselineMatrix, previousMatrix);
            if (initialBaselineScore >= targetMinScore)
            {
                LOGGER.info("   ✅ [Visual Settling] Target visual baseline matched on initial capture (SSIM: {} >= {})",
                    String.format("%.4f", initialBaselineScore), targetMinScore);
                return previousState;
            }
        }

        SutState currentState = previousState;
        String currentMatrix = previousMatrix;

        // 2. Poll consecutive frames at intervals
        for (int attempt = 1; attempt <= effectiveMaxAttempts; attempt++)
        {
            try
            {
                Thread.sleep(effectiveInterval);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
                LOGGER.debug("   ⚠️ Visual stability polling interrupted at attempt {}/{}", attempt, effectiveMaxAttempts);
                break;
            }

            currentState = executor.captureState(captureLevel, isFullPage);
            currentMatrix = extractSsimMatrix(currentState);

            if (currentMatrix == null)
            {
                LOGGER.warn("   ⚠️ Frame capture at attempt {} produced no image attachment", attempt);
                continue;
            }

            if (expectedBaselineMatrix != null && !expectedBaselineMatrix.isBlank())
            {
                final double baselineScore = ScreenshotHasher.calculateSsim(expectedBaselineMatrix, currentMatrix);
                LOGGER.debug("   🖼️ [Visual Baseline Polling] Attempt {}/{} (interval: {}ms): Baseline SSIM = {} (Target: >= {})",
                    attempt, effectiveMaxAttempts, effectiveInterval, String.format("%.4f", baselineScore), targetMinScore);

                if (baselineScore >= targetMinScore)
                {
                    LOGGER.info("   ✅ [Visual Settling] Target visual baseline matched (SSIM: {} >= {}) at attempt {}/{}",
                        String.format("%.4f", baselineScore), targetMinScore, attempt, effectiveMaxAttempts);
                    return currentState;
                }
            }

            final double interFrameStability = ScreenshotHasher.calculateSsim(previousMatrix, currentMatrix);
            LOGGER.debug("   🖼️ [Visual Stability] Attempt {}/{} (interval: {}ms): Inter-frame SSIM = {} (Target: >= {})",
                attempt, effectiveMaxAttempts, effectiveInterval, String.format("%.4f", interFrameStability), stabilityThreshold);

            if (expectedBaselineMatrix == null && interFrameStability >= stabilityThreshold)
            {
                LOGGER.info("   ✅ [Visual Settling] SUT visually stabilized after {} attempt(s) (inter-frame stability: {} >= {})",
                    attempt, String.format("%.4f", interFrameStability), stabilityThreshold);
                return currentState;
            }

            if (expectedBaselineMatrix != null && !expectedBaselineMatrix.isBlank())
            {
                LOGGER.debug("   ⏳ [Visual Settling] Target visual baseline not yet reached (attempt {}/{}, stability: {}). Waiting {}ms...",
                    attempt, effectiveMaxAttempts, String.format("%.4f", interFrameStability), effectiveInterval);
            }
            else
            {
                LOGGER.debug("   ⏳ [Visual Settling] SUT still transitioning (attempt {}/{}, stability: {} < {}). Waiting {}ms...",
                    attempt, effectiveMaxAttempts, String.format("%.4f", interFrameStability), stabilityThreshold, effectiveInterval);
            }

            previousState = currentState;
            previousMatrix = currentMatrix;
        }

        LOGGER.warn("   ⚠️ [Visual Settling] Reached maximum settling attempts ({}) without reaching target baseline or stability threshold ({}). Proceeding with latest frame.",
            effectiveMaxAttempts, stabilityThreshold);
        return currentState;
    }

    /**
     * Extracts the 64x64 SSIM luminance matrix from image attachments in the given {@link SutState}.
     *
     * @param state the SUT state
     * @return Base64-encoded SSIM matrix string, or null if no image attachment is found
     */
    public static String extractSsimMatrix(final SutState state)
    {
        if (state == null || state.getAttachments() == null)
        {
            return null;
        }

        for (final SutAttachment attachment : state.getAttachments())
        {
            if (attachment.mediaType() != null && attachment.mediaType().startsWith("image/") && attachment.base64Data() != null)
            {
                return ScreenshotHasher.computeSsimMatrix(attachment.base64Data());
            }
        }
        return null;
    }
}
