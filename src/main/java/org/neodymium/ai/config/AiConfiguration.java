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
import java.util.Properties;
import org.neodymium.util.Neodymium;

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
     * Map storing loaded hierarchical properties.
     */
    private final Properties properties = new Properties();

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

        // 7. Overlay with System properties
        System.getProperties().forEach((key, val) -> {
            final String propKey = String.valueOf(key);
            if (propKey.startsWith("neodymium.ai"))
            {
                this.properties.setProperty(propKey, String.valueOf(val));
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
            }
            catch (final IOException e)
            {
                // Suppress properties load exceptions
            }
        }
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
                return String.valueOf(threadVal);
            }
        }
        catch (final Throwable ignored)
        {
            // Fallback in case Neodymium class is not initialized or on classpath
        }
        return this.properties.getProperty(key, defaultValue);
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
        if (val == null)
        {
            return defaultValue;
        }
        try
        {
            return Integer.parseInt(val.trim());
        }
        catch (final NumberFormatException e)
        {
            return defaultValue;
        }
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
        return getProperty("neodymium.ai.model", "gemini-3.5-flash");
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
}
