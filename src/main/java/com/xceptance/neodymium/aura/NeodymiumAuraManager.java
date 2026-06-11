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

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Neodymium Aura Manager: A lightweight standalone web server to browse,
 * create, edit, queue, run, and view reports of Neodymium YAML test data files.
 * 
 * // AI-generated: Gemini 2.5 Pro
 */
public final class NeodymiumAuraManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NeodymiumAuraManager.class);
    private static final Pattern STATS_PATTERN = Pattern.compile("Tests run:\\s*(\\d+),\\s*Failures:\\s*(\\d+),\\s*Errors:\\s*(\\d+)");
    private static final Gson gson = new Gson();
    
    private static final List<HttpExchange> sseClients = new CopyOnWriteArrayList<>();
    private static final AtomicReference<Process> activeProcess = new AtomicReference<>(null);
    private static final AtomicInteger globalTestsRun = new AtomicInteger(0);
    private static final AtomicInteger globalPassed = new AtomicInteger(0);
    private static final AtomicInteger globalFailed = new AtomicInteger(0);
    private static final AtomicBoolean runningQueue = new AtomicBoolean(false);
    private static final AtomicReference<String> activeFile = new AtomicReference<>("");

    private static final ScheduledExecutorService shutdownScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final Thread t = new Thread(runnable, "Aura-Shutdown-Scheduler");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private static ScheduledFuture<?> pendingShutdown = null;

    private NeodymiumAuraManager()
    {
    }

    public static void main(final String[] args)
    {
        try
        {
            final HttpServer server = startServer(18080);
            final int port = server.getAddress().getPort();
            
            final String serverUrl = "http://localhost:" + port;
            LOGGER.info("========================================================================");
            LOGGER.info("  Neodymium Aura: Standalone Manager Running Successfully!");
            LOGGER.info("========================================================================");
            LOGGER.info("  Dashboard URL: {}", serverUrl);
            LOGGER.info("========================================================================");
            
            openBrowser(serverUrl);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to start Neodymium Aura Manager", e);
            System.exit(1);
        }
    }

    public static HttpServer startServer(final int startPort) throws IOException
    {
        validateEnvironment();
        
        int port = startPort;
        HttpServer server = null;
        while (port < startPort + 100)
        {
            try
            {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
                break;
            }
            catch (final IOException e)
            {
                port++;
            }
        }
        
        if (server == null)
        {
            throw new IOException("Could not find any free port starting from " + startPort);
        }
        
        server.createContext("/", new MainHandler());
        server.setExecutor(Executors.newCachedThreadPool((final Runnable runnable) -> {
            final Thread t = new Thread(runnable);
            t.setDaemon(true);
            return t;
        }));
        server.start();
        return server;
    }

    public static void stopServer(final HttpServer server)
    {
        if (server != null)
        {
            server.stop(0);
        }
    }

    private static void validateEnvironment()
    {
        final String javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null || javaHome.trim().isEmpty() || !new File(javaHome).isDirectory())
        {
            LOGGER.error("========================================================================");
            LOGGER.error("  ERROR: JAVA_HOME environment variable is not set or invalid!");
            LOGGER.error("  Please set JAVA_HOME to a valid JDK installation directory.");
            LOGGER.error("========================================================================");
            throw new IllegalStateException("JAVA_HOME environment variable is not set or invalid");
        }
        
        try
        {
            final String os = System.getProperty("os.name").toLowerCase();
            final String[] checkCmd = os.contains("win") ? new String[]{"cmd.exe", "/c", "mvn", "-v"} : new String[]{"mvn", "-v"};
            final ProcessBuilder pb = new ProcessBuilder(checkCmd);
            final Process p = pb.start();
            p.waitFor();
        }
        catch (final Exception e)
        {
            LOGGER.error("========================================================================");
            LOGGER.error("  ERROR: Maven command 'mvn' is not available in system PATH!");
            LOGGER.error("  Please ensure Maven is installed and 'mvn' is executable.");
            LOGGER.error("========================================================================");
            throw new IllegalStateException("Maven command 'mvn' is not available", e);
        }
    }

    private static void openBrowser(final String url)
    {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
        {
            try
            {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
            catch (final Exception e)
            {
                // ignore and fall back
            }
        }
        
        try
        {
            final String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win"))
            {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            }
            else if (os.contains("mac"))
            {
                Runtime.getRuntime().exec("open " + url);
            }
            else
            {
                Runtime.getRuntime().exec("xdg-open " + url);
            }
        }
        catch (final Exception e)
        {
            // ignore
        }
    }

    private static final class MainHandler implements HttpHandler
    {
        @Override
        public void handle(final HttpExchange exchange) throws IOException
        {
            final String path = exchange.getRequestURI().getPath();
            final String method = exchange.getRequestMethod();

            LOGGER.info("[Aura Server] {} {} - Request received", method, path);

            // Enable CORS
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(method))
            {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try
            {
                if ("/".equals(path))
                {
                    handleDashboard(exchange);
                }
                else if ("/api/files".equals(path) && "GET".equalsIgnoreCase(method))
                {
                    handleListFiles(exchange);
                }
                else if ("/api/read".equals(path) && "GET".equalsIgnoreCase(method))
                {
                    handleReadFile(exchange);
                }
                else if ("/api/save".equals(path) && "POST".equalsIgnoreCase(method))
                {
                    handleSaveFile(exchange);
                }
                else if ("/api/delete".equals(path) && "POST".equalsIgnoreCase(method))
                {
                    handleDeleteFile(exchange);
                }
                else if ("/api/create".equals(path) && "POST".equalsIgnoreCase(method))
                {
                    handleCreateFile(exchange);
                }
                else if ("/api/run".equals(path) && "POST".equalsIgnoreCase(method))
                {
                    handleRunQueue(exchange);
                }
                else if ("/api/status".equals(path) && "GET".equalsIgnoreCase(method))
                {
                    handleStatusStream(exchange);
                }
                else if ("/api/allure/history".equals(path) && "GET".equalsIgnoreCase(method))
                {
                    handleAllureHistory(exchange);
                }
                else if (path.startsWith("/api/allure/report/"))
                {
                    handleServeReportFile(exchange, path);
                }
                else if ("/api/allure/generate".equals(path) && "POST".equalsIgnoreCase(method))
                {
                    handleGenerateAllure(exchange);
                }
                else if ("/api/allure/delete".equals(path) && "POST".equalsIgnoreCase(method))
                {
                    handleDeleteReport(exchange);
                }
                else if ("/api/stop".equals(path) && "POST".equalsIgnoreCase(method))
                {
                    handleStopProcess(exchange);
                }
                else
                {
                    sendError(exchange, 404, "Endpoint not found");
                }
            }
            catch (final Exception e)
            {
                sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        }

        private void handleDashboard(final HttpExchange exchange) throws IOException
        {
            final InputStream is = NeodymiumAuraManager.class.getClassLoader()
                .getResourceAsStream("com/xceptance/neodymium/aura/neodymium-aura-dashboard.html");
            if (is == null)
            {
                sendError(exchange, 404, "neodymium-aura-dashboard.html resource not found on classpath.");
                return;
            }
            final byte[] bytes = is.readAllBytes();
            sendResponse(exchange, 200, "text/html; charset=UTF-8", bytes);
        }

        private void handleListFiles(final HttpExchange exchange) throws IOException
        {
            final List<String> yamlFiles = new ArrayList<>();
            final File resourcesDir = new File("src/test/resources").getAbsoluteFile();
            LOGGER.info("[Aura Server] Scanning directory: {}", resourcesDir.getAbsolutePath());
            if (resourcesDir.exists() && resourcesDir.isDirectory())
            {
                scanDir(resourcesDir, resourcesDir, yamlFiles);
                yamlFiles.sort(String::compareTo);
                LOGGER.info("[Aura Server] Scan completed. Found {} YAML file(s): {}", yamlFiles.size(), yamlFiles);
            }
            else
            {
                LOGGER.error("[Aura Server] Directory src/test/resources does not exist or is not a directory: {}", resourcesDir.getAbsolutePath());
            }
            sendJsonResponse(exchange, 200, gson.toJson(yamlFiles));
        }

        private void scanDir(final File baseDir, final File currentDir, final List<String> yamlFiles)
        {
            final File[] files = currentDir.listFiles();
            if (files != null)
            {
                for (final File file : files)
                {
                    if (file.isDirectory())
                    {
                        scanDir(baseDir, file, yamlFiles);
                    }
                    else
                    {
                        final String name = file.getName().toLowerCase();
                        if (name.endsWith(".yaml") || name.endsWith(".yml"))
                        {
                            final String relativePath = baseDir.toURI().relativize(file.toURI()).getPath();
                            yamlFiles.add(relativePath);
                        }
                    }
                }
            }
        }

        private void handleReadFile(final HttpExchange exchange) throws IOException
        {
            final String query = exchange.getRequestURI().getQuery();
            String filename = null;
            if (query != null)
            {
                for (final String param : query.split("&"))
                {
                    final String[] pair = param.split("=");
                    if (pair.length > 1 && "file".equals(pair[0]))
                    {
                        filename = pair[1];
                        break;
                    }
                }
            }

            if (filename == null || filename.trim().isEmpty())
            {
                LOGGER.error("[Aura Server] Read file request failed: Missing 'file' parameter");
                sendError(exchange, 400, "Missing 'file' parameter");
                return;
            }

            final File resourcesDir = new File("src/test/resources").getCanonicalFile();
            final File file = new File(resourcesDir, filename).getCanonicalFile();
            if (!file.getPath().startsWith(resourcesDir.getPath()))
            {
                LOGGER.error("[Aura Server] Directory traversal attempt detected: {}", filename);
                sendError(exchange, 403, "Access denied: Directory traversal detected");
                return;
            }

            if (!file.exists() || !file.isFile())
            {
                LOGGER.error("[Aura Server] File not found: {}", file.getAbsolutePath());
                sendError(exchange, 404, "File not found");
                return;
            }

            LOGGER.info("[Aura Server] Reading file content: {}", filename);
            final String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            final Map<String, String> response = new HashMap<>();
            response.put("content", content);
            sendJsonResponse(exchange, 200, gson.toJson(response));
        }

        private void handleSaveFile(final HttpExchange exchange) throws IOException
        {
            final String body = readBody(exchange);
            final SaveRequest req = gson.fromJson(body, SaveRequest.class);
            if (req == null || req.file == null || req.content == null)
            {
                LOGGER.error("[Aura Server] Save file request failed: Missing 'file' or 'content' in body");
                sendError(exchange, 400, "Missing 'file' or 'content' in body");
                return;
            }

            final File resourcesDir = new File("src/test/resources").getCanonicalFile();
            final File file = new File(resourcesDir, req.file).getCanonicalFile();
            if (!file.getPath().startsWith(resourcesDir.getPath()))
            {
                LOGGER.error("[Aura Server] Directory traversal attempt detected: {}", req.file);
                sendError(exchange, 403, "Access denied: Directory traversal detected");
                return;
            }

            LOGGER.info("[Aura Server] Saving file: {}", req.file);
            Files.writeString(file.toPath(), req.content, StandardCharsets.UTF_8);
            sendJsonResponse(exchange, 200, gson.toJson(Map.of("success", true)));
        }

        private void handleDeleteFile(final HttpExchange exchange) throws IOException
        {
            final String body = readBody(exchange);
            final DeleteRequest req = gson.fromJson(body, DeleteRequest.class);
            if (req == null || req.file == null)
            {
                LOGGER.error("[Aura Server] Delete file request failed: Missing 'file' in body");
                sendError(exchange, 400, "Missing 'file' in body");
                return;
            }

            final File resourcesDir = new File("src/test/resources").getCanonicalFile();
            final File file = new File(resourcesDir, req.file).getCanonicalFile();
            if (!file.getPath().startsWith(resourcesDir.getPath()))
            {
                LOGGER.error("[Aura Server] Directory traversal attempt detected: {}", req.file);
                sendError(exchange, 403, "Access denied: Directory traversal detected");
                return;
            }

            if (file.exists() && file.isFile())
            {
                LOGGER.info("[Aura Server] Deleting file: {}", req.file);
                if (file.delete())
                {
                    sendJsonResponse(exchange, 200, gson.toJson(Map.of("success", true)));
                }
                else
                {
                    LOGGER.error("[Aura Server] Failed to delete file from disk: {}", file.getAbsolutePath());
                    sendError(exchange, 500, "Failed to delete file");
                }
            }
            else
            {
                LOGGER.error("[Aura Server] File not found for deletion: {}", file.getAbsolutePath());
                sendError(exchange, 404, "File not found");
            }
        }

        private void handleCreateFile(final HttpExchange exchange) throws IOException
        {
            final String body = readBody(exchange);
            final CreateRequest req = gson.fromJson(body, CreateRequest.class);
            if (req == null || req.name == null || req.name.trim().isEmpty())
            {
                LOGGER.error("[Aura Server] Create file request failed: Missing 'name' in body");
                sendError(exchange, 400, "Missing 'name' in body");
                return;
            }

            final String kebab = req.name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
            final String filename = kebab + ".yaml";

            final File resourcesDir = new File("src/test/resources").getAbsoluteFile();
            final File file = new File(resourcesDir, filename).getAbsoluteFile();
            if (file.exists())
            {
                LOGGER.error("[Aura Server] Create file failed: File already exists: {}", filename);
                sendError(exchange, 400, "File already exists: " + filename);
                return;
            }

            LOGGER.info("[Aura Server] Creating new test file: {} (testName: \"{}\")", filename, req.name.trim());
            final String boilerplate = "# Neodymium YAML Test Data File\n" +
                                       "steps: |\n" +
                                       "  Open browser\n" +
                                       "data:\n" +
                                       "  - testId: \"" + req.name.trim() + "\"\n";
            Files.writeString(file.toPath(), boilerplate, StandardCharsets.UTF_8);
            sendJsonResponse(exchange, 200, gson.toJson(Map.of("success", true, "file", filename)));
        }

        private void handleRunQueue(final HttpExchange exchange) throws IOException
        {
            final String body = readBody(exchange);
            final RunRequest req = gson.fromJson(body, RunRequest.class);
            if (req == null || req.files == null || req.files.isEmpty())
            {
                LOGGER.error("[Aura Server] Run queue request failed: Missing 'files' in body");
                sendError(exchange, 400, "Missing 'files' in body");
                return;
            }

            if (runningQueue.get())
            {
                LOGGER.error("[Aura Server] Run queue request failed: A queue is already executing");
                sendError(exchange, 409, "A queue is already executing");
                return;
            }

            LOGGER.info("[Aura Server] Spawning test run queue for {} file(s) (headless={}, interactive={})", req.files.size(), req.headless, req.interactive);
            executeQueue(req);
            sendJsonResponse(exchange, 200, gson.toJson(Map.of("success", true)));
        }

        private void handleStatusStream(final HttpExchange exchange) throws IOException
        {
            LOGGER.info("[Aura Server] SSE client connected: {}", exchange.getRemoteAddress());
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            exchange.sendResponseHeaders(200, 0);

            synchronized (sseClients)
            {
                if (pendingShutdown != null)
                {
                    pendingShutdown.cancel(false);
                    pendingShutdown = null;
                    LOGGER.info("[Aura Server] New client connected. Cancelled pending server shutdown.");
                }
                sseClients.add(exchange);
            }
            
            // Broadcast initial status to this client
            final String initialStatus = String.format(
                "{\"type\":\"status\",\"total\":%d,\"passed\":%d,\"failed\":%d,\"running\":%b,\"activeFile\":\"%s\"}",
                globalTestsRun.get(), globalPassed.get(), globalFailed.get(), runningQueue.get(), activeFile.get()
            );
            try
            {
                synchronized (exchange)
                {
                    exchange.getResponseBody().write(("data: " + initialStatus + "\n\n").getBytes(StandardCharsets.UTF_8));
                    exchange.getResponseBody().flush();
                }
            }
            catch (final IOException e)
            {
                synchronized (sseClients)
                {
                    sseClients.remove(exchange);
                }
                LOGGER.info("[Aura Server] SSE client disconnected before initiation: {}", exchange.getRemoteAddress());
                exchange.close();
                checkShutdownOnDisconnect();
                return;
            }

            try
            {
                while (sseClients.contains(exchange))
                {
                    Thread.sleep(15000);
                    try
                    {
                        synchronized (exchange)
                        {
                            exchange.getResponseBody().write(":\n\n".getBytes(StandardCharsets.UTF_8));
                            exchange.getResponseBody().flush();
                        }
                    }
                    catch (final IOException e)
                    {
                        break;
                    }
                }
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            finally
            {
                synchronized (sseClients)
                {
                    sseClients.remove(exchange);
                }
                LOGGER.info("[Aura Server] SSE client disconnected: {}", exchange.getRemoteAddress());
                try
                {
                    exchange.close();
                }
                catch (final Exception e)
                {
                    // ignore
                }
                checkShutdownOnDisconnect();
            }
        }


        private void handleAllureHistory(final HttpExchange exchange) throws IOException
        {
            final File historyDir = new File("allure-reports-history").getAbsoluteFile();
            final List<Map<String, String>> historyList = new ArrayList<>();

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
                        final File metadataFile = new File(dir, "metadata.json");
                        if (metadataFile.exists() && metadataFile.isFile())
                        {
                            try
                            {
                                final String meta = Files.readString(metadataFile.toPath(), StandardCharsets.UTF_8);
                                final Map<?, ?> map = gson.fromJson(meta, Map.class);
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
                                }
                            }
                            catch (final Exception e)
                            {
                                // ignore
                            }
                        }

                        final Map<String, String> item = new HashMap<>();
                        item.put("id", dir.getName());
                        item.put("status", status);
                        item.put("timestamp", timestamp);
                        historyList.add(item);
                    }
                }
            }

            sendJsonResponse(exchange, 200, gson.toJson(historyList));
        }

        private void handleServeReportFile(final HttpExchange exchange, final String path) throws IOException
        {
            final String prefix = "/api/allure/report/";
            final String subPath = path.substring(prefix.length());
            final File historyDir = new File("allure-reports-history").getCanonicalFile();
            final File file = new File(historyDir, subPath).getCanonicalFile();

            if (!file.getPath().startsWith(historyDir.getPath()))
            {
                sendError(exchange, 403, "Access denied: Directory traversal detected");
                return;
            }

            if (!file.exists() || !file.isFile())
            {
                sendError(exchange, 404, "File not found");
                return;
            }

            final byte[] bytes = Files.readAllBytes(file.toPath());
            final String contentType = getMimeType(file.getName());
            sendResponse(exchange, 200, contentType, bytes);
        }

        private void handleGenerateAllure(final HttpExchange exchange) throws IOException
        {
            if (runningQueue.get())
            {
                LOGGER.error("[Aura Server] Allure generation request failed: Queue is currently executing");
                sendError(exchange, 409, "Cannot compile Allure report while queue is executing");
                return;
            }

            LOGGER.info("[Aura Server] Spawning thread to compile Allure report...");
            final Thread thread = new Thread(() -> generateAllureReport(List.of()));
            thread.setName("NeodymiumAuraManualAllureCompiler");
            thread.start();

            sendJsonResponse(exchange, 200, gson.toJson(Map.of("success", true)));
        }

        private void handleDeleteReport(final HttpExchange exchange) throws IOException
        {
            final String body = readBody(exchange);
            final DeleteReportRequest req = gson.fromJson(body, DeleteReportRequest.class);
            if (req == null || req.id == null)
            {
                LOGGER.error("[Aura Server] Delete report request failed: Missing 'id' in body");
                sendError(exchange, 400, "Missing 'id' in body");
                return;
            }

            LOGGER.info("[Aura Server] POST /api/allure/delete - Request received for ID: {}", req.id);

            final File historyDir = new File("allure-reports-history").getCanonicalFile();
            final File reportDir = new File(historyDir, req.id).getCanonicalFile();

            if (!reportDir.getPath().startsWith(historyDir.getPath()))
            {
                LOGGER.error("[Aura Server] Directory traversal attempt detected: {}", req.id);
                sendError(exchange, 403, "Access denied: Directory traversal detected");
                return;
            }

            if (reportDir.exists() && reportDir.isDirectory())
            {
                deleteDirRecursively(reportDir);
                LOGGER.info("[Aura Server] Deleted report history directory: {}", reportDir.getAbsolutePath());
                sendJsonResponse(exchange, 200, gson.toJson(Map.of("success", true)));
            }
            else
            {
                LOGGER.error("[Aura Server] Report directory not found: {}", reportDir.getAbsolutePath());
                sendError(exchange, 404, "Report directory not found");
            }
        }

        private void deleteDirRecursively(final File file)
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

        private void handleStopProcess(final HttpExchange exchange) throws IOException
        {
            LOGGER.info("[Aura Server] User requested to stop active execution subprocess");
            final Process p = activeProcess.get();
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
            sendJsonResponse(exchange, 200, gson.toJson(Map.of("success", true)));
        }

        private String getMimeType(final String filename)
        {
            final String lower = filename.toLowerCase();
            if (lower.endsWith(".html"))
            {
                return "text/html; charset=UTF-8";
            }
            if (lower.endsWith(".css"))
            {
                return "text/css";
            }
            if (lower.endsWith(".js"))
            {
                return "application/javascript";
            }
            if (lower.endsWith(".png"))
            {
                return "image/png";
            }
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
            {
                return "image/jpeg";
            }
            if (lower.endsWith(".svg"))
            {
                return "image/svg+xml";
            }
            if (lower.endsWith(".json"))
            {
                return "application/json";
            }
            if (lower.endsWith(".woff"))
            {
                return "font/woff";
            }
            if (lower.endsWith(".woff2"))
            {
                return "font/woff2";
            }
            if (lower.endsWith(".ttf"))
            {
                return "font/ttf";
            }
            return "application/octet-stream";
        }

        private String readBody(final HttpExchange exchange) throws IOException
        {
            try (final InputStream is = exchange.getRequestBody();
                 final BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
            {
                final StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null)
                {
                    sb.append(line);
                }
                return sb.toString();
            }
        }

        private void sendResponse(final HttpExchange exchange, final int status, final String contentType, final byte[] bytes) throws IOException
        {
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            try (final OutputStream os = exchange.getResponseBody())
            {
                os.write(bytes);
            }
        }

        private void sendJsonResponse(final HttpExchange exchange, final int status, final String json) throws IOException
        {
            sendResponse(exchange, status, "application/json; charset=UTF-8", json.getBytes(StandardCharsets.UTF_8));
        }

        private void sendError(final HttpExchange exchange, final int status, final String message) throws IOException
        {
            final String json = gson.toJson(Map.of("error", message));
            sendJsonResponse(exchange, status, json);
        }
    }

    private static void executeQueue(final RunRequest req)
    {
        if (!runningQueue.compareAndSet(false, true))
        {
            return;
        }

        final Thread thread = new Thread(() -> {
            final File tempRunnerDir = new File("src/test/java/com/xceptance/neodymium/aura");
            final File tempRunnerFile = new File(tempRunnerDir, "AuraYamlRunnerTest.java");
            boolean createdTempFile = false;
            try
            {
                if (!tempRunnerFile.exists())
                {
                    tempRunnerDir.mkdirs();
                    final String runnerSource = 
                        "package com.xceptance.neodymium.aura;\n\n" +
                        "import com.xceptance.neodymium.common.browser.Browser;\n" +
                        "import com.xceptance.neodymium.common.testdata.DataFolder;\n" +
                        "import com.xceptance.neodymium.junit5.NeodymiumTest;\n" +
                        "import com.xceptance.neodymium.util.Neodymium;\n\n" +
                        "@Browser()\n" +
                        "@DataFolder(\".\")\n" +
                        "public final class AuraYamlRunnerTest\n" +
                        "{\n" +
                        "    @NeodymiumTest\n" +
                        "    public final void executeYamlTest() throws Throwable\n" +
                        "    {\n" +
                        "        Neodymium.ai().execute();\n" +
                        "    }\n" +
                        "}\n";
                    Files.writeString(tempRunnerFile.toPath(), runnerSource, StandardCharsets.UTF_8);
                    createdTempFile = true;
                    LOGGER.info("[Aura Server] Created temporary test runner: {}", tempRunnerFile.getAbsolutePath());
                }

                LOGGER.info("[Aura Server] Starting execution of {} test(s) in queue.", req.files.size());
                globalTestsRun.set(0);
                globalPassed.set(0);
                globalFailed.set(0);

                for (int i = 0; i < req.files.size(); i++)
                {
                    final String file = req.files.get(i);
                    activeFile.set(file);
                    broadcastStatus(true);

                    broadcastLog("\n[INFO] Spawning Maven Subprocess for YAML test: " + file + "...");

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
                    if (i == 0)
                    {
                        command.add("clean");
                    }
                    command.add("test");
                    command.add("-Dtest=com.xceptance.neodymium.aura.AuraYamlRunnerTest");
                    command.add("-Dneodymium.testFileFilter=" + file.replace(".", "\\."));
                    command.add("-Dbrowserprofile.Default.headless=" + req.headless);
                    command.add("-Dselenide.headless=" + req.headless);
                    command.add("-Dneodymium.ai.interactive=" + req.interactive);

                    LOGGER.info("[Aura Server] Executing command: {}", String.join(" ", command));
                    broadcastLog("[INFO] Command: " + String.join(" ", command));
                    broadcastLog("[INFO] ------------------------------------------------------------------------");

                    final ProcessBuilder pb = new ProcessBuilder(command);
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
                        continue;
                    }

                    final AtomicInteger fileTestsRun = new AtomicInteger(0);
                    final AtomicInteger fileFailures = new AtomicInteger(0);
                    final AtomicInteger fileErrors = new AtomicInteger(0);

                    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)))
                    {
                        String line;
                        while ((line = reader.readLine()) != null)
                        {
                            broadcastLog(line);

                            final Matcher m = STATS_PATTERN.matcher(line);
                            if (m.find())
                            {
                                fileTestsRun.set(Integer.parseInt(m.group(1)));
                                fileFailures.set(Integer.parseInt(m.group(2)));
                                fileErrors.set(Integer.parseInt(m.group(3)));
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

                    if (fileTestsRun.get() > 0)
                    {
                        globalTestsRun.addAndGet(fileTestsRun.get());
                        final int failures = fileFailures.get() + fileErrors.get();
                        globalFailed.addAndGet(failures);
                        globalPassed.addAndGet(fileTestsRun.get() - failures);
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

                    activeProcess.set(null);
                }

                if (req.allure)
                {
                    LOGGER.info("[Aura Server] Auto-generating Allure report as requested.");
                    generateAllureReport(req.files);
                }

                LOGGER.info("[Aura Server] Queue execution completed. Total: {}, Passed: {}, Failed: {}", globalTestsRun.get(), globalPassed.get(), globalFailed.get());
                broadcastLog("\n[INFO] Queue execution completed.");
            }
            catch (final Exception e)
            {
                LOGGER.error("[Aura Server] Exception during queue execution", e);
                broadcastLog("[ERROR] Queue execution failed: " + e.getMessage());
            }
            finally
            {
                if (createdTempFile && tempRunnerFile.exists())
                {
                    tempRunnerFile.delete();
                    LOGGER.info("[Aura Server] Deleted temporary test runner: {}", tempRunnerFile.getAbsolutePath());
                    File parent = tempRunnerDir;
                    while (parent != null && parent.getPath().startsWith("src/test/java"))
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
                }
                runningQueue.set(false);
                activeFile.set("");
                broadcastStatus(false);
            }
        });
        thread.setName("NeodymiumAuraQueueExecutor");
        thread.start();
    }

    private static void generateAllureReport(final List<String> files)
    {
        broadcastLog("\n[INFO] Compiling Allure report...");
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

        final ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try
        {
            LOGGER.info("[Aura Server] Executing Allure command: {}", String.join(" ", command));
            final Process p = pb.start();
            activeProcess.set(p);

            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    broadcastLog(line);
                }
            }

            final int exitCode = p.waitFor();
            activeProcess.set(null);

            if (exitCode == 0)
            {
                LOGGER.info("[Aura Server] Allure report successfully compiled.");
                broadcastLog("[INFO] Allure report successfully compiled.");
                copyAllureReportToHistory(files);
            }
            else
            {
                LOGGER.error("[Aura Server] Allure report compilation failed with exit code: {}", exitCode);
                broadcastLog("[ERROR] Allure report compilation failed with exit code: " + exitCode);
            }
        }
        catch (final Exception e)
        {
            LOGGER.error("[Aura Server] Failed to compile Allure report", e);
            broadcastLog("[ERROR] Failed to compile Allure report: " + e.getMessage());
            activeProcess.set(null);
        }
    }

    private static void copyAllureReportToHistory(final List<String> files)
    {
        final File srcDir = new File("target/site/allure-maven-plugin");
        if (!srcDir.exists() || !srcDir.isDirectory())
        {
            LOGGER.error("[Aura Server] Allure report source directory not found: {}", srcDir.getAbsolutePath());
            broadcastLog("[WARN] Allure report source directory not found: " + srcDir.getAbsolutePath());
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
        final File destDir = new File("allure-reports-history", folderName).getAbsoluteFile();

        try
        {
            copyDirectory(srcDir, destDir);

            // Write metadata.json
            final File metadataFile = new File(destDir, "metadata.json");
            final String status = globalFailed.get() == 0 ? "Passed" : "Failed";
            final String metadataContent = String.format("{\n  \"status\": \"%s\",\n  \"timestamp\": \"%s\"\n}", status, timestamp);
            Files.writeString(metadataFile.toPath(), metadataContent, StandardCharsets.UTF_8);

            LOGGER.info("[Aura Server] Allure report copied to history: {}", destDir.getName());
            broadcastLog("[INFO] Allure report copied to history: " + destDir.getName());
        }
        catch (final IOException e)
        {
            LOGGER.error("[Aura Server] Failed to copy Allure report to history", e);
            broadcastLog("[ERROR] Failed to copy Allure report to history: " + e.getMessage());
        }
    }

    private static void copyDirectory(final File src, final File dest) throws IOException
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

    public static String stripAnsi(final String line)
    {
        if (line == null)
        {
            return null;
        }
        // Strip standard ANSI escape codes
        String clean = line.replaceAll("(?i)\\u001B\\[[;0-9]*[a-zA-Z]", "");
        // Clean up leftover patterns like [1;34m if generated without ESC char
        clean = clean.replaceAll("\\[[0-9]+(;[0-9]+)*[a-zA-Z]", "");
        // Clean up leftover reset codes like [m (but not [INFO], [WARNING] or other words)
        clean = clean.replaceAll("(?i)\\[[a-zA-Z](?![a-zA-Z0-9])", "");
        return clean;
    }

    private static void broadcastLog(final String line)
    {
        final String cleanLine = stripAnsi(line);
        final Map<String, Object> event = new HashMap<>();
        event.put("type", "log");
        event.put("line", cleanLine);
        broadcast(gson.toJson(event));
    }

    private static void broadcastStatus(final boolean running)
    {
        final Map<String, Object> event = new HashMap<>();
        event.put("type", "status");
        event.put("total", globalTestsRun.get());
        event.put("passed", globalPassed.get());
        event.put("failed", globalFailed.get());
        event.put("running", running);
        event.put("activeFile", activeFile.get());
        broadcast(gson.toJson(event));
    }

    private static void checkShutdownOnDisconnect()
    {
        synchronized (sseClients)
        {
            if (sseClients.isEmpty())
            {
                if (pendingShutdown == null || pendingShutdown.isDone())
                {
                    LOGGER.info("[Aura Server] No clients connected. Scheduling server shutdown in 5 seconds...");
                    pendingShutdown = shutdownScheduler.schedule(() -> {
                        synchronized (sseClients)
                        {
                            if (sseClients.isEmpty())
                            {
                                LOGGER.info("[Aura Server] No clients connected for 5 seconds. Shutting down...");
                                
                                // Stop any active subprocesses
                                final Process p = activeProcess.get();
                                if (p != null)
                                {
                                    p.destroy();
                                }
                                
                                System.exit(0);
                            }
                        }
                    }, 5, TimeUnit.SECONDS);
                }
            }
        }
    }

    private static void broadcast(final String eventJson)
    {
        final byte[] payload = ("data: " + eventJson + "\n\n").getBytes(StandardCharsets.UTF_8);
        for (final HttpExchange exchange : sseClients)
        {
            try
            {
                synchronized (exchange)
                {
                    exchange.getResponseBody().write(payload);
                    exchange.getResponseBody().flush();
                }
            }
            catch (final IOException e)
            {
                synchronized (sseClients)
                {
                    sseClients.remove(exchange);
                }
                try
                {
                    exchange.close();
                }
                catch (final Exception ex)
                {
                    // ignore
                }
                checkShutdownOnDisconnect();
            }
        }
    }

    private static final class SaveRequest
    {
        String file;
        String content;
    }

    private static final class CreateRequest
    {
        String name;
    }

    private static final class DeleteRequest
    {
        String file;
    }

    private static final class DeleteReportRequest
    {
        String id;
    }

    private static final class RunRequest
    {
        List<String> files;
        boolean headless;
        boolean interactive;
        boolean allure;
    }
}
