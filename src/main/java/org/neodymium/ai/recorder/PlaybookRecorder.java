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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.List;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEvent;
import org.neodymium.ai.event.ExecutionListener;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.resources.PlaybookResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener that monitors execution event dispatches, compiles executed actions,
 * and writes the final parameterized recording as a JSON file through a resource manager
 * only upon successful session completion when updates occurred.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookRecorder implements ExecutionListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaybookRecorder.class);

    private final PlaybookResourceManager resourceManager;
    private final String recordingPath;
    private final List<PlaybookStep> playbookSteps;
    private final ExecutionMode executionMode;

    /**
     * Constructs a PlaybookRecorder with default recording mode.
     */
    public PlaybookRecorder(
        final PlaybookResourceManager resourceManager,
        final String recordingPath,
        final List<PlaybookStep> playbookSteps
    )
    {
        this(resourceManager, recordingPath, playbookSteps, ExecutionMode.LLM_RECORDING);
    }

    /**
     * Constructs a PlaybookRecorder with explicit execution mode.
     */
    public PlaybookRecorder(
        final PlaybookResourceManager resourceManager,
        final String recordingPath,
        final List<PlaybookStep> playbookSteps,
        final ExecutionMode executionMode
    )
    {
        this.resourceManager = resourceManager;
        this.recordingPath = recordingPath;
        this.playbookSteps = playbookSteps;
        this.executionMode = executionMode != null ? executionMode : ExecutionMode.LLM_RECORDING;
    }

    @Override
    public void onEvent(final ExecutionEvent event)
    {
        if (event instanceof SessionFinishedEvent)
        {
            final SessionFinishedEvent sessionFinishedEvent = (SessionFinishedEvent) event;
            if (!sessionFinishedEvent.isSuccess())
            {
                LOGGER.info("Session finished with failure. Skipping playbook recording write to {}", this.recordingPath);
                return;
            }

            try
            {
                final ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                final String candidateJson = mapper.writeValueAsString(this.playbookSteps);

                if (!this.executionMode.isRecording())
                {
                    final String existingJson = readAsString(this.recordingPath);
                    if (existingJson != null && existingJson.trim().equals(candidateJson.trim()))
                    {
                        LOGGER.info("Session finished with success and 0 step changes. Skipping playbook recording write to {}", this.recordingPath);
                        return;
                    }
                    if (existingJson != null)
                    {
                        LOGGER.info("Session finished with success and healed step updates. Overwriting playbook recording at {}", this.recordingPath);
                    }
                }

                LOGGER.info("Writing candidate playbook recording to {}", this.recordingPath);
                this.resourceManager.write(this.recordingPath, candidateJson);
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to write playbook recording to {}", this.recordingPath, e);
            }
        }
    }

    private String readAsString(final String path)
    {
        try (final java.io.InputStream in = this.resourceManager.read(path))
        {
            if (in == null)
            {
                return null;
            }
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        catch (final Exception e)
        {
            return null;
        }
    }
}



