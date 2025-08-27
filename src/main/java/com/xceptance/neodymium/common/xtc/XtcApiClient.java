package com.xceptance.neodymium.common.xtc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xceptance.neodymium.common.xtc.dto.CreateRunRequest;
import com.xceptance.neodymium.common.xtc.dto.CreateRunResponse;
import com.xceptance.neodymium.common.xtc.dto.UpdateRunRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;

public class XtcApiClient
{
    private static final String HOST = "https://xtc.xceptance.com";

    private final String apiUrl;

    private final String encodedOrg;

    private final String encodedProject;

    private final TokenManager tokenManager;

    private final HttpClient client = HttpClient.newBuilder()
                                                .connectTimeout(Duration.ofSeconds(300))
                                                .build();

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    private static final Logger LOGGER = LoggerFactory.getLogger(XtcApiClient.class);

    /**
     * Constructs an XtcApiClient instance, initializing the API URL and token manager. The organization and project are encoded to ensure they are safe for use
     * in URLs. All parameters are taken from the XtcApiContext configuration.
     */
    public XtcApiClient()
    {
        this.encodedOrg = URLEncoder.encode(XtcApiContext.configuration.xtcApiOrganization(), StandardCharsets.UTF_8);
        this.encodedProject = URLEncoder.encode(XtcApiContext.configuration.xtcApiProject(), StandardCharsets.UTF_8);

        this.apiUrl = HOST + "/public/api/v2/orgs/" + encodedOrg + "/projects/" + encodedProject + "/executions";

        this.tokenManager = new TokenManager();
    }

    /**
     * Creates a test run in the XTC API with the provided request data. Mandatory parameters are {@code startedAt} and {@code name}.
     *
     * @param createRunRequest
     *     the request data for creating the test run
     * @return the response containing the created test run details
     * @throws IOException
     *     if an I/O error occurs during the request
     * @throws InterruptedException
     *     if the thread is interrupted while waiting for the response
     */
    public CreateRunResponse createTestRun(CreateRunRequest createRunRequest) throws IOException, InterruptedException
    {
        LOGGER.info("Creating test run in XTC API...");

        String jsonRequestBody = gson.toJson(createRunRequest);
        LOGGER.info("Request body: {}", jsonRequestBody);

        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(apiUrl))
                                         .header("Content-Type", "application/json")
                                         .header("Authorization", "Bearer " + tokenManager.getToken())
                                         .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                                         .build();

        HttpResponse<String> response = HttpUtils.sendWithRetries(this.client, request);
        if (response.statusCode() != 200 && response.statusCode() != 201)
        {
            String errorMessage = String.format("Failed to create test run. Status: %d, Response: %s", response.statusCode(), response.body());
            LOGGER.error(errorMessage);

            if (XtcApiContext.configuration.xtcApiThrowExceptionForRunCreationError())
            {
                throw new IOException(errorMessage);
            }
        }

        LOGGER.info("Test run created successfully. Response: {}", response.body());
        return gson.fromJson(response.body(), CreateRunResponse.class);
    }

    // TODO rename to finishTestRun when update is implemented and can be used (or a way is found to update the test run in Neo without concurrency issues)
    /**
     * Updates an existing test run in the XTC API with the provided request data. This method allows you to modify the details of a test run after it has been
     * created. The {@code testRunId} is required to identify which test run to update. Mandatory parameters are {@code totalTestCases},
     * {@code failedTestCases}, {@code skippedTestCases},{@code brokenTestCases} and {@code passedTestCases}. If {@code finishedAt} is provided,
     * {@code finishedAt} and {@code finalResult} are also required.
     *
     * @param testRunId
     *     the ID of the test run to update
     * @param updateRunRequest
     *     the request data for updating the test run
     * @return the response containing the updated test run details
     * @throws IOException
     *     if an I/O error occurs during the request
     * @throws InterruptedException
     *     if the thread is interrupted while waiting for the response
     */
    public UpdateRunRequest updateTestRun(int testRunId, UpdateRunRequest updateRunRequest) throws IOException, InterruptedException
    {
        LOGGER.info("Updating test run in XTC API...");

        String jsonRequestBody = gson.toJson(updateRunRequest);
        LOGGER.info("Request body: {}", jsonRequestBody);

        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(apiUrl + "/" + testRunId))
                                         .header("Content-Type", "application/json")
                                         .header("Authorization", "Bearer " + tokenManager.getToken())
                                         .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                                         .build();

        HttpResponse<String> response = HttpUtils.sendWithRetries(this.client, request);
        if (response.statusCode() != 200)
        {
            String errorMessage = String.format("Failed to update test run. Status: %d, Response: %s", response.statusCode(), response.body());
            LOGGER.error(errorMessage);

            if (XtcApiContext.configuration.xtcApiThrowExceptionForRunUpdateError())
            {
                throw new IOException(errorMessage);
            }
        }

        LOGGER.info("Test run updated successfully. Response: {}", response.body());
        return gson.fromJson(response.body(), UpdateRunRequest.class);
    }

    /**
     * Uploads the report {@code reportPath} to the XTC API. This method is used to upload the report {@code reportPath} of a test run after it has been
     * created. The {@code reportPath} should be a valid path to the report and is mandatory.  The {@code reportPath} must be in gzip format, and the
     * Content-Type header must be set to {@code "application/gzip"}. The {@code testRunId} is required to identify which test run to update.
     *
     * @param testRunId
     *     the ID of the test run to which the report should be uploaded
     * @param reportPath
     *     the path to the report reportPath to upload
     */
    public void uploadReport(int testRunId, Path reportPath)
    {
        LOGGER.info("Uploading report to XTC API...");

        try
        {
            String url = apiUrl + "/" + testRunId + "/report";
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(url))
                                             .header("Authorization", "Bearer " + tokenManager.getToken())
                                             .header("Content-Type", "application/gzip")
                                             .POST(HttpRequest.BodyPublishers.ofFile(reportPath))
                                             .build();

            HttpResponse<String> response = HttpUtils.sendWithRetries(this.client, request);
            if (response.statusCode() != 200 && response.statusCode() != 201)
            {
                String errorMessage = String.format("Failed to upload report. Status: %d, Response: %s", response.statusCode(), response.body());
                LOGGER.error(errorMessage);

                if (XtcApiContext.configuration.xtcApiThrowExceptionForReportUploadError())
                {
                    throw new IOException(errorMessage);
                }
            }

            LOGGER.info("Report uploaded successfully. Response: {}", response.body());
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to upload report: {}", e.getMessage(), e);
            LOGGER.error("Exception while creating test run: ", e);

            e.printStackTrace(System.err);
        }
    }
}
