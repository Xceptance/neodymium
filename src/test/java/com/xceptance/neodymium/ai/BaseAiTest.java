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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import com.xceptance.neodymium.ai.util.EmbeddedHtmlServer;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookManager;
import com.xceptance.neodymium.ai.core.AiStats;
import com.xceptance.neodymium.util.Neodymium;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;

/**
 * Base class for all AI tests. 
 * Automatically manages an embedded HTTP server and determines the test URL based on class/method name.
 * 
 * AI-generated: Gemini 2.5 Pro
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
        // Phase 1: Live execution with LLM.
        Neodymium.getData().put("skipReplay", "true");
        Neodymium.setAiPlaybook(null);
        
        runSteps.run();

        final Playbook playbook = Neodymium.getAiPlaybook();
        if (playbook != null && playbook.isChanged())
        {
            PlaybookManager.savePlaybook(playbook);
        }

        final AiStats stats = Neodymium.ai().getStats();
        final int initialCalls = stats.getOverallCallCount();

        // Phase 2: Offline playback check with zero LLM calls.
        final String activeUrl = com.codeborne.selenide.WebDriverRunner.url();
        if (activeUrl != null && !activeUrl.equals("about:blank"))
        {
            Selenide.open(activeUrl);
        }

        Neodymium.setAiPlaybook(null);
        Neodymium.getData().put("skipReplay", "false");

        Neodymium.initializePlaybook();
        final Playbook loadedPlaybook = Neodymium.getAiPlaybook();
        if (loadedPlaybook != null)
        {
            loadedPlaybook.setRecording(false);
            loadedPlaybook.setCursor(0);
        }

        runSteps.run();

        final int finalCalls = stats.getOverallCallCount();
        org.junit.jupiter.api.Assertions.assertEquals(initialCalls, finalCalls,
            "Replay execution made LLM calls! Expected exactly 0 new LLM calls during playback.");
    }
}

