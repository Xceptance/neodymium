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
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import com.xceptance.neodymium.ai.core.LlmClient;
import com.xceptance.neodymium.ai.core.AiStats;
import com.xceptance.neodymium.util.Neodymium;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import com.xceptance.neodymium.ai.action.ActionRegistry;

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
    
    private static final Map<String, Long> activeClients = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<String> currentRunLogs = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Map<String, Object>> currentRunEvents = new CopyOnWriteArrayList<>();
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
                server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
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
        LOGGER.info("[Aura Server] HTTP server started on http://localhost:{}", port);

        // Also start HTTPS secure server on the next port if keystore.p12 is available
        try
        {
            final KeyStore ks = KeyStore.getInstance("PKCS12");
            try (final InputStream ksf = Thread.currentThread().getContextClassLoader().getResourceAsStream("keystore.p12"))
            {
                if (ksf != null)
                {
                    ks.load(ksf, "changeit".toCharArray());
                    final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                    kmf.init(ks, "changeit".toCharArray());
                    final SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(kmf.getKeyManagers(), null, null);

                    int httpsPort = port + 1;
                    HttpsServer httpsServer = null;
                    while (httpsPort < port + 100)
                    {
                        try
                        {
                            httpsServer = HttpsServer.create(new InetSocketAddress("0.0.0.0", httpsPort), 0);
                            break;
                        }
                        catch (final IOException e)
                        {
                            httpsPort++;
                        }
                    }

                    if (httpsServer != null)
                    {
                        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
                        httpsServer.createContext("/", new MainHandler());
                        httpsServer.setExecutor(server.getExecutor());
                        httpsServer.start();
                        LOGGER.info("[Aura Server] HTTPS secure server started on https://localhost:{}", httpsPort);
                    }
                }
            }
        }
        catch (final Exception e)
        {
            LOGGER.warn("[Aura Server] Failed to initialize secure HttpsServer: {}", e.getMessage());
        }

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
                else if ("/api/chat".equals(path) && "POST".equalsIgnoreCase(method))
                {
                    handleChat(exchange);
                }
                else if ("/api/disconnect".equals(path) && "POST".equalsIgnoreCase(method))
                {
                    handleDisconnect(exchange);
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

            final List<YamlFileDto> responseList = new ArrayList<>();
            for (final String file : yamlFiles)
            {
                final File yamlFile = new File(resourcesDir, file);
                final Map<String, Object> details = getFileDetails(yamlFile);
                final List<DatasetDto> datasets = (List<DatasetDto>) details.get("datasets");
                responseList.add(new YamlFileDto(file, datasets != null ? datasets : new ArrayList<>()));
            }

            sendJsonResponse(exchange, 200, gson.toJson(responseList));
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
            if (req == null || req.datasets == null || req.datasets.isEmpty())
            {
                LOGGER.error("[Aura Server] Run queue request failed: Missing 'datasets' in body");
                sendError(exchange, 400, "Missing 'datasets' in body");
                return;
            }

            if (runningQueue.get())
            {
                LOGGER.error("[Aura Server] Run queue request failed: A queue is already executing");
                sendError(exchange, 409, "A queue is already executing");
                return;
            }

            LOGGER.info("[Aura Server] Spawning test run queue for {} dataset(s) (headless={}, interactive={})", req.datasets.size(), req.headless, req.interactive);
            executeQueue(req);
            sendJsonResponse(exchange, 200, gson.toJson(Map.of("success", true)));
        }

        private void handleStatusStream(final HttpExchange exchange) throws IOException
        {
            final String query = exchange.getRequestURI().getQuery();
            String clientId = null;
            int lastIndex = 0;
            int lastEventIndex = 0;
            if (query != null)
            {
                for (final String param : query.split("&"))
                {
                    final String[] pair = param.split("=");
                    if (pair.length > 1)
                    {
                        if ("clientId".equals(pair[0]))
                        {
                            clientId = pair[1];
                        }
                        else if ("lastIndex".equals(pair[0]))
                        {
                            try
                            {
                                lastIndex = Integer.parseInt(pair[1]);
                            }
                            catch (NumberFormatException e)
                            {
                                // ignore
                            }
                        }
                        else if ("lastEventIndex".equals(pair[0]))
                        {
                            try
                            {
                                lastEventIndex = Integer.parseInt(pair[1]);
                            }
                            catch (NumberFormatException e)
                            {
                                // ignore
                            }
                        }
                    }
                }
            }
            if (clientId == null || clientId.trim().isEmpty())
            {
                clientId = "fallback-" + UUID.randomUUID().toString();
            }

            activeClients.put(clientId, System.currentTimeMillis());

            if (pendingShutdown != null)
            {
                pendingShutdown.cancel(false);
                pendingShutdown = null;
                LOGGER.info("[Aura Server] Client polled. Cancelled pending server shutdown.");
            }

            final Map<String, Object> status = new HashMap<>();
            status.put("type", "status");
            status.put("total", globalTestsRun.get());
            status.put("passed", globalPassed.get());
            status.put("failed", globalFailed.get());
            status.put("running", runningQueue.get());
            status.put("activeFile", activeFile.get());

            final List<String> logs = new ArrayList<>();
            final int currentLogSize = currentRunLogs.size();
            for (int i = lastIndex; i < currentLogSize; i++)
            {
                logs.add(currentRunLogs.get(i));
            }

            final List<Map<String, Object>> events = new ArrayList<>();
            final int currentEventSize = currentRunEvents.size();
            for (int i = lastEventIndex; i < currentEventSize; i++)
            {
                events.add(currentRunEvents.get(i));
            }

            final Map<String, Object> response = new HashMap<>();
            response.put("status", status);
            response.put("logs", logs);
            response.put("newIndex", currentLogSize);
            response.put("events", events);
            response.put("newEventIndex", currentEventSize);

            sendJsonResponse(exchange, 200, gson.toJson(response));
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
                        String total = "-";
                        String passed = "-";
                        String failed = "-";
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
                                    if (map.containsKey("total"))
                                    {
                                        total = String.valueOf(Math.round(Double.parseDouble(String.valueOf(map.get("total")))));
                                    }
                                    if (map.containsKey("passed"))
                                    {
                                        passed = String.valueOf(Math.round(Double.parseDouble(String.valueOf(map.get("passed")))));
                                    }
                                    if (map.containsKey("failed"))
                                    {
                                        failed = String.valueOf(Math.round(Double.parseDouble(String.valueOf(map.get("failed")))));
                                    }
                                }
                            }
                            catch (final Exception e)
                            {
                                // ignore
                            }
                        }

                        final File indexHtml = new File(dir, "index.html");
                        final boolean hasReport = indexHtml.exists() && indexHtml.isFile();

                        final Map<String, String> item = new HashMap<>();
                        item.put("id", dir.getName());
                        item.put("status", status);
                        item.put("timestamp", timestamp);
                        item.put("total", total);
                        item.put("passed", passed);
                        item.put("failed", failed);
                        item.put("hasReport", String.valueOf(hasReport));
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

        private void handleDisconnect(final HttpExchange exchange) throws IOException
        {
            final String query = exchange.getRequestURI().getQuery();
            String clientId = null;
            if (query != null)
            {
                for (final String param : query.split("&"))
                {
                    final String[] pair = param.split("=");
                    if (pair.length > 1 && "clientId".equals(pair[0]))
                    {
                        clientId = pair[1];
                        break;
                    }
                }
            }

            LOGGER.info("[Aura Server] Disconnect request received for clientId: {}", clientId);

            if (clientId != null && !clientId.trim().isEmpty())
            {
                activeClients.remove(clientId);
                LOGGER.info("[Aura Server] Removed active client: {}", clientId);
            }

            sendJsonResponse(exchange, 200, gson.toJson(Map.of("success", true)));
            checkShutdownOnDisconnect();
        }

        private void handleChat(final HttpExchange exchange) throws IOException
        {
            final String body = readBody(exchange);
            final ChatRequest req = gson.fromJson(body, ChatRequest.class);
            if (req == null || req.prompt == null || req.prompt.trim().isEmpty())
            {
                LOGGER.error("[Aura Server] Chat request failed: Missing 'prompt' in body");
                sendError(exchange, 400, "Missing 'prompt' in body");
                return;
            }

            try
            {
                // Verify API key configuration
                final String apiKey = Neodymium.aiConfiguration().aiApiKey();
                if (apiKey == null || apiKey.trim().isEmpty())
                {
                    sendError(exchange, 400, "API Key is missing or invalid. Please configure 'neodymium.ai.apiKey' in properties or system environment.");
                    return;
                }

                final ChatResponse response = runChatWorkflow(req);
                sendJsonResponse(exchange, 200, gson.toJson(response));
            }
            catch (final AssertionError | Exception e)
            {
                LOGGER.error("[Aura Server] Chat execution failed", e);
                sendError(exchange, 500, "LLM Client Error: " + e.getMessage() + ". Please verify that your Gemini API key is configured and valid.");
            }
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
            try (final InputStream is = exchange.getRequestBody())
            {
                final byte[] bytes = is.readAllBytes();
                return new String(bytes, StandardCharsets.UTF_8);
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

                final Map<String, List<String>> datasetsByFile = new LinkedHashMap<>();
                for (final DatasetSelection selection : req.datasets)
                {
                    datasetsByFile.computeIfAbsent(selection.file, k -> new ArrayList<>()).add(selection.id);
                }

                LOGGER.info("[Aura Server] Starting execution of {} dataset(s) in {} file(s) in queue.", req.datasets.size(), datasetsByFile.size());
                globalTestsRun.set(0);
                globalPassed.set(0);
                globalFailed.set(0);
                currentRunLogs.clear();
                currentRunEvents.clear();

                final List<Map.Entry<String, List<String>>> entries = new ArrayList<>(datasetsByFile.entrySet());
                for (int i = 0; i < entries.size(); i++)
                {
                    final Map.Entry<String, List<String>> entry = entries.get(i);
                    final String file = entry.getKey();
                    final List<String> ids = entry.getValue();

                    activeFile.set(file);
                    broadcastStatus(true);

                    final StringBuilder idFilterBuilder = new StringBuilder();
                    idFilterBuilder.append("^(");
                    for (int j = 0; j < ids.size(); j++)
                    {
                        if (j > 0)
                        {
                            idFilterBuilder.append("|");
                        }
                        idFilterBuilder.append(Pattern.quote(ids.get(j)));
                    }
                    idFilterBuilder.append(")$");

                    broadcastLog("\n[INFO] Spawning Maven Subprocess for YAML test: " + file + " [Datasets: " + ids + "]...");

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
                    if (i == 0 && !"true".equals(System.getProperty("neodymium.aura.test")))
                    {
                        command.add("clean");
                    }
                    command.add("test");
                    command.add("-Dtest=com.xceptance.neodymium.aura.AuraYamlRunnerTest");
                    command.add("-Dneodymium.testFileFilter=" + file.replace(".", "\\."));
                    command.add("-Dneodymium.testIdFilter=" + idFilterBuilder.toString());
                    command.add("-Dbrowserprofile.Default.headless=" + req.headless);
                    command.add("-Dselenide.headless=" + req.headless);
                    command.add("-Dneodymium.ai.interactive=" + req.interactive);
                    command.add("-Dvideo.enableFilming=" + req.video);
                    command.add("-Dneodymium.webDriver.keepBrowserOpen=" + req.keepOpen);
                    command.add("-Dfile.encoding=UTF-8");
                    command.add("-Dsun.stdout.encoding=UTF-8");
                    command.add("-Dsun.stderr.encoding=UTF-8");
                    command.add("-Dnative.encoding=UTF-8");
                    // Run tests in-process so all stdout/stderr flows back through our reader.
                    // Without this, Surefire forks a child JVM and its output is silently lost.
                    command.add("-DforkCount=0");
                    command.add("-DreuseForks=true");
                    command.add("-Dsurefire.useFile=false");

                    LOGGER.info("[Aura Server] Executing command: {}", String.join(" ", command));
                    broadcastLog("[INFO] Command: " + String.join(" ", command));
                    broadcastLog("[INFO] ------------------------------------------------------------------------");

                    final ProcessBuilder pb = new ProcessBuilder(command);
                    pb.environment().put("MAVEN_OPTS", "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8");
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
                            LOGGER.info("[Aura Subprocess] {}", stripAnsi(line));

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
                    final List<String> uniqueFiles = new ArrayList<>(datasetsByFile.keySet());
                    generateAllureReport(uniqueFiles);
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
            final String metadataContent = String.format("{\n  \"status\": \"%s\",\n  \"timestamp\": \"%s\",\n  \"total\": %d,\n  \"passed\": %d,\n  \"failed\": %d\n}", 
                status, timestamp, globalTestsRun.get(), globalPassed.get(), globalFailed.get());
            Files.writeString(metadataFile.toPath(), metadataContent, StandardCharsets.UTF_8);

            final File logFile = new File(destDir, "execution.log");
            Files.write(logFile.toPath(), currentRunLogs, StandardCharsets.UTF_8);

            LOGGER.info("[Aura Server] Allure report copied to history: {}", destDir.getName());
            broadcastLog("[INFO] Allure report copied to history: " + destDir.getName());
            broadcastReportReady(destDir.getName());
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
        currentRunLogs.add(cleanLine);
    }

    private static void broadcastReportReady(final String reportId)
    {
        final Map<String, Object> event = new HashMap<>();
        event.put("type", "reportReady");
        event.put("reportId", reportId);
        currentRunEvents.add(event);
    }

    private static void broadcastStatus(final boolean running)
    {
        // Status is now computed on-the-fly during /api/status polling.
        // We leave this method here to avoid breaking existing calls.
    }

    private static void checkShutdownOnDisconnect()
    {
        synchronized (activeClients)
        {
            if (activeClients.isEmpty())
            {
                if (pendingShutdown == null || pendingShutdown.isDone())
                {
                    LOGGER.info("[Aura Server] No active clients. Scheduling server shutdown in 5 seconds...");
                    pendingShutdown = shutdownScheduler.schedule(() -> {
                        synchronized (activeClients)
                        {
                            if (activeClients.isEmpty())
                            {
                                LOGGER.info("[Aura Server] No active clients for 5 seconds. Shutting down...");
                                
                                final Process p = activeProcess.get();
                                if (p != null)
                                {
                                    p.destroy();
                                }
                                
                                if (System.getProperty("neodymium.aura.test") == null)
                                {
                                    System.exit(0);
                                }
                                else
                                {
                                    LOGGER.info("[Aura Server] Skipping System.exit(0) in test environment.");
                                }
                            }
                        }
                    }, 5, TimeUnit.SECONDS);
                }
            }
        }
    }

    private static void broadcast(final String eventJson)
    {
        // Removed as we are using polling instead of SSE stream
    }

    public static Map<String, Object> getFileDetails(final File file)
    {
        final Map<String, Object> details = new HashMap<>();
        details.put("path", file.getName());
        try
        {
            final Yaml yaml = new Yaml();
            final Object data;
            try (final InputStream is = new FileInputStream(file))
            {
                data = yaml.load(is);
            }
            if (data instanceof Map)
            {
                final Map<String, Object> root = (Map<String, Object>) data;
                
                // Extract steps info
                if (root.containsKey("steps"))
                {
                    final String stepsStr = String.valueOf(root.get("steps"));
                    final String[] steps = stepsStr.split("\n");
                    final List<String> cleanSteps = new ArrayList<>();
                    for (final String step : steps)
                    {
                        if (!step.trim().isEmpty())
                        {
                            cleanSteps.add(step.trim());
                        }
                    }
                    details.put("totalSteps", cleanSteps.size());
                    final List<String> summary = cleanSteps.subList(0, Math.min(cleanSteps.size(), 3));
                    details.put("stepsSummary", summary);
                }
                else
                {
                    details.put("totalSteps", 0);
                    details.put("stepsSummary", new ArrayList<>());
                }

                // Extract data / test IDs / dataset keys
                final List<DatasetDto> datasetsList = new ArrayList<>();
                final List<String> datasetKeys = new ArrayList<>();
                if (root.containsKey("data"))
                {
                    final Object dataObj = root.get("data");
                    if (dataObj instanceof List)
                    {
                        final List<?> dataList = (List<?>) dataObj;
                        for (int i = 0; i < dataList.size(); i++)
                        {
                            final Object item = dataList.get(i);
                            if (item instanceof Map)
                            {
                                final Map<?, ?> itemMap = (Map<?, ?>) item;
                                final Object tId = itemMap.get("testId") != null ? itemMap.get("testId") : itemMap.get("TEST_ID");
                                if (tId != null && !String.valueOf(tId).trim().isEmpty())
                                {
                                    datasetsList.add(new DatasetDto(String.valueOf(tId), String.valueOf(tId), true));
                                }
                                else
                                {
                                    datasetsList.add(new DatasetDto(String.valueOf(i + 1), "Dataset " + (i + 1), false));
                                }
                                for (final Object key : itemMap.keySet())
                                {
                                    final String keyStr = String.valueOf(key);
                                    if (!datasetKeys.contains(keyStr) && !keyStr.startsWith("neodymium."))
                                    {
                                        datasetKeys.add(keyStr);
                                    }
                                }
                            }
                        }
                    }
                }
                else
                {
                    final Object tId = root.get("testId") != null ? root.get("testId") : root.get("TEST_ID");
                    if (tId != null && !String.valueOf(tId).trim().isEmpty())
                    {
                        datasetsList.add(new DatasetDto(String.valueOf(tId), String.valueOf(tId), true));
                    }
                    else
                    {
                        datasetsList.add(new DatasetDto("1", "Dataset 1", false));
                    }
                    for (final Object key : root.keySet())
                    {
                        final String keyStr = String.valueOf(key);
                        if (!"steps".equals(keyStr) && !"data".equals(keyStr) && !keyStr.startsWith("neodymium."))
                        {
                            datasetKeys.add(keyStr);
                        }
                    }
                }
                details.put("datasets", datasetsList);
                final List<String> testIds = new ArrayList<>();
                for (final DatasetDto d : datasetsList)
                {
                    testIds.add(d.label);
                }
                details.put("testIds", testIds);
                details.put("datasetKeys", datasetKeys);
            }
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to parse YAML file details for: " + file.getAbsolutePath(), e);
            details.put("error", e.getMessage());
        }
        return details;
    }

    private static void scanDirStatic(final File baseDir, final File currentDir, final List<String> yamlFiles)
    {
        final File[] files = currentDir.listFiles();
        if (files != null)
        {
            for (final File file : files)
            {
                if (file.isDirectory())
                {
                    scanDirStatic(baseDir, file, yamlFiles);
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

    private static ChatResponse runChatWorkflow(final ChatRequest req)
    {
        final StringBuilder thinkingLog = new StringBuilder();
        thinkingLog.append("[AI Intent Classification] Running Stage 1...\n");

        final LlmClient client = new LlmClient(Neodymium.aiConfiguration(), new AiStats());

        // Stage 1: Intent Classifier
        final String stage1SystemPrompt = 
            "You are an AI router for a test automation manager called Neodymium Aura.\n" +
            "Your job is to classify the user's intent into one of the following categories:\n" +
            "- \"select\": The user wants to select, run, filter, check, or execute one or more test cases.\n" +
            "- \"edit\": The user wants to edit, update, create, delete, add steps, or modify a test case.\n" +
            "- \"both\": The user request implies both selection and editing.\n" +
            "- \"neither\": The user is asking a general question, greeting, or querying system statistics/status.\n\n" +
            "Respond ONLY with a valid JSON object matching this schema:\n" +
            "{\n" +
            "  \"intent\": \"select\" | \"edit\" | \"both\" | \"neither\",\n" +
            "  \"reason\": \"Brief reason for classification\"\n" +
            "}";

        final List<ChatMessage> stage1Messages = new ArrayList<>();
        stage1Messages.add(SystemMessage.from(stage1SystemPrompt));
        if (req.history != null)
        {
            for (final ChatMessageDto msg : req.history)
            {
                if ("user".equalsIgnoreCase(msg.role))
                {
                    stage1Messages.add(UserMessage.from(msg.content));
                }
                else if ("assistant".equalsIgnoreCase(msg.role))
                {
                    stage1Messages.add(AiMessage.from(msg.content));
                }
            }
        }
        stage1Messages.add(UserMessage.from(req.prompt));

        final String stage1Response = client.chat(stage1Messages);
        thinkingLog.append("Stage 1 Response: ").append(stage1Response).append("\n");

        String intent = "neither";
        try
        {
            final Map<?, ?> result = gson.fromJson(stage1Response, Map.class);
            if (result != null && result.containsKey("intent"))
            {
                intent = String.valueOf(result.get("intent"));
            }
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to parse Stage 1 response", e);
            thinkingLog.append("Error parsing Stage 1 response: ").append(e.getMessage()).append("\n");
        }

        thinkingLog.append("Classified intent: ").append(intent).append("\n");

        if ("edit".equalsIgnoreCase(intent) || "both".equalsIgnoreCase(intent))
        {
            // Run editor / creation workflow
            thinkingLog.append("[AI Test Generation] Running Stage 2 (Edit/Create)...\n");

            // Assemble dynamic instructions from action plugins
            final StringBuilder actionInstructions = new StringBuilder();
            try
            {
                for (final Object plugin : ActionRegistry.getAllPlugins())
                {
                    final Method getInstructionsMethod = plugin.getClass().getMethod("getPromptInstructions");
                    final Object instructions = getInstructionsMethod.invoke(plugin);
                    if (instructions != null)
                    {
                        actionInstructions.append(instructions).append("\n");
                    }
                }
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to load action plugins instructions", e);
                thinkingLog.append("Error loading action plugins: ").append(e.getMessage()).append("\n");
            }

            final File resourcesDir = new File("src/test/resources").getAbsoluteFile();
            String activeFileContent = "";
            if (req.activeFile != null && !req.activeFile.trim().isEmpty())
            {
                try
                {
                    final File file = new File(resourcesDir, req.activeFile).getCanonicalFile();
                    if (file.exists() && file.isFile() && file.getPath().startsWith(resourcesDir.getPath()))
                    {
                        activeFileContent = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.error("Failed to read active file: " + req.activeFile, e);
                }
            }

            final String editorSystemPrompt =
                "You are the Neodymium Aura Test Editor Assistant.\n" +
                "Your goal is to edit or create a YAML test case file based on the user's request.\n" +
                "You MUST generate the steps using the available action plugins. Here are the instructions for the registered actions:\n" +
                actionInstructions.toString() + "\n" +
                "CRITICAL REQUIREMENT ON YAML FORMATTING:\n" +
                "The YAML file content must strictly follow this format:\n" +
                "- Do NOT include metadata fields like 'name', 'description', or any other root-level properties.\n" +
                "- The 'steps' property MUST be a multiline YAML string (using the '|' indicator), where each line is a natural English step sentence. Example:\n" +
                "  steps: |\n" +
                "    Open https://posters.xceptance.io:8443/posters/\n" +
                "    Type \"${searchTerm}\" into the search field.\n" +
                "    Click the Search Icon.\n" +
                "    Verify that the main heading contains \"${searchTerm}\".\n" +
                "- The 'data' property (optional, for parameterized datasets) must be a list of parameter maps. Example:\n" +
                "  data:\n" +
                "    - searchTerm: Test\n" +
                "    - searchTerm: Chair\n" +
                "- Do NOT output steps as structured arrays/lists of action/target/value objects (e.g. '- action: NAVIGATE'). Only write them as plain, natural English statements.\n" +
                "- Do NOT guess or hallucinate HTML element IDs, CSS selectors, class names, or XPaths (such as \"search-form-input\" or \"div.no-results\") unless they are explicitly given. Stick to high-level, simple natural English descriptions of elements (e.g., \"search field\", \"no products found message\").\n\n" +
                "If the user wants to EDIT an existing file, the current content of the active file is:\n" +
                activeFileContent + "\n\n" +
                "You MUST respond in JSON format with the following schema:\n" +
                "{\n" +
                "  \"status\": \"COMPLETE\" | \"ERROR\",\n" +
                "  \"reasoning\": \"Explain your step-by-step thinking process for generating the test steps.\",\n" +
                "  \"filename\": \"name-of-the-file.yaml\",\n" +
                "  \"content\": \"The complete new or modified YAML file content (valid YAML format, with steps and optional data sections)\",\n" +
                "  \"message\": \"Your user-facing response message summarizing what you changed or created.\"\n" +
                "}";

            final List<ChatMessage> editMessages = new ArrayList<>();
            editMessages.add(SystemMessage.from(editorSystemPrompt));
            if (req.history != null)
            {
                for (final ChatMessageDto msg : req.history)
                {
                    if ("user".equalsIgnoreCase(msg.role))
                    {
                        editMessages.add(UserMessage.from(msg.content));
                    }
                    else if ("assistant".equalsIgnoreCase(msg.role))
                    {
                        editMessages.add(AiMessage.from(msg.content));
                    }
                }
            }
            editMessages.add(UserMessage.from(req.prompt));

            final String editorResponse = client.chat(editMessages);
            thinkingLog.append("Editor Response: ").append(editorResponse).append("\n");

            try
            {
                final Map<?, ?> editResult = gson.fromJson(editorResponse, Map.class);
                final String status = String.valueOf(editResult.get("status"));
                final String message = String.valueOf(editResult.get("message"));

                if ("COMPLETE".equalsIgnoreCase(status))
                {
                    final String filename = String.valueOf(editResult.get("filename"));
                    final String content = String.valueOf(editResult.get("content"));

                    final File file = new File(resourcesDir, filename).getCanonicalFile();
                    if (file.getPath().startsWith(resourcesDir.getPath()))
                    {
                        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
                        thinkingLog.append("Successfully saved file: ").append(filename).append("\n");
                        return new ChatResponse(message, thinkingLog.toString(), "edit_or_create_test", null, filename, content, null);
                    }
                    else
                    {
                        return new ChatResponse("Access denied: Directory traversal detected", thinkingLog.toString(), "error", null, null, null, null);
                    }
                }
                else
                {
                    return new ChatResponse("Failed to generate test case: " + message, thinkingLog.toString(), "error", null, null, null, null);
                }
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to parse editor response JSON", e);
                return new ChatResponse("Failed to parse generated test response: " + e.getMessage(), thinkingLog.toString(), "error", null, null, null, null);
            }
        }
        else if ("select".equalsIgnoreCase(intent))
        {
            // Run selection escalation loop
            thinkingLog.append("[AI Test Selection] Running Stage 2 (Selection Escalation)...\n");

            final List<String> allFiles = new ArrayList<>();
            final File resourcesDir = new File("src/test/resources").getAbsoluteFile();
            scanDirStatic(resourcesDir, resourcesDir, allFiles);
            allFiles.sort(String::compareTo);

            final List<String> availableDatasets = new ArrayList<>();
            for (final String file : allFiles)
            {
                final File yamlFile = new File(resourcesDir, file);
                final Map<String, Object> details = getFileDetails(yamlFile);
                final List<DatasetDto> datasets = (List<DatasetDto>) details.get("datasets");
                if (datasets != null)
                {
                    for (final DatasetDto d : datasets)
                    {
                        availableDatasets.add("File: " + file + ", Dataset ID: " + d.id);
                    }
                }
            }

            final List<ChatMessage> conversation = new ArrayList<>();

            final String selectionSystemPrompt =
                "You are the Neodymium Aura Test Selector Assistant.\n" +
                "Your goal is to identify and select the list of datasets (specifying both their YAML file name and dataset ID/index) that match the user's request.\n" +
                "You have access to the test cases in three escalation levels:\n" +
                "- Level 1: Just the relative file paths and dataset IDs of all available datasets.\n" +
                "- Level 2: Metadata for specific files (step count, step summary, dataset keys).\n" +
                "- Level 3: The complete YAML file content for specific files.\n\n" +
                "To minimize token usage, you must start with Level 1. If you cannot decide which datasets to select based only on file paths and dataset IDs, you must output a JSON response requesting Level 2 metadata for candidate files. If you still cannot decide, request Level 3 full content.\n" +
                "However, before requesting metadata or full content, if you realize you need specific data parameters or steps to identify the matching test cases, you can ask the user directly or explain what you need.\n\n" +
                "CRITICAL REQUIREMENT ON DISCARDING FILES/DATASETS:\n" +
                "When evaluating available datasets at Level 1 based on their file names and dataset IDs:\n" +
                "- You MUST NOT assume a dataset is irrelevant just because its file name or dataset ID does not contain keywords from the user request.\n" +
                "- Only rule out/discard files whose names and IDs make it completely impossible or highly improbable to match.\n" +
                "- If a filename or ID is generic or ambiguous, you MUST request Level 2 metadata (\"NEED_DETAILS\") to inspect its steps before making a decision.\n\n" +
                "The current available datasets (Level 1) are:\n" +
                gson.toJson(availableDatasets) + "\n\n" +
                "You MUST respond in JSON format with the following schema:\n" +
                "{\n" +
                "  \"status\": \"NEED_DETAILS\" | \"NEED_FULL_CONTENT\" | \"COMPLETE\" | \"ASK_USER\",\n" +
                "  \"reasoning\": \"Explain your step-by-step thinking process, which candidate files/datasets you are looking at and why.\",\n" +
                "  \"requestedFiles\": [\"relative/path/to/file1.yaml\", ...], // Files you need details/content for (only when status is NEED_DETAILS or NEED_FULL_CONTENT)\n" +
                "  \"selectedDatasets\": [{\"file\": \"relative/path/to/file1.yaml\", \"id\": \"posters-search\"}, ...], // The final selected datasets (only when status is COMPLETE)\n" +
                "  \"message\": \"Your user-facing response message. If status is COMPLETE, summarize which datasets you selected. If status is ASK_USER, ask for clarification.\"\n" +
                "}";

            conversation.add(SystemMessage.from(selectionSystemPrompt));
            if (req.history != null)
            {
                for (final ChatMessageDto msg : req.history)
                {
                    if ("user".equalsIgnoreCase(msg.role))
                    {
                        conversation.add(UserMessage.from(msg.content));
                    }
                    else if ("assistant".equalsIgnoreCase(msg.role))
                    {
                        conversation.add(AiMessage.from(msg.content));
                    }
                }
            }
            conversation.add(UserMessage.from(req.prompt));

            int iterations = 0;
            while (iterations < 5)
            {
                iterations++;
                thinkingLog.append("Escalation Loop Iteration ").append(iterations).append("...\n");

                final String responseText = client.chat(conversation);
                thinkingLog.append("Response: ").append(responseText).append("\n");

                try
                {
                    final Map<?, ?> selResult = gson.fromJson(responseText, Map.class);
                    final String status = String.valueOf(selResult.get("status"));
                    final String message = String.valueOf(selResult.get("message"));

                    if ("ASK_USER".equalsIgnoreCase(status))
                    {
                        return new ChatResponse(message, thinkingLog.toString(), null, null, null, null, null);
                    }
                    else if ("COMPLETE".equalsIgnoreCase(status))
                    {
                        final List<Map<?, ?>> rawDatasets = (List<Map<?, ?>>) selResult.get("selectedDatasets");
                        final List<DatasetSelection> selectedDatasets = new ArrayList<>();
                        if (rawDatasets != null)
                        {
                            for (final Map<?, ?> rawItem : rawDatasets)
                            {
                                final DatasetSelection sel = new DatasetSelection();
                                sel.file = String.valueOf(rawItem.get("file"));
                                sel.id = String.valueOf(rawItem.get("id"));
                                selectedDatasets.add(sel);
                            }
                        }
                        return new ChatResponse(message, thinkingLog.toString(), "select_tests", null, null, null, selectedDatasets);
                    }
                    else if ("NEED_DETAILS".equalsIgnoreCase(status))
                    {
                        final List<String> requested = (List<String>) selResult.get("requestedFiles");
                        final List<Map<String, Object>> detailsList = new ArrayList<>();
                        for (final String reqFile : requested)
                        {
                            final File f = new File(resourcesDir, reqFile).getCanonicalFile();
                            if (f.exists() && f.isFile() && f.getPath().startsWith(resourcesDir.getPath()))
                            {
                                detailsList.add(getFileDetails(f));
                            }
                        }
                        thinkingLog.append("Retrieved Level 2 details for: ").append(requested).append("\n");

                        conversation.add(AiMessage.from(responseText));
                        conversation.add(UserMessage.from("Here is the Level 2 metadata for your requested files:\n" + gson.toJson(detailsList)));
                    }
                    else if ("NEED_FULL_CONTENT".equalsIgnoreCase(status))
                    {
                        final List<String> requested = (List<String>) selResult.get("requestedFiles");
                        final Map<String, String> contentsMap = new HashMap<>();
                        for (final String reqFile : requested)
                        {
                            final File f = new File(resourcesDir, reqFile).getCanonicalFile();
                            if (f.exists() && f.isFile() && f.getPath().startsWith(resourcesDir.getPath()))
                            {
                                contentsMap.put(reqFile, Files.readString(f.toPath(), StandardCharsets.UTF_8));
                            }
                        }
                        thinkingLog.append("Retrieved Level 3 content for: ").append(requested).append("\n");

                        conversation.add(AiMessage.from(responseText));
                        conversation.add(UserMessage.from("Here is the Level 3 full content for your requested files:\n" + gson.toJson(contentsMap)));
                    }
                    else
                    {
                        return new ChatResponse("AI selector returned unknown status: " + status, thinkingLog.toString(), "error", null, null, null, null);
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.error("Failed to parse or execute loop step", e);
                    return new ChatResponse("AI selector execution error: " + e.getMessage(), thinkingLog.toString(), "error", null, null, null, null);
                }
            }
            return new ChatResponse("AI selector loop exceeded maximum iterations.", thinkingLog.toString(), "error", null, null, null, null);
        }
        else
        {
            // General talk / neither
            thinkingLog.append("[AI General Chat] Running Stage 2 (Neither)...\n");
            final String fallbackSystemPrompt =
                "You are the Neodymium Aura AI Assistant.\n" +
                "Respond nicely to the user's general message, greeting, or question.\n" +
                "You can also inform them about your capabilities to select/run tests or create/edit test cases.\n\n" +
                "You MUST respond in JSON format matching this schema:\n" +
                "{\n" +
                "  \"message\": \"Your markdown-formatted response message to the user.\"\n" +
                "}";

            final List<ChatMessage> fallbackMessages = new ArrayList<>();
            fallbackMessages.add(SystemMessage.from(fallbackSystemPrompt));
            if (req.history != null)
            {
                for (final ChatMessageDto msg : req.history)
                {
                    if ("user".equalsIgnoreCase(msg.role))
                    {
                        fallbackMessages.add(UserMessage.from(msg.content));
                    }
                    else if ("assistant".equalsIgnoreCase(msg.role))
                    {
                        fallbackMessages.add(AiMessage.from(msg.content));
                    }
                }
            }
            fallbackMessages.add(UserMessage.from(req.prompt));

            final String fallbackResponse = client.chat(fallbackMessages);
            String message = fallbackResponse;
            try
            {
                final Map<?, ?> fallbackResult = gson.fromJson(fallbackResponse, Map.class);
                if (fallbackResult != null && fallbackResult.containsKey("message"))
                {
                    message = String.valueOf(fallbackResult.get("message"));
                }
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to parse fallback response JSON", e);
            }
            return new ChatResponse(message, thinkingLog.toString(), null, null, null, null, null);
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

    private static final class DatasetSelection
    {
        String file;
        String id; // testId or index
    }

    private static final class RunRequest
    {
        List<DatasetSelection> datasets;
        boolean headless;
        boolean interactive;
        boolean allure;
        boolean video;
        boolean keepOpen;
    }

    private static final class DatasetDto
    {
        final String id;
        final String label;
        final boolean hasTestId;

        DatasetDto(final String id, final String label, final boolean hasTestId)
        {
            this.id = id;
            this.label = label;
            this.hasTestId = hasTestId;
        }
    }

    private static final class YamlFileDto
    {
        final String file;
        final List<DatasetDto> datasets;

        YamlFileDto(final String file, final List<DatasetDto> datasets)
        {
            this.file = file;
            this.datasets = datasets;
        }
    }

    private static final class ChatMessageDto
    {
        String role;
        String content;
    }

    private static final class ChatRequest
    {
        String prompt;
        String activeFile;
        List<ChatMessageDto> history;
    }

    private static final class ChatResponse
    {
        String message;
        String thinking;
        String action;
        List<String> files;
        String filename;
        String content;
        List<DatasetSelection> selectedDatasets;

        ChatResponse(final String message, final String thinking, final String action, final List<String> files, final String filename, final String content, final List<DatasetSelection> selectedDatasets)
        {
            this.message = message;
            this.thinking = thinking;
            this.action = action;
            this.files = files;
            this.filename = filename;
            this.content = content;
            this.selectedDatasets = selectedDatasets;
        }
    }
}
