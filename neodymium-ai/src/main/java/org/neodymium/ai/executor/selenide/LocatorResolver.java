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
package org.neodymium.ai.executor.selenide;

import java.util.Locale;

import org.neodymium.ai.executor.selenide.selector.PlaywrightBy;
import org.neodymium.ai.executor.selenide.selector.PlaywrightSelectorParser;
import org.openqa.selenium.By;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;

/**
 * Centralized locator resolution and sanitizer for Neodymium AI Selenide/Selenium execution.
 * <p>
 * Translates Playwright selectors, CSS, XPath, Neodymium automation IDs (data-ai),
 * and pseudo-selectors into {@link PlaywrightBy} locators that execute across
 * open Shadow DOM boundaries in document order.
 * </p>
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class LocatorResolver
{
    private LocatorResolver()
    {
        // Utility class
    }

    /**
     * Resolves a target selector string into a Selenium/Selenide {@link By} locator.
     * <p>
     * Pure XPath expressions (starting with {@code //}, {@code (}, or {@code xpath=}) resolve
     * to native {@link By#xpath(String)} (and per Playwright spec, strictly do not pierce shadow DOM).
     * All other selectors are validated via {@link PlaywrightSelectorParser} and resolved to
     * {@link PlaywrightBy}, enabling transparent open Shadow DOM traversal, Playwright text matching,
     * ARIA roles, and layout pseudo-classes.
     * </p>
     *
     * @param target the target selector string
     * @return a resolved {@link By} locator
     * @throws org.openqa.selenium.InvalidSelectorException if the selector syntax is invalid or unsupported
     */
    public static By resolveLocator(final String target)
    {
        if (target == null || target.isBlank())
        {
            return By.cssSelector("*");
        }
        final String clean = target.trim();

        // 1. Explicit XPath expressions: //..., (...), or xpath=...
        // Per Playwright specification, XPath strictly does NOT pierce shadow DOM.
        final String lower = clean.toLowerCase(Locale.ROOT);
        if (clean.startsWith("/") || clean.startsWith("(") || lower.startsWith("xpath="))
        {
            final String xpath = lower.startsWith("xpath=")
                    ? clean.substring(6).trim()
                    : clean;
            return By.xpath(xpath);
        }

        // 2. Legacy explicit ::shadow syntax for backwards compatibility
        if (clean.contains("::shadow"))
        {
            final String[] parts = clean.split("::shadow");
            final String shadowTarget = parts[parts.length - 1].trim();
            final String[] shadowHosts = new String[parts.length - 1];
            for (int i = 0; i < parts.length - 1; i++)
            {
                shadowHosts[i] = parts[i].trim();
            }
            return Selectors.shadowCss(shadowTarget, shadowHosts);
        }

        // 3. Playwright selector engine validation and resolution
        // Parse and validate upfront via PlaywrightSelectorParser; throws InvalidSelectorException with actionable diagnostics
        PlaywrightSelectorParser.parse(clean);
        return PlaywrightBy.of(clean);
    }

    /**
     * Executes element lookup in Selenide using resolved locators.
     *
     * @param target the target selector string
     * @return an {@link ElementsCollection} matching the target
     * @throws org.openqa.selenium.InvalidSelectorException if the selector syntax is invalid or unsupported
     */
    public static ElementsCollection findElements(final String target)
    {
        if (target == null || target.isBlank())
        {
            return Selenide.$$("*");
        }
        final String clean = target.trim();
        final By locator = resolveLocator(clean);
        return Selenide.$$(locator);
    }

    /**
     * Safely escapes string values for insertion into XPath string literals.
     *
     * @param value the raw string to escape
     * @return an XPath-safe string literal expression
     */
    public static String escapeXpath(final String value)
    {
        if (value == null)
        {
            return "''";
        }
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

    /**
     * Strips enclosing single or double quotes from a string if present.
     *
     * @param s the raw string
     * @return the unquoted string
     */
    public static String unquote(final String s)
    {
        if (s == null)
        {
            return "";
        }
        final String trimmed = s.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'")))
        {
            if (trimmed.length() >= 2)
            {
                return trimmed.substring(1, trimmed.length() - 1);
            }
        }
        return trimmed;
    }

    /**
     * Translates a CSS selector prefix paired with Playwright text pseudo conditions
     * into a resolved {@link By} locator.
     *
     * @param cssPrefix the CSS selector prefix
     * @param textVal the text value to match
     * @param isExact whether the text match is exact or substring
     * @return a resolved {@link By} locator
     */
    public static By buildPseudoSelectorXpath(final String cssPrefix, final String textVal, final boolean isExact)
    {
        if (textVal == null)
        {
            return resolveLocator(cssPrefix);
        }
        final String pseudo = isExact ? ":text-is(" + quoteString(textVal) + ")" : ":has-text(" + quoteString(textVal) + ")";
        if (cssPrefix == null || cssPrefix.isBlank() || "*".equals(cssPrefix.trim()))
        {
            return resolveLocator(pseudo);
        }
        return resolveLocator(cssPrefix.trim() + pseudo);
    }

    private static String quoteString(final String val)
    {
        if (!val.contains("\""))
        {
            return "\"" + val + "\"";
        }
        return "'" + val + "'";
    }
}
