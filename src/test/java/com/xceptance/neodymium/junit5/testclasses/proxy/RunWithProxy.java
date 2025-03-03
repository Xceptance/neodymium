package com.xceptance.neodymium.junit5.testclasses.proxy;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriverException;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.browser.Browser;

@Browser("Chrome_headless")
public class RunWithProxy
{
    // the test is expected to fail since we configured a not working proxy server
    @NeodymiumTest
    public void testProxyConfiguration()
    {
        Assertions.assertThrows(WebDriverException.class, () -> {
            Selenide.open("https://github.com/Xceptance/neodymium-library/");
        });
    }

    // the test is expected to run since we configured a bypass for "www.xceptance.com"
    @NeodymiumTest
    public void testProxyBypassConfiguration()
    {
        Selenide.open("https://www.xceptance.com");
        $("#page #navigation").shouldBe(visible);
    }
}
