## ADDED Requirements

### Requirement: Relative Context Level Escalation
When a step execution or validation fails, the runner MUST escalate the detail level of the captured SUT state sequentially (`AXTREE` ➔ `LEAN` ➔ `STANDARD` ➔ `VISUAL`) using a retry budget.

#### Scenario: Step failure escalates context level
- **WHEN** the runner executes a step at `LEAN` level and it fails verification
- **THEN** it catches the failure, consumes 1 unit of retry budget, upgrades the context level to `STANDARD`, clears the cached state, and retries execution.

### Requirement: Absolute Target Level Jumps
If a step fails with a specific error or diagnosis, it MUST support jumping directly to a designated target `ContextLevel`, bypassing the standard relative sequential order.

#### Scenario: Visual level jump
- **WHEN** a verification step throws a `ToLevelEscalationException` targeting the `VISUAL` level
- **THEN** the context level is set directly to `VISUAL`, skipping `STANDARD` level, and SUT state is captured.
