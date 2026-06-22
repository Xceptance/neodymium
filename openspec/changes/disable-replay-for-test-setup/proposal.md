## Why

Neodymium automatically loads recorded playbooks for playback if a matching JSON baseline exists on disk. While this supports fast, deterministic, offline test replay, it complicates testing Neodymium itself (such as AI action generator development, visual audits, or dHash visual defects) and makes it difficult to use live LLMs for more complex, dynamic, or non-deterministic user applications. Developers currently have to manually delete files, or hack dataset properties to bypass replay. A clean, standardized way is needed to disable playbook playback globally for a test run or selectively per dataset/test case.

## What Changes

- **Global Replay Bypass Configuration**: Introduce a new global AI property `neodymium.ai.playbook.replay` (boolean, defaulting to `true`). If configured to `false`, playbook replay is bypassed entirely across all tests.
- **Selective Dataset Replay Control**: Ensure that both legacy `skipReplay` and modern `neodymium.ai.skipReplay` dataset/playbook properties are consistently evaluated. A dataset-level bypass of replay takes precedence over the global default.
- **Flexible Testing and LLM Execution**: Forcing a live AI execution instead of replay allows direct end-to-end AI testing and LLM execution in challenging environments.

## Capabilities

### New Capabilities
- `playbook-replay-control`: Provides global and granular configuration settings to completely disable playbook replay, enabling live LLM execution on demand.

### Modified Capabilities
<!-- None -->

## Impact

- `com.xceptance.neodymium.ai.config.AiConfiguration`: Add property `neodymium.ai.playbook.replay`.
- `com.xceptance.neodymium.util.Neodymium`: Update `initializePlaybook()` to check the global configuration as well as the dataset-specific skipReplay/neodymium.ai.skipReplay parameters.
- No breaking changes are introduced; default behavior remains unchanged.
