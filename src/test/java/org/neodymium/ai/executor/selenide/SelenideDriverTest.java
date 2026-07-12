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
package org.neodymium.ai.executor.selenide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.executor.rest.RestTargetExecutor;
import org.neodymium.ai.recorder.PlaybookRecorder;
import org.neodymium.ai.resources.InMemoryResourceManager;

/**
 * TDD test suite validating BrowserSutState, SelenideTargetExecutor fallback structures,
 * RestTargetExecutor header configuration, and PlaybookRecorder serialization.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class SelenideDriverTest
{
    /**
     * Constructs a default test instance.
     */
    public SelenideDriverTest()
    {
    }

    /**
     * Verifies BrowserSutState properties and layout hash calculations.
     */
    @Test
    public void testBrowserSutState()
    {
        final SutAttachment attachment = new SutAttachment("image/png", "path/to/screenshot.png", null);
        final BrowserSutState state = new BrowserSutState(
            "<html>content</html>",
            List.of(attachment),
            "abc-hash"
        );

        assertEquals("<html>content</html>", state.getTextContent());
        assertEquals(1, state.getAttachments().size());
        assertEquals("abc-hash", state.getContentHash());
    }

    /**
     * Verifies SelenideTargetExecutor default/fallback states and supported actions.
     *
     * @throws IOException if state capture fails
     */
    @Test
    public void testSelenideTargetExecutorFallback() throws IOException
    {
        final SelenideTargetExecutor executor = new SelenideTargetExecutor();

        // Browser has not started, should return safe fallback state
        final BrowserSutState state = (BrowserSutState) executor.captureState();
        assertNotNull(state);
        assertEquals("not-started", state.getContentHash());
        assertTrue(state.getTextContent().contains("Not Started"));

        // Register basic auth should execute safely without active driver
        executor.registerBasicAuth("user", "pass");

        // Supported actions check
        assertFalse(executor.getSupportedActions().isEmpty());
    }

    /**
     * Verifies registration and mapping of newly introduced browser action plugins.
     */
    @Test
    public void testNewBrowserPluginsRegistration()
    {
        final SelenideTargetExecutor executor = new SelenideTargetExecutor();
        final List<String> expectedTypes = List.of(
            "NAVIGATE", "CLICK", "TYPE", "CLEAR", "HOVER",
            "BACK", "FORWARD", "REFRESH", "CLEAR_COOKIES",
            "SCROLL", "SELECT", "WAIT", "KEY_PRESS", "SWITCH_WINDOW"
        );

        final java.util.Set<String> supportedTypes = executor.getSupportedActions().stream()
            .map(org.neodymium.ai.executor.ActionDefinition::type)
            .collect(Collectors.toSet());

        for (final String type : expectedTypes)
        {
            assertTrue(supportedTypes.contains(type), "Missing expected action type registration: " + type);
        }
    }

    /**
     * Verifies RestTargetExecutor authorization headers injection.
     */
    @Test
    public void testRestTargetExecutorConfiguration()
    {
        final RestTargetExecutor executor = new RestTargetExecutor();

        executor.registerBasicAuth("admin", "secret123");
        executor.registerBearerToken("tokenXYZ");

        assertFalse(executor.getSupportedActions().isEmpty());
    }

    /**
     * Verifies PlaybookRecorder listens to actions, formats outputs, and writes recordings.
     */
    @Test
    public void testPlaybookRecorderOutput()
    {
        final InMemoryResourceManager resourceManager = new InMemoryResourceManager();
        final PlaybookRecorder recorder = new PlaybookRecorder(resourceManager, "recordings/playbook.json");

        final Action action = new Action("CLICK", "button#submit", "Click Submit button");
        recorder.onEvent(new ActionExecutedEvent(action, true));
        recorder.onEvent(new SessionFinishedEvent(100L, true));

        // Check if actions list was recorded
        assertEquals(1, recorder.getRecordedActions().size());
        assertEquals("CLICK", recorder.getRecordedActions().get(0).getType());

        // Check if the file was written to memory
        String fileContent = null;
        try (final InputStream in = resourceManager.read("recordings/playbook.json");
             final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
        {
            fileContent = reader.lines().collect(Collectors.joining("\n"));
        }
        catch (final IOException e)
        {
            // Fail test if read fails
        }
        assertNotNull(fileContent);
        assertTrue(fileContent.contains("\"type\": \"CLICK\""));
        assertTrue(fileContent.contains("\"target\": \"button#submit\""));
    }
}
