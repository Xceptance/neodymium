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
package com.xceptance.neodymium.ai.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.config.AiConfiguration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Unit test suite validating {@link InteractiveConsoleServer} network binding and CORS origin isolation.
 *
 * @author AI-generated: Claude Sonnet 4.5
 * @author Xceptance GmbH 2026
 */
public class InteractiveConsoleServerTest
{
    private String originalBindAddress;

    @BeforeEach
    public void setUp()
    {
        originalBindAddress = System.getProperty("neodymium.ai.console.bindAddress");
        AiConfiguration.resetInstance();
    }

    @AfterEach
    public void tearDown()
    {
        if (originalBindAddress != null)
        {
            System.setProperty("neodymium.ai.console.bindAddress", originalBindAddress);
        }
        else
        {
            System.clearProperty("neodymium.ai.console.bindAddress");
        }
        AiConfiguration.resetInstance();
    }

    @Test
    public void testIsLocalhostOriginAllowsLocalOrigins()
    {
        assertTrue(InteractiveConsoleServer.isLocalhostOrigin("http://localhost"));
        assertTrue(InteractiveConsoleServer.isLocalhostOrigin("http://localhost:8080"));
        assertTrue(InteractiveConsoleServer.isLocalhostOrigin("https://localhost:18090"));
        assertTrue(InteractiveConsoleServer.isLocalhostOrigin("http://127.0.0.1"));
        assertTrue(InteractiveConsoleServer.isLocalhostOrigin("http://127.0.0.1:18090"));
        assertTrue(InteractiveConsoleServer.isLocalhostOrigin("null"));
    }

    @Test
    public void testIsLocalhostOriginRejectsExternalOrigins()
    {
        assertFalse(InteractiveConsoleServer.isLocalhostOrigin("https://evil.com"));
        assertFalse(InteractiveConsoleServer.isLocalhostOrigin("http://evil.com:8080"));
        assertFalse(InteractiveConsoleServer.isLocalhostOrigin("http://localhost.evil.com"));
        assertFalse(InteractiveConsoleServer.isLocalhostOrigin("http://127.0.0.1.attacker.org"));
        assertFalse(InteractiveConsoleServer.isLocalhostOrigin("http://192.168.1.10:8080"));
        assertFalse(InteractiveConsoleServer.isLocalhostOrigin(null));
        assertFalse(InteractiveConsoleServer.isLocalhostOrigin(""));
    }

    @Test
    public void testDefaultBindingIsLoopback() throws IOException
    {
        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("test-bind");
        InteractiveConsoleServer server = null;
        try
        {
            server = new InteractiveConsoleServer(engine);
            final InetSocketAddress address = server.getAddress();
            assertNotNull(address);
            assertTrue(address.getAddress().isLoopbackAddress(), "Server must bind to a loopback address by default");
            assertEquals("127.0.0.1", address.getAddress().getHostAddress());
        }
        finally
        {
            if (server != null)
            {
                server.stop();
            }
        }
    }

    @Test
    public void testInteractiveConsoleJsIntegrityAndNoDuplicateDeclarations() throws IOException
    {
        final String resourcePath = "com/xceptance/neodymium/ai/console/interactive_console.js";
        try (final InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath))
        {
            assertNotNull(in, "interactive_console.js must be present on the classpath");
            final String jsContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(jsContent.isBlank(), "interactive_console.js must not be blank");

            // Verify dismissInitialLoadingOverlay function definition
            assertTrue(jsContent.contains("function dismissInitialLoadingOverlay()"),
                "dismissInitialLoadingOverlay helper must be defined");

            // Extract checkActionApprovals function content and assert single declaration of runBtn
            final int funcStart = jsContent.indexOf("function checkActionApprovals()");
            assertTrue(funcStart >= 0, "checkActionApprovals function must exist");
            final int funcEnd = jsContent.indexOf("function renderBlock(", funcStart);
            assertTrue(funcEnd > funcStart, "renderBlock function must follow checkActionApprovals");
            final String funcBody = jsContent.substring(funcStart, funcEnd);

            final int firstDecl = funcBody.indexOf("const runBtn");
            assertTrue(firstDecl >= 0, "runBtn must be declared in checkActionApprovals");
            final int secondDecl = funcBody.indexOf("const runBtn", firstDecl + "const runBtn".length());
            assertEquals(-1, secondDecl, "runBtn must not be redeclared inside checkActionApprovals (would cause SyntaxError)");

            // Assert that checkActionApprovals does not disable button for running steps when paused
            assertFalse(funcBody.contains("s.status === 'running'"),
                "checkActionApprovals must not treat step status 'running' as completed or disable runBtn");
        }
    }

    @Test
    public void testInteractiveConsoleJsApplyStatePreservesFinalPauseToken() throws IOException
    {
        final String resourcePath = "com/xceptance/neodymium/ai/console/interactive_console.js";
        try (final InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath))
        {
            assertNotNull(in, "interactive_console.js must be present on the classpath");
            final String jsContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            // Verify that applyState only nullifies currentPauseId on status 'running' and preserves explicit pauseId on passed/failed
            final int applyStart = jsContent.indexOf("function applyState(state)");
            assertTrue(applyStart >= 0, "applyState function must exist");
            final int applyEnd = jsContent.indexOf("function checkActionApprovals()", applyStart);
            assertTrue(applyEnd > applyStart, "checkActionApprovals function must follow applyState");
            final String applyBody = jsContent.substring(applyStart, applyEnd);

            assertTrue(applyBody.contains("if (state.status === 'running')"),
                "applyState must check state.status === 'running' to invalidate currentPauseId");
            assertFalse(applyBody.contains("state.status === 'passed' || state.status === 'failed'"),
                "applyState must not wipe currentPauseId unconditionally on passed or failed statuses");
        }
    }

    @Test
    public void testInteractiveConsoleJsSaveEditDoesNotPrematurelyEnableButtons() throws IOException
    {
        final String resourcePath = "com/xceptance/neodymium/ai/console/interactive_console.js";
        try (final InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath))
        {
            assertNotNull(in, "interactive_console.js must be present on the classpath");
            final String jsContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            final int saveEditStart = jsContent.indexOf("function saveEdit(btnElement)");
            assertTrue(saveEditStart >= 0, "saveEdit function must exist");
            final int saveEditEnd = jsContent.indexOf("function toggleBp(", saveEditStart);
            assertTrue(saveEditEnd > saveEditStart, "toggleBp function must follow saveEdit");
            final String saveEditBody = jsContent.substring(saveEditStart, saveEditEnd);

            final int isTempIndex = saveEditBody.indexOf("if (isTemp)");
            assertTrue(isTempIndex >= 0, "saveEdit must handle isTemp");
            final int cancelEditIndex = saveEditBody.indexOf("cancelEdit", isTempIndex);
            assertTrue(cancelEditIndex > isTempIndex, "saveEdit must contain cancelEdit call");
            final int elseIndex = saveEditBody.indexOf("} else {", cancelEditIndex);
            assertTrue(elseIndex > cancelEditIndex, "saveEdit must contain outer else branch for isTemp");
            final String isTempBranch = saveEditBody.substring(isTempIndex, elseIndex);

            assertFalse(isTempBranch.contains("setButtonsEnabled(true)"),
                "saveEdit must not prematurely enable buttons when ADD action is dispatched");
            assertTrue(isTempBranch.contains("selectedStepIndexForDetails = null"),
                "saveEdit must reset selectedStepIndexForDetails on new step addition");
        }
    }

    @Test
    public void testInteractiveConsoleJsRunButtonDisabledDuringExecutionAndThinking() throws IOException
    {
        final String resourcePath = "com/xceptance/neodymium/ai/console/interactive_console.js";
        try (final InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath))
        {
            assertNotNull(in, "interactive_console.js must be present on the classpath");
            final String jsContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            // Verify setButtonsEnabled has execution guard based on pause token and running status
            final int setButtonsStart = jsContent.indexOf("function setButtonsEnabled(enabled)");
            assertTrue(setButtonsStart >= 0, "setButtonsEnabled function must exist");
            final int setButtonsEnd = jsContent.indexOf("function syncAutoButton()", setButtonsStart);
            assertTrue(setButtonsEnd > setButtonsStart, "syncAutoButton function must follow setButtonsEnabled");
            final String setButtonsBody = jsContent.substring(setButtonsStart, setButtonsEnd);

            assertTrue(setButtonsBody.contains("const isExecuting = (!currentPauseId) || (currentState && currentState.status === 'running') || isStructuralActionInFlight || isRunActionInFlight;"),
                "setButtonsEnabled must evaluate isExecuting based on pause token, running status, structural action in flight, and run action in flight");
            assertTrue(setButtonsBody.contains("const runDisabled = !enabled || isEditing || isExecuting;"),
                "setButtonsEnabled must compute runDisabled incorporating isExecuting");
            assertTrue(setButtonsBody.contains("runBtn.disabled = runDisabled;"),
                "setButtonsEnabled must assign runDisabled to runBtn.disabled");

            // Verify checkActionApprovals maintains thinking and executing guards
            final int checkStart = jsContent.indexOf("function checkActionApprovals()");
            assertTrue(checkStart >= 0, "checkActionApprovals function must exist");
            final int checkEnd = jsContent.indexOf("function renderBlock(", checkStart);
            assertTrue(checkEnd > checkStart, "renderBlock function must follow checkActionApprovals");
            final String checkBody = jsContent.substring(checkStart, checkEnd);

            assertTrue(checkBody.contains("const isExecuting = !currentPauseId || (currentState && currentState.status === 'running') || isStructuralActionInFlight || isRunActionInFlight || isAutoMode;"),
                "checkActionApprovals must evaluate isExecuting based on pause token, running status, structural action in flight, run action in flight, and auto mode");
            assertTrue(checkBody.contains("const isThinking = (s && !s.reasoning && !hasPlannedActions && !isStructuralActionInFlight) || isTopThinking || isStepThinking;"),
                "checkActionApprovals must evaluate isThinking incorporating step actions, reasoning, and thinking state indicators");
            assertTrue(checkBody.contains("if (isThinking || isExecuting || isRunActionInFlight)"),
                "checkActionApprovals must disable runBtn when isThinking, isExecuting, or isRunActionInFlight is true");
            assertTrue(checkBody.contains("runBtn.disabled = false;"),
                "checkActionApprovals must enable runBtn when not thinking and not executing");
        }
    }

    @Test
    public void testRunButtonTargetCardIsolationAndPauseGuard() throws IOException
    {
        final String resourcePath = "com/xceptance/neodymium/ai/console/interactive_console.js";
        try (final InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath))
        {
            assertNotNull(in, "interactive_console.js must be present on the classpath");
            final String jsContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            // Verify applyState immediately calls setButtonsEnabled(false) when executing, but enables when paused
            final int applyStart = jsContent.indexOf("function applyState(state)");
            assertTrue(applyStart >= 0, "applyState function must exist");
            final int applyEnd = jsContent.indexOf("function checkActionApprovals()", applyStart);
            assertTrue(applyEnd > applyStart, "checkActionApprovals function must follow applyState");
            final String applyBody = jsContent.substring(applyStart, applyEnd);

            assertTrue(applyBody.contains("if (!currentPauseId || state.status === 'running' || isRunActionInFlight)"),
                "applyState must guard setButtonsEnabled(false) when no pause token exists, status is running, or run action is in flight");
            assertTrue(applyBody.contains("setButtonsEnabled(true);"),
                "applyState must call setButtonsEnabled(true) when paused");
            assertTrue(applyBody.contains("checkActionApprovals();"),
                "applyState must invoke checkActionApprovals when paused");

            // Verify checkActionApprovals does not fall back to selected-details unless explicitly paused
            final int checkStart = jsContent.indexOf("function checkActionApprovals()");
            final int checkEnd = jsContent.indexOf("function renderBlock(", checkStart);
            final String checkBody = jsContent.substring(checkStart, checkEnd);

            assertTrue(checkBody.contains("const targetCard = activeCard || (currentPauseId ? document.querySelector('.step-card.selected-details') : null);"),
                "checkActionApprovals must only fall back to selected-details if currentPauseId is truthy");
            assertTrue(checkBody.contains("if (s && (s.status === 'passed' || s.status === 'skipped' || s.status === 'healed' || s.status === 'aborted'))"),
                "checkActionApprovals must keep runBtn disabled for finished steps");
        }
    }

    @Test
    public void testToolbarButtonsIndependentOfRunExecutionState() throws IOException
    {
        final String resourcePath = "com/xceptance/neodymium/ai/console/interactive_console.js";
        try (final InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath))
        {
            assertNotNull(in, "interactive_console.js must be present on the classpath");
            final String jsContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            final int setButtonsStart = jsContent.indexOf("function setButtonsEnabled(enabled)");
            assertTrue(setButtonsStart >= 0, "setButtonsEnabled function must exist");
            final int setButtonsEnd = jsContent.indexOf("function syncAutoButton()", setButtonsStart);
            final String setButtonsBody = jsContent.substring(setButtonsStart, setButtonsEnd);

            // Verify toolbar buttons Skip, Auto, and Cancel remain enabled independently of the executing state
            assertTrue(setButtonsBody.contains("if (skipBtn) { skipBtn.disabled = isEditing;"),
                "skipBtn must remain enabled during execution and thinking unless inline editing is active");
            assertTrue(setButtonsBody.contains("if (autoBtn) { autoBtn.disabled = isEditing;"),
                "autoBtn must remain enabled during execution and thinking unless inline editing is active");
            assertTrue(setButtonsBody.contains("if (btnCancel) { btnCancel.disabled = isEditing;"),
                "btnCancel must remain enabled during execution and thinking unless inline editing is active");

            // Verify queued skip support when no pause token exists
            assertTrue(jsContent.contains("let pendingSkipAfterSync = false;"),
                "interactive_console.js must track pendingSkipAfterSync flag");
            assertTrue(jsContent.contains("pendingSkipAfterSync = true;"),
                "sendAction must arm pendingSkipAfterSync when currentPauseId is null");
            assertTrue(jsContent.contains("sendAction('SKIP');"),
                "pause handler and applyState must dispatch sendAction('SKIP') upon receiving a pause token if skip was armed");
        }
    }

    @Test
    public void testRunExecutionLatchPreventsTransientEnablement() throws IOException
    {
        final String resourcePath = "com/xceptance/neodymium/ai/console/interactive_console.js";
        try (final InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath))
        {
            assertNotNull(in, "interactive_console.js must be present on the classpath");
            final String jsContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            // Verify declaration of run execution latch variables
            assertTrue(jsContent.contains("let isRunActionInFlight = false;"),
                "interactive_console.js must declare isRunActionInFlight");
            assertTrue(jsContent.contains("let runActionPauseId = null;"),
                "interactive_console.js must declare runActionPauseId");

            // Verify sendAction arms isRunActionInFlight for RUN, HEAL, and SKIP
            final int sendActionStart = jsContent.indexOf("function sendAction(action, extra)");
            assertTrue(sendActionStart >= 0, "sendAction function must exist");
            final int sendActionEnd = jsContent.indexOf("function isStructuralAction(action, extra)", sendActionStart);
            final String sendActionBody = jsContent.substring(sendActionStart, sendActionEnd > 0 ? sendActionEnd : jsContent.length());

            assertTrue(sendActionBody.contains("if (action === 'RUN' || action === 'HEAL' || action === 'SKIP')"),
                "sendAction must identify RUN, HEAL, and SKIP actions to arm latch");
            assertTrue(sendActionBody.contains("isRunActionInFlight = true;"),
                "sendAction must set isRunActionInFlight to true");
            assertTrue(sendActionBody.contains("runActionPauseId = currentPauseId;"),
                "sendAction must record runActionPauseId from currentPauseId");

            // Verify SSE pause listener filters out stale pause token
            final int pauseListenerStart = jsContent.indexOf("eventSource.addEventListener('pause'");
            assertTrue(pauseListenerStart >= 0, "pause SSE listener must exist");
            final int pauseListenerEnd = jsContent.indexOf("function applyState(state)", pauseListenerStart);
            final String pauseListenerBody = jsContent.substring(pauseListenerStart, pauseListenerEnd > 0 ? pauseListenerEnd : jsContent.length());

            assertTrue(pauseListenerBody.contains("if (runActionPauseId && data.pauseId === runActionPauseId)"),
                "pause SSE listener must ignore stale pause tokens matching runActionPauseId");
            assertTrue(pauseListenerBody.contains("if (runActionPauseId && data.pauseId !== runActionPauseId)"),
                "pause SSE listener must release latch only upon receiving a distinct pause token");
        }
    }

    @Test
    public void testStructuralMutationLockAndVisualSyncIndicator() throws IOException
    {
        final String htmlPath = "com/xceptance/neodymium/ai/console/interactive_console.html";
        try (final InputStream in = getClass().getClassLoader().getResourceAsStream(htmlPath))
        {
            assertNotNull(in, "interactive_console.html must be present on the classpath");
            final String htmlContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(htmlContent.contains("id=\"timelineSyncIndicator\""),
                "interactive_console.html must declare #timelineSyncIndicator for visual feedback");
            assertTrue(htmlContent.contains("Structuring changes..."),
                "timelineSyncIndicator must communicate structural changes to the user");
        }

        final String cssPath = "com/xceptance/neodymium/ai/console/interactive_console.css";
        try (final InputStream in = getClass().getClassLoader().getResourceAsStream(cssPath))
        {
            assertNotNull(in, "interactive_console.css must be present on the classpath");
            final String cssContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(cssContent.contains(".step-add-after-btn:disabled"),
                "interactive_console.css must define disabled styles for .step-add-after-btn");
            assertTrue(cssContent.contains(".steps-block-content.drag-disabled .step-add-after-btn"),
                "interactive_console.css must suppress inline card add button in drag-disabled containers");
            assertTrue(cssContent.contains(".step-card:hover .step-add-after-btn:not(:disabled):not(.disabled)"),
                "interactive_console.css must guard hover visibility against disabled step-add-after-btn");
            assertTrue(cssContent.contains(".step-add-after-btn:not(:disabled):not(.disabled):hover"),
                "interactive_console.css must guard hover background accent against disabled step-add-after-btn");
            assertTrue(cssContent.contains(".step-num-badge input:disabled"),
                "interactive_console.css must define disabled styles for .step-num-badge input");
            assertTrue(cssContent.contains(".steps-block-content.drag-disabled .step-num-badge"),
                "interactive_console.css must suppress pointer events on step-num-badge in drag-disabled containers");
            assertTrue(cssContent.contains(".step-card.selected .step-num-badge.sync-disabled"),
                "interactive_console.css must neutralize active border and cursor on selected step-num-badge when syncing");
        }

        final String jsPath = "com/xceptance/neodymium/ai/console/interactive_console.js";
        try (final InputStream in = getClass().getClassLoader().getResourceAsStream(jsPath))
        {
            assertNotNull(in, "interactive_console.js must be present on the classpath");
            final String jsContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            // Verify watchdog safety net
            assertTrue(jsContent.contains("function startStructuralSyncWatchdog()"),
                "interactive_console.js must define startStructuralSyncWatchdog");
            assertTrue(jsContent.contains("function clearStructuralSyncWatchdog()"),
                "interactive_console.js must define clearStructuralSyncWatchdog");

            // Verify setButtonsEnabled structural lock
            final int setButtonsStart = jsContent.indexOf("function setButtonsEnabled(enabled)");
            assertTrue(setButtonsStart >= 0, "setButtonsEnabled function must exist");
            final int setButtonsEnd = jsContent.indexOf("function syncAutoButton()", setButtonsStart);
            final String setButtonsBody = jsContent.substring(setButtonsStart, setButtonsEnd);

            assertTrue(setButtonsBody.contains("const isSyncing = isStructuralActionInFlight;"),
                "setButtonsEnabled must track isSyncing from isStructuralActionInFlight");
            assertTrue(setButtonsBody.contains("syncIndicator.style.display = isSyncing ? 'flex' : 'none';"),
                "setButtonsEnabled must toggle timelineSyncIndicator visibility based on isSyncing");
            assertTrue(setButtonsBody.contains("block.classList.add('drag-disabled');"),
                "setButtonsEnabled must add drag-disabled class to step blocks during sync");
            assertTrue(setButtonsBody.contains("const shouldDisable = !enabled || isEditing || isSyncing;"),
                "add-step buttons must be disabled during sync or editing");
            assertTrue(setButtonsBody.contains("input.disabled = isSyncing || !enabled;"),
                "badge inputs must be disabled during sync or execution");
            assertTrue(setButtonsBody.contains("input.setAttribute('disabled', 'true');"),
                "badge inputs must receive disabled attribute during sync");
            assertTrue(setButtonsBody.contains("input.setAttribute('readonly', 'true');"),
                "badge inputs must receive readonly attribute during sync");
            assertTrue(setButtonsBody.contains("if (isSyncing) input.blur();"),
                "badge inputs must be blurred when syncing begins");
            assertTrue(setButtonsBody.contains("const cardActionBtns = document.querySelectorAll('.step-edit-btn, .step-add-after-btn, .step-rewind-btn, .step-delete-btn');"),
                "setButtonsEnabled must target inline card action buttons including rewind and add");

            // Verify step card inline action button generation disabled during sync
            assertTrue(jsContent.contains("const syncDisabledAttr = isSyncing ? ' disabled=\"true\"' : '';"),
                "interactive_console.js must set disabled attribute for card buttons during sync");
            assertTrue(jsContent.contains("${isSyncing ? 'disabled readonly ' : ''}"),
                "interactive_console.js must render step number input with disabled and readonly attributes during sync");

            // Verify action guards against in-flight structural actions
            assertTrue(jsContent.contains("function restoreFocus() {\n    if (isStructuralActionInFlight) return;"),
                "restoreFocus must not steal focus to badge input while a structural action is in-flight");
            assertTrue(jsContent.contains("if (isStructuralActionInFlight || !currentPauseId) {\n        if (event && event.preventDefault) event.preventDefault();\n        return;\n    }"),
                "handleStepInputInput must guard against typing or debounce timers during structural sync");
            assertTrue(jsContent.contains("function enableEdit(btnElement) {\n    if (isStructuralActionInFlight) return;"),
                "enableEdit must guard against concurrent in-flight structural mutations");
            assertTrue(jsContent.contains("function triggerRewind(event, index) {\n    if (event) event.stopPropagation();\n    if (!currentPauseId || isStructuralActionInFlight) return;"),
                "triggerRewind must guard against in-flight structural mutations");
            assertTrue(jsContent.contains("function openAddStepOverlay(blockName, targetIndex) {\n    if (isStructuralActionInFlight) return;"),
                "openAddStepOverlay must guard against concurrent in-flight structural mutations");
            assertTrue(jsContent.contains("function handleDragStart(event, index, blockName) {\n    if (!currentPauseId || isStructuralActionInFlight || document.querySelector('.step-card.editing')) {"),
                "handleDragStart must prevent dragging while a structural action is in-flight");
            assertTrue(jsContent.contains("function reorderStep(event, stepIndex, direction, blockName) {\n    if (!currentPauseId || isStructuralActionInFlight) return;"),
                "reorderStep must prevent reordering while a structural action is in-flight");
        }
    }

    private JsonObject createStep(final int index, final String instruction, final String status)
    {
        final JsonObject step = new JsonObject();
        step.addProperty("index", index);
        step.addProperty("instruction", instruction);
        step.addProperty("status", status);
        step.addProperty("source", "playbook");
        step.addProperty("includeLevel", 0);
        step.add("actions", new JsonArray());
        return step;
    }

    private JsonObject createSimulationState(final JsonArray before, final JsonArray steps, final JsonArray after)
    {
        final JsonObject state = new JsonObject();
        final JsonObject blocks = new JsonObject();
        blocks.add("before", before);
        blocks.add("steps", steps);
        blocks.add("after", after);
        state.add("blocks", blocks);
        return state;
    }

    @Test
    public void testApplyReorderForwardWithinSameBlock()
    {
        final JsonArray stepsArray = new JsonArray();
        stepsArray.add(createStep(0, "Step A", "pending"));
        stepsArray.add(createStep(1, "Step B", "pending"));
        stepsArray.add(createStep(2, "Step C", "pending"));

        final JsonObject state = createSimulationState(new JsonArray(), stepsArray, new JsonArray());
        final List<JsonObject> allSteps = new ArrayList<>();

        final JsonObject response = new JsonObject();
        response.addProperty("action", "REORDER");
        response.addProperty("fromBlock", "steps");
        response.addProperty("fromIndex", 0);
        response.addProperty("toBlock", "steps");
        response.addProperty("toIndex", 1);

        final int newStepIndex = InteractiveConsoleServer.applyReorder(state, allSteps, response, 0);

        assertEquals(0, newStepIndex);
        assertTrue(state.get("interactivePromptChanged").getAsBoolean());
        assertEquals(3, allSteps.size());

        // Step 0 was moved to index 1 -> new order: Step B, Step A, Step C
        assertEquals("Step B", allSteps.get(0).get("instruction").getAsString());
        assertEquals(0, allSteps.get(0).get("index").getAsInt());

        assertEquals("Step A", allSteps.get(1).get("instruction").getAsString());
        assertEquals(1, allSteps.get(1).get("index").getAsInt());

        assertEquals("Step C", allSteps.get(2).get("instruction").getAsString());
        assertEquals(2, allSteps.get(2).get("index").getAsInt());

        // Now move Step B (now at index 0) to index 2 (end of block)
        final JsonObject response2 = new JsonObject();
        response2.addProperty("action", "REORDER");
        response2.addProperty("fromBlock", "steps");
        response2.addProperty("fromIndex", 0);
        response2.addProperty("toBlock", "steps");
        response2.addProperty("toIndex", 2);

        InteractiveConsoleServer.applyReorder(state, allSteps, response2, 0);

        // New order: Step A, Step C, Step B
        assertEquals("Step A", allSteps.get(0).get("instruction").getAsString());
        assertEquals(0, allSteps.get(0).get("index").getAsInt());

        assertEquals("Step C", allSteps.get(1).get("instruction").getAsString());
        assertEquals(1, allSteps.get(1).get("index").getAsInt());

        assertEquals("Step B", allSteps.get(2).get("instruction").getAsString());
        assertEquals(2, allSteps.get(2).get("index").getAsInt());
    }

    @Test
    public void testApplyReorderBackwardWithinSameBlock()
    {
        final JsonArray stepsArray = new JsonArray();
        stepsArray.add(createStep(0, "Step A", "pending"));
        stepsArray.add(createStep(1, "Step B", "pending"));
        stepsArray.add(createStep(2, "Step C", "pending"));

        final JsonObject state = createSimulationState(new JsonArray(), stepsArray, new JsonArray());
        final List<JsonObject> allSteps = new ArrayList<>();

        // Move Step C (index 2) to index 0
        final JsonObject response = new JsonObject();
        response.addProperty("action", "REORDER");
        response.addProperty("fromBlock", "steps");
        response.addProperty("fromIndex", 2);
        response.addProperty("toBlock", "steps");
        response.addProperty("toIndex", 0);

        InteractiveConsoleServer.applyReorder(state, allSteps, response, 0);

        assertEquals(3, allSteps.size());
        assertEquals("Step C", allSteps.get(0).get("instruction").getAsString());
        assertEquals(0, allSteps.get(0).get("index").getAsInt());

        assertEquals("Step A", allSteps.get(1).get("instruction").getAsString());
        assertEquals(1, allSteps.get(1).get("index").getAsInt());

        assertEquals("Step B", allSteps.get(2).get("instruction").getAsString());
        assertEquals(2, allSteps.get(2).get("index").getAsInt());

        // Move Step B (index 2) to index 1
        final JsonObject response2 = new JsonObject();
        response2.addProperty("action", "REORDER");
        response2.addProperty("fromBlock", "steps");
        response2.addProperty("fromIndex", 2);
        response2.addProperty("toBlock", "steps");
        response2.addProperty("toIndex", 1);

        InteractiveConsoleServer.applyReorder(state, allSteps, response2, 0);

        assertEquals("Step C", allSteps.get(0).get("instruction").getAsString());
        assertEquals("Step B", allSteps.get(1).get("instruction").getAsString());
        assertEquals("Step A", allSteps.get(2).get("instruction").getAsString());
    }

    @Test
    public void testApplyReorderCrossBlock()
    {
        final JsonArray beforeArray = new JsonArray();
        beforeArray.add(createStep(0, "Setup A", "passed"));

        final JsonArray stepsArray = new JsonArray();
        stepsArray.add(createStep(1, "Action B", "running"));
        stepsArray.add(createStep(2, "Action C", "pending"));

        final JsonArray afterArray = new JsonArray();
        afterArray.add(createStep(3, "Teardown D", "pending"));

        final JsonObject state = createSimulationState(beforeArray, stepsArray, afterArray);
        final List<JsonObject> allSteps = new ArrayList<>();

        // Move Setup A from "before" (index 0) to "steps" at index 1 (between Action B and Action C)
        final JsonObject response = new JsonObject();
        response.addProperty("action", "REORDER");
        response.addProperty("fromBlock", "before");
        response.addProperty("fromIndex", 0);
        response.addProperty("toBlock", "steps");
        response.addProperty("toIndex", 1);

        final int newStepIndex = InteractiveConsoleServer.applyReorder(state, allSteps, response, 1);

        final JsonObject blocks = state.getAsJsonObject("blocks");
        assertEquals(0, blocks.getAsJsonArray("before").size());
        assertEquals(3, blocks.getAsJsonArray("steps").size());
        assertEquals(1, blocks.getAsJsonArray("after").size());
        assertEquals(4, allSteps.size());

        assertEquals("Action B", allSteps.get(0).get("instruction").getAsString());
        assertEquals(0, allSteps.get(0).get("index").getAsInt());

        assertEquals("Setup A", allSteps.get(1).get("instruction").getAsString());
        assertEquals(1, allSteps.get(1).get("index").getAsInt());

        assertEquals("Action C", allSteps.get(2).get("instruction").getAsString());
        assertEquals(2, allSteps.get(2).get("index").getAsInt());

        assertEquals("Teardown D", allSteps.get(3).get("instruction").getAsString());
        assertEquals(3, allSteps.get(3).get("index").getAsInt());

        // Running step was Action B, which is now at index 0
        assertEquals(0, newStepIndex);
    }

    @Test
    public void testApplyReorderBoundaryClamping()
    {
        final JsonArray stepsArray = new JsonArray();
        stepsArray.add(createStep(0, "Step A", "pending"));
        stepsArray.add(createStep(1, "Step B", "pending"));

        final JsonObject state = createSimulationState(new JsonArray(), stepsArray, new JsonArray());
        final List<JsonObject> allSteps = new ArrayList<>();

        // Negative toIndex clamped to 0
        final JsonObject responseNeg = new JsonObject();
        responseNeg.addProperty("action", "REORDER");
        responseNeg.addProperty("fromBlock", "steps");
        responseNeg.addProperty("fromIndex", 1);
        responseNeg.addProperty("toBlock", "steps");
        responseNeg.addProperty("toIndex", -10);

        InteractiveConsoleServer.applyReorder(state, allSteps, responseNeg, 0);

        assertEquals("Step B", allSteps.get(0).get("instruction").getAsString());
        assertEquals("Step A", allSteps.get(1).get("instruction").getAsString());

        // Out-of-bounds excessive toIndex clamped to size
        final JsonObject responseExcess = new JsonObject();
        responseExcess.addProperty("action", "REORDER");
        responseExcess.addProperty("fromBlock", "steps");
        responseExcess.addProperty("fromIndex", 0);
        responseExcess.addProperty("toBlock", "steps");
        responseExcess.addProperty("toIndex", 999);

        InteractiveConsoleServer.applyReorder(state, allSteps, responseExcess, 0);

        assertEquals("Step A", allSteps.get(0).get("instruction").getAsString());
        assertEquals("Step B", allSteps.get(1).get("instruction").getAsString());
    }

    @Test
    public void testApplyReorderTracksRunningStepIndex()
    {
        final JsonArray stepsArray = new JsonArray();
        stepsArray.add(createStep(0, "Step A", "passed"));
        stepsArray.add(createStep(1, "Step B", "running"));
        stepsArray.add(createStep(2, "Step C", "pending"));

        final JsonObject state = createSimulationState(new JsonArray(), stepsArray, new JsonArray());
        final List<JsonObject> allSteps = new ArrayList<>();

        // Move Step A (index 0) to end (index 2) -> running Step B becomes index 0
        final JsonObject response = new JsonObject();
        response.addProperty("action", "REORDER");
        response.addProperty("fromBlock", "steps");
        response.addProperty("fromIndex", 0);
        response.addProperty("toBlock", "steps");
        response.addProperty("toIndex", 2);

        final int newStepIndex = InteractiveConsoleServer.applyReorder(state, allSteps, response, 1);
        assertEquals(0, newStepIndex);
        assertEquals("running", allSteps.get(newStepIndex).get("status").getAsString());
        assertEquals("Step B", allSteps.get(newStepIndex).get("instruction").getAsString());

        // Move running Step B (index 0) to index 2
        final JsonObject response2 = new JsonObject();
        response2.addProperty("action", "REORDER");
        response2.addProperty("fromBlock", "steps");
        response2.addProperty("fromIndex", 0);
        response2.addProperty("toBlock", "steps");
        response2.addProperty("toIndex", 2);

        final int newStepIndex2 = InteractiveConsoleServer.applyReorder(state, allSteps, response2, 0);
        assertEquals(2, newStepIndex2);
        assertEquals("running", allSteps.get(newStepIndex2).get("status").getAsString());
        assertEquals("Step B", allSteps.get(newStepIndex2).get("instruction").getAsString());
    }
}
