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
package org.neodymium.ai.executor.selenide.plugins;

import java.time.Duration;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.WebElementCondition;

/**
 * Concrete action plugin executing WAIT commands.
 * Supports static pauses (sleep) or waiting for elements to meet visibility conditions.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class WaitAction implements BrowserActionPlugin
{
    /**
     * Constructs a WaitAction.
     */
    public WaitAction()
    {
    }

    /**
     * Executes the wait command.
     *
     * @param action the wait action
     * @throws Exception if wait condition is not met within timeout
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action == null)
        {
            return;
        }

        final String target = action.getTarget();
        final String value = action.getValue();

        if (target == null || target.trim().isEmpty())
        {
            long delayMs = 1000L;
            if (value != null)
            {
                try
                {
                    final String cleanVal = value.toLowerCase().trim();
                    if (cleanVal.endsWith("ms"))
                    {
                        delayMs = Long.parseLong(cleanVal.substring(0, cleanVal.length() - 2));
                    }
                    else if (cleanVal.endsWith("s"))
                    {
                        delayMs = Long.parseLong(cleanVal.substring(0, cleanVal.length() - 1)) * 1000;
                    }
                    else
                    {
                        delayMs = Long.parseLong(cleanVal);
                    }
                }
                catch (final NumberFormatException e)
                {
                    // Fallback to default
                }
            }
            Selenide.sleep(delayMs);
        }
        else
        {
            if ("text".equalsIgnoreCase(target) && value != null && !value.isBlank())
            {
                Selenide.$(Selectors.withText(value)).shouldBe(Condition.visible, Duration.ofSeconds(10));
            }
            else
            {
                boolean isCustomTimeout = false;
                long timeoutMs = 10000;
                if (value != null && !value.isBlank())
                {
                    try
                    {
                        timeoutMs = Long.parseLong(value.trim());
                        isCustomTimeout = true;
                    }
                    catch (final NumberFormatException ignored)
                    {
                    }
                }

                if (!isCustomTimeout && value != null && !value.isBlank())
                {
                    final String val = value.toLowerCase().trim();
                    if (val.contains("exist") || val.contains("present"))
                    {
                        SelenideElementFinder.findElement(target).shouldBe(Condition.exist, Duration.ofSeconds(10));
                    }
                    else if (val.contains("hidden") || val.contains("invisible") || val.contains("absent"))
                    {
                        SelenideElementFinder.findElement(target).shouldBe(Condition.hidden, Duration.ofSeconds(10));
                    }
                    else if (val.contains("visible"))
                    {
                        SelenideElementFinder.findElement(target).shouldBe(Condition.visible, Duration.ofSeconds(10));
                    }
                    else
                    {
                        SelenideElementFinder.findElement(target).shouldHave(Condition.text(value), Duration.ofSeconds(10));
                    }
                }
                else
                {
                    SelenideElementFinder.findElement(target).shouldBe(Condition.visible, Duration.ofMillis(timeoutMs));
                }
            }
        }
    }
}
