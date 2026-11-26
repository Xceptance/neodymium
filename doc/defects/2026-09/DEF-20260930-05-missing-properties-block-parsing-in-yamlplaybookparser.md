# [DEF-20260930-05] Missing _properties Block Parsing in YamlPlaybookParser Causing Ignored Playbook Configuration and Token Budget Failures

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `neodymium-core` (`ai-playbook`, `config`, `junit`)
- **Scope:** `Framework`
- **Symptom:** Setting framework configurations (such as `neodymium.ai.tokenBudget.input`) via `_properties:` or `properties:` in YAML playbooks had no effect. AI test runs aborted prematurely with `Token budget exceeded for test run: Input tokens consumed (...) exceeded configured input token budget (500000)`.
- **Root Cause:**
  1. `YamlPlaybookParser.java` omitted parsing for `_properties` and `properties` at both the root map level and inside dataset items, and omitted `_properties|properties` from `YAML_BLOCK_PATTERN`.
  2. For playbooks with `_properties` but without an explicit `data:` block, no default dataset was generated, preventing thread-local variables and `SessionData` from being populated.
  3. `NeodymiumAiRunner.java` and `TokenBudgetGuard.java` used disjoint data stores (`transientData`, annotations) rather than standardizing on `Neodymium.getData()` as the single source of truth for runtime execution settings.
- **Detection Gap ("What did we miss?"):** Existing `YamlPlaybookParserTest` cases tested `data:` and `steps:`, but lacked test cases exercising `_properties:` or `properties:` blocks.
- **Resolution:**
  1. Updated `YamlPlaybookParser.java` to recognize `_properties` and `properties` in `YAML_BLOCK_PATTERN`, flatten nested maps to dotted property keys, propagate root properties to all datasets, and generate a default dataset when properties exist without a `data:` section.
  2. Standardized runtime test configuration on `Neodymium.getData()`: `NeodymiumAiRunner` writes all dataset properties and `@AiContext` overrides directly into `Neodymium.getData()`, and `TokenBudgetGuard` / `AiConfiguration` read directly from it.
  3. Reset thread-local context cleanly in `beforeEach` and `afterEach` via `Neodymium.clearThreadContext()` to ensure zero property bleed across test iterations.
- **Safety Net Added:** Added unit tests in `YamlPlaybookParserTest` for root and dataset-level `_properties` flattening, and in `TokenBudgetGuardTest` for dynamic `Neodymium.getData()` budget resolution.
