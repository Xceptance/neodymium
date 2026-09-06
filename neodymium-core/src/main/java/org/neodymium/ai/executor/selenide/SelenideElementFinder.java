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
import java.util.Set;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.action.LocatorCandidate;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.model.DomFeatureVector;
import org.neodymium.ai.util.SelectorSyntaxChecker;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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
 *   <li><b>Shadow DOM Fallback:</b> Automatic deep shadow-root traversal and compound selector piercing across open shadow boundaries when light DOM yields no match.</li>
 * </ol>
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class SelenideElementFinder
{
    private static final Logger LOG = LoggerFactory.getLogger(SelenideElementFinder.class);
    private static final long RETRY_INTERVAL_MS = 100L;

    private static final String SHADOW_DOM_HELPER_JS = """
                var allRoots = [document];
                function collectAllRoots(root) {
                    try {
                        var els = root.querySelectorAll('*');
                        for (var i = 0; i < els.length; i++) {
                            if (els[i].shadowRoot) {
                                allRoots.push(els[i].shadowRoot);
                                collectAllRoots(els[i].shadowRoot);
                            }
                        }
                    } catch(e) {}
                }
                collectAllRoots(document);

                function isVisible(el) {
                    if (!el || !el.isConnected) return false;
                    try {
                        var style = window.getComputedStyle(el);
                        if (style.display === 'none' || style.visibility === 'hidden') return false;
                        var rect = el.getBoundingClientRect();
                        if (rect.width > 0 && rect.height > 0) return true;
                        var tag = el.tagName ? el.tagName.toLowerCase() : '';
                        if (['input', 'button', 'a', 'select', 'textarea'].indexOf(tag) !== -1) return true;
                    } catch(e) {}
                    return false;
                }

                function collectDescendantRoots(node) {
                    var roots = [];
                    if (node.shadowRoot) {
                        roots.push(node.shadowRoot);
                        collectInner(node.shadowRoot);
                    }
                    collectInner(node);
                    function collectInner(parent) {
                        try {
                            var els = parent.querySelectorAll('*');
                            for (var i = 0; i < els.length; i++) {
                                if (els[i].shadowRoot) {
                                    roots.push(els[i].shadowRoot);
                                    collectInner(els[i].shadowRoot);
                                }
                            }
                        } catch(e) {}
                    }
                    return roots;
                }

                function splitSelector(sel) {
                    var parts = [];
                    var current = '';
                    var inQuotes = false;
                    var quoteChar = '';
                    var inBrackets = 0;
                    var inParens = 0;
                    for (var i = 0; i < sel.length; i++) {
                        var c = sel[i];
                        if (inQuotes) {
                            current += c;
                            if (c === quoteChar && sel[i - 1] !== '\\\\') {
                                inQuotes = false;
                            }
                        } else if (c === '"' || c === "'") {
                            inQuotes = true;
                            quoteChar = c;
                            current += c;
                        } else if (c === '[') {
                            inBrackets++;
                            current += c;
                        } else if (c === ']') {
                            if (inBrackets > 0) inBrackets--;
                            current += c;
                        } else if (c === '(') {
                            inParens++;
                            current += c;
                        } else if (c === ')') {
                            if (inParens > 0) inParens--;
                            current += c;
                        } else if (/\\s/.test(c)) {
                            if (inBrackets === 0 && inParens === 0) {
                                if (current.length > 0) {
                                    parts.push(current);
                                    current = '';
                                }
                            } else {
                                current += c;
                            }
                        } else {
                            current += c;
                        }
                    }
                    if (current.length > 0) {
                        parts.push(current);
                    }
                    return parts;
                }
            """;

    private static final String SHADOW_FIND_SCRIPT = """
            return (function(selector) {
            """
        + SHADOW_DOM_HELPER_JS
        + """
                if (!selector) return null;
                var clean = selector.trim();
                if (clean.toLowerCase().indexOf('css=') === 0) {
                    clean = clean.substring(4).trim();
                }

                // A. Automation ID resolution (data-ai / xc_...)
                var aiMatch = clean.match(/(xc[a-zA-Z0-9_\\-]+)/);
                if (aiMatch) {
                    var neoId = aiMatch[1];
                    for (var i = 0; i < allRoots.length; i++) {
                        try {
                            var el = allRoots[i].querySelector('[data-ai="' + neoId + '"]');
                            if (el && isVisible(el)) return el;
                        } catch (e) {}
                    }
                }

                // B. Handle Playwright text pseudo-selectors e.g. text="...", has-text="..."
                var textMatch = clean.match(/^(?:text|has-text)=(?:"(.*)"|'(.*)'|(.*))$/i);
                if (textMatch) {
                    var rawText = (textMatch[1] || textMatch[2] || textMatch[3] || '').trim().toLowerCase();
                    if (rawText) {
                        for (var i = 0; i < allRoots.length; i++) {
                            try {
                                var candidates = allRoots[i].querySelectorAll('a, button, input, [role="button"], span, div, p, label');
                                for (var j = 0; j < candidates.length; j++) {
                                    var c = candidates[j];
                                    var txt = (c.innerText || c.value || c.getAttribute('aria-label') || '').trim().toLowerCase();
                                    if (txt === rawText || txt.indexOf(rawText) !== -1) {
                                        if (isVisible(c)) return c;
                                    }
                                }
                            } catch(e) {}
                        }
                    }
                }

                // C. Direct query across all roots first
                for (var i = 0; i < allRoots.length; i++) {
                    try {
                        var els = allRoots[i].querySelectorAll(clean);
                        for (var j = 0; j < els.length; j++) {
                            if (isVisible(els[j])) return els[j];
                        }
                    } catch (e) {}
                }

                // D. Multi-segment shadow-piercing traversal (e.g. "c-community-input-email input")
                var parts = splitSelector(clean);
                if (parts.length > 1) {
                    var currentNodes = [document];
                    for (var p = 0; p < parts.length; p++) {
                        var part = parts[p];
                        if (part === '>') continue;
                        var nextNodes = [];
                        var seen = [];
                        for (var n = 0; n < currentNodes.length; n++) {
                            var node = currentNodes[n];
                            var subRoots = (node === document) ? allRoots : collectDescendantRoots(node);
                            for (var r = 0; r < subRoots.length; r++) {
                                try {
                                    var matches = subRoots[r].querySelectorAll(part);
                                    for (var m = 0; m < matches.length; m++) {
                                        var el = matches[m];
                                        if (seen.indexOf(el) === -1) {
                                            seen.push(el);
                                            nextNodes.push(el);
                                        }
                                    }
                                } catch (e) {}
                            }
                        }
                        currentNodes = nextNodes;
                        if (currentNodes.length === 0) break;
                    }

                    for (var i = 0; i < currentNodes.length; i++) {
                        if (isVisible(currentNodes[i])) return currentNodes[i];
                    }
                    if (currentNodes.length > 0) return currentNodes[0];
                }

                // E. Plain text fallback in shadow roots
                if (clean.indexOf('<') === -1 && clean.indexOf('>') === -1 && clean.length > 1 && clean.length < 100) {
                    var lowerSearch = clean.toLowerCase();
                    for (var i = 0; i < allRoots.length; i++) {
                        try {
                            var candidates = allRoots[i].querySelectorAll('a, button, input, [role="button"], span, div, p, label');
                            for (var j = 0; j < candidates.length; j++) {
                                var c = candidates[j];
                                var txt = (c.innerText || c.value || c.getAttribute('aria-label') || '').trim().toLowerCase();
                                if (txt === lowerSearch) {
                                    if (isVisible(c)) return c;
                                }
                            }
                        } catch(e) {}
                    }
                }

                return null;
            })(arguments[0]);
            """;

    private static final String SHADOW_FIND_ALL_SCRIPT = """
            return (function(selector) {
            """
        + SHADOW_DOM_HELPER_JS
        + """
                if (!selector) return [];
                var clean = selector.trim();
                if (clean.toLowerCase().indexOf('css=') === 0) {
                    clean = clean.substring(4).trim();
                }

                var parts = splitSelector(clean);
                if (parts.length > 1) {
                    var currentNodes = [document];
                    for (var p = 0; p < parts.length; p++) {
                        var part = parts[p];
                        if (part === '>') continue;
                        var nextNodes = [];
                        var seen = [];
                        for (var n = 0; n < currentNodes.length; n++) {
                            var node = currentNodes[n];
                            var subRoots = (node === document) ? allRoots : collectDescendantRoots(node);
                            for (var r = 0; r < subRoots.length; r++) {
                                try {
                                    var matches = subRoots[r].querySelectorAll(part);
                                    for (var m = 0; m < matches.length; m++) {
                                        var el = matches[m];
                                        if (seen.indexOf(el) === -1) {
                                            seen.push(el);
                                            nextNodes.push(el);
                                        }
                                    }
                                } catch (e) {}
                            }
                        }
                        currentNodes = nextNodes;
                        if (currentNodes.length === 0) break;
                    }
                    return currentNodes.filter(isVisible);
                }

                var results = [];
                var seen = [];
                for (var i = 0; i < allRoots.length; i++) {
                    try {
                        var els = allRoots[i].querySelectorAll(clean);
                        for (var j = 0; j < els.length; j++) {
                            var el = els[j];
                            if (seen.indexOf(el) === -1 && isVisible(el)) {
                                seen.push(el);
                                results.push(el);
                            }
                        }
                    } catch (e) {}
                }
                return results;
            })(arguments[0]);
            """;

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

        final String target = action.getTarget();
        final String val = action.getValue();
        final String type = action.getType() != null ? action.getType().toUpperCase() : "";

        // When a target locator and a text value are both provided for element targeting actions (e.g. CLICK, SELECT),
        // query elements matching target filtered by the expected text value first
        if (target != null && !target.isBlank() && val != null && !val.isBlank())
        {
            final boolean isTargetingAction = "CLICK".equals(type) || "SELECT".equals(type) || "HOVER".equals(type)
                || "DOUBLE_CLICK".equals(type) || "CONTEXT_CLICK".equals(type) || "CHECK".equals(type);
            if (isTargetingAction)
            {
                try
                {
                    final ElementsCollection matched = Selenide.$$(LocatorResolver.resolveLocator(target))
                        .filterBy(Condition.text(val));
                    final SelenideElement visible = findFirstVisible(matched, target);
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
        final DomFeatureVector vector = action.getDomFeatureVector();
        return findElement(action.getTarget(), fallbacks, vector);
    }

    /**
     * Resolves and returns an {@link ElementsCollection} matching the action's target locator.
     *
     * @param action the action containing target locator
     * @return the resolved ElementsCollection
     * @throws IllegalArgumentException if action or target is null or blank
     */
    public static ElementsCollection findElements(final Action action)
    {
        if (action == null || action.getTarget() == null || action.getTarget().isBlank())
        {
            throw new IllegalArgumentException("Action and target cannot be null or blank");
        }
        final ElementsCollection collection = Selenide.$$(LocatorResolver.resolveLocator(action.getTarget()));
        if (collection.isEmpty())
        {
            final ElementsCollection shadowCollection = findAllInShadowRoots(action.getTarget());
            if (!shadowCollection.isEmpty())
            {
                return shadowCollection;
            }
        }
        return collection;
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
        return findElement(target, fallbackCandidates, null);
    }

    /**
     * Resolves and returns a {@link SelenideElement} based on target locator, fallback candidates,
     * and optional DOM feature vector proximity scoring.
     *
     * @param target             the primary target locator or text content
     * @param fallbackCandidates optional fallback candidate locators attempted in order
     * @param recordedVector     optional recorded feature vector for proximity fallback
     * @return the resolved {@link SelenideElement}
     * @throws IllegalArgumentException if {@code target} is null or blank
     */
    public static SelenideElement findElement(
        final String target,
        final List<String> fallbackCandidates,
        final DomFeatureVector recordedVector)
    {
        if (target == null || target.isBlank())
        {
            throw new IllegalArgumentException("Target cannot be empty");
        }

        final List<String> allCandidates = new ArrayList<>();
        final Set<String> candidateSet = new LinkedHashSet<>();
        candidateSet.add(target);
        if (fallbackCandidates != null)
        {
            candidateSet.addAll(fallbackCandidates);
        }

        allCandidates.addAll(candidateSet);
        final long start = System.currentTimeMillis();
        final long timeoutMs = Configuration.timeout;

        while (true)
        {
            for (final String candidate : allCandidates)
            {
                final SelenideElement found = findDirect(candidate);
                if (found != null)
                {
                    return found;
                }
            }

            // Feature proximity matching fallback when recorded vector is available
            if (recordedVector != null)
            {
                try
                {
                    LOG.trace("   🧬 Attempting DomFeatureVector proximity match for target '{}': {}", target, recordedVector.toSummaryString());
                    final WebElement matchedWebElement = new PageAnalyzer().findLiveElementByFeatureVector(
                        WebDriverRunner.getWebDriver(),
                        recordedVector,
                        0.65);
                    if (matchedWebElement != null)
                    {
                        LOG.trace("   ✅ Proximity match found live element for target '{}'", target);
                        return Selenide.$(matchedWebElement);
                    }
                }
                catch (final Exception ignored)
                {
                }
            }

            // Shadow DOM fallback: When light DOM lookups fail for all candidates, search across all open shadow roots
            for (final String candidate : allCandidates)
            {
                final SelenideElement foundInShadow = findInShadowRoots(candidate);
                if (foundInShadow != null)
                {
                    return foundInShadow;
                }
            }

            if (System.currentTimeMillis() - start >= timeoutMs)
            {
                break;
            }

            try
            {
                Thread.sleep(RETRY_INTERVAL_MS);
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

    private static SelenideElement findDirect(final String rawCandidate)
    {
        if (rawCandidate == null || rawCandidate.isBlank())
        {
            return null;
        }

        String clean = rawCandidate.trim();
        boolean forceXpath = false;
        boolean forceCss = false;
        boolean forceText = false;

        final String lower = clean.toLowerCase();
        if (lower.startsWith("xpath="))
        {
            clean = clean.substring(6).trim();
            forceXpath = true;
        }
        else if (lower.startsWith("css="))
        {
            clean = clean.substring(4).trim();
            forceCss = true;
        }
        else if (lower.startsWith("text=") || lower.startsWith("has-text="))
        {
            forceText = true;
        }

        if (!forceXpath && !forceText && (clean.contains("data-ai=") || clean.startsWith("[data-ai=") || clean.matches(".*#xc[a-zA-Z0-9_\\-]+.*")))
        {
            final SelenideElement el = tryResolveAutomationId(clean);
            if (el != null)
            {
                return el;
            }
        }

        if (!forceXpath && !forceCss && (forceText || clean.contains(":") || clean.contains("=")))
        {
            final SelenideElement el = tryResolvePlaywrightPseudo(clean);
            if (el != null)
            {
                return el;
            }
        }

        if (!forceCss && !forceText && (forceXpath || clean.startsWith("/") || clean.startsWith("./") || clean.startsWith("(")))
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

        if (!forceXpath && !forceText)
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

        if (!forceCss && !forceXpath)
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

        if (!forceCss && !forceXpath && !SelectorSyntaxChecker.isCssSelector(clean) && !SelectorSyntaxChecker.isXpathExpression(clean) && !clean.contains("<") && !clean.contains(">"))
        {
            try
            {
                final String escaped = LocatorResolver.escapeXpath(clean);
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

        if (!forceXpath && clean.matches("^[a-zA-Z0-9_-]+$"))
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

            final WebDriver driver = WebDriverRunner.getWebDriver();
            if (driver != null)
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
        }
        catch (final Exception ignored)
        {
        }
        return null;
    }

    private static SelenideElement tryResolvePlaywrightPseudo(final String clean)
    {
        final String lower = clean.toLowerCase();
        if (lower.startsWith("text=") || lower.startsWith("text*=") || lower.startsWith("has-text=") || lower.startsWith("has-text*="))
        {
            final int eqIdx = clean.indexOf('=');
            String text = clean.substring(eqIdx + 1).trim();
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
                    final String escaped = LocatorResolver.escapeXpath(text);
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
                    final String xpath = String.format("//%s[contains(normalize-space(.), %s)]", tag, LocatorResolver.escapeXpath(text));
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

    public static SelenideElement findFirstVisible(final ElementsCollection els, final String originalTarget)
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

        final String cleanTarget = originalTarget != null ? originalTarget.trim().toLowerCase() : "";
        for (final SelenideElement el : visibleEls)
        {
            try
            {
                final String text = el.getText();
                if (text != null && text.trim().equalsIgnoreCase(cleanTarget))
                {
                    return el;
                }
                final String val = el.getValue();
                if (val != null && val.trim().equalsIgnoreCase(cleanTarget))
                {
                    return el;
                }
                final String aria = el.getAttribute("aria-label");
                if (aria != null && aria.trim().equalsIgnoreCase(cleanTarget))
                {
                    return el;
                }
            }
            catch (final Exception ignored)
            {
            }
        }

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

        return visibleEls.get(0);
    }

    private static SelenideElement findInShadowRoots(final String rawCandidate)
    {
        final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
        if (driver == null)
        {
            return null;
        }

        try
        {
            final Object result = ((JavascriptExecutor) driver).executeScript(SHADOW_FIND_SCRIPT, rawCandidate);
            if (result instanceof WebElement webElement)
            {
                return Selenide.$(webElement);
            }
        }
        catch (final Exception e)
        {
            LOG.trace("Shadow DOM element lookup failed for '{}': {}", rawCandidate, e.getMessage());
        }
        return null;
    }

    public static ElementsCollection findAllInShadowRoots(final String rawCandidate)
    {
        final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
        if (driver == null)
        {
            return Selenide.$$(List.of());
        }

        try
        {
            final Object result = ((JavascriptExecutor) driver).executeScript(SHADOW_FIND_ALL_SCRIPT, rawCandidate);
            if (result instanceof List<?> list)
            {
                final List<WebElement> elements = new ArrayList<>();
                for (final Object item : list)
                {
                    if (item instanceof WebElement webElement)
                    {
                        elements.add(webElement);
                    }
                }
                return Selenide.$$(elements);
            }
        }
        catch (final Exception e)
        {
            LOG.trace("Shadow DOM elements lookup failed for '{}': {}", rawCandidate, e.getMessage());
        }
        return Selenide.$$(List.of());
    }

    public static By resolveLocator(final String target)
    {
        return LocatorResolver.resolveLocator(target);
    }
}
