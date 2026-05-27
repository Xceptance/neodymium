## ADDED Requirements

### Requirement: Layout Diagnostics Action Registration
Neodymium SHALL register a new core AI action plugin `LayoutDiagnosticsAction` that parses natural language script steps related to layout assertions and maps them to layout verification actions. The parser SHALL recognize steps containing the `(layout)` tag case-insensitively anywhere in the instruction.

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
The `LayoutDiagnosticsAction` SHALL support alignment checks of multiple elements with alignment boundary hints (`top`, `bottom`, `left`, `right`, `vertical-center`, `horizontal-center`) from steps containing the `(layout)` tag. The system SHALL require at least two matching elements for any alignment assertion to succeed.

#### Scenario: Verify top alignment of cards
- **WHEN** the AI Playbook runs a step `Verify alignment of '.card' elements (top) (layout)`
- **THEN** the system verifies that the top Y-coordinate of all elements matching the selector `.card` are equal within the given tolerance

#### Scenario: Verify horizontal center alignment of logo and user menu
- **WHEN** the AI Playbook runs a step `Verify alignment of '.header-item' elements (horizontal-center) (layout)`
- **THEN** the system verifies that the horizontal center coordinates (X midpoint = left + width/2) of all matching elements are equal within the given tolerance

#### Scenario: Alignment check fails due to misalignment
- **WHEN** the step is `Verify alignment of '.card' elements (top) (layout)` and one card is offset vertically by 10 pixels
- **THEN** the system fails throwing an `AssertionError` identifying the misaligned element and its offset

#### Scenario: Alignment check fails due to insufficient elements
- **WHEN** the step is `Verify alignment of '.unique-logo' elements (top) (layout)` and only 1 matching element is found
- **THEN** the system fails throwing an `AssertionError` stating that alignment checks require at least 2 elements to evaluate

### Requirement: Element Overlap Diagnostics
The `LayoutDiagnosticsAction` SHALL support verification that two elements do not overlap, or that they explicitly overlap if required, from steps containing the `(layout)` tag.

#### Scenario: Verify that header and main content do not overlap
- **WHEN** the step is `Verify that '#header' does not overlap '#main-content' (layout)`
- **THEN** the system verifies that the bounding boxes of `#header` and `#main-content` do not intersect

#### Scenario: Overlap check fails
- **WHEN** the step is `Verify that '#header' does not overlap '#main-content' (layout)` and '#main-content' overlaps '#header' by 5 pixels
- **THEN** the system fails throwing an `AssertionError` detailing the intersecting bounding boxes and the overlap distance

### Requirement: Direct Context Routing for Layout Tag
Neodymium's routing system SHALL scan playbook instructions for case-insensitive `(layout)` tags and route matching steps directly to `ContextLevel.VISUAL` on the very first turn.

#### Scenario: Route step containing layout tag
- **WHEN** the user runs a step `Verify that '.logo' is left of '.menu-bar' (layout)`
- **THEN** the AI agent initializes execution directly at `ContextLevel.VISUAL`, loading full DOM information and the current page screenshot without context escalation.

### Requirement: Spacing and Alignment Tolerance Configurations
The `LayoutDiagnosticsAction` math evaluation engine SHALL utilize a default subpixel tolerance defined by the global configuration property `neodymium.ai.layout.defaultTolerance` (default: 1px), and support local step-level overrides specified via `(tolerance: Xpx)` parameters inside instructions.

#### Scenario: Spacing check uses global default tolerance
- **WHEN** the step is `Verify that '.logo' is left of '.menu-bar' (distance: 20px) (layout)` and no local tolerance parameter is specified, with `neodymium.ai.layout.defaultTolerance` set to 1px and actual gap being 19.4px
- **THEN** the assertion succeeds because the actual 0.6px deviation is within the global 1px default tolerance.

#### Scenario: Spacing check uses local step-level tolerance override
- **WHEN** the step is `Verify that '.logo' is left of '.menu-bar' (distance: 20px, tolerance: 3px) (layout)` with actual gap being 17.5px
- **THEN** the assertion succeeds because the actual 2.5px deviation is within the overridden local step-level tolerance of 3px.

### Requirement: Container Inner-Boundary Distance Diagnostics
The `LayoutDiagnosticsAction` SHALL support verification of the inner distance between a parent container element and its child elements along four edges: `top`, `bottom`, `left`, and `right`.

#### Scenario: Verify container inner distance boundary on the left
- **WHEN** the step is `Verify container '.container' has a minimum inner distance of 20px on the left (layout)`
- **THEN** the system verifies that the distance between the container's left outer edge and the left outer edge of all matching child elements inside `.container` is $\ge$ 20px within the tolerance limit.

#### Scenario: Inner boundary verification fails
- **WHEN** the step is `Verify container '.container' has a minimum inner distance of 20px on the left (layout)` and a child element has a left coordinate only 15px from the container's left edge
- **THEN** the system fails throwing an `AssertionError` identifying the offending child element, its left boundary offset, and the exact 5px violation.

### Requirement: Layout Stability Verification
To avoid false positives from moving elements, the `LayoutDiagnosticsAction` SHALL wait for element coordinates to stabilize before performing layout mathematical checks. This stabilization wait SHALL be driven by a non-blocking Java-based polling loop executing every 100ms. In each poll, Java SHALL query the target coordinates using a high-performance JavaScript call and compare them to the coordinates from the previous poll. Stability is reached when element coordinates are identical across two consecutive polls. The polling loop SHALL respect the maximum timeout specified by the global configuration property `neodymium.ai.layout.stabilityTimeout` (default: 2000ms), and support local step-level overrides specified via `(timeout: Xms)`, `(stability: Xms)`, or `(wait: Xms)`.

#### Scenario: Wait for animation to finish using global timeout
- **WHEN** an element is animating and stabilizes after 600ms, with the global timeout set to 2000ms
- **THEN** the Java polling loop queries coordinates every 100ms, detects that coordinate values remain identical between the 500ms and 600ms polls, and executes the layout verification.

#### Scenario: Wait for animation to finish using local step-level timeout override
- **WHEN** an element is animating and stabilizes after 4000ms, with a step containing `(timeout: 5000ms)`
- **THEN** the Java polling loop queries coordinates every 100ms up to 5000ms, detects stability at 4000ms, and runs the layout verification successfully.

### Requirement: Visual Overlays for Failures via Canvas
When a layout verification fails, the system SHALL temporarily inject a single, absolute-positioned HTML5 `<canvas>` element layered directly over the viewport prior to taking the failure screenshot, and remove it in the cleanup phase.

#### Scenario: Render visual overlays on temporary HTML5 canvas
- **WHEN** a spatial distance check fails between `.logo` and `.menu-bar`
- **THEN** the system injects a single absolute-positioned canvas (`z-index: 2147483647`), draws red semi-transparent bounding boxes and distance lines using the canvas 2D context, captures the failure screenshot, and removes the canvas cleanly from the DOM during cleanup.



