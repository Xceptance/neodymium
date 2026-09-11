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
        System.clearProperty("neodymium.ai.interactive");
        System.clearProperty("neodymium.managerActive");
        System.clearProperty("neodymium.ai.consoleExecutionLogs.directory");
        System.clearProperty("neodymium.ai.consoleLog.directory");
        System.clearProperty("neodymium.ai.report.disk.directory");
        System.clearProperty("neodymium.ai.reportDirectory");
        System.clearProperty("neodymium.ai.replay.delayScale");
        System.clearProperty("neodymium.ai.replay.useRecordedDelays");
    }

    @Test
    public void testIsInteractiveAndIsManagerActiveDefaultsAndOverrides()
    {
        System.setProperty("neodymium.ai.interactive", "false");
        System.setProperty("neodymium.managerActive", "false");
        AiConfiguration.resetInstance();

        final AiConfiguration falseConfig = AiConfiguration.getInstance();
        assertFalse(falseConfig.isInteractive(), "Interactive mode should be false when set to false.");
        assertFalse(falseConfig.isManagerActive(), "Manager active should be false when set to false.");

        System.setProperty("neodymium.ai.interactive", "true");
        System.setProperty("neodymium.managerActive", "true");
        AiConfiguration.resetInstance();

        final AiConfiguration trueConfig = AiConfiguration.getInstance();
        assertTrue(trueConfig.isInteractive(), "Interactive mode should resolve true when property is set to true.");
        assertTrue(trueConfig.isManagerActive(), "Manager active should resolve true when property is set to true.");
    }

    @Test
    public void testVisualRcaEnabledDefaultAndOverride()
    {
        System.setProperty("neodymium.ai.visualRca.enabled", "true");
        AiConfiguration.resetInstance();
        assertTrue(AiConfiguration.getInstance().isVisualRcaEnabled(), "Visual RCA should be enabled when system property is set to true.");

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
    public void testDefaultExecutionModeIsLlmRecording()
    {
        System.clearProperty("neodymium.ai.executionMode");
        System.clearProperty("neodymium.ai.execution.mode");

        AiConfiguration.resetInstance();
        final AiConfiguration config = AiConfiguration.getInstance();
        assertEquals(ExecutionMode.LLM_RECORDING, config.getExecutionMode(), "Default execution mode should be LLM_RECORDING.");
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

    @Test
    public void testPlaceholderSubstitution()
    {
        System.setProperty("MY_TEST_SECRET", "super-secret-key-123");
        System.setProperty("neodymium.ai.testProp", "Prefix-${MY_TEST_SECRET}-Suffix");

        AiConfiguration.resetInstance();
        final AiConfiguration config = AiConfiguration.getInstance();
        assertEquals("Prefix-super-secret-key-123-Suffix", config.getProperty("neodymium.ai.testProp", null),
            "Placeholders in property values should be dynamically substituted.");

        System.clearProperty("MY_TEST_SECRET");
        System.clearProperty("neodymium.ai.testProp");
    }

    @Test
    public void testPesapEnabledAndMaxRetriesAtMaxLevel()
    {
        System.clearProperty("neodymium.ai.pesap.enabled");
        System.clearProperty("neodymium.ai.maxRetriesAtMaxLevel");
        AiConfiguration.resetInstance();

        final AiConfiguration defaultConfig = AiConfiguration.getInstance();
        assertTrue(defaultConfig.isPesapEnabled(), "PESAP should be enabled by default.");
        assertEquals(1, defaultConfig.getMaxRetriesAtMaxLevel(), "Max retries at max level should default to 1.");

        System.setProperty("neodymium.ai.pesap.enabled", "false");
        System.setProperty("neodymium.ai.maxRetriesAtMaxLevel", "3");
        AiConfiguration.resetInstance();

        final AiConfiguration customConfig = AiConfiguration.getInstance();
        assertFalse(customConfig.isPesapEnabled(), "PESAP should be false when set to false.");
        assertEquals(3, customConfig.getMaxRetriesAtMaxLevel(), "Max retries at max level should resolve custom value.");

        System.clearProperty("neodymium.ai.pesap.enabled");
        System.clearProperty("neodymium.ai.maxRetriesAtMaxLevel");
    }

    @Test
    public void testConsoleExecutionLogsDirectoryDefaultAndOverride()
    {
        AiConfiguration.resetInstance();
        final AiConfiguration defaultConfig = AiConfiguration.getInstance();
        final String expectedDefault = System.getProperty("allure.results.directory", "target/aura-sandbox/allure-results");
        assertEquals(expectedDefault, defaultConfig.getConsoleExecutionLogsDirectory(),
            "Console execution logs directory should fall back to default path.");

        System.setProperty("neodymium.ai.consoleExecutionLogs.directory", "target/custom-console-logs");
        AiConfiguration.resetInstance();
        final AiConfiguration customConfig = AiConfiguration.getInstance();
        assertEquals("target/custom-console-logs", customConfig.getConsoleExecutionLogsDirectory(),
            "Console execution logs directory should resolve custom configured folder path.");
    }

    @Test
    public void testDiskReportDirectoryDefaultAndOverrides()
    {
        AiConfiguration.resetInstance();
        final AiConfiguration defaultConfig = AiConfiguration.getInstance();
        final String expectedDefault = defaultConfig.getProperty("neodymium.ai.report.disk.directory", "target/ai-reports");
        assertEquals(expectedDefault, defaultConfig.getDiskReportDirectory(),
            "Disk report directory should default to configured default path.");

        System.setProperty("neodymium.ai.report.disk.directory", "target/custom-ai-reports");
        AiConfiguration.resetInstance();
        final AiConfiguration customConfig = AiConfiguration.getInstance();
        assertEquals("target/custom-ai-reports", customConfig.getDiskReportDirectory(),
            "Disk report directory should resolve property override.");
    }

    @Test
    public void testReplayDelayScaleAndUseRecordedDelays()
    {
        AiConfiguration.resetInstance();
        final AiConfiguration defaultConfig = AiConfiguration.getInstance();
        assertEquals(0.0, defaultConfig.getReplayDelayScale(), 0.001, "Delay scale should default to 0.0 (disabled).");
        assertFalse(defaultConfig.isUseRecordedDelays(), "isUseRecordedDelays should default to false.");

        System.setProperty("neodymium.ai.replay.delayScale", "0.5");
        AiConfiguration.resetInstance();
        final AiConfiguration scaledConfig = AiConfiguration.getInstance();
        assertEquals(0.5, scaledConfig.getReplayDelayScale(), 0.001, "Delay scale should resolve 0.5.");
        assertTrue(scaledConfig.isUseRecordedDelays(), "isUseRecordedDelays should be true when delayScale > 0.0.");

        System.setProperty("neodymium.ai.replay.delayScale", "0.0");
        System.setProperty("neodymium.ai.replay.useRecordedDelays", "true");
        AiConfiguration.resetInstance();
        final AiConfiguration legacyConfig = AiConfiguration.getInstance();
        assertTrue(legacyConfig.isUseRecordedDelays(), "isUseRecordedDelays should be true when legacy flag is true.");

        System.clearProperty("neodymium.ai.replay.delayScale");
        System.clearProperty("neodymium.ai.replay.useRecordedDelays");
    }
}

