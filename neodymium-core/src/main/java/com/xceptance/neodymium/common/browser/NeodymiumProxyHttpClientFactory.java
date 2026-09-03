package com.xceptance.neodymium.common.browser;

/**
 * @deprecated Use {@link org.neodymium.common.browser.NeodymiumProxyHttpClientFactory} instead.
 */
@Deprecated
public class NeodymiumProxyHttpClientFactory extends org.neodymium.common.browser.NeodymiumProxyHttpClientFactory
{
    public NeodymiumProxyHttpClientFactory(org.neodymium.common.browser.configuration.TestEnvironment testEnvironment) throws java.net.MalformedURLException
    {
        super(testEnvironment);
    }
}
