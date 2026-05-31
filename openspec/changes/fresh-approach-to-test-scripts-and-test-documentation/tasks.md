# Task List: List-Based YAML Steps & Unified Inclusions

A granular, component-level checklist to implement and verify list-based YAML steps, unified `_include:` directives, and step location tracing.

---

## Component: `YamlFileReader`

- [ ] **Task 1: Root Key Extraction, Schema Tolerance, & Framework Key Guard**
  *   Extract `_meta`, `_properties`, `_context`, `_data`, `_beforeAll`, `_beforeEach`, `_steps`, `_onSuccessEach`, `_onFailureEach`, `_afterEach`, `_onSuccessAll`, `_onFailureAll`, and `_afterAll`. Support fallback to legacy `data`, `steps`, `before`, and `after` if required.
  *   Tolerate and ignore all top-level or dataset keys that do not start with `_` (Human Space).
  *   Implement **Framework Key Guard**: strictly validate all underscored keys at root and dataset-map levels against the registry (`_meta`, `_properties`, `_context`, `_data`, `_beforeAll`, `_beforeEach`, `_steps`, `_onSuccessEach`, `_onFailureEach`, `_afterEach`, `_onSuccessAll`, `_onFailureAll`, `_afterAll`, `_include`, `_id`). Throw `MalformedPlaybookException` if an unknown key starting with `_` is found (e.g. `_step:` or `_dta:`).
  *   Detect whether `_beforeAll`, `_beforeEach`, `_steps`, `_onSuccessEach`, `_onFailureEach`, `_afterEach`, `_onSuccessAll`, `_onFailureAll`, and `_afterAll` are `String` (block scalar, legacy) or `List` (new format) values.
  *   For `List` format, iterate entries: `String` entries are step instructions, `Map` entries with an `_include` key are inclusion directives.

- [ ] **Task 2: `_properties` YAML Flattening & Precedence**
  *   Implement recursive flattening for nested properties (e.g. `{ neodymium: { ai: { pesap: { enabled: false } } } }` -> `neodymium.ai.pesap.enabled = false`).
  *   Ensure that `skipReplay` is strictly parsed and stored as `neodymium.ai.skipReplay`.
  *   Inject flattened properties into the runtime dataset map.
  *   Implement **Property Precedence**: ensure dotted flat keys take precedence and override nested equivalents if both are defined.
  *   Verify that the execution engine reads only `neodymium.ai.skipReplay` for checking playback bypass.

- [ ] **Task 3: `_include:` Step Expansion**
  *   For each `{_include: <path>}` entry in the steps list, resolve the path relative to the parent file's directory.
  *   Read the target file as raw text and expand its lines in-place into the steps list.
  *   Reconstruct the final steps string from all expanded entries.

- [ ] **Task 4: `_include:` Data Resolve & Merge**
  *   Implement root-level block inclusion: If the root `data` is a Map with `_include: <path>`, load and parse the file as a YAML document, unwrapping a nested `data:` or `_data:` list if present, and substituting the whole block.
  *   Implement list-level inline inclusion: If an element in the `data` list is a Map containing ONLY `_include: <path>`, load and parse the file as a YAML document, unwrapping a nested `data:` or `_data:` list if present, and splice the list entries in-place.
  *   Implement map-level override merge: If a dataset map contains an `_include: <path>` alongside other override keys (like `_testId` or username), load and parse the external Map and merge its entries into the dataset map without overwriting local keys. Remove the `_include` key.

- [ ] **Task 5: Filesystem Base Directory Resolution**
  *   Ensure `readFile(File file)` captures and passes the absolute base directory.
  *   Extend classpath URL parsing to check for `file:` protocol, converting classpath resource URLs to canonical filesystem `File`s.

- [ ] **Task 6: Interleaved Variable & Inclusion Loop (Scope Isolated)**
  *   Implement the interleaved loop (max `MAX_DEPTH = 10` iterations) in a **strictly isolated scope** per iteration:
    *   Pass 1: Resolve `${var}` placeholders across the document according to the two lifecycle scopes:
      *   **Static Scope (`_beforeAll`, `_onSuccessAll`, `_onFailureAll`, `_afterAll`):** Resolved once per playbook using static `_meta` variables.
      *   **Iteration Scope (`_properties`, `_context`, `_beforeEach`, `_steps`, `_onSuccessEach`, `_onFailureEach`, `_afterEach`):** Resolved per dataset row using active variables overlaid on `_meta` defaults (supporting explicit `_meta` namespace references).
    *   Pass 2: Expand `_include:` markers using resolved paths.
  *   Final cleanup pass: Resolve any remaining variables brought in by included files (e.g., inside step files).
  *   Guard: Throw `MalformedPlaybookException` with the exact circular inclusion path if markers remain after max iterations.

- [ ] **Task 7: Step Location Tracing**
  *   Generate a full inclusion stack trace path for each expanded step:
    *   `"MyTest.yaml:7"` — Direct step.
    *   `"fragments/login.steps:2 -> MyTest.yaml:9"` — Included step.
  *   Serialize the list of trace strings as a JSON array and inject it internally under the `_steps` key in the final dataset map.

---

## Component: `AiAgent` & Security

- [ ] **Task 8: Step Location Reporting**
  *   Update `AiAgent.execute` to read the `_steps` JSON array from the dataset map.
  *   In runtime step logs, print the exact trace string.
  *   In step failure messages, output the exact location trace string inside `formatFailureLogContext`.

- [ ] **Task 9: Sensitive Data Scrubbing (`_sensitive`)**
  *   Implement metadata key registration: parse `_meta._sensitive` inside `YamlFileReader` and populate a thread-local active sensitive registry.
  *   Update `AiAgent` dynamic prompting and framework logs: intercept variable values before LLM transmission and replace registered sensitive keys with `[REDACTED]`.
  *   Update core terminal loggers to intercept and mask sensitive values with `********`.
  *   Ensure local WebDriver runtime retains fully functional raw access to the sensitive values.

---

## Component: Layered Datasets & Multipliers

- [ ] **Task 10: Layered Dataset Resolution (`_constants`, `_dimensions`, `_lookup`)**
  *   Detect when `_data` is a `Map` structure.
  *   Parse `_constants`, `_dimensions`, and `_lookup` blocks.
  *   Compute the Cartesian product of the active dimensions.
  *   Resolve override lookups per dimension target and merge with constants to explode the dynamic dataset rows.

- [ ] **Task 11: Execution Environment Precedence Engine**
  *   Extend `NeodymiumRunner` to resolve target environments dynamically.
  *   Implement priority hierarchy: CLI System Properties (`mvn -D...`) > `neodymium.properties` defaults > Local test case environment recommendations.
  *   Apply Cartesian multiplication of active environment targets and active logical test profiles at execution time.

---

## Component: Testing & Verification

- [ ] **Task 12: Unit and Integration Tests**
  *   Create a complete test suite verifying:
    *   Underscore prefixes (`_steps`, `_data`, `_meta`, `_context`, `_properties`).
    *   **Framework Key Guard**: check that unrecognized keys prefixed with an underscore throw `MalformedPlaybookException` at root or inside dataset.
    *   **Conditional Hooks**: check execution of `_onSuccess`, `_onFailure`, `_onSuccessAll`, `_onFailureAll` and their aliases.
    *   **Property Precedence**: check that dotted flat keys override nested map configurations in `_properties`.
    *   Hierarchical property flattening in `_properties`.
    *   Tolerating and ignoring unknown, extra, or free-text keys *without* an underscore at all levels.
    *   `_include:` step expansion with filesystem-relative resolution.
    *   `_include:` data resolution across root-level, inline list-level, and map-level with overrides.
    *   Interleaved variable/inclusion loop with parameterized paths.
    *   **Scope Isolation**: verify that variables resolved in one dataset iteration do not pollute other iterations.
    *   `MAX_DEPTH` guard and circular dependency exception detailing the inclusion chain path.
    *   YAML anchor and merge key dataset inheritance.
    *   `_steps` internal trace generation and accuracy.
    *   **Sensitive Data Protection**: verify that `_sensitive` registered fields are masked in logs (`********`) and redacted in LLM prompts (`[REDACTED]`), but raw values type correctly in tests.
    *   **Layered Datasets**: verify Cartesian product explosion of `_constants`, `_dimensions`, and `_lookup` maps.
    *   **Environment Precedence**: verify that CLI parameters override system properties, which override local descriptor targets.
  *   Ensure 100% code coverage on all new parsing, scrubbing, and execution multiplication logic.
