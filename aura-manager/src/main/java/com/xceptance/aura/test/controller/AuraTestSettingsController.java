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

import com.xceptance.neodymium.aura.AuraSettingsService;
import com.xceptance.neodymium.aura.AuraSettingsService.SettingsDataDto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for rendering and updating Neodymium property settings modal.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Controller
@RequestMapping("/api/settings")
public class AuraTestSettingsController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraTestSettingsController.class);

    private final AuraSettingsService settingsService;

    public AuraTestSettingsController(final AuraSettingsService settingsService)
    {
        this.settingsService = settingsService;
    }

    private void populateSettingsModel(final Model model, final String activeTab, final String saveMessage)
    {
        final SettingsDataDto settingsData = settingsService.loadSettingsData();
        model.addAttribute("generalGroups", settingsData.getGeneralGroups());
        model.addAttribute("browserGroup", settingsData.getBrowserGroup());
        model.addAttribute("autocompleteKeys", settingsData.getAutocompleteKeys());
        model.addAttribute("overriddenKeys", settingsData.getOverriddenKeys());
        model.addAttribute("activeTab", (activeTab != null && !activeTab.isBlank()) ? activeTab : "general");
        model.addAttribute("isOpenModal", true);
        if (saveMessage != null && !saveMessage.isBlank())
        {
            model.addAttribute("saveMessage", saveMessage);
        }
    }

    @GetMapping
    public String getSettingsModal(@RequestParam(value = "activeTab", required = false, defaultValue = "general") final String activeTab, final Model model)
    {
        populateSettingsModel(model, activeTab, null);
        return "fragments/settings-modal :: settingsModal";
    }

    @GetMapping("/json")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getSettingsJson()
    {
        final Map<String, String> map = new HashMap<>();
        final SettingsDataDto data = settingsService.loadSettingsData();
        if (data != null && data.getGeneralGroups() != null)
        {
            for (final com.xceptance.neodymium.aura.AuraSettingsService.PropertyGroupDto group : data.getGeneralGroups())
            {
                if (group.getSections() != null)
                {
                    for (final com.xceptance.neodymium.aura.AuraSettingsService.PropertySectionDto sec : group.getSections())
                    {
                        if (sec.getEntries() != null)
                        {
                            for (final com.xceptance.neodymium.aura.AuraSettingsService.PropertyEntryDto entry : sec.getEntries())
                            {
                                if (entry.getKey() != null)
                                {
                                    map.put(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
                                }
                            }
                        }
                    }
                }
            }
        }
        return ResponseEntity.ok(map);
    }

    @PostMapping("/save")
    public String saveSettings(@RequestBody(required = false) final MultiValueMap<String, String> formData,
                               final Model model)
    {
        try
        {
            final String activeTab = (formData != null && formData.containsKey("activeTab") && !formData.get("activeTab").isEmpty())
                    ? formData.getFirst("activeTab")
                    : "general";

            if (formData != null)
            {
                final Map<String, Map<String, String>> filePropertyUpdates = new HashMap<>();
                final List<Map.Entry<String, String>> devNeoEntries = new ArrayList<>();

                final List<String> devNeoKeys = formData.getOrDefault("devNeoKey", List.of());
                final List<String> devNeoValues = formData.getOrDefault("devNeoValue", List.of());

                for (int i = 0; i < devNeoKeys.size(); i++)
                {
                    final String k = devNeoKeys.get(i).trim();
                    final String v = (i < devNeoValues.size()) ? devNeoValues.get(i).trim() : "";
                    if (!k.isEmpty())
                    {
                        devNeoEntries.add(Map.entry(k, v));
                    }
                }

                for (final Map.Entry<String, List<String>> entry : formData.entrySet())
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

                for (final Map.Entry<String, Map<String, String>> update : filePropertyUpdates.entrySet())
                {
                    settingsService.savePropertyFile(update.getKey(), update.getValue());
                }

                settingsService.saveDevNeoProperties(devNeoEntries);
            }

            populateSettingsModel(model, activeTab, "Settings saved successfully!");
        }
        catch (final IOException e)
        {
            LOGGER.error("Error saving settings: {}", e.getMessage(), e);
            populateSettingsModel(model, "general", "Error saving settings: " + e.getMessage());
        }
        return "fragments/settings-modal :: settingsModal";
    }

    @PostMapping("/add-browser-profile")
    public String addBrowserProfile(@RequestParam(value = "newProfileName", required = false, defaultValue = "New_Profile") final String profileName,
                                    @RequestParam(value = "newBrowserType", required = false, defaultValue = "chrome") final String browserType,
                                    final Model model)
    {
        try
        {
            settingsService.addBrowserProfile(profileName, browserType);
            populateSettingsModel(model, "browser", "New browser profile '" + profileName.trim() + "' added!");
        }
        catch (final IOException e)
        {
            LOGGER.error("Error adding browser profile: {}", e.getMessage(), e);
            populateSettingsModel(model, "browser", "Error adding profile: " + e.getMessage());
        }
        return "fragments/settings-modal :: settingsModal";
    }

    @PostMapping("/add-browser-property")
    public String addBrowserProperty(@RequestParam(value = "profileTag", required = false, defaultValue = "global") final String profileTag,
                                     @RequestParam(value = "newPropertyName", required = false, defaultValue = "") final String propertyName,
                                     @RequestParam(value = "newPropertyValue", required = false, defaultValue = "") final String propertyValue,
                                     final Model model)
    {
        try
        {
            if (!propertyName.trim().isEmpty())
            {
                settingsService.addBrowserProperty(profileTag, propertyName, propertyValue);
                populateSettingsModel(model, "browser", "Property '" + propertyName.trim() + "' added to profile '" + profileTag + "'!");
            }
            else
            {
                populateSettingsModel(model, "browser", null);
            }
        }
        catch (final IOException e)
        {
            LOGGER.error("Error adding browser property: {}", e.getMessage(), e);
            populateSettingsModel(model, "browser", "Error adding property: " + e.getMessage());
        }
        return "fragments/settings-modal :: settingsModal";
    }
}
