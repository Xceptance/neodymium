package com.xceptance.neodymium.junit4.testclasses.webDriver;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.UnhandledAlertException;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
public class DriverCustomCapabilitiesTestClass
{
    private static final String DATA_URI_PAGE =
        "data:text/html;charset=utf-8,"
        + "<html><body>"
        + "<button id='alertBtn' onclick=\"alert('I am a JS Alert')\">Show alert</button>"
        + "<div id='navLink'>Next action</div>"
        + "</body></html>";

    @Browser("Chrome_headless")
    @Test
    public void failsOnUnhandledAlert()
    {
        Assert.assertThrows(UnhandledAlertException.class, () -> provokeUnhandledAlertException());
    }

    @Browser("Chrome_with_capability")
    @Browser("FF_with_capability")
    @Test
    public void doesntFailOnUnhandledAlert()
    {
        provokeUnhandledAlertException();
    }

    public void provokeUnhandledAlertException()
    {
        Selenide.open(DATA_URI_PAGE);

        Neodymium.getDriver().findElement(By.id("alertBtn")).click();
        Selenide.sleep(300);

        Neodymium.getDriver().findElement(By.id("navLink")).click();
    }
}
