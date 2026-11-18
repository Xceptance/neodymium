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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;

/**
 * Loads and executes the consolidated browser-side Playwright selector evaluator script.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaywrightSelectorEvaluatorLoader
{
    private static final String SCRIPT = loadEvaluatorScript();

    private PlaywrightSelectorEvaluatorLoader()
    {
    }

    public static String getScript()
    {
        return SCRIPT;
    }

    /**
     * Executes the Playwright selector evaluator against the given {@link SearchContext}.
     *
     * @param context the Selenium search context (WebDriver or WebElement)
     * @param rawSelector the raw Playwright selector string
     * @return unmodifiable list of matching {@link WebElement} instances
     */
    public static List<WebElement> evaluate(final SearchContext context, final String rawSelector)
    {
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(rawSelector, "rawSelector cannot be null");

        final WebDriver driver = extractDriver(context);
        if (!(driver instanceof JavascriptExecutor js))
        {
            return Collections.emptyList();
        }

        final Object rootNode = (context instanceof WebElement el) ? el : null;
        final Object result = js.executeScript(SCRIPT, rootNode, rawSelector);

        if (result instanceof List<?> list)
        {
            final List<WebElement> elements = new ArrayList<>(list.size());
            for (final Object item : list)
            {
                if (item instanceof WebElement el)
                {
                    elements.add(el);
                }
            }
            return Collections.unmodifiableList(elements);
        }
        return Collections.emptyList();
    }

    private static WebDriver extractDriver(final SearchContext context)
    {
        if (context instanceof WebDriver d)
        {
            return d;
        }
        if (context instanceof WrapsDriver wd)
        {
            return wd.getWrappedDriver();
        }
        return null;
    }

    private static String loadEvaluatorScript()
    {
        try (final InputStream in = PlaywrightSelectorEvaluatorLoader.class.getResourceAsStream("/ai-scripts/playwright-selector-evaluator.js"))
        {
            if (in == null)
            {
                throw new IllegalStateException("Failed to load /ai-scripts/playwright-selector-evaluator.js from classpath");
            }
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
            {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
        catch (final Exception e)
        {
            throw new IllegalStateException("Failed to initialize PlaywrightSelectorEvaluatorLoader", e);
        }
    }
}
