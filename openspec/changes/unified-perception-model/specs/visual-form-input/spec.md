## Purpose

Formalizes input and action handling for `<canvas>`, SVG, and purely visual elements by decoupling coordinate-based focus clicks from raw keyboard event dispatch and protecting clicks via SSIM tile gating.

## ADDED Requirements

### Requirement: Anchor-Relative Spatial Pinning
The system SHALL calculate and store Tier 5 visual click coordinates relative to the nearest stable parent DOM container rather than absolute viewport coordinates.

#### Scenario: Element inside canvas moves with container
- **WHEN** a canvas container shifts downward due to an injected promotional banner
- **THEN** the target coordinates calculated relative to the container remain spatially aligned with the canvas content

### Requirement: SSIM Tile Micro-Crop Gating
The system SHALL capture a $64 \times 64$ luminance tile around the target centroid during recording and verify the live region against this baseline ($\ge 0.95$ SSIM) before dispatching coordinate actions during replay.

#### Scenario: Safe abortion on visual reflow
- **WHEN** a responsive layout reflow alters the content under the target coordinates such that local SSIM falls below 0.95
- **THEN** the system aborts the blind coordinate click and escalates to multimodal LLM healing

### Requirement: Visual Form Input Decoupling
The system SHALL handle typing into non-DOM visual elements by first issuing an anchor-relative coordinate click to grant focus, followed by raw browser keyboard event dispatch via the WebDriver Actions API.

#### Scenario: Typing into a WebGL form input
- **WHEN** a playbook step requests typing into a canvas-based form field
- **THEN** the runner clicks the visual target coordinates to activate keyboard focus and dispatches raw keystrokes without calling element.sendKeys()
