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
import com.xceptance.neodymium.aura.dto.BrowserGroupDto;
import com.xceptance.neodymium.aura.dto.BrowserProfileDto;
import com.xceptance.neodymium.aura.dto.DatasetDto;
import com.xceptance.neodymium.aura.dto.DatasetSelection;
import com.xceptance.neodymium.aura.dto.RunRequest;
import com.xceptance.neodymium.aura.dto.YamlFileDto;
import org.neodymium.common.browser.configuration.BrowserConfiguration;
import org.neodymium.common.browser.configuration.MultibrowserConfiguration;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.Context;

/**
 * Controller managing test datasets selection queue, run execution trigger,
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
    private final Set<String> globalBrowserProfiles = Collections.synchronizedSet(new LinkedHashSet<>());
    private boolean headless = true;
    private boolean video = false;
    private String executionMode = "REPLAY_WITH_HEALING";
    private boolean interactive = false;
    private boolean allure = true;

    public AuraManagerQueueController(final AuraQueueService queueService, final AuraFileService fileService, final AuraInteractiveService interactiveService, final NeodymiumAuraManager manager)
    {
        this.queueService = queueService;
        this.fileService = fileService;
        this.interactiveService = interactiveService;
        this.manager = manager;

        final List<BrowserProfileDto> available = getAvailableBrowserProfiles();
        if (!available.isEmpty())
        {
            boolean added = false;
            for (final BrowserProfileDto p : available)
            {
                if ("Chrome_1024x768".equalsIgnoreCase(p.id))
                {
                    globalBrowserProfiles.add(p.id);
                    added = true;
                    break;
                }
            }
            if (!added)
            {
                globalBrowserProfiles.add(available.get(0).id);
            }
        }
        else
        {
            globalBrowserProfiles.add("Chrome_1024x768");
        }
    }

    public final List<DatasetSelection> getSelectedQueue()
    {
        return this.selectedQueue;
    }

    public final Set<String> getGlobalBrowserProfiles()
    {
        return this.globalBrowserProfiles;
    }

    public final void setGlobalBrowserProfiles(final Collection<String> profiles)
    {
        synchronized (this.globalBrowserProfiles)
        {
            this.globalBrowserProfiles.clear();
            if (profiles != null)
            {
                this.globalBrowserProfiles.addAll(profiles);
            }
        }
    }

    public final int getTotalExecutionRuns()
    {
        int total = 0;
        final int globalCount = Math.max(1, globalBrowserProfiles.size());
        synchronized (selectedQueue)
        {
            for (final DatasetSelection sel : selectedQueue)
            {
                if (sel.browserProfiles != null && !sel.browserProfiles.isEmpty())
                {
                    total += sel.browserProfiles.size();
                }
                else
                {
                    total += globalCount;
                }
            }
        }
        return total;
    }

    public List<BrowserProfileDto> getAvailableBrowserProfiles()
    {
        final MultibrowserConfiguration config = MultibrowserConfiguration.getInstance();
        final Map<String, BrowserConfiguration> rawProfiles = config.getBrowserProfiles();
        final List<BrowserProfileDto> list = new ArrayList<>();
        if (rawProfiles != null)
        {
            for (final Map.Entry<String, BrowserConfiguration> entry : rawProfiles.entrySet())
            {
                final String tag = entry.getKey();
                final BrowserConfiguration bc = entry.getValue();
                if (tag == null || tag.trim().isEmpty() || "default".equalsIgnoreCase(tag) || "global".equalsIgnoreCase(tag))
                {
                    continue;
                }
                final String name = bc.getName() != null && !bc.getName().trim().isEmpty() ? bc.getName() : tag;
                final String rawBrowserName = bc.getCapabilities() != null && bc.getCapabilities().getBrowserName() != null
                        ? bc.getCapabilities().getBrowserName().toLowerCase()
                        : "";
                final String tagLower = tag.toLowerCase();
                final String browser;
                if (tagLower.contains("galaxy") || tagLower.contains("iphone") || tagLower.contains("pixel")
                        || tagLower.contains("mobile") || tagLower.contains("nexus") || tagLower.contains("android")
                        || tagLower.contains("ipad") || rawBrowserName.contains("android") || rawBrowserName.contains("iphone")
                        || rawBrowserName.contains("ipad"))
                {
                    browser = "mobile";
                }
                else if (rawBrowserName.contains("chrome") || tagLower.startsWith("chrome") || tagLower.contains("_chrome")
                        || tagLower.contains("chromium"))
                {
                    browser = "chrome";
                }
                else if (rawBrowserName.contains("firefox") || tagLower.startsWith("ff") || tagLower.contains("firefox")
                        || tagLower.contains("_ff"))
                {
                    browser = "firefox";
                }
                else if (rawBrowserName.contains("safari") || tagLower.startsWith("safari") || tagLower.contains("_safari")
                        || tagLower.contains("webkit"))
                {
                    browser = "safari";
                }
                else if (rawBrowserName.contains("edge") || rawBrowserName.contains("microsoftedge") || tagLower.startsWith("edge")
                        || tagLower.contains("_edge"))
                {
                    browser = "edge";
                }
                else
                {
                    browser = "other";
                }

                final String res = (bc.getBrowserWidth() > 0 && bc.getBrowserHeight() > 0)
                        ? bc.getBrowserWidth() + "x" + bc.getBrowserHeight()
                        : "";
                final boolean isHeadless = bc.isHeadless();

                list.add(new BrowserProfileDto(tag, name, browser, res, isHeadless));
            }
        }
        return list;
    }

    public List<BrowserGroupDto> getGroupedBrowserProfiles(final Set<String> activeProfiles)
    {
        final List<BrowserProfileDto> all = getAvailableBrowserProfiles();
        final Map<String, BrowserGroupDto> groupMap = new LinkedHashMap<>();
        groupMap.put("chrome", new BrowserGroupDto("chrome", "Google Chrome"));
        groupMap.put("firefox", new BrowserGroupDto("firefox", "Mozilla Firefox"));
        groupMap.put("safari", new BrowserGroupDto("safari", "Apple Safari"));
        groupMap.put("edge", new BrowserGroupDto("edge", "Microsoft Edge"));
        groupMap.put("mobile", new BrowserGroupDto("mobile", "Mobile Devices"));
        groupMap.put("other", new BrowserGroupDto("other", "Other Profiles"));

        for (final BrowserProfileDto p : all)
        {
            final String key = groupMap.containsKey(p.browser) ? p.browser : "other";
            final BrowserGroupDto group = groupMap.get(key);
            group.profiles.add(p);
            if (activeProfiles != null && activeProfiles.contains(p.id))
            {
                group.selectedCount++;
            }
        }

        final List<BrowserGroupDto> result = new ArrayList<>();
        for (final BrowserGroupDto g : groupMap.values())
        {
            if (!g.profiles.isEmpty())
            {
                result.add(g);
            }
        }
        return result;
    }

    public List<String> getBrowserTypesForProfiles(final List<String> profileIds)
    {
        final List<BrowserProfileDto> all = getAvailableBrowserProfiles();
        final Set<String> targetIds = new HashSet<>(profileIds != null && !profileIds.isEmpty() ? profileIds : globalBrowserProfiles);
        final Set<String> types = new LinkedHashSet<>();
        for (final BrowserProfileDto p : all)
        {
            if (targetIds.contains(p.id))
            {
                types.add(p.browser);
            }
        }
        if (types.isEmpty())
        {
            types.add("chrome");
        }
        return new ArrayList<>(types);
    }

    public void populateQueueContext(final Context context)
    {
        context.setVariable("queue", selectedQueue);
        context.setVariable("availableBrowserProfiles", getAvailableBrowserProfiles());
        context.setVariable("globalBrowserProfiles", getGlobalBrowserProfiles());
        context.setVariable("browserGroups", getGroupedBrowserProfiles(globalBrowserProfiles));
        context.setVariable("totalRuns", getTotalExecutionRuns());
        context.setVariable("queueController", this);
        context.setVariable("running", isRunning());
        context.setVariable("activeEditingFile", fileService.getActiveEditingFile());
        context.setVariable("headless", headless);
        context.setVariable("video", video);
        context.setVariable("executionMode", executionMode);
        context.setVariable("interactive", interactive);
        context.setVariable("allure", allure);
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

    public Set<String> getPartiallySelectedFileKeys(final List<YamlFileDto> files)
    {
        final Set<String> partialFiles = new HashSet<>();
        final Set<String> keys = getSelectedQueueKeys();
        if (files != null)
        {
            for (final YamlFileDto f : files)
            {
                if (f.datasets != null && !f.datasets.isEmpty())
                {
                    int selectedCount = 0;
                    for (final DatasetDto d : f.datasets)
                    {
                        if (keys.contains(f.file + "::" + d.id))
                        {
                            selectedCount++;
                        }
                    }
                    if (selectedCount > 0 && selectedCount < f.datasets.size())
                    {
                        partialFiles.add(f.file);
                    }
                }
            }
        }
        return partialFiles;
    }

    public final boolean isHeadless()
    {
        return this.headless;
    }

    public final boolean isVideo()
    {
        return this.video;
    }

    public final String getExecutionMode()
    {
        return this.executionMode;
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
        populateQueueContext(context);
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
        populateQueueContext(context);
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
        populateQueueContext(context);
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
        populateQueueContext(context);
        final String html = manager.getTemplateEngine().process("fragments/queue", Set.of("queueListContainerContent", "runControls"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleClearQueue(final HttpExchange exchange) throws IOException
    {
        selectedQueue.clear();
        final Context context = new Context();
        populateQueueContext(context);
        final String html = manager.getTemplateEngine().process("fragments/queue", Set.of("queueListContainerContent", "runControls"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleToggleConfig(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String key = params.get("key");
        final String modeParam = params.get("mode") != null ? params.get("mode") : params.get("value");

        if (key != null)
        {
            switch (key)
            {
                case "headless" -> headless = !headless;
                case "video" -> video = !video;
                case "executionMode", "mode" -> {
                    if (modeParam != null && !modeParam.isBlank())
                    {
                        executionMode = modeParam.trim();
                    }
                }
                case "interactive" -> interactive = !interactive;
                case "allure" -> allure = !allure;
            }
        }
        else if (modeParam != null && !modeParam.isBlank())
        {
            executionMode = modeParam.trim();
        }

        final Context context = new Context();
        populateQueueContext(context);

        final String html = manager.getTemplateEngine().process("dashboard", Set.of("configPanel"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleToggleBrowserProfile(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String profile = params.get("profile");
        if (profile != null && !profile.isBlank())
        {
            final String trimmed = profile.trim();
            synchronized (globalBrowserProfiles)
            {
                if (globalBrowserProfiles.contains(trimmed))
                {
                    globalBrowserProfiles.remove(trimmed);
                }
                else
                {
                    globalBrowserProfiles.add(trimmed);
                }
            }
        }

        final Context context = new Context();
        populateQueueContext(context);
        final String html = manager.getTemplateEngine().process("dashboard", Set.of("configPanel", "queueListContainerContent", "runControls"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleBrowserPreset(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String preset = params.get("preset");
        final List<BrowserProfileDto> available = getAvailableBrowserProfiles();

        if (preset != null)
        {
            synchronized (globalBrowserProfiles)
            {
                switch (preset.toLowerCase())
                {
                    case "chrome-ff" -> {
                        globalBrowserProfiles.clear();
                        for (final BrowserProfileDto p : available)
                        {
                            if ("chrome".equalsIgnoreCase(p.browser))
                            {
                                globalBrowserProfiles.add(p.id);
                                break;
                            }
                        }
                        for (final BrowserProfileDto p : available)
                        {
                            if ("firefox".equalsIgnoreCase(p.browser))
                            {
                                globalBrowserProfiles.add(p.id);
                                break;
                            }
                        }
                    }
                    case "desktop" -> {
                        globalBrowserProfiles.clear();
                        for (final BrowserProfileDto p : available)
                        {
                            if (!"mobile".equalsIgnoreCase(p.browser))
                            {
                                globalBrowserProfiles.add(p.id);
                            }
                        }
                    }
                    case "mobile" -> {
                        globalBrowserProfiles.clear();
                        for (final BrowserProfileDto p : available)
                        {
                            if ("mobile".equalsIgnoreCase(p.browser))
                            {
                                globalBrowserProfiles.add(p.id);
                            }
                        }
                    }
                    case "all" -> {
                        globalBrowserProfiles.clear();
                        for (final BrowserProfileDto p : available)
                        {
                            globalBrowserProfiles.add(p.id);
                        }
                    }
                    case "clear" -> {
                        globalBrowserProfiles.clear();
                    }
                }
            }
        }

        final Context context = new Context();
        populateQueueContext(context);
        final String html = manager.getTemplateEngine().process("dashboard", Set.of("configPanel", "queueListContainerContent", "runControls"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleUpdateItemBrowserProfiles(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String file = params.get("file");
        final String id = params.get("id");
        final boolean inherit = "true".equalsIgnoreCase(params.get("inherit"));
        final String profilesParam = params.get("profiles");

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
            DatasetSelection target = null;
            if (index >= 0 && index < selectedQueue.size())
            {
                target = selectedQueue.get(index);
            }
            else if (file != null && id != null)
            {
                for (final DatasetSelection item : selectedQueue)
                {
                    if (file.equals(item.file) && id.equals(item.id))
                    {
                        target = item;
                        break;
                    }
                }
            }

            if (target != null)
            {
                if (inherit || profilesParam == null || profilesParam.isBlank())
                {
                    target.browserProfiles = null;
                }
                else
                {
                    final List<String> list = new ArrayList<>();
                    for (final String part : profilesParam.split(","))
                    {
                        final String trimmed = part.trim();
                        if (!trimmed.isEmpty())
                        {
                            list.add(trimmed);
                        }
                    }
                    target.browserProfiles = list;
                }
            }
        }

        final Context context = new Context();
        populateQueueContext(context);
        final String html = manager.getTemplateEngine().process("fragments/queue", Set.of("queueListContainerContent", "runControls"), context);
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
                req.globalBrowserProfiles = new ArrayList<>(globalBrowserProfiles);
                req.headless = headless;
                req.video = video;
                req.executionMode = executionMode;
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
            req.globalBrowserProfiles = new ArrayList<>(globalBrowserProfiles);
            req.headless = headless;
            req.video = video;
            req.executionMode = executionMode;
            req.interactive = interactive;
            req.allure = allure;
        }

        if (req.globalBrowserProfiles == null)
        {
            req.globalBrowserProfiles = new ArrayList<>(globalBrowserProfiles);
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
