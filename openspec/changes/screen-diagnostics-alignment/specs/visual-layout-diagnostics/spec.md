## ADDED Requirements

### Requirement: Layout Diagnostics Action Registration
Neodymium SHALL register a new core AI action plugin `LayoutDiagnosticsAction` that parses natural language script steps related to layout assertions and maps them to layout verification actions. The parser SHALL recognize steps containing the `(layout)` or `[layout]` tag case-insensitively anywhere in the instruction.

#### Scenario: Parse basic spatial relationship step with layout tag
- **WHEN** the AI Playbook runs a step `Verify that '.logo' is left of '.menu-bar' (layout)`
- **THEN** the system parses the step as a spatial layout check with selector A `'.logo'`, selector B `'.menu-bar'`, and relation `left-of`

### Requirement: Spatial Relations and Distance Hints
The `LayoutDiagnosticsAction` SHALL support spatial relations (`left-of`, `right-of`, `above`, `below`) and parse parenthetical distance hints including expected distance and tolerance from steps containing the `(layout)` tag.

#### Scenario: Parse relation with distance and tolerance hints
- **WHEN** the step is `Verify that '.logo' is left of '.menu-bar' (distance: 20px, tolerance: 5px) (layout)`
- **THEN** the system evaluates that the horizontal gap between the right edge of `.logo` and the left edge of `.menu-bar` is between 15px and 25px

#### Scenario: Verification fails with descriptive error
- **WHEN** the step is `Verify that '.logo' is left of '.menu-bar' (distance: 20px) (layout)` and the actual distance is 35px
- **THEN** the action fails throwing an `AssertionError` stating that the actual distance of 35px deviates from the expected 20px

### Requirement: Alignment Hints
The `LayoutDiagnosticsAction` SHALL support alignment checks of multiple elements with alignment boundary hints (`top`, `bottom`, `left`, `right`, `center`) from steps containing the `(layout)` tag.

#### Scenario: Verify top alignment of cards
- **WHEN** the step is `Verify alignment of '.card' elements (top) (layout)`
- **THEN** the system verifies that the top Y-coordinate of all elements matching the selector `.card` are equal

#### Scenario: Alignment check fails
- **WHEN** the step is `Verify alignment of '.card' elements (top) (layout)` and one card is offset vertically by 10 pixels
- **THEN** the system fails throwing an `AssertionError` identifying the misaligned element and its offset

### Requirement: Viewport Health Check Action
The `LayoutDiagnosticsAction` SHALL support natural language steps containing the `(layout)` tag to execute a full viewport layout health audit.

#### Scenario: Verify viewport health with no issues
- **WHEN** the step is `Verify viewport health (layout)` and no elements overlap, overflow, or clip
- **THEN** the action passes successfully

#### Scenario: Verify viewport health with overlapping elements
- **WHEN** the step is `Verify viewport health (layout)` and two text elements overlap each other
- **THEN** the action fails throwing an `AssertionError` specifying the overlapping selectors and their collision coordinates
