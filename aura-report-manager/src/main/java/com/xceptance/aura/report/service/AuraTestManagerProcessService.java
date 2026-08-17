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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service managing the lifecycle, execution, and health monitoring of the Neodymium Aura Test Manager.
 *
 * @author Xceptance GmbH 2026
 */
@Service
public class AuraTestManagerProcessService
{
    private static final Logger LOG = LoggerFactory.getLogger(AuraTestManagerProcessService.class);

    private static final String SCRIPT_PATH = "/home/olha/git/neodymium/run-aura.sh";
    private static final String WORK_DIR = "/home/olha/git/neodymium";
    private static final int TARGET_PORT = 18080;
    private static final String MANAGER_URL = "http://localhost:" + TARGET_PORT;

    private Process processHandle = null;
    private boolean starting = false;

    public synchronized Map<String, Object> getStatus()
    {
        final boolean running = isPortListening(TARGET_PORT);
        if (running)
        {
            starting = false;
        }

        final Map<String, Object> status = new HashMap<>();
        status.put("running", running);
        status.put("starting", starting && !running);
        status.put("port", TARGET_PORT);
        status.put("url", MANAGER_URL);
        status.put("scriptPath", SCRIPT_PATH);
        return status;
    }

    public synchronized Map<String, Object> startManager()
    {
        if (isPortListening(TARGET_PORT))
        {
            LOG.info("Neodymium Aura Test Manager is already running on port {}.", TARGET_PORT);
            starting = false;
            return getStatus();
        }

        final File scriptFile = new File(SCRIPT_PATH);
        final File workDirFile = new File(WORK_DIR);

        if (!scriptFile.exists())
        {
            LOG.error("Aura Test Manager script does not exist at path: {}", SCRIPT_PATH);
            final Map<String, Object> err = getStatus();
            err.put("error", "Script file not found: " + SCRIPT_PATH);
            return err;
        }

        starting = true;

        new Thread(() -> {
            try
            {
                final ProcessBuilder pb = new ProcessBuilder("/bin/bash", SCRIPT_PATH);
                pb.directory(workDirFile);
                pb.redirectErrorStream(true);

                LOG.info("Launching Neodymium Aura Test Manager process via script: {}", SCRIPT_PATH);
                synchronized (AuraTestManagerProcessService.this)
                {
                    processHandle = pb.start();
                }

                try (final var reader = new java.io.BufferedReader(new java.io.InputStreamReader(processHandle.getInputStream())))
                {
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        LOG.info("[AuraTestManager] {}", line);
                    }
                }
            }
            catch (final Exception e)
            {
                LOG.error("Failed to execute Neodymium Aura Test Manager script: {}", e.getMessage(), e);
            }
            finally
            {
                synchronized (AuraTestManagerProcessService.this)
                {
                    starting = false;
                }
            }
        }, "AuraTestManager-ProcessLauncher").start();

        // Brief 1.5s wait for immediate output
        try
        {
            Thread.sleep(1500);
        }
        catch (final InterruptedException ignored)
        {
        }

        return getStatus();
    }

    public synchronized Map<String, Object> stopManager()
    {
        starting = false;
        if (processHandle != null && processHandle.isAlive())
        {
            LOG.info("Stopping Neodymium Aura Test Manager process handle...");
            processHandle.destroy();
            try
            {
                processHandle.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            }
            catch (final InterruptedException ignored)
            {
            }
            if (processHandle.isAlive())
            {
                processHandle.destroyForcibly();
            }
            processHandle = null;
        }

        if (isPortListening(TARGET_PORT))
        {
            try
            {
                LOG.info("Port {} is still listening. Executing fuser kill for port {}...", TARGET_PORT, TARGET_PORT);
                new ProcessBuilder("fuser", "-k", TARGET_PORT + "/tcp").start().waitFor();
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to execute fuser kill on port {}: {}", TARGET_PORT, e.getMessage());
            }
        }

        return getStatus();
    }

    private boolean isPortListening(final int port)
    {
        try (final Socket socket = new Socket())
        {
            socket.connect(new InetSocketAddress("localhost", port), 800);
            return true;
        }
        catch (final IOException e)
        {
            return false;
        }
    }
}
