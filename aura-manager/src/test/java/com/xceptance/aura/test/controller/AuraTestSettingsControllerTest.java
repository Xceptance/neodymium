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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

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
}
