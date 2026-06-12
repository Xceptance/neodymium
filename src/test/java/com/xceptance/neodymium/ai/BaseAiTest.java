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
import java.util.Collections;
import java.util.List;
import com.xceptance.neodymium.common.browser.BrowserMethodData;
import com.xceptance.neodymium.common.browser.BrowserRunner;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import com.xceptance.neodymium.ai.util.EmbeddedHtmlServer;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookStep;
import com.xceptance.neodymium.ai.playbook.PlaybookManager;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.core.AiStats;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
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
    private TestInfo testInfo;

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
        this.testInfo = testInfo;
        final String className = testInfo.getTestClass().get().getSimpleName();
        final String methodName = testInfo.getTestMethod().get().getName();
        
        currentTestUrl = String.format("http://localhost:%d/%s/%s.html", server.getPort(), className, methodName);
        Neodymium.setAiPlaybook(null);
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "false");
    }

    /**
     * Cleans up any active browser/WebDriver session left open at the end of the test.
     * This ensures orphaned browsers from resetBrowser() calls are properly closed.
     */
    @AfterEach
    public final void cleanUpActiveBrowser()
    {
        if (Neodymium.getWebDriverStateContainer() != null)
        {
            final String profileName = Neodymium.getBrowserProfileName();
            if (profileName != null)
            {
                final BrowserRunner runner = new BrowserRunner();
                runner.teardown(false, true,
                    new BrowserMethodData(profileName, false, false, true, true, Collections.emptyList()),
                    Neodymium.getWebDriverStateContainer());
            }
        }
    }



    /**
     * Resolves the verification modes from the @AiTestVerification annotation on the test method or test class.
     * Fallback to the default modes (LIVE_LLM, OFFLINE_REPLAY) if the annotation is absent.
     */
    private VerificationMode[] resolveVerificationModes()
    {
        VerificationMode[] declaredModes = null;
        if (testInfo != null)
        {
            // 1. Check method level
            final var method = testInfo.getTestMethod().orElse(null);
            if (method != null && method.isAnnotationPresent(AiTestVerification.class))
            {
                declaredModes = method.getAnnotation(AiTestVerification.class).value();
            }
            else
            {
                // 2. Check class level
                final var testClass = testInfo.getTestClass().orElse(null);
                if (testClass != null && testClass.isAnnotationPresent(AiTestVerification.class))
                {
                    declaredModes = testClass.getAnnotation(AiTestVerification.class).value();
                }
            }
        }
        
        if (declaredModes == null)
        {
            declaredModes = new VerificationMode[]{VerificationMode.LIVE_LLM, VerificationMode.OFFLINE_REPLAY};
        }

        // If no API key is configured, filter out all live LLM modes to prevent test failures on missing credentials
        final String apiKey = Neodymium.aiConfiguration().aiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty())
        {
            final java.util.List<VerificationMode> offlineModes = new java.util.ArrayList<>();
            for (final VerificationMode mode : declaredModes)
            {
                if (mode == VerificationMode.OFFLINE_REPLAY || mode == VerificationMode.HUD_OFFLINE_REPLAY || mode == VerificationMode.REPLAY || mode == VerificationMode.HUD_REPLAY)
                {
                    offlineModes.add(mode);
                }
            }
            if (offlineModes.isEmpty())
            {
                offlineModes.add(VerificationMode.OFFLINE_REPLAY);
            }
            return offlineModes.toArray(new VerificationMode[0]);
        }

        return declaredModes;
    }

    /**
     * Executes the test run steps sequentially through all configured verification modes.
     * 
     * @param runSteps the runnable steps containing Neodymium AI execution blocks
     */
    protected void assertAiExecution(final Runnable runSteps)
    {
        final VerificationMode[] modes = resolveVerificationModes();
        final String playbookId = Neodymium.getTestName();
        
        // Save original configurations
        final String origInteractive = String.valueOf(Neodymium.aiConfiguration().aiInteractive());
        final String origAutoSkip = System.getProperty("neodymium.ai.interactive.autoSkip", "false");
        
        try
        {
            final AiStats stats = Neodymium.ai().getStats();
            int previousCalls = stats.getOverallCallCount();

            for (int i = 0; i < modes.length; i++)
            {
                final VerificationMode mode = modes[i];
                
                // Reset/Open URL if it is not the first mode run, to have a clean SUT state
                if (i > 0)
                {
                    final String activeUrl = WebDriverRunner.url();
                    if (activeUrl != null && !activeUrl.equals("about:blank"))
                    {
                        Selenide.open(activeUrl);
                    }
                }

                switch (mode)
                {
                    case LIVE_LLM ->
                    {
                        Neodymium.getData().put("skipReplay", "true");
                        Neodymium.setAiPlaybook(null);
                        Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", "false");
                        System.setProperty("neodymium.ai.interactive.autoSkip", "false");
                        
                        runSteps.run();

                        final Playbook generatedPlaybook = Neodymium.getAiPlaybook();
                        if (generatedPlaybook != null && generatedPlaybook.isChanged())
                        {
                            PlaybookManager.savePlaybook(generatedPlaybook);
                        }
                        
                        previousCalls = stats.getOverallCallCount();
                    }
                    case OFFLINE_REPLAY ->
                    {
                        // Enforce strictly offline - playbook must exist
                        final Playbook playbook = PlaybookManager.loadPlaybook(playbookId);
                        if (playbook == null)
                        {
                            Assertions.fail("No playbook found for OFFLINE_REPLAY: " + playbookId);
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

                        System.setProperty("neodymium.ai.offline", "true");
                        try
                        {
                            runSteps.run();
                        }
                        finally
                        {
                            System.clearProperty("neodymium.ai.offline");
                        }

                        final int currentCalls = stats.getOverallCallCount();
                        Assertions.assertEquals(0, currentCalls - previousCalls,
                            "OFFLINE_REPLAY execution made LLM calls! Expected exactly 0 new LLM calls during playback.");
                        previousCalls = currentCalls;
                    }
                    case REPLAY ->
                    {
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
                        
                        previousCalls = stats.getOverallCallCount();
                    }
                    case HUD_LLM ->
                    {
                        Neodymium.setAiPlaybook(null);
                        Neodymium.getData().put("skipReplay", "true");
                        Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", "true");
                        System.setProperty("neodymium.ai.interactive.autoSkip", "true");

                        runSteps.run();
                        
                        previousCalls = stats.getOverallCallCount();
                    }
                    case HUD_OFFLINE_REPLAY ->
                    {
                        // Enforce strictly offline - playbook must exist
                        final Playbook playbook = PlaybookManager.loadPlaybook(playbookId);
                        if (playbook == null)
                        {
                            Assertions.fail("No playbook found for HUD_OFFLINE_REPLAY: " + playbookId);
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

                        System.setProperty("neodymium.ai.offline", "true");
                        try
                        {
                            runSteps.run();
                        }
                        finally
                        {
                            System.clearProperty("neodymium.ai.offline");
                        }

                        final int currentCalls = stats.getOverallCallCount();
                        Assertions.assertEquals(0, currentCalls - previousCalls,
                            "HUD_OFFLINE_REPLAY execution made LLM calls! Expected exactly 0 new LLM calls during HUD playback.");
                        previousCalls = currentCalls;
                    }
                    case HUD_REPLAY ->
                    {
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

                        runSteps.run();
                        
                        previousCalls = stats.getOverallCallCount();
                    }
                }
            }
        }
        finally
        {
            // Restore clean offline state
            Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", origInteractive);
            System.setProperty("neodymium.ai.interactive.autoSkip", origAutoSkip);
        }
    }

    protected void assertAiExecution(final String steps)
    {
        assertAiExecution(() ->
        {
            Neodymium.ai().execute(steps);
        });
    }

    protected void assertAiExecution()
    {
        assertAiExecution(() ->
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
     * Closes the current browser and WebDriver session to ensure a completely clean state.
     * The next browser interaction (e.g., Selenide.open) will automatically start a new browser session.
     */
    protected final void resetBrowser()
    {
        final String profileName = Neodymium.getBrowserProfileName();
        if (profileName != null)
        {
            final BrowserRunner runner = new BrowserRunner();
            runner.teardown(false, true,
                new BrowserMethodData(profileName, false, false, true, true, Collections.emptyList()),
                Neodymium.getWebDriverStateContainer());
            try
            {
                Thread.sleep(1000);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            runner.setUpTest(
                new BrowserMethodData(profileName, false, false, true, true, Collections.emptyList()),
                Neodymium.getTestName());
        }
        else
        {
            Selenide.closeWebDriver();
        }
    }

    /**
     * Executes the test run steps under the specified verification mode.
     *
     * @param steps the natural language instructions
     * @param mode  the verification mode to run under
     * @return the execution result
     */
    protected final AiExecutionResult runAi(final String steps, final VerificationMode mode)
    {
        return runAi(() ->
        {
            Neodymium.ai().execute(steps);
        }, mode);
    }

    /**
     * Executes the test run steps under the specified verification mode.
     *
     * @param runSteps the runnable steps containing Neodymium AI execution blocks
     * @param mode     the verification mode to run under
     * @return the execution result
     */
    protected final AiExecutionResult runAi(final Runnable runSteps, final VerificationMode mode)
    {
        final String playbookId = Neodymium.getTestName();
        final String origInteractive = String.valueOf(Neodymium.aiConfiguration().aiInteractive());
        final String origAutoSkip = System.getProperty("neodymium.ai.interactive.autoSkip", "false");
        
        final AiStats stats = Neodymium.ai().getStats();
        final int previousCalls = stats.getOverallCallCount();
        
        try
        {
            switch (mode)
            {
                case LIVE_LLM ->
                {
                    Neodymium.getData().put("skipReplay", "true");
                    if (Neodymium.getAiPlaybook() == null)
                    {
                        Neodymium.setAiPlaybook(null);
                    }
                    Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", "false");
                    System.setProperty("neodymium.ai.interactive.autoSkip", "false");
                    
                    runSteps.run();

                    final Playbook generatedPlaybook = Neodymium.getAiPlaybook();
                    if (generatedPlaybook != null && generatedPlaybook.isChanged())
                    {
                        PlaybookManager.savePlaybook(generatedPlaybook);
                    }
                }
                case OFFLINE_REPLAY ->
                {
                    final Playbook playbook = PlaybookManager.loadPlaybook(playbookId);
                    if (playbook == null)
                    {
                        Assertions.fail("No playbook found for OFFLINE_REPLAY: " + playbookId);
                    }

                    Neodymium.getData().put("skipReplay", "false");
                    Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", "false");
                    System.setProperty("neodymium.ai.interactive.autoSkip", "false");

                    if (Neodymium.getAiPlaybook() == null)
                    {
                        Neodymium.setAiPlaybook(null);
                        Neodymium.initializePlaybook();
                    }
                    final Playbook loadedPlaybook = Neodymium.getAiPlaybook();
                    if (loadedPlaybook != null)
                    {
                        loadedPlaybook.setRecording(false);
                        loadedPlaybook.setCursor(0);
                    }

                    System.setProperty("neodymium.ai.offline", "true");
                    try
                    {
                        runSteps.run();
                    }
                    finally
                    {
                        System.clearProperty("neodymium.ai.offline");
                    }

                    final int currentCalls = stats.getOverallCallCount();
                    Assertions.assertEquals(0, currentCalls - previousCalls,
                        "OFFLINE_REPLAY execution made LLM calls! Expected exactly 0 new LLM calls during playback.");
                }
                case REPLAY ->
                {
                    Neodymium.getData().put("skipReplay", "false");
                    Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", "false");
                    System.setProperty("neodymium.ai.interactive.autoSkip", "false");

                    if (Neodymium.getAiPlaybook() == null)
                    {
                        Neodymium.setAiPlaybook(null);
                        Neodymium.initializePlaybook();
                    }
                    final Playbook loadedPlaybook = Neodymium.getAiPlaybook();
                    if (loadedPlaybook != null)
                    {
                        loadedPlaybook.setRecording(false);
                        loadedPlaybook.setCursor(0);
                    }

                    runSteps.run();
                }
                case HUD_LLM ->
                {
                    Neodymium.getData().put("skipReplay", "true");
                    if (Neodymium.getAiPlaybook() == null)
                    {
                        Neodymium.setAiPlaybook(null);
                    }
                    Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", "true");
                    System.setProperty("neodymium.ai.interactive.autoSkip", "true");

                    runSteps.run();
                }
                case HUD_OFFLINE_REPLAY ->
                {
                    final Playbook playbook = PlaybookManager.loadPlaybook(playbookId);
                    if (playbook == null)
                    {
                        Assertions.fail("No playbook found for HUD_OFFLINE_REPLAY: " + playbookId);
                    }

                    Neodymium.getData().put("skipReplay", "false");
                    Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", "true");
                    System.setProperty("neodymium.ai.interactive.autoSkip", "true");

                    if (Neodymium.getAiPlaybook() == null)
                    {
                        Neodymium.setAiPlaybook(null);
                        Neodymium.initializePlaybook();
                    }
                    final Playbook hudPlaybook = Neodymium.getAiPlaybook();
                    if (hudPlaybook != null)
                    {
                        hudPlaybook.setRecording(false);
                        hudPlaybook.setCursor(0);
                    }

                    System.setProperty("neodymium.ai.offline", "true");
                    try
                    {
                        runSteps.run();
                    }
                    finally
                    {
                        System.clearProperty("neodymium.ai.offline");
                    }

                    final int currentCalls = stats.getOverallCallCount();
                    Assertions.assertEquals(0, currentCalls - previousCalls,
                        "HUD_OFFLINE_REPLAY execution made LLM calls! Expected exactly 0 new LLM calls during HUD playback.");
                }
                case HUD_REPLAY ->
                {
                    Neodymium.getData().put("skipReplay", "false");
                    Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", "true");
                    System.setProperty("neodymium.ai.interactive.autoSkip", "true");

                    if (Neodymium.getAiPlaybook() == null)
                    {
                        Neodymium.setAiPlaybook(null);
                        Neodymium.initializePlaybook();
                    }
                    final Playbook hudPlaybook = Neodymium.getAiPlaybook();
                    if (hudPlaybook != null)
                    {
                        hudPlaybook.setRecording(false);
                        hudPlaybook.setCursor(0);
                    }

                    runSteps.run();
                }
            }
            return Neodymium.getLastAiExecutionResult();
        }
        finally
        {
            Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", origInteractive);
            System.setProperty("neodymium.ai.interactive.autoSkip", origAutoSkip);
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
        assertPlaybookEquals(expected, actual, false);
    }

    private void assertPlaybookEquals(final Playbook expected, final Playbook actual, final boolean allowFewerSteps)
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

        if (!allowFewerSteps)
        {
            if (expectedSteps.size() != actualSteps.size())
            {
                throw new AssertionFailedError("Playbook step count mismatch! Expected: " + expectedSteps.size() + " steps, but got: " + actualSteps.size());
            }
        }

        final int limit = Math.min(expectedSteps.size(), actualSteps.size());
        for (int i = 0; i < limit; i++)
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

            // Relax assertion/verification steps comparison to tolerate LLM non-determinism
            final boolean isAssertionStep = expectedStep.getPromptLine() != null &&
                (expectedStep.getPromptLine().toLowerCase().startsWith("verify") ||
                 expectedStep.getPromptLine().toLowerCase().startsWith("assert"));

            if (expectedActions.size() != actualActions.size())
            {
                if (isAssertionStep)
                {
                    final boolean expectedAllAssert = expectedActions.stream().allMatch(a -> "ASSERT".equals(a.getType()));
                    final boolean actualAllAssert = actualActions.stream().allMatch(a -> "ASSERT".equals(a.getType()));
                    if ((expectedActions.isEmpty() && actualAllAssert) || (actualActions.isEmpty() && expectedAllAssert))
                    {
                        continue;
                    }
                }
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

                // Compare target / selector (normalized to prevent minor LLM format drifts)
                final String expectedTarget = normalizeTarget(expectedAction.getTarget());
                final String actualTarget = normalizeTarget(actualAction.getTarget());
                if (!Objects.equals(expectedTarget, actualTarget))
                {
                    throw new AssertionFailedError("Playbook step " + i + " Action " + j + " target selector mismatch! Expected: \"" + expectedAction.getTarget() + "\" (normalized: \"" + expectedTarget + "\"), but got: \"" + actualAction.getTarget() + "\" (normalized: \"" + actualTarget + "\")");
                }

                // Compare values
                if (!Objects.equals(expectedAction.getValue(), actualAction.getValue()))
                {
                    throw new AssertionFailedError("Playbook step " + i + " Action " + j + " value mismatch! Expected: " + expectedAction.getValue() + ", but got: " + actualAction.getValue());
                }

                // Compare frameId (normalized to ignore dynamic driver session handles)
                final String expectedFrameId = normalizeFrameId(expectedAction.getFrameId());
                final String actualFrameId = normalizeFrameId(actualAction.getFrameId());
                if (!Objects.equals(expectedFrameId, actualFrameId))
                {
                    throw new AssertionFailedError("Playbook step " + i + " Action " + j + " frameId mismatch! Expected: \"" + expectedAction.getFrameId() + "\" (normalized: \"" + expectedFrameId + "\"), but got: \"" + actualAction.getFrameId() + "\" (normalized: \"" + actualFrameId + "\")");
                }
            }
        }
    }

    /**
     * Normalizes action target selectors to prevent mismatches caused by minor LLM format drifts.
     * 
     * @param target the raw target string
     * @return the normalized target
     */
    private String normalizeTarget(final String target)
    {
        if (target == null || target.trim().isEmpty())
        {
            return "";
        }
        String cleaned = target.trim();
        if (cleaned.startsWith("#xc_"))
        {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.toLowerCase().startsWith("xpath="))
        {
            cleaned = cleaned.substring(6);
        }
        else if (cleaned.toLowerCase().startsWith("css="))
        {
            cleaned = cleaned.substring(4);
        }
        if (cleaned.contains("data-neo-ref="))
        {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile("data-neo-ref=['\"]?(xc_[a-zA-Z0-9_]+)['\"]?")
                    .matcher(cleaned);
            if (m.find())
            {
                cleaned = m.group(1);
            }
        }
        if (cleaned.startsWith("[") && cleaned.endsWith("]"))
        {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        cleaned = cleaned.replaceAll("^['\"]|['\"]$", "");
        return cleaned;
    }

    /**
     * Normalizes a frame ID to ignore dynamic driver session handles.
     * 
     * @param frameId the raw frame ID
     * @return the normalized frame ID
     */
    private String normalizeFrameId(final String frameId)
    {
        if (frameId == null)
        {
            return null;
        }
        final int colonIndex = frameId.indexOf(':');
        if (colonIndex != -1)
        {
            return frameId.substring(colonIndex);
        }
        return frameId;
    }

}

