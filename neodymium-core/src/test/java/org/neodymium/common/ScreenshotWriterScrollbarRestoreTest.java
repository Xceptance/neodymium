/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.common;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.HasCdp;
import org.openqa.selenium.devtools.Command;
import org.openqa.selenium.devtools.Connection;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.idealized.Domains;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.WebSocket;

/**
 * Unit tests for viewport scrollbar restoration around beyond-viewport CDP screenshots in
 * {@link ScreenshotWriter}.
 *
 * @author AI-generated: Gemini
 * @author Xceptance GmbH 2026
 */
public class ScreenshotWriterScrollbarRestoreTest
{
    private static final String MINIMAL_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    private interface CdpJsDriver extends WebDriver, HasCdp, JavascriptExecutor
    {
    }

    private interface DevToolsJsDriver extends WebDriver, HasDevTools, JavascriptExecutor
    {
    }

    private record DriverScenario(
            long fullWidth,
            long fullHeight,
            long viewWidth,
            long viewHeight,
            List<Long> scrollbarDims,
            long scrollX,
            long scrollY,
            boolean throwOnCdpCapture)
    {
    }

    /**
     * Verifies that {@link ScreenshotWriter#takeScreenshotWithCDP} measures scrollbars before capture
     * and restores them before restoring the original scroll offset when content exceeds the viewport.
     */
    @Test
    public void testTakeScreenshotWithCdpRestoresScrollbarsBeforeScrollOffsetWhenExceedingViewport()
    {
        final List<String> events = new ArrayList<>();
        final DriverScenario scenario = new DriverScenario(1500L, 2500L, 1500L, 857L, List.of(15L, 0L), 0L, 420L, false);
        final CdpJsDriver driver = createCdpDriver(scenario, events);

        final Optional<byte[]> result = ScreenshotWriter.takeScreenshotWithCDP(driver, OutputType.BYTES);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(
                List.of(
                        "measureScrollbars",
                        "scrollToTop",
                        "cdp:Page.captureScreenshot:true",
                        "restoreScrollbars:15,0",
                        "scrollToOriginal:0,420"),
                events);
    }

    /**
     * Verifies that {@link ScreenshotWriter#takeScreenshotWithCDP} still restores scrollbars and
     * original scroll offset in its {@code finally} block if {@code Page.captureScreenshot} throws.
     */
    @Test
    public void testTakeScreenshotWithCdpRestoresScrollbarsWhenCaptureThrows()
    {
        final List<String> events = new ArrayList<>();
        final DriverScenario scenario = new DriverScenario(1500L, 2500L, 1500L, 857L, List.of(15L, 0L), 0L, 300L, true);
        final CdpJsDriver driver = createCdpDriver(scenario, events);

        Assertions.assertThrows(IllegalStateException.class,
                () -> ScreenshotWriter.takeScreenshotWithCDP(driver, OutputType.BYTES));

        Assertions.assertEquals(
                List.of(
                        "measureScrollbars",
                        "scrollToTop",
                        "cdp:Page.captureScreenshot:true",
                        "restoreScrollbars:15,0",
                        "scrollToOriginal:0,300"),
                events);
    }

    /**
     * Verifies that {@link ScreenshotWriter#takeScreenshotWithCDP} skips both scrollbar measurement
     * and scrollbar restoration when document dimensions fit within the viewport.
     */
    @Test
    public void testTakeScreenshotWithCdpSkipsScrollbarRestoreWhenWithinViewport()
    {
        final List<String> events = new ArrayList<>();
        final DriverScenario scenario = new DriverScenario(1500L, 857L, 1500L, 857L, List.of(15L, 0L), 0L, 0L, false);
        final CdpJsDriver driver = createCdpDriver(scenario, events);

        final Optional<byte[]> result = ScreenshotWriter.takeScreenshotWithCDP(driver, OutputType.BYTES);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(List.of("cdp:Page.captureScreenshot:false"), events);
    }

    /**
     * Verifies that {@link ScreenshotWriter#takeScreenshotWithCDP} skips scrollbar restoration when
     * no viewport scrollbars existed prior to capture.
     */
    @Test
    public void testTakeScreenshotWithCdpSkipsScrollbarRestoreWhenNoScrollbarsInitially()
    {
        final List<String> events = new ArrayList<>();
        final DriverScenario scenario = new DriverScenario(1500L, 2500L, 1500L, 857L, List.of(0L, 0L), 0L, 120L, false);
        final CdpJsDriver driver = createCdpDriver(scenario, events);

        final Optional<byte[]> result = ScreenshotWriter.takeScreenshotWithCDP(driver, OutputType.BYTES);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(
                List.of(
                        "measureScrollbars",
                        "scrollToTop",
                        "cdp:Page.captureScreenshot:true",
                        "scrollToOriginal:0,120"),
                events);
    }

    /**
     * Verifies that the DevTools overload {@link ScreenshotWriter#takeScreenshot} restores viewport
     * scrollbars before restoring scroll offset when capturing beyond the viewport.
     */
    @Test
    public void testTakeScreenshotWithDevToolsRestoresScrollbarsBeforeScrollOffsetWhenExceedingViewport()
    {
        final List<String> events = new ArrayList<>();
        final DriverScenario scenario = new DriverScenario(1500L, 2500L, 1500L, 857L, List.of(15L, 0L), 0L, 250L, false);
        final DevTools stubDevTools = createStubDevTools(events);
        final DevToolsJsDriver driver = createDevToolsDriver(scenario, stubDevTools, events);

        final Optional<byte[]> result = ScreenshotWriter.takeScreenshot(driver, OutputType.BYTES);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(
                List.of(
                        "devTools:createSession:main-window",
                        "measureScrollbars",
                        "scrollToTop",
                        "devTools:send:Page.captureScreenshot",
                        "restoreScrollbars:15,0",
                        "scrollToOriginal:0,250"),
                events);
    }

    private static CdpJsDriver createCdpDriver(final DriverScenario scenario, final List<String> events)
    {
        return (CdpJsDriver) Proxy.newProxyInstance(
                ScreenshotWriterScrollbarRestoreTest.class.getClassLoader(),
                new Class<?>[] { CdpJsDriver.class },
                (proxy, method, args) -> handleDriverCall(scenario, null, events, method.getName(), args));
    }

    private static DevToolsJsDriver createDevToolsDriver(
            final DriverScenario scenario,
            final DevTools devTools,
            final List<String> events)
    {
        return (DevToolsJsDriver) Proxy.newProxyInstance(
                ScreenshotWriterScrollbarRestoreTest.class.getClassLoader(),
                new Class<?>[] { DevToolsJsDriver.class },
                (proxy, method, args) -> handleDriverCall(scenario, devTools, events, method.getName(), args));
    }

    private static DevTools createStubDevTools(final List<String> events)
    {
        final ClassLoader loader = ScreenshotWriterScrollbarRestoreTest.class.getClassLoader();
        final WebSocket dummySocket = (WebSocket) Proxy.newProxyInstance(
                loader,
                new Class<?>[] { WebSocket.class },
                (proxy, method, args) -> null);
        final HttpClient dummyHttpClient = (HttpClient) Proxy.newProxyInstance(
                loader,
                new Class<?>[] { HttpClient.class },
                (proxy, method, args) -> "openSocket".equals(method.getName()) ? dummySocket : null);
        final Domains dummyDomains = (Domains) Proxy.newProxyInstance(
                loader,
                new Class<?>[] { Domains.class },
                (proxy, method, args) -> null);
        final Connection dummyConnection = new Connection(dummyHttpClient, "http://localhost:9222");

        return new DevTools(dt -> dummyDomains, dummyConnection)
        {
            @Override
            public void createSessionIfThereIsNotOne(final String windowHandle)
            {
                events.add("devTools:createSession:" + windowHandle);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <X> X send(final Command<X> command)
            {
                events.add("devTools:send:" + command.getMethod());
                return (X) MINIMAL_PNG_BASE64;
            }
        };
    }

    private static Object handleDriverCall(
            final DriverScenario scenario,
            final DevTools devTools,
            final List<String> events,
            final String methodName,
            final Object[] args)
    {
        if ("getDevTools".equals(methodName))
        {
            return devTools;
        }
        if ("maybeGetDevTools".equals(methodName))
        {
            return Optional.ofNullable(devTools);
        }
        if ("getWindowHandle".equals(methodName))
        {
            return "main-window";
        }
        if ("executeCdpCommand".equals(methodName))
        {
            final String commandName = String.valueOf(args[0]);
            final Map<?, ?> params = (Map<?, ?>) args[1];
            events.add("cdp:" + commandName + ":" + params.get("captureBeyondViewport"));
            if (scenario.throwOnCdpCapture())
            {
                throw new IllegalStateException("Simulated CDP capture failure");
            }
            return Map.of("data", MINIMAL_PNG_BASE64);
        }
        if ("executeScript".equals(methodName))
        {
            final String script = String.valueOf(args[0]);
            final Object[] scriptArgs = args.length > 1 && args[1] instanceof final Object[] varargs
                    ? varargs
                    : new Object[0];
            return handleScriptCall(scenario, events, script, scriptArgs);
        }
        return null;
    }

    private static Object handleScriptCall(
            final DriverScenario scenario,
            final List<String> events,
            final String script,
            final Object[] scriptArgs)
    {
        if (script.contains("scrollWidth"))
        {
            return scenario.fullWidth();
        }
        if (script.contains("scrollHeight"))
        {
            return scenario.fullHeight();
        }
        if ("return window.innerWidth".equals(script))
        {
            return scenario.viewWidth();
        }
        if ("return window.innerHeight".equals(script))
        {
            return scenario.viewHeight();
        }
        if (script.contains("overflow-x"))
        {
            events.add("restoreScrollbars:" + scriptArgs[0] + "," + scriptArgs[1]);
            return null;
        }
        if (script.contains("window.innerWidth - t.clientWidth"))
        {
            events.add("measureScrollbars");
            return scenario.scrollbarDims();
        }
        if (script.contains("window.scrollX"))
        {
            return scenario.scrollX();
        }
        if (script.contains("window.scrollY"))
        {
            return scenario.scrollY();
        }
        if ("window.scrollTo(0, 0)".equals(script))
        {
            events.add("scrollToTop");
            return null;
        }
        if (script.startsWith("window.scrollTo(arguments[0], arguments[1])"))
        {
            events.add("scrollToOriginal:" + scriptArgs[0] + "," + scriptArgs[1]);
            return null;
        }
        return null;
    }
}
