# Design: List-Based YAML Steps & Unified Inclusions

This document defines the technical architecture for parsing list-based YAML test cases and executing unified, context-aware `include:` directives in the Neodymium library.

---

## 1. Core Format

The format stays 100% standard YAML. The key change is that steps become a **YAML list** instead of a block scalar, and includes are special map entries within the list. Dataset inheritance uses YAML's native anchors and merge keys.

```yaml
# Test: End-to-End Order Checkout

_context: Validates checkout on staging

steps:
  - Navigate to home page
  # Set up user session
  - _include: fragments/login.steps
  - Select product matching "${role}"
  - Add item to cart
  # Complete order
  - _include: fragments/checkout.steps
  - Verify confirmation for "${username}"

data:
  - _testId: GuestCheckout
    _include: fragments/default_user.yaml
    shippingMethod: Ground
    promoCode: FREESHIP

  - _testId: PremiumCheckout
    _include: fragments/default_user.yaml
    username: premium_user@xceptance.com # Adjusts/overrides the guest username
    shippingMethod: NextDay
    promoCode: PREMIUM20
```

---

## 2. Parsing Pipeline

### Phase A: YAML Loading
SnakeYAML parses the entire file as a standard YAML document. The root is expected to be a Map.
*   **Schema Tolerance for Free Text:** The parser tolerates and ignores any unknown, extra, or free-text keys without a leading underscore. This allows authors to add arbitrary annotations, comments, or metadata without breaking execution.
*   **Framework Key Guard:** Any key starting with an underscore (`_`) is considered a machine-space instruction. The parser strictly validates all root and dataset-level underscored keys against the official framework registry: `_meta`, `_properties`, `_context`, `_data`, `_beforeAll`, `_beforeEach`, `_steps`, `_onSuccessEach`, `_onFailureEach`, `_afterEach`, `_onSuccessAll`, `_onFailureAll`, `_afterAll`, `_include`, `_id`, `_sensitive`, and `_dynamic`. If an unrecognized underscored key is detected (e.g. `_step:` or `_dta:`), the parser must immediately throw a `MalformedPlaybookException`.
*   **Property Precedence & Nested Flattening:** Keys in `_properties` are recursively flattened into standard dotted keys. If a key is defined both in a nested structure and as a dotted key (e.g. `neodymium.ai.pesap.enabled: true` alongside `neodymium: ai: pesap: enabled: false`), the dotted flat key takes absolute precedence.
    *   **Unified AI Prefix Resolution (`skipReplay`):** To resolve the naming inconsistency where AI configurations use the `neodymium.ai` prefix but `skipReplay` was a raw key, `skipReplay` is strictly replaced by `neodymium.ai.skipReplay` (nested inside the `_properties` block as `neodymium -> ai -> skipReplay`). No backwards compatibility is needed.
        *   The runtime execution engine and tests will read and evaluate only `neodymium.ai.skipReplay`.
*   **Lifecycle Blocks:** `_beforeAll`, `_beforeEach`, `_steps`, `_onSuccessEach`, `_onFailureEach`, `_afterEach`, `_onSuccessAll`, `_onFailureAll`, and `_afterAll` are loaded as `List<Object>`. Each element is either a `String` (an instruction), a `Map` (a directive like `{_include: path}`), or any other unrecognized type/structure (which is ignored).
*   **Data:** Loaded as a `List<Map>`. Standard YAML anchors (`&name`) and merge keys (`<<: *name`) continue to be supported natively by SnakeYAML, but the primary method for data composition is the dataset inclusion mechanism (`_include: <path>`) with local key overrides.
*   **Comments:** Standard YAML `#` comments are stripped during parsing (standard YAML behavior). They are visible in the source file but never reach the execution engine.
 
### Phase B: Variable Reflection & Include Resolution (Interleaved Loop)
The execution lifecycle consists of two distinct scopes (Static Playbook Scope and Dynamic Iteration Scope):
 
#### 1. Static Playbook Scope (Runs Once per Playbook)
* **Blocks:** `_beforeAll`, `_onSuccessAll`, `_onFailureAll`, `_afterAll`.
* **Reflection Hierarchy:** These blocks only have access to static global variables defined in `_meta`. They can be referenced either directly (e.g. `${_testId}`) or via the explicit `_meta` namespace (e.g. `${_meta._testId}`). They do **not** receive dynamic variables from `_data` rows.
* **Resolution:** Executed once before/after the entire dataset iteration cycle. `_onSuccessAll` runs only if the overall playbook execution completes successfully. `_onFailureAll` runs only if any part of the playbook execution fails. `_afterAll` always runs like a `finally` block.
 
#### 2. Dynamic Iteration Scope (Runs per Dataset Row)
* **Blocks:** `_properties`, `_context`, `_beforeEach`, `_steps`, `_onSuccessEach`, `_onFailureEach`, `_afterEach`.
* **Reflection Hierarchy:** For each dataset row in `_data`, the parser builds an isolated Active Variable Map by overlaying the dataset keys on top of the static global `_meta` defaults. Placeholders `${var}` inside the target blocks are dynamically resolved using these active values.
* **Resolution:** Evaluated in a strictly isolated scope per row to prevent data leakage between iterations. During execution:
  * `_beforeEach` setup always runs first.
  * `_steps` is executed.
  * If steps succeed, `_onSuccessEach` is executed.
  * If steps fail, `_onFailureEach` is executed.
  * `_afterEach` teardown always runs last.

The steps expansion and variable resolution run as follows (max 10 iterations):
1.  **Reconstruct lifecycle strings:** Join all `String` entries from each block list, separated by newlines. For each `{_include: path}` entry, insert the marker `[_include:<path>]` into the string.
2.  **Run the interleaved loop** (max 10 iterations):
    *   **Resolve variables:** Evaluate `${var}` placeholders across steps, context, and properties blocks using the active variables context. This resolves parameterized paths (e.g. `_include: fragments/${type}.steps`).
    *   **Expand inclusions:** Find any remaining `[_include:<path>]` markers, read the referenced file from disk (relative to the parent file's directory), and replace the marker with the file's content.
3.  **Final variable cleanup:** One last interpolation pass to resolve variables brought in by included files (e.g. `${username}` inside `login.steps`).
4.  **Guard:** If `[_include:]` markers still remain after 10 iterations, throw a `MalformedPlaybookException` detailing the exact recursive dependency chain (e.g. `Circular inclusion detected: main.yaml -> fragments/A.yaml -> fragments/B.yaml -> fragments/A.yaml`).

### Phase C: Data-Level Include Resolution

We support three levels of data-level inclusions with flexible file structuring (automatically unwrapping top-level `data:` or `_data:` annotations in included files):

1.  **Full Block Inclusion (Root `data` level):**
    If the root `data` property is parsed as a Map containing an `_include: <path>` key (e.g., `data: {_include: path}`):
    *   Load and parse the target file as a YAML document.
    *   If the parsed document is a Map containing a `data:` or `_data:` key, extract that nested list. Otherwise, treat the parsed document directly as the dataset list of Maps.
    *   Replace the entire `data` block with this resolved list.

2.  **List-Level Inclusion (Element expansion inside `data` list):**
    If an element inside the `data` list is a Map containing ONLY the `_include: <path>` key and no other properties:
    *   Load and parse the target file as a YAML document.
    *   If the parsed document is a Map containing a `data:` or `_data:` key, extract that nested list. Otherwise, treat the parsed document directly as the dataset list of Maps.
    *   Expand and splice all entries from that list in-place into the parent `data` list at the position of the inclusion directive.

3.  **Map-Level Inclusion (Merge with local overrides):**
    If a dataset map contains `_include: <path>` alongside other custom keys (e.g., `_testId`, `username` overrides):
    *   Load and parse the target file as a YAML Map.
    *   Merge all key-values from the loaded map into the current dataset map, **preserving existing local keys as overrides**.
    *   Remove the `_include` key from the final dataset map.

---

## 3. Step Location Tracing

During Phase B (Inclusion Expansion), we construct a trace path string for every expanded step:

*   Direct step: `"MyTest.yaml:7"`
*   Included step: `"fragments/login.steps:2 -> MyTest.yaml:9"`
*   Nested included step: `"fragments/form.steps:1 -> fragments/login.steps:3 -> MyTest.yaml:9"`

We serialize this list of trace strings as a JSON array and inject it under the internal key `_steps` in the dataset map. 
Meanwhile, the fully resolved step text instructions are joined with newlines and injected under the legacy key `steps` for the runner to execute.

`AiAgent` reads the `_steps` metadata at runtime to print precise stack traces in execution logs and failure messages:
```
⚠️ Actions failed: Element not found (fragments/form.steps:1 -> fragments/login.steps:3 -> MyTest.yaml:9)
```

---

## 4. Filesystem and Classpath Resolution

All inclusion paths are resolved relative to the base context of the active playbook:

1.  **Direct File Execution:** When parsing via `YamlFileReader.readFile(File file)`, we capture `file.getAbsoluteFile().getParentFile()` as the base directory. Relative inclusion paths are resolved via `new File(baseDir, relativePath).getCanonicalFile()`.
2.  **Classpath/Surefire Execution & /tmp Bypass:** When `TestDataUtils` discovers playbooks in the classpath:
    *   If the playbook resource URL uses the `file:` protocol (e.g. during local IDE or Maven runs), we bypass copying the file to the temporary directory. We extract the canonical `File` from the URL, preserving the real base directory context for relative inclusions.
    *   If the playbook is packaged inside a JAR (e.g. `jar:file:` protocol), relative inclusions are resolved relative to the package resource folder using the classloader (e.g. resolving `fragments/login.steps` relative to package `com/xceptance/posters/checkout/`).

---

## 5. Backward Compatibility

The existing block scalar format (`steps: |` with multi-line text) continues to work unchanged. The new list-based format is an **additive capability** — the parser detects whether `steps` is a `String` (block scalar, legacy) or a `List` (new format) and handles both transparently.

## 6. Sensitive Data Protection (Dual-Context Resolution Guard with nested `_sensitive` Maps)

To protect sensitive keys (e.g., credentials, passwords, card numbers) from leaking to external AI logging contexts while preserving data format realism for the LLM, the parser executes a **Dual-Context Resolution Guard** during resolution:

1. **In-Place Sensitive Declaration:** Instead of a decoupled global registry, sensitive variable keys are declared and defined completely in-place under an explicit **`_sensitive`** map block inside dataset constants (`_constants`) or dataset rows (`_data` rows).
2. **Implicit Registry Registration:** The parser automatically flags any variable key defined inside a `_sensitive:` map block as sensitive.
3. **Parallel Context Map Construction & Anonymization:**
   For each generated execution dataset row:
   - **Native Automation Context Map (Raw):** Constructed containing all raw key-value pairs (including actual credentials and PII). For sensitive variables, the parser extracts the raw value from the nested `_value` key (e.g. `password._value`).
   - **Guarded AI Context Map (Sanitized):** Constructed by copying the Native Map, but immediately replacing the values of sensitive keys with realistic **Stand-in Data** *before* any placeholder resolution begins:
     - **Precedence 1 (User-Defined Stand-in):** The parser extracts the explicit mock value defined inside the nested `_mock` key (e.g., `password._mock`).
       - *Note on Markdown Parsing (Keys and Values):* When the Markdown compiler parses table headers or profile list keys:
         - If a key contains a trailing parenthetical safety indicator—specifically `(sensitive)` or `(private)` (e.g., `Card Number (sensitive)` or `Password (private)`)—the compiler flags that key as sensitive and extracts the clean, stripped camelCase/snake_case variable name (e.g., `cardNumber` or `password`) for standard variable mapping.
         - If the value column contains inline parenthetical mock notation (e.g., `` `4111111111111111` (mock: `4000123456789010`) ``), it automatically parses it into the nested `_sensitive` Map structure: setting `_value: "4111111111111111"` (for raw execution) and `_mock: "4000123456789010"` (the explicit mock stand-in).
     - **Precedence 2 (Framework-Fabricated Stand-in):** If `_mock` is omitted, the framework automatically generates a format-realistic fake value based on the field name (e.g., using test credit card number structures for keys matching `/card|credit/i`, `999` for CVV, `mockPassword_123` for passwords, and `mock_[keyname]` as a fallback).
3. **Dual Resolution Paths (Phase B Loop):**
   When the Phase B interleaved loop resolves variable placeholders (`${var}`) across the playbook components:
   - **Execution Path (Local):** Resolves step instructions (`_steps`, `_beforeEach`, `_afterEach`, `_properties`) using variables from the **Native Map** to produce the exact uncensored values required by the local browser automation runtime.
   - **LLM/AI Path (External):** Resolves instructions using variables from the **Guarded Map** to produce the format-realistic anonymized strings passed to the LLM (e.g., inside dynamic context prompts, error logs, and execution stack traces). Because sensitive variables are resolved as realistic mock data at the source, any compound variable expression (e.g. `secret-token-${password}`) automatically resolves safely (e.g. `secret-token-mockPassword_123`).
4. **Log Masking:** For standard framework logger terminal outputs, the engine automatically replaces any string patterns matching values in the sensitive registry with asterisks (`********`).

---

## 6a. Dynamic Data Protection (`_dynamic` — Playbook Recording Sanitization)

When the AI agent records actions into a playbook during the initial generation run, resolved variable values (e.g. `john.doe.us.8f3a@example.com`) are baked directly into the JSON action fields (`value`, `target`, `hint`). If any of those variables are **dynamic** — meaning their resolved value changes between runs — the recorded playbook becomes non-deterministic: replays will fail on value mismatches (e.g. unique constraint violations for re-used emails, or wrong port numbers for programmatic URLs).

The `_dynamic` directive solves this by telling the playbook recorder to **replace resolved runtime values with their original `${placeholder}` form** before persisting actions, so that replays re-resolve the variable freshly from the active dataset context.

### Declaration Scopes

`_dynamic` is a **list of variable key names** and can appear at any level where data is defined:

1. **`_constants` scope:** Declares dynamic keys that apply to every generated execution row.
2. **`_lookup` per-dimension scope:** Declares additional dynamic keys specific to that dimension value.
3. **Legacy flat `data:` rows:** Can appear directly inside a dataset map.

```yaml
_data:
  _constants:
    _dynamic:
      - baseUrl       # programmatic, injected at runtime
      - email         # template uses ${random}, value changes every run
    _sensitive:
      password:
        _value: "Secret123!"
        _mock: "mockPassword_abc"

  _lookup:
    locale:
      en-GB:
        _dynamic:
          - couponCode  # dimension-specific dynamic variable
```

Legacy flat format:
```yaml
data:
  - testId: "us"
    email: john.doe.us.${random}@example.com
    password: Password123!
    _dynamic: [email, baseUrl]
```

### Relationship to `_sensitive` and Other Implicit Sources

Variables are determined to be dynamic using the following classification hierarchy:

| Variable Origin | Classification | YAML Declaration Required? |
| :--- | :--- | :--- |
| `_data` / `_constants` / `_lookup` | Explicit | **Yes** — must be listed in `_dynamic` |
| `_sensitive` map block | Implicit | **No** — always dynamic |
| `store` action (runtime-captured) | Implicit | **No** — always dynamic |
| Programmatic injection (Java API) | Declared at runtime | **No** — defined via API |

There is **no auto-detection** of dynamic variables in static data blocks; they must be listed explicitly. However:
1. All `_sensitive` variables are **implicitly dynamic**.
2. All runtime variables captured via `store` actions (e.g. `Store text from X as "varName"`) are automatically registered as dynamic.
3. All variables injected via the Java API with a `DYNAMIC` classification are registered as dynamic.

This prevents redundant boilerplate entries in the YAML file for values that the framework already programmatically knows are dynamic.

### Playbook Recording Sanitization Pipeline

When the playbook recorder persists an `Action` object to JSON:

1. **Build Dynamic Lookup Table:** For the active dataset row, collect all keys flagged as dynamic (explicit `_dynamic` list + implicit `_sensitive` keys). For each key, map its **resolved runtime value** → **original `${key}` placeholder string**.
2. **Sort by Value Length (Descending):** To prevent partial substring corruption (e.g. a short value like `8080` accidentally matching inside a longer unrelated string), the lookup entries are sorted by resolved value length, longest first.
3. **Sanitize Action Fields:** For each action's `value` field, scan for occurrences of resolved dynamic values and replace them with the corresponding `${placeholder}`. The `target` and `hint` fields are sanitized using the same lookup, but only when the resolved value appears as a **complete segment** (not as a substring of a CSS selector token or XPath expression) to prevent locator corruption.
4. **Persist:** The sanitized action JSON is written to the playbook file.

### Replay Re-Resolution

When replaying a persisted playbook, the execution engine encounters `${placeholder}` tokens inside action field values. These are resolved against the **current active dataset row** at replay time, producing fresh runtime values (new random suffixes, current ports, etc.).

---

## 6b. Java Programmatic Variable Injection & Retrieval API

In many integration tests, variables must be resolved and injected programmatically at the start of a test (e.g. dynamic staging URLs or randomly generated database entities in a JUnit `@BeforeEach` method) rather than statically defined in the playbook file.

To allow Java setup code to inject and retrieve these variables with their proper classification context without forcing the author to declare them in the YAML, we expose a structured, namespace-based data API:

```java
package com.xceptance.neodymium.util;

public final class Neodymium
{
    /** Returns the thread-local programmatic data context manager */
    public static NeodymiumData data()
    {
        return getContext().neodymiumData;
    }
}
```

The `NeodymiumData` interface exposes the separate classification scopes, as well as root-level hierarchy-aware resolution methods:

```java
package com.xceptance.neodymium.common.testdata;

public interface NeodymiumData
{
    /** Dynamic scope: values replaced with ${placeholder} on playbook recording */
    DataScope dynamic();
    
    /** Sensitive scope: masked in logs and faked in LLM prompts, but raw for execution */
    SensitiveDataScope sensitive();
    
    /** Constants scope: global constants merged into all runs */
    DataScope constants();
    
    /** Normal scope: standard iteration-scoped variables */
    DataScope normal();

    /** Unified, hierarchy-aware resolution methods */
    String get(final String name);
    <T> T get(final String name, final Class<T> clazz);
    boolean exists(final String name);
}
```

The individual scopes allow clean setter and getter methods:

```java
package com.xceptance.neodymium.common.testdata;

public interface DataScope
{
    void set(final String name, final String value);
    String get(final String name);
    <T> T get(final String name, final Class<T> clazz);
    boolean exists(final String name);
}

public interface SensitiveDataScope
{
    void set(final String name, final String value, final String mockValue);
    void set(final String name, final String value); // Auto-fabricated mock stand-in
    String get(final String name);
    <T> T get(final String name, final Class<T> clazz);
    boolean exists(final String name);
}
```

### Hierarchy Resolution Order
When calling `Neodymium.data().get(name)` or `Neodymium.data().exists(name)`, the framework traverses the following resolution hierarchy in order, returning the first matching value:

1. **Programmatic Java Scopes (Highest Precedence):**
   - `sensitive()` scope
   - `dynamic()` scope
   - `normal()` scope
   - `constants()` scope
2. **Active YAML/Markdown Dataset Context (Current Iteration Scope):**
   - Active iteration variables resolved for the current dataset row (combining dimension-specific lookups and dataset constants).
3. **Global Playbook Defaults:**
   - Static defaults declared inside the `_meta` block.
4. **External Environment Configuration (Lowest Precedence):**
   - Dotted properties mapped in `_properties` or JVM/System properties (e.g., `neodymium.properties`).

### Injection & Scoping Mechanics
When calling `.set()` on a scope (e.g. `Neodymium.data().dynamic().set("a", "b")`):
- The variable is registered directly in the active test execution's respective metadata scope.
- Injected dynamic and sensitive variables are automatically tracked and sanitized during playbook recording.
- Since the classification is supplied at the point of injection, there is no need to write boilerplate `_dynamic` or `_sensitive` blocks in the playbook YAML file.
- Any variable can be read back with the corresponding `.get()` method on a specific scope (e.g. `Neodymium.data().dynamic().get("a")`), keeping data classification explicit and symmetric for both reading and writing.
- Alternatively, calling `Neodymium.data().get("a")` traverses the entire context hierarchy to retrieve the currently active, valid value. This is especially useful during complex multi-browser, parameterized execution splits where the active context shifts dynamically.

---

## 7. Layered / Test Profile Data Structure

To support clean i18n test profiles without copying entire data columns, `_data` supports an additive **layered Map structure**:

```yaml
_data:
  _constants:
    username: "john.doe@example.com"
  _dimensions:
    locale: ["en-US", "de-DE"]
  _lookup:
    locale:
      en-US: { productName: "Grizzly Bear", subtotal: "$17.00" }
      de-DE: { productName: "Grizzlybär", subtotal: "14,96 €" }
```

### Parsing & Explosion Pipeline:
1. **Type Detection:** If `_data` is loaded as a `Map`, the parser reads the `_constants`, `_dimensions`, and `_lookup` sub-keys. If it is a `List`, it falls back directly to legacy flat rows.
2. **Cartesian Product:** The parser computes the Cartesian product of all active keys defined in `_dimensions`.
3. **Lookup Resolution:** For each combination, the parser pulls the lookup override values from `_lookup` matching that dimension's value.
4. **Merge and Flatten:** Overrides are merged with the `_constants` to build the isolated Active Variable Map per execution row.

### Dropping Manual Dataset-level Identifiers (`testId` / `dataId`)

In traditional Neodymium test datasets, every row required a manual unique identifier key (often `testId` or `dataId`) to name the test execution instance in reports.

Under the new layered Cartesian model, these manual dataset-level identifiers are completely **dropped and deprecated**:
- **Automated Run Names:** The execution engine dynamically construct the iteration name using the playbook filename and the Cartesian coordinates of the active dimensions (e.g. `CheckoutPlaybook [locale=en-US]`).
- **Reduction of Boilerplate:** Eliminating required ID fields in datasets avoids typing errors and makes data sheets/lookup tables cleaner.
- **Backward Compatibility:** For legacy flat dataset lists, manual `_testId` and `_id` are still tolerated as optional safety keys, but are no longer required.

---

## 8. External Environment Precedence Hierarchy

The execution engine resolves active environment parameters (e.g., target `browser`, `viewport`) dynamically by evaluating the source settings in a strict priority order:

1. **Runner Context (Command-Line Override):** Highest priority. System properties passed dynamically during maven runs (e.g., `mvn test -Dbrowser=Chrome,Firefox`).
2. **System Defaults (`neodymium.properties`):** Global baseline configuration for the active environment profile.
3. **Suite / Test Run Descriptor (Local suggestions):** Lowest priority. Playbook-specific local default environment recommendations.

At runtime, the Cartesian product of the active logical Test Profiles (`_data` rows) and the resolved Environment targets is exploded programmatically by the runner to spawn the final execution instances.

---

## 9. Pure Formats vs. Hybrid Mix (Architectural Decision Record)

We have explicitly chosen to support **strictly Pure Markdown** and **strictly Pure YAML**, deprecating and removing any mixed or hybrid formats (such as embedding raw YAML code blocks within Markdown files).

### Why the Hybrid Mix is Deprecated:
*   **User Friction:** Forcing business-focused authors to write or debug indentation-sensitive raw YAML blocks inside Markdown files completely breaks the "document-driven" abstraction.
*   **Syntax Fragility:** Indentation errors inside Markdown code blocks are extremely difficult to diagnose and are prone to crashing execution parser passes.
*   **Decoupled Compilers:** By standardizing on two pure front-ends that compile directly to a **Unified Intermediate Representation (IR)** AST, both audiences (business analysts writing pure MD and automation engineers writing pure YAML) are fully supported without half-measure mixtures.

### Design Standards:
*   **Pure Markdown (.md):** 100% natural, human-readable documents. Datasets are parsed exclusively from natural Markdown tables (columns mapped to variables) or structured bulleted lists. Steps are clean numbered lists.
*   **Pure YAML (.yaml):** Clean, standard structured YAML playbooks using the layered `_data` model (constants, dimensions, lookups). No mixed code elements.
