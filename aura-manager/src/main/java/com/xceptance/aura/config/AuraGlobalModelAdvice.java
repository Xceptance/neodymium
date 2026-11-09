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
    public void addGlobalAttributes(final Model model)
    {
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
                        Arrays.stream(r.getLocalesCsv().split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(lSet::add);
                    }
                    if (r.getEnvironment() != null && !r.getEnvironment().isBlank())
                    {
                        eSet.add(r.getEnvironment().trim());
                    }
                    if (dataService != null)
                    {
                        dataService.getRunAreas(r).forEach(a -> aSet.add(AuraReportDataService.normalizeArea(a)));
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
                    lSet.addAll(allKnown.get("location"));
                }
                if (allKnown.containsKey("environment"))
                {
                    eSet.addAll(allKnown.get("environment"));
                }
                if (allKnown.containsKey("area"))
                {
                    allKnown.get("area").forEach(a -> aSet.add(AuraReportDataService.normalizeArea(a)));
                }
            }
            if (bSet.isEmpty())
            {
                bSet.add("Chrome");
            }
            if (lSet.isEmpty())
            {
                lSet.add("Default");
            }
            if (eSet.isEmpty())
            {
                eSet.add("Default");
            }
            if (aSet.isEmpty())
            {
                aSet.add("@Browsing (default)");
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
            model.addAttribute("candidateLocales", new ArrayList<>(lSet));
            model.addAttribute("candidateEnvs", new ArrayList<>(eSet));
            model.addAttribute("candidateAreas", new ArrayList<>(aSet));
            model.addAttribute("availableIdentityTags", availableIdentityTags);
            model.addAttribute("candidateCustomTags", candidateCustomTags);
        }
        if (queueService != null && model != null)
        {
            final String activeRunId = queueService.getCurrentRunId();
            final boolean running = queueService.isRunningQueue();
            final String currentBatch = queueService.getCurrentBatchName();
            final String effectiveBatch = (currentBatch != null && !currentBatch.isBlank()) ? currentBatch : "Default";
            if (activeRunId != null && !activeRunId.isEmpty())
            {
                model.addAttribute("activeRunId", activeRunId);
                model.addAttribute("activeBatchName", effectiveBatch);
                model.addAttribute("activeRunStatus", running ? "running" : "finished");
                model.addAttribute("activeRunReportUrl", "/run-report?runId=" + activeRunId);
                model.addAttribute("hasRunContext", true);
            }
            else
            {
                model.addAttribute("activeRunId", "");
                model.addAttribute("activeBatchName", "Default");
                model.addAttribute("activeRunStatus", "");
                model.addAttribute("activeRunReportUrl", "");
                model.addAttribute("hasRunContext", false);
            }
            model.addAttribute("running", running);
            model.addAttribute("hasParseError", false);
            model.addAttribute("parseErrorMessage", "");
            model.addAttribute("editorMode", "visual");
        }
    }
}
