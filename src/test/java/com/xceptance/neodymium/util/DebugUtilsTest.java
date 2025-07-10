package com.xceptance.neodymium.util;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;

import java.util.List;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class DebugUtilsTest
{
    @Test
    public void testHighlighting()
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.duration", "1000");

        Selenide.open("https://blog.xceptance.com/");
        DebugUtils.injectJavaScript();
        assertJsSuccessfullyInjected();

        final List<WebElement> list = $("body").findElements(By.cssSelector("#masthead"));
        DebugUtils.highlightElements(list, Neodymium.getDriver());
        $(".neodymium-highlight-box").shouldBe(visible);

        DebugUtils.resetAllHighlight();
        $(".neodymium-highlight-box").shouldNot(exist);

        final List<WebElement> list2 = $("body").findElements(By.cssSelector("#content article"));
        DebugUtils.highlightElements(list2, Neodymium.getDriver());
        $$(".neodymium-highlight-box").shouldHave(size(10));

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

        final List<WebElement> list = $("body").findElements(By.cssSelector("#masthead"));
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
        $("body #masthead").should(exist);
        Assert.assertEquals(1, eventListener.implicitWaitCount);
        assertJsSuccessfullyInjected();

        // two waits due to chain finding
        $("body").findElements(By.cssSelector("#content article"));
        Assert.assertEquals(3, eventListener.implicitWaitCount);

        // on wait due to find and click
        $("#content article h1").click();
        Assert.assertEquals(4, eventListener.implicitWaitCount);

        // additional one wait due to find and click
        $("#masthead .search-toggle").click();
        Assert.assertEquals(5, eventListener.implicitWaitCount);

        // one wait due to find and change value
        $("#search-container .search-form input.search-field").val("abc");
        Assert.assertEquals(6, eventListener.implicitWaitCount);

        // one wait due to find and press enter
        $("#search-container .search-form input.search-field").pressEnter();
        Assert.assertEquals(7, eventListener.implicitWaitCount);
    }

    @Test
    public void testIFrames() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.duration", "750");

        Selenide.open("https://www.w3schools.com/tags/tryit.asp?filename=tryhtml_select");

        // check if the cookie banner is present and accept it if so
        SelenideElement acceptCookiesButton = $("#snigel-cmp-framework #accept-choices");
        
        SelenideAddons.optionalWaitUntilCondition(acceptCookiesButton, visible);
        if (acceptCookiesButton.isDisplayed())
        {
            acceptCookiesButton.click();
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
