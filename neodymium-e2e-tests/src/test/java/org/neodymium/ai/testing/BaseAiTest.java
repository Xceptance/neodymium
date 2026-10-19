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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Random;
import org.neodymium.ai.server.client.TestServerClient;
import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.BrowserMethodData;
import org.neodymium.common.browser.BrowserRunner;
import org.neodymium.common.browser.configuration.BrowserConfiguration;
import org.neodymium.common.browser.configuration.MultibrowserConfiguration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.server.JavalinTestServer;
import org.neodymium.common.browser.SuppressBrowsers;
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
    protected static JavalinTestServer server;
    protected String currentTestUrl;
    private TestInfo testInfo;
    private static volatile boolean shutdownHookRegistered = false;

    /**
     * Starts the embedded server before any tests in the class are run.
     * 
     * @throws IOException if server fails to start
     */
    @BeforeAll
    public static synchronized void startServer() throws IOException
    {
        Configuration.headless = true;
        if (System.getProperty("selenide.headless") != null)
        {
            Configuration.headless = Boolean.parseBoolean(System.getProperty("selenide.headless"));
        }
        if (server == null || !server.isRunning())
        {
            server = new JavalinTestServer();
            server.start();
        }
        if (!shutdownHookRegistered)
        {
            shutdownHookRegistered = true;
            Runtime.getRuntime().addShutdownHook(new Thread(() ->
            {
                if (server != null && server.isRunning())
                {
                    server.stop();
                }
            }));
        }
    }

    /**
     * Stops the embedded server after all tests in the class have finished.
     */
    @AfterAll
    public static synchronized void stopServer()
    {
        // Server is kept running across test classes to prevent OS socket TIME_WAIT port exhaustion.
        // It will shut down automatically on JVM exit via shutdown hook.
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
        if (server == null || !server.isRunning())
        {
            try
            {
                startServer();
            }
            catch (final IOException e)
            {
                throw new RuntimeException("Failed to ensure JavalinTestServer is running", e);
            }
        }
        server.resetAll();

        final boolean isInteractive = AiConfiguration.getInstance().isInteractive();
        Assertions.assertFalse(isInteractive,
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
        else
        {
            // Resolve @Browser annotation directly when running standalone without Neodymium extension
            Browser browserAnnotation = null;
            if (testInfo.getTestMethod().isPresent())
            {
                browserAnnotation = testInfo.getTestMethod().get().getAnnotation(Browser.class);
            }
            if (browserAnnotation == null && testInfo.getTestClass().isPresent())
            {
                browserAnnotation = testInfo.getTestClass().get().getAnnotation(Browser.class);
            }
            if (browserAnnotation != null)
            {
                final String profile = browserAnnotation.value();
                final BrowserConfiguration config = MultibrowserConfiguration.getInstance()
                    .getBrowserProfiles().get(profile);
                if (config != null)
                {
                    Configuration.headless = config.isHeadless();
                }
                else if (profile != null && profile.toLowerCase().contains("headless"))
                {
                    Configuration.headless = true;
                }
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
     * Resolves a test resource file on the filesystem, supporting execution both from the project root
     * and from the submodule directory in multi-module builds.
     *
     * @param relativePath the relative resource path under src/test/resources (e.g. "playbooks/...")
     * @return the resolved File pointing to the existing file if found, or the direct path candidate
     */
    protected File getTestResourceFile(final String relativePath)
    {
        final File direct = new File("src/test/resources", relativePath);
        if (direct.exists())
        {
            return direct;
        }
        final File submodule = new File("neodymium-core/src/test/resources", relativePath);
        if (submodule.exists())
        {
            return submodule;
        }
        return direct;
    }

    /**
     * Computes a deterministic pseudo-random latency in [minMs, maxMs] seeded by the session ID.
     * Purely numeric session IDs are parsed as numbers; otherwise the String hash code is used as seed.
     *
     * @param sessionId the session identifier or seed
     * @param minMs minimum latency in milliseconds
     * @param maxMs maximum latency in milliseconds
     * @return the calculated latency in milliseconds
     */
    protected int deriveSessionLatencyMs(final String sessionId, final int minMs, final int maxMs)
    {
        if (sessionId == null || sessionId.isBlank() || minMs <= 0 && maxMs <= 0)
        {
            return 0;
        }
        final long seed = sessionId.chars().allMatch(Character::isDigit)
            ? Long.parseLong(sessionId)
            : (long) sessionId.hashCode();
        final int safeMax = Math.max(minMs, maxMs);
        final int bound = safeMax - minMs + 1;
        return minMs + new Random(seed).nextInt(bound);
    }

    /**
     * Returns the full URL for an Aura Glance sandbox page on the running test server.
     *
     * @param path relative path under /AuraGlanceTest
     * @return full HTTP URL
     */
    protected String getSandboxUrl(final String path)
    {
        return getSandboxUrl(path, null);
    }

    /**
     * Returns the full URL for an Aura Glance sandbox page on the running test server,
     * binding a custom test session on initial load.
     *
     * @param path relative path under /AuraGlanceTest
     * @param customSessionId the test session identifier
     * @return full HTTP URL
     */
    protected String getSandboxUrl(final String path, final String customSessionId)
    {
        final String clean = (path == null || path.isBlank()) ? "" : (path.startsWith("/") ? path : "/" + path);
        if (customSessionId == null || customSessionId.isBlank())
        {
            return String.format("http://localhost:%d/AuraGlanceTest%s", server.getPort(), clean);
        }
        return String.format("http://localhost:%d/AuraGlanceTest%s?customSession=%s",
            server.getPort(), clean, customSessionId.trim());
    }

    /**
     * Returns the full URL for a VÉRLA storefront or endpoint on the running test server.
     *
     * @param path relative path from root
     * @return full HTTP URL
     */
    protected String getVerlaUrl(final String path)
    {
        return getVerlaUrl(path, null);
    }

    /**
     * Returns the full URL for a VÉRLA storefront or endpoint on the running test server,
     * binding a custom test session on initial load.
     *
     * @param path relative path from root
     * @param customSessionId the test session identifier
     * @return full HTTP URL
     */
    protected String getVerlaUrl(final String path, final String customSessionId)
    {
        final String clean = (path == null || path.isBlank()) ? "" : (path.startsWith("/") ? path : "/" + path);
        if (customSessionId == null || customSessionId.isBlank())
        {
            return String.format("http://localhost:%d%s", server.getPort(), clean);
        }
        return String.format("http://localhost:%d%s?customSession=%s",
            server.getPort(), clean, customSessionId.trim());
    }

    /**
     * Returns a {@link TestServerClient} for the given session ID communicating via REST.
     *
     * @param customSessionId the test session identifier
     * @return the test server client
     */
    protected TestServerClient getTestClient(final String customSessionId)
    {
        if (server == null)
        {
            throw new IllegalStateException("Test server has not been started yet");
        }
        return server.getClient(customSessionId);
    }

    /**
     * Derives a deterministic latency seeded by the session ID and applies it to the test session via REST.
     *
     * @param customSessionId the test session identifier
     * @param minMs minimum latency in milliseconds
     * @param maxMs maximum latency in milliseconds
     * @return the applied latency in milliseconds
     */
    protected int configureSessionLatency(final String customSessionId, final int minMs, final int maxMs)
    {
        final int latency = deriveSessionLatencyMs(customSessionId, minMs, maxMs);
        getTestClient(customSessionId).setAuraGlanceDelay(latency);
        return latency;
    }

    /**
     * Resets simulated latency and faults for the specified session via REST.
     *
     * @param customSessionId the test session identifier
     */
    protected void resetSession(final String customSessionId)
    {
        if (server != null && customSessionId != null && !customSessionId.isBlank())
        {
            getTestClient(customSessionId).resetAuraGlance();
        }
    }
}
