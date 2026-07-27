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
import com.xceptance.neodymium.aura.dto.YamlFileDto;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.thymeleaf.context.Context;

/**
 * Controller handling YAML test files discovery, tree panel rendering, and expand/collapse toggles.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraManagerFileController
{
    private final AuraFileService fileService;
    private final NeodymiumAuraManager manager;

    public AuraManagerFileController(final AuraFileService fileService, final NeodymiumAuraManager manager)
    {
        this.fileService = fileService;
        this.manager = manager;
    }

    public void handleListFilesPanel(final HttpExchange exchange) throws IOException
    {
        final Context context = new Context();
        final String fragmentPath = getYamlFilesPanel(context);
        final String[] parts = fragmentPath.split("::");
        final String template = parts[0].trim();
        final Set<String> fragments = Set.of(parts[1].trim());

        final String html = manager.getTemplateEngine().process(template, fragments, context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleToggleFileExpansion(final HttpExchange exchange) throws IOException
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

        final Context context = new Context();
        final String fragmentPath = toggleFileExpansion(file, context);
        final String[] parts = fragmentPath.split("::");
        final String template = parts[0].trim();
        final Set<String> fragments = Set.of(parts[1].trim());

        final String html = manager.getTemplateEngine().process(template, fragments, context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleListFiles(final HttpExchange exchange) throws IOException
    {
        final List<YamlFileDto> responseList = fileService.getYamlFilesList();
        AuraHttpUtils.sendJsonResponse(exchange, 200, AuraHttpUtils.gson.toJson(responseList));
    }

    public String getYamlFilesPanel(final Context context)
    {
        final List<YamlFileDto> responseList = fileService.getYamlFilesList();
        context.setVariable("files", responseList);
        context.setVariable("expandedFiles", fileService.getExpandedFiles());
        return "fragments/test-selection :: yamlFileList";
    }

    public void handleSearchFiles(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> queryParams = AuraHttpUtils.getQueryParams(exchange);
        String query = queryParams.get("q");
        if (query == null)
        {
            query = queryParams.get("query");
        }

        final List<YamlFileDto> filteredList = fileService.getFilteredYamlFilesList(query);

        final Context context = new Context();
        context.setVariable("files", filteredList);
        context.setVariable("expandedFiles", fileService.getExpandedFiles());

        final String html = manager.getTemplateEngine().process("fragments/test-selection", Set.of("yamlFileList"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public String toggleFileExpansion(final String file, final Context context)
    {
        if (file != null)
        {
            fileService.toggleFileExpansion(file);
        }
        return getYamlFilesPanel(context);
    }
}
