package com.xceptance.aura.mcp.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TwoStepConfirmationManager handshake, validation, and expiration.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class TwoStepConfirmationManagerTest
{
    private TwoStepConfirmationManager manager;

    @BeforeEach
    public void setUp()
    {
        manager = new TwoStepConfirmationManager();
    }

    @Test
    public void testIssueAndConsumeTokenSuccess()
    {
        final String token = manager.issueToken("delete_test", "tests/Obsolete.yaml");
        Assertions.assertNotNull(token);
        Assertions.assertFalse(token.isBlank());

        // Token consumed on valid verification
        final boolean firstConsume = manager.validateAndConsumeToken(token, "delete_test", "tests/Obsolete.yaml");
        Assertions.assertTrue(firstConsume);

        // Cannot be replayed
        final boolean replayConsume = manager.validateAndConsumeToken(token, "delete_test", "tests/Obsolete.yaml");
        Assertions.assertFalse(replayConsume);
    }

    @Test
    public void testMismatchOperationOrTargetFails()
    {
        final String token = manager.issueToken("delete_test", "tests/FileA.yaml");

        // Wrong action
        Assertions.assertFalse(manager.validateAndConsumeToken(token, "cancel_run", "tests/FileA.yaml"));

        // Wrong target
        Assertions.assertFalse(manager.validateAndConsumeToken(token, "delete_test", "tests/FileB.yaml"));
    }

    @Test
    public void testNullOrInvalidTokenFails()
    {
        Assertions.assertFalse(manager.validateAndConsumeToken(null, "delete_test", "test.yaml"));
        Assertions.assertFalse(manager.validateAndConsumeToken("", "delete_test", "test.yaml"));
        Assertions.assertFalse(manager.validateAndConsumeToken("non-existent-token", "delete_test", "test.yaml"));
    }
}
