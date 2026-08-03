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
 * only upon successful session completion.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookRecorder implements ExecutionListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaybookRecorder.class);

    /**
     * The resource manager to write recording outputs to.
     */
    private final PlaybookResourceManager resourceManager;

    /**
     * The target path where the recording should be written.
     */
    private final String recordingPath;

    /**
     * The list of playbook steps to record and serialize.
     */
    private final List<PlaybookStep> playbookSteps;

    /**
     * Constructs a PlaybookRecorder.
     *
     * @param resourceManager the resource manager
     * @param recordingPath the target path for the recording file
     * @param playbookSteps the steps list to record and serialize
     */
    public PlaybookRecorder(
        final PlaybookResourceManager resourceManager,
        final String recordingPath,
        final List<PlaybookStep> playbookSteps
    )
    {
        this.resourceManager = resourceManager;
        this.recordingPath = recordingPath;
        this.playbookSteps = playbookSteps;
    }

    /**
     * Consumes session finished events to serialize and write the steps to resource path
     * if and only if the session finished successfully.
     *
     * @param event the dispatched execution event
     */
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
                final String json = mapper.writeValueAsString(this.playbookSteps);
                this.resourceManager.write(this.recordingPath, json);
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to write playbook recording to {}", this.recordingPath, e);
            }
        }
    }
}

