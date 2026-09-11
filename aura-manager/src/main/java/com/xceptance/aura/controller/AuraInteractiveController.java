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
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine.ActionResult;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.neodymium.aura.AuraQueueService;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
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
 * REST controller handling interactive CDP console operations, Server-Sent Events,
 * action submissions, and screenshot retrieval by delegating to {@link AuraInteractiveService}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@RestController
public class AuraInteractiveController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraInteractiveController.class);

    private final AuraInteractiveService interactiveService;
    private final AuraQueueService queueService;

    public AuraInteractiveController(
        final AuraInteractiveService interactiveService,
        final AuraQueueService queueService)
    {
        this.interactiveService = interactiveService;
        this.queueService = queueService;
    }

    @GetMapping("/api/interactive/active")
    public ResponseEntity<Map<String, Object>> getActiveEngines()
    {
        final InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        return ResponseEntity.ok(Map.of("activeCount", engine != null ? 1 : 0));
    }

    @PostMapping("/api/interactive/stop")
    public ResponseEntity<Map<String, Object>> stopEngine(@RequestParam(value = "sessionId", required = false) final String sessionId)
    {
        final InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        if (engine != null)
        {
            engine.abort();
        }
        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
    }

    @PostMapping("/api/console/internal/pushState")
    public ResponseEntity<Map<String, Object>> handlePushState(@RequestBody final String body)
    {
        final String status = interactiveService.pushState(body, queueService.isManuallyStopped());
        return ResponseEntity.ok(Map.of("status", status));
    }

    @GetMapping("/api/console/internal/waitForAction")
    public ResponseEntity<String> handleWaitForAction(@RequestParam(value = "pauseId", required = false) final String pauseId)
    {
        try
        {
            final String responseJson = interactiveService.waitForAction(pauseId, queueService.isManuallyStopped());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(responseJson);
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"error\":\"Interrupted waiting for user action\"}");
        }
    }

    @PostMapping("/api/console/internal/broadcast")
    public ResponseEntity<Map<String, Object>> handleBroadcast(
        @RequestParam(value = "event", required = false) final String eventParam,
        @RequestBody(required = false) final String body)
    {
        final InteractiveConsoleEngine engine = interactiveService.getOrCreateConsoleEngine();
        String event = eventParam;
        String payload = body != null ? body : "{}";
        try
        {
            if (body != null && body.trim().startsWith("{"))
            {
                final JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (event == null && json.has("event") && !json.get("event").isJsonNull())
                {
                    event = json.get("event").getAsString();
                }
                if (json.has("payload") && !json.get("payload").isJsonNull())
                {
                    payload = json.get("payload").getAsString();
                }
            }
        }
        catch (final Exception e)
        {
            LOGGER.warn("[Aura Server] Failed to parse broadcast payload: {}", e.getMessage());
        }
        if (event == null)
        {
            event = "state";
        }
        engine.broadcastSseEvent(event, payload);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping(value = "/api/console/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> handleConsoleEvents()
    {
        final InteractiveConsoleEngine engine = interactiveService.getOrCreateConsoleEngine();

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

        return ResponseEntity.ok()
                .headers(headers)
                .body(responseBody);
    }

    @PostMapping("/api/console/action")
    public ResponseEntity<String> handleAction(@RequestBody final String body)
    {
        final ActionResult result = interactiveService.submitAction(body);
        return ResponseEntity.status(result.statusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(result.responseBody());
    }

    @GetMapping("/api/console/screenshot")
    public ResponseEntity<Resource> handleScreenshot(
        @RequestParam(value = "file", required = false) final String fileName,
        @RequestParam(value = "runId", required = false) final String runId)
    {
        final Optional<File> targetFile = interactiveService.getScreenshotFile(fileName, runId);
        if (targetFile.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        try
        {
            final byte[] data = Files.readAllBytes(targetFile.get().toPath());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(new ByteArrayResource(data));
        }
        catch (final IOException e)
        {
            LOGGER.error("Failed to read screenshot file {}: {}", targetFile.get().getPath(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/api/disconnect")
    public ResponseEntity<Map<String, Object>> handleDisconnect()
    {
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping(
    {
      "/interactive_console.html", "/interactive_console"
    })
    public ResponseEntity<Resource> getInteractiveConsoleHtml()
    {
        final Resource resource = new ClassPathResource("com/xceptance/neodymium/ai/console/interactive_console.html");
        if (!resource.exists())
        {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource);
    }

    @GetMapping(value = "/interactive_console.css", produces = "text/css")
    public ResponseEntity<Resource> getInteractiveConsoleCss()
    {
        final Resource resource = new ClassPathResource("com/xceptance/neodymium/ai/console/interactive_console.css");
        if (!resource.exists())
        {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/css; charset=UTF-8"))
                .body(resource);
    }

    @GetMapping(value = "/interactive_console.js", produces = "application/javascript")
    public ResponseEntity<Resource> getInteractiveConsoleJs()
    {
        final Resource resource = new ClassPathResource("com/xceptance/neodymium/ai/console/interactive_console.js");
        if (!resource.exists())
        {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/javascript; charset=UTF-8"))
                .body(resource);
    }

    @GetMapping(value = "/run_example.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource> getRunExampleJson()
    {
        final Resource resource = new ClassPathResource("com/xceptance/neodymium/ai/console/run_example.json");
        if (!resource.exists())
        {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resource);
    }
}
