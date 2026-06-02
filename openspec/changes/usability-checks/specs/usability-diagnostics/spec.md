## ADDED Requirements

### Requirement: Usability Action Registration
Neodymium SHALL register a new core AI action plugin `UsabilityDiagnosticsAction` that parses natural language script steps related to UX and usability checks and maps them to usability assertions. The parser SHALL recognize steps containing the `(usability)` or `[usability]` tag case-insensitively anywhere in the instruction.

#### Scenario: Parse basic step with usability tag
- **WHEN** the AI Playbook runs a step `Audit page copy readability (usability)`
- **THEN** the system parses the step as a usability check, routing it directly to the usability diagnostics engine

### Requirement: Dead-End Interactivity Auditing
The `UsabilityDiagnosticsAction` SHALL support scanning the active DOM to detect "dead-end" interactive elements. An element SHALL be flagged as a dead-end if it is styled as interactive (specifically: has CSS `cursor: pointer` or interactive framework class names like `btn`, `nav-link`, `btn-primary`, or `btn-secondary`) but lacks any active action mechanism (such as a valid `href` attribute, inline `onclick` handler, registered event listeners, or standard form submittal role).

#### Scenario: Detect buttons with missing action handlers
- **WHEN** the step is `Verify no dead-end click targets (usability)` and a decorative icon element has `cursor: pointer` but no action/link associated with it
- **THEN** the action fails throwing an `AssertionError` specifying the selector, text content, and coordinates of the dead-end element

### Requirement: Copy Readability Scoring
The `UsabilityDiagnosticsAction` SHALL evaluate readability metrics on specified elements or overall page text. The evaluation SHALL compute average sentence length, syllable counts, and readability indexes (e.g. Flesch-Kincaid Grade Level) and verify that the complexity does not exceed a configured or step-specified threshold grade level.

#### Scenario: Readability score exceeds target threshold
- **WHEN** the step is `Verify readability of '.legal-terms' (max-grade: 12) (usability)` and the text grade level is calculated as 16 (highly complex jargon)
- **THEN** the action fails throwing an `AssertionError` stating that the text complexity grade of 16 exceeds the maximum permitted grade level of 12

### Requirement: User-Friendly Empty States
The `UsabilityDiagnosticsAction` SHALL support scanning data grids, search results lists, or table containers for empty state messages. If a list or table container is empty (contains no child rows/items), the action SHALL assert that a user-friendly empty state message or placeholder is visible within the container, failing if the container is rendered completely blank.

#### Scenario: Empty search result grid lacks placeholder message
- **WHEN** the step is `Verify search results empty state (usability)` and the selector `.search-results-grid` contains no items and has no visible text message explaining the empty state
- **THEN** the action fails throwing an `AssertionError` stating that the empty search results container lacks a user-friendly message

### Requirement: Viewport Usability Health Check Action
The `UsabilityDiagnosticsAction` SHALL support natural language steps containing the `(usability)` tag to execute a full viewport layout and usability health audit.

#### Scenario: Verify viewport health with no issues
- **WHEN** the step is `Verify viewport health (usability)` and no horizontal scrollbars exist, no text elements are visually clipped, and no interactive elements are off-screen
- **THEN** the action passes successfully

#### Scenario: Verify viewport health with horizontal overflow scrollbar
- **WHEN** the step is `Verify viewport health (usability)` and a horizontal scrollbar exists (`scrollWidth > innerWidth`)
- **THEN** the action fails throwing an `AssertionError` specifying the overflowing element that exceeds the viewport bounds

#### Scenario: Verify viewport health with clipped text
- **WHEN** the step is `Verify viewport health (usability)` and a visible text element is visually clipped by its container (scroll width/height exceeds client width/height without text-overflow: ellipsis)
- **THEN** the action fails throwing an `AssertionError` identifying the clipped element and its cut-off text content

#### Scenario: Verify viewport health with off-screen interactive elements
- **WHEN** the step is `Verify viewport health (usability)` and an interactive element is positioned completely off-screen (outside the active viewport bounds)
- **THEN** the action fails throwing an `AssertionError` identifying the off-screen element and its coordinates

