/*
 * Copyright (c) 2017-2026 Xceptance Software Technologies GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.util.layer.selenide;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import com.xceptance.neodymium.util.SelenideAddons;
import com.xceptance.neodymium.util.layer.AssertionFacade;

import org.junit.jupiter.api.Assertions;

/**
 * Selenide implementation of {@link AssertionFacade}.
 * <p>
 * Standard assertions are wrapped in {@link SelenideAddons#wrapAssertionError(Runnable)}
 * to ensure screenshots and page source are captured on failure.
 * </p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class SelenideAssertionFacade implements AssertionFacade
{
    @Override
    public void assertTrue(final String message, final boolean condition)
    {
        SelenideAddons.wrapAssertionError(() -> Assertions.assertTrue(condition, message));
    }

    @Override
    public void assertTrue(final boolean condition)
    {
        SelenideAddons.wrapAssertionError(() -> Assertions.assertTrue(condition));
    }

    @Override
    public void assertEquals(final String message, final Object expected, final Object actual)
    {
        SelenideAddons.wrapAssertionError(() -> Assertions.assertEquals(expected, actual, message));
    }

    @Override
    public void assertEquals(final Object expected, final Object actual)
    {
        SelenideAddons.wrapAssertionError(() -> Assertions.assertEquals(expected, actual));
    }

    @Override
    public void should(final SelenideElement element, final String... conditions)
    {
        element.should(mapConditions(conditions));
    }

    @Override
    public void should(final SelenideElement element, final Duration timeout, final String... conditions)
    {
        final WebElementCondition[] conds = mapConditions(conditions);
        if (conds.length == 1)
        {
            element.shouldBe(conds[0], timeout);
        }
        else
        {
            WebElementCondition combined = conds[0];
            for (int i = 1; i < conds.length; i++)
            {
                combined = Condition.and("matched conditions", combined, conds[i]);
            }
            element.shouldBe(combined, timeout);
        }
    }

    @Override
    public void shouldHaveAttribute(final SelenideElement element, final String attributeName, final String expectedValue)
    {
        element.shouldHave(Condition.attribute(attributeName, expectedValue));
    }

    /**
     * Maps human-readable condition names to Selenide {@link WebElementCondition}s.
     *
     * @param conditions names of conditions
     * @return array of Selenide conditions
     */
    private WebElementCondition[] mapConditions(final String... conditions)
    {
        final List<WebElementCondition> result = new ArrayList<>(conditions.length);
        for (final String cond : conditions)
        {
            switch (cond.toLowerCase())
            {
                case "exist":
                case "exists":
                    result.add(Condition.exist);
                    break;
                case "visible":
                    result.add(Condition.visible);
                    break;
                case "hidden":
                    result.add(Condition.hidden);
                    break;
                case "focused":
                    result.add(Condition.focused);
                    break;
                case "enabled":
                    result.add(Condition.enabled);
                    break;
                case "disabled":
                    result.add(Condition.disabled);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported condition: " + cond);
            }
        }
        return result.toArray(new WebElementCondition[0]);
    }
}
