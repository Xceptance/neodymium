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
 *
 * This class contains code derived from the Selenium project
 * (org.openqa.selenium.remote.internal.OkHttpClient).
 * Copyright (c) Software Freedom Conservancy, licensed under the Apache License, Version 2.0.
 * In compliance with Section 4(b) of the Apache License, Version 2.0, modifications are
 * documented inline.
 */
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
