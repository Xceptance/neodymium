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
package com.xceptance.neodymium.ai.action;

import java.util.List;

import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;

/**
 * Interface for AI actions that can be plugged into the Neodymium AI engine.
  *
 * @author AI-generated: Gemini 2.5 Flash
*/
public interface AiActionPlugin {

    /**
     * @return the unique name/type of this action, e.g. "CLICK", "NAVIGATE"
     */
    String getActionName();

    /**
     * Evaluates a user instruction to see if it matches this action directly.
     * Return an Action object with type, target, and value set, or null if it doesn't match.
     */
    List<Action> parseDirectInstruction(String instruction);

    /**
     * Determine if this action requires the LLM to generate actions or if it can be 
     * simply replayed from a playbook/direct execution without AI processing.
     * In most cases, this is false, unless the action uses AI internally.
     */
    default boolean requiresLlm(Action action) {
        return false;
    }

    /**
     * Determine if this action requires the LLM to generate actions, with executor context.
     * Overrides can use the executor to perform local checks.
     */
    default boolean requiresLlm(Action action, ActionExecutor actionExecutor) {
        return requiresLlm(action);
    }

    /**
     * Prepare phase for the action. Called before any screenshots are taken for the LLM.
     * Allows the plugin to inject scripts, modify DOM, or perform local comparisons.
     */
    default void prepare(Action action, ActionExecutor actionExecutor) throws ActionExecutionException {}

    /**
     * Executes the action via Selenium/Selenide.
     * 
     * @param action The parsed action with target/value/etc.
     * @param testInstance The instance of the current running test class
     * @param actionExecutor The ActionExecutor for utilizing helper methods (like findElement)
     */
    void execute(Action action, Object testInstance, ActionExecutor actionExecutor) throws ActionExecutionException;

    /**
     * Cleanup phase for the action. Called after execution.
     */
    default void cleanup(Action action, ActionExecutor actionExecutor) {}

    /**
     * Returns the markdown instructions to append to the system prompt
     * so the AI understands how and when to use this action type.
     */
    String getPromptInstructions();

    /**
     * Optional pre-execution validation for this action. E.g., verifying
     * that a targeted element is visible and interactable before continuing.
     * By default, performs no check.
     *
     * @param action The action being validated
     * @param actionExecutor The executor providing context and helper methods
     */
    default void preCheck(Action action, ActionExecutor actionExecutor) {}

    /**
     * Determine if this action requires a screenshot to be sent to the LLM.
     * Usually false, unless the action requires visual analysis.
     */
    default boolean requiresScreenshot(Action action) {
        return false;
    }
}
