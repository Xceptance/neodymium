/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.util;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;

import java.util.List;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class DebugUtilsTest
{
    @Before
    public void setUp()
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.headless.withoutVideo", "true");
    }

    @Test
    public void testHighlighting()
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.duration", "1000");

        Selenide.open("https://blog.xceptance.com/");
        DebugUtils.injectJavaScript();
        assertJsSuccessfullyInjected();

        final List<WebElement> list = $("body").findElements(By.cssSelector("header"));
        DebugUtils.highlightElements(list, Neodymium.getDriver());
        $(".neodymium-highlight-box").shouldBe(visible);

        DebugUtils.resetAllHighlight();
        $(".neodymium-highlight-box").shouldNot(exist);

        final List<WebElement> list2 = $("body").findElements(By.cssSelector("article"));
        DebugUtils.highlightElements(list2, Neodymium.getDriver());
        $$(".neodymium-highlight-box").shouldHave(sizeGreaterThan(0));

        DebugUtils.resetAllHighlight();
        $(".neodymium-highlight-box").shouldNot(exist);
    }

    @Test
    public void testHighlightingWithoutImplicitWaitTime()
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.duration", "500");

        Selenide.open("https://blog.xceptance.com/");
        DebugUtils.injectJavaScript();
        assertJsSuccessfullyInjected();

        final List<WebElement> list = $("body").findElements(By.cssSelector("header"));
        DebugUtils.highlightElements(list, Neodymium.getDriver());
        $(".neodymium-highlight-box").shouldBe(visible);

        DebugUtils.resetAllHighlight();
        $(".neodymium-highlight-box").shouldNot(exist);
    }

    @Test
    public void testWaiting()
    {
        NeodymiumWebDriverTestListener eventListener = new NeodymiumWebDriverTestListener();
        RemoteWebDriver driver = Neodymium.getRemoteWebDriver();
        WebDriver decoratedDriver = new EventFiringDecorator<WebDriver>(eventListener).decorate(driver);
        Neodymium.getWebDriverStateContainer().setDecoratedWebDriver(decoratedDriver);
        WebDriverRunner.setWebDriver(decoratedDriver);
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");

        // no wait due to navigation
        Selenide.open("https://blog.xceptance.com/");
        Assert.assertEquals(0, eventListener.implicitWaitCount);

        // one wait due to find
        $("body header").should(exist);
        Assert.assertEquals(1, eventListener.implicitWaitCount);
        assertJsSuccessfullyInjected();

        // two waits due to chain finding
        $("body").findElements(By.cssSelector("article"));
        Assert.assertEquals(3, eventListener.implicitWaitCount);

        // on wait due to find and click
        $("article a").scrollIntoView(true);
        Assert.assertEquals(4, eventListener.implicitWaitCount);

        $("article a").click();
        Assert.assertEquals(5, eventListener.implicitWaitCount);

        // additional one wait due to find and click
        $("#btn-open-search").click();
        Assert.assertEquals(6, eventListener.implicitWaitCount);

        // one wait due to find and change value
        $("#search-input-field").val("abc");
        Assert.assertEquals(7, eventListener.implicitWaitCount);

        // one wait due to find and press enter
        $("#search-input-field").pressEnter();
        Assert.assertEquals(8, eventListener.implicitWaitCount);
    }

    @Test
    public void testIFrames() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.duration", "750");

        Selenide.open("https://www.w3schools.com/tags/tryit.asp?filename=tryhtml_select");

        // check if the cookie banner is present and accept it if so
        SelenideElement acceptCookiesButtonFrame = $("#fast-cmp-iframe");

        SelenideAddons.optionalWaitUntilCondition(acceptCookiesButtonFrame, visible);
        if (acceptCookiesButtonFrame.isDisplayed())
        {
            Neodymium.getDriver().switchTo().frame(acceptCookiesButtonFrame);
            $(".fast-cmp-button-primary").click();
            Neodymium.getDriver().switchTo().defaultContent();
        }

        Neodymium.getDriver().switchTo().frame("iframeResult");
        SelenideElement body = $("body");
        body.click();
        assertJsSuccessfullyInjected();

        final List<WebElement> list = $("body").findElements(By.cssSelector("select"));

        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "false");
        DebugUtils.highlightElements(list, Neodymium.getDriver());
        $(".neodymium-highlight-box").shouldBe(visible);

        DebugUtils.resetAllHighlight();
        $(".neodymium-highlight-box").shouldNot(exist);
    }

    private void assertJsSuccessfullyInjected()
    {
        Assert.assertTrue(Selenide.executeJavaScript("return !!window.NEODYMIUM"));
    }
}
