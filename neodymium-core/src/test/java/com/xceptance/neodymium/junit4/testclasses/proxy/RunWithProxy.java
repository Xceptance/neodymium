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
package com.xceptance.neodymium.junit4.testclasses.proxy;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriverException;

import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit4.NeodymiumRunner;
@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class RunWithProxy
{
    // the test is expected to fail since we configured a not working proxy server
    @Test
    public void testProxyConfiguration()
    {
        Assert.assertThrows(WebDriverException.class, () -> {
            Selenide.open("https://github.com/Xceptance/neodymium/");
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
