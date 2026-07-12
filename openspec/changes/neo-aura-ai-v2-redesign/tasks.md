## 1. Parsers, Models & Resources

- [x] 1.1 Create `PlaybookStep` class definition with fields for instruction (string), actions (list), and status (enum: PENDING, RUNNING, SUCCESS, FAILED, SPLITTED).
- [x] 1.2 Add child steps tree collection (`List<PlaybookStep> subSteps`) and helper `isComposite()` in `PlaybookStep`.
- [x] 1.3 Create `Playbook` wrapper class that acts as the root of the playbook steps.
- [x] 1.4 Define `PlaybookResourceManager` interface with signatures `InputStream read(String)`, `void write(String, String)`, and `String resolveInclude(String, String)`.
- [x] 1.5 Write TDD tests for include path resolution using mock paths.
- [x] 1.6 Implement `LocalFileResourceManager` using standard Java NIO paths.
- [x] 1.7 Implement `InMemoryResourceManager` utilizing thread-safe string maps to store playbook recordings without disk writes.
- [x] 1.8 Define `PlaybookParser` interface signature.
- [x] 1.9 Write TDD tests parsing simple YAML files and nested includes using a mock parser.
- [x] 1.10 Implement `YamlPlaybookParser` parsing playbooks recursively using snakeyaml, including a cycle-detection check (tracking the include resolution path) to prevent infinite loops.
- [x] 1.11 Implement `InlinePlaybookParser` parsing raw multi-line strings into playbook steps.
- [x] 1.12 Create JUnit 5 verification suite for full playbook parsing and resource resolution.
- [x] 1.13 Copy and rewrite core utility helper classes (e.g., config loaders, tag extractors, and element selectors) from legacy packages into `org.neodymium.ai` to ensure completely independent, side-by-side evolution.

## 2. LLM Client Interface, Registry & Prompts

- [x] 2.1 Create `LlmCapability` enum (`TEXT_ONLY`, `VISION`, `STRUCTURED_JSON`, `STEP_SPLITTING`).
- [x] 2.2 Create request/response record structures `LlmRequest` (system prompt, user query, attachments, schema) and `LlmResponse` (text content, token usage, model name).
- [x] 2.3 Define `LlmProvider` interface with `LlmResponse chat(LlmRequest)` and `Set<LlmCapability> getCapabilities()`.
- [x] 2.4 Implement `MockLlmProvider` holding a thread-safe queue of canned responses to bypass LLM API calls in tests.
- [x] 2.5 Implement `LlmRegistry` supporting dynamic capability resolution and fallback to a default provider.
- [x] 2.6 Implement hierarchical configuration properties loading (`AiConfiguration`) resolving global defaults and role-specific (`pesap`, `execution`, `vision`, `audit`) overrides.
- [x] 2.7 Implement provider bootstrapping that instantiates and registers separate role-specific `LlmProvider`s based on configuration overrides.
- [x] 2.8 Define `AiPrompt<T>` interface specifying the generic prompts compilation and multi-stage response parsing/repairing.
- [x] 2.9 Write TDD tests asserting that prompts format system/user messages correctly and repair malformed model JSON responses.
- [x] 2.10 Implement a concrete prompt class (e.g. `ActionsPrompt` or `StringPrompt`) and verify its output parsing.
- [x] 2.11 Define and implement `PromptBuilderService` supporting model-specific formatting overlays (e.g. Gemini vs Mistral).
- [x] 2.12 Implement Multi-Stage Response Repairer processing raw strings, JSON Element trees, and deserialized model objects.

## 3. Session Data & Sanitization

- [x] 3.1 Create `SessionData` class supporting static (immutable) variables and dynamic (mutable concurrent) variables.
- [x] 3.2 Implement snapshot history mapping in `SessionData` to capture dynamic variable states.
- [x] 3.3 Write TDD tests verifying variable values lookup precedence (Dynamic > Static), state rollback, and immutability of the static dataset (defensive copying).
- [x] 3.4 Implement `ContextSanitizer` scanning payloads for credentials, replacing them with format-preserving mock patterns or user stand-in values.
- [x] 3.5 Implement `ActionSanitizer` performing on-the-fly variable parameterization of executed actions (raw input replaced with variable references).
- [x] 3.6 Write TDD tests verifying pre-LLM secret masking and reverse-mapping of stand-ins.
- [x] 3.7 Write TDD tests asserting that actions executed with raw secret values are recorded with variable syntax.

## 4. Target Abstractions & Event Bus

- [x] 4.1 Define abstract `TargetExecutor` interface with `captureState`, `execute`, and `getSupportedActions` methods.
- [x] 4.2 Define `SutState` interface (DOM source, attachments, content hash) and `ActionDefinition` records.
- [x] 4.3 Implement `MockTargetExecutor` and `MockSutState` returning canned HTML strings and screenshot hashes in tests.
- [x] 4.4 Define pipeline and diagnostic events (`StepStartedEvent`, `StateCapturedEvent`, `ActionExecutedEvent`, `StepFinishedEvent`, `SessionFinishedEvent`, `DiagnosticInfoEvent`, `DiagnosticWarningEvent`, `DiagnosticErrorEvent`).
- [x] 4.5 Implement `ExecutionEventBus` allowing listeners to register, receive events, and including an active listeners tracking set to prevent re-entrant recursion.
- [x] 4.6 Write TDD tests checking that event bus dispatches events in the correct order, and blocks re-entrant listener loop recursion.

## 5. Composable Pipeline & State Machine Runner

- [x] 5.1 Define `PipelineStep` interface and base `PipelineException` class.
- [x] 5.2 Implement subclass exceptions: `HealingRequiredException`, `DivergenceException`, and `ConclusiveFailureException`.
- [x] 5.3 Implement `EscalationException` and subclass `ToLevelEscalationException` to support relative and target level context jumps.
- [x] 5.4 Implement `StepSplitException` carrying parsed child sub-steps.
- [x] 5.5 Implement `ExecutionContext` carrying the LIFO stack runner queue, session context, transient data map, and a mutable recording metadata map.
- [x] 5.6 Implement structural pipeline steps: `SequenceStep`, `ConditionalBranchStep`, and `LoopStep`.
- [x] 5.7 Implement `TryCatchStep` holding a map of caught exception classes to subpipeline handlers.
- [x] 5.8 Implement concrete runner step `CaptureStateStep` (context-level state capture).
- [x] 5.9 Implement concrete runner steps: `CallLlmStep` (compiles and sends LLM query) and `ExecuteActionsStep` (executes actions on executor).
- [ ] 5.10 Implement concrete runner steps: `VerifyOutcomeStep` (post-step assertions) and `PrepareRetryStep` (clears input/closes alerts).
- [x] 5.11 Define the abstract `AiSession` class holding the execution context, LLM capability registry, event bus, and session data, implementing `AutoCloseable`.
- [x] 5.12 Define the `PreExecutionHook` and `PostExecutionHook` interfaces and support registering and executing them sequentially at session boundaries.
- [x] 5.13 Implement concrete package-private session classes (`MockBrowserSession`, `MockRestSession`) and static factory methods on `AiSession` (e.g. `AiSession.mock()`) for testing.
- [x] 5.14 Implement `StateMachineRunner` that wires the static live and replay pipelines and executes inside the active session.
- [ ] 5.15 Implement Visual Root Cause Analysis (RCA) step triggered on conclusive failures or debugger breakpoints.
- [ ] 5.16 Implement Two-Stage Semantic Healing comparison flow (Semantic Divergence Analysis) before generating corrective actions.
- [ ] 5.17 Implement concrete PostExecutionHook classes: `LlmExecutionAuditor`, `DataConsistencyAuditor`, and `AuraVisualAuditor`.
- [x] 5.18 Write TDD integration tests executing mock playbooks browserless. Verify successful step execution, pre/post execution hook lifecycles (including auditor runs), event-driven warning diagnostics collection, self-healing loops, splits, divergence, and visual RCA.
- [ ] 5.19 Implement and test dynamic run-time `INCLUDE` action step expansion using the parser and resource manager.

## 6. Concrete Domain Implementation (Selenide)

- [x] 6.1 Implement `SelenideTargetExecutor` and `BrowserSutState` wrapping browser operations.
- [x] 6.2 Implement Basic Auth interception in `SelenideTargetExecutor` using Selenium 4's `HasAuthentication` interface.
- [x] 6.3 Implement REST request interceptors in `RestTargetExecutor` injecting Bearer/Basic headers into API calls.
- [x] 6.4 Implement browser action plugins (`ClickAction`, `TypeAction`, etc.) resolving selectors and operating on elements.
- [x] 6.5 Implement `PlaybookRecorder` listener capturing step events to compile the final JSON recording.
- [x] 6.6 Write integration tests inside the `Aura Glance Sandbox` (`AuraGlanceTest.java`) to run actual browser sessions with mock/real LLM connections.

## 7. Debugger & HUD Integration

- [ ] 7.1 Define `SessionDebugger` interface with pause, resume, stepOver, and toggleBreakpoint signatures.
- [ ] 7.2 Implement breakpoint checks and thread locks inside `StateMachineRunner` before step transitions.
- [ ] 7.3 Implement stack rewinding (`rewindTo`) in `SessionDebugger` that clears sub-steps and resets execution stack cursors.
- [ ] 7.4 Create unit tests verifying debugger halts, step-overs, stack rewinding, and dynamic playbook updates during execution breaks.

## 8. JUnit 5/6 Annotation-Driven Integration

- [ ] 8.1 Define annotations: `@NeodymiumAiTest` (class-level runner mapping), `@AiPlaybook` (method-level file override).
- [ ] 8.2 Define configuration annotations: `@AiMode` (execution mode override array), `@AiDataSet` (dataset inclusion/exclusion filter with regex support).
- [ ] 8.3 Implement `NeodymiumAiRunner` extending JUnit 5's `TestTemplateInvocationContextProvider` to parse playbooks and resolve parameter dimensions.
- [ ] 8.4 Support sharing active `AiSession` and SUT browser state across `@BeforeEach`, `@Test`, and `@AfterEach` lifecycle boundaries for a single invocation.
- [ ] 8.5 Write JUnit 5 annotation-driven integration tests executing playbooks by convention, specific file overrides, and multi-dataset filters.

