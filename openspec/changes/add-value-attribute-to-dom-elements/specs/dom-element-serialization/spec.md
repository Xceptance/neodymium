## ADDED Requirements

### Requirement: Serialize DOM Element Value
The system SHALL include the `value` attribute/property when serializing DOM elements for LLM context.

#### Scenario: Serializing an input element with a value
- **WHEN** an `<input>` element with a current value of "test input" is serialized
- **THEN** the serialized representation sent to the LLM includes `value="test input"`

#### Scenario: Serializing a textarea element with content
- **WHEN** a `<textarea>` element containing "multiline text" is serialized
- **THEN** the serialized representation includes `value="multiline text"`
