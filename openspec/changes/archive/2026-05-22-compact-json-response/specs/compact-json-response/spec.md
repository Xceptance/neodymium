## ADDED Requirements

### Requirement: Compact JSON Parsing
The ActionParser and related JSON deserializers SHALL support parsing the compact short-key JSON structures returned by the LLM.

#### Scenario: Parsing Compact Short Keys
- **WHEN** the ActionParser receives a JSON payload containing compact short keys (e.g., "s", "a", "tc", "cs", "ss", "oia", "dl") and compact action-level keys (e.g., "t", "tg", "v", "d", "ed", "c", "th", "el", "ad", "ec", "r")
- **THEN** it SHALL successfully parse all properties and reconstruct the corresponding Java objects.

### Requirement: Playbook Format Immutability
The playbooks saved to or loaded from the disk SHALL continue to use the standard long-key JSON format, and SHALL NOT use compact keys.

#### Scenario: Saving Playbook
- **WHEN** a playbook is saved using PlaybookManager
- **THEN** the JSON output SHALL use long-key fields (e.g., "type", "target", "value", "description") to preserve standard compatibility and readability.

### Requirement: Pretty-Printed Debug Logging
The LLM JSON response in debug logs SHALL be pretty-printed.

#### Scenario: Pretty Printing raw response
- **WHEN** the ActionParser parses or AiAgent logs an LLM response
- **THEN** it SHALL log the pretty-printed JSON structure of the response using its short keys under the header "   📄 --- LLM Response (Pretty-Printed) ---" to make it easily readable in debug outputs.
