## 1. Pluggable Capability-Based Routing & Bidirectional Prompting

- [ ] 1.1 Create `LlmCapability` enum declaring capabilities (`TEXT_ONLY`, `VISION`, `STRUCTURED_JSON`, `STEP_SPLITTING`)
- [ ] 1.2 Create `LlmEngine` interface where `chat` receives `ExecutionContext` and queries it bidirectionally for prompts
- [ ] 1.3 Create session-scoped `LlmRegistry` supporting engine registration, capability-based matching, and default engine fallback
- [ ] 1.4 Create `PromptRegistry` supporting model-family-specific template lookups (e.g. customized prompts for Mistral vs. Gemini) with standard fallback


## 2. Context Isolation

- [ ] 2.1 Create `ExecutionContext` class holding steps list, playbook, results, cursor, and inclusion stack
- [ ] 2.2 Refactor `ActionExecutor` to remove global static `ThreadLocal` references and accept the run-scoped `ExecutionContext`
- [ ] 2.3 Refactor `IncludeAction` and `BranchAction` to read inclusion stack and condition results from the `ExecutionContext` via the executor

## 3. State Machine Runner and Interfaces

- [ ] 3.1 Define the `State` interface and the `StateTransition` record
- [ ] 3.2 Implement `StateMachineRunner` driving state execution loops
- [ ] 3.3 Create the `SutRecoveryHandler` interface and default browser checkpoint restoration routine
- [ ] 3.4 Create the `HudCommunicator` interface and `HudAction` record classes, decoupling direct HUD script injection from loop logic

## 4. Concrete State Implementations

- [ ] 4.1 Implement `ResolvePlaybackState` checking playbooks, verifying dHash matches, and routing to replay or live LLM path
- [ ] 4.2 Implement `ReplayActionsState` to execute pre-recorded actions
- [ ] 4.3 Implement `ResolveLiveLlmState` compiling DOM context, retrieving capability LLMs, and parsing action responses
- [ ] 4.4 Implement `ExecuteActionsState` delegating action executions to the thread-isolated `ActionExecutor`
- [ ] 4.5 Implement `VerifyState` running test assertions and handling outcome transitions
- [ ] 4.6 Implement `EscalateRetryState` to increment context levels, restore browser state via `SutRecoveryHandler`, and coordinate retries
- [ ] 4.7 Implement `InteractiveHudWaitState` blocking on `HudCommunicator` and processing user actions (Rewinds, Edits, Skips)

## 5. Dual-Context Resolution & Sanitization

- [ ] 5.1 Integrate dual-context resolution: resolve prompts using the Guarded AI Context Map in `ResolveLiveLlmState` and real values using the Native Map in `ExecuteActionsState`
- [ ] 5.2 Integrate dynamic (`_dynamic`) and sensitive (`_sensitive`) key serialization in the playbook recorder during generation
- [ ] 5.3 Implement `PlaybookReader` and `PlaybookWriter` for decoupled serialization, reverse placeholder sanitization, and variable enrichment

## 6. Monolithic loop refactoring & Verification

- [ ] 6.1 Replace the monolithic loop in `AiAgent` with `StateMachineRunner` execution
- [ ] 6.2 Write unit and integration tests covering the new state machine, registry routing, and isolated parallel execution

