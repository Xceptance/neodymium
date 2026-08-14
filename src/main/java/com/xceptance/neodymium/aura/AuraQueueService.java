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
import com.xceptance.neodymium.aura.dto.DatasetSelection;
import com.xceptance.neodymium.aura.dto.RunRequest;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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

    /** Wall-clock start time in ms of the current (or most recent) queue run. */
    private final AtomicLong runStartTimeMs = new AtomicLong(0);

    /** The RunRequest that initiated the most recent queue execution, used for archiving metadata. */
    private final AtomicReference<RunRequest> lastRunRequest = new AtomicReference<>(null);

    private final Set<File> createdTempFiles = Collections.synchronizedSet(new HashSet<>());

    private final AuraReportingService reportingService;
    private final AuraInteractiveService interactiveService;

    public AuraQueueService(final AuraReportingService reportingService, final AuraInteractiveService interactiveService)
    {
        this.reportingService = reportingService;
        this.interactiveService = interactiveService;
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
                    reportingService.deleteDirRecursively(allureResultsDir);
                }

                final Map<String, List<String>> datasetsByFile = new LinkedHashMap<>();
                for (final DatasetSelection selection : req.datasets)
                {
                    datasetsByFile.computeIfAbsent(selection.file, k -> new ArrayList<>()).add(selection.id);
                }

                // Group the requested datasets by file order to match the physical execution order
                final List<DatasetSelection> groupedDatasets = new ArrayList<>();
                for (final Map.Entry<String, List<String>> entry : datasetsByFile.entrySet())
                {
                    for (final String id : entry.getValue())
                    {
                        final DatasetSelection ds = new DatasetSelection();
                        ds.file = entry.getKey();
                        ds.id = id;
                        groupedDatasets.add(ds);
                    }
                }
                req.datasets = groupedDatasets;

                LOGGER.info("[Aura Server] Starting execution of {} dataset(s) in {} file(s) in queue.",
                        req.datasets.size(), datasetsByFile.size());
                globalTestsRun.set(0);
                globalPassed.set(0);
                globalFailed.set(0);
                globalSkipped.set(0);
                manuallyStopped.set(false);
                currentRunLogs.clear();
                currentRunEvents.clear();
                completedFiles.clear();

                runStartTimeMs.set(System.currentTimeMillis());
                lastRunRequest.set(req);

                final List<Map.Entry<String, List<String>>> entries = new ArrayList<>(datasetsByFile.entrySet());
                for (int i = 0; i < entries.size(); i++)
                {
                    final Map.Entry<String, List<String>> entry = entries.get(i);
                    final String file = entry.getKey();
                    final List<String> ids = entry.getValue();

                    activeFile.set(file);

                    // Generate temporary runner
                    final String safeName = file.replaceAll("[^a-zA-Z0-9]", "_");
                    final String className = "Aura_" + safeName + "_Test";
                    final File tempRunnerFile = new File(tempRunnerDir, className + ".java");
                    if (!tempRunnerFile.exists())
                    {
                        tempRunnerDir.mkdirs();
                        final String runnerSource = "package com.xceptance.neodymium.aura.sandbox;\n\n" +
                                "import com.xceptance.neodymium.common.browser.Browser;\n" +
                                "import com.xceptance.neodymium.common.testdata.DataFolder;\n" +
                                "import com.xceptance.neodymium.junit5.NeodymiumTest;\n" +
                                "import org.neodymium.ai.junit.NeodymiumAiTest;\n" +
                                "import org.junit.jupiter.api.DisplayName;\n\n" +
                                "@Browser()\n" +
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
                        LOGGER.info("[Aura Server] Created temporary test runner: {}",
                                tempRunnerFile.getAbsolutePath());
                    }
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
                            idFilterBuilder.append(Pattern.quote(ids.get(j)));
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
                    final String runId = "run-" + System.currentTimeMillis();
                    final InteractiveConsoleEngine engine = new InteractiveConsoleEngine(runId);
                    interactiveService.setCurrentConsoleEngine(engine);
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
                        command.add("mvn");
                    }
                    command.add("test");
                    command.add("-Dmaven.compiler.skip=true");
                    command.add("-Dcompiler.skip=true");
                    final File compiledClassFile = new File("target/test-classes/com/xceptance/neodymium/aura/sandbox/" + className + ".class");
                    if (compiledClassFile.exists())
                    {
                        command.add("-Dtest=com.xceptance.neodymium.aura.sandbox." + className);
                    }
                    else
                    {
                        command.add("-Dtest=com.xceptance.neodymium.aura.AuraYamlRunnerTest");
                    }
                    command.add("-Dneodymium.testFileFilter=" + file.replace(".", "\\."));
                    command.add("-Dallure.results.directory=" + new File("target/aura-sandbox/allure-results").getAbsolutePath());
                    command.add("-Dneodymium.ai.console.screenshotsDir=" + new File("target/aura-sandbox/ai-console-screenshots").getAbsolutePath());
                    if (hasIds)
                    {
                        command.add("-Dneodymium.testIdFilter=" + idFilterBuilder.toString());
                    }
                    command.add("-Dbrowserprofile.Default.headless=" + req.headless);
                    command.add("-Dselenide.headless=" + req.headless);
                    command.add("-Dneodymium.ai.interactive=" + req.interactive);
                    command.add("-Dvideo.enableFilming=" + req.video);
                    command.add("-Dneodymium.ai.executionMode=" + (req.executionMode != null ? req.executionMode : "REPLAY_WITH_HEALING"));
                    command.add("-Dneodymium.managerActive=true");
                    command.add("-Dneodymium.aura.test=" + System.getProperty("neodymium.aura.test", "false"));
                    command.add("-Dneodymium.managerRunId=" + runId);
                    command.add("-Dneodymium.managerUrl=http://localhost:" + serverPort);
                    command.add("-Dfile.encoding=UTF-8");
                    command.add("-Dsun.stdout.encoding=UTF-8");
                    command.add("-Dsun.stderr.encoding=UTF-8");
                    command.add("-Dnative.encoding=UTF-8");
                    command.add("-DforkCount=0");
                    command.add("-DreuseForks=true");
                    command.add("-Dsurefire.useFile=false");

                    LOGGER.info("[Aura Server] Executing command: {}", String.join(" ", command));
                    broadcastLog("[INFO] Command: " + String.join(" ", command));
                    broadcastLog("[INFO] ------------------------------------------------------------------------");

                    final ProcessBuilder pb = new ProcessBuilder(command);
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

                    try (final BufferedReader reader = new BufferedReader(
                            new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)))
                    {
                        String line;
                        while ((line = reader.readLine()) != null)
                        {
                            broadcastLog(line);
                            LOGGER.info("[Aura Subprocess] {}", NeodymiumAuraManager.stripAnsi(line));

                            final String cleanLine = NeodymiumAuraManager.stripAnsi(line);
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
                                fileTestsRun.set(Integer.parseInt(m.group(1)));
                                fileFailures.set(Integer.parseInt(m.group(2)));
                                fileErrors.set(Integer.parseInt(m.group(3)));
                                if (m.group(4) != null)
                                {
                                    fileSkipped.set(Integer.parseInt(m.group(4)));
                                }
                            }
                        }
                    }
                    catch (final IOException e)
                    {
                        LOGGER.error("[Aura Server] Error reading subprocess stream for " + file, e);
                        broadcastLog("[ERROR] Error reading process output: " + e.getMessage());
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

                    if (fileTestsRun.get() > 0)
                    {
                        globalTestsRun.addAndGet(fileTestsRun.get());
                        final int failures = fileFailures.get() + fileErrors.get();
                        final int skipped = fileSkipped.get();
                        globalFailed.addAndGet(failures);
                        globalSkipped.addAndGet(skipped);
                        globalPassed.addAndGet(fileTestsRun.get() - failures - skipped);
                    }
                    else
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
                }

                if (!manuallyStopped.get())
                {
                    final List<String> uniqueFiles = new ArrayList<>(datasetsByFile.keySet());
                    if (req.allure)
                    {
                        LOGGER.info("[Aura Server] Auto-generating report as requested.");
                        reportingService.generateReport(uniqueFiles, req, runStartTimeMs.get(), globalTestsRun.get(),
                                globalPassed.get(), globalFailed.get(), globalSkipped.get(), manuallyStopped.get(),
                                currentRunLogs, currentRunEvents);
                    }
                    else
                    {
                        LOGGER.info("[Aura Server] Archiving execution run to history...");
                        reportingService.copyReportToHistory(uniqueFiles, req, runStartTimeMs.get(), globalTestsRun.get(),
                                globalPassed.get(), globalFailed.get(), globalSkipped.get(), manuallyStopped.get(),
                                currentRunLogs, currentRunEvents);
                    }
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
}
