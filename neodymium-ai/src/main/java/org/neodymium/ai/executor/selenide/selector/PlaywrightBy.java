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

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

/**
 * Selenium {@link By} implementation backed by the Playwright selector engine and in-browser evaluator,
 * searching transparently across open Shadow DOM boundaries in document order.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaywrightBy extends By implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final String rawSelector;
    private final SelectorChain chain;

    public PlaywrightBy(final String rawSelector, final SelectorChain chain)
    {
        this.rawSelector = Objects.requireNonNull(rawSelector, "rawSelector cannot be null");
        this.chain = Objects.requireNonNull(chain, "chain cannot be null");
    }

    public static PlaywrightBy of(final String selector)
    {
        final SelectorChain parsedChain = PlaywrightSelectorParser.parse(selector);
        return new PlaywrightBy(selector, parsedChain);
    }

    public String getRawSelector()
    {
        return this.rawSelector;
    }

    public SelectorChain getChain()
    {
        return this.chain;
    }

    @Override
    public List<WebElement> findElements(final SearchContext context)
    {
        // XPath single step: strictly evaluate via native By.xpath (does NOT pierce shadow DOM)
        if (this.chain.getSteps().size() == 1 && this.chain.getSteps().get(0).getEngine() == SelectorEngine.XPATH)
        {
            return By.xpath(this.chain.getSteps().get(0).getBody()).findElements(context);
        }

        return PlaywrightSelectorEvaluatorLoader.evaluate(context, this.rawSelector);
    }

    @Override
    public WebElement findElement(final SearchContext context)
    {
        final List<WebElement> elements = findElements(context);
        if (elements.isEmpty())
        {
            throw new NoSuchElementException("Cannot locate an element using " + toString());
        }
        return elements.get(0);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        final PlaywrightBy that = (PlaywrightBy) o;
        return Objects.equals(this.rawSelector, that.rawSelector);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.rawSelector);
    }

    @Override
    public String toString()
    {
        return "PlaywrightBy: " + this.rawSelector;
    }
}
