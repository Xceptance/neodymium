package com.xceptance.neodymium.common.xtc;

import com.xceptance.neodymium.common.xtc.dto.CreateRunRequest;
import com.xceptance.neodymium.common.xtc.dto.CreateRunResponse;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class RunInitializer
{
    public static void main(String[] args) throws IOException, InterruptedException
    {
        System.out.println("RunInitializer starting...");

        if (!XtcApiContext.isXtcApiEnabled())
        {
            System.out.println("XTC API is disabled. Exiting...");
            return; // TODO throw an exception?
        }
        XtcApiContext.ensureRequiredConfiguration();

        System.out.println("XtcApiClient starting...");
        XtcApiClient xtcApiClient = new XtcApiClient();

        // do the REST calls to the XTC API
        System.out.println("Creating test run...");

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

        System.out.println("Adding run ID to system properties...");
        System.setProperty("xtc.run.id", runId);
    }
}
