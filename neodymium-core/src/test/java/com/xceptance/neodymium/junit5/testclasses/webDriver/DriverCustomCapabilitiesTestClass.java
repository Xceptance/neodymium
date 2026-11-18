/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.junit5.testclasses.webDriver;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.UnhandledAlertException;

import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

/**
 * Test class verifying the effect of the {@code unhandledPromptBehavior} capability.
 *
 * <p>Only Chrome headless is used for {@link #failsOnUnhandledAlert()} because Firefox
 * headless silently auto-accepts all JS dialogs and therefore never raises
 * {@link UnhandledAlertException}, regardless of that capability.
 *
 * <p>The test page is served as a self-contained {@code data:} URI to avoid any
 * network dependency.
 */
public class DriverCustomCapabilitiesTestClass
{
    /**
     * Minimal HTML page with a button that fires a JS alert on click,
     * plus a second element that can be interacted with afterward.
     */
    private static final String DATA_URI_PAGE =
        "data:text/html;charset=utf-8,"
        + "<html><body>"
        + "<button id='alertBtn' onclick=\"alert('I am a JS Alert')\">Show alert</button>"
        + "<div id='navLink'>Next action</div>"
        + "</body></html>";

    /**
     * Chrome headless raises {@link UnhandledAlertException} when a WebDriver action
     * is attempted while a JS dialog is open.
     * FF headless silently accepts all dialogs, so it is excluded here.
     */
    @Browser("Chrome_headless")
    @NeodymiumTest
    public void failsOnUnhandledAlert()
    {
        Assert.assertThrows(UnhandledAlertException.class, () -> provokeUnhandledAlertException());
    }

    /**
     * With {@code unhandledPromptBehavior=accept} capability set, the dialog is
     * auto-accepted and no exception is raised — verified for both Chrome and Firefox.
     */
    @Browser("Chrome_with_capability")
    @Browser("FF_with_capability")
    @NeodymiumTest
    public void doesntFailOnUnhandledAlert()
    {
        provokeUnhandledAlertException();
    }

    /**
     * Opens a self-contained page, clicks a button that triggers a JS alert, then
     * performs a second WebDriver action without handling the alert.
     *
     * <p>Chrome headless raises {@link UnhandledAlertException} on the second action.
     * Chrome with {@code unhandledPromptBehavior=accept} auto-accepts the alert and
     * the second action succeeds silently.
     */
    public void provokeUnhandledAlertException()
    {
        Selenide.open(DATA_URI_PAGE);

        // First raw Selenium click — fires the JS alert
        Neodymium.getDriver().findElement(By.id("alertBtn")).click();
        Selenide.sleep(300);

        // Second raw Selenium call — Chrome throws UnhandledAlertException here
        // if the alert is still open; Selenide wraps exceptions so we use raw driver
        Neodymium.getDriver().findElement(By.id("navLink")).click();
    }
}
