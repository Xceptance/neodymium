package com.xceptance.neodymium.common.xtc;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public class XtcApiClient
{
    public final String org;

    public final String project;

    public final String encodedOrg;

    public final String encodedProject;

    public static final String HOST = "https://xtc.xceptance.com";

    // common part for all requests https://xtc.xceptance.com/public/api/v2/orgs/{org}/projects/{project}/executions
    public final String apiUrl;

    public final String apiKey;

    public final String apiSecret;

    // TODO think about if the run ID can cause problems with concurrency
    private String runId = "42";

    // TODO check client configuration -> maybe timeout is too short for uploading large reports
    private static HttpClient client = HttpClient.newBuilder()
                                                 .connectTimeout(Duration.ofSeconds(100))
                                                 .build();

    public final TokenHandler tokenHandler;

    // TODO return status codes or response from API calls?

    // TODO implement retry logic for API calls

    // TODO add error handling and logging
    // logging instead of sout with the common logger (private static final Logger LOGGER = LoggerFactory.getLogger(MultibrowserConfiguration.class);)

    // TODO create JSON as payload using a library like Jackson or Gson instead of manually creating the JSON strings

    public XtcApiClient(String org, String project, String apiKey, String apiSecret)
    {
        this.org = org;
        this.project = project;

        this.encodedOrg = URLEncoder.encode(org, StandardCharsets.UTF_8);
        this.encodedProject = URLEncoder.encode(project, StandardCharsets.UTF_8);

        this.apiUrl = HOST + "/public/api/v2/orgs/" + encodedOrg + "/projects/" + encodedProject + "/executions";

        this.apiKey = apiKey;
        this.apiSecret = apiSecret;

        this.tokenHandler = new TokenHandler(apiKey, apiSecret);
    }

    // TODO move to TokenHandler?
    /**
     * Authenticates with the XTC API and retrieves a bearer token.
     */
    public void authenticate()
    {
        System.out.println("TokenHandler authenticating with XTC API...");
        tokenHandler.authenticate();
    }

    /**
     * Creates a test run in the XTC API. The request body is parameterized with the current time and other details.
     */
    public String createTestRun()
    {
        System.out.println("Creating test run in XTC API...");

        // TODO parameterize request body
        String requestBody = "{\n" +
            "  \"startedAt\": \"" + Instant.now().toString() + "\",\n" +
            "  \"estimatedDuration\": 0,\n" +
            "  \"name\": \"" + project + " Test Run\",\n" +
            "  \"testInstance\": \"Neodymium Example Instance\",\n" +
            "  \"profile\": \"default\",\n" +
            "  \"link\": \"https://example.com/test-run-link\",\n" +
            "  \"buildNumber\": \"1.0.0\",\n" +
            "  \"description\": \"Test run created by Neodymium XTC API client\"\n" +
            "}";

        try
        {
            System.out.println("Creating test run with URL: " + apiUrl);

            // /public/api/v2/orgs/{org}/projects/{project}/executions
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(apiUrl))
                                             .header("Content-Type", "application/json")
                                             .header("Authorization", "Bearer " + tokenHandler.getToken())
                                             .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                             .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Create test run response: " + response.body());

            // TODO validate response status code
            /*
            if (response.statusCode() != 201)
            {
                throw new RuntimeException("Failed to create test run: " + response.body());
            }
             */

            // Extract run ID from the response
            runId = extractIndex(response.body());
            System.out.println("Test run index extracted: " + runId);
        }
        catch (Exception e)
        {
            System.err.println("Failed to create test run: " + e.getMessage());
            System.err.println("Exception while creating test run: ");
            e.printStackTrace(System.err);
        }

        return runId;
    }

    // TODO rename to finishTestRun when update is implemented and can be used (or a way is found to update the test run in Neo without concurrency issues)
    /**
     * Updates the test run in the XTC API with the provided statistics.
     *
     * @param statistics
     *     the statistics to update the test run with
     */
    public void updateTestRun(TestRunStatistics statistics)
    {
        System.out.println("Updating test run in XTC API...");

        String requestBody = "{\n" +
            "  \"totalTestCases\": " + statistics.getTotalTests() + ",\n" +
            "  \"failedTestCases\": " + statistics.getFailedTests() + ",\n" +
            "  \"skippedTestCases\": " + statistics.getSkippedTests() + ",\n" +
            "  \"brokenTestCases\": " + statistics.getBrokenTests() + ",\n" +
            "  \"passedTestCases\": " + statistics.getPassedTests() + ",\n" +
            "  \"finishExecution\": {\n" +
            "    \"finishedAt\": \"" + Instant.now().toString() + "\",\n" +
            "    \"finalResult\": \"" + statistics.getStatus() + "\"\n" +
            "  }\n" +
            "}";

        try
        {
            // /public/api/v2/orgs/{org}/projects/{project}/executions/{testexecution}
            String url = apiUrl + "/" + runId;

            System.out.println("Updating test run with URL: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(url))
                                             .header("Content-Type", "application/json")
                                             .header("Authorization", "Bearer " + tokenHandler.getToken())
                                             .method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody))
                                             .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Update test run response: " + response.body());

            /*
            if (response.statusCode() != 201)
            {
                throw new RuntimeException("Failed to create test run: " + response.body());
            }
             */
        }
        catch (Exception e)
        {
            System.err.println("Failed to update test run: " + e.getMessage());
            System.err.println("Exception while creating test run: ");
            e.printStackTrace(System.err);
        }
    }

    /**
     * Uploads the report file to the XTC API.
     *
     * @param file
     *     the path to the report file to upload
     */
    public void uploadReport(Path file)
    {
        System.out.println("Uploading report to XTC API...");

        try
        {
            // /public/api/v2/orgs/{org}/projects/{project}/executions/{testexecution}/report
            String url = apiUrl + "/" + runId + "/report";

            System.out.println("Report upload URL: " + url);
            System.out.println("Uploading report file: " + file.toAbsolutePath());

            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(url))
                                             .header("Authorization", "Bearer " + tokenHandler.getToken())
                                             .header("Content-Type", "application/gzip")
                                             .POST(HttpRequest.BodyPublishers.ofFile(file))
                                             .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Upload report response: " + response.body());
        }
        catch (Exception e)
        {
            System.err.println("Failed to upload report: " + e.getMessage());
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
    private String extractAccessToken(String jsonResponse)
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
     * Extracts the index from the JSON response.
     *
     * @param jsonResponse
     *     the JSON response string
     * @return the extracted index
     */
    private String extractIndex(String jsonResponse)
    {
        System.out.println("Extracting index from response: " + jsonResponse);

        String indexKey = "\"index\":";
        int startIndex = jsonResponse.indexOf(indexKey);

        if (startIndex == -1)
        {
            throw new RuntimeException("Index not found in response");
        }

        startIndex += indexKey.length();

        // Skip any whitespace
        while (startIndex < jsonResponse.length() && Character.isWhitespace(jsonResponse.charAt(startIndex)))
        {
            startIndex++;
        }

        int endIndex = startIndex;

        // Find the end of the number (next comma or closing brace)
        while (endIndex < jsonResponse.length() &&
            Character.isDigit(jsonResponse.charAt(endIndex)))
        {
            endIndex++;
        }

        if (endIndex == startIndex)
        {
            throw new RuntimeException("Invalid index format in response");
        }

        return jsonResponse.substring(startIndex, endIndex);
    }

    public void setRunId(String runId)
    {
        this.runId = runId;
    }
}
