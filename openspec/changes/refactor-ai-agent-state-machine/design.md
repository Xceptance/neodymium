# Design: AI Agent State Machine & Pluggable Capability Routing

This document defines the technical architecture for refactoring the `com.xceptance.neodymium.ai` subsystem from a monolithic procedural loop to a strongly-typed state machine with thread-isolated execution context and pluggable capability-based LLM routing.

---

## Context

The current `AiAgent` execution engine runs as a monolithic procedural loop (over 3,300 LOC) that manages execution, playback, retries, and HUD events. 
It relies heavily on static `ThreadLocal` variables to pass context between execution steps, inclusion stacks, and condition evaluators. This prevents parallel execution and causes state pollution and memory leaks.
Furthermore, the loop counter is directly mutated inside catch blocks to handle interactive HUD actions (rewind, skip, edit), making the flow brittle and untestable.
We are also introducing new features:
1. **Dynamic Placeholder Resolution** (as defined in `playbook-placeholder-variables`): Resolving `${variable}` placeholders at replay time.
2. **Dual-Context Resolution** (as defined in `fresh-approach-to-test-scripts-and-test-documentation`): Guarding sensitive data by replacing it with mock stand-ins in prompts sent to external LLMs while using raw native values for local browser execution.
3. **Step Location Tracing**: Tracking included step file names and lines for detailed error messages.

This refactoring decouples these concerns into a clean, testable state machine structure.

---

## Goals / Non-Goals

**Goals:**
- **Thread Isolation:** Eliminate all static `ThreadLocal` variables in the AI subsystem. All state must be encapsulated within a session-scoped `ExecutionContext`.
- **State Machine Architecture:** Model step execution, playback evaluation, action execution, and retry coordination as explicit, strongly-typed state transitions.
- **SUT Browser State Recovery:** Explicitly handle browser state mutation by providing a structured recovery transition to restore the SUT to a known checkpoint before retrying.
- **Pluggable LLM Routing:** Route LLM calls based on declared provider capabilities (e.g. `VISION`, `STRUCTURED_JSON`) with a default fallback provider (General Handler).
- **Dual-Context & Sanitization Integration:** Ensure the state machine resolves prompts using the Guarded Map for LLM calls and the Native Map for execution, and replaces dynamic/sensitive values with `${placeholder}` templates on recording.
- **Decoupled Event-Driven HUD:** Use a structured `HudAction` model and `HudCommunicator` interface instead of throwing control-flow exceptions. The HUD communicator acts strictly as an I/O bridge, only communicating with the `StateMachineRunner` (State layer) via events, and has no direct access or connection to `ExecutionContext`, `Playbook`, or any lower layers.

**Non-Goals:**
- Re-writing the YAML/Markdown parsing engines.
- Modifying the underlying Selenium/WebDriver implementation.

---

## Decisions

### Decision 1: Structured Steps & Thread-Isolated Execution Context

To support dynamic step lifetimes, appearing/disappearing steps, splitting, rewinding/retrying, and parallel execution, we introduce `ExecutionContext` which manages a dynamic list of structured `AiStep` elements instead of a raw `List<String>`.

#### `AiStepStatus.java`
```java
package com.xceptance.neodymium.ai.core;

/**
 * Execution status for a structured AI step.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public enum AiStepStatus
{
    PENDING,
    EXECUTING,
    SUCCESS,
    FAILED,
    SKIPPED,
    SPLIT_PARENT,
    NON_EXECUTABLE
}
```

#### `AiStep.java`
```java
package com.xceptance.neodymium.ai.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a structured step in the AI Agent's execution flow.
 * Supports execution status, parenting for splits, and visibility controls.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class AiStep
{
    private final String id;
    private String instruction;
    private AiStepStatus status = AiStepStatus.PENDING;
    private int lineNumber = -1;
    private String fileName;
    
    // Parenting / Splitting references
    private String parentStepId;
    private final List<String> childStepIds = new ArrayList<>();
    
    // Original unsplit compound instruction
    private String originalUnsplitInstruction;

    public AiStep(final String id, final String instruction)
    {
        this.id = id;
        this.instruction = instruction;
    }

    public String getId()
    {
        return id;
    }

    public String getInstruction()
    {
        return instruction;
    }

    public void setInstruction(final String instruction)
    {
        this.instruction = instruction;
    }

    public AiStepStatus getStatus()
    {
        return status;
    }

    public void setStatus(final AiStepStatus status)
    {
        this.status = status;
    }

    public int getLineNumber()
    {
        return lineNumber;
    }

    public void setLineNumber(final int lineNumber)
    {
        this.lineNumber = lineNumber;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(final String fileName)
    {
        this.fileName = fileName;
    }

    public String getParentStepId()
    {
        return parentStepId;
    }

    public void setParentStepId(final String parentStepId)
    {
        this.parentStepId = parentStepId;
    }

    public List<String> getChildStepIds()
    {
        return childStepIds;
    }

    public String getOriginalUnsplitInstruction()
    {
        return originalUnsplitInstruction;
    }

    public void setOriginalUnsplitInstruction(final String originalUnsplitInstruction)
    {
        this.originalUnsplitInstruction = originalUnsplitInstruction;
    }

    /**
     * Resolves whether this step is executable based on its status/type.
     * Note: Keep in mind during implementation that branch statements might interact with split steps 
     * (e.g. split parent states, skipped steps, or non-executable metadata/comment steps).
     */
    public boolean isExecutable()
    {
        return status != AiStepStatus.SPLIT_PARENT 
            && status != AiStepStatus.SKIPPED 
            && status != AiStepStatus.NON_EXECUTABLE;
    }
}
```

#### `ExecutionContext.java`
```java
package com.xceptance.neodymium.ai.core;

import java.util.ArrayList;
import java.util.List;
import com.xceptance.neodymium.ai.playbook.Playbook;

/**
 * Carries the isolated execution state for a browser session, managing
 * dynamic step insertions, splits, status tracking, and rewind cleanups.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class ExecutionContext
{
    private final List<AiStep> stepsList = new ArrayList<>();
    private final Playbook playbook;
    private final AiExecutionResult result;
    private final List<String> includeStack = new ArrayList<>();
    private Boolean lastConditionResult = null;
    private int stepCursor = 0;

    public ExecutionContext(final List<AiStep> steps, final Playbook playbook, final AiExecutionResult result)
    {
        this.stepsList.addAll(steps);
        this.playbook = playbook;
        this.result = result;
    }

    public List<AiStep> getStepsList()
    {
        return stepsList;
    }

    public Playbook getPlaybook()
    {
        return playbook;
    }

    public AiExecutionResult getResult()
    {
        return result;
    }

    public List<String> getIncludeStack()
    {
        return includeStack;
    }

    public Boolean getLastConditionResult()
    {
        return lastConditionResult;
    }

    public void setLastConditionResult(final Boolean result)
    {
        this.lastConditionResult = result;
    }

    public int getStepCursor()
    {
        return stepCursor;
    }

    public void setStepCursor(final int stepCursor)
    {
        this.stepCursor = stepCursor;
    }

    public AiStep getCurrentStep()
    {
        while (stepCursor < stepsList.size() && !stepsList.get(stepCursor).isExecutable())
        {
            stepCursor++;
        }
        return stepCursor < stepsList.size() ? stepsList.get(stepCursor) : null;
    }

    public void insertStep(final int index, final AiStep step)
    {
        this.stepsList.add(index, step);
    }

    public void splitStep(final String parentId, final String firstPartInstruction, final String secondPartInstruction)
    {
        // 1. Locate parent step
        AiStep parent = null;
        int parentIdx = -1;
        for (int i = 0; i < stepsList.size(); i++)
        {
            if (stepsList.get(i).getId().equals(parentId))
            {
                parent = stepsList.get(i);
                parentIdx = i;
                break;
            }
        }
        if (parent == null)
        {
            return;
        }

        // 2. Deactivate parent step
        parent.setStatus(AiStepStatus.SPLIT_PARENT);
        if (parent.getOriginalUnsplitInstruction() == null)
        {
            parent.setOriginalUnsplitInstruction(parent.getInstruction());
        }

        // 3. Create and link child steps
        final AiStep childA = new AiStep(parent.getId() + "_part1", firstPartInstruction);
        childA.setParentStepId(parent.getId());
        childA.setOriginalUnsplitInstruction(parent.getOriginalUnsplitInstruction());
        childA.setLineNumber(parent.getLineNumber());
        childA.setFileName(parent.getFileName());

        final AiStep childB = new AiStep(parent.getId() + "_part2", secondPartInstruction);
        childB.setParentStepId(parent.getId());
        childB.setOriginalUnsplitInstruction(parent.getOriginalUnsplitInstruction());
        childB.setLineNumber(parent.getLineNumber());
        childB.setFileName(parent.getFileName());

        parent.getChildStepIds().add(childA.getId());
        parent.getChildStepIds().add(childB.getId());

        // 4. Insert after parent
        insertStep(parentIdx + 1, childA);
        insertStep(parentIdx + 2, childB);
    }

    public void rewindTo(final int targetIndex)
    {
        if (targetIndex < 0 || targetIndex >= stepsList.size())
        {
            return;
        }

        // Clean up subsequent dynamic steps and revert splits
        final List<AiStep> toRemove = new ArrayList<>();
        for (int i = stepsList.size() - 1; i >= targetIndex; i--)
        {
            final AiStep step = stepsList.get(i);
            // If the step is a child of a split that starts before/at targetIndex, we remove it and restore parent
            if (step.getParentStepId() != null)
            {
                toRemove.add(step);
                final AiStep parent = findStepById(step.getParentStepId());
                if (parent != null)
                {
                    parent.setStatus(AiStepStatus.PENDING);
                    parent.getChildStepIds().remove(step.getId());
                }
            }
            else if (step.getId().contains("_dynamic")) // dynamically appeared step
            {
                toRemove.add(step);
            }
            else
            {
                step.setStatus(AiStepStatus.PENDING);
            }
        }
        stepsList.removeAll(toRemove);
        this.stepCursor = targetIndex;

        // Truncate playbook history matching targetIndex
        if (playbook != null && playbook.getSteps().size() > targetIndex)
        {
            playbook.getSteps().subList(targetIndex, playbook.getSteps().size()).clear();
            playbook.setChanged(true);
        }
    }

    private AiStep findStepById(final String id)
    {
        for (final AiStep step : stepsList)
        {
            if (step.getId().equals(id))
            {
                return step;
            }
        }
        return null;
    }
}
```

---


### Decision 2: State Machine Transition Engine

The execution loop is driven by a runner that executes states sequentially:

```java
package com.xceptance.neodymium.ai.state;

import com.xceptance.neodymium.ai.core.ExecutionContext;

/**
 * Executor that drives the state machine transitions.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class StateMachineRunner
{
    public void run(final ExecutionContext context, final State initialState)
    {
        State currentState = initialState;
        while (currentState != null)
        {
            final StateTransition transition = currentState.execute(context);
            if (transition.isTerminal())
            {
                break;
            }
            currentState = transition.nextState();
        }
    }
}
```

#### State Definitions
- **`ResolvePlaybackState`**:
  Enforces the **Three-Phase Step Routing** hierarchy. 
  1. *Playbook Replay Phase*: Inspects the active `Playbook`. If the prompt matches and visual verification (comparing live viewport dHash against cached `screenshotHash` via Hamming distance) succeeds, returns a transition to `ReplayActionsState`.
     - *Expected Failures*: If the replayed playbook step is marked as an expected failure `(bug)`, immediately throws the cached `ActionExecutionException` to simulate the bug.
     - *Cached Visual Failures*: If the replayed step has a cached visual failure signature and the live viewport dHash matches the failure dHash, fast-fails offline immediately.
  2. *Direct Action Bypass Phase*: Checks the instruction against registered direct action regular expressions. If a match occurs, bypasses LLM query and transitions to `ExecuteActionsState` with the pre-compiled regex actions.
  3. *Live LLM Query Phase*: Transitions to `ResolveLiveLlmState`.
  
- **`ResolveLiveLlmState`**:
  Obtains actions from the LLM. 
  - *Registry Query*: Selects the engine matching the capability (e.g. `VISION`, `STRUCTURED_JSON`, `STEP_SPLITTING`), falling back to the default `LlmEngine` (General Handler).
  - *Context Optimization*: Starts execution at the context level predicted by **JIT PESAP** or saved in the playbook step's **`healedContextLevel`** (Historical Context Learning).
  - *Smart Escalation Jumps*: If the LLM returns `status = ESCALATE` and specifies a `targetContext` (e.g. `VISUAL`), the runner jumps directly to that level on the next attempt.
  - *Dual-Context Prompting*: Compiles the page DOM and screenshot, resolving placeholders in the prompts using the **Guarded AI Context Map** to anonymize sensitive parameters before sending.
  - *Transition*: Parses JSON actions and transitions to `ExecuteActionsState`.

- **`ExecuteActionsState`**:
  Runs the resolved actions. 
  - *Dual-Context Resolution*: Resolves action targets and input values using the **Native Context Map** (real credentials).
  - *Programmatic Assertions (`JAVA_METHOD`)*: If the LLM generates a `JAVA_METHOD` action, it resolves and executes the target method via reflection, validating that the target method is annotated with `@AiMethod`.
  - *Transition*: On successful execution, transitions to `VerifyState`. If an exception is encountered, transitions to `EscalateRetryState`.

- **`VerifyState`**:
  Validates the final state of the SUT. Runs any post-step assertions. If verification/assertion fails, transitions to `EscalateRetryState` (for retryable errors) or `InteractiveHudWaitState` (for definitive failures requiring HUD manual overrides).

- **`EscalateRetryState`**:
  Coordinates retry loops.
  - *Two-Tier Retry Budgets*: Manages two distinct counters. Decrements the `errorCount` budget on action execution/WebDriver exceptions, and the `noActionsCount` budget when the LLM returns success but empty actions.
  - *Context Escalation*: Raises the context level for the next LLM attempt.
  - *SUT Recovery*: Invokes `SutRecoveryHandler` to restore the browser state back to a known stable checkpoint to handle state mutation side-effects before returning to `ResolveLiveLlmState`.
  - *Exhaustion*: If either budget is exhausted, transitions to `InteractiveHudWaitState` to await user interaction.

- **`InteractiveHudWaitState`**:
  Blocks on the `HudCommunicator`, receiving a `HudAction` event (Rewind, Skip, Edit). It updates the `ExecutionContext` cursor/steps (truncating playbook history on rewinds), and transitions to `ResolvePlaybackState` or `ResolveLiveLlmState` based on the user's action type.
  *   **Crucial Constraint**: The state machine and its concrete states (specifically `InteractiveHudWaitState`) MUST NOT trigger playbook saving or serialization operations (such as calling `Playbook.save()`). Playbook serialization is strictly decoupled from state transitions and is handled exclusively by the session lifecycle manager (`AiSession.close()`) during test completion.


---

### Decision 3: Pluggable Capability-Based LLM Registry, Prompt Profiles & Exception-Driven Execution

We decouple LLM interactions into an abstract model engine service (`LlmService`), group prompt templates with matched response parsers via a `PromptProfile`, support multi-stage pluggable response fixers, and communicate parsing/execution errors back to the runner via structured exceptions.

```java
package com.xceptance.neodymium.ai.core;

import java.util.List;
import java.util.Set;

/**
 * Pluggable client adapter connecting to an LLM provider model.
 * Uses a multi-turn message list to support retry context accumulation
 * (prior failed responses, error diagnostics, escalation hints).
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public interface LlmEngine
{
    /** Human-readable name of this engine (e.g. "Gemini 2.5 Flash"). */
    String getName();

    /** Capabilities supported by this engine. */
    Set<LlmCapability> getCapabilities();

    /**
     * Sends a multi-turn message list to the LLM and returns the text response.
     * The message list carries the full conversation context: system prompt,
     * prior user/assistant turns (for retry context), and the current user prompt.
     *
     * @param messages the ordered conversation messages
     * @return the text response from the LLM
     */
    String chat(final List<ChatMessage> messages);
}

package com.xceptance.neodymium.ai.prompt;

import com.google.gson.JsonObject;
import com.xceptance.neodymium.ai.action.LlmResponse;

/** Pre-processing fixer operating on raw response text. */
public interface StringResponseFixer
{
    String getName();
    String fix(final String rawText, final Exception parseError);
}

/** Structural fixer operating on deserialized GSON JSON elements. */
public interface JsonResponseFixer
{
    String getName();
    void fix(final JsonObject json, final Exception parseError);
}

/** Generic model-level fixer operating on the final parsed Java object representation. */
public interface ModelResponseFixer<T>
{
    String getName();
    T fix(final T model, final Exception parseError);
}

/** Custom parser mapping raw text responses into structured Java LlmResponse records. */
public interface LlmResponseParser
{
    LlmResponse parse(final String rawLlmResponse);
}

/** Resolved bundle carrying fully-assembled prompts with their matched parser. */
public final class PromptProfile
{
    private final String resolvedSystemPrompt;
    private final String resolvedUserPrompt;
    private final LlmResponseParser parser;

    public PromptProfile(final String resolvedSystemPrompt, final String resolvedUserPrompt, final LlmResponseParser parser)
    {
        this.resolvedSystemPrompt = resolvedSystemPrompt;
        this.resolvedUserPrompt = resolvedUserPrompt;
        this.parser = parser;
    }

    public String getResolvedSystemPrompt() { return resolvedSystemPrompt; }
    public String getResolvedUserPrompt() { return resolvedUserPrompt; }
    public LlmResponseParser getParser() { return parser; }
}

/** Registry returning PromptProfiles tailored to model engines. */
public interface PromptRegistry
{
    PromptProfile getProfile(
        final LlmTask task,
        final String engineName,
        final ExecutionContext context,
        final ContextLevel level
    );
}
```

#### Lazy Registry Initialization & Execution Flow
The `LlmService` dynamically matches the required capability set for a task, pulls the `PromptProfile` bundle from the registry, executes the chat, and runs the parsing and registered fixer pipeline:
1. **Lazy Loading**: The engines, prompt registry, and API configurations are lazily resolved only when entering `ResolveLiveLlmState`. Successful replay executions bypass LLM service loading entirely.
2. **Dynamic Capabilities**: Selection searches for engines matching the complete subset of required capabilities (`containsAll(requiredCapabilities)`). If a task requires both `VISION` and `STRUCTURED_JSON`, it selects a vision-capable JSON engine (Gemini), while text-only tasks select cheaper models (Mistral or local Llama).
3. **Structured Exceptions**: If the response is not a clean success, the service throws structured exceptions (subclasses of `LlmResponseException` like `LlmResponseParseFailureException`, `LlmResponseEscalateException`, or `LlmResponseFailureException`). Each exception carries a list of `ParserMessage` trace logs, forcing the caller (the state machine runner) to explicitly implement recovery, context escalation, and self-correction loops inside clear `catch` blocks.




---

### Decision 4: Safe SUT State Recovery

To solve the **Browser State Mutation Problem**, we define an `SutRecoveryHandler` interface:

```java
package com.xceptance.neodymium.ai.core;

/**
 * Handles browser state restoration to a known stable checkpoint.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public interface SutRecoveryHandler
{
    /** Restores the browser back to the checkpoint matching the step index. */
    void restoreToCheckpoint(final int stepIndex);
}
```
During an `EscalateRetryState` or HUD `REWIND` transition, the recovery handler is invoked to re-align SUT state before executing new instructions.

---

### Decision 5: Playbook Recording Sanitization

When recording actions in `ExecuteActionsState`, resolved dynamic variables (declared in `_dynamic`) and sensitive variables (declared in `_sensitive`) must be sanitized.
The recorder builds a reverse lookup table of:
`Resolved Runtime Value` $\rightarrow$ `${originalPlaceholder}`
It sorts this table by resolved value length descending (to avoid partial substring collisions) and replaces occurrences in action targets and values before serializing.

---

### Decision 6: Decomposed Step Execution Lifecycle & HUD Command Routing

To allow step execution to be prepared, replayed, executed, and recovered in isolation, we decompose the step loop. Instead of one single monolithic execution state, a step progresses through distinct sequential sub-states:

1. **`PrepareStepState`**: Performs preparation for step execution:
   - Takes screenshots and calculates live viewport dHash.
   - Binds the dual-context variables, resolving placeholders with Guarded AI Map values.
   - Evaluates whether offline Replay can be attempted (by matching prompt and dHash threshold). If yes, transitions to `ReplayActionsState`.
   - If not, checks if it is a direct action bypass (e.g. Navigation). If yes, transitions to `ExecuteActionsState` with prepared direct actions.
   - Otherwise, transitions to `ResolveLiveLlmState`.
2. **`ResolveLiveLlmState`**: Resolves actions via the matched `LlmEngine` and parser.
   - In case of a split response (SPLIT action returned), it invokes `ExecutionContext.splitStep(...)` to insert child steps, executes the prefix actions, and updates the step lists.
   - Transitions to `ExecuteActionsState` with the LLM-generated actions.
3. **`ReplayActionsState`**: Retrieves pre-recorded actions from the playbook step.
   - Prepares execution details and transitions to `ExecuteActionsState`.
4. **`ExecuteActionsState`**: Receives a list of actions and invokes `ActionExecutor` to run them.
   - Before execution, action parameters are resolved using the Native AI Map (raw values).
   - Dynamic parameters and assertion results are saved back to the context.
   - Transitions to `VerifyState`.
5. **`VerifyState`**: Evaluates step outcome assertions (reflective `@AiMethod` calls).
   - If all check out, updates step status to `SUCCESS`, increments the step cursor, and transitions back to `PrepareStepState` for the next step.
   - If any execution or assertion failure occurs, throws an `ActionExecutionException` or `AssertionError`, which the runner routes to `EscalateRetryState`.
6. **`EscalateRetryState`**: Manages recovery loops.
   - Increments context levels or decrements retry budgets.
   - Triggers `SutRecoveryHandler` to restore the browser to the last stable checkpoint.
   - Loops back to `PrepareStepState` for the current step cursor.
   - If budgets are exhausted, transitions to `InteractiveHudWaitState`.
7. **`InteractiveHudWaitState`**: Blocks and listens to the `HudCommunicator` for interactive IDE / HUD commands. The state machine runner receives a structured `HudAction` command event and performs the corresponding modifications on the context/steps directly, ensuring the HUD communicator itself does not touch the session context:
    - `MODIFY_STEP`: The runner modifies the instruction in `ExecutionContext`, clears previous actions, resets status to `PENDING`, and loops back to `PrepareStepState`.
    - `ADD_STEP`: The runner inserts a new `AiStep(status = PENDING)` into the context at the cursor, and routes back to `PrepareStepState`.
    - `REMOVE_STEP`: The runner marks the step in `ExecutionContext` as `SKIPPED`/`NON_EXECUTABLE`, and routes to `PrepareStepState` (advancing the cursor).
    - `REWIND_STEP`: The runner invokes `ExecutionContext.rewindTo(N)` to clean up splits/dynamic steps and reset statuses to `PENDING`, and loops back to `PrepareStepState` at the rewinded step cursor.

---

### Decision 7: Playbook Reader & Writer Serialization and Variable Enrichment

Playbook loading and saving are separated into dedicated, testable reader/writer operations:

- **`PlaybookReader`**: Loads playbook files:
   - Deserializes JSON to the raw models.
   - Enriches action lists: instantiates concrete `Action` instances from their JSON representations.
   - Prepares dHash screenshot baselines and failure caches for offline visual checks.
- **`PlaybookWriter`**: Saves playbook files:
   - Performs **Playbook Recording Sanitization**: Uses the reverse mapping of variables to detect any runtime-resolved values in action parameters and replaces them back with `${originalPlaceholder}` variables to guard sensitive/dynamic data.
   - Extracts and enriches properties: caches successful `healedContextLevel`, screenshot dHashes, and expected bug ID signatures.
   - Serializes the final sanitized/enriched `Playbook` model to JSON.

---

### Decision 8: Multi-Backend & Mixed-Mode Action Execution Extensibility

To support multiple automation backends (e.g. Selenide/Selenium, Playwright) and non-browser actions (such as direct REST API requests or database assertions) within the same session, we decouple the SUT (System Under Test) resource management from static thread-locals or hardcoded classes:

1. **`ExecutionContext` SUT Registry**:
   - `ExecutionContext` maintains a type-safe registry of session-scoped active SUT resources (drivers, HTTP clients, browser pages, connection pools):
     ```java
     private final Map<Class<?>, Object> sutResources = new HashMap<>();

     public <T> T getSutResource(final Class<T> type)
     {
         final Object resource = sutResources.get(type);
         return type.isInstance(resource) ? type.cast(resource) : null;
     }

     public <T> void registerSutResource(final Class<T> type, final T resource)
     {
         this.sutResources.put(type, resource);
     }
     ```
2. **Resource-Aware Action Execution**:
   - The thread-isolated `ActionExecutor` holds a reference to `ExecutionContext`.
   - When an `AiActionPlugin` is invoked (via `execute(action, testInstance, actionExecutor)`), it retrieves its required backend resource directly from the executor's context:
     - Browser-based Selenide/Selenium plugins can retrieve `WebDriver.class`.
     - Playwright-based plugins can retrieve `Page.class` or similar.
     - API/REST plugins can retrieve custom clients like `HttpClient.class` or `RestAssuredClient.class`.
3. **Mixed-Mode Support**:
   - Multiple resources can coexist in `sutResources`.
   - A single test run can execute a sequence of browser-based steps (e.g. login, shop interactions) and transition immediately to API-based steps (e.g. verify database order creation via REST API calls) at the end of the session, referencing the corresponding action plugins and resources.
4. **Generic SUT State Recovery**:
   - The `SutRecoveryHandler` is decoupled from WebDriver; it accesses the execution context to perform recovery actions across all active SUT resources (e.g. reloading browser pages, clearing cookies, resetting API client transaction tokens, or rolling back test database records).

---

### Decision 9: Pluggable Recovery & Diagnostic Action Communication

To handle action failures with fine-grained control and communicate details back to the execution runner, HUD, and LLM, we specify a pluggable, hierarchical recovery model:

1. **Action-Specific Retryability & Recovery (`AiActionPlugin`)**:
   Action plugins can declare if they are safe to retry and how they want their failures to be handled:
   ```java
   public enum ActionRecoveryInstruction
   {
       RETRY_WITH_SUT_RECOVERY, // Reset browser/backends and retry the action
       RETRY_IMMEDIATELY,       // Retry the action directly (transient error retry)
       FAIL_STEP,               // Instantly fail the step (trigger HUD/exception path)
       HANDLED                  // Action plugin fixed the state internally; proceed
   }

   /**
    * Returns whether this action is safe to retry.
    * If false (e.g. non-idempotent payment submission), the engine bypasses retries and fails.
    */
   default boolean isRetryable(final Action action)
   {
       return true; // default behavior
   }

   /**
    * Returns whether this action is safe to recover.
    * If false, SUT state recovery is bypassed, but immediate micro-level retries may still run.
    */
   default boolean isRecoverable(final Action action)
   {
       return true; // default behavior
   }

   /**
    * Returns the recovery instruction when this action fails.
    */
   default ActionRecoveryInstruction handleFailure(
       final Action action,
       final Exception exception,
       final ActionExecutor executor
   )
   {
       return ActionRecoveryInstruction.RETRY_WITH_SUT_RECOVERY; // default fallback
   }
   ```
2. **Diagnostic Action Failure Details**:
   We expand the static inner class `ActionExecutionException` to contain a structured `ActionExecutionFailure` details payload:
   ```java
   public final class ActionExecutionFailure
   {
       private final Action action;
       private final String errorMessage;
       private final String locatorUsed;
       private final List<String> attemptedLocators;
       private final byte[] screenshotBytes; // base64 / binary for visual audit
       private final String htmlSource;
       private final List<ParserMessage> diagnosticLogs;

       public ActionExecutionFailure(
           final Action action,
           final String errorMessage,
           final String locatorUsed,
           final List<String> attemptedLocators,
           final byte[] screenshotBytes,
           final String htmlSource,
           final List<ParserMessage> diagnosticLogs
       )
       {
           this.action = action;
           this.errorMessage = errorMessage;
           this.locatorUsed = locatorUsed;
           this.attemptedLocators = attemptedLocators;
           this.screenshotBytes = screenshotBytes;
           this.htmlSource = htmlSource;
           this.diagnosticLogs = diagnosticLogs;
       }

       // getters...
   }
   ```
   - When an action execution fails, the corresponding `AiActionPlugin` or `ActionExecutor` builds this diagnostic payload and throws it wrapped inside `ActionExecutionException`.
   - The state machine runner catches this, records the diagnostics in the execution results, propagates the failure structure to the HUD (so the HUD can render the exact failed locator, screenshots, and logs), and passes it to the `ResolveLiveLlmState` context level compilation to assist the LLM in self-correcting the failure.

---

### Decision 10: Centralized Execution Logging & Usage Reporting

To aggregate overall execution logs, metrics, and token usage, and print structured summaries, we introduce centralized logging and metrics services bound to `ExecutionContext`:

1. **Structured Log Event (`ExecutionEvent`)**:
   We define a structured, serializable model representing individual trace logs:
   ```java
   package com.xceptance.neodymium.ai.logging;

   import java.util.Map;

   public record ExecutionEvent(
       long timestamp,
       String logLevel, // INFO, DEBUG, WARN, ERROR
       String phase,    // PREPARE, REPLAY, LLM_QUERY, EXECUTE, VERIFY, RECOVERY, HUD
       int stepIndex,
       String message,
       Map<String, String> metadata
   ) {}
   ```
2. **Centralized Logger (`AiExecutionLogger`)**:
   Holds an in-memory sequential list of `ExecutionEvent` objects for the browser/execution session:
   ```java
   package com.xceptance.neodymium.ai.logging;

   import java.util.List;
   import java.util.Queue;
   import java.util.concurrent.ConcurrentLinkedQueue;

   public final class AiExecutionLogger
   {
       private final Queue<ExecutionEvent> events = new ConcurrentLinkedQueue<>();

       public void log(final ExecutionEvent event)
       {
           events.add(event);
       }

       public List<ExecutionEvent> getEvents()
       {
           return List.copyOf(events);
       }
   }
   ```
3. **Usage Metrics Aggregator (`AiUsageTracker`)**:
   Tracks token counters and execution durations:
   ```java
   package com.xceptance.neodymium.ai.logging;

   public final class AiUsageTracker
   {
       private long inputTokens;
       private long outputTokens;
       private long cachedTokens;
       private long startTimestamp;
       private long endTimestamp;

       // Phase-specific execution durations
       private long llmDelayMs;
       private long executionMs;
       private long recoveryMs;
       private long hudWaitMs;

       // Phase invocation counters
       private int llmCallCount;
       private int replayCount;
       private int bypassCount;
       private int recoveryCount;
       private int hudWaitCount;

       // Getters, setters, and incrementers...
   }
   ```
4. **Guidance on Log Generation**:
   - **Step Preparation**: Log index, instruction text, lines, visual screenshot hash baseline match result, Hamming distance.
   - **Direct Bypass**: Log regex matched, bypassed actions.
   - **Replay**: Log playbook step match status, screenshot hashes matching outcome.
   - **LLM Call**: Log targeted capabilities, prompt profile used, dual-context Guarded AI prompt details (native variables masked), raw response, parse outcome, token count.
   - **Action Execution**: Log parameter interpolations, frame switches, successful executions.
   - **Failures**: Log failed action index, diagnostic error messages, attempted locators.
   - **Recovery**: Log active backends restored (Meso), full-flow recovery reset triggered (Macro).
   - **HUD**: Log interactive user commands (edits, rewinds, additions, skips).
5. **Formatted Print Out**:
   At the end of the test run, `AiReportFormatter` compiles the collected logs and metrics from `AiExecutionLogger` and `AiUsageTracker` into a clean, human-readable markdown report printed to the console (or saved to the report output directory as JSON/HTML for external viewer integration).

---

### Decision 11: Decoupled Engines & Shared Connection/Caching Subsystems

To support concurrent execution without state contamination while still benefiting from performance optimizations like connection pooling and LLM prompt token caching, we split the routing engine and registry state from the communication layer:

1. **Session-Isolated Engines and Registry**:
   - Each `ExecutionContext` owns and initializes its own isolated `LlmRegistry` and `LlmService` instances.
   - Mutable configuration overrides or registered mock providers in one test execution thread are strictly scoped to that thread's context and cannot leak to other test runs.
2. **Shared Heavyweight Clients and Connections**:
   - Underlying HTTP network client pools (e.g. OkHttpClient) and client wrappers are instantiated globally and shared across all active `LlmEngine` instances.
   - This ensures concurrent execution reuse sockets and benefits from connection pooling.
3. **Pluggable & Shared Token Cache (`LlmTokenCache`)**:
   - To leverage advanced LLM prompt caching (such as Gemini context caching for large visual/DOM inputs), we introduce a shared `LlmTokenCache` subsystem.
   - The token cache resides globally and is injected into thread-isolated `LlmEngine` instances upon creation:
     ```java
     package com.xceptance.neodymium.ai.cache;

     /**
      * Shared interface for caching LLM prompt states and token IDs.
      * <p>
      * <b>Thread-safety contract</b>: Implementations MUST be safe for concurrent
      * access from multiple test execution threads. Use {@code ConcurrentHashMap}
      * or equivalent lock-free structures internally. The interface itself imposes
      * no eviction policy or TTL — implementations may add size bounds or
      * time-based expiry as needed (e.g. to match Gemini server-side cache TTLs).
      *
      * @author AI-generated: Gemini 2.5 Pro
      * @author Xceptance GmbH 2026
      */
     public interface LlmTokenCache
     {
         String getCachedPromptId(final String promptSignature);
         void cachePrompt(final String promptSignature, final String cachedPromptId);
     }
     ```
   - When a thread-isolated `LlmEngine` makes a visual or large DOM call, it queries the shared `LlmTokenCache` using the visual page DOM/hash signature. If a cache ID is returned, the engine attaches it to the request header/body to leverage the cached context, significantly reducing input token costs.

4. **General Shared Cache Contract**:
   - The `LlmTokenCache` is the first shared cache, but others may follow (e.g. screenshot hash caches, playbook lookup caches, action registry caches).
   - **All globally shared caches** injected into thread-isolated components must satisfy the same contract:
     - Thread-safe reads and writes (no external synchronization required by callers).
     - Implementations must document their concurrency strategy (e.g. `ConcurrentHashMap`, `ReadWriteLock`, copy-on-write).
     - Callers must not hold cache references across session boundaries — retrieve fresh values per step cycle.

---

### Decision 12: Offline Unit Testability & Mock Infrastructures

To ensure the entire state machine loop, registries, and logging systems can be thoroughly verified offline, we design mock infrastructures that stub out external network and driver systems:

1. **`MockActionExecutor` & SUT Resource Mocks**:
   - `MockActionExecutor` (an implementation of `ActionExecutor`) bypasses Selenium/WebDriver completely.
   - It intercepts actions (e.g. click, navigate, type) and stores them in an internal invocation list for verification in unit test assertions.
   - SUT driver resources in the execution context are mocked using lightweight stubs.
2. **`MockLlmEngine`**:
   - An implementation of `LlmEngine` that returns pre-configured mock JSON actions/responses based on the input prompts, simulating model successes, escalations, or parse errors without remote API calls.
3. **`MockHudCommunicator`**:
   - Programmatically simulates user HUD actions.
   - Rather than injecting and executing javascript or pausing thread execution waiting for websocket responses, the mock communicator returns queued `HudAction` structures (e.g. REWIND, EDIT, SKIP) to drive the state machine's wait loops dynamically.
4. **`MockSutRecoveryHandler`**:
   - A stubbed recovery handler that increments invocation counters and records checkpoint targets, verifying SUT state restoration paths in testing without opening real browser sessions.

---

### Decision 13: Thread-Isolated Dependency Injection of Session Configuration

To eliminate the use of global configuration singletons (like `Neodymium.aiConfiguration()`) and thread-local configuration accessors during step execution, configuration resolution and management is fully decoupled:

1. **Pre-Execution Resolution**: At the session entry point (inside `AiBrowser`), the active configuration is resolved by merging the static properties from the global `AiConfiguration` with any system properties and run-specific test data.
2. **Context Injection**: The resolved, immutable configuration object is injected directly into the `ExecutionContext` constructor at session startup.
3. **Context-Driven Parameters**: All execution components—including `StateMachineRunner`, `ActionExecutor`, `LlmRegistry`, recovery coordinators, and action plugins—retrieve configuration settings exclusively via `ExecutionContext.getConfig()`.
4. **Thread-Safe Overrides**: Any runtime configuration adjustments (such as toggling interactive HUD modes or changing API keys mid-run) are stored in a localized override map inside `ExecutionContext`. This ensures that overrides are thread-isolated and do not leak to concurrent test threads.

---

### Decision 14: Pluggable AI Debug Reporting & Artifact Dumping System

To capture detailed execution evidence (screenshots, HTML source, LLM prompts/responses, and runtime events) for later reporting and troubleshooting without hardcoding I/O logic, we introduce a pluggable, event-driven debug reporting subsystem:

1. **`StepArtifacts` Record**:
   An immutable container representing all debug evidence captured for a single step:
   ```java
   package com.xceptance.neodymium.ai.reporting;

   import java.util.List;

   public record StepArtifacts(
       int stepIndex,
       String stepId,
       String instruction,
       byte[] preScreenshot,
       String preHtmlSource,
       byte[] postScreenshot,
       String postHtmlSource,
       String llmSystemPrompt,
       String llmUserPrompt,
       String llmRawResponse,
       List<String> actionsExecuted,
       String status, // SUCCESS, FAILED, etc.
       Throwable error
   ) {}
   ```

2. **`AiDebugReporter` Interface**:
   Pluggable interface for writing debug artifacts to external destinations:
   ```java
   package com.xceptance.neodymium.ai.reporting;

   public interface AiDebugReporter
   {
       /** Invoked at the completion of a step to stream captured artifacts. */
       void reportStep(final StepArtifacts artifacts);

       /** Invoked at the completion of the execution session to dump consolidated summaries. */
       void reportSession(final SessionSummary summary);
   }
   ```

3. **Pluggable Implementations**:
   - **`FileDebugReporter`**: Writes step screenshots (as `.png`), HTML source (as `.html`), and conversation trace dumps (as `.txt` / `.json`) into a configurable local directory (e.g., `target/neodymium-ai-dumps/{timestamp}/`).
   - **`AllureDebugReporter`**: Automatically attaches screenshots, HTML pages, and LLM text prompts directly to the active Allure test run context for rich, integrated test reporting.
   - **`ConsoleMarkdownReporter`**: Formats the session logs, metrics, and token usages into a structured markdown document printed to stdout/stderr.

4. **Integration with State Machine & Context**:
   - The `ExecutionContext` manages a registry of configured `AiDebugReporter` instances.
   - At the completion of each step state (inside `VerifyState` or `EscalateRetryState`), the runner constructs a `StepArtifacts` object using viewport screenshots and DOM snapshots, then pushes it to the registered reporters.
   - This ensures all debug dumps are decoupled from execution logic and are easily customizable.

---

### Decision 15: Generic Session Lifecycle Owner (AiSession), AiBrowser Facade, and Multi-Call Lifecycle

To decouple the execution entry point from browser-specific dependencies, and to support multiple consecutive `ai()` or `execute()` calls within a single cohesive test session, we introduce `AiSession` as the generic orchestrator:

1. **Generic Session Owner (`AiSession`)**:
   - Acts as the primary, `AutoCloseable` entry point. It instantiates the session-scoped `ExecutionContext`, `StateMachineRunner`, registries, and logging/reporting subsystems.
   - It maintains the lifetime of the registered SUT resources (browsers, HTTP clients) across the entire session.
   - It saves the playbook, aggregates usage metrics, and disposes of SUT resources when `close()` is called (typically at the end of a JUnit test method's try-with-resources block).

2. **Multi-Call State Preservation**:
   - In a test case execution, a developer can invoke `execute(instruction)` multiple times on the same session:
     ```java
     try (final AiSession session = new AiSession(test)) {
         session.execute("navigate to shop");
         // ...
         session.execute("search for product");
     }
     ```
   - Each call to `execute()` parses the instruction into new `AiStep` instances and *appends* them to the active `ExecutionContext`'s step list.
   - The `StateMachineRunner` is invoked starting from the current cursor index (the index of the first newly appended step), preserving SUT resources, visual checkpoints, and dual-context variables resolved in previous calls.

3. **`AiBrowser` as a Specialized Facade**:
   - For backward compatibility and specialized browser-based test scenarios, `AiBrowser` is preserved as a thin facade extending `AiSession`.
   - On construction, it automatically initializes and registers the default Selenium/Selenide WebDriver driver into the SUT registry (`ExecutionContext.registerSutResource(WebDriver.class, driver)`).

4. **Mixed-Mode & Non-Browser Testing**:
   - By calling `AiSession` directly, developers can perform API-only testing, database validation, or mixed-mode runs (e.g. executing browser steps and verifying DB/API state in a single session) without creating dummy browsers:
     ```java
     try (final AiSession session = new AiSession(config, test)) {
         session.registerSutResource(HttpClient.class, new CustomHttpClient());
         session.execute("send POST request to create test data"); // routes to API plugin
         
         session.registerSutResource(WebDriver.class, selenideDriver);
         session.execute("login and verify checkout"); // routes to WebDriver plugin
     }
     ```

### Decision 16: Pluggable Domain Interaction Bridges (SUT Bridges)

To support multiple automation domains (Browser, REST API, Database) and offline runners without code duplication or tight coupling, we define the generic concept of a **`DomainInteractionBridge`** registered inside the execution context's SUT registry:

1. **Unifying SUT Bridges**:
   Each automation domain declares an abstract bridge interface to define unified, backend-agnostic operations. Standard action plugins for that domain retrieve their matched bridge from `ExecutionContext.getSutResource(...)`:
   - **`BrowserInteractionBridge`**: Handles UI actions (`navigate`, `click`, `type`, `getHtmlSource`, `takeScreenshot`).
   - **`ApiInteractionBridge`** (Concept for Later): Handles REST/API actions (`sendRequest`, `verifyResponse`, `clearHeaders`).
   - **`DatabaseInteractionBridge`** (Concept for Later): Handles DB actions (`executeQuery`, `verifyRows`, `rollbackTransaction`).

2. **Browser Domain Implementations**:
   - **`SelenideInteractionBridge`**: Implements operations using the Selenide/Selenium API (e.g., `$(selector).click()`, `WebDriverRunner.getWebDriver().getPageSource()`). Fully active and operational.
   - **`PlaywrightInteractionBridge`**: A stub implementation for Microsoft Playwright Java API (to be completed in a later phase). 
   - **`MockInteractionBridge`**: A simulation bridge that records calls and returns canned HTML/screenshots. Fully active and operational to support offline unit testing and offline replays.

3. **Decoupled Action Plugins**:
   Standard browser action plugins (e.g., `ClickActionPlugin`, `TypeActionPlugin`) retrieve the active `BrowserInteractionBridge` from the `ExecutionContext` SUT registry:
   ```java
   public void execute(final Action action, final Object test, final ActionExecutor executor)
   {
       final BrowserInteractionBridge browser = executor.getContext().getSutResource(BrowserInteractionBridge.class);
       if (browser == null)
       {
           throw new ActionExecutionException("No active browser resource registered.");
       }
       browser.click(action.getTarget());
   }
   ```
   This ensures action plugins are entirely backend-agnostic.

4. **Dynamic Loading & Modular Classpaths**:
   To avoid classpath pollution, concrete bridge implementations reside in separate modules and are loaded dynamically (e.g., via SPI ServiceLoader) or registered manually in the `ExecutionContext` at session startup.

---

### Decision 17: Timeout & Cancellation Model

To prevent unbounded execution, runaway LLM calls, and infinite retry loops, we define a hierarchical timeout and cancellation model with three scopes:

#### 1. LLM Call Timeout (Per-Request Time Box)
Every `LlmEngine.chat()` invocation is bounded by a configurable wall-clock timeout. If the LLM does not respond within the limit, the call is aborted and the engine throws a `LlmTimeoutException`.

```java
package com.xceptance.neodymium.ai.core;

/**
 * Thrown when an LLM call exceeds the configured timeout.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class LlmTimeoutException extends RuntimeException
{
    private final long timeoutMs;
    private final String engineName;

    public LlmTimeoutException(final String engineName, final long timeoutMs)
    {
        super("LLM call to '" + engineName + "' timed out after " + timeoutMs + "ms");
        this.engineName = engineName;
        this.timeoutMs = timeoutMs;
    }

    public long getTimeoutMs() { return timeoutMs; }
    public String getEngineName() { return engineName; }
}
```

- **Configuration**: `neodymium.ai.llm.timeoutMs` (default: `120000` — 2 minutes).
- **Responsibility**: Each `LlmEngine` implementation enforces this internally (e.g. via OkHttp call timeout, or `Future.get(timeout, unit)`).
- **State Machine Impact**: `ResolveLiveLlmState` catches `LlmTimeoutException` and treats it as a retryable failure, routing to `EscalateRetryState` with the error budget decremented.

#### 2. Retry Budget Limits (Per-Step Retry Ceiling)
The existing two-tier retry budgets in `EscalateRetryState` (Decision 2 & 6) are hardened with explicit, configurable limits:

| Budget | Config Property | Default | Behavior on Exhaustion |
|--------|----------------|---------|------------------------|
| Error retries | `neodymium.ai.step.maxErrorRetries` | `3` | Transitions to `InteractiveHudWaitState` (if HUD connected) or fails the step definitively |
| No-action retries | `neodymium.ai.step.maxNoActionRetries` | `2` | Same as above |
| Context escalation levels | `neodymium.ai.step.maxEscalationLevels` | `3` | After exhausting all context levels, no further escalation is attempted |

- **`EscalateRetryState` enforces**: Before decrementing a budget, it checks the remaining count. If zero, the step is considered unrecoverable at the retry level.
- **Combined ceiling**: The total number of LLM calls for a single step is bounded by `maxErrorRetries × maxEscalationLevels + maxNoActionRetries`. This prevents runaway token consumption.

#### 3. Step Execution Wall-Clock Limit
Each step execution cycle (from `PrepareStepState` entry to `VerifyState` success) is bounded by a configurable wall-clock timeout. This guards against pathological retry loops, slow SUT recovery, and hung browser interactions.

```java
/**
 * Checked by the StateMachineRunner at each state transition.
 * If the step started more than stepTimeoutMs ago and HUD is not connected,
 * the runner aborts the step and marks it FAILED.
 */
private void checkStepTimeout(final ExecutionContext context)
{
    final long elapsed = System.currentTimeMillis() - context.getCurrentStepStartTime();
    final long limit = context.getConfig().getStepTimeoutMs();
    if (elapsed > limit && !context.isHudConnected())
    {
        throw new StepTimeoutException(context.getStepCursor(), elapsed, limit);
    }
}
```

- **Configuration**: `neodymium.ai.step.timeoutMs` (default: `300000` — 5 minutes).
- **HUD Override**: When the HUD is connected (`context.isHudConnected() == true`), the step timeout is **suspended**. The user is actively observing and interacting, so wall-clock limits should not interrupt manual debugging sessions.
- **State Machine Impact**: `StateMachineRunner.run()` calls `checkStepTimeout()` before entering each new state. If the timeout fires, the runner catches `StepTimeoutException`, records the timeout in `AiExecutionResult`, and either:
  - Transitions to `InteractiveHudWaitState` if the HUD is connected (allowing manual recovery).
  - Marks the step as `FAILED` and advances to the next step (or terminates) if running headless.

#### 4. Configuration Summary

| Property | Scope | Default | Description |
|----------|-------|---------|-------------|
| `neodymium.ai.llm.timeoutMs` | Per LLM call | `120000` (2 min) | Max wall-clock for a single LLM request |
| `neodymium.ai.step.maxErrorRetries` | Per step | `3` | Max error-triggered retries before exhaustion |
| `neodymium.ai.step.maxNoActionRetries` | Per step | `2` | Max empty-action retries before exhaustion |
| `neodymium.ai.step.maxEscalationLevels` | Per step | `3` | Max context escalation levels |
| `neodymium.ai.step.timeoutMs` | Per step cycle | `300000` (5 min) | Max wall-clock for full step execution (suspended when HUD connected) |

All values are resolved through `AiConfiguration` with the standard precedence: Test Data → System Property → `ai.properties` → `neodymium.properties` → Default (see Decision 13).

---

## Risks / Trade-offs

- **[Risk] State Machine Boilerplate** $\rightarrow$ Refactoring the loop into multiple state classes adds files.
  - *Mitigation:* The state classes are highly cohesive and thin, making them easy to maintain and test individually using mock context classes.
- **[Risk] SUT State Recovery Complexity** $\rightarrow$ Re-aligning browser state during retries requires test checkpoint tracking.
  - *Mitigation:* Default recovery will reload the page and fast-replay up to the target step, or utilize session-defined checkpoint cookies/localstorage where available.

---

## Migration Plan

1. **Phase 1:** Introduce `LlmEngine`, `LlmRegistry`, and `ExecutionContext` classes (fully backward compatible).
2. **Phase 2:** Refactor `ActionExecutor` to accept `ExecutionContext` and replace the static `ThreadLocal` variables in `IncludeAction` and `BranchAction`.
3. **Phase 3:** Create `PlaybookReader` and `PlaybookWriter` to handle serialization, reverse placeholder sanitization, and variable enrichment.
4. **Phase 4:** Implement the `StateMachineRunner` and state classes. Replace the monolithic loop in `AiAgent` with `StateMachineRunner.run()`.
5. **Phase 5:** Integrate the new YAML `_dynamic` and `_sensitive` serialization rules into the playbook recorder.
6. **Phase 6:** Implement `AiExecutionLogger` and `AiUsageTracker` and print formatting summaries at test completion.
7. **Phase 7:** Extract the shared HTTP client connections and integrate the `LlmTokenCache` prompt cache manager.
8. **Phase 8:** Develop comprehensive offline unit tests verifying state transitions, SUT recovery routing, and registry-based capability matching.





