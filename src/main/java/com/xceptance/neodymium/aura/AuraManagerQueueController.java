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
import com.xceptance.neodymium.aura.dto.DatasetDto;
import com.xceptance.neodymium.aura.dto.DatasetSelection;
import com.xceptance.neodymium.aura.dto.RunRequest;
import com.xceptance.neodymium.aura.dto.YamlFileDto;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
    private boolean headless = true;
    private boolean video = false;
    private boolean keepOpen = false;
    private boolean interactive = false;
    private boolean allure = true;

    public AuraManagerQueueController(final AuraQueueService queueService, final AuraFileService fileService, final AuraInteractiveService interactiveService, final NeodymiumAuraManager manager)
    {
        this.queueService = queueService;
        this.fileService = fileService;
        this.interactiveService = interactiveService;
        this.manager = manager;
    }

    public final List<DatasetSelection> getSelectedQueue()
    {
        return this.selectedQueue;
    }

    public Set<String> getSelectedQueueKeys()
    {
        final Set<String> keys = new HashSet<>();
        synchronized (selectedQueue)
        {
            for (final DatasetSelection sel : selectedQueue)
            {
                if (sel.file != null && sel.id != null)
                {
                    keys.add(sel.file + "::" + sel.id);
                }
            }
        }
        return keys;
    }

    public Set<String> getFullySelectedFileKeys(final List<YamlFileDto> files)
    {
        final Set<String> fullFiles = new HashSet<>();
        final Set<String> keys = getSelectedQueueKeys();
        if (files != null)
        {
            for (final YamlFileDto f : files)
            {
                if (f.datasets != null && !f.datasets.isEmpty())
                {
                    boolean all = true;
                    for (final DatasetDto d : f.datasets)
                    {
                        if (!keys.contains(f.file + "::" + d.id))
                        {
                            all = false;
                            break;
                        }
                    }
                    if (all)
                    {
                        fullFiles.add(f.file);
                    }
                }
            }
        }
        return fullFiles;
    }

    public final boolean isHeadless()
    {
        return this.headless;
    }

    public final boolean isVideo()
    {
        return this.video;
    }

    public final boolean isKeepOpen()
    {
        return this.keepOpen;
    }

    public final boolean isInteractive()
    {
        return this.interactive;
    }

    public final boolean isAllure()
    {
        return this.allure;
    }

    public final boolean isRunning()
    {
        return this.queueService.isRunningQueue();
    }

    public final int getGlobalTestsRun()
    {
        return this.queueService.getGlobalTestsRun();
    }

    public final int getGlobalPassed()
    {
        return this.queueService.getGlobalPassed();
    }

    public final int getGlobalFailed()
    {
        return this.queueService.getGlobalFailed();
    }

    public final int getGlobalSkipped()
    {
        return this.queueService.getGlobalSkipped();
    }


    public void handleToggleQueue(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String file = params.get("file");
        final String id = params.get("id");

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
        context.setVariable("running", isRunning());
        context.setVariable("activeEditingFile", fileService.getActiveEditingFile());
        final String html = manager.getTemplateEngine().process("fragments/queue", Set.of("queueListContainerContent", "runControls"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleToggleAllQueue(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String file = params.get("file");

        if (file != null)
        {
            final String targetFile = file;
            final List<YamlFileDto> files = fileService.getYamlFilesList();
            List<DatasetDto> datasets = null;
            for (final YamlFileDto f : files)
            {
                if (targetFile.equals(f.file))
                {
                    datasets = f.datasets;
                    break;
                }
            }

            if (datasets != null && !datasets.isEmpty())
            {
                synchronized (selectedQueue)
                {
                    final boolean shouldAdd;
                    if (params.containsKey("checked"))
                    {
                        shouldAdd = Boolean.parseBoolean(params.get("checked"));
                    }
                    else
                    {
                        int count = 0;
                        for (final DatasetDto d : datasets)
                        {
                            for (final DatasetSelection item : selectedQueue)
                            {
                                if (targetFile.equals(item.file) && d.id.equals(item.id))
                                {
                                    count++;
                                    break;
                                }
                            }
                        }
                        shouldAdd = (count < datasets.size());
                    }

                    if (shouldAdd)
                    {
                        for (final DatasetDto d : datasets)
                        {
                            boolean found = false;
                            for (final DatasetSelection item : selectedQueue)
                            {
                                if (targetFile.equals(item.file) && d.id.equals(item.id))
                                {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found)
                            {
                                final DatasetSelection sel = new DatasetSelection();
                                sel.file = targetFile;
                                sel.id = d.id;
                                selectedQueue.add(sel);
                            }
                        }
                    }
                    else
                    {
                        selectedQueue.removeIf(item -> targetFile.equals(item.file));
                    }
                }
            }
        }

        final Context context = new Context();
        context.setVariable("queue", selectedQueue);
        context.setVariable("running", isRunning());
        context.setVariable("activeEditingFile", fileService.getActiveEditingFile());
        final String html = manager.getTemplateEngine().process("fragments/queue", Set.of("queueListContainerContent", "runControls"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }


    public void handleMoveQueue(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        int index = -1;
        if (params.containsKey("index"))
        {
            try
            {
                index = Integer.parseInt(params.get("index"));
            }
            catch (final NumberFormatException e)
            {
                // ignore
            }
        }
        String direction = params.get("direction");
        if (direction == null && params.containsKey("dir"))
        {
            final String dirVal = params.get("dir");
            if ("-1".equals(dirVal) || "up".equalsIgnoreCase(dirVal))
            {
                direction = "up";
            }
            else if ("1".equals(dirVal) || "down".equalsIgnoreCase(dirVal))
            {
                direction = "down";
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
        context.setVariable("running", isRunning());
        context.setVariable("activeEditingFile", fileService.getActiveEditingFile());
        final String html = manager.getTemplateEngine().process("fragments/queue", Set.of("queueListContainerContent", "runControls"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleRemoveQueue(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String file = params.get("file");
        final String id = params.get("id");
        int index = -1;
        if (params.containsKey("index"))
        {
            try
            {
                index = Integer.parseInt(params.get("index"));
            }
            catch (final NumberFormatException e)
            {
                // ignore
            }
        }

        synchronized (selectedQueue)
        {
            if (index >= 0 && index < selectedQueue.size())
            {
                selectedQueue.remove(index);
            }
            else if (file != null && id != null)
            {
                selectedQueue.removeIf(item -> file.equals(item.file) && id.equals(item.id));
            }
        }

        final Context context = new Context();
        context.setVariable("queue", selectedQueue);
        context.setVariable("running", isRunning());
        context.setVariable("activeEditingFile", fileService.getActiveEditingFile());
        final String html = manager.getTemplateEngine().process("fragments/queue", Set.of("queueListContainerContent", "runControls"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleClearQueue(final HttpExchange exchange) throws IOException
    {
        selectedQueue.clear();
        final Context context = new Context();
        context.setVariable("queue", selectedQueue);
        context.setVariable("running", isRunning());
        context.setVariable("activeEditingFile", fileService.getActiveEditingFile());
        final String html = manager.getTemplateEngine().process("fragments/queue", Set.of("queueListContainerContent", "runControls"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleToggleConfig(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String key = params.get("key");

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
        context.setVariable("queue", getSelectedQueue());
        context.setVariable("running", isRunning());
        context.setVariable("activeEditingFile", fileService.getActiveEditingFile());

        final String html = manager.getTemplateEngine().process("dashboard", Set.of("configPanel"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleRunQueue(final HttpExchange exchange) throws IOException
    {
        final String query = exchange.getRequestURI().getQuery();
        boolean runCurrent = false;
        if (query != null)
        {
            for (final String param : query.split("&"))
            {
                final String[] pair = param.split("=");
                if (pair.length > 1 && "runCurrent".equals(pair[0]) && "true".equals(pair[1]))
                {
                    runCurrent = true;
                    break;
                }
            }
        }

        RunRequest req = null;
        if (runCurrent)
        {
            final String activeFile = fileService.getActiveEditingFile();
            if (activeFile != null)
            {
                req = new RunRequest();
                final DatasetSelection sel = new DatasetSelection();
                sel.file = activeFile;
                sel.id = null;
                req.datasets = List.of(sel);
                req.headless = headless;
                req.video = video;
                req.interactive = interactive;
                req.allure = allure;
            }
        }

        if (req == null)
        {
            final String body = AuraHttpUtils.readBody(exchange);
            if (body != null && !body.trim().isEmpty() && body.trim().startsWith("{"))
            {
                req = AuraHttpUtils.gson.fromJson(body, RunRequest.class);
            }
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
        
        renderExecutionStatePanels(exchange);
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

    public void handleStatusPanel(final HttpExchange exchange) throws IOException
    {
        renderExecutionStatePanels(exchange);
    }

    public void handleStopProcess(final HttpExchange exchange) throws IOException
    {
        LOGGER.info("[Aura Server] User requested to stop active execution subprocess");
        queueService.stopProcess();
        renderExecutionStatePanels(exchange);
    }

    private void renderExecutionStatePanels(final HttpExchange exchange) throws IOException
    {
        final Context context = new Context();
        context.setVariable("running", isRunning());
        context.setVariable("total", getGlobalTestsRun());
        context.setVariable("passed", getGlobalPassed());
        context.setVariable("failed", getGlobalFailed());
        context.setVariable("skipped", getGlobalSkipped());
        context.setVariable("queue", getSelectedQueue());
        context.setVariable("activeEditingFile", fileService.getActiveEditingFile());

        final String html = manager.getTemplateEngine().process("dashboard", Set.of("runControls", "statsPanel", "executionTrigger"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }
}
