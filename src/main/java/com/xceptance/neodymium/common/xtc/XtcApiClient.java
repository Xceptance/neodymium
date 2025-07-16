package com.xceptance.neodymium.common.xtc;

import com.xceptance.neodymium.common.xtc.config.XtcApiConfiguration;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

public class XtcApiClient
{
    public static XtcApiConfiguration configuration = ConfigFactory.create(XtcApiConfiguration.class);

    public static final String ORG = "xc";

    public static final String PROJECT = "neo-example";

    public static final String ENCODED_ORG = URLEncoder.encode(configuration.xtcApiOrganization(), StandardCharsets.UTF_8);

    public static final String ENCODED_PROJECT = URLEncoder.encode(configuration.xtcApiProject(), StandardCharsets.UTF_8);

    public static final String HOST = "https://xtc.xceptance.com";

    // TODO can I insert the values right away or are there some timing issues?
    // common part for all requests /public/api/v2/orgs/{org}/projects/{project}/executions
    // public static final String API_URL = HOST + "/public/api/v2/orgs/";
    public static final String API_URL = HOST + "/public/api/v2/orgs/" + ENCODED_ORG + "/projects/" + ENCODED_PROJECT + "/executions";

    public static final String API_KEY = "6863e1f0da374a1374e6c1cd";

    public static final String API_SECRET = "dhttTqYanwR9gvcG1xwMg38SU31VBkIvNjg2M2UxZjBkYTM3NGExMzc0ZTZjMWNk";

    private static String BEARER_TOKEN = "your_bearer_token_here";

    private static String RUN_ID = "your_run_id_here";

    private static final String teststring = configuration.xtcApiNumberOfRetries() + " " + configuration.xtcApiScope();

    // TODO check client configuration -> maybe timeout is too short for uploading large reports
    private static HttpClient client = HttpClient.newBuilder()
                                                 .connectTimeout(Duration.ofSeconds(100))
                                                 .build();

    // TODO implement properties file for configuration
    // fine

    // TODO get Project and Organization from properties file or environment variables
    // fine

    // TODO implement retry logic for API calls

    // TODO add error handling and logging
    // logging instead of sout with the common logger (private static final Logger LOGGER = LoggerFactory.getLogger(MultibrowserConfiguration.class);)

    // TODO save run number in properties or sys env when initializing it before the test run

    // TODO return status codes or response from API calls?

    // TODO move to ResultProcessor class
    public static void main(String[] args) throws IOException
    {
        System.out.println("XtcApiClient running...");

        System.out.println(teststring);

        // TODO config instead of arguments?
        if (args.length == 0)
        {
            System.out.println("No arguments provided. Please provide test results directories as arguments. Exiting...");
            System.out.println("--surefire-dir=/path/to/surefire/reports");
            System.out.println("--allure-dir=/path/to/allure/results");

            return;
        }

        System.out.println("Processing test results...");
        Arrays.stream(args).forEach(arg -> System.out.println("Argument: " + arg));

        String surefireReportsDir = null;
        String allureResultsDir = null;
        String allureReportDir = null;

        // Parse arguments
        for (String arg : args)
        {
            if (arg.startsWith("--surefire-dir="))
            {
                surefireReportsDir = arg.substring("--surefire-dir=".length());
            }
            if (arg.startsWith("--allure-dir="))
            {
                allureResultsDir = arg.substring("--allure-dir=".length());
            }
            if (arg.startsWith("--allure-report-dir="))
            {
                allureReportDir = arg.substring("--allure-report-dir=".length());
            }
        }

        System.out.println("Surefire reports directory: " + surefireReportsDir);
        System.out.println("Allure results directory: " + allureResultsDir);
        System.out.println("Allure report directory: " + allureReportDir);

        // do the REST calls to the XTC API
        authenticate();

        createTestRun();

        // update the test run with the statistics from the surefire reports if available
        if (surefireReportsDir != null)
        {
            SurefireResultParser surefireResultParser = new SurefireResultParser();
            TestRunStatistics statistics = surefireResultParser.parseResults(surefireReportsDir);

            System.out.println(statistics);

            updateTestRun(statistics);
        }

        // compress and upload the allure report if available
        if (allureReportDir != null)
        {
            System.out.println("Processing Allure results...");

            // check if the allure results directory exists
            Path allurePath = Path.of(allureReportDir);

            if (!Files.exists(allurePath) || !Files.isDirectory(allurePath))
            {
                System.err.println("Invalid allure results directory: " + allureReportDir);
                return;
            }

            // compress the allure report directory into a tar.gz archive and set the path to the archive
            Path archivePath = createTarGzArchive(allurePath, "allure-report.tar.gz");
            // TODO check if the archive exists?

            uploadReport(archivePath);
        }
    }

    /**
     * Authenticates with the XTC API and retrieves a bearer token.
     */
    public static void authenticate()
    {
        System.out.println("Authenticating with XTC API...");

        // Create the payload for the authentication request
        String formData = "client_id=" + URLEncoder.encode(API_KEY, StandardCharsets.UTF_8) +
            "&client_secret=" + URLEncoder.encode(API_SECRET, StandardCharsets.UTF_8) +
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
            BEARER_TOKEN = extractAccessToken(response.body());
            System.out.println("Bearer token extracted: " + BEARER_TOKEN);
        }
        catch (Exception e)
        {
            System.err.println("Authentication failed: " + e.getMessage());
            System.err.println("Exception while creating test run: ");
            e.printStackTrace(System.err);
        }
    }

    /**
     * Creates a test run in the XTC API. The request body is parameterized with the current time and other details.
     */
    public static void createTestRun()
    {
        System.out.println("Creating test run in XTC API...");

        // TODO parameterize request body
        // TODO check / ask where to get the values for the request body (testInstance, profile, link, buildNumber, description)
        // TODO think about how to get the estimated duration
        String requestBody = "{\n" +
            "  \"startedAt\": \"" + Instant.now().toString() + "\",\n" +
            "  \"estimatedDuration\": 0,\n" +
            "  \"name\": \"" + PROJECT + " Test Run\",\n" +
            "  \"testInstance\": \"Neodymium Example Instance\",\n" +
            "  \"profile\": \"default\",\n" +
            "  \"link\": \"https://example.com/test-run-link\",\n" +
            "  \"buildNumber\": \"1.0.0\",\n" +
            "  \"description\": \"Test run created by Neodymium XTC API client\"\n" +
            "}";

        try
        {
            System.out.println("Creating test run with URL: " + API_URL);

            // /public/api/v2/orgs/{org}/projects/{project}/executions
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(API_URL))
                                             .header("Content-Type", "application/json")
                                             .header("Authorization", "Bearer " + BEARER_TOKEN)
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
            RUN_ID = extractIndex(response.body());
            System.out.println("Test run index extracted: " + RUN_ID);
        }
        catch (Exception e)
        {
            System.err.println("Failed to create test run: " + e.getMessage());
            System.err.println("Exception while creating test run: ");
            e.printStackTrace(System.err);
        }
    }

    /**
     * Updates the test run in the XTC API with the provided statistics.
     *
     * @param statistics
     *     the statistics to update the test run with
     */
    public static void updateTestRun(TestRunStatistics statistics)
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
            String url = API_URL + "/" + RUN_ID;

            System.out.println("Updating test run with URL: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(url))
                                             .header("Content-Type", "application/json")
                                             .header("Authorization", "Bearer " + BEARER_TOKEN)
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
    public static void uploadReport(Path file)
    {
        System.out.println("Uploading report to XTC API...");

        try
        {
            // /public/api/v2/orgs/{org}/projects/{project}/executions/{testexecution}/report
            String url = API_URL + "/" + RUN_ID + "/report";

            System.out.println("Report upload URL: " + url);
            System.out.println("Uploading report file: " + file.toAbsolutePath());

            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(url))
                                             .header("Authorization", "Bearer " + BEARER_TOKEN)
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
     * Extracts the index from the JSON response.
     *
     * @param jsonResponse
     *     the JSON response string
     * @return the extracted index
     */
    private static String extractIndex(String jsonResponse)
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

    /**
     * Creates a tar.gz archive of the specified directory.
     *
     * @param sourceDir
     *     the directory to archive
     * @param archiveName
     *     the name of the resulting archive file
     * @return the path to the created archive
     * @throws IOException
     *     if an I/O error occurs
     */
    private static Path createTarGzArchive(Path sourceDir, String archiveName) throws IOException
    {
        Path archivePath = sourceDir.getParent().resolve(archiveName);

        try (FileOutputStream fos = new FileOutputStream(archivePath.toFile());
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            GZIPOutputStream gzos = new GZIPOutputStream(bos);
            TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos))
        {
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            // Walk through the source directory and add files to the archive
            Files.walk(sourceDir)
                 .filter(Files::isRegularFile)
                 .forEach(file -> {
                     try
                     {
                         String relativePath = sourceDir.relativize(file).toString();
                         TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), relativePath);
                         taos.putArchiveEntry(entry);
                         Files.copy(file, taos);
                         taos.closeArchiveEntry();
                     }
                     catch (IOException e)
                     {
                         throw new RuntimeException("Failed to add file to archive: " + file, e);
                     }
                 });
        }

        System.out.println("Created archive: " + archivePath.toAbsolutePath());
        return archivePath;
    }
}
