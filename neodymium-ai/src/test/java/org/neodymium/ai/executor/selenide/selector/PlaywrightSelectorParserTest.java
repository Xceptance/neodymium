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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.InvalidSelectorException;

/**
 * Unit tests for {@link PlaywrightSelectorParser}, covering the 13 defect symptoms of DEF-20261013-1642.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class PlaywrightSelectorParserTest
{
    @Test
    public void testVerlaLogoSelectorDoesNotBleedHtmlClass()
    {
        final String raw = "header.top-nav-bar a[href=\"index.html\"]:contains(\"VÉRLA\")";
        final SelectorChain chain = PlaywrightSelectorParser.parse(raw);

        Assertions.assertEquals(1, chain.getSteps().size());
        final SelectorStep step = chain.getSteps().get(0);
        Assertions.assertEquals(SelectorEngine.CSS, step.getEngine());

        final CssAst.Root css = (CssAst.Root) step.getParsed();
        Assertions.assertEquals(1, css.getSelectors().size());

        final CssAst.ComplexSelector complex = css.getSelectors().get(0);
        Assertions.assertEquals(2, complex.getCompounds().size());

        final CssAst.CompoundSelector anchor = complex.getCompounds().get(1);
        Assertions.assertEquals("a", anchor.getTagName());

        boolean hasHrefAttr = false;
        boolean hasHtmlClass = false;
        boolean hasContainsPseudo = false;

        for (final CssAst.SimpleSelector simple : anchor.getSimpleSelectors())
        {
            if (simple instanceof CssAst.AttributeSelector attr)
            {
                if ("href".equals(attr.getName()) && "index.html".equals(attr.getValue()))
                {
                    hasHrefAttr = true;
                }
            }
            if (simple instanceof CssAst.ClassSelector cls && "html".equals(cls.getClassName()))
            {
                hasHtmlClass = true;
            }
            if (simple instanceof CssAst.PseudoClassSelector pseudo && "contains".equals(pseudo.getName()))
            {
                hasContainsPseudo = true;
            }
        }

        Assertions.assertTrue(hasHrefAttr, "Attribute [href='index.html'] must be parsed");
        Assertions.assertTrue(hasContainsPseudo, "Pseudo :contains('VÉRLA') must be parsed");
        Assertions.assertFalse(hasHtmlClass, "Bogus .html class condition must NOT exist");
    }

    @Test
    public void testHashInAttributeIsNotParsedAsId()
    {
        final SelectorChain chain = PlaywrightSelectorParser.parse("a[href=\"#top\"]");
        final SelectorStep step = chain.getSteps().get(0);
        final CssAst.Root css = (CssAst.Root) step.getParsed();
        final CssAst.CompoundSelector compound = css.getSelectors().get(0).getCompounds().get(0);

        boolean hasIdSelector = false;
        boolean hasHrefAttr = false;

        for (final CssAst.SimpleSelector simple : compound.getSimpleSelectors())
        {
            if (simple instanceof CssAst.IdSelector)
            {
                hasIdSelector = true;
            }
            if (simple instanceof CssAst.AttributeSelector attr && "href".equals(attr.getName()) && "#top".equals(attr.getValue()))
            {
                hasHrefAttr = true;
            }
        }

        Assertions.assertTrue(hasHrefAttr, "href='#top' must be an attribute selector");
        Assertions.assertFalse(hasIdSelector, "href='#top' must NOT generate an IdSelector");
    }

    @Test
    public void testChainSeparatorInsideQuotesIsLiteral()
    {
        final SelectorChain chain = PlaywrightSelectorParser.parse("text=\"A >> B\"");
        Assertions.assertEquals(1, chain.getSteps().size());
        final SelectorStep step = chain.getSteps().get(0);
        Assertions.assertEquals(SelectorEngine.TEXT, step.getEngine());
        Assertions.assertEquals("\"A >> B\"", step.getBody());
    }

    @Test
    public void testAutomationIdInsideQuotesIsLiteral()
    {
        final SelectorChain chain = PlaywrightSelectorParser.parse("text=\"Open #xcode\"");
        Assertions.assertEquals(1, chain.getSteps().size());
        final SelectorStep step = chain.getSteps().get(0);
        Assertions.assertEquals(SelectorEngine.TEXT, step.getEngine());
        Assertions.assertEquals("\"Open #xcode\"", step.getBody());
    }

    @Test
    public void testHiddenInsideAttributeIsNotVisibilityFilter()
    {
        final SelectorChain chain = PlaywrightSelectorParser.parse("[title=\"mode:hidden\"]");
        final SelectorStep step = chain.getSteps().get(0);
        Assertions.assertEquals(SelectorEngine.CSS, step.getEngine());
        final CssAst.Root css = (CssAst.Root) step.getParsed();
        final CssAst.CompoundSelector compound = css.getSelectors().get(0).getCompounds().get(0);

        boolean hasTitleAttr = false;
        boolean hasPseudo = false;
        for (final CssAst.SimpleSelector s : compound.getSimpleSelectors())
        {
            if (s instanceof CssAst.AttributeSelector attr && "title".equals(attr.getName()) && "mode:hidden".equals(attr.getValue()))
            {
                hasTitleAttr = true;
            }
            if (s instanceof CssAst.PseudoClassSelector)
            {
                hasPseudo = true;
            }
        }

        Assertions.assertTrue(hasTitleAttr);
        Assertions.assertFalse(hasPseudo);
    }

    @Test
    public void testAttributeOperatorsParsedCorrectly()
    {
        final SelectorChain chain = PlaywrightSelectorParser.parse("a[href$='.pdf'][title*='Report'][data-rel~='external']");
        final CssAst.Root css = (CssAst.Root) chain.getSteps().get(0).getParsed();
        final CssAst.CompoundSelector compound = css.getSelectors().get(0).getCompounds().get(0);

        Assertions.assertEquals(3, compound.getSimpleSelectors().size());
        final CssAst.AttributeSelector a1 = (CssAst.AttributeSelector) compound.getSimpleSelectors().get(0);
        final CssAst.AttributeSelector a2 = (CssAst.AttributeSelector) compound.getSimpleSelectors().get(1);
        final CssAst.AttributeSelector a3 = (CssAst.AttributeSelector) compound.getSimpleSelectors().get(2);

        Assertions.assertEquals("$=", a1.getOperator());
        Assertions.assertEquals(".pdf", a1.getValue());
        Assertions.assertEquals("*=", a2.getOperator());
        Assertions.assertEquals("Report", a2.getValue());
        Assertions.assertEquals("~=", a3.getOperator());
        Assertions.assertEquals("external", a3.getValue());
    }

    @Test
    public void testUnknownPseudoClassThrowsActionableException()
    {
        final InvalidSelectorException ex = Assertions.assertThrows(
            InvalidSelectorException.class,
            () -> PlaywrightSelectorParser.parse("button:frobnicate")
        );
        Assertions.assertTrue(ex.getMessage().contains(":frobnicate"), "Error message should mention the unknown token");
    }

    @Test
    public void testUnclosedBracketThrowsException()
    {
        Assertions.assertThrows(
            InvalidSelectorException.class,
            () -> PlaywrightSelectorParser.parse("div[data-x=\"1\"")
        );
    }

    @Test
    public void testEnterFrameControlThrowsActionableException()
    {
        final InvalidSelectorException ex = Assertions.assertThrows(
            InvalidSelectorException.class,
            () -> PlaywrightSelectorParser.parse("internal:control=enter-frame")
        );
        Assertions.assertTrue(ex.getMessage().contains("enter-frame"), "Error should explain frame switching requirement");
    }

    @Test
    public void testCaptureIntermediateStep()
    {
        final SelectorChain chain = PlaywrightSelectorParser.parse("*css=article >> text=Hello");
        Assertions.assertEquals(2, chain.getSteps().size());
        Assertions.assertTrue(chain.hasCapture());
        Assertions.assertEquals(0, chain.getCapturedIndex());
        Assertions.assertTrue(chain.getSteps().get(0).isCaptured());
        Assertions.assertFalse(chain.getSteps().get(1).isCaptured());
    }

    @Test
    public void testLegacyXPathShorthands()
    {
        final SelectorChain chain1 = PlaywrightSelectorParser.parse("//button[@type='submit']");
        Assertions.assertEquals(SelectorEngine.XPATH, chain1.getSteps().get(0).getEngine());

        final SelectorChain chain2 = PlaywrightSelectorParser.parse("(//input)[1]");
        Assertions.assertEquals(SelectorEngine.XPATH, chain2.getSteps().get(0).getEngine());

        final SelectorChain chain3 = PlaywrightSelectorParser.parse("../div");
        Assertions.assertEquals(SelectorEngine.XPATH, chain3.getSteps().get(0).getEngine());
    }

    @Test
    public void testAutomationIdShorthandInCss()
    {
        final SelectorChain chain = PlaywrightSelectorParser.parse("div > #xc4o48u");
        final CssAst.Root css = (CssAst.Root) chain.getSteps().get(0).getParsed();
        final CssAst.CompoundSelector second = css.getSelectors().get(0).getCompounds().get(1);

        Assertions.assertEquals(1, second.getSimpleSelectors().size());
        final CssAst.IdSelector idSel = (CssAst.IdSelector) second.getSimpleSelectors().get(0);
        Assertions.assertEquals("xc4o48u", idSel.getId());
        Assertions.assertTrue(idSel.isAutomationId());
    }
}
