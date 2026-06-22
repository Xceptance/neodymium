## Context

The Neodymium AI-native testing framework uses recorded playbooks (JSON files) to replay test interactions without making external live LLM calls, providing deterministic, fast, offline validation. However, developers testing the AI components of Neodymium itself (e.g., visual defect detection, linter audits, dHash baselines) or writing advanced tests in highly dynamic environments need a way to completely bypass replay.

The current implementation only supports dataset-specific `skipReplay` via localized YAML data structures, with no global configuration override. This proposal defines a cohesive mechanism for both global and dataset-level control over the playbook replay lifecycle.

## Goals / Non-Goals

**Goals:**
- Provide a global configuration property `neodymium.ai.playbook.replay` that can disable all playbook replays across the test execution setup.
- Standardize the priority hierarchy of replay controls: Global disabled takes absolute precedence, followed by dataset-specific bypasses (`neodymium.ai.skipReplay` or `skipReplay`), followed by the global default (replay enabled).
- Keep the default behavior fully backwards-compatible (playbooks are replayed if found).

**Non-Goals:**
- Removing playbook recording controls (`neodymium.ai.playbook.record`).
- Deleting generated playbook JSON baseline files automatically when replay is bypassed.

## Decisions

### Decision: Addition of `neodymium.ai.playbook.replay` to `AiConfiguration`
We will add `playbookReplayEnabled()` to `AiConfiguration.java`, bound to the property `neodymium.ai.playbook.replay` with a default value of `true`.
* **Rationale**: This integrates naturally with the existing Aeonbits Owner-based configuration architecture in Neodymium, making it easy to configure via `config/ai.properties` or command line system properties (e.g. `-Dneodymium.ai.playbook.replay=false`).

### Decision: Evaluation Order in `Neodymium.initializePlaybook()`
In `Neodymium.initializePlaybook()`, we will compute `skipReplay` as follows:
```java
final boolean skipReplay = !Neodymium.aiConfiguration().playbookReplayEnabled()
    || (Neodymium.getData().exists("skipReplay") && Neodymium.getData().asBoolean("skipReplay", false))
    || (Neodymium.getData().exists("neodymium.ai.skipReplay") && Neodymium.getData().asBoolean("neodymium.ai.skipReplay", false));
```
* **Rationale**: This enforces a clean priority chain:
  1. If `playbookReplayEnabled()` is false, `skipReplay` becomes true (replay is bypassed).
  2. If global is true, dataset properties are checked (both legacy flat `skipReplay` and modern `neodymium.ai.skipReplay`).
  3. If none are true, replay is performed (default behavior).

## Risks / Trade-offs

- **[Risk] High LLM Token Usage / Slow execution** -> If a suite-wide test is run with `neodymium.ai.playbook.replay=false` by mistake, it will trigger live AI generation sessions for all tests, consuming large amounts of tokens and taking more time.
  - *Mitigation*: Set the default value to `true` to ensure playbooks are used unless explicitly requested otherwise. Provide clear logging or metrics when live sessions are run.
