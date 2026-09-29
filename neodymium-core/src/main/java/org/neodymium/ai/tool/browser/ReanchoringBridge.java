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

    private static final String RESOLVE_DOM_ELEMENT_SCRIPT = """
        return (function(vx, vy) {
            var el = document.elementFromPoint(vx, vy);
            if (!el || el === document.body || el === document.documentElement) {
                return null;
            }
            var rect = el.getBoundingClientRect();
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

            var text = (el.innerText || el.textContent || '').trim().replace(/\\s+/g, ' ');
            if (text.length > 50) text = text.substring(0, 50);

            return {
                selector: selector,
                tagName: el.tagName.toLowerCase(),
                text: text,
                relX: Math.round(vx - rect.left),
                relY: Math.round(vy - rect.top),
                x: Math.round(rect.left),
                y: Math.round(rect.top),
                width: Math.round(rect.width),
                height: Math.round(rect.height)
            };
        })(arguments[0], arguments[1]);
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
                    final Map<String, Object> map = (Map<String, Object>) rawMap;
                    final String selector = (String) map.get("selector");
                    final String tagName = (String) map.get("tagName");
                    final String text = (String) map.get("text");
                    final int relX = ((Number) map.getOrDefault("relX", 0)).intValue();
                    final int relY = ((Number) map.getOrDefault("relY", 0)).intValue();

                    final Map<String, Object> featureVector = new LinkedHashMap<>();
                    featureVector.put("selector", selector);
                    featureVector.put("tagName", tagName);
                    featureVector.put("text", text);
                    featureVector.put("x", map.get("x"));
                    featureVector.put("y", map.get("y"));
                    featureVector.put("width", map.get("width"));
                    featureVector.put("height", map.get("height"));

                    return new ReanchoredElement(selector, tagName, text, relX, relY, Collections.unmodifiableMap(featureVector));
                }
            }
            catch (final Exception e)
            {
                // Fallback
            }
        }
        return null;
    }
}
