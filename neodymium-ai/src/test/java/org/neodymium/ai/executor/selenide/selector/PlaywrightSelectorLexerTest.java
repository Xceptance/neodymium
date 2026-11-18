/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package org.neodymium.ai.executor.selenide.selector;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PlaywrightSelectorLexer}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class PlaywrightSelectorLexerTest
{
    @Test
    public void testSplitSimpleClauses()
    {
        final List<String> clauses = PlaywrightSelectorLexer.splitClauses("header >> a");
        Assertions.assertEquals(List.of("header", "a"), clauses);
    }

    @Test
    public void testSplitClausesWithCapture()
    {
        final List<String> clauses = PlaywrightSelectorLexer.splitClauses("*css=article >> text=Hello");
        Assertions.assertEquals(List.of("*css=article", "text=Hello"), clauses);
    }

    @Test
    public void testSplitClausesIgnoresArrowInsideDoubleQuotes()
    {
        final List<String> clauses = PlaywrightSelectorLexer.splitClauses("text=\"A >> B\"");
        Assertions.assertEquals(List.of("text=\"A >> B\""), clauses);
    }

    @Test
    public void testSplitClausesIgnoresArrowInsideSingleQuotes()
    {
        final List<String> clauses = PlaywrightSelectorLexer.splitClauses("button:has-text('Submit >> Next')");
        Assertions.assertEquals(List.of("button:has-text('Submit >> Next')"), clauses);
    }

    @Test
    public void testSplitClausesIgnoresArrowInsideBrackets()
    {
        final List<String> clauses = PlaywrightSelectorLexer.splitClauses("div[data-flow=\"step1 >> step2\"] >> span");
        Assertions.assertEquals(List.of("div[data-flow=\"step1 >> step2\"]", "span"), clauses);
    }

    @Test
    public void testSplitClausesIgnoresArrowInsideParentheses()
    {
        final List<String> clauses = PlaywrightSelectorLexer.splitClauses("div:has(span >> b) >> p");
        Assertions.assertEquals(List.of("div:has(span >> b)", "p"), clauses);
    }

    @Test
    public void testSplitClausesIgnoresArrowInsideRegex()
    {
        final List<String> clauses = PlaywrightSelectorLexer.splitClauses("text=/\\d+ >> \\w+/ >> button");
        Assertions.assertEquals(List.of("text=/\\d+ >> \\w+/", "button"), clauses);
    }

    @Test
    public void testTokenizeCssWithAttributeAndPseudo()
    {
        final String selector = "header.top-nav-bar a[href=\"index.html\"]:contains(\"VÉRLA\")";
        final List<PlaywrightSelectorLexer.Token> tokens = PlaywrightSelectorLexer.tokenize(selector);

        Assertions.assertFalse(tokens.isEmpty());
        // Verify tokens preserve quotes and don't treat .html as a class
        boolean sawIndexHtmlAttr = false;
        boolean sawHtmlClass = false;
        for (final PlaywrightSelectorLexer.Token token : tokens)
        {
            if (token.type() == PlaywrightSelectorLexer.TokenType.STRING && "index.html".equals(token.value()))
            {
                sawIndexHtmlAttr = true;
            }
            if (token.type() == PlaywrightSelectorLexer.TokenType.CLASS && "html".equals(token.value()))
            {
                sawHtmlClass = true;
            }
        }
        Assertions.assertTrue(sawIndexHtmlAttr, "Should have found 'index.html' string token");
        Assertions.assertFalse(sawHtmlClass, "Should NOT have found 'html' as a class token");
    }

    @Test
    public void testTokenizeHashInAttributeIsNotId()
    {
        final String selector = "a[href=\"#top\"]";
        final List<PlaywrightSelectorLexer.Token> tokens = PlaywrightSelectorLexer.tokenize(selector);

        boolean sawTopId = false;
        for (final PlaywrightSelectorLexer.Token token : tokens)
        {
            if (token.type() == PlaywrightSelectorLexer.TokenType.HASH && "top".equals(token.value()))
            {
                sawTopId = true;
            }
        }
        Assertions.assertFalse(sawTopId, "Should NOT parse #top inside attribute as an ID token");
    }
}
