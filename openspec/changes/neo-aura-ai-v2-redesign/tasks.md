## 1. Parsers, Models & Resources

- [ ] 1.1 Implement `PlaybookStep` composite tree data structure representing instructions, nested sub-steps, actions, and status fields.
- [ ] 1.2 Implement `Playbook` wrapper class holding the list of root steps.
- [ ] 1.3 Implement `PlaybookResourceManager` interface with read, write, and resolve relative path methods.
- [ ] 1.4 Implement `LocalFileResourceManager` using Java NIO paths.
- [ ] 1.5 Implement `InMemoryResourceManager` holding playbooks and recordings in memory to bypass disk I/O.
- [ ] 1.6 Implement `PlaybookParser` interface and `YamlPlaybookParser` using snakeyaml.
- [ ] 1.7 Implement `InlinePlaybookParser` for loading playbooks from test code strings.
- [ ] 1.8 Create JUnit 5 unit tests for YAML parsing and include resolving (using both LocalFile and InMemory resource managers).

## 2. LLM Client Interface, Registry & Prompts

- [ ] 2.1 Implement `LlmCapability` enum (TEXT_ONLY, VISION, STRUCTURED_JSON, STEP_SPLITTING) and client models `LlmRequest` / `LlmResponse`.
- [ ] 2.2 Implement `LlmProvider` interface and capability routing in `LlmRegistry`.
- [ ] 2.3 Implement `MockLlmProvider` allowing queuing of canned LLM responses for browserless unit testing.
- [ ] 2.4 Implement `AiPrompt<T>` interface defining prompts compilation and multi-stage response parsing/repairing.
- [ ] 2.5 Create JUnit 5 unit tests verifying mock LLM calls, capability selection, prompt compilation, and JSON response repairing/deserialization.

## 3. Session Data & Masking/Sanitization

- [ ] 3.1 Implement `SessionData` dual-layer map with snapshot and rollback logic.
- [ ] 3.2 Implement `ContextSanitizer` performing pre-LLM secret masking using format-preserving mock patterns or user stand-in values.
- [ ] 3.3 Implement `ActionSanitizer` performing on-the-fly variable parameterization of executed actions before they are recorded.
- [ ] 3.4 Create JUnit 5 unit tests for `SessionData` snapshots and rollbacks.
- [ ] 3.5 Create JUnit 5 unit tests verifying that sensitive keys are masked before LLM calls and reverse-mapped on action generation.
- [ ] 3.6 Create JUnit 5 unit tests verifying that executed actions are parameterized on the fly into variable references (e.g. raw text string replaced with `${userPassword}`).

## 4. Target Abstractions & Event Bus

- [ ] 4.1 Implement abstract `TargetExecutor`, `SutState`, and `ActionDefinition` interfaces.
- [ ] 4.2 Implement `MockTargetExecutor` and `MockSutState` representing UI and API simulated states.
- [ ] 4.3 Implement `ExecutionEventBus` and lifecycle events (StepStartedEvent, StateCapturedEvent, ActionExecutedEvent, StepFinishedEvent, SessionFinishedEvent).
- [ ] 4.4 Create JUnit 5 unit tests verifying event publisher dispatching, synchronous listener registration, and event contents.

## 5. Composable Pipeline & State Machine Runner

- [ ] 5.1 Implement `PipelineStep` interface and the typed `PipelineException` hierarchy (Escalation, Split, Divergence, Healing, Conclusive Failure exceptions).
- [ ] 5.2 Implement `ExecutionContext` maintaining the runner LIFO stack, session properties, and transient scratchpad map.
- [ ] 5.3 Implement structural composite steps: `SequenceStep`, `ConditionalBranchStep`, and `LoopStep`.
- [ ] 5.4 Implement `TryCatchStep` supporting a map of exception types to mapped catch handler subpipelines.
- [ ] 5.5 Implement concrete steps: `LintStep` (linter check), `CaptureStateStep` (adaptive state capture), `CallLlmStep` (compiled LLM prompts).
- [ ] 5.6 Implement concrete steps: `ExecuteActionsStep` (routes to executor), `VerifyOutcomeStep` (outcome assertion), `PrepareRetryStep` (in-page input clear and dialog closes).
- [ ] 5.7 Implement `StateMachineRunner` orchestrating static pipelines (`createLiveExecutionPipeline` and `createReplayPipeline`).
- [ ] 5.8 Create JUnit 5 pipeline integration tests using `MockTargetExecutor` and `MockLlmProvider`. Assert success, self-healing loop execution, dynamic context level increments, and compound step splitting (via `StepSplitException`).

## 6. Concrete Domain Implementation (Selenide)

- [ ] 6.1 Implement `SelenideTargetExecutor` and `BrowserSutState` wrapping Web WebDriver operations.
- [ ] 6.2 Implement Basic Auth interception in `SelenideTargetExecutor` using Selenium 4's `HasAuthentication` interface.
- [ ] 6.3 Implement API header interceptors in `RestTargetExecutor` (REST client authorization injection).
- [ ] 6.4 Implement browser action plugins (`ClickAction`, `TypeAction`, etc.) typed to `BrowserSutState`.
- [ ] 6.5 Implement `PlaybookRecorder` listening to event bus to compile and write recordings on session completion.
- [ ] 6.6 Create browser verification integration tests inside `Aura Glance Sandbox` (`AuraGlanceTest.java`) to verify end-to-end runs.

## 7. Debugger & HUD Integration

- [ ] 7.1 Implement `SessionDebugger` interface in `AiSession` and `StateMachineRunner`.
- [ ] 7.2 Implement breakpoints (`toggleBreakpoint`), manual pauses (`pause`), and execution step-over blocks.
- [ ] 7.3 Implement stack rewinding (`rewindTo`) clearing sub-steps and resetting stack pointers.
- [ ] 7.4 Create unit tests verifying debugger session halts, step-overs, stack rewinding, and dynamic variable/playbook mutations.
