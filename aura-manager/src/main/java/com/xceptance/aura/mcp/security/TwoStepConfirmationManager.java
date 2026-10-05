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
