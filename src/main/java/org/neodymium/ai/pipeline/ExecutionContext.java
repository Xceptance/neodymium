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
package org.neodymium.ai.pipeline;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.structural.EndTryStep;

/**
 * Thread-isolated execution context tracking the LIFO step execution queue,
 * session context, transient runtime variables, and active recording metadata.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ExecutionContext
{
    private static final ThreadLocal<ExecutionContext> activeContext = new ThreadLocal<>();

    /**
     * Retrieves the thread-active ExecutionContext.
     *
     * @return the active context, or null if none is bound to the current thread
     */
    public static ExecutionContext getActiveContext()
    {
        return activeContext.get();
    }

    /**
     * Binds or unbinds the ExecutionContext to the current thread.
     *
     * @param context the context to bind, or null to unbind
     */
    public static void setActiveContext(final ExecutionContext context)
    {
        if (context == null)
        {
            activeContext.remove();
        }
        else
        {
            activeContext.set(context);
        }
    }

    /**
     * Context transient data map keys.
     */
    public static final String KEY_SESSION = "session";
    public static final String KEY_TARGET_EXECUTOR = "targetExecutor";
    public static final String KEY_LAST_LLM_RESULT = "lastLlmResult";
    public static final String KEY_LAST_STATE = "lastState";
    public static final String KEY_RECORDING = "recording";
    public static final String KEY_RESOURCE_MANAGER = "resourceManager";
    public static final String KEY_PLAYBOOK_PARSER = "playbookParser";
    public static final String KEY_RUNTIME_INCLUDE_STACK = "runtimeIncludeStack";
    public static final String KEY_CURRENT_PLAYBOOK_IDENTIFIER = "currentPlaybookIdentifier";
    public static final String KEY_CURRENT_PLAYBOOK_STEP = "currentPlaybookStep";
    public static final String KEY_CURRENT_INSTRUCTION = "currentInstruction";
    public static final String KEY_ACTIVE_PROMPT = "activePrompt";
    public static final String KEY_CURRENT_STEP_ACTIONS = "currentStepActions";
    public static final String KEY_VERIFICATION_TOKEN_USAGE = "verificationTokenUsage";
    public static final String KEY_STANDARD_TOKEN_USAGE = "standardTokenUsage";
    public static final String KEY_PESAP_TOKEN_USAGE = "pesapTokenUsage";
    public static final String KEY_JUDGE_TOKEN_USAGE = "judgeTokenUsage";
    public static final String KEY_RCA_TOKEN_USAGE = "rcaTokenUsage";
    public static final String KEY_STANDARD_CALL_COUNT = "standardCallCount";
    public static final String KEY_VERIFICATION_CALL_COUNT = "verificationCallCount";
    public static final String KEY_PESAP_CALL_COUNT = "pesapCallCount";
    public static final String KEY_JUDGE_CALL_COUNT = "judgeCallCount";
    public static final String KEY_RCA_CALL_COUNT = "rcaCallCount";
    public static final String KEY_TOTAL_LLM_CALLS = "totalLlmCalls";
    public static final String KEY_TOTAL_REPLAYS = "totalReplays";
    public static final String KEY_CURRENT_CONTEXT_LEVEL = "currentContextLevel";
    public static final String KEY_SEMANTIC_DIFF_SUMMARY = "semanticDiffSummary";
    public static final String KEY_VISUAL_RCA_EXPLANATION = "visualRcaExplanation";
    public static final String KEY_VISUAL_RCA_SUMMARY = "visualRcaSummary";
    public static final String KEY_EXECUTION_MODE = "executionMode";
    public static final String KEY_LAST_EXECUTION_ERROR = "lastExecutionError";
    public static final String KEY_ACTIVE_DATASET_LABEL = "activeDatasetLabel";
    public static final String KEY_PLAYBOOK = "playbook";
    public static final String KEY_ACTIVE_MODEL = "activeModel";
    public static final String KEY_EXECUTION_WARNINGS = "executionWarnings";
    public static final String KEY_VERIFICATION_WARNINGS = "verificationWarnings";
    public static final String KEY_YAML_MISMATCH_WARNING = "yamlMismatchWarning";
    public static final String KEY_IS_HEALED_STEP = "isHealedStep";
    public static final String KEY_INTERNAL_CACHE_HITS = "internalCacheHits";
    public static final String KEY_TOKEN_BUDGET_INPUT = "tokenBudgetInput";
    public static final String KEY_TOKEN_BUDGET_OUTPUT = "tokenBudgetOutput";
    public static final String KEY_PESAP_INTENT = "pesapIntent";

    /**
     * The LIFO execution stack containing steps yet to be processed.
     */
    private final Deque<PipelineStep> runStack = new ArrayDeque<>();

    /**
     * The active session data container.
     */
    private final SessionData sessionData;

    /**
     * The try-catch blocks hierarchy stack.
     */
    private final Deque<PipelineStep> tryCatchStack = new ArrayDeque<>();

    /**
     * Thread-safe map for transient runtime state/variables.
     */
    private final Map<String, Object> transientData = new ConcurrentHashMap<>();

    /**
     * Thread-safe map containing recording/auditing metadata.
     */
    private final Map<String, Object> recordingMetadata = new ConcurrentHashMap<>();

    /**
     * Constructs an ExecutionContext with the specified session variables.
     *
     * @param sessionData the active session data
     */
    public ExecutionContext(final SessionData sessionData)
    {
        this.sessionData = sessionData;
    }

    /**
     * Pushes a step onto the LIFO execution stack.
     *
     * @param step the step to push
     */
    public void pushStep(final PipelineStep step)
    {
        if (step != null)
        {
            this.runStack.push(step);
        }
    }

    /**
     * Pops the top step from the execution stack.
     *
     * @return the popped step, or null if the stack is empty
     */
    public PipelineStep popStep()
    {
        return this.runStack.isEmpty() ? null : this.runStack.pop();
    }

    /**
     * Checks if the execution stack contains any remaining steps.
     *
     * @return true if steps exist, false otherwise
     */
    public boolean hasSteps()
    {
        return !this.runStack.isEmpty();
    }

    /**
     * Retrieves the session data container.
     *
     * @return the session data
     */
    public SessionData getSessionData()
    {
        return this.sessionData;
    }

    /**
     * Retrieves the thread-safe transient data map.
     *
     * @return the transient data map
     */
    public Map<String, Object> getTransientData()
    {
        return this.transientData;
    }

    /**
     * Retrieves the thread-safe recording metadata map.
     *
     * @return the recording metadata map
     */
    public Map<String, Object> getRecordingMetadata()
    {
        return this.recordingMetadata;
    }

    /**
     * Pushes a try-catch step onto the active try-catch hierarchy stack.
     *
     * @param tryCatch the try-catch step
     */
    public void pushTryCatch(final PipelineStep tryCatch)
    {
        if (tryCatch != null)
        {
            this.tryCatchStack.push(tryCatch);
        }
    }

    /**
     * Pops the top try-catch step from the active hierarchy stack.
     *
     * @return the popped try-catch step, or null if empty
     */
    public PipelineStep popTryCatch()
    {
        return this.tryCatchStack.isEmpty() ? null : this.tryCatchStack.pop();
    }

    /**
     * Peeks at the active try-catch step at the top of the hierarchy stack.
     *
     * @return the active try-catch step, or null if none
     */
    public PipelineStep peekTryCatch()
    {
        return this.tryCatchStack.isEmpty() ? null : this.tryCatchStack.peek();
    }

    /**
     * Discards all pending steps on the execution stack up to and including the
     * boundary marker matching the specified try-catch block.
     *
     * @param tryCatch the try-catch block to discard steps for
     */
    public void discardStepsUpToTryCatch(final PipelineStep tryCatch)
    {
        while (!this.runStack.isEmpty())
        {
            final PipelineStep popped = this.runStack.pop();
            if (popped instanceof EndTryStep endTry)
            {
                if (endTry.getTryCatchStep() == tryCatch)
                {
                    break;
                }
            }
        }
    }

    /**
     * Clears all pending steps from the execution stack.
     */
    public void clearSteps()
    {
        this.runStack.clear();
    }
}
