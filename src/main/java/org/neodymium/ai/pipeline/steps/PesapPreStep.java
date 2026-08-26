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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.llm.LlmRequestSentEvent;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.StepStats;
import org.neodymium.ai.prompt.DefaultActionSanitizer;
import org.neodymium.ai.prompt.PesapPrompt;
import org.neodymium.ai.session.AiSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone pipeline step executing Just-In-Time (JIT) pre-step PESAP analysis.
 * Predicts minimal required context levels and splits compound instructions prior to execution.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class PesapPreStep implements PipelineStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PesapPreStep.class);

    private final PlaybookStep step;
    private final AiSession session;

    /**
     * Constructs a PesapPreStep.
     *
     * @param step the playbook step
     * @param session the active AI session
     */
    public PesapPreStep(final PlaybookStep step, final AiSession session)
    {
        this.step = step;
        this.session = session;
    }

    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        executePreStep(context);
    }

    /**
     * Executes pre-step PESAP analysis and returns true if compound step was split into sub-steps.
     *
     * @param context the execution context
     * @return true if step was split into sub-steps, false otherwise
     * @throws PipelineException if pre-step fails fatally
     */
    public boolean executePreStep(final ExecutionContext context) throws PipelineException
    {
        if (this.step == null || context == null)
        {
            return false;
        }

        final String resolvedInstruction = context.getSessionData().resolveVariables(this.step.getInstruction());
        final StepStats stats = (StepStats) context.getTransientData().get("KEY_CURRENT_STEP_STATS");

        @SuppressWarnings("unchecked")
        final Set<PlaybookStep> alreadySplitSteps = (Set<PlaybookStep>) context.getTransientData()
            .computeIfAbsent("pesap.alreadySplitSteps", k -> new HashSet<>());

        final ExecutionMode executionMode = (ExecutionMode) context.getTransientData()
            .computeIfAbsent(ExecutionContext.KEY_EXECUTION_MODE, k -> AiConfiguration.getInstance().getExecutionMode());
        final boolean isReplay = executionMode == ExecutionMode.REPLAY_STRICT
            || (executionMode != null && executionMode.isReplay() && !this.step.isNoReplay() && this.step.getActions() != null && (!this.step.getActions().isEmpty() || this.step.getScreenshotHash() != null));
        final AiConfiguration config = AiConfiguration.getInstance();

        if (!isReplay && config.getBoolean("neodymium.ai.pesap.enabled", true) && !alreadySplitSteps.contains(this.step))
        {
            alreadySplitSteps.add(this.step);
            try
            {
                final PesapPrompt pesapPrompt = new PesapPrompt(resolvedInstruction);
                final LlmProvider provider = this.session.getLlmRegistry().getProvider(LlmCapability.PESAP);
                final double temp = config.getTemperature("action");
                final int timeoutSeconds = config.getTimeoutSeconds("action");

                final LlmRequest request = new LlmRequest(
                    pesapPrompt.compileSystemMessage(context),
                    pesapPrompt.compileUserMessage(context),
                    Collections.emptyList(),
                    pesapPrompt.getResponseSchema(),
                    temp,
                    timeoutSeconds
                );

                LOGGER.debug("💬 [Pre-Step PESAP] Running analysis using provider '{}'", provider.getClass().getSimpleName());
                if (LOGGER.isTraceEnabled())
                {
                    LOGGER.trace("System Prompt:\n{}", request.systemMessage());
                    LOGGER.trace("User Prompt:\n{}", request.userMessage());
                }

                if (this.session != null && this.session.getEventBus() != null)
                {
                    this.session.getEventBus().dispatch(new LlmRequestSentEvent(request, "PESAP"));
                }

                final long startTime = System.currentTimeMillis();
                final LlmResponse response = provider.chat(request);
                final long durationMs = System.currentTimeMillis() - startTime;

                if (this.session != null && this.session.getEventBus() != null)
                {
                    this.session.getEventBus().dispatch(new LlmResponseReceivedEvent(request, response, durationMs, "PESAP"));
                }

                LOGGER.debug("LLM response received. Length: {} chars (duration: {} ms)",
                    response.content() != null ? response.content().length() : 0, durationMs);
                if (LOGGER.isTraceEnabled())
                {
                    LOGGER.trace("Raw response content:\n{}", CallLlmStep.formatJsonForLogging(response.content()));
                }

                context.getTransientData().compute("pesapCallCount", (k, v) -> v == null ? 1 : ((Integer) v) + 1);

                final TokenUsage newUsage = response.tokenUsage();
                if (newUsage != null)
                {
                    if (stats != null)
                    {
                        stats.addPesapCall(newUsage.inputTokenCount(), newUsage.outputTokenCount(), newUsage.cachedTokenCount());
                    }

                    LOGGER.debug("   📊 [Pre-Step PESAP] Tokens: {} in ({} cached) → {} out (total: {})",
                        newUsage.inputTokenCount(), newUsage.cachedTokenCount(), newUsage.outputTokenCount(), newUsage.totalTokenCount());

                    final TokenUsage existing = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_PESAP_TOKEN_USAGE);
                    if (existing == null)
                    {
                        context.getTransientData().put(ExecutionContext.KEY_PESAP_TOKEN_USAGE, newUsage);
                    }
                    else
                    {
                        context.getTransientData().put(ExecutionContext.KEY_PESAP_TOKEN_USAGE, new TokenUsage(
                            existing.inputTokenCount() + newUsage.inputTokenCount(),
                            existing.outputTokenCount() + newUsage.outputTokenCount(),
                            existing.totalTokenCount() + newUsage.totalTokenCount(),
                            existing.cachedTokenCount() + newUsage.cachedTokenCount()
                        ));
                    }
                }

                final PesapPrompt.PesapResult pesapResult = pesapPrompt.parseResponse(response.content(), context);

                final boolean isPureNavigation = resolvedInstruction != null && resolvedInstruction.trim().matches("(?i)^(open|navigate\\s+to|go\\s+to)\\s+https?://\\S+$");
                if (!isPureNavigation && pesapResult.splitSteps() != null && pesapResult.splitSteps().size() > 1)
                {
                    LOGGER.info("✂️ Upfront JIT step split detected: \"{}\" split into {}", resolvedInstruction, pesapResult.splitSteps());
                    final DefaultActionSanitizer sanitizer = new DefaultActionSanitizer();
                    for (final String part : pesapResult.splitSteps())
                    {
                        final String cleanPart = sanitizer.sanitizeText(part, context.getSessionData());
                        final PlaybookStep subStep = new PlaybookStep(cleanPart);
                        subStep.setSourceFile(this.step.getSourceFile());
                        subStep.setLineNumber(this.step.getLineNumber());
                        subStep.setParent(this.step);
                        
                        // Explicitly copy control flags from the parent step
                        subStep.setBug(this.step.isBug());
                        subStep.setBugDetails(this.step.getBugDetails());
                        subStep.setContinueOnError(this.step.isContinueOnError());
                        subStep.setNoHealing(this.step.isNoHealing());
                        subStep.setOptional(this.step.isOptional());
                        
                        this.step.getSubSteps().add(subStep);
                    }

                    final List<PipelineStep> subPipelineSteps = new ArrayList<>();
                    for (final PlaybookStep subStep : this.step.getSubSteps())
                    {
                        subPipelineSteps.add(ExecuteActionsStep.mapPlaybookStepToPipelineStep(subStep, this.session, context));
                    }
                    for (int i = subPipelineSteps.size() - 1; i >= 0; i--)
                    {
                        context.pushStep(subPipelineSteps.get(i));
                    }
                    return true;
                }

                if (pesapResult.contextLevel() != null)
                {
                    try
                    {
                        final ContextLevel predicted = ContextLevel.valueOf(pesapResult.contextLevel().toUpperCase().trim());
                        final ContextLevel currentLevel = (ContextLevel) context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL);
                        final boolean isExplicitTag = (currentLevel == ContextLevel.HINT || (currentLevel != null && currentLevel.includesScreenshot()));
                        if (!isExplicitTag || (predicted.ordinal() > currentLevel.ordinal()))
                        {
                            context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, predicted);
                            this.step.setContextLevel(predicted.name());
                        }
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
            }
            catch (final Exception e)
            {
                LOGGER.warn("⚠️ Pre-Step PESAP failed for step '{}' — falling back to defaults: {}", resolvedInstruction, e.getMessage());
            }
        }
        return false;
    }
}
