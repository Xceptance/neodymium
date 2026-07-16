## MODIFIED Requirements

### Requirement: Unified Inclusions Directive
The framework SHALL support a unified `_include:` directive to dynamically load and splice external steps or datasets (supporting full-block, inline-list, and map-level overrides). 
When an `_include:` directive is used inline within a Java step execution (e.g., via `ai.execute()`), the path resolution SHALL NOT be incorrectly relative to the test data file (if one exists for the test case), but SHALL resolve correctly relative to the project or class context.

#### Scenario: Lifecycle block inclusion
- **WHEN** a lifecycle block (e.g., `_steps`) contains an `_include: <path>` directive
- **THEN** the parser loads the referenced YAML steps file and splices its steps dynamically in-place

#### Scenario: Inline inclusion within Java test method with existing test data file
- **WHEN** a test step is executed inline in Java containing `_include: <path>` and a test data YAML file exists for the test case
- **THEN** the parser resolves the `<path>` correctly relative to the test execution context without erroneously appending it to the test data file's path
