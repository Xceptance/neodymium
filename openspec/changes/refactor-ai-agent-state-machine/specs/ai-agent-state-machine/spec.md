# AI Agent State Machine Specification

Purpose: Model the execution loop of the AI Agent as a strongly-typed state machine transitioning through discrete execution and recovery states, isolating the session context from thread-locals, and routing HUD actions via structured events.

## ADDED Requirements

### Requirement: State Machine Transition Loop
The AI Agent's execution loop SHALL be modeled as a strongly-typed state machine transitioning through discrete states. The execution run SHALL begin at a playback resolution state and transition dynamically through execution, verification, escalation/retry, and HUD wait states based on execution outcomes.

#### Scenario: Successful replay of steps
- **WHEN** the state machine is initialized with a playbook containing matching recorded steps
- **THEN** it transitions from playback resolution to replaying actions, executes them, and completes the execution run successfully

#### Scenario: Fallback to LLM on playback mismatch
- **WHEN** the current instruction does not match the recorded step in the playbook or a visual mismatch is detected
- **THEN** the state machine transitions to the live LLM resolution state to obtain new actions from the provider

### Requirement: Isolated Execution Context
All execution-related variables (instruction lists, execution result logging, step cursor, inclusion stack, condition cache) MUST be stored inside a thread-isolated `ExecutionContext` object. No static thread-local variables SHALL be used to pass execution context or action state between different phases or threads.

#### Scenario: Parallel execution isolation
- **WHEN** multiple browser sessions execute AI steps concurrently in different threads
- **THEN** each thread maintains its own isolated `ExecutionContext` and changes to one context do not affect other active threads

### Requirement: Event-Driven HUD Command Routing
Interacting with the HUD during execution SHALL be handled by an event-driven `HudCommunicator` returning a structured `HudAction` object. The state machine SHALL process the HUD action to update the execution context step cursor and steps list, and transition to the correct state without throwing control-flow exceptions.

#### Scenario: User triggers a step rewind
- **WHEN** the user selects to rewind to a previous step on the HUD
- **THEN** the state machine updates the step cursor, truncates subsequent executed steps in the playbook, and transitions to the playback resolution state

#### Scenario: User edits a step
- **WHEN** the user edits an instruction on the HUD
- **THEN** the state machine updates the instruction in the context steps list, clears the actions of that step in the playbook, and transitions to the live LLM resolution state to re-generate actions

### Requirement: System Under Test State Recovery
The execution engine MUST support System Under Test (SUT) state recovery across all active automation backends (web browsers, API clients, databases). When a step execution fails or a rewind is requested, the system SHALL attempt to restore the SUT state to a known stable checkpoint (such as restarting the browser session, resetting REST API connection contexts, rolling back transaction scopes, or reloading cache states) before re-trying or executing.

#### Scenario: Recover SUT state on retry
- **WHEN** an action execution fails and a retry with context escalation is initiated
- **THEN** the state machine invokes the SUT recovery routine to restore all active SUT resources and backends to their last stable checkpoints before transitioning back to the LLM resolution state

### Requirement: Dual-Context Prompt Resolution
The State Machine SHALL support dual-context prompt resolution. When constructing a prompt to send to the LLM during the live resolution state, the prompt instructions and parameters MUST be resolved using the Guarded AI Context Map (with sensitive parameters masked or replaced with realistic stand-in data). When executing the generated actions locally in the target backends (browsers, APIs, or database targets), the action targets and values MUST be resolved using the Native Map (uncensored credentials/parameters).

#### Scenario: Anonymize external LLM prompts but execute raw values locally in SUT backends
- **WHEN** a step uses a sensitive variable key (e.g., `${password}`)
- **THEN** the state machine resolves the prompt with the mock password (e.g., `mockPassword_abc`) for the LLM request, but executes the actual raw password in the targeted SUT backend (web browser or API client) during action execution

### Requirement: Playbook Recording Sanitization
When recording actions into a playbook, the state machine or its playbook recorder delegate SHALL sanitize the action targets and values by replacing resolved runtime dynamic or sensitive data with their original `${variable}` placeholder syntax.

#### Scenario: Replace runtime dynamic values with placeholders in playbook
- **WHEN** the agent records a step execution containing dynamic properties (e.g., a randomly generated email or a port number)
- **THEN** the persisted playbook JSON actions represent the value using its `${placeholder}` variable key instead of the resolved runtime value

### Requirement: Three-Phase Step Routing
The state machine SHALL execute step instructions using a three-phase routing hierarchy:
1. **Playbook Replay Phase**: Attempt to replay recorded actions from the playbook.
2. **Direct Action Bypass Phase**: If no playbook matches, check the instruction against a catalog of direct action regular expressions. If it matches a direct instruction (e.g. `Navigate to`, `Go back`), bypass the LLM and execute the action directly.
3. **Live LLM Query Phase**: If neither matches, query the LLM to resolve and generate actions.

#### Scenario: Bypassing LLM on navigation steps
- **WHEN** a navigation instruction (e.g., "Navigate to https://example.com") is processed without a playbook
- **THEN** the state machine bypasses the LLM call and executes the navigation action directly

### Requirement: Historical Context Learning
The state machine SHALL support historical context level learning. When a step is successfully executed at an escalated context level (e.g., `STANDARD` or `VISUAL`), the resolved context level MUST be written to the playbook step as `healedContextLevel`. On subsequent execution runs, the state machine SHALL start the step execution context directly at the saved `healedContextLevel` instead of starting from the lowest level.

#### Scenario: Skip escalation loop using cached healed context
- **WHEN** a playbook step has a cached `healedContextLevel` of `STANDARD`
- **THEN** the state machine initiates execution of that step starting at `STANDARD` context level, bypassing `AXTREE` and `LEAN`

### Requirement: Dynamic Visual Replay Caching
During the playbook replay phase, the state machine SHALL perform perceptual visual checks for steps containing visual assertions by comparing the dHash of the live browser viewport against the cached step `screenshotHash`. If the Hamming distance is within the configured threshold, the replay SHALL proceed. If not, the replay SHALL fail. If a visual step is recorded as failing, its defective dHash and error message SHALL be cached to allow offline fast-failing on subsequent replays.

#### Scenario: Verify visual replay match
- **WHEN** replaying a step with a cached visual screenshot hash, and the live viewport dHash Hamming distance is less than or equal to the threshold
- **THEN** the replay succeeds offline without LLM visual queries

### Requirement: Two-Tier Retry Budgets
The state machine SHALL enforce two distinct retry budgets during step execution:
1. **Error Retry Budget**: Decremented on WebDriver or action execution exceptions.
2. **No-Action Retry Budget**: Decremented when the LLM returns a success status but proposes an empty actions array.
If either budget is exceeded, the state machine SHALL transition to a failure state.

#### Scenario: Retry budget exhausted on empty actions
- **WHEN** the LLM continuously returns empty actions and the no-action retry budget is exhausted
- **THEN** the state machine fails execution and transitions to the HUD wait or terminal failure state

### Requirement: Programmatic Assertions Reflection
When the LLM returns a `JAVA_METHOD` action, the state machine SHALL resolve and execute the target method via reflection. The target method MUST be annotated with `@AiMethod` to prevent arbitrary code execution, and resolved using a multi-stage lookup (Test Instance -> Dynamically Registered Classes -> Configured Packages/Classes).

#### Scenario: Execute registered custom assertion method
- **WHEN** the LLM emits a `JAVA_METHOD` action targeting a method annotated with `@AiMethod`
- **THEN** the state machine dynamically invokes the method via reflection and passes the parameters successfully

### Requirement: Playbook-Based Expected Failures
The state machine SHALL support expected failures. When an instruction contains a `(bug: ID)` tag, the playbook step SHALL capture it as an expected failure with its error message. During replay, if this step is reached, the state machine SHALL immediately throw an execution failure matching the cached bug signature.

#### Scenario: Throw expected failure on replay
- **WHEN** the state machine encounters a replayed step marked as an expected failure in the playbook
- **THEN** it immediately throws the cached action execution exception without querying the LLM

### Requirement: Decoupled Replay and LLM Engines
The state machine execution SHALL strictly separate the offline playback engine from the live LLM provider engine. When a test executes and matches playbook step cache successfully, the state machine SHALL execute actions without querying or initializing LLM providers, registering services, or validating API credentials. LLM provider initialization and API key validation MUST be lazily deferred and triggered only when execution fails and transitions to the live LLM healing state.

#### Scenario: Successful offline playbook execution
- **WHEN** a test run executes and succeeds purely via playbook replay without any step failures
- **THEN** the state machine completes successfully without initializing LLM registry providers or validating API key properties


#### Scenario: Lazy credentials validation on visual healing
- **WHEN** a playbook replay fails, triggering a transition to the live LLM healing state, but no API key is configured
- **THEN** the state machine throws a configuration exception during the transition to the live state rather than failing on test startup

### Requirement: Dynamic Step Lifetime and History Management
The execution context SHALL manage step lists as dynamic structures supporting steps appearing (inclusions/branches), disappearing (skipped/removed), splitting (parent deactivation), and rewinding.
When rewinding or retrying a step, any steps dynamically added or split during or after the rewind target MUST be cleaned up, restoring the parent step and removing child/dynamically appeared steps.

#### Scenario: Dynamic Step Insertion
- **WHEN** a dynamic block (such as an inclusion action) executes nested instructions
- **THEN** these nested steps are inserted dynamically at the current cursor index as `PENDING` steps and the execution proceeds through them

#### Scenario: Step Splitting and Parent Deactivation
- **WHEN** an instruction is split during execution into a resolved prefix action and a remaining instruction
- **THEN** the current step is marked as `SPLIT_PARENT` (non-executable), and new child steps representing the executed part and the remainder are inserted as `PENDING` steps linked to the parent

#### Scenario: Rewind Cleanup of Splits and Dynamic Steps
- **WHEN** the state machine retries or rewinds to a step that is a parent of a split or a step before dynamic insertion occurred
- **THEN** all associated child steps and dynamically appeared steps are removed from the steps list, and the parent step is restored to its original unsplit instruction and marked as `PENDING`

### Requirement: HUD/IDE Aura Interactive Mutations
The state machine execution engine SHALL dynamically process HUD mutations (such as step modification, insertion, deletion, and rewinding) by adjusting the execution context's steps structure and state.

#### Scenario: HUD updates step instruction
- **WHEN** the user edits a step on the HUD
- **THEN** the execution context updates the instruction text of the step, resets its status to `PENDING`, clears its actions, and routes the runner to re-prepare the step

#### Scenario: HUD inserts a step
- **WHEN** the user adds a new step on the HUD
- **THEN** a new step is dynamically created with status `PENDING` and inserted into the steps list at the requested index

#### Scenario: HUD removes a step
- **WHEN** the user deletes a step on the HUD
- **THEN** the step is removed or marked as `SKIPPED` in the execution context, so that it is skipped by the cursor

### Requirement: Playbook Serialization and Variable Enrichment
The playbook subsystem SHALL extract, sanitize, and enrich step execution data during loading (reading) and saving (writing).

#### Scenario: Playbook Reader restores structured actions
- **WHEN** a playbook file is loaded from disk
- **THEN** the reader parses the recorded JSON, instantiates actions, maps visual baseline hashes, and initializes step states

#### Scenario: Playbook Writer sanitizes and enriches playbook steps
- **WHEN** a playbook is written to disk
- **THEN** the writer performs reverse variable lookup to replace native runtime credentials with their original `${placeholder}` keys, extracts screenshot hashes, records healed context levels, and persists the JSON

### Requirement: Hierarchical Recovery System
The execution engine SHALL support a pluggable, hierarchical recovery model operating at two levels: Action-Specific Recovery (Micro) and SUT/Backend Checkpoint Recovery (Meso).

#### Scenario: Action Retryability and Recoverability Declarations
- **WHEN** an action fails execution
- **THEN** the engine evaluates the action's design properties:
  - **IF** it is marked as both retryable and recoverable (default): the engine performs SUT recovery and retries the execution.
  - **IF** it is marked as retryable but NOT recoverable: the engine can retry the action immediately (micro-retry) but is forbidden from running SUT recovery.
  - **IF** it is marked as NOT retryable (regardless of recoverability): the engine immediately skips retries and transitions to the HUD wait or terminal failure state.

#### Scenario: Route SUT recovery to registered backend handlers
- **WHEN** a step retry or rewind occurs, and multiple SUT backends (browser, API client) are active
- **THEN** the state machine delegates recovery to the registered handlers matching the active backend types to restore stable checkpoints

### Requirement: Step Timeout and Retry Limits
The state machine SHALL enforce execution timeouts and retry limits to prevent infinite execution loops or resource leakage.
1. **LLM Time-boxing**: Every LLM engine chat call SHALL be configured with a strict connection/response timeout limit.
2. **Step Execution Time Limit**: Every step execution SHALL have a wall-clock timeout limit (checked before entering each state transition). If exceeded, it throws a `StepTimeoutException` and transitions to `InteractiveHudWaitState` (if HUD is connected) or fails the step definitively. If the HUD is connected, the wall-clock step execution timer SHALL be paused.
3. **Retry Limits**: The number of execution attempts per step is strictly bounded by the two-tier retry budgets.

#### Scenario: Step execution timeout with connected HUD
- **WHEN** a step execution takes longer than the configured step timeout and a HUD is connected
- **THEN** the state machine catches the timeout exception and transitions to the HUD wait state to allow manual intervention

#### Scenario: Step execution timeout without connected HUD
- **WHEN** a step execution takes longer than the configured step timeout and no HUD is connected
- **THEN** the state machine catches the timeout exception and fails the step and test execution definitively

### Requirement: Diagnostic Action Failure Communication
Action execution failures MUST carry structured diagnostic details to propagate context to the state machine, HUD, and LLM for self-correction.

#### Scenario: Propagate failure details to HUD and LLM
- **WHEN** an action fails execution
- **THEN** it throws an exception carrying the failed action definition, parameters, locator attempts, and system state (HTML/screenshots), allowing the HUD to display exact diagnostics and the LLM to self-correct during subsequent healing

### Requirement: Centralized Execution Logging
The execution context SHALL maintain a thread-isolated, centralized logger collecting structured log events across all execution phases.

#### Scenario: Log structured events sequentially
- **WHEN** the state machine progresses through step preparation, bypasses, LLM queries, action execution, SUT recovery, or HUD waiting states
- **THEN** the runner and services emit structured logs capturing the step index, timestamps, exact prompts, execution locators, visual comparison details, and intermediate outcomes to the centralized logger

### Requirement: Usage Metrics Collection and Reporting
The AI subsystem MUST aggregate LLM token usage, execution durations, and step completion statistics, providing formatting utilities to generate detailed execution reports.

#### Scenario: Aggregating and printing execution summaries
- **WHEN** a test execution session finishes
- **THEN** the collected logs and metrics are aggregated to print a markdown execution report containing token consumption, estimated costs, performance statistics, and success rates per phase

### Requirement: Offline Unit Testability & Mock Infrastructures
The entire state machine loop, execution context, routing registry, and parser subsystems MUST support comprehensive offline testing via unit tests. The core interfaces (LLM engine, SUT recovery, HUD communicator, and Action executor) SHALL be mockable to verify logic execution without external dependencies.

#### Scenario: Verify execution flow offline
- **WHEN** the state machine runner is initialized with a mock SUT executor, mock HUD communicator, and mock LLM provider
- **THEN** the runner completes the execution flow synchronously, allowing unit tests to assert the exact state transition path, recorded playbook changes, logging traces, and recovery invocations without triggering Selenium WebDriver commands or remote HTTP requests







