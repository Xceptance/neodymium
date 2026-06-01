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
package com.xceptance.neodymium.ai;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import com.xceptance.neodymium.ai.util.EmbeddedHtmlServer;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookStep;
import com.xceptance.neodymium.ai.playbook.PlaybookManager;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.core.AiStats;
import com.xceptance.neodymium.util.Neodymium;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;
import java.util.Objects;


/**
 * Base class for all AI tests. 
 * Automatically manages an embedded HTTP server and determines the test URL based on class/method name.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 */
public abstract class BaseAiTest
{
    protected static EmbeddedHtmlServer server;
    protected String currentTestUrl;

    /**
     * Starts the embedded server before any tests in the class are run.
     * 
     * @throws IOException if server fails to start
     */
    @BeforeAll
    public static void startServer() throws IOException
    {
        Configuration.headless = true;
        // Resolve key dynamically from standard environment variable to avoid hardcoding secrets in git
        final String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty() && System.getProperty("neodymium.ai.apiKey") == null)
        {
            System.setProperty("neodymium.ai.apiKey", envKey.trim());
        }
        server = new EmbeddedHtmlServer();
        server.start();
    }

    /**
     * Stops the embedded server after all tests in the class have finished.
     */
    @AfterAll
    public static void stopServer()
    {
        if (server != null)
        {
            server.stop();
        }
    }

    /**
     * Prepares the URL for the current test case based on class and method name.
     * 
     * @param testInfo the JUnit 5 test info injected automatically
     */
    @BeforeEach
    public void setupPageUrl(final TestInfo testInfo)
    {
        final String className = testInfo.getTestClass().get().getSimpleName();
        final String methodName = testInfo.getTestMethod().get().getName();
        
        currentTestUrl = String.format("http://localhost:%d/%s/%s.html", server.getPort(), className, methodName);
    }

    /**
     * Executes the given steps in two phases:
     * 1. Live phase: skipReplay=true, generating/saving the playbook cache baseline via LLM.
     * 2. Replay phase: skipReplay=false, replaying the steps offline with zero LLM calls.
     * 
     * @param steps the natural language instructions to run
     */
    protected void assertTwoPhaseExecution(final String steps)
    {
        assertTwoPhaseExecution(() ->
        {
            Neodymium.ai().execute(steps);
        });
    }

    /**
     * Executes implicit playbook steps (from the dataset steps field) in two phases:
     * 1. Live phase: skipReplay=true, generating/saving the playbook cache baseline via LLM.
     * 2. Replay phase: skipReplay=false, replaying the steps offline with zero LLM calls.
     */
    protected void assertTwoPhaseExecution()
    {
        assertTwoPhaseExecution(() ->
        {
            try
            {
                Neodymium.ai().execute();
            }
            catch (final Throwable t)
            {
                if (t instanceof RuntimeException)
                {
                    throw (RuntimeException) t;
                }
                throw new RuntimeException(t);
            }
        });
    }

    /**
     * Executes the given runnable steps in two phases:
     * 1. Live phase: skipReplay=true, generating/saving the playbook cache baseline via LLM.
     * 2. Replay phase: skipReplay=false, replaying the steps offline with zero LLM calls.
     * 
     * @param runSteps the runnable steps to execute
     */
    protected void assertTwoPhaseExecution(final Runnable runSteps)
    {
        assertMultiPhaseExecution(runSteps);
    }

    /**
     * Executes the implicit playbook steps (from the dataset steps field) in multiple verification phases to guarantee playbook consistency.
     */
    protected void assertMultiPhaseExecution()
    {
        assertMultiPhaseExecution(() ->
        {
            try
            {
                Neodymium.ai().execute();
            }
            catch (final Throwable t)
            {
                if (t instanceof RuntimeException)
                {
                    throw (RuntimeException) t;
                }
                throw new RuntimeException(t);
            }
        });
    }

    /**
     * Executes the given runnable steps in multiple verification phases to guarantee playbook consistency:
     * Phase 1: Live LLM baseline generation (skipReplay=true, interactive=false), comparing the generated playbook against the original baseline.
     * Phase 2: Offline Replay verification (skipReplay=false, interactive=false), replaying the steps offline with zero LLM calls.
     * Phase 3: Automated HUD Replay verification (skipReplay=false, interactive=true, interactive.autoSkip=true), ensuring the HUD does not interfere with the SUT page.
     * 
     * @param runSteps the runnable steps to execute
     */
    protected void assertMultiPhaseExecution(final Runnable runSteps)
    {
        final boolean fullVerification = Boolean.parseBoolean(System.getProperty("neodymium.ai.test.fullVerification", "true"));
        if (!fullVerification)
        {
            // Run only Phase 2: Offline Replay verification
            Neodymium.setAiPlaybook(null);
            Neodymium.getData().put("skipReplay", "false");
            Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", "false");
            System.setProperty("neodymium.ai.interactive.autoSkip", "false");

            Neodymium.initializePlaybook();
            final Playbook loadedPlaybook = Neodymium.getAiPlaybook();
            if (loadedPlaybook != null)
            {
                loadedPlaybook.setRecording(false);
                loadedPlaybook.setCursor(0);
            }

            runSteps.run();
            return;
        }

        final String playbookId = Neodymium.getTestName();
        
        // Backup the existing playbook file (if it exists)
        final Playbook backupPlaybook = PlaybookManager.loadPlaybook(playbookId);
        
        // Phase 1: Live LLM baseline generation and comparison
        Neodymium.getData().put("skipReplay", "true");
        Neodymium.setAiPlaybook(null);
        
        // Save original interactive settings
        final String origInteractive = String.valueOf(Neodymium.aiConfiguration().aiInteractive());
        final String origAutoSkip = System.getProperty("neodymium.ai.interactive.autoSkip", "false");
        
        Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", "false");
        System.setProperty("neodymium.ai.interactive.autoSkip", "false");
        
        try
        {
            runSteps.run();
        }
        finally
        {
            // Restore interactive properties
            Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", origInteractive);
            System.setProperty("neodymium.ai.interactive.autoSkip", origAutoSkip);
        }

        final Playbook generatedPlaybook = Neodymium.getAiPlaybook();
        if (generatedPlaybook != null && generatedPlaybook.isChanged())
        {
            PlaybookManager.savePlaybook(generatedPlaybook);
        }

        // Compare playbooks (Agreement on Drift: Fail on mismatch)
        if (backupPlaybook != null)
        {
            final Playbook newlySavedPlaybook = PlaybookManager.loadPlaybook(playbookId);
            assertPlaybookEquals(backupPlaybook, newlySavedPlaybook);
        }

        final AiStats stats = Neodymium.ai().getStats();
        final int initialCalls = stats.getOverallCallCount();

        // Phase 2: Offline Replay verification
        final String activeUrl = WebDriverRunner.url();
        if (activeUrl != null && !activeUrl.equals("about:blank"))
        {
            Selenide.open(activeUrl);
        }

        Neodymium.setAiPlaybook(null);
        Neodymium.getData().put("skipReplay", "false");
        Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", "false");
        System.setProperty("neodymium.ai.interactive.autoSkip", "false");

        Neodymium.initializePlaybook();
        final Playbook loadedPlaybook = Neodymium.getAiPlaybook();
        if (loadedPlaybook != null)
        {
            loadedPlaybook.setRecording(false);
            loadedPlaybook.setCursor(0);
        }

        runSteps.run();

        final int finalCalls = stats.getOverallCallCount();
        Assertions.assertEquals(initialCalls, finalCalls,
            "Replay execution made LLM calls! Expected exactly 0 new LLM calls during playback.");

        // Phase 3: Automated HUD Replay verification
        if (activeUrl != null && !activeUrl.equals("about:blank"))
        {
            Selenide.open(activeUrl);
        }

        Neodymium.setAiPlaybook(null);
        Neodymium.getData().put("skipReplay", "false");
        Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", "true");
        System.setProperty("neodymium.ai.interactive.autoSkip", "true");

        Neodymium.initializePlaybook();
        final Playbook hudPlaybook = Neodymium.getAiPlaybook();
        if (hudPlaybook != null)
        {
            hudPlaybook.setRecording(false);
            hudPlaybook.setCursor(0);
        }

        try
        {
            runSteps.run();
        }
        finally
        {
            // Restore clean offline state
            Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", "false");
            System.setProperty("neodymium.ai.interactive.autoSkip", "false");
        }

        final int hudCalls = stats.getOverallCallCount();
        Assertions.assertEquals(initialCalls, hudCalls,
            "HUD Replay execution made LLM calls! Expected exactly 0 new LLM calls during HUD playback.");

        // Phase 4: Live LLM with HUD verification
        if (activeUrl != null && !activeUrl.equals("about:blank"))
        {
            Selenide.open(activeUrl);
        }

        Neodymium.setAiPlaybook(null);
        Neodymium.getData().put("skipReplay", "true");
        Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", "true");
        System.setProperty("neodymium.ai.interactive.autoSkip", "true");

        try
        {
            runSteps.run();
        }
        finally
        {
            // Restore clean offline state
            Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", "false");
            System.setProperty("neodymium.ai.interactive.autoSkip", "false");
        }
    }

    /**
     * Asserts structural step-by-step and action-level equivalence between two playbooks.
     * 
     * @param expected the expected baseline playbook
     * @param actual the actual newly generated playbook
     * @throws AssertionFailedError if a difference is found
     */
    private void assertPlaybookEquals(final Playbook expected, final Playbook actual)
    {
        if (expected == null && actual == null)
        {
            return;
        }
        if (expected == null || actual == null)
        {
            throw new AssertionFailedError("Playbook mismatch: one playbook is null. Expected: " + expected + ", Actual: " + actual);
        }

        final List<PlaybookStep> expectedSteps = expected.getSteps();
        final List<PlaybookStep> actualSteps = actual.getSteps();

        if (expectedSteps.size() != actualSteps.size())
        {
            throw new AssertionFailedError("Playbook step count mismatch! Expected: " + expectedSteps.size() + " steps, but got: " + actualSteps.size());
        }

        for (int i = 0; i < expectedSteps.size(); i++)
        {
            final PlaybookStep expectedStep = expectedSteps.get(i);
            final PlaybookStep actualStep = actualSteps.get(i);

            // Compare step prompts/instructions
            final String expectedPrompt = expectedStep.getPromptLine();
            final String actualPrompt = actualStep.getPromptLine();
            if (!Objects.equals(expectedPrompt, actualPrompt))
            {
                throw new AssertionFailedError("Playbook step " + i + " prompt mismatch! Expected: \"" + expectedPrompt + "\", but got: \"" + actualPrompt + "\"");
            }

            final List<Action> expectedActions = expectedStep.getActions();
            final List<Action> actualActions = actualStep.getActions();

            if (expectedActions.size() != actualActions.size())
            {
                throw new AssertionFailedError("Playbook step " + i + " action count mismatch! Expected: " + expectedActions.size() + " actions, but got: " + actualActions.size());
            }

            for (int j = 0; j < expectedActions.size(); j++)
            {
                final Action expectedAction = expectedActions.get(j);
                final Action actualAction = actualActions.get(j);

                // Compare action type
                if (!Objects.equals(expectedAction.getType(), actualAction.getType()))
                {
                    throw new AssertionFailedError("Playbook step " + i + " Action " + j + " type mismatch! Expected: \"" + expectedAction.getType() + "\", but got: \"" + actualAction.getType() + "\"");
                }

                // Compare target / selector
                if (!Objects.equals(expectedAction.getTarget(), actualAction.getTarget()))
                {
                    throw new AssertionFailedError("Playbook step " + i + " Action " + j + " target selector mismatch! Expected: \"" + expectedAction.getTarget() + "\", but got: \"" + actualAction.getTarget() + "\"");
                }

                // Compare values
                if (!Objects.equals(expectedAction.getValue(), actualAction.getValue()))
                {
                    throw new AssertionFailedError("Playbook step " + i + " Action " + j + " value mismatch! Expected: " + expectedAction.getValue() + ", but got: " + actualAction.getValue());
                }

                // Compare frameId
                if (!Objects.equals(expectedAction.getFrameId(), actualAction.getFrameId()))
                {
                    throw new AssertionFailedError("Playbook step " + i + " Action " + j + " frameId mismatch! Expected: \"" + expectedAction.getFrameId() + "\", but got: \"" + actualAction.getFrameId() + "\"");
                }
            }
        }
    }

}

