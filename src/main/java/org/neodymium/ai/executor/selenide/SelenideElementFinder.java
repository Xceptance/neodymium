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
import java.util.ArrayList;
import java.util.List;
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

        final long start = System.currentTimeMillis();
        final long timeoutMs = Configuration.timeout;

        while (true)
        {
            final List<String> candidates = splitCandidates(target);
            for (final String candidate : candidates)
            {
                final SelenideElement found = tryResolveCandidate(candidate);
                if (found != null)
                {
                    return found;
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

        // Final Fallback: Default Selenide behavior to throw appropriate Selenide ElementNotFound exception
        final String firstCandidate = splitCandidates(target).get(0);
        if (firstCandidate.startsWith("/") || firstCandidate.startsWith("("))
        {
            return Selenide.$x(firstCandidate);
        }
        return Selenide.$(resolveLocator(firstCandidate));
    }

    /**
     * Attempts to resolve a single candidate locator across Neodymium ID, CSS, XPath, and text matching.
     */
    private static SelenideElement tryResolveCandidate(final String rawCandidate)
    {
        if (rawCandidate == null || rawCandidate.trim().isEmpty())
        {
            return null;
        }

        String clean = rawCandidate.trim();
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

        // -------------------------------------------------------------------------
        // Strategy 1: Neodymium Automation ID (xc_...) extraction
        // -------------------------------------------------------------------------
        if (!forceXpath && clean.contains("xc_"))
        {
            final java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(xc_[a-zA-Z0-9_\\-]+)").matcher(clean);
            if (matcher.find())
            {
                final String neoId = matcher.group(1);
                try
                {
                    final ElementsCollection els = Selenide.$$(By.cssSelector("[data-neo-ref='" + neoId + "']"));
                    if (!els.isEmpty())
                    {
                        return els.first();
                    }

                    // Dynamically stamp data-neo-ref attributes into live DOM if absent
                    try
                    {
                        new PageAnalyzer(WebDriverRunner.getWebDriver()).captureSimplifiedDom(ContextLevel.LEAN);
                    }
                    catch (final Exception ignored)
                    {
                    }

                    final ElementsCollection retryEls = Selenide.$$(By.cssSelector("[data-neo-ref='" + neoId + "']"));
                    if (!retryEls.isEmpty())
                    {
                        return retryEls.first();
                    }
                }
                catch (final Exception ignored)
                {
                }
            }
        }

        // -------------------------------------------------------------------------
        // Strategy 2: Playwright Pseudo-Selector Translation (:has-text, :text, :contains, text=..., has-text=...)
        // -------------------------------------------------------------------------
        if (!forceXpath && !forceCss)
        {
            final String lower = clean.toLowerCase();
            if (lower.startsWith("text=") || lower.startsWith("has-text="))
            {
                String text = clean.substring(clean.indexOf('=') + 1).trim();
                if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'")))
                {
                    if (text.length() >= 2)
                    {
                        text = text.substring(1, text.length() - 1);
                    }
                }
                if (!text.isEmpty())
                {
                    try
                    {
                        final String escaped = escapeXpath(text);
                        final String xpath = String.format(
                            "//*[not(ancestor-or-self::*[@id='neo-ai-hud']) and (contains(normalize-space(text()), %s) or contains(normalize-space(.), %s) or contains(@value, %s) or contains(@aria-label, %s))]",
                            escaped, escaped, escaped, escaped
                        );
                        final ElementsCollection els = Selenide.$$x(xpath);
                        if (!els.isEmpty())
                        {
                            return els.first();
                        }
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
            }

            final java.util.regex.Matcher pwMatcher = java.util.regex.Pattern.compile("^(.*?):(has-text|text|contains)\\(['\"]?(.*?)['\"]?\\)$", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(clean);
            if (pwMatcher.find())
            {
                String tag = pwMatcher.group(1).trim();
                if (tag.isEmpty())
                {
                    tag = "*";
                }
                final String text = pwMatcher.group(3).trim();
                if (!text.isEmpty())
                {
                    try
                    {
                        final String xpath = String.format("//%s[contains(normalize-space(.), %s)]", tag, escapeXpath(text));
                        final ElementsCollection els = Selenide.$$x(xpath);
                        if (!els.isEmpty())
                        {
                            return els.first();
                        }
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
            }
        }

        // -------------------------------------------------------------------------
        // Strategy 3: Standard CSS Selector / Playwright Pseudo / Text Selector
        // -------------------------------------------------------------------------
        if (!forceXpath)
        {
            try
            {
                final ElementsCollection els = Selenide.$$(resolveLocator(clean));
                if (!els.isEmpty())
                {
                    return els.first();
                }
            }
            catch (final Exception ignored)
            {
            }
        }

        // -------------------------------------------------------------------------
        // Strategy 4: XPath Expression
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
                catch (final Exception ignored)
                {
                }
            }
        }

        // -------------------------------------------------------------------------
        // Strategy 5: Link Text Matching
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
            catch (final Exception ignored)
            {
            }
        }

        // -------------------------------------------------------------------------
        // Strategy 6: Text Content Searching
        // -------------------------------------------------------------------------
        if (!forceCss && !forceXpath && !clean.contains("<") && !clean.contains(">"))
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
            catch (final Exception ignored)
            {
            }
        }

        return null;
    }

    /**
     * Splits a comma-separated target string into individual candidate locators, respecting quotes and brackets.
     */
    private static List<String> splitCandidates(final String target)
    {
        final List<String> result = new java.util.ArrayList<>();
        if (target == null)
        {
            return result;
        }

        final StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int bracketDepth = 0;

        for (int i = 0; i < target.length(); i++)
        {
            final char c = target.charAt(i);
            if (c == '\'' && !inDoubleQuote)
            {
                inSingleQuote = !inSingleQuote;
            }
            else if (c == '"' && !inSingleQuote)
            {
                inDoubleQuote = !inDoubleQuote;
            }
            else if (c == '[' && !inSingleQuote && !inDoubleQuote)
            {
                bracketDepth++;
            }
            else if (c == ']' && !inSingleQuote && !inDoubleQuote)
            {
                bracketDepth--;
            }

            if (c == ',' && !inSingleQuote && !inDoubleQuote && bracketDepth <= 0)
            {
                final String candidate = current.toString().trim();
                if (!candidate.isEmpty())
                {
                    result.add(candidate);
                }
                current.setLength(0);
            }
            else
            {
                current.append(c);
            }
        }

        final String finalCandidate = current.toString().trim();
        if (!finalCandidate.isEmpty())
        {
            result.add(finalCandidate);
        }

        if (result.isEmpty())
        {
            result.add(target.trim());
        }

        return result;
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

    public static By resolveLocator(final String target)
    {
        return com.xceptance.neodymium.ai.action.LocatorResolver.resolveLocator(target);
    }
}
