package com.xceptance.neodymium.common.xtc;

import com.xceptance.neodymium.common.xtc.config.XtcApiConfiguration;
import org.aeonbits.owner.ConfigFactory;

public class RunInitializer
{
    public static XtcApiConfiguration configuration = ConfigFactory.create(XtcApiConfiguration.class);

    public static void main(String[] args)
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
        xtcApiClient.authenticate();

        System.out.println("Creating test run...");
        String runId = xtcApiClient.createTestRun();

        System.out.println("Adding run ID to system properties...");
        System.setProperty("xtc.run.id", runId);
    }
}
