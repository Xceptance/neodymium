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
package org.neodymium.ai.server.session;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages test sessions and provides opaque, anti-leak token mappings
 * so that LLMs cannot infer test automation metadata from browser cookies.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class SessionManager
{
    private static final Logger LOG = LoggerFactory.getLogger(SessionManager.class);

    public static final String DEFAULT_SESSION_ID = "default";
    public static final String COOKIE_NAME = "_v_sid";

    private final Map<String, TestSession> sessionsByCustomId = new ConcurrentHashMap<>();
    private final Map<String, String> customIdByOpaqueToken = new ConcurrentHashMap<>();
    private final Map<String, String> opaqueTokenByCustomId = new ConcurrentHashMap<>();

    public SessionManager()
    {
        getOrCreateSession(DEFAULT_SESSION_ID);
    }

    public TestSession getDefaultSession()
    {
        return getOrCreateSession(DEFAULT_SESSION_ID);
    }

    public TestSession getOrCreateSession(final String customSessionId)
    {
        final String resolvedId = (customSessionId == null || customSessionId.isBlank())
            ? DEFAULT_SESSION_ID
            : customSessionId.trim();

        final TestSession session = sessionsByCustomId.computeIfAbsent(resolvedId, TestSession::new);
        session.touch();
        return session;
    }

    public TestSession getSession(final String customSessionId)
    {
        if (customSessionId == null || customSessionId.isBlank())
        {
            return getDefaultSession();
        }
        return sessionsByCustomId.get(customSessionId.trim());
    }

    public TestSession resolveSession(final String opaqueToken)
    {
        if (opaqueToken != null)
        {
            final String customId = customIdByOpaqueToken.get(opaqueToken.trim());
            if (customId != null)
            {
                final TestSession session = sessionsByCustomId.get(customId);
                if (session != null)
                {
                    session.touch();
                    return session;
                }
            }
        }
        return getDefaultSession();
    }

    public String getOrCreateOpaqueToken(final String customSessionId)
    {
        final String resolvedId = (customSessionId == null || customSessionId.isBlank())
            ? DEFAULT_SESSION_ID
            : customSessionId.trim();

        getOrCreateSession(resolvedId);

        return opaqueTokenByCustomId.computeIfAbsent(resolvedId, k ->
        {
            final String token = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            customIdByOpaqueToken.put(token, resolvedId);
            LOG.debug("Created opaque token for session '{}'", resolvedId);
            return token;
        });
    }

    public boolean removeSession(final String customSessionId)
    {
        if (customSessionId == null || DEFAULT_SESSION_ID.equals(customSessionId))
        {
            return false;
        }
        final String trimmed = customSessionId.trim();
        final TestSession removed = sessionsByCustomId.remove(trimmed);
        final String token = opaqueTokenByCustomId.remove(trimmed);
        if (token != null)
        {
            customIdByOpaqueToken.remove(token);
        }
        LOG.debug("Removed test session '{}' (existed={})", trimmed, removed != null);
        return removed != null;
    }

    public void reapIdleSessions(final long maxIdleMs)
    {
        final long now = System.currentTimeMillis();
        for (final Map.Entry<String, TestSession> entry : sessionsByCustomId.entrySet())
        {
            final String id = entry.getKey();
            if (DEFAULT_SESSION_ID.equals(id))
            {
                continue;
            }
            final TestSession session = entry.getValue();
            if (now - session.getLastAccessedAt() > maxIdleMs)
            {
                removeSession(id);
                LOG.info("Reaped idle test session '{}' (idle > {}ms)", id, maxIdleMs);
            }
        }
    }

    public void resetAllSessions()
    {
        for (final TestSession session : sessionsByCustomId.values())
        {
            session.resetAll();
        }
        LOG.info("Reset all {} active test sessions.", sessionsByCustomId.size());
    }

    public int getActiveSessionCount()
    {
        return sessionsByCustomId.size();
    }
}
