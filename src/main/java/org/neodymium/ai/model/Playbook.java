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
    /**
     * The unmodifiable sequential list of primary playbook steps.
     */
    private final List<PlaybookStep> steps;

    /**
     * The unmodifiable list of dataset maps containing test parameters.
     */
    private final List<Map<String, SessionData.DataEntry>> dataSets;

    /**
     * The unmodifiable map of custom system prompt add-ons by type.
     */
    private final Map<String, String> systemPromptAddons;

    /**
     * Constructs an immutable Playbook with defensive copies of steps and datasets.
     *
     * @param steps the list of logical playbook steps
     * @param dataSets the list of dataset parameters maps
     */
    public Playbook(final List<PlaybookStep> steps, final List<Map<String, SessionData.DataEntry>> dataSets)
    {
        this(steps, dataSets, Collections.emptyMap());
    }

    /**
     * Constructs an immutable Playbook with defensive copies of steps, datasets, and custom system prompt add-ons.
     *
     * @param steps the list of logical playbook steps
     * @param dataSets the list of dataset parameters maps
     * @param systemPromptAddons the map of custom system prompt add-ons by type
     */
    public Playbook(final List<PlaybookStep> steps, final List<Map<String, SessionData.DataEntry>> dataSets, final Map<String, String> systemPromptAddons)
    {
        // Defensive copy of steps
        this.steps = steps != null ? new ArrayList<>(steps) : new ArrayList<>();

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
        this.systemPromptAddons = systemPromptAddons != null ? Collections.unmodifiableMap(new HashMap<>(systemPromptAddons)) : Collections.emptyMap();
    }

    /**
     * Returns the unmodifiable list of playbook steps.
     *
     * @return the unmodifiable steps list
     */
    public List<PlaybookStep> getSteps()
    {
        return Collections.unmodifiableList(this.steps);
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

    /**
     * Returns the unmodifiable map of custom system prompt add-ons by type.
     *
     * @return the unmodifiable system prompt add-ons map
     */
    public Map<String, String> getSystemPromptAddons()
    {
        return this.systemPromptAddons;
    }
}

