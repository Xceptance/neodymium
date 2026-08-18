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

import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neodymium.ai.config.AiConfiguration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.URLEncoder;

/**
 * Core engine for the Interactive Console.
 * 
 * <p>
 * Exposes two HTTP endpoints intended to be registered on either an
 * {@link InteractiveConsoleServer} (standalone mode) or the Neodymium Aura Manager's
 * embedded {@link HttpServer} (managed mode):
 * </p>
 * 
 * <ul>
 * <li>{@code GET /api/console/events} — Server-Sent Events (SSE) stream pushing
 * live state JSON to all connected browser clients. Clients should open this with
 * {@code new EventSource('/api/console/events')}.</li>
 * <li>{@code POST /api/console/action} — Receives a user-triggered command from
 * a browser client. The body must be a JSON object containing at minimum:
 * {@code "runId"}, {@code "pauseId"}, and {@code "action"}.</li>
 * </ul>
 *
 * <h2>Multi-tab / Multi-device Safety</h2>
 * 
 * <p>
 * Every time the Java test runner pauses and calls {@link #waitForAction(long)},
 * a fresh {@code pauseId} UUID is broadcast to
 * all SSE clients. The first POST
 * carrying the correct {@code runId} and {@code pauseId} wins; duplicate POSTs
 * for the same pause are silently rejected with HTTP 200 (action already
 * handled).
 * POSTs for a different {@code runId} are rejected with HTTP 409 and a
 * stale-tab
 * hint the browser UI can render as a warning.
 * </p>
 *
 * @author AI-generated: Claude Sonnet 4.5
 * @author AI-generated: Antigravity
 * 
 * @author Xceptance GmbH 2026
 */
public final class InteractiveConsoleEngine {
    private static final Logger LOG = LoggerFactory.getLogger(InteractiveConsoleEngine.class);

    private static final long DEFAULT_TIMEOUT_MS = TimeUnit.HOURS.toMillis(1);
    private static final Gson GSON = new Gson();

    /** A unique identifier for the currently active test run. */

    private String runId;
    /**
     * The UUID generated on each pause, consumed by the first valid POST action.
     * {@code null} when not paused.
     */
    private final AtomicReference<String> currentPauseId = new AtomicReference<>(null);

    /** The actual LAN IP of this machine, detected on startup. */
    private final String lanIp;

    /**
     * The pending action deposited by a POST handler, picked up by the waiting
     * test-runner thread. {@code null} while paused and awaiting input.
     */
    private final AtomicReference<JsonObject> pendingAction = new AtomicReference<>(null);

    /**
     * The current state snapshot that is broadcast to SSE clients.
     * Updated by the test runner before each pause.
     */
    private volatile String currentStateJson = "{}";

    /** Active SSE client output streams. */
    private final CopyOnWriteArrayList<OutputStream> sseClients = new CopyOnWriteArrayList<>();

    /**
     * Lock object for {@link Object#wait(long)} / {@link Object#notifyAll()}.
     * The test-runner thread waits on this; POST handlers notify it.
     */
    private final Object lock = new Object();

    /**
     * Creates a new engine for the given run ID.
     *
     * @param runId a unique identifier for the test run (e.g.
     *              {@code "run_2026-06-29_10-00-00"})
     */
    public InteractiveConsoleEngine(final String runId) {
        this.runId = runId;

        String detectedIp = "localhost";
        try {
            final InetAddress addr = getActualLocalIP();
            if (addr != null) {
                detectedIp = addr.getHostAddress();
            }
        } catch (final SocketException e) {
            LOG.warn("Failed to detect LAN IP for Interactive Console: {}", e.getMessage());
        }
        this.lanIp = detectedIp;
    }

    // -------------------------------------------------------------------------
    // Public API — called by the test runner
    // -------------------------------------------------------------------------

    /**
     * Returns the run ID this engine was created for.
     *
     * @return the run ID
     */
    public String getRunId() {
        return this.runId;
    }

    public void setRunId(final String runId) {
        this.runId = runId;
    }

    /**
     * Returns the detected LAN IP of this machine.
     *
     * @return the LAN IP string
     */

    public String getLanIp() {
        return this.lanIp;
    }

    /**
     * Pushes a new state snapshot to all connected SSE clients.
     * Call this whenever the test runner's state changes (new step started,
     * step completed, error, etc.) so the UI updates instantly.
     *
     * 
     * @param stateJson the full current state as a JSON string
     */
    public void pushState(final String stateJson) {
        String minified;
        try {
            final JsonObject state = JsonParser.parseString(stateJson).getAsJsonObject();

            // Inject the LAN IP so the UI can use it for QR codes/links
            state.addProperty("lanIp", this.lanIp);

            if (state.has("pauseId") && !state.get("pauseId").isJsonNull())
            {
                final String pid = state.get("pauseId").getAsString();
                if (pid != null && !pid.isEmpty())
                {
                    this.currentPauseId.set(pid);
                }
            }
            else if (this.currentPauseId.get() != null)
            {
                state.addProperty("pauseId", this.currentPauseId.get());
            }

            minified = GSON.toJson(state);
        } catch (final Exception e) {
            minified = stateJson.replace("\r", "").replace("\n", "");
        }
        
        this.currentStateJson = minified;
        
        if (AiConfiguration.getInstance().isManagerActive())
        {
            try
            {
                final String managerUrl = AiConfiguration.getInstance().getProperty("neodymium.managerUrl", null);
                final HttpClient client = HttpClient.newHttpClient();
                final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(managerUrl + "/api/console/internal/pushState"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(minified, StandardCharsets.UTF_8))
                    .build();
                final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200)
                {
                    final JsonObject respJson = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (respJson.has("status") && "stopped".equals(respJson.get("status").getAsString()))
                    {
                        LOG.warn("[InteractiveConsole] Aura Manager requested stop. Exiting JVM...");
                        System.exit(0);
                    }
                }
            }
            catch (final Exception e)
            {
                LOG.error("Failed to proxy state to Aura Manager", e);
            }
            return;
        }

        broadcastSseEvent("state", minified);
    }
    
    public String getCurrentStateJson() {
        return this.currentStateJson;
    }

    /**
     * Blocks the calling thread (the test-runner thread) until a valid action
     * is received from a browser client, or until the timeout expires.
     *
     * <p>Before blocking, this method:</p>
     * <ol>
     *   <li>Generates a fresh {@code pauseId} UUID.</li>
     *   <li>Augments the current state JSON with the new {@code pauseId} and
     *       broadcasts it to all SSE clients so they can include it in their
     *       subsequent POST requests.</li>
     * </ol>
     /**
     * Pre-registers a pause token to avoid race conditions where the UI acts 
     * on the state SSE event before waitForAction is fully invoked.
     * @param pauseId the expected pause token
     */
    public void registerPauseId(final String pauseId)
    {
        this.currentPauseId.set(pauseId);
        this.pendingAction.set(null);
    }

    /**
     * Returns the active pause token, or null if not paused.
     *
     * @return the active pause token string, or null
     */
    public String getCurrentPauseId()
    {
        return this.currentPauseId.get();
    }

    /**
     * Blocks the current thread until an action is received from the UI.
     * Sends a Server-Sent Event (SSE) indicating the runner is paused.
     *
     * @param timeoutMs maximum time to wait in milliseconds; use
     *                  {@link #DEFAULT_TIMEOUT_MS} for the default 1-hour timeout
     * @return the raw {@link JsonObject} received from the POST body, never {@code null}
     * @throws InterruptedException if the thread is interrupted while waiting
     * @throws RuntimeException     if no action is received within the timeout
     */
    public JsonObject waitForAction(final String customPauseId, final long timeoutMs) throws InterruptedException
    {
        if (AiConfiguration.getInstance().isManagerActive())
        {
            try
            {
                final String managerUrl = AiConfiguration.getInstance().getProperty("neodymium.managerUrl", null);
                final HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
                String uriStr = managerUrl + "/api/console/internal/waitForAction";
                if (customPauseId != null && !customPauseId.isEmpty())
                {
                    uriStr += "?pauseId=" + URLEncoder.encode(customPauseId, StandardCharsets.UTF_8);
                }
                final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uriStr))
                    .timeout(java.time.Duration.ofMillis(timeoutMs + 5000))
                    .GET()
                    .build();
                LOG.info("[InteractiveConsole] Proxying waitForAction to Manager at {}", managerUrl);
                final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200)
                {
                    final JsonObject action = JsonParser.parseString(response.body()).getAsJsonObject();
                    LOG.info("[InteractiveConsole] Action received from Manager: {}", action);
                    return action;
                }
                else
                {
                    throw new RuntimeException("Manager returned error status: " + response.statusCode());
                }
            }
            catch (InterruptedException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new RuntimeException("Failed to proxy waitForAction to Aura Manager", e);
            }
        }

        // Generate a fresh pause token and clear any stale pending action,
        // UNLESS the pauseId was already pre-registered.
        final String pauseId = (customPauseId != null && !customPauseId.isEmpty()) ? customPauseId : UUID.randomUUID().toString();
        if (!pauseId.equals(this.currentPauseId.get()))
        {
            this.currentPauseId.set(pauseId);
            this.pendingAction.set(null);
        }

        // Broadcast the updated state including the new pauseId so all tabs know they can act.
        broadcastSseEvent("pause", buildPausePayload(pauseId));

        LOG.info("[InteractiveConsole] Paused — waiting for user action (runId={}, pauseId={})", this.runId, pauseId);

        synchronized (this.lock)
        {
            final long deadline = System.currentTimeMillis() + timeoutMs;
            while (this.pendingAction.get() == null)
            {
                final long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                {
                    throw new RuntimeException(
                        "Interactive Console: no user action received within " + timeoutMs + " ms. Halting execution.");
                }
                this.lock.wait(Math.min(remaining, 5_000));
            }
        }

        // Invalidate the pause token so duplicate clicks are ignored.
        this.currentPauseId.set(null);

        final JsonObject action = this.pendingAction.getAndSet(null);
        LOG.info("[InteractiveConsole] Action received: {}", action);
        return action;
    }

    public JsonObject waitForAction(final String customPauseId) throws InterruptedException
    {
        return waitForAction(customPauseId, DEFAULT_TIMEOUT_MS);
    }

    public JsonObject waitForAction(final long timeoutMs) throws InterruptedException
    {
        return waitForAction(null, timeoutMs);
    }

    public JsonObject waitForAction() throws InterruptedException
    {
        return waitForAction(null, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Forcefully deposits an abort action and wakes up any waiting threads.
     *
     * @author AI-generated: Antigravity
     * @author Xceptance GmbH 2026
     */
    public void abort()
    {
        final JsonObject abortAction = new JsonObject();
        abortAction.addProperty("action", "ABORT");
        this.pendingAction.set(abortAction);
        synchronized (this.lock)
        {
            this.lock.notifyAll();
        }
    }

    // -------------------------------------------------------------------------
    // HTTP Handler Factories — register these on your HttpServer
    // -------------------------------------------------------------------------

    /**
     * Returns an {@link HttpHandler} for the SSE endpoint ({@code GET /api/console/events}).
     * <p>
     * Register this on your server:
     * </p>
     * 
     * <pre>
     * server.createContext("/api/console/events", engine.createSseHandler());
     * </pre>
     *
     * @return the SSE handler
     */
    public HttpHandler createSseHandler()
    {
        return new SseHandler();
    }

    /**
     * Returns an {@link HttpHandler} for the action endpoint ({@code POST /api/console/action}).
     * <p>
     * Register this on your server:
     * </p>
     * 
     * <pre>
     * server.createContext("/api/console/action", engine.createActionHandler());
     * </pre>
     *
     * @return the action POST handler
     */
    public HttpHandler createActionHandler()
    {
        return new ActionHandler();
    }

    // -------------------------------------------------------------------------
    // SSE broadcasting
    // -------------------------------------------------------------------------

    /**
     * Broadcasts a single SSE event to all currently connected clients.
     * Disconnected clients are silently removed from the list.
     *
     * @param eventName the SSE {@code event:} field value
     * @param payloadJson      the SSE {@code data:} field value (typically a JSON string)
     */
    public void broadcastSseEvent(final String eventName, final String payloadJson)
    {
        if (AiConfiguration.getInstance().isManagerActive()) {
            try {
                final String managerUrl = AiConfiguration.getInstance().getProperty("neodymium.managerUrl", null);
                final JsonObject envelope = new JsonObject();
                envelope.addProperty("event", eventName);
                envelope.addProperty("payload", payloadJson);
                
                final HttpClient client = HttpClient.newHttpClient();
                final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(managerUrl + "/api/console/internal/broadcast"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(envelope), StandardCharsets.UTF_8))
                    .build();
                client.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                LOG.error("Failed to proxy SSE broadcast to Aura Manager", e);
            }
            return;
        }

        final String sseFrame = "event: " + eventName + "\ndata: " + payloadJson + "\n\n";
        final byte[] bytes = sseFrame.getBytes(StandardCharsets.UTF_8);

        final List<OutputStream> dead = new ArrayList<>();
        for (final OutputStream out : this.sseClients)
        {
            try
            {
                synchronized (out) {
                    out.write(bytes);
                    out.flush();
                }
            }
            catch (final IOException e)
            {
                dead.add(out);
            }
        }
        this.sseClients.removeAll(dead);
    }

    public record ActionResult(int statusCode, String responseBody)
    {
    }

    /**
     * Submits an action to the engine and deposits it to wake up any blocked test runner thread.
     *
     * @param req JSON object containing runId, pauseId, action, etc.
     * @return ActionResult containing HTTP status code and response body JSON
     * @author AI-generated: Antigravity
     * @author Xceptance GmbH 2026
     */
    public ActionResult submitAction(final JsonObject req)
    {
        final String incomingRunId = req != null && req.has("runId") && !req.get("runId").isJsonNull()
                ? req.get("runId").getAsString()
                : null;
        if (incomingRunId != null && !this.runId.equals(incomingRunId))
        {
            final String hint = "{\"error\":\"stale-tab\",\"activeRunId\":\"" + this.runId
                    + "\",\"message\":\"This tab is connected to an old test run. Refresh to the current run.\"}";
            return new ActionResult(409, hint);
        }

        final String incomingPauseId = req != null && req.has("pauseId") && !req.get("pauseId").isJsonNull()
                ? req.get("pauseId").getAsString()
                : null;
        final String activePauseId = this.currentPauseId.get();

        if (activePauseId == null || !activePauseId.equals(incomingPauseId))
        {
            LOG.info("[InteractiveConsole] Action rejected: inactive or mismatched pauseId. activePauseId={}, incomingPauseId={}",
                    activePauseId, incomingPauseId);
            return new ActionResult(200, "{\"status\":\"already-handled\"}");
        }

        LOG.info("[InteractiveConsole] Action accepted: pauseId={}, req={}", incomingPauseId, req);
        this.pendingAction.set(req);
        synchronized (this.lock)
        {
            this.lock.notifyAll();
        }

        return new ActionResult(200, "{\"status\":\"accepted\"}");
    }

    /**
     * Registers an SSE output stream to receive live events.
     *
     * @param out output stream
     * @author AI-generated: Antigravity
     * @author Xceptance GmbH 2026
     */
    public void addSseClient(final OutputStream out)
    {
        if (out != null)
        {
            this.sseClients.add(out);
        }
    }

    /**
     * Unregisters an SSE output stream.
     *
     * @param out output stream
     * @author AI-generated: Antigravity
     * @author Xceptance GmbH 2026
     */
    public void removeSseClient(final OutputStream out)
    {
        if (out != null)
        {
            this.sseClients.remove(out);
        }
    }

    /**
     * Builds the JSON payload broadcast to all SSE clients when the runner pauses.
     *
     * @param pauseId the freshly generated pause token
     * @return a compact JSON string containing {@code runId} and {@code pauseId}
     */
    public String buildPausePayload(final String pauseId)
    {
        return "{\"runId\":\"" + this.runId + "\",\"pauseId\":\"" + pauseId + "\"}";
    }

    // -------------------------------------------------------------------------
    // Inner handler classes
    // -------------------------------------------------------------------------

    /**
     * HTTP handler that opens a persistent SSE connection for a browser client and
     * keeps it alive, flushing any state updates immediately.
     */
    private final class SseHandler implements HttpHandler
    {
        @Override
        public void handle(final HttpExchange exchange) throws IOException
        {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod()))
            {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod()))
            {
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=UTF-8");
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            exchange.sendResponseHeaders(200, 0); // 0 = chunked / streaming

            final OutputStream out = exchange.getResponseBody();
            sseClients.add(out);

            try
            {
                synchronized (out) {
                    // Immediately send the current state so the client is up-to-date on connect.
                    final String initial = "event: state\ndata: " + currentStateJson + "\n\n";
                    out.write(initial.getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    // If a pause is already active, also send the pause event so a late-joining
                    // tab immediately knows the token it should use.
                    final String activePauseId = currentPauseId.get();
                    if (activePauseId != null)
                    {
                        final String pause = "event: pause\ndata: " + buildPausePayload(activePauseId) + "\n\n";
                        out.write(pause.getBytes(StandardCharsets.UTF_8));
                        out.flush();
                    }
                }

                // Keep the connection open until the client disconnects (write will throw).
                while (true)
                {
                    // Send a keep-alive comment every 15 s to prevent proxy timeouts.
                    Thread.sleep(15_000);
                    synchronized (out) {
                        out.write(": keep-alive\n\n".getBytes(StandardCharsets.UTF_8));
                        out.flush();
                    }
                }
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            catch (final IOException e)
            {
                // Client disconnected — normal, just clean up.
            }
            finally
            {
                sseClients.remove(out);
                try
                {
                    out.close();
                }
                catch (final IOException ignored)
                {
                }
            }
        }
    }

    /**
     * HTTP handler that receives a user action from a browser client via POST.
     *
     * <p>
     * Expected JSON body:
     * </p>
     * 
     * <pre>
     * {
     *   "runId":  "run_2026-06-29_...",   // must match the engine's runId
     *   "pauseId": "uuid-...",            // must match the current pause token
     *   "action": "RUN",                 // one of the DebugAction enum values
     *   ...                              // any additional action-specific fields
     * }
     * </pre>
     *
     * <p>
     * Possible response codes:
     * </p>
     * <ul>
     * <li>{@code 200} — Action accepted (or already handled — idempotent).</li>
     * <li>{@code 400} — Malformed request body.</li>
     * <li>{@code 409} — The {@code runId} does not match the currently active run.
     * The UI should display a stale-tab warning.</li>
     * </ul>
     */
    private final class ActionHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            final String body;
            try {
                body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            } catch (final IOException e) {
                sendJson(exchange, 400, "{\"error\":\"Could not read request body\"}");
                return;
            }

            final JsonObject req;
            try {
                req = JsonParser.parseString(body).getAsJsonObject();
            } catch (final Exception e) {
                sendJson(exchange, 400, "{\"error\":\"Invalid JSON body\"}");
                return;
            }

            final ActionResult result = submitAction(req);
            sendJson(exchange, result.statusCode(), result.responseBody());
        }

        /**
         * Writes a JSON response with the given HTTP status code.
         *
         * @param exchange   the HTTP exchange
         * @param statusCode the HTTP status code
         * @param json       the JSON string to send as the response body
         * @throws IOException if writing fails
         */
        private void sendJson(final HttpExchange exchange, final int statusCode, final String json) throws IOException {
            final byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            final boolean isHead = "HEAD".equalsIgnoreCase(exchange.getRequestMethod());
            exchange.sendResponseHeaders(statusCode, isHead ? -1 : bytes.length);
            if (!isHead) {
                try (final OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
        }
    }

    /**
     * Utility to find the actual local IP address of this machine, filtering out
     * loopback and virtual interfaces.
     *
     * @return the actual LAN IP address, or {@code null} if not found
     * @throws SocketException if an I/O error occurs
     */
    private static InetAddress getActualLocalIP() throws SocketException {
        final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        for (final NetworkInterface netInterface : Collections.list(interfaces)) {
            // 1. Skip loopback (127.0.0.1) and inactive interfaces
            if (netInterface.isLoopback() || !netInterface.isUp()) {
                continue;
            }

            // 2. Filter out common virtual interfaces (Docker, WSL, VirtualBox, VPNs)
            final String displayName = netInterface.getDisplayName().toLowerCase();
            final String name = netInterface.getName().toLowerCase();
            if (displayName.contains("docker") || name.contains("docker") ||
                    displayName.contains("vbox") || name.contains("vbox") ||
                    displayName.contains("virtual") || name.contains("wsl") ||
                    displayName.contains("vnic") || displayName.contains("vethernet")) {
                continue;
            }

            // 3. Look through the IP addresses assigned to this valid interface
            final Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
            for (final InetAddress address : Collections.list(addresses)) {
                // We usually want an IPv4 address for local networks
                if (address instanceof Inet4Address) {
                    // Double check it's not a loopback address
                    if (!address.isLoopbackAddress()) {
                        return address;
                    }
                }
            }
        }
        return null;
    }
}
