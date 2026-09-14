/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.aura.test.config;

import com.xceptance.neodymium.aura.AuraChatService;
import com.xceptance.neodymium.aura.AuraChatSessionService;
import com.xceptance.neodymium.aura.AuraFileService;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.neodymium.aura.AuraQueueService;
import com.xceptance.neodymium.aura.AuraReportingService;
import com.xceptance.neodymium.aura.AuraSettingsService;
import com.xceptance.neodymium.aura.QueueRunProgressListener;
import com.xceptance.aura.report.service.AuraReportDataService;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration providing canonical Neodymium Aura service singletons to test manager controllers.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Configuration
public class AuraServiceConfiguration
{
    @Bean
    public AuraFileService auraFileService()
    {
        return new AuraFileService();
    }

    @Bean
    public AuraSettingsService auraSettingsService()
    {
        return new AuraSettingsService();
    }

    @Bean
    public AuraChatService auraChatService(final AuraFileService fileService)
    {
        return new AuraChatService(fileService);
    }

    @Bean
    public AuraChatSessionService auraChatSessionService()
    {
        return new AuraChatSessionService();
    }

    @Bean
    public AuraInteractiveService auraInteractiveService()
    {
        return new AuraInteractiveService();
    }

    @Bean
    public AuraReportingService auraReportingService(final AuraInteractiveService interactiveService)
    {
        final AuraReportingService reportingService = new AuraReportingService();
        reportingService.setInteractiveService(interactiveService);
        return reportingService;
    }

    @Bean
    public AuraQueueService auraQueueService(final AuraReportingService reportingService, final AuraInteractiveService interactiveService, final AuraReportDataService reportDataService)
    {
        final AuraQueueService queueService = new AuraQueueService(reportingService, interactiveService);
        queueService.setQueueRunProgressListener(new QueueRunProgressListener()
        {
            @Override
            public void onRunStarted(final String runId, final String batchName, final String environment)
            {
                reportDataService.startRun(runId, batchName, environment, "queue");
            }

            @Override
            public void onTestExecutionCompleted(final String runId, final Map<String, Object> executionData)
            {
                reportDataService.ingestExecution(runId, executionData);
            }

            @Override
            public void onRunFinished(final String runId)
            {
                reportDataService.finishRun(runId);
            }
        });
        return queueService;
    }
}
