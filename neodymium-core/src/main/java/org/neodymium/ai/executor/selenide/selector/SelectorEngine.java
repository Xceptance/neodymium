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
package org.neodymium.ai.executor.selenide.selector;

import java.util.Locale;

/**
 * Enumeration of selector engines supported in Playwright selector strings and Neodymium extensions.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public enum SelectorEngine
{
    CSS("css"),
    XPATH("xpath"),
    TEXT("text"),
    ROLE("role"),
    ID("id"),
    DATA_TESTID("data-testid"),
    DATA_TEST_ID("data-test-id"),
    DATA_TEST("data-test"),
    DATA_QA("data-qa"),
    DATA_CY("data-cy"),
    NTH("nth"),
    VISIBLE("visible"),
    INTERNAL("internal"),
    LABEL("label"),
    PLACEHOLDER("placeholder"),
    ALT("alt"),
    TITLE("title"),
    AUTOMATION_ID("data-ai");

    private final String prefix;

    SelectorEngine(final String prefix)
    {
        this.prefix = prefix;
    }

    public String getPrefix()
    {
        return this.prefix;
    }

    /**
     * Resolves the matching engine for the given prefix name (case-insensitive).
     *
     * @param name engine name prefix
     * @return matching {@link SelectorEngine} or {@code null} if not a recognized engine
     */
    public static SelectorEngine fromPrefix(final String name)
    {
        if (name == null || name.isBlank())
        {
            return null;
        }
        final String clean = name.trim().toLowerCase(Locale.ROOT);
        if (clean.startsWith("internal:"))
        {
            return INTERNAL;
        }
        for (final SelectorEngine engine : values())
        {
            if (engine.prefix.equalsIgnoreCase(clean))
            {
                return engine;
            }
        }
        return null;
    }
}
