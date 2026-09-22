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
import java.util.Locale;
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

        // 1. Chained Playwright selectors (e.g. "#header >> button" or ".card >> text=Buy")
        if (clean.contains(">>"))
        {
            final By chained = resolveChainedLocator(clean);
            if (chained != null)
            {
                return chained;
            }
        }

        final String lower = clean.toLowerCase(Locale.ROOT);

        // 2. Explicit prefixes: xpath= and css=
        if (lower.startsWith("xpath="))
        {
            return By.xpath(clean.substring(6).trim());
        }
        if (lower.startsWith("css="))
        {
            return resolveLocator(clean.substring(4).trim());
        }

        // 3. Neodymium Automation Reference ID shorthand (e.g. "data-ai=xc123")
        if (lower.startsWith("data-ai="))
        {
            final int eqIdx = clean.indexOf('=');
            final String refId = unquote(clean.substring(eqIdx + 1).trim());
            return By.cssSelector("[data-ai='" + refId + "']");
        }

        // 4. Test ID selectors (e.g. "data-testid=submit-btn", "testid=submit-btn", "data-test=submit-btn")
        if (lower.startsWith("data-testid=") || lower.startsWith("testid=") || lower.startsWith("data-test="))
        {
            final int eqIdx = clean.indexOf('=');
            final String testId = unquote(clean.substring(eqIdx + 1).trim());
            return By.cssSelector("[data-testid='" + testId + "']");
        }

        // 5. Attribute shorthands (id=, placeholder=, alt=, title=, label=)
        if (lower.startsWith("id="))
        {
            final String idVal = unquote(clean.substring(3).trim());
            return By.cssSelector("#" + idVal);
        }
        if (lower.startsWith("placeholder="))
        {
            final String placeholderVal = unquote(clean.substring(12).trim());
            return By.cssSelector("[placeholder='" + placeholderVal + "']");
        }
        if (lower.startsWith("alt="))
        {
            final String altVal = unquote(clean.substring(4).trim());
            return By.cssSelector("[alt='" + altVal + "']");
        }
        if (lower.startsWith("title="))
        {
            final String titleVal = unquote(clean.substring(6).trim());
            return By.cssSelector("[title='" + titleVal + "']");
        }
        if (lower.startsWith("label="))
        {
            final String labelVal = unquote(clean.substring(6).trim());
            final String escaped = escapeXpath(labelVal);
            return By.xpath("//input[@id=//label[normalize-space(.)=" + escaped + "]/@for] | //label[normalize-space(.)=" + escaped + "]//input | //*[@aria-label=" + escaped + "]");
        }

        // 6. Playwright role selectors (e.g. "role=button[name='Submit']", "role=button")
        if (lower.startsWith("role="))
        {
            final By roleBy = resolveRoleLocator(clean);
            if (roleBy != null)
            {
                return roleBy;
            }
        }

        // 7. XPath Expressions
        if (clean.startsWith("/") || clean.startsWith("("))
        {
            return By.xpath(clean);
        }

        // 8. Playwright text= / text*= / text: / text*: / has-text= / has-text*= / has-text: / has-text*:
        if (lower.startsWith("text=") || lower.startsWith("text*=") || lower.startsWith("text:") || lower.startsWith("text*:")
                || lower.startsWith("has-text=") || lower.startsWith("has-text*=") || lower.startsWith("has-text:") || lower.startsWith("has-text*:"))
        {
            final int delimIdx = clean.indexOf(clean.contains("=") ? '=' : ':');
            final String rawVal = clean.substring(delimIdx + 1).trim();
            final boolean isQuoted = (rawVal.startsWith("\"") && rawVal.endsWith("\"")) || (rawVal.startsWith("'") && rawVal.endsWith("'"));
            final String textVal = unquote(rawVal);
            if (!textVal.isEmpty())
            {
                return isQuoted ? Selectors.byText(textVal) : Selectors.withText(textVal);
            }
        }

        // 9. Playwright/jQuery pseudo-selectors (e.g. div:has-text("..."), span:contains('...'), :text("..."))
        final Matcher matcher = PLAYWRIGHT_PSEUDO_PATTERN.matcher(clean);
        if (matcher.matches())
        {
            final String tag = matcher.group(1).trim();
            final String pseudoType = matcher.group(2).trim().toLowerCase(Locale.ROOT);
            final String rawVal = matcher.group(3).trim();
            final String textVal = unquote(rawVal);
            final boolean isExact = "text-is".equals(pseudoType) || "has-text-is".equals(pseudoType) || "exact-text".equals(pseudoType);
            if (tag.isEmpty() || "*".equals(tag))
            {
                return isExact ? Selectors.byText(textVal) : Selectors.withText(textVal);
            }
            return buildPseudoSelectorXpath(tag, textVal, isExact);
        }

        // 10. Explicit Shadow DOM targets (e.g. host-el ::shadow button)
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

        // 11. Standard CSS Selector with fallback safety for non-standard pseudo syntax (e.g. text:nth-of-type(4))
        if (clean.toLowerCase(Locale.ROOT).startsWith("text:"))
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
        final String textCondition = (textVal == null) ? null : (isExact
            ? "(normalize-space(.)=" + escapeXpath(textVal) + " or normalize-space(text())=" + escapeXpath(textVal) + ")"
            : "contains(normalize-space(.), " + escapeXpath(textVal) + ")");

        if (cssPrefix == null || cssPrefix.isBlank() || "*".equals(cssPrefix.trim()))
        {
            if (textCondition == null)
            {
                return By.xpath("//*");
            }
            return By.xpath("//*[not(self::html or self::body or self::head) and " + textCondition + "]");
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

        if ("*".equals(tag) && extraCondition != null && !extraCondition.isBlank())
        {
            conditions.add("not(self::html or self::body or self::head)");
        }

        if (conditions.isEmpty())
        {
            return tag;
        }

        return tag + "[" + String.join(" and ", conditions) + "]";
    }

    /**
     * Strips leading and trailing single or double quotes from a string if enclosed.
     *
     * @param s the raw input string
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
     * Translates a Playwright ARIA role selector (e.g. {@code role=button[name="Submit"]} or {@code role=button})
     * into a standard Selenium locator.
     *
     * @param clean the sanitized role selector string
     * @return the resolved {@link By} locator, or {@code null} if parsing fails
     */
    private static By resolveRoleLocator(final String clean)
    {
        final Matcher matcher = Pattern.compile("^role=([a-zA-Z0-9_-]+)(?:\\[(.*?)\\])?$", Pattern.CASE_INSENSITIVE).matcher(clean);
        if (!matcher.matches())
        {
            return null;
        }

        final String role = matcher.group(1).toLowerCase(Locale.ROOT);
        final String attrs = matcher.group(2);
        String nameVal = null;

        if (attrs != null && !attrs.isBlank())
        {
            final Matcher nameMatcher = Pattern.compile("name=(?:\"([^\"]*)\"|'([^']*)'|([^,\\]]+))").matcher(attrs);
            if (nameMatcher.find())
            {
                if (nameMatcher.group(1) != null)
                {
                    nameVal = nameMatcher.group(1);
                }
                else if (nameMatcher.group(2) != null)
                {
                    nameVal = nameMatcher.group(2);
                }
                else
                {
                    nameVal = nameMatcher.group(3).trim();
                }
            }
        }

        if (nameVal != null)
        {
            final String escaped = escapeXpath(nameVal);
            if ("button".equals(role))
            {
                return By.xpath("//button[contains(normalize-space(.), " + escaped + ") or @value=" + escaped + " or @aria-label=" + escaped + "]"
                        + " | //input[(@type='button' or @type='submit') and (contains(normalize-space(.), " + escaped + ") or @value=" + escaped + " or @aria-label=" + escaped + ")]"
                        + " | //*[@role='button' and (contains(normalize-space(.), " + escaped + ") or @aria-label=" + escaped + " or @value=" + escaped + ")]");
            }
            if ("link".equals(role))
            {
                return By.xpath("//a[contains(normalize-space(.), " + escaped + ") or @aria-label=" + escaped + "]"
                        + " | //*[@role='link' and (contains(normalize-space(.), " + escaped + ") or @aria-label=" + escaped + ")]");
            }
            if ("heading".equals(role))
            {
                return By.xpath("//*[self::h1 or self::h2 or self::h3 or self::h4 or self::h5 or self::h6 or @role='heading'][contains(normalize-space(.), " + escaped + ") or @aria-label=" + escaped + "]");
            }
            return By.xpath("//*[@role='" + role + "' and (contains(normalize-space(.), " + escaped + ") or @aria-label=" + escaped + " or @value=" + escaped + " or @title=" + escaped + ")]");
        }

        if ("button".equals(role))
        {
            return By.cssSelector("button, input[type='button'], input[type='submit'], [role='button']");
        }
        if ("link".equals(role))
        {
            return By.cssSelector("a, [role='link']");
        }
        if ("heading".equals(role))
        {
            return By.cssSelector("h1, h2, h3, h4, h5, h6, [role='heading']");
        }
        if ("checkbox".equals(role))
        {
            return By.cssSelector("input[type='checkbox'], [role='checkbox']");
        }
        if ("radio".equals(role))
        {
            return By.cssSelector("input[type='radio'], [role='radio']");
        }
        if ("textbox".equals(role))
        {
            return By.cssSelector("input:not([type]), input[type='text'], input[type='email'], input[type='password'], textarea, [role='textbox']");
        }

        return By.cssSelector("[role='" + role + "']");
    }

    /**
     * Resolves Playwright chained selectors (delimited by {@code >>}) into an integrated XPath locator.
     *
     * @param clean the raw selector containing {@code >>}
     * @return a resolved {@link By} XPath locator
     */
    private static By resolveChainedLocator(final String clean)
    {
        final String[] parts = clean.split("\\s*>>\\s*");
        if (parts.length <= 1)
        {
            return null;
        }

        final StringBuilder xpath = new StringBuilder();

        for (int i = 0; i < parts.length; i++)
        {
            final String part = parts[i].trim();
            final String lowerPart = part.toLowerCase(Locale.ROOT);
            final String prefix = "//";

            if (lowerPart.startsWith("text=") || lowerPart.startsWith("has-text="))
            {
                final int eqIdx = part.indexOf('=');
                final String rawVal = part.substring(eqIdx + 1).trim();
                final boolean isExact = (rawVal.startsWith("\"") && rawVal.endsWith("\"")) || (rawVal.startsWith("'") && rawVal.endsWith("'"));
                final String textVal = unquote(rawVal);
                final String textCondition = isExact
                        ? "normalize-space(.)=" + escapeXpath(textVal)
                        : "contains(normalize-space(.), " + escapeXpath(textVal) + ")";
                xpath.append(prefix).append("*[not(self::html or self::body or self::head) and ").append(textCondition).append("]");
            }
            else if (lowerPart.startsWith("role="))
            {
                final By roleBy = resolveRoleLocator(part);
                if (roleBy instanceof By.ByXPath byXPath)
                {
                    String roleXpath = byXPath.toString();
                    if (roleXpath.startsWith("By.xpath: "))
                    {
                        roleXpath = roleXpath.substring(10).trim();
                    }
                    if (roleXpath.startsWith("//"))
                    {
                        roleXpath = roleXpath.substring(2);
                    }
                    xpath.append(prefix).append("(").append(roleXpath).append(")");
                }
                else
                {
                    xpath.append(prefix).append(toXPathSegment(part));
                }
            }
            else
            {
                xpath.append(prefix).append(toXPathSegment(part));
            }
        }

        return By.xpath(xpath.toString());
    }

    /**
     * Converts a single CSS or XPath segment to a relative XPath expression without leading slashes.
     *
     * @param segment the segment to convert
     * @return relative XPath segment string
     */
    private static String toXPathSegment(final String segment)
    {
        final String cleanSeg = segment.trim();
        if (cleanSeg.startsWith("xpath="))
        {
            String xp = cleanSeg.substring(6).trim();
            while (xp.startsWith("/"))
            {
                xp = xp.substring(1);
            }
            return xp;
        }
        if (cleanSeg.startsWith("/"))
        {
            String xp = cleanSeg;
            while (xp.startsWith("/"))
            {
                xp = xp.substring(1);
            }
            return xp;
        }
        final By by = buildPseudoSelectorXpath(cleanSeg, null, false);
        String xp = by.toString();
        if (xp.startsWith("By.xpath: "))
        {
            xp = xp.substring(10).trim();
        }
        while (xp.startsWith("/"))
        {
            xp = xp.substring(1);
        }
        return xp;
    }
}
