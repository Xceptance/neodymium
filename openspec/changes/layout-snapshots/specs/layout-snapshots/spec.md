## Purpose

Provides deterministic, fuzzy layout snapshot recording and replay verification using browser-side color wireframe neutralization and 3-channel color SSIM matching.

## ADDED Requirements

### Requirement: Layout Directive Recognition and Parameterization
The system SHALL parse step instructions to identify layout verification directives using `(layout)` case-insensitively, supporting optional threshold overrides (e.g. `(layout: threshold=0.90)` or `(layout: 90%)`) and full-page context flags (`(layout: full)`).

#### Scenario: Step specifies standard layout directive
- **WHEN** a playbook step contains `(layout)` without explicit parameters
- **THEN** the system marks the step as a layout step with default similarity threshold of 0.92

#### Scenario: Step specifies custom threshold override
- **WHEN** a playbook step contains `(layout: threshold=0.88)` or `(layout: 88%)`
- **THEN** the system parses the threshold and assigns an SSIM minimum score of 0.88 to the step

#### Scenario: Step specifies full-page layout directive
- **WHEN** a playbook step contains `(layout: full)`
- **THEN** the system enables full-page scrollable capture for the layout step

### Requirement: Color Wireframe Transformation During State Capture
The system SHALL inject a temporary color wireframe transformation into the active browser page prior to capturing layout screenshots, and SHALL revert the transformation immediately following capture.

#### Scenario: Wireframe transforms dynamic content and preserves design system
- **WHEN** the browser executes the wireframe transformation
- **THEN** all `img`, `video`, `canvas`, and content `svg` elements are replaced by solid neutral boxes, all text nodes are rendered as colored bars using their computed `currentColor`, content background images are stripped, and all container background colors, borders, and dimensions are preserved

#### Scenario: Wireframe cleanup restores interactive page state
- **WHEN** screenshot capture for the layout step finishes
- **THEN** the wireframe stylesheet is removed from the DOM and the live page returns to its exact original appearance within 5 milliseconds

### Requirement: 3-Channel Color SSIM Baseline and Replay Comparison
The system SHALL downsample wireframe screenshots into a 3-channel RGB 128x128 matrix, record the color matrix into the playbook step during recording mode, and evaluate color SSIM against the baseline during replay mode.

#### Scenario: Replay matches baseline within layout tolerance
- **WHEN** a test replaying a layout step produces a live color wireframe scoring greater than or equal to the step's layout threshold (default 0.92)
- **THEN** the system approves the layout step without invoking external LLMs and continues execution at machine speed

#### Scenario: Replay detects structural or color divergence
- **WHEN** a test replaying a layout step produces a live color wireframe scoring below the step's layout threshold
- **THEN** the system rejects the step with a DivergenceException detailing the actual score, expected threshold, and layout instruction
