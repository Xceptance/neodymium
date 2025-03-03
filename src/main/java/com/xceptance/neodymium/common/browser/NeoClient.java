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
