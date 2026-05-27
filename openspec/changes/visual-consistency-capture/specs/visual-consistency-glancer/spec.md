## ADDED Requirements

### Requirement: DOM style and layout capture
The system SHALL capture detailed DOM structural hierarchies, computed styles (including colors, fonts, margins, paddings, display, and visibility properties), and bounding-box coordinates for all visible elements in the active viewport.

#### Scenario: Visual data extraction on active page
- **WHEN** the layout capture is triggered on the active web page
- **THEN** the system returns a structured model containing computed CSS properties, coordinates, and tag hierarchies of all visible elements.

### Requirement: Background collection via listener
The system SHALL support automatic visual profile collection during test execution by hooking into standard WebDriver events (such as post-navigation, post-click, and post-text entry) using the `NeodymiumWebDriverListener`.

#### Scenario: Automatic capture on page interaction
- **WHEN** background capture is enabled and a test performs a click or navigation action
- **THEN** the listener automatically triggers the visual data capture immediately after the action completes.

### Requirement: Performance-optimized extraction
The system MUST extract the structural and visual layout metadata using a single optimized JavaScript execution block to avoid multiple driver-to-browser round-trips and keep execution overhead under 50 milliseconds per capture.

#### Scenario: Execution of single-pass JS query
- **WHEN** the visual data capture is triggered
- **THEN** the system executes a single `executeScript` call that returns the compiled layout payload.

### Requirement: Configuration management
The system SHALL support configuring the background capture behavior, including enablement toggles and custom target element selectors, via `neodymium.properties`.

#### Scenario: Control capture via configuration
- **WHEN** `neodymium.visual.glancer.enabled` is set to `true` in `neodymium.properties`
- **THEN** Neodymium automatically initializes and registers the visual capture listener during test startup.
