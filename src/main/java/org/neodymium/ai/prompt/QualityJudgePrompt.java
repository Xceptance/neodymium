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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.action.LocatorCandidate;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.executor.selenide.SelenideTargetExecutor;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Prompt builder and parser for the Quality Judge ("second opinion") evaluation step.
 * Compiles DOM context, proposed action, and candidate locators into a structured LLM query.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class QualityJudgePrompt implements AiPrompt<QualityJudgePrompt.QualityJudgeResult>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(QualityJudgePrompt.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String systemPrompt;

    public QualityJudgePrompt()
    {
        this.systemPrompt = AiAgentPrompts.getQualityJudgePrompt().trim();
    }

    @Override
    public org.neodymium.ai.client.ResponseSchema getResponseSchema()
    {
        return org.neodymium.ai.client.ResponseSchema.TEXT;
    }

    @Override
    public String compileSystemMessage(final ExecutionContext context)
    {
        final Object targetExecutor = context != null ? context.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR) : null;
        final boolean isSelenideMode = targetExecutor instanceof SelenideTargetExecutor || targetExecutor == null;

        String compiledSystemPrompt = systemPrompt;
        if (isSelenideMode)
        {
            compiledSystemPrompt = compiledSystemPrompt + ActionExtractionPrompt.SELENIDE_LOCATOR_RULE;
        }
        return compiledSystemPrompt;
    }

    @Override
    public String compileUserMessage(final ExecutionContext context)
    {
        if (context == null)
        {
            return "";
        }
        final String instruction = (String) context.getTransientData().get("KEY_CURRENT_INSTRUCTION");
        final String domContext = (String) context.getTransientData().get("KEY_DOM_CONTEXT");
        final Action proposedAction = (Action) context.getTransientData().get("KEY_PROPOSED_ACTION");
        final AiConfiguration aiConfig = AiConfiguration.getInstance();
        return compileUserMessage(instruction, domContext, proposedAction, aiConfig);
    }

    /**
     * Compiles user query message from components.
     *
     * @param instruction step instruction
     * @param domContext DOM context text
     * @param proposedAction proposed action
     * @param aiConfig AI configuration
     * @return compiled user prompt message
     */
    public String compileUserMessage(
            final String instruction,
            final String domContext,
            final Action proposedAction,
            final AiConfiguration aiConfig)
    {
        final StringBuilder userMsg = new StringBuilder();
        userMsg.append("## Execution Instruction\n");
        userMsg.append(instruction != null ? instruction : "").append("\n\n");

        if (proposedAction != null)
        {
            userMsg.append("## Proposed Action\n");
            userMsg.append("Action Type: ").append(proposedAction.getType()).append("\n");
            userMsg.append("Primary Locator: ").append(proposedAction.getTarget()).append("\n");
            userMsg.append("Value: ").append(proposedAction.getValue() != null ? proposedAction.getValue() : "").append("\n");
            userMsg.append("isRegex: ").append(proposedAction.isRegex()).append("\n");
            userMsg.append("Reasoning: ").append(proposedAction.getReasoning()).append("\n\n");

            userMsg.append("## Candidate Locators\n");
            final List<LocatorCandidate> candidates = proposedAction.getCandidateLocators();
            final boolean isCompact = "COMPACT".equalsIgnoreCase(aiConfig != null ? aiConfig.getLocatorsFormat() : "DETAILED");
            if (candidates != null && !candidates.isEmpty())
            {
                for (int i = 0; i < candidates.size(); i++)
                {
                    final LocatorCandidate cand = candidates.get(i);
                    if (isCompact)
                    {
                        userMsg.append(String.format("Candidate %d: '%s' (%s)\n", i + 1, cand.getLocator(), cand.getStrategy()));
                    }
                    else
                    {
                        final String reasoning = cand.getReasoning();
                        if (reasoning != null && !reasoning.isBlank())
                        {
                            userMsg.append(String.format("Candidate %d: locator='%s', strategy='%s', score=%.2f, reasoning='%s'\n",
                                    i + 1, cand.getLocator(), cand.getStrategy(), cand.getScore(), reasoning.trim()));
                        }
                        else
                        {
                            userMsg.append(String.format("Candidate %d: locator='%s', strategy='%s', score=%.2f\n",
                                    i + 1, cand.getLocator(), cand.getStrategy(), cand.getScore()));
                        }
                    }
                }
            }
            else
            {
                userMsg.append("Candidate 1: locator='").append(proposedAction.getTarget()).append("'\n");
            }
            userMsg.append("\n");
        }

        userMsg.append("## Current DOM Context\n");
        userMsg.append(domContext != null ? domContext : "").append("\n");

        return userMsg.toString();
    }

    /**
     * Compiles an LlmRequest for evaluating the proposed action against the DOM context.
     *
     * @param instruction      the step instruction
     * @param domContext       the current DOM text context
     * @param proposedAction   the primary action extracted by the first LLM call
     * @param aiConfig         AI configuration instance
     * @return compiled LlmRequest
     */
    public LlmRequest compileRequest(
            final String instruction,
            final String domContext,
            final Action proposedAction,
            final AiConfiguration aiConfig)
    {
        final String userMsg = compileUserMessage(instruction, domContext, proposedAction, aiConfig);

        final double temp = aiConfig != null ? aiConfig.getTemperature("judge") : 0.0;
        final int timeout = aiConfig != null ? aiConfig.getTimeoutSeconds("judge") : 30;

        final ExecutionContext activeContext = ExecutionContext.getActiveContext();
        final Object targetExecutor = activeContext != null ? activeContext.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR) : null;
        final boolean isSelenideMode = targetExecutor instanceof SelenideTargetExecutor || targetExecutor == null;

        String compiledSystemPrompt = systemPrompt;
        if (isSelenideMode)
        {
            compiledSystemPrompt = compiledSystemPrompt + ActionExtractionPrompt.SELENIDE_LOCATOR_RULE;
        }

        return new LlmRequest(compiledSystemPrompt, userMsg, java.util.Collections.emptyList(), null, temp, timeout);
    }

    @Override
    public QualityJudgeResult parseResponse(final String rawContent, final ExecutionContext context) throws Exception
    {
        return parseResponse(rawContent);
    }

    /**
     * Parses the Quality Judge LLM JSON response.
     *
     * @param rawResponse raw JSON string from LLM provider
     * @return QualityJudgeResult containing judgment, chosen locator, isRegex, and reasoning
     */
    public QualityJudgeResult parseResponse(final String rawResponse)
    {
        if (rawResponse == null || rawResponse.isBlank())
        {
            return new QualityJudgeResult("APPROVED", "", null, false, 0.5, "Empty response from judge");
        }

        try
        {
            String cleanedJson = rawResponse.trim();
            if (cleanedJson.startsWith("```json"))
            {
                cleanedJson = cleanedJson.substring(7);
            }
            if (cleanedJson.startsWith("```"))
            {
                cleanedJson = cleanedJson.substring(3);
            }
            if (cleanedJson.endsWith("```"))
            {
                cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
            }
            cleanedJson = cleanedJson.trim();

            return MAPPER.readValue(cleanedJson, QualityJudgeResult.class);
        }
        catch (final Exception e)
        {
            LOGGER.warn("Failed to parse Quality Judge LLM response: {}. Raw: {}", e.getMessage(), rawResponse);
            return new QualityJudgeResult("APPROVED", "", null, false, 0.5, "Parse error, defaulting to primary action");
        }
    }

    /**
     * DTO for deserializing Quality Judge evaluation result.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QualityJudgeResult
    {
        private final String judgment;
        private final String chosenLocator;
        private final String chosenValue;
        private final boolean isRegex;
        private final double confidence;
        private final String reasoning;

        @JsonCreator
        public QualityJudgeResult(
                @JsonProperty("judgment") final String judgment,
                @JsonProperty("chosenLocator") final String chosenLocator,
                @JsonProperty("chosenValue") final String chosenValue,
                @JsonProperty("isRegex") @JsonAlias({"isRegex", "regex"}) final Boolean isRegex,
                @JsonProperty("confidence") final Double confidence,
                @JsonProperty("reasoning") final String reasoning)
        {
            this.judgment = judgment != null ? judgment.trim().toUpperCase() : "APPROVED";
            this.chosenLocator = chosenLocator != null ? chosenLocator.trim() : "";
            this.chosenValue = chosenValue != null ? chosenValue.trim() : null;
            this.isRegex = isRegex != null ? isRegex : false;
            this.confidence = confidence != null ? confidence : 0.9;
            this.reasoning = reasoning != null ? reasoning.trim() : "";
        }

        public String getJudgment()
        {
            return judgment;
        }

        public String getChosenLocator()
        {
            return chosenLocator;
        }

        public String getChosenValue()
        {
            return chosenValue;
        }

        public boolean isRegex()
        {
            return isRegex;
        }

        public double getConfidence()
        {
            return confidence;
        }

        public String getReasoning()
        {
            return reasoning;
        }
    }
}
