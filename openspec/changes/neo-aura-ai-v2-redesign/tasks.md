## 1. Parsers, Models & Resources

- [ ] 1.1 Create `PlaybookStep` class definition with fields for instruction (string), actions (list), and status (enum: PENDING, RUNNING, SUCCESS, FAILED, SPLITTED).
- [ ] 1.2 Add child steps tree collection (`List<PlaybookStep> subSteps`) and helper `isComposite()` in `PlaybookStep`.
- [ ] 1.3 Create `Playbook` wrapper class that acts as the root of the playbook steps.
- [ ] 1.4 Define `PlaybookResourceManager` interface with signatures `InputStream read(String)`, `void write(String, String)`, and `String resolveInclude(String, String)`.
- [ ] 1.5 Write TDD tests for include path resolution using mock paths.
- [ ] 1.6 Implement `LocalFileResourceManager` using standard Java NIO paths.
- [ ] 1.7 Implement `InMemoryResourceManager` utilizing thread-safe string maps to store playbook recordings without disk writes.
- [ ] 1.8 Define `PlaybookParser` interface signature.
- [ ] 1.9 Write TDD tests parsing simple YAML files and nested includes using a mock parser.
- [ ] 1.10 Implement `YamlPlaybookParser` parsing playbooks recursively using snakeyaml.
- [ ] 1.11 Implement `InlinePlaybookParser` parsing raw multi-line strings into playbook steps.
- [ ] 1.12 Create JUnit 5 verification suite for full playbook parsing and resource resolution.

## 2. LLM Client Interface, Registry & Prompts

- [ ] 2.1 Create `LlmCapability` enum (`TEXT_ONLY`, `VISION`, `STRUCTURED_JSON`, `STEP_SPLITTING`).
- [ ] 2.2 Create request/response record structures `LlmRequest` (system prompt, user query, attachments, schema) and `LlmResponse` (text content, token usage, model name).
- [ ] 2.3 Define `LlmProvider` interface with `LlmResponse chat(LlmRequest)` and `Set<LlmCapability> getCapabilities()`.
- [ ] 2.4 Implement `MockLlmProvider` holding a thread-safe queue of canned responses to bypass LLM API calls in tests.
- [ ] 2.5 Implement `LlmRegistry` supporting dynamic capability resolution and fallback to a default provider.
- [ ] 2.6 Implement hierarchical configuration properties loading (`AiConfiguration`) resolving global defaults and role-specific (`pesap`, `execution`, `vision`, `audit`) overrides.
- [ ] 2.7 Implement provider bootstrapping that instantiates and registers separate role-specific `LlmProvider`s based on configuration overrides.
- [ ] 2.8 Define `AiPrompt<T>` interface specifying the generic prompts compilation and multi-stage response parsing/repairing.
- [ ] 2.9 Write TDD tests asserting that prompts format system/user messages correctly and repair malformed model JSON responses.
- [ ] 2.10 Implement a concrete prompt class (e.g. `ActionsPrompt` or `StringPrompt`) and verify its output parsing.

## 3. Session Data & Sanitization

- [ ] 3.1 Create `SessionData` class supporting static (immutable) variables and dynamic (mutable concurrent) variables.
- [ ] 3.2 Implement snapshot history mapping in `SessionData` to capture dynamic variable states.
- [ ] 3.3 Write TDD tests verifying variable values lookup precedence (Dynamic > Static) and state rollback.
- [ ] 3.4 Implement `ContextSanitizer` scanning payloads for credentials, replacing them with format-preserving mock patterns or user stand-in values.
- [ ] 3.5 Implement `ActionSanitizer` performing on-the-fly variable parameterization of executed actions (raw input replaced with variable references).
- [ ] 3.6 Write TDD tests verifying pre-LLM secret masking and reverse-mapping of stand-ins.
- [ ] 3.7 Write TDD tests asserting that actions executed with raw secret values are recorded with variable syntax.

## 4. Target Abstractions & Event Bus

- [ ] 4.1 Define abstract `TargetExecutor` interface with `captureState`, `execute`, and `getSupportedActions` methods.
- [ ] 4.2 Define `SutState` interface (DOM source, attachments, content hash) and `ActionDefinition` records.
- [ ] 4.3 Implement `MockTargetExecutor` and `MockSutState` returning canned HTML strings and screenshot hashes in tests.
- [ ] 4.4 Define pipeline and diagnostic events (`StepStartedEvent`, `StateCapturedEvent`, `ActionExecutedEvent`, `StepFinishedEvent`, `SessionFinishedEvent`, `DiagnosticInfoEvent`, `DiagnosticWarningEvent`, `DiagnosticErrorEvent`).
- [ ] 4.5 Implement `ExecutionEventBus` allowing listeners to register, receive events, and including an active listeners tracking set to prevent re-entrant recursion.
- [ ] 4.6 Write TDD tests checking that event bus dispatches events in the correct order, and blocks re-entrant listener loop recursion.

## 5. Composable Pipeline & State Machine Runner

- [ ] 5.1 Define `PipelineStep` interface and base `PipelineException` class.
- [ ] 5.2 Implement subclass exceptions: `HealingRequiredException`, `DivergenceException`, and `ConclusiveFailureException`.
- [ ] 5.3 Implement `EscalationException` and subclass `ToLevelEscalationException` to support relative and target level context jumps.
- [ ] 5.4 Implement `StepSplitException` carrying parsed child sub-steps.
- [ ] 5.5 Implement `ExecutionContext` carrying the LIFO stack runner queue, session context, transient data map, and a mutable recording metadata map.
- [ ] 5.6 Implement structural pipeline steps: `SequenceStep`, `ConditionalBranchStep`, and `LoopStep`.
- [ ] 5.7 Implement `TryCatchStep` holding a map of caught exception classes to subpipeline handlers.
- [ ] 5.8 Implement concrete runner step `CaptureStateStep` (context-level state capture).
- [ ] 5.9 Implement concrete runner steps: `CallLlmStep` (compiles and sends LLM query) and `ExecuteActionsStep` (executes actions on executor).
- [ ] 5.10 Implement concrete runner steps: `VerifyOutcomeStep` (post-step assertions) and `PrepareRetryStep` (clears input/closes alerts).
- [ ] 5.11 Define the abstract `AiSession` class holding the execution context, LLM capability registry, event bus, and session data, implementing `AutoCloseable`.
- [ ] 5.12 Define the `PreExecutionHook` and `PostExecutionHook` interfaces and support registering and executing them sequentially at session boundaries.
- [ ] 5.13 Implement concrete package-private session classes (`MockBrowserSession`, `MockRestSession`) and static factory methods on `AiSession` (e.g. `AiSession.mock()`) for testing.
- [ ] 5.14 Implement `StateMachineRunner` that wires the static live and replay pipelines and executes inside the active session.
- [ ] 5.15 Write TDD integration tests executing mock playbooks browserless. Verify successful step execution, pre/post execution hook lifecycles, event-driven warning diagnostics collection, self-healing loops, splits, and divergence.

## 6. Concrete Domain Implementation (Selenide)

- [ ] 6.1 Implement `SelenideTargetExecutor` and `BrowserSutState` wrapping browser operations.
- [ ] 6.2 Implement Basic Auth interception in `SelenideTargetExecutor` using Selenium 4's `HasAuthentication` interface.
- [ ] 6.3 Implement REST request interceptors in `RestTargetExecutor` injecting Bearer/Basic headers into API calls.
- [ ] 6.4 Implement browser action plugins (`ClickAction`, `TypeAction`, etc.) resolving selectors and operating on elements.
- [ ] 6.5 Implement `PlaybookRecorder` listener capturing step events to compile the final JSON recording.
- [ ] 6.6 Write integration tests inside the `Aura Glance Sandbox` (`AuraGlanceTest.java`) to run actual browser sessions with mock/real LLM connections.

## 7. Debugger & HUD Integration

- [ ] 7.1 Define `SessionDebugger` interface with pause, resume, stepOver, and toggleBreakpoint signatures.
- [ ] 7.2 Implement breakpoint checks and thread locks inside `StateMachineRunner` before step transitions.
- [ ] 7.3 Implement stack rewinding (`rewindTo`) in `SessionDebugger` that clears sub-steps and resets execution stack cursors.
- [ ] 7.4 Create unit tests verifying debugger halts, step-overs, stack rewinding, and dynamic playbook updates during execution breaks.
