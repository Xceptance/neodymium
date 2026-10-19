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

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Re-anchors coordinate- or visual-badge-based interactions back into deterministic DOM locators
 * and feature vectors using {@code document.elementFromPoint(x, y)}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class ReanchoringBridge
{

    private static final String EXTRACT_ELEMENT_HELPER_JS = """
        function computeGraphicSig(el) {
            if (!el) return '';
            var tName = el.tagName ? el.tagName.toLowerCase() : '';
            var svg = tName === 'svg' ? el : (el.querySelector ? el.querySelector('svg') : null);
            if (svg) {
                var vb = svg.getAttribute('viewBox') || '';
                var parts = [];
                var shapes = svg.querySelectorAll ? svg.querySelectorAll('path, circle, rect, line, polygon, polyline, use') : [];
                for (var s = 0; s < shapes.length && s < 6; s++) {
                    var sh = shapes[s];
                    var st = sh.tagName ? sh.tagName.toLowerCase() : '';
                    var d = sh.getAttribute('d') || sh.getAttribute('points') || sh.getAttribute('href') || sh.getAttribute('xlink:href')
                        || ((sh.getAttribute('cx') || '') + ',' + (sh.getAttribute('cy') || '') + ',' + (sh.getAttribute('r') || '')
                            + (sh.getAttribute('width') || '') + 'x' + (sh.getAttribute('height') || ''));
                    if (d) {
                        d = d.trim().replace(/\\s+/g, ' ');
                        if (d.length > 48) d = d.substring(0, 48);
                        parts.push(st + ':' + d);
                    }
                }
                return 'svg[' + vb + ']{' + parts.join(';') + '}';
            }
            var img = tName === 'img' ? el : (el.querySelector ? el.querySelector('img') : null);
            if (img) {
                var src = img.getAttribute('src') || '';
                if (src) {
                    var qIdx = src.indexOf('?');
                    if (qIdx > 0) src = src.substring(0, qIdx);
                    if (src.length > 64) src = src.substring(src.length - 64);
                    return 'img:' + src;
                }
            }
            return '';
        }
        function buildReanchoredResult(el, vx, vy) {
            if (!el || el === document.body || el === document.documentElement) {
                return null;
            }
            var rect = el.getBoundingClientRect ? el.getBoundingClientRect() : {left: 0, top: 0, width: 0, height: 0};
            var id = el.id ? '#' + el.id : null;
            var dataAi = el.getAttribute('data-ai') ? '[data-ai="' + el.getAttribute('data-ai') + '"]' : null;
            var name = el.getAttribute('name') ? el.tagName.toLowerCase() + '[name="' + el.getAttribute('name') + '"]' : null;
            var selector = id || dataAi || name;

            if (!selector && el.className && typeof el.className === 'string' && el.className.trim()) {
                var cls = el.className.trim().split(/\\s+/)[0];
                if (cls && !cls.includes(':') && !cls.includes('/')) {
                    selector = el.tagName.toLowerCase() + '.' + cls;
                }
            }
            if (!selector) {
                selector = el.tagName.toLowerCase();
            }

            var text = (el.innerText || el.value || el.placeholder || el.textContent || '').trim().replace(/\\s+/g, ' ');
            if (text.length > 50) text = text.substring(0, 50);

            var attrs = {};
            if (el.attributes) {
                for (var i = 0; i < el.attributes.length; i++) {
                    var a = el.attributes[i];
                    if (a.name !== 'class' && a.name !== 'style' && a.name !== 'data-m' && !a.name.startsWith('data-ai')) {
                        attrs[a.name] = a.value;
                    }
                }
            }
            var gSig = computeGraphicSig(el);
            if (gSig) {
                attrs['data-graphic-sig'] = gSig;
            }

            var classes = [];
            if (el.className && typeof el.className === 'string') {
                classes = el.className.trim().split(/\\s+/).filter(Boolean);
            }

            var role = el.getAttribute('role') || '';
            var accessibleName = el.getAttribute('aria-label') || el.getAttribute('title') || el.getAttribute('placeholder')
                || (el.labels && el.labels[0] ? el.labels[0].innerText : '') || text;
            var parentTag = el.parentElement ? (el.parentElement.tagName ? el.parentElement.tagName.toLowerCase() : '') : '';
            var siblingIndex = (el.parentElement && el.parentElement.children)
                ? Array.prototype.indexOf.call(el.parentElement.children, el) : 0;

            var effVx = (typeof vx === 'number') ? vx : Math.round(rect.left + rect.width / 2);
            var effVy = (typeof vy === 'number') ? vy : Math.round(rect.top + rect.height / 2);

            return {
                selector: selector,
                tag: el.tagName.toLowerCase(),
                tagName: el.tagName.toLowerCase(),
                text: text,
                classes: classes,
                attributes: attrs,
                role: role,
                accessibleName: accessibleName ? accessibleName.trim() : '',
                parentTag: parentTag,
                siblingIndex: siblingIndex >= 0 ? siblingIndex : 0,
                relX: Math.round(effVx - rect.left),
                relY: Math.round(effVy - rect.top),
                x: Math.round(rect.left),
                y: Math.round(rect.top),
                width: Math.round(rect.width),
                height: Math.round(rect.height)
            };
        }
        """;

    private static final String RESOLVE_DOM_ELEMENT_SCRIPT =
        "return (function(vx, vy) {\n"
        + EXTRACT_ELEMENT_HELPER_JS
        + """
            var el = document.elementFromPoint(vx, vy);
            if (!el || el === document.body || el === document.documentElement) {
                return null;
            }
            if (el.closest) {
                var marked = el.closest('[data-m]');
                if (marked) {
                    el = marked;
                } else {
                    var t = el.tagName ? el.tagName.toLowerCase() : '';
                    if (['svg', 'path', 'circle', 'rect', 'line', 'polygon', 'polyline', 'use', 'g', 'span', 'i', 'em', 'strong', 'b', 'img'].indexOf(t) !== -1) {
                        var inter = el.closest('button, a, input, select, textarea, summary, label, [role="button"], [role="link"], [role="checkbox"], [role="switch"], [role="radio"], [role="menuitem"], [role="tab"], [onclick]');
                        if (inter) {
                            el = inter;
                        }
                    }
                }
            }
            return buildReanchoredResult(el, vx, vy);
        })(arguments[0], arguments[1]);
        """;

    private static final String RESOLVE_MARKED_ELEMENT_SCRIPT =
        "return (function(markerId) {\n"
        + EXTRACT_ELEMENT_HELPER_JS
        + """
            var el = document.querySelector('[data-m="' + markerId + '"]');
            if (!el) {
                return null;
            }
            return buildReanchoredResult(el, null, null);
        })(arguments[0]);
        """;

    /**
     * Immutable result of re-anchoring a coordinate click to a real DOM element.
     *
     * @param selector resilient CSS selector for the resolved element
     * @param tagName tag name of the element
     * @param text snippet of element text
     * @param relativeX X offset relative to the element top-left
     * @param relativeY Y offset relative to the element top-left
     * @param domFeatureVector feature metadata map for offline similarity healing
     */
    public record ReanchoredElement(
            String selector,
            String tagName,
            String text,
            int relativeX,
            int relativeY,
            Map<String, Object> domFeatureVector)
    {
    }

    private ReanchoringBridge()
    {
        // Static utility
    }

    /**
     * Resolves the DOM element at the given viewport coordinates and captures its feature vector.
     *
     * @param driver active WebDriver
     * @param viewportX X coordinate
     * @param viewportY Y coordinate
     * @return ReanchoredElement or null if no element could be resolved
     */
    @SuppressWarnings("unchecked")
    public static ReanchoredElement resolveElementAtPoint(final WebDriver driver, final int viewportX, final int viewportY)
    {
        if (driver instanceof final JavascriptExecutor js)
        {
            try
            {
                final Object result = js.executeScript(RESOLVE_DOM_ELEMENT_SCRIPT, viewportX, viewportY);
                if (result instanceof Map<?, ?> rawMap)
                {
                    return toReanchoredElement((Map<String, Object>) rawMap);
                }
            }
            catch (final Exception e)
            {
                // Fallback
            }
        }
        return null;
    }

    /**
     * Resolves a Set-of-Marks element directly by its {@code data-m} attribute value before
     * marker cleanup removes the badge attribute, avoiding any {@code document.elementFromPoint}
     * child SVG/icon ambiguity.
     *
     * @param driver active WebDriver
     * @param markerId the numeric or string marker identifier (e.g. {@code "15"})
     * @return ReanchoredElement or null if no marked element exists
     */
    @SuppressWarnings("unchecked")
    public static ReanchoredElement resolveMarkedElement(final WebDriver driver, final String markerId)
    {
        if (markerId == null || markerId.isBlank())
        {
            return null;
        }
        if (driver instanceof final JavascriptExecutor js)
        {
            try
            {
                final Object result = js.executeScript(RESOLVE_MARKED_ELEMENT_SCRIPT, markerId.trim());
                if (result instanceof Map<?, ?> rawMap)
                {
                    return toReanchoredElement((Map<String, Object>) rawMap);
                }
            }
            catch (final Exception e)
            {
                // Fallback
            }
        }
        return null;
    }

    private static ReanchoredElement toReanchoredElement(final Map<String, Object> map)
    {
        final String selector = (String) map.get("selector");
        final String tagName = (String) (map.get("tagName") != null ? map.get("tagName") : map.get("tag"));
        final String text = (String) map.getOrDefault("text", "");
        final int relX = map.get("relX") instanceof Number nX ? nX.intValue() : 0;
        final int relY = map.get("relY") instanceof Number nY ? nY.intValue() : 0;

        final Map<String, Object> featureVector = new LinkedHashMap<>();
        featureVector.put("selector", selector);
        featureVector.put("tag", tagName);
        featureVector.put("tagName", tagName);
        featureVector.put("text", text);
        if (map.get("classes") != null)
        {
            featureVector.put("classes", map.get("classes"));
        }
        if (map.get("attributes") != null)
        {
            featureVector.put("attributes", map.get("attributes"));
        }
        featureVector.put("role", map.getOrDefault("role", ""));
        featureVector.put("accessibleName", map.getOrDefault("accessibleName", text != null ? text : ""));
        featureVector.put("parentTag", map.getOrDefault("parentTag", ""));
        featureVector.put("siblingIndex", map.get("siblingIndex") instanceof Number nSib ? nSib.intValue() : 0);
        featureVector.put("x", map.get("x"));
        featureVector.put("y", map.get("y"));
        featureVector.put("width", map.get("width"));
        featureVector.put("height", map.get("height"));
        if (map.get("visualHash") != null)
        {
            featureVector.put("visualHash", map.get("visualHash"));
        }
        if (map.get("tileSsim") != null)
        {
            featureVector.put("tileSsim", map.get("tileSsim"));
        }

        return new ReanchoredElement(selector, tagName, text, relX, relY, Collections.unmodifiableMap(featureVector));
    }
}
