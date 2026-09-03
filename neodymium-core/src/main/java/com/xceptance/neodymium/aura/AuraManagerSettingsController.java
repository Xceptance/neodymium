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
import com.xceptance.neodymium.aura.AuraSettingsService.SettingsDataDto;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.Context;

/**
 * Controller handling settings modal rendering and property file saving.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraManagerSettingsController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraManagerSettingsController.class);

    private final AuraSettingsService settingsService;
    private final NeodymiumAuraManager manager;

    public AuraManagerSettingsController(final AuraSettingsService settingsService, final NeodymiumAuraManager manager)
    {
        this.settingsService = settingsService;
        this.manager = manager;
    }

    public void handleGetSettingsModal(final HttpExchange exchange) throws IOException
    {
        try
        {
            final String query = exchange.getRequestURI().getQuery();
            final String activeTab = parseQueryParam(query, "activeTab", "general");

            final SettingsDataDto settingsData = settingsService.loadSettingsData();
            final Context context = new Context();
            context.setVariable("generalGroups", settingsData.getGeneralGroups());
            context.setVariable("browserGroup", settingsData.getBrowserGroup());
            context.setVariable("autocompleteKeys", settingsData.getAutocompleteKeys());
            context.setVariable("overriddenKeys", settingsData.getOverriddenKeys());
            context.setVariable("activeTab", activeTab);
            context.setVariable("isOpenModal", true);

            final String html = manager.getTemplateEngine().process("fragments/settings-modal", Set.of("settingsModal"), context);
            AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
        }
        catch (final Exception e)
        {
            LOGGER.error("[Aura Server] Exception in handleGetSettingsModal: {}", e.getMessage(), e);
            AuraHttpUtils.sendError(exchange, 500, e.getMessage());
        }
    }

    public void handleSaveSettings(final HttpExchange exchange) throws IOException
    {
        try
        {
            final String body = AuraHttpUtils.readBody(exchange);
            final Map<String, List<String>> formMultiParams = parseMultiFormParams(body);

            final String query = exchange.getRequestURI().getQuery();
            final String formActiveTab = formMultiParams.containsKey("activeTab") && !formMultiParams.get("activeTab").isEmpty()
                    ? formMultiParams.get("activeTab").get(0)
                    : parseQueryParam(query, "activeTab", "general");

            // Group parameters by file name
            final Map<String, Map<String, String>> filePropertyUpdates = new HashMap<>();
            final List<Map.Entry<String, String>> devNeoEntries = new ArrayList<>();

            // Process dev-neodymium.properties keys/values
            final List<String> devNeoKeys = formMultiParams.getOrDefault("devNeoKey", List.of());
            final List<String> devNeoValues = formMultiParams.getOrDefault("devNeoValue", List.of());

            for (int i = 0; i < devNeoKeys.size(); i++)
            {
                final String k = devNeoKeys.get(i).trim();
                final String v = (i < devNeoValues.size()) ? devNeoValues.get(i).trim() : "";
                if (!k.isEmpty())
                {
                    devNeoEntries.add(Map.entry(k, v));
                }
            }

            // Process standard property file fields: prop__<filename>__<key>
            for (final Map.Entry<String, List<String>> entry : formMultiParams.entrySet())
            {
                final String paramName = entry.getKey();
                if (paramName.startsWith("prop__"))
                {
                    final String[] parts = paramName.split("__", 3);
                    if (parts.length == 3)
                    {
                        final String fileName = parts[1];
                        final String key = parts[2];
                        final String val = entry.getValue().isEmpty() ? "" : entry.getValue().get(0);

                        filePropertyUpdates.computeIfAbsent(fileName, k -> new HashMap<>()).put(key, val);
                    }
                }
            }

            // Save standard property files & browser.properties
            for (final Map.Entry<String, Map<String, String>> update : filePropertyUpdates.entrySet())
            {
                settingsService.savePropertyFile(update.getKey(), update.getValue());
            }

            // Save dev-neodymium.properties
            settingsService.saveDevNeoProperties(devNeoEntries);

            // Reload updated settings
            final SettingsDataDto settingsData = settingsService.loadSettingsData();
            final Context context = new Context();
            context.setVariable("generalGroups", settingsData.getGeneralGroups());
            context.setVariable("browserGroup", settingsData.getBrowserGroup());
            context.setVariable("autocompleteKeys", settingsData.getAutocompleteKeys());
            context.setVariable("overriddenKeys", settingsData.getOverriddenKeys());
            context.setVariable("activeTab", formActiveTab);
            context.setVariable("saveMessage", "Settings saved successfully!");
            context.setVariable("isOpenModal", true);

            final String html = manager.getTemplateEngine().process("fragments/settings-modal", Set.of("settingsModal"), context);
            AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
        }
        catch (final Exception e)
        {
            LOGGER.error("[Aura Server] Exception in handleSaveSettings: {}", e.getMessage(), e);
            AuraHttpUtils.sendError(exchange, 500, e.getMessage());
        }
    }

    public void handleAddBrowserProfile(final HttpExchange exchange) throws IOException
    {
        try
        {
            final String body = AuraHttpUtils.readBody(exchange);
            final Map<String, List<String>> formMultiParams = parseMultiFormParams(body);

            final String profileName = formMultiParams.containsKey("newProfileName") && !formMultiParams.get("newProfileName").isEmpty()
                    ? formMultiParams.get("newProfileName").get(0)
                    : "New_Profile";
            final String browserType = formMultiParams.containsKey("newBrowserType") && !formMultiParams.get("newBrowserType").isEmpty()
                    ? formMultiParams.get("newBrowserType").get(0)
                    : "chrome";

            settingsService.addBrowserProfile(profileName, browserType);

            // Reload updated settings modal fragment with activeTab = browser
            final SettingsDataDto settingsData = settingsService.loadSettingsData();
            final Context context = new Context();
            context.setVariable("generalGroups", settingsData.getGeneralGroups());
            context.setVariable("browserGroup", settingsData.getBrowserGroup());
            context.setVariable("autocompleteKeys", settingsData.getAutocompleteKeys());
            context.setVariable("overriddenKeys", settingsData.getOverriddenKeys());
            context.setVariable("activeTab", "browser");
            context.setVariable("saveMessage", "New browser profile '" + profileName.trim() + "' added!");
            context.setVariable("isOpenModal", true);

            final String html = manager.getTemplateEngine().process("fragments/settings-modal", Set.of("settingsModal"), context);
            AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
        }
        catch (final Exception e)
        {
            LOGGER.error("[Aura Server] Exception in handleAddBrowserProfile: {}", e.getMessage(), e);
            AuraHttpUtils.sendError(exchange, 500, e.getMessage());
        }
    }

    public void handleAddBrowserProperty(final HttpExchange exchange) throws IOException
    {
        try
        {
            final String body = AuraHttpUtils.readBody(exchange);
            final Map<String, List<String>> formMultiParams = parseMultiFormParams(body);

            final String profileTag = formMultiParams.containsKey("profileTag") && !formMultiParams.get("profileTag").isEmpty()
                    ? formMultiParams.get("profileTag").get(0)
                    : "global";
            final String propertyName = formMultiParams.containsKey("newPropertyName") && !formMultiParams.get("newPropertyName").isEmpty()
                    ? formMultiParams.get("newPropertyName").get(0)
                    : "";
            final String propertyValue = formMultiParams.containsKey("newPropertyValue") && !formMultiParams.get("newPropertyValue").isEmpty()
                    ? formMultiParams.get("newPropertyValue").get(0)
                    : "";

            if (!propertyName.trim().isEmpty())
            {
                settingsService.addBrowserProperty(profileTag, propertyName, propertyValue);
            }

            // Reload updated settings modal fragment with activeTab = browser
            final SettingsDataDto settingsData = settingsService.loadSettingsData();
            final Context context = new Context();
            context.setVariable("generalGroups", settingsData.getGeneralGroups());
            context.setVariable("browserGroup", settingsData.getBrowserGroup());
            context.setVariable("autocompleteKeys", settingsData.getAutocompleteKeys());
            context.setVariable("overriddenKeys", settingsData.getOverriddenKeys());
            context.setVariable("activeTab", "browser");
            context.setVariable("saveMessage", "Property '" + propertyName.trim() + "' added to profile '" + profileTag + "'!");
            context.setVariable("isOpenModal", true);

            final String html = manager.getTemplateEngine().process("fragments/settings-modal", Set.of("settingsModal"), context);
            AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
        }
        catch (final Exception e)
        {
            LOGGER.error("[Aura Server] Exception in handleAddBrowserProperty: {}", e.getMessage(), e);
            AuraHttpUtils.sendError(exchange, 500, e.getMessage());
        }
    }

    private String parseQueryParam(final String query, final String paramName, final String defaultValue)
    {
        if (query == null || query.isEmpty())
        {
            return defaultValue;
        }
        for (final String param : query.split("&"))
        {
            final String[] pair = param.split("=", 2);
            if (pair[0].equalsIgnoreCase(paramName) && pair.length > 1)
            {
                return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }
        return defaultValue;
    }

    private Map<String, List<String>> parseMultiFormParams(final String body)
    {
        final Map<String, List<String>> map = new HashMap<>();
        if (body == null || body.isEmpty())
        {
            return map;
        }

        for (final String param : body.split("&"))
        {
            final String[] pair = param.split("=", 2);
            final String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            final String val = (pair.length > 1) ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";

            map.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
        }
        return map;
    }
}

