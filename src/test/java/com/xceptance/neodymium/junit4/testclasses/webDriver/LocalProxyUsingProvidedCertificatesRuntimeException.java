package com.xceptance.neodymium.junit4.testclasses.webDriver;

import static com.codeborne.selenide.Condition.visible;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.browserup.bup.proxy.auth.AuthType;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;
@Retry(exceptions = "Could not start a new session. Response code 500")
@RunWith(NeodymiumRunner.class)
public class LocalProxyUsingProvidedCertificatesRuntimeException
{
    @Test()
    @Browser("Chrome_headless")
    public void test1()
    {
        Assert.assertNotNull(Neodymium.getDriver());
        Assert.assertNotNull(Neodymium.getLocalProxy());
        Neodymium.getLocalProxy().autoAuthorization("authenticationtest.com", "User", "Pass", AuthType.BASIC);

        Selenide.open("https://authenticationtest.com/HTTPAuth/");
        Assert.assertEquals("Authentication Test", Selenide.title());
        Selenide.$(".alert-success").shouldBe(visible);
    }
}
