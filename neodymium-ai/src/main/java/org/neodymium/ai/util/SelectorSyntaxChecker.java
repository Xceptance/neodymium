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

import javax.xml.xpath.XPathFactory;

import org.jsoup.select.QueryParser;
import org.jsoup.select.Selector.SelectorParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fast offline utility for syntax validation and classification of CSS and XPath selectors.
 * <p>
 * Uses jsoup {@link QueryParser} for offline CSS AST validation and JDK {@link XPathFactory}
 * for XPath syntax validation.
 * </p>
 *
 * @author AI-generated: Gemini 3.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class SelectorSyntaxChecker
{
    private static final Logger LOG = LoggerFactory.getLogger(SelectorSyntaxChecker.class);

    /**
     * Enumeration of candidate target locator types.
     */
    public enum SelectorType
    {
        CSS,
        XPATH,
        TEXT
    }

    private SelectorSyntaxChecker()
    {
        // Private constructor for utility class
    }

    /**
     * Determines whether the given candidate string is a structured CSS selector, XPath expression, or plain text.
     *
     * @param candidate the target string candidate to check
     * @return the resolved {@link SelectorType}
     */
    public static SelectorType determineType(final String candidate)
    {
        if (candidate == null || candidate.isBlank())
        {
            return SelectorType.TEXT;
        }

        final String clean = candidate.trim();

        // URLs with scheme (e.g. http://, https://, file://) are plain text values/targets, not locators
        if (clean.contains("://"))
        {
            return SelectorType.TEXT;
        }

        // 1. Explicit Prefixes & Structural Indicators
        if (clean.toLowerCase().startsWith("xpath=") || clean.startsWith("//") || clean.startsWith("./") || clean.startsWith("(/"))
        {
            return SelectorType.XPATH;
        }

        if (clean.toLowerCase().startsWith("css=") || clean.startsWith("[data-ai="))
        {
            return SelectorType.CSS;
        }

        // 2. CSS Syntax Validation via jsoup QueryParser
        if (isCssSelector(clean))
        {
            return SelectorType.CSS;
        }

        // 3. XPath Syntax Validation via JDK XPathFactory
        if (isXpathExpression(clean))
        {
            return SelectorType.XPATH;
        }

        return SelectorType.TEXT;
    }

    /**
     * Checks whether a candidate string is syntactically a valid CSS selector using jsoup {@link QueryParser}.
     *
     * @param candidate the string candidate to check
     * @return true if candidate is a valid CSS selector; false otherwise
     */
    public static boolean isCssSelector(final String candidate)
    {
        if (candidate == null || candidate.isBlank())
        {
            return false;
        }

        final String clean = candidate.trim();

        if (clean.contains("://"))
        {
            return false;
        }

        if (clean.startsWith("css=") || clean.startsWith("[data-ai="))
        {
            return true;
        }

        // Plain text heuristics (sentences, punctuation, prices with currency symbols)
        if (clean.contains(": ") || clean.contains(". ") || clean.endsWith(".")
            || clean.matches(".*\\p{Sc}.*")
            || clean.contains("!") || clean.contains("?"))
        {
            return false;
        }

        // Reject invalid combinator sequences (e.g. "> >", leading/trailing combinators)
        if (clean.matches(".*[>+~]\\s*[>+~].*") || clean.startsWith(">") || clean.startsWith("+") || clean.startsWith("~")
                || clean.endsWith(">") || clean.endsWith("+") || clean.endsWith("~"))
        {
            return false;
        }

        // Must contain structural CSS tokens (#, ., [, >, ~, +, or pseudo-class :[a-z])
        // to avoid single words (like "Submit", "Login") from being parsed as bare tag selectors
        final boolean hasId = clean.contains("#");
        final boolean hasBracket = clean.contains("[") && clean.contains("]");
        final boolean hasCombinator = clean.contains(">") || clean.contains("~") || clean.contains("+");
        final boolean hasClassDot = clean.matches(".*(^|[\\s>+~])\\.[a-zA-Z_][a-zA-Z0-9_\\-]*.*");
        final boolean hasPseudoClass = clean.matches(".*:[a-zA-Z\\-]+.*");

        if (!hasId && !hasBracket && !hasCombinator && !hasClassDot && !hasPseudoClass)
        {
            return false;
        }

        String toValidate = clean;
        if (toValidate.toLowerCase().startsWith("css="))
        {
            toValidate = toValidate.substring(4).trim();
        }

        // Normalize browser UI state pseudo-classes/elements not modeled in jsoup DOM
        final String normalized = toValidate.replaceAll(":(hover|focus|focus-visible|focus-within|active|visited|target|disabled|enabled|checked|selected|indeterminate|before|after)\\b", "").trim();
        if (normalized.isEmpty() || "*".equals(normalized))
        {
            return true;
        }

        try
        {
            QueryParser.parse(normalized);
            return true;
        }
        catch (final SelectorParseException e)
        {
            LOG.trace("Candidate '{}' failed jsoup QueryParser check: {}", clean, e.getMessage());
            return false;
        }
        catch (final Exception e)
        {
            LOG.trace("Candidate '{}' caused unexpected error during CSS parse: {}", clean, e.getMessage());
            return false;
        }
    }

    /**
     * Checks whether a candidate string is syntactically a valid XPath expression using JDK {@link XPathFactory}.
     *
     * @param candidate the string candidate to check
     * @return true if candidate is a valid XPath expression; false otherwise
     */
    public static boolean isXpathExpression(final String candidate)
    {
        if (candidate == null || candidate.isBlank())
        {
            return false;
        }

        final String clean = candidate.trim();

        if (clean.contains("://"))
        {
            return false;
        }

        final boolean hasXpathTokens = clean.startsWith("/") || clean.startsWith("./") || clean.startsWith("(/")
                || clean.contains("//") || clean.contains("@") || clean.contains("text()") || clean.contains("contains(");

        if (!hasXpathTokens)
        {
            return false;
        }

        try
        {
            String xpathToCompile = clean;
            if (xpathToCompile.toLowerCase().startsWith("xpath="))
            {
                xpathToCompile = xpathToCompile.substring(6).trim();
            }
            XPathFactory.newInstance().newXPath().compile(xpathToCompile);
            return true;
        }
        catch (final Exception e)
        {
            LOG.trace("Candidate '{}' failed XPathFactory check: {}", clean, e.getMessage());
            return false;
        }
    }
}
