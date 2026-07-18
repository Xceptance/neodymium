# Neodymium AI v2 Redesign - Features & Documentation

The redesigned v2 Neodymium AI framework (contained in `org.neodymium.ai.*`) delivers advanced agentic test execution, featuring structured playbook companion recordings, pre-step analysis, soft failure tolerance, and deterministic replay capabilities.

---

## 1. Execution Playbooks & Replay Cache
Instead of executing LLM calls dynamically on every run, the v2 framework uses **Structured Playbooks**:
* **YAML Playbook**: Contains natural language steps (the test scenario definition).
* **JSON Companion**: A recording compiled during the initial `FORCE_RECORDING` run. It maps each natural language step to a list of concrete structured SUT actions (e.g., `NAVIGATE`, `CLICK`, `TYPE`, `ASSERT`) along with visual `screenshotHash` baselines.
* **Offline Replay**: Subsequent test runs (`REPLAY_STRICT` or `REPLAY_WITH_HEALING`) load the companion JSON file directly, executing recorded browser interactions in milliseconds without making any LLM calls.

---

## 2. Explicit Control Tags

Control tags can be appended to natural language steps inside the YAML playbook to customize execution behavior at runtime. All tags are case-insensitive and whitespace-tolerant.

### `(no-replay)`
Forces live execution and completely bypasses the replay cache for a specific step.
* **Behavior**: Bypasses the recorded actions in the companion JSON file and forces a live LLM extraction call (plus live outcome verification) for that step, even during a replay run.
* **Recording**: The fresh actions executed live are updated in the in-memory playbook and stats.
* **LLM Payload Cleanliness**: Stripped upfront at parsing/model instantiation time, ensuring the prompt payload compiled for the LLM remains clean.
* **Syntax Examples**: `(no-replay)`, `(NO-REPLAY)`, `( no-replay )`

### `(optional)` / `(soft)`
Allows failures on steps to be tolerated and logged as warnings instead of failing the test case.
* **Assertion Failures**: If an assertion action (like `ASSERT`) fails, the failure is caught, logged, and reported at the end under warnings. The test continues.
* **Interaction Failures**: If element interaction (like locating or clicking a button) fails, the runner tries all live escalations first. If it still fails, the step is bypassed, the failure is logged as a warning, and execution continues to the next playbook step.
* **No Replay Healing**: During replay of an optional step, the runner does not attempt semantic healing; it immediately logs the failure as a warning and continues.
* **Syntax Examples**: `(optional)`, `(soft)`, `( OPTIONAL )`, `( Soft )`

---

## 3. Runtime Instruction Preparation
Before compiling prompts or sending request payloads to the LLM, the framework runs a dedicated instruction preparation helper. It dynamically strips all explicit control tags case-insensitively, including:
* `(no-replay)`
* `(bug)` / `(bug: ...)`
* `(optional)` / `(soft)`
* `(timeout: ...)`

This prevents internal execution instructions from polluting the natural language prompts sent to the LLM.

---

## 4. Pre-Step Split Analysis (PESAP)
To handle complex or compound steps, the pipeline executes a Pre-Step Split Analysis.
* **Behavior**: Before execution, a lightweight LLM query checks if a step represents multiple separate actions (e.g., `"Enter username and password, then click login"`).
* **Splitting**: If compound, it splits the instruction into distinct sub-steps (leaf steps) and pushes them onto the execution stack sequentially.
* **Bypassing on Replay**: PESAP is only executed in live recording mode; replay runs execute the pre-split leaf steps directly from the companion JSON.

---

## 5. Dynamic Variable Parameterization
Ensures recorded playbooks remain reusable across environment and data changes:
* **Resolution**: Resolves variables (e.g. `${username}`) at runtime before executing actions.
* **Masking & Sanitization**: Compares SUT actions against sensitive dataset keys (e.g., passwords or tokens) and dynamically masks/sanitizes them before recording.
* **Parameterization**: Automatically matches generated dynamic values (such as order numbers or generated URLs) back to their variable definitions, writing parameterized entries like `"${order.number}"` into the companion JSON instead of hardcoded session values.
