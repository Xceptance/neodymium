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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.neodymium.ai.executor.probe.LocatorProbeResult;
import org.neodymium.ai.executor.probe.ProbeBoundingRect;
import org.neodymium.ai.executor.probe.ProbeElementSummary;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Driver probing implementation for Selenium / Selenide WebDriver SUT execution.
 * Extracts read-only W3C element telemetry, interactability, geometry, and attributes.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class SelenideLocatorProber
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SelenideLocatorProber.class);

    private static final List<String> STANDARD_PROBE_ATTRIBUTES = List.of(
            "id", "name", "class", "type", "role", "aria-label", "aria-expanded",
            "data-testid", "data-test", "placeholder", "href", "value", "title", "data-ai"
    );

    private SelenideLocatorProber()
    {
        // Static utility
    }

    /**
     * Probes candidate locators against the live WebDriver DOM.
     *
     * @param driver active WebDriver instance
     * @param candidateLocators list of candidate locators to probe
     * @param maxDepth maximum elements to summarize per candidate
     * @return list of probe results
     */
    public static List<LocatorProbeResult> probe(
            final WebDriver driver,
            final List<String> candidateLocators,
            final int maxDepth)
    {
        if (driver == null || candidateLocators == null || candidateLocators.isEmpty())
        {
            return Collections.emptyList();
        }

        final int depth = maxDepth > 0 ? maxDepth : 3;
        final List<LocatorProbeResult> results = new ArrayList<>(candidateLocators.size());

        for (final String candidate : candidateLocators)
        {
            if (candidate == null || candidate.isBlank())
            {
                results.add(LocatorProbeResult.error("", "Blank locator string"));
                continue;
            }

            try
            {
                final By by = LocatorResolver.resolveLocator(candidate);
                final List<WebElement> matched = driver.findElements(by);
                final int totalCount = matched.size();

                final List<ProbeElementSummary> summaries = new ArrayList<>();
                final int limit = Math.min(totalCount, depth);

                for (int i = 0; i < limit; i++)
                {
                    final WebElement el = matched.get(i);
                    summaries.add(extractSummary(el, i));
                }

                results.add(LocatorProbeResult.supported(candidate, totalCount, summaries));
            }
            catch (final Exception e)
            {
                LOGGER.debug("Probing failed for candidate '{}': {}", candidate, e.getMessage());
                results.add(LocatorProbeResult.error(candidate, e.getMessage()));
            }
        }

        return Collections.unmodifiableList(results);
    }

    private static ProbeElementSummary extractSummary(final WebElement element, final int index)
    {
        final String tagName = safeGetTagName(element);
        final String text = safeGetText(element);
        final Map<String, String> attributes = safeGetAttributes(element);
        final boolean visible = safeIsDisplayed(element);
        final boolean enabled = safeIsEnabled(element);
        final boolean selected = safeIsSelected(element);
        final ProbeBoundingRect rect = safeGetRect(element);
        final String snippet = safeGetOuterHtml(element);

        return new ProbeElementSummary(index, tagName, text, attributes, visible, enabled, selected, rect, snippet);
    }

    private static String safeGetTagName(final WebElement element)
    {
        try
        {
            final String tag = element.getTagName();
            return tag != null ? tag.toLowerCase() : "";
        }
        catch (final Exception e)
        {
            return "";
        }
    }

    private static String safeGetText(final WebElement element)
    {
        try
        {
            final String txt = element.getText();
            return txt != null ? txt.trim() : "";
        }
        catch (final Exception e)
        {
            return "";
        }
    }

    private static Map<String, String> safeGetAttributes(final WebElement element)
    {
        final Map<String, String> map = new LinkedHashMap<>();
        for (final String attrName : STANDARD_PROBE_ATTRIBUTES)
        {
            try
            {
                final String val = element.getAttribute(attrName);
                if (val != null && !val.isBlank())
                {
                    map.put(attrName, val.trim());
                }
            }
            catch (final Exception ignored)
            {
                // Attribute not accessible
            }
        }
        return map;
    }

    private static boolean safeIsDisplayed(final WebElement element)
    {
        try
        {
            return element.isDisplayed();
        }
        catch (final Exception e)
        {
            return false;
        }
    }

    private static boolean safeIsEnabled(final WebElement element)
    {
        try
        {
            return element.isEnabled();
        }
        catch (final Exception e)
        {
            return false;
        }
    }

    private static boolean safeIsSelected(final WebElement element)
    {
        try
        {
            return element.isSelected();
        }
        catch (final Exception e)
        {
            return false;
        }
    }

    private static ProbeBoundingRect safeGetRect(final WebElement element)
    {
        try
        {
            final Rectangle r = element.getRect();
            if (r != null)
            {
                return new ProbeBoundingRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
            }
            final Point loc = element.getLocation();
            final Dimension dim = element.getSize();
            return new ProbeBoundingRect(loc.getX(), loc.getY(), dim.getWidth(), dim.getHeight());
        }
        catch (final Exception e)
        {
            return new ProbeBoundingRect(0, 0, 0, 0);
        }
    }

    private static String safeGetOuterHtml(final WebElement element)
    {
        try
        {
            final String outerHtml = element.getAttribute("outerHTML");
            if (outerHtml == null || outerHtml.isBlank())
            {
                return "";
            }
            final String trimmed = outerHtml.trim();
            if (trimmed.length() > 250)
            {
                return trimmed.substring(0, 250) + "...";
            }
            return trimmed;
        }
        catch (final Exception e)
        {
            return "";
        }
    }
}
