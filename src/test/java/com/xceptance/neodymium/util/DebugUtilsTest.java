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

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class DebugUtilsTest {
    @Test
    public void testHighlighting() throws Exception {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.duration", "1000");

        Selenide.open("https://blog.xceptance.com/");
        $(".site-title").shouldBe(visible);
        DebugUtils.injectJavaScript();
        assertJsSuccessfullyInjected();

        final List<WebElement> list = Neodymium.getDriver().findElements(By.cssSelector(".site-title"));
        System.out.println("=== DIAGNOSTIC START ===");
        System.out.println("Element list size: " + list.size());
        if (!list.isEmpty())
        {
            final WebElement el = list.get(0);
            System.out.println("TagName: " + el.getTagName());
            System.out.println("Displayed (Java): " + el.isDisplayed());
            System.out.println("Location: " + el.getLocation());
            System.out.println("Size: " + el.getSize());
            System.out.println("JS getComputedStyle visibility: " + Selenide.executeJavaScript("return window.getComputedStyle(arguments[0]).visibility;", el));
            System.out.println("JS getComputedStyle display: " + Selenide.executeJavaScript("return window.getComputedStyle(arguments[0]).display;", el));
            System.out.println("JS getBoundingClientRect: " + Selenide.executeJavaScript("var r = arguments[0].getBoundingClientRect(); return r.width + 'x' + r.height;", el));
            System.out.println("JS window.NEODYMIUM.isDisplayed: " + Selenide.executeJavaScript("return window.NEODYMIUM ? window.NEODYMIUM.isDisplayed(arguments[0]) : 'undefined';", el));
            System.out.println("JS window.NEODYMIUM.consumesSpace: " + Selenide.executeJavaScript("return window.NEODYMIUM ? window.NEODYMIUM.consumesSpace(arguments[0]) : 'undefined';", el));
            System.out.println("JS window.NEODYMIUM.getOverflowState: " + Selenide.executeJavaScript("return window.NEODYMIUM ? window.NEODYMIUM.getOverflowState(arguments[0]) : 'undefined';", el));
            System.out.println("JS window.NEODYMIUM.isVisible: " + Selenide.executeJavaScript("return window.NEODYMIUM ? window.NEODYMIUM.isVisible(arguments[0]) : 'undefined';", el));
        }
        System.out.println("=== DIAGNOSTIC END ===");
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

        Selenide.open("https://blog.xceptance.com/");
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
    public void testWaiting() {
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
    public void testIFrames() throws Exception {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.duration", "750");

        Selenide.open("https://www.w3schools.com/tags/tryit.asp?filename=tryhtml_select");

        // check if the cookie banner is present and accept it if so
        SelenideElement acceptCookiesButtonFrame = $("#fast-cmp-iframe");

        SelenideAddons.optionalWaitUntilCondition(acceptCookiesButtonFrame, visible);
        if (acceptCookiesButtonFrame.isDisplayed()) {
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

    @Test
    public void testHighlightingWithBlinkCount() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.duration", "150");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.blink.count", "2");

        Selenide.open("https://blog.xceptance.com/");
        $(".site-title").shouldBe(visible);
        DebugUtils.injectJavaScript();
        assertJsSuccessfullyInjected();

        final List<WebElement> list = Neodymium.getDriver().findElements(By.cssSelector(".site-title"));
        DebugUtils.highlightAllElements(list, Neodymium.getDriver());
        $(".neodymium-highlight-box").shouldNot(exist);
    }

    private void assertJsSuccessfullyInjected() {
        Assert.assertTrue(Selenide.executeJavaScript("return !!window.NEODYMIUM"));
    }
}
