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

import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.configuration.MultibrowserConfiguration;
import org.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.Alert;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

@Browser("Chrome_1500x1000_headless")
public class AlertHandlingTest extends AbstractNeodymiumTest
{
    @BeforeAll
    public static void beforeClass() throws IOException
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("video.enableFilming", "true");

        File tempConfigFile = File.createTempFile("AlertHandlingTest", "", new File("./config/"));
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        // this line is important as we initialize the config from the temporary file we created above
        MultibrowserConfiguration.clearAllInstances();
        MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
    }

    @NeodymiumTest
    public void test()
    {
        Selenide.open("https://www.xceptance.com");
        $("#introduction").shouldBe(visible);

        Selenide.executeJavaScript("alert('Hello! I am an alert box!');");

        Alert alert = Selenide.switchTo().alert();
        Assertions.assertEquals("Hello! I am an alert box!", alert.getText());
        alert.accept();

        $("#introduction").shouldBe(visible);
    }
}
