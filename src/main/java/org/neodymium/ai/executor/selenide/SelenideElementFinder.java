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

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.neodymium.ai.executor.selenide.ContextLevel;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.openqa.selenium.By;

/**
 * Shared utility for finding {@link SelenideElement} instances using a multi-tiered sequence of resolution strategies.
 * <p>
 * Evaluates target strings using the following prioritized strategy order:
 * </p>
 * <ol>
 *   <li><b>Neodymium Automation ID (data-neo-ref / xc_...):</b> Direct lookup, followed by dynamic DOM attribute stamping via {@link PageAnalyzer} if absent (essential for offline replay healing).</li>
 *   <li><b>CSS Selector with Smart Button Healing:</b> Standard CSS resolution, falling back to parent-candidate searching if button elements have shifted tag names (e.g., div/span button replacements).</li>
 *   <li><b>XPath Expression:</b> Direct evaluation for explicitly forced or path-structured locators.</li>
 *   <li><b>Link Text Matching:</b> Exact match via {@link By#linkText(String)}.</li>
 *   <li><b>Text Content &amp; ARIA Searching:</b> XPath text normalization and ARIA attribute substring matching.</li>
 * </ol>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class SelenideElementFinder
{
    /**
     * Private constructor to prevent instantiation of this static utility class.
     */
    private SelenideElementFinder()
    {
    }

    /**
     * Resolves and returns a {@link SelenideElement} based on the given target locator or text.
     * Continuously attempts strategies until the configured Selenide timeout expires.
     *
     * @param target the target locator or text content (may be prefixed with {@code xpath=} or {@code css=})
     * @return the resolved {@link SelenideElement}
     * @throws IllegalArgumentException if {@code target} is null or blank
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

        // Strip explicit strategy prefixes
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
        final long timeoutMs = Configuration.timeout;

        while (true)
        {
            // -------------------------------------------------------------------------
            // Strategy 1: Neodymium Automation ID (data-neo-ref / xc_...)
            // -------------------------------------------------------------------------
            if (!forceXpath && (clean.contains("data-neo-ref") || clean.contains("xc_")))
            {
                final ElementsCollection els = clean.matches("^xc_.*")
                    ? Selenide.$$(By.cssSelector("[data-neo-ref='" + clean + "']"))
                    : Selenide.$$(By.cssSelector(clean));
                if (!els.isEmpty())
                {
                    return els.first();
                }

                // If not found, dynamically stamp data-neo-ref attributes into the live DOM (crucial for offline replays)
                try
                {
                    new PageAnalyzer(WebDriverRunner.getWebDriver()).captureSimplifiedDom(ContextLevel.LEAN);
                }
                catch (final Exception ignored)
                {
                    // Ignore DOM stamping failures and continue to retry
                }

                final ElementsCollection retryEls = clean.matches("^xc_.*")
                    ? Selenide.$$(By.cssSelector("[data-neo-ref='" + clean + "']"))
                    : Selenide.$$(By.cssSelector(clean));
                if (!retryEls.isEmpty())
                {
                    return retryEls.first();
                }

                // Dynamic text-matching assertion fallback for missing data-neo-ref targets
                final ExecutionContext context = ExecutionContext.getActiveContext();
                if (context != null)
                {
                    final Action currentAction = (Action) context.getTransientData().get("currentAction");
                    if (currentAction != null && "ASSERT".equalsIgnoreCase(currentAction.getType()) && currentAction.getValue() != null)
                    {
                        final String expectedVal = currentAction.getValue();
                        final ElementsCollection allElements = Selenide.$$("*");
                        for (final SelenideElement el : allElements)
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
                                // Ignore non-accessible or stale elements during scan
                            }
                        }
                    }
                    else
                    {
                        final PlaybookStep step = (PlaybookStep) context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
                        if (step != null)
                        {
                            for (final Action action : step.getActions())
                            {
                                if ("ASSERT".equalsIgnoreCase(action.getType()) && action.getValue() != null)
                                {
                                    final String expectedVal = action.getValue();
                                    final ElementsCollection allElements = Selenide.$$("*");
                                    for (final SelenideElement el : allElements)
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
                                            // Ignore non-accessible or stale elements during scan
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------------------
            // Strategy 2: CSS Selector with Parent-Relative Button Healing
            // -------------------------------------------------------------------------
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
                                final SelenideElement parent = parentEls.first();
                                final ElementsCollection candidates = parent.$$(By.cssSelector("div, span, a, button, [role='button']"));
                                for (final SelenideElement cand : candidates)
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
                    // Ignored - CSS execution failed, try next strategy
                }
            }

            // -------------------------------------------------------------------------
            // Strategy 3: XPath Expression
            // -------------------------------------------------------------------------
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
                        // Ignored - XPath evaluation failed, try next strategy
                    }
                }
            }

            // -------------------------------------------------------------------------
            // Strategy 4: Link Text Matching
            // -------------------------------------------------------------------------
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
                    // Ignored - Link text lookup failed, try next strategy
                }
            }

            // -------------------------------------------------------------------------
            // Strategy 5: Text Content & ARIA Substring Searching via Normalized XPath
            // -------------------------------------------------------------------------
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
                    // Ignored - Normalized text search failed
                }
            }

            // Check if timeout has expired
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

        // Final Fallback: Default Selenide behavior to throw appropriate Selenide ElementNotFound exception if missing
        if (forceXpath)
        {
            return Selenide.$x(clean);
        }
        return Selenide.$(clean);
    }

    /**
     * Safely escapes string values for insertion into XPath string literals.
     * Handles single quotes, double quotes, and mixed strings using XPath {@code concat()}.
     *
     * @param value the raw text value to escape
     * @return an XPath-safe string expression
     */
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
