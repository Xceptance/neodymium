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
import com.google.gson.JsonObject;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.google.gson.Gson;
import com.xceptance.neodymium.aura.AuraFileService;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.neodymium.aura.AuraQueueService;
import com.xceptance.neodymium.aura.dto.BrowserGroupDto;
import com.xceptance.neodymium.aura.dto.BrowserProfileDto;
import com.xceptance.neodymium.aura.dto.DatasetDto;
import com.xceptance.neodymium.aura.dto.DatasetSelection;
import com.xceptance.neodymium.aura.dto.RunRequest;
import com.xceptance.neodymium.aura.dto.YamlFileDto;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.neodymium.common.browser.configuration.BrowserConfiguration;
import org.neodymium.common.browser.configuration.MultibrowserConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.Context;

/**
 * Controller for managing the execution queue, status polling, and running test playbooks and test classes.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Controller
@RequestMapping
public class AuraTestQueueController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraTestQueueController.class);
    private static final Gson GSON = new Gson();

    private final AuraQueueService queueService;

    private final AuraFileService fileService;

    private final AuraInteractiveService interactiveService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<DatasetSelection> selectedQueue = Collections.synchronizedList(new ArrayList<>());

    private final Set<String> globalBrowserProfiles = Collections.synchronizedSet(new LinkedHashSet<>());

    private boolean headless = true;

    private boolean video = false;

    private String executionMode = "LLM_RECORDING";

    private boolean interactive = false;

    public AuraTestQueueController(final AuraQueueService queueService, final AuraFileService fileService,
        final AuraInteractiveService interactiveService)
    {
        this.queueService = queueService;
        this.fileService = fileService;
        this.interactiveService = interactiveService;

        final List<BrowserProfileDto> available = getAvailableBrowserProfiles();
        if (!available.isEmpty())
        {
            boolean added = false;
            // 1. Prefer available Chrome profile
            for (final BrowserProfileDto p : available)
            {
                if (p.available && ("chrome".equalsIgnoreCase(p.browser) || p.id.toLowerCase().contains("chrome")))
                {
                    globalBrowserProfiles.add(p.id);
                    added = true;
                    break;
                }
            }
            // 2. If no Chrome profile available, pick first available profile
            if (!added)
            {
                for (final BrowserProfileDto p : available)
                {
                    if (p.available)
                    {
                        globalBrowserProfiles.add(p.id);
                        added = true;
                        break;
                    }
                }
            }
            // 3. Fallback only if no profile is available
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

    private void populateQueueModel(final Model model)
    {
        model.addAttribute("queue", selectedQueue);
        model.addAttribute("running", queueService.isRunningQueue());
        model.addAttribute("headless", headless);
        model.addAttribute("video", video);
        model.addAttribute("interactive", interactive);
        model.addAttribute("executionMode", executionMode);
        model.addAttribute("selectedBrowser", "chrome");
        model.addAttribute("globalBrowserProfiles", globalBrowserProfiles);
        model.addAttribute("availableBrowserProfiles", getAvailableBrowserProfiles());
        model.addAttribute("browserGroups", getGroupedBrowserProfiles(globalBrowserProfiles));
        model.addAttribute("totalRuns", getTotalExecutionRuns());
        model.addAttribute("queueController", this);
        model.addAttribute("activeEditingFile", fileService.getActiveEditingFile());
    }

    private Map<String, String> extractParams(final HttpServletRequest request)
    {
        final Map<String, String> result = new HashMap<>();
        if (request == null)
        {
            return result;
        }
        final Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements())
        {
            final String name = paramNames.nextElement();
            final String val = request.getParameter(name);
            if (val != null)
            {
                result.put(name, val);
            }
            if (name != null && (name.startsWith("{") || name.contains("\"file\"")))
            {
                try
                {
                    final String cleanName = name.replace("\\\"", "\"").replace("&quot;", "\"");
                    if (cleanName.startsWith("{") && cleanName.endsWith("}"))
                    {
                        final Map<?, ?> map = objectMapper.readValue(cleanName, Map.class);
                        for (final Map.Entry<?, ?> entry : map.entrySet())
                        {
                            if (entry.getKey() != null && entry.getValue() != null && !result.containsKey(String.valueOf(entry.getKey())))
                            {
                                result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                            }
                        }
                    }
                }
                catch (final Exception e)
                {
                    // ignore
                }
            }
        }
        if (!result.containsKey("file") && request.getQueryString() != null)
        {
            for (final String pair : request.getQueryString().split("&"))
            {
                final String[] kv = pair.split("=", 2);
                if (kv.length == 2)
                {
                    final String k = java.net.URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                    final String v = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    if (!result.containsKey(k))
                    {
                        result.put(k, v);
                    }
                }
            }
        }
        if (!result.containsKey("file"))
        {
            try
            {
                final byte[] bytes = request.getInputStream().readAllBytes();
                if (bytes.length > 0)
                {
                    final String body = new String(bytes, StandardCharsets.UTF_8);
                    final String cleanBody = body.trim().replace("\\\"", "\"").replace("&quot;", "\"");
                    if (cleanBody.startsWith("{") && cleanBody.endsWith("}"))
                    {
                        final Map<?, ?> map = objectMapper.readValue(cleanBody, Map.class);
                        for (final Map.Entry<?, ?> entry : map.entrySet())
                        {
                            if (entry.getKey() != null && entry.getValue() != null && !result.containsKey(String.valueOf(entry.getKey())))
                            {
                                result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                            }
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                // ignore
            }
        }
        if (result.containsKey("path") && !result.containsKey("file"))
        {
            result.put("file", result.get("path"));
        }
        return result;
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
                boolean hasWholeFileSelection = false;
                synchronized (selectedQueue)
                {
                    for (final DatasetSelection sel : selectedQueue)
                    {
                        if (f.file.equals(sel.file) && (sel.id == null || sel.id.isBlank()))
                        {
                            hasWholeFileSelection = true;
                            break;
                        }
                    }
                }
                if (hasWholeFileSelection)
                {
                    fullFiles.add(f.file);
                    continue;
                }

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
                else
                {
                    synchronized (selectedQueue)
                    {
                        for (final DatasetSelection sel : selectedQueue)
                        {
                            if (f.file.equals(sel.file))
                            {
                                fullFiles.add(f.file);
                                break;
                            }
                        }
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

    public final Set<String> getGlobalBrowserProfiles()
    {
        return this.globalBrowserProfiles;
    }

    public final void setGlobalBrowserProfiles(final Collection<String> profiles)
    {
        final Set<String> availableIds = new HashSet<>();
        for (final BrowserProfileDto p : getAvailableBrowserProfiles())
        {
            if (p.available)
            {
                availableIds.add(p.id);
            }
        }
        synchronized (this.globalBrowserProfiles)
        {
            this.globalBrowserProfiles.clear();
            if (profiles != null)
            {
                for (final String prof : profiles)
                {
                    if (availableIds.contains(prof))
                    {
                        this.globalBrowserProfiles.add(prof);
                    }
                }
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
        return queueService.getAvailableBrowserProfiles();
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
        return types.isEmpty() ? List.of("chrome") : new ArrayList<>(types);
    }

    public boolean isHeadless()
    {
        return this.headless;
    }

    public boolean isVideo()
    {
        return this.video;
    }

    public String getExecutionMode()
    {
        return this.executionMode;
    }

    public boolean isInteractive()
    {
        return this.interactive;
    }

    public boolean isRunning()
    {
        return queueService.isRunningQueue();
    }

    public int getGlobalTestsRun()
    {
        return queueService.getGlobalTestsRun();
    }

    public int getGlobalPassed()
    {
        return queueService.getGlobalPassed();
    }

    public int getGlobalFailed()
    {
        return queueService.getGlobalFailed();
    }

    public int getGlobalSkipped()
    {
        return queueService.getGlobalSkipped();
    }

    @PostMapping("/api/queue/toggle")
    public String toggleQueue(final HttpServletRequest request, final Model model)
    {
        final Map<String, String> params = extractParams(request);
        final String file = params.get("file");
        final String idParam = params.get("id");
        final String id = idParam != null ? idParam : "";

        if (file != null && !file.isBlank())
        {
            if (id.isBlank())
            {
                final List<YamlFileDto> files = fileService.getYamlFilesList();
                List<DatasetDto> datasets = null;
                for (final YamlFileDto f : files)
                {
                    if (file.equals(f.file))
                    {
                        datasets = f.datasets;
                        break;
                    }
                }
                if (datasets != null && !datasets.isEmpty())
                {
                    return toggleAllQueue(request, model);
                }
            }

            synchronized (selectedQueue)
            {
                boolean removed = false;
                for (int i = 0; i < selectedQueue.size(); i++)
                {
                    final DatasetSelection item = selectedQueue.get(i);
                    final String itemId = (item.id != null) ? item.id : "";
                    if (file.equals(item.file) && id.equals(itemId))
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

        populateQueueModel(model);
        return "fragments/queue :: queueListContainerContent";
    }

    @PostMapping("/api/queue/toggleAll")
    public String toggleAllQueue(final HttpServletRequest request, final Model model)
    {
        final Map<String, String> params = extractParams(request);
        final String file = params.get("file");

        if (file != null && !file.isBlank())
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
                    final boolean shouldAdd = (count < datasets.size());

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
            else
            {
                synchronized (selectedQueue)
                {
                    boolean found = false;
                    for (final DatasetSelection item : selectedQueue)
                    {
                        if (targetFile.equals(item.file))
                        {
                            found = true;
                            break;
                        }
                    }
                    final boolean shouldAdd = !found;

                    if (shouldAdd)
                    {
                        boolean inQueue = false;
                        for (final DatasetSelection item : selectedQueue)
                        {
                            if (targetFile.equals(item.file))
                            {
                                inQueue = true;
                                break;
                            }
                        }
                        if (!inQueue)
                        {
                            final DatasetSelection sel = new DatasetSelection();
                            sel.file = targetFile;
                            sel.id = "";
                            selectedQueue.add(sel);
                        }
                    }
                    else
                    {
                        selectedQueue.removeIf(item -> targetFile.equals(item.file));
                    }
                }
            }
        }

        populateQueueModel(model);
        return "fragments/queue :: queueListContainerContent";
    }

    @PostMapping("/api/queue/move")
    public String moveQueue(final HttpServletRequest request, final Model model)
    {
        final Map<String, String> params = extractParams(request);
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
        return "fragments/queue :: queueListContainerContent";
    }

    @PostMapping("/api/queue/remove")
    public String removeFromQueue(final HttpServletRequest request, final Model model)
    {
        final Map<String, String> params = extractParams(request);
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
        return "fragments/queue :: queueListContainerContent";
    }

    @PostMapping("/api/queue/clear")
    public String clearQueue(final Model model)
    {
        selectedQueue.clear();
        final Context context = new Context();
        context.setVariable("queue", selectedQueue);
        context.setVariable("running", isRunning());
        context.setVariable("activeEditingFile", fileService.getActiveEditingFile());
        return "fragments/queue :: queueListContainerContent";
    }

    @PostMapping("/api/config/toggle")
    public String toggleConfig(final HttpServletRequest request, final Model model)
    {
        final Map<String, String> params = extractParams(request);
        final String key = params.get("key");
        final String modeParam = params.get("mode") != null ? params.get("mode") : params.get("value");

        if (key != null)
        {
            switch (key)
            {
                case "headless" -> headless = !headless;
                case "video" -> video = !video;
                case "interactive" -> interactive = !interactive;
                case "executionMode", "mode" -> {
                    if (modeParam != null && !modeParam.isBlank())
                    {
                        executionMode = modeParam.trim();
                    }
                }
            }
        }
        else if (modeParam != null && !modeParam.isBlank())
        {
            executionMode = modeParam.trim();
        }

        populateQueueModel(model);
        return "fragments/queue :: configPanel";
    }

    @PostMapping("/api/config/mode")
    public String setConfigMode(final HttpServletRequest request, final Model model)
    {
        final Map<String, String> params = extractParams(request);
        String mode = params.get("mode");
        if (mode == null || mode.isBlank())
        {
            mode = params.get("value");
        }

        if (mode != null && !mode.isBlank())
        {
            executionMode = mode.trim();
        }
        populateQueueModel(model);
        return "fragments/queue :: configPanel";
    }

    @PostMapping({"/api/config/browser", "/api/config/browser/toggle"})
    public String toggleBrowserProfile(final HttpServletRequest request, final Model model)
    {
        final Map<String, String> params = extractParams(request);
        final String profileParam = params.get("profile");
        final String action = params.get("action");

        if (profileParam != null && !profileParam.isBlank())
        {
            final String profile = profileParam.trim();
            final List<BrowserProfileDto> all = getAvailableBrowserProfiles();
            final BrowserProfileDto target = all.stream().filter(p -> profile.equalsIgnoreCase(p.id)).findFirst().orElse(null);
            if (target == null || !target.available)
            {
                if ("remove".equalsIgnoreCase(action))
                {
                    synchronized (globalBrowserProfiles)
                    {
                        globalBrowserProfiles.remove(profile);
                    }
                }
                populateQueueModel(model);
                return "fragments/queue :: configPanel";
            }

            synchronized (globalBrowserProfiles)
            {
                if ("add".equalsIgnoreCase(action))
                {
                    globalBrowserProfiles.add(profile);
                }
                else if ("remove".equalsIgnoreCase(action))
                {
                    globalBrowserProfiles.remove(profile);
                }
                else
                {
                    if (globalBrowserProfiles.contains(profile))
                    {
                        globalBrowserProfiles.remove(profile);
                    }
                    else
                    {
                        globalBrowserProfiles.add(profile);
                    }
                }
            }
        }

        populateQueueModel(model);
        return "fragments/queue :: configPanel";
    }

    @PostMapping("/api/config/browser/preset")
    public String applyBrowserPreset(@RequestParam(value = "preset", required = false) final String preset, final Model model)
    {
        final List<BrowserProfileDto> all = getAvailableBrowserProfiles();
        synchronized (globalBrowserProfiles)
        {
            globalBrowserProfiles.clear();
            if ("chrome-ff".equalsIgnoreCase(preset))
            {
                for (final BrowserProfileDto p : all)
                {
                    if (p.available && ("chrome".equalsIgnoreCase(p.browser) || "firefox".equalsIgnoreCase(p.browser)))
                    {
                        globalBrowserProfiles.add(p.id);
                    }
                }
            }
            else if ("desktop".equalsIgnoreCase(preset))
            {
                for (final BrowserProfileDto p : all)
                {
                    if (p.available && !"mobile".equalsIgnoreCase(p.browser))
                    {
                        globalBrowserProfiles.add(p.id);
                    }
                }
            }
            else if ("mobile".equalsIgnoreCase(preset))
            {
                for (final BrowserProfileDto p : all)
                {
                    if (p.available && "mobile".equalsIgnoreCase(p.browser))
                    {
                        globalBrowserProfiles.add(p.id);
                    }
                }
            }
            else if ("all".equalsIgnoreCase(preset))
            {
                for (final BrowserProfileDto p : all)
                {
                    if (p.available)
                    {
                        globalBrowserProfiles.add(p.id);
                    }
                }
            }
        }

        populateQueueModel(model);
        return "fragments/queue :: configPanel";
    }

    @PostMapping({"/api/queue/item/browser", "/api/queue/item-browser"})
    public String setItemBrowserProfiles(final HttpServletRequest request, final Model model)
    {
        final Map<String, String> params = extractParams(request);
        int index = -1;
        if (params.containsKey("index"))
        {
            try
            {
                index = Integer.parseInt(params.get("index"));
            }
            catch (final NumberFormatException ignored)
            {
            }
        }

        final String profilesStr = params.get("profiles");
        final List<String> profiles = new ArrayList<>();
        if (profilesStr != null && !profilesStr.isBlank())
        {
            for (final String s : profilesStr.split(","))
            {
                if (!s.trim().isEmpty())
                {
                    profiles.add(s.trim());
                }
            }
        }

        synchronized (selectedQueue)
        {
            if (index >= 0 && index < selectedQueue.size())
            {
                selectedQueue.get(index).browserProfiles = profiles;
            }
        }

        populateQueueModel(model);
        return "fragments/queue :: queueListContainerContent";
    }

    private String renderExecutionStatePanels(final Model model)
    {
        model.addAttribute("running", queueService.isRunningQueue());
        model.addAttribute("total", queueService.getGlobalTestsRun());
        model.addAttribute("passed", queueService.getGlobalPassed());
        model.addAttribute("failed", queueService.getGlobalFailed());
        model.addAttribute("skipped", queueService.getGlobalSkipped());
        model.addAttribute("queue", selectedQueue);
        model.addAttribute("activeEditingFile", fileService.getActiveEditingFile());

        return "fragments/queue :: runControls";
    }

    @PostMapping(
    {
      "/api/queue/start", "/api/run"
    })
    public String startExecution(final HttpServletRequest request, final Model model)
    {
        final Map<String, String> params = extractParams(request);
        final boolean runCurrent = "true".equalsIgnoreCase(params.get("runCurrent")) || "true".equalsIgnoreCase(request.getParameter("runCurrent"));

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
                req.executionMode = executionMode;
                req.interactive = interactive;
            }
        }

        if (req == null)
        {
            try
            {
                if (request != null && request.getInputStream() != null)
                {
                    final String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                    if (body.startsWith("{"))
                    {
                        req = GSON.fromJson(body, RunRequest.class);
                    }
                }
            }
            catch (Exception ignored)
            {
            }
        }

        if (req == null || req.datasets == null || req.datasets.isEmpty())
        {
            req = new RunRequest();
            req.datasets = new ArrayList<>(selectedQueue);
            req.headless = headless;
            req.video = video;
            req.executionMode = executionMode;
            req.interactive = interactive;
        }

        if (req.globalBrowserProfiles == null || req.globalBrowserProfiles.isEmpty())
        {
            req.globalBrowserProfiles = new ArrayList<>(globalBrowserProfiles);
        }

        if (req.datasets == null || req.datasets.isEmpty())
        {
            LOGGER.error("[Aura Server] Run queue request failed: No datasets in queue");
            return renderExecutionStatePanels(model);
        }

        if (queueService.isRunningQueue())
        {
            LOGGER.error("[Aura Server] Run queue request failed: A queue is already executing");
            return renderExecutionStatePanels(model);
        }

        LOGGER.info("[Aura Server] Spawning test run queue for {} dataset(s) (headless={}, interactive={})",
                    req.datasets.size(), req.headless, req.interactive);

        final int port = request != null && request.getServerPort() > 0 ? request.getServerPort() : 8080;
        queueService.executeQueue(req, port);

        return renderExecutionStatePanels(model);
    }

    @PostMapping(
    {
      "/api/queue/stop", "/api/stop"
    })
    public String stopExecution(final Model model)
    {
        LOGGER.info("[Aura Server] User requested to stop active execution subprocess");
        queueService.stopProcess();
        return renderExecutionStatePanels(model);
    }

    @GetMapping("/api/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatusStream(
                                                               @RequestParam(value = "clientId", required = false) final String clientIdParam,
                                                               @RequestParam(value = "lastIndex", required = false, defaultValue = "0") final int lastIndex,
                                                               @RequestParam(value = "lastEventIndex", required = false, defaultValue = "0") final int lastEventIndex)
    {
        final String clientId = (clientIdParam != null && !clientIdParam.isBlank()) ? clientIdParam : "fallback-" + UUID.randomUUID().toString();

        final Map<String, Object> statusObj = new HashMap<>();
        statusObj.put("type", "status");
        statusObj.put("sessionId", "aura-test-manager-session");
        statusObj.put("total", queueService.getGlobalTestsRun());
        statusObj.put("passed", queueService.getGlobalPassed());
        statusObj.put("failed", queueService.getGlobalFailed());
        statusObj.put("skipped", queueService.getGlobalSkipped());
        statusObj.put("running", queueService.isRunningQueue());
        statusObj.put("activeFile", queueService.getActiveFile());

        String activeTestId = "";
        if (interactiveService != null)
        {
            final InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
            if (engine != null)
            {
                final String stateJson = engine.getCurrentStateJson();
                if (stateJson != null)
                {
                    try
                    {
                        final JsonObject stateObj = GSON.fromJson(stateJson, JsonObject.class);
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
        }
        statusObj.put("activeTestId", activeTestId);

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
        statusObj.put("tests", datasetsList);
        statusObj.put("completedFiles", new ArrayList<>(queueService.getCompletedFiles()));

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
        response.put("status", statusObj);
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

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/status/panel")
    public String getStatusPanel(final Model model)
    {
        model.addAttribute("running", queueService.isRunningQueue());
        model.addAttribute("total", queueService.getGlobalTestsRun());
        model.addAttribute("passed", queueService.getGlobalPassed());
        model.addAttribute("failed", queueService.getGlobalFailed());
        model.addAttribute("skipped", queueService.getGlobalSkipped());
        model.addAttribute("queue", selectedQueue);
        model.addAttribute("activeEditingFile", fileService.getActiveEditingFile());
        return "fragments/queue :: statsPanelContent";
    }

    @GetMapping("/api/queue/state")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getQueueState()
    {
        final Map<String, Object> state = new HashMap<>();
        state.put("queue", selectedQueue);
        state.put("executionInProgress", queueService.isRunningQueue());
        state.put("headless", headless);
        state.put("video", video);
        state.put("executionMode", executionMode);
        state.put("selectedBrowser", "chrome");
        return ResponseEntity.ok(state);
    }

    public List<DatasetSelection> getRunQueue()
    {
        return selectedQueue;
    }

    public boolean isExecutionInProgress()
    {
        return queueService.isRunningQueue();
    }

    public String getSelectedBrowser()
    {
        return "chrome";
    }
}
