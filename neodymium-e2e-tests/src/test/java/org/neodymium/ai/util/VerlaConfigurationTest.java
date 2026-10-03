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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.util.Neodymium;

/**
 * Unit tests verifying property loading, scaling factors, dynamic per-test overrides,
 * and delay calculation logic in {@link VerlaConfiguration}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class VerlaConfigurationTest
{
    @BeforeEach
    public void setup()
    {
        VerlaConfiguration.resetInstance();
        if (Neodymium.getData() != null)
        {
            Neodymium.getData().clear();
        }
    }

    @AfterEach
    public void tearDown()
    {
        VerlaConfiguration.resetInstance();
        if (Neodymium.getData() != null)
        {
            Neodymium.getData().clear();
        }
    }

    /**
     * Tests that default properties are correctly loaded from config/verla.properties.
     */
    @Test
    public void testDefaultPropertiesLoaded()
    {
        final VerlaConfiguration config = VerlaConfiguration.getInstance();

        Assertions.assertTrue(config.isLatencyEnabled(), "Latency should be enabled by default");
        Assertions.assertEquals(1.0, config.getLatencyScale(), 0.001, "Default scaling factor should be 1.0");

        Assertions.assertEquals(150L, config.getCartAddMinMs(), "Default cart add min should be 150ms");
        Assertions.assertEquals(300L, config.getCartAddMaxMs(), "Default cart add max should be 300ms");

        Assertions.assertEquals(100L, config.getSearchSuggestMinMs(), "Default search suggest min should be 100ms");
        Assertions.assertEquals(220L, config.getSearchSuggestMaxMs(), "Default search suggest max should be 220ms");

        Assertions.assertEquals(500L, config.getCheckoutPurchaseMinMs(), "Default purchase min should be 500ms");
        Assertions.assertEquals(900L, config.getCheckoutPurchaseMaxMs(), "Default purchase max should be 900ms");
    }

    /**
     * Tests scaling factor calculation with scale 2.0 (doubled latency) and 0.2 (20% latency).
     */
    @Test
    public void testLatencyScalingFactor()
    {
        final VerlaConfiguration config = VerlaConfiguration.getInstance();

        // Standard scale (1.0)
        final long delayStandard = config.calculateDelay(100L, 100L);
        Assertions.assertEquals(100L, delayStandard);

        // Double scale (2.0)
        config.setLatencyScale(2.0);
        final long delayDouble = config.calculateDelay(100L, 100L);
        Assertions.assertEquals(200L, delayDouble);

        // Scaled down (0.2)
        config.setLatencyScale(0.2);
        final long delayFast = config.calculateDelay(100L, 100L);
        Assertions.assertEquals(20L, delayFast);

        // Instant (0.0)
        config.setLatencyScale(0.0);
        final long delayInstant = config.calculateDelay(100L, 100L);
        Assertions.assertEquals(0L, delayInstant);
    }

    /**
     * Tests disabling latency master switch results in zero delay.
     */
    @Test
    public void testLatencyDisabled()
    {
        final VerlaConfiguration config = VerlaConfiguration.getInstance();
        config.setLatencyEnabled(false);

        Assertions.assertFalse(config.isLatencyEnabled());
        final long delay = config.calculateDelay(500L, 900L);
        Assertions.assertEquals(0L, delay, "Disabled latency should calculate 0 delay");
    }

    /**
     * Tests quick per-test overrides via Neodymium.getData().
     */
    @Test
    public void testThreadLocalNeodymiumDataOverride()
    {
        final VerlaConfiguration config = VerlaConfiguration.getInstance();

        // Override scale via Neodymium.getData()
        Neodymium.getData().put("verla.latency.scale", "0.5");
        Assertions.assertEquals(0.5, config.getLatencyScale(), 0.001);

        // Override specific min/max bounds via Neodymium.getData()
        Neodymium.getData().put("verla.latency.cart.add.min", "40");
        Neodymium.getData().put("verla.latency.cart.add.max", "60");

        Assertions.assertEquals(40L, config.getCartAddMinMs());
        Assertions.assertEquals(60L, config.getCartAddMaxMs());

        // Override master toggle
        Neodymium.getData().put("verla.latency.enabled", "false");
        Assertions.assertFalse(config.isLatencyEnabled());
    }

    /**
     * Tests programmatic override method and resetInstance.
     */
    @Test
    public void testProgrammaticOverrideAndReset()
    {
        final VerlaConfiguration config = VerlaConfiguration.getInstance();

        config.override("verla.latency.checkout.purchase.min", "50");
        Assertions.assertEquals(50L, config.getCheckoutPurchaseMinMs());

        VerlaConfiguration.resetInstance();
        final VerlaConfiguration reloaded = VerlaConfiguration.getInstance();
        Assertions.assertEquals(500L, reloaded.getCheckoutPurchaseMinMs(), "After reset, defaults should be restored");
    }

    /**
     * Tests random delay bounds generation.
     */
    @Test
    public void testCalculateDelayRange()
    {
        final VerlaConfiguration config = VerlaConfiguration.getInstance();

        for (int i = 0; i < 50; i++)
        {
            final long delay = config.calculateDelay(150L, 300L);
            Assertions.assertTrue(delay >= 150L && delay <= 300L, "Generated delay " + delay + " must be between 150 and 300");
        }
    }
}
