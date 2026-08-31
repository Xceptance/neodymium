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
package com.xceptance.aura.test.controller;

import com.xceptance.neodymium.aura.AuraChatSessionService;
import com.xceptance.neodymium.aura.AuraFileService;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.neodymium.aura.AuraReportingService;
import com.xceptance.neodymium.aura.dto.ChatSessionDto;
import com.xceptance.neodymium.aura.dto.YamlFileDto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller rendering the main Aura Test Manager Dashboard view integrated into Aura Report Manager.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Controller
public class AuraTestDashboardController
{
    private final AuraInteractiveService interactiveService;
    private final AuraFileService fileService;
    private final AuraReportingService reportingService;
    private final AuraChatSessionService sessionService;
    private final AuraTestQueueController queueController;

    public AuraTestDashboardController(final AuraInteractiveService interactiveService,
                                        final AuraFileService fileService,
                                        final AuraReportingService reportingService,
                                        final AuraChatSessionService sessionService,
                                        final AuraTestQueueController queueController)
    {
        this.interactiveService = interactiveService;
        this.fileService = fileService;
        this.reportingService = reportingService;
        this.sessionService = sessionService;
        this.queueController = queueController;
    }

    @GetMapping(
    {
      "/aura-test-manager", "/aura-test-manager/history", "/dashboard", "/history"
    })
    public String renderDashboard(
        @RequestParam(value = "view", required = false) final String view,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        final HttpServletRequest request,
        final Model model)
    {
        final List<YamlFileDto> testFiles = fileService.getYamlFilesList();
        final List<Map<String, Object>> runs = AuraTestReportingController.enrichHistoryList(reportingService.getHistoryList());

        final String requestUri = request != null ? request.getRequestURI() : "";
        final boolean isHistoryView = "history".equalsIgnoreCase(view) || requestUri.endsWith("/history");
        final String activeView = isHistoryView ? "history" : "workspace";

        model.addAttribute("pageTitle", "Neodymium Aura Test Manager");
        model.addAttribute("activeTab", "AuraTestManager");
        model.addAttribute("activeView", activeView);
        model.addAttribute("viewFragment", "fragments/aura-test-manager :: auraTestManager");

        model.addAttribute("theme", interactiveService != null ? interactiveService.getActiveTheme() : "dark");
        model.addAttribute("files", testFiles);
        model.addAttribute("testFiles", testFiles);
        model.addAttribute("expandedFiles", fileService.getExpandedFiles());
        model.addAttribute("queue", queueController.getSelectedQueue());
        model.addAttribute("runQueue", queueController.getSelectedQueue());
        model.addAttribute("selectedKeys", queueController.getSelectedQueueKeys());
        model.addAttribute("selectedFileKeys", queueController.getFullySelectedFileKeys(testFiles));
        model.addAttribute("partiallySelectedFileKeys", queueController.getPartiallySelectedFileKeys(testFiles));
        model.addAttribute("selectedBrowser", queueController.getSelectedBrowser());
        model.addAttribute("headless", queueController.isHeadless());
        model.addAttribute("video", queueController.isVideo());
        model.addAttribute("executionMode", queueController.getExecutionMode());
        model.addAttribute("interactive", queueController.isInteractive());
        model.addAttribute("allure", queueController.isAllure());

        model.addAttribute("availableBrowserProfiles", queueController.getAvailableBrowserProfiles());
        model.addAttribute("globalBrowserProfiles", queueController.getGlobalBrowserProfiles());
        model.addAttribute("browserGroups", queueController.getGroupedBrowserProfiles(queueController.getGlobalBrowserProfiles()));
        model.addAttribute("totalRuns", queueController.getTotalExecutionRuns());
        model.addAttribute("queueController", queueController);

        final List<ChatSessionDto> chatSessions = sessionService != null ? sessionService.getSessions() : List.of();
        final String currentSessionId = chatSessions.isEmpty() ? "" : chatSessions.get(0).id;
        model.addAttribute("chatSessions", chatSessions);
        model.addAttribute("currentSessionId", currentSessionId);
        model.addAttribute("chatMessages", chatSessions.isEmpty() ? List.of() : chatSessions.get(0).messages);

        final boolean running = queueController.isRunning();
        model.addAttribute("running", running);
        model.addAttribute("executionInProgress", running);
        model.addAttribute("total", queueController.getGlobalTestsRun());
        model.addAttribute("passed", queueController.getGlobalPassed());
        model.addAttribute("failed", queueController.getGlobalFailed());
        model.addAttribute("skipped", queueController.getGlobalSkipped());
        model.addAttribute("activeEditingFile", fileService.getActiveEditingFile());

        model.addAttribute("history", runs);
        model.addAttribute("reportingHistoryList", runs);

        if (running)
        {
            final LocalDateTime ldt = LocalDateTime.now();
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            model.addAttribute("runningStartTime", ldt.format(formatter));
        }

        if ("true".equals(hxRequest))
        {
            return "fragments/aura-test-manager :: auraTestManager";
        }
        return "index";
    }

    @PostMapping("/api/theme")
    public String changeTheme(@RequestParam(value = "theme", required = false, defaultValue = "dark") final String theme, final Model model)
    {
        if (interactiveService != null)
        {
            interactiveService.setActiveTheme(theme);
        }
        model.addAttribute("theme", theme);
        return "dashboard :: themeContainer";
    }
}
