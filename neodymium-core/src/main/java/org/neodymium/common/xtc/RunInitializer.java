/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.common.xtc;

import org.neodymium.common.xtc.dto.CreateRunRequest;
import org.neodymium.common.xtc.dto.CreateRunResponse;
import org.neodymium.util.AllureAddons;
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
