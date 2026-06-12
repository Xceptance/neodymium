## ADDED Requirements

### Requirement: Step Syntax Parsing
The system SHALL parse step instructions to extract and validate special directives/tags (case-insensitive `(visual)`, `(glance)`, `(soft)`, `(bug)`, `(bug: <id>)`, `(hint: <selector>)`, and `(selector: <selector>)`).

#### Scenario: Parse visual and glance tags
- **WHEN** the step contains the directive `(visual)` or `(glance)`
- **THEN** the parser SHALL successfully identify and extract the directive

#### Scenario: Parse parameterised tags with nested parentheses
- **WHEN** the step contains `(hint: label[for='toggle-contrast'])` or `(selector: div:not(.active))`
- **THEN** the parser SHALL correctly extract the selector payload by balancing nested parentheses

#### Scenario: Reject malformed step syntax
- **WHEN** the step contains unbalanced parentheses like `Click (visual` or `(hint: button`
- **THEN** the parser SHALL throw a syntax validation exception or return a validation error indicating unbalanced parentheses
