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
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
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
                       .mockText("test mock", RequestMatchers.urlMatches(HttpMethod.GET, Pattern.compile(".*neodymium-library.*")),
                                 () -> textMock);
        Selenide.open("https://github.com/Xceptance/neodymium-library");
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
