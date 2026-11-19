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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.DriverArgumentsTest;
import org.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("FF_with_args")
@Browser("Chrome_with_args")
public class DriverArgumentsConfigTest
{
    private File chromeLogFile = new File(DriverArgumentsTest.logFileNameChrome);

    private File firefoxLogFile = new File(DriverArgumentsTest.logFileNameFirefox);

    @Test
    public void test() throws IOException
    {
        Selenide.open("https://www.xceptance.com/en/");
        if (Neodymium.getBrowserName().equals("chrome"))
        {
            Assertions.assertTrue(chromeLogFile.exists(), "No log file found for chrome");
            Assertions.assertTrue(Files.readString(chromeLogFile.toPath()).contains(DriverArgumentsTest.chromePort), "No log file found for chrome");
        }
        else
        {
            Assertions.assertTrue(new File(DriverArgumentsTest.logFileNameFirefox).exists(), "No log file found for firefox");
            Assertions.assertTrue(Files.readString(firefoxLogFile.toPath()).contains(DriverArgumentsTest.firefoxPort), "Port not found in firefox log");
            Assertions.assertTrue(Files.readString(firefoxLogFile.toPath()).contains(DriverArgumentsTest.firefoxWebsocketPort),
                                  "Websocket port not found in firefox log");
        }
    }

    @After
    public void cleanup()
    {
        if (chromeLogFile.exists())
        {
            chromeLogFile.delete();
        }

        if (firefoxLogFile.exists())
        {
            firefoxLogFile.delete();
        }
    }
}
