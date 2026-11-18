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

import java.util.Objects;

/**
 * Represents a single step/clause within a Playwright selector chain.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class SelectorStep
{
    private final boolean captured;
    private final SelectorEngine engine;
    private final String engineName;
    private final String body;
    private final Object parsed;

    public SelectorStep(final boolean captured, final SelectorEngine engine, final String engineName, final String body, final Object parsed)
    {
        this.captured = captured;
        this.engine = Objects.requireNonNull(engine, "engine cannot be null");
        this.engineName = Objects.requireNonNull(engineName, "engineName cannot be null");
        this.body = Objects.requireNonNull(body, "body cannot be null");
        this.parsed = parsed;
    }

    public boolean isCaptured()
    {
        return this.captured;
    }

    public SelectorEngine getEngine()
    {
        return this.engine;
    }

    public String getEngineName()
    {
        return this.engineName;
    }

    public String getBody()
    {
        return this.body;
    }

    public Object getParsed()
    {
        return this.parsed;
    }

    @Override
    public String toString()
    {
        return (this.captured ? "*" : "") + this.engineName + "=" + this.body;
    }
}
