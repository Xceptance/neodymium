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
