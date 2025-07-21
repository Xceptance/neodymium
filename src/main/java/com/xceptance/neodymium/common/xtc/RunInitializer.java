package com.xceptance.neodymium.common.xtc;

import com.xceptance.neodymium.common.xtc.dto.CreateRunRequest;
import com.xceptance.neodymium.common.xtc.dto.CreateRunResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
            return; // TODO throw an exception?
        }
        XtcApiContext.ensureRequiredConfiguration();

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
    }
}
