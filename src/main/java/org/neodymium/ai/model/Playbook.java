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

    /**
     * Creates a new Builder instance for programmatically constructing a Playbook.
     *
     * @return a new Playbook.Builder
     */
    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Builder class for fluent programmatic construction of {@link Playbook} instances.
     */
    public static final class Builder
    {
        private final List<PlaybookStep> steps = new ArrayList<>();
        private final List<Map<String, SessionData.DataEntry>> dataSets = new ArrayList<>();
        private final Map<String, String> systemPromptAddons = new HashMap<>();

        private Builder()
        {
        }

        /**
         * Adds a step by instruction string.
         *
         * @param instruction the step instruction prompt
         * @return this builder instance
         */
        public Builder step(final String instruction)
        {
            if (instruction != null && !instruction.isBlank())
            {
                this.steps.add(new PlaybookStep(instruction));
            }
            return this;
        }

        /**
         * Adds an included sub-playbook by relative path.
         *
         * @param includeRelativePath the relative path to the included playbook file
         * @return this builder instance
         */
        public Builder include(final String includeRelativePath)
        {
            if (includeRelativePath != null && !includeRelativePath.isBlank())
            {
                final String rawPath = includeRelativePath.startsWith("_include:") || includeRelativePath.startsWith("include:")
                    ? includeRelativePath.substring(includeRelativePath.indexOf(':') + 1).trim()
                    : includeRelativePath.trim();
                final String formattedStep = "_include: " + rawPath;
                final PlaybookStep includeStep = new PlaybookStep(formattedStep);
                try
                {
                    org.neodymium.ai.resources.PlaybookResourceManager manager = new org.neodymium.ai.resources.ClasspathResourceManager();
                    try (final java.io.InputStream in = manager.read(rawPath))
                    {
                        // Classpath resource found
                    }
                    catch (final java.io.IOException ioe)
                    {
                        manager = new org.neodymium.ai.resources.LocalFileResourceManager(java.nio.file.Path.of("."));
                    }
                    final Playbook subPlaybook = new org.neodymium.ai.playbook.YamlPlaybookParser().parse(rawPath, manager);
                    if (subPlaybook != null && subPlaybook.getSteps() != null)
                    {
                        includeStep.getSubSteps().addAll(subPlaybook.getSteps());
                    }
                }
                catch (final Exception e)
                {
                    // Fall back to unexpanded step if path is resolved dynamically at runtime
                }
                this.steps.add(includeStep);
            }
            return this;
        }

        /**
         * Adds a {@link PlaybookStep}.
         *
         * @param step the playbook step
         * @return this builder instance
         */
        public Builder step(final PlaybookStep step)
        {
            if (step != null)
            {
                this.steps.add(step);
            }
            return this;
        }

        /**
         * Adds a list of {@link PlaybookStep} instances.
         *
         * @param steps the list of steps to add
         * @return this builder instance
         */
        public Builder steps(final List<PlaybookStep> steps)
        {
            if (steps != null)
            {
                this.steps.addAll(steps);
            }
            return this;
        }

        /**
         * Builds and returns the immutable {@link Playbook} instance.
         *
         * @return a new Playbook
         */
        public Playbook build()
        {
            return new Playbook(this.steps, this.dataSets, this.systemPromptAddons);
        }
    }
}

