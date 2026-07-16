## Why

When using `_include` directives inside inline steps for the AI agent within a Java test method (e.g., `ai.execute("_include: src/...")`), the inclusion path is sometimes incorrectly resolved. If the test case has an associated test data YAML file, the path is mistakenly resolved relative to that test data file's path (e.g., `logodata/tests/aura/kita/src/test/java/...`) instead of treating it appropriately. This breaks the ability to dynamically include steps from within inline strings if a test data file is present.

## What Changes

- Modify `YamlFileReader.java` (and related path resolution logic) to ensure `_include` statements correctly resolve file paths even when invoked inline via `ai.execute()` and a test data YAML file exists.
- Specifically, the path resolution must differentiate between includes originating from the filesystem (relative to the test data file) and includes originating from inline strings within Java source.

## Capabilities

### New Capabilities

- None

### Modified Capabilities

- `includes-resolution`: The `_include` path resolution behavior must be updated to correctly handle include directives within inline AI execution strings without misinterpreting the base path when a test data file exists.

## Impact

- `com.xceptance.neodymium.common.testdata.util.YamlFileReader` (and potentially related execution engine code).
- Existing test cases using `ai.execute` with `_include` directives will now correctly locate the step definitions regardless of the presence of a test data YAML file.
