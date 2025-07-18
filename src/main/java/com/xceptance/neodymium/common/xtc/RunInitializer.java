package com.xceptance.neodymium.common.xtc;

import com.xceptance.neodymium.common.xtc.config.XtcApiConfiguration;
import com.xceptance.neodymium.common.xtc.dto.CreateRunRequest;
import com.xceptance.neodymium.common.xtc.dto.CreateRunResponse;
import org.aeonbits.owner.ConfigFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class RunInitializer
{
    public static XtcApiConfiguration configuration = ConfigFactory.create(XtcApiConfiguration.class);

    public static void main(String[] args) throws IOException, InterruptedException
    {
        if (!configuration.xtcApiIsEnabled())
        {
            System.out.println("XTC API is disabled. Exiting...");
            return;
        }

        System.out.println("RunInitializer starting...");

        // initialize
        System.out.println("XtcApiClient starting...");
        XtcApiClient xtcApiClient = new XtcApiClient(configuration.xtcApiOrganization(),
                                                     configuration.xtcApiProject(),
                                                     configuration.xtcApiKey(),
                                                     configuration.xtcApiSecret());

        // do the REST calls to the XTC API
        System.out.println("Creating test run...");

        CreateRunRequest createRunRequest = new CreateRunRequest(Instant.now().truncatedTo(ChronoUnit.MILLIS).toString(),
                                                                 configuration.xtcApiEstimatedDuration(),
                                                                 configuration.xtcApiName(),
                                                                 configuration.xtcApiTestInstance(),
                                                                 configuration.xtcApiProfile(),
                                                                 configuration.xtcApiPipelineLink(),
                                                                 configuration.xtcApiBuildNumber(),
                                                                 configuration.xtcApiDescription());

        CreateRunResponse createRunResponse = xtcApiClient.createTestRun(createRunRequest);
        String runId = createRunResponse.getData().getIndex();

        System.out.println("Adding run ID to system properties...");
        System.setProperty("xtc.run.id", runId);
    }
}
