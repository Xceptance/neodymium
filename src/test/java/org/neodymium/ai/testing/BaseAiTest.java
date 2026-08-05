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

    /**
     * Helper to programmatically parse and execute a playbook YAML string on the given AI session.
     *
     * @param session   the executing thread-isolated AiSession
     * @param stepsYaml the multiline YAML steps/playbook string
     */
    protected final void runPlaybook(final AiSession session, final String stepsYaml)
    {
        final ExecutionContext context = session.getExecutionContext();
        org.neodymium.ai.config.ExecutionMode mode = (org.neodymium.ai.config.ExecutionMode) context.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE);
        if (mode == null && session != null)
        {
            mode = session.getExecutionMode();
            context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, mode);
        }

        final PlaybookParser parser = new YamlPlaybookParser();
        final InMemoryResourceManager manager = new InMemoryResourceManager();
        try
        {

            @SuppressWarnings("unchecked")
            final List<org.neodymium.ai.model.PlaybookStep> sessionSteps = (List<org.neodymium.ai.model.PlaybookStep>) context.getTransientData().get("playbook.steps");

            manager.write("programmatic-playbook.yaml", stepsYaml);
            final org.neodymium.ai.model.Playbook playbook = parser.parse("programmatic-playbook.yaml", manager);
            final List<org.neodymium.ai.model.PlaybookStep> stepsToExecute = playbook.getSteps();



            if (mode != null && mode.isReplay() && sessionSteps != null && !sessionSteps.isEmpty())
            {
                for (int i = 0; i < stepsToExecute.size() && i < sessionSteps.size(); i++)
                {
                    final org.neodymium.ai.model.PlaybookStep parsed = stepsToExecute.get(i);
                    final org.neodymium.ai.model.PlaybookStep recorded = sessionSteps.get(i);
                    if (recorded.getActions() != null && !recorded.getActions().isEmpty())
                    {
                        parsed.setActions(recorded.getActions());
                    }
                }
            }

            if (sessionSteps != null)
            {
                sessionSteps.clear();
                sessionSteps.addAll(stepsToExecute);
            }

            // Push steps in reverse order onto execution context stack
            for (int i = stepsToExecute.size() - 1; i >= 0; i--)
            {
                context.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(stepsToExecute.get(i), session, context));
            }

            // Execute using the StateMachineRunner
            final StateMachineRunner runner = new StateMachineRunner(session);
            runner.run();
        }
        catch (final Exception e)
        {
            Throwable root = e;
            while (root != null)
            {
                if (root instanceof AssertionError ae)
                {
                    throw ae;
                }
                if (root.getCause() == root)
                {
                    break;
                }
                root = root.getCause();
            }
            if (e instanceof RuntimeException re)
            {
                throw re;
            }
            throw new RuntimeException("Failed to run programmatic playbook", e);
        }
    }
}
