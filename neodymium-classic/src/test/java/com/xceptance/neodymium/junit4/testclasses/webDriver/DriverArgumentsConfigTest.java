package com.xceptance.neodymium.junit4.testclasses.webDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.DriverArgumentsTest;
import com.xceptance.neodymium.util.Neodymium;

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
