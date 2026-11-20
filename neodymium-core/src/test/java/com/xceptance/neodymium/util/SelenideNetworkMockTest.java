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
