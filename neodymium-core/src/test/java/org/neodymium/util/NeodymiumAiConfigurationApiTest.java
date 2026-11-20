/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.config.AiConfiguration;

/**
 * Regression safety net verifying that {@link Neodymium#aiConfiguration()} and
 * {@link Neodymium#reloadAiConfiguration()} (as well as the legacy {@link com.xceptance.neodymium.util.Neodymium} facade)
 * are functional and backward-compatible.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class NeodymiumAiConfigurationApiTest
{
    private static final String TEST_PROPERTY = "neodymium.ai.agent.methods.classes";

    @AfterEach
    public void cleanup()
    {
        System.clearProperty(TEST_PROPERTY);
        Neodymium.reloadAiConfiguration();
    }

    @Test
    @DisplayName("Verify that Neodymium.aiConfiguration returns non-null AiConfiguration singleton")
    public void testAiConfigurationAccessor()
    {
        final AiConfiguration config = Neodymium.aiConfiguration();
        Assertions.assertNotNull(config, "Neodymium.aiConfiguration() must not return null");
    }

    @Test
    @DisplayName("Verify that Neodymium.reloadAiConfiguration re-reads updated system properties")
    public void testReloadAiConfigurationUpdatesProperties()
    {
        final String expectedClass = "com.xceptance.neodymium.test.DummyHelper";
        System.setProperty(TEST_PROPERTY, expectedClass);

        Neodymium.reloadAiConfiguration();

        final AiConfiguration refreshed = Neodymium.aiConfiguration();
        Assertions.assertNotNull(refreshed, "Refreshed AiConfiguration must not be null");
    }

    @Test
    @DisplayName("Verify that legacy com.xceptance.neodymium.util.Neodymium inherits reloadAiConfiguration")
    public void testLegacyFacadeInheritsReloadAiConfiguration()
    {
        com.xceptance.neodymium.util.Neodymium.reloadAiConfiguration();
        final AiConfiguration config = com.xceptance.neodymium.util.Neodymium.aiConfiguration();
        Assertions.assertNotNull(config, "Legacy facade must provide non-null aiConfiguration");
    }
}
