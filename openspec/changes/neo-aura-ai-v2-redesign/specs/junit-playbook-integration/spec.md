## ADDED Requirements

### Requirement: Annotation-Driven Test Execution
The framework MUST support running AI playbooks using native JUnit 5/6 annotations (`@NeodymiumAiTest` on the class, `@AiPlaybook` on the test method), resolving playbook files by package convention or explicit overrides.

#### Scenario: Running playbook by naming convention
- **GIVEN** a test class `GuestCheckoutTest` marked with `@NeodymiumAiTest`
- **AND** a playbook file `GuestCheckoutTest.yaml` exists in the same package directory
- **WHEN** the test is executed in the IDE or Maven
- **THEN** the JUnit extension loads `GuestCheckoutTest.yaml` and executes it.

### Requirement: Sequential Execution Mode Configuration
The framework MUST support executing a playbook sequentially across multiple modes (e.g. `RECORD` followed by `REPLAY_ONLY`) within a single test template invocation using the `@AiMode` annotation.

#### Scenario: Verify recording is replayable in one invocation
- **WHEN** a method is marked with `@AiMode({ExecutionMode.RECORD, ExecutionMode.REPLAY_ONLY})`
- **THEN** the runner first executes the playbook live via the LLM to generate the recording, and immediately replays the cached recording to verify consistency.

### Requirement: Dataset Filtering
The framework MUST support filtering playbook dataset execution using the `@AiDataSet` annotation, allowing inclusion or exclusion filters by `testId` using regex patterns.

#### Scenario: Excluding broken datasets
- **WHEN** a class is marked with `@AiDataSet(exclude = "broken-.*")`
- **THEN** all datasets whose `testId` matches `broken-.*` are skipped from the JUnit execution template.
