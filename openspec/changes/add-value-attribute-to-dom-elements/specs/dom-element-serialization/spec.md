## ADDED Requirements

### Requirement: Serialize DOM Element Value for Non-Editable Inputs
The system SHALL include the `value` attribute/property when serializing DOM elements for LLM context, but ONLY for non user-editable input types (e.g., `button`, `submit`, `reset`, `hidden`).

#### Scenario: Serializing a non-editable input element with a value
- **WHEN** an `<input type="button">` element with a value of "Click Me" is serialized
- **THEN** the serialized representation sent to the LLM includes `value="Click Me"`

#### Scenario: Serializing an editable input element with a value
- **WHEN** an `<input type="text">` or `<textarea>` element containing "user input" is serialized
- **THEN** the serialized representation DOES NOT include the `value` attribute
