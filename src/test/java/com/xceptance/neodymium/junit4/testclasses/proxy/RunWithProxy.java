package com.xceptance.neodymium.junit4.testclasses.proxy;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriverException;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class RunWithProxy
{
    // the test is expected to fail since we configured a not working proxy server
    @Test
    public void testProxyConfiguration()
    {
        Assert.assertThrows(WebDriverException.class, () -> {
            Selenide.open("https://github.com/Xceptance/neodymium-library/");
        });
    }

    // the test is expected to run since we configured a bypass for "www.xceptance.com"
    @Test
    public void testProxyBypassConfiguration()
    {
        Selenide.open("https://www.xceptance.com");
        $("#page #navigation").shouldBe(visible);
    }
}
