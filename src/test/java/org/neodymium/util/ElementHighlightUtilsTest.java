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
package org.neodymium.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit4.NeodymiumRunner;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.codeborne.selenide.Selenide;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

/**
 * Unit and integration tests for {@link ElementHighlightUtils}.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@RunWith(NeodymiumRunner.class)
@Browser("Chrome_1500x1000_headless")
public class ElementHighlightUtilsTest
{
    @Test
    public void testAbstractJavaScriptExecutor()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);
        final ElementHighlightUtils.JavaScriptExecutor customExecutor = (final String script, final Object... args) ->
        {
            executed.set(true);
            return "SUCCESS";
        };

        ElementHighlightUtils.injectJavaScript(customExecutor);
        Assert.assertTrue("Custom JavaScriptExecutor should have been invoked", executed.get());
    }

    @Test
    public void testHighlightingWithSelenide()
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.duration", "1000");

        Selenide.open("https://blog.xceptance.com/");
        $(".site-title").shouldBe(visible);

        ElementHighlightUtils.injectHighlightingJs();
        Assert.assertTrue(Boolean.TRUE.equals(Selenide.executeJavaScript("return !!window.NEODYMIUM")));

        final List<WebElement> elements = Neodymium.getDriver().findElements(By.cssSelector(".site-title"));
        Assert.assertFalse("Element list should not be empty", elements.isEmpty());

        ElementHighlightUtils.highlightElements(elements, Selenide::executeJavaScript, 1000, 2);
        $(".neodymium-highlight-box").shouldBe(visible);

        ElementHighlightUtils.resetAllHighlight(Selenide::executeJavaScript);
        $(".neodymium-highlight-box").shouldNot(exist);
    }
}
