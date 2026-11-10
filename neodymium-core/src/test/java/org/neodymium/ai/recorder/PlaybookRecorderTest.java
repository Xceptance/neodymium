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
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
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

    /**
     * Goal: Verifies that a recording written at session end contains the steps of every lifecycle phase
     * (setup, test method, teardown) in execution order, each top-level step exactly once, and that
     * sub-steps are not promoted to top-level entries.
     */
    @Test
    public void testRecordingContainsAllLifecyclePhaseStepsOnceInExecutionOrder() throws IOException
    {
        final PlaybookStep setup = new PlaybookStep("Setup step");
        final PlaybookStep main = new PlaybookStep("Main step");
        final PlaybookStep child = new PlaybookStep("Child of main step");
        child.setParent(main);
        final PlaybookStep teardown = new PlaybookStep("Teardown step");

        final ExecutionContext context = new ExecutionContext(new SessionData(new HashMap<>()));
        context.getTransientData().put("playbook.flatSteps", new ArrayList<>(List.of(setup, main, child, teardown)));

        final ExecutionContext previous = ExecutionContext.getActiveContext();
        final InMemoryResourceManager manager = new InMemoryResourceManager();
        final PlaybookRecorder recorder = new PlaybookRecorder(manager, "recordings/lifecycle.json", new ArrayList<>(), ExecutionMode.FORCE_RECORDING);
        try
        {
            ExecutionContext.setActiveContext(context);
            recorder.onEvent(new SessionFinishedEvent(100L, true));
        }
        finally
        {
            ExecutionContext.setActiveContext(previous);
        }

        try (final InputStream inputStream = manager.read("recordings/lifecycle.json"))
        {
            final String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            final int setupAt = json.indexOf("\"Setup step\"");
            final int mainAt = json.indexOf("\"Main step\"");
            final int teardownAt = json.indexOf("\"Teardown step\"");
            assertTrue(setupAt >= 0 && setupAt < mainAt && mainAt < teardownAt, "Phases must be recorded in execution order: " + json);
            assertEquals(1, json.split("\"instruction\" : \"Setup step\"", -1).length - 1, "Setup step must be recorded exactly once");
            assertEquals(1, json.split("\"instruction\" : \"Main step\"", -1).length - 1, "Main step must be recorded exactly once");
            assertEquals(1, json.split("\"instruction\" : \"Teardown step\"", -1).length - 1, "Teardown step must be recorded exactly once");
        }
    }

    /**
     * Goal: Verifies that a composite step is recorded once, together with its sub-steps. The executed-step
     * list ({@code playbook.flatSteps}) only holds the leaf sub-steps of a composite, never the composite itself,
     * so the recorder must walk up to the root step instead of dropping steps that have a parent.
     */
    @Test
    public void testRecordingKeepsCompositeStepWithSubStepsOnce() throws IOException
    {
        final PlaybookStep plain = new PlaybookStep("Plain step");
        final PlaybookStep composite = new PlaybookStep("Composite step");
        final PlaybookStep childOne = new PlaybookStep("Child one");
        final PlaybookStep childTwo = new PlaybookStep("Child two");
        for (final PlaybookStep child : List.of(childOne, childTwo))
        {
            child.setParent(composite);
            composite.getSubSteps().add(child);
        }

        final ExecutionContext context = new ExecutionContext(new SessionData(new HashMap<>()));
        // Mirrors NeodymiumAiRunner.flattenSteps: the composite itself is absent, only its leaves are listed.
        context.getTransientData().put("playbook.flatSteps", new ArrayList<>(List.of(plain, childOne, childTwo)));

        final ExecutionContext previous = ExecutionContext.getActiveContext();
        final InMemoryResourceManager manager = new InMemoryResourceManager();
        final PlaybookRecorder recorder = new PlaybookRecorder(manager, "recordings/composite.json", new ArrayList<>(), ExecutionMode.FORCE_RECORDING);
        try
        {
            ExecutionContext.setActiveContext(context);
            recorder.onEvent(new SessionFinishedEvent(100L, true));
        }
        finally
        {
            ExecutionContext.setActiveContext(previous);
        }

        try (final InputStream inputStream = manager.read("recordings/composite.json"))
        {
            final String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(1, json.split("\"instruction\" : \"Plain step\"", -1).length - 1, "Plain step must be recorded once: " + json);
            assertEquals(1, json.split("\"instruction\" : \"Composite step\"", -1).length - 1, "Composite step must be recorded once: " + json);
            assertEquals(1, json.split("\"instruction\" : \"Child one\"", -1).length - 1, "First sub-step must be recorded once, nested in its composite: " + json);
            assertEquals(1, json.split("\"instruction\" : \"Child two\"", -1).length - 1, "Second sub-step must be recorded once, nested in its composite: " + json);
        }
    }
}

