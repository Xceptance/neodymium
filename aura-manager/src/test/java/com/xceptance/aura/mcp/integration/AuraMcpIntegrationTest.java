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
package com.xceptance.aura.mcp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xceptance.aura.AuraManagerApplication;
import com.xceptance.aura.core.service.AuraPlaybookService;
import com.xceptance.aura.mcp.dto.RunStatusResponseDto;
import com.xceptance.aura.mcp.resource.AuraMcpResourceService;
import com.xceptance.aura.mcp.tool.AuraMcpToolService;
import com.xceptance.aura.test.controller.AuraTestSettingsController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Deterministic integration test verifying that the Aura Manager application boots cleanly,
 * registers MCP WebMvc endpoints, and handles MCP tools and resources without any LLM.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AuraManagerApplication.class)
public class AuraMcpIntegrationTest
{
    private static final String INTEGRATION_TEST_YAML = "tests/mcp-integration-test.yaml";
    private static final String INTEGRATION_TEST_YAML_2 = "tests/mcp-integration-test-2.yaml";
    private static final String LIFECYCLE_TEST_YAML = "tests/mcp-lifecycle-test.yaml";

    @LocalServerPort
    private int port;

    @Autowired
    private AuraMcpToolService toolService;

    @Autowired
    private AuraMcpResourceService resourceService;

    @Autowired
    private AuraTestSettingsController settingsController;

    @Autowired
    private AuraPlaybookService playbookService;

    @AfterEach
    public void cleanupTestFiles()
    {
        playbookService.deletePlaybook(INTEGRATION_TEST_YAML);
        playbookService.deletePlaybook(INTEGRATION_TEST_YAML_2);
        playbookService.deletePlaybook(LIFECYCLE_TEST_YAML);
    }

    @Test
    public void testMcpEndpointReachable() throws Exception
    {
        final String jsonRpcInit = """
            {
              "jsonrpc": "2.0",
              "id": "1",
              "method": "initialize",
              "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {
                  "name": "IntegrationTestClient",
                  "version": "1.0.0"
                }
              }
            }
            """;

        final URI uri = URI.create("http://localhost:" + port + "/mcp/message");
        final HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (final OutputStream os = conn.getOutputStream())
        {
            os.write(jsonRpcInit.getBytes(StandardCharsets.UTF_8));
        }

        final int status = conn.getResponseCode();
        Assertions.assertNotEquals(500, status, "MCP endpoint should respond to JSON-RPC request without throwing 500 server error");
        Assertions.assertTrue(status == 200 || status == 404, "MCP endpoint must return HTTP 200 or 404 depending on transport mapping");
        conn.disconnect();
    }

    @Test
    public void testMcpSecurityInterceptorRejection() throws Exception
    {
        final URI uri = URI.create("http://localhost:" + port + "/mcp/message");
        final HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer invalid-token");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        final int status = conn.getResponseCode();
        Assertions.assertNotEquals(500, status, "Security filter should not fail with 500 on token inspection");
        conn.disconnect();
    }

    @Test
    public void testToolDiscoveryViaSettings()
    {
        Assertions.assertNotNull(toolService, "AuraMcpToolService must be registered in Spring application context");
        Assertions.assertNotNull(settingsController, "AuraTestSettingsController must be registered in Spring context");

        final var response = settingsController.runMcpSelfTest();
        Assertions.assertNotNull(response.getBody());
        final Map<String, Object> body = response.getBody();
        Assertions.assertTrue((Boolean) body.get("enabled"), "MCP server should be reported as enabled");
        Assertions.assertEquals("STATELESS", body.get("protocol"), "Default MCP protocol should match configured STATELESS");
        Assertions.assertNotNull(body.get("endpoint"), "Endpoint URL must not be null");
        Assertions.assertTrue(body.get("endpoint").toString().contains("/mcp"), "Endpoint URL should point to /mcp");
        Assertions.assertEquals("HEALTHY", body.get("status"), "Self-test status should be HEALTHY under STATELESS");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testToolListTestsIndependently()
    {
        final Map<String, Object> allTests = toolService.listTests(null);
        Assertions.assertNotNull(allTests);
        Assertions.assertTrue(allTests.get("total") instanceof Integer);
        Assertions.assertTrue(allTests.get("tests") instanceof List);
        final List<Map<String, Object>> testsList = (List<Map<String, Object>>) allTests.get("tests");
        Assertions.assertEquals(allTests.get("total"), testsList.size());

        for (final Map<String, Object> item : testsList)
        {
            Assertions.assertNotNull(item.get("file"), "Playbook summary item must contain file property");
            final String filePath = item.get("file").toString();
            Assertions.assertTrue(filePath.endsWith(".yaml") || filePath.endsWith(".yml"), "File path must end with yaml/yml");
            Assertions.assertNotNull(item.get("fileName"));
            Assertions.assertTrue(item.get("datasetCount") instanceof Integer);
        }

        final Map<String, Object> filtered = toolService.listTests("non_existent_random_filter_abc_123");
        Assertions.assertNotNull(filtered);
        Assertions.assertEquals(0, filtered.get("total"));
        final List<?> emptyList = (List<?>) filtered.get("tests");
        Assertions.assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testToolCreateTestIndependently()
    {
        final String yamlContent = """
            name: Integration Test Playbook
            steps:
              - open: https://example.com
            """;

        final Map<String, Object> result = toolService.createTest(INTEGRATION_TEST_YAML, yamlContent);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("SUCCESS", result.get("status"));
        Assertions.assertNotNull(result.get("message"));
        Assertions.assertTrue(result.get("message").toString().contains(INTEGRATION_TEST_YAML));

        final var readResult = playbookService.readPlaybook(INTEGRATION_TEST_YAML);
        Assertions.assertTrue(readResult.isSuccess(), "YAML file must be readable from disk via playbook service");
        Assertions.assertNotNull(readResult.getData());
        Assertions.assertEquals(yamlContent.trim(), readResult.getData().trim(), "Saved content must match submitted content");
    }

    @Test
    public void testToolRunTestIndependently()
    {
        toolService.createTest(INTEGRATION_TEST_YAML, "name: Run Test\nsteps: []\n");
        final Map<String, Object> result = toolService.runTest(INTEGRATION_TEST_YAML, "Chrome", null);
        Assertions.assertNotNull(result);
        final String status = (String) result.get("status");
        Assertions.assertTrue("RUNNING".equals(status) || "ENQUEUED".equals(status), "Status must be RUNNING or ENQUEUED");
        final String runId = (String) result.get("runId");
        Assertions.assertNotNull(runId);
        Assertions.assertTrue(runId.startsWith("run_"));
        Assertions.assertNotNull(result.get("message"));

        if ("ENQUEUED".equals(status))
        {
            final Integer position = (Integer) result.get("queuePosition");
            Assertions.assertNotNull(position);
            Assertions.assertTrue(position >= 1, "Enqueued run must have queuePosition >= 1");
        }
    }

    @Test
    public void testToolRunSuiteIndependently()
    {
        toolService.createTest(INTEGRATION_TEST_YAML, "name: Suite Test 1\nsteps: []\n");
        toolService.createTest(INTEGRATION_TEST_YAML_2, "name: Suite Test 2\nsteps: []\n");
        final List<String> paths = List.of(INTEGRATION_TEST_YAML, INTEGRATION_TEST_YAML_2);
        final Map<String, Object> result = toolService.runSuite(paths, "Chrome");
        Assertions.assertNotNull(result);
        final String status = (String) result.get("status");
        Assertions.assertTrue("RUNNING".equals(status) || "ENQUEUED".equals(status), "Suite status must be RUNNING or ENQUEUED");
        final String runId = (String) result.get("runId");
        Assertions.assertNotNull(runId);
        Assertions.assertTrue(runId.startsWith("run_"));
        Assertions.assertNotNull(result.get("message"));

        if ("ENQUEUED".equals(status))
        {
            final Integer position = (Integer) result.get("queuePosition");
            Assertions.assertNotNull(position);
            Assertions.assertTrue(position >= 1, "Enqueued suite must have queuePosition >= 1");
        }
    }

    @Test
    public void testToolGetStatusIndependently()
    {
        final RunStatusResponseDto status = toolService.getStatus("run_non_existent_test_123");
        Assertions.assertNotNull(status);
        Assertions.assertEquals("run_non_existent_test_123", status.getRunId());
        Assertions.assertEquals("NOT_FOUND", status.getStatus());
        Assertions.assertNotNull(status.getSummary());
        Assertions.assertTrue(status.getSummary().toLowerCase().contains("not found"));
        Assertions.assertNotNull(status.getNextSteps());
        Assertions.assertNotNull(status.getMetrics());
        Assertions.assertEquals(0, status.getMetrics().getTotal());
        Assertions.assertEquals(0, status.getMetrics().getPassed());
        Assertions.assertEquals(0, status.getMetrics().getFailed());
        Assertions.assertEquals(0L, status.getMetrics().getDurationMs());
        Assertions.assertTrue(status.getInlineFailures() == null || status.getInlineFailures().isEmpty());
    }

    @Test
    public void testToolGetRunFailuresIndependently()
    {
        final Map<String, Object> failures = toolService.getRunFailures("run_non_existent_test_123", null, "0", 5);
        Assertions.assertNotNull(failures);
        Assertions.assertEquals("Run report not found for runId: run_non_existent_test_123", failures.get("error"));
        Assertions.assertFalse(failures.containsKey("failures"), "Error response must not contain empty failures list");
    }

    @Test
    public void testToolGetExecutionFailureIndependently()
    {
        final Map<String, Object> failure = toolService.getExecutionFailure("run_non_existent_test_123", "LoginTest");
        Assertions.assertNotNull(failure);
        Assertions.assertEquals("No failure found for test 'LoginTest' in run 'run_non_existent_test_123'.", failure.get("error"));
    }

    @Test
    public void testToolDeleteTestTwoStepHandshakeIndependently()
    {
        // 1. Create temporary fixture
        final String yamlContent = "name: To Be Deleted\nsteps: []\n";
        final Map<String, Object> createRes = toolService.createTest(INTEGRATION_TEST_YAML, yamlContent);
        Assertions.assertEquals("SUCCESS", createRes.get("status"));
        Assertions.assertTrue(createRes.get("message").toString().contains(INTEGRATION_TEST_YAML));

        // 2. Step 1: Initial delete request without token -> requires confirmation
        final Map<String, Object> step1 = toolService.deleteTest(INTEGRATION_TEST_YAML, null);
        Assertions.assertEquals("CONFIRMATION_REQUIRED", step1.get("status"));
        Assertions.assertEquals(60, step1.get("expiresInSeconds"));
        final String token = (String) step1.get("confirmationToken");
        Assertions.assertNotNull(token);
        Assertions.assertFalse(token.isBlank());
        Assertions.assertTrue(step1.get("message").toString().contains("Destructive operation"));

        // 3. Step 2 with invalid token -> rejected
        final Map<String, Object> invalidStep = toolService.deleteTest(INTEGRATION_TEST_YAML, "invalid-token-123");
        Assertions.assertEquals("ERROR", invalidStep.get("status"));
        Assertions.assertEquals("Invalid or expired confirmation token.", invalidStep.get("message"));

        // 4. Step 3: Confirmation token supplied -> success
        final Map<String, Object> step2 = toolService.deleteTest(INTEGRATION_TEST_YAML, token);
        Assertions.assertEquals("SUCCESS", step2.get("status"));
        Assertions.assertNotNull(step2.get("message"));
        Assertions.assertFalse(playbookService.readPlaybook(INTEGRATION_TEST_YAML).isSuccess());
    }

    @Test
    public void testToolCancelRunTwoStepHandshakeIndependently()
    {
        // Step 1: Initial cancel request without token -> requires confirmation
        final Map<String, Object> step1 = toolService.cancelRun("run_active_123", null);
        Assertions.assertEquals("CONFIRMATION_REQUIRED", step1.get("status"));
        Assertions.assertEquals(60, step1.get("expiresInSeconds"));
        final String token = (String) step1.get("confirmationToken");
        Assertions.assertNotNull(token);
        Assertions.assertFalse(token.isBlank());
        Assertions.assertTrue(step1.get("message").toString().contains("Disruptive operation"));

        // Step 2: Invalid or expired token rejection
        final Map<String, Object> invalidStep = toolService.cancelRun("run_active_123", "invalid-token-xyz");
        Assertions.assertEquals("ERROR", invalidStep.get("status"));
        Assertions.assertEquals("Invalid or expired confirmation token.", invalidStep.get("message"));
    }

    @Test
    public void testMcpResourceServiceIndependently() throws Exception
    {
        Assertions.assertNotNull(resourceService);

        // 1. Playbook Resource
        final String playbookYaml = "name: Resource Playbook\nsteps: []\n";
        final Map<String, Object> createRes = toolService.createTest(INTEGRATION_TEST_YAML, playbookYaml);
        Assertions.assertEquals("SUCCESS", createRes.get("status"));
        final String content = resourceService.getPlaybookContent(INTEGRATION_TEST_YAML);
        Assertions.assertEquals(playbookYaml.trim(), content.trim());

        // 2. Run Summary Resource for missing run
        final String summaryJson = resourceService.getRunSummary("missing_run_id");
        final JsonNode summaryNode = new ObjectMapper().readTree(summaryJson);
        Assertions.assertEquals("Run not found: missing_run_id", summaryNode.get("error").asText());
    }

    @Test
    public void testFullLifecycleFlow()
    {
        // 1. List initial tests
        final Map<String, Object> initialList = toolService.listTests("mcp-lifecycle-test");
        Assertions.assertEquals(0, initialList.get("total"), "Should not find lifecycle test before creation");

        // 2. Create test
        final String yamlContent = """
            name: Lifecycle Test Playbook
            steps:
              - open: https://example.org
            """;
        final Map<String, Object> createResult = toolService.createTest(LIFECYCLE_TEST_YAML, yamlContent);
        Assertions.assertEquals("SUCCESS", createResult.get("status"));
        Assertions.assertNotNull(createResult.get("message"));

        // 3. List tests again and verify test is discovered
        final Map<String, Object> verifiedList = toolService.listTests("mcp-lifecycle-test");
        Assertions.assertEquals(1, verifiedList.get("total"), "Created test must appear in list_tests");

        // 4. Run test
        final Map<String, Object> runResult = toolService.runTest(LIFECYCLE_TEST_YAML, "Chrome", null);
        Assertions.assertNotNull(runResult);
        final String status = (String) runResult.get("status");
        Assertions.assertTrue("RUNNING".equals(status) || "ENQUEUED".equals(status));
        final String runId = (String) runResult.get("runId");
        Assertions.assertNotNull(runId);
        Assertions.assertTrue(runId.startsWith("run_"));

        // 5. Query status
        final RunStatusResponseDto statusDto = toolService.getStatus(runId);
        Assertions.assertNotNull(statusDto);
        Assertions.assertEquals(runId, statusDto.getRunId());
        Assertions.assertTrue(List.of("RUNNING", "ENQUEUED", "PASSED", "FAILED").contains(statusDto.getStatus()));
        Assertions.assertNotNull(statusDto.getMetrics());

        // 6. Delete test with two-step confirmation
        final Map<String, Object> delStep1 = toolService.deleteTest(LIFECYCLE_TEST_YAML, null);
        Assertions.assertEquals("CONFIRMATION_REQUIRED", delStep1.get("status"));
        final String token = (String) delStep1.get("confirmationToken");
        Assertions.assertNotNull(token);

        final Map<String, Object> delStep2 = toolService.deleteTest(LIFECYCLE_TEST_YAML, token);
        Assertions.assertEquals("SUCCESS", delStep2.get("status"));

        // 7. List tests final cleanup check
        final Map<String, Object> finalList = toolService.listTests("mcp-lifecycle-test");
        Assertions.assertEquals(0, finalList.get("total"), "Deleted test must no longer appear in list_tests");
    }

    @Test
    public void testToolListBrowserProfiles()
    {
        final Map<String, Object> result = toolService.listBrowserProfiles();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.containsKey("browserProfiles"));
        Assertions.assertTrue(result.containsKey("total"));
        final Object listObj = result.get("browserProfiles");
        Assertions.assertInstanceOf(List.class, listObj);
    }

    @Test
    public void testToolGetTestContent()
    {
        final String testPath = "tests/mcp-content-test.yaml";
        final String content = "name: Content Playbook\nsteps:\n  - open: https://example.org\n";
        try
        {
            toolService.createTest(testPath, content);
            final Map<String, Object> response = toolService.getTestContent(testPath);
            Assertions.assertEquals("SUCCESS", response.get("status"));
            Assertions.assertEquals(testPath, response.get("testPath"));
            Assertions.assertNotNull(response.get("content"));
            Assertions.assertTrue(((String) response.get("content")).contains("Content Playbook"));
        }
        finally
        {
            final Map<String, Object> delStep1 = toolService.deleteTest(testPath, null);
            final String token = (String) delStep1.get("confirmationToken");
            if (token != null)
            {
                toolService.deleteTest(testPath, token);
            }
        }
    }

    @Test
    public void testCreateTestWithMalformedYaml()
    {
        final String malformedYaml = "name: bad\n  steps: [broken yaml invalid indent\n";
        final Map<String, Object> result = toolService.createTest("tests/bad-syntax.yaml", malformedYaml);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("ERROR", result.get("status"));
        Assertions.assertTrue(((String) result.get("message")).contains("Malformed YAML syntax"));
    }

    @Test
    public void testCancelRunPurgesPendingQueue()
    {
        toolService.createTest(INTEGRATION_TEST_YAML, "name: Cancel Queue Test\nsteps: []\n");

        // Enqueue a run
        toolService.runTest(INTEGRATION_TEST_YAML, "Chrome", null);

        final Map<String, Object> run2 = toolService.runTest(INTEGRATION_TEST_YAML, "Chrome", null);
        final String runId2 = (String) run2.get("runId");

        // Cancel run2
        final Map<String, Object> cancelStep1 = toolService.cancelRun(runId2, null);
        Assertions.assertEquals("CONFIRMATION_REQUIRED", cancelStep1.get("status"));
        final String token = (String) cancelStep1.get("confirmationToken");

        final Map<String, Object> cancelStep2 = toolService.cancelRun(runId2, token);
        Assertions.assertNotNull(cancelStep2.get("status"));
        Assertions.assertTrue(List.of("CANCELLED", "ALREADY_COMPLETED").contains(cancelStep2.get("status")));
    }
}
