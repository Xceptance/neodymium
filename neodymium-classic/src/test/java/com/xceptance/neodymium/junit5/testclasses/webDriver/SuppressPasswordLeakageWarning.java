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
