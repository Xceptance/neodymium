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
import com.xceptance.neodymium.aura.dto.DatasetSelection;
import com.xceptance.neodymium.aura.dto.RunRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.Context;

/**
 * Controller handling Run Queue management, test execution triggers,
 * and live server-side polling of execution logs and events.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraManagerQueueController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraManagerQueueController.class);

    private final AuraQueueService queueService;
    private final AuraFileService fileService;
    private final AuraInteractiveService interactiveService;
    private final NeodymiumAuraManager manager;

    private final List<DatasetSelection> selectedQueue = Collections.synchronizedList(new ArrayList<>());
    private boolean headless = false;
    private boolean video = false;
    private boolean keepOpen = false;
    private boolean interactive = false;
    private boolean allure = false;

    public AuraManagerQueueController(final AuraQueueService queueService, final AuraFileService fileService, final AuraInteractiveService interactiveService, final NeodymiumAuraManager manager)
    {
        this.queueService = queueService;
        this.fileService = fileService;
        this.interactiveService = interactiveService;
        this.manager = manager;
    }

    public void handleToggleQueue(final HttpExchange exchange) throws IOException
    {
        final String query = exchange.getRequestURI().getQuery();
        String file = null;
        String id = null;
        if (query != null)
        {
            for (final String param : query.split("&"))
            {
                final String[] pair = param.split("=");
                if (pair.length > 1)
                {
                    if ("file".equals(pair[0]))
                    {
                        file = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    }
                    else if ("id".equals(pair[0]))
                    {
                        id = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    }
                }
            }
        }

        if (file != null && id != null)
        {
            boolean removed = false;
            synchronized (selectedQueue)
            {
                for (int i = 0; i < selectedQueue.size(); i++)
                {
                    final DatasetSelection item = selectedQueue.get(i);
                    if (file.equals(item.file) && id.equals(item.id))
                    {
                        selectedQueue.remove(i);
                        removed = true;
                        break;
                    }
                }
                if (!removed)
                {
                    final DatasetSelection sel = new DatasetSelection();
                    sel.file = file;
                    sel.id = id;
                    selectedQueue.add(sel);
                }
            }
        }

        final Context context = new Context();
        context.setVariable("queue", selectedQueue);
        final String html = manager.getTemplateEngine().process("dashboard", Set.of("queueListContainer"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleMoveQueue(final HttpExchange exchange) throws IOException
    {
        final String query = exchange.getRequestURI().getQuery();
        int index = -1;
        String direction = null;
        if (query != null)
        {
            for (final String param : query.split("&"))
            {
                final String[] pair = param.split("=");
                if (pair.length > 1)
                {
                    if ("index".equals(pair[0]))
                    {
                        try
                        {
                            index = Integer.parseInt(pair[1]);
                        }
                        catch (final NumberFormatException e)
                        {
                            // ignore
                        }
                    }
                    else if ("direction".equals(pair[0]))
                    {
                        direction = pair[1];
                    }
                }
            }
        }

        if (index >= 0 && direction != null)
        {
            synchronized (selectedQueue)
            {
                if ("up".equalsIgnoreCase(direction) && index > 0 && index < selectedQueue.size())
                {
                    Collections.swap(selectedQueue, index, index - 1);
                }
                else if ("down".equalsIgnoreCase(direction) && index >= 0 && index < selectedQueue.size() - 1)
                {
                    Collections.swap(selectedQueue, index, index + 1);
                }
            }
        }

        final Context context = new Context();
        context.setVariable("queue", selectedQueue);
        final String html = manager.getTemplateEngine().process("dashboard", Set.of("queueListContainer"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleClearQueue(final HttpExchange exchange) throws IOException
    {
        selectedQueue.clear();
        final Context context = new Context();
        context.setVariable("queue", selectedQueue);
        final String html = manager.getTemplateEngine().process("dashboard", Set.of("queueListContainer"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleToggleConfig(final HttpExchange exchange) throws IOException
    {
        final String query = exchange.getRequestURI().getQuery();
        String key = null;
        if (query != null)
        {
            for (final String param : query.split("&"))
            {
                final String[] pair = param.split("=");
                if (pair.length > 1 && "key".equals(pair[0]))
                {
                    key = pair[1];
                    break;
                }
            }
        }

        if (key != null)
        {
            switch (key)
            {
                case "headless" -> headless = !headless;
                case "video" -> video = !video;
                case "keepOpen" -> keepOpen = !keepOpen;
                case "interactive" -> interactive = !interactive;
                case "allure" -> allure = !allure;
            }
        }

        final Context context = new Context();
        context.setVariable("headless", headless);
        context.setVariable("video", video);
        context.setVariable("keepOpen", keepOpen);
        context.setVariable("interactive", interactive);
        context.setVariable("allure", allure);

        final String html = manager.getTemplateEngine().process("dashboard", Set.of("configPanel"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleRunQueue(final HttpExchange exchange) throws IOException
    {
        final String body = AuraHttpUtils.readBody(exchange);
        RunRequest req = null;
        if (body != null && !body.trim().isEmpty())
        {
            req = AuraHttpUtils.gson.fromJson(body, RunRequest.class);
        }

        if (req == null || req.datasets == null || req.datasets.isEmpty())
        {
            req = new RunRequest();
            req.datasets = new ArrayList<>(selectedQueue);
            req.headless = headless;
            req.video = video;
            req.interactive = interactive;
            req.allure = allure;
        }

        if (req.datasets == null || req.datasets.isEmpty())
        {
            LOGGER.error("[Aura Server] Run queue request failed: No datasets in queue");
            AuraHttpUtils.sendError(exchange, 400, "No datasets in queue to run");
            return;
        }

        if (queueService.isRunningQueue())
        {
            LOGGER.error("[Aura Server] Run queue request failed: A queue is already executing");
            AuraHttpUtils.sendError(exchange, 409, "A queue is already executing");
            return;
        }

        LOGGER.info("[Aura Server] Spawning test run queue for {} dataset(s) (headless={}, interactive={})",
                req.datasets.size(), req.headless, req.interactive);
        queueService.executeQueue(req, manager.getPort());
        AuraHttpUtils.sendJsonResponse(exchange, 200, AuraHttpUtils.gson.toJson(Map.of("success", true)));
    }

    public void handleStatusStream(final HttpExchange exchange) throws IOException
    {
        final String query = exchange.getRequestURI().getQuery();
        String clientId = null;
        int lastIndex = 0;
        int lastEventIndex = 0;
        if (query != null)
        {
            for (final String param : query.split("&"))
            {
                final String[] pair = param.split("=");
                if (pair.length > 1)
                {
                    if ("clientId".equals(pair[0]))
                    {
                        clientId = pair[1];
                    }
                    else if ("lastIndex".equals(pair[0]))
                    {
                        try
                        {
                            lastIndex = Integer.parseInt(pair[1]);
                        }
                        catch (final NumberFormatException e)
                        {
                            // ignore
                        }
                    }
                    else if ("lastEventIndex".equals(pair[0]))
                    {
                        try
                        {
                            lastEventIndex = Integer.parseInt(pair[1]);
                        }
                        catch (final NumberFormatException e)
                        {
                            // ignore
                        }
                    }
                }
            }
        }
        if (clientId == null || clientId.trim().isEmpty())
        {
            clientId = "fallback-" + UUID.randomUUID().toString();
        }

        manager.registerClient(clientId);

        final Map<String, Object> status = new HashMap<>();
        status.put("type", "status");
        status.put("sessionId", manager.getServerSessionId());
        status.put("total", queueService.getGlobalTestsRun());
        status.put("passed", queueService.getGlobalPassed());
        status.put("failed", queueService.getGlobalFailed());
        status.put("skipped", queueService.getGlobalSkipped());
        status.put("running", queueService.isRunningQueue());
        status.put("activeFile", queueService.getActiveFile());

        String activeTestId = "";
        final InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        if (engine != null)
        {
            final String stateJson = engine.getCurrentStateJson();
            if (stateJson != null)
            {
                try
                {
                    final JsonObject stateObj = AuraHttpUtils.gson.fromJson(stateJson, JsonObject.class);
                    if (stateObj != null && stateObj.has("testId"))
                    {
                        activeTestId = stateObj.get("testId").getAsString();
                    }
                }
                catch (final Exception e)
                {
                    // ignore
                }
            }
        }
        status.put("activeTestId", activeTestId);

        final List<Map<String, String>> datasetsList = new ArrayList<>();
        final RunRequest lastReq = queueService.getLastRunRequest();
        if (lastReq != null && lastReq.datasets != null)
        {
            for (final DatasetSelection selection : lastReq.datasets)
            {
                final Map<String, String> m = new HashMap<>();
                m.put("file", selection.file);
                m.put("id", selection.id);
                datasetsList.add(m);
            }
        }
        status.put("tests", datasetsList);
        status.put("completedFiles", new ArrayList<>(queueService.getCompletedFiles()));

        final List<String> logs = new ArrayList<>();
        final int currentLogSize;
        synchronized (queueService.getCurrentRunLogs())
        {
            currentLogSize = queueService.getCurrentRunLogs().size();
            for (int i = lastIndex; i < currentLogSize; i++)
            {
                logs.add(queueService.getCurrentRunLogs().get(i));
            }
        }

        final List<Map<String, Object>> events = new ArrayList<>();
        final int currentEventSize;
        synchronized (queueService.getCurrentRunEvents())
        {
            currentEventSize = queueService.getCurrentRunEvents().size();
            for (int i = lastEventIndex; i < currentEventSize; i++)
            {
                events.add(queueService.getCurrentRunEvents().get(i));
            }
        }

        final Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("logs", logs);
        if (!events.isEmpty())
        {
            response.put("events", events);
            response.put("newEventIndex", currentEventSize);
        }
        if (!logs.isEmpty())
        {
            response.put("newIndex", currentLogSize);
        }
        AuraHttpUtils.sendJsonResponse(exchange, 200, AuraHttpUtils.gson.toJson(response));
    }

    public void handleStopProcess(final HttpExchange exchange) throws IOException
    {
        LOGGER.info("[Aura Server] User requested to stop active execution subprocess");
        queueService.stopProcess();
        AuraHttpUtils.sendJsonResponse(exchange, 200, AuraHttpUtils.gson.toJson(Map.of("success", true)));
    }
}
