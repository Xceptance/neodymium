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
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.thymeleaf.context.Context;

import com.xceptance.neodymium.aura.dto.ChatSessionDto;
import com.xceptance.neodymium.aura.dto.YamlFileDto;

/**
 * Controller handling dashboard core rendering, static themes, and assets.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraManagerDashboardController
{
    private final AuraInteractiveService interactiveService;
    private final AuraFileService fileService;
    private final AuraManagerQueueController queueController;
    private final AuraManagerReportingController reportingController;
    private final AuraChatSessionService sessionService;
    private final NeodymiumAuraManager manager;

    public AuraManagerDashboardController(final AuraInteractiveService interactiveService,
            final AuraFileService fileService, final AuraManagerQueueController queueController,
            final AuraManagerReportingController reportingController, final AuraChatSessionService sessionService,
            final NeodymiumAuraManager manager)
    {
        this.interactiveService = interactiveService;
        this.fileService = fileService;
        this.queueController = queueController;
        this.reportingController = reportingController;
        this.sessionService = sessionService;
        this.manager = manager;
    }

    public void handleDashboard(final HttpExchange exchange) throws IOException
    {
        final Context context = new Context();
        final List<YamlFileDto> filesList = fileService.getYamlFilesList();
        context.setVariable("theme", interactiveService.getActiveTheme());
        context.setVariable("files", filesList);
        context.setVariable("expandedFiles", fileService.getExpandedFiles());
        context.setVariable("queue", queueController.getSelectedQueue());
        context.setVariable("selectedKeys", queueController.getSelectedQueueKeys());
        context.setVariable("selectedFileKeys", queueController.getFullySelectedFileKeys(filesList));
        context.setVariable("partiallySelectedFileKeys", queueController.getPartiallySelectedFileKeys(filesList));
        context.setVariable("headless", queueController.isHeadless());
        context.setVariable("video", queueController.isVideo());
        context.setVariable("executionMode", queueController.getExecutionMode());
        context.setVariable("interactive", queueController.isInteractive());
        context.setVariable("allure", queueController.isAllure());

        // Inject chat session variables
        final List<ChatSessionDto> chatSessions = sessionService.getSessions();
        final String currentSessionId = chatSessions.isEmpty() ? "" : chatSessions.get(0).id;
        context.setVariable("chatSessions", chatSessions);
        context.setVariable("currentSessionId", currentSessionId);
        context.setVariable("chatMessages", chatSessions.isEmpty() ? List.of() : chatSessions.get(0).messages);

        // Inject execution status variables
        final boolean running = queueController.isRunning();
        context.setVariable("running", running);
        context.setVariable("total", queueController.getGlobalTestsRun());
        context.setVariable("passed", queueController.getGlobalPassed());
        context.setVariable("failed", queueController.getGlobalFailed());
        context.setVariable("skipped", queueController.getGlobalSkipped());
        context.setVariable("activeEditingFile", fileService.getActiveEditingFile());

        // Inject initial reporting history list
        final List<Map<String, Object>> historyList = reportingController.getHistoryList();
        context.setVariable("history", historyList);

        if (running)
        {
            // Just use current time or some fallback for the initial running row
            final java.time.LocalDateTime ldt = java.time.LocalDateTime.now();
            final java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            context.setVariable("runningStartTime", ldt.format(formatter));
        }

        final String html = manager.getTemplateEngine().process("dashboard", context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleApiTheme(final HttpExchange exchange) throws IOException
    {
        final String query = exchange.getRequestURI().getQuery();
        String theme = "system";
        if (query != null)
        {
            for (final String param : query.split("&"))
            {
                final String[] pair = param.split("=");
                if (pair.length > 1 && "theme".equals(pair[0]))
                {
                    theme = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                }
            }
        }
        interactiveService.setActiveTheme(theme);

        final Context context = new Context();
        context.setVariable("theme", theme);
        final String fragment = manager.getTemplateEngine().process("dashboard", Set.of("themeContainer"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", fragment.getBytes(StandardCharsets.UTF_8));
    }

    public void handleDashboardAssets(final HttpExchange exchange) throws IOException
    {
        final String path = exchange.getRequestURI().getPath();
        final InputStream is = getClass().getClassLoader()
                .getResourceAsStream("com/xceptance/neodymium/aura" + path);
        if (is == null)
        {
            AuraHttpUtils.sendError(exchange, 404, "Not found");
            return;
        }
        final String contentType;
        if (path.endsWith(".css"))
        {
            contentType = "text/css; charset=UTF-8";
        }
        else if (path.endsWith(".woff2"))
        {
            contentType = "font/woff2";
        }
        else
        {
            contentType = "application/javascript; charset=UTF-8";
        }
        AuraHttpUtils.sendResponse(exchange, 200, contentType, is.readAllBytes());
    }
}
