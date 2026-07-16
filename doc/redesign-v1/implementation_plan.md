# Neodymium AI Architecture Rewrite Plan

Based on the `refactor-ai-agent-state-machine` OpenSpec and your instruction to **not maintain backward compatibility**, this plan outlines the complete rewrite of the AI subsystem.

## User Review Required

> [!WARNING]
> **Complete API Breakage & Removal of `AiBrowser`**
> As requested, we are **dropping backward compatibility**. The legacy `AiBrowser` facade and older monolithic classes will be completely deleted. All tests and clients must migrate to directly using `AiSession` as the generic orchestrator.

> [!IMPORTANT]
> **Extensive Refactoring Ahead**
> This rewrite completely replaces the 3,300-line `AiAgent` loop with a State Machine, introduces `ExecutionContext` to eliminate all `ThreadLocal` usage, and decouples prompt management via `PromptRegistry` and `LlmRegistry`.

## Open Questions

> [!TIP]
> **LLM Integrations for Phase 1**
> The new `LlmEngine` interface supports capability-based routing (e.g., Vision vs. Text). Which LLM clients (e.g., Gemini 2.5 Flash, local Llama) should be implemented during the initial rollout to prove the new `LlmRegistry`?

## Proposed Architecture & Design

### 1. Parallel Implementation Strategy (`ai2` Package)
- **Zero Disruption**: The entire rewrite will take place inside a new `com.xceptance.neodymium.ai2` package (or similar, like `ai.v2`). 
- **Side-by-Side Comparability**: The existing `com.xceptance.neodymium.ai` package remains completely untouched. This allows test suites to opt-in to the new engine incrementally, and enables direct A/B performance and reliability comparisons between the legacy monolithic loop and the new state machine.
- **Hard Break from Legacy**: Because this is a greenfield package, we drop backward compatibility completely within `ai2`. `AiBrowser` is not ported; `AiSession` acts as the sole entry point. Legacy exceptions like `HudActionException` are abandoned.

### 2. Context Isolation & SUT Management
- **`ExecutionContext`**: Replaces all static `ThreadLocal` variables. It holds the active `AiStep` list, playback state, and execution history.
- **Pluggable SUT Registry**: `ExecutionContext` holds a registry of backend resources (e.g., WebDriver, HttpClient), making the orchestrator entirely backend-agnostic via `InteractionBridge` interfaces.

### 3. State Machine Runner
- **Decomposed Execution Loop**: The monolithic loop is replaced by `StateMachineRunner`.
- **Discrete States**:
  - `PrepareStepState`
  - `ResolvePlaybackState`
  - `ResolveLiveLlmState`
  - `ReplayActionsState`
  - `ExecuteActionsState`
  - `VerifyState`
  - `EscalateRetryState`
  - `InteractiveHudWaitState`

### 4. PESAP & Semantic Analysis Integration
- **Decoupled from Execution**: PESAP is no longer a hardcoded, upfront blocking call. It is integrated directly into the `ResolveLiveLlmState`.
- **Capability-Based Cost Savings**: PESAP operations (Step Splitting, Semantic Linting) are mapped to `LlmCapability.STEP_SPLITTING`. This allows the `LlmRegistry` to route PESAP checks to cheap, fast, text-only local models while reserving expensive multimodal models exclusively for execution/vision.
- **JIT Caching & Bypass**: `ResolveLiveLlmState` will read `healedContextLevel` from the playbook (set by previous PESAP predictions) to instantly bypass PESAP on subsequent runs, drastically reducing API latency.

### 5. Pluggable Capability Routing & Prompt Management
- **`LlmEngine` & `LlmRegistry`**: Routes tasks based on capabilities (`VISION`, `STRUCTURED_JSON`, `STEP_SPLITTING`).
- **`PromptProfile` & Fixers**: Decouples prompts from the execution logic and uses pluggable Response Fixers to auto-correct bad LLM outputs.

### 6. Reliable Playbook Persistence & SUT Recovery
- **Safe Recovery**: Introduces `SutRecoveryHandler` to cleanly restore SUT state to a known checkpoint during retry escalation.
- **Serialization Decoupling**: Introduces `PlaybookReader` and `PlaybookWriter` to safely handle dynamic/sensitive variable substitution and atomic file saving.

## Phased Implementation Plan (Granular & Test-Driven)

Per your request, the implementation will be broken down into highly granular, individually verifiable steps. Each component will be developed in isolation with extensive unit tests (using the proposed mock infrastructures like `MockLlmEngine` and `MockActionExecutor`) before being integrated into the larger state machine.

### Phase 1: Core Models & Context (Isolated)
1. **Define `AiStep` and `AiStepStatus`**: Create the basic step models and write unit tests for status transitions and tree structures (parent/child splitting).
2. **Implement `ExecutionContext`**: Build the session state holder. Write unit tests to verify dynamic step insertion, splitting logic, and rewind operations without any SUT or LLM dependencies.
3. **Implement `AiSession`**: Create the top-level orchestrator class (replacing `AiBrowser`). Unit test its lifecycle management and resource cleanup.

### Phase 2: Pluggable Registries & LLM Bridges (Isolated)
1. **Define `LlmCapability`, `LlmEngine`, and `LlmRegistry`**: Implement the registry logic. Write unit tests ensuring capability matching works and fallbacks are correctly selected.
2. **Implement `MockLlmEngine`**: Create the offline stub for testing.
3. **Build `PromptProfile` & Fixers**: Implement the response fixers (`StringResponseFixer`, `JsonResponseFixer`) and write unit tests feeding them malformed JSON to ensure they auto-correct properly.

### Phase 3: SUT Bridging & Action Execution (Isolated)
1. **Define `DomainInteractionBridge` & `BrowserInteractionBridge`**: Establish the backend-agnostic interfaces.
2. **Refactor `ActionExecutor`**: Update it to pull bridges from `ExecutionContext` instead of global state. 
3. **Implement `MockActionExecutor` & `MockSutRecoveryHandler`**: Build these to support offline testing of the state machine. Write unit tests verifying action execution routing.

### Phase 4: Playbook I/O & Sanitization (Isolated)
1. **Implement `PlaybookReader` & `PlaybookWriter`**: Build the new decoupled I/O classes.
2. **Unit Test Sanitization**: Write extensive unit tests ensuring `_dynamic` and `_sensitive` variables are perfectly redacted before saving, and properly re-hydrated during reading.

### Phase 5: The State Machine (Integration)
1. **Define `State` & `StateMachineRunner`**: Build the core loop engine.
2. **Implement States Individually**: Build `PrepareStepState`, `ResolveLiveLlmState`, `ExecuteActionsState`, etc., one by one. Unit test each state in complete isolation using `MockExecutionContext` and `MockLlmEngine`.
3. **Assemble and Test**: Wire the states together into the runner and execute full end-to-end offline tests using the mock infrastructures.

### Phase 6: Observability
1. **Implement `AiExecutionLogger` & `AiUsageTracker`**: Add telemetry and usage tracking. Unit test the aggregation logic.
