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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neodymium.util.Neodymium;
import org.neodymium.util.PropertiesUtil;

/**
 * Hierarchical configuration properties loader for Neodymium AI.
 * Resolves properties from system properties, environment variables,
 * and config properties files. Supports role-specific overrides.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class AiConfiguration
{
    /**
     * Cached shared configuration instance.
     */
    private static volatile AiConfiguration instance;

    /**
     * Map storing loaded hierarchical properties.
     */
    private final Properties properties = new Properties();

    /**
     * Gets the shared, cached AiConfiguration instance.
     *
     * @return the cached configuration instance
     */
    public static AiConfiguration getInstance()
    {
        AiConfiguration result = instance;
        if (result == null)
        {
            synchronized (AiConfiguration.class)
            {
                result = instance;
                if (result == null)
                {
                    instance = result = new AiConfiguration();
                }
            }
        }
        return result;
    }

    /**
     * Resets the cached AiConfiguration instance, forcing a re-read on next access.
     */
    public static void resetInstance()
    {
        synchronized (AiConfiguration.class)
        {
            instance = null;
        }
    }

    /**
     * Constructs a default configuration and loads properties hierarchically.
     */
    public AiConfiguration()
    {
        loadHierarchicalProperties();
    }

    /**
     * Helper to load properties.
     */
    private void loadHierarchicalProperties()
    {
        // 1. Load from file: config/neodymium.properties (if exists)
        loadFromFile("config/neodymium.properties");
        // 2. Load from file: config/ai.properties (if exists)
        loadFromFile("config/ai.properties");
        // 3. Load from file: config/credentials.properties (if exists)
        loadFromFile("config/credentials.properties");
        // 4. Load from dev properties (if exists)
        loadFromFile("config/dev-neodymium.properties");
        
        // 5. Load temporary config file if specified
        final String tempFile = System.getProperty("neodymium.temporaryConfigFile");
        if (tempFile != null)
        {
            loadFromFile(tempFile);
        }

        // 6. Overlay with System Environment variables (matching naming mapping)
        System.getenv().forEach((key, val) -> {
            final String propKey = envToPropKey(key);
            if (propKey.startsWith("neodymium.ai"))
            {
                this.properties.setProperty(propKey, val);
            }
        });


    }

    /**
     * Maps an environment variable name to a lowercase dot-separated property key.
     *
     * @param envKey the environment variable key
     * @return the mapped dot-separated property key
     */
    private String envToPropKey(final String envKey)
    {
        return envKey.toLowerCase().replace('_', '.');
    }

    /**
     * Normalizes a property or environment key by converting to lowercase and stripping dots and underscores.
     *
     * @param key the key string
     * @return normalized alphanumeric key string
     */
    private static String normalizeKey(final String key)
    {
        if (key == null)
        {
            return "";
        }
        return key.toLowerCase().replace(".", "").replace("_", "");
    }

    private final Map<String, String> normalizedFileProps = new java.util.concurrent.ConcurrentHashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(AiConfiguration.class);

    private void rebuildNormalizedCache()
    {
        this.normalizedFileProps.clear();
        for (final String propName : this.properties.stringPropertyNames())
        {
            this.normalizedFileProps.put(normalizeKey(propName), propName);
        }
    }

    /**
     * Loads properties from a file if it exists.
     *
     * @param filePath the file path to load properties from
     */
    private void loadFromFile(final String filePath)
    {
        final File file = new File(filePath);
        if (file.exists() && file.isFile())
        {
            try (final InputStream in = new FileInputStream(file))
            {
                this.properties.load(in);
                rebuildNormalizedCache();
            }
            catch (final IOException e)
            {
                LOG.warn("⚠️ Failed to load configuration properties from file {}: {}", file.getAbsolutePath(), e.getMessage());
            }
        }
    }

    /**
     * Resolves property placeholders formatted as {@code ${VAR_NAME}} against System properties,
     * environment variables, loaded configuration properties, and Neodymium data overrides
     * using {@link PropertiesUtil#substitutePropertyValue(String, Map, java.util.Set)}.
     *
     * @param value the raw property value
     * @return the resolved property value with placeholders replaced
     */
    private String resolvePlaceholders(final String value)
    {
        if (value == null || !value.contains("${"))
        {
            return value;
        }

        final Map<String, String> mergedMap = new HashMap<>();

        // 1. Base properties loaded from configuration files
        for (final String propName : this.properties.stringPropertyNames())
        {
            mergedMap.put(propName, this.properties.getProperty(propName));
        }

        // 2. System Environment variables
        System.getenv().forEach(mergedMap::put);

        // 3. System Properties
        System.getProperties().forEach((k, v) -> mergedMap.put(String.valueOf(k), String.valueOf(v)));

        // 4. Thread-local Neodymium data overrides
        try
        {
            Neodymium.getData().forEach((k, v) -> {
                if (v != null)
                {
                    mergedMap.put(k, String.valueOf(v));
                }
            });
        }
        catch (final Throwable ignored)
        {
        }

        return PropertiesUtil.substitutePropertyValue(value, mergedMap, new HashSet<>());
    }

    /**
     * Gets a configuration value by key, returning the default value if key is not found.
     * Checks thread-local overrides in Neodymium data first.
     *
     * @param key the property key
     * @param defaultValue the default value fallback
     * @return the resolved property value
     */
    public String getProperty(final String key, final String defaultValue)
    {
        try
        {
            final Object threadVal = Neodymium.getData().get(key);
            if (threadVal != null)
            {
                return resolvePlaceholders(String.valueOf(threadVal));
            }
        }
        catch (final Throwable ignored)
        {
            // Fallback in case Neodymium class is not initialized or on classpath
        }

        final String sysProp = System.getProperty(key);
        if (sysProp != null)
        {
            return resolvePlaceholders(sysProp);
        }

        final String exactValue = this.properties.getProperty(key);
        if (exactValue != null)
        {
            return resolvePlaceholders(exactValue);
        }

        final String targetNormalized = normalizeKey(key);
        final String cachedNormalizedName = this.normalizedFileProps.get(targetNormalized);
        if (cachedNormalizedName != null)
        {
            final String cachedVal = this.properties.getProperty(cachedNormalizedName);
            if (cachedVal != null)
            {
                return resolvePlaceholders(cachedVal);
            }
        }

        if (key.startsWith("neodymium"))
        {
            for (final java.util.Map.Entry<Object, Object> sysEntry : System.getProperties().entrySet())
            {
                final String sysKey = String.valueOf(sysEntry.getKey());
                if (sysKey.startsWith("neodymium.ai") && normalizeKey(sysKey).equals(targetNormalized))
                {
                    return resolvePlaceholders(String.valueOf(sysEntry.getValue()));
                }
            }
        }

        return resolvePlaceholders(defaultValue);
    }


    /**
     * Gets a configuration value as an integer.
     *
     * @param key the property key
     * @param defaultValue the default value fallback
     * @return the resolved integer value
     */
    public int getInt(final String key, final int defaultValue)
    {
        final String val = getProperty(key, null);
        if (val != null)
        {
            try
            {
                return Integer.parseInt(val.trim());
            }
            catch (final NumberFormatException e)
            {
                // Fallback
            }
        }
        return defaultValue;
    }

    /**
     * Retrieves a property as a long value, or returns the default value if missing/invalid.
     */
    public long getLong(final String key, final long defaultValue)
    {
        final String val = getProperty(key, null);
        if (val != null)
        {
            try
            {
                return Long.parseLong(val.trim());
            }
            catch (final NumberFormatException e)
            {
                // Fallback
            }
        }
        return defaultValue;
    }

    /**
     * Gets a configuration value as a double.
     *
     * @param key the property key
     * @param defaultValue the default value fallback
     * @return the resolved double value
     */
    public double getDouble(final String key, final double defaultValue)
    {
        final String val = getProperty(key, null);
        if (val == null)
        {
            return defaultValue;
        }
        try
        {
            return Double.parseDouble(val.trim());
        }
        catch (final NumberFormatException e)
        {
            return defaultValue;
        }
    }

    /**
     * Gets a configuration value as a boolean.
     *
     * @param key the property key
     * @param defaultValue the default value fallback
     * @return the resolved boolean value
     */
    public boolean getBoolean(final String key, final boolean defaultValue)
    {
        final String val = getProperty(key, null);
        if (val == null)
        {
            return defaultValue;
        }
        return Boolean.parseBoolean(val.trim());
    }

    /**
     * Resolves model name for a specific role, falling back to global default.
     *
     * @param role the execution role (e.g., "pesap", "execution", "vision", "audit")
     * @return the resolved model name
     */
    public String getModel(final String role)
    {
        final String roleVal = getProperty("neodymium.ai." + role + ".model", null);
        if (roleVal != null)
        {
            return roleVal;
        }
        return getProperty("neodymium.ai.model", "gemini-3.5-flash-lite");
    }

    /**
     * Resolves the default active AI model name for execution.
     *
     * @return the active AI model name
     */
    public String aiModel()
    {
        return getModel("execution");
    }

    /**
     * Resolves API key for a specific role, falling back to global default.
     *
     * @param role the execution role
     * @return the resolved API key
     */
    public String getApiKey(final String role)
    {
        final String roleVal = getProperty("neodymium.ai." + role + ".apiKey", null);
        if (roleVal != null)
        {
            return roleVal;
        }
        return getProperty("neodymium.ai.apiKey", null);
    }

    /**
     * Resolves timeout seconds for a specific role, falling back to global default.
     *
     * @param role the execution role
     * @return the resolved timeout in seconds
     */
    public int getTimeoutSeconds(final String role)
    {
        final int roleVal = getInt("neodymium.ai." + role + ".timeoutSeconds", -1);
        if (roleVal != -1)
        {
            return roleVal;
        }
        return getInt("neodymium.ai.timeoutSeconds", 180);
    }

    /**
     * Resolves temperature for a specific role, falling back to global default.
     *
     * @param role the execution role
     * @return the resolved temperature
     */
    public double getTemperature(final String role)
    {
        final double roleVal = getDouble("neodymium.ai." + role + ".temperature", -1.0);
        if (roleVal >= 0.0)
        {
            return roleVal;
        }
        return getDouble("neodymium.ai.temperature", 0.0);
    }

    /**
     * Resolves the target directory for recorded companion playbook JSON files.
     *
     * @return the recording directory path, or {@code null} if default parent path should be used
     */
    public String playbookRecordingDirectory()
    {
        final String primary = getProperty("neodymium.ai.playbook.recordingDirectory", null);
        if (primary != null && !primary.trim().isEmpty())
        {
            return primary.trim();
        }
        final String fallback = getProperty("neodymium.ai.playbook.recordingDir", null);
        if (fallback != null && !fallback.trim().isEmpty())
        {
            return fallback.trim();
        }
        return null;
    }

    /**
     * Resolves provider identifier for a specific role, falling back to global default.
     *
     * @param role the execution role
     * @return the resolved provider name
     */
    public String getProvider(final String role)
    {
        final String roleVal = getProperty("neodymium.ai." + role + ".provider", null);
        if (roleVal != null)
        {
            return roleVal;
        }
        return getProperty("neodymium.ai.provider", "gemini");
    }

    /**
     * Checks if semantic verification is enabled for pipeline steps outcome validation.
     *
     * @return true if semantic verification is enabled, false otherwise
     */
    public boolean isSemanticVerificationEnabled()
    {
        return getBoolean("neodymium.ai.semanticVerification.enabled", true);
    }

    /**
     * Checks if Visual Root Cause Analysis (RCA) is enabled on step execution failures.
     *
     * @return true if Visual RCA is enabled (default: true), false otherwise
     */
    public boolean isVisualRcaEnabled()
    {
        return getBoolean("neodymium.ai.visualRca.enabled", true);
    }

    /**
     * Checks if the LLM Quality Judge ("second opinion") evaluation step is enabled.
     *
     * @return true if Quality Judge is enabled (default: false), false otherwise
     */
    public boolean isJudgeEnabled()
    {
        return getBoolean("neodymium.ai.judge.enabled", false);
    }

    /**
     * Checks if multilingual prompt guidance is enabled.
     *
     * @return true if neodymium.ai.multilingual is set to true (default: false)
     */
    public boolean isMultilingual()
    {
        return getBoolean("neodymium.ai.multilingual", false);
    }

    /**
     * Checks if replay execution should respect recorded delays and pacing.
     *
     * @return true if neodymium.ai.replay.useRecordedDelays is set to true (default: false)
     */
    public boolean isUseRecordedDelays()
    {
        return getBoolean("neodymium.ai.replay.useRecordedDelays", false);
    }

    /**
     * Returns the speed scaling multiplier applied to recorded delays during replay.
     * For example, 1.0 is real-time, 0.5 is 2x speed, 2.0 is half speed.
     *
     * @return the delay scale factor (default: 1.0)
     */
    public double getReplayDelayScale()
    {
        return getDouble("neodymium.ai.replay.delayScale", 1.0);
    }
    /**
     * Returns the minimum structural similarity index (SSIM) score required for visual assertion matches.
     *
     * @return minimum SSIM score threshold (default: 0.99)
     */
    public double getVisualSsimMinScore()
    {
        return getDouble("neodymium.ai.ssim.minScore", 0.99);
    }

    /**
     * Returns the polling interval in milliseconds between consecutive frame captures during visual stability detection.
     *
     * @return interval in milliseconds (default: 1000L / 1 second)
     */
    public long getVisualStabilityIntervalMs()
    {
        return getLong("neodymium.ai.visual.stabilityIntervalMs", 1000L);
    }

    /**
     * Returns the maximum number of attempts allowed for temporal frame-to-frame visual stability settling.
     *
     * @return maximum settling attempts (default: 5)
     */
    public int getVisualStabilityMaxAttempts()
    {
        return getInt("neodymium.ai.visual.stabilityMaxAttempts", 5);
    }

    /**
     * Returns the minimum inter-frame SSIM score required to consider the SUT visually quiescent/settled.
     *
     * @return stability threshold (default: 0.999)
     */
    public double getVisualStabilityThreshold()
    {
        return getDouble("neodymium.ai.visual.stabilityThreshold", 0.999);
    }

    /**
     * Resolves the execution mode for the LLM Quality Judge.
     * Valid options: "ON_AMBIGUITY" (default), "ALWAYS", "ON_FAIL".
     *
     * @return the resolved judge mode string (uppercase)
     */
    public String getJudgeMode()
    {
        final String mode = getProperty("neodymium.ai.judge.mode", "ON_AMBIGUITY");
        return mode != null ? mode.trim().toUpperCase() : "ON_AMBIGUITY";
    }

    /**
     * Resolves the candidate locators formatting mode.
     * Valid options: "DETAILED" (default) or "COMPACT".
     *
     * @return the locators format mode string (uppercase)
     */
    public String getLocatorsFormat()
    {
        final String format = getProperty("neodymium.ai.action.locators.format", "DETAILED");
        return format != null ? format.trim().toUpperCase() : "DETAILED";
    }

    /**
     * Checks if automatic locator improvement is enabled for recorded/executed actions.
     *
     * @return true if locator improvement is enabled (default: true), false otherwise
     */
    public boolean isLocatorImproverEnabled()
    {
        return getBoolean("neodymium.ai.locatorImprover.enabled", true);
    }

    /**
     * Checks whether interactive console mode is enabled.
     *
     * @return true if neodymium.ai.interactive is set to true
     */
    public boolean isInteractive()
    {
        return getBoolean("neodymium.ai.interactive", false);
    }

    /**
     * Checks whether Aura Manager integration is active.
     *
     * @return true if neodymium.managerActive is set to true
     */
    public boolean isManagerActive()
    {
        return getBoolean("neodymium.managerActive", false);
    }

    /**
     * Checks whether console execution log files (console-execution-*.json) should be produced
     * during test execution. Defaults to true.
     *
     * @return true if console execution log file writing is enabled
     */
    public boolean isConsoleExecutionLogsEnabled()
    {
        return getBoolean("neodymium.ai.consoleExecutionLogs", true);
    }

    /**
     * Gets the active execution mode for the AI pipeline.
     * Defaults to REPLAY_WITH_HEALING.
     *
     * @return the execution mode enum
     */
    public ExecutionMode getExecutionMode()
    {
        final String modeStr = getProperty("neodymium.ai.executionMode", "REPLAY_WITH_HEALING");
        try
        {
            return ExecutionMode.valueOf(modeStr.trim().toUpperCase());
        }
        catch (final IllegalArgumentException e)
        {
            return ExecutionMode.REPLAY_WITH_HEALING;
        }
    }

    /**
     * Resolves all configured volatile ID detection regex patterns.
     * Scans for Option B indexed properties (neodymium.ai.dom.volatileIdPatterns.1, .2, ...)
     * as well as single comma-separated property (neodymium.ai.dom.volatileIdPatterns).
     *
     * @return list of non-empty regex pattern strings
     */
    public List<String> getVolatileIdPatterns()
    {
        final List<String> patterns = new ArrayList<>();
        
        // 1. Scan Option B indexed keys: neodymium.ai.dom.volatileIdPatterns.1, .2, ...
        int index = 1;
        while (true)
        {
            final String pattern = getProperty("neodymium.ai.dom.volatileIdPatterns." + index, null);
            if (pattern == null || pattern.isBlank())
            {
                break;
            }
            patterns.add(pattern.trim());
            index++;
        }

        // 2. Also check single comma-separated property fallback
        final String singleProp = getProperty("neodymium.ai.dom.volatileIdPatterns", "");
        for (final String token : singleProp.split(","))
        {
            if (!token.isBlank())
            {
                patterns.add(token.trim());
            }
        }

        return patterns;
    }

    /**
     * Convenience getter for API key.
     *
     * @return the configured API key
     */
    public String aiApiKey()
    {
        return getApiKey("execution");
    }

    /**
     * Convenience getter for Aura Manager shutdown delay in seconds.
     *
     * @return the shutdown delay in seconds
     */
    public int auraManagerShutdownDelay()
    {
        return getInt("neodymium.ai.auraManagerShutdownDelay", 5);
    }

    /**
     * Resolves the maximum input (prompt) token budget limit per test run.
     * Default fallback is -1 (unlimited).
     *
     * @return maximum input token limit
     */
    public int getTokenBudgetInput()
    {
        return getInt("neodymium.ai.tokenBudget.input", -1);
    }

    /**
     * Resolves the maximum output (completion) token budget limit per test run.
     * Default fallback is -1 (unlimited).
     *
     * @return maximum output token limit
     */
    public int getTokenBudgetOutput()
    {
        return getInt("neodymium.ai.tokenBudget.output", -1);
    }

    /**
     * Checks if preliminary disk reporting is enabled for test runs.
     * Default is true.
     *
     * @return true if preliminary disk report listener is enabled, false otherwise
     */
    public boolean isDiskReportEnabled()
    {
        return getBoolean("neodymium.ai.report.disk.enabled", true);
    }

    /**
     * Resolves the target directory path where preliminary disk reports are stored.
     * Default is "target/ai-reports".
     *
     * @return disk report target directory path
     */
    public String getDiskReportDirectory()
    {
        return getProperty("neodymium.ai.report.disk.directory", "target/ai-reports");
    }

    /**
     * Resolves the output formats for preliminary disk reporting.
     * Default is "ALL" (generates HTML, Markdown, and JSON).
     *
     * @return comma-delimited or keyword format string
     */
    public String getDiskReportFormat()
    {
        final String primary = getProperty("neodymium.ai.report.disk.format", null);
        if (primary != null && !primary.isBlank())
        {
            return primary;
        }
        return getProperty("neodymium.ai.report.disk.formats", "ALL");
    }
}
