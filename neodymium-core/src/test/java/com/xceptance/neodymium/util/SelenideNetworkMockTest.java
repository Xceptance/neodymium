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
package com.xceptance.neodymium.util;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.aeonbits.owner.ConfigFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.proxy.RequestMatcher.HttpMethod;
import com.codeborne.selenide.proxy.RequestMatchers;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class SelenideNetworkMockTest extends NeodymiumTest
{
    private static File tempConfigFile;

    @BeforeClass
    public static void enableProxy()
    {
        final String fileLocation = "config/temp-SelenideNetworkMockTest-neodymium.properties";
        tempConfigFile = new File("./" + fileLocation);
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.selenideProxy", "true");
        NeodymiumTest.writeMapToPropertiesFile(properties, tempConfigFile);
        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);
    }

    @Test
    public void testProxyForMocking()
    {
        Configuration.proxyEnabled = true;
        String textMock = "{here}";
        Selenide.open();
        WebDriverRunner.getSelenideProxy().responseMocker()
                       .mockText("test mock", RequestMatchers.urlMatches(HttpMethod.GET, Pattern.compile(".*neodymium.*")),
                                 () -> textMock);
        Selenide.open("https://github.com/Xceptance/neodymium");
        $("body").shouldHave(exactText(textMock));
        Selenide.open("https://blog.xceptance.com/");
        $("#masthead .search-toggle").shouldBe(visible);
    }

    @AfterClass
    public static void disableProxy()
    {
        Configuration.proxyEnabled = false;
        NeodymiumTest.deleteTempFile(tempConfigFile);
    }
}
