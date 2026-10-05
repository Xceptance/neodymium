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
