package com.xceptance.neodymium.common.xtc;

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
    public static final String API_URL = "https://xtc.xceptance.com/public/api/v2";

    public static final String ORG = "xc";

    public static final String PROJECT = "neo-example";

    public static final String API_KEY = "6863e1f0da374a1374e6c1cd";

    public static final String API_SECRET = "dhttTqYanwR9gvcG1xwMg38SU31VBkIvNjg2M2UxZjBkYTM3NGExMzc0ZTZjMWNk";

    public static String BEARER_TOKEN = "your_bearer_token_here";

    public static String RUN_ID = "your_run_id_here";

    private static HttpClient client = HttpClient.newBuilder()
                                                 .connectTimeout(Duration.ofSeconds(100))
                                                 .build();

    // TODO implement properties file for configuration

    // TODO get Project and Organization from properties file or environment variables

    // TODO implement retry logic for API calls

    // TODO add error handling and logging

    // TODO move to ResultProcessor class
    public static void main(String[] args) throws IOException
    {
        System.out.println("XtcApiClient running...");

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

        // do the REST calls to the XTC API
        authenticate();

        createTestRun();

        if (surefireReportsDir != null)
        {
            SurefireResultParser surefireResultParser = new SurefireResultParser();
            TestRunStatistics statistics = surefireResultParser.parseResults(surefireReportsDir);

            System.out.println(statistics);

            updateTestRun(statistics);
        }

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

            // TODO check if the archive exists
            Path archivePath = createTarGzArchive(allurePath, "allure-report.tar.gz");

            uploadReport(archivePath);
        }
    }

    public static void authenticate()
    {
        System.out.println("Authenticating with XTC API...");

        try
        {
            // URL encode the parameters properly
            String formData = "client_id=" + URLEncoder.encode(API_KEY, StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(API_SECRET, StandardCharsets.UTF_8) +
                "&grant_type=" + URLEncoder.encode("client_credentials", StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode("TESTEXECUTION_CREATE TESTEXECUTION_FINISH TESTEXECUTION_LIST TESTEXECUTION_REPORT_UPLOAD TESTEXECUTION_UPDATE",
                                              StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create("https://xtc.xceptance.com/oauth/token"))
                                             .header("Content-Type", "application/x-www-form-urlencoded")
                                             .POST(HttpRequest.BodyPublishers.ofString(formData))
                                             .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Auth response: " + response.body());

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

    public static void createTestRun()
    {
        System.out.println("Creating test run in XTC API...");

        // TODO parameterize request body
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
            // URL encode the org and project names for the path
            String encodedOrg = URLEncoder.encode(ORG, StandardCharsets.UTF_8);
            String encodedProject = URLEncoder.encode(PROJECT, StandardCharsets.UTF_8);

            // public/api/v2/orgs/xc/projects/neo-example/executions
            String url = API_URL + "/orgs/" + encodedOrg + "/projects/" + encodedProject + "/executions";

            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(url))
                                             .header("Content-Type", "application/json")
                                             .header("Authorization", "Bearer " + BEARER_TOKEN)
                                             .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                             .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Create test run response: " + response.body());

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
            // URL encode the org and project names for the path
            String encodedOrg = URLEncoder.encode(ORG, StandardCharsets.UTF_8);
            String encodedProject = URLEncoder.encode(PROJECT, StandardCharsets.UTF_8);

            // /public/api/v2/orgs/{org}/projects/{project}/executions/{testexecution}
            String url = API_URL + "/orgs/" + encodedOrg + "/projects/" + encodedProject + "/executions/" + RUN_ID;

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

    public static void uploadReport(Path file)
    {
        System.out.println("Uploading report to XTC API...");

        try
        {
            // URL encode the org and project names for the path
            String encodedOrg = URLEncoder.encode(ORG, StandardCharsets.UTF_8);
            String encodedProject = URLEncoder.encode(PROJECT, StandardCharsets.UTF_8);

            // /public/api/v2/orgs/{org}/projects/{project}/executions/{testexecution}/report
            String url = API_URL + "/orgs/" + encodedOrg + "/projects/" + encodedProject + "/executions/" + RUN_ID + "/report";

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

    private static Path createTarGzArchive(Path sourceDir, String archiveName) throws IOException
    {
        Path archivePath = sourceDir.getParent().resolve(archiveName);

        try (FileOutputStream fos = new FileOutputStream(archivePath.toFile());
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            GZIPOutputStream gzos = new GZIPOutputStream(bos);
            TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos))
        {
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            Files.walk(sourceDir)
                 .filter(Files::isRegularFile)
                 .forEach(file -> {
                     try {
                         String relativePath = sourceDir.relativize(file).toString();
                         TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), relativePath);
                         taos.putArchiveEntry(entry);
                         Files.copy(file, taos);
                         taos.closeArchiveEntry();
                     } catch (IOException e) {
                         throw new RuntimeException("Failed to add file to archive: " + file, e);
                     }
                 });
        }

        System.out.println("Created archive: " + archivePath.toAbsolutePath());
        return archivePath;
    }
}
