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
import org.neodymium.ai.action.Action;
import org.neodymium.ai.action.LocatorCandidate;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.prompt.QualityJudgePrompt;
import org.neodymium.ai.prompt.QualityJudgePrompt.QualityJudgeResult;
import org.neodymium.ai.session.AiSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pipeline step executing the LLM Quality Judge ("second opinion") review.
 * Evaluates extracted action locators and candidate options against stability,
 * uniqueness, and text targeting rules prior to SUT execution.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class QualityJudgeStep implements PipelineStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(QualityJudgeStep.class);
    private final QualityJudgePrompt judgePrompt;

    public QualityJudgeStep()
    {
        this.judgePrompt = new QualityJudgePrompt();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(final ExecutionContext context) throws PipelineException
    {
        final AiConfiguration config = AiConfiguration.getInstance();

        if (!config.isJudgeEnabled())
        {
            LOGGER.debug("Quality Judge step is disabled via neodymium.ai.judge.enabled=false. Skipping evaluation.");
            return;
        }

        final Object rawResult = context.getTransientData().get(ExecutionContext.KEY_LAST_LLM_RESULT);
        List<Action> actions = null;
        if (rawResult instanceof List<?> list)
        {
            actions = (List<Action>) list;
        }

        if (actions == null || actions.isEmpty())
        {
            LOGGER.debug("No extracted actions found in context. Skipping Quality Judge step.");
            return;
        }

        final Action proposedAction = actions.get(0);
        if (proposedAction == null || proposedAction.getType().isEmpty())
        {
            LOGGER.debug("First extracted action is empty. Skipping Quality Judge step.");
            return;
        }

        final String judgeMode = config.getJudgeMode();
        final boolean isRetry = Boolean.TRUE.equals(context.getTransientData().get("isRetryExecution"));

        if ("ON_FAIL".equals(judgeMode) && !isRetry)
        {
            LOGGER.debug("Quality Judge mode is ON_FAIL. Skipping initial evaluation before execution.");
            return;
        }

        if ("ON_AMBIGUITY".equals(judgeMode) && !isRetry)
        {
            final List<LocatorCandidate> candidates = proposedAction.getCandidateLocators();
            if (candidates.size() < 2)
            {
                LOGGER.debug("Quality Judge mode is ON_AMBIGUITY but less than 2 candidates exist. Skipping.");
                return;
            }
            final double score1 = candidates.get(0).getScore();
            final double score2 = candidates.get(1).getScore();
            if ((score1 - score2) >= 0.15)
            {
                LOGGER.debug("Quality Judge mode is ON_AMBIGUITY. Clear candidate winner found (diff: {}). Skipping.", String.format("%.2f", score1 - score2));
                return;
            }
            LOGGER.info("⚖️ Quality Judge triggered due to ambiguous top candidate scores (diff: {}).", String.format("%.2f", score1 - score2));
        }

        final String instruction = (String) context.getTransientData().get(ExecutionContext.KEY_CURRENT_INSTRUCTION);
        final Object stateObj = context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
        final String domContext = stateObj instanceof SutState sutState ? sutState.getTextContent() : "";
        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
        if (session == null)
        {
            LOGGER.debug("No AiSession found in context. Skipping Quality Judge step.");
            return;
        }

        LOGGER.info("⚖️ Quality Judge reviewing proposed action '{}' on target '{}'...", proposedAction.getType(), proposedAction.getTarget());

        final LlmRequest request = this.judgePrompt.compileRequest(instruction, domContext, proposedAction, config);

        try
        {
            final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
            final LlmResponse response = provider.chat(request);

            if (response != null && response.content() != null)
            {
                final QualityJudgeResult result = this.judgePrompt.parseResponse(response.content());
                LOGGER.info("⚖️ Quality Judge Judgment: {} | Chosen Locator: '{}' | Reasoning: {}",
                        result.getJudgment(), result.getChosenLocator(), result.getReasoning());

                if (!result.getChosenLocator().isEmpty())
                {
                    Action updatedAction = proposedAction.withTarget(result.getChosenLocator());
                    if (result.getChosenValue() != null && !result.getChosenValue().isEmpty())
                    {
                        updatedAction = updatedAction.withValue(result.getChosenValue());
                    }
                    if (result.isRegex() != proposedAction.isRegex())
                    {
                        updatedAction = updatedAction.withIsRegex(result.isRegex());
                    }
                    final List<Action> updatedActions = new ArrayList<>(actions);
                    updatedActions.set(0, updatedAction);
                    context.getTransientData().put(ExecutionContext.KEY_LAST_LLM_RESULT, updatedActions);
                }
            }
        }
        catch (final Exception e)
        {
            LOGGER.warn("Quality Judge step execution encountered an error: {}. Proceeding with primary action.", e.getMessage(), e);
        }
    }
}
