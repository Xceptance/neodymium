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

import com.xceptance.neodymium.aura.AuraFileService;
import com.xceptance.neodymium.aura.dto.YamlFileDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for test file discovery, tree panel rendering, and expand/collapse toggles.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Controller
public class AuraTestFileController
{
    private final AuraFileService fileService;
    private final AuraTestQueueController queueController;

    public AuraTestFileController(final AuraFileService fileService, final AuraTestQueueController queueService)
    {
        this.fileService = fileService;
        this.queueController = queueService;
    }

    private void populateFileModel(final Model model, final List<YamlFileDto> files)
    {
        model.addAttribute("files", files);
        model.addAttribute("testFiles", files);
        model.addAttribute("expandedFiles", fileService.getExpandedFiles());
        model.addAttribute("selectedKeys", queueController.getSelectedQueueKeys());
        model.addAttribute("selectedFileKeys", queueController.getFullySelectedFileKeys(files));
        model.addAttribute("partiallySelectedFileKeys", queueController.getPartiallySelectedFileKeys(files));
    }

    @GetMapping("/api/files/panel")
    public String getFilesPanel(final HttpServletResponse response, final Model model)
    {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        populateFileModel(model, fileService.getYamlFilesList());
        return "fragments/test-selection :: testSelectionContent";
    }

    @GetMapping({"/api/files", "/api/files/list", "/list"})
    public String getFilesListFragment(final HttpServletResponse response, final Model model)
    {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        populateFileModel(model, fileService.getYamlFilesList());
        return "fragments/test-selection :: yamlFileListContent";
    }

    @GetMapping("/api/files/json")
    @ResponseBody
    public ResponseEntity<List<YamlFileDto>> getFilesJson()
    {
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(fileService.getYamlFilesList());
    }

    @GetMapping("/api/files/search")
    public String searchFiles(@RequestParam(value = "q", required = false, defaultValue = "") final String query,
                              final HttpServletResponse response,
                              final Model model)
    {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        final List<YamlFileDto> filtered = fileService.getFilteredYamlFilesList(query);
        populateFileModel(model, filtered);
        return "fragments/test-selection :: yamlFileListContent";
    }

    @GetMapping("/api/files/expand")
    public String expandDirectoryGet(@RequestParam(value = "file", required = false) final String file,
                                     final HttpServletResponse response,
                                     final Model model)
    {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        if (file != null && !file.isBlank())
        {
            fileService.toggleFileExpansion(file);
        }
        populateFileModel(model, fileService.getYamlFilesList());
        return "fragments/test-selection :: yamlFileListContent";
    }

    @PostMapping({"/api/expand-dir", "/api/files/toggle"})
    public String expandDirectoryPost(
            @RequestParam(value = "file", required = false) final String fileParam,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Model model)
    {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        String file = fileParam;
        if ((file == null || file.isBlank()) && request != null)
        {
            file = request.getParameter("file");
        }
        if (file != null && !file.isBlank())
        {
            fileService.toggleFileExpansion(file);
        }
        populateFileModel(model, fileService.getYamlFilesList());
        return "fragments/test-selection :: yamlFileListContent";
    }
}
