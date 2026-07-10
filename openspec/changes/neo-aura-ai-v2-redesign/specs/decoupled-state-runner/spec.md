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
