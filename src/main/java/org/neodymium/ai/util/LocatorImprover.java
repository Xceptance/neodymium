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
package org.neodymium.ai.util;

import java.util.ArrayList;
import java.util.List;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.executor.selenide.VolatileIdDetector;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for evaluating, scoring, and improving element locators.
 * <p>
 * Inspects a target {@link WebElement} to generate candidate CSS locators based on standard DOM attributes
 * (such as {@code id}, {@code data-testid}, {@code name}, {@code aria-label}, and {@code placeholder}).
 * Candidate locators are validated for:
 * </p>
 * <ol>
 *   <li><b>Higher Quality Score:</b> Candidate score must strictly exceed the original locator score.</li>
 *   <li><b>Uniqueness:</b> The candidate locator must match exactly 1 element in the DOM.</li>
 *   <li><b>Identity Match:</b> The element matched by the candidate locator must be equal to the target element.</li>
 * </ol>
 * <p>
 * Guarded by configuration property {@code neodymium.ai.locatorImprover.enabled} (default {@code true}).
 * </p>
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class LocatorImprover
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LocatorImprover.class);
    private static final VolatileIdDetector VOLATILE_ID_DETECTOR = new VolatileIdDetector();

    /**
     * Private constructor to prevent instantiation.
     */
    private LocatorImprover()
    {
    }

    /**
     * Scores the quality of a given CSS/XPath locator string.
     * Higher score indicates a more robust, standard locator.
     *
     * @param locator the locator string to score
     * @return an integer score from 0 (poor/invalid) to 10 (gold standard)
     */
    public static int scoreLocator(final String locator)
    {
        if (locator == null || locator.isBlank())
        {
            return 0;
        }

        final String trimmed = locator.trim();

        // Unique ID selector (#my-id or tag#my-id)
        if (trimmed.startsWith("#") || (trimmed.contains("#") && !trimmed.contains(" ") && !trimmed.contains(">")))
        {
            final String idVal = trimmed.substring(trimmed.indexOf('#') + 1);
            if (VOLATILE_ID_DETECTOR.isVolatile(idVal))
            {
                return 0;
            }
            return 10;
        }

        // Test ID attributes (data-testid / data-test)
        if (trimmed.contains("[data-testid=") || trimmed.contains("[data-test="))
        {
            return 10;
        }

        // Standard attributes (name, aria-label, placeholder)
        if (trimmed.contains("[name=") || trimmed.contains("[aria-label=") || trimmed.contains("[placeholder="))
        {
            return 8;
        }

        // Semantic data attributes (data-country, data-value, data-code, data-lang, data-qa, data-id)
        if (trimmed.contains("[data-country=") || trimmed.contains("[data-value=") || trimmed.contains("[data-code=")
            || trimmed.contains("[data-lang=") || trimmed.contains("[data-qa=") || trimmed.contains("[data-id="))
        {
            return 9;
        }

        // Single clean CSS class (.btn-primary)
        if (trimmed.startsWith(".") && !trimmed.contains(" ") && !trimmed.contains(">") && !trimmed.contains(":"))
        {
            return 6;
        }

        // Synthetic Neodymium data-ai tag
        if (trimmed.contains("data-ai"))
        {
            return 4;
        }

        // Complex combinators, nth-child, or raw XPath expressions
        if (trimmed.contains(">") || trimmed.contains(":") || trimmed.startsWith("/"))
        {
            return 2;
        }

        return 5;
    }

    /**
     * Generates a list of candidate CSS locator strings from standard element attributes.
     *
     * @param element the target DOM element
     * @return ordered list of candidate locator strings
     */
    public static List<String> generateCandidates(final WebElement element)
    {
        final List<String> candidates = new ArrayList<>();
        if (element == null)
        {
            return candidates;
        }

        try
        {
            final String tagName = element.getTagName() != null ? element.getTagName().toLowerCase() : "";

            // Candidate 1: ID
            final String id = element.getAttribute("id");
            if (id != null && !id.isBlank() && !VOLATILE_ID_DETECTOR.isVolatile(id))
            {
                candidates.add("#" + escapeCssIdentifier(id));
            }

            // Candidate 2: data-testid / data-test
            final String testId = element.getAttribute("data-testid");
            if (testId != null && !testId.isBlank())
            {
                candidates.add("[data-testid='" + escapeAttributeValue(testId) + "']");
            }
            final String testAttr = element.getAttribute("data-test");
            if (testAttr != null && !testAttr.isBlank())
            {
                candidates.add("[data-test='" + escapeAttributeValue(testAttr) + "']");
            }

            // Candidate 3: Semantic domain data attributes
            for (final String attr : new String[] {"data-country", "data-value", "data-code", "data-lang", "data-qa", "data-id"})
            {
                final String val = element.getAttribute(attr);
                if (val != null && !val.isBlank() && !VOLATILE_ID_DETECTOR.isVolatile(val))
                {
                    final String tagPrefix = !tagName.isBlank() ? tagName : "";
                    candidates.add(tagPrefix + "[" + attr + "='" + escapeAttributeValue(val) + "']");
                    candidates.add("[" + attr + "='" + escapeAttributeValue(val) + "']");
                }
            }

            // Candidate 4: name
            final String name = element.getAttribute("name");
            if (name != null && !name.isBlank())
            {
                final String tagPrefix = !tagName.isBlank() ? tagName : "";
                candidates.add(tagPrefix + "[name='" + escapeAttributeValue(name) + "']");
            }

            // Candidate 5: aria-label
            final String ariaLabel = element.getAttribute("aria-label");
            if (ariaLabel != null && !ariaLabel.isBlank())
            {
                candidates.add("[aria-label='" + escapeAttributeValue(ariaLabel) + "']");
            }

            // Candidate 6: placeholder
            final String placeholder = element.getAttribute("placeholder");
            if (placeholder != null && !placeholder.isBlank())
            {
                candidates.add("[placeholder='" + escapeAttributeValue(placeholder) + "']");
            }
        }
        catch (final Exception e)
        {
            LOGGER.debug("Failed extracting candidate attributes from element: {}", e.getMessage());
        }

        return candidates;
    }

    /**
     * Attempts to improve a given locator for a target element.
     * Evaluates candidate attributes, validates uniqueness and identity match against the active WebDriver,
     * and returns the upgraded locator if candidate score strictly exceeds the original score.
     *
     * @param driver the active WebDriver instance
     * @param targetElement the resolved target element
     * @param originalLocator the current locator string
     * @return the upgraded locator if valid and higher scoring, or originalLocator otherwise
     */
    public static String improveLocator(final WebDriver driver, final WebElement targetElement, final String originalLocator)
    {
        if (!AiConfiguration.getInstance().isLocatorImproverEnabled())
        {
            return originalLocator;
        }

        if (driver == null || targetElement == null)
        {
            return originalLocator;
        }

        final int originalScore = scoreLocator(originalLocator);
        if (originalScore >= 10)
        {
            // Already at maximum quality score (e.g. #id or data-testid)
            return originalLocator;
        }

        final List<String> candidates = generateCandidates(targetElement);
        for (final String candidate : candidates)
        {
            final int candidateScore = scoreLocator(candidate);
            if (candidateScore <= originalScore)
            {
                continue;
            }

            if (isValidAndIdentical(driver, candidate, targetElement))
            {
                LOGGER.info("💡 [Locator Improver] Upgraded locator from '{}' (score: {}) to '{}' (score: {})",
                    originalLocator, originalScore, candidate, candidateScore);
                return candidate;
            }
        }

        return originalLocator;
    }

    /**
     * Validates that a candidate CSS selector matches exactly 1 element in the DOM
     * and that element is equal to the target element.
     *
     * @param driver the active WebDriver
     * @param candidateCss the candidate CSS selector string
     * @param targetElement the expected target element
     * @return true if candidate matches exactly 1 element and is equal to targetElement, false otherwise
     */
    public static boolean isValidAndIdentical(final WebDriver driver, final String candidateCss, final WebElement targetElement)
    {
        if (driver == null || candidateCss == null || candidateCss.isBlank() || targetElement == null)
        {
            return false;
        }

        try
        {
            final List<WebElement> matches = driver.findElements(By.cssSelector(candidateCss));
            if (matches.size() != 1)
            {
                LOGGER.debug("   [Locator Improver] Candidate '{}' rejected: matches {} elements (expected 1)", candidateCss, matches.size());
                return false;
            }

            final WebElement matched = matches.get(0);
            if (!matched.equals(targetElement))
            {
                LOGGER.debug("   [Locator Improver] Candidate '{}' rejected: matched element does not equal target element", candidateCss);
                return false;
            }

            return true;
        }
        catch (final Exception e)
        {
            LOGGER.debug("   [Locator Improver] Candidate '{}' failed validation check: {}", candidateCss, e.getMessage());
            return false;
        }
    }

    private static String escapeCssIdentifier(final String str)
    {
        if (str == null)
        {
            return "";
        }
        return str.replaceAll("([!\"#$%&'()*+,./:;<=>?@\\[\\]^`{|}~])", "\\\\$1");
    }

    private static String escapeAttributeValue(final String str)
    {
        if (str == null)
        {
            return "";
        }
        return str.replace("\\", "\\\\").replace("'", "\\'");
    }
}
