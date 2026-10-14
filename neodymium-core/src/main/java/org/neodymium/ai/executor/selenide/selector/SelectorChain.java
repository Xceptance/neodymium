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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a sequence of selector steps chained by {@code >>}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class SelectorChain
{
    private final List<SelectorStep> steps;
    private final int capturedIndex;

    public SelectorChain(final List<SelectorStep> steps)
    {
        Objects.requireNonNull(steps, "steps cannot be null");
        if (steps.isEmpty())
        {
            throw new IllegalArgumentException("Selector chain cannot be empty");
        }
        this.steps = Collections.unmodifiableList(steps);
        int captured = -1;
        for (int i = 0; i < steps.size(); i++)
        {
            if (steps.get(i).isCaptured())
            {
                if (captured != -1)
                {
                    throw new IllegalArgumentException("Only one step in a selector chain may be captured with '*'");
                }
                captured = i;
            }
        }
        this.capturedIndex = captured;
    }

    public List<SelectorStep> getSteps()
    {
        return this.steps;
    }

    public int getCapturedIndex()
    {
        return this.capturedIndex;
    }

    public boolean hasCapture()
    {
        return this.capturedIndex != -1;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.steps.size(); i++)
        {
            if (i > 0)
            {
                sb.append(" >> ");
            }
            sb.append(this.steps.get(i));
        }
        return sb.toString();
    }
}
