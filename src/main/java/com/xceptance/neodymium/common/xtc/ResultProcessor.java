package com.xceptance.neodymium.common.xtc;

import com.xceptance.neodymium.common.xtc.config.XtcApiConfiguration;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

// called from maven
// parses the results of the tests and uploads them to XTC using the XTC API client
public class ResultProcessor
{
    public static XtcApiConfiguration configuration = ConfigFactory.create(XtcApiConfiguration.class);

    public static void main(String[] args) throws IOException
    {
        if (!configuration.xtcApiIsEnabled())
        {
            System.out.println("XTC API is disabled. Exiting...");
            return;
        }

        System.out.println("ResultProcessor starting...");

        // TODO config instead of arguments?
        System.out.println("Parsing arguments...");
        if (args.length == 0)
        {
            System.out.println("No arguments provided. Please provide test results directories as arguments. Exiting...");
            System.out.println("--surefire-dir=/path/to/surefire/reports");
            System.out.println("--allure-dir=/path/to/allure/results");

            return;
        }

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

        // initialize
        System.out.println("XtcApiClient starting...");
        XtcApiClient xtcApiClient = new XtcApiClient(configuration.xtcApiOrganization(),
                                                     configuration.xtcApiProject(),
                                                     configuration.xtcApiKey(),
                                                     configuration.xtcApiSecret());

        // do the REST calls to the XTC API
        xtcApiClient.authenticate();

        System.out.println("Reading run ID from system properties...");

        String runId = System.getProperty("xtc.run.id");
        System.out.println("Run ID: " + runId);
        xtcApiClient.setRunId(runId);

        // update the test run with the statistics from the surefire reports if available
        System.out.println("Processing test results...");
        if (surefireReportsDir != null)
        {
            SurefireResultParser surefireResultParser = new SurefireResultParser();
            TestRunStatistics statistics = surefireResultParser.parseResults(surefireReportsDir);

            System.out.println(statistics);

            xtcApiClient.updateTestRun(statistics);
        }

        // compress and upload the allure report if available
        System.out.println("Processing Allure results...");
        if (allureReportDir != null)
        {
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

            xtcApiClient.uploadReport(archivePath);
        }

        System.out.println("Remove run ID from system properties on exit...");
        System.clearProperty("xtc.run.id");
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
