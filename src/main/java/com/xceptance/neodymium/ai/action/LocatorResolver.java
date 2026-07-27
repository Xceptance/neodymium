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
package com.xceptance.neodymium.ai.action;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;

/**
 * Centralized locator resolution and sanitizer for Neodymium AI Selenide/Selenium execution.
 * <p>
 * Translates standard CSS, XPath, Neodymium automation IDs (data-neo-ref),
 * Shadow DOM selectors, and Playwright/jQuery vendor pseudo-selectors (such as
 * {@code text=...}, {@code :has-text("...")}, and {@code :contains("...")}) into
 * W3C-compliant Selenium/Selenide {@link By} locators.
 * </p>
 * <p>
 * <b>Engine Note:</b> This translator is strictly used by the Selenide/Selenium execution
 * pipeline (e.g. {@link ActionExecutor} and {@code SelenideElementFinder}). Native Playwright
 * execution drivers execute Playwright selector strings directly without requiring translation.
 * </p>
 *
 * @author AI-generated: Gemini 3.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class LocatorResolver
{
    private static final Logger LOG = LoggerFactory.getLogger(LocatorResolver.class);

    private static final Pattern PLAYWRIGHT_PSEUDO_PATTERN = Pattern.compile(
            "^(.*?):(has-text|contains|text)\\((.*?)\\)$", Pattern.CASE_INSENSITIVE);

    private LocatorResolver()
    {
        // Utility class
    }

    /**
     * Resolves a target selector string into a Selenium/Selenide {@link By} locator.
     * <p>
     * Handles XPath, Playwright text prefixes, Playwright/jQuery pseudo-selectors,
     * Shadow DOM hosts, and standard CSS selectors.
     * </p>
     *
     * @param target the target selector string
     * @return a resolved {@link By} locator
     */
    public static By resolveLocator(final String target)
    {
        if (target == null || target.isBlank())
        {
            return By.cssSelector("*");
        }
        final String clean = target.trim();

        // 1. Neodymium Automation Reference ID shorthand (e.g. "c12" or "neo-ref=c12" or "xc_c12")
        if (clean.toLowerCase().startsWith("neo-ref=") || clean.toLowerCase().startsWith("data-neo-ref="))
        {
            final int eqIdx = clean.indexOf('=');
            final String refId = clean.substring(eqIdx + 1).trim();
            return By.cssSelector("[data-neo-ref='" + refId + "']");
        }

        // 2. XPath Expressions
        if (clean.startsWith("/") || clean.startsWith("("))
        {
            return By.xpath(clean);
        }

        // 3. Playwright text= / text: prefix (e.g. text=Total Paid: $27.58 or text:Total Paid: $27.58)
        if (clean.toLowerCase().startsWith("text=") || clean.toLowerCase().startsWith("text:")
                || clean.toLowerCase().startsWith("has-text=") || clean.toLowerCase().startsWith("has-text:"))
        {
            final int delimIdx = clean.indexOf(clean.contains("=") ? '=' : ':');
            String textVal = clean.substring(delimIdx + 1).trim();
            if ((textVal.startsWith("\"") && textVal.endsWith("\"")) || (textVal.startsWith("'") && textVal.endsWith("'")))
            {
                if (textVal.length() >= 2)
                {
                    textVal = textVal.substring(1, textVal.length() - 1);
                }
            }
            if (!textVal.isEmpty())
            {
                return Selectors.withText(textVal);
            }
        }

        // 4. Playwright/jQuery pseudo-selectors (e.g. div:has-text("..."), span:contains('...'), :text("..."))
        final Matcher matcher = PLAYWRIGHT_PSEUDO_PATTERN.matcher(clean);
        if (matcher.matches())
        {
            final String tag = matcher.group(1).trim();
            String textVal = matcher.group(3).trim();
            if ((textVal.startsWith("\"") && textVal.endsWith("\"")) || (textVal.startsWith("'") && textVal.endsWith("'")))
            {
                if (textVal.length() >= 2)
                {
                    textVal = textVal.substring(1, textVal.length() - 1);
                }
            }
            if (tag.isEmpty() || "*".equals(tag))
            {
                return Selectors.withText(textVal);
            }
            return By.xpath("//" + tag + "[contains(normalize-space(.), " + escapeXpath(textVal) + ")]");
        }

        // 5. Explicit Shadow DOM targets (e.g. host-el ::shadow button)
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

        // 6. Standard CSS Selector with fallback safety for non-standard pseudo syntax (e.g. text:nth-of-type(4))
        if (clean.toLowerCase().startsWith("text:"))
        {
            final String afterPrefix = clean.substring(5).trim();
            if (afterPrefix.startsWith("nth-") || afterPrefix.startsWith(":"))
            {
                return By.cssSelector("*" + afterPrefix);
            }
            return Selectors.withText(afterPrefix);
        }

        return By.cssSelector(clean);
    }

    /**
     * Safely executes element lookup in Selenide using resolved locators, falling back to
     * text search if invalid CSS syntax is encountered.
     *
     * @param target the target selector string
     * @return an {@link ElementsCollection} matching the target
     */
    public static ElementsCollection findElements(final String target)
    {
        if (target == null || target.isBlank())
        {
            return Selenide.$$("*");
        }
        final String clean = target.trim();

        try
        {
            final By locator = resolveLocator(clean);
            final ElementsCollection elements = Selenide.$$(locator);
            if (!elements.isEmpty())
            {
                return elements;
            }
        }
        catch (final InvalidSelectorException e)
        {
            LOG.warn("⚠️ Invalid CSS selector '{}', falling back to text search: {}", clean, e.getMessage());
            return Selenide.$$(Selectors.withText(clean));
        }
        catch (final Exception e)
        {
            LOG.debug("🔍 Locator resolution failed for target '{}': {}", clean, e.getMessage());
        }

        // Fallback: try direct text matching
        return Selenide.$$(Selectors.withText(clean));
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
}
