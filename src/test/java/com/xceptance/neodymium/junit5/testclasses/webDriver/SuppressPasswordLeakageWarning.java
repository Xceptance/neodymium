package com.xceptance.neodymium.junit5.testclasses.webDriver;

import java.io.IOException;
import java.time.Duration;

import org.junit.Assume;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@Browser("Chrome_SuppressPasswordLeakageWarningTest")
@Browser("Chrome_DoNotSuppressPasswordLeakageWarningTest")
public class SuppressPasswordLeakageWarning
{
    public static boolean shouldBeSuppressed = true;

    @NeodymiumTest
    public void test() throws IOException
    {
        Assume.assumeTrue(shouldBeSuppressed ? Neodymium.getBrowserProfileName().equals("Chrome_SuppressPasswordLeakageWarningTest")
                                             : Neodymium.getBrowserProfileName().equals("Chrome_DoNotSuppressPasswordLeakageWarningTest"));
        Selenide.open("https://www.saucedemo.com/");
        Selenide.$("[data-test='username']").val("standard_user");
        Selenide.$("[data-test='password']").val("secret_sauce");
        Selenide.$("[data-test='login-button']").click();
        Selenide.Wait().withMessage("Alert is " + (shouldBeSuppressed ? "" : "not ") + "fired").withTimeout(Duration.ofMillis(30000)).until((driver) -> {
            Object hasFocus = Selenide.executeJavaScript("return document.hasFocus();");
            return hasFocus != null && hasFocus instanceof Boolean && shouldBeSuppressed == ((Boolean) hasFocus);
        });
    }
}
