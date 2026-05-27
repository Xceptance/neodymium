## ADDED Requirements

### Requirement: Bounding-box collision detection
The system SHALL evaluate the coordinates of all visible elements and detect overlapping visual bounding boxes between elements that are not in a direct parent-child or descendant relationship, representing potential text overlaps.

#### Scenario: Visual overlapping elements detected
- **WHEN** the anomaly detector scans a page state where two separate text divs overlap each other
- **THEN** the system registers a visual collision anomaly including the exact pixel intersection area.

### Requirement: Viewport boundary overflow check
The system SHALL identify elements whose physical bounds extend beyond either the browser viewport width or their parent container's visible bounds (where overflow is not explicitly handled), which causes clipped content or unwanted horizontal scrolling.

#### Scenario: Element clipping detected
- **WHEN** an element's right boundary exceeds the viewport width of the browser
- **THEN** the system logs a viewport overflow anomaly for that element.

### Requirement: Statistical style anomaly detection
The system SHALL dynamically cluster captured elements by structural patterns (such as identical class lists, roles, or CSS selectors) and evaluate their computed style properties. The system MUST flag elements whose style properties (e.g., background color, font size, border radius) deviate statistically from the majority standard of their cluster.

#### Scenario: Inconsistent button styling discovered
- **WHEN** 19 primary buttons have a blue background and 1 primary button has a red background
- **THEN** the system detects a visual style anomaly on the single red button.

### Requirement: Standard UX and layout heuristics
The system SHALL enforce built-in, general UX layout linting heuristics, including validating that all interactive tap targets meet minimum sizing limits (e.g., 44x44 pixels) and that all input elements have visible, associated text labels or accessible descriptions.

#### Scenario: Small tap target detected
- **WHEN** an interactive button has a bounding box of 30x20 pixels
- **THEN** the system raises a heuristic size violation anomaly.

### Requirement: Natural language style and positioning rules
The system SHALL parse a set of natural language visual guidelines using standard sentence patterns and evaluate them against all captured element states. The system MUST support style assertions (e.g., "<selector> must have <property> <value>") and spatial assertions (e.g., "<selector-A> must be <left-of/above/right-of/below> <selector-B>").

#### Scenario: Natural language rule violation flagged
- **WHEN** a parsed natural language rule specifies "primary buttons must have background-color #0056b3" and a primary button is computed as red
- **THEN** the system registers a design rule violation anomaly.

### Requirement: Immediate on-demand validation
The system SHALL support explicit, synchronous visual consistency assertions on the active page state. The engine MUST evaluate the current viewport's computed layout metadata against general heuristics and compiled design guidelines immediately and throw an `AssertionError` listing all discovered violations if any are found.

#### Scenario: Visual validation throws inline assertion error
- **WHEN** an on-demand visual consistency assertion is triggered on a page where a modal close button is misaligned
- **THEN** the system immediately throws an `AssertionError` describing the misalignment violation.

### Requirement: Interactive visual anomaly reporting


The system SHALL generate an interactive visual report (such as JSON data or a self-contained HTML page) summarizing all captured anomalies, including screenshots with highlighted bounding boxes and detailed style discrepancies.

#### Scenario: Diagnostic report generation
- **WHEN** the test suite execution finishes
- **THEN** the system writes a comprehensive visual consistency report to the designated build target directory.
