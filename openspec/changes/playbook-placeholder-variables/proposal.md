## Why

Currently, recorded playbook JSON files store fully resolved values for action targets, navigation URLs, and inputs (such as emails, names, and passwords). This prevents playbooks from being replayed on different environments, ports, locales, or with dynamic/parameterized test datasets (e.g. unique user credentials or randomized input values). 

## What Changes

- **Dynamic Interpolation in Replay**: Introduce support for resolving and interpolating `${variable}` placeholders inside playbook JSON steps, prompts, and action attributes (e.g., target, value, basic auth credentials, expected error messages, or descriptions) during playbook replay.
- **Variable Propagation from Test Data**: Placeholders will be resolved dynamically at replay time using `Neodymium.getData()` (JUnit test data) and `Neodymium.configuration()` properties, leveraging the existing `AiBrowser.resolveTestDataToPrompt` utility.
- **Support for Interactive HUD**: Ensure the interactive HUD properly handles and displays both unresolved placeholder-based steps and their resolved runtime values, preserving user edits correctly.
- **Backward Compatibility**: Ensure that existing playbooks without placeholders continue to work perfectly as-is.

## Capabilities

### New Capabilities
- `playbook-placeholder-variables`: Covers the capability to write, store, and execute playbooks containing dynamic `${variable}` placeholders, resolving them against the active test dataset and system configurations at replay time.

### Modified Capabilities
<!-- None -->

## Impact

- `com.xceptance.neodymium.ai.playbook.Playbook`: Used for step actions representation.
- `com.xceptance.neodymium.ai.core.AiAgent`: The replay execution flow where step actions are loaded and processed.
- `com.xceptance.neodymium.ai.action.Action`: Playbook step action definitions (handling value and target interpolation).
- `com.xceptance.neodymium.ai.generator.InteractiveHud`: Needs to display resolved text in the user interface.
