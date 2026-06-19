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
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link LlmAssert} helper utility class.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmAssertTest
{
    /**
     * Verifies that assertViaLlmSemanticMatch passes when the LLM returns a passed JSON response.
     */
    @Test
    public void testSemanticMatchSuccess()
    {
        final MockLlmClient client = new MockLlmClient();
        client.addResponse(AiMockResponse.builder()
                .responseText("{\"passed\": true, \"reasoning\": \"The text successfully explains the error\"}")
                .build());

        LlmAssert.assertViaLlmSemanticMatch(client, "Error: Invalid password", "should explain why login failed");
    }

    /**
     * Verifies that assertViaLlmSemanticMatch throws an AssertionError when the LLM returns a failed JSON response.
     */
    @Test
    public void testSemanticMatchFailure()
    {
        final MockLlmClient client = new MockLlmClient();
        client.addResponse(AiMockResponse.builder()
                .responseText("{\"passed\": false, \"reasoning\": \"No explanation of payment failure present\"}")
                .build());

        final AssertionError exception = Assertions.assertThrows(AssertionError.class, () ->
        {
            LlmAssert.assertViaLlmSemanticMatch(client, "Success: Action completed", "should explain that payment failed");
        });

        Assertions.assertTrue(exception.getMessage().contains("No explanation of payment failure present"));
        Assertions.assertTrue(exception.getMessage().contains("Success: Action completed"));
        Assertions.assertTrue(exception.getMessage().contains("should explain that payment failed"));
    }

    /**
     * Verifies that assertViaLlmSemanticEquivalence passes when two texts are semantically equivalent.
     */
    @Test
    public void testSemanticEquivalenceSuccess()
    {
        final MockLlmClient client = new MockLlmClient();
        client.addResponse(AiMockResponse.builder()
                .responseText("{\"passed\": true, \"reasoning\": \"Both texts describe a login failure due to bad credentials\"}")
                .build());

        LlmAssert.assertViaLlmSemanticEquivalence(client, "The login failed because of wrong credentials", "Wrong credentials entered, authentication failed");
    }

    /**
     * Verifies that assertViaLlmSemanticEquivalence throws an AssertionError when texts are not semantically equivalent.
     */
    @Test
    public void testSemanticEquivalenceFailure()
    {
        final MockLlmClient client = new MockLlmClient();
        client.addResponse(AiMockResponse.builder()
                .responseText("{\"passed\": false, \"reasoning\": \"The actual text describes a success while expected is a failure\"}")
                .build());

        final AssertionError exception = Assertions.assertThrows(AssertionError.class, () ->
        {
            LlmAssert.assertViaLlmSemanticEquivalence(client, "Login successful", "Login failed due to credentials error");
        });

        Assertions.assertTrue(exception.getMessage().contains("The actual text describes a success"));
        Assertions.assertTrue(exception.getMessage().contains("Login successful"));
        Assertions.assertTrue(exception.getMessage().contains("Login failed due to credentials error"));
    }

    /**
     * Verifies that assertViaLlmEvaluation passes when custom prompts succeed.
     */
    @Test
    public void testEvaluationSuccess()
    {
        final MockLlmClient client = new MockLlmClient();
        client.addResponse(AiMockResponse.builder()
                .responseText("{\"passed\": true, \"reasoning\": \"Constraint satisfied\"}")
                .build());

        LlmAssert.assertViaLlmEvaluation(client, "Input text", "System instruction");
    }

    /**
     * Verifies that assertViaLlmEvaluation throws AssertionError on constraint failure.
     */
    @Test
    public void testEvaluationFailure()
    {
        final MockLlmClient client = new MockLlmClient();
        client.addResponse(AiMockResponse.builder()
                .responseText("{\"passed\": false, \"reasoning\": \"Constraint violated\"}")
                .build());

        final AssertionError exception = Assertions.assertThrows(AssertionError.class, () ->
        {
            LlmAssert.assertViaLlmEvaluation(client, "Input text", "System instruction");
        });

        Assertions.assertTrue(exception.getMessage().contains("Constraint violated"));
    }

    /**
     * Verifies that any assert method throws AssertionError when the LLM returns non-JSON text.
     */
    @Test
    public void testEvaluationMalformedJsonResponse()
    {
        final MockLlmClient client = new MockLlmClient();
        client.addResponse(AiMockResponse.builder()
                .responseText("not a json object")
                .build());

        final AssertionError exception = Assertions.assertThrows(AssertionError.class, () ->
        {
            LlmAssert.assertViaLlmEvaluation(client, "Input text", "System instruction");
        });

        Assertions.assertTrue(exception.getMessage().contains("Failed to parse semantic assertion response"));
    }

    /**
     * Verifies that the assert methods validate null arguments.
     */
    @Test
    public void testNullArgumentChecks()
    {
        final MockLlmClient client = new MockLlmClient();

        Assertions.assertThrows(IllegalArgumentException.class, () ->
        {
            LlmAssert.assertViaLlmSemanticMatch(null, "text", "criteria");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () ->
        {
            LlmAssert.assertViaLlmSemanticMatch(client, null, "criteria");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () ->
        {
            LlmAssert.assertViaLlmSemanticMatch(client, "text", null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () ->
        {
            LlmAssert.assertViaLlmSemanticEquivalence(null, "text", "expected");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () ->
        {
            LlmAssert.assertViaLlmSemanticEquivalence(client, null, "expected");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () ->
        {
            LlmAssert.assertViaLlmSemanticEquivalence(client, "text", null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () ->
        {
            LlmAssert.assertViaLlmEvaluation(null, "user", "system");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () ->
        {
            LlmAssert.assertViaLlmEvaluation(client, null, "system");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () ->
        {
            LlmAssert.assertViaLlmEvaluation(client, "user", null);
        });
    }
}
