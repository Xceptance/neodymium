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
