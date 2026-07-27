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
package com.xceptance.neodymium.ai.core;

/**
 * Execution engines supported by Neodymium AI.
 * Used for multi-dimensional prompt resolution and driver routing.
 *
 * @author AI-generated: Gemini 3.5 Pro
 * @author Xceptance GmbH 2026
 */
public enum ExecutionEngine
{
    /** Selenide / W3C Selenium WebDriver backend */
    SELENIDE("selenide", "Selenide / Selenium WebDriver"),

    /** Native Playwright engine backend */
    PLAYWRIGHT("playwright", "Playwright Native Engine");

    private final String engineId;
    private final String displayName;

    ExecutionEngine(final String engineId, final String displayName)
    {
        this.engineId = engineId;
        this.displayName = displayName;
    }

    /**
     * @return the lowercase identifier for folder lookup (e.g. "selenide", "playwright")
     */
    public final String getEngineId()
    {
        return this.engineId;
    }

    /**
     * @return human-readable engine name
     */
    public final String getDisplayName()
    {
        return this.displayName;
    }

    /**
     * Resolves an engine from string name, defaulting to SELENIDE if unknown or null.
     *
     * @param name engine name
     * @return resolved ExecutionEngine
     */
    public static ExecutionEngine fromName(final String name)
    {
        if (name == null || name.isBlank())
        {
            return SELENIDE;
        }
        final String clean = name.trim().toLowerCase();
        for (final ExecutionEngine engine : values())
        {
            if (engine.engineId.equalsIgnoreCase(clean) || engine.name().equalsIgnoreCase(clean))
            {
                return engine;
            }
        }
        return SELENIDE;
    }
}
