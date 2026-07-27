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

import com.xceptance.neodymium.aura.dto.DatasetSelection;
import com.xceptance.neodymium.aura.dto.RunRequest;
import com.xceptance.neodymium.util.Neodymium;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service handling test execution reporting, run history, metadata storage, and report compilation.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraReportingService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraReportingService.class);

    private final AtomicReference<Process> activeProcess = new AtomicReference<>(null);

    public AuraReportingService()
    {
    }

    public File getReportHistoryDir()
    {
        final String path = Neodymium.configuration().reportHistoryDir();
        final File file = new File(path);
        if (file.isAbsolute())
        {
            try
            {
                return file.getCanonicalFile();
            }
            catch (final IOException e)
            {
                return file.getAbsoluteFile();
            }
        }
        else
        {
            try
            {
                return new File(System.getProperty("user.dir"), path).getCanonicalFile();
            }
            catch (final IOException e)
            {
                return new File(System.getProperty("user.dir"), path).getAbsoluteFile();
            }
        }
    }

    public List<Map<String, Object>> getHistoryList()
    {
        final File historyDir = getReportHistoryDir();
        final List<Map<String, Object>> historyList = new ArrayList<>();

        if (historyDir.exists() && historyDir.isDirectory())
        {
            final File[] dirs = historyDir.listFiles(File::isDirectory);
            if (dirs != null)
            {
                Arrays.sort(dirs, (a, b) -> b.getName().compareTo(a.getName()));
                for (final File dir : dirs)
                {
                    String status = "Passed";
                    String timestamp = "";
                    String total = "-";
                    String passed = "-";
                    String failed = "-";
                    long durationMs = 0L;
                    boolean headless = false;
                    boolean allureEnabled = true;
                    boolean videoEnabled = false;
                    final File metadataFile = new File(dir, "metadata.json");
                    if (metadataFile.exists() && metadataFile.isFile())
                    {
                        try
                        {
                            final String meta = Files.readString(metadataFile.toPath(), StandardCharsets.UTF_8);
                            final Map<?, ?> map = AuraHttpUtils.gson.fromJson(meta, Map.class);
                            if (map != null)
                            {
                                if (map.containsKey("status"))
                                {
                                    status = String.valueOf(map.get("status"));
                                }
                                if (map.containsKey("timestamp"))
                                {
                                    timestamp = String.valueOf(map.get("timestamp"));
                                }
                                if (map.containsKey("total"))
                                {
                                    total = String.valueOf(
                                            Math.round(Double.parseDouble(String.valueOf(map.get("total")))));
                                }
                                if (map.containsKey("passed"))
                                {
                                    passed = String.valueOf(
                                            Math.round(Double.parseDouble(String.valueOf(map.get("passed")))));
                                }
                                if (map.containsKey("failed"))
                                {
                                    failed = String.valueOf(
                                            Math.round(Double.parseDouble(String.valueOf(map.get("failed")))));
                                }
                                if (map.containsKey("durationMs"))
                                {
                                    try
                                    {
                                        durationMs = Math.round(Double.parseDouble(String.valueOf(map.get("durationMs"))));
                                    }
                                    catch (final NumberFormatException ignore)
                                    {
                                        // keep default
                                    }
                                }
                                if (map.containsKey("headless"))
                                {
                                    headless = Boolean.parseBoolean(String.valueOf(map.get("headless")));
                                }
                                if (map.containsKey("allureEnabled"))
                                {
                                    allureEnabled = Boolean.parseBoolean(String.valueOf(map.get("allureEnabled")));
                                }
                                if (map.containsKey("videoEnabled"))
                                {
                                    videoEnabled = Boolean.parseBoolean(String.valueOf(map.get("videoEnabled")));
                                }
                            }
                        }
                        catch (final Exception e)
                        {
                            // ignore
                        }
                    }

                    String runConfigRaw = null;
                    if (metadataFile.exists() && metadataFile.isFile())
                    {
                        try
                        {
                            final String meta = Files.readString(metadataFile.toPath(), StandardCharsets.UTF_8);
                            final com.google.gson.JsonObject metaObj = com.google.gson.JsonParser
                                    .parseString(meta).getAsJsonObject();
                            if (metaObj.has("runConfig") && !metaObj.get("runConfig").isJsonNull())
                            {
                                runConfigRaw = AuraHttpUtils.gson.toJson(metaObj.get("runConfig"));
                            }
                        }
                        catch (final Exception e)
                        {
                            // ignore
                        }
                    }

                    final File indexHtml = new File(new File(dir, "allure-report"), "index.html");
                    final boolean hasReport = indexHtml.exists() && indexHtml.isFile();

                    final Map<String, Object> item = new HashMap<>();
                    item.put("id", dir.getName());
                    item.put("status", status);
                    item.put("timestamp", timestamp);
                    item.put("total", total);
                    item.put("passed", passed);
                    item.put("failed", failed);
                    item.put("durationMs", durationMs);
                    item.put("headless", headless);
                    item.put("allureEnabled", allureEnabled);
                    item.put("videoEnabled", videoEnabled);
                    item.put("hasReport", hasReport);
                    item.put("runConfig", runConfigRaw);

                    historyList.add(item);
                }
            }
        }
        return historyList;
    }

    public File resolveReportAsset(final String reportId, final String assetPath) throws IOException
    {
        final File historyDir = getReportHistoryDir().getCanonicalFile();
        final File reportDir = new File(historyDir, reportId).getCanonicalFile();
        if (!reportDir.getPath().startsWith(historyDir.getPath()))
        {
            throw new SecurityException("Access denied: Directory traversal detected");
        }
        final File assetFile = new File(reportDir, assetPath).getCanonicalFile();
        if (!assetFile.getPath().startsWith(reportDir.getPath()))
        {
            throw new SecurityException("Access denied: Directory traversal detected");
        }
        return assetFile;
    }

    public void deleteReport(final String id) throws IOException
    {
        final File historyDir = getReportHistoryDir().getCanonicalFile();
        final File reportDir = new File(historyDir, id).getCanonicalFile();
        if (!reportDir.getPath().startsWith(historyDir.getPath()))
        {
            throw new SecurityException("Access denied: Directory traversal detected");
        }
        if (reportDir.exists() && reportDir.isDirectory())
        {
            deleteDirRecursively(reportDir);
        }
    }

    public void deleteDirRecursively(final File file)
    {
        final File[] children = file.listFiles();
        if (children != null)
        {
            for (final File child : children)
            {
                deleteDirRecursively(child);
            }
        }
        file.delete();
    }

    public void copyDirectory(final File src, final File dest) throws IOException
    {
        if (src.isDirectory())
        {
            if (!dest.exists() && !dest.mkdirs())
            {
                throw new IOException("Failed to create directory: " + dest.getAbsolutePath());
            }
            final String[] children = src.list();
            if (children != null)
            {
                for (final String child : children)
                {
                    copyDirectory(new File(src, child), new File(dest, child));
                }
            }
        }
        else
        {
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void cancelReportCompilation()
    {
        final Process p = activeProcess.getAndSet(null);
        if (p != null)
        {
            p.destroy();
        }
    }

    public void generateReport(final List<String> files, final RunRequest req, final long runStartTimeMs,
            final int testsRun, final int passed, final int failed, final int skipped, final boolean manuallyStoppedVal,
            final List<String> runLogs, final List<Map<String, Object>> runEvents)
    {
        LOGGER.info("[Aura Server] Compiling report history...");
        final List<String> command = new ArrayList<>();
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
        command.add("io.qameta.allure:allure-maven:report");
        command.add("-Dallure.results.directory=" + new File("target/allure-results").getAbsolutePath());

        final ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try
        {
            LOGGER.info("[Aura Server] Executing report compilation command: {}", String.join(" ", command));
            final Process p = pb.start();
            activeProcess.set(p);

            try (final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    LOGGER.info(line);
                    synchronized (runLogs)
                    {
                        runLogs.add(NeodymiumAuraManager.stripAnsi(line));
                    }
                }
            }

            final int exitCode = p.waitFor();
            activeProcess.set(null);

            if (exitCode == 0)
            {
                LOGGER.info("[Aura Server] Report successfully compiled.");
                copyReportToHistory(files, req, runStartTimeMs, testsRun, passed, failed, skipped, manuallyStoppedVal, runLogs, runEvents);
            }
            else
            {
                LOGGER.error("[Aura Server] Report compilation failed with exit code: {}", exitCode);
            }
        }
        catch (final Exception e)
        {
            LOGGER.error("[Aura Server] Failed to compile report", e);
            activeProcess.set(null);
        }
    }

    private void copyReportToHistory(final List<String> files, final RunRequest req, final long runStartTimeMs,
            final int testsRun, final int passed, final int failed, final int skipped, final boolean manuallyStoppedVal,
            final List<String> runLogs, final List<Map<String, Object>> runEvents)
    {
        final File srcDir = new File("target/site/allure-maven-plugin");
        if (!srcDir.exists() || !srcDir.isDirectory())
        {
            LOGGER.error("[Aura Server] Report source directory not found: {}", srcDir.getAbsolutePath());
            return;
        }

        final String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final List<String> names = new ArrayList<>();
        for (final String file : files)
        {
            final String name = new File(file).getName();
            final int dotIdx = name.lastIndexOf('.');
            names.add(dotIdx > 0 ? name.substring(0, dotIdx) : name);
        }
        final String joined = String.join("_", names);
        final String folderName = timestamp + "_" + (joined.isEmpty() ? "run" : joined);
        final File destDir = new File(getReportHistoryDir(), folderName).getAbsoluteFile();

        try
        {
            copyDirectory(srcDir, new File(destDir, "allure-report"));

            // Write metadata.json
            final File metadataFile = new File(destDir, "metadata.json");
            final String status = manuallyStoppedVal ? "Aborted"
                    : (failed == 0 && passed == 0 && skipped > 0 ? "Aborted" : (failed == 0 ? "Passed" : "Failed"));
            final long durationMs = System.currentTimeMillis() - runStartTimeMs;
            final boolean headless = req != null && req.headless;
            final boolean allureEnabled = req != null && req.allure;
            final boolean videoEnabled = req != null && req.video;

            final String runConfigJson = AuraHttpUtils.gson.toJson(req);

            final String metadataContent = String.format(
                    "{%n  \"status\": \"%s\",%n  \"timestamp\": \"%s\",%n  \"total\": %d,%n  \"passed\": %d,%n  \"failed\": %d,%n  \"skipped\": %d,%n  \"durationMs\": %d,%n  \"headless\": %b,%n  \"allureEnabled\": %b,%n  \"videoEnabled\": %b,%n  \"runConfig\": %s%n}",
                    status, timestamp, testsRun, passed, failed, skipped,
                    durationMs, headless, allureEnabled, videoEnabled, runConfigJson);
            Files.writeString(metadataFile.toPath(), metadataContent, StandardCharsets.UTF_8);

            // Write full execution log
            final File logFile = new File(destDir, "execution.log");
            final List<String> logSnapshot;
            synchronized (runLogs)
            {
                logSnapshot = new ArrayList<>(runLogs);
            }
            Files.write(logFile.toPath(), logSnapshot, StandardCharsets.UTF_8);

            // Write per-test logs
            writePerTestLogs(destDir, logSnapshot);

            final File targetDir = new File("target/allure-results");
            final File[] consoleFiles = targetDir
                    .listFiles((dir, name) -> name.startsWith("console-execution") && name.endsWith(".json"));
            if (consoleFiles != null && consoleFiles.length > 0)
            {
                for (final File consoleFile : consoleFiles)
                {
                    final File destConsoleFile = new File(destDir, consoleFile.getName());
                    Files.copy(consoleFile.toPath(), destConsoleFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                LOGGER.info("[Aura Server] Archived {} console-execution JSONs to {}", consoleFiles.length,
                        destDir.getName());
            }
            else if (files != null && !files.isEmpty())
            {
                int testCounter = 1;
                for (final String file : files)
                {
                    final String baseName = new File(file).getName();
                    final String yamlLabel = baseName.endsWith(".yaml") ? baseName.substring(0, baseName.length() - 5) : baseName;
                    final List<String> datasets = new ArrayList<>();
                    if (req != null && req.datasets != null)
                    {
                        for (final DatasetSelection ds : req.datasets)
                        {
                            if (file.equals(ds.file) && ds.id != null && !ds.id.isEmpty())
                            {
                                datasets.add(ds.id);
                            }
                        }
                    }
                    
                    if (!datasets.isEmpty())
                    {
                        for (final String ds : datasets)
                        {
                            final Map<String, Object> testData = new HashMap<>();
                            testData.put("testName", yamlLabel + " (" + ds + ")");
                            testData.put("testId", ds);
                            testData.put("yamlSource", file);
                            testData.put("status", status);
                            testData.put("steps", Collections.emptyList());
                            
                            final String json = AuraHttpUtils.gson.toJson(testData);
                            Files.writeString(new File(destDir, "console-execution-" + (testCounter++) + ".json").toPath(), json, StandardCharsets.UTF_8);
                        }
                    }
                    else
                    {
                        final Map<String, Object> testData = new HashMap<>();
                        testData.put("testName", yamlLabel);
                        testData.put("yamlSource", file);
                        testData.put("status", status);
                        testData.put("steps", Collections.emptyList());
                        
                        final String json = AuraHttpUtils.gson.toJson(testData);
                        Files.writeString(new File(destDir, "console-execution-" + (testCounter++) + ".json").toPath(), json, StandardCharsets.UTF_8);
                    }
                }
            }

            final File screenshotsDir = new File("target/ai-console-screenshots");
            if (screenshotsDir.exists() && screenshotsDir.isDirectory())
            {
                final File destScreenshots = new File(destDir, "screenshots");
                copyDirectory(screenshotsDir, destScreenshots);
                LOGGER.info("[Aura Server] Archived screenshots to {}", destDir.getName());
            }

            LOGGER.info("[Aura Server] Report copied to history: {}", destDir.getName());
            final Map<String, Object> event = new HashMap<>();
            event.put("type", "reportReady");
            event.put("reportId", destDir.getName());
            synchronized (runEvents)
            {
                runEvents.add(event);
            }
        }
        catch (final IOException e)
        {
            LOGGER.error("[Aura Server] Failed to copy report to history", e);
        }
    }

    private void writePerTestLogs(final File destDir, final List<String> logSnapshot)
    {
        final Pattern surefireBoundary = Pattern.compile("^Running\\s+(\\S+)$");
        final Map<String, String> classToTestName = new HashMap<>();
        final File[] consoleFiles = destDir
                .listFiles((d, name) -> name.startsWith("console-execution") && name.endsWith(".json"));
        if (consoleFiles != null)
        {
            for (final File cf : consoleFiles)
            {
                try
                {
                    final String content = Files.readString(cf.toPath(), StandardCharsets.UTF_8);
                    final Map<?, ?> map = AuraHttpUtils.gson.fromJson(content, Map.class);
                    if (map != null && map.containsKey("testName"))
                    {
                        final String testName = String.valueOf(map.get("testName"));
                        final String cfBase = cf.getName()
                                .replace("console-execution-", "")
                                .replace(".json", "");
                        classToTestName.put(cfBase, testName);
                    }
                }
                catch (final Exception ignore)
                {
                    // ignore, best-effort only
                }
            }
        }

        final Map<String, List<String>> segmentsByClass = new LinkedHashMap<>();
        String currentClass = null;

        for (final String line : logSnapshot)
        {
            final Matcher m = surefireBoundary.matcher(line.trim());
            if (m.find())
            {
                final String fqcn = m.group(1);
                currentClass = fqcn.contains(".") ? fqcn.substring(fqcn.lastIndexOf('.') + 1) : fqcn;
                segmentsByClass.computeIfAbsent(currentClass, k -> new ArrayList<>());
            }
            if (currentClass != null)
            {
                segmentsByClass.get(currentClass).add(line);
            }
        }

        for (final Map.Entry<String, List<String>> entry : segmentsByClass.entrySet())
        {
            final String classKey = entry.getKey();
            final List<String> lines = entry.getValue();

            final String testName = classToTestName.getOrDefault(classKey, classKey);
            final String safeLogName = testName.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".log";
            final File logFile = new File(destDir, safeLogName);
            try
            {
                Files.write(logFile.toPath(), lines, StandardCharsets.UTF_8);
                LOGGER.info("[Aura Server] Wrote per-test log: {}", safeLogName);
            }
            catch (final IOException e)
            {
                LOGGER.warn("[Aura Server] Failed to write per-test log {}: {}", safeLogName, e.getMessage());
            }
        }
    }
}
