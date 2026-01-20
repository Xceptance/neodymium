package com.xceptance.neodymium.util;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit5.NeodymiumTest;
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

@Browser("Chrome_1500x1000")
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
