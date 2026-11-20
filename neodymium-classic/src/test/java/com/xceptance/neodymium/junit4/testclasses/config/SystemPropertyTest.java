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
package com.xceptance.neodymium.junit4.testclasses.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_1024x768")
public class SystemPropertyTest extends NeodymiumTest
{
    @BeforeClass
    public static void beforeClass() throws IOException
    {
        File tempConfigFile = new File("./config/neodymium.temp");
        File configFile = new File("./config/neodymium.properties");
        System.setProperty("neodymium.webDriver.window.height", "1000");
        Map<String, String> properties1 = new HashMap<>();
        properties1.put("neodymium.webDriver.window.height", "500");
        FileUtils.copyFile(configFile, tempConfigFile);
        writeMapToPropertiesFile(properties1, configFile);
    }

    @Test
    public void testSystemProperty()
    {
        // Goto the home page
        Selenide.open("https://www.xceptance.com/en/");
        Assertions.assertEquals(1000, Neodymium.configuration().getWindowHeight());
    }

    @AfterClass
    public static void afterClass() throws IOException
    {
        File tempConfigFile = new File("./config/neodymium.temp");
        File configFile = new File("./config/neodymium.properties");
        FileUtils.copyFile(tempConfigFile, configFile);
        System.clearProperty("neodymium.webDriver.window.height");
    }
}
