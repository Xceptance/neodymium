## Why

YAML files/playbooks in Neodymium often share common steps or lifecycle logic. Currently, there is no way to include files or fragments, leading to step duplication and maintenance overhead. Integrating support for includes in YAML files allows test writers to modularize playbooks and dynamically branch execution using inclusions within conditional steps.

## What Changes

- Add support for a unified `_include:` directive in YAML test scripts and datasets.
- Support static, dynamic, and data-level inclusions.
- Implement interleaved variable and include resolution with a circular dependency guard.
- Bypass copying files to `/tmp` for `file:` protocol resources.
- Support trace logs/location tracing for steps loaded from included files.

## Capabilities

### New Capabilities
- `includes-resolution`: Define the syntax, parsing pipeline, scope resolution rules, and error handling for static and dynamic inclusions (`_include:`) within YAML test scripts and datasets.

### Modified Capabilities
None.

## Impact

Modifies `YamlFileReader`, `TestDataUtils`, `AiAgent`, `BranchAction`, and registers `IncludeAction`. Added new tests to cover includes.
