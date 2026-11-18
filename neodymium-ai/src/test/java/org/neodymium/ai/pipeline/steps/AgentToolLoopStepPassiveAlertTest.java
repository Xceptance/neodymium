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
package org.neodymium.ai.pipeline.steps;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.neodymium.ai.monitor.ConsoleLogMonitor;
import org.neodymium.ai.monitor.NetworkTrafficMonitor;

/**
 * Unit tests verifying that passive diagnostic alerts are generated when actions fail
 * in presence of buffered HTTP 5xx or unhandled JS exceptions, and suppressed when clean.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class AgentToolLoopStepPassiveAlertTest
{
    @Test
    public void testAlertSuppressedWhenBuffersAreClean()
    {
        final NetworkTrafficMonitor netMonitor = new NetworkTrafficMonitor(10, null);
        final ConsoleLogMonitor consoleMonitor = new ConsoleLogMonitor(10);

        final String alert = AgentToolLoopStep.generatePassiveDiagnosticAlert(netMonitor, consoleMonitor);
        assertNull(alert, "Alert must be suppressed when buffers are clean");
    }

    @Test
    public void testAlertSuppressedOnHttp404Only()
    {
        final NetworkTrafficMonitor netMonitor = new NetworkTrafficMonitor(10, null);
        final ConsoleLogMonitor consoleMonitor = new ConsoleLogMonitor(10);

        netMonitor.recordFailure("https://example.com/api/missing", 404, "GET", "Not Found");
        consoleMonitor.recordEntry("INFO", "Loaded page", "app.js", 1, null);

        final String alert = AgentToolLoopStep.generatePassiveDiagnosticAlert(netMonitor, consoleMonitor);
        assertNull(alert, "Alert must not fire for 404 client errors or INFO logs");
    }

    @Test
    public void testAlertGeneratedOnHttp500()
    {
        final NetworkTrafficMonitor netMonitor = new NetworkTrafficMonitor(10, null);
        final ConsoleLogMonitor consoleMonitor = new ConsoleLogMonitor(10);

        netMonitor.recordFailure("https://example.com/api/cart/add", 500, "POST", "Internal Server Error");

        final String alert = AgentToolLoopStep.generatePassiveDiagnosticAlert(netMonitor, consoleMonitor);
        assertNotNull(alert);
        assertTrue(alert.contains("[Diagnostic Alert]"));
        assertTrue(alert.contains("HTTP 500"));
        assertTrue(alert.contains("/api/cart/add"));
    }

    @Test
    public void testAlertGeneratedOnSevereJsException()
    {
        final NetworkTrafficMonitor netMonitor = new NetworkTrafficMonitor(10, null);
        final ConsoleLogMonitor consoleMonitor = new ConsoleLogMonitor(10);

        consoleMonitor.recordException(
            "TypeError: Cannot read properties of undefined",
            "checkout.js",
            55,
            "at render (checkout.js:55:10)"
        );

        final String alert = AgentToolLoopStep.generatePassiveDiagnosticAlert(netMonitor, consoleMonitor);
        assertNotNull(alert);
        assertTrue(alert.contains("[Diagnostic Alert]"));
        assertTrue(alert.contains("TypeError: Cannot read properties of undefined"));
        assertTrue(alert.contains("checkout.js:55"));
    }

    @Test
    public void testAlertGeneratedForBothNetworkAndConsole()
    {
        final NetworkTrafficMonitor netMonitor = new NetworkTrafficMonitor(10, null);
        final ConsoleLogMonitor consoleMonitor = new ConsoleLogMonitor(10);

        netMonitor.recordFailure("https://example.com/api/order", 500, "POST", "Fatal Error");
        consoleMonitor.recordException("ReferenceError: OrderEngine is not defined", "order.js", 12, null);

        final String alert = AgentToolLoopStep.generatePassiveDiagnosticAlert(netMonitor, consoleMonitor);
        assertNotNull(alert);
        assertTrue(alert.contains("HTTP 500"));
        assertTrue(alert.contains("ReferenceError"));
    }
}
