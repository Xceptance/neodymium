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
package org.neodymium.ai.util;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import java.time.Duration;
import org.neodymium.ai.tool.browser.BrowserToolProvider;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Universal, framework-agnostic DOM quiescence watcher.
 * <p>
 * Monitors in-browser DOM mutations via {@code MutationObserver}, active {@code fetch} and
 * {@code XMLHttpRequest} network calls, and flushes rendering queues via double {@code requestAnimationFrame}.
 * Never relies on SUT-specific framework hooks (such as HTMX, React, Angular, or Vue).
 * </p>
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class DomQuiescenceWatcher
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DomQuiescenceWatcher.class);

    private static final Duration DEFAULT_MAX_TIMEOUT = Duration.ofMillis(1500);
    private static final Duration DEFAULT_QUIET_PERIOD = Duration.ofMillis(150);

    private static final String INSTALL_TRACKER_SCRIPT = """
        if (!window.__neo_network_tracker) {
            window.__neo_network_tracker = { active: 0, lastActivity: performance.now() };
            try {
                if (typeof window.fetch === 'function') {
                    var origFetch = window.fetch;
                    window.fetch = function() {
                        window.__neo_network_tracker.active++;
                        window.__neo_network_tracker.lastActivity = performance.now();
                        return origFetch.apply(this, arguments).finally(function() {
                            window.__neo_network_tracker.active = Math.max(0, window.__neo_network_tracker.active - 1);
                            window.__neo_network_tracker.lastActivity = performance.now();
                        });
                    };
                }
                if (window.XMLHttpRequest) {
                    var origSend = XMLHttpRequest.prototype.send;
                    XMLHttpRequest.prototype.send = function() {
                        window.__neo_network_tracker.active++;
                        window.__neo_network_tracker.lastActivity = performance.now();
                        this.addEventListener('loadend', function() {
                            window.__neo_network_tracker.active = Math.max(0, window.__neo_network_tracker.active - 1);
                            window.__neo_network_tracker.lastActivity = performance.now();
                        });
                        return origSend.apply(this, arguments);
                    };
                }
            } catch (e) {}
        }
        """;

    private static final String WAIT_FOR_QUIET_SCRIPT = """
        var quietMs = arguments[0];
        var maxTimeoutMs = arguments[1];
        var done = arguments[arguments.length - 1];

        // Ensure network tracker is present
        if (!window.__neo_network_tracker) {
            window.__neo_network_tracker = { active: 0, lastActivity: performance.now() };
            try {
                if (typeof window.fetch === 'function') {
                    var origFetch = window.fetch;
                    window.fetch = function() {
                        window.__neo_network_tracker.active++;
                        window.__neo_network_tracker.lastActivity = performance.now();
                        return origFetch.apply(this, arguments).finally(function() {
                            window.__neo_network_tracker.active = Math.max(0, window.__neo_network_tracker.active - 1);
                            window.__neo_network_tracker.lastActivity = performance.now();
                        });
                    };
                }
                if (window.XMLHttpRequest) {
                    var origSend = XMLHttpRequest.prototype.send;
                    XMLHttpRequest.prototype.send = function() {
                        window.__neo_network_tracker.active++;
                        window.__neo_network_tracker.lastActivity = performance.now();
                        this.addEventListener('loadend', function() {
                            window.__neo_network_tracker.active = Math.max(0, window.__neo_network_tracker.active - 1);
                            window.__neo_network_tracker.lastActivity = performance.now();
                        });
                        return origSend.apply(this, arguments);
                    };
                }
            } catch (e) {}
        }

        // If the document is hidden/backgrounded, requestAnimationFrame is paused and DOM rendering is frozen.
        // Complete immediately to avoid async script timeouts.
        if (document.hidden) {
            done(true);
            return;
        }

        var lastChange = performance.now();
        var observer = null;
        var checkInterval = null;
        var maxTimer = null;
        var finished = false;

        function cleanup() {
            if (observer) {
                try { observer.disconnect(); } catch (e) {}
            }
            if (checkInterval) clearInterval(checkInterval);
            if (maxTimer) clearTimeout(maxTimer);
        }

        function finish() {
            if (finished) return;
            finished = true;
            cleanup();
            if (document.hidden) {
                done(true);
                return;
            }
            if (window.requestAnimationFrame) {
                var rafFired = false;
                var fallbackTimer = setTimeout(function() {
                    if (!rafFired) {
                        rafFired = true;
                        done(true);
                    }
                }, 100);
                window.requestAnimationFrame(function() {
                    window.requestAnimationFrame(function() {
                        if (!rafFired) {
                            rafFired = true;
                            clearTimeout(fallbackTimer);
                            done(true);
                        }
                    });
                });
            } else {
                done(true);
            }
        }

        try {
            if (window.MutationObserver) {
                observer = new MutationObserver(function() {
                    lastChange = performance.now();
                });
                observer.observe(document.documentElement || document.body, {
                    childList: true,
                    subtree: true,
                    attributes: true,
                    characterData: true
                });
            }

            maxTimer = setTimeout(finish, maxTimeoutMs);

            checkInterval = setInterval(function() {
                if (document.hidden) {
                    finish();
                    return;
                }
                var now = performance.now();
                var tracker = window.__neo_network_tracker;
                var activeRequests = (tracker && typeof tracker.active === 'number') ? tracker.active : 0;
                
                if (activeRequests === 0 && (now - lastChange) >= quietMs) {
                    finish();
                }
            }, 25);
        } catch (e) {
            finish();
        }
        """;

    private DomQuiescenceWatcher()
    {
    }

    /**
     * Installs the lightweight in-browser network activity tracker if not already present.
     */
    public static void installTracker()
    {
        if (!WebDriverRunner.hasWebDriverStarted() || BrowserToolProvider.isAlertPresent(WebDriverRunner.getWebDriver()))
        {
            return;
        }
        try
        {
            BrowserToolProvider.ensureValidWindowFocus(WebDriverRunner.getWebDriver());
            Selenide.executeJavaScript(INSTALL_TRACKER_SCRIPT);
        }
        catch (final Exception e)
        {
            LOGGER.debug("Could not install DOM network tracker: {}", e.getMessage());
        }
    }

    /**
     * Waits for the DOM to settle using default timeouts.
     */
    public static void waitForDomQuiet()
    {
        waitForDomQuiet(DEFAULT_MAX_TIMEOUT, DEFAULT_QUIET_PERIOD);
    }

    /**
     * Waits for DOM mutations and active network requests to cease for the given quiet period,
     * up to the specified maximum timeout.
     *
     * @param maxTimeout the maximum total duration to wait
     * @param quietPeriod the required uninterrupted period of quiet (no mutations, no active requests)
     */
    public static void waitForDomQuiet(final Duration maxTimeout, final Duration quietPeriod)
    {
        if (!WebDriverRunner.hasWebDriverStarted() || BrowserToolProvider.isAlertPresent(WebDriverRunner.getWebDriver()))
        {
            return;
        }

        final long quietMs = quietPeriod != null ? quietPeriod.toMillis() : DEFAULT_QUIET_PERIOD.toMillis();
        final long maxTimeoutMs = maxTimeout != null ? maxTimeout.toMillis() : DEFAULT_MAX_TIMEOUT.toMillis();

        Duration prevTimeout = null;
        WebDriver driver = null;
        try
        {
            driver = WebDriverRunner.getWebDriver();
            BrowserToolProvider.ensureValidWindowFocus(driver);
            prevTimeout = driver.manage().timeouts().getScriptTimeout();
            driver.manage().timeouts().scriptTimeout(Duration.ofMillis(maxTimeoutMs + 1000));
        }
        catch (final Exception ignored)
        {
        }

        try
        {
            Selenide.executeAsyncJavaScript(WAIT_FOR_QUIET_SCRIPT, quietMs, maxTimeoutMs);
        }
        catch (final Exception e)
        {
            LOGGER.debug("DOM quiescence wait completed or bypassed: {}", e.getMessage());
        }
        finally
        {
            if (driver != null && prevTimeout != null)
            {
                try
                {
                    driver.manage().timeouts().scriptTimeout(prevTimeout);
                }
                catch (final Exception ignored)
                {
                }
            }
        }
    }
}
