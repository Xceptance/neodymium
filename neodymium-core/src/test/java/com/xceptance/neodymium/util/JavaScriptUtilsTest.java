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
package com.xceptance.neodymium.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class JavaScriptUtilsTest
{
    private static LocalTestHttpServer server;

    @BeforeClass
    public static void startServer() throws IOException
    {
        server = LocalTestHttpServer.startWithHtml("<html><body><div id=\"main-content\">Content</div></body></html>");
    }

    @Before
    public void resetConfiguration()
    {
        Neodymium.configuration().removeProperty("neodymium.javaScriptUtils.loading.animationSelector");
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.loading.jQueryIsRequired", "false");
    }

    @AfterClass
    public static void stopServer()
    {
        if (server != null)
        {
            server.close();
        }
    }

    @Test
    public void testWaitingAnimationSelectorUnconfigured()
    {
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.timeout", "10000");
        Selenide.open(server.url("/"));
        final long start = System.currentTimeMillis();
        JavaScriptUtils.waitForReady();
        final long end = System.currentTimeMillis();

        assertTrue("The waiting animation selector is not set, timeout shouldn't be reached.", end - start < Neodymium.configuration().javaScriptTimeout());
    }

    @Test
    public void testWaitingAnimationSelectorExistsOnPage()
    {
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.loading.animationSelector", "#main-content");
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.timeout", "10000");

        Selenide.open(server.url("/"));
        final long start = System.currentTimeMillis();
        JavaScriptUtils.waitForReady();
        final long end = System.currentTimeMillis();

        assertTrue("The waiting animation selector is available on the site, timeout should be reached.",
                   end - start > Neodymium.configuration().javaScriptTimeout());
    }

    @Test
    public void testWaitingAnimationSelectorUnavailableOnPage()
    {
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.loading.animationSelector", ".cantFindThisClass");
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.timeout", "10000");

        Selenide.open(server.url("/"));
        final long start = System.currentTimeMillis();
        JavaScriptUtils.waitForReady();
        final long end = System.currentTimeMillis();

        assertTrue("The waiting animation selector is unavailable on the site, timeout shouldn't be reached.",
                   end - start < Neodymium.configuration().javaScriptTimeout());
    }

    @Test
    public void testWaitingJQueryIsRequired()
    {
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.loading.jQueryIsRequired", "true");
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.timeout", "20000");

        Selenide.open(server.url("/"));
        final long start = System.currentTimeMillis();
        JavaScriptUtils.waitForReady();
        final long end = System.currentTimeMillis();

        assertTrue("jQuery is unavailable on the site, timeout should be reached.",
                   end - start > Neodymium.configuration().javaScriptTimeout());
    }

    @Test
    public void testWaitingJQueryIsNotRequired()
    {
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.loading.jQueryIsRequired", "false");
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.timeout", "10000");

        Selenide.open(server.url("/"));
        final long start = System.currentTimeMillis();
        JavaScriptUtils.waitForReady();
        final long end = System.currentTimeMillis();

        assertTrue("jQuery is  not required, so the timeout shouldn't be reached.",
                   end - start < Neodymium.configuration().javaScriptTimeout());
    }
}
