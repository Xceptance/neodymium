package com.xceptance.neodymium.common.xtc;

import com.xceptance.neodymium.common.xtc.dto.UpdateRunRequest;
import com.xceptance.neodymium.common.xtc.dto.UpdateRunRequest.FinishExecution;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.zip.GZIPOutputStream;

import static com.xceptance.neodymium.util.AllureAddons.JSON_VIEWER_SCRIPT_PATH;

// called from maven
// parses the results of the tests and uploads them to XTC using the XTC API client
public class ResultProcessor
{
    private static String surefireReportsDir = XtcApiContext.configuration.xtcApiSurefireResultDirectory();

    private static String allureResultsDir = XtcApiContext.configuration.xtcApiAllureResultDirectory(); // not necessary now, but most likely when updating the test results during the test run

    private static String allureReportDir = XtcApiContext.configuration.xtcApiAllureReportDirectory();

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultProcessor.class);

    public static void main(String[] args) throws IOException, InterruptedException
    {
        LOGGER.info("ResultProcessor starting...");

        if (!XtcApiContext.isXtcApiEnabled())
        {
            LOGGER.info("XTC API is disabled. Exiting...");
            return;
        }

        String runIdRaw = System.getProperty("xtc.run.id");

        if (StringUtils.isBlank(runIdRaw))
        {
            LOGGER.info("XTC run ID system property not found. Checking temp run id file.");

            String filePath = StringUtils.isNotBlank(XtcApiContext.configuration.xtcApiRunIdStorageFilePath())
                ? XtcApiContext.configuration.xtcApiRunIdStorageFilePath()
                : System.getProperty("build.dir") + File.separator + "temp_run_id.txt";

            runIdRaw = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
        }

        if (StringUtils.isBlank(runIdRaw))
        {
            LOGGER.error(
                "Run ID is not set in system properties or temp run id file. Please run RunInitializer first to create a test run and make sure to that either the property or the file is written.");
            throw new RuntimeException(
                "Run ID is not set in system properties or temp run id file. Please run RunInitializer first to create a test run and make sure to that either the property or the file is written.");
        }

        int runId = Integer.parseInt(runIdRaw);
        LOGGER.info("Run ID: {}", runId);

        LOGGER.info("Surefire reports directory: {}", surefireReportsDir);
        LOGGER.info("Allure results directory: {}", allureResultsDir);
        LOGGER.info("Allure report directory: {}", allureReportDir);

        LOGGER.info("XtcApiClient starting...");
        XtcApiClient xtcApiClient = new XtcApiClient();

        // read the run ID from the system properties
        LOGGER.info("Reading run ID from system properties...");

        updateTestRunResults(xtcApiClient, runId);
        uploadAllureReport(xtcApiClient, runId);

        // remove the run ID from the system properties
        LOGGER.info("Remove run ID from system properties on exit...");
        System.clearProperty("xtc.run.id");
    }

    /**
     * Updates the test run results in XTC using the statistics from the surefire reports. This method reads the surefire reports from the specified directory,
     * parses the results, and sends an update request to the XTC API with the test run statistics. The directory for the surefire reports needs to be set.
     *
     * @param xtcApiClient
     *     the XTC API client
     * @param runId
     *     the ID of the test run to update
     * @throws IOException
     *     if an I/O error occurs
     * @throws InterruptedException
     *     if the thread is interrupted while waiting for a response from the XTC API
     */
    public static void updateTestRunResults(XtcApiClient xtcApiClient, int runId) throws IOException, InterruptedException
    {
        LOGGER.info("Processing test results...");

        if (surefireReportsDir != null)
        {
            SurefireResultParser surefireResultParser = new SurefireResultParser();
            TestRunStatistics statistics = surefireResultParser.parseResults(surefireReportsDir);

            LOGGER.info("Test run statistics: {}", statistics);

            FinishExecution finishExecution = new FinishExecution(Instant.now().truncatedTo(ChronoUnit.MILLIS).toString(),
                                                                  statistics.getStatus());

            UpdateRunRequest updateRunRequest = new UpdateRunRequest(statistics.getTotalTests(),
                                                                     statistics.getFailedTests(),
                                                                     statistics.getSkippedTests(),
                                                                     statistics.getBrokenTests(),
                                                                     statistics.getPassedTests(),
                                                                     finishExecution);

            xtcApiClient.updateTestRun(runId, updateRunRequest);
        }
    }

    /**
     * Uploads the Allure report to XTC. This method compresses the Allure report directory into a tar.gz archive and uploads it to the specified test run in
     * XTC. The directory for the Allure report needs to be set.
     *
     * @param xtcApiClient
     *     the XTC API client
     * @param runId
     *     the ID of the test run to upload the report to
     * @throws IOException
     *     if an I/O error occurs while creating the archive or uploading the report
     * @throws InterruptedException
     *     if the thread is interrupted while waiting for a response from the XTC API
     */
    public static void uploadAllureReport(XtcApiClient xtcApiClient, int runId) throws IOException, InterruptedException
    {
        LOGGER.info("Processing Allure results...");

        if (allureReportDir != null)
        {
            // check if the allure results directory exists
            Path allurePath = Path.of(allureReportDir);

            if (!Files.exists(allurePath) || !Files.isDirectory(allurePath))
            {
                LOGGER.error("Invalid allure results directory: {}", allureReportDir);
                return;
            }

            // insert the JSON viewer script
            moveFileToReportDirectory(Path.of(JSON_VIEWER_SCRIPT_PATH));

            // compress the allure report directory into a tar.gz archive and set the path to the archive
            Path archivePath = createTarGzArchive(allurePath, "allure-report.tar.gz");
            // TODO check if the archive exists?

            xtcApiClient.uploadReport(runId, archivePath);
        }
    }

    private static void moveFileToReportDirectory(Path source)
    {
        Path destination = Paths.get(allureReportDir + File.separator + source);

        try
        {
            // Ensure the parent directory exists
            Path parentDir = destination.getParent();
            if (parentDir != null && !Files.exists(parentDir))
            {
                LOGGER.info("Creating directory: {}", parentDir);
                Files.createDirectories(parentDir);
            }

            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            LOGGER.error("Moving the JSON-Viewer script failed.\nSource: {} \nDestination: {}", source, destination, e);
            throw new RuntimeException("Moving the JSON-Viewer script failed.\nSource: " + source + " \nDestination: " + destination, e);
        }
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
                         LOGGER.error("Failed to add file to archive: {}", file, e);
                         throw new RuntimeException("Failed to add file to archive: " + file, e);
                     }
                 });
        }

        LOGGER.info("Created archive: {}", archivePath.toAbsolutePath());
        return archivePath;
    }
}
