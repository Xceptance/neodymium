# Full-Page Screenshot Capture

## Purpose

Provides full-page screenshot capture capabilities for web application test execution, visual report generation, and AI visual context analysis, with support for escalation-based triggers and explicit full-page visual tags.

## Requirements

### Requirement: Full-Page Screenshot Capture
The system SHALL support capturing full-page screenshots encompassing the complete scrollable area of the document beyond the visible browser viewport.

#### Scenario: Capturing full-page screenshot when enabled
- **WHEN** screenshot capture is invoked with full-page screenshot mode enabled
- **THEN** the system captures the full scrollable document height and width and returns the full-page image

### Requirement: Escalation-Driven Full-Page Screenshot Trigger
The system SHALL initiate visual context at standard viewport screenshot size for `VISUAL` context level, and automatically escalate to full-page screenshot capture upon visual context escalation (`VISUAL_LEAN`, `VISUAL_RICH`).

#### Scenario: Initial visual step uses viewport screenshot
- **WHEN** an AI step is executed at `VISUAL` context level without prior escalation
- **THEN** the system captures a standard viewport screenshot

#### Scenario: Escalated visual step switches to full-page screenshot
- **WHEN** an AI step escalates visually (from `VISUAL` to `VISUAL_LEAN` or `VISUAL_RICH`)
- **THEN** the system captures a full-page screenshot for the escalated context

### Requirement: Explicit Full-Page Visual Tag ((visual: full) / (visual:full))
The system SHALL support explicit `(visual: full)` and `(visual:full)` tags (prefixed with `(visual`) that immediately capture a full-page screenshot on the initial attempt.

#### Scenario: Step specifies visual: full tag
- **WHEN** an AI step specifies `(visual: full)` or `(visual:full)` tag
- **THEN** the system immediately captures a full-page screenshot without requiring prior escalation

### Requirement: Viewport Highlighting Overlay
The system SHALL allow rendering a visual highlight boundary around the visible viewport coordinates within full-page screenshots.

#### Scenario: Viewport highlight on full-page screenshot
- **WHEN** full-page screenshot capture is executed with viewport highlighting enabled
- **THEN** the system draws a visual highlight border around the region of the full-page screenshot matching the scroll position and dimensions of the current viewport
