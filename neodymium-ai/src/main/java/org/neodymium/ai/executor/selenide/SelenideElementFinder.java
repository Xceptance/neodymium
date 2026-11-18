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
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.action.LocatorCandidate;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.executor.selenide.frame.FrameContextManager;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.model.DomFeatureVector;
import org.neodymium.ai.util.SelectorSyntaxChecker;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;
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
    private static final long DOM_STAMP_THROTTLE_MS = 1200L;
    private static volatile long lastGlobalDomStampTime = 0L;
    private static volatile String lastStampedUrl = "";

    public static void resetDomStampCacheForTesting()
    {
        lastGlobalDomStampTime = 0L;
        lastStampedUrl = "";
    }

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

        // A compound target ("#a >>> #btn") is merged with an explicit frame so neither information source is lost
        final FrameContextManager.FrameTarget compound = FrameContextManager.splitFrameTarget(action.getTarget());
        if (compound.frame() != null)
        {
            return findElement(action
                .withFrame(FrameContextManager.combine(action.getFrame(), compound.frame()))
                .withTarget(compound.target()));
        }

        if (action.getFrame() != null && !action.getFrame().isBlank()
            && !FrameContextManager.isDefaultContent(action.getFrame())
            && WebDriverRunner.hasWebDriverStarted())
        {
            // The enclosing FrameScope (executor / tool wrapper) restores the previous context
            FrameContextManager.ensureEntered(WebDriverRunner.getWebDriver(), action.getFrame());
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
                catch (final InvalidSelectorException e)
                {
                    throw e;
                }
                catch (final AssertionError | Exception ignored)
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
                    final FrameContextManager.FrameTarget candSplit = FrameContextManager.splitFrameTarget(candidate.getLocator());
                    fallbacks.add(candSplit.target());
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

        // A compound target ("#a >>> #item") is merged with an explicit frame
        final FrameContextManager.FrameTarget compound = FrameContextManager.splitFrameTarget(action.getTarget());
        if (compound.frame() != null)
        {
            return findElements(action
                .withFrame(FrameContextManager.combine(action.getFrame(), compound.frame()))
                .withTarget(compound.target()));
        }

        if (action.getFrame() != null && !action.getFrame().isBlank()
            && !FrameContextManager.isDefaultContent(action.getFrame())
            && WebDriverRunner.hasWebDriverStarted())
        {
            FrameContextManager.ensureEntered(WebDriverRunner.getWebDriver(), action.getFrame());
        }

        if (isAutomationIdSelector(action.getTarget()))
        {
            ensureAutomationIdsStampedIfNeeded(action.getTarget());
        }
        final ElementsCollection collection = Selenide.$$(resolveLocator(action.getTarget()));
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

        final FrameContextManager.FrameTarget compoundTarget = FrameContextManager.splitFrameTarget(target);
        if (compoundTarget.frame() != null)
        {
            if (WebDriverRunner.hasWebDriverStarted())
            {
                FrameContextManager.ensureEntered(WebDriverRunner.getWebDriver(), compoundTarget.frame());
            }
            final List<String> cleanFallbacks = new ArrayList<>();
            if (fallbackCandidates != null)
            {
                for (final String fb : fallbackCandidates)
                {
                    if (fb != null)
                    {
                        cleanFallbacks.add(FrameContextManager.splitFrameTarget(fb).target());
                    }
                }
            }
            return findElement(compoundTarget.target(), cleanFallbacks, recordedVector);
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
                catch (final AssertionError | Exception ignored)
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
                // Opt-in last resort, only after the full timeout: search child iframes (see AiConfiguration)
                final SelenideElement inChildFrame = findInChildFrames(allCandidates);
                if (inChildFrame != null)
                {
                    return inChildFrame;
                }
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

    /**
     * Checks whether the given target locator is directly present and visible on the active page
     * without blocking or waiting for timeouts.
     *
     * @param target the target locator (CSS, XPath, Playwright pseudo, etc.)
     * @return true if the element is currently attached and visible in the DOM
     */
    public static boolean isDirectlyPresent(final String target)
    {
        if (target == null || target.isBlank() || !WebDriverRunner.hasWebDriverStarted())
        {
            return false;
        }
        try
        {
            return findDirect(target) != null;
        }
        catch (final AssertionError | Exception ignored)
        {
            return false;
        }
    }

    private static SelenideElement findDirect(final String rawCandidate)
    {
        if (rawCandidate == null || rawCandidate.isBlank())
        {
            return null;
        }

        final String clean = rawCandidate.trim();
        final String lowerClean = clean.toLowerCase(Locale.ROOT);
        if (lowerClean.startsWith("marker:") || lowerClean.startsWith("badge:"))
        {
            final int colonIdx = clean.indexOf(':');
            final String markerNum = clean.substring(colonIdx + 1).trim();
            if (!markerNum.isBlank())
            {
                try
                {
                    final ElementsCollection markedEls = Selenide.$$(By.cssSelector("[data-m='" + markerNum + "']"));
                    final SelenideElement visibleMarked = findFirstVisible(markedEls, clean);
                    if (visibleMarked != null)
                    {
                        return visibleMarked;
                    }
                    if (!markedEls.isEmpty())
                    {
                        return markedEls.first();
                    }
                }
                catch (final AssertionError | Exception ignored)
                {
                }
            }
        }

        if (isAutomationIdSelector(clean))
        {
            final SelenideElement el = tryResolveAutomationId(clean);
            if (el != null)
            {
                return el;
            }
        }

        try
        {
            final ElementsCollection els = Selenide.$$(LocatorResolver.resolveLocator(clean));
            final SelenideElement visible = findFirstVisible(els, clean);
            if (visible != null)
            {
                return visible;
            }
            for (final SelenideElement el : els)
            {
                try
                {
                    if ("select".equalsIgnoreCase(el.getTagName()))
                    {
                        return el;
                    }
                }
                catch (final AssertionError | Exception ignored)
                {
                }
            }
        }
        catch (final InvalidSelectorException e)
        {
            throw e;
        }
        catch (final AssertionError | Exception ignored)
        {
        }

        if (!SelectorSyntaxChecker.isXpathExpression(clean))
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
            catch (final AssertionError | Exception ignored)
            {
            }
        }

        if (!SelectorSyntaxChecker.isCssSelector(clean) && !SelectorSyntaxChecker.isXpathExpression(clean) && !clean.contains("<") && !clean.contains(">"))
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
            catch (final AssertionError | Exception ignored)
            {
            }
        }

        if (clean.matches("^[a-zA-Z0-9_-]+$"))
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
            catch (final AssertionError | Exception ignored)
            {
            }
        }

        return null;
    }

    private static SelenideElement tryResolveAutomationId(final String clean)
    {
        final Matcher matcher = Pattern.compile("(xc[a-zA-Z0-9_\\-]+)").matcher(clean);
        if (!matcher.find())
        {
            return null;
        }
        final String neoId = matcher.group(1);
        try
        {
            final String transformedCss = clean.replaceAll("#(xc[a-zA-Z0-9_\\-]+)", "[data-ai='$1']");
            ElementsCollection els = Selenide.$$(By.cssSelector(transformedCss));
            SelenideElement visible = findFirstVisible(els, clean);
            if (visible != null)
            {
                return visible;
            }

            ensureAutomationIdsStampedIfNeeded(clean);
            els = Selenide.$$(By.cssSelector(transformedCss));
            visible = findFirstVisible(els, clean);
            if (visible != null)
            {
                return visible;
            }

            final boolean isSimple = !clean.contains(" ") && !clean.contains(">") && !clean.contains("+") && !clean.contains("~") && !clean.contains(",");
            if (isSimple)
            {
                els = Selenide.$$(By.cssSelector("[data-ai='" + neoId + "']"));
                visible = findFirstVisible(els, clean);
                if (visible != null)
                {
                    return visible;
                }
            }

            if (!els.isEmpty())
            {
                return els.first();
            }
        }
        catch (final AssertionError | Exception ignored)
        {
        }
        return null;
    }

    /**
     * Determines whether the given selector targets a Neodymium automation ID (data-ai or xc... signature).
     *
     * @param target the target selector string
     * @return true if target contains or references an automation ID
     */
    public static boolean isAutomationIdSelector(final String target)
    {
        if (target == null || target.isBlank())
        {
            return false;
        }
        final String clean = target.trim();
        return clean.contains("data-ai=")
                || clean.startsWith("[data-ai=")
                || clean.matches(".*#xc[a-zA-Z0-9_\\-]+.*")
                || clean.matches("^xc[a-zA-Z0-9_\\-]+$");
    }

    /**
     * Dynamically triggers DOM attribute stamping via {@link PageAnalyzer} if the target selector
     * references an automation ID and the active page has not been stamped yet or the URL changed.
     *
     * @param target the target locator to inspect
     */
    public static void ensureAutomationIdsStampedIfNeeded(final String target)
    {
        if (target == null || target.isBlank() || !WebDriverRunner.hasWebDriverStarted())
        {
            return;
        }

        final String clean = target.trim();
        if (isAutomationIdSelector(clean))
        {
            final WebDriver driver = WebDriverRunner.getWebDriver();
            if (driver != null)
            {
                try
                {
                    final long now = System.currentTimeMillis();
                    final String currentUrl = driver.getCurrentUrl();
                    final boolean urlChanged = currentUrl != null && !currentUrl.equals(lastStampedUrl);
                    final boolean neverStamped = !Selenide.$("[data-ai]").exists();
                    if (urlChanged || neverStamped || (now - lastGlobalDomStampTime >= DOM_STAMP_THROTTLE_MS))
                    {
                        lastGlobalDomStampTime = now;
                        lastStampedUrl = currentUrl != null ? currentUrl : "";
                        new PageAnalyzer(driver).captureSimplifiedDom(ContextLevel.STANDARD);
                    }
                }
                catch (final AssertionError | Exception ignored)
                {
                }
            }
        }
    }

    public static SelenideElement findFirstVisible(final ElementsCollection els, final String originalTarget)
    {
        if (els == null)
        {
            return null;
        }

        try
        {
            final ElementsCollection visibleEls = els.filter(Condition.visible);
            if (visibleEls.isEmpty())
            {
                return null;
            }

            final String cleanTarget = originalTarget != null ? originalTarget.trim() : "";
            final boolean targetIsRoot = "body".equalsIgnoreCase(cleanTarget) || "html".equalsIgnoreCase(cleanTarget);
            final ElementsCollection candidates = targetIsRoot
                    ? visibleEls
                    : visibleEls.filter(Condition.not(Condition.match("root container", el -> {
                        final String tag = el.getTagName();
                        return "html".equalsIgnoreCase(tag) || "body".equalsIgnoreCase(tag) || "head".equalsIgnoreCase(tag);
                    })));

            if (candidates.isEmpty())
            {
                return null;
            }

            if (candidates.size() == 1)
            {
                final SelenideElement single = candidates.first();
                return single.is(Condition.visible) ? single : null;
            }

            if (!cleanTarget.isBlank())
            {
                final SelenideElement matched = candidates.find(Condition.or("target match",
                    Condition.exactText(cleanTarget),
                    Condition.value(cleanTarget),
                    Condition.attribute("aria-label", cleanTarget)));
                if (matched.is(Condition.visible))
                {
                    return matched;
                }
            }

            final SelenideElement focused = candidates.find(Condition.focused);
            if (focused.is(Condition.visible))
            {
                return focused;
            }

            final SelenideElement first = candidates.first();
            return first.is(Condition.visible) ? first : null;
        }
        catch (final AssertionError | Exception ignored)
        {
            return null;
        }
    }

    private static SelenideElement findInShadowRoots(final String rawCandidate)
    {
        if (rawCandidate == null || rawCandidate.isBlank())
        {
            return null;
        }

        final By by = LocatorResolver.resolveLocator(rawCandidate);
        if (!WebDriverRunner.hasWebDriverStarted())
        {
            return null;
        }

        try
        {
            final ElementsCollection els = Selenide.$$(by);
            return findFirstVisible(els, rawCandidate);
        }
        catch (final InvalidSelectorException e)
        {
            throw e;
        }
        catch (final AssertionError | Exception e)
        {
            LOG.trace("Shadow DOM element lookup failed for '{}': {}", rawCandidate, e.getMessage());
            return null;
        }
    }

    public static ElementsCollection findAllInShadowRoots(final String rawCandidate)
    {
        if (rawCandidate == null || rawCandidate.isBlank())
        {
            return Selenide.$$(List.of());
        }

        final By by = LocatorResolver.resolveLocator(rawCandidate);
        if (!WebDriverRunner.hasWebDriverStarted())
        {
            return Selenide.$$(List.of());
        }

        try
        {
            return Selenide.$$(by);
        }
        catch (final InvalidSelectorException e)
        {
            throw e;
        }
        catch (final AssertionError | Exception e)
        {
            LOG.trace("Shadow DOM elements lookup failed for '{}': {}", rawCandidate, e.getMessage());
            return Selenide.$$(List.of());
        }
    }

    public static By resolveLocator(final String target)
    {
        return LocatorResolver.resolveLocator(target);
    }

    /**
     * Scrolls the given element into view using centered block alignment if any part of the element
     * lies outside the current browser viewport boundaries.
     * <p>
     * Uses centered alignment ({@code block: 'center', inline: 'nearest'}) to prevent sticky headers
     * or footers from occluding the target element.
     * </p>
     *
     * @param element the Selenide element to inspect and scroll if needed
     */
    public static void scrollIntoViewIfNeeded(final SelenideElement element)
    {
        if (element == null)
        {
            return;
        }
        try
        {
            Selenide.executeJavaScript(
                "(function(el) {" +
                "  if (!el || typeof el.getBoundingClientRect !== 'function') return;" +
                "  var target = el;" +
                "  var rect = target.getBoundingClientRect();" +
                "  if (rect.width === 0 && rect.height === 0 && target.parentElement) {" +
                "    target = target.parentElement;" +
                "    rect = target.getBoundingClientRect();" +
                "  }" +
                "  var vh = window.innerHeight || document.documentElement.clientHeight;" +
                "  var vw = window.innerWidth || document.documentElement.clientWidth;" +
                "  var outOfView = rect.top < 0 || rect.bottom > vh || rect.left < 0 || rect.right > vw;" +
                "  if (outOfView) {" +
                "    target.scrollIntoView({ behavior: 'instant', block: 'center', inline: 'nearest' });" +
                "  }" +
                "})(arguments[0]);",
                element);
        }
        catch (final Throwable ignored)
        {
        }
    }

    /**
     * Opt-in last-resort lookup: recursively searches child iframes of the current browsing context for any of the
     * given candidates. Only active if {@code neodymium.ai.frames.autoDiscovery.enabled=true}.
     * On a hit the driver is left inside the matching frame and the tracked frame path is updated (the enclosing
     * {@link FrameContextManager.FrameScope} restores the previous context); on a miss the original context is restored.
     *
     * @param candidates the candidate selectors, in priority order
     * @return the resolved {@link SelenideElement} in a child frame, or {@code null} if disabled or not found
     */
    private static SelenideElement findInChildFrames(final List<String> candidates)
    {
        final AiConfiguration config = AiConfiguration.getInstance();
        if (!config.isFrameAutoDiscoveryEnabled() || !WebDriverRunner.hasWebDriverStarted())
        {
            return null;
        }
        final WebDriver driver = WebDriverRunner.getWebDriver();
        final String originalFrame = FrameContextManager.getActiveFramePath();
        try
        {
            final SelenideElement found = searchChildFramesRecursively(driver, candidates, originalFrame, 1,
                config.getFrameAutoDiscoveryMaxDepth());
            if (found != null)
            {
                LOG.warn("   🖼️ Resolved {} via iframe auto-discovery in frame '{}'. Set an explicit 'frame' to make this deterministic.",
                    candidates, FrameContextManager.getActiveFramePath());
                return found;
            }
        }
        catch (final Exception e)
        {
            LOG.debug("Iframe auto-discovery aborted: {}", e.getMessage());
        }
        FrameContextManager.restoreContext(driver, originalFrame);
        return null;
    }

    /**
     * Depth-first search through the child frames of the current context. Every frame is entered once and all
     * candidates are tried inside it before descending further.
     *
     * @param driver      the WebDriver positioned in the frame whose children are searched
     * @param candidates  the candidate selectors
     * @param currentPath the tracked path of the current context (null for default content)
     * @param depth       the current nesting depth (1 = direct children)
     * @param maxDepth    the maximum nesting depth to search
     * @return the element, with the driver left inside its frame, or {@code null}
     */
    private static SelenideElement searchChildFramesRecursively(
        final WebDriver driver,
        final List<String> candidates,
        final String currentPath,
        final int depth,
        final int maxDepth)
    {
        if (depth > maxDepth)
        {
            return null;
        }

        final List<WebElement> iframes = driver.findElements(By.cssSelector("iframe, frame"));
        for (final WebElement frameEl : iframes)
        {
            boolean switched = false;
            try
            {
                final String frameSelector = FrameContextManager.describeFrame(driver, frameEl);
                final String newPath = FrameContextManager.combine(currentPath, frameSelector);

                driver.switchTo().frame(frameEl);
                switched = true;

                for (final String candidate : candidates)
                {
                    final SelenideElement direct = findDirect(candidate);
                    if (direct != null)
                    {
                        FrameContextManager.setActiveFramePath(newPath);
                        return direct;
                    }
                }

                final SelenideElement deeper = searchChildFramesRecursively(driver, candidates, newPath, depth + 1, maxDepth);
                if (deeper != null)
                {
                    return deeper;
                }
            }
            catch (final Exception e)
            {
                LOG.debug("Skipping iframe during auto-discovery: {}", e.getMessage());
            }
            if (switched)
            {
                try
                {
                    driver.switchTo().parentFrame();
                }
                catch (final Exception e)
                {
                    // Cannot navigate back (e.g. frame detached): abort, the caller restores the original context
                    LOG.debug("Could not return to parent frame: {}", e.getMessage());
                    return null;
                }
            }
        }
        return null;
    }
}
