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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xceptance.neodymium.aura.AuraFileService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraTestEditorController.class);

    private final AuraFileService fileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuraTestEditorController(final AuraFileService fileService)
    {
        this.fileService = fileService;
    }

    @GetMapping("/api/editor")
    public String getEditorFragment(@RequestParam(value = "file", required = false) final String relativePath, final Model model)
    {
        model.addAttribute("stepsFiles", fileService.getStepsFilesList());
        model.addAttribute("fragmentRequiredVarsMap", fileService.getFragmentRequiredVariablesMap());
        if (relativePath != null && !relativePath.isBlank())
        {
            fileService.setActiveEditingFile(relativePath);
            String content = "";
            try
            {
                content = fileService.readYamlFileContent(relativePath);
            }
            catch (final Exception ignored)
            {
            }
            final boolean isFragment = relativePath.toLowerCase().endsWith(".steps");
            model.addAttribute("activeEditingFile", relativePath);
            model.addAttribute("editingFileContent", content);
            model.addAttribute("currentTestFile", relativePath);
            model.addAttribute("fileContent", content);
            model.addAttribute("isFragment", isFragment);

            final Map<String, Object> sections = fileService.parsePlaybookSections(content);
            final boolean hasParseError = Boolean.TRUE.equals(sections.get("hasError"));
            final String parseErrorMessage = (String) sections.get("error");
            model.addAttribute("hasParseError", hasParseError);
            model.addAttribute("parseErrorMessage", parseErrorMessage != null ? parseErrorMessage : "");
            model.addAttribute("editorMode", hasParseError ? "raw" : "visual");
            model.addAttribute("beforeSteps", sections.get("beforeSteps"));
            model.addAttribute("mainSteps", sections.get("mainSteps"));
            model.addAttribute("afterSteps", sections.get("afterSteps"));
            model.addAttribute("dataMatrix", sections.get("dataMatrix"));
            model.addAttribute("varKeys", sections.get("varKeys"));
            model.addAttribute("fragmentVarScopes", sections.get("fragmentVarScopes"));
            model.addAttribute("yamlFiles", fileService.getYamlFilesList());
        }
        else
        {
            fileService.setActiveEditingFile("");
            model.addAttribute("activeEditingFile", "");
            model.addAttribute("editingFileContent", "");
            model.addAttribute("currentTestFile", "");
            model.addAttribute("fileContent", "");
            model.addAttribute("isFragment", false);
            model.addAttribute("hasParseError", false);
            model.addAttribute("parseErrorMessage", "");
            model.addAttribute("editorMode", "visual");
            model.addAttribute("beforeSteps", List.of());
            model.addAttribute("mainSteps", List.of());
            model.addAttribute("afterSteps", List.of());
            model.addAttribute("dataMatrix", List.of());
            model.addAttribute("varKeys", List.of());
            model.addAttribute("fragmentVarScopes", Map.of());
            model.addAttribute("yamlFiles", fileService.getYamlFilesList());
        }
        return "fragments/editor :: editorPanelContent";
    }

    @GetMapping("/api/editor/steps-files")
    @ResponseBody
    public ResponseEntity<List<String>> getStepsFiles(@RequestParam(value = "q", required = false) final String query)
    {
        if (query != null && !query.isBlank())
        {
            return ResponseEntity.ok(fileService.getFilteredStepsFilesList(query));
        }
        return ResponseEntity.ok(fileService.getStepsFilesList());
    }

    @GetMapping("/api/editor/include-tree")
    public String getIncludeTreeFragment(@RequestParam("file") final String relativePath,
                                         @RequestParam("cardId") final String cardId,
                                         final Model model)
    {
        final String cleanPath = relativePath != null ? relativePath.trim().replaceAll("^[\"']|[\"']$", "") : "";
        String content = null;
        boolean fileExists = false;
        try
        {
            content = fileService.readYamlFileContent(cleanPath);
            fileExists = (content != null);
        }
        catch (final Exception ignored)
        {
        }

        final Map<String, Object> sections = fileService.parsePlaybookSections(content != null ? content : "");
        @SuppressWarnings("unchecked")
        final List<String> includeSteps = (List<String>) sections.getOrDefault("mainSteps", List.of());

        model.addAttribute("cardId", cardId);
        model.addAttribute("includeFile", cleanPath);
        model.addAttribute("fileExists", fileExists);
        model.addAttribute("includeSteps", includeSteps);

        return "fragments/editor :: includeTreeCardFragment";
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
    public ResponseEntity<Map<String, Object>> saveFile(@RequestParam("file") final String file,
                                                       @RequestParam(value = "content", required = false) final String content)
    {
        boolean success = false;
        if (file != null && !file.isBlank())
        {
            try
            {
                fileService.saveYamlFileContent(file, content != null ? content : "");
                success = true;
            }
            catch (final Exception ignored)
            {
            }
        }
        final Map<String, Object> response = new HashMap<>();
        response.put("status", success ? "SUCCESS" : "ERROR");
        response.put("file", file);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createFile(@RequestParam(value = "name", required = false) final String name)
    {
        final Map<String, Object> response = new HashMap<>();
        if (name == null || name.isBlank())
        {
            response.put("error", "File name must not be empty.");
            return ResponseEntity.badRequest().body(response);
        }
        try
        {
            final String sanitizedName = (name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".steps"))
                    ? name
                    : name + ".yaml";
            fileService.createYamlFile(name);
            fileService.setActiveEditingFile(sanitizedName);
            response.put("file", sanitizedName);
            return ResponseEntity.ok(response);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to create file '{}'", name, e);
            response.put("error", "Failed to create file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/api/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteFile(
            @RequestParam(value = "file", required = false) final String fileParam,
            final HttpServletRequest request)
    {
        String relativePath = fileParam;
        if ((relativePath == null || relativePath.isBlank()) && request != null)
        {
            relativePath = request.getParameter("file");
        }
        if ((relativePath == null || relativePath.isBlank()) && request != null && request.getContentType() != null && request.getContentType().contains("application/json"))
        {
            try
            {
                @SuppressWarnings("unchecked")
                final Map<String, Object> body = objectMapper.readValue(request.getInputStream(), Map.class);
                if (body != null && body.containsKey("file"))
                {
                    relativePath = String.valueOf(body.get("file"));
                }
            }
            catch (final Exception e)
            {
                LOGGER.debug("Could not parse JSON payload for deleteFile", e);
            }
        }

        boolean success = false;
        if (relativePath != null && !relativePath.isBlank())
        {
            try
            {
                success = fileService.deleteYamlFile(relativePath);
                if (relativePath.equals(fileService.getActiveEditingFile()))
                {
                    fileService.setActiveEditingFile("");
                }
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to delete test file '{}'", relativePath, e);
            }
        }
        final Map<String, Object> response = new HashMap<>();
        response.put("status", success ? "SUCCESS" : "ERROR");
        response.put("file", relativePath != null ? relativePath : "");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/editor/close")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> closeEditor()
    {
        fileService.setActiveEditingFile("");
        final Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        return ResponseEntity.ok(response);
    }
}
