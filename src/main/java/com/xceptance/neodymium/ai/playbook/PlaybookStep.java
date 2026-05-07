package com.xceptance.neodymium.ai.playbook;

import java.util.ArrayList;
import java.util.List;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;

public class PlaybookStep {

    private String promptLine;
    private String reasoning;
    private List<Action> actions;

    private transient String lastFailure;

    public PlaybookStep() {
        this.actions = new ArrayList<Action>();
    }

    public PlaybookStep(String promptLine, String reasoning, List<Action> actions)
    {
        this.promptLine = promptLine;
        this.reasoning = reasoning;
        this.actions = actions;
    }

    public String getPromptLine() {
        return promptLine;
    }

    public void setPromptLine(String promptLine) {
        this.promptLine = promptLine;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public boolean failed()
    {
        return lastFailure != null;
    }

    public void setFailure(ActionExecutionException e)
    {
        if (e != null)
        {
            this.lastFailure = e.getMessage();
        }
    }
    
    public String getLastFailure()
    {
        return lastFailure;
    }

    @Override
    public String toString()
    {
        return "PlaybookStep [promptLine=" + promptLine + ", reasoning=" + reasoning + ", actions=" + actions + "]";
    }
}
