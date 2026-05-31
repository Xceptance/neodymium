# fresh-approach-to-test-scripts-and-test-documentation

## ADDED Requirements

### Requirement: List-Based Steps & Standard Playbook Lifecycle
The framework SHALL support defining test steps as a standard YAML list under the `_steps` key.
To make playbooks highly organized, the framework SHALL support the standard modern execution lifecycle blocks:
- **`_beforeAll` (Static Scope):** Setup steps executed once before any tests start.
- **`_beforeEach` (Iteration Scope):** Setup steps executed before each dataset iteration.
- **`_steps` (Iteration Scope):** The main execution sequence.
- **`_onSuccessEach` (Iteration Scope):** Executed right after `_steps` of an iteration succeeds, before `_afterEach`.
- **`_onFailureEach` (Iteration Scope):** Executed right after `_steps` of an iteration fails, before `_afterEach`.
- **`_afterEach` (Iteration Scope):** Teardown steps executed after each dataset iteration (always runs, like a `finally` block).
- **`_onSuccessAll` (Static Scope):** Executed once after all dataset iterations succeed, before `_afterAll`.
- **`_onFailureAll` (Static Scope):** Executed once if any part of the playbook fails, before `_afterAll`.
- **`_afterAll` (Static Scope):** Teardown steps executed once after all dataset iterations complete (always runs, like a `finally` block).

To allow free-text natural comments, the parser SHALL tolerate and ignore any unrecognized keys at the root level or dataset maps that do not start with a leading underscore (Human Space).
To prevent human typos in framework directives, the parser SHALL validate all keys starting with a leading underscore (Machine Space). If an underscored key is not in the registered keys (`_meta`, `_properties`, `_context`, `_data`, `_beforeAll`, `_beforeEach`, `_steps`, `_onSuccessEach`, `_onFailureEach`, `_afterEach`, `_onSuccessAll`, `_onFailureAll`, `_afterAll`, `_include`, `_id`), the parser SHALL throw a `MalformedPlaybookException`.

#### Scenario: Valid lifecycle blocks and comments
- **WHEN** the playbook contains modernized lifecycle lists and arbitrary un-underscored keys
- **THEN** the parser successfully extracts the steps and ignores the un-underscored keys

#### Scenario: Typo in framework key
- **WHEN** the playbook contains an invalid underscored key (e.g. `_beforeeach:`)
- **THEN** the parser throws a `MalformedPlaybookException` indicating the unrecognized key

### Requirement: Unified AI Config Properties & Hierarchy Flattening
The framework SHALL support nested property structures under `_properties` and recursively flatten them into standard dotted keys. Dotted properties SHALL take precedence over their equivalent nested map representations if both are defined.
To ensure naming consistency across AI configuration properties:
- The legacy raw `skipReplay` key SHALL be strictly replaced by `neodymium.ai.skipReplay` (via nested `neodymium.ai.skipReplay` or flat dotted key in the `_properties` block). No backwards compatibility fallback is required.

#### Scenario: Precedence of flat dotted key
- **WHEN** a configuration property is defined as both a nested structure and a dotted flat key
- **THEN** the dotted key's value overrides the nested map representation

### Requirement: Unified Inclusions, Reflection Scopes, and Namespace Resolution
The framework SHALL support a unified `_include:` directive to dynamically load and splice external steps or datasets (supporting full-block, inline-list, and map-level overrides).
The parser SHALL resolve variables and inclusions in an interleaved loop (max `MAX_DEPTH = 10` iterations) in a strictly isolated scope per dataset iteration:
- **Static Scopes (`_beforeAll`, `_afterAll`):** Placeholders SHALL resolve once using static `_meta` variables.
- **Iteration Scopes (`_properties`, `_context`, `_beforeEach`, `_steps`, `_afterEach`):** Placeholders SHALL resolve per dataset row using active variables overlaid on `_meta` defaults.
- **Namespace Resolution:** Dotted prefixes SHALL be supported to explicitly access metadata fields (e.g., `${_meta._author}`) to prevent variable collisions.

If a circular inclusion chain is detected, it SHALL throw a `MalformedPlaybookException` displaying the exact circular path.

#### Scenario: Metadata namespace resolution
- **WHEN** a step references a metadata field via the explicit namespace (e.g. `${_meta._author}`)
- **THEN** the parser resolves the placeholder correctly using the _meta block field value

#### Scenario: Circular inclusion loop
- **WHEN** included files form a circular inclusion chain (e.g. `A.yaml -> B.yaml -> A.yaml`)
- **THEN** the parser throws a `MalformedPlaybookException` detailing the circular path

### Requirement: Step Location Tracing
The framework SHALL construct and serialize a location trace string path for every expanded step across all static and iteration lifecycle blocks, stored as a JSON array under the internal key `_steps` in the dataset map, enabling `AiAgent` to display precise stack traces on step execution failure.

#### Scenario: Precise failure trace
- **WHEN** a step expanded from an included file fails
- **THEN** the `AiAgent` displays the exact step location trace path in failure logs

### Requirement: Sensitive / Post-Data Protection (Dual-Context Guard with Stand-in Data)
To prevent accidental leakage of sensitive credentials or PII (Personally Identifiable Information) to external LLM services or terminal logs while preserving data format realism for the AI:
- The framework SHALL support designating sensitive variable keys via an explicit `_sensitive` list inside the `_meta` block.
- The execution engine SHALL construct two parallel variable maps during resolution:
  - **Native Automation Context Map (Raw):** Contains raw values accessible to the local WebDriver browser automation runtime.
  - **Guarded AI Context Map (Sanitized):** Contains all keys from the native map, but with all registered sensitive keys replaced with realistic **Stand-in Data** *at the source* before placeholder interpolation occurs.
- The Guarded AI Context Map SHALL resolve stand-in data according to the following precedence:
  1. **User-Defined Stand-in:** An explicit alternative value defined via a corresponding `_mock` or `_standin` suffix variable (e.g. `password_mock`).
  2. **Framework-Fabricated Stand-in:** A realistic anonymized value automatically fabricated by the framework based on the key name format (e.g., generating valid test card numbers for credit cards or `mockPassword_123` for passwords).
- The engine SHALL run **Dual Resolution Paths** during Phase B: the Execution Path resolves placeholders using the Native Map, while the LLM/AI Path resolves placeholders using the Guarded Map containing the stand-in values.

#### Scenario: Sensitive data masking via Dual-Context Guard with Stand-in
- **WHEN** a dataset row contains `password: "Secret123!"` and `password_mock: "mockPassword_abc"` registered under `_meta._sensitive`
- **THEN** a step `Log in with ${password}` compiles to `Log in with Secret123!` for execution, but compiles strictly to `Log in with mockPassword_abc` for LLM contexts and framework logging.

### Requirement: Layered / Test Profile Data Structure
To cleanly separate common constants, logical datasets, and lookup parameters:
- The framework's `_data` block SHALL support a layered structural format (as a Map) in addition to the legacy flat dataset list format.
- The layered format SHALL support three distinct sub-keys:
  - **`_constants`:** Common key-value variables merged into every generated iteration.
  - **`_dimensions`:** Orthogonal parameter axes where each key defines a dimension and its value is a list of targets.
  - **`_lookup`:** A dictionary providing override parameters per dimension value (e.g., locale-specific expected prices or localized text).
- The parser SHALL programmatically explode the Cartesian product of active logical profiles and dimension targets to produce the final flat dataset rows.

#### Scenario: Cartesian explosion of dimensions
- **WHEN** a playbook defines 4 active locales in `_dimensions` and 2 viewports in environment targets
- **THEN** the parser explodes the Cartesian product to generate exactly 8 test execution iterations

### Requirement: Execution Environment Multiplier and Precedence
To ensure that environment parameters (browsers, viewports, target platforms) can be dynamically multiplied and overridden from the outside without recompiling tests, the runner SHALL resolve target environments using a strict priority hierarchy:
1. **Runner Context (CLI Override):** Highest priority. System properties passed directly via command-line flags (e.g., `-Dbrowser=Chrome,Firefox`).
2. **System Defaults (`neodymium.properties`):** Baseline project-level environment profiles.
3. **Suite / Test Run Descriptor (Local suggestions):** Lowest priority. Local default target recommendations parsed from the playbook file or test suite files.

#### Scenario: CLI override precedence
- **WHEN** a test run descriptor recommends Firefox, but the operator runs `mvn test -Dbrowser=Chrome`
- **THEN** the runner context takes precedence and executes the tests exclusively on Chrome
