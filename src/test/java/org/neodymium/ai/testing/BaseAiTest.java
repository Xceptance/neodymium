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
package org.neodymium.ai.testing;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.playbook.PlaybookParser;
import org.neodymium.ai.playbook.YamlPlaybookParser;
import org.neodymium.ai.resources.InMemoryResourceManager;
import org.neodymium.ai.pipeline.steps.ExecuteActionsStep;
import org.neodymium.ai.runner.StateMachineRunner;
import org.neodymium.common.browser.BrowserMethodData;
import org.neodymium.common.browser.BrowserRunner;
import org.neodymium.common.browser.configuration.BrowserConfiguration;
import org.neodymium.common.browser.configuration.MultibrowserConfiguration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import org.neodymium.common.browser.SuppressBrowsers;
import org.neodymium.ai.util.EmbeddedHtmlServer;
import org.neodymium.util.Neodymium;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;

/**
 * Base class for all AI tests in the decoupled org.neodymium.ai package.
 * Automatically manages an embedded HTTP server and determines the test URL.
 * 
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public abstract class BaseAiTest extends BaseLlmTest
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
        Configuration.headless = Boolean.parseBoolean(System.getProperty("selenide.headless", "true"));
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
        EmbeddedHtmlServer.resetInventory();

        final boolean isInteractive = org.neodymium.ai.config.AiConfiguration.getInstance().isInteractive();
        org.junit.jupiter.api.Assertions.assertFalse(isInteractive,
            "Interactive mode is currently active ('neodymium.ai.interactive'=true or configured in properties). Automated batch tests must run with interactive mode disabled (isInteractive=false) to prevent halting and waiting for manual UI console input.");

        final String className = testInfo.getTestClass().get().getSimpleName();
        final String methodName = testInfo.getTestMethod().get().getName();
        
        currentTestUrl = String.format("http://localhost:%d/%s/%s.html", server.getPort(), className, methodName);

        // Apply headless configuration from the active browser profile if configured
        final String profileName = Neodymium.getBrowserProfileName();
        if (profileName != null)
        {
            final BrowserConfiguration config = MultibrowserConfiguration.getInstance()
                .getBrowserProfiles().get(profileName);
            if (config != null)
            {
                Configuration.headless = config.isHeadless();
            }
        }
    }

    /**
     * Cleans up any active browser/WebDriver session left open at the end of the test.
     */
    @AfterEach
    @SuppressBrowsers
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
     * Closes the current browser and WebDriver session to ensure a completely clean state.
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

}
