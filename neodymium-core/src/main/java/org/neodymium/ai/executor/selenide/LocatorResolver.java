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

import java.util.ArrayList;
import java.util.List;
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
 * Translates standard CSS, XPath, Neodymium automation IDs (data-ai),
 * Shadow DOM selectors, and Playwright/jQuery vendor pseudo-selectors (such as
 * {@code text=...}, {@code :has-text("...")}, and {@code :contains("...")}) into
 * W3C-compliant Selenium/Selenide {@link By} locators.
 * </p>
 *
 * @author AI-generated: Gemini 3.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class LocatorResolver
{
    private static final Logger LOG = LoggerFactory.getLogger(LocatorResolver.class);

    private static final Pattern PLAYWRIGHT_PSEUDO_PATTERN = Pattern.compile(
            "^(.*?):(has-text|has-text\\*|has-text-is|contains|text|text\\*|text-is|exact-text)\\((.*?)\\)$", Pattern.CASE_INSENSITIVE);

    private LocatorResolver()
    {
        // Utility class
    }

    /**
     * Resolves a target selector string into a Selenium/Selenide {@link By} locator.
     * <p>
     * Handles XPath, Playwright text prefixes (including {@code text*=...}, {@code has-text*=...}),
     * Playwright/jQuery pseudo-selectors, Shadow DOM hosts, and standard CSS selectors.
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

        // 1. Neodymium Automation Reference ID shorthand (e.g. "data-ai=xc123")
        if (clean.toLowerCase().startsWith("data-ai="))
        {
            final int eqIdx = clean.indexOf('=');
            final String refId = clean.substring(eqIdx + 1).trim();
            return By.cssSelector("[data-ai='" + refId + "']");
        }

        // 2. XPath Expressions
        if (clean.startsWith("/") || clean.startsWith("("))
        {
            return By.xpath(clean);
        }

        // 3. Playwright text= / text*= / text: / text*: / has-text= / has-text*= / has-text: / has-text*:
        final String lower = clean.toLowerCase();
        if (lower.startsWith("text=") || lower.startsWith("text*=") || lower.startsWith("text:") || lower.startsWith("text*:")
                || lower.startsWith("has-text=") || lower.startsWith("has-text*=") || lower.startsWith("has-text:") || lower.startsWith("has-text*:"))
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
            final String pseudoType = matcher.group(2).trim().toLowerCase();
            String textVal = matcher.group(3).trim();
            if ((textVal.startsWith("\"") && textVal.endsWith("\"")) || (textVal.startsWith("'") && textVal.endsWith("'")))
            {
                if (textVal.length() >= 2)
                {
                    textVal = textVal.substring(1, textVal.length() - 1);
                }
            }
            final boolean isExact = "text-is".equals(pseudoType) || "has-text-is".equals(pseudoType) || "exact-text".equals(pseudoType);
            if (tag.isEmpty() || "*".equals(tag))
            {
                return isExact ? Selectors.byText(textVal) : Selectors.withText(textVal);
            }
            return buildPseudoSelectorXpath(tag, textVal, isExact);
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
            return Selenide.$$(locator);
        }
        catch (final InvalidSelectorException e)
        {
            LOG.warn("⚠️ Invalid selector '{}', falling back to text search: {}", clean, e.getMessage());
            return Selenide.$$(Selectors.withText(clean));
        }
        catch (final Exception e)
        {
            LOG.debug("🔍 Locator resolution failed for target '{}': {}", clean, e.getMessage());
            return Selenide.$$(Selectors.withText(clean));
        }
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
     * Translates a CSS selector prefix paired with Playwright text pseudo conditions
     * into a valid standard XPath expression.
     *
     * @param cssPrefix the CSS selector prefix (e.g. ".quick-add-dropdown.active button")
     * @param textVal the text value to match
     * @param isExact whether the text match is exact or substring
     * @return a resolved {@link By} XPath locator
     */
    public static By buildPseudoSelectorXpath(final String cssPrefix, final String textVal, final boolean isExact)
    {
        final String textCondition = isExact
            ? "(normalize-space(.)=" + escapeXpath(textVal) + " or normalize-space(text())=" + escapeXpath(textVal) + ")"
            : "contains(normalize-space(.), " + escapeXpath(textVal) + ")";

        if (cssPrefix == null || cssPrefix.isBlank() || "*".equals(cssPrefix.trim()))
        {
            return By.xpath("//*[" + textCondition + "]");
        }

        final String normalized = cssPrefix.trim().replaceAll("\\s*>\\s*", " > ");
        final String[] tokens = normalized.split("\\s+");
        final List<String> segments = new ArrayList<>();
        final List<Boolean> isChildCombinator = new ArrayList<>();
        boolean nextIsChild = false;

        for (final String token : tokens)
        {
            if (">".equals(token))
            {
                nextIsChild = true;
            }
            else
            {
                segments.add(token);
                isChildCombinator.add(nextIsChild);
                nextIsChild = false;
            }
        }

        final StringBuilder xpath = new StringBuilder();
        for (int i = 0; i < segments.size(); i++)
        {
            final String segment = segments.get(i);
            final boolean child = isChildCombinator.get(i);
            final boolean isLast = (i == segments.size() - 1);

            if (i == 0)
            {
                xpath.append("//");
            }
            else if (child)
            {
                xpath.append("/");
            }
            else
            {
                xpath.append("//");
            }

            xpath.append(buildSegmentPredicate(segment, isLast ? textCondition : null));
        }

        return By.xpath(xpath.toString());
    }

    /**
     * Builds an XPath tag and attribute predicate for a single CSS segment.
     *
     * @param segment the single CSS segment (e.g. "button.active" or "#confirmPassword")
     * @param extraCondition optional additional condition (e.g. text match condition) for the segment
     * @return formatted XPath segment string
     */
    private static String buildSegmentPredicate(final String segment, final String extraCondition)
    {
        String tag = "*";
        final List<String> conditions = new ArrayList<>();

        final Matcher tagMatcher = Pattern.compile("^([a-zA-Z0-9_-]+)").matcher(segment);
        int cursor = 0;
        if (tagMatcher.find())
        {
            tag = tagMatcher.group(1);
            cursor = tagMatcher.end();
        }

        final String rest = segment.substring(cursor);

        // Match attributes: [name='value'] or [required]
        final Matcher attrMatcher = Pattern.compile("\\[([a-zA-Z0-9_-]+)(?:([*^$|~]?=)(['\"]?)(.*?)\\3)?\\]").matcher(rest);
        while (attrMatcher.find())
        {
            final String attrName = attrMatcher.group(1);
            final String op = attrMatcher.group(2);
            final String attrVal = attrMatcher.group(4);

            if (op == null || attrVal == null)
            {
                conditions.add("@" + attrName);
            }
            else if ("=".equals(op))
            {
                conditions.add("@" + attrName + "=" + escapeXpath(attrVal));
            }
            else if ("*=".equals(op))
            {
                conditions.add("contains(@" + attrName + ", " + escapeXpath(attrVal) + ")");
            }
            else if ("^=".equals(op))
            {
                conditions.add("starts-with(@" + attrName + ", " + escapeXpath(attrVal) + ")");
            }
            else
            {
                conditions.add("@" + attrName + "=" + escapeXpath(attrVal));
            }
        }

        // Match IDs: #id
        final Matcher idMatcher = Pattern.compile("#([a-zA-Z0-9_-]+)").matcher(rest);
        while (idMatcher.find())
        {
            conditions.add("@id=" + escapeXpath(idMatcher.group(1)));
        }

        // Match classes: .class
        final Matcher classMatcher = Pattern.compile("\\.([a-zA-Z0-9_-]+)").matcher(rest);
        while (classMatcher.find())
        {
            conditions.add("contains(concat(' ', normalize-space(@class), ' '), ' " + classMatcher.group(1) + " ')");
        }

        if (extraCondition != null && !extraCondition.isBlank())
        {
            conditions.add(extraCondition);
        }

        if (conditions.isEmpty())
        {
            return tag;
        }

        return tag + "[" + String.join(" and ", conditions) + "]";
    }
}
