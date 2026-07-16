## ADDED Requirements

### Requirement: Lossless parsing of select options
The `PageAnalyzer` SHALL extract `<option>` elements within `<select>` elements and represent them without stripping internal whitespace like non-breaking spaces. Options SHALL be represented unambiguously (either as a properly formatted JSON array of strings or as separate child elements) to allow accurate automated testing.

#### Scenario: Select element with complex option texts
- **WHEN** a `<select>` element has `<option>`s containing non-breaking spaces or commas
- **THEN** the analyzer output accurately retains those characters without trimming them away

#### Scenario: Array representation in options attribute
- **WHEN** options are parsed into the `options` attribute (as opposed to separate elements)
- **THEN** they MUST be formatted as a valid quoted array (e.g., `options='["yearly", "half-yearly"]'`) so values with spaces or commas are unambiguous
