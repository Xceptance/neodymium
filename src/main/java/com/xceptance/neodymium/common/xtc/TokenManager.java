package com.xceptance.neodymium.common.xtc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xceptance.neodymium.common.xtc.config.XtcApiConfiguration;
import com.xceptance.neodymium.common.xtc.dto.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    // General configuration
    private static final String HOST = "https://xtc.xceptance.com";

    private final String apiKey;

    private final String apiSecret;

    private final String scope;

    private final HttpClient client = HttpClient.newBuilder()
                                                .connectTimeout(Duration.ofSeconds(60))
                                                .build();

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    // Internal state for the token and its expiration time
    private volatile String token;

    private volatile Instant tokenExpiry;

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenManager.class);

    /**
     * Constructs a TokenManager instance, initializing the API key, secret, and scope from the XtcApiContext configuration. This constructor is used to manage
     * the authentication process with the XTC API. All parameters are taken from the XtcApiContext configuration.
     */
    public TokenManager()
    {
        XtcApiConfiguration configuration = XtcApiContext.configuration;

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
            LOGGER.info("Token is expired or null. Fetching a new one...");
            authenticate();
        }
        return token;
    }

    /**
     * Authenticates with the XTC API and retrieves a bearer token. This method sends a POST request to the XTC API's OAuth endpoint with the client credentials
     * and scope. Mandatory parameters are the client ID, client secret, and scope.
     */
    public void authenticate() throws IOException, InterruptedException
    {
        LOGGER.info("Authenticating with XTC API...");

        // Create the URL-encoded form string as payload for the authentication request
        String formData = "client_id=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8) +
            "&client_secret=" + URLEncoder.encode(apiSecret, StandardCharsets.UTF_8) +
            "&grant_type=" + URLEncoder.encode("client_credentials", StandardCharsets.UTF_8) +
            "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(HOST + "/oauth/token"))
                                         .header("Content-Type", "application/x-www-form-urlencoded")
                                         .POST(HttpRequest.BodyPublishers.ofString(formData))
                                         .build();

        HttpResponse<String> response = HttpUtils.sendWithRetries(this.client, request);
        if (response.statusCode() != 200)
        {
            LOGGER.error("Failed to authenticate with XTC API. Status code: {}, Response: {}", response.statusCode(), response.body());
            throw new IOException("Failed to authenticate with XTC API. Status code: " + response.statusCode() +
                                      ", Response: " + response.body());
        }

        AuthResponse authResponse = gson.fromJson(response.body(), AuthResponse.class);
        if (authResponse == null || authResponse.getAccessToken() == null)
        {
            LOGGER.error("Failed to parse access_token from auth response: {}", response.body());
            throw new IOException("Failed to parse access_token from auth response: " + response.body());
        }

        this.token = authResponse.getAccessToken();
        this.tokenExpiry = Instant.now().plusSeconds(authResponse.getExpiresIn() - 60); // Subtract 60 seconds for safety

        LOGGER.info("Bearer token extracted: {}", this.token);
        LOGGER.info("Token expires in: {} seconds", authResponse.getExpiresIn());
    }
}
