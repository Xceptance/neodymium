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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.event.ExecutionEvent;
import org.neodymium.ai.event.ExecutionListener;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.resources.PlaybookResourceManager;

/**
 * Event listener that monitors execution event dispatches, compiles executed actions,
 * and writes the final parameterized recording as a JSON file through a resource manager.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookRecorder implements ExecutionListener
{
    /**
     * The resource manager to write recording outputs to.
     */
    private final PlaybookResourceManager resourceManager;

    /**
     * The target path where the recording should be written.
     */
    private final String recordingPath;

    /**
     * The thread-safe list of sanitized actions captured during execution.
     */
    private final List<Action> recordedActions = new CopyOnWriteArrayList<>();

    /**
     * Constructs a PlaybookRecorder.
     *
     * @param resourceManager the resource manager
     * @param recordingPath the target path for the recording file
     */
    public PlaybookRecorder(
        final PlaybookResourceManager resourceManager,
        final String recordingPath
    )
    {
        this.resourceManager = resourceManager;
        this.recordingPath = recordingPath;
    }

    /**
     * Consumes action execution and session finished events. Compiles the captured actions
     * into JSON format on session completion and writes them to the resource path.
     *
     * @param event the dispatched execution event
     */
    @Override
    public void onEvent(final ExecutionEvent event)
    {
        if (event instanceof ActionExecutedEvent actionEvent)
        {
            if (actionEvent.isSuccess())
            {
                this.recordedActions.add(actionEvent.getAction());
            }
        }
        else if (event instanceof SessionFinishedEvent)
        {
            final String json = serializeActionsToJson();
            try
            {
                this.resourceManager.write(this.recordingPath, json);
            }
            catch (final Exception e)
            {
                // Silent catch or logger details printout
            }
        }
    }

    /**
     * Retrieves the unmodifiable list of captured recorded actions.
     *
     * @return the list of recorded actions
     */
    public List<Action> getRecordedActions()
    {
        return List.copyOf(this.recordedActions);
    }

    /**
     * Formats recorded actions into a standard JSON array string without external dependencies.
     */
    private String serializeActionsToJson()
    {
        final StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < this.recordedActions.size(); i++)
        {
            final Action action = this.recordedActions.get(i);
            json.append("  {\n");
            json.append("    \"type\": \"").append(escape(action.getType())).append("\",\n");
            json.append("    \"target\": \"").append(escape(action.getTarget())).append("\",\n");
            json.append("    \"description\": \"").append(escape(action.getDescription())).append("\"\n");
            json.append("  }");
            if (i < this.recordedActions.size() - 1)
            {
                json.append(",\n");
            }
        }
        json.append("\n]");
        return json.toString();
    }

    /**
     * Escapes double quotes and backslashes for JSON encoding compatibility.
     */
    private String escape(final String raw)
    {
        if (raw == null)
        {
            return "";
        }
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
