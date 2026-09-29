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
import java.util.List;
import java.util.Map;

/**
 * Injects and cleans up temporary numeric Set-of-Marks (SoM) visual badges over
 * interactive element centers for visual multimodal grounding.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class VisualBadgeInjector
{
    private static final String INJECT_BADGES_SCRIPT = """
        return (function() {
            var existing = document.getElementById('__neo_som_badges__');
            if (existing) existing.remove();

            var container = document.createElement('div');
            container.id = '__neo_som_badges__';
            container.style.position = 'fixed';
            container.style.top = '0';
            container.style.left = '0';
            container.style.width = '100vw';
            container.style.height = '100vh';
            container.style.pointerEvents = 'none';
            container.style.zIndex = '2147483647';

            var interactiveSelectors = 'a, button, input, select, textarea, [role="button"], [role="link"], [onclick]';
            var elements = document.querySelectorAll(interactiveSelectors);
            var badgeList = [];
            var index = 1;

            for (var i = 0; i < elements.length; i++) {
                var el = elements[i];
                var rect = el.getBoundingClientRect();
                if (rect.width <= 0 || rect.height <= 0) continue;
                if (rect.bottom < 0 || rect.top > window.innerHeight || rect.right < 0 || rect.left > window.innerWidth) continue;

                var style = window.getComputedStyle(el);
                if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') continue;

                var badge = document.createElement('div');
                badge.innerText = index;
                badge.style.position = 'absolute';
                badge.style.left = (rect.left + rect.width / 2 - 10) + 'px';
                badge.style.top = (rect.top + rect.height / 2 - 10) + 'px';
                badge.style.backgroundColor = '#facc15';
                badge.style.color = '#000000';
                badge.style.border = '2px solid #000000';
                badge.style.borderRadius = '50%';
                badge.style.width = '20px';
                badge.style.height = '20px';
                badge.style.fontSize = '12px';
                badge.style.fontWeight = 'bold';
                badge.style.display = 'flex';
                badge.style.alignItems = 'center';
                badge.style.justifyContent = 'center';
                badge.style.boxShadow = '0 2px 4px rgba(0,0,0,0.5)';

                container.appendChild(badge);

                var autoId = el.id ? '#' + el.id : (el.tagName.toLowerCase() + (el.className ? '.' + el.className.trim().split(/\\s+/)[0] : ''));
                badgeList.push({
                    badge: index,
                    selector: autoId,
                    centerX: Math.round(rect.left + rect.width / 2),
                    centerY: Math.round(rect.top + rect.height / 2)
                });
                index++;
                if (index > 100) break;
            }

            document.body.appendChild(container);
            return badgeList;
        })();
        """;

    private static final String REMOVE_BADGES_SCRIPT = """
        var existing = document.getElementById('__neo_som_badges__');
        if (existing) existing.remove();
        """;

    private VisualBadgeInjector()
    {
        // Static utility
    }

    /**
     * Injects visual numeric badges over interactive elements and returns the badge mapping.
     *
     * @param driver active WebDriver instance
     * @return list of badge metadata maps
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> injectBadges(final WebDriver driver)
    {
        if (driver instanceof final JavascriptExecutor js)
        {
            try
            {
                final Object result = js.executeScript(INJECT_BADGES_SCRIPT);
                if (result instanceof List<?> list)
                {
                    return (List<Map<String, Object>>) list;
                }
            }
            catch (final Exception e)
            {
                // Non-fatal, continue without badges
            }
        }
        return Collections.emptyList();
    }

    /**
     * Removes temporary visual badges from the page.
     *
     * @param driver active WebDriver instance
     */
    public static void removeBadges(final WebDriver driver)
    {
        if (driver instanceof final JavascriptExecutor js)
        {
            try
            {
                js.executeScript(REMOVE_BADGES_SCRIPT);
            }
            catch (final Exception e)
            {
                // Non-fatal cleanup
            }
        }
    }
}
