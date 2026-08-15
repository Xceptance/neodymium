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

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.action.LocatorCandidate;
import org.neodymium.ai.util.SelectorSyntaxChecker;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared utility for finding {@link SelenideElement} instances using a multi-tiered sequence of resolution strategies.
 * <p>
 * Evaluates target strings and candidate fallbacks using syntactic pre-classification and the following prioritized strategy order:
 * </p>
 * <ol>
 *   <li><b>Neodymium Automation ID (data-ai / xc_...):</b> Direct lookup, followed by dynamic DOM attribute stamping via {@link PageAnalyzer} if absent (essential for offline replay healing).</li>
 *   <li><b>Playwright Pseudo-Selector Translation:</b> Direct translation of {@code text=...}, {@code :has-text(...)}) into XPath.</li>
 *   <li><b>CSS Selector:</b> Standard CSS resolution via {@link LocatorResolver}.</li>
 *   <li><b>XPath Expression:</b> Direct evaluation for explicitly forced or path-structured locators.</li>
 *   <li><b>Link Text Matching:</b> Exact match via {@link By#linkText(String)}.</li>
 *   <li><b>Text Content &amp; ARIA Searching:</b> XPath text normalization and ARIA attribute substring matching for plain text queries.</li>
 * </ol>
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class SelenideElementFinder
{
    private static final Logger LOG = LoggerFactory.getLogger(SelenideElementFinder.class);
    private static final Map<String, Long> LAST_STAMP_TIMESTAMP_PER_URL = new ConcurrentHashMap<>();
    private static final long STAMP_THROTTLE_MS = 2000L;

    /**
     * Private constructor to prevent instantiation of this static utility class.
     */
    private SelenideElementFinder()
    {
    }

    /**
     * Checks if dynamic DOM attribute stamping should be attempted based on a 2-second per-URL throttle.
     *
     * @param driver the active WebDriver
     * @return true if DOM stamping should be attempted, false if throttled
     */
    private static boolean shouldAttemptDomStamp(final WebDriver driver)
    {
        if (driver == null)
        {
            return false;
        }
        try
        {
            final String url = driver.getCurrentUrl();
            if (url == null || url.isEmpty() || "data:,".equals(url) || "about:blank".equals(url))
            {
                return false;
            }
            final long now = System.currentTimeMillis();
            final Long lastStamp = LAST_STAMP_TIMESTAMP_PER_URL.get(url);
            if (lastStamp != null && (now - lastStamp) < STAMP_THROTTLE_MS)
            {
                return false;
            }
            LAST_STAMP_TIMESTAMP_PER_URL.put(url, now);
            return true;
        }
        catch (final Exception e)
        {
            return false;
        }
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
        return findElement(target, List.of());
    }

    /**
     * Resolves and returns a {@link SelenideElement} based on an {@link Action}, trying the primary target
     * and any alternative candidate locators attached to the action in confidence order.
     *
     * @param action the action containing primary target and candidate locators
     * @return the resolved {@link SelenideElement}
     * @throws IllegalArgumentException if {@code action} is null or has an empty target
     */
    public static SelenideElement findElement(final Action action)
    {
        if (action == null)
        {
            throw new IllegalArgumentException("Action cannot be null");
        }
        final List<String> fallbacks = new ArrayList<>();
        if (action.getCandidateLocators() != null)
        {
            for (final LocatorCandidate candidate : action.getCandidateLocators())
            {
                if (candidate != null && candidate.getLocator() != null && !candidate.getLocator().isBlank())
                {
                    fallbacks.add(candidate.getLocator());
                }
            }
        }
        return findElement(action.getTarget(), fallbacks);
    }

    /**
     * Resolves and returns a {@link SelenideElement} based on the given target locator and fallback candidates.
     * Continuously attempts strategies until the configured Selenide timeout expires.
     *
     * @param target             the primary target locator or text content
     * @param fallbackCandidates optional fallback candidate locators attempted in order
     * @return the resolved {@link SelenideElement}
     * @throws IllegalArgumentException if {@code target} is null or blank
     */
    public static SelenideElement findElement(final String target, final List<String> fallbackCandidates)
    {
        final Set<String> candidateSet = new LinkedHashSet<>();
        if (target != null && !target.isBlank())
        {
            candidateSet.addAll(splitCandidates(target));
        }
        if (fallbackCandidates != null)
        {
            for (final String fallback : fallbackCandidates)
            {
                if (fallback != null && !fallback.isBlank())
                {
                    candidateSet.addAll(splitCandidates(fallback));
                }
            }
        }

        if (candidateSet.isEmpty())
        {
            throw new IllegalArgumentException("Target cannot be empty");
        }

        final List<String> allCandidates = new ArrayList<>(candidateSet);
        final long start = System.currentTimeMillis();
        final long timeoutMs = Configuration.timeout;

        while (true)
        {
            for (final String candidate : allCandidates)
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
        final String firstCandidate = allCandidates.get(0);
        if (firstCandidate.startsWith("/") || firstCandidate.startsWith("("))
        {
            return Selenide.$x(firstCandidate);
        }
        return Selenide.$(resolveLocator(firstCandidate));
    }

    /**
     * Attempts to resolve a single candidate locator across Neodymium ID, CSS, XPath, and text matching
     * using targeted strategy dispatch based on syntactic pre-classification.
     *
     * @param rawCandidate the raw candidate locator string
     * @return the resolved visible SelenideElement, or null
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

        final SelectorSyntaxChecker.SelectorType type = SelectorSyntaxChecker.determineType(clean);

        // -------------------------------------------------------------------------
        // Strategy 1: Neodymium Automation ID (data-ai=... or #xc...) extraction
        // -------------------------------------------------------------------------
        if (!forceXpath && (clean.contains("data-ai=") || clean.startsWith("[data-ai=") || clean.matches(".*#xc[a-zA-Z0-9_\\-]+.*")))
        {
            final SelenideElement el = tryResolveAutomationId(clean);
            if (el != null)
            {
                return el;
            }
        }

        // -------------------------------------------------------------------------
        // Strategy 2: Playwright Pseudo-Selector Translation (:has-text, :text, :contains, text=..., has-text=...)
        // -------------------------------------------------------------------------
        if (!forceXpath && !forceCss && (type == SelectorSyntaxChecker.SelectorType.TEXT || clean.contains(":") || clean.contains("=")))
        {
            final SelenideElement el = tryResolvePlaywrightPseudo(clean);
            if (el != null)
            {
                return el;
            }
        }

        // -------------------------------------------------------------------------
        // Strategy 3: Standard CSS Selector
        // -------------------------------------------------------------------------
        if (!forceXpath && (forceCss || type == SelectorSyntaxChecker.SelectorType.CSS))
        {
            try
            {
                final ElementsCollection els = Selenide.$$(LocatorResolver.resolveLocator(clean));
                final SelenideElement visible = findFirstVisible(els, clean);
                if (visible != null)
                {
                    return visible;
                }
            }
            catch (final Exception ignored)
            {
            }
        }

        // -------------------------------------------------------------------------
        // Strategy 4: XPath Expression
        // -------------------------------------------------------------------------
        if (!forceCss && (forceXpath || type == SelectorSyntaxChecker.SelectorType.XPATH || clean.startsWith("/") || clean.startsWith("./") || clean.startsWith("(")))
        {
            try
            {
                final ElementsCollection els = Selenide.$$x(clean);
                final SelenideElement visible = findFirstVisible(els, clean);
                if (visible != null)
                {
                    return visible;
                }
            }
            catch (final Exception ignored)
            {
            }
        }

        // -------------------------------------------------------------------------
        // Strategy 5: Link Text Matching
        // -------------------------------------------------------------------------
        if (!forceCss && !forceXpath && type == SelectorSyntaxChecker.SelectorType.TEXT)
        {
            try
            {
                final ElementsCollection els = Selenide.$$(By.linkText(clean));
                final SelenideElement visible = findFirstVisible(els, clean);
                if (visible != null)
                {
                    return visible;
                }
            }
            catch (final Exception ignored)
            {
            }
        }

        // -------------------------------------------------------------------------
        // Strategy 6: Text Content Searching (ONLY for plain text queries, not structured CSS/XPath)
        // -------------------------------------------------------------------------
        if (!forceCss && !forceXpath && type == SelectorSyntaxChecker.SelectorType.TEXT && !clean.contains("<") && !clean.contains(">"))
        {
            try
            {
                final String escaped = escapeXpath(clean);
                final String xpath = String.format(
                    "//*[not(ancestor-or-self::*[@id='neo-ai-hud']) and (contains(normalize-space(text()), %s) or contains(@value, %s) or contains(@aria-label, %s))]",
                    escaped, escaped, escaped
                );
                final ElementsCollection els = Selenide.$$x(xpath);
                final SelenideElement visible = findFirstVisible(els, clean);
                if (visible != null)
                {
                    return visible;
                }
            }
            catch (final Exception ignored)
            {
            }
        }

        // Fallback for bare tag names classified as TEXT (e.g. "button", "select", "input")
        if (!forceXpath && type == SelectorSyntaxChecker.SelectorType.TEXT && clean.matches("^[a-zA-Z0-9_-]+$"))
        {
            try
            {
                final ElementsCollection els = Selenide.$$(By.tagName(clean));
                final SelenideElement visible = findFirstVisible(els, clean);
                if (visible != null)
                {
                    return visible;
                }
            }
            catch (final Exception ignored)
            {
            }
        }

        return null;
    }

    /**
     * Resolves Neodymium Automation ID selectors (data-ai or xc_ identifiers).
     */
    private static SelenideElement tryResolveAutomationId(final String clean)
    {
        final java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(xc[a-zA-Z0-9_\\-]+)").matcher(clean);
        if (!matcher.find())
        {
            return null;
        }
        final String neoId = matcher.group(1);
        try
        {
            final String transformedCss = clean.replaceAll("#" + java.util.regex.Pattern.quote(neoId), "[data-ai='" + neoId + "']");
            ElementsCollection els = Selenide.$$(By.cssSelector(transformedCss));
            SelenideElement visible = findFirstVisible(els, clean);
            if (visible != null)
            {
                return visible;
            }

            els = Selenide.$$(By.cssSelector("[data-ai='" + neoId + "']"));
            visible = findFirstVisible(els, clean);
            if (visible != null)
            {
                return visible;
            }

            // Dynamically stamp data-ai attributes into live DOM if absent (throttled to at most 1 stamp per 2 seconds per URL)
            final WebDriver driver = WebDriverRunner.getWebDriver();
            if (shouldAttemptDomStamp(driver))
            {
                try
                {
                    new PageAnalyzer(driver).captureSimplifiedDom(ContextLevel.LEAN);
                    els = Selenide.$$(By.cssSelector(transformedCss));
                    visible = findFirstVisible(els, clean);
                    if (visible != null)
                    {
                        return visible;
                    }
                    els = Selenide.$$(By.cssSelector("[data-ai='" + neoId + "']"));
                    visible = findFirstVisible(els, clean);
                    if (visible != null)
                    {
                        return visible;
                    }
                }
                catch (final Exception ignored)
                {
                }
            }

            final ElementsCollection retryEls = Selenide.$$(By.cssSelector("[data-ai='" + neoId + "']"));
            return findFirstVisible(retryEls, clean);
        }
        catch (final Exception ignored)
        {
            return null;
        }
    }

    /**
     * Resolves Playwright and jQuery pseudo-selectors into XPath queries.
     */
    private static SelenideElement tryResolvePlaywrightPseudo(final String clean)
    {
        final String lower = clean.toLowerCase();
        if (lower.startsWith("text=") || lower.startsWith("text*=") || lower.startsWith("text:") || lower.startsWith("text*:")
                || lower.startsWith("has-text=") || lower.startsWith("has-text*=") || lower.startsWith("has-text:") || lower.startsWith("has-text*:"))
        {
            final int delimIdx = clean.indexOf(clean.contains("=") ? '=' : ':');
            String text = clean.substring(delimIdx + 1).trim();
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
                    final SelenideElement visible = findFirstVisible(els, clean);
                    if (visible != null)
                    {
                        return visible;
                    }
                }
                catch (final Exception ignored)
                {
                }
            }
        }

        final java.util.regex.Matcher pwMatcher = java.util.regex.Pattern.compile("^(.*?):(has-text|has-text\\*|text|text\\*|contains)\\(['\"]?(.*?)['\"]?\\)$", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(clean);
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
                    final SelenideElement visible = findFirstVisible(els, clean);
                    if (visible != null)
                    {
                        return visible;
                    }
                }
                catch (final Exception ignored)
                {
                }
            }
        }
        return null;
    }

    /**
     * Helper method to filter an {@link ElementsCollection} and return the first element that is currently displayed in the live DOM.
     * Detects ambiguous multi-matches and logs a warning while smartly disambiguating active/focused elements.
     *
     * @param els       the collection of elements
     * @param candidate the candidate locator string for diagnostic logging
     * @return the resolved visible element, or null if none is displayed or collection is empty
     */
    private static SelenideElement findFirstVisible(final ElementsCollection els, final String candidate)
    {
        if (els == null || els.isEmpty())
        {
            return null;
        }

        final List<SelenideElement> visibleEls = new ArrayList<>();
        for (final SelenideElement el : els)
        {
            try
            {
                if (el.isDisplayed())
                {
                    visibleEls.add(el);
                }
            }
            catch (final Exception ignored)
            {
            }
        }

        if (visibleEls.isEmpty())
        {
            return null;
        }

        if (visibleEls.size() == 1)
        {
            return visibleEls.get(0);
        }

        // Ambiguity detected: multiple visible elements match the candidate locator
        LOG.warn("⚠️ [AMBIGUITY WARNING] Target locator '{}' matched {} visible elements in the DOM (out of {} total matching elements). Applying smart selection.",
            candidate, visibleEls.size(), els.size());

        // Disambiguation Priority 1: Currently focused element
        for (final SelenideElement el : visibleEls)
        {
            try
            {
                if (el.is(Condition.focused))
                {
                    return el;
                }
            }
            catch (final Exception ignored)
            {
            }
        }

        // Disambiguation Priority 2: Enabled interactive element over disabled
        final List<SelenideElement> enabledEls = new ArrayList<>();
        for (final SelenideElement el : visibleEls)
        {
            try
            {
                if (el.isEnabled())
                {
                    enabledEls.add(el);
                }
            }
            catch (final Exception ignored)
            {
            }
        }
        if (enabledEls.size() == 1)
        {
            return enabledEls.get(0);
        }

        // Disambiguation Priority 3: First visible in document order
        return visibleEls.get(0);
    }

    /**
     * Splits a comma-separated target string into individual candidate locators, respecting quotes, brackets, and parentheses.
     */
    private static List<String> splitCandidates(final String target)
    {
        final List<String> result = new ArrayList<>();
        if (target == null)
        {
            return result;
        }

        final StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int bracketDepth = 0;
        int parenDepth = 0;

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
            else if (c == '(' && !inSingleQuote && !inDoubleQuote)
            {
                parenDepth++;
            }
            else if (c == ')' && !inSingleQuote && !inDoubleQuote)
            {
                parenDepth--;
            }

            if (c == ',' && !inSingleQuote && !inDoubleQuote && bracketDepth <= 0 && parenDepth <= 0)
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

    /**
     * Resolves a target locator into a Selenium {@link By} instance using {@link LocatorResolver}.
     *
     * @param target the target string
     * @return resolved By locator
     */
    public static By resolveLocator(final String target)
    {
        return LocatorResolver.resolveLocator(target);
    }
}
