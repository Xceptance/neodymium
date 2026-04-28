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
}
