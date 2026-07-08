# Neodymium AI Agent: Architectural Options & Deep Analysis

This document provides a thorough analysis of the architectural patterns considered for refactoring the `com.xceptance.neodymium.ai` subsystem. It examines the concurrency model, the pluggable LLM capability routing, and the execution loop design, comparing three major architectural options.

---

## 1. Context Isolation (Eliminating ThreadLocals)

Before looking at the execution loop, we must establish a solid isolation foundation. The current codebase uses `ThreadLocal` variables for the active agent, active execution results, inclusion stacks, and condition results. This design fails in parallel test environments because thread pools recycle threads, leading to memory leaks and state pollution.

### Proposed Isolation Model: The `ExecutionContext` Session
To isolate each run, we encapsulate all state within an `ExecutionContext` (or `AiSession`) object. This context is created by `AiBrowser` when starting a test run and passed down explicitly to the orchestrator, state machine, and action plugins.

```
┌────────────────────────────────────────────────────────┐
│                   ExecutionContext                     │
├────────────────────────────────────────────────────────┤
│ - activeBrowser: AiBrowser                             │
│ - activePlaybook: Playbook                             │
│ - activeResult: AiExecutionResult                      │
│ - activeConfig: AiConfiguration                        │
│ - stepsList: List<String>                              │
│ - stepCursor: int                                      │
│ - includeStack: List<String>                           │
│ - lastConditionResult: Boolean                         │
└────────────────────────────────────────────────────────┘
```
All static `ThreadLocal` maps and fields are completely eliminated.

---

## 2. Pluggable LLM Capability & Fallback Architecture

We want to allow different LLMs to be configured per browser/client session and route tasks dynamically based on their announced capabilities, with a reliable fallback chain.

### Capability Registry & Fallback Chain
1. **Capabilities Declaration**: Each provider announces what it can do using a strongly-typed capability set:
   ```java
   public enum LlmCapability
   {
       TEXT_ONLY,       // Fast text generation (e.g., local Llama)
       VISION,          // Can process screenshots/images (e.g., Gemini Vision)
       STRUCTURED_JSON, // Can guarantee schema-conforming JSON output
       STEP_SPLITTING   // Can split compound instructions (PESAP)
   }
   ```
2. **Pluggable Fallback Routing (General Handler)**: The orchestrator queries a session-scoped `LlmRegistry`. If no specialized provider matches the required capability, the registry falls back to a designated **default provider (General Handler)**.

```
Request (e.g., STEP_SPLITTING)
      │
      ▼
┌──────────────┐      Found?
│ LlmRegistry  ├──────────────────► Yes ──► Execute on specialized provider (e.g., Ollama)
└──────┬───────┘
       │ No
       ▼
┌──────────────┐
│ Default      │ (General Handler - e.g., Gemini Cloud)
│ Provider     ├──────────────────────────► Execute
└──────────────┘
```

#### Registry Implementation:
```java
package com.xceptance.neodymium.ai.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry matching capabilities to providers with default fallback routing.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmRegistry
{
    private final List<LlmProvider> providers = new ArrayList<>();
    private LlmProvider defaultProvider;

    public void registerProvider(final LlmProvider provider)
    {
        providers.add(provider);
    }

    public void setDefaultProvider(final LlmProvider provider)
    {
        this.defaultProvider = provider;
        registerProvider(provider);
    }

    /**
     * Selects the provider matching the capability, falling back to the default provider if none matches.
     *
     * @param requiredCapability the requested capability
     * @return the selected provider
     */
    public LlmProvider getProviderFor(final LlmCapability requiredCapability)
    {
        return providers.stream()
                .filter(p -> p.getCapabilities().contains(requiredCapability))
                .findFirst()
                .orElseGet(() -> {
                    if (defaultProvider != null)
                    {
                        return defaultProvider;
                    }
                    throw new IllegalStateException("No provider found for capability " 
                            + requiredCapability + " and no default fallback provider is set.");
                });
    }
}
```

---

## 3. The Execution Loop: Comparing Architectural Patterns

A critical challenge in web automation is the **Browser State Mutation Problem**.
When an instruction step executes, it mutates the browser state:
$$\text{Action}(State_t) \rightarrow State_{t+1}$$
If step 3 fails, the browser is at $State_3$. Simply "jumping back" or "rewinding" the instruction pointer to step 2 does not restore the browser to $State_1$. Executing the actions of step 2 on browser state $State_3$ will fail or cause side effects (e.g., resubmitting a form).

We analyze three patterns to handle this execution, rewind, and recovery flow.

---

### Option A: The Procedural Orchestrator (Current Model)

The current implementation uses a monolithic procedural loop in `AiAgent` that mutates the loop counter variable `i` directly inside catch blocks to handle HUD interruptions.

```
+-----------------------------------------------------------+
|                   Procedural Loop                         |
|                                                           |
|  for (int i = 0; i < steps.size(); i++) {                 |
|     try {                                                 |
|         executeStep(i);                                   |
|     } catch (HudActionException e) {                      |
|         i = processHudActionException(e, i);              |
|     }                                                     |
|  }                                                        |
+-----------------------------------------------------------+
```

#### Tradeoffs:
*   **Pros**:
    *   Direct and simple to trace linearly.
    *   No complex architectural abstractions.
*   **Cons**:
    *   **Monolithic**: Replay routing, visual hash checking, step-splitting, LLM calls, and retries are all smashed into one 3,300+ line class.
    *   **Untestable**: Unit testing individual phases (like replay verification) is impossible without initializing full Selenium/Selenide drivers.
    *   **Brittle Loop Mutation**: Directly modifying the loop counter `i` from catch blocks makes it extremely hard to introduce custom validators, step skip policies, or automatic checkpointing.

---

### Option B: The Filter Pipeline Pattern (Interceptors)

This pattern structures execution as a sequence of processors or filters acting on a shared context bag.

```
StepContext ────► [TagExtractor] ────► [PlaybackRouter] ────► [ActionResolver] ────► [ActionExecutor]
```

#### Tradeoffs:
*   **Pros**:
    *   High decoupling of individual phases.
    *   Easy to inject new logging, diagnostics, or validation steps as pipeline filters.
*   **Cons**:
    *   **Poorly Typed State**: Pipelines usually rely on a loosely-typed context map to pass variables between filters, making refactoring and compilation safety difficult.
    *   **Complex Rewinds**: A pipeline is designed as a forward-flowing chain. Implementing a "rewind" or "jump back" requires either throwing control-flow exceptions out of the pipeline or having the pipeline controller manually reset and re-run the entire pipeline from scratch, which is highly complex to coordinate.
    *   **State Mutation Blindness**: Filters execute sequentially without awareness of whether the previous filter mutated the browser state, leading to unsafe retries.

---

### Option C: The State Machine Pattern (Recommended)

Instead of a loop or a pipeline, we model the execution as an explicit state machine. Each execution step is processed by transitioning through strongly-typed states.

```
       ┌────────────────────────┐
       │         START          │
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │   ResolvePlaybackState ├───────────Playback matches?
       └───────────┬────────────┘           │ Yes
                   │ No                     ▼
                   ▼            ┌────────────────────────┐
       ┌────────────────────────┐│   ReplayActionsState   │
       │  ResolveLiveLlmState   │└──────────┬─────────────┘
       └───────────┬────────────┘           │
                   ├────────────────────────┘
                   ▼
       ┌────────────────────────┐
       │  ExecuteActionsState   │
       └───────────┬────────────┘
                   │
         Action fails? ────► [EscalateRetryState] ──► (Increment Context, Refresh SUT)
                   │ No                                   │
                   ▼                                      ▼
       ┌────────────────────────┐               [RecoverState] ──► (Navigate to checkpoint)
       │      VerifyState       │
       └───────────┬────────────┘
                   │
         Assertion fails? ──► [InteractiveHudWaitState] ──► (User chooses rewind/edit)
                   │
                   ▼
       ┌────────────────────────┐
       │       NextStep         │
       └────────────────┘
```

#### Key State Classes:
*   `ResolvePlaybackState`: Checks the playbook. If matching prompt and visual dHash match, transitions to `ReplayActionsState`.
*   `ResolveLiveLlmState`: Selects the appropriate `LlmProvider` based on capability, captures page DOM/screenshot, and generates actions.
*   `ExecuteActionsState`: Feeds actions to `ActionExecutor`.
*   `EscalateRetryState`: Consults `EscalationCoordinator`. If retry is allowed, escalates context (e.g., `LEAN` -> `FULL_DOM`), resets the browser back to the last stable checkpoint if needed, and transitions back to `ResolveLiveLlmState`.
*   `InteractiveHudWaitState`: Pauses run, communicates with the HUD, and waits for manual step edit, add, or rewind instructions.

#### Tradeoffs:
*   **Pros**:
    *   **Explicit State Transitions**: Eliminates the fragile loop mutation of Option A and the exception-control-flow of Option B.
    *   **Safe State Recovery**: Provides an explicit state (`EscalateRetryState` / `RecoverState`) that can handle browser state restoration (e.g., replaying from the last checkpoint or refreshing SUT) before attempting to re-resolve actions.
    *   **Highly Testable**: Each state class can be unit-tested in isolation by mocking the context and asserting the returned next-state transition.
*   **Cons**:
    *   Requires writing more classes (one per state), resulting in a slight increase in initial boilerplate.

---

## 4. HUD-Driven State Mutations & Cursor Control

A central requirement of the Neodymium AI framework is the **Interactive HUD**, which allows users to dynamically edit, insert, skip, or rewind steps during test execution. 

### Eliminating Exceptions for Flow Control
In the current monolithic loop, HUD events are implemented by throwing a `HudActionException`, which is caught by the orchestrator to dynamically recalculate the loop index `i`. 

In the State Machine model, HUD interactions are treated as **Event Inputs** that trigger explicit state transitions and context changes:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        InteractiveHudWaitState                         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                       User clicks HUD button...
                                    │
                                    ▼
                      hudCommunicator.waitForAction()
                                    │
             ┌──────────────────────┼──────────────────────┐
             ▼ (REWIND)             ▼ (EDIT / ADD)         ▼ (SAVE_EXIT)
       Update stepCursor      Modify stepsList       Save playbook
       Clear executed list    Reset step actions     Write YAML file
             │                      │                      │
             ▼                      ▼                      ▼
    ResolvePlaybackState   ResolveLiveLlmState    TerminateExecutionState
```

#### Modeling the HUD Action:
Instead of throwing an exception, the communicator blocks and returns a structured `HudAction` result:

```java
package com.xceptance.neodymium.ai.core;

import java.util.Map;

/**
 * Representation of a user action triggered via the interactive HUD.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public record HudAction(
        HudActionType type,
        int targetIndex,
        String instruction,
        Map<String, String> dataBindings)
{
}
```

#### Processing HUD Actions within `InteractiveHudWaitState`:
When `InteractiveHudWaitState` receives a `HudAction`, it mutates the `ExecutionContext` session state in a controlled manner and selects the next state transition:

```java
package com.xceptance.neodymium.ai.state;

import com.xceptance.neodymium.ai.core.ExecutionContext;
import com.xceptance.neodymium.ai.core.HudAction;
import com.xceptance.neodymium.ai.core.HudCommunicator;

/**
 * Blocks execution and waits for interactive user commands via the HUD.
 * Transitions state and updates the execution context cursor based on user input.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class InteractiveHudWaitState implements State
{
    private final HudCommunicator hudCommunicator;

    public InteractiveHudWaitState(final HudCommunicator hudCommunicator)
    {
        this.hudCommunicator = hudCommunicator;
    }

    @Override
    public StateTransition execute(final ExecutionContext context)
    {
        final HudAction action = hudCommunicator.waitForAction(context);

        return switch (action.type())
        {
            case REWIND -> {
                context.setStepCursor(action.targetIndex());
                // Truncate results and executed lists in context back to target index
                context.truncateStepHistory(action.targetIndex());
                // Jump back to playback routing for the rewound step
                yield new StateTransition(new ResolvePlaybackState());
            }
            case EDIT -> {
                context.getStepsList().set(action.targetIndex(), action.instruction());
                context.setStepCursor(action.targetIndex());
                // Reset recorded step actions in playbook to trigger re-recording
                context.getPlaybook().getSteps().get(action.targetIndex()).clearActions();
                context.getPlaybook().setRecording(true);
                yield new StateTransition(new ResolveLiveLlmState());
            }
            case ADD -> {
                context.getStepsList().add(action.targetIndex(), action.instruction());
                context.setStepCursor(action.targetIndex());
                yield new StateTransition(new ResolveLiveLlmState());
            }
            case SAVE_EXIT -> {
                context.getPlaybook().save();
                yield new StateTransition(new TerminateExecutionState());
            }
            case SKIP -> {
                context.incrementStepCursor();
                yield new StateTransition(new ResolvePlaybackState());
            }
        };
    }
}
```

---

## 5. Escalation and HUD Communication Flow

To ensure the architecture is thoroughly decoupled, we separate the logic of *deciding* when to escalate from the *mechanism* of displaying progress or asking for help.

### Decoupled HUD Communication (The Event Model)
Instead of the orchestrator directly injecting scripts into the browser or writing files, we use an event-driven `HudCommunicator` model.

```java
public interface HudCommunicator
{
    void publish(final HudEvent event);
    
    /** Blocks execution until user performs an action on the HUD. */
    HudAction waitForAction(final ExecutionContext context);
}
```

#### Event Hierarchy:
*   `ExecutionProgressEvent`: Sent when starting a step or action execution.
*   `EscalationEvent`: Sent when a step fails, detailing the error and the next context escalation level.
*   `UserConfirmationRequiredEvent`: Sent when a definitive failure is reached, forcing the HUD to display edit/rewind buttons.

This decouples the agent completely. We can plug in a `SeleniumHtmlHudCommunicator` for browser runs, or a `ConsoleHudCommunicator` / `MockHudCommunicator` for server-side testing.

---

## 6. Priority and Implementation Plan

If Option C (State Machine) is approved, we propose the following phased implementation plan:

1.  **Phase 1: Interface Definition & Isolation (Low Risk)**
    *   Define `LlmProvider`, `LlmCapability`, and the `ExecutionContext` session context.
    *   Implement and unit-test the capability-based `LlmRegistry`.
2.  **Phase 2: Action Executor Refactoring (Medium Risk)**
    *   Remove `ThreadLocal` from `ActionExecutor`, passing the `ExecutionContext` explicitly.
    *   Update action plugins (`IncludeAction`, `BranchAction`) to read run-state from the executor.
3.  **Phase 3: State Machine Execution Engine (High Risk)**
    *   Implement the state machine transition runner.
    *   Port current procedural loops into the explicit `State` classes (`ResolvePlaybackState`, `ResolveLiveLlmState`, `ExecuteActionsState`).
    *   Inject the event-driven `HudCommunicator` interface.
