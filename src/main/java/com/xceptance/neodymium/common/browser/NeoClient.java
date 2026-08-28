package com.xceptance.neodymium.common.browser;

/**
 * @deprecated Use {@link org.neodymium.common.browser.NeoClient} instead.
 */
@Deprecated
public class NeoClient extends org.neodymium.common.browser.NeoClient
{
    public NeoClient(okhttp3.OkHttpClient client, org.openqa.selenium.remote.http.ClientConfig clientConfig)
    {
        super(client, clientConfig);
    }
}
