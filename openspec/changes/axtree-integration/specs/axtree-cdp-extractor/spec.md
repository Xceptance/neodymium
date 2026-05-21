## ADDED Requirements

### Requirement: Native Chrome DevTools Protocol Accessibility Tree Retrieval
The system MUST support browser-native accessibility tree extraction directly via the Chrome DevTools Protocol (CDP) for Chromium-based browsers.

#### Scenario: Successful AXTree Retrieval on Chromium
- **WHEN** the browser is Chromium-based (Chrome or Edge) and has active session
- **THEN** the system SHALL successfully execute the `Accessibility.getFullAXTree` CDP command to fetch the full accessibility tree

### Requirement: Transparent Driver Check and Cross-Browser Fallback
The system MUST gracefully fall back to JS-based `LEAN` DOM extraction if the active WebDriver does not support CDP or if the CDP command execution fails.

#### Scenario: Fallback on Firefox or Safari
- **WHEN** the driver is not a Chromium driver (e.g., Firefox or Safari) or when `HasCdp` is not implemented
- **THEN** the system SHALL transparently execute standard JS `LEAN` extraction and proceed without throwing exceptions

### Requirement: Dynamic DOM Mapping and data-neo-ref ID Stamping
The system MUST map accessibility nodes to standard DOM elements and stamp unique `data-neo-ref` identifiers on interactive elements dynamically using CDP Runtime execution.

#### Scenario: Stamping refs using remote object execution
- **WHEN** an accessibility node is identified and resolved to a backend DOM node ID
- **THEN** the system SHALL execute `DOM.resolveNode` and `Runtime.callFunctionOn` to stamp the unique reference ID on the live DOM element
