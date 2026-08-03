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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.resources.InMemoryResourceManager;

/**
 * Unit tests for {@link PlaybookRecorder}.
 * Validates step recording and serialization on SessionFinishedEvent.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class PlaybookRecorderTest
{
    @Test
    public void testOnSessionFinishedWritesJsonRecording() throws IOException
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
}
