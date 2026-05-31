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
package com.xceptance.neodymium.ai.playbook;

import java.util.ArrayList;
import java.util.List;
import com.xceptance.neodymium.ai.action.Action;

/**
 * Represents an execution playbook containing a sequence of {@link PlaybookStep} instructions.
 * Provides APIs to track state, manage step-by-step progress, rewind historical execution,
 * and facilitate test recording and replay modes.
 *
 * @author AI-generated: Gemini 2.5 Flash
 */
public final class Playbook
{
    private String id;
    private List<PlaybookStep> steps;
    private transient boolean changed = false;
    private transient int cursor;
    private transient boolean recording = true;

    /**
     * Constructs a new Playbook with the specified unique identifier.
     *
     * @param id the unique ID of this playbook (usually derived from test class and method names)
     */
    public Playbook(final String id)
    {
        this.id = id;
        this.steps = new ArrayList<>();
    }

    /**
     * Gets the list of playbook steps.
     *
     * @return the list of {@link PlaybookStep}s
     */
    public List<PlaybookStep> getSteps()
    {
        return this.steps;
    }

    /**
     * Sets the list of playbook steps.
     *
     * @param steps the new list of {@link PlaybookStep}s
     */
    public void setSteps(final List<PlaybookStep> steps)
    {
        this.steps = steps;
    }

    /**
     * Adds a single playbook step to the sequence and marks the playbook as modified.
     *
     * @param step the {@link PlaybookStep} to append
     */
    public void addStep(final PlaybookStep step)
    {
        this.steps.add(step);
        this.changed = true;
    }

    /**
     * Checks if this playbook has been modified during the execution.
     *
     * @return {@code true} if modified, {@code false} otherwise
     */
    public boolean isChanged()
    {
        return this.changed;
    }

    /**
     * Explicitly sets the modified state of this playbook.
     *
     * @param changed the modified status to set
     */
    public void setChanged(final boolean changed)
    {
        this.changed = changed;
    }

    /**
     * Marks all actions in all steps as replay actions.
     * This prepares the actions for subsequent offline dHash-cached evaluations.
     */
    public void markActionsReplay()
    {
        for (final PlaybookStep step : this.steps)
        {
            for (final Action action : step.getActions())
            {
                action.markReplay();
            }
        }
    }

    /**
     * Gets the unique identifier of this playbook.
     *
     * @return the playbook identifier string
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * Sets the unique identifier of this playbook.
     *
     * @param id the new playbook identifier string to set
     */
    public void setId(final String id)
    {
        this.id = id;
    }

    /**
     * Checks if the playbook is currently in recording mode.
     *
     * @return {@code true} if recording is enabled, {@code false} if operating in replay review mode
     */
    public boolean isRecording()
    {
        return this.recording;
    }

    /**
     * Sets the recording mode of the playbook.
     *
     * @param recording the recording state to set
     */
    public void setRecording(final boolean recording)
    {
        this.recording = recording;
    }

    /**
     * Discards any planned steps located ahead of the current execution cursor.
     * This is useful when the execution rewinds and branches into a different instruction flow.
     */
    public void removeFutureSteps()
    {
        if (this.steps.size() > this.cursor)
        {
            this.steps.subList(this.cursor, this.steps.size()).clear();
        }
        this.changed = true;
    }

    /**
     * Advances the execution cursor to the next step.
     */
    public void nextStep()
    {
        this.cursor++;
    }

    /**
     * Sets the execution cursor position to the specified step index.
     *
     * @param cursor the zero-based step index to set
     */
    public void setCursor(final int cursor)
    {
        this.cursor = cursor;
    }

    /**
     * Returns the current cursor position (zero-based index of the next step to execute).
     * Steps at indices {@code 0} to {@code getCursor() - 1} have already been completed.
     *
     * @return the current cursor position index
     */
    public int getCursor()
    {
        return this.cursor;
    }

    /**
     * Retrieves or instantiates the {@link PlaybookStep} at the current cursor position.
     * If the cursor points beyond the current steps size, a new step is automatically appended.
     *
     * @return the active {@link PlaybookStep} at the current cursor
     */
    public PlaybookStep getCurrentStep()
    {
        final PlaybookStep step;
        if (this.steps.size() > this.cursor)
        {
            step = this.steps.get(this.cursor);
        }
        else
        {
            step = new PlaybookStep();
            this.steps.add(step);
            this.changed = true;
        }
        return step;
    }

    @Override
    public String toString()
    {
        return "Playbook [id=" + id + ", steps=" + steps + ", changed=" + changed + ", cursor=" + cursor + ", recording=" + recording + "]";
    }
}
