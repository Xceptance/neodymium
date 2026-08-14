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
package com.xceptance.aura.report.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Native Spring Service managing the test execution queue, starting Maven test processes,
 * streaming live stdout logs, and parsing execution counters natively.
 *
 * @author Xceptance GmbH 2026
 */
@Service
public class NeodymiumAuraQueueService
{
    private static final Logger LOG = LoggerFactory.getLogger(NeodymiumAuraQueueService.class);
    private static final Pattern STATS_PATTERN = Pattern
        .compile("Tests run:\\s*(\\d+),\\s*Failures:\\s*(\\d+),\\s*Errors:\\s*(\\d+)(?:,\\s*Skipped:\\s*(\\d+))?");

    private static final String NEODYMIUM_WORK_DIR = "/home/olha/git/neodymium";

    private final List<String> liveLogBuffer = Collections.synchronizedList(new ArrayList<>());
    private final AtomicReference<Process> activeProcess = new AtomicReference<>(null);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<String> activeSuite = new AtomicReference<>("ALL");

    private final AtomicInteger testsRun = new AtomicInteger(0);
    private final AtomicInteger passed = new AtomicInteger(0);
    private final AtomicInteger failed = new AtomicInteger(0);
    private final AtomicInteger skipped = new AtomicInteger(0);

    public synchronized Map<String, Object> getLiveData()
    {
        final Map<String, Object> data = new HashMap<>();
        data.put("running", running.get());
        data.put("activeSuite", activeSuite.get());
        data.put("testsRun", testsRun.get());
        data.put("passed", passed.get());
        data.put("failed", failed.get());
        data.put("skipped", skipped.get());

        synchronized (liveLogBuffer)
        {
            final int size = liveLogBuffer.size();
            final int fromIndex = Math.max(0, size - 150);
            data.put("logs", new ArrayList<>(liveLogBuffer.subList(fromIndex, size)));
        }

        return data;
    }

    public synchronized boolean startRun(final String suiteName, final String environment, final String browser)
    {
        if (running.get())
        {
            LOG.info("A test suite execution is already in progress.");
            return false;
        }

        liveLogBuffer.clear();
        testsRun.set(0);
        passed.set(0);
        failed.set(0);
        skipped.set(0);

        final String effectiveSuite = (suiteName != null && !suiteName.isEmpty()) ? suiteName : "ALL";
        activeSuite.set(effectiveSuite);
        running.set(true);

        liveLogBuffer.add("[Aura Engine] Enqueuing test run: " + effectiveSuite + " (Env: " + environment + ", Browser: " + browser + ")...");

        new Thread(() -> {
            try
            {
                final String testClassPattern;
                if ("ALL".equalsIgnoreCase(effectiveSuite) || effectiveSuite.isEmpty())
                {
                    testClassPattern = "*Test";
                }
                else if (effectiveSuite.contains("."))
                {
                    testClassPattern = effectiveSuite;
                }
                else if (effectiveSuite.toLowerCase().contains("smoke"))
                {
                    testClassPattern = "com.xceptance.neodymium.aura.data.runs.**.*";
                }
                else
                {
                    testClassPattern = "**/*" + effectiveSuite;
                }

                final List<String> cmd = new ArrayList<>();
                cmd.add("mvn");
                cmd.add("test");
                cmd.add("-Dtest=" + testClassPattern);
                cmd.add("-Dneodymium.browser=" + (browser != null ? browser : "Chrome"));

                final ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(new File(NEODYMIUM_WORK_DIR));
                pb.redirectErrorStream(true);

                LOG.info("Launching Neodymium test process in {}: {}", NEODYMIUM_WORK_DIR, String.join(" ", cmd));
                final Process p = pb.start();
                activeProcess.set(p);

                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream())))
                {
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        final String cleanLine = stripAnsi(line);
                        liveLogBuffer.add(cleanLine);
                        parseLineForStats(cleanLine);
                    }
                }

                p.waitFor();
                liveLogBuffer.add("[Aura Engine] Test execution finished with exit code " + p.exitValue() + ".");
            }
            catch (final Exception e)
            {
                LOG.error("Error running test execution: {}", e.getMessage(), e);
                liveLogBuffer.add("[Aura Engine] Execution error: " + e.getMessage());
            }
            finally
            {
                activeProcess.set(null);
                running.set(false);
            }
        }, "AuraTestRunner-Thread").start();

        return true;
    }

    public synchronized void stopRun()
    {
        final Process p = activeProcess.getAndSet(null);
        if (p != null && p.isAlive())
        {
            LOG.info("Terminating active Maven test execution process...");
            p.destroy();
            try
            {
                p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
            }
            catch (final InterruptedException ignored)
            {
            }
            if (p.isAlive())
            {
                p.destroyForcibly();
            }
            liveLogBuffer.add("[Aura Engine] Execution manually stopped by user.");
        }
        running.set(false);
    }

    public void clearLogs()
    {
        liveLogBuffer.clear();
    }

    private void parseLineForStats(final String line)
    {
        if (line == null || !line.contains("Tests run:"))
        {
            return;
        }

        final Matcher matcher = STATS_PATTERN.matcher(line);
        if (matcher.find())
        {
            try
            {
                final int total = Integer.parseInt(matcher.group(1));
                final int fails = Integer.parseInt(matcher.group(2));
                final int errors = Integer.parseInt(matcher.group(3));
                final int skps = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 0;

                final int totalFails = fails + errors;
                final int pass = Math.max(0, total - totalFails - skps);

                testsRun.set(total);
                passed.set(pass);
                failed.set(totalFails);
                skipped.set(skps);
            }
            catch (final Exception ignored)
            {
            }
        }
    }

    private static String stripAnsi(final String line)
    {
        if (line == null) return "";
        return line.replaceAll("(?i)\\u001B\\[[;0-9]*[a-zA-Z]", "");
    }
}
