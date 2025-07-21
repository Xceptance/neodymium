package com.xceptance.neodymium.common.xtc;

import com.xceptance.neodymium.common.xtc.dto.UpdateRunRequest;
import com.xceptance.neodymium.common.xtc.dto.UpdateRunRequest.FinishExecution;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.zip.GZIPOutputStream;

// called from maven
// parses the results of the tests and uploads them to XTC using the XTC API client
public class ResultProcessor
{
    // The directories for the surefire reports and allure results are set via command line arguments
    private static String surefireReportsDir;

    private static String allureResultsDir; // not necessary not, but most likely when updating the test run results during the test run

    private static String allureReportDir;

    private static final String DEFAULT_RESULTS_DIRECTORIES = System.lineSeparator() + "--surefire-dir=${project.build.directory}/surefire-reports" +
        System.lineSeparator() + "--allure-dir=${project.build.directory}/allure-results" +
        System.lineSeparator() + "--allure-report-dir=${project.build.directory}/site/allure-maven-plugin";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultProcessor.class);

    public static void main(String[] args) throws IOException, InterruptedException
    {
        LOGGER.info("ResultProcessor starting...");

        if (!XtcApiContext.isXtcApiEnabled())
        {
            LOGGER.info("XTC API is disabled. Exiting...");
            return; // TODO throw an exception or change all exceptions to logger.error() and break the execution?
        }
        XtcApiContext.ensureRequiredConfiguration();

        // TODO config instead of arguments? this is probably better, so it is easier to change and can be validated using the XtcApiContext.ensureRequiredConfiguration() method
        parseArguments(args);

        LOGGER.info("XtcApiClient starting...");
        XtcApiClient xtcApiClient = new XtcApiClient();

        // read the run ID from the system properties
        LOGGER.info("Reading run ID from system properties...");
        int runId = Integer.parseInt(System.getProperty("xtc.run.id"));
        LOGGER.info("Run ID: {}", runId);

        updateTestRunResults(xtcApiClient, runId);
        uploadAllureReport(xtcApiClient, runId);

        // remove the run ID from the system properties
        LOGGER.info("Remove run ID from system properties on exit...");
        System.clearProperty("xtc.run.id");
    }

    /**
     * Parses the command line arguments to set the directories for surefire reports, allure results, and allure report. Will throw an exception if the required
     * parameters are not provided.
     *
     * @param args
     *     the command line arguments
     */
    private static void parseArguments(String[] args)
    {
        LOGGER.info("Parsing arguments...");

        if (args.length == 0)
        {
            LOGGER.error(
                "No arguments provided. Please provide test results directories as arguments. surefire-dir, allure-dir, allure-report-dir must be set. " +
                    "The correct values are most likely the following: {}", DEFAULT_RESULTS_DIRECTORIES);
            throw new RuntimeException(
                "No arguments provided. Please provide test results directories as arguments. surefire-dir, allure-dir, allure-report-dir must be set. " +
                    "The correct values are most likely the following:" + DEFAULT_RESULTS_DIRECTORIES);
        }

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

        LOGGER.info("Surefire reports directory: {}", surefireReportsDir);
        LOGGER.info("Allure results directory: {}", allureResultsDir);
        LOGGER.info("Allure report directory: {}", allureReportDir);

        if (surefireReportsDir == null || allureResultsDir == null || allureReportDir == null)
        {
            LOGGER.error(
                "Missing required arguments. Please provide surefire-dir, allure-dir, and allure-report-dir as arguments. The correct values are most likely the following: {}",
                DEFAULT_RESULTS_DIRECTORIES);
            throw new RuntimeException(
                "Missing required arguments. Please provide surefire-dir, allure-dir, and allure-report-dir as arguments." +
                    " The correct values are most likely the following:" + DEFAULT_RESULTS_DIRECTORIES);
        }
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

            // compress the allure report directory into a tar.gz archive and set the path to the archive
            Path archivePath = createTarGzArchive(allurePath, "allure-report.tar.gz");
            // TODO check if the archive exists?

            xtcApiClient.uploadReport(runId, archivePath);
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
