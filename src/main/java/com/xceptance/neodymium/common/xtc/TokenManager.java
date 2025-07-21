package com.xceptance.neodymium.common.xtc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xceptance.neodymium.common.xtc.config.XtcApiConfiguration;
import com.xceptance.neodymium.common.xtc.dto.AuthResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

public class TokenManager
{
    // Dependencies injected via constructor and general configuration
    private static final String HOST = "https://xtc.xceptance.com";

    private final String apiKey;

    private final String apiSecret;

    private String scope;

    private static HttpClient client = HttpClient.newBuilder()
                                                 .connectTimeout(Duration.ofSeconds(60))
                                                 .build();

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    // Internal state for the token and its expiration time
    private volatile String token;

    private volatile Instant tokenExpiry;

    public TokenManager()
    {
        //XtcApiConfiguration configuration = ConfigFactory.create(XtcApiConfiguration.class);
        XtcApiConfiguration configuration = XtcApiContext.configuration;

        System.out.println("xtcApiKey: " + configuration.xtcApiKey());
        System.out.println("xtcApiSecret: " + configuration.xtcApiSecret());
        System.out.println("xtcApiScope: " + configuration.xtcApiScope());

        this.apiKey = configuration.xtcApiKey();
        this.apiSecret = configuration.xtcApiSecret();

        this.scope = configuration.xtcApiScope();
    }

    /**
     * Returns a valid bearer token. If the cached token is expired or missing, it fetches a new one. This method is thread-safe.
     */
    public synchronized String getToken() throws IOException, InterruptedException
    {
        if (token == null || Instant.now().isAfter(tokenExpiry))
        {
            System.out.println("Token is expired or null. Fetching a new one...");
            authenticate();
        }
        return token;
    }

    /**
     * Authenticates with the XTC API and retrieves a bearer token.
     */
    public void authenticate() throws IOException, InterruptedException
    {
        System.out.println("Authenticating with XTC API...");

        // Create the URL-encoded form string as payload for the authentication request
        String formData = "client_id=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8) +
            "&client_secret=" + URLEncoder.encode(apiSecret, StandardCharsets.UTF_8) +
            "&grant_type=" + URLEncoder.encode("client_credentials", StandardCharsets.UTF_8) +
            "&scope=" + URLEncoder.encode(scope,
                                          StandardCharsets.UTF_8);

        // Create the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(HOST + "/oauth/token"))
                                         .header("Content-Type", "application/x-www-form-urlencoded")
                                         .POST(HttpRequest.BodyPublishers.ofString(formData))
                                         .build();

        // Send the request with retries
        HttpResponse<String> response = HttpUtils.sendWithRetries(this.client, request);
        if (response.statusCode() != 200)
        {
            throw new IOException("Failed to authenticate with XTC API. Status code: " + response.statusCode() +
                                      ", Response: " + response.body());
        }

        // Parse the response body to extract the access token
        AuthResponse authResponse = gson.fromJson(response.body(), AuthResponse.class);
        if (authResponse == null || authResponse.getAccessToken() == null)
        {
            throw new IOException("Failed to parse access_token from auth response: " + response.body());
        }

        // Store the token and its expiration time
        this.token = authResponse.getAccessToken();
        this.tokenExpiry = Instant.now().plusSeconds(authResponse.getExpiresIn() - 60); // Subtract 60 seconds for safety

        System.out.println("Bearer token extracted: " + this.token);
        System.out.println("Token expires in: " + authResponse.getExpiresIn() + " seconds");
    }
}
