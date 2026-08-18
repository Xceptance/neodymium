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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.neodymium.aura.AuraQueueService;
import com.xceptance.neodymium.aura.AuraReportingService;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit and integration tests for {@link AuraTestInteractiveController}.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class AuraTestInteractiveControllerTest
{
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AuraInteractiveService interactiveService;

    @Autowired
    private AuraReportingService reportingService;

    @Autowired
    private AuraQueueService queueService;

    private MockMvc mockMvc;
    private final File sandboxResultsDir = new File("target/aura-sandbox/allure-results");

    @BeforeEach
    public void setUp()
    {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        queueService.setManuallyStopped(false);
        interactiveService.resetExecutionIndexes();
    }

    @AfterEach
    public void tearDown()
    {
        if (sandboxResultsDir.exists())
        {
            reportingService.deleteDirRecursively(sandboxResultsDir);
        }
    }

    @Test
    public void testGetActiveAndStopEngines() throws Exception
    {
        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("test-run-active");
        interactiveService.setCurrentConsoleEngine(engine);

        mockMvc.perform(get("/api/interactive/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCount").value(1));

        mockMvc.perform(post("/api/interactive/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    public void testPushStateWritesConsoleExecutionJson() throws Exception
    {
        final String runId = "test-run-" + System.currentTimeMillis();
        final JsonObject stateJson = new JsonObject();
        stateJson.addProperty("runId", runId);
        stateJson.addProperty("testName", "Wikipedia Search Test");
        stateJson.addProperty("playbookFile", "tests/examples/wikipedia_search.yml");
        stateJson.addProperty("datasetId", "example_1");
        stateJson.addProperty("status", "Passed");

        mockMvc.perform(post("/api/console/internal/pushState")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stateJson.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        final File expectedExecutionFile = new File(sandboxResultsDir, "console-execution-1.json");
        assertTrue(expectedExecutionFile.exists(), "console-execution-1.json should be written by pushState");

        final String savedContent = Files.readString(expectedExecutionFile.toPath(), StandardCharsets.UTF_8);
        final JsonObject parsed = JsonParser.parseString(savedContent).getAsJsonObject();
        assertEquals(runId, parsed.get("runId").getAsString());
        assertEquals("Wikipedia Search Test", parsed.get("testName").getAsString());
        assertEquals("tests/examples/wikipedia_search.yml", parsed.get("playbookFile").getAsString());
        assertEquals("example_1", parsed.get("datasetId").getAsString());
    }

    @Test
    public void testPushStateReturnsStoppedWhenManuallyStopped() throws Exception
    {
        final String runId = "test-stopped-run";
        final JsonObject stateJson = new JsonObject();
        stateJson.addProperty("runId", runId);

        // Initial state push for this runId
        mockMvc.perform(post("/api/console/internal/pushState")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stateJson.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        // User stops execution via UI
        queueService.setManuallyStopped(true);

        // Subsequent state push for the same runId returns "stopped"
        mockMvc.perform(post("/api/console/internal/pushState")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stateJson.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("stopped"));
    }

    @Test
    public void testWaitForActionReturnsAbortWhenManuallyStopped() throws Exception
    {
        queueService.setManuallyStopped(true);

        mockMvc.perform(get("/api/console/internal/waitForAction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("ABORT"));
    }

    @Test
    public void testWaitForActionAndActionDepositHandshake() throws Exception
    {
        final String runId = "test-run-handshake";
        final String pauseId = "pause-token-123";

        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine(runId);
        engine.registerPauseId(pauseId);
        interactiveService.setCurrentConsoleEngine(engine);

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<MvcResult> waitResultRef = new AtomicReference<>(null);

        final Thread waitThread = new Thread(() -> {
            try
            {
                final MvcResult result = mockMvc.perform(get("/api/console/internal/waitForAction")
                                .param("pauseId", pauseId))
                        .andExpect(status().isOk())
                        .andReturn();
                waitResultRef.set(result);
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                latch.countDown();
            }
        });
        waitThread.start();

        // Give waitThread a brief moment to block on waitForAction
        Thread.sleep(150);

        final JsonObject actionJson = new JsonObject();
        actionJson.addProperty("runId", runId);
        actionJson.addProperty("pauseId", pauseId);
        actionJson.addProperty("action", "CONTINUE");

        mockMvc.perform(post("/api/console/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actionJson.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));

        final boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertTrue(completed, "waitForAction should unblock upon action submission");

        final MvcResult waitResult = waitResultRef.get();
        assertNotNull(waitResult);
        final String responseBody = waitResult.getResponse().getContentAsString();
        final JsonObject receivedAction = JsonParser.parseString(responseBody).getAsJsonObject();
        assertEquals("CONTINUE", receivedAction.get("action").getAsString());
    }

    @Test
    public void testBroadcastEndpoint() throws Exception
    {
        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("test-run-broadcast");
        interactiveService.setCurrentConsoleEngine(engine);

        final JsonObject broadcastPayload = new JsonObject();
        broadcastPayload.addProperty("event", "testEvent");
        broadcastPayload.addProperty("payload", "{\"foo\":\"bar\"}");

        mockMvc.perform(post("/api/console/internal/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(broadcastPayload.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    public void testScreenshotPathTraversalBlocked() throws Exception
    {
        mockMvc.perform(get("/api/console/screenshot").param("file", "../secret.txt"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/console/screenshot").param("file", "sub/secret.txt"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/console/screenshot").param("file", "sub\\secret.txt"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testScreenshotNotFound() throws Exception
    {
        mockMvc.perform(get("/api/console/screenshot").param("file", "nonexistent.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testScreenshotSuccess() throws Exception
    {
        final File screenshotsDir = new File("target/aura-sandbox/ai-console-screenshots");
        screenshotsDir.mkdirs();
        final File testImage = new File(screenshotsDir, "test_shot.png");
        final byte[] dummyBytes = new byte[]{1, 2, 3, 4};
        Files.write(testImage.toPath(), dummyBytes);

        try
        {
            mockMvc.perform(get("/api/console/screenshot").param("file", "test_shot.png"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG))
                    .andExpect(content().bytes(dummyBytes));
        }
        finally
        {
            testImage.delete();
        }
    }

    @Test
    public void testDisconnectEndpoint() throws Exception
    {
        mockMvc.perform(post("/api/disconnect").param("clientId", "client-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
