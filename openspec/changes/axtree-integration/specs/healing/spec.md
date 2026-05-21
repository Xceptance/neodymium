## ADDED Requirements

### Requirement: Self-Healing with AXTree Coordinates
The system MUST support self-healing of cached actions when replaying steps under `AXTREE` context levels.

#### Scenario: Self-healing a reference
- **WHEN** a recorded playbook action reference fails to match exactly on a modified webpage layout
- **THEN** the system SHALL re-evaluate the page using the active AXTREE representation to locate the healed candidate and map its reference accordingly
