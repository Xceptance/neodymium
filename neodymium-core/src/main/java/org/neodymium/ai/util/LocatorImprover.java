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
import java.util.Locale;
import java.util.regex.Pattern;
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

    private static final Pattern POSITIONAL_PSEUDO_PATTERN = Pattern.compile(
            ":(nth-child|nth-of-type|nth-last-child|nth-last-of-type|first-child|last-child|first-of-type|last-of-type|only-child|only-of-type)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern XPATH_INDEXED_PATTERN = Pattern.compile("\\[\\d+\\]");

    private static final Pattern PLAYWRIGHT_NTH_PATTERN = Pattern.compile(">>\\s*nth\\s*=", Pattern.CASE_INSENSITIVE);

    private static final Pattern EXACT_TEXT_PSEUDO_PATTERN = Pattern.compile(
            ":(text-is|exact-text|has-text-is)\\(",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PARTIAL_TEXT_PSEUDO_PATTERN = Pattern.compile(
            ":(has-text|contains|text)\\(",
            Pattern.CASE_INSENSITIVE);

    /**
     * Private constructor to prevent instantiation.
     */
    private LocatorImprover()
    {
    }

    /**
     * Strips the contents of single- and double-quoted strings from the selector.
     * Replaces quoted contents with empty quotes ("" or '') to ensure that combinators, colons,
     * or spaces within text arguments (e.g. :text-is("Next > Step: 1")) do not contaminate parsing.
     *
     * @param input the raw selector
     * @return the selector with string literal contents stripped
     */
    public static String stripQuotedStrings(final String input)
    {
        if (input == null || input.isEmpty())
        {
            return "";
        }
        final StringBuilder sb = new StringBuilder(input.length());
        boolean inDouble = false;
        boolean inSingle = false;
        for (int i = 0; i < input.length(); i++)
        {
            final char c = input.charAt(i);
            if (c == '"' && !inSingle)
            {
                inDouble = !inDouble;
                sb.append('"');
            }
            else if (c == '\'' && !inDouble)
            {
                inSingle = !inSingle;
                sb.append('\'');
            }
            else if (!inDouble && !inSingle)
            {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean hasChildCombinatorOutsideBrackets(final String input)
    {
        int bracketDepth = 0;
        int parenDepth = 0;
        for (int i = 0; i < input.length(); i++)
        {
            final char c = input.charAt(i);
            if (c == '[')
            {
                bracketDepth++;
            }
            else if (c == ']')
            {
                bracketDepth = Math.max(0, bracketDepth - 1);
            }
            else if (c == '(')
            {
                parenDepth++;
            }
            else if (c == ')')
            {
                parenDepth = Math.max(0, parenDepth - 1);
            }
            else if (c == '>' && bracketDepth == 0 && parenDepth == 0)
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isExactTextXPath(final String unquoted)
    {
        return (unquoted.startsWith("//") || unquoted.startsWith(".//"))
                && (unquoted.contains("normalize-space()=")
                || unquoted.contains("normalize-space(.)=")
                || unquoted.contains("text()="));
    }

    private static boolean isPartialTextXPath(final String unquoted)
    {
        return (unquoted.startsWith("//") || unquoted.startsWith(".//"))
                && (unquoted.contains("contains(normalize-space(")
                || unquoted.contains("contains(text("));
    }

    private static boolean hasUnrecognizedPseudoClass(final String unquoted)
    {
        int bracketDepth = 0;
        for (int i = 0; i < unquoted.length(); i++)
        {
            final char c = unquoted.charAt(i);
            if (c == '[')
            {
                bracketDepth++;
            }
            else if (c == ']')
            {
                bracketDepth = Math.max(0, bracketDepth - 1);
            }
            else if (c == ':' && bracketDepth == 0)
            {
                final String rest = unquoted.substring(i);
                if (!startsWithRecognizedPseudo(rest))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean startsWithRecognizedPseudo(final String rest)
    {
        final String lower = rest.toLowerCase(Locale.ROOT);
        return lower.startsWith(":text-is(")
                || lower.startsWith(":exact-text(")
                || lower.startsWith(":has-text-is(")
                || lower.startsWith(":has-text(")
                || lower.startsWith(":contains(")
                || lower.startsWith(":text(")
                || lower.startsWith(":has(")
                || lower.startsWith(":not(")
                || lower.startsWith(":checked")
                || lower.startsWith(":disabled")
                || lower.startsWith(":enabled")
                || lower.startsWith(":selected")
                || lower.startsWith(":focus");
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
        final String unquoted = stripQuotedStrings(trimmed);

        // 1. Structural / Positional Fragility Check (Priority Rule: Score 2)
        // Positional pseudo-classes, raw indexed XPath, or Playwright nth= ordinals are always fragile,
        // even if combined with IDs, classes, or text (e.g. #id:nth-child(2) or div:nth-child(2) a:text-is("Tops")).
        if (POSITIONAL_PSEUDO_PATTERN.matcher(unquoted).find()
                || XPATH_INDEXED_PATTERN.matcher(unquoted).find()
                || PLAYWRIGHT_NTH_PATTERN.matcher(unquoted).find()
                || unquoted.startsWith("/html")
                || unquoted.startsWith("/body"))
        {
            return 2;
        }

        // Child combinators (outside Playwright >> chains and :has(...) containers): e.g. header > div > form > input
        final String withoutChains = unquoted.replace(">>", "  ");
        if (hasChildCombinatorOutsideBrackets(withoutChains))
        {
            return 2;
        }

        // 2. Unique ID selector (#my-id or tag#my-id without descendant combinators, pseudo-classes, or multiple tokens)
        if (!unquoted.contains(" ") && !unquoted.contains(">") && !unquoted.contains(":") && !unquoted.contains("[")
                && (unquoted.startsWith("#") || (unquoted.contains("#") && !unquoted.contains("."))))
        {
            final String idVal = unquoted.substring(unquoted.indexOf('#') + 1);
            if (VOLATILE_ID_DETECTOR.isVolatile(idVal))
            {
                return 0;
            }
            return 10;
        }

        // 3. Test ID attributes (data-testid / data-test)
        if (unquoted.contains("[data-testid=") || unquoted.contains("[data-test="))
        {
            return 10;
        }

        // 4. Semantic data attributes (data-country, data-value, data-code, data-lang, data-qa, data-id)
        if (unquoted.contains("[data-country=") || unquoted.contains("[data-value=") || unquoted.contains("[data-code=")
                || unquoted.contains("[data-lang=") || unquoted.contains("[data-qa=") || unquoted.contains("[data-id="))
        {
            return 9;
        }

        // 5. Standard attributes (name, aria-label, placeholder)
        if (unquoted.contains("[name=") || unquoted.contains("[aria-label=") || unquoted.contains("[placeholder="))
        {
            return 8;
        }

        // 6. Neodymium Exact Text Pseudo-Selectors and Semantic XPath Exact Text
        if (EXACT_TEXT_PSEUDO_PATTERN.matcher(unquoted).find() || isExactTextXPath(unquoted))
        {
            return 7;
        }

        // 7. Neodymium Partial Text Pseudo-Selectors, Semantic XPath Substring, Clean Classes, and Clean Playwright Chaining
        if (PARTIAL_TEXT_PSEUDO_PATTERN.matcher(unquoted).find() || isPartialTextXPath(unquoted))
        {
            return 6;
        }

        // Single clean CSS class (.btn-primary)
        if (unquoted.startsWith(".") && !unquoted.contains(" ") && !unquoted.contains(">") && !unquoted.contains(":"))
        {
            return 6;
        }

        // Clean Playwright relational chaining without positional ordinals (container >> child)
        if (unquoted.contains(">>") && !unquoted.contains(">"))
        {
            return 6;
        }

        // 8. Container pseudo-classes (:has, etc.)
        if (unquoted.contains(":has("))
        {
            return 6;
        }

        // 9. Synthetic Neodymium data-ai tag
        if (unquoted.contains("data-ai"))
        {
            return 4;
        }

        // 10. Raw XPath without text predicates (e.g. //div/span/a)
        if (unquoted.startsWith("/"))
        {
            return 2;
        }

        // 11. Remaining pseudo-classes or pseudo-elements not recognized above (e.g. ::before, :root)
        if (hasUnrecognizedPseudoClass(unquoted))
        {
            return 2;
        }

        // 12. Standard fallback (e.g. bare tags 'button', simple compound 'header a')
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
