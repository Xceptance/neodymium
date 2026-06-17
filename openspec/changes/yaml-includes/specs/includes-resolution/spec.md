## ADDED Requirements

### Requirement: Unified Inclusions Directive
The framework SHALL support a unified `_include:` directive to dynamically load and splice external steps or datasets (supporting full-block, inline-list, and map-level overrides).

#### Scenario: Lifecycle block inclusion
- **WHEN** a lifecycle block (e.g., `_steps`) contains an `_include: <path>` directive
- **THEN** the parser loads the referenced YAML steps file and splices its steps dynamically in-place

### Requirement: Interleaved Variable & Include Resolution Loop
The parser SHALL resolve variables and inclusions in an interleaved loop (max `MAX_DEPTH = 10` iterations) in a strictly isolated scope per dataset iteration:
- **Static Scopes (`_beforeAll`, `_onSuccessAll`, `_onFailureAll`, `_afterAll`):** Placeholders SHALL resolve once using static `_meta` variables.
- **Iteration Scopes (`_properties`, `_context`, `_beforeEach`, `_steps`, `_onSuccessEach`, `_onFailureEach`, `_afterEach`):** Placeholders SHALL resolve per dataset row using active variables overlaid on `_meta` defaults.

To support dynamic configuration, the include paths themselves (e.g., `_include: ${myIncludePath}`) SHALL be dynamically resolvable from active test data (constants or dataset rows) during the variable resolution phase of the loop.

#### Scenario: Iteration-isolated scope resolution
- **WHEN** dynamic variables from a dataset row are used within included files
- **THEN** variables are resolved using the active dataset row values without leaking data between iterations

#### Scenario: Include path parameterized via test data
- **WHEN** a test script step or dataset contains `_include: ${my_fragment_path}`
- **THEN** the variable `${my_fragment_path}` is resolved from the active test data during the interleaved loop, and the correct file is loaded dynamically

### Requirement: Namespace Resolution
Dotted prefixes SHALL be supported to explicitly access metadata fields (e.g., `${_meta._author}`) to prevent variable collisions.

#### Scenario: Metadata namespace resolution
- **WHEN** a step references a metadata field via the explicit namespace (e.g., `${_meta._author}`)
- **THEN** the parser resolves the placeholder correctly using the `_meta` block field value

### Requirement: Circular Inclusion Guard
If a circular inclusion chain is detected, the parser SHALL throw a `MalformedPlaybookException` displaying the exact circular path.

#### Scenario: Circular inclusion loop
- **WHEN** included files form a circular inclusion chain (e.g., `A.yaml -> B.yaml -> A.yaml`)
- **THEN** the parser throws a `MalformedPlaybookException` detailing the circular path

### Requirement: Classpath Resolution Bypass
When discovering YAML test scripts in the classpath:
- If the resource URL uses the `file:` protocol, the parser SHALL bypass copying the file to `/tmp` and retrieve the canonical `File` directly from the URL.
- If the resource protocol is `jar:file:`, relative inclusions SHALL be resolved relative to the package folder using the class loader context to prevent breaking relative inclusion directories.

#### Scenario: Local Maven/IDE execution bypass
- **WHEN** YAML test scripts are loaded from classpath using the `file:` protocol
- **THEN** the parser retrieves the file directly without copying to `/tmp`, ensuring relative inclusion paths remain valid

### Requirement: Data-Level Inclusion Modes
The parser SHALL support three levels of data-level inclusions, automatically unwrapping nested `data:` or `_data:` keys in included files if present:
1. **Full Block Inclusion (Root `data` level):** Replacing the entire `data` block with the resolved list.
2. **List-Level Inclusion (Element expansion):** Splicing entries from the referenced list in-place.
3. **Map-Level Inclusion (Merge with overrides):** Merging the loaded map into the dataset map while preserving local keys as overrides.

#### Scenario: Map-level inclusion with local overrides
- **WHEN** a dataset row has an `_include: <path>` directive alongside custom variable definitions
- **THEN** the parsed map is merged into the dataset row map, preserving the custom local variables as overrides

### Requirement: Inclusions within Conditional Steps (LLM-Interpreted Conditions)
To support conditional inclusion execution without fragile hardcoded patterns or complex Java regex-based control flow logic:
- The framework SHALL support embedding `_include: <path>` directives within conditional step expressions (e.g. `If the cart is empty, then _include: fragments/add-to-cart.steps, else proceed to checkout`).
- During the resolution phase, the framework SHALL resolve and expand the `_include` directive by reading the referenced file and replacing the directive inline with the raw step content (e.g., as parenthesized, semicolon-separated instructions: `If the cart is empty, then (Click product; Click Add to Cart), else proceed to checkout`).
- The framework SHALL NOT use regular expressions or custom Java patterns to evaluate or parse conditional branches programmatically.
- The LLM SHALL dynamically interpret and evaluate the entire conditional instruction based on the live page state.

#### Scenario: Conditional inclusion resolved inline
- **WHEN** a step contains a conditional inclusion `If user is guest, then _include: fragments/register.steps`
- **THEN** the parser expands the steps from `fragments/register.steps` inline, formatting it for the LLM to interpret dynamically at runtime

### Requirement: Step Location Tracing
During inclusion expansion, the parser SHALL build and serialize a trace path string (inclusion stack trace) for every expanded step and store it in a JSON array under the internal `_steps` key.

#### Scenario: Step traceback verification
- **WHEN** a step inside an included file fails at runtime
- **THEN** the execution log prints the full inclusion path trace (e.g., `fragments/form.steps:1 -> fragments/login.steps:3 -> MyTest.yaml:9`)
