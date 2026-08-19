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
package org.neodymium.ai.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.neodymium.util.Neodymium;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe configuration manager for the VÉRLA demo store.
 * Resolves runtime latency simulation settings, scaling factors, and operation-specific bounds
 * hierarchically with support for dynamic per-test overrides via {@link Neodymium#getData()},
 * system properties, environment variables, or programmatic setters.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class VerlaConfiguration
{
    private static final Logger LOG = LoggerFactory.getLogger(VerlaConfiguration.class);

    private static final String DEFAULT_CONFIG_FILE = "config/verla.properties";
    private static final String DEV_CONFIG_FILE = "config/dev-verla.properties";

    /**
     * Singleton instance.
     */
    private static volatile VerlaConfiguration instance;

    /**
     * Loaded base properties from configuration files.
     */
    private final Properties fileProperties = new Properties();

    /**
     * Programmatic in-memory overrides.
     */
    private final Map<String, String> programmaticOverrides = new ConcurrentHashMap<>();

    /**
     * Gets the shared {@link VerlaConfiguration} instance.
     *
     * @return the configuration instance
     */
    public static VerlaConfiguration getInstance()
    {
        VerlaConfiguration result = instance;
        if (result == null)
        {
            synchronized (VerlaConfiguration.class)
            {
                result = instance;
                if (result == null)
                {
                    instance = result = new VerlaConfiguration();
                }
            }
        }
        return result;
    }

    /**
     * Resets the shared configuration instance, clearing overrides and forcing a reload from files.
     */
    public static void resetInstance()
    {
        synchronized (VerlaConfiguration.class)
        {
            instance = null;
        }
    }

    /**
     * Private constructor initializing configuration from properties files.
     */
    private VerlaConfiguration()
    {
        loadProperties();
    }

    /**
     * Loads properties hierarchically from config/verla.properties and config/dev-verla.properties.
     */
    private void loadProperties()
    {
        loadFromFile(DEFAULT_CONFIG_FILE);
        loadFromFile(DEV_CONFIG_FILE);
    }

    /**
     * Loads properties from a file or classpath resource if it exists.
     *
     * @param filePath the file path
     */
    private void loadFromFile(final String filePath)
    {
        final File file = new File(filePath);
        if (file.exists() && file.isFile())
        {
            try (final InputStream in = new FileInputStream(file))
            {
                this.fileProperties.load(in);
                LOG.debug("Loaded VÉRLA configuration from {}", filePath);
                return;
            }
            catch (final IOException e)
            {
                LOG.warn("Failed to load VÉRLA configuration from {}", filePath, e);
            }
        }

        // Check classpath resource fallback
        try (final InputStream in = VerlaConfiguration.class.getClassLoader().getResourceAsStream(filePath))
        {
            if (in != null)
            {
                this.fileProperties.load(in);
                LOG.debug("Loaded VÉRLA configuration from classpath resource: {}", filePath);
            }
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to load VÉRLA configuration from classpath resource: {}", filePath, e);
        }
    }

    /**
     * Resolves a property string value by key using hierarchical resolution:
     * 1. Thread-local test data ({@link Neodymium#getData()})
     * 2. Programmatic in-memory overrides
     * 3. JVM System properties ({@code System.getProperty})
     * 4. System Environment variables ({@code System.getenv})
     * 5. Configuration files ({@code dev-verla.properties}, {@code verla.properties})
     * 6. Default fallback value
     *
     * @param key the property key
     * @param defaultValue the default fallback value
     * @return the resolved property string
     */
    public String getProperty(final String key, final String defaultValue)
    {
        // 1. Thread-local Neodymium test data override
        try
        {
            if (Neodymium.getData() != null)
            {
                final Object threadVal = Neodymium.getData().get(key);
                if (threadVal != null)
                {
                    return String.valueOf(threadVal).trim();
                }
            }
        }
        catch (final Throwable ignored)
        {
        }

        // 2. Programmatic in-memory override
        final String progVal = this.programmaticOverrides.get(key);
        if (progVal != null)
        {
            return progVal.trim();
        }

        // 3. JVM System property
        final String sysVal = System.getProperty(key);
        if (sysVal != null && !sysVal.isEmpty())
        {
            return sysVal.trim();
        }

        // 4. System Environment variable
        final String envKey = key.toUpperCase().replace('.', '_');
        final String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isEmpty())
        {
            return envVal.trim();
        }

        // 5. File properties
        final String fileVal = this.fileProperties.getProperty(key);
        if (fileVal != null && !fileVal.isEmpty())
        {
            return fileVal.trim();
        }

        // 6. Default value
        return defaultValue;
    }

    /**
     * Resolves a boolean property value.
     *
     * @param key the property key
     * @param defaultValue default boolean
     * @return resolved boolean
     */
    public boolean getBoolean(final String key, final boolean defaultValue)
    {
        final String val = getProperty(key, Boolean.toString(defaultValue));
        return Boolean.parseBoolean(val);
    }

    /**
     * Resolves a long property value.
     *
     * @param key the property key
     * @param defaultValue default long
     * @return resolved long
     */
    public long getLong(final String key, final long defaultValue)
    {
        final String val = getProperty(key, Long.toString(defaultValue));
        try
        {
            return Long.parseLong(val);
        }
        catch (final NumberFormatException e)
        {
            LOG.warn("Invalid long value '{}' for property '{}', falling back to {}", val, key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Resolves a double property value.
     *
     * @param key the property key
     * @param defaultValue default double
     * @return resolved double
     */
    public double getDouble(final String key, final double defaultValue)
    {
        final String val = getProperty(key, Double.toString(defaultValue));
        try
        {
            return Double.parseDouble(val);
        }
        catch (final NumberFormatException e)
        {
            LOG.warn("Invalid double value '{}' for property '{}', falling back to {}", val, key, defaultValue);
            return defaultValue;
        }
    }

    // =========================================================================
    // Programmatic Override API
    // =========================================================================

    /**
     * Overrides a configuration property in-memory.
     *
     * @param key the property key
     * @param value the property value
     */
    public void override(final String key, final String value)
    {
        if (key != null)
        {
            if (value == null)
            {
                this.programmaticOverrides.remove(key);
            }
            else
            {
                this.programmaticOverrides.put(key, value);
            }
        }
    }

    /**
     * Clears all in-memory programmatic overrides.
     */
    public void clearOverrides()
    {
        this.programmaticOverrides.clear();
    }

    /**
     * Sets the master latency enable flag programmatically.
     *
     * @param enabled whether latency simulation is enabled
     */
    public void setLatencyEnabled(final boolean enabled)
    {
        override("verla.latency.enabled", Boolean.toString(enabled));
    }

    /**
     * Sets the global latency scaling factor programmatically.
     *
     * @param scale the scaling multiplier (e.g. 1.0, 2.0, 0.2, 0.0)
     */
    public void setLatencyScale(final double scale)
    {
        override("verla.latency.scale", Double.toString(scale));
    }

    // =========================================================================
    // Property Getters
    // =========================================================================

    /**
     * Returns whether runtime latency simulation is enabled.
     *
     * @return true if latency simulation is enabled, false otherwise
     */
    public boolean isLatencyEnabled()
    {
        return getBoolean("verla.latency.enabled", true);
    }

    /**
     * Returns the global latency scaling multiplier.
     *
     * @return the scaling factor (default 1.0)
     */
    public double getLatencyScale()
    {
        return getDouble("verla.latency.scale", 1.0);
    }

    // --- Cart Latencies ---

    public long getCartAddMinMs()
    {
        return getLong("verla.latency.cart.add.min", 150L);
    }

    public long getCartAddMaxMs()
    {
        return getLong("verla.latency.cart.add.max", 300L);
    }

    public long getCartUpdateMinMs()
    {
        return getLong("verla.latency.cart.update.min", 100L);
    }

    public long getCartUpdateMaxMs()
    {
        return getLong("verla.latency.cart.update.max", 250L);
    }

    public long getCartRemoveMinMs()
    {
        return getLong("verla.latency.cart.remove.min", 100L);
    }

    public long getCartRemoveMaxMs()
    {
        return getLong("verla.latency.cart.remove.max", 200L);
    }

    public long getCartCouponMinMs()
    {
        return getLong("verla.latency.cart.coupon.min", 120L);
    }

    public long getCartCouponMaxMs()
    {
        return getLong("verla.latency.cart.coupon.max", 250L);
    }

    public long getCartDropdownMinMs()
    {
        return getLong("verla.latency.cart.dropdown.min", 80L);
    }

    public long getCartDropdownMaxMs()
    {
        return getLong("verla.latency.cart.dropdown.max", 180L);
    }

    // --- Search Latencies ---

    public long getSearchSuggestMinMs()
    {
        return getLong("verla.latency.search.suggest.min", 100L);
    }

    public long getSearchSuggestMaxMs()
    {
        return getLong("verla.latency.search.suggest.max", 220L);
    }

    public long getSearchQueryMinMs()
    {
        return getLong("verla.latency.search.query.min", 200L);
    }

    public long getSearchQueryMaxMs()
    {
        return getLong("verla.latency.search.query.max", 400L);
    }

    public long getProductFilterMinMs()
    {
        return getLong("verla.latency.products.filter.min", 180L);
    }

    public long getProductFilterMaxMs()
    {
        return getLong("verla.latency.products.filter.max", 350L);
    }

    // --- Checkout & Order Latencies ---

    public long getCheckoutPurchaseMinMs()
    {
        return getLong("verla.latency.checkout.purchase.min", 500L);
    }

    public long getCheckoutPurchaseMaxMs()
    {
        return getLong("verla.latency.checkout.purchase.max", 900L);
    }

    public long getOrderLookupMinMs()
    {
        return getLong("verla.latency.order.lookup.min", 120L);
    }

    public long getOrderLookupMaxMs()
    {
        return getLong("verla.latency.order.lookup.max", 250L);
    }

    // --- Auth Latencies ---

    public long getAuthLoginMinMs()
    {
        return getLong("verla.latency.auth.login.min", 150L);
    }

    public long getAuthLoginMaxMs()
    {
        return getLong("verla.latency.auth.login.max", 300L);
    }

    public long getAuthRegisterMinMs()
    {
        return getLong("verla.latency.auth.register.min", 200L);
    }

    public long getAuthRegisterMaxMs()
    {
        return getLong("verla.latency.auth.register.max", 400L);
    }

    // =========================================================================
    // Latency Calculation & Simulation Helpers
    // =========================================================================

    /**
     * Calculates the scaled delay duration in milliseconds based on min/max bounds and current scale factor.
     *
     * @param minMs minimum delay in ms
     * @param maxMs maximum delay in ms
     * @return calculated delay in milliseconds (0 if disabled or scale <= 0)
     */
    public long calculateDelay(final long minMs, final long maxMs)
    {
        if (!isLatencyEnabled())
        {
            return 0L;
        }

        final double scale = getLatencyScale();
        if (scale <= 0.0)
        {
            return 0L;
        }

        if (minMs <= 0 && maxMs <= 0)
        {
            return 0L;
        }

        final long baseDelay;
        if (minMs >= maxMs)
        {
            baseDelay = minMs;
        }
        else
        {
            baseDelay = ThreadLocalRandom.current().nextLong(minMs, maxMs + 1);
        }

        return Math.max(0L, Math.round(baseDelay * scale));
    }

    /**
     * Simulates latency by sleeping the current worker thread for the calculated duration.
     *
     * @param minMs minimum delay in ms
     * @param maxMs maximum delay in ms
     */
    public void simulateDelay(final long minMs, final long maxMs)
    {
        final long delay = calculateDelay(minMs, maxMs);
        if (delay > 0)
        {
            try
            {
                Thread.sleep(delay);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    // --- Specific Simulation Helpers ---

    public void simulateCartAdd()
    {
        simulateDelay(getCartAddMinMs(), getCartAddMaxMs());
    }

    public void simulateCartUpdate()
    {
        simulateDelay(getCartUpdateMinMs(), getCartUpdateMaxMs());
    }

    public void simulateCartRemove()
    {
        simulateDelay(getCartRemoveMinMs(), getCartRemoveMaxMs());
    }

    public void simulateCartCoupon()
    {
        simulateDelay(getCartCouponMinMs(), getCartCouponMaxMs());
    }

    public void simulateCartDropdown()
    {
        simulateDelay(getCartDropdownMinMs(), getCartDropdownMaxMs());
    }

    public void simulateSearchSuggest()
    {
        simulateDelay(getSearchSuggestMinMs(), getSearchSuggestMaxMs());
    }

    public void simulateSearchQuery()
    {
        simulateDelay(getSearchQueryMinMs(), getSearchQueryMaxMs());
    }

    public void simulateProductFilter()
    {
        simulateDelay(getProductFilterMinMs(), getProductFilterMaxMs());
    }

    public void simulatePurchase()
    {
        simulateDelay(getCheckoutPurchaseMinMs(), getCheckoutPurchaseMaxMs());
    }

    public void simulateOrderLookup()
    {
        simulateDelay(getOrderLookupMinMs(), getOrderLookupMaxMs());
    }

    public void simulateAuthLogin()
    {
        simulateDelay(getAuthLoginMinMs(), getAuthLoginMaxMs());
    }

    public void simulateAuthRegister()
    {
        simulateDelay(getAuthRegisterMinMs(), getAuthRegisterMaxMs());
    }
}
