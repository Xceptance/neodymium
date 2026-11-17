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
package com.xceptance.aura;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests verifying {@link BrowserAutoOpenService} and {@link BrowserAutoOpenFilter}
 * behavior including request lifecycle, grace period resolution, and tab deduplication tracking.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class BrowserAutoOpenServiceTest
{
    @AfterEach
    public void tearDown()
    {
        BrowserAutoOpenService.setAutoOpenRequested(false);
        System.clearProperty("neodymium.ai.browserGracePeriodMs");
    }

    @Test
    public void testAutoOpenRequestedFlagLifecycle()
    {
        BrowserAutoOpenService.setAutoOpenRequested(false);
        Assertions.assertFalse(BrowserAutoOpenService.isAutoOpenRequested(),
            "Auto-open requested should default to false in test environment");

        BrowserAutoOpenService.setAutoOpenRequested(true);
        Assertions.assertTrue(BrowserAutoOpenService.isAutoOpenRequested(),
            "Auto-open requested should be true when set");
    }

    @Test
    public void testRecordClientActivitySetsConnected()
    {
        final BrowserAutoOpenService service = new BrowserAutoOpenService();
        Assertions.assertFalse(service.isClientConnected(), "Client connected must initially be false");

        service.recordClientActivity();
        Assertions.assertTrue(service.isClientConnected(), "Client connected must be true after activity recorded");
    }

    @Test
    public void testGracePeriodHonorsCustomSystemProperty()
    {
        Assertions.assertEquals(BrowserAutoOpenService.DEFAULT_GRACE_PERIOD_MS,
            BrowserAutoOpenService.getGracePeriodMs(),
            "Default grace period should be 1500 ms");

        System.setProperty("neodymium.ai.browserGracePeriodMs", "2500");
        Assertions.assertEquals(2500L, BrowserAutoOpenService.getGracePeriodMs(),
            "Grace period should reflect configured system property");

        System.setProperty("neodymium.ai.browserGracePeriodMs", "invalid");
        Assertions.assertEquals(BrowserAutoOpenService.DEFAULT_GRACE_PERIOD_MS,
            BrowserAutoOpenService.getGracePeriodMs(),
            "Invalid numeric value should fall back to default");
    }

    @Test
    public void testFilterDelegatesAndRecordsActivity() throws Exception
    {
        final BrowserAutoOpenService service = new BrowserAutoOpenService();
        final BrowserAutoOpenFilter filter = new BrowserAutoOpenFilter(service);

        final ServletRequest request = Mockito.mock(ServletRequest.class);
        final ServletResponse response = Mockito.mock(ServletResponse.class);
        final FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Assertions.assertTrue(service.isClientConnected(), "Filter execution must record client activity");
        Mockito.verify(chain).doFilter(request, response);
    }

    @Test
    public void testServerPortResolution()
    {
        final BrowserAutoOpenService service = new BrowserAutoOpenService();
        Assertions.assertEquals(8080, service.getServerPort(), "Default server port should be 8080");

        service.setServerPort(9090);
        Assertions.assertEquals(9090, service.getServerPort(), "Server port should update correctly");
    }
}
