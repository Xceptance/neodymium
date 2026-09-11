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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine.ActionResult;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.util.Neodymium;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service managing live interactive CDP HUD sessions, theme preferences, execution tracking,
 * action synchronization, and screenshot file resolution.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class AuraInteractiveService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraInteractiveService.class);

    private final AtomicReference<InteractiveConsoleEngine> currentConsoleEngine = new AtomicReference<>(null);
    private final AtomicReference<String> lastProcessedRunId = new AtomicReference<>(null);
    private final AtomicReference<String> activeTheme = new AtomicReference<>("system");
    private final Map<String, Integer> executionIndexMap = new ConcurrentHashMap<>();
    private final AtomicInteger executionIndexCounter = new AtomicInteger(0);

    public AuraInteractiveService()
    {
    }

    public AtomicReference<InteractiveConsoleEngine> getCurrentConsoleEngineReference()
    {
        return currentConsoleEngine;
    }

    public InteractiveConsoleEngine getCurrentConsoleEngine()
    {
        return currentConsoleEngine.get();
    }

    public void setCurrentConsoleEngine(final InteractiveConsoleEngine engine)
    {
        currentConsoleEngine.set(engine);
    }

    public InteractiveConsoleEngine getOrCreateConsoleEngine()
    {
        InteractiveConsoleEngine engine = currentConsoleEngine.get();
        if (engine == null)
        {
            engine = new InteractiveConsoleEngine("initializing");
            currentConsoleEngine.set(engine);
        }
        return engine;
    }

    public AtomicReference<String> getLastProcessedRunIdReference()
    {
        return lastProcessedRunId;
    }

    public String getLastProcessedRunId()
    {
        return lastProcessedRunId.get();
    }

    public void setLastProcessedRunId(final String runId)
    {
        lastProcessedRunId.set(runId);
    }

    public String getActiveTheme()
    {
        return activeTheme.get();
    }

    public void setActiveTheme(final String theme)
    {
        activeTheme.set(theme);
    }

    public int getExecutionIndex(final String executionKey)
    {
        if (executionKey == null || executionKey.isEmpty())
        {
            return 1;
        }
        return executionIndexMap.computeIfAbsent(executionKey, k -> executionIndexCounter.incrementAndGet());
    }

    public void resetExecutionIndexes()
    {
        executionIndexMap.clear();
        executionIndexCounter.set(0);
    }

    /**
     * Ingests a new HUD execution snapshot, manages runId state transitions, persists disk snapshots,
     * and forwards the state to the active console engine.
     *
     * @param body JSON representation of the console state
     * @param isStopped true if the run was manually stopped
     * @return execution status: "stopped" if manually stopped, otherwise "ok"
     */
    public String pushState(final String body, final boolean isStopped)
    {
        final InteractiveConsoleEngine engine = getOrCreateConsoleEngine();
        JsonObject json = null;
        try
        {
            json = JsonParser.parseString(body).getAsJsonObject();
            if (json != null && json.has("runId") && !json.get("runId").isJsonNull())
            {
                final String incomingRunId = json.get("runId").getAsString();
                final String lastId = lastProcessedRunId.getAndSet(incomingRunId);
                if (incomingRunId != null && !incomingRunId.equals(lastId))
                {
                    LOGGER.info("[InteractiveService] New runId detected: {}. Resetting execution indexes.", incomingRunId);
                    resetExecutionIndexes();
                }
                engine.setRunId(incomingRunId);
            }
        }
        catch (final Exception e)
        {
            LOGGER.warn("[InteractiveService] Failed to parse state push payload: {}", e.getMessage());
        }

        engine.pushState(body);

        try
        {
            final String executionKey = extractExecutionKey(json);
            final int index = getExecutionIndex(executionKey);

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
                    LOGGER.warn("[InteractiveService] Could not save execution snapshot in {}: {}", baseDir.getPath(), e.getMessage());
                }
            }
        }
        catch (final Exception e)
        {
            LOGGER.warn("[InteractiveService] Failed to save console execution snapshot: {}", e.getMessage());
        }

        return isStopped ? "stopped" : "ok";
    }

    /**
     * Waits for an interactive user action token, aborting immediately if a manual stop was requested.
     *
     * @param pauseId the pause identifier
     * @param isStopped true if execution was cancelled
     * @return JSON response payload containing the selected user action
     * @throws InterruptedException if thread waiting is interrupted
     */
    public String waitForAction(final String pauseId, final boolean isStopped) throws InterruptedException
    {
        if (isStopped)
        {
            final JsonObject abortAction = new JsonObject();
            abortAction.addProperty("action", "ABORT");
            return abortAction.toString();
        }

        final InteractiveConsoleEngine engine = getOrCreateConsoleEngine();
        final JsonObject action = engine.waitForAction(pauseId);
        return action != null ? action.toString() : "{}";
    }

    /**
     * Submits an action JSON payload to the underlying console engine.
     *
     * @param jsonBody action request JSON
     * @return action result with status code and response body
     */
    public ActionResult submitAction(final String jsonBody)
    {
        final InteractiveConsoleEngine engine = getCurrentConsoleEngine();
        if (engine == null)
        {
            return new ActionResult(404, "{\"error\":\"No active interactive engine\"}");
        }

        final JsonObject req;
        try
        {
            req = JsonParser.parseString(jsonBody).getAsJsonObject();
        }
        catch (final Exception e)
        {
            return new ActionResult(400, "{\"error\":\"Invalid JSON body\"}");
        }

        return engine.submitAction(req);
    }

    /**
     * Resolves and verifies a screenshot file by file name and optional runId, protecting against
     * path traversal attempts.
     *
     * @param fileName file name of the screenshot
     * @param runId optional run ID for historical reports
     * @return Optional containing the validated File if it exists, or empty Optional
     */
    public Optional<File> getScreenshotFile(final String fileName, final String runId)
    {
        if (fileName == null || fileName.contains("/") || fileName.contains("\\") || fileName.contains(".."))
        {
            return Optional.empty();
        }

        if (runId != null && !runId.isEmpty())
        {
            try
            {
                final String reportHistoryPath = Neodymium.configuration().reportHistoryDir();
                final File reportHistoryDir = resolveCanonicalDirectory(reportHistoryPath);
                final File historyDir = new File(reportHistoryDir, runId).getCanonicalFile();
                final File targetFile = new File(historyDir, "screenshots/" + fileName).getCanonicalFile();
                if (targetFile.getPath().startsWith(historyDir.getPath()) && targetFile.exists() && targetFile.isFile())
                {
                    return Optional.of(targetFile);
                }
            }
            catch (final Exception ignore)
            {
            }
        }

        try
        {
            final String screenshotsDirPath = AiConfiguration.getInstance().getProperty(
                    "neodymium.ai.console.screenshotsDir", "target/aura-sandbox/ai-console-screenshots");
            final File activeDir = resolveCanonicalDirectory(screenshotsDirPath);
            final File targetFile = new File(activeDir, fileName).getCanonicalFile();
            if (targetFile.getPath().startsWith(activeDir.getPath()) && targetFile.exists() && targetFile.isFile())
            {
                return Optional.of(targetFile);
            }
        }
        catch (final Exception ignore)
        {
        }

        return Optional.empty();
    }

    /**
     * Helper to broadcast Server-Sent Events to all connected clients.
     *
     * @param event event name
     * @param payload event payload
     */
    public void broadcast(final String event, final String payload)
    {
        final InteractiveConsoleEngine engine = getCurrentConsoleEngine();
        if (engine != null)
        {
            engine.broadcastSseEvent(event, payload);
        }
    }

    private static File resolveCanonicalDirectory(final String rawPath) throws IOException
    {
        final File file = new File(rawPath);
        if (file.isAbsolute())
        {
            return file.getCanonicalFile();
        }
        return new File(System.getProperty("user.dir"), rawPath).getCanonicalFile();
    }

    private static String extractExecutionKey(final JsonObject json)
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
}
