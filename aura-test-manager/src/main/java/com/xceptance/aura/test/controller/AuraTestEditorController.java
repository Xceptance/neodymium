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
import com.xceptance.neodymium.aura.dto.SaveRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for reading, editing, saving, and creating test playbook files.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Controller
public class AuraTestEditorController
{
    private final AuraFileService fileService;

    public AuraTestEditorController(final AuraFileService fileService)
    {
        this.fileService = fileService;
    }

    @GetMapping("/api/editor")
    public String getEditorFragment(@RequestParam(value = "file", required = false) final String relativePath, final Model model)
    {
        if (relativePath != null && !relativePath.isBlank())
        {
            String content = "";
            try
            {
                content = fileService.readYamlFileContent(relativePath);
            }
            catch (final Exception ignored)
            {
            }
            model.addAttribute("activeEditingFile", relativePath);
            model.addAttribute("editingFileContent", content);
            model.addAttribute("currentTestFile", relativePath);
            model.addAttribute("fileContent", content);
        }
        else
        {
            model.addAttribute("activeEditingFile", "");
            model.addAttribute("editingFileContent", "");
            model.addAttribute("currentTestFile", "");
            model.addAttribute("fileContent", "");
        }
        return "fragments/editor :: editorPanelContent";
    }

    @GetMapping("/api/read")
    @ResponseBody
    public ResponseEntity<Map<String, String>> readFile(@RequestParam("file") final String relativePath)
    {
        String content = "";
        try
        {
            content = fileService.readYamlFileContent(relativePath);
        }
        catch (final Exception ignored)
        {
        }
        final Map<String, String> response = new HashMap<>();
        response.put("file", relativePath);
        response.put("content", content);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveFile(@RequestBody final SaveRequest saveRequest)
    {
        boolean success = false;
        try
        {
            fileService.saveYamlFileContent(saveRequest.file, saveRequest.content);
            success = true;
        }
        catch (final Exception ignored)
        {
        }
        final Map<String, Object> response = new HashMap<>();
        response.put("status", success ? "SUCCESS" : "ERROR");
        response.put("file", saveRequest.file);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteFile(@RequestParam("file") final String relativePath)
    {
        boolean success = false;
        try
        {
            success = fileService.deleteYamlFile(relativePath);
        }
        catch (final Exception ignored)
        {
        }
        final Map<String, Object> response = new HashMap<>();
        response.put("status", success ? "SUCCESS" : "ERROR");
        return ResponseEntity.ok(response);
    }
}
