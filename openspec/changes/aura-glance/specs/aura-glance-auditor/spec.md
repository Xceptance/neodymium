## ADDED Requirements

### Requirement: Multimodal AI visual glance audit
The system SHALL evaluate page screenshots and Aura AST profiles using Gemini's multimodal visual reasoning. The AI MUST observe the page layout as a human designer would to flag obvious visual anomalies, style guides drift, overlaps, clipped content, alignment errors, and usability violations.

#### Scenario: Visual anomalies detected by observation
- **WHEN** Gemini scans a page screenshot where a modal close button overlaps the header text
- **THEN** the system registers a visual overlap anomaly including the approximate coordinates and a clear textual description of the issue.

---

### Requirement: Non-blocking warning mode via (soft) tag
The system SHALL support non-blocking assertions when playbook steps contain the case-insensitive `(soft)` tag. If anomalies are found, they MUST be logged as `status: "WARNING"` in the execution report without raising a test-halting exception.

#### Scenario: Visual warning logged for soft step
- **WHEN** the playbook executes a step `Observe visual consistency (soft) (visual)` on a page with a clipped paragraph
- **THEN** the system logs the clipped paragraph anomaly as a `WARNING` entry, displays a console warning, and continues executing subsequent steps.

---

### Requirement: Optional step silent bypassing
The system SHALL support silent execution bypassing when playbook steps contain the case-insensitive `(optional)` tag. If an optional action fails or is skipped due to page state, the runner MUST log `status: "SKIPPED"` (or `"PASSED"`) without writing warnings or failing the test.

#### Scenario: Optional step silently skipped
- **WHEN** the playbook step `Click "Close Banner" (optional)` fails because the banner is not present
- **THEN** the system registers `status: "SKIPPED"` in the test trace and safely continues execution without warnings or errors.

---

### Requirement: Low-latency local Visual Playbook cache
The system SHALL support a local dHash-based Visual Playbook caching engine. The system MUST compute the live viewport screenshot's perceptual dHash and compare it locally to the approved baseline. If the Hamming distance is within the approved limit, the system MUST bypass the LLM visual audit entirely and pass the step instantly and offline.

#### Scenario: Stable UI state bypasses LLM
- **WHEN** a `(visual)` step is executed and the live screen dHash matches the approved baseline dHash (Hamming distance $\le 2$)
- **THEN** the system bypasses all LLM vision calls and marks the step as passed offline in under 50 microseconds.

---

### Requirement: Immediate on-demand visual assertions
The system SHALL support explicit, synchronous visual assertions on the active viewport. The engine MUST evaluate the current viewport's screenshot and Aura AST against general heuristics immediately and throw an `AssertionError` listing all discovered visual violations if any are found (where `(soft)` is not specified).

#### Scenario: Visual assertion fails inline
- **WHEN** an on-demand visual glance assertion is triggered on a page where main text overlaps a sidebar and no soft-tag is active
- **THEN** the system immediately throws an `AssertionError` describing the layout collision, failing the test step inline.

---

### Requirement: Interactive visual diagnostics dashboard
The system SHALL index and display captured visual anomalies in the Aura Server Trace Viewer dashboard, rendering bounding box overlays dynamically on top of screenshots and detailing the AI's textual reasoning.

#### Scenario: Visual lab dashboard display
- **WHEN** the test suite run completes and results are opened in Aura Server
- **THEN** the visual lab displays the screenshots with interactive swipe sliders, highlighted bounding boxes for anomalies, and an "Approve Baseline" control to baseline intentional design changes.
