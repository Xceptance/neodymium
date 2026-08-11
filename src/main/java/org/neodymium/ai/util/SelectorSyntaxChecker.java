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

import java.util.regex.Pattern;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for syntax classification of target locator candidates into CSS, XPath, or Plain Text.
 * <p>
 * Combines structural CSS pattern analysis and JDK {@link XPathFactory} (for XPath syntax validation)
 * to prevent structured CSS/XPath locators from falling back to literal text content searching in element finders.
 * </p>
 *
 * @author AI-generated: Gemini 3.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class SelectorSyntaxChecker
{
    private static final Logger LOG = LoggerFactory.getLogger(SelectorSyntaxChecker.class);

    private static final Pattern STRUCTURAL_CSS_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\*\n\r\t ]*([#\\.\\[\\:>]).*$");
    private static final Pattern PSEUDO_CLASS_PATTERN = Pattern.compile(":[a-zA-Z\\-]+\\b");

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

        // 1. Explicit Prefixes & Structural Indicators
        if (clean.toLowerCase().startsWith("xpath=") || clean.startsWith("//") || clean.startsWith("./") || clean.startsWith("(/"))
        {
            return SelectorType.XPATH;
        }

        if (clean.toLowerCase().startsWith("css=") || clean.startsWith("[data-ai="))
        {
            return SelectorType.CSS;
        }

        // 2. CSS Syntax Validation via structural indicators
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
     * Checks whether a candidate string is syntactically a CSS selector using structural checks.
     *
     * @param candidate the cleaned string candidate
     * @return true if candidate is a CSS selector; false otherwise
     */
    public static boolean isCssSelector(final String candidate)
    {
        if (candidate == null || candidate.isBlank())
        {
            return false;
        }

        final String clean = candidate.trim();

        // Explicit CSS indicators (ID, class, attribute, prefix)
        if (clean.startsWith("#") || clean.startsWith(".") || clean.startsWith("[") || clean.startsWith("css=") || clean.startsWith("[data-ai="))
        {
            return true;
        }

        // Plain text with colon punctuation (e.g., "Search Results for: neodymium")
        if (clean.contains(": ") && !clean.startsWith(":") && !clean.contains(">") && !clean.contains("#") && !clean.contains("."))
        {
            return false;
        }

        // Must contain structural CSS tokens (#, ., [, >, or pseudo-class :[a-z])
        if (!clean.contains("#") && !clean.contains(".") && !clean.contains("[") && !clean.contains(">"))
        {
            final boolean hasPseudoClass = PSEUDO_CLASS_PATTERN.matcher(clean).find() && !clean.contains(": ");
            if (!hasPseudoClass)
            {
                return false;
            }
        }

        return STRUCTURAL_CSS_PATTERN.matcher(clean).matches();
    }

    /**
     * Checks whether a candidate string is syntactically a valid XPath expression using JDK XPathFactory.
     *
     * @param candidate the cleaned string candidate
     * @return true if candidate is a valid XPath expression; false otherwise
     */
    public static boolean isXpathExpression(final String candidate)
    {
        if (candidate == null || candidate.isBlank())
        {
            return false;
        }

        final String clean = candidate.trim();

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
            LOG.trace("Candidate '{}' failed XPathFactory syntax compile check: {}", clean, e.getMessage());
            return false;
        }
    }
}
