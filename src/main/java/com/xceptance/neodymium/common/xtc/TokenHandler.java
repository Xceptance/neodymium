package com.xceptance.neodymium.common.xtc;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

public class TokenHandler
{
    public static final String HOST = "https://xtc.xceptance.com";

    public final String apiKey;

    public final String apiSecret;

    private static HttpClient client = HttpClient.newBuilder()
                                                 .connectTimeout(Duration.ofSeconds(60))
                                                 .build();

    private volatile String token;

    private volatile Instant tokenExpirationTime;

    public TokenHandler(String apiKey, String apiSecret)
    {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    /**
     * Returns a valid bearer token. If the cached token is expired or missing, it fetches a new one. This method is thread-safe.
     */
    public synchronized String getToken()
    {
        if (token == null || Instant.now().isAfter(tokenExpirationTime))
        {
            System.out.println("Token is expired or null. Fetching a new one...");
            authenticate();
        }
        return token;
    }

    /**
     * Authenticates with the XTC API and retrieves a bearer token.
     */
    public void authenticate()
    {
        System.out.println("Authenticating with XTC API...");

        // Create the payload for the authentication request
        String formData = "client_id=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8) +
            "&client_secret=" + URLEncoder.encode(apiSecret, StandardCharsets.UTF_8) +
            "&grant_type=" + URLEncoder.encode("client_credentials", StandardCharsets.UTF_8) +
            "&scope=" + URLEncoder.encode("TESTEXECUTION_CREATE TESTEXECUTION_FINISH TESTEXECUTION_LIST TESTEXECUTION_REPORT_UPLOAD TESTEXECUTION_UPDATE",
                                          StandardCharsets.UTF_8);

        try
        {
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(HOST + "/oauth/token"))
                                             .header("Content-Type", "application/x-www-form-urlencoded")
                                             .POST(HttpRequest.BodyPublishers.ofString(formData))
                                             .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Auth response: " + response.body());

            // TODO validate response status code

            // Extract access token using string parsing
            this.token = extractAccessToken(response.body());
            this.tokenExpirationTime = Instant.now().plusSeconds(extractExpiresIn(response.body()) - 60);

            System.out.println("Bearer token extracted: " + this.token);
        }
        catch (Exception e)
        {
            System.err.println("Authentication failed: " + e.getMessage());
            System.err.println("Exception while creating test run: ");
            e.printStackTrace(System.err);
        }
    }

    /**
     * Extracts the access token from the JSON response.
     *
     * @param jsonResponse
     *     the JSON response string
     * @return the extracted access token
     */
    private static String extractAccessToken(String jsonResponse)
    {
        String tokenKey = "\"access_token\":\"";
        int startIndex = jsonResponse.indexOf(tokenKey);

        if (startIndex == -1)
        {
            throw new RuntimeException("Access token not found in response");
        }

        startIndex += tokenKey.length();
        int endIndex = jsonResponse.indexOf("\"", startIndex);

        if (endIndex == -1)
        {
            throw new RuntimeException("Invalid access token format in response");
        }

        return jsonResponse.substring(startIndex, endIndex);
    }

    /**
     * Extracts the expires_in value from the JSON response.
     *
     * @param jsonResponse
     *     the JSON response string
     * @return the expires_in value in seconds
     */
    private static int extractExpiresIn(String jsonResponse)
    {
        String expiresInKey = "\"expires_in\":";
        int startIndex = jsonResponse.indexOf(expiresInKey);

        if (startIndex == -1)
        {
            throw new RuntimeException("expires_in not found in response");
        }

        startIndex += expiresInKey.length();

        // Skip any whitespace after the colon
        while (startIndex < jsonResponse.length() && Character.isWhitespace(jsonResponse.charAt(startIndex)))
        {
            startIndex++;
        }

        // Find the end of the number (comma, closing brace, or whitespace)
        int endIndex = startIndex;
        while (endIndex < jsonResponse.length() &&
            Character.isDigit(jsonResponse.charAt(endIndex)))
        {
            endIndex++;
        }

        if (endIndex == startIndex)
        {
            throw new RuntimeException("Invalid expires_in format in response");
        }

        return Integer.parseInt(jsonResponse.substring(startIndex, endIndex));
    }
}
