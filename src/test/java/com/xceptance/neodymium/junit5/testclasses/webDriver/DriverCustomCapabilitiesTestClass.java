package com.xceptance.neodymium.junit5.testclasses.webDriver;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.UnhandledAlertException;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

public class DriverCustomCapabilitiesTestClass
{
    @Browser("Chrome_headless")
    @Browser("FF_headless")
    @NeodymiumTest
    public void failsOnUnhandledAlert()
    {
        Assert.assertThrows(UnhandledAlertException.class, () -> provokeUnhandledAlertException());
    }

    @Browser("Chrome_with_capability")
    @Browser("FF_with_capability")
    @NeodymiumTest
    public void doesntFailOnUnhandledAlert()
    {
        provokeUnhandledAlertException();
    }

    public void provokeUnhandledAlertException()
    {
        Selenide.open("https://www.xceptance.com/");
        String elementToProvokeAlert = "var e = document.createElement('div');"
                                       + " e.innerHTML = 'testThing'; "
                                       + "e.setAttribute('data-testid','closeIcon');"
                                       + " e.setAttribute('onclick',\"alert(\\\"I am a JS Alert\\\")\");"
                                       + " document.body.appendChild(e);";
        Selenide.executeJavaScript(elementToProvokeAlert);
        Selenide.sleep(1500);
        // could only reproduce with pure Selenium calls, Selenide logs the exception but doesn't throw is
        Neodymium.getDriver().findElement(By.cssSelector("[data-testid='closeIcon']")).click();
        Neodymium.getDriver().findElement(By.cssSelector(".navbar-brand")).click();
    }
}
