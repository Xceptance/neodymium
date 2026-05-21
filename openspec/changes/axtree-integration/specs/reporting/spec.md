## ADDED Requirements

### Requirement: Context Level Reporting in Execution Logs
The system MUST record when `AXTREE` is used as the active context level in execution logs and test results.

#### Scenario: Execution log capture
- **WHEN** a test step executes using the AXTREE context level
- **THEN** the execution logs and Playbook metadata SHALL indicate `Attempt [...] [AXTREE]` to show exactly what level of context was evaluated
