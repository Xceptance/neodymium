## ADDED Requirements

### Requirement: Global Playbook Replay Control
The Neodymium framework SHALL support a global configuration property `neodymium.ai.playbook.replay` to enable or disable the loading and playback of playbooks across the entire test execution setup. By default, this property SHALL be `true`. When set to `false`, Neodymium SHALL bypass loading playbooks entirely, forcing live AI/LLM generation sessions instead.

#### Scenario: Global replay enabled by default
- **WHEN** `neodymium.ai.playbook.replay` is not specified, or is explicitly set to `true`
- **THEN** Neodymium attempts to load and playback recorded playbooks if they exist on disk

#### Scenario: Global replay disabled entirely
- **WHEN** `neodymium.ai.playbook.replay` is set to `false`
- **THEN** Neodymium bypasses loading playbooks entirely and executes live AI/LLM generation sessions, even if playbook files exist on disk

### Requirement: Dataset-Specific Playbook Replay Control
The Neodymium framework SHALL support bypassing playbook replay for individual test cases or dataset iterations. Replay SHALL be bypassed for a dataset iteration if either `skipReplay` or `neodymium.ai.skipReplay` is resolved as `true` in the dataset's active properties. If the global `neodymium.ai.playbook.replay` is configured to `false`, playback SHALL be bypassed for all datasets, regardless of any dataset-level settings.

#### Scenario: Dataset-specific replay bypass
- **WHEN** `neodymium.ai.playbook.replay` is `true` but the dataset's properties contain `skipReplay` or `neodymium.ai.skipReplay` set to `true`
- **THEN** replay is bypassed for that specific dataset iteration

#### Scenario: Global replay disabled but dataset has it enabled
- **WHEN** `neodymium.ai.playbook.replay` is `false` but the dataset's properties contain `skipReplay` or `neodymium.ai.skipReplay` set to `false`
- **THEN** replay is still bypassed, as the global configuration disable takes precedence over dataset settings
