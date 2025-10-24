package com.xceptance.neodymium.common.xtc;

import com.xceptance.neodymium.common.xtc.dto.CreateRunRequest;
import com.xceptance.neodymium.common.xtc.dto.CreateRunResponse;
import com.xceptance.neodymium.util.AllureAddons;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class RunInitializer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RunInitializer.class);

    public static void main(String[] args) throws IOException, InterruptedException
    {
        LOGGER.info("RunInitializer starting...");

        if (!XtcApiContext.isXtcApiEnabled())
        {
            LOGGER.info("XTC API is disabled. Exiting...");

            if (XtcApiContext.configuration.xtcApiThrowExceptionForRunInitialization())
            {
                throw new RuntimeException("XTC API is disabled and exception asserting this is enabled. Please enable the XTC API in the configuration. " +
                                               "Otherwise, the run will not be created. If this is intended, disable the 'xtc.api.xtcApiThrowExceptionForRunInitialization' " +
                                               "configuration option.");
            }

            return;
        }

        LOGGER.info("Downloading JSON viewer script...");
        AllureAddons.downloadJsonViewerScript();

        LOGGER.info("XtcApiClient starting...");
        XtcApiClient xtcApiClient = new XtcApiClient();

        // do the REST calls to the XTC API
        LOGGER.info("Creating test run...");

        CreateRunRequest createRunRequest = new CreateRunRequest(Instant.now().truncatedTo(ChronoUnit.MILLIS).toString(),
                                                                 XtcApiContext.configuration.xtcApiEstimatedDuration(),
                                                                 XtcApiContext.configuration.xtcApiName(),
                                                                 XtcApiContext.configuration.xtcApiTestInstance(),
                                                                 XtcApiContext.configuration.xtcApiProfile(),
                                                                 XtcApiContext.configuration.xtcApiPipelineLink(),
                                                                 XtcApiContext.configuration.xtcApiBuildNumber(),
                                                                 XtcApiContext.configuration.xtcApiDescription());

        CreateRunResponse createRunResponse = xtcApiClient.createTestRun(createRunRequest);
        String runId = createRunResponse.getData().getIndex();

        LOGGER.info("Adding run ID to system properties: {}", runId);
        System.setProperty("xtc.run.id", runId);

        String filePath = StringUtils.isNotBlank(XtcApiContext.configuration.xtcApiRunIdStorageFilePath())
            ? XtcApiContext.configuration.xtcApiRunIdStorageFilePath()
            : System.getProperty("build.dir") + File.separator + "temp_run_id.txt";

        FileUtils.writeStringToFile(new File(filePath), runId, StandardCharsets.UTF_8);
        LOGGER.info("Created temp file for run ID: {} to {}", runId, filePath);
    }
}
