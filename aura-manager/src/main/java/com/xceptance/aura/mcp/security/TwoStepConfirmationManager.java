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
package com.xceptance.aura.mcp.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages short-lived confirmation tokens for destructive or disruptive operations (e.g. delete_test, cancel_run).
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Component
public class TwoStepConfirmationManager
{
    private static final Duration DEFAULT_TTL = Duration.ofSeconds(60);

    private final Map<String, TokenEntry> tokenCache = new ConcurrentHashMap<>();

    /**
     * Issues a new confirmation token for a specific action and target resource.
     *
     * @param action the action name (e.g. "delete_test", "cancel_run")
     * @param target the resource identifier or path
     * @return the issued token string
     */
    public String issueToken(final String action, final String target)
    {
        evictExpiredTokens();
        final String token = UUID.randomUUID().toString();
        tokenCache.put(token, new TokenEntry(action, target, Instant.now().plus(DEFAULT_TTL)));
        return token;
    }

    /**
     * Validates and consumes the confirmation token for an action and target.
     * Tokens are single-use only.
     *
     * @param token the confirmation token presented
     * @param action the requested action
     * @param target the target resource
     * @return true if token is valid and unexpired
     */
    public boolean validateAndConsumeToken(final String token, final String action, final String target)
    {
        if (token == null || token.isBlank())
        {
            return false;
        }

        final TokenEntry entry = tokenCache.remove(token);
        if (entry == null)
        {
            return false;
        }

        if (Instant.now().isAfter(entry.expiresAt()))
        {
            return false;
        }

        return entry.action().equalsIgnoreCase(action) && entry.target().equals(target);
    }

    private void evictExpiredTokens()
    {
        final Instant now = Instant.now();
        tokenCache.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }

    private record TokenEntry(String action, String target, Instant expiresAt)
    {
    }
}
