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
package com.xceptance.neodymium.junit5.testclasses.webDriver;

import java.io.IOException;
import java.time.Duration;

import org.junit.Assume;

import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

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
