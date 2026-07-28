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
package org.neodymium.ai.executor.selenide;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.config.AiConfiguration;

/**
 * Unit tests for {@link VolatileIdDetector} and Option B indexed property configurations.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class VolatileIdDetectorTest
{
    @Test
    public void testDefaultConfiguredVolatilePatterns()
    {
        final VolatileIdDetector detector = new VolatileIdDetector(new AiConfiguration());

        // 1. React 18 useId() hook
        Assertions.assertTrue(detector.isVolatile(":r0:"));
        Assertions.assertTrue(detector.isVolatile(":r1a:"));

        // 2. Modern UI component framework prefixes
        Assertions.assertTrue(detector.isVolatile("radix-:r0:"));
        Assertions.assertTrue(detector.isVolatile("react-aria-1234"));
        Assertions.assertTrue(detector.isVolatile("mui-9382"));
        Assertions.assertTrue(detector.isVolatile("rc-select-1029"));
        Assertions.assertTrue(detector.isVolatile("antd-id-9382"));
        Assertions.assertTrue(detector.isVolatile("el-id-1029-3"));
        Assertions.assertTrue(detector.isVolatile("pv_id_1"));
        Assertions.assertTrue(detector.isVolatile("cdk-overlay-3"));
        Assertions.assertTrue(detector.isVolatile("mat-input-0"));
        Assertions.assertTrue(detector.isVolatile("ember1234"));
        Assertions.assertTrue(detector.isVolatile("svelte-1a2b3c"));

        // 3. Vue / Vuetify dynamic node hashes
        Assertions.assertTrue(detector.isVolatile("v-btn-338321"));
        Assertions.assertTrue(detector.isVolatile("v-node-142226"));
        Assertions.assertTrue(detector.isVolatile("v-inner-305370"));
        Assertions.assertTrue(detector.isVolatile("react-div-343141"));
        Assertions.assertTrue(detector.isVolatile("react-hydrated-header-948201"));

        // 4. UUIDs / GUIDs
        Assertions.assertTrue(detector.isVolatile("e4e9b940-84a1-43e5-a0c3-426614174000"));

        // 5. Trailing numeric hash suffixes (4+ digits)
        Assertions.assertTrue(detector.isVolatile("button_10293"));
        Assertions.assertTrue(detector.isVolatile("node-938210"));

        // 6. Valid human-written static IDs MUST NOT be marked as volatile
        Assertions.assertFalse(detector.isVolatile("search-input"));
        Assertions.assertFalse(detector.isVolatile("country-trigger-btn"));
        Assertions.assertFalse(detector.isVolatile("cart-btn-wrapper"));
        Assertions.assertFalse(detector.isVolatile("username"));
        Assertions.assertFalse(detector.isVolatile("password"));
        Assertions.assertFalse(detector.isVolatile("login-form"));
        Assertions.assertFalse(detector.isVolatile("main-nav"));
    }

    @Test
    public void testExplicitCustomPatterns()
    {
        final VolatileIdDetector detector = new VolatileIdDetector(List.of("^mycmp-.*", "^temp_id_.*"));

        Assertions.assertTrue(detector.isVolatile("mycmp-widget-1"));
        Assertions.assertTrue(detector.isVolatile("temp_id_99"));
        Assertions.assertFalse(detector.isVolatile("search-input"));
    }
}
