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
package com.xceptance.aura.config;

import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestRunRepository;
import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.aura.report.service.ReportDashboardSettingService;
import com.xceptance.neodymium.aura.AuraQueueService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Controller advice providing global model attributes for active run context and custom dashboard pages
 * across all Aura Manager views.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@ControllerAdvice(basePackages = "com.xceptance.aura")
public class AuraGlobalModelAdvice
{
    private final AuraQueueService queueService;
    private final ReportDashboardSettingService dashboardSettingService;
    private final TestRunRepository runRepository;
    private final AuraReportDataService dataService;

    @Autowired
    public AuraGlobalModelAdvice(
        final AuraQueueService queueService,
        @Autowired(required = false) final ReportDashboardSettingService dashboardSettingService,
        @Autowired(required = false) final TestRunRepository runRepository,
        @Autowired(required = false) final AuraReportDataService dataService)
    {
        this.queueService = queueService;
        this.dashboardSettingService = dashboardSettingService;
        this.runRepository = runRepository;
        this.dataService = dataService;
    }

    public AuraGlobalModelAdvice(
        final AuraQueueService queueService,
        final ReportDashboardSettingService dashboardSettingService)
    {
        this(queueService, dashboardSettingService, null, null);
    }

    public AuraGlobalModelAdvice(final AuraQueueService queueService)
    {
        this(queueService, null, null, null);
    }

    @ModelAttribute
    public void addGlobalAttributes(
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        final Model model)
    {
        if (model != null)
        {
            final boolean isHxRequest = hxRequest != null && !hxRequest.isBlank();
            model.addAttribute("isHxRequest", isHxRequest);
        }
        if (dashboardSettingService != null && model != null)
        {
            model.addAttribute("customPages", dashboardSettingService.getCustomPages());
            model.addAttribute("rootCustomPages", dashboardSettingService.getRootCustomPages());
            model.addAttribute("defaultWidgetsCount", dashboardSettingService.getDashboardWidgets().size());
        }
        else if (model != null)
        {
            model.addAttribute("customPages", Collections.emptyList());
            model.addAttribute("rootCustomPages", Collections.emptyList());
            model.addAttribute("defaultWidgetsCount", 0);
        }
        if (model != null && !model.containsAttribute("candidateBrowsers"))
        {
            final Set<String> bSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            final Set<String> lSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            final Set<String> eSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            final Set<String> aSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            final List<String> availableIdentityTags = new ArrayList<>();

            if (runRepository != null)
            {
                final List<TestRunEntity> allRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();
                for (final TestRunEntity r : allRuns)
                {
                    if (r.getBrowsersCsv() != null && !r.getBrowsersCsv().isBlank())
                    {
                        Arrays.stream(r.getBrowsersCsv().split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(bSet::add);
                    }
                    if (r.getLocalesCsv() != null && !r.getLocalesCsv().isBlank())
                    {
                        Arrays.stream(r.getLocalesCsv().split(","))
                            .map(String::trim)
                            .filter(AuraReportDataService::isRealLocale)
                            .forEach(lSet::add);
                    }
                    if (r.getEnvironment() != null && AuraReportDataService.isRealEnvironment(r.getEnvironment()))
                    {
                        eSet.add(r.getEnvironment().trim());
                    }
                    if (dataService != null)
                    {
                        dataService.getRunAreas(r).forEach(a -> {
                            if (AuraReportDataService.isRealArea(a))
                            {
                                aSet.add(AuraReportDataService.normalizeArea(a));
                            }
                        });
                    }
                }
            }
            if (dataService != null)
            {
                final Map<String, List<String>> allKnown = dataService.getAllKnownTagMap();
                if (allKnown.containsKey("browser"))
                {
                    bSet.addAll(allKnown.get("browser"));
                }
                if (allKnown.containsKey("location"))
                {
                    allKnown.get("location").forEach(loc -> {
                        if (AuraReportDataService.isRealLocale(loc))
                        {
                            lSet.add(loc.trim());
                        }
                    });
                }
                if (allKnown.containsKey("environment"))
                {
                    allKnown.get("environment").forEach(env -> {
                        if (AuraReportDataService.isRealEnvironment(env))
                        {
                            eSet.add(env.trim());
                        }
                    });
                }
                if (allKnown.containsKey("area"))
                {
                    allKnown.get("area").forEach(a -> {
                        if (AuraReportDataService.isRealArea(a))
                        {
                            aSet.add(AuraReportDataService.normalizeArea(a));
                        }
                    });
                }
            }
            if (bSet.isEmpty())
            {
                bSet.add("Chrome");
            }
            final List<String> candidateLocales = new ArrayList<>(lSet);
            final List<String> candidateEnvs = new ArrayList<>(eSet);
            final List<String> candidateAreas = new ArrayList<>();
            for (final String a : aSet)
            {
                if (AuraReportDataService.isRealArea(a))
                {
                    candidateAreas.add(a);
                }
            }

            if (dataService != null)
            {
                for (final String key : dataService.getAllTagKeys())
                {
                    if (key != null && !key.isBlank() && !"batch".equalsIgnoreCase(key) && !availableIdentityTags.contains(key.trim()))
                    {
                        availableIdentityTags.add(key.trim());
                    }
                }
            }
            availableIdentityTags.sort(String.CASE_INSENSITIVE_ORDER);

            final Map<String, List<String>> candidateCustomTags = (dataService != null)
                ? dataService.getCandidateCustomTags()
                : Collections.emptyMap();

            model.addAttribute("candidateBrowsers", new ArrayList<>(bSet));
            model.addAttribute("candidateLocales", candidateLocales);
            model.addAttribute("candidateEnvs", candidateEnvs);
            model.addAttribute("candidateAreas", candidateAreas);
            model.addAttribute("availableIdentityTags", availableIdentityTags);
            model.addAttribute("candidateCustomTags", candidateCustomTags);
        }
        if (model != null)
        {
            final boolean isQueueRunning = queueService != null && queueService.isRunningQueue();
            String activeRunId = isQueueRunning ? queueService.getCurrentRunId() : null;
            boolean running = isQueueRunning;
            String currentBatch = isQueueRunning ? queueService.getCurrentBatchName() : null;
            Integer executedCount = isQueueRunning ? queueService.getGlobalTestsRun() : null;
            Integer scheduledCount = null;
            if (isQueueRunning && queueService.getLastRunRequest() != null)
            {
                if (queueService.getLastRunRequest().datasets != null)
                {
                    scheduledCount = queueService.getLastRunRequest().datasets.size();
                }
                if (currentBatch == null || !AuraReportDataService.isRealBatch(currentBatch))
                {
                    currentBatch = AuraReportDataService.resolveEffectiveBatchNameFromMap(currentBatch, queueService.getLastRunRequest().tags);
                }
            }

            final List<TestRunEntity> allRuns = runRepository != null
                ? runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()
                : (dataService != null ? dataService.getAllRuns() : Collections.emptyList());
            if (dataService != null && !allRuns.isEmpty())
            {
                dataService.populateRunTags(allRuns);
            }

            final String queueRunId = activeRunId;
            TestRunEntity activeEntity = null;
            if (queueRunId != null && !queueRunId.isBlank())
            {
                activeEntity = allRuns.stream()
                    .filter(r -> queueRunId.equals(r.getId()))
                    .findFirst()
                    .orElse(null);
            }
            if (activeEntity == null)
            {
                activeEntity = allRuns.stream()
                    .filter(r -> "IN_PROGRESS".equalsIgnoreCase(r.getStatus()))
                    .findFirst()
                    .orElse(null);
            }
            if (activeEntity == null && dataService != null)
            {
                activeEntity = allRuns.stream()
                    .filter(r -> dataService.isRunCurrentlyActiveOnDisk(r.getId()))
                    .findFirst()
                    .orElse(null);
            }

            String progressText = "";
            if (activeEntity != null)
            {
                if (activeRunId == null || activeRunId.isBlank())
                {
                    activeRunId = activeEntity.getId();
                }
                running = isQueueRunning || "IN_PROGRESS".equalsIgnoreCase(activeEntity.getStatus())
                    || (dataService != null && dataService.isRunCurrentlyActiveOnDisk(activeEntity.getId()));
                if (currentBatch == null || !AuraReportDataService.isRealBatch(currentBatch))
                {
                    currentBatch = AuraReportDataService.resolveEffectiveBatchName(activeEntity.getBatchName(), activeEntity.getTags());
                    if ((currentBatch == null || !AuraReportDataService.isRealBatch(currentBatch)) && dataService != null)
                    {
                        currentBatch = AuraReportDataService.resolveEffectiveBatchNameFromMap(activeEntity.getBatchName(), dataService.getRunTagsMap(activeEntity.getId()));
                    }
                }

                final int runningCount = activeEntity.getRunningCountSafe();
                final int total = activeEntity.getTotalTestsSafe();
                final int entityExecuted = Math.max(0, total - runningCount);
                if (executedCount == null || entityExecuted > executedCount)
                {
                    executedCount = entityExecuted;
                }
                if (scheduledCount == null && total > 0)
                {
                    scheduledCount = total;
                }

                if (running)
                {
                    if (scheduledCount != null && scheduledCount > 0)
                    {
                        if (runningCount > 0)
                        {
                            progressText = executedCount + " of " + scheduledCount + " tests (" + runningCount + " running)";
                        }
                        else
                        {
                            progressText = executedCount + " of " + scheduledCount + " tests";
                        }
                    }
                    else if (runningCount > 0)
                    {
                        progressText = executedCount + " executed (" + runningCount + " running)";
                    }
                    else
                    {
                        progressText = executedCount + " executed";
                    }
                }
                else
                {
                    progressText = executedCount + " tests completed";
                }
            }
            else if (running)
            {
                if (executedCount == null)
                {
                    executedCount = 0;
                }
                if (scheduledCount != null && scheduledCount > 0)
                {
                    progressText = executedCount + " of " + scheduledCount + " tests";
                }
                else
                {
                    progressText = executedCount + " executed";
                }
            }
            else if (!allRuns.isEmpty())
            {
                final TestRunEntity latestRun = allRuns.get(0);
                activeRunId = latestRun.getId();
                running = false;
                currentBatch = AuraReportDataService.resolveEffectiveBatchName(latestRun.getBatchName(), latestRun.getTags());
                if ((currentBatch == null || !AuraReportDataService.isRealBatch(currentBatch)) && dataService != null)
                {
                    currentBatch = AuraReportDataService.resolveEffectiveBatchNameFromMap(latestRun.getBatchName(), dataService.getRunTagsMap(latestRun.getId()));
                }
                executedCount = latestRun.getTotalTestsSafe();
                scheduledCount = latestRun.getTotalTestsSafe();
                progressText = executedCount + " tests completed";
            }

            final String effectiveBatch = (currentBatch != null && AuraReportDataService.isRealBatch(currentBatch)) ? currentBatch.trim() : "";
            if (activeRunId != null && !activeRunId.isEmpty())
            {
                model.addAttribute("activeRunId", activeRunId);
                model.addAttribute("activeBatchName", effectiveBatch);
                model.addAttribute("activeRunStatus", running ? "running" : "finished");
                model.addAttribute("activeRunReportUrl", "/run-report?runId=" + activeRunId);
                model.addAttribute("activeRunProgress", progressText);
                model.addAttribute("activeRunExecutedCount", executedCount);
                model.addAttribute("activeRunScheduledCount", scheduledCount);
                model.addAttribute("hasRunContext", true);
            }
            else
            {
                model.addAttribute("activeRunId", "");
                model.addAttribute("activeBatchName", "");
                model.addAttribute("activeRunStatus", "");
                model.addAttribute("activeRunReportUrl", "");
                model.addAttribute("activeRunProgress", "");
                model.addAttribute("activeRunExecutedCount", 0);
                model.addAttribute("activeRunScheduledCount", 0);
                model.addAttribute("hasRunContext", false);
            }
            model.addAttribute("running", running);
            model.addAttribute("hasParseError", false);
            model.addAttribute("parseErrorMessage", "");
            model.addAttribute("editorMode", "visual");
        }
    }

    /**
     * Backward-compatible overload for {@link #addGlobalAttributes(String, Model)} without HTMX request header.
     *
     * @param model Spring MVC model
     */
    public void addGlobalAttributes(final Model model)
    {
        addGlobalAttributes(null, model);
    }
}
