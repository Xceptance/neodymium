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
package org.neodymium.ai.executor.selenide.frame;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.neodymium.ai.executor.selenide.LocatorResolver;
import org.neodymium.ai.executor.selenide.VolatileIdDetector;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeborne.selenide.Configuration;

/**
 * Manages hierarchical browsing context transitions across nested and stacked iframes.
 * Provides an {@link AutoCloseable} scope that navigates from default content down through
 * parent frames to the target frame and, on closure, puts the driver <b>and</b> the tracked frame path back
 * into the state they had when the scope was opened (so scopes can be nested safely).
 *
 * <p>The tracked frame path is thread-local and only mirrors what this class (and the element finder's
 * auto-discovery) did to the driver. Frame switches performed by foreign code (e.g. hand-written test code)
 * are not visible to it.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author AI-generated: Claude Sonnet 5.5
 * @author Xceptance GmbH 2026
 */
public final class FrameContextManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FrameContextManager.class);

    /**
     * Separator between the frame selectors of a hierarchical frame path (and between the frame path and the
     * element selector in compound targets such as {@code "#outer >>> #inner >>> #button"}).
     * Note: this literal sequence (with surrounding spaces) is reserved by Neodymium AI and is therefore
     * interpreted as frame traversal, not as the legacy CSS shadow-piercing combinator.
     */
    public static final String FRAME_DELIMITER = " >>> ";

    /** Poll interval while waiting for a frame element to appear. */
    private static final long FRAME_POLL_INTERVAL_MS = 100L;

    /** Matches a plain CSS identifier that can safely be used in {@code #id} form. */
    private static final String SAFE_CSS_IDENT = "[A-Za-z_][A-Za-z0-9_-]*";

    /**
     * Computes a structural CSS path from {@code html} to the given element using sibling-correct
     * {@code :nth-of-type} indices. Used when no unique id/name/title selector exists.
     */
    private static final String STRUCTURAL_PATH_SCRIPT = """
        var el = arguments[0];
        var parts = [];
        while (el && el.nodeType === 1 && el.tagName !== 'HTML') {
          var idx = 1;
          for (var s = el.previousElementSibling; s; s = s.previousElementSibling) {
            if (s.tagName === el.tagName) { idx++; }
          }
          parts.unshift(el.tagName.toLowerCase() + ':nth-of-type(' + idx + ')');
          el = el.parentElement;
        }
        return 'html > ' + parts.join(' > ');
        """;

    /** Active (normalized) frame path of the current thread; {@code null} means default content. */
    private static final ThreadLocal<String> ACTIVE_FRAME_PATH = new ThreadLocal<>();

    /** Cached detector for dynamic/volatile frame id attributes. */
    private static final VolatileIdDetector VOLATILE_ID_DETECTOR = new VolatileIdDetector();

    private FrameContextManager()
    {
        // Utility class
    }

    /**
     * Result of splitting a compound target into its frame path and the inner element selector.
     *
     * @param frame  the frame path, or {@code null} if the target had no frame prefix
     * @param target the element selector without frame prefix
     */
    public record FrameTarget(String frame, String target)
    {
    }

    /**
     * Gets the active frame path recorded for the current thread, or null if at default content.
     *
     * @return the active frame path, or null
     */
    public static String getActiveFramePath()
    {
        return ACTIVE_FRAME_PATH.get();
    }

    /**
     * Sets the active frame path for the current thread.
     *
     * @param path the active frame path
     */
    public static void setActiveFramePath(final String path)
    {
        if (path == null || path.isBlank() || isDefaultContent(path))
        {
            ACTIVE_FRAME_PATH.remove();
        }
        else
        {
            ACTIVE_FRAME_PATH.set(normalizeFramePath(path));
        }
    }

    /**
     * Clears the active frame path for the current thread. Must be called when a test/session ends so that a
     * pooled thread never leaks a stale frame into the next test.
     */
    public static void resetActiveFramePath()
    {
        ACTIVE_FRAME_PATH.remove();
    }

    /**
     * Checks if the given path represents root/default content.
     *
     * @param framePath the frame path string
     * @return true if representing default content or empty
     */
    public static boolean isDefaultContent(final String framePath)
    {
        if (framePath == null || framePath.isBlank())
        {
            return true;
        }
        final String clean = framePath.trim();
        return "main".equalsIgnoreCase(clean)
            || "default".equalsIgnoreCase(clean)
            || "defaultcontent".equalsIgnoreCase(clean)
            || clean.matches("^win_\\d+:main$");
    }

    /**
     * Normalizes a frame path by stripping window prefixes (e.g. {@code "win_0:main >>> #pact1" -> "#pact1"}).
     *
     * @param rawPath the raw frame path string
     * @return the normalized frame path
     */
    public static String normalizeFramePath(final String rawPath)
    {
        if (rawPath == null || rawPath.isBlank())
        {
            return "";
        }
        String clean = rawPath.trim();
        // Strip window prefix like "win_0:" or "win_1:"
        if (clean.matches("^win_\\d+:.*"))
        {
            final int colonIdx = clean.indexOf(':');
            clean = clean.substring(colonIdx + 1).trim();
        }
        // Strip leading "main >>> " or "main>>>" if present
        if (clean.matches("(?i)^main\\s*>>>\\s*.*"))
        {
            clean = clean.replaceFirst("(?i)^main\\s*>>>\\s*", "").trim();
        }
        else if ("main".equalsIgnoreCase(clean))
        {
            return "";
        }
        return clean;
    }

    /**
     * Splits a normalized frame path into sequential individual frame locators.
     *
     * @param framePath the frame path string
     * @return list of individual frame selectors
     */
    public static List<String> parseFrameHierarchy(final String framePath)
    {
        final String normalized = normalizeFramePath(framePath);
        if (normalized.isEmpty())
        {
            return List.of();
        }
        final String[] segments = normalized.split("\\s*>>>\\s*");
        final List<String> result = new ArrayList<>(segments.length);
        for (final String seg : segments)
        {
            final String trimmed = seg.trim();
            if (!trimmed.isEmpty() && !"main".equalsIgnoreCase(trimmed))
            {
                result.add(trimmed);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Splits a compound target such as {@code "#a >>> #b >>> #btn"} at its last {@link #FRAME_DELIMITER} into
     * the frame path ({@code "#a >>> #b"}) and the element selector ({@code "#btn"}).
     *
     * @param rawTarget the (possibly compound) target
     * @return the split result; {@link FrameTarget#frame()} is {@code null} if the target has no delimiter
     */
    public static FrameTarget splitFrameTarget(final String rawTarget)
    {
        if (rawTarget == null)
        {
            return new FrameTarget(null, null);
        }
        if (rawTarget.contains("://") || rawTarget.startsWith("coord:"))
        {
            return new FrameTarget(null, rawTarget);
        }
        final int last = findLastUnquotedDelimiter(rawTarget, ">>>");
        if (last < 0)
        {
            return new FrameTarget(null, rawTarget);
        }
        final String framePart = rawTarget.substring(0, last).trim();
        final String targetPart = rawTarget.substring(last + 3).trim();
        if (framePart.isEmpty())
        {
            return new FrameTarget(null, targetPart);
        }
        return new FrameTarget(framePart, targetPart);
    }

    /**
     * Finds the index of the last occurrence of {@code delimiter} in {@code s} that does not appear inside
     * single- or double-quoted string literals.
     *
     * @param s         the string to inspect
     * @param delimiter the literal delimiter to look for
     * @return the last unquoted index, or -1 if not found
     */
    private static int findLastUnquotedDelimiter(final String s, final String delimiter)
    {
        if (s == null || !s.contains(delimiter))
        {
            return -1;
        }
        final int delimLen = delimiter.length();
        boolean inSingle = false;
        boolean inDouble = false;
        int last = -1;
        for (int i = 0; i <= s.length() - delimLen; i++)
        {
            final char c = s.charAt(i);
            if (c == '\'' && !inDouble)
            {
                inSingle = !inSingle;
            }
            else if (c == '"' && !inSingle)
            {
                inDouble = !inDouble;
            }
            else if (!inSingle && !inDouble && s.startsWith(delimiter, i))
            {
                last = i;
            }
        }
        return last;
    }

    /**
     * Concatenates an explicit frame path with a frame prefix that was extracted from a compound selector, so that
     * neither information source is dropped.
     *
     * @param frame  the explicit frame path (may be null/blank)
     * @param prefix the additional frame prefix (may be null/blank)
     * @return the combined path, or {@code null} if both are empty
     */
    public static String combine(final String frame, final String prefix)
    {
        final List<String> segs1 = parseFrameHierarchy(frame);
        final List<String> segs2 = parseFrameHierarchy(prefix);
        if (segs1.isEmpty() && segs2.isEmpty())
        {
            return null;
        }
        if (segs1.isEmpty())
        {
            return String.join(FRAME_DELIMITER, segs2);
        }
        if (segs2.isEmpty())
        {
            return String.join(FRAME_DELIMITER, segs1);
        }

        // If segs1 already starts with segs2 (segs1 contains segs2 from root), segs1 is more specific
        if (segs1.size() >= segs2.size() && segs1.subList(0, segs2.size()).equals(segs2))
        {
            return String.join(FRAME_DELIMITER, segs1);
        }

        // If segs2 extends segs1 from root, segs2 is more specific
        if (segs2.size() >= segs1.size() && segs2.subList(0, segs1.size()).equals(segs1))
        {
            return String.join(FRAME_DELIMITER, segs2);
        }

        // Find longest suffix of segs1 that matches the prefix of segs2 (sliding-window overlap)
        final int maxOverlap = Math.min(segs1.size(), segs2.size());
        int overlap = 0;
        for (int k = maxOverlap; k >= 1; k--)
        {
            if (segs1.subList(segs1.size() - k, segs1.size()).equals(segs2.subList(0, k)))
            {
                overlap = k;
                break;
            }
        }

        final List<String> merged = new ArrayList<>(segs1);
        merged.addAll(segs2.subList(overlap, segs2.size()));
        return String.join(FRAME_DELIMITER, merged);
    }

    /**
     * Enters the specified hierarchical frame path from default content and returns an {@link AutoCloseable}
     * scope that restores the state at the time of the call (driver context and tracked path) when closed.
     *
     * @param driver    the active WebDriver instance
     * @param framePath the hierarchical frame path (e.g. {@code "#pact1 >>> #pact2"}); default content is a no-op
     * @return an AutoCloseable frame scope
     * @throws NoSuchFrameException if any frame in the hierarchy cannot be located or switched to
     */
    public static FrameScope enter(final WebDriver driver, final String framePath)
    {
        final String previous = ACTIVE_FRAME_PATH.get();
        if (driver == null)
        {
            return new FrameScope(null, false, previous);
        }
        if (isDefaultContent(framePath))
        {
            if (previous != null)
            {
                restoreContext(driver, null);
                return new FrameScope(driver, true, previous);
            }
            return new FrameScope(driver, false, previous);
        }

        final List<String> hierarchy = parseFrameHierarchy(framePath);
        if (hierarchy.isEmpty())
        {
            if (previous != null)
            {
                restoreContext(driver, null);
                return new FrameScope(driver, true, previous);
            }
            return new FrameScope(driver, false, previous);
        }

        try
        {
            descend(driver, hierarchy);
            ACTIVE_FRAME_PATH.set(normalizeFramePath(framePath));
            return new FrameScope(driver, true, previous);
        }
        catch (final Exception e)
        {
            restoreContext(driver, previous);
            throw new NoSuchFrameException("Failed to switch to frame hierarchy '" + framePath + "': " + e.getMessage(), e);
        }
    }

    /**
     * Makes sure the driver is inside the given frame path, skipping the (expensive) switch if the tracked path
     * already equals it. Unlike {@link #enter(WebDriver, String)} no scope is returned: the caller relies on an
     * enclosing scope to restore the previous context.
     *
     * @param driver    the active WebDriver instance
     * @param framePath the hierarchical frame path
     * @throws NoSuchFrameException if the frame cannot be reached
     */
    public static void ensureEntered(final WebDriver driver, final String framePath)
    {
        if (driver == null || isDefaultContent(framePath))
        {
            return;
        }
        if (Objects.equals(normalizeFramePath(framePath), ACTIVE_FRAME_PATH.get()))
        {
            return;
        }
        // Intentionally not closed: the enclosing scope is responsible for restoring the previous context
        enter(driver, framePath);
    }

    /**
     * Switches the driver to default content and clears the tracked frame path.
     *
     * @param driver the active WebDriver instance (may be null)
     */
    public static void switchToDefaultContent(final WebDriver driver)
    {
        restoreContext(driver, null);
    }

    /**
     * Puts the driver into the given frame path (or default content for {@code null}/blank) and synchronizes the
     * tracked path. Failures never propagate: on error the driver is switched to default content on a best-effort
     * basis and the tracked path is cleared so that both stay consistent.
     *
     * @param driver the WebDriver (may be null)
     * @param path   the frame path to restore, or null for default content
     */
    public static void restoreContext(final WebDriver driver, final String path)
    {
        if (driver == null)
        {
            ACTIVE_FRAME_PATH.remove();
            return;
        }
        try
        {
            if (path == null || path.isBlank() || isDefaultContent(path))
            {
                driver.switchTo().defaultContent();
                ACTIVE_FRAME_PATH.remove();
            }
            else
            {
                descend(driver, parseFrameHierarchy(path));
                ACTIVE_FRAME_PATH.set(normalizeFramePath(path));
            }
        }
        catch (final UnhandledAlertException | NoSuchWindowException e)
        {
            // Dialog open or window closed; leave cleanup to session lifecycle
            LOGGER.debug("Skipping frame context restore ({}): {}", e.getClass().getSimpleName(), e.getMessage());
            ACTIVE_FRAME_PATH.remove();
        }
        catch (final Exception e)
        {
            LOGGER.debug("Could not restore frame context '{}', falling back to default content: {}", path, e.getMessage());
            try
            {
                driver.switchTo().defaultContent();
            }
            catch (final Exception fallback)
            {
                LOGGER.debug("Default content fallback failed: {}", fallback.getMessage());
            }
            ACTIVE_FRAME_PATH.remove();
        }
    }

    /**
     * Computes a stable, unique CSS selector for an iframe element, relative to the document that contains it
     * (so it can be used as one segment of a frame path). Preference order: non-volatile {@code id}, {@code name},
     * {@code title} (each only if it matches exactly one element in the current document), then a structural
     * {@code html > ... > iframe:nth-of-type(n)} path with sibling-correct indices.
     *
     * @param driver  the WebDriver, positioned in the document that contains {@code frameEl}
     * @param frameEl the iframe/frame element
     * @return a CSS selector for the frame
     */
    public static String describeFrame(final WebDriver driver, final WebElement frameEl)
    {
        final String tag = safeTag(frameEl);

        final String id = safeAttribute(frameEl, "id");
        if (id != null && !id.isBlank() && !VOLATILE_ID_DETECTOR.isVolatile(id))
        {
            final String idSelector = id.trim().matches(SAFE_CSS_IDENT) ? "#" + id.trim() : attributeSelector(tag, "id", id.trim());
            if (isUnique(driver, idSelector))
            {
                return idSelector;
            }
        }
        for (final String attr : new String[] {"name", "title"})
        {
            final String value = safeAttribute(frameEl, attr);
            if (value != null && !value.isBlank())
            {
                final String selector = attributeSelector(tag, attr, value.trim());
                if (isUnique(driver, selector))
                {
                    return selector;
                }
            }
        }

        final String src = safeAttribute(frameEl, "src");
        if (src != null && src.indexOf('/') != -1 && !src.startsWith("blob:") && !src.startsWith("data:"))
        {
            final String cleanSrc = src.split("\\?")[0].split("#")[0];
            final String filename = cleanSrc.substring(cleanSrc.lastIndexOf('/') + 1);
            if (filename.length() >= 3)
            {
                final String srcSel = tag + "[src*='" + filename.replace("\\", "\\\\").replace("'", "\\'") + "']";
                if (isUnique(driver, srcSel))
                {
                    return srcSel;
                }
            }
        }

        if (driver instanceof JavascriptExecutor js)
        {
            try
            {
                final Object path = js.executeScript(STRUCTURAL_PATH_SCRIPT, frameEl);
                if (path instanceof String s && !s.isBlank())
                {
                    return s;
                }
            }
            catch (final Exception e)
            {
                LOGGER.debug("Structural frame path computation failed: {}", e.getMessage());
            }
        }
        return tag + ":nth-of-type(1)";
    }

    private static String safeTag(final WebElement el)
    {
        try
        {
            final String tag = el.getTagName();
            return tag == null || tag.isBlank() ? "iframe" : tag.toLowerCase(Locale.ROOT);
        }
        catch (final Exception e)
        {
            LOGGER.debug("Could not read frame tag name: {}", e.getMessage());
            return "iframe";
        }
    }

    private static String safeAttribute(final WebElement el, final String name)
    {
        try
        {
            return el.getAttribute(name);
        }
        catch (final Exception e)
        {
            LOGGER.debug("Could not read frame attribute '{}': {}", name, e.getMessage());
            return null;
        }
    }

    private static String attributeSelector(final String tag, final String attribute, final String value)
    {
        return tag + "[" + attribute + "='" + value.replace("\\", "\\\\").replace("'", "\\'") + "']";
    }

    private static boolean isUnique(final WebDriver driver, final String cssSelector)
    {
        try
        {
            return driver.findElements(By.cssSelector(cssSelector)).size() == 1;
        }
        catch (final Exception e)
        {
            LOGGER.debug("Uniqueness check for '{}' failed: {}", cssSelector, e.getMessage());
            return false;
        }
    }

    /**
     * Switches to default content and then descends through the given frame selectors, waiting up to the
     * configured Selenide timeout for each frame element to appear.
     */
    private static void descend(final WebDriver driver, final List<String> hierarchy)
    {
        driver.switchTo().defaultContent();
        for (final String frameSelector : hierarchy)
        {
            final By by = LocatorResolver.resolveLocator(frameSelector);
            driver.switchTo().frame(awaitFrameElement(driver, by, frameSelector));
        }
    }

    private static WebElement awaitFrameElement(final WebDriver driver, final By by, final String selector)
    {
        final long deadline = System.currentTimeMillis() + Configuration.timeout;
        while (true)
        {
            final List<WebElement> found = driver.findElements(by);
            if (!found.isEmpty())
            {
                return found.get(0);
            }
            if (System.currentTimeMillis() >= deadline)
            {
                throw new NoSuchFrameException("Frame element '" + selector + "' not found within " + Configuration.timeout + " ms");
            }
            try
            {
                Thread.sleep(FRAME_POLL_INTERVAL_MS);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new NoSuchFrameException("Interrupted while waiting for frame '" + selector + "'", e);
            }
        }
    }

    /**
     * Scoped frame context that restores the context that was active when the scope was opened.
     */
    public static final class FrameScope implements AutoCloseable
    {
        private final WebDriver driver;
        private final boolean switched;
        private final String previousFrame;

        private FrameScope(final WebDriver driver, final boolean switched, final String previousFrame)
        {
            this.driver = driver;
            this.switched = switched;
            this.previousFrame = previousFrame;
        }

        /**
         * Restores the driver and tracked path to the state at scope creation. Does nothing if the scope neither
         * switched frames nor observed a changed tracked path (e.g. via auto-discovery).
         */
        @Override
        public void close()
        {
            if (this.driver != null && (this.switched || !Objects.equals(ACTIVE_FRAME_PATH.get(), this.previousFrame)))
            {
                restoreContext(this.driver, this.previousFrame);
            }
        }
    }
}
