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
package com.xceptance.aura.report.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Verifies that {@link H2ConsoleConfig} is disabled by default so the embedded H2 database console
 * is never exposed unless {@code spring.h2.console.enabled=true} is explicitly configured.
 *
 * @author AI-generated: Gemini 4 Neon
 * @author Xceptance GmbH 2026
 */
public class H2ConsoleConfigTest
{
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withUserConfiguration(H2ConsoleConfig.class);

    @Test
    public void testH2ConsoleDisabledByDefault()
    {
        contextRunner.run(context -> {
            assertFalse(context.containsBean("h2ServletRegistration"),
                "H2 console servlet must not be registered when spring.h2.console.enabled is unset");
        });
    }

    @Test
    public void testH2ConsoleDisabledWhenExplicitlyFalse()
    {
        contextRunner
            .withPropertyValues("spring.h2.console.enabled=false")
            .run(context -> {
                assertFalse(context.containsBean("h2ServletRegistration"),
                    "H2 console servlet must not be registered when spring.h2.console.enabled=false");
            });
    }

    @Test
    public void testH2ConsoleEnabledWhenExplicitlyTrue()
    {
        contextRunner
            .withPropertyValues("spring.h2.console.enabled=true")
            .run(context -> {
                assertTrue(context.containsBean("h2ServletRegistration"),
                    "H2 console servlet must be registered when spring.h2.console.enabled=true");
            });
    }
}
