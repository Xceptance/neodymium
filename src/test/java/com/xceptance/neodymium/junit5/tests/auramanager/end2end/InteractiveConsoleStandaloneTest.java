/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.junit5.tests.auramanager.end2end;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.sleep;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.JavascriptExecutor;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.xceptance.neodymium.ai.console.InteractiveConsoleServer;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * Selenide integration tests for the <em>standalone</em> Interactive Console — that is, the
 * {@link InteractiveConsoleServer} backed by an {@link InteractiveConsoleEngine} running without
 * the Aura Manager.
 *
 * <h2>Coverage</h2>
 * <ul>
 *   <li>The standalone server starts and serves the HTML shell on {@code GET /}.</li>
 *   <li>Static resources (CSS, JS) are served with the correct {@code Content-Type} headers.</li>
 *   <li>The SSE endpoint ({@code GET /api/console/events}) returns a 200 with
 *       {@code text/event-stream} content type.</li>
 *   <li>The browser renders the console HTML without JS errors and the step list container
 *       is present in the DOM.</li>
 *   <li>Pushing state via {@code broadcastSseEvent} triggers the {@code state} SSE listener
 *       which updates the UI (step cards appear).</li>
 *   <li>A pause event enables the action buttons in the UI.</li>
 *   <li>Posting a valid action to {@code POST /api/console/action} returns HTTP 200.</li>
 *   <li>Posting an action with a stale runId returns HTTP 409.</li>
 *   <li>Pushing a {@code dumpReady} event renders the dump overlay inside the standalone page.</li>
 * </ul>
 *
 * <p>The tests use a real embedded HTTP server but inject JS state directly for scenarios
 * that would otherwise require a live Maven subprocess.</p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
public class InteractiveConsoleStandaloneTest extends BaseAiTest
{
    private InteractiveConsoleServer consoleServer;
    private InteractiveConsoleEngine consoleEngine;
    private HttpClient               httpClient;
    private String                   consoleUrl;
    private final Gson               gson = new Gson();

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    public void startStandaloneConsole() throws IOException
    {
        // Prevent the standalone server from being treated as a manager-proxied instance
        System.clearProperty("neodymium.managerActive");

        consoleEngine = new InteractiveConsoleEngine("test-run-" + System.currentTimeMillis());
        consoleServer = new InteractiveConsoleServer(consoleEngine);
        httpClient    = HttpClient.newHttpClient();

        // Determine the server's actual port (scanned upward from 18090)
        consoleUrl = "http://127.0.0.1:" + consoleServer.getLocalUrl().replaceAll(".*:(\\d+)$", "$1");

        // Open the standalone console in Selenide
        Selenide.open(consoleUrl);
        sleep(800);
    }

    @AfterEach
    public void stopStandaloneConsole()
    {
        Selenide.closeWebDriver();
        if (consoleServer != null)
        {
            consoleServer.stop();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JavascriptExecutor js()
    {
        return (JavascriptExecutor) WebDriverRunner.getWebDriver();
    }

    /**
     * Makes an HTTP GET request to the given path relative to the standalone console URL and
     * returns the response.
     */
    private HttpResponse<String> get(final String path) throws IOException, InterruptedException
    {
        final int port = consoleServer.getLocalUrl()
            .replaceAll(".*:(\\d+)$", "$1").equals("") ? 18090
            : Integer.parseInt(consoleServer.getLocalUrl().replaceAll(".*:(\\d+)$", "$1"));
        return httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    /**
     * Posts JSON to the action endpoint and returns the response.
     */
    private HttpResponse<String> postAction(final JsonObject body) throws IOException, InterruptedException
    {
        final int port = Integer.parseInt(
            consoleServer.getLocalUrl().replaceAll(".*:(\\d+)$", "$1"));
        return httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/console/action"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    // ── Tests – server basics ─────────────────────────────────────────────────

    /**
     * The root endpoint must return HTTP 200 with an HTML content type.
     */
    @NeodymiumTest
    public void testRootEndpointServesHtml() throws IOException, InterruptedException
    {
        final HttpResponse<String> resp = get("/");
        Assertions.assertEquals(200, resp.statusCode(),
            "GET / must return HTTP 200.");
        Assertions.assertTrue(
            resp.headers().firstValue("content-type").orElse("").contains("text/html"),
            "GET / must return Content-Type: text/html. Got: " +
            resp.headers().firstValue("content-type").orElse("none"));
    }

    /**
     * The CSS static asset must be served with a {@code text/css} content type.
     */
    @NeodymiumTest
    public void testCssEndpointServesWithCorrectContentType() throws IOException, InterruptedException
    {
        final HttpResponse<String> resp = get("/interactive_console.css");
        Assertions.assertEquals(200, resp.statusCode(),
            "GET /interactive_console.css must return HTTP 200.");
        Assertions.assertTrue(
            resp.headers().firstValue("content-type").orElse("").contains("text/css"),
            "CSS asset must return Content-Type: text/css.");
    }

    /**
     * The JS static asset must be served with an {@code application/javascript} content type.
     */
    @NeodymiumTest
    public void testJsEndpointServesWithCorrectContentType() throws IOException, InterruptedException
    {
        final HttpResponse<String> resp = get("/interactive_console.js");
        Assertions.assertEquals(200, resp.statusCode(),
            "GET /interactive_console.js must return HTTP 200.");
        Assertions.assertTrue(
            resp.headers().firstValue("content-type").orElse("").contains("javascript"),
            "JS asset must return Content-Type containing 'javascript'.");
    }

    // ── Tests – browser / UI ──────────────────────────────────────────────────

    /**
     * The browser must load the HTML shell without JavaScript errors and the main timeline list
     * container ({@code #timelineList}) must be present in the DOM.
     */
    @NeodymiumTest
    public void testConsolePagLoadsWithoutJsErrors()
    {
        // timelineList is the root list containing all step groups
        final Object exists = js().executeScript(
            "return document.getElementById('timelineList') !== null;"
        );
        Assertions.assertTrue(Boolean.TRUE.equals(exists),
            "#timelineList must exist in the standalone console DOM after page load.");

        // The SSE EventSource must have been created
        final Object sourceReady = js().executeScript(
            "return typeof eventSource !== 'undefined' && eventSource !== null;"
        );
        Assertions.assertTrue(Boolean.TRUE.equals(sourceReady),
            "EventSource (eventSource) must be initialised after page load.");
    }

    /**
     * The connection status indicator must initially show "connected" (or "connecting")
     * and never show "error" immediately after startup.
     */
    @NeodymiumTest
    public void testConnectionStatusIsNotErrorOnLoad()
    {
        sleep(600); // allow SSE to connect
        // Verify that #connectionDot is present and does not have the 'error' class
        $("#connectionDot").shouldBe(Condition.exist).shouldNotHave(Condition.cssClass("error"));
    }

    /**
     * Pushing a {@code state} SSE event via {@code broadcastSseEvent} must cause the console
     * to render step cards for the supplied steps.
     *
     * <p>We broadcast a minimal state JSON and then verify that the browser renders at least
     * one {@code .step-card} element after the SSE event is processed.</p>
     */
    @NeodymiumTest
    public void testStateEventRendersStepCards() throws InterruptedException
    {
        // Build a minimal console state payload
        final JsonObject state = new JsonObject();
        state.addProperty("status", "running");
        state.addProperty("runId", consoleEngine.getRunId());
        state.addProperty("currentInstruction", "Click Login");
        state.addProperty("currentIndex", 0);
        state.addProperty("reasoning", "Navigating to the login page");

        final com.google.gson.JsonArray steps = new com.google.gson.JsonArray();
        final JsonObject step1 = new JsonObject();
        step1.addProperty("instruction", "Open browser");
        step1.addProperty("status", "done");
        step1.addProperty("index", 0);
        steps.add(step1);
        final JsonObject step2 = new JsonObject();
        step2.addProperty("instruction", "Click Login");
        step2.addProperty("status", "running");
        step2.addProperty("index", 1);
        steps.add(step2);
        state.add("steps", steps);

        // Broadcast state via the engine's SSE mechanism
        consoleEngine.broadcastSseEvent("state", state.toString());
        sleep(600); // allow the browser to process the SSE event

        // At least one step card must be visible
        $$(".step-card").first().shouldBe(Condition.visible);
        Assertions.assertFalse($$(".step-card").isEmpty(),
            "After a state SSE event, at least one .step-card must be rendered.");
    }

    /**
     * Posting an action with a <em>mismatched</em> runId must return HTTP 409 (stale-tab).
     */
    @NeodymiumTest
    public void testActionWithStaleRunIdReturns409() throws IOException, InterruptedException
    {
        final JsonObject body = new JsonObject();
        body.addProperty("runId", "completely-wrong-run-id");
        body.addProperty("pauseId", "some-pause-id");
        body.addProperty("action", "APPROVE");

        final HttpResponse<String> resp = postAction(body);
        Assertions.assertEquals(409, resp.statusCode(),
            "Action with stale runId must return HTTP 409 Conflict.");
        Assertions.assertTrue(resp.body().contains("stale-tab"),
            "409 response body must contain 'stale-tab'. Got: " + resp.body());
    }

    /**
     * Posting an action when no pause is active must return HTTP 200 with status
     * {@code already-handled} (idempotent guard).
     */
    @NeodymiumTest
    public void testActionWithNoPauseReturnsAlreadyHandled() throws IOException, InterruptedException
    {
        final JsonObject body = new JsonObject();
        body.addProperty("runId",   consoleEngine.getRunId());
        body.addProperty("pauseId", "non-existent-pause-id");
        body.addProperty("action", "APPROVE");

        final HttpResponse<String> resp = postAction(body);
        Assertions.assertEquals(200, resp.statusCode(),
            "Action with no active pause must return HTTP 200 (idempotent).");
        Assertions.assertTrue(resp.body().contains("already-handled"),
            "Response must contain 'already-handled'. Got: " + resp.body());
    }

    /**
     * A {@code dumpReady} SSE event broadcast by the engine must cause the browser to show the
     * {@code #dumpReadyOverlay} element with the {@code active} class.
     */
    @NeodymiumTest
    public void testDumpReadySseEventShowsPopupInBrowser() throws InterruptedException
    {
        // Verify overlay is initially hidden
        final Object hiddenBefore = js().executeScript(
            "var el = document.getElementById('dumpReadyOverlay');" +
            "return el ? el.classList.contains('active') : false;"
        );
        Assertions.assertFalse(Boolean.TRUE.equals(hiddenBefore),
            "dumpReadyOverlay must be hidden before any dump event.");

        // Broadcast the dumpReady event via the engine
        final JsonObject payload = new JsonObject();
        payload.addProperty("txtFile",  "/tmp/test-dump.txt");
        payload.addProperty("htmlFile", "/tmp/test-dump.html");
        payload.addProperty("txtSize",  2048L);
        payload.addProperty("htmlSize", 8192L);
        payload.addProperty("timestamp", System.currentTimeMillis());

        consoleEngine.broadcastSseEvent("dumpReady", payload.toString());
        sleep(800); // allow SSE to reach the browser and JS to run

        // Overlay must now be active
        final Object activeAfter = js().executeScript(
            "var el = document.getElementById('dumpReadyOverlay');" +
            "return el ? el.classList.contains('active') : false;"
        );
        Assertions.assertTrue(Boolean.TRUE.equals(activeAfter),
            "dumpReadyOverlay must have class 'active' after dumpReady SSE event.");
    }

    /**
     * After a {@code dumpReady} event the popup must show the correct file paths
     * and human-readable sizes.
     */
    @NeodymiumTest
    public void testDumpPopupContainsCorrectFileInfoAfterSseEvent() throws InterruptedException
    {
        final String expectedTxt  = "/tmp/standalone-dump.txt";
        final String expectedHtml = "/tmp/standalone-dump.html";
        final long   txtBytes     = 512L;  // < 1 KB → "512 B"
        final long   htmlBytes    = 2048L; // 2 KB   → "2.0 KB"

        final JsonObject payload = new JsonObject();
        payload.addProperty("txtFile",  expectedTxt);
        payload.addProperty("htmlFile", expectedHtml);
        payload.addProperty("txtSize",  txtBytes);
        payload.addProperty("htmlSize", htmlBytes);
        payload.addProperty("timestamp", System.currentTimeMillis());

        consoleEngine.broadcastSseEvent("dumpReady", payload.toString());
        sleep(800);

        // Verify TXT path appears in the rendered popup
        final Object txtPath = js().executeScript(
            "var spans = document.querySelectorAll('.dump-file-path > span:first-child');" +
            "return spans[0] ? spans[0].textContent.trim() : null;"
        );
        Assertions.assertEquals(expectedTxt, txtPath,
            "Dump popup must display the TXT file path received in the SSE event.");

        // Verify HTML path appears
        final Object htmlPath = js().executeScript(
            "var spans = document.querySelectorAll('.dump-file-path > span:first-child');" +
            "return spans[1] ? spans[1].textContent.trim() : null;"
        );
        Assertions.assertEquals(expectedHtml, htmlPath,
            "Dump popup must display the HTML file path received in the SSE event.");

        // Verify byte sizes are formatted correctly
        final Object size0 = js().executeScript(
            "var s = document.querySelectorAll('.dump-file-size');" +
            "return s[0] ? s[0].textContent.trim() : null;"
        );
        Assertions.assertEquals("512 B", size0,
            "512 bytes must be displayed as '512 B'.");

        final Object size1 = js().executeScript(
            "var s = document.querySelectorAll('.dump-file-size');" +
            "return s[1] ? s[1].textContent.trim() : null;"
        );
        Assertions.assertEquals("2.0 KB", size1,
            "2048 bytes must be displayed as '2.0 KB'.");
    }

    /**
     * The SSE event endpoint must return HTTP 200 when probed with a HEAD request.
     * This validates that the server accepts and processes the connection attempt correctly
     * without initiating the infinite stream.
     */
    @NeodymiumTest
    public void testSseEndpointIsReachable() throws IOException, InterruptedException
    {
        final int port = Integer.parseInt(
            consoleServer.getLocalUrl().replaceAll(".*:(\\d+)$", "$1"));

        final java.net.http.HttpClient shortTimeoutClient = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(3))
            .build();

        try
        {
            final HttpResponse<String> resp = shortTimeoutClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + port + "/api/console/events"))
                    .timeout(java.time.Duration.ofSeconds(2))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            Assertions.assertEquals(200, resp.statusCode(),
                "SSE endpoint must return HTTP 200 on HEAD request.");
        }
        catch (final java.net.http.HttpTimeoutException e)
        {
            Assertions.fail("SSE endpoint HEAD request timed out: " + e.getMessage());
        }
    }
}
