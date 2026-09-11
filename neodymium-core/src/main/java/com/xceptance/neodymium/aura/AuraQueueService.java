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

import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.xceptance.neodymium.aura.dto.BrowserProfileDto;
import com.xceptance.neodymium.aura.dto.DatasetSelection;
import com.xceptance.neodymium.aura.dto.RunRequest;
import org.neodymium.common.browser.configuration.BrowserConfiguration;
import org.neodymium.common.browser.configuration.MultibrowserConfiguration;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neodymium.ai.config.AiConfiguration;

/**
 * Service managing the test execution queue, starting test processes, monitoring output,
 * parsing test status statistics, and stopping execution.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraQueueService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraQueueService.class);
    private static final Pattern STATS_PATTERN = Pattern
            .compile("Tests run:\\s*(\\d+),\\s*Failures:\\s*(\\d+),\\s*Errors:\\s*(\\d+)(?:,\\s*Skipped:\\s*(\\d+))?");

    private final List<String> currentRunLogs = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> currentRunEvents = Collections.synchronizedList(new ArrayList<>());
    private final List<String> completedFiles = Collections.synchronizedList(new ArrayList<>());

    private final AtomicReference<Process> activeProcess = new AtomicReference<>(null);
    private final AtomicInteger globalTestsRun = new AtomicInteger(0);
    private final AtomicInteger globalPassed = new AtomicInteger(0);
    private final AtomicInteger globalFailed = new AtomicInteger(0);
    private final AtomicInteger globalSkipped = new AtomicInteger(0);
    private final AtomicBoolean runningQueue = new AtomicBoolean(false);
    private final AtomicBoolean manuallyStopped = new AtomicBoolean(false);
    private final AtomicReference<String> activeFile = new AtomicReference<>("");
    private final AtomicReference<String> currentRunId = new AtomicReference<>("");
    private volatile Consumer<String> onRunCompletedListener;

    /** Wall-clock start time in ms of the current (or most recent) queue run. */
    private final AtomicLong runStartTimeMs = new AtomicLong(0);

    /** The RunRequest that initiated the most recent queue execution, used for archiving metadata. */
    private final AtomicReference<RunRequest> lastRunRequest = new AtomicReference<>(null);

    private final Set<File> createdTempFiles = Collections.synchronizedSet(new HashSet<>());

    private final AuraInteractiveService interactiveService;

    public AuraQueueService(final AuraInteractiveService interactiveService)
    {
        this.interactiveService = interactiveService;
    }

    public static void deleteDirRecursively(final File file)
    {
        if (file == null || !file.exists())
        {
            return;
        }
        if (file.isDirectory())
        {
            final File[] children = file.listFiles();
            if (children != null)
            {
                for (final File child : children)
                {
                    deleteDirRecursively(child);
                }
            }
        }
        file.delete();
    }

    public String getCurrentRunId()
    {
        return currentRunId.get();
    }

    public void setOnRunCompletedListener(final Consumer<String> onRunCompletedListener)
    {
        this.onRunCompletedListener = onRunCompletedListener;
    }

    public List<String> getCurrentRunLogs()
    {
        return currentRunLogs;
    }

    public List<Map<String, Object>> getCurrentRunEvents()
    {
        return currentRunEvents;
    }

    public List<String> getCompletedFiles()
    {
        return completedFiles;
    }

    public AtomicReference<Process> getActiveProcessReference()
    {
        return activeProcess;
    }

    public Process getActiveProcess()
    {
        return activeProcess.get();
    }

    public int getGlobalTestsRun()
    {
        return globalTestsRun.get();
    }

    public int getGlobalPassed()
    {
        return globalPassed.get();
    }

    public int getGlobalFailed()
    {
        return globalFailed.get();
    }

    public int getGlobalSkipped()
    {
        return globalSkipped.get();
    }

    public boolean isRunningQueue()
    {
        return runningQueue.get();
    }

    public boolean isManuallyStopped()
    {
        return manuallyStopped.get();
    }

    public void setManuallyStopped(final boolean stopped)
    {
        manuallyStopped.set(stopped);
    }

    public String getActiveFile()
    {
        return activeFile.get();
    }

    public long getRunStartTimeMs()
    {
        return runStartTimeMs.get();
    }

    public RunRequest getLastRunRequest()
    {
        return lastRunRequest.get();
    }

    public void stopProcess()
    {
        LOGGER.info("[Aura Server] Setting manuallyStopped=true.");
        manuallyStopped.set(true);
        final InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        if (engine != null)
        {
            engine.abort();
        }
        final Process p = activeProcess.getAndSet(null);
        if (p != null && p.isAlive())
        {
            p.destroy();
            try
            {
                if (!p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS))
                {
                    LOGGER.warn("[Aura Server] Subprocess did not stop on destroy, forcing termination...");
                    p.destroyForcibly();
                }
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            LOGGER.info("[Aura Server] Subprocess terminated.");
            broadcastLog("[INFO] Active Maven process terminated by user.");
        }
        else
        {
            LOGGER.info("[Aura Server] No active subprocess found to stop.");
        }
        synchronized (createdTempFiles)
        {
            for (final File f : createdTempFiles)
            {
                if (f != null && f.exists())
                {
                    f.delete();
                    LOGGER.info("[Aura Server] Deleted temporary test runner on stopProcess: {}", f.getAbsolutePath());
                }
            }
            createdTempFiles.clear();
        }
    }

    public void broadcastLog(final String line)
    {
        final String cleanLine = NeodymiumAuraManager.stripAnsi(line);
        currentRunLogs.add(cleanLine);
    }

    private void broadcastInteractiveConsoleReady(final String url)
    {
        final Map<String, Object> event = new HashMap<>();
        event.put("type", "interactiveConsoleReady");
        event.put("url", url);
        currentRunEvents.add(event);
    }

    public void executeQueue(final RunRequest req, final int serverPort)
    {
        if (runningQueue.getAndSet(true))
        {
            LOGGER.warn("[Aura Server] Cannot execute run queue: execution already in progress!");
            return;
        }

        final Thread thread = new Thread(() -> {
            final File tempRunnerDir = new File("src/test/java/com/xceptance/neodymium/aura/sandbox").getAbsoluteFile();

            try
            {
                final File allureResultsDir = new File("target/aura-sandbox/allure-results");
                if (allureResultsDir.exists())
                {
                    deleteDirRecursively(allureResultsDir);
                }
                final File allureReportSiteDir = new File("target/aura-sandbox/site/allure-maven-plugin");
                if (allureReportSiteDir.exists())
                {
                    deleteDirRecursively(allureReportSiteDir);
                }
                final File defaultSiteDir = new File("target/site/allure-maven-plugin");
                if (defaultSiteDir.exists())
                {
                    deleteDirRecursively(defaultSiteDir);
                }

                final List<DatasetSelection> preservedDatasets = new ArrayList<>();
                if (req.datasets != null)
                {
                    for (final DatasetSelection selection : req.datasets)
                    {
                        if (selection != null)
                        {
                            final DatasetSelection ds = new DatasetSelection();
                            ds.file = selection.file;
                            ds.id = selection.id;
                            ds.browserProfiles = (selection.browserProfiles != null && !selection.browserProfiles.isEmpty())
                                    ? new ArrayList<>(selection.browserProfiles)
                                    : null;
                            preservedDatasets.add(ds);
                        }
                    }
                }
                req.datasets = preservedDatasets;

                final List<ExecutionBatch> batches = new ArrayList<>();
                final Set<String> uniqueFiles = new LinkedHashSet<>();
                if (req.datasets != null)
                {
                    for (final DatasetSelection selection : req.datasets)
                    {
                        if (selection.file == null)
                        {
                            continue;
                        }
                        uniqueFiles.add(selection.file);
                        final List<String> profiles = getEffectiveBrowserProfiles(selection, req.globalBrowserProfiles);

                        ExecutionBatch match = null;
                        for (final ExecutionBatch b : batches)
                        {
                            if (selection.file.equals(b.file) && profiles.equals(b.targetProfiles))
                            {
                                match = b;
                                break;
                            }
                        }
                        if (match == null)
                        {
                            match = new ExecutionBatch(selection.file, profiles);
                            batches.add(match);
                        }
                        if (selection.id != null)
                        {
                            match.datasetIds.add(selection.id);
                        }
                    }
                }

                LOGGER.info("[Aura Server] Starting execution of {} dataset(s) across {} batch(es) in queue.",
                        req.datasets.size(), batches.size());
                globalTestsRun.set(0);
                globalPassed.set(0);
                globalFailed.set(0);
                globalSkipped.set(0);
                manuallyStopped.set(false);
                currentRunLogs.clear();
                currentRunEvents.clear();
                completedFiles.clear();
                interactiveService.resetExecutionIndexes();

                runStartTimeMs.set(System.currentTimeMillis());
                final String generatedRunId = "run_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date(runStartTimeMs.get()));
                currentRunId.set(generatedRunId);
                lastRunRequest.set(req);

                for (int i = 0; i < batches.size(); i++)
                {
                    final ExecutionBatch batch = batches.get(i);
                    final String file = batch.file;
                    final List<String> ids = batch.datasetIds;
                    final List<String> targetProfiles = batch.targetProfiles;

                    activeFile.set(file);

                    if ("true".equalsIgnoreCase(System.getProperty("neodymium.aura.test", "false")))
                    {
                        LOGGER.info("[Aura Server] Test mode active (neodymium.aura.test=true). Simulating queue execution for {} with profiles {}", file, targetProfiles);
                        broadcastLog("[INFO] Test mode active: simulating Maven subprocess for " + file + " [Profiles: " + targetProfiles + "]");
                        final int simRuns = Math.max(1, ids.size()) * Math.max(1, targetProfiles.size());
                        broadcastLog("[INFO] Tests run: " + simRuns + ", Failures: 0, Errors: 0, Skipped: 0");
                        globalTestsRun.addAndGet(simRuns);
                        globalPassed.addAndGet(simRuns);
                        completedFiles.add(file);
                        continue;
                    }

                    // Generate temporary runner
                    final String safeName = file.replaceAll("[^a-zA-Z0-9]", "_");
                    final String className = "Aura_" + safeName + "_Test";
                    final File tempRunnerFile = new File(tempRunnerDir, className + ".java");

                    final StringBuilder browserAnnotations = new StringBuilder();
                    for (final String profile : targetProfiles)
                    {
                        browserAnnotations.append("@Browser(\"").append(profile.replace("\"", "\\\"")).append("\")\n");
                    }

                    tempRunnerDir.mkdirs();
                    final String runnerSource = "package com.xceptance.neodymium.aura.sandbox;\n\n" +
                            "import org.neodymium.common.browser.Browser;\n" +
                            "import org.neodymium.common.testdata.DataFolder;\n" +
                            "import org.neodymium.junit5.NeodymiumTest;\n" +
                            "import org.neodymium.ai.junit.NeodymiumAiTest;\n" +
                            "import org.junit.jupiter.api.DisplayName;\n\n" +
                            browserAnnotations.toString() +
                            "@DataFolder(\".\")\n" +
                            "@NeodymiumAiTest\n" +
                            "@DisplayName(\"YAML Test: " + file.replace("\"", "\\\"") + "\")\n" +
                            "public final class " + className + "\n" +
                            "{\n" +
                            "    @NeodymiumTest\n" +
                            "    public final void executeYamlTest() throws Throwable\n" +
                            "    {\n" +
                            "    }\n" +
                            "}\n";
                    Files.writeString(tempRunnerFile.toPath(), runnerSource, StandardCharsets.UTF_8);
                    LOGGER.info("[Aura Server] Created temporary test runner with profiles {}: {}",
                            targetProfiles, tempRunnerFile.getAbsolutePath());
                    createdTempFiles.add(tempRunnerFile);
                    tempRunnerFile.deleteOnExit();

                    final StringBuilder idFilterBuilder = new StringBuilder();
                    boolean hasIds = false;
                    for (int j = 0; j < ids.size(); j++)
                    {
                        if (ids.get(j) != null)
                        {
                            if (hasIds)
                            {
                                idFilterBuilder.append("|");
                            }
                            else
                            {
                                idFilterBuilder.append("^(");
                            }
                            idFilterBuilder.append(ids.get(j).replaceAll("([\\\\.*+?^${}()|\\[\\]])", "\\\\$1"));
                            hasIds = true;
                        }
                    }
                    if (hasIds)
                    {
                        idFilterBuilder.append(")$");
                    }

                    broadcastLog("\n[INFO] Spawning Maven Subprocess for YAML test: " + file + " [Datasets: " + ids
                            + "]...");

                    final List<String> command = new ArrayList<>();
                    final String runId = currentRunId.get();
                    final InteractiveConsoleEngine engine = new InteractiveConsoleEngine(runId);
                    interactiveService.setCurrentConsoleEngine(engine);

                    final List<File> runStorageDirs = List.of(
                        new File("storage/runs", runId),
                        new File(AiConfiguration.getInstance().getConsoleExecutionLogsDirectory(), runId)
                    );
                    for (final File runDir : runStorageDirs)
                    {
                        if (!runDir.exists())
                        {
                            runDir.mkdirs();
                        }
                    }

                    if (req.interactive)
                    {
                        broadcastInteractiveConsoleReady("/interactive_console.html");
                    }

                    final String os = System.getProperty("os.name").toLowerCase();
                    if (os.contains("win"))
                    {
                        command.add("cmd.exe");
                        command.add("/c");
                        command.add("mvn");
                    }
                    else
                    {
                        if (new File("/usr/bin/stdbuf").exists() || new File("/bin/stdbuf").exists())
                        {
                            command.add("stdbuf");
                            command.add("-oL");
                            command.add("-eL");
                        }
                        command.add("mvn");
                    }
                    command.add("test");
                    command.add("-Dtest=com.xceptance.neodymium.aura.sandbox." + className);
                    command.add("-Dneodymium.testFileFilter=" + file.replace(".", "\\."));
                    command.add("-Dneodymium.ai.console.screenshotsDir=" + new File("target/aura-sandbox/ai-console-screenshots").getAbsolutePath());
                    if (hasIds)
                    {
                        command.add("-Dneodymium.testIdFilter=" + idFilterBuilder.toString());
                    }
                    for (final String profile : targetProfiles)
                    {
                        command.add("-Dbrowserprofile." + profile + ".headless=" + req.headless);
                    }
                    command.add("-Dbrowserprofile.global.headless=" + req.headless);
                    command.add("-Dbrowserprofile.Default.headless=" + req.headless);
                    command.add("-Dselenide.headless=" + req.headless);
                    command.add("-Dneodymium.ai.interactive=" + req.interactive);
                    command.add("-Dvideo.enableFilming=" + req.video);
                    command.add("-Dneodymium.ai.executionMode=" + (req.executionMode != null ? req.executionMode : "LLM_RECORDING"));
                    command.add("-Dneodymium.managerActive=true");
                    command.add("-Dneodymium.aura.test=" + System.getProperty("neodymium.aura.test", "false"));
                    command.add("-Dneodymium.managerRunId=" + runId);
                    command.add("-Dneodymium.runId=" + runId);
                    command.add("-Dneodymium.managerUrl=http://localhost:" + serverPort);
                    command.add("-Dfile.encoding=UTF-8");
                    command.add("-Dsun.stdout.encoding=UTF-8");
                    command.add("-Dsun.stderr.encoding=UTF-8");
                    command.add("-Dnative.encoding=UTF-8");
                    command.add("-DforkCount=1");
                    command.add("-DreuseForks=true");
                    command.add("-Dsurefire.useFile=false");
                    command.add("-Dsurefire.streamLogs=true");
                    command.add("-Dsurefire.console.output.reporter.stdout=true");

                    LOGGER.info("[Aura Server] Executing command: {}", String.join(" ", command));
                    broadcastLog("[INFO] Command: " + String.join(" ", command));
                    broadcastLog("[INFO] ------------------------------------------------------------------------");

                    final ProcessBuilder pb = new ProcessBuilder(command);
                    final String javaHome = System.getProperty("java.home");
                    if (javaHome != null && !javaHome.isBlank())
                    {
                        pb.environment().put("JAVA_HOME", javaHome);
                    }
                    pb.environment().put("MAVEN_OPTS",
                            "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8");
                    pb.redirectErrorStream(true);

                    final Process p;
                    try
                    {
                        p = pb.start();
                        activeProcess.set(p);
                    }
                    catch (final IOException e)
                    {
                        LOGGER.error("[Aura Server] Failed to start subprocess for " + file, e);
                        broadcastLog("[ERROR] Failed to start process: " + e.getMessage());
                        globalTestsRun.incrementAndGet();
                        globalFailed.incrementAndGet();
                        completedFiles.add(file);
                        continue;
                    }

                    final AtomicInteger fileTestsRun = new AtomicInteger(0);
                    final AtomicInteger fileFailures = new AtomicInteger(0);
                    final AtomicInteger fileErrors = new AtomicInteger(0);
                    final AtomicInteger fileSkipped = new AtomicInteger(0);

                    int prevReportedRuns = 0;
                    int prevReportedPassed = 0;
                    int prevReportedFailed = 0;
                    int prevReportedSkipped = 0;

                    try (final BufferedReader reader = new BufferedReader(
                            new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)))
                    {
                        String line;
                        while ((line = reader.readLine()) != null)
                        {
                            broadcastLog(line);
                            LOGGER.info("[Aura Subprocess] {}", stripAnsi(line));

                            final String cleanLine = stripAnsi(line);
                            final Matcher consoleMatcher = java.util.regex.Pattern
                                    .compile("Interactive Console:\\s*(http://.*)").matcher(cleanLine);
                            if (consoleMatcher.find()
                                    && !AiConfiguration.getInstance().isManagerActive())
                            {
                                broadcastInteractiveConsoleReady(consoleMatcher.group(1));
                            }

                            final Matcher m = STATS_PATTERN.matcher(cleanLine);
                            if (m.find())
                            {
                                final int currentRuns = Integer.parseInt(m.group(1));
                                final int currentFailures = Integer.parseInt(m.group(2)) + Integer.parseInt(m.group(3));
                                final int currentSkipped = m.group(4) != null ? Integer.parseInt(m.group(4)) : 0;
                                final int currentPassed = Math.max(0, currentRuns - currentFailures - currentSkipped);

                                fileTestsRun.set(currentRuns);
                                fileFailures.set(Integer.parseInt(m.group(2)));
                                fileErrors.set(Integer.parseInt(m.group(3)));
                                fileSkipped.set(currentSkipped);

                                final int deltaRuns = currentRuns - prevReportedRuns;
                                final int deltaPassed = currentPassed - prevReportedPassed;
                                final int deltaFailed = currentFailures - prevReportedFailed;
                                final int deltaSkipped = currentSkipped - prevReportedSkipped;

                                if (deltaRuns > 0)
                                {
                                    globalTestsRun.addAndGet(deltaRuns);
                                    if (deltaPassed > 0)
                                    {
                                        globalPassed.addAndGet(deltaPassed);
                                    }
                                    if (deltaFailed > 0)
                                    {
                                        globalFailed.addAndGet(deltaFailed);
                                    }
                                    if (deltaSkipped > 0)
                                    {
                                        globalSkipped.addAndGet(deltaSkipped);
                                    }

                                    prevReportedRuns = currentRuns;
                                    prevReportedPassed = currentPassed;
                                    prevReportedFailed = currentFailures;
                                    prevReportedSkipped = currentSkipped;
                                }
                            }
                        }
                    }
                    catch (final IOException e)
                    {
                        final boolean isStopped = manuallyStopped.get() || !p.isAlive() || activeProcess.get() == null;
                        final boolean isExpectedStreamClose = e.getMessage() != null
                                && (e.getMessage().equalsIgnoreCase("Stream closed")
                                        || e.getMessage().toLowerCase().contains("pipe closed")
                                        || e.getMessage().toLowerCase().contains("bad file descriptor"));

                        if (isStopped || isExpectedStreamClose)
                        {
                            LOGGER.debug("[Aura Server] Subprocess stream closed for {} during process termination/stop: {}", file, e.getMessage());
                        }
                        else
                        {
                            LOGGER.error("[Aura Server] Error reading subprocess stream for " + file, e);
                            broadcastLog("[ERROR] Error reading process output: " + e.getMessage());
                        }
                    }

                    int exitCode = -1;
                    try
                    {
                        exitCode = p.waitFor();
                    }
                    catch (final InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        LOGGER.warn("[Aura Server] Execution thread interrupted waiting for {}", file);
                        broadcastLog("[WARN] Thread interrupted while waiting for process completion.");
                    }

                    LOGGER.info("[Aura Server] Subprocess for {} completed with exit code: {}", file, exitCode);

                    if (manuallyStopped.get())
                    {
                        broadcastLog("[WARN] Process execution aborted by user.");
                        activeProcess.set(null);
                        break;
                    }

                    if (fileTestsRun.get() == 0)
                    {
                        globalTestsRun.incrementAndGet();
                        if (exitCode != 0)
                        {
                            globalFailed.incrementAndGet();
                            broadcastLog("[ERROR] Process exited with code " + exitCode + " and no tests were run.");
                        }
                        else
                        {
                            globalPassed.incrementAndGet();
                        }
                    }

                    completedFiles.add(file);
                    activeProcess.set(null);

                    final String primaryBrowser = (!targetProfiles.isEmpty()) ? targetProfiles.get(0) : "Default";
                    final String statusStr = (exitCode == 0) ? "passed" : "failed";

                    for (final File baseDir : runStorageDirs)
                    {
                        final File classDir = new File(baseDir, className);
                        if (!classDir.exists())
                        {
                            classDir.mkdirs();
                        }
                        final File execJson = new File(classDir, "console-execution-1.json");
                        if (!execJson.exists())
                        {
                            final String fallbackJson = String.format(
                                "{\"runId\":\"%s\",\"status\":\"%s\",\"currentStepIndex\":0,\"testName\":\"%s\",\"browser\":\"%s\",\"timestamp\":\"%s\"}",
                                runId, statusStr, className, primaryBrowser, new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new java.util.Date())
                            );
                            try
                            {
                                Files.writeString(execJson.toPath(), fallbackJson, StandardCharsets.UTF_8);
                            }
                            catch (final Exception e)
                            {
                                LOGGER.warn("[Aura Server] Could not write fallback execution snapshot to {}: {}", baseDir.getPath(), e.getMessage());
                            }
                        }
                    }
                }

                final String completedRunId = currentRunId.get();
                if (!manuallyStopped.get())
                {
                    if (onRunCompletedListener != null)
                    {
                        try
                        {
                            LOGGER.info("[Aura Server] Notifying run completion listener for runId: {}", completedRunId);
                            onRunCompletedListener.accept(completedRunId);
                        }
                        catch (final Exception e)
                        {
                            LOGGER.error("[Aura Server] Error in onRunCompletedListener for runId {}: {}", completedRunId, e.getMessage(), e);
                        }
                    }
                }

                final Map<String, Object> event = new HashMap<>();
                event.put("type", "reportReady");
                event.put("reportId", completedRunId);
                synchronized (currentRunEvents)
                {
                    currentRunEvents.add(event);
                }

                LOGGER.info("[Aura Server] Queue execution completed. Total: {}, Passed: {}, Failed: {}",
                        globalTestsRun.get(), globalPassed.get(), globalFailed.get());
                broadcastLog("\n[INFO] Queue execution completed.");
            }
            catch (final Exception e)
            {
                LOGGER.error("[Aura Server] Exception during queue execution", e);
                broadcastLog("[ERROR] Queue execution failed: " + e.getMessage());
            }
            finally
            {
                synchronized (createdTempFiles)
                {
                    for (final File f : createdTempFiles)
                    {
                        if (f != null && f.exists())
                        {
                            f.delete();
                            LOGGER.info("[Aura Server] Deleted temporary test runner: {}", f.getAbsolutePath());
                        }
                    }
                    createdTempFiles.clear();
                }
                File parent = tempRunnerDir;
                while (parent != null && parent.getPath().contains("src/test/java/com/xceptance/neodymium/aura/sandbox"))
                {
                    final File[] children = parent.listFiles();
                    if (children == null || children.length == 0)
                    {
                        parent.delete();
                        parent = parent.getParentFile();
                    }
                    else
                    {
                        break;
                    }
                }
                runningQueue.set(false);
                activeFile.set("");
            }
        });
        thread.setName("NeodymiumAuraQueueExecutor");
        thread.start();
    }

    private static final class ExecutionBatch
    {
        final String file;
        final List<String> targetProfiles;
        final List<String> datasetIds = new ArrayList<>();

        ExecutionBatch(final String file, final List<String> targetProfiles)
        {
            this.file = file;
            this.targetProfiles = targetProfiles;
        }
    }

    public List<String> getEffectiveBrowserProfiles(final DatasetSelection selection, final List<String> globalProfiles)
    {
        final List<String> rawCandidates = new ArrayList<>();
        if (selection != null && selection.browserProfiles != null && !selection.browserProfiles.isEmpty())
        {
            rawCandidates.addAll(selection.browserProfiles);
        }
        else if (globalProfiles != null && !globalProfiles.isEmpty())
        {
            rawCandidates.addAll(globalProfiles);
        }

        final List<BrowserProfileDto> allProfiles = getAvailableBrowserProfiles();
        final Set<String> availableIds = new HashSet<>();
        for (final BrowserProfileDto dto : allProfiles)
        {
            if (dto.available)
            {
                availableIds.add(dto.id);
            }
        }

        final List<String> sanitized = new ArrayList<>();
        for (final String candidate : rawCandidates)
        {
            if (availableIds.contains(candidate))
            {
                sanitized.add(candidate);
            }
            else
            {
                LOGGER.warn("[Aura Server] Skipping unavailable browser profile '{}' for execution on current platform.", candidate);
            }
        }

        if (!sanitized.isEmpty())
        {
            return sanitized;
        }

        // Default to first available Chrome profile
        for (final BrowserProfileDto dto : allProfiles)
        {
            if (dto.available && ("chrome".equalsIgnoreCase(dto.browser) || dto.id.toLowerCase().contains("chrome")))
            {
                return List.of(dto.id);
            }
        }

        // Otherwise default to first available profile
        for (final BrowserProfileDto dto : allProfiles)
        {
            if (dto.available)
            {
                return List.of(dto.id);
            }
        }

        return List.of("Chrome_1024x768");
    }

    /**
     * Strips ANSI escape sequences and console control codes from a log line.
     *
     * @param line input text containing ANSI escape sequences
     * @return cleaned text with control sequences removed
     */
    public static String stripAnsi(final String line)
    {
        if (line == null)
        {
            return null;
        }
        final String clean = line.replaceAll("(?i)\\u001B\\[[;0-9]*[a-zA-Z]", "");
        final String cleanNoLeftover = clean.replaceAll("\\[[0-9]+(;[0-9]+)*[a-zA-Z]", "");
        return cleanNoLeftover.replaceAll("(?i)\\[[a-zA-Z](?![a-zA-Z0-9])", "");
    }

    /**
     * Resolves and maps all configured multibrowser profiles available to test executions.
     *
     * @return list of populated BrowserProfileDto representations
     */
    public List<BrowserProfileDto> getAvailableBrowserProfiles()
    {
        final MultibrowserConfiguration config = MultibrowserConfiguration.getInstance();
        final Map<String, BrowserConfiguration> rawProfiles = config.getBrowserProfiles();
        final List<BrowserProfileDto> list = new ArrayList<>();
        if (rawProfiles != null)
        {
            for (final Map.Entry<String, BrowserConfiguration> entry : rawProfiles.entrySet())
            {
                final String tag = entry.getKey();
                final BrowserConfiguration bc = entry.getValue();
                if (tag == null || tag.trim().isEmpty() || "default".equalsIgnoreCase(tag) || "global".equalsIgnoreCase(tag))
                {
                    continue;
                }
                final String name = bc.getName() != null && !bc.getName().trim().isEmpty() ? bc.getName() : tag;
                final String rawBrowserName = bc.getCapabilities() != null && bc.getCapabilities().getBrowserName() != null
                        ? bc.getCapabilities().getBrowserName().toLowerCase()
                        : "";
                final String tagLower = tag.toLowerCase();
                final String browser;
                if (tagLower.contains("galaxy") || tagLower.contains("iphone") || tagLower.contains("pixel")
                        || tagLower.contains("mobile") || tagLower.contains("nexus") || tagLower.contains("android")
                        || tagLower.contains("ipad") || rawBrowserName.contains("android") || rawBrowserName.contains("iphone")
                        || rawBrowserName.contains("ipad"))
                {
                    browser = "mobile";
                }
                else if (rawBrowserName.contains("chrome") || tagLower.startsWith("chrome") || tagLower.contains("_chrome")
                        || tagLower.contains("chromium"))
                {
                    browser = "chrome";
                }
                else if (rawBrowserName.contains("firefox") || tagLower.startsWith("ff") || tagLower.contains("firefox")
                        || tagLower.contains("_ff"))
                {
                    browser = "firefox";
                }
                else if (rawBrowserName.contains("safari") || tagLower.startsWith("safari") || tagLower.contains("_safari")
                        || tagLower.contains("webkit"))
                {
                    browser = "safari";
                }
                else if (rawBrowserName.contains("edge") || rawBrowserName.contains("microsoftedge") || tagLower.startsWith("edge")
                        || tagLower.contains("_edge"))
                {
                    browser = "edge";
                }
                else
                {
                    browser = "other";
                }

                final String res = (bc.getBrowserWidth() > 0 && bc.getBrowserHeight() > 0)
                        ? bc.getBrowserWidth() + "x" + bc.getBrowserHeight()
                        : "";
                final boolean isHeadless = bc.isHeadless();

                final BrowserAvailabilityChecker.CheckResult checkResult = BrowserAvailabilityChecker.check(tag, bc, browser);
                list.add(new BrowserProfileDto(tag, name, browser, res, isHeadless, checkResult.isAvailable(), checkResult.getReason()));
            }
        }
        return list;
    }
}
