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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Service managing automated browser launching on Neodymium AI Manager server startup.
 * Implements a heartbeat grace period upon {@link ApplicationReadyEvent} to detect
 * if an existing browser tab has already reconnected, avoiding opening duplicate tabs
 * on server restart.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Service
public class BrowserAutoOpenService
{
    private static final Logger LOG = LoggerFactory.getLogger(BrowserAutoOpenService.class);

    static final long DEFAULT_GRACE_PERIOD_MS = 1500L;

    private static volatile boolean autoOpenRequested = false;

    private final AtomicBoolean clientConnected = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        final Thread t = new Thread(r, "aura-browser-auto-open");
        t.setDaemon(true);
        return t;
    });

    @Value("${server.port:8080}")
    private int serverPort = 8080;

    /**
     * Enables automated browser opening for this JVM session.
     * Invoked exclusively by {@link AiLauncher#main(String[])}.
     *
     * @param requested true if browser auto-open should be evaluated on startup
     */
    public static void setAutoOpenRequested(final boolean requested)
    {
        autoOpenRequested = requested;
    }

    public static boolean isAutoOpenRequested()
    {
        return autoOpenRequested;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(final ApplicationReadyEvent event)
    {
        final Integer localPort = event.getApplicationContext().getEnvironment().getProperty("local.server.port", Integer.class);
        if (localPort != null)
        {
            this.serverPort = localPort;
        }

        if (!autoOpenRequested || !AiLauncher.isBrowserOpenEnabled())
        {
            return;
        }

        // When Spring Boot DevTools restart triggers, do not re-evaluate or open secondary tabs
        if ("restartedMain".equals(Thread.currentThread().getName()))
        {
            return;
        }

        final long gracePeriodMs = getGracePeriodMs();
        LOG.debug("Starting browser launch grace window of {} ms to detect existing tabs...", gracePeriodMs);

        scheduler.schedule(() -> {
            try
            {
                if (clientConnected.get())
                {
                    LOG.info("[Neodymium AI Manager] Active browser tab reconnected. Suppressing duplicate browser window.");
                }
                else
                {
                    final String dashboardUrl = "http://localhost:" + serverPort;
                    LOG.info("[Neodymium AI Manager] No active browser session detected. Opening dashboard at {}...", dashboardUrl);
                    AiLauncher.openBrowser(dashboardUrl);
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to complete automated browser launch: {}", e.getMessage(), e);
            }
        }, gracePeriodMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Records that an active client HTTP request (heartbeat, status poll, or UI navigation)
     * was received by the server.
     */
    public void recordClientActivity()
    {
        clientConnected.set(true);
    }

    public boolean isClientConnected()
    {
        return clientConnected.get();
    }

    public int getServerPort()
    {
        return serverPort;
    }

    void setServerPort(final int serverPort)
    {
        this.serverPort = serverPort;
    }

    static long getGracePeriodMs()
    {
        final String custom = System.getProperty("neodymium.ai.browserGracePeriodMs");
        if (custom != null)
        {
            try
            {
                return Long.parseLong(custom);
            }
            catch (final NumberFormatException ignored)
            {
            }
        }
        return DEFAULT_GRACE_PERIOD_MS;
    }
}
