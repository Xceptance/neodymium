/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package org.neodymium.ai.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AiConfiguration}.
 * Validates property resolution, environment variable camelCase mapping, and instance caching.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class AiConfigurationTest
{
    @BeforeEach
    @AfterEach
    public void resetConfig()
    {
        AiConfiguration.resetInstance();
        System.clearProperty("neodymium.ai.executionMode");
        System.clearProperty("neodymium.ai.timeoutSeconds");
        System.clearProperty("neodymium.ai.execution.mode");
        System.clearProperty("neodymium.ai.timeout.seconds");
        System.clearProperty("neodymium.ai.visualRca.enabled");
    }

    @Test
    public void testVisualRcaEnabledDefaultAndOverride()
    {
        assertTrue(AiConfiguration.getInstance().isVisualRcaEnabled(), "Visual RCA should be enabled by default.");

        System.setProperty("neodymium.ai.visualRca.enabled", "false");
        AiConfiguration.resetInstance();
        assertFalse(AiConfiguration.getInstance().isVisualRcaEnabled(), "Visual RCA should be disabled when property is set to false.");
    }

    @Test
    public void testGetInstanceCaching()
    {
        final AiConfiguration instance1 = AiConfiguration.getInstance();
        final AiConfiguration instance2 = AiConfiguration.getInstance();
        assertNotNull(instance1, "Instance should not be null.");
        assertSame(instance1, instance2, "getInstance() should return cached singleton instance.");
    }

    @Test
    public void testResetInstance()
    {
        final AiConfiguration instance1 = AiConfiguration.getInstance();
        AiConfiguration.resetInstance();
        final AiConfiguration instance2 = AiConfiguration.getInstance();
        assertNotSame(instance1, instance2, "resetInstance() should invalidate cached instance.");
    }

    @Test
    public void testSystemPropertyCamelCaseResolution()
    {
        System.setProperty("neodymium.ai.executionMode", "LLM_ONLY");
        System.setProperty("neodymium.ai.timeoutSeconds", "45");

        AiConfiguration.resetInstance();
        final AiConfiguration config = AiConfiguration.getInstance();
        assertEquals(ExecutionMode.LLM_ONLY, config.getExecutionMode(), "Execution mode should resolve from System property.");
        assertEquals(45, config.getTimeoutSeconds("execution"), "Timeout seconds should resolve from System property.");
    }

    @Test
    public void testNormalizedEnvKeyResolution()
    {
        System.setProperty("neodymium.ai.execution.mode", "LLM_RECORDING");
        System.setProperty("neodymium.ai.timeout.seconds", "60");

        AiConfiguration.resetInstance();
        final AiConfiguration config = AiConfiguration.getInstance();
        assertEquals(ExecutionMode.LLM_RECORDING, config.getExecutionMode(), "Dot-separated execution mode should resolve to getExecutionMode().");
        assertEquals(60, config.getTimeoutSeconds("execution"), "Dot-separated timeout should resolve to getTimeoutSeconds().");
    }
}

