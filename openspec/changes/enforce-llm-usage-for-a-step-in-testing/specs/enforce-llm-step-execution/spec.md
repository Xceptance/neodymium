## ADDED Requirements

### Requirement: Enforced LLM Execution Engine Routing
The execution engine (`AiBrowser`) SHALL bypass all deterministic local fallbacks, playbook caching, and heuristic matching for any execution step flagged for enforced LLM processing. It SHALL route the step's prompt directly to the underlying LLM client for live evaluation.

#### Scenario: Step flagged for LLM enforcement
- **WHEN** the execution engine receives a step configured to enforce LLM usage
- **THEN** it skips the playbook cache and invokes the LLM directly to resolve the step

### Requirement: Enforced LLM Step Telemetry
The framework SHALL log and include in the test report a clear indicator when a step was forcibly evaluated by the LLM.

#### Scenario: Telemetry reporting for forced LLM step
- **WHEN** a step configured to enforce LLM usage completes execution
- **THEN** the execution report and logs display a tag or marker confirming that a live LLM evaluation was enforced
