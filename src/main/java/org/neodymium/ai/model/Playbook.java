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
package org.neodymium.ai.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable root container representing a parsed playbook with its logical steps
 * and parameter datasets.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class Playbook
{
    private final List<PlaybookStep> steps;
    private final List<Map<String, SessionData.DataEntry>> dataSets;

    /**
     * Constructs an immutable Playbook with defensive copies of steps and datasets.
     *
     * @param steps the list of logical playbook steps
     * @param dataSets the list of dataset parameters maps
     */
    public Playbook(final List<PlaybookStep> steps, final List<Map<String, SessionData.DataEntry>> dataSets)
    {
        // Defensive copy and wrap steps to ensure immutability
        this.steps = Collections.unmodifiableList(steps != null ? new ArrayList<>(steps) : new ArrayList<>());

        // Defensive copy of datasets list and individual dataset parameter maps
        final List<Map<String, SessionData.DataEntry>> datasetsCopy = new ArrayList<>();
        if (dataSets != null)
        {
            for (final Map<String, SessionData.DataEntry> map : dataSets)
            {
                if (map != null)
                {
                    datasetsCopy.add(Collections.unmodifiableMap(new HashMap<>(map)));
                }
            }
        }
        this.dataSets = Collections.unmodifiableList(datasetsCopy);
    }

    /**
     * Returns the unmodifiable list of playbook steps.
     *
     * @return the unmodifiable steps list
     */
    public List<PlaybookStep> getSteps()
    {
        return this.steps;
    }

    /**
     * Returns the unmodifiable list of datasets parameter maps.
     *
     * @return the unmodifiable datasets list
     */
    public List<Map<String, SessionData.DataEntry>> getDataSets()
    {
        return this.dataSets;
    }
}
