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
