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

import com.xceptance.aura.mcp.config.AuraMcpProperties;
import com.xceptance.aura.mcp.session.McpSubscriptionManager;
import com.xceptance.aura.mcp.tool.AuraMcpToolService;
import com.xceptance.neodymium.aura.AuraSettingsService;
import com.xceptance.neodymium.aura.AuraSettingsService.SettingsDataDto;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
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
public class AuraTestSettingsController implements ApplicationListener<WebServerInitializedEvent>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraTestSettingsController.class);

    private final AuraSettingsService settingsService;
    private final AuraMcpProperties mcpProperties;
    private final McpSubscriptionManager subscriptionManager;
    private final AuraMcpToolService toolService;

    @Value("${server.port:8080}")
    private int configuredServerPort = 8080;

    private volatile int activeServerPort = -1;

    @Value("${spring.ai.mcp.server.protocol:SSE}")
    private String serverProtocol = "SSE";

    public AuraTestSettingsController(final AuraSettingsService settingsService,
                                      final AuraMcpProperties mcpProperties,
                                      final McpSubscriptionManager subscriptionManager,
                                      final AuraMcpToolService toolService)
    {
        this.settingsService = settingsService;
        this.mcpProperties = mcpProperties;
        this.subscriptionManager = subscriptionManager;
        this.toolService = toolService;
    }

    @Override
    public void onApplicationEvent(final WebServerInitializedEvent event)
    {
        this.activeServerPort = event.getWebServer().getPort();
    }

    private int getServerPort()
    {
        return (activeServerPort > 0) ? activeServerPort : configuredServerPort;
    }

    private String resolveEndpointPath()
    {
        return "SSE".equalsIgnoreCase(serverProtocol) ? "/sse" : "/mcp";
    }

    private String resolveFullEndpointUrl()
    {
        return "http://localhost:" + getServerPort() + resolveEndpointPath();
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

        // MCP Server Tab Attributes
        final String endpointUrl = resolveFullEndpointUrl();
        final String protocolName = serverProtocol != null ? serverProtocol.toUpperCase() : "SSE";
        final int registeredToolsCount = resolveMcpToolsList().size();

        model.addAttribute("mcpEnabled", mcpProperties.isEnabled());
        model.addAttribute("mcpToken", mcpProperties.getAuth() != null ? mcpProperties.getAuth().getToken() : "");
        model.addAttribute("mcpHeartbeat", mcpProperties.getHeartbeatIntervalSeconds());
        model.addAttribute("mcpEndpointUrl", endpointUrl);
        model.addAttribute("mcpServerPort", getServerPort());
        model.addAttribute("mcpProtocol", protocolName);
        final String endpointDesc = switch (protocolName)
        {
            case "STATELESS" -> "Stateless HTTP POST endpoint URL for Antigravity, Claude Desktop, or Cursor agents.";
            case "STREAMABLE" -> "Streamable HTTP endpoint URL for Claude Desktop, Cursor, or Antigravity agents.";
            default -> "Server-Sent Events (SSE) endpoint URL for Claude Desktop, Cursor, or Antigravity agents.";
        };
        model.addAttribute("mcpEndpointDesc", endpointDesc);
        model.addAttribute("mcpToolsCount", registeredToolsCount);
        model.addAttribute("mcpToolsList", resolveMcpToolsList());
        model.addAttribute("mcpActiveSessionsCount", subscriptionManager != null ? subscriptionManager.getActiveSessionCount() : 0);
        model.addAttribute("mcpClientConfigSnippet", String.format("""
{
  "mcpServers": {
    "neodymium": {
      "serverUrl": "%s"
    }
  }
}""", endpointUrl));

        if (saveMessage != null && !saveMessage.isBlank())
        {
            model.addAttribute("saveMessage", saveMessage);
        }
    }

    private List<Map<String, String>> resolveMcpToolsList()
    {
        final List<Map<String, String>> tools = new ArrayList<>();
        if (toolService != null)
        {
            for (final Method method : toolService.getClass().getDeclaredMethods())
            {
                if (method.isAnnotationPresent(McpTool.class))
                {
                    final McpTool toolAnn = method.getAnnotation(McpTool.class);
                    final StringBuilder paramsDesc = new StringBuilder();
                    for (final Parameter param : method.getParameters())
                    {
                        if (param.isAnnotationPresent(McpToolParam.class))
                        {
                            final McpToolParam paramAnn = param.getAnnotation(McpToolParam.class);
                            if (!paramsDesc.isEmpty())
                            {
                                paramsDesc.append(", ");
                            }
                            paramsDesc.append(param.getName()).append(" (").append(paramAnn.description()).append(")");
                        }
                    }
                    final Map<String, String> item = new HashMap<>();
                    item.put("name", toolAnn.name());
                    item.put("description", toolAnn.description());
                    item.put("parameters", paramsDesc.isEmpty() ? "None" : paramsDesc.toString());
                    tools.add(item);
                }
            }
        }
        return tools;
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

                // Update runtime MCP properties if present in formData
                if (formData.containsKey("mcpEnabled"))
                {
                    mcpProperties.setEnabled(Boolean.parseBoolean(formData.getFirst("mcpEnabled")));
                }
                if (formData.containsKey("mcpToken"))
                {
                    if (mcpProperties.getAuth() == null)
                    {
                        mcpProperties.setAuth(new AuraMcpProperties.Auth());
                    }
                    mcpProperties.getAuth().setToken(formData.getFirst("mcpToken"));
                }
                if (formData.containsKey("mcpHeartbeat"))
                {
                    try
                    {
                        mcpProperties.setHeartbeatIntervalSeconds(Integer.parseInt(formData.getFirst("mcpHeartbeat")));
                    }
                    catch (final NumberFormatException ignored)
                    {
                    }
                }
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

    @GetMapping("/mcp/sessions")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMcpSessions()
    {
        final Map<String, Object> result = new HashMap<>();
        if (subscriptionManager != null)
        {
            final Map<String, Instant> sessions = subscriptionManager.getSessionDetails();
            final List<Map<String, String>> sessionList = new ArrayList<>();
            for (final Map.Entry<String, Instant> entry : sessions.entrySet())
            {
                final Map<String, String> item = new HashMap<>();
                item.put("sessionId", entry.getKey());
                item.put("lastActive", entry.getValue().toString());
                sessionList.add(item);
            }
            result.put("activeCount", sessions.size());
            result.put("sessions", sessionList);
        }
        else
        {
            result.put("activeCount", 0);
            result.put("sessions", List.of());
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/mcp/self-test")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> runMcpSelfTest()
    {
        final Map<String, Object> response = new HashMap<>();
        final boolean enabled = mcpProperties != null && mcpProperties.isEnabled();
        final String endpointUrl = resolveFullEndpointUrl();
        final String protocolName = serverProtocol != null ? serverProtocol.toUpperCase() : "SSE";

        response.put("enabled", enabled);
        response.put("protocol", protocolName);
        response.put("endpoint", endpointUrl);
        response.put("messageEndpoint", "http://localhost:" + getServerPort() + "/mcp/message");

        if (!enabled)
        {
            response.put("status", "DISABLED");
            response.put("message", "MCP Server is disabled in settings.");
            return ResponseEntity.ok(response);
        }

        try
        {
            final URI uri = URI.create(endpointUrl);
            final HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(1500))
                    .build();

            final HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMillis(2000));

            if (mcpProperties != null && mcpProperties.getAuth() != null && mcpProperties.getAuth().getToken() != null && !mcpProperties.getAuth().getToken().isBlank())
            {
                reqBuilder.header("Authorization", "Bearer " + mcpProperties.getAuth().getToken().trim());
            }

            final HttpRequest request;
            if ("STREAMABLE".equalsIgnoreCase(protocolName) || "STATELESS".equalsIgnoreCase(protocolName))
            {
                final String initPayload = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":"
                        + "{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},"
                        + "\"clientInfo\":{\"name\":\"aura-manager-selftest\",\"version\":\"1.0\"}}}";
                request = reqBuilder
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json, text/event-stream")
                        .POST(HttpRequest.BodyPublishers.ofString(initPayload))
                        .build();
            }
            else
            {
                request = reqBuilder
                        .header("Accept", "text/event-stream")
                        .GET()
                        .build();
            }

            final HttpResponse<Void> httpResponse = client.send(
                    request, HttpResponse.BodyHandlers.discarding());

            final int statusCode = httpResponse.statusCode();
            response.put("httpStatus", statusCode);
            if (statusCode == 200)
            {
                response.put("status", "HEALTHY");
                response.put("message", protocolName + " handshake successful (HTTP 200). Server is listening.");
            }
            else
            {
                response.put("status", "UNEXPECTED_STATUS");
                response.put("message", "Endpoint responded with HTTP " + statusCode);
            }
        }
        catch (final Exception e)
        {
            LOGGER.warn("[Aura MCP] Self-test handshake failed: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "Handshake test failed: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}
