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
package com.xceptance.aura.mcp.session;

import com.xceptance.aura.mcp.config.AuraMcpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages MCP connection sessions and monitors transport keep-alive heartbeats and idle timeouts.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Component
public class McpSubscriptionManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(McpSubscriptionManager.class);

    private final AuraMcpProperties properties;

    private final Map<String, Instant> activeSessions = new ConcurrentHashMap<>();

    public McpSubscriptionManager(final AuraMcpProperties properties)
    {
        this.properties = properties;
    }

    /**
     * Registers or touches an active MCP session.
     *
     * @param sessionId the session identifier
     */
    public void recordActivity(final String sessionId)
    {
        if (sessionId != null && !sessionId.isBlank())
        {
            activeSessions.put(sessionId, Instant.now());
        }
    }

    /**
     * Removes an active MCP session upon termination.
     *
     * @param sessionId the session identifier
     */
    public void removeSession(final String sessionId)
    {
        if (sessionId != null)
        {
            activeSessions.remove(sessionId);
        }
    }

    /**
     * Periodically inspects and evicts idle sessions based on sessionTimeoutSeconds.
     */
    @Scheduled(fixedRate = 60000)
    public void sweepExpiredSessions()
    {
        final int timeoutSec = properties.getSessionTimeoutSeconds();
        if (timeoutSec <= 0)
        {
            return;
        }

        final Instant cutoff = Instant.now().minusSeconds(timeoutSec);
        activeSessions.entrySet().removeIf(entry -> {
            final boolean expired = entry.getValue().isBefore(cutoff);
            if (expired)
            {
                LOGGER.info("[Aura MCP] Evicted idle session '{}' (exceeded {}s timeout)", entry.getKey(), timeoutSec);
            }
            return expired;
        });
    }

    public int getActiveSessionCount()
    {
        return activeSessions.size();
    }

    /**
     * Returns an unmodifiable snapshot of active sessions and their last active timestamps.
     *
     * @return map of session IDs to last active Instant
     */
    public Map<String, Instant> getSessionDetails()
    {
        return Map.copyOf(activeSessions);
    }
}
