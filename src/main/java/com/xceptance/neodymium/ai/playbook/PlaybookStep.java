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
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.core.ContextLevel;

public class PlaybookStep {

    private String promptLine;
    private String reasoning;
    private List<Action> actions;

    private String screenshotHash;

    private boolean expectedFailure;
    private String bugId;
    private String expectedErrorType;
    private String expectedErrorMessage;

    private transient String lastFailure;

    /**
     * The context level that was successfully used for this step.
     * {@code null} if not yet recorded. Used to skip predictable escalation
     * failures on subsequent runs and healing attempts.
     */
    private ContextLevel healedContextLevel;

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

    public String getScreenshotHash()
    {
        return screenshotHash;
    }

    public void setScreenshotHash(final String screenshotHash)
    {
        this.screenshotHash = screenshotHash;
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

    /**
     * Returns the context level that was needed when this step was last
     * successfully healed, or {@code null} if this step was never healed.
     *
     * @return the healed context level, or {@code null}
     */
    public ContextLevel getHealedContextLevel()
    {
        return healedContextLevel;
    }

    /**
     * Records the context level that was needed for healing. This allows
     * subsequent healing attempts to start at this level instead of
     * {@link ContextLevel#LEAN}, avoiding a predictable wasted call.
     *
     * @param level the context level that succeeded during healing
     */
    public void setHealedContextLevel(final ContextLevel level)
    {
        this.healedContextLevel = level;
    }

    public boolean isExpectedFailure()
    {
        return expectedFailure;
    }

    public void setExpectedFailure(final boolean expectedFailure)
    {
        this.expectedFailure = expectedFailure;
    }

    public String getBugId()
    {
        return bugId;
    }

    public void setBugId(final String bugId)
    {
        this.bugId = bugId;
    }

    public String getExpectedErrorType()
    {
        return expectedErrorType;
    }

    public void setExpectedErrorType(final String expectedErrorType)
    {
        this.expectedErrorType = expectedErrorType;
    }

    public String getExpectedErrorMessage()
    {
        return expectedErrorMessage;
    }

    public void setExpectedErrorMessage(final String expectedErrorMessage)
    {
        this.expectedErrorMessage = expectedErrorMessage;
    }

    @Override
    public String toString()
    {
        return "PlaybookStep [promptLine=" + promptLine + ", reasoning=" + reasoning + ", actions=" + actions + ", screenshotHash=" + screenshotHash + ", expectedFailure=" + expectedFailure + ", bugId=" + bugId + ", expectedErrorType=" + expectedErrorType + ", expectedErrorMessage=" + expectedErrorMessage + "]";
    }
}
