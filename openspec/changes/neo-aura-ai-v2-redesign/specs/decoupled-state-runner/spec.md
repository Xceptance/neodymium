## ADDED Requirements

### Requirement: Domain-Blind State Machine Execution
The state machine runner MUST execute scenario steps using only abstract execution interfaces (`TargetExecutor`, `SutState`, and `Action`). It SHALL NOT make direct calls to Selenide, Selenium, or any specific driver.

#### Scenario: Browserless simulation run
- **WHEN** the runner is executed using `MockTargetExecutor` and `MockLlmProvider`
- **THEN** it executes the playbook steps successfully, resolves actions, and processes outcomes without invoking a browser or making external API calls.

### Requirement: Exception-Based Pipeline Flow Control
Pipeline control flow and execution routing MUST be managed by throwing typed subclasses of `PipelineException` inside composite `TryCatchStep` nodes. The runner SHALL NOT use custom flow control enums or wrapper result objects for non-standard flow control.

#### Scenario: Catch block routing
- **WHEN** a composite pipeline step encounters an execution error and throws a `HealingRequiredException`
- **THEN** the outer `TryCatchStep` intercepts the exception and executes the registered catch subpipeline matching the exception type.

### Requirement: Pre/Post-Execution Lifecycle Hooks
The execution session MUST run all registered `PreExecutionHook` implementations sequentially before starting step executions, and run all registered `PostExecutionHook` implementations sequentially after step executions complete. If any hook throws an exception, the session SHALL immediately terminate and propagate the exception. Hooks MUST support publishing non-blocking warnings or diagnostic info by sending events to the `ExecutionEventBus`.

#### Scenario: Setup hook fails and aborts run
- **WHEN** a registered `PreExecutionHook` throws an exception at start
- **THEN** the session does not capture SUT state or run the pipeline, aborts immediately, and propagates the exception to the caller.

#### Scenario: Post-execution hook publishes diagnostic warning
- **WHEN** a registered `PostExecutionHook` publishes a `DiagnosticWarningEvent` to the event bus
- **THEN** the session runner captures the warning and attaches it to the final `PlaybookRecording` metadata without aborting or failing the run.
