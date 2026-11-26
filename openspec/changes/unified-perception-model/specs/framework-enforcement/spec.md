## Purpose

Enforces strict execution framework matching between recording and replay phases to prevent silent DOM traversal failures caused by incompatible selector syntax.

## ADDED Requirements

### Requirement: Target Framework Metadata Inscription
The system SHALL record the active driver framework (e.g. `SELENIUM_SELENIDE` or `PLAYWRIGHT`) in the top-level companion JSON metadata during recording.

#### Scenario: Inscribing framework metadata on recording
- **WHEN** a playbook test is recorded under the Selenide target executor
- **THEN** the companion JSON metadata contains `"targetFramework": "SELENIUM_SELENIDE"`

### Requirement: Fast-Fail Compatibility Validation
The system SHALL validate the recorded `targetFramework` against the active `TargetExecutor.getFrameworkName()` prior to executing replay steps and throw an `IncompatibleFrameworkException` if a mismatch is detected.

#### Scenario: Incompatible replay engine detection
- **WHEN** a companion JSON recorded with SELENIUM_SELENIDE is replayed under a PLAYWRIGHT runner
- **THEN** the runner immediately throws an IncompatibleFrameworkException detailing the mismatch and halting execution before any browser interaction
