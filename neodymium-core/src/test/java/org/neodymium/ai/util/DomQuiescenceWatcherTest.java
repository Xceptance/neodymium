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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.Duration;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying {@link DomQuiescenceWatcher} utility contract, safety guards,
 * and lifecycle execution when WebDriver is not active.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
final class DomQuiescenceWatcherTest
{
    private String previousBrowser;

    @AfterEach
    void tearDown()
    {
        if (WebDriverRunner.hasWebDriverStarted())
        {
            Selenide.closeWebDriver();
        }
        if (previousBrowser != null)
        {
            Configuration.browser = previousBrowser;
        }
    }

    @Test
    void testUtilityClassProperties() throws Exception
    {
        final Constructor<DomQuiescenceWatcher> constructor = DomQuiescenceWatcher.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor must be private");

        constructor.setAccessible(true);
        final DomQuiescenceWatcher instance = constructor.newInstance();
        assertTrue(instance instanceof DomQuiescenceWatcher);
    }

    @Test
    void testInstallTrackerWithoutBrowser()
    {
        assertDoesNotThrow(() -> DomQuiescenceWatcher.installTracker());
    }

    @Test
    void testWaitForDomQuietWithoutBrowser()
    {
        assertDoesNotThrow(() -> DomQuiescenceWatcher.waitForDomQuiet());
        assertDoesNotThrow(() -> DomQuiescenceWatcher.waitForDomQuiet(Duration.ofMillis(200), Duration.ofMillis(50)));
        assertDoesNotThrow(() -> DomQuiescenceWatcher.waitForDomQuiet(null, null));
    }

    @Test
    void testBareFetchCallAfterInstallingTracker()
    {
        previousBrowser = Configuration.browser;
        Configuration.browser = "firefox";
        Configuration.headless = true;
        Selenide.open("data:text/html;charset=utf-8,<html><head><title>Test</title></head><body></body></html>");

        DomQuiescenceWatcher.installTracker();

        final String result = Selenide.executeAsyncJavaScript("""
            var done = arguments[arguments.length - 1];
            (async function() {
                try {
                    await fetch('data:text/plain,ok');
                    done('SUCCESS');
                } catch (err) {
                    done('ERROR [' + navigator.userAgent + ']: ' + err.name + ' - ' + err.message);
                }
            })();
            """);

        assertEquals("SUCCESS", result,
                     "Bare fetch() call must succeed after tracker installation");
    }

    @Test
    void testBareFetchCallAfterWaitForDomQuiet()
    {
        previousBrowser = Configuration.browser;
        Configuration.browser = "firefox";
        Configuration.headless = true;
        Selenide.open("data:text/html;charset=utf-8,<html><head><title>Test</title></head><body></body></html>");

        DomQuiescenceWatcher.waitForDomQuiet(Duration.ofMillis(50), Duration.ofMillis(10));

        final String result = Selenide.executeAsyncJavaScript("""
            var done = arguments[arguments.length - 1];
            (async function() {
                try {
                    await fetch('data:text/plain,ok');
                    done('SUCCESS');
                } catch (err) {
                    done('ERROR [' + navigator.userAgent + ']: ' + err.name + ' - ' + err.message);
                }
            })();
            """);

        assertEquals("SUCCESS", result,
                     "Bare fetch() call must succeed after waitForDomQuiet");
    }
}
