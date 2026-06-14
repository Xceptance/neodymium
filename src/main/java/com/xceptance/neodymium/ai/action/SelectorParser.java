/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xceptance.neodymium.ai.action;

/**
 * Utility class to parse and validate selector targets in direct command shortcuts.
 * Supports css= and xpath= prefixes, defaulting to CSS.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class SelectorParser
{
    public enum SelectorType
    {
        CSS,
        XPATH
    }

    public static final class ParsedSelector
    {
        private final SelectorType type;
        private final String expression;

        public ParsedSelector(final SelectorType type, final String expression)
        {
            this.type = type;
            this.expression = expression;
        }

        public final SelectorType getType()
        {
            return this.type;
        }

        public final String getExpression()
        {
            return this.expression;
        }

        @Override
        public String toString()
        {
            return this.type + "=" + this.expression;
        }
    }

    private SelectorParser()
    {
        // Prevent instantiation
    }

    /**
     * Parses a raw selector string.
     *
     * @param rawSelector the raw selector target (e.g. "xpath=//div", "css=#btn", "#btn")
     * @return the ParsedSelector containing the type and the cleaned expression
     */
    public static ParsedSelector parse(final String rawSelector)
    {
        if (rawSelector == null || rawSelector.isBlank())
        {
            throw new IllegalArgumentException("Selector cannot be null or empty");
        }
        final String trimmed = rawSelector.strip();
        if (trimmed.toLowerCase().startsWith("xpath="))
        {
            String expr = trimmed.substring(6).strip();
            if (expr.isEmpty())
            {
                throw new IllegalArgumentException("XPath selector expression cannot be empty");
            }
            if (!expr.startsWith("/") && !expr.startsWith("("))
            {
                expr = "(" + expr + ")";
            }
            return new ParsedSelector(SelectorType.XPATH, expr);
        }
        else if (trimmed.toLowerCase().startsWith("css="))
        {
            final String expr = trimmed.substring(4).strip();
            if (expr.isEmpty())
            {
                throw new IllegalArgumentException("CSS selector expression cannot be empty");
            }
            return new ParsedSelector(SelectorType.CSS, expr);
        }
        else
        {
            return new ParsedSelector(SelectorType.CSS, trimmed);
        }
    }
}
