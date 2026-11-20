/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.util;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit4.NeodymiumRunner;
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
@Browser("Chrome_1500x1000_headless")
public class DebugUtilsTest
{
    private static final String TEST_PAGE_URL = "data:text/html;charset=utf-8,"
        + "<!DOCTYPE html><html><head><title>DebugUtils Test</title></head><body>"
        + "<div id='masthead'>"
        + "  <h1 class='site-title'>Neodymium Test Page</h1>"
        + "  <button class='search-toggle' onclick=\"document.getElementById('search-container').style.display='block'\">Search</button>"
        + "</div>"
        + "<div id='content'>"
        + "  <article>"
        + "    <h1 onclick=\"this.innerText='Clicked'\">Article Heading</h1>"
        + "    <p>Sample content</p>"
        + "  </article>"
        + "</div>"
        + "<div id='search-container'>"
        + "  <form class='search-form' onsubmit='return false;'>"
        + "    <input class='search-field' type='text' />"
        + "  </form>"
        + "</div>"
        + "</body></html>";

    private static final String IFRAME_TEST_PAGE_URL = "data:text/html;charset=utf-8,"
        + "<!DOCTYPE html><html><head><title>iFrame Test</title></head><body>"
        + "<iframe id='iframeResult' srcdoc='<!DOCTYPE html><html><body><select><option>Volvo</option><option>Saab</option></select></body></html>'></iframe>"
        + "</body></html>";

    @Before
    public void setUp()
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.headless.withoutVideo", "true");
    }

    @Test
    public void testHighlighting() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.duration", "1000");

        Selenide.open(TEST_PAGE_URL);
        $(".site-title").shouldBe(visible);
        DebugUtils.injectJavaScript();
        assertJsSuccessfullyInjected();

        final List<WebElement> list = Neodymium.getDriver().findElements(By.cssSelector(".site-title"));
        DebugUtils.highlightElements(list, Neodymium.getDriver());
        $(".neodymium-highlight-box").shouldBe(visible);

        DebugUtils.resetAllHighlight();
        $(".neodymium-highlight-box").shouldNot(exist);

        final List<WebElement> list2 = Neodymium.getDriver().findElements(By.cssSelector(".site-title"));
        DebugUtils.highlightElements(list2, Neodymium.getDriver());
        $$(".neodymium-highlight-box").shouldHave(sizeGreaterThan(0));

        DebugUtils.resetAllHighlight();
        $(".neodymium-highlight-box").shouldNot(exist);
    }

    @Test
    public void testHighlightingWithoutImplicitWaitTime() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.duration", "1000");

        Selenide.open(TEST_PAGE_URL);
        $(".site-title").shouldBe(visible);
        DebugUtils.injectJavaScript();
        assertJsSuccessfullyInjected();

        final List<WebElement> list = Neodymium.getDriver().findElements(By.cssSelector(".site-title"));
        DebugUtils.highlightElements(list, Neodymium.getDriver());
        $(".neodymium-highlight-box").shouldBe(visible);

        DebugUtils.resetAllHighlight();
        $(".neodymium-highlight-box").shouldNot(exist);
    }

    @Test
    public void testWaiting()
    {
        final NeodymiumWebDriverTestListener eventListener = new NeodymiumWebDriverTestListener();
        final RemoteWebDriver driver = Neodymium.getRemoteWebDriver();
        final WebDriver decoratedDriver = new EventFiringDecorator<WebDriver>(eventListener).decorate(driver);
        Neodymium.getWebDriverStateContainer().setDecoratedWebDriver(decoratedDriver);
        WebDriverRunner.setWebDriver(decoratedDriver);
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");

        // no wait due to navigation
        Selenide.open(TEST_PAGE_URL);
        Assert.assertEquals(0, eventListener.implicitWaitCount);

        // one wait due to find
        $("body #masthead").should(exist);
        Assert.assertEquals(1, eventListener.implicitWaitCount);
        assertJsSuccessfullyInjected();

        // two waits due to chain finding
        $("body").findElements(By.cssSelector("#content article"));
        Assert.assertEquals(3, eventListener.implicitWaitCount);

        // on wait due to find and click
        $("#content article h1").scrollIntoView(true);
        Assert.assertEquals(4, eventListener.implicitWaitCount);

        $("#content article h1").click();
        Assert.assertEquals(5, eventListener.implicitWaitCount);

        // additional one wait due to find and click
        $("#masthead .search-toggle").click();
        Assert.assertEquals(6, eventListener.implicitWaitCount);

        // one wait due to find and change value
        $("#search-container .search-form input.search-field").val("abc");
        Assert.assertEquals(7, eventListener.implicitWaitCount);

        // one wait due to find and press enter
        $("#search-container .search-form input.search-field").pressEnter();
        Assert.assertEquals(8, eventListener.implicitWaitCount);
    }

    @Test
    public void testIFrames() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.duration", "750");

        Selenide.open(IFRAME_TEST_PAGE_URL);

        Neodymium.getDriver().switchTo().frame("iframeResult");
        final SelenideElement body = $("body");
        body.click();
        assertJsSuccessfullyInjected();

        final List<WebElement> list = $("body").findElements(By.cssSelector("select"));

        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "false");
        DebugUtils.highlightElements(list, Neodymium.getDriver());
        $(".neodymium-highlight-box").shouldBe(visible);

        DebugUtils.resetAllHighlight();
        $(".neodymium-highlight-box").shouldNot(exist);
    }

    @Test
    public void testHighlightingWithBlinkCount() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.duration", "150");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.blink.count", "2");

        Selenide.open(TEST_PAGE_URL);
        $(".site-title").shouldBe(visible);
        DebugUtils.injectJavaScript();
        assertJsSuccessfullyInjected();

        final List<WebElement> list = Neodymium.getDriver().findElements(By.cssSelector(".site-title"));
        DebugUtils.highlightAllElements(list, Neodymium.getDriver());
        $(".neodymium-highlight-box").shouldNot(exist);
    }

    private void assertJsSuccessfullyInjected()
    {
        Assert.assertTrue(Selenide.executeJavaScript("return !!window.NEODYMIUM"));
    }
}
