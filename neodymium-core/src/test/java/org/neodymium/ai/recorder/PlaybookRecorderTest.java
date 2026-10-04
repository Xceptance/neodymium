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
package org.neodymium.ai.recorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.resources.InMemoryResourceManager;

/**
 * Unit tests for {@link PlaybookRecorder}.
 * Validates step recording and serialization on SessionFinishedEvent.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class PlaybookRecorderTest
{
    @Test
    public void testOnSessionFinishedWritesJsonRecordingOnSuccess() throws IOException
    {
        final InMemoryResourceManager manager = new InMemoryResourceManager();
        final List<PlaybookStep> steps = new ArrayList<>();
        steps.add(new PlaybookStep("Open homepage"));

        final PlaybookRecorder recorder = new PlaybookRecorder(manager, "recordings/test.json", steps);
        recorder.onEvent(new SessionFinishedEvent(100L, true));

        try (final InputStream inputStream = manager.read("recordings/test.json"))
        {
            final String writtenJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertNotNull(writtenJson, "Recorded output should not be null.");
            assertTrue(writtenJson.contains("Open homepage"), "JSON output should contain step instruction.");
        }
    }

    @Test
    public void testOnSessionFinishedDoesNotWriteRecordingOnFailure()
    {
        final InMemoryResourceManager manager = new InMemoryResourceManager();
        final List<PlaybookStep> steps = new ArrayList<>();
        steps.add(new PlaybookStep("Open homepage"));

        final PlaybookRecorder recorder = new PlaybookRecorder(manager, "recordings/test.json", steps);
        recorder.onEvent(new SessionFinishedEvent(100L, false));

        assertThrows(FileNotFoundException.class, () -> manager.read("recordings/test.json"),
            "Recording file should not be created on session failure.");
    }

    @Test
    public void testOnSessionFinishedFailurePreservesExistingRecording() throws IOException
    {
        final InMemoryResourceManager manager = new InMemoryResourceManager();
        final String existingContent = "{\"existing\":\"data\"}";
        manager.write("recordings/test.json", existingContent);

        final List<PlaybookStep> steps = new ArrayList<>();
        steps.add(new PlaybookStep("Failed new step"));

        final PlaybookRecorder recorder = new PlaybookRecorder(manager, "recordings/test.json", steps);
        recorder.onEvent(new SessionFinishedEvent(100L, false));

        try (final InputStream inputStream = manager.read("recordings/test.json"))
        {
            final String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(existingContent, content, "Pre-existing recording file must remain untouched on failure.");
        }
    }

    @Test
    public void testPlaybookStepYamlHashSerialization() throws IOException
    {
        final InMemoryResourceManager manager = new InMemoryResourceManager();
        final List<PlaybookStep> steps = new ArrayList<>();
        final PlaybookStep step = new PlaybookStep("Open homepage");
        step.setSourceYamlHash("abc123def456");
        steps.add(step);

        final PlaybookRecorder recorder = new PlaybookRecorder(manager, "recordings/hash_test.json", steps);
        recorder.onEvent(new SessionFinishedEvent(100L, true));

        try (final InputStream inputStream = manager.read("recordings/hash_test.json"))
        {
            final String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"sourceYamlHash\" : \"abc123def456\""), "JSON output should serialize sourceYamlHash.");
            assertTrue(json.contains("\"schemaVersion\" : \"" + PlaybookStep.CURRENT_SCHEMA_VERSION + "\""), "JSON output should serialize schemaVersion.");
        }
    }

    /**
     * Modes that must never touch a recording: {@code LLM_ONLY} documents that it does not record,
     * {@code LINTER_ONLY} never executes steps, and {@code REPLAY_STRICT} is strictly read-only.
     */
    private static final EnumSet<ExecutionMode> NON_PERSISTING_MODES = EnumSet.of(
            ExecutionMode.LLM_ONLY,
            ExecutionMode.LINTER_ONLY,
            ExecutionMode.REPLAY_STRICT);

    @Test
    public void testNonPersistingModesNeverOverwriteExistingRecording() throws IOException
    {
        for (final ExecutionMode mode : NON_PERSISTING_MODES)
        {
            final InMemoryResourceManager manager = new InMemoryResourceManager();
            final String existingContent = "{\"existing\":\"data\"}";
            manager.write("recordings/test.json", existingContent);

            final List<PlaybookStep> steps = new ArrayList<>();
            steps.add(new PlaybookStep("Freshly parsed pending step"));

            final PlaybookRecorder recorder = new PlaybookRecorder(manager, "recordings/test.json", steps, mode);
            recorder.onEvent(new SessionFinishedEvent(100L, true));

            try (final InputStream inputStream = manager.read("recordings/test.json"))
            {
                final String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                assertEquals(existingContent, content, "Existing recording must stay untouched in mode " + mode);
            }
        }
    }

    @Test
    public void testNonPersistingModesNeverCreateRecording()
    {
        for (final ExecutionMode mode : NON_PERSISTING_MODES)
        {
            final InMemoryResourceManager manager = new InMemoryResourceManager();
            final List<PlaybookStep> steps = new ArrayList<>();
            steps.add(new PlaybookStep("Freshly parsed pending step"));

            final PlaybookRecorder recorder = new PlaybookRecorder(manager, "recordings/test.json", steps, mode);
            recorder.onEvent(new SessionFinishedEvent(100L, true));

            assertThrows(FileNotFoundException.class, () -> manager.read("recordings/test.json"),
                "No recording file may be created in mode " + mode);
        }
    }

    @Test
    public void testPersistingModesStillWriteRecording() throws IOException
    {
        final EnumSet<ExecutionMode> persistingModes = EnumSet.complementOf(NON_PERSISTING_MODES);
        for (final ExecutionMode mode : persistingModes)
        {
            final InMemoryResourceManager manager = new InMemoryResourceManager();
            final List<PlaybookStep> steps = new ArrayList<>();
            steps.add(new PlaybookStep("Recorded step"));

            final PlaybookRecorder recorder = new PlaybookRecorder(manager, "recordings/test.json", steps, mode);
            recorder.onEvent(new SessionFinishedEvent(100L, true));

            try (final InputStream inputStream = manager.read("recordings/test.json"))
            {
                final String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(content.contains("Recorded step"), "Recording must be written in mode " + mode);
            }
        }
    }
}

