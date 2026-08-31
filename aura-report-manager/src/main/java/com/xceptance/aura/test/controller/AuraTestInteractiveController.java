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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.neodymium.ai.config.AiConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Controller handling interactive console engine management, internal state synchronization,
 * HUD Server-Sent Events, action submissions, and screenshot retrieval.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@RestController
public class AuraTestInteractiveController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraTestInteractiveController.class);

    private final AuraInteractiveService interactiveService;
    private final AuraReportingService reportingService;
    private final AuraQueueService queueService;

    public AuraTestInteractiveController(final AuraInteractiveService interactiveService,
            final AuraReportingService reportingService,
            final AuraQueueService queueService)
    {
        this.interactiveService = interactiveService;
        this.reportingService = reportingService;
        this.queueService = queueService;
    }

    @GetMapping("/api/interactive/active")
    public ResponseEntity<Map<String, Object>> getActiveEngines()
    {
        final Map<String, Object> resp = new HashMap<>();
        final InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        resp.put("activeCount", engine != null ? 1 : 0);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/api/interactive/stop")
    public ResponseEntity<Map<String, Object>> stopEngine(@RequestParam(value = "sessionId", required = false) final String sessionId)
    {
        final InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        if (engine != null)
        {
            engine.abort();
        }
        final Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/api/console/internal/pushState")
    public ResponseEntity<Map<String, Object>> handlePushState(@RequestBody final String body)
    {
        InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        if (engine == null)
        {
            engine = new InteractiveConsoleEngine("initializing");
            interactiveService.setCurrentConsoleEngine(engine);
        }

        JsonObject json = null;
        try
        {
            json = JsonParser.parseString(body).getAsJsonObject();
            if (json != null && json.has("runId") && !json.get("runId").isJsonNull())
            {
                final String incomingRunId = json.get("runId").getAsString();
                final String lastId = interactiveService.getLastProcessedRunIdReference().getAndSet(incomingRunId);
                if (incomingRunId != null && !incomingRunId.equals(lastId))
                {
                    LOGGER.info("[Aura Server] New runId detected: {}. Resetting manuallyStopped flag and execution indexes.", incomingRunId);
                    queueService.setManuallyStopped(false);
                    interactiveService.resetExecutionIndexes();
                }
                engine.setRunId(incomingRunId);
            }
        }
        catch (final Exception e)
        {
            LOGGER.warn("[Aura Server] Failed to parse state push payload: {}", e.getMessage());
        }

        engine.pushState(body);

        try
        {
            final String executionKey = extractExecutionKey(json);
            final int index = interactiveService.getExecutionIndex(executionKey);

            final String runFolder = (engine.getRunId() != null && !engine.getRunId().isBlank())
                ? (engine.getRunId().startsWith("run_") || engine.getRunId().startsWith("run-") ? engine.getRunId() : "run_" + engine.getRunId())
                : InteractiveConsoleEngine.getRunFolder();
            final String testClassFolder = InteractiveConsoleEngine.extractTestClassFolder(json);

            final List<File> baseDirs = List.of(
                new File("storage/runs"),
                new File(AiConfiguration.getInstance().getConsoleExecutionLogsDirectory())
            );

            for (final File baseDir : baseDirs)
            {
                try
                {
                    final File structuredDir = new File(baseDir, runFolder + "/" + testClassFolder);
                    if (!structuredDir.exists())
                    {
                        structuredDir.mkdirs();
                    }
                    final File structuredJson = new File(structuredDir, "console-execution-" + index + ".json");
                    Files.writeString(structuredJson.toPath(), body, StandardCharsets.UTF_8);

                    final File runLevelDir = new File(baseDir, runFolder);
                    if (!structuredDir.getCanonicalPath().equals(runLevelDir.getCanonicalPath()))
                    {
                        final File runLevelJson = new File(runLevelDir, "console-execution-" + index + ".json");
                        if (runLevelJson.exists())
                        {
                            runLevelJson.delete();
                        }
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.warn("[Aura Server] Could not save execution snapshot in {}: {}", baseDir.getPath(), e.getMessage());
                }
            }
        }
        catch (final Exception e)
        {
            LOGGER.warn("[Aura Server] Failed to save console execution snapshot: {}", e.getMessage());
        }

        final Map<String, Object> resp = new HashMap<>();
        resp.put("status", queueService.isManuallyStopped() ? "stopped" : "ok");
        return ResponseEntity.ok(resp);
    }

    private String extractExecutionKey(final JsonObject json)
    {
        if (json == null)
        {
            return "default";
        }
        String key = "";
        if (json.has("testName") && !json.get("testName").isJsonNull())
        {
            final String testName = json.get("testName").getAsString();
            if (testName != null && !testName.isEmpty() && !"Live Test Run".equals(testName))
            {
                key = testName;
            }
        }
        if (key.isEmpty())
        {
            if (json.has("playbookFile") && !json.get("playbookFile").isJsonNull())
            {
                key += json.get("playbookFile").getAsString();
            }
            else if (json.has("testFile") && !json.get("testFile").isJsonNull())
            {
                key += json.get("testFile").getAsString();
            }
            if (json.has("datasetId") && !json.get("datasetId").isJsonNull())
            {
                key += "::" + json.get("datasetId").getAsString();
            }
        }
        if (json.has("browser") && !json.get("browser").isJsonNull())
        {
            final String browser = json.get("browser").getAsString();
            if (browser != null && !browser.isEmpty())
            {
                if (!key.isEmpty())
                {
                    key += "::";
                }
                key += browser;
            }
        }
        return key.isEmpty() ? "default" : key;
    }

    @GetMapping("/api/console/internal/waitForAction")
    public ResponseEntity<String> handleWaitForAction(
            @RequestParam(value = "pauseId", required = false) final String pauseId)
    {
        if (queueService.isManuallyStopped())
        {
            final JsonObject abortAction = new JsonObject();
            abortAction.addProperty("action", "ABORT");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(abortAction.toString());
        }

        InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        if (engine == null)
        {
            engine = new InteractiveConsoleEngine("initializing");
            interactiveService.setCurrentConsoleEngine(engine);
        }

        try
        {
            final JsonObject action = engine.waitForAction(pauseId);
            final String responseJson = action != null ? action.toString() : "{}";
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseJson);
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Interrupted while waiting for action\"}");
        }
    }

    @PostMapping("/api/console/internal/broadcast")
    public ResponseEntity<Map<String, Object>> handleBroadcast(@RequestBody final String body)
    {
        InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        if (engine == null)
        {
            engine = new InteractiveConsoleEngine("initializing");
            interactiveService.setCurrentConsoleEngine(engine);
        }

        try
        {
            final JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            final String event = json.has("event") && !json.get("event").isJsonNull()
                    ? json.get("event").getAsString()
                    : "state";
            final String payload = json.has("payload") && !json.get("payload").isJsonNull()
                    ? json.get("payload").getAsString()
                    : "{}";
            engine.broadcastSseEvent(event, payload);
        }
        catch (final Exception e)
        {
            LOGGER.warn("[Aura Server] Failed to parse broadcast payload: {}", e.getMessage());
        }

        final Map<String, Object> resp = new HashMap<>();
        resp.put("status", "ok");
        return ResponseEntity.ok(resp);
    }

    @GetMapping(value = "/api/console/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> handleConsoleEvents()
    {
        final InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        if (engine == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final HttpHeaders headers = new HttpHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Cache-Control", "no-cache");
        headers.set("Connection", "keep-alive");
        headers.set("Content-Type", "text/event-stream; charset=UTF-8");

        final StreamingResponseBody responseBody = outputStream -> {
            engine.addSseClient(outputStream);
            try
            {
                synchronized (outputStream)
                {
                    final String initial = "event: state\ndata: " + engine.getCurrentStateJson() + "\n\n";
                    outputStream.write(initial.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();

                    final String activePauseId = engine.getCurrentPauseId();
                    if (activePauseId != null)
                    {
                        final String pause = "event: pause\ndata: " + engine.buildPausePayload(activePauseId) + "\n\n";
                        outputStream.write(pause.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    }
                }

                while (true)
                {
                    Thread.sleep(15_000);
                    synchronized (outputStream)
                    {
                        outputStream.write(": keep-alive\n\n".getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    }
                }
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            catch (final IOException ignored)
            {
                // Client disconnected
            }
            finally
            {
                engine.removeSseClient(outputStream);
                try
                {
                    outputStream.close();
                }
                catch (final IOException ignored)
                {
                }
            }
        };

        return ResponseEntity.ok().headers(headers).body(responseBody);
    }

    @PostMapping("/api/console/action")
    public ResponseEntity<String> handleConsoleAction(@RequestBody final String body)
    {
        final InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        if (engine == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"No active interactive engine\"}");
        }

        final JsonObject req;
        try
        {
            req = JsonParser.parseString(body).getAsJsonObject();
        }
        catch (final Exception e)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Invalid JSON body\"}");
        }

        final InteractiveConsoleEngine.ActionResult result = engine.submitAction(req);
        return ResponseEntity.status(result.statusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(result.responseBody());
    }

    @GetMapping("/api/console/screenshot")
    public ResponseEntity<Resource> handleConsoleScreenshot(
            @RequestParam(value = "file", required = false) final String file,
            @RequestParam(value = "runId", required = false) final String runId)
    {
        if (file == null || file.contains("/") || file.contains("\\") || file.contains(".."))
        {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        File targetFile = null;
        if (runId != null && !runId.isEmpty())
        {
            try
            {
                final File historyDir = new File(reportingService.getReportHistoryDir(), runId).getCanonicalFile();
                targetFile = new File(historyDir, "screenshots/" + file).getCanonicalFile();
                if (!targetFile.getPath().startsWith(historyDir.getPath()))
                {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
            catch (final Exception ignore)
            {
                // ignore
            }
        }

        if (targetFile == null || !targetFile.exists())
        {
            final String screenshotsDirPath = AiConfiguration.getInstance().getProperty(
                    "neodymium.ai.console.screenshotsDir", "target/aura-sandbox/ai-console-screenshots");
            try
            {
                final File activeDir = new File(screenshotsDirPath).getCanonicalFile();
                targetFile = new File(activeDir, file).getCanonicalFile();
                if (!targetFile.getPath().startsWith(activeDir.getPath()))
                {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
            catch (final Exception ignore)
            {
                // ignore
            }
        }

        if (targetFile != null && targetFile.exists() && targetFile.isFile())
        {
            try
            {
                final byte[] bytes = Files.readAllBytes(targetFile.toPath());
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(new ByteArrayResource(bytes));
            }
            catch (final IOException e)
            {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PostMapping("/api/disconnect")
    public ResponseEntity<Map<String, Object>> handleDisconnect(
            @RequestParam(value = "clientId", required = false) final String clientId)
    {
        LOGGER.info("[Aura Server] Disconnect request received for clientId: {}", clientId);
        final Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        return ResponseEntity.ok(resp);
    }
}
