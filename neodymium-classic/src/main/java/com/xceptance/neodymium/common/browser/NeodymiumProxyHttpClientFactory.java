package com.xceptance.neodymium.common.browser;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.remote.http.HttpClient;

import com.xceptance.neodymium.common.browser.configuration.TestEnvironment;

import okhttp3.Authenticator;
import okhttp3.ConnectionPool;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class NeodymiumProxyHttpClientFactory implements HttpClient.Factory
{
    private final ConnectionPool pool = new ConnectionPool();

    private TestEnvironment testEnvironment;

    public NeodymiumProxyHttpClientFactory(TestEnvironment testEnvironment) throws MalformedURLException
    {
        this.testEnvironment = testEnvironment;
    }

    @Override
    public HttpClient createClient(ClientConfig config)
    {
        // copied from org.openqa.selenium.remote.internal.OkHttpClient.Factory.Builder to prevent big
        // differences in implementation
        Builder client = new OkHttpClient.Builder();
        client.connectionPool(pool)
              .followRedirects(true)
              .followSslRedirects(true)
              .readTimeout(config.readTimeout().toMillis(), MILLISECONDS)
              .connectTimeout(config.connectionTimeout().toMillis(), MILLISECONDS);

        // Neodymium integration to use proxy
        configureClientWithProxy(client, config.baseUrl());

        // copied from org.openqa.selenium.remote.internal.OkHttpClient.Factory.Builder to prevent big
        // differences in implementation
        client.addNetworkInterceptor(chain -> {
            Request request = chain.request();
            Response response = chain.proceed(request);
            return response.code() == 408
                                          ? response.newBuilder().code(500).message("Server-Side Timeout").build()
                                          : response;
        });
        return new NeoClient(client.build(), config);
    }

    private void configureClientWithProxy(okhttp3.OkHttpClient.Builder client, URL url)
    {
        if (testEnvironment.useProxy())
        {
            String host = testEnvironment.getProxyHost();
            Integer port = testEnvironment.getProxyPort();
            client.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)));

            String proxyUsername = testEnvironment.getProxyUsername();
            String proxyPassword = testEnvironment.getProxyPassword();
            if (StringUtils.isNoneEmpty(proxyUsername, proxyPassword))
            {
                String credentials = Credentials.basic(proxyUsername, proxyPassword);
                client.proxyAuthenticator(new NeodymiumAuthenticator("Proxy-Authorization", credentials));
            }
        }
    }

    @Override
    public void cleanupIdleClients()
    {
        pool.evictAll();
    }

    private static class NeodymiumAuthenticator implements Authenticator
    {
        String headerName;

        String credentials;

        public NeodymiumAuthenticator(String headerName, String credentials)
        {
            this.headerName = headerName;
            this.credentials = credentials;
        }

        @Override
        public Request authenticate(Route route, Response response) throws IOException
        {
            if (response.request().header(headerName) != null)
            {
                return null; // Give up, we've already attempted to authenticate.
            }
            return response.request().newBuilder()
                           .header(headerName, credentials)
                           .build();
        }
    }
}
