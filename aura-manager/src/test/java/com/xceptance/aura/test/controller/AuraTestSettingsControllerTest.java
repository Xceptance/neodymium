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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xceptance.aura.mcp.config.AuraMcpProperties;
import com.xceptance.aura.mcp.session.McpSubscriptionManager;
import com.xceptance.aura.mcp.tool.AuraAnalyticsToolService;
import com.xceptance.aura.mcp.tool.AuraMcpToolService;
import com.xceptance.neodymium.aura.AuraSettingsService;
import com.xceptance.neodymium.aura.AuraSettingsService.SettingsDataDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Unit tests for AuraTestSettingsController MCP status, session details, and self-test diagnostics.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class AuraTestSettingsControllerTest
{
    private AuraSettingsService settingsService;
    private AuraMcpProperties mcpProperties;
    private McpSubscriptionManager subscriptionManager;
    private AuraMcpToolService toolService;
    private AuraTestSettingsController controller;

    @BeforeEach
    public void setUp()
    {
        settingsService = Mockito.mock(AuraSettingsService.class);
        mcpProperties = new AuraMcpProperties();
        subscriptionManager = Mockito.mock(McpSubscriptionManager.class);
        toolService = Mockito.mock(AuraMcpToolService.class);

        final SettingsDataDto mockData = new SettingsDataDto(List.of(), null, List.of(), Set.of());
        Mockito.when(settingsService.loadSettingsData()).thenReturn(mockData);

        controller = new AuraTestSettingsController(settingsService, mcpProperties, subscriptionManager, toolService);
    }

    @Test
    public void testGetSettingsModalPopulatesMcpAttributes()
    {
        mcpProperties.setEnabled(true);
        mcpProperties.setHeartbeatIntervalSeconds(45);
        Mockito.when(subscriptionManager.getActiveSessionCount()).thenReturn(3);

        final Model model = new ConcurrentModel();
        final String view = controller.getSettingsModal("mcp", model);

        Assertions.assertEquals("fragments/settings-modal :: settingsModal", view);
        Assertions.assertEquals(true, model.getAttribute("mcpEnabled"));
        Assertions.assertEquals("http://localhost:8080/sse", model.getAttribute("mcpEndpointUrl"));
        Assertions.assertEquals(3, model.getAttribute("mcpActiveSessionsCount"));
        Assertions.assertNotNull(model.getAttribute("mcpToolsCount"));
        Assertions.assertNotNull(model.getAttribute("mcpToolsList"));
    }

    @Test
    public void testGetMcpSessionsReturnsActiveSessionsList()
    {
        final Instant now = Instant.now();
        Mockito.when(subscriptionManager.getSessionDetails()).thenReturn(Map.of("session-123", now));

        final ResponseEntity<Map<String, Object>> response = controller.getMcpSessions();

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        final Map<String, Object> body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(1, body.get("activeCount"));

        @SuppressWarnings("unchecked")
        final List<Map<String, String>> sessions = (List<Map<String, String>>) body.get("sessions");
        Assertions.assertEquals(1, sessions.size());
        Assertions.assertEquals("session-123", sessions.getFirst().get("sessionId"));
        Assertions.assertEquals(now.toString(), sessions.getFirst().get("lastActive"));
    }

    @Test
    public void testRunMcpSelfTestDisabledServer()
    {
        mcpProperties.setEnabled(false);

        final ResponseEntity<Map<String, Object>> response = controller.runMcpSelfTest();

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        final Map<String, Object> body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals("DISABLED", body.get("status"));
        Assertions.assertEquals(false, body.get("enabled"));
    }

    @Test
    public void testRunMcpSelfTestBuildsCorrectStatelessRequest() throws NoSuchFieldException, IllegalAccessException
    {
        mcpProperties.setEnabled(true);
        final java.lang.reflect.Field protocolField = AuraTestSettingsController.class.getDeclaredField("serverProtocol");
        protocolField.setAccessible(true);
        protocolField.set(controller, "STATELESS");

        final ResponseEntity<Map<String, Object>> response = controller.runMcpSelfTest();

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        final Map<String, Object> body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(true, body.get("enabled"));
        Assertions.assertEquals("STATELESS", body.get("protocol"));
        Assertions.assertTrue(body.get("endpoint").toString().contains("/mcp"));
    }

    @Test
    public void testRunMcpSelfTestPropagatesBearerToken() throws NoSuchFieldException, IllegalAccessException
    {
        mcpProperties.setEnabled(true);
        mcpProperties.getAuth().setToken("secret-12345");
        final java.lang.reflect.Field protocolField = AuraTestSettingsController.class.getDeclaredField("serverProtocol");
        protocolField.setAccessible(true);
        protocolField.set(controller, "STATELESS");

        final ResponseEntity<Map<String, Object>> response = controller.runMcpSelfTest();

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        final Map<String, Object> body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(true, body.get("enabled"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetSettingsModalIncludesAnalyticsToolsWhenConfigured()
    {
        final AuraAnalyticsToolService analyticsToolService = Mockito.mock(AuraAnalyticsToolService.class);
        final AuraTestSettingsController fullController = new AuraTestSettingsController(
                settingsService, mcpProperties, subscriptionManager, toolService, analyticsToolService);

        final Model model = new ConcurrentModel();
        fullController.getSettingsModal("mcp", model);

        Assertions.assertEquals(15, model.getAttribute("mcpToolsCount"));
        final List<Map<String, String>> tools = (List<Map<String, String>>) model.getAttribute("mcpToolsList");
        Assertions.assertNotNull(tools);
        final Set<String> toolNames = tools.stream()
                .map(t -> t.get("name"))
                .collect(Collectors.toSet());
        Assertions.assertTrue(toolNames.containsAll(Set.of(
                "list_tests",
                "list_custom_dashboards",
                "analyze_dashboard_page",
                "list_runs",
                "list_batches")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBeanFactoryConstructorDiscoversCustomMcpToolsAndJsonPropertyNames()
    {
        final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("toolService", toolService);
        beanFactory.addBean("customTools", new SampleCustomToolBean());

        final AuraTestSettingsController beanFactoryController = new AuraTestSettingsController(
                settingsService, mcpProperties, subscriptionManager, beanFactory);

        final Model model = new ConcurrentModel();
        beanFactoryController.getSettingsModal("mcp", model);

        Assertions.assertEquals(12, model.getAttribute("mcpToolsCount"));
        final List<Map<String, String>> tools = (List<Map<String, String>>) model.getAttribute("mcpToolsList");
        Assertions.assertNotNull(tools);
        final Map<String, String> customTool = tools.stream()
                .filter(t -> "custom_echo".equals(t.get("name")))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("echo_text (Text to echo)", customTool.get("parameters"));
    }

    private static final class SampleCustomToolBean
    {
        @McpTool(name = "custom_echo", description = "Echoes text")
        public String echo(
                @JsonProperty("echo_text")
                @McpToolParam(required = true, description = "Text to echo") final String rawText)
        {
            return rawText;
        }
    }

    @Test
    public void testSaveSettingsPersistsPropertyFileAndDevNeoOverrides() throws Exception
    {
        final MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("activeTab", "general");
        formData.add("prop__ai.properties__neodymium.ai.apiKey", "secret-key-123");
        formData.add("devNeoKey", "neodymium.ai.timeoutSeconds");
        formData.add("devNeoValue", "60");

        final Model model = new ConcurrentModel();
        final String view = controller.saveSettings(formData, model);

        Assertions.assertEquals("fragments/settings-modal :: settingsModal", view);
        Assertions.assertEquals("Settings saved successfully!", model.getAttribute("saveMessage"));
        Assertions.assertEquals("general", model.getAttribute("activeTab"));

        Mockito.verify(settingsService).savePropertyFile("ai.properties", Map.of("neodymium.ai.apiKey", "secret-key-123"));
        Mockito.verify(settingsService).saveDevNeoProperties(List.of(Map.entry("neodymium.ai.timeoutSeconds", "60")));
    }

    @Test
    public void testSaveSettingsHandlesNullOrEmptyFormDataGracefully() throws Exception
    {
        final Model model = new ConcurrentModel();
        final String view = controller.saveSettings(null, model);

        Assertions.assertEquals("fragments/settings-modal :: settingsModal", view);
        Assertions.assertEquals("Settings saved successfully!", model.getAttribute("saveMessage"));
    }

    @Test
    public void testSaveSettingsSupportsHyphenDelimiter() throws Exception
    {
        final MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("activeTab", "general");
        formData.add("prop--ai.properties--neodymium.ai.apiKey", "secret-key-456");
        formData.add("prop--browser.properties--browserprofile.global.headless", "true");

        final Model model = new ConcurrentModel();
        final String view = controller.saveSettings(formData, model);

        Assertions.assertEquals("fragments/settings-modal :: settingsModal", view);
        Assertions.assertEquals("Settings saved successfully!", model.getAttribute("saveMessage"));

        Mockito.verify(settingsService).savePropertyFile("ai.properties", Map.of("neodymium.ai.apiKey", "secret-key-456"));
        Mockito.verify(settingsService).savePropertyFile("browser.properties", Map.of("browserprofile.global.headless", "true"));
    }

    @Test
    public void testGetApiKeyStatus()
    {
        Mockito.when(settingsService.hasConfiguredApiKey()).thenReturn(true);
        Mockito.when(settingsService.isApiKeyWarningIgnored()).thenReturn(false);
        Mockito.when(settingsService.getProjectId()).thenReturn("test-proj-12345");

        final ResponseEntity<Map<String, Object>> response = controller.getApiKeyStatus();
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(true, response.getBody().get("hasApiKey"));
        Assertions.assertEquals(false, response.getBody().get("warningIgnored"));
        Assertions.assertEquals("test-proj-12345", response.getBody().get("projectId"));
    }

    @Test
    public void testIgnoreApiKeyWarning()
    {
        Mockito.when(settingsService.getProjectId()).thenReturn("test-proj-12345");

        final ResponseEntity<Map<String, Object>> response = controller.ignoreApiKeyWarning();
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(true, response.getBody().get("success"));
        Assertions.assertEquals(true, response.getBody().get("warningIgnored"));
        Assertions.assertEquals("test-proj-12345", response.getBody().get("projectId"));
        Mockito.verify(settingsService).setApiKeyWarningIgnored(true);
    }
}
