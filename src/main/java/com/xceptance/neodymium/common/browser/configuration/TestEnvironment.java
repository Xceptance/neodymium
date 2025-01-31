package com.xceptance.neodymium.common.browser.configuration;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

public class TestEnvironment
{
    private String url;

    private String username;

    private String password;

    private String optionsTag;

    private boolean useProxy = false;

    private String proxyHost;

    private Integer proxyPort;

    private String proxyUsername;

    private String proxyPassword;

    protected TestEnvironment(Properties properties, String baseKey)
    {
        url = properties.getProperty(baseKey + ".url");
        username = properties.getProperty(baseKey + ".username");
        if (StringUtils.isBlank(username))
        {
            username = System.getenv("BROWSERSTACK_USERNAME");
        }
        password = properties.getProperty(baseKey + ".password");
        if (StringUtils.isBlank(password))
        {
            password = System.getenv("BROWSERSTACK_PASSWORD");
        }
        optionsTag = properties.getProperty(baseKey + ".optionsTag");
        useProxy = Boolean.valueOf(properties.getProperty(baseKey + ".proxy"));
        if (useProxy)
        {
            proxyHost = properties.getProperty(baseKey + ".proxy.host");
            if (StringUtils.isBlank(proxyHost))
            {
                throw new RuntimeException("The proxy host configured for environment: \"" + baseKey + "\" needs to be set.");
            }

            String port = properties.getProperty(baseKey + ".proxy.port");
            if (StringUtils.isNotBlank(port))
            {
                try
                {
                    proxyPort = Integer.valueOf(port);
                }
                catch (NumberFormatException e)
                {
                    throw new RuntimeException("The proxy port configured for environment: \"" + baseKey + "\" needs to be an Integer.");
                }
            }
            else
            {
                throw new RuntimeException("The proxy port configured for environment: \"" + baseKey + "\" needs to be set.");
            }
            proxyUsername = properties.getProperty(baseKey + ".proxy.username");
            proxyPassword = properties.getProperty(baseKey + ".proxy.password");
        }
    }

    public String getUrl()
    {
        return url;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public boolean useProxy()
    {
        return useProxy;
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    public Integer getProxyPort()
    {
        return proxyPort;
    }

    public String getProxyUsername()
    {
        return proxyUsername;
    }

    public String getProxyPassword()
    {
        return proxyPassword;
    }

    public String getOptionsTag()
    {
        return optionsTag;
    }

    public boolean isUseProxy()
    {
        return useProxy;
    }
}
