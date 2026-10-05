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
            "^(.*?):(has-text|has-text\\*|has-text-is|contains|text|text\\*|text-is|exact-text)\\(((?:[^()]|\"[^\"]*\"|'[^']*')*)\\)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern EXTENDED_PSEUDO_PATTERN = Pattern.compile(
            ":(has-text|has-text\\*|has-text-is|contains|text|text\\*|text-is|exact-text)\\(", Pattern.CASE_INSENSITIVE);

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

        // 0. Pre-validate unsupported vendor pseudo-classes and layout selectors
        if (clean.matches("(?i).*:(visible|hidden)\\b.*"))
        {
            final String pseudo = clean.toLowerCase(Locale.ROOT).contains(":visible") ? ":visible" : ":hidden";
            throw new InvalidSelectorException("Unsupported selector syntax '" + clean + "': pseudo-class '"
                    + pseudo
                    + "' is not supported in W3C WebDriver. Neodymium automatically verifies element visibility during interactions. Please use standard CSS or text locators (e.g. 'button' or 'text=Submit').");
        }

        final Matcher spatialMatcher = Pattern.compile(":(right-of|left-of|above|below|near)\\(", Pattern.CASE_INSENSITIVE).matcher(clean);
        if (spatialMatcher.find())
        {
            final String pseudo = spatialMatcher.group(1).toLowerCase(Locale.ROOT);
            throw new InvalidSelectorException("Unsupported selector syntax '" + clean + "': Spatial layout selector ':" + pseudo
                    + "()' is not supported in W3C WebDriver. Please use standard hierarchical selectors, CSS combinators, or chained '>>' locators.");
        }

        final Matcher unsupportedPseudos = Pattern.compile(":text-matches\\(", Pattern.CASE_INSENSITIVE).matcher(clean);
        if (unsupportedPseudos.find())
        {
            throw new InvalidSelectorException("Unsupported selector syntax '" + clean + "': Playwright ':text-matches()' pseudo-class is not supported in W3C WebDriver. Please use standard CSS or text locators.");
        }

        // Playwright :nth-match(selector, N) or selector:nth-match(N)
        final Matcher nthMatchMatcher = Pattern.compile("^(.*?):nth-match\\((?:(.+?),\\s*)?(\\d+)\\)$", Pattern.CASE_INSENSITIVE).matcher(clean);
        if (nthMatchMatcher.matches())
        {
            final String prefix = nthMatchMatcher.group(1).trim();
            final String inner = nthMatchMatcher.group(2) != null ? nthMatchMatcher.group(2).trim() : prefix;
            final int index = Integer.parseInt(nthMatchMatcher.group(3).trim());
            final String seg = toXPathSegment(inner);
            return By.xpath("(//" + seg + ")[" + index + "]");
        }

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

        // Playwright standalone nth=N (e.g. "nth=0", "nth=2", "nth=-1")
        if (lower.replaceAll("\\s+", "").matches("^nth=\\-?\\d+$"))
        {
            final int index = Integer.parseInt(lower.replaceAll("\\s+", "").substring(4));
            if (index >= 0)
            {
                return By.xpath("(//*)[ " + (index + 1) + " ]");
            }
            else if (index == -1)
            {
                return By.xpath("(//*)[last()]");
            }
            else
            {
                final int offset = Math.abs(index) - 1;
                return By.xpath("(//*)[last()-" + offset + "]");
            }
        }

        // 2. Playwright codegen internal prefixes (e.g. internal:role=..., internal:text=...)
        if (lower.startsWith("internal:"))
        {
            String unwrapped = clean.substring(9).trim();
            // Normalize case-insensitive flag in attributes: e.g. [name="Submit"i] -> [name="Submit"]
            unwrapped = unwrapped.replaceAll("(?i)\\[([a-zA-Z0-9_-]+)=([\"'])(.*?)\\2i\\]", "[$1=$2$3$2]");
            return resolveLocator(unwrapped);
        }

        // 3. Explicit prefixes: xpath= and css=
        if (lower.startsWith("xpath="))
        {
            return By.xpath(clean.substring(6).trim());
        }
        if (lower.startsWith("css="))
        {
            return resolveLocator(clean.substring(4).trim());
        }

        // 4. Neodymium Automation Reference ID shorthand (e.g. "data-ai=xc123", "#xc123", or selectors containing "#xc...")
        if (clean.matches(".*#xc[a-zA-Z0-9_\\-]+.*"))
        {
            final String transformed = clean.replaceAll("#(xc[a-zA-Z0-9_\\-]+)", "[data-ai='$1']");
            return resolveLocator(transformed);
        }
        if (clean.matches("^xc[a-zA-Z0-9_\\-]+$"))
        {
            return By.cssSelector("[data-ai='" + clean + "']");
        }
        if (lower.startsWith("data-ai="))
        {
            final int eqIdx = clean.indexOf('=');
            final String refId = unquote(clean.substring(eqIdx + 1).trim());
            return By.cssSelector("[data-ai='" + refId + "']");
        }

        // 5. Test ID selectors (e.g. "data-testid=submit-btn", "testid=submit-btn", "data-test=submit-btn", "data-test-id=submit-btn")
        if (lower.startsWith("data-test-id="))
        {
            final int eqIdx = clean.indexOf('=');
            final String testId = unquote(clean.substring(eqIdx + 1).trim());
            return By.cssSelector("[data-test-id='" + testId + "']");
        }
        if (lower.startsWith("data-test="))
        {
            final int eqIdx = clean.indexOf('=');
            final String testId = unquote(clean.substring(eqIdx + 1).trim());
            return By.cssSelector("[data-test='" + testId + "'], [data-testid='" + testId + "']");
        }
        if (lower.startsWith("data-testid=") || lower.startsWith("testid="))
        {
            final int eqIdx = clean.indexOf('=');
            final String testId = unquote(clean.substring(eqIdx + 1).trim());
            return By.cssSelector("[data-testid='" + testId + "']");
        }

        // 6. Attribute shorthands (id=, placeholder=, alt=, title=, label=)
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
            return By.xpath("//*[self::input or self::select or self::textarea or self::button][@id=//label[normalize-space(.)=" + escaped + "]/@for]"
                    + " | //label[normalize-space(.)=" + escaped + "]//*[self::input or self::select or self::textarea or self::button]"
                    + " | //*[@aria-label=" + escaped + "]");
        }

        // 7. Playwright role selectors (e.g. "role=button[name='Submit']", "role=button")
        if (lower.startsWith("role="))
        {
            final By roleBy = resolveRoleLocator(clean);
            if (roleBy != null)
            {
                return roleBy;
            }
        }

        // 8. XPath Expressions
        if (clean.startsWith("/") || clean.startsWith("("))
        {
            return By.xpath(clean);
        }

        // 9. CSS pseudo-class/pseudo-element fallback on text tags (e.g. text:nth-of-type(4), text::before)
        if (lower.startsWith("text:"))
        {
            final String afterPrefix = clean.substring(5).trim();
            if (afterPrefix.startsWith("nth-") || afterPrefix.startsWith(":") || afterPrefix.startsWith("first-") || afterPrefix.startsWith("last-"))
            {
                return By.cssSelector("*" + clean.substring(clean.indexOf(':')).trim());
            }
        }

        // 10. Playwright text= / text*= / text: / text*: / has-text= / has-text*= / has-text: / has-text*:
        if (lower.startsWith("text=") || lower.startsWith("text*=") || lower.startsWith("text:") || lower.startsWith("text*:")
                || lower.startsWith("has-text=") || lower.startsWith("has-text*=") || lower.startsWith("has-text:") || lower.startsWith("has-text*:"))
        {
            final boolean isColon = lower.startsWith("text:") || lower.startsWith("text*:")
                    || lower.startsWith("has-text:") || lower.startsWith("has-text*:");
            final int delimIdx = clean.indexOf(isColon ? ':' : '=');
            final String rawVal = clean.substring(delimIdx + 1).trim();
            final boolean isQuoted = (rawVal.startsWith("\"") && rawVal.endsWith("\"")) || (rawVal.startsWith("'") && rawVal.endsWith("'"));
            final String textVal = unquote(rawVal);
            if (!textVal.isEmpty())
            {
                return isQuoted ? Selectors.byText(textVal) : Selectors.withText(textVal);
            }
        }

        // 11. Playwright/jQuery pseudo-selectors (e.g. div:has-text("..."), span:contains('...'), :text("..."))
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

        if (EXTENDED_PSEUDO_PATTERN.matcher(clean).find())
        {
            return buildPseudoSelectorXpath(clean, null, false);
        }

        // 12. Explicit Shadow DOM targets (e.g. host-el ::shadow button)
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

        final List<String> tokens = tokenizeCssSelector(cssPrefix.trim());
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

        // Match substring text pseudo-classes: :has-text(...), :has-text*(...), :contains(...), :text(...), :text*(...)
        final String[] subTextPseudos = {"has-text", "has-text*", "contains", "text", "text*"};
        for (final String pseudo : subTextPseudos)
        {
            for (final String textArg : extractBalancedPseudoArgs(rest, pseudo))
            {
                final String textVal = unquote(textArg);
                conditions.add("contains(normalize-space(.), " + escapeXpath(textVal) + ")");
            }
        }

        // Match exact text pseudo-classes: :has-text-is(...), :text-is(...), :exact-text(...)
        final String[] exactTextPseudos = {"has-text-is", "text-is", "exact-text"};
        for (final String pseudo : exactTextPseudos)
        {
            for (final String textArg : extractBalancedPseudoArgs(rest, pseudo))
            {
                final String textVal = unquote(textArg);
                conditions.add("(normalize-space(.)=" + escapeXpath(textVal) + " or normalize-space(text())=" + escapeXpath(textVal) + ")");
            }
        }

        // Match :has(...) e.g. :has(td) or :has(.btn) or :has([role="cell"]) or :has(> h3:text-is("Order Summary"))
        for (final String inner : extractBalancedPseudoArgs(rest, "has"))
        {
            final List<String> subInners = splitSelectorList(inner);
            final List<String> hasOrConditions = new ArrayList<>();
            for (final String sub : subInners)
            {
                String target = sub.trim();
                String axis = "descendant::";
                if (target.startsWith(">"))
                {
                    axis = "child::";
                    target = target.substring(1).trim();
                }
                hasOrConditions.add(axis + toXPathSegment(target));
            }
            if (hasOrConditions.size() == 1)
            {
                conditions.add(hasOrConditions.get(0));
            }
            else if (!hasOrConditions.isEmpty())
            {
                conditions.add("(" + String.join(" or ", hasOrConditions) + ")");
            }
        }

        // Match :not(...) e.g. :not(th) or :not(.header) or :not(:has(...))
        for (final String inner : extractBalancedPseudoArgs(rest, "not"))
        {
            if (inner.startsWith(":has(") && inner.endsWith(")"))
            {
                final String nested = inner.substring(5, inner.length() - 1).trim();
                conditions.add("not(descendant::" + toXPathSegment(nested) + ")");
            }
            else if (inner.startsWith("."))
            {
                conditions.add("not(contains(concat(' ', normalize-space(@class), ' '), ' " + inner.substring(1) + " '))");
            }
            else
            {
                final String target = toXPathSegment(inner);
                conditions.add("not(self::" + target + " or descendant::" + target + ")");
            }
        }

        String cleanedRest = rest;
        for (final String pseudo : subTextPseudos)
        {
            cleanedRest = stripBalancedPseudo(cleanedRest, pseudo);
        }
        for (final String pseudo : exactTextPseudos)
        {
            cleanedRest = stripBalancedPseudo(cleanedRest, pseudo);
        }
        cleanedRest = stripBalancedPseudo(cleanedRest, "has");
        cleanedRest = stripBalancedPseudo(cleanedRest, "not");

        // Match attributes: [name='value'] or [required]
        final Matcher attrMatcher = Pattern.compile("\\[([a-zA-Z0-9_-]+)(?:([*^$|~]?=)(['\"]?)(.*?)\\3)?\\]").matcher(cleanedRest);
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
        final Matcher idMatcher = Pattern.compile("#([a-zA-Z0-9_-]+)").matcher(cleanedRest);
        while (idMatcher.find())
        {
            conditions.add("@id=" + escapeXpath(idMatcher.group(1)));
        }

        // Match classes: .class
        final Matcher classMatcher = Pattern.compile("\\.([a-zA-Z0-9_-]+)").matcher(cleanedRest);
        while (classMatcher.find())
        {
            conditions.add("contains(concat(' ', normalize-space(@class), ' '), ' " + classMatcher.group(1) + " ')");
        }

        // Match :nth-child(N)
        final Matcher nthChildMatcher = Pattern.compile(":nth-child\\((\\d+)\\)").matcher(cleanedRest);
        while (nthChildMatcher.find())
        {
            conditions.add("position()=" + nthChildMatcher.group(1));
        }

        // Match :nth-of-type(N)
        final Matcher nthOfTypeMatcher = Pattern.compile(":nth-of-type\\((\\d+)\\)").matcher(cleanedRest);
        while (nthOfTypeMatcher.find())
        {
            conditions.add("position()=" + nthOfTypeMatcher.group(1));
        }

        if (cleanedRest.contains(":first-child") || cleanedRest.contains(":first-of-type"))
        {
            conditions.add("position()=1");
        }

        if (cleanedRest.contains(":last-child") || cleanedRest.contains(":last-of-type"))
        {
            conditions.add("position()=last()");
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
     * to Selenide's native W3C {@link Selectors#byRole}.
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
            return Selectors.byRole(role, nameVal);
        }

        return Selectors.byRole(role);
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
            String part = parts[i].trim();
            if (part.toLowerCase(Locale.ROOT).startsWith("internal:"))
            {
                part = part.substring(9).trim().replaceAll("(?i)\\[([a-zA-Z0-9_-]+)=([\"'])(.*?)\\2i\\]", "[$1=$2$3$2]");
            }
            final String lowerPart = part.toLowerCase(Locale.ROOT);
            final String normalizedPart = lowerPart.replaceAll("\\s+", "");
            final String prefix = "//";

            if (normalizedPart.matches("^nth=\\-?\\d+$")
                    || "first".equals(normalizedPart) || "first()".equals(normalizedPart)
                    || "last".equals(normalizedPart) || "last()".equals(normalizedPart))
            {
                final int index;
                if ("first".equals(normalizedPart) || "first()".equals(normalizedPart))
                {
                    index = 0;
                }
                else if ("last".equals(normalizedPart) || "last()".equals(normalizedPart))
                {
                    index = -1;
                }
                else
                {
                    index = Integer.parseInt(normalizedPart.substring(4));
                }

                final String current = xpath.length() == 0 ? "//*" : xpath.toString();
                xpath.setLength(0);
                if (index >= 0)
                {
                    final int xpathIndex = index + 1;
                    xpath.append("(").append(current).append(")[").append(xpathIndex).append("]");
                }
                else if (index == -1)
                {
                    xpath.append("(").append(current).append(")[last()]");
                }
                else
                {
                    final int offset = Math.abs(index) - 1;
                    xpath.append("(").append(current).append(")[last()-").append(offset).append("]");
                }
                continue;
            }

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
                xpath.append(prefix).append(resolveRoleXPathForChain(part));
            }
            else if (lowerPart.startsWith("id="))
            {
                final String idVal = unquote(part.substring(3).trim());
                xpath.append(prefix).append("*[@id=").append(escapeXpath(idVal)).append("]");
            }
            else if (lowerPart.startsWith("data-test-id="))
            {
                final int eqIdx = part.indexOf('=');
                final String testId = unquote(part.substring(eqIdx + 1).trim());
                xpath.append(prefix).append("*[@data-test-id=").append(escapeXpath(testId)).append("]");
            }
            else if (lowerPart.startsWith("data-test="))
            {
                final int eqIdx = part.indexOf('=');
                final String testId = unquote(part.substring(eqIdx + 1).trim());
                xpath.append(prefix).append("*[@data-test=").append(escapeXpath(testId))
                        .append(" or @data-testid=").append(escapeXpath(testId)).append("]");
            }
            else if (lowerPart.startsWith("data-testid=") || lowerPart.startsWith("testid="))
            {
                final int eqIdx = part.indexOf('=');
                final String testId = unquote(part.substring(eqIdx + 1).trim());
                xpath.append(prefix).append("*[@data-testid=").append(escapeXpath(testId)).append("]");
            }
            else if (lowerPart.startsWith("data-ai="))
            {
                final int eqIdx = part.indexOf('=');
                final String refId = unquote(part.substring(eqIdx + 1).trim());
                xpath.append(prefix).append("*[@data-ai=").append(escapeXpath(refId)).append("]");
            }
            else if (lowerPart.startsWith("placeholder="))
            {
                final String placeholderVal = unquote(part.substring(12).trim());
                xpath.append(prefix).append("*[@placeholder=").append(escapeXpath(placeholderVal)).append("]");
            }
            else if (lowerPart.startsWith("alt="))
            {
                final String altVal = unquote(part.substring(4).trim());
                xpath.append(prefix).append("*[@alt=").append(escapeXpath(altVal)).append("]");
            }
            else if (lowerPart.startsWith("title="))
            {
                final String titleVal = unquote(part.substring(6).trim());
                xpath.append(prefix).append("*[@title=").append(escapeXpath(titleVal)).append("]");
            }
            else if (lowerPart.startsWith("label="))
            {
                // The standalone label= locator is a union of three location paths. A union cannot be
                // appended to a scope with "//" (the former "//( a | b | c )" is invalid XPath 1.0),
                // so the same three associations are expressed as one predicate on the descendant axis:
                //   1. form control referenced by label/@for
                //   2. form control nested in a matching label (ancestor axis replaces //label//control)
                //   3. any element carrying a matching aria-label
                final String labelVal = unquote(part.substring(6).trim());
                final String escaped = escapeXpath(labelVal);
                xpath.append(prefix).append("*[((self::input or self::select or self::textarea or self::button)")
                        .append(" and (@id=//label[normalize-space(.)=").append(escaped).append("]/@for")
                        .append(" or ancestor::label[normalize-space(.)=").append(escaped).append("]))")
                        .append(" or @aria-label=").append(escaped).append("]");
            }
            else
            {
                if (hasCssCombinator(part))
                {
                    final By resolvedPart = buildPseudoSelectorXpath(part, null, false);
                    String rawPart = resolvedPart.toString();
                    if (rawPart.startsWith("By.xpath: "))
                    {
                        rawPart = rawPart.substring(10).trim();
                    }
                    if (rawPart.startsWith("//"))
                    {
                        rawPart = rawPart.substring(2);
                    }
                    xpath.append(prefix).append(rawPart);
                }
                else
                {
                    xpath.append(prefix).append(toXPathSegment(part));
                }
            }
        }

        return By.xpath(xpath.toString());
    }

    /**
     * Checks whether a CSS selector segment contains combinators (whitespace or {@code >}) outside of quotes,
     * brackets, or parentheses.
     *
     * @param selector the CSS selector to check
     * @return {@code true} if an unnested CSS combinator is present
     */
    private static boolean hasCssCombinator(final String selector)
    {
        if (selector == null || selector.isBlank())
        {
            return false;
        }
        int parenDepth = 0;
        int bracketDepth = 0;
        boolean inQuote = false;
        char quoteChar = 0;

        for (int i = 0; i < selector.length(); i++)
        {
            final char c = selector.charAt(i);
            if (inQuote)
            {
                if (c == quoteChar)
                {
                    inQuote = false;
                }
            }
            else if (c == '"' || c == '\'')
            {
                inQuote = true;
                quoteChar = c;
            }
            else if (c == '(')
            {
                parenDepth++;
            }
            else if (c == ')')
            {
                parenDepth = Math.max(0, parenDepth - 1);
            }
            else if (c == '[')
            {
                bracketDepth++;
            }
            else if (c == ']')
            {
                bracketDepth = Math.max(0, bracketDepth - 1);
            }
            else if (parenDepth == 0 && bracketDepth == 0)
            {
                if (Character.isWhitespace(c) || c == '>')
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tokenizes a CSS selector into segments and combinators, respecting quoted strings, attribute brackets,
     * and pseudo-class parentheses.
     *
     * @param selector the CSS selector to tokenize
     * @return a list of tokens and combinators
     */
    private static List<String> tokenizeCssSelector(final String selector)
    {
        final List<String> tokens = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        int bracketDepth = 0;
        boolean inQuote = false;
        char quoteChar = 0;

        for (int i = 0; i < selector.length(); i++)
        {
            final char c = selector.charAt(i);
            if (inQuote)
            {
                current.append(c);
                if (c == quoteChar)
                {
                    inQuote = false;
                }
            }
            else if (c == '"' || c == '\'')
            {
                inQuote = true;
                quoteChar = c;
                current.append(c);
            }
            else if (c == '(')
            {
                parenDepth++;
                current.append(c);
            }
            else if (c == ')')
            {
                parenDepth = Math.max(0, parenDepth - 1);
                current.append(c);
            }
            else if (c == '[')
            {
                bracketDepth++;
                current.append(c);
            }
            else if (c == ']')
            {
                bracketDepth = Math.max(0, bracketDepth - 1);
                current.append(c);
            }
            else if (parenDepth == 0 && bracketDepth == 0 && (Character.isWhitespace(c) || c == '>'))
            {
                if (current.length() > 0)
                {
                    tokens.add(current.toString().trim());
                    current.setLength(0);
                }
                if (c == '>')
                {
                    tokens.add(">");
                }
            }
            else
            {
                current.append(c);
            }
        }
        if (current.length() > 0 && !current.toString().isBlank())
        {
            tokens.add(current.toString().trim());
        }
        return tokens;
    }

    /**
     * Extracts argument contents of balanced pseudo-classes such as {@code :has(...)} or {@code :not(...)}
     * from a CSS selector substring, respecting parenthesis, bracket, and quote nesting so inner
     * pseudo-classes are not misidentified as top-level arguments.
     *
     * @param input the raw selector substring
     * @param pseudoName the pseudo-class name without colon or parentheses
     * @return a list of inner arguments for matched occurrences
     */
    private static List<String> extractBalancedPseudoArgs(final String input, final String pseudoName)
    {
        final List<String> results = new ArrayList<>();
        if (input == null || input.isBlank())
        {
            return results;
        }
        final String prefix = ":" + pseudoName + "(";
        int parenDepth = 0;
        int bracketDepth = 0;
        boolean inQuote = false;
        char quoteChar = 0;

        for (int i = 0; i < input.length(); i++)
        {
            final char c = input.charAt(i);
            if (inQuote)
            {
                if (c == quoteChar)
                {
                    inQuote = false;
                }
            }
            else if (c == '"' || c == '\'')
            {
                inQuote = true;
                quoteChar = c;
            }
            else if (c == '(')
            {
                parenDepth++;
            }
            else if (c == ')')
            {
                parenDepth = Math.max(0, parenDepth - 1);
            }
            else if (c == '[')
            {
                bracketDepth++;
            }
            else if (c == ']')
            {
                bracketDepth = Math.max(0, bracketDepth - 1);
            }
            else if (parenDepth == 0 && bracketDepth == 0 && i + prefix.length() <= input.length()
                    && input.regionMatches(true, i, prefix, 0, prefix.length()))
            {
                final int start = i + prefix.length();
                int depth = 1;
                int pos = start;
                boolean innerQuote = false;
                char innerQuoteChar = 0;
                while (pos < input.length() && depth > 0)
                {
                    final char ic = input.charAt(pos);
                    if (innerQuote)
                    {
                        if (ic == innerQuoteChar)
                        {
                            innerQuote = false;
                        }
                    }
                    else if (ic == '"' || ic == '\'')
                    {
                        innerQuote = true;
                        innerQuoteChar = ic;
                    }
                    else if (ic == '(')
                    {
                        depth++;
                    }
                    else if (ic == ')')
                    {
                        depth--;
                    }
                    pos++;
                }
                if (depth == 0)
                {
                    results.add(input.substring(start, pos - 1).trim());
                    i = pos - 1;
                }
            }
        }
        return results;
    }

    /**
     * Strips balanced pseudo-classes such as {@code :has-text(...)} from a selector string so that
     * embedded punctuation (e.g. dots in numbers, brackets, quotes) does not interfere with standard CSS matching.
     * Respects parenthesis, bracket, and quote nesting so nested pseudo-classes are not prematurely stripped.
     *
     * @param input the raw selector substring
     * @param pseudoName the pseudo-class name without colon or parentheses
     * @return the selector string with matching balanced pseudo occurrences removed
     */
    private static String stripBalancedPseudo(final String input, final String pseudoName)
    {
        if (input == null || input.isBlank())
        {
            return input;
        }
        final String prefix = ":" + pseudoName + "(";
        final StringBuilder sb = new StringBuilder();
        int parenDepth = 0;
        int bracketDepth = 0;
        boolean inQuote = false;
        char quoteChar = 0;

        for (int i = 0; i < input.length(); i++)
        {
            final char c = input.charAt(i);
            if (inQuote)
            {
                sb.append(c);
                if (c == quoteChar)
                {
                    inQuote = false;
                }
            }
            else if (c == '"' || c == '\'')
            {
                inQuote = true;
                quoteChar = c;
                sb.append(c);
            }
            else if (c == '(')
            {
                parenDepth++;
                sb.append(c);
            }
            else if (c == ')')
            {
                parenDepth = Math.max(0, parenDepth - 1);
                sb.append(c);
            }
            else if (c == '[')
            {
                bracketDepth++;
                sb.append(c);
            }
            else if (c == ']')
            {
                bracketDepth = Math.max(0, bracketDepth - 1);
                sb.append(c);
            }
            else if (parenDepth == 0 && bracketDepth == 0 && i + prefix.length() <= input.length()
                    && input.regionMatches(true, i, prefix, 0, prefix.length()))
            {
                final int start = i + prefix.length();
                int depth = 1;
                int pos = start;
                boolean innerQuote = false;
                char innerQuoteChar = 0;
                while (pos < input.length() && depth > 0)
                {
                    final char ic = input.charAt(pos);
                    if (innerQuote)
                    {
                        if (ic == innerQuoteChar)
                        {
                            innerQuote = false;
                        }
                    }
                    else if (ic == '"' || ic == '\'')
                    {
                        innerQuote = true;
                        innerQuoteChar = ic;
                    }
                    else if (ic == '(')
                    {
                        depth++;
                    }
                    else if (ic == ')')
                    {
                        depth--;
                    }
                    pos++;
                }
                if (depth == 0)
                {
                    i = pos - 1;
                }
                else
                {
                    sb.append(c);
                }
            }
            else
            {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Splits a comma-separated list of selector strings, respecting quotes, attribute brackets,
     * and parentheses.
     *
     * @param input the raw selector list
     * @return list of split selectors
     */
    private static List<String> splitSelectorList(final String input)
    {
        final List<String> results = new ArrayList<>();
        if (input == null || input.isBlank())
        {
            return results;
        }
        final StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        int bracketDepth = 0;
        boolean inQuote = false;
        char quoteChar = 0;

        for (int i = 0; i < input.length(); i++)
        {
            final char c = input.charAt(i);
            if (inQuote)
            {
                current.append(c);
                if (c == quoteChar)
                {
                    inQuote = false;
                }
            }
            else if (c == '"' || c == '\'')
            {
                inQuote = true;
                quoteChar = c;
                current.append(c);
            }
            else if (c == '(')
            {
                parenDepth++;
                current.append(c);
            }
            else if (c == ')')
            {
                parenDepth = Math.max(0, parenDepth - 1);
                current.append(c);
            }
            else if (c == '[')
            {
                bracketDepth++;
                current.append(c);
            }
            else if (c == ']')
            {
                bracketDepth = Math.max(0, bracketDepth - 1);
                current.append(c);
            }
            else if (c == ',' && parenDepth == 0 && bracketDepth == 0)
            {
                if (!current.toString().isBlank())
                {
                    results.add(current.toString().trim());
                }
                current.setLength(0);
            }
            else
            {
                current.append(c);
            }
        }
        if (!current.toString().isBlank())
        {
            results.add(current.toString().trim());
        }
        return results;
    }

    /**
     * Resolves a bare ARIA role into an XPath element predicate suitable for chained locators.
     *
     * @param role the lowercased role name
     * @return XPath relative step
     */
    private static String resolveRoleXPathForChain(final String part)
    {
        final Matcher matcher = Pattern.compile("^role=([a-zA-Z0-9_-]+)(?:\\[(.*?)\\])?$", Pattern.CASE_INSENSITIVE).matcher(part);
        if (!matcher.matches())
        {
            return "*";
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
                return "*[(self::button or (self::input and (@type='button' or @type='submit')) or @role='button') and (contains(normalize-space(.), " + escaped + ") or @value=" + escaped + " or @aria-label=" + escaped + ")]";
            }
            if ("link".equals(role))
            {
                return "*[(self::a or @role='link') and (contains(normalize-space(.), " + escaped + ") or @aria-label=" + escaped + ")]";
            }
            if ("heading".equals(role))
            {
                return "*[(self::h1 or self::h2 or self::h3 or self::h4 or self::h5 or self::h6 or @role='heading') and (contains(normalize-space(.), " + escaped + ") or @aria-label=" + escaped + ")]";
            }
            if ("row".equals(role))
            {
                return "*[(self::tr or @role='row') and (contains(normalize-space(.), " + escaped + ") or @aria-label=" + escaped + ")]";
            }
            if ("cell".equals(role) || "gridcell".equals(role))
            {
                return "*[(self::td or self::th or @role='cell' or @role='gridcell') and (contains(normalize-space(.), " + escaped + ") or @aria-label=" + escaped + ")]";
            }
            if ("tab".equals(role))
            {
                return "*[@role='tab' and (contains(normalize-space(.), " + escaped + ") or @aria-label=" + escaped + ")]";
            }
            return "*[@role='" + role + "' and (contains(normalize-space(.), " + escaped + ") or @aria-label=" + escaped + " or @value=" + escaped + " or @title=" + escaped + ")]";
        }
        return resolveBareRoleXPath(role);
    }

    private static String resolveBareRoleXPath(final String role)
    {
        if ("button".equals(role))
        {
            return "*[self::button or (self::input and (@type='button' or @type='submit')) or @role='button']";
        }
        if ("link".equals(role))
        {
            return "*[self::a or @role='link']";
        }
        if ("heading".equals(role))
        {
            return "*[self::h1 or self::h2 or self::h3 or self::h4 or self::h5 or self::h6 or @role='heading']";
        }
        if ("checkbox".equals(role))
        {
            return "*[self::input[@type='checkbox'] or @role='checkbox']";
        }
        if ("radio".equals(role))
        {
            return "*[self::input[@type='radio'] or @role='radio']";
        }
        if ("textbox".equals(role))
        {
            return "*[self::textarea or (self::input and (not(@type) or @type='text' or @type='email' or @type='password')) or @role='textbox']";
        }
        if ("row".equals(role))
        {
            return "*[self::tr or @role='row']";
        }
        if ("cell".equals(role) || "gridcell".equals(role))
        {
            return "*[self::td or self::th or @role='cell' or @role='gridcell']";
        }
        if ("table".equals(role))
        {
            return "*[self::table or @role='table' or @role='grid']";
        }
        if ("tab".equals(role))
        {
            return "*[@role='tab']";
        }
        return "*[@role='" + role + "']";
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
        if (cleanSeg.startsWith(">"))
        {
            return "child::" + toXPathSegment(cleanSeg.substring(1).trim());
        }
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
        final Matcher pseudoMatcher = PLAYWRIGHT_PSEUDO_PATTERN.matcher(cleanSeg);
        if (pseudoMatcher.matches())
        {
            final String tag = pseudoMatcher.group(1).trim();
            final String pseudoType = pseudoMatcher.group(2).trim().toLowerCase(Locale.ROOT);
            final String rawVal = pseudoMatcher.group(3).trim();
            final String textVal = unquote(rawVal);
            final boolean isExact = "text-is".equals(pseudoType) || "has-text-is".equals(pseudoType) || "exact-text".equals(pseudoType);
            final By by = buildPseudoSelectorXpath(tag.isEmpty() ? "*" : tag, textVal, isExact);
            return stripLeadingSlashes(by);
        }

        final By by = buildPseudoSelectorXpath(cleanSeg, null, false);
        return stripLeadingSlashes(by);
    }

    private static String stripLeadingSlashes(final By by)
    {
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
