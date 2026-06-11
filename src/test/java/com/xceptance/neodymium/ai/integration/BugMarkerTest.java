/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
// AI-generated: Gemini 3.5 Flash
package com.xceptance.neodymium.ai.integration;

import com.xceptance.neodymium.ai.AiTestVerification;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.ai.core.AiAgent.DefinitiveAssertionError;
import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.ai.testing.LlmAssert;

import org.junit.jupiter.api.BeforeEach;
import java.util.List;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Test that we fail as expected and don't continue. Verifies expected failures (bug tags)
 * for both structural text checks and visual checks.
 */
@Browser("Chrome_1500x1000")
@Tag("featuretest")
public class BugMarkerTest extends BaseAiTest
{
    private String url;

    /**
     * Set up storefront url parameter before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "false");

        url = String.format("http://localhost:%d/AuraGlanceTest/shop-posters-homepage/index.html", server.getPort());
        Neodymium.getData().put("posters.storefront.url", url);
    }

    /**
     * Verifies that expected bugs (failures) in text checks are caught and ignored,
     * allowing the execution to complete without failure.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    public final void failButMarkedAsBug_StaySilent() throws Throwable
    {
        final String steps = """
                # Homepage
                Open ${posters.storefront.url}
                # Verify something that is not true aka a defect and we know that
                # Sure this is not a true bug, just made up for this test.
                Verify that the minicart shows two items (bug: APP-17171).
                # This is not executed!!!
                Verify that the top header shows a warning about a demo application.
                """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(2)
            .hasEscalations(1)
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(1);

        // have we taken the bug number from the step?
        assertEquals("Expected failure abort for bug: APP-17171", r1.getSteps().get(1).getFailureReason());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(1);

        assertEquals("Expected failure abort for bug: APP-17171", r2.getSteps().get(1).getFailureReason());
    }

    /**
     * Compares bug marker execution with and without PESAP enabled, asserting equivalent results.
     */
    @NeodymiumTest
    public final void failButMarkedAsBug_StaySilent_PesapComparison() throws Throwable
    {
        final String steps = """
                # Homepage
                Open ${posters.storefront.url}
                # Verify something that is not true aka a defect and we know that
                # Sure this is not a true bug, just made up for this test.
                Verify that the minicart shows two items (bug: APP-17171).
                # This is not executed!!!
                Verify that the top header shows a warning about a demo application.
                """;

        final AiExecutionResult rWithPesap = runAi(steps, VerificationMode.LIVE_LLM, true);
        this.resetBrowser();
        final AiExecutionResult rWithoutPesap = runAi(steps, VerificationMode.LIVE_LLM, false);

        assertEquals(rWithPesap.getActions().size(), rWithoutPesap.getActions().size());
        assertEquals(rWithPesap.getActions().get(0).getType(), rWithoutPesap.getActions().get(0).getType());
        assertEquals(rWithPesap.getActions().get(0).getTarget(), rWithoutPesap.getActions().get(0).getTarget());
        assertEquals(rWithPesap.getSteps().get(1).getFailureReason(), rWithoutPesap.getSteps().get(1).getFailureReason());
    }

    /**
     * Verifies that when a bug is not marked, execution fails with a DefinitiveAssertionError.
     * Uses LlmAssert to check the error message since it comes from the LLM.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    public final void assertionFailBug() throws Throwable
    {
        final String steps = """
                # Homepage
                Open ${posters.storefront.url}
                # Verify something that is not true aka a defect
                Verify that the minicart shows two items.
                """;

        // Live LLM run
        final DefinitiveAssertionError e1 = assertThrows(DefinitiveAssertionError.class, () ->
        {
            runAi(steps, VerificationMode.LIVE_LLM);
        });
        LlmAssert.assertViaLlmSemanticMatch(e1.getMessage(),
            "Verification failed: The minicart currently shows '0 items', but the instruction requires verifying 'two items'.");

        // Replay run (also expected to fail with the assertion error)
        this.resetBrowser();
        final DefinitiveAssertionError e2 = assertThrows(DefinitiveAssertionError.class, () ->
        {
            runAi(steps, VerificationMode.REPLAY);
        });
        LlmAssert.assertViaLlmSemanticMatch(e2.getMessage(),
            "Verification failed: The minicart currently shows '0 items', but the instruction requires verifying 'two items'.");
    }

    /**
     * Verifies that if an expected bug is gone (the check succeeds), an AssertionError
     * is raised to flag that the bug has been resolved.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    @AiTestVerification({
        VerificationMode.LIVE_LLM,
        VerificationMode.OFFLINE_REPLAY
    })
    public final void assertionFailBug_BugGone() throws Throwable
    {
        assertThrows(AssertionError.class, () ->
        {
            assertAiExecution(() ->
            {
                try
                {
                    Neodymium.ai()
                            .steps("""
                                    # Homepage
                                    Open ${posters.storefront.url}
                                    # Verify something that is not true aka a defect and we know that
                                    # Sure this is not a true bug, just made up for this test.
                                    Verify that the minicart shows 0 items (bug).
                                    # This is not executed because we fail aboce because the defect is gone
                                    Verify that the top header shows a warning about a demo application.
                                    """)
                            .execute();
                }
                catch (final Throwable t)
                {
                    if (t instanceof RuntimeException)
                    {
                        throw (RuntimeException) t;
                    }
                    if (t instanceof Error)
                    {
                        throw (Error) t;
                    }
                    throw new RuntimeException(t);
                }
            });
        });
    }

    /**
     * Verifies that expected bugs (failures) in visual checks are caught and ignored,
     * allowing the execution to complete without failure.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    public final void assertionFailVisualBug() throws Throwable
    {
        final String steps = """
                # Homepage
                Open ${posters.storefront.url}
                # Verify something that is not true aka a defect and we know that
                # Sure this is not a true bug, just made up for this test.
                Verify that the screen is mostly black and white (bug) (visual).
                # This is not executed!!!
                Verify that the top header shows a warning about a demo application.
                """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        final Action open = r1.getActions().get(0);
        assertEquals("NAVIGATE", open.getType());
        assertEquals(url, open.getValue());

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(1);
            // Step 0: Open storefront URL
            // Step 1: Visual verification (bug)
            // Step 2: Unexecuted step

        // Explicit step details verifications for LIVE_LLM
        final var stepDetails0 = r1.getSteps().get(0);
        assertTrue(stepDetails0.isDirectParse());
        assertFalse(stepDetails0.isReplayed());
        assertTrue(stepDetails0.getLlmCalls().isEmpty());

        final var stepDetails1 = r1.getSteps().get(1);
        assertFalse(stepDetails1.isDirectParse());
        assertFalse(stepDetails1.isReplayed());
        
        final var llmCall1 = stepDetails1.getLlmCalls().get(0);
        assertEquals(ContextLevel.VISUAL_LEAN, llmCall1.getContextLevel());
        assertNotNull(llmCall1.getBase64Screenshot());
        assertFalse(llmCall1.getBase64Screenshot().isEmpty());

        final var stepDetails2 = r1.getSteps().get(2);
        assertFalse(stepDetails2.isDirectParse());
        assertFalse(stepDetails2.isReplayed());
        assertTrue(stepDetails2.getLlmCalls().isEmpty());

        // back to start
        this.resetBrowser();

        // replay
        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        final Action replayOpen = r2.getActions().get(0);
        assertEquals("NAVIGATE", replayOpen.getType());
        assertEquals(url, replayOpen.getValue());

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(1);
            // Step 0: Open storefront URL
            // Step 1: Visual verification (bug)
            // Step 2: Unexecuted step

        // Explicit step details verifications for REPLAY
        final var replayStep0 = r2.getSteps().get(0);
        assertFalse(replayStep0.isDirectParse());
        assertTrue(replayStep0.isReplayed());
        assertTrue(replayStep0.getLlmCalls().isEmpty());

        final var replayStep1 = r2.getSteps().get(1);
        assertFalse(replayStep1.isDirectParse());
        assertTrue(replayStep1.isReplayed());
        assertTrue(replayStep1.getLlmCalls().isEmpty());

        final var replayStep2 = r2.getSteps().get(2);
        assertFalse(replayStep2.isDirectParse());
        assertFalse(replayStep2.isReplayed());
        assertTrue(replayStep2.getLlmCalls().isEmpty());
    }

    /**
     * Verifies that if an expected visual bug is gone (the check succeeds), an AssertionError
     * is raised to flag that the bug has been resolved.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    public final void assertionFailVisualBug_BugGone() throws Throwable
    {
        assertThrows(AssertionError.class, () ->
        {
            assertAiExecution(() ->
            {
                try
                {
                    Neodymium.ai()
                            .steps("""
                                    # Homepage
                                    Open ${posters.storefront.url}
                                    # Verify something that is not true aka a defect and we know that
                                    # Sure this is not a true bug, just made up for this test.
                                    Verify that the screen is mostly blue and white (bug) (visual).
                                    # This is not executed!!!
                                    Verify that the top header shows a warning about a demo application.
                                    """)
                            .execute();
                }
                catch (final Throwable t)
                {
                    if (t instanceof RuntimeException)
                    {
                        throw (RuntimeException) t;
                    }
                    if (t instanceof Error)
                    {
                        throw (Error) t;
                    }
                    throw new RuntimeException(t);
                }
            });
        });
    }
}
