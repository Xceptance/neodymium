## ADDED Requirements

### Requirement: Semantic Divergence Diffing
When a divergence is detected, the runner MUST perform a comparison between the baseline state and current state to build a Semantic Diff Summary before calling the action generation prompt.

#### Scenario: Element ID update is identified
- **GIVEN** a recorded element was `#btn-checkout`
- **AND** the current page has renamed it to `#btn-pay-now`
- **WHEN** replay diverges on this step
- **THEN** the runner performs a comparison query and passes the diff summary `ID changed from checkout to pay-now` to the action prompt.

### Requirement: Visual Root Cause Analysis (RCA) Diagnostics
When a test execution fails conclusively or breaks at a debugger breakpoint, the runner MUST run a multimodal Vision-based LLM query to analyze the page state and record a plain-English explanation.

#### Scenario: Visual RCA on overlap
- **GIVEN** the Checkout button is obscured by a cookies popup
- **WHEN** the step fails conclusively after all context escalation retries
- **THEN** the runner runs Visual RCA and publishes a `DiagnosticErrorEvent` explaining that the button is covered.
