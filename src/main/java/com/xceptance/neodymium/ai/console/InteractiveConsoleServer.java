/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package com.xceptance.neodymium.ai.console;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neodymium.ai.config.AiConfiguration;
import org.yaml.snakeyaml.Yaml;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Lightweight standalone HTTP server for the Interactive Console.
 * <p>
 * This wrapper is used when tests are executed with {@code neodymium.ai.interactive=true} but without the Neodymium
 * Aura Manager. It starts a minimal {@link HttpServer} and wires in the provided {@link InteractiveConsoleEngine}'s
 * HTTP handlers.
 * </p>
 * <p>
 * When the Aura Manager is active (detected via the {@code aura.manager} JVM property), this class should <em>not</em>
 * be instantiated. Instead, the engine's handlers are registered directly on the Aura Manager's existing server.
 * </p>
 * <h2>Endpoints served</h2>
 * <ul>
 * <li>{@code GET /} — serves the {@code interactive_console.html} UI template</li>
 * <li>{@code GET /api/console/events} — SSE stream (delegated to engine)</li>
 * <li>{@code POST /api/console/action} — action receiver (delegated to engine)</li>
 * </ul>
 *
 * @author AI-generated: Claude Sonnet 4.5
 * @author Xceptance GmbH 2026
 */
public final class InteractiveConsoleServer
{
    private static final Logger LOG = LoggerFactory.getLogger(InteractiveConsoleServer.class);

    /** Classpath location of the HTML template served at {@code GET /}. */
    private static final String HTML_RESOURCE = "com/xceptance/neodymium/ai/console/interactive_console.html";

    /** Default starting port; the server will scan upwards for a free port. */
    private static final int DEFAULT_START_PORT = 18090;

    private final HttpServer server;

    private final InteractiveConsoleEngine engine;

    private final int port;

    private com.codeborne.selenide.SelenideDriver consoleDriver;

    /**
     * Creates and starts the standalone server, binding it to the first free port at or above
     * {@link #DEFAULT_START_PORT} on all network interfaces ({@code 0.0.0.0}).
     *
     * @param engine
     *            the {@link InteractiveConsoleEngine} whose handlers will be registered
     * @throws IOException
     *             if no free port can be found or the server cannot start
     */
    public InteractiveConsoleServer(final InteractiveConsoleEngine engine) throws IOException
    {
        this.engine = engine;
        this.server = createServer(engine);
        this.port = this.server.getAddress().getPort();
    }

    /**
     * Returns the URL at which the Interactive Console can be opened in a browser. The URL uses {@code localhost} for
     * display purposes; the server is actually bound to all interfaces, so it is also reachable via the host's LAN IP.
     *
     * @return the local URL, e.g. {@code http://localhost:18090}
     */
    public String getLocalUrl()
    {
        return "http://localhost:" + this.port;
    }

    public String getLanUrl()
    {
        return "http://" + (engine != null ? engine.getLanIp() : "localhost") + ":" + this.port;
    }

    public void openBrowser()
    {
        final String url = getLocalUrl();
        LOG.info("========================================================================");
        LOG.info("  Interactive Console: {}", url);
        LOG.info("  (Also accessible on your local network via your machine's IP)");
        LOG.info("========================================================================");

        try
        {
            final com.codeborne.selenide.SelenideConfig config = new com.codeborne.selenide.SelenideConfig();
            String browser = Neodymium.getBrowserName();
            if (browser == null || browser.isEmpty())
            {
                browser = "chrome";
            }
            config.browser(browser);
            config.headless(false); // force non-headless
            if (browser.toLowerCase().contains("chrome"))
            {
                final org.openqa.selenium.chrome.ChromeOptions options = new org.openqa.selenium.chrome.ChromeOptions();
                options.addArguments("--app=" + url);
                config.browserCapabilities(options);
            }
            this.consoleDriver = new com.codeborne.selenide.SelenideDriver(config);
            this.consoleDriver.open(url);
            return;
        }
        catch (final Exception e)
        {
            LOG.warn("Could not open browser via Selenide: {}. Falling back to Desktop.", e.getMessage());
        }
        // Fallback if selnide failed.

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
        {
            try
            {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
            catch (final Exception e)
            {
                LOG.warn("Could not open browser via Desktop.browse: {}. Trying OS fallback.", e.getMessage());
            }
        }

        try
        {
            final String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win"))
            {
                Runtime.getRuntime().exec(new String[] { "cmd.exe", "/c", "start", url });
            }
            else if (os.contains("mac"))
            {
                Runtime.getRuntime().exec(new String[] { "open", url });
            }
            else
            {
                Runtime.getRuntime().exec(new String[] { "xdg-open", url });
            }
        }
        catch (final Exception e)
        {
            LOG.warn("Could not open browser automatically. Please open manually: {}", url);
        }
    }

    /**
     * Stops the HTTP server gracefully and closes the interactive console browser if running.
     */
    public void stop()
    {
        if (this.consoleDriver != null)
        {
            try
            {
                this.consoleDriver.close();
                if (this.consoleDriver.hasWebDriverStarted())
                {
                    this.consoleDriver.getWebDriver().quit();
                }
            }
            catch (final Exception e)
            {
                LOG.warn("[InteractiveConsoleServer] Could not close console browser driver: {}", e.getMessage());
            }
            this.consoleDriver = null;
        }
        this.server.stop(0);
        LOG.info("[InteractiveConsoleServer] Stopped.");
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Allocates an {@link HttpServer} on the first available port starting at {@link #DEFAULT_START_PORT} and registers
     * all required context handlers.
     *
     * @param engine
     *            the engine to wire in
     * @return a started server
     * @throws IOException
     *             if all 100 candidate ports are occupied
     */
    private static HttpServer createServer(final InteractiveConsoleEngine engine) throws IOException
    {
        HttpServer httpServer = null;
        int port = DEFAULT_START_PORT;
        while (port < DEFAULT_START_PORT + 100)
        {
            try
            {
                httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
                break;
            }
            catch (final IOException e)
            {
                port++;
            }
        }

        if (httpServer == null)
        {
            throw new IOException("Could not find a free port for the Interactive Console starting at " + DEFAULT_START_PORT);
        }

        // Wire engine handlers
        httpServer.createContext("/api/console/events", engine.createSseHandler());
        httpServer.createContext("/api/console/action", engine.createActionHandler());
        httpServer.createContext("/api/console/screenshot", new ScreenshotFileHandler());
        httpServer.createContext("/api/stop", new StopHandler());

        // Serve the HTML template and static assets at the root
        httpServer.createContext("/", new StaticResourceHandler());

        httpServer.setExecutor(Executors.newCachedThreadPool(runnable -> {
            final Thread t = new Thread(runnable, "InteractiveConsole-HTTP");
            t.setDaemon(true);
            return t;
        }));
        httpServer.start();

        LOG.info("[InteractiveConsoleServer] Started on port {}", port);
        return httpServer;
    }

    // -------------------------------------------------------------------------
    // Inner handlers
    // -------------------------------------------------------------------------

    /**
     * Stop handler that terminates the local execution process.
     */
    private static final class StopHandler implements com.sun.net.httpserver.HttpHandler
    {
        @Override
        public void handle(final HttpExchange exchange) throws IOException
        {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod()))
            {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod()))
            {
                LOG.info("[InteractiveConsoleServer] User requested to stop active execution via /api/stop");
                final byte[] response = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                try (final OutputStream os = exchange.getResponseBody())
                {
                    os.write(response);
                }

                new Thread(() -> {
                    try
                    {
                        Thread.sleep(500);
                    }
                    catch (final InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }
                    System.exit(0);
                }).start();
            }
            else
            {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    /**
     * Serves the static resources from the classpath.
     */
    private static final class StaticResourceHandler implements com.sun.net.httpserver.HttpHandler
    {
        @Override
        public void handle(final HttpExchange exchange) throws IOException
        {
            final String path = exchange.getRequestURI().getPath();

            String resourcePath;
            String contentType;

            if ("/".equals(path) || "/index.html".equals(path))
            {
                resourcePath = HTML_RESOURCE;
                contentType = "text/html; charset=UTF-8";
            }
            else if ("/interactive_console.css".equals(path))
            {
                resourcePath = "com/xceptance/neodymium/ai/console/interactive_console.css";
                contentType = "text/css; charset=UTF-8";
            }
            else if ("/interactive_console.js".equals(path))
            {
                resourcePath = "com/xceptance/neodymium/ai/console/interactive_console.js";
                contentType = "application/javascript; charset=UTF-8";
            }
            else
            {
                final byte[] msg = "Not Found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, msg.length);
                try (final OutputStream out = exchange.getResponseBody())
                {
                    out.write(msg);
                }

                return;
            }

            try (final InputStream is = InteractiveConsoleServer.class.getClassLoader()
                                                                      .getResourceAsStream(resourcePath))
            {
                if (is == null)
                {
                    final byte[] msg = ("Resource not found on classpath: " + resourcePath)
                                                                                           .getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(500, msg.length);
                    try (final OutputStream out = exchange.getResponseBody())
                    {
                        out.write(msg);
                    }
                    return;
                }

                final byte[] bytes = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);

                try (final OutputStream out = exchange.getResponseBody())
                {
                    out.write(bytes);
                }

            }
        }
    }

    // -------------------------------------------------------------------------
    // Static JSON file handler
    // -------------------------------------------------------------------------

    static class ScreenshotFileHandler implements com.sun.net.httpserver.HttpHandler
    {
        @Override
        public void handle(final HttpExchange exchange) throws IOException
        {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod()))
            {
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            final String query = exchange.getRequestURI().getQuery();
            if (query != null)
            {
                String fileName = null;
                for (final String param : query.split("&"))
                {
                    final String[] pair = param.split("=");
                    if (pair.length > 1 && "file".equals(pair[0]))
                    {
                        fileName = pair[1];
                        break;
                    }
                }

                if (fileName != null)
                {
                    if (fileName.contains("/") || fileName.contains("\\") || fileName.contains(".."))
                    {
                        exchange.sendResponseHeaders(403, -1);
                        return;
                    }
                    final String screenshotsDir = AiConfiguration.getInstance().getProperty("neodymium.ai.console.screenshotsDir", "target/aura-sandbox/ai-console-screenshots");
                    final Path file = Paths.get(screenshotsDir, fileName);
                    if (Files.exists(file))
                    {
                        exchange.getResponseHeaders().set("Content-Type", "image/png");
                        final byte[] bytes = Files.readAllBytes(file);
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (final OutputStream os = exchange.getResponseBody())
                        {
                            os.write(bytes);
                        }
                        return;
                    }
                }
            }
            exchange.sendResponseHeaders(404, -1);
        }
    }

    /**
     * Serves a single static JSON file from disk at {@code GET /run_data.json}. This allows the browser to fetch the
     * run data even when no live engine is attached.
     */
    private static final class StaticJsonHandler implements com.sun.net.httpserver.HttpHandler
    {
        private final Path jsonPath;

        StaticJsonHandler(final Path jsonPath)
        {
            this.jsonPath = jsonPath;
        }

        @Override
        public void handle(final HttpExchange exchange) throws IOException
        {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            final byte[] bytes = Files.readAllBytes(this.jsonPath);
            exchange.sendResponseHeaders(200, bytes.length);
            try (final OutputStream out = exchange.getResponseBody())
            {
                out.write(bytes);
            }
        }
    }

    /**
     * Serves the default {@code run_example.json} from the classpath at {@code GET /run_data.json} when no external
     * file path is provided in standalone mode.
     */
    private static final class ClasspathJsonHandler implements com.sun.net.httpserver.HttpHandler
    {
        private static final String JSON_RESOURCE = "com/xceptance/neodymium/ai/console/run_example.json";

        @Override
        public void handle(final HttpExchange exchange) throws IOException
        {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            try (final InputStream is = InteractiveConsoleServer.class.getClassLoader()
                                                                      .getResourceAsStream(JSON_RESOURCE))
            {
                if (is == null)
                {
                    final byte[] msg = ("Resource not found on classpath: " + JSON_RESOURCE)
                                                                                            .getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(500, msg.length);
                    try (final OutputStream out = exchange.getResponseBody())
                    {
                        out.write(msg);
                    }
                    return;
                }

                final byte[] bytes = is.readAllBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (final OutputStream out = exchange.getResponseBody())
                {
                    out.write(bytes);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Standalone entry point
    // -------------------------------------------------------------------------

    /**
     * Starts the Interactive Console in read-only static mode, serving a given run JSON file. Useful for reviewing a
     * completed or in-progress test result without running a full test.
     * <p>
     * Usage:
     * </p>
     * 
     * <pre>
     *   java -cp ... InteractiveConsoleServer [path/to/run.json] [path/to/test.yaml]
     * </pre>
     * <p>
     * If no argument is given, it looks for {@code run_example.json} on the classpath.
     * </p>
     *
     * @param args
     *            optional: path to the run JSON file and optionally test YAML file
     */
    public static void main(final String[] args)
    {
        try
        {
            // Load execution database JSON
            String jsonContent = null;
            String runId = "static-view";

            if (args.length > 0)
            {
                final Path jsonPath = Paths.get(args[0]).toAbsolutePath();
                if (!Files.exists(jsonPath))
                {
                    System.err.println("JSON file not found: " + jsonPath);
                    System.exit(1);
                }
                jsonContent = new String(Files.readAllBytes(jsonPath), StandardCharsets.UTF_8);
            }
            else
            {
                try (final InputStream is = InteractiveConsoleServer.class.getClassLoader()
                                                                          .getResourceAsStream(ClasspathJsonHandler.JSON_RESOURCE))
                {
                    if (is != null)
                    {
                        jsonContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            }

            // Extract correct runId from JSON if available to avoid UI warning
            if (jsonContent != null)
            {
                try
                {
                    final JsonObject parsed = JsonParser.parseString(jsonContent).getAsJsonObject();
                    if (parsed.has("runId"))
                    {
                        runId = parsed.get("runId").getAsString();
                    }
                }
                catch (final Exception e)
                {
                    LOG.warn("Failed to parse runId from JSON database: {}", e.getMessage());
                }
            }

            final InteractiveConsoleEngine engine = new InteractiveConsoleEngine(runId);
            final InteractiveConsoleServer srv = new InteractiveConsoleServer(engine);

            // Register handlers
            if (args.length > 0)
            {
                final Path jsonPath = Paths.get(args[0]).toAbsolutePath();
                srv.server.createContext("/run_data.json", new StaticJsonHandler(jsonPath));
                LOG.info("[InteractiveConsoleServer] Serving run data from: {}", jsonPath);
            }
            else
            {
                srv.server.createContext("/run_data.json", new ClasspathJsonHandler());
                LOG.info("[InteractiveConsoleServer] Serving default run data from classpath resource: {}",
                         ClasspathJsonHandler.JSON_RESOURCE);
            }

            // Load test YAML definition
            String yamlContent = null;
            if (args.length > 1)
            {
                final Path yamlPath = Paths.get(args[1]).toAbsolutePath();
                if (Files.exists(yamlPath))
                {
                    yamlContent = new String(Files.readAllBytes(yamlPath), StandardCharsets.UTF_8);
                }
            }
            else
            {
                final String yamlResource = "com/xceptance/neodymium/ai/console/run_example.yaml";
                try (final InputStream is = InteractiveConsoleServer.class.getClassLoader()
                                                                          .getResourceAsStream(yamlResource))
                {
                    if (is != null)
                    {
                        yamlContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            }

            // Start simulation loop combining YAML definition & JSON metrics
            if (jsonContent != null && yamlContent != null)
            {
                final String simJson = jsonContent;
                final String simYaml = yamlContent;
                final Thread simThread = new Thread(() -> runSimulation(engine, simYaml, simJson), "Console-Simulation");
                simThread.setDaemon(true);
                simThread.start();
            }
            else if (jsonContent != null)
            {
                // Fallback to json-only simulation if YAML is missing
                final String simJson = jsonContent;
                final Thread simThread = new Thread(() -> runSimulation(engine, simJson), "Console-Simulation");
                simThread.setDaemon(true);
                simThread.start();
            }

            srv.openBrowser();
            LOG.info("[InteractiveConsoleServer] Open your browser at: {}", srv.getLocalUrl());
            LOG.info("[InteractiveConsoleServer] Press Ctrl+C to stop.");

            // Keep running until killed
            Thread.currentThread().join();
        }
        catch (final Exception e)
        {
            LOG.error("[InteractiveConsoleServer] Fatal error: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Simulation using JSON only (fallback).
     */
    private static void runSimulation(final InteractiveConsoleEngine engine, final String jsonContent)
    {
        try
        {
            final Gson gson = new Gson();
            final JsonObject template = JsonParser.parseString(jsonContent).getAsJsonObject();
            runSimulationInternal(engine, template, template);
        }
        catch (final Exception e)
        {
            LOG.error("[Simulation] Error in json simulation: {}", e.getMessage(), e);
        }
    }

    /**
     * Simulation combining YAML structure and JSON mock execution results.
     */
    public static void runSimulation(final InteractiveConsoleEngine engine, final String yamlContent,
                              final String jsonContent)
    {
        try
        {
            final Yaml yaml = new Yaml();
            final Object yamlData = yaml.load(yamlContent);
            final String yamlJson = new Gson().toJson(yamlData);

            final JsonObject yamlObj = JsonParser.parseString(yamlJson).getAsJsonObject();
            final JsonObject jsonDatabase = JsonParser.parseString(jsonContent).getAsJsonObject();

            // Build the dynamic active state from YAML
            final JsonObject activeState = new JsonObject();
            if (jsonDatabase.has("runId"))
            {
                activeState.addProperty("runId", jsonDatabase.get("runId").getAsString());
            }

            parseYamlToSteps(yamlObj, activeState);

            runSimulationInternal(engine, activeState, jsonDatabase);
        }
        catch (final Exception e)
        {
            LOG.error("[Simulation] Error starting yaml+json simulation: {}", e.getMessage(), e);
        }
    }

    private static void parseYamlToSteps(final JsonObject yamlObj, final JsonObject activeState)
    {
        final JsonObject blocks = new JsonObject();
        final JsonArray beforeArray = new JsonArray();
        final JsonArray stepsArray = new JsonArray();
        final JsonArray afterArray = new JsonArray();

        blocks.add("before", beforeArray);
        blocks.add("steps", stepsArray);
        blocks.add("after", afterArray);

        activeState.add("blocks", blocks);

        // Parse dataset metadata from first data entry
        JsonObject dataEntry = null;
        if (yamlObj.has("data"))
        {
            final JsonArray dataArray = yamlObj.getAsJsonArray("data");
            if (dataArray.size() > 0)
            {
                dataEntry = dataArray.get(0).getAsJsonObject();
                if (dataEntry.has("testId"))
                {
                    activeState.add("testId", dataEntry.get("testId"));
                }

                final JsonObject bindings = new JsonObject();
                for (final Map.Entry<String, JsonElement> entry : dataEntry.entrySet())
                {
                    bindings.add(entry.getKey(), entry.getValue());
                }
                activeState.add("dataBindings", bindings);
            }
        }

        // Add JUnit5 tags
        final JsonArray junitTags = new JsonArray();
        junitTags.add("@AiValidated");
        junitTags.add("@Regression");
        junitTags.add("@CheckoutTest");
        activeState.add("junitTags", junitTags);

        // Populate source info for the "Test Info" panel
        activeState.addProperty("testFile", "com.xceptance.neodymium.tests.CheckoutFlowTest");
        activeState.addProperty("yamlSource", "src/test/resources/checkout/checkout-flow.yaml");
        activeState.addProperty("playbookFile", "src/test/resources/checkout/checkout-flow.json");

        // Parse lifecycle blocks passing dataEntry down to resolve placeholders
        parseYamlBlock(yamlObj.get("before"), beforeArray, "before", 0, dataEntry, new ArrayList<>());
        parseYamlBlock(yamlObj.get("steps"), stepsArray, "steps", 0, dataEntry, new ArrayList<>());
        parseYamlBlock(yamlObj.get("after"), afterArray, "after", 0, dataEntry, new ArrayList<>());
    }

    private static String resolvePlaceholders(final String instruction, final JsonObject dataEntry)
    {
        if (instruction == null || dataEntry == null)
        {
            return instruction;
        }

        String resolved = instruction;
        for (final Map.Entry<String, JsonElement> entry : dataEntry.entrySet())
        {
            final String placeholder = "${" + entry.getKey() + "}";
            if (resolved.contains(placeholder) && entry.getValue().isJsonPrimitive())
            {
                resolved = resolved.replace(placeholder, entry.getValue().getAsString());
            }
        }
        return resolved;
    }

    private static void parseYamlBlock(
                                       final JsonElement blockObj,
                                       final JsonArray targetArray,
                                       final String blockName,
                                       final int initialIncludeLevel,
                                       final JsonObject dataEntry,
                                       final List<String> includeChain)
    {
        if (blockObj == null)
        {
            return;
        }

        if (blockObj.isJsonArray())
        {
            for (final JsonElement item : blockObj.getAsJsonArray())
            {
                processStepItem(item, targetArray, blockName, initialIncludeLevel, dataEntry, includeChain);
            }
        }
        else if (blockObj.isJsonPrimitive() && blockObj.getAsJsonPrimitive().isString())
        {
            final String stepsStr = blockObj.getAsString();
            final String[] lines = stepsStr.split("\\r?\\n");
            for (final String line : lines)
            {
                final String trimmed = line.trim();
                if (trimmed.isEmpty())
                {
                    continue;
                }

                final JsonObject item = new JsonObject();
                item.addProperty("instruction", trimmed);
                processStepItem(item, targetArray, blockName, initialIncludeLevel, dataEntry, includeChain);
            }
        }
    }

    private static void processStepItem(
                                        final JsonElement item,
                                        final JsonArray targetArray,
                                        final String blockName,
                                        final int includeLevel,
                                        final JsonObject dataEntry,
                                        final List<String> includeChain)
    {
        if (item.isJsonObject())
        {
            final JsonObject obj = item.getAsJsonObject();
            if (obj.has("_include"))
            {
                final String includeFile = obj.get("_include").getAsString();
                final List<String> nextChain = new ArrayList<>(includeChain);
                nextChain.add(includeFile);
                resolveInclude(includeFile, targetArray, blockName, includeLevel + 1, dataEntry, nextChain);
            }
            else if (obj.has("instruction"))
            {
                final String raw = obj.get("instruction").getAsString().trim();
                if (raw.startsWith("_include:"))
                {
                    final String includeFile = raw.substring(9).trim();
                    final List<String> nextChain = new ArrayList<>(includeChain);
                    nextChain.add(includeFile);
                    resolveInclude(includeFile, targetArray, blockName, includeLevel + 1, dataEntry, nextChain);
                }
                else
                {
                    obj.addProperty("instruction", raw);
                    obj.addProperty("includeLevel", includeLevel);
                    if (!obj.has("source"))
                    {
                        obj.addProperty("source", "playbook");
                    }
                    final JsonArray chainArr = new JsonArray();
                    for (final String s : includeChain)
                    {
                        chainArr.add(s);
                    }
                    obj.add("includeChain", chainArr);
                    targetArray.add(obj);
                }
            }
        }
        else if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isString())
        {
            final String str = item.getAsString().trim();
            if (str.startsWith("_include:"))
            {
                final String includeFile = str.substring(9).trim();
                final List<String> nextChain = new ArrayList<>(includeChain);
                nextChain.add(includeFile);
                resolveInclude(includeFile, targetArray, blockName, includeLevel + 1, dataEntry, nextChain);
            }
            else
            {
                final JsonObject step = new JsonObject();
                step.addProperty("instruction", str);
                step.addProperty("source", "playbook");
                step.addProperty("includeLevel", includeLevel);
                final JsonArray chainArr = new JsonArray();
                for (final String s : includeChain)
                {
                    chainArr.add(s);
                }
                step.add("includeChain", chainArr);
                targetArray.add(step);
            }
        }
    }

    private static void resolveInclude(
                                       final String includeFile,
                                       final JsonArray targetArray,
                                       final String blockName,
                                       final int includeLevel,
                                       final JsonObject dataEntry,
                                       final List<String> includeChain)
    {
        final String resourcePath = "com/xceptance/neodymium/ai/console/" + includeFile;
        try (final InputStream is = InteractiveConsoleServer.class.getClassLoader().getResourceAsStream(resourcePath))
        {
            if (is != null)
            {
                final String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                final Yaml yaml = new Yaml();
                final Object loaded = yaml.load(content);
                final String jsonStr = new Gson().toJson(loaded);
                final JsonObject parsed = JsonParser.parseString(jsonStr).getAsJsonObject();

                if (parsed.has("steps"))
                {
                    parseYamlBlock(parsed.get("steps"), targetArray, blockName, includeLevel, dataEntry, includeChain);
                }
                else
                {
                    parseYamlBlock(parsed, targetArray, blockName, includeLevel, dataEntry, includeChain);
                }
            }
            else
            {
                LOG.error("Include file not found on classpath: {}", resourcePath);
            }
        }
        catch (final Exception e)
        {
            LOG.error("Failed to parse include file: {}", includeFile, e);
        }
    }

    private static void runSimulationInternal(
                                              final InteractiveConsoleEngine engine,
                                              final JsonObject stateToDrive,
                                              final JsonObject database)
    {
        try
        {
            final Gson gson = new Gson();
            final JsonObject activeState = stateToDrive.deepCopy();
            final List<JsonObject> allSteps = new ArrayList<>();

            // Rebuild/clean blocks structures
            ensureBlocksExist(activeState);
            final JsonObject blocks = activeState.getAsJsonObject("blocks");

            prepareStepsBlock(blocks.getAsJsonArray("before"), allSteps);
            prepareStepsBlock(blocks.getAsJsonArray("steps"), allSteps);
            prepareStepsBlock(blocks.getAsJsonArray("after"), allSteps);

            // Assign sequential indices to the activeState blocks
            int seqIdx = 0;
            for (final JsonObject step : allSteps)
            {
                step.addProperty("index", seqIdx++);
            }

            int stepIndex = 0;
            while (stepIndex >= 0 && stepIndex < allSteps.size())
            {
                final JsonObject step = allSteps.get(stepIndex);
                final int originalIndex = step.get("index").getAsInt();

                // Mark current step as running (thinking state)
                step.addProperty("status", "running");
                step.remove("actions");
                step.remove("reasoning");
                activeState.addProperty("reasoning", "AI is thinking...");
                activeState.addProperty("reasoningFailed", false);

                // Reset subsequent steps to pending
                for (int i = stepIndex + 1; i < allSteps.size(); i++)
                {
                    allSteps.get(i).addProperty("status", "pending");
                    allSteps.get(i).remove("errorMessage");
                    allSteps.get(i).remove("actions");
                    allSteps.get(i).remove("screenshot");
                    allSteps.get(i).remove("durationMs");
                    allSteps.get(i).remove("reasoning");
                }

                engine.pushState(gson.toJson(activeState));

                // Retrieve template step to check for playbook source / fail status
                final JsonObject templateStep = findTemplateStep(database, originalIndex);
                boolean isPlaybook = false;
                boolean isFailed = false;
                if (templateStep != null)
                {
                    if (templateStep.has("source") && "playbook".equals(templateStep.get("source").getAsString()))
                    {
                        isPlaybook = true;
                    }
                    if (templateStep.has("status") && "failed".equals(templateStep.get("status").getAsString()))
                    {
                        isFailed = true;
                    }
                }
                else
                {
                    isPlaybook = true;
                }

                // Retrieve mock details from execution database (done thinking state)
                if (templateStep != null)
                {
                    if (templateStep.has("actions"))
                    {
                        step.add("actions", templateStep.get("actions"));
                    }
                    if (templateStep.has("reasoning"))
                    {
                        step.add("reasoning", templateStep.get("reasoning"));
                        activeState.add("reasoning", templateStep.get("reasoning"));
                    }
                    else
                    {
                        activeState.addProperty("reasoning", "AI planned action(s). Please review and approve.");
                    }
                    if (templateStep.has("thinkingTimeMs"))
                    {
                        step.add("thinkingTimeMs", templateStep.get("thinkingTimeMs"));
                    }
                    else
                    {
                        step.addProperty("thinkingTimeMs", (!isPlaybook || isFailed) ? 1500 : 0);
                    }
                    if (templateStep.has("inputTokens"))
                    {
                        step.add("inputTokens", templateStep.get("inputTokens"));
                    }
                    if (templateStep.has("outputTokens"))
                    {
                        step.add("outputTokens", templateStep.get("outputTokens"));
                    }
                    if (templateStep.has("simplifiedDom"))
                    {
                        step.add("simplifiedDom", templateStep.get("simplifiedDom"));
                    }
                    if (templateStep.has("screenshot"))
                    {
                        step.add("screenshot", templateStep.get("screenshot"));
                    }
                }
                else
                {
                    activeState.addProperty("reasoning", "No actions planned for this step.");
                }

                engine.pushState(gson.toJson(activeState));

                // Wait for user interaction
                final JsonObject response = engine.waitForAction();
                final String action = response.has("action") ? response.get("action").getAsString() : "RUN";

                if ("RUN".equals(action))
                {
                    if (templateStep != null)
                    {
                        step.addProperty("status",
                                         templateStep.has("status") ? templateStep.get("status").getAsString() : "passed");
                        if (templateStep.has("actions"))
                        {
                            step.add("actions", templateStep.get("actions"));
                        }
                        if (templateStep.has("screenshot"))
                        {
                            step.add("screenshot", templateStep.get("screenshot"));
                        }
                        if (templateStep.has("errorMessage"))
                        {
                            step.add("errorMessage", templateStep.get("errorMessage"));
                        }
                        if (templateStep.has("durationMs"))
                        {
                            step.add("durationMs", templateStep.get("durationMs"));
                        }
                        if (templateStep.has("reasoning"))
                        {
                            step.add("reasoning", templateStep.get("reasoning"));
                        }
                        if (templateStep.has("thinkingTimeMs"))
                        {
                            step.add("thinkingTimeMs", templateStep.get("thinkingTimeMs"));
                        }
                        if (templateStep.has("inputTokens"))
                        {
                            step.add("inputTokens", templateStep.get("inputTokens"));
                        }
                        if (templateStep.has("outputTokens"))
                        {
                            step.add("outputTokens", templateStep.get("outputTokens"));
                        }
                        if (templateStep.has("simplifiedDom"))
                        {
                            step.add("simplifiedDom", templateStep.get("simplifiedDom"));
                        }

                        // Simulate variable storage for 'store' actions
                        if (templateStep.has("actions"))
                        {
                            final JsonArray actions = templateStep.getAsJsonArray("actions");
                            for (final JsonElement a : actions)
                            {
                                if (a.isJsonObject())
                                {
                                    final JsonObject ao = a.getAsJsonObject();
                                    if ("store".equals(ao.get("type").getAsString()) && ao.has("variable")
                                        && ao.has("value"))
                                    {
                                        if (!activeState.has("storedVariables"))
                                        {
                                            activeState.add("storedVariables", new JsonObject());
                                        }
                                        final JsonObject stored = activeState.getAsJsonObject("storedVariables");
                                        stored.add(ao.get("variable").getAsString(), ao.get("value"));
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        step.addProperty("status", "passed");
                    }

                    final String status = step.get("status").getAsString();
                    if ("failed".equals(status))
                    {
                        activeState.addProperty("autoRun", false);
                        activeState.addProperty("reasoning",
                                                database.has("reasoning") ? database.get("reasoning").getAsString()
                                                                          : "Execution stopped: step failed.");
                        activeState.addProperty("reasoningFailed", true);
                        engine.pushState(gson.toJson(activeState));

                        LOG.info("[Simulation] Step failed. Awaiting user action to recover.");
                        final JsonObject failResponse = engine.waitForAction();
                        final String failAction = failResponse.has("action") ? failResponse.get("action").getAsString()
                                                                             : "SAVE_EXIT";

                        if ("REWIND".equals(failAction))
                        {
                            int targetIdx = stepIndex;
                            if (failResponse.has("targetIndex"))
                            {
                                targetIdx = failResponse.get("targetIndex").getAsInt();
                            }
                            else if (stepIndex > 0)
                            {
                                targetIdx = stepIndex - 1;
                            }

                            if (targetIdx >= 0 && targetIdx < allSteps.size())
                            {
                                stepIndex = targetIdx;
                                for (int i = targetIdx; i < allSteps.size(); i++)
                                {
                                    allSteps.get(i).addProperty("status", "pending");
                                    allSteps.get(i).remove("errorMessage");
                                    allSteps.get(i).remove("actions");
                                    allSteps.get(i).remove("screenshot");
                                    allSteps.get(i).remove("durationMs");
                                    allSteps.get(i).remove("reasoning");
                                }
                            }
                        }
                        else if ("SKIP".equals(failAction))
                        {
                            step.addProperty("status", "skipped");
                            if (templateStep != null && templateStep.has("reasoning"))
                            {
                                step.add("reasoning", templateStep.get("reasoning"));
                            }
                            stepIndex++;
                        }
                        else if ("RUN".equals(failAction))
                        {
                            step.addProperty("status", "pending");
                            step.remove("errorMessage");
                            step.remove("actions");
                            step.remove("reasoning");
                        }
                        else if ("SAVE_EXIT".equals(failAction))
                        {
                            LOG.info("[Simulation] Exiting simulation after failed step.");
                            break;
                        }
                        continue;
                    }

                    stepIndex++;
                }
                else if ("SKIP".equals(action))
                {
                    step.addProperty("status", "skipped");
                    if (templateStep != null && templateStep.has("reasoning"))
                    {
                        step.add("reasoning", templateStep.get("reasoning"));
                    }
                    stepIndex++;
                }
                else if ("REWIND".equals(action))
                {
                    int targetIdx = stepIndex;
                    if (response.has("targetIndex"))
                    {
                        targetIdx = response.get("targetIndex").getAsInt();
                    }
                    else if (stepIndex > 0)
                    {
                        targetIdx = stepIndex - 1;
                    }

                    if (targetIdx >= 0 && targetIdx < allSteps.size())
                    {
                        stepIndex = targetIdx;
                        for (int i = targetIdx; i < allSteps.size(); i++)
                        {
                            allSteps.get(i).addProperty("status", "pending");
                            allSteps.get(i).remove("errorMessage");
                            allSteps.get(i).remove("actions");
                            allSteps.get(i).remove("screenshot");
                            allSteps.get(i).remove("durationMs");
                            allSteps.get(i).remove("reasoning");
                        }
                    }
                }
                else if ("EDIT".equals(action))
                {
                    final int targetIdx = response.get("index").getAsInt();
                    final String instruction = response.get("instruction").getAsString();

                    final JsonObject blocksObj = activeState.getAsJsonObject("blocks");
                    if (blocksObj != null)
                    {
                        for (final String bKey : new String[]
                        {
                          "before", "steps", "after"
                        })
                        {
                            final JsonArray array = blocksObj.getAsJsonArray(bKey);
                            if (array != null)
                            {
                                for (final JsonElement el : array)
                                {
                                    final JsonObject stepObj = el.getAsJsonObject();
                                    if (stepObj.has("index") && stepObj.get("index").getAsInt() == targetIdx)
                                    {
                                        stepObj.addProperty("instruction", instruction);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    for (final JsonObject s : allSteps)
                    {
                        if (s.has("index") && s.get("index").getAsInt() == targetIdx)
                        {
                            s.addProperty("instruction", instruction);
                            break;
                        }
                    }
                }
                else if ("ADD".equals(action))
                {
                    final String blockName = response.has("block") ? response.get("block").getAsString() : "steps";
                    final String instruction = response.get("instruction").getAsString();

                    final JsonObject blocksObj = activeState.getAsJsonObject("blocks");
                    if (blocksObj != null && blocksObj.has(blockName))
                    {
                        final JsonArray array = blocksObj.getAsJsonArray(blockName);
                        final JsonObject newStep = new JsonObject();
                        newStep.addProperty("instruction", instruction);
                        newStep.addProperty("status", "pending");
                        newStep.addProperty("source", "llm");
                        newStep.addProperty("includeLevel", 0);
                        newStep.add("actions", new JsonArray());

                        array.add(newStep);
                    }

                    allSteps.clear();
                    final JsonObject blocksObjRef = activeState.getAsJsonObject("blocks");
                    if (blocksObjRef != null)
                    {
                        for (final String bKey : new String[]
                        {
                          "before", "steps", "after"
                        })
                        {
                            final JsonArray arr = blocksObjRef.getAsJsonArray(bKey);
                            if (arr != null)
                            {
                                for (final JsonElement el : arr)
                                {
                                    allSteps.add(el.getAsJsonObject());
                                }
                            }
                        }
                    }

                    int idx = 0;
                    for (final JsonObject s : allSteps)
                    {
                        s.addProperty("index", idx++);
                    }

                    for (int i = 0; i < allSteps.size(); i++)
                    {
                        if ("running".equals(allSteps.get(i).get("status").getAsString()))
                        {
                            stepIndex = i;
                            break;
                        }
                    }
                }
                else if ("REORDER".equals(action))
                {
                    final String fromBlock = response.has("fromBlock") ? response.get("fromBlock").getAsString()
                                                                       : response.get("block").getAsString();
                    final int fromIndex = response.get("fromIndex").getAsInt();
                    final String toBlock = response.has("toBlock") ? response.get("toBlock").getAsString() : fromBlock;
                    final int toIndex = response.get("toIndex").getAsInt();

                    final JsonObject blocksObj = activeState.getAsJsonObject("blocks");
                    if (blocksObj != null && blocksObj.has(fromBlock) && blocksObj.has(toBlock))
                    {
                        final JsonArray fromArray = blocksObj.getAsJsonArray(fromBlock);
                        final JsonArray toArray = blocksObj.getAsJsonArray(toBlock);

                        if (fromIndex >= 0 && fromIndex < fromArray.size())
                        {
                            final JsonElement element = fromArray.remove(fromIndex);

                            // Adjust target index if moving within the same block forward
                            int adjustedToIndex = toIndex;
                            if (fromBlock.equals(toBlock) && adjustedToIndex > fromIndex)
                            {
                                adjustedToIndex--;
                            }

                            // Ensure toIndex is within bounds for the target array (now potentially empty
                            // or different)
                            if (adjustedToIndex < 0)
                                adjustedToIndex = 0;
                            if (adjustedToIndex > toArray.size())
                                adjustedToIndex = toArray.size();

                            final JsonArray newToArray = new JsonArray();
                            for (int i = 0; i < toArray.size(); i++)
                            {
                                if (i == adjustedToIndex)
                                {
                                    newToArray.add(element);
                                }
                                newToArray.add(toArray.get(i));
                            }
                            if (adjustedToIndex >= toArray.size())
                            {
                                newToArray.add(element);
                            }
                            blocksObj.add(toBlock, newToArray);
                        }
                    }

                    allSteps.clear();
                    final JsonObject blocksObjRef = activeState.getAsJsonObject("blocks");
                    if (blocksObjRef != null)
                    {
                        for (final String bKey : new String[]
                        {
                          "before", "steps", "after"
                        })
                        {
                            final JsonArray arr = blocksObjRef.getAsJsonArray(bKey);
                            if (arr != null)
                            {
                                for (final JsonElement el : arr)
                                {
                                    allSteps.add(el.getAsJsonObject());
                                }
                            }
                        }
                    }

                    int idx = 0;
                    for (final JsonObject s : allSteps)
                    {
                        s.addProperty("index", idx++);
                    }

                    for (int i = 0; i < allSteps.size(); i++)
                    {
                        if ("running".equals(allSteps.get(i).get("status").getAsString()))
                        {
                            stepIndex = i;
                            break;
                        }
                    }
                }
                else if ("SAVE_EXIT".equals(action))
                {
                    LOG.info("[Simulation] Received stop signal. Exiting simulation.");
                    break;
                }
            }

            // Final state push
            engine.pushState(gson.toJson(activeState));
        }
        catch (final Exception e)
        {
            LOG.error("[Simulation] Error in simulation loop: {}", e.getMessage(), e);
        }
    }

    private static void ensureBlocksExist(final JsonObject state)
    {
        if (!state.has("blocks"))
        {
            state.add("blocks", new JsonObject());
        }
        final JsonObject blocks = state.getAsJsonObject("blocks");
        if (!blocks.has("before"))
        {
            blocks.add("before", new JsonArray());
        }
        if (!blocks.has("steps"))
        {
            blocks.add("steps", new JsonArray());
        }
        if (!blocks.has("after"))
        {
            blocks.add("after", new JsonArray());
        }
    }

    private static void prepareStepsBlock(final JsonArray block, final List<JsonObject> allSteps)
    {
        if (block != null)
        {
            for (final JsonElement el : block)
            {
                final JsonObject step = el.getAsJsonObject();
                step.addProperty("status", "pending");
                step.remove("actions");
                step.remove("screenshot");
                step.remove("errorMessage");
                step.remove("durationMs");
                step.remove("reasoning");
                allSteps.add(step);
            }
        }
    }

    private static JsonObject findTemplateStep(final JsonObject template, final int index)
    {
        final JsonObject blocks = template.getAsJsonObject("blocks");
        if (blocks != null)
        {
            JsonObject step = findStepInArray(blocks.getAsJsonArray("before"), index);
            if (step != null)
                return step;
            step = findStepInArray(blocks.getAsJsonArray("steps"), index);
            if (step != null)
                return step;
            step = findStepInArray(blocks.getAsJsonArray("after"), index);
            if (step != null)
                return step;
        }
        return null;
    }

    private static JsonObject findStepInArray(final JsonArray array, final int index)
    {
        if (array != null)
        {
            for (final JsonElement el : array)
            {
                final JsonObject step = el.getAsJsonObject();
                if (step.has("index") && step.get("index").getAsInt() == index)
                {
                    return step;
                }
            }
        }
        return null;
    }
}
