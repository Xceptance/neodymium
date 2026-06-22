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
To prevent human typos in framework directives, the parser SHALL validate all keys starting with a leading underscore (Machine Space). If an underscored key is not in the registered safety keys (`_meta`, `_properties`, `_context`, `_data`, `_beforeAll`, `_beforeEach`, `_steps`, `_onSuccessEach`, `_onFailureEach`, `_afterEach`, `_onSuccessAll`, `_onFailureAll`, `_afterAll`, `_include`, `_id`, `_testId`, `_sensitive`, `_dynamic`), the parser SHALL throw a `MalformedPlaybookException`.

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
- **Classpath Resolution Bypass:** When discovering playbooks in the classpath, if the resource URL uses the `file:` protocol, the parser SHALL bypass copying the file to `/tmp` and retrieve the canonical `File` directly from the URL. If the resource protocol is `jar:file:`, relative inclusions SHALL be resolved relative to the package folder using the class loader context to prevent breaking relative inclusion directories.

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

### Requirement: Sensitive / Post-Data Protection (Dual-Context Guard with nested `_sensitive` Maps)
To prevent accidental leakage of sensitive credentials or PII (Personally Identifiable Information) to external LLM services or terminal logs while preserving data format realism for the AI:
- The framework SHALL support designating sensitive variable keys via an explicit in-place **`_sensitive`** map block inside dataset constants (`_constants`) or dataset maps (`_data` rows).
- Defining a key under the `_sensitive` block SHALL implicitly register it as sensitive, eliminating any requirement for a separate global registry.
- Each sensitive entry SHALL contain a **`_value`** (for raw local execution) and a **`_mock`** (for sanitized LLM prompting).
- If `_mock` is missing, the framework SHALL automatically fabricate a realistic anonymized value based on the key name format matching the following rules:
  - Keys matching `/card|credit/i` SHALL produce a realistic fake credit card value (`4000123456789010`).
  - Keys matching `/cvv|cvc/i` SHALL produce a realistic CVV value (`999`).
  - Keys matching `/pass|pwd|secret/i` SHALL produce a format-realistic dummy password (`mockPassword_123`).
  - Any other key matching sensitive pattern SHALL fallback to a format of `mock_[key]`.
- The engine SHALL construct two parallel context maps during resolution:
  - **Native Automation Context Map (Raw):** Contains `_value` entries, accessible to the local WebDriver browser automation runtime.
  - **Guarded AI Context Map (Sanitized):** Contains `_mock` / fabricated fakes, sent to LLM prompts and written to framework logs.

#### Scenario: Sensitive data masking via nested `_sensitive` map
- **WHEN** a dataset defines a sensitive password under `_sensitive.password` with `_value: "Secret123!"` and `_mock: "mockPassword_abc"`
- **THEN** a step `Log in with ${password}` compiles to `Log in with Secret123!` for execution, but compiles strictly to `Log in with mockPassword_abc` for LLM contexts and framework logging.

### Requirement: Dynamic Data Protection (`_dynamic` — Playbook Recording Sanitization)
To prevent non-deterministic playbook replays caused by resolved dynamic values (e.g. random email suffixes, programmatic port numbers) being baked into recorded action JSON:
- The framework SHALL support designating dynamic variable keys via an explicit in-place **`_dynamic`** list inside dataset constants (`_constants`), dataset maps (`_data` rows), or per-dimension `_lookup` scope entries.
- The `_dynamic` list SHALL contain the **variable key names** (not values) whose resolved runtime values must be replaced with their original `${placeholder}` form during playbook recording.
- There SHALL be **no auto-detection** of dynamic variables defined inside static YAML data blocks. Every such dynamic variable must be explicitly listed in a `_dynamic` block.
- All `_sensitive` variables SHALL be **implicitly dynamic**. Listing a sensitive key in `_dynamic` is redundant but harmless.
- All runtime-captured variables created by `store` steps (e.g. storing text into a variable name) SHALL be **implicitly dynamic** and do not require YAML declaration.
- All programmatically injected variables classified as `DYNAMIC` via the Java API SHALL be **implicitly dynamic** and do not require YAML declaration.
- During playbook recording sanitization, the recorder SHALL:
  1. Build a lookup table mapping each dynamic variable's resolved runtime value to its `${key}` placeholder string.
  2. Sort entries by resolved value length (descending) to prevent partial substring corruption.
  3. Scan and replace occurrences of resolved dynamic values in the action's `value` field with the corresponding `${placeholder}`. The `target` and `hint` fields SHALL be sanitized only when the resolved value appears as a complete segment (not as a substring of a CSS selector token or XPath expression).
- During playbook replay, the execution engine SHALL re-resolve `${placeholder}` tokens inside persisted action fields against the **current active dataset row** to produce fresh runtime values.


#### Scenario: Dynamic email sanitization during playbook recording
- **GIVEN** a dataset defines `email: john.doe.${random}@example.com` and `_dynamic: [email]`
- **WHEN** the initial generation run resolves email to `john.doe.8f3a@example.com` and the AI agent records a TYPE action with value `john.doe.8f3a@example.com`
- **THEN** the playbook recorder sanitizes the value field to `${email}` before persisting the action JSON

#### Scenario: Dynamic variable re-resolution on replay
- **GIVEN** a persisted playbook action contains `value: "${email}"`
- **WHEN** the playbook is replayed with a fresh dataset where `${random}` resolves to `b2c9`
- **THEN** the action's value is resolved at replay time to `john.doe.b2c9@example.com`

#### Scenario: Unlisted dynamic variable is NOT sanitized
- **GIVEN** a dataset defines `email: john.doe.${random}@example.com` but does NOT list `email` in `_dynamic`
- **WHEN** the playbook recorder persists a TYPE action with value `john.doe.8f3a@example.com`
- **THEN** the recorder persists the resolved value as-is (no sanitization occurs)

#### Scenario: Sensitive variables are implicitly dynamic
- **GIVEN** a dataset defines `password` under `_sensitive` with `_value: "Secret123!"` and does NOT list `password` in `_dynamic`
- **WHEN** the playbook recorder persists a TYPE action with value `Secret123!`
- **THEN** the recorder sanitizes the value field to `${password}` before persisting

#### Scenario: Stored variables are implicitly dynamic
- **GIVEN** a playbook executes a step `Store text from "#ord-id" as "orderId"` and gets the runtime value "ORD-999"
- **WHEN** a subsequent step type action uses the value "ORD-999" during the same run
- **THEN** the recorder sanitizes that value to `${orderId}` in the persisted playbook action

### Requirement: Java Programmatic Variable Injection & Retrieval API
To support runtime-injected values (such as dynamically provisioned URLs or test user credentials generated before test runs) without requiring YAML playbook modifications:
- The framework SHALL support injecting and retrieving variables programmatically in Java context setup methods (e.g. `@BeforeEach`).
- The programmatic API SHALL support separate scopes corresponding to variable classifications:
  - `constants()`: For global constants.
  - `normal()`: For standard iteration-scoped variables.
  - `dynamic()`: For variables marked as dynamic (values replaced with `${placeholder}` on recording).
  - `sensitive()`: For variables containing sensitive values and mock representations.
- Programmatically injected variables SHALL be fully functional and registered automatically without requiring any entry inside the playbook's YAML data/dynamic blocks.
- Programmatically injected variables SHALL be retrievable using symmetric `.get()` calls matching the classification scope.
- The root programmatic API `Neodymium.data()` SHALL expose unified `get(key)` and `exists(key)` methods that resolve the currently active, valid value by traversing the context hierarchy in the following order:
  1. Programmatic Java Scopes (`sensitive()` > `dynamic()` > `normal()` > `constants()`).
  2. Active dataset row context (dimension-specific lookups > dataset constants).
  3. Static playbook defaults (`_meta`).
  4. External environment configuration (`_properties` > system properties).

#### Scenario: Programmatic dynamic variable injection
- **GIVEN** JUnit setup runs `Neodymium.data().dynamic().set("baseUrl", "http://test-env:8080")`
- **WHEN** a recorded playbook action targets "http://test-env:8080"
- **THEN** the recorder automatically replaces the target with `${baseUrl}` in the action JSON
- **AND** a call to `Neodymium.data().dynamic().get("baseUrl")` returns the runtime value "http://test-env:8080"

#### Scenario: Hierarchy resolution precedence
- **GIVEN** a dataset defines `username: "yamlUser"`
- **AND** JUnit setup programmatically executes `Neodymium.data().normal().set("username", "javaUser")`
- **WHEN** `Neodymium.data().get("username")` is called during test execution
- **THEN** it resolves to `"javaUser"` (programmatic scope overrides dataset scope)
- **AND** `Neodymium.data().exists("username")` returns `true`

### Requirement: Layered / Test Profile Data Structure
To cleanly separate common constants, logical datasets, and lookup parameters:
- The framework's `_data` block SHALL support a layered structural format (as a Map) in addition to the legacy flat dataset list format.
- The layered format SHALL support three distinct sub-keys:
  - **`_constants`:** Common key-value variables merged into every generated iteration.
  - **`_dimensions`:** Orthogonal parameter axes where each key defines a dimension and its value is a list of targets.
  - **`_lookup`:** A dictionary providing override parameters per dimension value (e.g., locale-specific expected prices or localized text).
- The parser SHALL programmatically explode the Cartesian product of active logical profiles and dimension targets to produce the final flat dataset rows.
- The parser SHALL drop manual `testId` / `dataId` requirements for Cartesian profile execution, automatically generating unique iteration names using Cartesian coordinate dimensions (e.g. `[locale=en-US]`).

#### Scenario: Cartesian explosion of dimensions and automated run naming
- **WHEN** a playbook defines 4 active locales in `_dimensions` and 2 viewports in environment targets
- **THEN** the parser explodes the Cartesian product to generate exactly 8 test execution iterations, using automated coordinate-derived names without requiring manual dataset-level `testId`/`dataId` keys


### Requirement: Execution Environment Multiplier and Precedence
To ensure that environment parameters (browsers, viewports, target platforms) can be dynamically multiplied and overridden from the outside without recompiling tests, the runner SHALL resolve target environments using a strict priority hierarchy:
1. **Runner Context (CLI Override):** Highest priority. System properties passed directly via command-line flags (e.g., `-Dbrowser=Chrome,Firefox`).
2. **System Defaults (`neodymium.properties`):** Baseline project-level environment profiles.
3. **Suite / Test Run Descriptor (Local suggestions):** Lowest priority. Local default target recommendations parsed from the playbook file or test suite files.

#### Scenario: CLI override precedence
- **WHEN** a test run descriptor recommends Firefox, but the operator runs `mvn test -Dbrowser=Chrome`
- **THEN** the runner context takes precedence and executes the tests exclusively on Chrome
