## ADDED Requirements

### Requirement: DOM and screenshot capture
The system SHALL capture the viewport screenshot and detailed DOM structural layouts (Aura AST, including computed colors, fonts, margins, paddings, display, and visibility properties of visible leaf text and interactive elements) in the active viewport.

#### Scenario: Visual profile extraction on active page
- **WHEN** layout capture is triggered on the active web page
- **THEN** the system returns a structured model containing computed CSS properties, coordinates, and tag hierarchies of visible leaf elements, alongside a high-resolution screenshot.

---

### Requirement: Background capture via AuraCaptureListener
The system SHALL support automatic visual profile collection during test execution by hooking into standard WebDriver events (such as post-navigation, post-click, and post-text entry) using the `AuraCaptureListener`.

#### Scenario: Automatic capture on page interaction
- **WHEN** background capture is enabled and a Neo AI test performs a click or navigation action
- **THEN** the listener automatically triggers the visual data and screenshot capture immediately after the action completes.

---

### Requirement: Performance-optimized extraction
The system MUST extract the structural and visual layout metadata using a single optimized JavaScript execution block to avoid multiple driver-to-browser round-trips and keep execution overhead under 30 milliseconds per capture.

#### Scenario: Execution of single-pass JS query
- **WHEN** visual data capture is triggered
- **THEN** the system executes a single `executeScript` call that returns the compiled Aura AST payload.

---

### Requirement: Playbook tag routing for (visual)
Neodymium's routing system SHALL scan playbook instructions for case-insensitive `(visual)` tags and route matching steps directly to `ContextLevel.VISUAL` on the very first turn.

#### Scenario: Route step containing visual tag
- **WHEN** the user runs a step `Observe visual consistency (visual)`
- **THEN** the AI agent initializes execution directly at `ContextLevel.VISUAL`, loading full DOM information and the current page screenshot without context escalation.

---

### Requirement: Configuration management
The system SHALL support configuring the background capture behavior, including enablement toggles and custom target element selectors, via `neodymium.properties`.

#### Scenario: Control capture via configuration
- **WHEN** `neodymium.visual.glancer.enabled` is set to `true` in `neodymium.properties`
- **THEN** Neodymium automatically initializes and registers the visual capture listener during test startup.
