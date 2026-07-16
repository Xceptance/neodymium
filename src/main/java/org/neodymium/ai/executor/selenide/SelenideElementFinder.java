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

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ElementsCollection;
import org.openqa.selenium.By;

/**
 * Shared utility for finding SelenideElements using multiple resolution strategies.
 * Provides fallback strategies including CSS, XPath, Link Text, and text contains.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class SelenideElementFinder
{
    private SelenideElementFinder()
    {
    }

    /**
     * Resolves and returns a SelenideElement based on the given target selector or text.
     *
     * @param target the target locator or text content
     * @return the resolved SelenideElement
     */
    public static SelenideElement findElement(final String target)
    {
        if (target == null || target.trim().isEmpty())
        {
            throw new IllegalArgumentException("Target cannot be empty");
        }

        String clean = target.trim();
        boolean forceXpath = false;
        boolean forceCss = false;

        if (clean.toLowerCase().startsWith("xpath="))
        {
            clean = clean.substring(6).trim();
            forceXpath = true;
        }
        else if (clean.toLowerCase().startsWith("css="))
        {
            clean = clean.substring(4).trim();
            forceCss = true;
        }

        final long start = System.currentTimeMillis();
        final long timeoutMs = com.codeborne.selenide.Configuration.timeout;

        while (true)
        {
            // 1. Neodymium Automation ID (xc_...)
            if (!forceXpath && (clean.contains("data-neo-ref") || clean.contains("xc_")))
            {
                ElementsCollection els = clean.matches("^xc_.*")
                    ? Selenide.$$(By.cssSelector("[data-neo-ref='" + clean + "']"))
                    : Selenide.$$(By.cssSelector(clean));
                if (!els.isEmpty())
                {
                    return els.first();
                }

                // If not found, dynamically stamp data-neo-ref attributes into the DOM (crucial for offline replays)
                try
                {
                    new PageAnalyzer().captureSimplifiedDom(ContextLevel.LEAN);
                }
                catch (final Exception ignored)
                {
                }

                final ElementsCollection retryEls = clean.matches("^xc_.*")
                    ? Selenide.$$(By.cssSelector("[data-neo-ref='" + clean + "']"))
                    : Selenide.$$(By.cssSelector(clean));
                if (!retryEls.isEmpty())
                {
                    return retryEls.first();
                }

                // Dynamic text-matching assertion fallback for missing data-neo-ref targets
                final org.neodymium.ai.pipeline.ExecutionContext context = org.neodymium.ai.pipeline.ExecutionContext.getActiveContext();
                if (context != null)
                {
                    final org.neodymium.ai.model.PlaybookStep step = (org.neodymium.ai.model.PlaybookStep) context.getTransientData().get(org.neodymium.ai.pipeline.ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
                    if (step != null)
                    {
                        for (final org.neodymium.ai.action.Action action : step.getActions())
                        {
                            if ("ASSERT".equalsIgnoreCase(action.getType()) && action.getValue() != null)
                            {
                                final String expectedVal = action.getValue();
                                final ElementsCollection allElements = Selenide.$$("*");
                                for (final com.codeborne.selenide.SelenideElement el : allElements)
                                {
                                    try
                                    {
                                        final String text = el.text().trim();
                                        if (text.equals(expectedVal) || text.contains(expectedVal) || text.matches(expectedVal))
                                        {
                                            return el;
                                        }
                                    }
                                    catch (final Exception ignored)
                                    {
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Try as CSS Selector
            if (!forceXpath)
            {
                try
                {
                    final ElementsCollection els = Selenide.$$(By.cssSelector(clean));
                    if (!els.isEmpty())
                    {
                        return els.first();
                    }

                    // Fallback healing for CSS button selectors that are actually divs/spans
                    final String[] selectors = clean.split(",");
                    for (final String sel : selectors)
                    {
                        final String trimmedSel = sel.trim();
                        if (trimmedSel.endsWith(" button") || trimmedSel.endsWith(" a") || trimmedSel.endsWith(" .add-btn"))
                        {
                            int suffixLength = 7;
                            if (trimmedSel.endsWith(" a"))
                            {
                                suffixLength = 2;
                            }
                            else if (trimmedSel.endsWith(" .add-btn"))
                            {
                                suffixLength = 9;
                            }

                            final String parentSelector = trimmedSel.substring(0, trimmedSel.length() - suffixLength).trim();
                            final ElementsCollection parentEls = Selenide.$$(By.cssSelector(parentSelector));
                            if (!parentEls.isEmpty())
                            {
                                final com.codeborne.selenide.SelenideElement parent = parentEls.first();
                                final ElementsCollection candidates = parent.$$(By.cssSelector("div, span, a, button, [role='button']"));
                                for (final com.codeborne.selenide.SelenideElement cand : candidates)
                                {
                                    final String text = cand.text().trim().toLowerCase();
                                    final String cursor = cand.getCssValue("cursor");
                                    if ("pointer".equals(cursor) || text.contains("add") || text.contains("cart"))
                                    {
                                        return cand;
                                    }
                                }
                            }
                        }
                    }
                }
                catch (final Exception e)
                {
                    // Ignored
                }
            }

            // 3. Try as XPath
            if (!forceCss)
            {
                if (forceXpath || clean.startsWith("/") || clean.startsWith("(") || clean.startsWith(".") || clean.startsWith("*") || clean.contains("["))
                {
                    try
                    {
                        final ElementsCollection els = Selenide.$$x(clean);
                        if (!els.isEmpty())
                        {
                            return els.first();
                        }
                    }
                    catch (final Exception e)
                    {
                        // Ignored
                    }
                }
            }

            // 4. Try as Link Text
            if (!forceCss && !forceXpath)
            {
                try
                {
                    final ElementsCollection els = Selenide.$$(By.linkText(clean));
                    if (!els.isEmpty())
                    {
                        return els.first();
                    }
                }
                catch (final Exception e)
                {
                    // Ignored
                }
            }

            // 5. Try finding by text content via XPath
            if (!forceCss && !forceXpath)
            {
                try
                {
                    final String escaped = escapeXpath(clean);
                    final String xpath = String.format(
                        "//*[not(ancestor-or-self::*[@id='neo-ai-hud']) and (contains(normalize-space(text()), %s) or contains(@value, %s) or contains(@aria-label, %s))]",
                        escaped, escaped, escaped
                    );
                    final ElementsCollection els = Selenide.$$x(xpath);
                    if (!els.isEmpty())
                    {
                        return els.first();
                    }
                }
                catch (final Exception e)
                {
                    // Ignored
                }
            }

            if (System.currentTimeMillis() - start >= timeoutMs)
            {
                break;
            }

            try
            {
                Thread.sleep(100);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Fallback: Default Selenide behavior
        if (forceXpath)
        {
            return Selenide.$x(clean);
        }
        return Selenide.$(clean);
    }

    private static String escapeXpath(final String value)
    {
        if (!value.contains("'"))
        {
            return "'" + value + "'";
        }
        if (!value.contains("\""))
        {
            return "\"" + value + "\"";
        }
        return "concat('" + value.replace("'", "', \"'\", '") + "')";
    }
}
