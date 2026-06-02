## ADDED Requirements

### Requirement: Tracking Per-Step Execution Context Metrics
The test execution engine SHALL dynamically track step-by-step PESAP execution context metrics for each step in a playbook during the run.

#### Scenario: Recording starting and final context level
- **WHEN** a playbook step is executed with PESAP enabled
- **THEN** the system SHALL record the step index, instruction text, predicted starting context level (before-state), final successful context level (after-state), number of escalations, and retry counts in the active run stats.

### Requirement: Final Console Verdict Banner
The system SHALL print a diagnostic summary and suggested rules to the standard console at the end of a successful test run.

#### Scenario: Console output with optimizations and rules template
- **WHEN** the test run completes successfully with PESAP metrics collected and console summary is enabled
- **THEN** the system SHALL print a "PESAP Execution Verdict" section to the console displaying step metrics and a copy-pasteable custom rules template containing suggested optimal starting context levels based on escalations and retries.

### Requirement: Markdown Test Report Integration
The reporting system SHALL format and append a dedicated PESAP execution verdict and recommendations section to the final generated Markdown test report.

#### Scenario: Report file containing detailed PESAP table and recommendations
- **WHEN** the Markdown test report file is generated at the end of a successful test run
- **THEN** it SHALL contain a dedicated section titled "🔮 PESAP Execution Verdict & Optimization Recommendations" displaying a table comparing the before-state (starting context level) and after-state (final context level) for each step, and clear, actionable rule recommendations to optimize future runs.
