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
package org.neodymium.ai.server.auraglance;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Encapsulates mutable simulation state (artificial latency, injected faults, request counters)
 * for Aura Glance test sessions.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class AuraGlanceState
{
    private volatile int artificialDelayMs = 0;
    private volatile boolean simulateServerFailure = false;
    private volatile String lastRequestedUri = "";
    private final AtomicInteger requestCount = new AtomicInteger(0);

    public AuraGlanceState()
    {
        reset();
    }

    /**
     * Resets the simulation state to clean defaults.
     */
    public void reset()
    {
        this.artificialDelayMs = 0;
        this.simulateServerFailure = false;
        this.lastRequestedUri = "";
        this.requestCount.set(0);
    }

    public int getArtificialDelayMs()
    {
        return artificialDelayMs;
    }

    public void setArtificialDelayMs(final int artificialDelayMs)
    {
        this.artificialDelayMs = Math.max(0, artificialDelayMs);
    }

    public boolean isSimulateServerFailure()
    {
        return simulateServerFailure;
    }

    public void setSimulateServerFailure(final boolean simulateServerFailure)
    {
        this.simulateServerFailure = simulateServerFailure;
    }

    public String getLastRequestedUri()
    {
        return lastRequestedUri;
    }

    public void setLastRequestedUri(final String lastRequestedUri)
    {
        this.lastRequestedUri = lastRequestedUri != null ? lastRequestedUri : "";
    }

    public int incrementAndGetRequestCount()
    {
        return this.requestCount.incrementAndGet();
    }

    public int getRequestCount()
    {
        return this.requestCount.get();
    }
}
