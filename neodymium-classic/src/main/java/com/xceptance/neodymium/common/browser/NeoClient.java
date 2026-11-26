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
package com.xceptance.neodymium.common.browser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.openqa.selenium.remote.http.AddSeleniumUserAgent;
import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.http.WebSocket;
import org.openqa.selenium.remote.http.WebSocket.Listener;
import org.openqa.selenium.remote.http.jdk.JdkHttpClient;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Custom http client to communicate with Selenium Grid. In contrast to the default http client offered by Selenium,
 * uses {@link OkHttpClient} for the actual communication and therefore supports accessing grid under proxy
 */
public class NeoClient implements HttpClient
{
    private OkHttpClient client;

    private HttpClient jdkClient;

    private ClientConfig config;

    public NeoClient(OkHttpClient client, ClientConfig clientConfig)
    {
        this.client = client;
        this.config = clientConfig;
    }

    @Override
    public HttpResponse execute(HttpRequest req) throws UncheckedIOException
    {
        Builder builder = buildRequest(req);

        try
        {
            Response response = client.newCall(builder.build()).execute();

            HttpResponse res = new HttpResponse();
            res.setStatus(response.code());
            response
                    .headers()
                    .forEach(
                             (pair) -> res.addHeader(pair.component1(), pair.component2()));
            byte[] responseBody = response.body().bytes();
            if (responseBody != null)
            {
                res.setContent(() -> new ByteArrayInputStream(responseBody));
            }

            return res;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Builder buildRequest(HttpRequest req)
    {
        Builder builder = new Request.Builder().url(createHttpUrl(req));

        req.forEachHeader((name, value) -> {
            builder.header(name, value);
        });

        if (req.getHeader("User-Agent") == null)
        {
            builder.header("User-Agent", AddSeleniumUserAgent.USER_AGENT);
        }
        switch (req.getMethod())
        {
            case DELETE:
                builder.delete();
                break;

            case GET:
                builder.get();
                break;

            case POST:
                builder.post(RequestBody.create(Contents.bytes(req.getContent())));
                break;

            case PUT:
                builder.put(RequestBody.create(Contents.bytes(req.getContent())));
                break;

            default:
                throw new IllegalArgumentException(String.format("Unsupported request method %s: %s", req.getMethod(), req));
        }
        return builder;
    }

    private HttpUrl createHttpUrl(HttpRequest req)
    {
        final HttpUrl.Builder url = HttpUrl.parse(config.baseUrl() + req.getUri()).newBuilder();

        for (String queryParamName : req.getQueryParameterNames())
        {
            url.addQueryParameter(queryParamName, req.getQueryParameter(queryParamName));
        }
        return url.build();
    }

    @Override
    public WebSocket openSocket(HttpRequest request, Listener listener)
    {
        jdkClient = new JdkHttpClient.Factory().createClient(config);
        return jdkClient.openSocket(request, listener);
    }

}
