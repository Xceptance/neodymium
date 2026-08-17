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
package com.xceptance.aura.report.controller;

import com.xceptance.aura.report.dto.TestClassInfoDto;
import com.xceptance.aura.report.service.NeodymiumAuraFileService;
import com.xceptance.aura.report.service.NeodymiumAuraQueueService;
import com.xceptance.aura.report.service.NeodymiumAuraSettingsService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Spring MVC Controller providing native view routes and HTMX endpoints for the Neodymium Aura Test Manager.
 *
 * @author Xceptance GmbH 2026
 */
@Controller
public class NeodymiumAuraManagerViewController
{
    private final NeodymiumAuraQueueService queueService;
    private final NeodymiumAuraFileService fileService;
    private final NeodymiumAuraSettingsService settingsService;

    public NeodymiumAuraManagerViewController(
        final NeodymiumAuraQueueService queueService,
        final NeodymiumAuraFileService fileService,
        final NeodymiumAuraSettingsService settingsService)
    {
        this.queueService = queueService;
        this.fileService = fileService;
        this.settingsService = settingsService;
    }

    @GetMapping({"/aura-test-manager", "/fragments/aura-test-manager"})
    public String auraTestManager(
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        final Model model)
    {
        populateModel(model, "");
        model.addAttribute("pageTitle", "Neodymium Aura Test Manager");
        model.addAttribute("activeTab", "AuraTestManager");
        model.addAttribute("viewFragment", "fragments/aura-test-manager :: auraTestManager");

        if ("true".equals(hxRequest)) {
            return "fragments/aura-test-manager :: auraTestManager";
        }
        return "index";
    }

    @PostMapping("/aura-test-manager/queue/run")
    public String startQueueRun(
        @RequestParam(name = "suiteName", defaultValue = "ALL") final String suiteName,
        @RequestParam(name = "environment", required = false) final String environment,
        @RequestParam(name = "browser", required = false) final String browser,
        final Model model)
    {
        queueService.startRun(suiteName, environment, browser);
        populateModel(model, "");
        return "fragments/aura-test-manager :: auraTestManager";
    }

    @PostMapping("/aura-test-manager/queue/stop")
    public String stopQueueRun(final Model model)
    {
        queueService.stopRun();
        populateModel(model, "");
        return "fragments/aura-test-manager :: auraTestManager";
    }

    @PostMapping("/aura-test-manager/queue/clear-logs")
    public String clearQueueLogs(final Model model)
    {
        queueService.clearLogs();
        populateModel(model, "");
        return "fragments/aura-test-manager :: auraTestManager";
    }

    @GetMapping("/aura-test-manager/queue/live-data")
    public String getLiveConsoleData(final Model model)
    {
        model.addAttribute("queueData", queueService.getLiveData());
        return "fragments/aura-test-manager :: liveConsolePartial";
    }

    @GetMapping("/aura-test-manager/editor/file")
    public String loadEditorFile(
        @RequestParam(name = "filePath", required = false) final String filePath,
        final Model model)
    {
        populateModel(model, filePath);
        return "fragments/aura-test-manager :: editorPartial";
    }

    @PostMapping("/aura-test-manager/editor/save")
    public String saveEditorFile(
        @RequestParam(name = "filePath") final String filePath,
        @RequestParam(name = "content") final String content,
        final Model model)
    {
        final boolean saved = fileService.saveFileContent(filePath, content);
        model.addAttribute("saveSuccess", saved);
        populateModel(model, filePath);
        return "fragments/aura-test-manager :: editorPartial";
    }

    @PostMapping("/aura-test-manager/settings/save")
    public String saveSettings(
        @RequestParam(name = "environment", required = false) final String environment,
        @RequestParam(name = "browser", required = false) final String browser,
        @RequestParam(name = "headless", required = false) final Boolean headless,
        @RequestParam(name = "threads", required = false) final Integer threads,
        final Model model)
    {
        settingsService.updateSettings(environment, browser, headless, threads);
        populateModel(model, "");
        model.addAttribute("settingsSuccess", true);
        return "fragments/aura-test-manager :: auraTestManager";
    }

    private void populateModel(final Model model, final String selectedFilePath)
    {
        final Map<String, Object> queueData = queueService.getLiveData();
        final List<String> files = fileService.listTestDataFiles();
        final List<TestClassInfoDto> detectedTests = fileService.detectTestClasses();
        final Map<String, Object> settings = settingsService.getSettings();

        final String activeFile = (selectedFilePath != null && !selectedFilePath.isEmpty()) ? selectedFilePath :
                                 (!files.isEmpty() ? files.get(0) : "");
        final String fileContent = fileService.readFileContent(activeFile);

        model.addAttribute("queueData", queueData);
        model.addAttribute("testFiles", files);
        model.addAttribute("detectedTestClasses", detectedTests);
        model.addAttribute("selectedFilePath", activeFile);
        model.addAttribute("fileContent", fileContent);
        model.addAttribute("settings", settings);
    }
}
