/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.junit4.testclasses.webDriver;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.UnhandledAlertException;

import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit4.NeodymiumRunner;
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
@RunWith(NeodymiumRunner.class)
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
    @Test
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
    @Test
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
