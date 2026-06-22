## Why

Test automation often suffers from a strict divide between human-readable documentation and machine-executable code. Currently, tests are written in YAML, which lacks reuse mechanisms, is sensitive to indentation, and forces business testers into a rigid, non-natural format. We need a fresh approach that first fixes the fragilities of our current YAML engine, and second, introduces a compiler architecture that allows pure test documentation (like Markdown) to serve as the favored, primary input document for execution. This unifies documentation and automation into a single source of truth.

## What Changes

### Phase 1: Freshening Up Our Current YAML Engine
- **List-Based Steps**: Steps are defined as a standard YAML list (`- step text`), eliminating indentation sensitivity.
- **Unified `_include:` Directive**: Support a special `_include: <relative_path>` entry to dynamically load and expand external files.
- **Flexible Data-Level Inclusions**: Support three dynamic inclusion modes (block, list-splice, and map-override).
- **Flexible Included File Structure**: Automatically extract nested `data:` lists from included files.
- **Schema Tolerance for Free Text**: The parser tolerates and ignores any unknown, extra, or free-text keys, allowing embedded metadata/notes.
- **Internal Prefix Convention**: All internal framework keys start with an underscore to strictly separate machine logic from human test data. The registered framework safety keys are `_meta`, `_properties`, `_context`, `_data`, `_beforeAll`, `_beforeEach`, `_steps`, `_onSuccessEach`, `_onFailureEach`, `_afterEach`, `_onSuccessAll`, `_onFailureAll`, `_afterAll`, `_include`, and `_id`.
- **Modern Playbook Lifecycle Blocks & Conditional Hooks**: Support complete execution lifecycle phases including static setup/teardown (`_beforeAll`, `_afterAll`), iteration setup/teardown (`_beforeEach`, `_afterEach`), and conditional outcome hooks executed on success or failure (`_onSuccessEach` and `_onFailureEach` for iterations; `_onSuccessAll` and `_onFailureAll` for the entire playbook).
- **Interleaved Variable Loop**: Variables (`${var}`) and inclusions are resolved in an interleaved loop with a depth guard (`MAX_DEPTH = 10`).
- **Step Location Tracing**: Each executed step carries a full inclusion stack trace stored internally under the `_steps` key.

### Phase 2: Multi-Format Support & Document-Driven Automation
- **Pure Test Documentation as Input**: Markdown (`.md`) becomes the favored execution format. 
- **Unified Execution Model (IR)**: Decouple the authoring frontend from the backend runner by compiling everything to a unified Intermediate Representation (AST).
- **LLM-as-a-Transpiler**: Introduce an AI compiler pipeline that uses LLMs to extract steps and datasets from free-flowing Markdown into the strict IR schema.
- **Hash-Based Caching**: Cache transpiled IRs using SHA-256 hashes to eliminate latency and LLM costs on repeated executions.

## Capabilities

### New Capabilities
- `fresh-approach-to-test-scripts-and-test-documentation`: Implement the two-phase modernization of test scripts. Phase 1 provides resilient list-based YAML parsing, schema tolerance, sensitive data scrubbing, layered dataset structures, and dynamic `_include:` directives. Phase 2 introduces an AI-driven compiler architecture to support pure Markdown documentation as the primary execution format via a Unified Intermediate Representation.

### Modified Capabilities
*None*

## Impact

- `com.xceptance.neodymium.common.testdata.util.YamlFileReader`: Modify to detect list-based steps, resolve `_include:` entries, support schema tolerance with machine key validation, parse layered `_data` structures (constants, dimensions, lookups), enforce sensitive key registries (`_sensitive`), run the interleaved resolution loop, and generate `_stepLocations`.
- `com.xceptance.neodymium.common.testdata.util.TestDataUtils`: Propagate file context for relative paths.
- `com.xceptance.neodymium.ai.core.AiAgent`: Read `_stepLocations` for trace reporting and automatically redact sensitive parameters (`_sensitive`) from prompts.
- `com.xceptance.neodymium.common.runner.NeodymiumRunner`: Modify the execution engine to dynamically resolve execution environments according to the strict priority precedence hierarchy (Runner CLI > System Default properties > Playbook Suggestions) and explode the Cartesian target grid.
- **New Architecture Components (Future)**: Development of `MarkdownTestParser`, the Unified IR AST, and the `LlmMarkdownTranspiler` plugin with hash-based caching.
