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
package org.neodymium.ai.tool.browser;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Injects and cleans up dual-surface visual element markers (Set-of-Marks)
 * comprising high-contrast screen overlays and compact {@code data-m="N"} DOM attributes.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class VisualBadgeInjector
{
    private static final String INJECT_MARKERS_SCRIPT = """
        return (function(scopeSelector, maxLimit) {
            var existing = document.getElementById('__neo_som_badges__');
            if (existing) existing.remove();

            var oldStamped = document.querySelectorAll('[data-m]');
            for (var k = 0; k < oldStamped.length; k++) {
                oldStamped[k].removeAttribute('data-m');
            }

            var limit = (typeof maxLimit === 'number' && maxLimit > 0) ? maxLimit : 100;
            var root = document;
            if (scopeSelector && typeof scopeSelector === 'string' && scopeSelector.trim()) {
                var scopedEl = document.querySelector(scopeSelector.trim());
                if (scopedEl) {
                    try {
                        scopedEl.scrollIntoView({ behavior: 'instant', block: 'center', inline: 'nearest' });
                    } catch (e) {
                        try { scopedEl.scrollIntoView(true); } catch (e2) {}
                    }
                    root = scopedEl;
                }
            }

            var interactiveSelectors = 'a, button, input, select, textarea, summary, label[for], [contenteditable="true"], [tabindex="0"], '
                + '[role="button"], [role="link"], [role="menuitem"], [role="tab"], [role="checkbox"], [role="radio"], '
                + '[role="switch"], [role="combobox"], [role="option"], [role="slider"], [role="spinbutton"], [role="searchbox"], [role="treeitem"], '
                + '[onclick], [data-action], [data-clickable], '
                + '[class*="btn"], [class*="clickable"], [class*="swatch"], [class*="chip"], [class*="pill"], svg';
            var rawElements = Array.prototype.slice.call(root.querySelectorAll(interactiveSelectors));
            if (root !== document && root.matches && root.matches(interactiveSelectors)) {
                rawElements.unshift(root);
            }
            var seen = new Set();
            var visibleCandidates = [];

            for (var i = 0; i < rawElements.length; i++) {
                var el = rawElements[i];
                if (seen.has(el)) continue;
                seen.add(el);

                // If label wraps an interactive control, target the control directly rather than duplicating
                if (el.tagName.toLowerCase() === 'label' && el.querySelector('input, select, textarea, button')) {
                    continue;
                }

                // If child is inside an interactive ancestor, suppress child so the parent container gets marked
                if (el.parentElement && el.parentElement.closest('a, button, select, textarea, summary, [role="button"], [role="link"], [role="tab"], [role="menuitem"], [role="checkbox"], [role="radio"], [role="switch"]')) {
                    continue;
                }

                var rect = el.getBoundingClientRect();
                if (rect.width <= 0 || rect.height <= 0) continue;
                if (rect.bottom < 0 || rect.top > window.innerHeight || rect.right < 0 || rect.left > window.innerWidth) continue;

                var style = window.getComputedStyle(el);
                if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') continue;

                // For non-semantic class-matched elements, verify they exhibit clickable cursor or event handler
                var tag = el.tagName.toLowerCase();
                var isSemantic = tag === 'a' || tag === 'button' || tag === 'input' || tag === 'select' || tag === 'textarea'
                    || tag === 'summary' || tag === 'label' || tag === 'svg' || el.hasAttribute('role') || el.hasAttribute('onclick')
                    || el.hasAttribute('data-action') || el.hasAttribute('tabindex')
                    || el.isContentEditable || el.hasAttribute('contenteditable');
                if (!isSemantic && style.cursor !== 'pointer') {
                    continue;
                }

                visibleCandidates.push({ el: el, rect: rect });
            }

            // Sort in visual reading order: top-to-bottom (bucketed by row) then left-to-right
            visibleCandidates.sort(function(a, b) {
                var rowA = Math.floor(a.rect.top / 12);
                var rowB = Math.floor(b.rect.top / 12);
                if (rowA !== rowB) return rowA - rowB;
                return a.rect.left - b.rect.left;
            });

            var container = document.createElement('div');
            container.id = '__neo_som_badges__';
            if (scopeSelector && typeof scopeSelector === 'string' && scopeSelector.trim()) {
                container.setAttribute('data-scope', scopeSelector.trim());
            }
            container.style.position = 'fixed';
            container.style.top = '0';
            container.style.left = '0';
            container.style.width = '100vw';
            container.style.height = '100vh';
            container.style.pointerEvents = 'none';
            container.style.zIndex = '2147483647';

            var markerList = [];
            var count = Math.min(visibleCandidates.length, limit);

            for (var j = 0; j < count; j++) {
                var item = visibleCandidates[j];
                var targetEl = item.el;
                var r = item.rect;
                var markerIndex = j + 1;

                // 1. Stamp DOM with compact foreign key attribute
                targetEl.setAttribute('data-m', String(markerIndex));

                // 2. High-contrast perimeter outline (leaves target interior visible)
                var box = document.createElement('div');
                box.style.position = 'fixed';
                box.style.left = Math.round(r.left) + 'px';
                box.style.top = Math.round(r.top) + 'px';
                box.style.width = Math.round(r.width) + 'px';
                box.style.height = Math.round(r.height) + 'px';
                box.style.border = '2px solid #e11d48';
                box.style.backgroundColor = 'rgba(225, 29, 72, 0.04)';
                box.style.boxSizing = 'border-box';
                box.style.pointerEvents = 'none';
                container.appendChild(box);

                // 3. Boundary-adaptive micro-tag (flips inside box if near viewport top edge)
                var tag = document.createElement('div');
                tag.innerText = String(markerIndex);
                tag.style.position = 'fixed';
                tag.style.left = Math.max(0, Math.round(r.left)) + 'px';
                var topPos = r.top >= 18 ? (r.top - 18) : r.top;
                tag.style.top = Math.max(0, Math.round(topPos)) + 'px';
                tag.style.backgroundColor = '#e11d48';
                tag.style.color = '#ffffff';
                tag.style.padding = '1px 5px';
                tag.style.borderRadius = '3px';
                tag.style.fontSize = '11px';
                tag.style.fontWeight = 'bold';
                tag.style.lineHeight = '14px';
                tag.style.fontFamily = 'monospace, sans-serif';
                tag.style.boxShadow = '0 1px 3px rgba(0,0,0,0.6)';
                tag.style.pointerEvents = 'none';
                container.appendChild(tag);

                markerList.push({
                    id: markerIndex,
                    m: markerIndex,
                    tag: targetEl.tagName.toLowerCase(),
                    x: Math.round(r.left),
                    y: Math.round(r.top),
                    w: Math.round(r.width),
                    h: Math.round(r.height),
                    width: Math.round(r.width),
                    height: Math.round(r.height),
                    centerX: Math.round(r.left + r.width / 2),
                    centerY: Math.round(r.top + r.height / 2)
                });
            }

            document.body.appendChild(container);
            return markerList;
        })(arguments[0], arguments[1]);
        """;

    private static final String REMOVE_MARKERS_SCRIPT = """
        var existing = document.getElementById('__neo_som_badges__');
        if (existing) existing.remove();
        var stamped = document.querySelectorAll('[data-m]');
        for (var i = 0; i < stamped.length; i++) {
            stamped[i].removeAttribute('data-m');
        }
        """;

    private static volatile String activeScope = null;

    private VisualBadgeInjector()
    {
        // Static utility
    }

    /**
     * Returns the active CSS scope selector used during the most recent marker injection, or {@code null}.
     *
     * @return active scope selector, or {@code null}
     */
    public static String getActiveScope()
    {
        return activeScope;
    }

    /**
     * Injects visual markers over interactive elements, stamps {@code data-m="N"} into the DOM,
     * and returns the candidate metadata list.
     *
     * @param driver active WebDriver instance
     * @return list of marker candidate metadata maps
     */
    public static List<Map<String, Object>> injectMarkers(final WebDriver driver)
    {
        return injectMarkers(driver, null, 100);
    }

    /**
     * Injects visual markers within an optional scope, stamps {@code data-m="N"} into the DOM,
     * and returns the candidate metadata list up to {@code maxCount}.
     *
     * @param driver active WebDriver instance
     * @param scope optional CSS selector scoping marker injection
     * @param maxCount maximum number of elements to mark
     * @return list of marker candidate metadata maps
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> injectMarkers(final WebDriver driver, final String scope, final int maxCount)
    {
        activeScope = (scope != null && !scope.isBlank()) ? scope.trim() : null;
        if (driver instanceof final JavascriptExecutor js)
        {
            try
            {
                final Object result = js.executeScript(INJECT_MARKERS_SCRIPT, scope, maxCount);
                if (result instanceof List<?> list)
                {
                    return (List<Map<String, Object>>) list;
                }
            }
            catch (final Exception e)
            {
                // Non-fatal, continue without markers
            }
        }
        return Collections.emptyList();
    }

    /**
     * Removes all visual marker overlays and purges {@code data-m} attributes from the DOM.
     *
     * @param driver active WebDriver instance
     */
    public static void removeMarkers(final WebDriver driver)
    {
        activeScope = null;
        if (driver instanceof final JavascriptExecutor js)
        {
            try
            {
                js.executeScript(REMOVE_MARKERS_SCRIPT);
            }
            catch (final Exception e)
            {
                // Non-fatal cleanup
            }
        }
    }

    /**
     * Backwards-compatible alias for {@link #injectMarkers(WebDriver)}.
     *
     * @param driver active WebDriver instance
     * @return list of badge metadata maps
     */
    public static List<Map<String, Object>> injectBadges(final WebDriver driver)
    {
        return injectMarkers(driver);
    }

    /**
     * Backwards-compatible alias for {@link #injectMarkers(WebDriver, String, int)}.
     *
     * @param driver active WebDriver instance
     * @param scope optional CSS selector scoping injection
     * @param maxCount maximum elements to mark
     * @return list of badge metadata maps
     */
    public static List<Map<String, Object>> injectBadges(final WebDriver driver, final String scope, final int maxCount)
    {
        return injectMarkers(driver, scope, maxCount);
    }

    /**
     * Backwards-compatible alias for {@link #removeMarkers(WebDriver)}.
     *
     * @param driver active WebDriver instance
     */
    public static void removeBadges(final WebDriver driver)
    {
        removeMarkers(driver);
    }
}
