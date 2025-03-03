package com.xceptance.neodymium.junit5.tests;

import com.xceptance.neodymium.junit5.testclasses.proxy.RunWithProxy;
import com.xceptance.neodymium.junit5.testclasses.proxy.SetProxyForWebDriver;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;
import com.xceptance.neodymium.util.Neodymium;
import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProxyConfigurationTest extends AbstractNeodymiumTest
{
    private static final String HOST = "bemylittleproxydarling.se";

    private static final String PORT = "1323";

    private static final String BYPASS = "www.xceptance.com";

    private static final String SOCKET_USERNAME = "username";

    private static final String SOCKET_PASSWORD = "password";

    private static final String SOCKET_VERSION = "4";

    @BeforeAll
    public static void beforeClass() throws IOException
    {
        final String fileLocation = "config/temp-proxy-neodymiumProxyConfigurationTest.properties";

        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.proxy", "true");
        properties.put("neodymium.proxy.host", HOST);
        properties.put("neodymium.proxy.port", PORT);
        properties.put("neodymium.proxy.bypassForHosts", BYPASS);

        properties.put("neodymium.proxy.socket.userName", SOCKET_USERNAME);
        properties.put("neodymium.proxy.socket.password", SOCKET_PASSWORD);
        properties.put("neodymium.proxy.socket.version", SOCKET_VERSION);

        // set the new properties
        for (String key : properties.keySet())
        {
            Neodymium.configuration().setProperty(key, properties.get(key));
        }

        File tempConfigFile = new File("./" + fileLocation);
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);
    }

    @Test
    public void testExecutionWithProxy()
    {
        // test proxy configuration as far as possible without setting up a proxy
        NeodymiumTestExecutionSummary summary = run(RunWithProxy.class);
        checkPass(summary, 2, 0);
    }

    @Test
    public void testSettingOfProxyForWebdriver()
    {
        // test adding proxy configuration to different WebDriver options and validate them
        NeodymiumTestExecutionSummary summary = run(SetProxyForWebDriver.class);
        checkPass(summary, 4, 0);
    }
}
