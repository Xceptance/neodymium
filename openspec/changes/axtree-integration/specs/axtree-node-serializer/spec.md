## ADDED Requirements

### Requirement: Compact Accessibility Node Serialization
The system MUST serialize selected AXNodes into an ultra-low-token, semantic textual outline that clearly describes element roles, resolved accessible names, values, and states.

#### Scenario: Serializing a link or button node
- **WHEN** serializing an interactive accessibility node (e.g. link with accessible name "Login")
- **THEN** the system SHALL produce a compact representation such as `link [xc_ax_12] "Login"` that minimizes token consumption to approximately 5 tokens per element

### Requirement: Semantic Element Filtering
The accessibility tree serializer MUST filter out presentational, layout, or decorative nodes (e.g. static wrapper divs), including only nodes with interactive roles, non-empty accessible names, or key structural landmarks.

#### Scenario: Filtering layout nodes
- **WHEN** building the serialized text representation of a page containing layout grids and a clickable button
- **THEN** the system SHALL exclude the layout nodes while including the button node to prevent token bloat
