## ADDED Requirements

### Requirement: Pluggable Post-Execution Auditors
The framework MUST support executing a list of registered `PostExecutionHook` verification auditors at the end of each session.

#### Scenario: Running multiple auditors
- **WHEN** the session completes execution
- **THEN** it executes `LlmExecutionAuditor`, `DataConsistencyAuditor`, and `AuraVisualAuditor` sequentially.

### Requirement: Auditor Warnings Logging
If a post-execution auditor detects a soft issue, it MUST publish diagnostic events to the event bus to be stored in the final recording.

#### Scenario: Data consistency auditor finds abnormal ID
- **WHEN** `DataConsistencyAuditor` runs and detects an extracted order ID containing letters instead of only digits
- **THEN** it publishes a `DiagnosticWarningEvent` which the playbook recorder captures in the recording metadata.
