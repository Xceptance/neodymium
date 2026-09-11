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
package com.xceptance.aura.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.neodymium.aura.AuraQueueService;
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
import org.springframework.util.FileSystemUtils;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit and integration tests for {@link AuraInteractiveController}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class AuraInteractiveControllerTest
{
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AuraInteractiveService interactiveService;

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
            FileSystemUtils.deleteRecursively(sandboxResultsDir);
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

        mockMvc.perform(post("/api/disconnect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void handlePushStateAndResetIndexes() throws Exception
    {
        final JsonObject statePayload1 = new JsonObject();
        statePayload1.addProperty("runId", "run-101");
        statePayload1.addProperty("testName", "SampleTestClass");
        statePayload1.addProperty("status", "running");

        mockMvc.perform(post("/api/console/internal/pushState")
                .contentType(MediaType.APPLICATION_JSON)
                .content(statePayload1.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        assertEquals(1, interactiveService.getExecutionIndex("SampleTestClass"));

        final JsonObject statePayload2 = new JsonObject();
        statePayload2.addProperty("runId", "run-102");
        statePayload2.addProperty("testName", "SampleTestClass");
        statePayload2.addProperty("status", "running");

        mockMvc.perform(post("/api/console/internal/pushState")
                .contentType(MediaType.APPLICATION_JSON)
                .content(statePayload2.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        assertEquals("run-102", interactiveService.getLastProcessedRunId());

        final File baseDir = new File("storage/runs/run-102/sampletestclass");
        if (baseDir.exists())
        {
            FileSystemUtils.deleteRecursively(new File("storage/runs/run-102"));
        }
    }

    @Test
    public void handleWaitForActionStoppedAndManual() throws Exception
    {
        queueService.setManuallyStopped(true);

        mockMvc.perform(get("/api/console/internal/waitForAction")
                .param("pauseId", "pause-test-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.action").value("ABORT"));

        queueService.setManuallyStopped(false);
        final InteractiveConsoleEngine engine = interactiveService.getOrCreateConsoleEngine();

        final AtomicReference<String> asyncResponse = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        final Thread requestThread = new Thread(() -> {
            try
            {
                final MvcResult result = mockMvc.perform(get("/api/console/internal/waitForAction")
                        .param("pauseId", "pause-async-2"))
                        .andExpect(status().isOk())
                        .andReturn();
                asyncResponse.set(result.getResponse().getContentAsString());
            }
            catch (final Exception e)
            {
                asyncResponse.set(e.getMessage());
            }
            finally
            {
                latch.countDown();
            }
        });
        requestThread.start();

        Thread.sleep(150);

        final JsonObject userAction = new JsonObject();
        userAction.addProperty("action", "RESUME");
        userAction.addProperty("pauseId", "pause-async-2");
        engine.submitAction(userAction);

        assertTrue(latch.await(3, TimeUnit.SECONDS), "Timed out waiting for waitForAction to return");
        assertNotNull(asyncResponse.get());
        assertTrue(asyncResponse.get().contains("RESUME"));
    }

    @Test
    public void testActionHandling() throws Exception
    {
        interactiveService.getOrCreateConsoleEngine();

        final JsonObject actionJson = new JsonObject();
        actionJson.addProperty("action", "PAUSE");

        mockMvc.perform(post("/api/console/action")
                .contentType(MediaType.APPLICATION_JSON)
                .content(actionJson.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("already-handled"));

        mockMvc.perform(post("/api/console/action")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid-json{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testScreenshotHandlingWithValidation() throws Exception
    {
        mockMvc.perform(get("/api/console/screenshot")
                .param("file", "../../secret.txt"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/console/screenshot")
                .param("file", "nonexistent.png"))
                .andExpect(status().isNotFound());

        final File testDir = new File("target/aura-sandbox/ai-console-screenshots");
        testDir.mkdirs();
        final File mockPng = new File(testDir, "test-valid.png");
        Files.writeString(mockPng.toPath(), "fake-png-binary-content", StandardCharsets.UTF_8);

        try
        {
            mockMvc.perform(get("/api/console/screenshot")
                    .param("file", "test-valid.png"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG));
        }
        finally
        {
            if (mockPng.exists())
            {
                mockPng.delete();
            }
        }
    }

    @Test
    public void testEventBroadcast() throws Exception
    {
        mockMvc.perform(post("/api/console/internal/broadcast")
                .param("event", "status_update")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phase\":\"executing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    public void testSseEventStreaming() throws Exception
    {
        final MvcResult mvcResult = mockMvc.perform(get("/api/console/events"))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(mvcResult.getResponse().getContentType().startsWith("text/event-stream"));
    }

    @Test
    public void testInteractiveConsoleHtml() throws Exception
    {
        mockMvc.perform(get("/interactive_console.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

        mockMvc.perform(get("/interactive_console"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    public void testInteractiveConsoleCss() throws Exception
    {
        mockMvc.perform(get("/interactive_console.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/css")));
    }

    @Test
    public void testInteractiveConsoleJs() throws Exception
    {
        mockMvc.perform(get("/interactive_console.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/javascript")));
    }

    @Test
    public void testRunExampleJson() throws Exception
    {
        mockMvc.perform(get("/run_example.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
