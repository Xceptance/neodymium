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

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neodymium.ai.config.AiConfiguration;

/**
 * Controller handling live event routing, step submissions, screenshots serving,
 * and command handshakes for the Aura Interactive HUD execution.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraManagerInteractiveController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraManagerInteractiveController.class);

    private final AuraInteractiveService interactiveService;
    private final AuraReportingService reportingService;
    private final AuraQueueService queueService;
    private final NeodymiumAuraManager manager;

    public AuraManagerInteractiveController(final AuraInteractiveService interactiveService,
            final AuraReportingService reportingService, final AuraQueueService queueService,
            final NeodymiumAuraManager manager)
    {
        this.interactiveService = interactiveService;
        this.reportingService = reportingService;
        this.queueService = queueService;
        this.manager = manager;
    }

    public void handleConsoleEvents(final HttpExchange exchange) throws IOException
    {
        final InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        if (engine != null)
        {
            engine.createSseHandler().handle(exchange);
        }
        else
        {
            AuraHttpUtils.sendError(exchange, 404, "No active interactive engine");
        }
    }

    public void handleConsoleAction(final HttpExchange exchange) throws IOException
    {
        final InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        if (engine != null)
        {
            engine.createActionHandler().handle(exchange);
        }
        else
        {
            AuraHttpUtils.sendError(exchange, 404, "No active interactive engine");
        }
    }

    public void handleConsoleScreenshot(final HttpExchange exchange) throws IOException
    {
        final String query = exchange.getRequestURI().getQuery();
        String file = null;
        String runId = null;
        if (query != null)
        {
            for (final String param : query.split("&"))
            {
                final String[] pair = param.split("=");
                if (pair.length > 1)
                {
                    if ("file".equals(pair[0]))
                    {
                        file = pair[1];
                    }
                    if ("runId".equals(pair[0]))
                    {
                        runId = pair[1];
                    }
                }
            }
        }

        if (file == null || file.contains("/") || file.contains("\\") || file.contains(".."))
        {
            AuraHttpUtils.sendError(exchange, 403, "Access denied: invalid file");
            return;
        }

        File targetFile = null;
        if (runId != null && !runId.isEmpty())
        {
            try
            {
                final File historyDir = new File(reportingService.getReportHistoryDir(), runId).getCanonicalFile();
                targetFile = new File(historyDir, "screenshots/" + file).getCanonicalFile();
                if (!targetFile.getPath().startsWith(historyDir.getPath()))
                {
                    AuraHttpUtils.sendError(exchange, 403, "Access denied");
                    return;
                }
            }
            catch (final Exception ignore)
            {
                // ignore
            }
        }
        if (targetFile == null || !targetFile.exists())
        {
            final String screenshotsDirPath = AiConfiguration.getInstance().getProperty("neodymium.ai.console.screenshotsDir", "target/aura-sandbox/ai-console-screenshots");
            final File activeDir = new File(screenshotsDirPath).getCanonicalFile();
            targetFile = new File(activeDir, file).getCanonicalFile();
            if (!targetFile.getPath().startsWith(activeDir.getPath()))
            {
                AuraHttpUtils.sendError(exchange, 403, "Access denied");
                return;
            }
        }

        if (targetFile.exists() && targetFile.isFile())
        {
            final byte[] bytes = Files.readAllBytes(targetFile.toPath());
            AuraHttpUtils.sendResponse(exchange, 200, "image/png", bytes);
        }
        else
        {
            AuraHttpUtils.sendError(exchange, 404, "File not found");
        }
    }

    public void handlePushState(final HttpExchange exchange) throws IOException
    {
        InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        if (engine == null)
        {
            engine = new InteractiveConsoleEngine("initializing");
            interactiveService.setCurrentConsoleEngine(engine);
        }
        final String body = AuraHttpUtils.readBody(exchange);
        try
        {
            final JsonObject json = AuraHttpUtils.gson.fromJson(body, JsonObject.class);
            if (json != null && json.has("runId"))
            {
                final String incomingRunId = json.get("runId").getAsString();
                final String lastId = interactiveService.getLastProcessedRunIdReference().getAndSet(incomingRunId);
                if (incomingRunId != null && !incomingRunId.equals(lastId))
                {
                    LOGGER.info("[Aura Server] New runId detected: {}. Resetting manuallyStopped flag.", incomingRunId);
                    queueService.setManuallyStopped(false);
                }
                engine.setRunId(incomingRunId);
            }
        }
        catch (final Exception e)
        {
            // ignore parsing error
        }
        engine.pushState(body);
        final String responseJson = queueService.isManuallyStopped()
                ? "{\"status\":\"stopped\"}"
                : "{\"status\":\"ok\"}";
        AuraHttpUtils.sendResponse(exchange, 200, "application/json", responseJson.getBytes(StandardCharsets.UTF_8));
    }

    public void handleBroadcast(final HttpExchange exchange) throws IOException
    {
        InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        if (engine == null)
        {
            engine = new InteractiveConsoleEngine("initializing");
            interactiveService.setCurrentConsoleEngine(engine);
        }
        final String body = AuraHttpUtils.readBody(exchange);
        final JsonObject json = AuraHttpUtils.gson.fromJson(body, JsonObject.class);
        engine.broadcastSseEvent(json.get("event").getAsString(), json.get("payload").getAsString());
        AuraHttpUtils.sendResponse(exchange, 200, "application/json", "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8));
    }

    public void handleWaitForAction(final HttpExchange exchange) throws IOException
    {
        if (queueService.isManuallyStopped())
        {
            final JsonObject abortAction = new JsonObject();
            abortAction.addProperty("action", "ABORT");
            AuraHttpUtils.sendResponse(exchange, 200, "application/json", abortAction.toString().getBytes(StandardCharsets.UTF_8));
            return;
        }
        InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        if (engine == null)
        {
            engine = new InteractiveConsoleEngine("initializing");
            interactiveService.setCurrentConsoleEngine(engine);
        }
        final String query = exchange.getRequestURI().getQuery();
        String tempPauseId = null;
        if (query != null)
        {
            for (final String param : query.split("&"))
            {
                final String[] pair = param.split("=");
                if (pair.length > 1 && "pauseId".equals(pair[0]))
                {
                    tempPauseId = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    break;
                }
            }
        }
        final String pauseId = tempPauseId;
        final JsonObject action;
        try
        {
            action = engine.waitForAction(pauseId);
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
            AuraHttpUtils.sendError(exchange, 500, "Interrupted while waiting for action");
            return;
        }
        AuraHttpUtils.sendResponse(exchange, 200, "application/json",
                action != null ? action.toString().getBytes(StandardCharsets.UTF_8) : "{}".getBytes(StandardCharsets.UTF_8));
    }

    public void handleHtml(final HttpExchange exchange) throws IOException
    {
        try (final InputStream is = getClass().getClassLoader()
                .getResourceAsStream("com/xceptance/neodymium/ai/console/interactive_console.html"))
        {
            if (is == null)
            {
                AuraHttpUtils.sendError(exchange, 404, "Not found");
                return;
            }
            AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", is.readAllBytes());
        }
    }

    public void handleCss(final HttpExchange exchange) throws IOException
    {
        try (final InputStream is = getClass().getClassLoader()
                .getResourceAsStream("com/xceptance/neodymium/ai/console/interactive_console.css"))
        {
            if (is == null)
            {
                AuraHttpUtils.sendError(exchange, 404, "Not found");
                return;
            }
            AuraHttpUtils.sendResponse(exchange, 200, "text/css", is.readAllBytes());
        }
    }

    public void handleJs(final HttpExchange exchange) throws IOException
    {
        try (final InputStream is = getClass().getClassLoader()
                .getResourceAsStream("com/xceptance/neodymium/ai/console/interactive_console.js"))
        {
            if (is == null)
            {
                AuraHttpUtils.sendError(exchange, 404, "Not found");
                return;
            }
            AuraHttpUtils.sendResponse(exchange, 200, "application/javascript", is.readAllBytes());
        }
    }

    public void handleDisconnect(final HttpExchange exchange) throws IOException
    {
        final String query = exchange.getRequestURI().getQuery();
        String clientId = null;
        if (query != null)
        {
            for (final String param : query.split("&"))
            {
                final String[] pair = param.split("=");
                if (pair.length > 1 && "clientId".equals(pair[0]))
                {
                    clientId = pair[1];
                    break;
                }
            }
        }

        LOGGER.info("[Aura Server] Disconnect request received for clientId: {}", clientId);

        if (clientId != null && !clientId.trim().isEmpty())
        {
            manager.removeClient(clientId);
            LOGGER.info("[Aura Server] Removed active client: {}", clientId);
        }

        AuraHttpUtils.sendJsonResponse(exchange, 200, AuraHttpUtils.gson.toJson(Map.of("success", true)));
    }
}
