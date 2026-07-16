## Why

The current parsing of `select` element options in the `PageAnalyzer` is lossy and inaccurate. The option texts are trimmed (losing spaces such as `nbsp`) and are not parsed in quotes. This causes issues with representing the original DOM accurately. For example, if options have special spaces, they are lost, and arrays of values might not be accurately depicted in either `<select>` representation or `[form-field]`.

## What Changes

- Modify `PageAnalyzer` to properly extract and format `<select>` options.
- Retain whitespace like non-breaking spaces.
- Decide and implement a new options representation: either accurately formatting them within the `options` attribute (using proper arrays/quotes), or representing them as separate indented child elements (`<option>`) below the `<select>` element.

## Capabilities

### New Capabilities
- `select-options-parsing`: Improve how `<select>` element options are parsed, formatted, and represented in the analyzed output.

### Modified Capabilities

## Impact

- `PageAnalyzer.java` and internal representation logic.
- AI test pages and test cases related to `PageAnalyzer` that assert on `<select>` elements.
