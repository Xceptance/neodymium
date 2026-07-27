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
package com.xceptance.neodymium.aura;

import com.sun.net.httpserver.HttpExchange;
import com.xceptance.neodymium.aura.dto.SaveRequest;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.Context;

/**
 * Controller handling Yaml editor rendering, file reading, saving, creating, and deleting operations.
 * Supports both HTMX fragment responses and legacy REST JSON payloads.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraManagerEditorController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraManagerEditorController.class);

    private final AuraFileService fileService;
    private final NeodymiumAuraManager manager;

    public AuraManagerEditorController(final AuraFileService fileService, final NeodymiumAuraManager manager)
    {
        this.fileService = fileService;
        this.manager = manager;
    }

    public void handleEditorPanel(final HttpExchange exchange) throws IOException
    {
        final String query = exchange.getRequestURI().getQuery();
        String file = null;
        if (query != null)
        {
            for (final String param : query.split("&"))
            {
                final String[] pair = param.split("=");
                if (pair.length > 1 && "file".equals(pair[0]))
                {
                    file = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    break;
                }
            }
        }

        if (file == null || file.trim().isEmpty())
        {
            AuraHttpUtils.sendError(exchange, 400, "Missing 'file' parameter");
            return;
        }

        try
        {
            final Context context = new Context();
            final String fragmentPath = getEditorPanel(file, context);
            final String[] parts = fragmentPath.split("::");
            final String template = parts[0].trim();
            final Set<String> fragments = Set.of(parts[1].trim());

            final String html = manager.getTemplateEngine().process(template, fragments, context);
            AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
        }
        catch (final SecurityException e)
        {
            LOGGER.error("[Aura Server] Security Exception in handleEditorPanel: {}", e.getMessage());
            AuraHttpUtils.sendError(exchange, 403, e.getMessage());
        }
        catch (final Exception e)
        {
            LOGGER.error("[Aura Server] Exception in handleEditorPanel: {}", e.getMessage(), e);
            AuraHttpUtils.sendError(exchange, 500, e.getMessage());
        }
    }

    public void handleReadFile(final HttpExchange exchange) throws IOException
    {
        final String query = exchange.getRequestURI().getQuery();
        String filename = null;
        if (query != null)
        {
            for (final String param : query.split("&"))
            {
                final String[] pair = param.split("=");
                if (pair.length > 1 && "file".equals(pair[0]))
                {
                    filename = pair[1];
                    break;
                }
            }
        }

        if (filename == null || filename.trim().isEmpty())
        {
            LOGGER.error("[Aura Server] Read file request failed: Missing 'file' parameter");
            AuraHttpUtils.sendError(exchange, 400, "Missing 'file' parameter");
            return;
        }

        try
        {
            final String content = fileService.readYamlFileContent(filename);
            final Map<String, String> response = new HashMap<>();
            response.put("content", content);
            AuraHttpUtils.sendJsonResponse(exchange, 200, AuraHttpUtils.gson.toJson(response));
        }
        catch (final SecurityException se)
        {
            LOGGER.error("[Aura Server] Directory traversal attempt detected: {}", filename);
            AuraHttpUtils.sendError(exchange, 403, se.getMessage());
        }
    }

    public void handleSaveFile(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        String file = params.get("file");
        String content = params.get("content");

        if ((file == null || file.trim().isEmpty() || content == null) && exchange.getRequestBody() != null)
        {
            try
            {
                final String body = AuraHttpUtils.readBody(exchange);
                final SaveRequest req = AuraHttpUtils.gson.fromJson(body, SaveRequest.class);
                if (req != null)
                {
                    if (file == null || file.trim().isEmpty())
                    {
                        file = req.file;
                    }
                    if (content == null)
                    {
                        content = req.content;
                    }
                }
            }
            catch (final Exception e)
            {
                // ignore
            }
        }

        if (file == null || file.trim().isEmpty() || content == null)
        {
            LOGGER.error("[Aura Server] Save file request failed: Missing 'file' or 'content'");
            AuraHttpUtils.sendError(exchange, 400, "Missing 'file' or 'content'");
            return;
        }

        try
        {
            fileService.saveYamlFileContent(file, content);
            LOGGER.info("[Aura Server] Saving file: {}", file);
            if (isHtmxRequest(exchange))
            {
                exchange.getResponseHeaders().set("HX-Trigger",
                        AuraHttpUtils.gson.toJson(Map.of("fileSaved", Map.of("file", file))));
                AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", new byte[0]);
            }
            else
            {
                AuraHttpUtils.sendJsonResponse(exchange, 200, AuraHttpUtils.gson.toJson(Map.of("success", true)));
            }
        }
        catch (final SecurityException se)
        {
            LOGGER.error("[Aura Server] Directory traversal attempt detected: {}", file);
            AuraHttpUtils.sendError(exchange, 403, se.getMessage());
        }
    }

    public void handleDeleteFile(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String file = params.get("file");

        if (file == null || file.trim().isEmpty())
        {
            LOGGER.error("[Aura Server] Delete file request failed: Missing 'file' parameter");
            AuraHttpUtils.sendError(exchange, 400, "Missing 'file' parameter");
            return;
        }

        try
        {
            fileService.deleteYamlFile(file);
            LOGGER.info("[Aura Server] Deleting file: {}", file);
            if (file.equals(fileService.getActiveEditingFile()))
            {
                fileService.setActiveEditingFile(null);
            }

            if (isHtmxRequest(exchange))
            {
                final Context context = new Context();
                context.setVariable("files", fileService.getYamlFilesList());
                context.setVariable("expandedFiles", fileService.getExpandedFiles());
                context.setVariable("activeEditingFile", fileService.getActiveEditingFile());

                final String deleteModalHtml = manager.getTemplateEngine().process("fragments/modals", Set.of("deleteTestModal"), context);
                final String yamlTreeHtml = manager.getTemplateEngine().process("fragments/test-selection", Set.of("yamlFileList"), context);
                final String editorHtml = manager.getTemplateEngine().process("dashboard", Set.of("editorPanel"), context);

                final String combinedHtml = deleteModalHtml +
                        "\n" + yamlTreeHtml.replace("id=\"yamlFileList\"", "id=\"yamlFileList\" hx-swap-oob=\"true\"") +
                        "\n" + editorHtml.replace("id=\"editorPanel\"", "id=\"editorPanel\" hx-swap-oob=\"true\"");

                AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", combinedHtml.getBytes(StandardCharsets.UTF_8));
            }
            else
            {
                AuraHttpUtils.sendJsonResponse(exchange, 200, AuraHttpUtils.gson.toJson(Map.of("success", true)));
            }
        }
        catch (final SecurityException se)
        {
            LOGGER.error("[Aura Server] Directory traversal attempt detected: {}", file);
            AuraHttpUtils.sendError(exchange, 403, se.getMessage());
        }
    }

    public void handleCreateFile(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String rawName = params.get("name");

        if (rawName == null || rawName.trim().isEmpty())
        {
            LOGGER.error("[Aura Server] Create file request failed: Missing 'name' parameter");
            if (isHtmxRequest(exchange))
            {
                renderCreateModalError(exchange, "Please provide a valid test case name.");
            }
            else
            {
                AuraHttpUtils.sendError(exchange, 400, "Missing 'name' in body");
            }
            return;
        }

        final String name = rawName.trim();
        final String kebab = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        final String filename = kebab + ".yaml";

        final File resourcesDir = new File("src/test/resources").getAbsoluteFile();
        final File file = new File(resourcesDir, filename).getAbsoluteFile();
        if (file.exists())
        {
            LOGGER.error("[Aura Server] Create file failed: File already exists: {}", filename);
            if (isHtmxRequest(exchange))
            {
                renderCreateModalError(exchange, "File already exists: " + filename);
            }
            else
            {
                AuraHttpUtils.sendError(exchange, 400, "File already exists: " + filename);
            }
            return;
        }

        LOGGER.info("[Aura Server] Creating new test file: {} (testName: \"{}\")", filename, name);
        final String boilerplate = "# Neodymium YAML Test Data File\n" +
                "steps: |\n" +
                "  Open ${neodymium.url} with username ${neodymium.basicauth.username} and password ${neodymium.basicauth.password}\n" +
                "  Verify the Page contains a header Navigation\n" +
                "\n" +
                "data:\n" +
                "  - testId: \"" + name + "\"\n";

        try
        {
            final File dest = fileService.resolveCanonicalFile(filename);
            Files.writeString(dest.toPath(), boilerplate, StandardCharsets.UTF_8);

            if (isHtmxRequest(exchange))
            {
                fileService.setActiveEditingFile(filename);
                final Context context = new Context();
                context.setVariable("files", fileService.getYamlFilesList());
                context.setVariable("expandedFiles", fileService.getExpandedFiles());
                context.setVariable("activeEditingFile", filename);
                context.setVariable("editorContent", boilerplate);

                final String createModalHtml = manager.getTemplateEngine().process("fragments/modals", Set.of("createTestModal"), context);
                final String yamlTreeHtml = manager.getTemplateEngine().process("fragments/test-selection", Set.of("yamlFileList"), context);
                final String editorHtml = manager.getTemplateEngine().process("dashboard", Set.of("editorPanel"), context);

                final String combinedHtml = createModalHtml +
                        "\n" + yamlTreeHtml.replace("id=\"yamlFileList\"", "id=\"yamlFileList\" hx-swap-oob=\"true\"") +
                        "\n" + editorHtml.replace("id=\"editorPanel\"", "id=\"editorPanel\" hx-swap-oob=\"true\"");

                AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", combinedHtml.getBytes(StandardCharsets.UTF_8));
            }
            else
            {
                AuraHttpUtils.sendJsonResponse(exchange, 200, AuraHttpUtils.gson.toJson(Map.of("success", true, "file", filename)));
            }
        }
        catch (final SecurityException se)
        {
            AuraHttpUtils.sendError(exchange, 403, se.getMessage());
        }
    }

    public void handleGetCreateModal(final HttpExchange exchange) throws IOException
    {
        final Context context = new Context();
        final String html = manager.getTemplateEngine().process("fragments/modals", Set.of("createTestModal"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleGetDeleteModal(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String file = params.get("file");

        final Context context = new Context();
        context.setVariable("fileToDelete", file);
        final String html = manager.getTemplateEngine().process("fragments/modals", Set.of("deleteTestModal"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    private void renderCreateModalError(final HttpExchange exchange, final String errorMsg) throws IOException
    {
        final Context context = new Context();
        context.setVariable("error", errorMsg);
        final String html = manager.getTemplateEngine().process("fragments/modals", Set.of("createTestModal"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isHtmxRequest(final HttpExchange exchange)
    {
        final String htmxHeader = exchange.getRequestHeaders().getFirst("HX-Request");
        if ("true".equalsIgnoreCase(htmxHeader))
        {
            return true;
        }
        final String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        return contentType != null && contentType.contains("application/x-www-form-urlencoded");
    }

    public String getEditorPanel(final String file, final Context context) throws IOException
    {
        final String content = fileService.readYamlFileContent(file);
        fileService.setActiveEditingFile(file);
        context.setVariable("activeEditingFile", file);
        context.setVariable("editorContent", content);
        return "dashboard :: editorPanel";
    }

    public void handleCloseEditor(final HttpExchange exchange) throws IOException
    {
        fileService.setActiveEditingFile(null);
        AuraHttpUtils.sendJsonResponse(exchange, 200, AuraHttpUtils.gson.toJson(Map.of("success", true)));
    }
}
