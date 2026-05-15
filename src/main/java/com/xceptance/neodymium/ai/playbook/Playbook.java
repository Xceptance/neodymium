/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
  *
 * // AI-generated: Gemini 2.0 Flash
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
