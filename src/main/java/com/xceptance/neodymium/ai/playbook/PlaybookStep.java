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
