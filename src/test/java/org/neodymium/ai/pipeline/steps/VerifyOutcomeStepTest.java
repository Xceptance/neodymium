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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockSutState;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.util.ScreenshotHasher;

/**
 * Unit test suite for {@link VerifyOutcomeStep}.
 * Tests semantic verification handling, missing session/executor checks, replay mode bypasses,
 * and JSON verification response parsing logic.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class VerifyOutcomeStepTest
{
    private ExecutionContext context;
    private MockTargetExecutor targetExecutor;
    private MockLlmProvider mockLlmProvider;
    private AiSession session;

    @BeforeEach
    public void setUp()
    {
        final SessionData sessionData = new SessionData(new HashMap<>());
        targetExecutor = new MockTargetExecutor();
        mockLlmProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockLlmProvider);
        registry.registerProvider(mockLlmProvider);
        final ExecutionEventBus eventBus = new ExecutionEventBus();

        session = AiSession.mock(sessionData, registry, eventBus, targetExecutor);
        context = session.getExecutionContext();

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, targetExecutor);
        context.getTransientData().put("semanticVerification.enabled", true);
    }

    /**
     * Goal: Verifies that {@link VerifyOutcomeStep#execute} throws a {@link ConclusiveFailureException}
     * when no active {@link AiSession} is present in the context transient map.
     */
    @Test
    public void testMissingSessionThrowsException()
    {
        final ExecutionContext emptyContext = new ExecutionContext(new SessionData(new HashMap<>()));
        emptyContext.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, targetExecutor);
        emptyContext.getTransientData().put("semanticVerification.enabled", true);
        final VerifyOutcomeStep step = new VerifyOutcomeStep();

        // Expect exception when session key is missing
        Assertions.assertThrows(ConclusiveFailureException.class, () ->
        {
            step.execute(emptyContext);
        });
    }

    /**
     * Goal: Verifies that {@link VerifyOutcomeStep#execute} throws a {@link ConclusiveFailureException}
     * when no active {@link MockTargetExecutor} is present in the context transient map.
     */
    @Test
    public void testMissingExecutorThrowsException()
    {
        final ExecutionContext emptyContext = new ExecutionContext(new SessionData(new HashMap<>()));
        emptyContext.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        emptyContext.getTransientData().put("semanticVerification.enabled", true);
        final VerifyOutcomeStep step = new VerifyOutcomeStep();

        // Expect exception when executor key is missing
        Assertions.assertThrows(ConclusiveFailureException.class, () ->
        {
            step.execute(emptyContext);
        });
    }

    /**
     * Goal: Verifies that semantic outcome verification is skipped when running in strict replay mode.
     */
    @Test
    public void testVerificationSkippedInReplayMode() throws Exception
    {
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);
        final PlaybookStep playbookStep = new PlaybookStep("Click sign in");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, playbookStep);

        final VerifyOutcomeStep step = new VerifyOutcomeStep();

        // Expect execution in REPLAY_STRICT mode to complete without error or LLM calls
        Assertions.assertDoesNotThrow(() ->
        {
            step.execute(context);
        });
    }

    /**
     * Goal: Verifies that a valid JSON multi-rubric verification response passing all criteria
     * completes cleanly without recording any verification warnings.
     */
    @Test
    public void testSuccessfulOutcomeVerification() throws Exception
    {
        System.setProperty("neodymium.ai.semanticVerificationEnabled", "true");
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_ONLY);
        final PlaybookStep playbookStep = new PlaybookStep("Click sign in");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, playbookStep);

        final String jsonResponse = """
            {
              "rubrics": {
                "intentMatch": { "analysis": "Clicked sign in button", "score": "PASS" },
                "visualDelta": { "analysis": "Login form displayed", "score": "PASS" },
                "absenceOfErrors": { "analysis": "No visual errors", "score": "PASS" }
              },
              "overallVerdict": { "passed": true, "summary": "Step execution verified" }
            }
            """;
        mockLlmProvider.addResponse(new LlmResponse(jsonResponse, new TokenUsage(10, 10, 20), "mock-model"));

        final VerifyOutcomeStep step = new VerifyOutcomeStep();
        step.execute(context);

        @SuppressWarnings("unchecked")
        final List<String> warnings = (List<String>) context.getTransientData().get("verificationWarnings");
        // Assert no verification warnings recorded for passing outcome
        Assertions.assertTrue(warnings == null || warnings.isEmpty());
    }

    /**
     * Goal: Verifies that a failing verification rubric response records a structured warning string
     * in the context's `verificationWarnings` transient list.
     */
    @Test
    public void testFailedOutcomeVerificationRecordsWarning() throws Exception
    {
        System.setProperty("neodymium.ai.semanticVerificationEnabled", "true");
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_ONLY);
        final PlaybookStep playbookStep = new PlaybookStep("Click sign in");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, playbookStep);

        final String jsonResponse = """
            {
              "rubrics": {
                "intentMatch": { "analysis": "Sign in button was disabled", "score": "FAIL" },
                "visualDelta": { "analysis": "Page state did not change", "score": "FAIL" },
                "absenceOfErrors": { "analysis": "Error prompt visible", "score": "FAIL" }
              },
              "overallVerdict": { "passed": false, "summary": "Sign in button was disabled" }
            }
            """;
        mockLlmProvider.addResponse(new LlmResponse(jsonResponse, new TokenUsage(10, 10, 20), "mock-model"));

        final VerifyOutcomeStep step = new VerifyOutcomeStep();
        step.execute(context);

        @SuppressWarnings("unchecked")
        final List<Object> warnings = (List<Object>) context.getTransientData().get("verificationWarnings");
        // Assert exactly 1 warning recorded with specific failing reason
        Assertions.assertNotNull(warnings);
        Assertions.assertEquals(1, warnings.size());
        Assertions.assertTrue(warnings.get(0).toString().contains("Sign in button was disabled"));
    }

    /**
     * Goal: Verifies that secret variable values are masked before LlmRequest is dispatched to provider during outcome verification.
     */
    @Test
    public void testVerifyOutcomeStepSecretMaskingInProvider() throws Exception
    {
        System.setProperty("neodymium.ai.semanticVerificationEnabled", "true");
        context.getSessionData().putDynamic("api_key", "SecretKey999!", true);
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_ONLY);

        final PlaybookStep playbookStep = new PlaybookStep("Verify key SecretKey999! is active");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, playbookStep);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Verify key SecretKey999! is active");

        final String jsonResponse = """
            {
              "rubrics": {
                "intentMatch": { "analysis": "Key verified", "score": "PASS" },
                "visualDelta": { "analysis": "UI match", "score": "PASS" },
                "absenceOfErrors": { "analysis": "No errors", "score": "PASS" }
              },
              "overallVerdict": { "passed": true, "summary": "Key verified" }
            }
            """;
        mockLlmProvider.addResponse(new LlmResponse(jsonResponse, new TokenUsage(10, 10, 20), "mock-model"));

        final VerifyOutcomeStep step = new VerifyOutcomeStep();
        try
        {
            ExecutionContext.setActiveContext(context);
            step.execute(context);
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
        }

        Assertions.assertNotNull(mockLlmProvider.getLastRequest(), "LlmRequest should be recorded by provider.");
        final String userMessage = mockLlmProvider.getLastRequest().userMessage();
        Assertions.assertTrue(userMessage.contains("[MASKED_VAR_api_key]"), "Outbound verification request user prompt should contain masked variable placeholder.");
        Assertions.assertFalse(userMessage.contains("SecretKey999!"), "Outbound verification request user prompt should NOT contain raw secret key.");
    }

    /**
     * Goal: Verifies that pure visual verification steps with empty actions do NOT consume
     * stale `KEY_POST_ACTION_STATE` from preceding action steps, and instead record their
     * visual baseline hash from the active `KEY_LAST_STATE` viewport capture.
     */
    @Test
    public void testVisualStepDoesNotConsumeStalePostActionState() throws Exception
    {
        final BufferedImage staleImg = new BufferedImage(1500, 2117, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g1 = staleImg.createGraphics();
        g1.setColor(Color.RED);
        g1.fillRect(0, 0, 1500, 2117);
        g1.dispose();

        final BufferedImage viewportImg = new BufferedImage(1500, 857, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2 = viewportImg.createGraphics();
        g2.setColor(Color.GREEN);
        g2.fillRect(0, 0, 1500, 857);
        g2.dispose();

        final String staleBase64;
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            ImageIO.write(staleImg, "png", baos);
            staleBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        }

        final String viewportBase64;
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            ImageIO.write(viewportImg, "png", baos);
            viewportBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        }

        final MockSutState staleState = new MockSutState(
            "<html>stale</html>",
            List.of(new SutAttachment("image/png", "stale.png", staleBase64)),
            "stale_hash");

        final MockSutState viewportState = new MockSutState(
            "<html>viewport</html>",
            List.of(new SutAttachment("image/png", "viewport.png", viewportBase64)),
            "viewport_hash");

        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_ONLY);
        context.getTransientData().put("KEY_POST_ACTION_STATE", staleState);
        context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, viewportState);

        final PlaybookStep pureVisualStep = new PlaybookStep("Verify page header (visual)");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, pureVisualStep);
        context.getTransientData().put("semanticVerification.enabled", false);

        final VerifyOutcomeStep step = new VerifyOutcomeStep();
        step.execute(context);

        final String expectedViewportHash = ScreenshotHasher.computeSsimMatrix(viewportBase64);
        final String expectedStaleHash = ScreenshotHasher.computeSsimMatrix(staleBase64);

        Assertions.assertNotNull(pureVisualStep.getScreenshotHash(), "Screenshot hash should be recorded.");
        Assertions.assertEquals(expectedViewportHash, pureVisualStep.getScreenshotHash(), "Baseline hash MUST match active viewport state, NOT stale full-page action state.");
        Assertions.assertNotEquals(expectedStaleHash, pureVisualStep.getScreenshotHash(), "Baseline hash must NOT match stale action state.");
        Assertions.assertNull(context.getTransientData().get("KEY_POST_ACTION_STATE"), "KEY_POST_ACTION_STATE must be removed.");
    }
}
