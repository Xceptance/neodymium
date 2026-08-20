package com.xceptance.neodymium.junit5.testclasses.webDriver;

import static com.codeborne.selenide.Condition.visible;

import org.junit.jupiter.api.Assertions;

import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;
public class LocalProxyTrustAllServers
{
    @NeodymiumTest
    @Browser("Chrome_headless")
    public void test1()
    {
        Assertions.assertNotNull(Neodymium.getDriver());
        Assertions.assertNotNull(Neodymium.getLocalProxy());

        Selenide.open("https://authenticationtest.com/HTTPAuth/");
        Assertions.assertEquals("Authentication Test", Selenide.title());
        Selenide.$(".alert-success").shouldBe(visible);
    }
}
