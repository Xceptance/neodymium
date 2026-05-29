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

public class Playbook {
    
    private String id;

    private List<PlaybookStep> steps;
    private transient boolean changed = false;

    private transient int cursor;

    private transient boolean recording = true;

    public Playbook(String id)
    {
        this.id = id;
        this.steps = new ArrayList<>();
    }

    public List<PlaybookStep> getSteps() {
        return steps;
    }

    public void setSteps(List<PlaybookStep> steps) {
        this.steps = steps;
    }

    public void addStep(PlaybookStep step) {
        this.steps.add(step);
        this.changed = true;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public void markActionsReplay()
    {
        for (PlaybookStep step : this.steps)
        {
            for (Action action : step.getActions())
            {
                action.markReplay();
            }
        }
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public boolean isRecording()
    {
        return this.recording;
    }

    public void setRecording(boolean recording)
    {
        this.recording = recording;
    }

    public void removeFutureSteps()
    {
        if (steps.size() > cursor)
        {
            steps.subList(cursor, steps.size()).clear();
        }
        this.changed = true;
    }

    public void nextStep()
    {
        cursor++;
    }

    public void setCursor(int cursor)
    {
        this.cursor = cursor;
    }

    /**
     * Returns the current cursor position (zero-based index of the next step to execute).
     * Steps at indices {@code 0} to {@code getCursor() - 1} have already been completed.
     *
     * @return the current cursor position
     */
    public int getCursor()
    {
        return cursor;
    }

    public PlaybookStep getCurrentStep()
    {
        PlaybookStep step;

        if (steps.size() > cursor)
        {
            step = steps.get(cursor);
        }
        else
        {
            step = new PlaybookStep();
            steps.add(step);
            changed = true;
        }
        return step;
    }

    @Override
    public String toString()
    {
        return "Playbook [id=" + id + ", steps=" + steps + ", changed=" + changed + ", cursor=" + cursor + ", recording=" + recording + "]";
    }

}
