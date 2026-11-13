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
 * Unit tests for HostAllowlistValidator SSRF and path traversal prevention.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class HostAllowlistValidatorTest
{
    private HostAllowlistValidator validator;

    @BeforeEach
    public void setUp()
    {
        validator = new HostAllowlistValidator();
    }

    @Test
    public void testIsSafeTargetUrl()
    {
        Assertions.assertTrue(validator.isSafeTargetUrl("https://example.com/test"));
        Assertions.assertTrue(validator.isSafeTargetUrl("http://localhost:8080/app"));

        // Cloud metadata endpoints blocked
        Assertions.assertFalse(validator.isSafeTargetUrl("http://169.254.169.254/latest/meta-data"));
        Assertions.assertFalse(validator.isSafeTargetUrl("http://metadata.google.internal/computeMetadata/v1"));
        Assertions.assertFalse(validator.isSafeTargetUrl("http://instance-data/latest"));

        // Invalid inputs
        Assertions.assertFalse(validator.isSafeTargetUrl(null));
        Assertions.assertFalse(validator.isSafeTargetUrl(""));
        Assertions.assertFalse(validator.isSafeTargetUrl("not-a-valid-uri"));
    }

    @Test
    public void testIsSafeTestPath()
    {
        Assertions.assertTrue(validator.isSafeTestPath("Login.yaml"));
        Assertions.assertTrue(validator.isSafeTestPath("tests/checkout/Payment.yaml"));
        Assertions.assertTrue(validator.isSafeTestPath("tests\\login\\User.yaml"));

        // Path traversal attacks
        Assertions.assertFalse(validator.isSafeTestPath("../secret.yaml"));
        Assertions.assertFalse(validator.isSafeTestPath("tests/../../etc/passwd"));
        Assertions.assertFalse(validator.isSafeTestPath("..\\Windows\\System32"));

        // Absolute paths
        Assertions.assertFalse(validator.isSafeTestPath("/etc/shadow"));
        Assertions.assertFalse(validator.isSafeTestPath("C:\\Windows\\win.ini"));

        // Non-YAML extensions
        Assertions.assertFalse(validator.isSafeTestPath("script.sh"));
        Assertions.assertFalse(validator.isSafeTestPath("payload.exe"));
        Assertions.assertFalse(validator.isSafeTestPath(null));
        Assertions.assertFalse(validator.isSafeTestPath(""));
    }
}
