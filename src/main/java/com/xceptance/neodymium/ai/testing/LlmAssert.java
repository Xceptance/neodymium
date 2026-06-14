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
package com.xceptance.neodymium.ai.testing;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xceptance.neodymium.ai.core.AiStats;
import com.xceptance.neodymium.ai.core.LlmClient;
import com.xceptance.neodymium.ai.core.LlmMode;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Specialized test helper class providing LLM-assisted semantic assertions.
 * Allows verifying unstructured text (like reasoning, explanations, or outputs)
 * against natural language criteria or checking semantic equivalence between two texts.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmAssert
{
    private static final Logger LOG = LoggerFactory.getLogger(LlmAssert.class);

    private LlmAssert()
    {
        // utility class
    }

    /**
     * Asserts that the actual text output satisfies the provided evaluation criteria.
     * Uses a default {@link LlmClient} configured with {@link LlmMode#ASSERT}.
     *
     * @param actualText the text returned from the LLM or agent to verify
     * @param criteria   the natural language criteria that the text must satisfy
     * @throws AssertionError if the text does not satisfy the criteria or evaluation fails
     */
    public static void assertViaLlmSemanticMatch(final String actualText, final String criteria)
    {
        final LlmClient defaultClient = new LlmClient(Neodymium.aiConfiguration(), new AiStats(), LlmMode.ASSERT);
        assertViaLlmSemanticMatch(defaultClient, actualText, criteria);
    }

    /**
     * Asserts that the actual text output satisfies the provided evaluation criteria
     * using the specified {@link LlmClient}.
     *
     * @param client     the LlmClient instance to use for the evaluation
     * @param actualText the text returned from the LLM or agent to verify
     * @param criteria   the natural language criteria that the text must satisfy
     * @throws AssertionError if the text does not satisfy the criteria or evaluation fails
     */
    public static void assertViaLlmSemanticMatch(final LlmClient client, final String actualText, final String criteria)
    {
        if (actualText == null)
        {
            throw new IllegalArgumentException("Actual text to evaluate cannot be null");
        }
        if (criteria == null)
        {
            throw new IllegalArgumentException("Evaluation criteria cannot be null");
        }

        LOG.debug("   💬 Evaluating semantic match against criteria: \"{}\" on actual text: \"{}\"", criteria, actualText);

        final String systemPrompt = """
            You are a strict, objective Semantic Evaluation Agent.
            Your task is to verify if the actual text output satisfies the provided evaluation criteria.
            The criteria describes what the text should contain, explain, or convey.
            
            You must return a JSON object with the following fields:
            - "passed": boolean (true if the text satisfies the criteria, false otherwise)
            - "reasoning": string (a concise explanation of why the text matches or fails to match the criteria)
            
            CRITICAL: Only output the JSON object. Do not include any formatting, markdown wrappers (like ```json), or extra text.
            """;

        final String userPrompt = String.format(
                "Text to evaluate:\n\"\"\"\n%s\n\"\"\"\n\nEvaluation Criteria:\n\"\"\"\n%s\n\"\"\"",
                actualText, criteria);

        final EvaluationResult result = evaluate(client, userPrompt, systemPrompt);
        if (!result.isPassed())
        {
            Assertions.fail(String.format(
                    "Semantic LLM match assertion failed.\nCriteria: \"%s\"\nActual text: \"%s\"\nReasoning: %s",
                    criteria, actualText, result.getReasoning()));
        }
    }

    /**
     * Asserts that the actual text output is semantically equivalent to the expected reference text.
     * They do not need to match word-for-word, but they must convey the same core meaning.
     * Uses a default {@link LlmClient} configured with {@link LlmMode#ASSERT}.
     *
     * @param actualText   the actual text to verify
     * @param expectedText the expected reference text
     * @throws AssertionError if the texts are not semantically equivalent or evaluation fails
     */
    public static void assertViaLlmSemanticEquivalence(final String actualText, final String expectedText)
    {
        final LlmClient defaultClient = new LlmClient(Neodymium.aiConfiguration(), new AiStats(), LlmMode.ASSERT);
        assertViaLlmSemanticEquivalence(defaultClient, actualText, expectedText);
    }

    /**
     * Asserts that the actual text output is semantically equivalent to the expected reference text
     * using the specified {@link LlmClient}.
     *
     * @param client       the LlmClient instance to use for the evaluation
     * @param actualText   the actual text to verify
     * @param expectedText the expected reference text
     * @throws AssertionError if the texts are not semantically equivalent or evaluation fails
     */
    public static void assertViaLlmSemanticEquivalence(final LlmClient client, final String actualText, final String expectedText)
    {
        if (actualText == null)
        {
            throw new IllegalArgumentException("Actual text to verify cannot be null");
        }
        if (expectedText == null)
        {
            throw new IllegalArgumentException("Expected reference text cannot be null");
        }

        LOG.debug("   💬 Evaluating semantic equivalence between expected: \"{}\" and actual: \"{}\"", expectedText, actualText);

        final String systemPrompt = """
            You are a strict, objective Semantic Equivalence Agent.
            Your task is to verify if the actual text is semantically equivalent to the expected reference text.
            They do not need to match word-for-word, but they must convey the exact same core meaning, facts, and intent. If the actual text contradicts the expected text, contains different facts, or lacks key information, the verification must fail.
            
            You must return a JSON object with the following fields:
            - "passed": boolean (true if the texts are semantically equivalent, false otherwise)
            - "reasoning": string (a concise explanation of why the texts are equivalent or why they differ)
            
            CRITICAL: Only output the JSON object. Do not include any formatting, markdown wrappers (like ```json), or extra text.
            """;

        final String userPrompt = String.format(
                "Expected Reference Text:\n\"\"\"\n%s\n\"\"\"\n\nActual Text to Verify:\n\"\"\"\n%s\n\"\"\"",
                expectedText, actualText);

        final EvaluationResult result = evaluate(client, userPrompt, systemPrompt);
        if (!result.isPassed())
        {
            Assertions.fail(String.format(
                    "Semantic LLM equivalence assertion failed.\nExpected: \"%s\"\nActual: \"%s\"\nReasoning: %s",
                    expectedText, actualText, result.getReasoning()));
        }
    }

    /**
     * Performs a generic LLM evaluation using custom user and system prompts.
     * Uses a default {@link LlmClient} configured with {@link LlmMode#ASSERT}.
     *
     * @param userPrompt   the prompt containing inputs or evaluation context
     * @param systemPrompt instructions for the LLM evaluation behavior
     * @throws AssertionError if the evaluation returns a failed verdict or parsing fails
     */
    public static void assertViaLlmEvaluation(final String userPrompt, final String systemPrompt)
    {
        final LlmClient defaultClient = new LlmClient(Neodymium.aiConfiguration(), new AiStats(), LlmMode.ASSERT);
        assertViaLlmEvaluation(defaultClient, userPrompt, systemPrompt);
    }

    /**
     * Performs a generic LLM evaluation using custom user and system prompts, utilizing the specified {@link LlmClient}.
     * Automatically appends strict JSON formatting rules to the system prompt to ensure parsing success.
     *
     * @param client       the LlmClient instance to use for the evaluation
     * @param userPrompt   the prompt containing inputs or evaluation context
     * @param systemPrompt instructions for the LLM evaluation behavior
     * @throws AssertionError if the evaluation returns a failed verdict or parsing fails
     */
    public static void assertViaLlmEvaluation(final LlmClient client, final String userPrompt, final String systemPrompt)
    {
        LOG.debug("   💬 Evaluating custom semantic assertion...");

        final EvaluationResult result = evaluate(client, userPrompt, systemPrompt);
        if (!result.isPassed())
        {
            Assertions.fail(String.format(
                    "Semantic LLM assertion failed.\nReasoning: %s",
                    result.getReasoning()));
        }
    }

    /**
     * Internal helper executing the LLM call and parsing the structured JSON response.
     * Appends formatting instructions to the system prompt.
     *
     * @param client       the LlmClient instance to use
     * @param userPrompt   the user instruction or context
     * @param systemPrompt instructions for the LLM
     * @return the parsed evaluation result
     */
    private static EvaluationResult evaluate(final LlmClient client, final String userPrompt, final String systemPrompt)
    {
        if (client == null)
        {
            throw new IllegalArgumentException("LlmClient cannot be null");
        }
        if (userPrompt == null)
        {
            throw new IllegalArgumentException("User prompt cannot be null");
        }
        if (systemPrompt == null)
        {
            throw new IllegalArgumentException("System prompt cannot be null");
        }

        final String fullSystemPrompt = systemPrompt + "\n\n" + """
            CRITICAL: You must return your final verdict as a JSON object with the following fields:
            - "passed": boolean (true if the criteria/rules are satisfied, false otherwise)
            - "reasoning": string (a concise explanation of your decision)
            
            Do not include any formatting, markdown wrappers (like ```json), or extra text outside the JSON object.
            """;

        final String response = client.chat(LlmMode.ASSERT, fullSystemPrompt, userPrompt);
        if (response == null || response.trim().isEmpty())
        {
            Assertions.fail("Received null or empty response from semantic evaluation LLM.");
        }

        try
        {
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode root = mapper.readTree(response);
            final boolean passed = root.path("passed").asBoolean();
            final String reasoning = root.path("reasoning").asText();

            LOG.debug("   💬 Evaluation result: passed={}, reasoning=\"{}\"", passed, reasoning);

            return new EvaluationResult(passed, reasoning);
        }
        catch (final Exception e)
        {
            throw new AssertionError("Failed to parse semantic assertion response from LLM: " + response, e);
        }
    }

    /**
     * Model representing the structured result returned by the semantic evaluation LLM.
     */
    public static final class EvaluationResult
    {
        private final boolean passed;
        private final String reasoning;

        /**
         * Constructs a new EvaluationResult with the specified verdict and reasoning.
         *
         * @param passed    the pass/fail verdict
         * @param reasoning the explanation for the verdict
         */
        public EvaluationResult(final boolean passed, final String reasoning)
        {
            this.passed = passed;
            this.reasoning = reasoning;
        }

        /**
         * Returns whether the assertion passed.
         *
         * @return true if passed, false otherwise
         */
        public boolean isPassed()
        {
            return this.passed;
        }

        /**
         * Returns the explanation/reasoning of the decision.
         *
         * @return the reasoning string
         */
        public String getReasoning()
        {
            return this.reasoning;
        }
    }
}
