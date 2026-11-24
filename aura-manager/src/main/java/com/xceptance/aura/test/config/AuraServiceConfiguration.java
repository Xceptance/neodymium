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
package com.xceptance.aura.test.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.aura.report.service.RunStorageSyncService;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.xceptance.neodymium.aura.AuraFileService;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.neodymium.aura.AuraQueueService;
import com.xceptance.neodymium.aura.AuraReportingService;
import com.xceptance.neodymium.aura.AuraSettingsService;
import com.xceptance.neodymium.aura.QueueRunProgressListener;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraServiceConfiguration.class);

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
    public AuraQueueService auraQueueService(final AuraReportingService reportingService, final AuraInteractiveService interactiveService,
                                              final RunStorageSyncService runStorageSyncService, final AuraReportDataService reportDataService,
                                              final ConfigurableApplicationContext applicationContext)
    {
        final AuraQueueService queueService = new AuraQueueService(reportingService, interactiveService);
        queueService.setQueueRunProgressListener(new QueueRunProgressListener()
        {
            @Override
            public void onRunStarted(final String runId, final String batchName, final String environment)
            {
                onRunStarted(runId, batchName, environment, null);
            }

            @Override
            public void onRunStarted(final String runId, final String batchName, final String environment, final Map<String, List<String>> tags)
            {
                if (applicationContext != null && applicationContext.isActive())
                {
                    try
                    {
                        reportDataService.startRun(runId, batchName, environment, "queue", tags);
                    }
                    catch (final Exception e)
                    {
                        LOGGER.warn("[Aura Server] Error in onRunStarted for runId {}: {}", runId, e.getMessage());
                    }
                }
            }

            @Override
            public void onTestExecutionCompleted(final String runId, final Map<String, Object> executionData)
            {
                if (applicationContext != null && applicationContext.isActive())
                {
                    try
                    {
                        reportDataService.ingestExecution(runId, executionData);
                    }
                    catch (final Exception e)
                    {
                        LOGGER.warn("[Aura Server] Error in onTestExecutionCompleted for runId {}: {}", runId, e.getMessage());
                    }
                }
            }

            @Override
            public void onRunFinished(final String runId)
            {
                if (applicationContext != null && applicationContext.isActive())
                {
                    try
                    {
                        reportDataService.finishRun(runId);
                    }
                    catch (final Exception e)
                    {
                        LOGGER.warn("[Aura Server] Error in onRunFinished for runId {}: {}", runId, e.getMessage());
                    }
                }
            }
        });
        queueService.setOnRunCompletedListener(runId -> {
            if (applicationContext != null && applicationContext.isActive())
            {
                try
                {
                    runStorageSyncService.importOrUpdateRunReport(runId);
                }
                catch (final Exception e)
                {
                    LOGGER.warn("[Aura Server] Could not import run report for runId {}: {}", runId, e.getMessage());
                }
            }
        });
        final Set<String> syncedActiveRuns = ConcurrentHashMap.newKeySet();
        interactiveService.setStatePushListener(runId -> {
            if (runId == null || runId.isBlank() || applicationContext == null || !applicationContext.isActive())
            {
                return;
            }
            try
            {
                reportDataService.evictRunReportCache(runId);
                boolean shouldSync = syncedActiveRuns.add(runId);
                final InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
                if (engine != null)
                {
                    final String currentJson = engine.getCurrentStateJson();
                    if (currentJson != null && !currentJson.isBlank())
                    {
                        final JsonObject stateObj = JsonParser.parseString(currentJson).getAsJsonObject();
                        final String status = (stateObj.has("status") && !stateObj.get("status").isJsonNull())
                            ? stateObj.get("status").getAsString().trim()
                            : "";
                        final boolean isRunningStatus = "running".equalsIgnoreCase(status)
                            || "in_progress".equalsIgnoreCase(status)
                            || "executing".equalsIgnoreCase(status)
                            || "pending".equalsIgnoreCase(status)
                            || status.isEmpty();
                        if (!isRunningStatus)
                        {
                            shouldSync = true;
                            syncedActiveRuns.remove(runId);
                        }
                    }
                }
                if (shouldSync)
                {
                    runStorageSyncService.importOrUpdateRunReport(runId);
                    reportDataService.evictRunReportCache(runId);
                }
            }
            catch (final Exception e)
            {
                LOGGER.warn("[Aura Server] Could not sync state push for runId {}: {}", runId, e.getMessage());
            }
        });
        return queueService;
    }
}
