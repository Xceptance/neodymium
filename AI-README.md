# Neodymium AI v2 Redesign - Features & Documentation

The redesigned v2 Neodymium AI framework (contained in `org.neodymium.ai.*`) delivers advanced agentic test execution, featuring structured playbook companion recordings, pre-step analysis, soft failure tolerance, and deterministic replay capabilities.

---

## 1. Execution Playbooks & Replay Cache
Instead of executing LLM calls dynamically on every run, the v2 framework uses **Structured Playbooks**:
* **YAML Playbook**: Contains natural language steps written either as a plain-text multiline block (`steps: |`) or a YAML list of step strings. Test scenarios support variables (`${username}`) and modular inclusions (`_include: ...`):
  - **Multiline Block**:
    ```yaml
    steps: |
      _include: common/setup.yaml
      Open ${verla.url}/verla-${quality}/index.html
      Click login button
    ```
  - **YAML List**:
    ```yaml
    steps:
      - "Open store homepage"
      - "Click login button"
    ```
* **JSON Companion**: A recording compiled automatically during the initial `FORCE_RECORDING` run. It maps each natural language step to a list of concrete structured SUT actions (e.g., `NAVIGATE`, `CLICK`, `TYPE`, `ASSERT`) along with visual `screenshotHash` baselines.
* **Offline Replay**: Subsequent test runs (`REPLAY_STRICT` or `REPLAY_WITH_HEALING`) load the companion JSON file directly, executing recorded browser interactions in milliseconds without making any LLM calls.
* **Recording Directory Configuration**: Companion `.json` recording output locations can be configured at the test class/method level or globally:
  - **Annotation-driven (`@AiPlaybook`)**: `@AiPlaybook(recordingDirectory = "target/playbooks/integration")` directs generated companion recordings to build output target directories to keep `src/` clean.
  - **Property-driven (`neodymium.ai.playbook.recordingDirectory`)**: Configured in `ai.properties` or JVM arguments (`-Dneodymium.ai.playbook.recordingDirectory=...`).
  - **Default**: When omitted, companion recordings are saved in the same parent directory as the source `.yaml` playbook (e.g. `src/test/resources/playbooks/...`).
  - **Strict Replay Error Handling**: If a test runs in replay mode (`REPLAY_STRICT` or `REPLAY_WITH_HEALING`) and no recorded companion `.json` file is found, execution fails immediately by throwing `FileNotFoundException`. No silent fallback to YAML playbooks occurs.

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

### `(bug)` / `(bug: id)` / `(bug: comment)`
Negates the outcome of a step when an expected bug exists in the SUT.
* **Expected Failure**: If the step fails for any reason (interaction or verification), the failure is caught, logged, and treated as **passed**. By default, execution halts immediately and skips subsequent steps (to prevent cascaded failures), but the overall test completes successfully.
* **Unexpected Success**: If the step succeeds (meaning the expected bug did not happen), the runner raises a failure and aborts the test immediately to flag that the bug has been resolved or was not encountered.
* **`(continue-on-error)` Support**: If a step is also tagged with `(continue-on-error)` (e.g. `Click button (bug) (continue-on-error)`), the runner continues executing subsequent steps regardless of whether the bug step failed or succeeded (if it succeeded, it just reports it as a warning).
* **Syntax Examples**: `(bug)`, `(bug: APP-123)`, `(bug: button missing)`

### `(no-healing)`
A standalone tag that disables all self-healing mechanisms for a specific step.
* **Behavior**: If the step fails during live or replay execution, the framework does not attempt LLM escalations or semantic self-healing. The failure is immediately propagated (which will either fail the test, trigger bug negation, or trigger optional soft-failure warnings, depending on the other tags present).
* **Syntax Examples**: `(no-healing)`, `(NO-HEALING)`, `( no-healing )`

### `(visual)` / `(visual: full)` / `(visual:full)`
Triggers visual execution mode with a page screenshot payload.
* **`(visual)`**: Triggers standard visual execution at `ContextLevel.VISUAL` (URL + Title header only, 0 DOM element nodes), capturing a standard viewport screenshot matching the active browser window size on the initial attempt (~2,800 tokens).
* **`(visual: full)` / `(visual:full)`**: Triggers visual execution starting at ultra-lean `ContextLevel.VISUAL` (URL + Title header only, 0 DOM element nodes) while forcing full-page screenshot capture (capturing full scrollable document height beyond the fold with a visual viewport border overlay) immediately on the initial attempt (~5,000–8,000 tokens).
* **Persistent Full-Page Flag During Escalation**: Stored in step transient data as `KEY_IS_FULL_PAGE_SCREENSHOT = true`. If visual evaluation fails or requires element interaction, escalation (`VISUAL` $\rightarrow$ `VISUAL_LEAN` $\rightarrow$ `VISUAL_RICH`) **continuously preserves full-page screenshot capture**. It will **never** revert to a small viewport screenshot during escalations.
* **Author Tag Protection**: Explicit `(visual)` and `(visual: full)` tags set by the test author are protected from being overwritten or downgraded by PESAP pre-step predictions.
* **Syntax Examples**: `(visual)`, `(visual: full)`, `(visual:full)`, `(visual-full)`, `(visual_full)`

---

## 3. Runtime Instruction Preparation
Before compiling prompts or sending request payloads to the LLM, the framework runs a dedicated instruction preparation helper. It dynamically strips all explicit control tags case-insensitively, including:
* `(no-replay)`
* `(bug)` / `(bug: ...)`
* `(continue-on-error)`
* `(no-healing)`
* `(optional)` / `(soft)`
* `(timeout: ...)`
* `(visual)` / `(visual: full)` / `(visual:full)`

This prevents internal execution instructions from polluting the natural language prompts sent to the LLM.

---

## 4. Pre-Step Split Analysis (PESAP) & Context Escalation
To handle complex, compound, or ambiguous instructions, the pipeline executes a **Pre-Step Split Analysis (PESAP)** using the `LlmCapability.PESAP` capability:
* **Contextual Inputs**: The analysis receives the current step, the previously executed step's instruction (for flow context), and up to two subsequent steps' instructions.
* **JIT Upfront Step Splitting**: If a compound step (e.g. `"Search for shirt, select size L, and click Checkout"`) is identified, the LLM splits the instruction into distinct leaf sub-steps. These are instantiated dynamically as child `PlaybookStep` instances and pushed onto the execution stack.
* **Conservative Non-Splitting Invariants**: Single-target instructions with multiple descriptive clauses (e.g. `"Select standard shipping option (5-7 business days) for $5.00"`) or referential verification instructions (e.g. `"Verify order total matches previous summary"`) are strictly preserved as single steps.
* **JIT Context-Level Detection**: Rather than relying on static defaults, PESAP dynamically determines the optimal initial interaction mode (Context Level) required for the step across the 8-tier context escalation ladder.
* **Prompt Optimization**: `pesap-pre-step-prompt.md` features a 56% token footprint reduction (~500 tokens $\rightarrow$ ~220 tokens), significantly lowering prompt overhead across test execution.
* **Bypassing on Replay**: PESAP runs during live recording mode; replay runs skip this analysis and execute the already-split steps directly from the JSON companion.

---

## 4.1 Tiered Context Escalation Ladder & Payload Modes

Neodymium AI uses an **8-tier context level escalation hierarchy** organized into **Two Strictly Monotonic Escalation Tracks** ($L_i \subset L_{i+1}$) that ensure the model never loses DOM context when escalating:

$$\textbf{Track A (DOM Track): } \text{HINT} \longrightarrow \mathbf{MINIMAL} \longrightarrow \mathbf{LEAN} \longrightarrow \mathbf{STANDARD} \longrightarrow \mathbf{RICH} \longrightarrow \mathbf{VISUAL\_RICH}$$
$$\textbf{Track B (Visual Track): } \mathbf{VISUAL} \longrightarrow \mathbf{VISUAL\_LEAN} \longrightarrow \mathbf{VISUAL\_RICH}$$

### Context Level Spectrum & Meaning

| Context Level | Mode Type | Payload Content Description | Trigger Conditions / Use Cases |
| :--- | :--- | :--- | :--- |
| **`HINT`** | Text | **0 DOM Nodes.** Explicit selector hint provided (e.g. `(hint: #id)`). Saves 100% of DOM tokens. | When explicit CSS selector hint is provided in playbook step. |
| **`MINIMAL`** | Text | **Interactive Form Controls Only.** Extracts inputs, textareas, selects, and form buttons without page chrome or paragraph copy (~300–800 tokens). | Ultra-lean default for form filling, sequential typing, and field entry. |
| **`LEAN`** | Text | **Interactive Elements + Headings + Container Skeleton + Concise Text Labels.** Filters out massive paragraph copy (`<p>`/`blockquote` > 120 chars). | Standard mode for navigation links, buttons, and card triggers. |
| **`STANDARD`** | Text | **`LEAN` + Standard Static Text.** Includes full static body `<p>` paragraph copy of any length, text spans, badges, and order totals. | Selected for text assertions, paragraph matching, or when `LEAN` escalates. |
| **`RICH`** | Text | **`STANDARD` + Full HTML Metadata.** Includes all `data-*`, `title`, `aria-describedby` attributes, un-truncated URLs, and 5-level parent context. | Selected for SKU/data-attribute targeting, table sorting, or deep card disambiguation. |
| **`VISUAL`** | Visual | **Viewport Screenshot + 0 DOM Element Nodes.** Pure visual assertion/check at standard screen size (~800–1,200 tokens). | Triggered by `(visual)` check/assertion without element interaction. Uses viewport screenshot. |
| **`VISUAL_LEAN`** | Visual | **Full-Page Screenshot + `LEAN` DOM.** Visual element interaction. | Triggered upon visual escalation; uses full-page screenshot. |
| **`VISUAL_RICH`** | Visual | **Full-Page Screenshot + `RICH` DOM.** Maximum multimodal context. Strictly preserves all DOM text and attributes. | Triggered by `(layout)` checks or cross-track escalation from `RICH`; uses full-page screenshot. |

> **Screenshot Capture Scope Strategy**:
> - **Viewport Screenshot**: Initial `VISUAL` steps (tagged `(visual)`) capture a standard viewport screenshot matching the active browser window size.
> - **Immediate Full-Page Trigger**: Steps tagged with `(visual: full)` or `(visual:full)` capture a full-page screenshot immediately on the initial attempt while using ultra-lean `ContextLevel.VISUAL` (0 DOM element nodes).
> - **Persistent Full-Page Escalation**: Once a step escalates visually (to `VISUAL_LEAN` or `VISUAL_RICH`) or starts with `(visual: full)`, screenshot capture **continuously preserves full-page mode** (capturing full document height beyond the fold, overlaid with a visual viewport border). It will never revert to a small viewport screenshot during retry escalations.

### DOM Serialization Differences: `MINIMAL` vs `LEAN` vs `STANDARD` vs `RICH`

To understand how `MINIMAL`, `LEAN`, `STANDARD`, and `RICH` differ at runtime:

```html
<!-- MINIMAL DOM Capture (Form Inputs & Action Buttons Only; Zero Surrounding Page Layout) -->
<form id="quick-order-form" data-ai="xc81920a">
  <input name="size" type="hidden" value="M" data-ai="xc93012b" />
  <button class="btn-quick-add" type="submit" data-ai="xce624f1">ADD TO BAG</button>
</form>
```

```html
<!-- LEAN DOM Capture (Interactive Elements + Headings + Concise Labels; Excludes Long Paragraph Copy) -->
<div class="product-card" data-ai="xce869u2">
  <div class="product-info" data-ai="xc9g93c4">
    <span class="product-category" selector="span.product-category" data-ai="xc2nupq6">TOPS</span>
    <h3 class="product-title" selector="h3.product-title" data-ai="xceg4w4t">Minimalist Oversized Hoodie</h3>
    <p class="product-price" selector="p.product-price" data-ai="xc1j5c6g">$120.00 USD</p>
    <div class="product-actions" data-ai="xc5f2v4m">
      <button class="btn-quick-add" type="button" selector="button.btn-quick-add:nth-of-type(1)" data-ai="xce624f1">ADD TO BAG</button>
    </div>
  </div>
</div>
```

```html
<!-- STANDARD DOM Capture (LEAN + Full Static Paragraph Copy & Body Text) -->
<div class="product-card" data-ai="xce869u2">
  <div class="product-info" data-ai="xc9g93c4">
    <span class="product-category" selector="span.product-category" data-ai="xc2nupq6">TOPS</span>
    <h3 class="product-title" selector="h3.product-title" data-ai="xceg4w4t">Minimalist Oversized Hoodie</h3>
    <p class="product-desc" selector="p.product-desc" data-ai="xch294lm">Minimalist silhouettes engineered with sustainable organic textiles and precision craftsmanship in Berlin.</p>
    <p class="product-price" selector="p.product-price" data-ai="xc1j5c6g">$120.00 USD</p>
    <div class="product-actions" data-ai="xc5f2v4m">
      <button class="btn-quick-add" type="button" selector="button.btn-quick-add:nth-of-type(1)" data-ai="xce624f1">ADD TO BAG</button>
    </div>
  </div>
</div>
```

```html
<!-- RICH DOM Capture (STANDARD + data-*, title, aria-describedby + 5-Level Parent Context) -->
<div class="product-card" data-product-id="prod-101" data-category="tops" data-ai="xce869u2">
  <div class="product-info" data-ai="xc9g93c4">
    <span class="product-category" selector="span.product-category" data-ai="xc2nupq6">TOPS</span>
    <h3 class="product-title" selector="h3.product-title" title="Minimalist Oversized Hoodie - Organic Cotton" data-ai="xceg4w4t">Minimalist Oversized Hoodie</h3>
    <p class="product-desc" selector="p.product-desc" data-ai="xch294lm">Minimalist silhouettes engineered with sustainable organic textiles and precision craftsmanship in Berlin.</p>
    <p class="product-price" selector="p.product-price" aria-describedby="price-disclaimer-101" data-ai="xc1j5c6g">$120.00 USD</p>
    <div class="product-actions" data-ai="xc5f2v4m">
      <button class="btn-quick-add" type="button" data-analytics="add-cart-top-101" selector="button.btn-quick-add:nth-of-type(1)" data-parent-text="TOPS > Minimalist Oversized Hoodie > $120.00 USD > Size M" data-ai="xce624f1">ADD TO BAG</button>
    </div>
  </div>
</div>
```

### Automatic Monotonic Escalation Flow Example

When an action step cannot be fulfilled at the initial context level, the framework automatically escalates to the next level:
1. **Initial Step**: Starts at **`MINIMAL`** (or `LEAN` / `VISUAL` based on PESAP prediction or explicit tags).
2. **Escalation 1 (`MINIMAL` $\rightarrow$ `LEAN`)**: Expands to all interactive elements, navigation links, and section headings.
3. **Escalation 2 (`LEAN` $\rightarrow$ `STANDARD`)**: Expands to static text, paragraphs, and order summary totals.
4. **Escalation 3 (`STANDARD` $\rightarrow$ `RICH`)**: Expands to all `data-*` attributes, ARIA descriptions, tables, and deep parent ancestry.
5. **Escalation 4 (`RICH` $\rightarrow$ `VISUAL_RICH`)**: Cross-track escalation attaches full-page screenshot while **strictly preserving all rich DOM text and attributes** (never regressing to 0 DOM nodes).

### 4.2 Dynamic Step Escalation Budget Model

To prevent infinite escalation loops while ensuring that steps starting at higher context levels (such as `LEAN` or `VISUAL_LEAN`) are never blocked from reaching `VISUAL_RICH`, the framework enforces a **Dynamic Step Escalation Budget**:

$$\text{Total Step Budget} = (\text{VISUAL\_RICH.ordinal()} - \text{initialLevel.ordinal()} + 1) + \text{maxRetriesAtMaxLevel}$$

* **Unused Level Budget Preservation**: When PESAP or explicit configuration starts a step at a higher level (e.g. `LEAN` or `VISUAL_LEAN`), unused lower levels (e.g. `MINIMAL`) are preserved. This provides full attempt capacity for higher-level interactions and visual checks.
* **Guaranteed Initial `VISUAL_RICH` Attempt**: Upward escalation into `VISUAL_RICH` for the first time is always permitted to run its initial attempt.
* **Circuit Breaker Enforcement**: The circuit breaker trips only when `attemptsUsed >= totalStepBudget` AND `currentLevel == VISUAL_RICH`.

---

## 5. Post-Action AI Outcome Verification
After executing the SUT actions for a step, the framework performs a **Post-Action Outcome Verification**:
* **Always Visual**: Regardless of the initial execution context level, the outcome verification always captures the SUT state at the `VISUAL` level to record baseline images and compute screenshot dHash baselines.
* **Semantic Verification Prompt**: Evaluates the natural language instruction against the final page DOM and screenshot using the `VerificationPrompt` template via the `LlmCapability.VERIFICATION` capability.
* **Advisory & Diagnostic by Design (Soft Failures)**: Verification failures perform post-step semantic auditing and diagnostic scoring. They are collected and reported as warnings at the end of the test case, allowing developers to inspect semantic discrepancies without crashing the automation flow. Verification failures do **not** fail the test case directly; explicit test assertions are enforced via concrete SUT `ASSERT` actions.

---

## 6. Dynamic Variable Parameterization & Outbound Secret Masking
Ensures recorded playbooks remain reusable and enterprise credentials remain confidential:
* **Resolution**: Resolves variables (e.g. `${username}`) at runtime before executing actions.
* **Outbound Secret Masking (`ContextSanitizer`)**: Before prompts, instructions, or captured SUT DOM state payloads are transmitted over the network to external LLM providers (Gemini, Mistral, Vertex), `DefaultContextSanitizer` scans the payload against sensitive session dataset entries (`DataEntry.sensitive() == true`) and replaces raw secret credentials with format-preserving `[MASKED_VAR_key]` placeholders (e.g. `[MASKED_VAR_password]`). Returned LLM responses are automatically reverse-mapped back to variable reference syntax (`${password}`) prior to action parsing, ensuring raw secrets never leave the client.
* **Recording Parameterization**: Automatically matches executed values and dynamic response strings (such as order numbers or generated URLs) back to variable definitions, writing parameterized entries like `"${order.number}"` into the companion JSON instead of hardcoded session values.

---

## 7. Decoupled Core Components & Resource Management
To ensure a modular architecture, all key interfaces are cleanly decoupled:
* **`PlaybookResourceManager`**: Decouples playbook loading/writing from specific file systems, serving as the interface for reading/saving playbooks (YAML & JSON) across local, classpath, or virtualized directories.
* **`TargetExecutor`**: Abstract driver interface separating the pipeline execution logic from browser drivers (e.g., Selenide/WebDriver) and REST clients.
* **`PlaybookParser`**: Standard interface for parsing structured or nested playbooks and inclusions.

---

## 8. Session-Centric Architecture & Thread Isolation
To support robust parallel execution (e.g., executing multiple tests concurrently in separate threads):
* **`AiSession`**: Serving as the thread-isolated lifecycle holder containing context state, target drivers, prompts, and config parameters.
* **Hierarchy Isolation**: Prompts and sessions can inherit configs from parent scopes but remain completely isolated at runtime, preventing thread cross-talk.
* **Dynamic Lifecycle Hooks**: Supports registering pre/post-execution hooks on sessions (e.g., clearing caches, starting servers, compiling reports).

---

## 9. Unified Event-Driven HUD & Logging
* **`EventBus`**: Centrally coordinates all framework execution events (e.g., `ActionExecutedEvent`, `SessionFinishedEvent`).
* **Heads-Up Display (HUD)**: Decoupled HUD listeners listen to event streams to render interactive overlays and debug windows without polluting the core execution pipeline.
* **Metrics Summary**: Automatically tracks duration, token count, LLM provider invocation logs, and semantic outcome errors on a per-step basis.

---

## 10. Registry & Pluggable LLM Routing
* **`LlmProviderRegistry`**: Hosts registered providers for LLM capabilities (e.g., `TEXT_ONLY`, `EXECUTION`, `VISION`, `PESAP`, `VERIFICATION`).
* **Capability-Based Routing**: Dynamically inspects SUT level requirements and routes prompts to the appropriate registered provider (e.g., utilizing vision models only when screenshots are attached).

---

## 11. Session-Level Authentication Setup
* **`BasicAuth` Configuration**: Registers credentials (username/password) dynamically on `AiSession` setup.
* **CDP Interception**: Intercepts basic auth browser challenges via low-level Chrome DevTools Protocol mechanisms, ensuring seamless authentication setup for headless SUT environments.

---

## 12. AI Prompt Taxonomy & Custom System Add-ons

The framework utilizes a dedicated taxonomy of prompts, each mapped to specific pipeline phases and LLM capabilities. To customize LLM behaviors without changing the core codebase, the framework supports injecting **Custom System Add-on Prompts**.

### A. Prompt Taxonomy

| Prompt Class | Pipeline Step / Context | LLM Capability | Inputs | Purpose & Output |
| :--- | :--- | :--- | :--- | :--- |
| **`PesapPrompt`** | `BeforeStep` / pre-step analysis | `PESAP` | Current instruction, previous instruction, next instructions. | Analyzes instruction flow to predict interaction `ContextLevel`, split compound instructions into sub-steps, and check if custom Java reflection methods are required. Outputs a structured JSON. |
| **`ActionExtractionPrompt`** | `CallLlmStep` / live action generation | `EXECUTION` | Current SUT DOM state, natural language instruction, step history. | Identifies the correct sequence of web automation actions (`CLICK`, `TYPE`, etc.) to implement the instruction. Outputs structured JSON actions. |
| **`QualityJudgePrompt`** | `QualityJudgeStep` / optional second-opinion evaluator | `JUDGE` / `EXECUTION` | Instruction, proposed primary action, candidate locators list, full DOM context. | Evaluates proposed locator and alternative candidates against full DOM tree for stability and uniqueness. Outputs structured judgment (`APPROVED`, `REFINED`, `REJECTED`), `chosenLocator`, and `chosenValue`. |
| **`VerificationPrompt`** | `VerifyOutcomeStep` / post-action validation | `VERIFICATION` | Natural language instruction, executed actions, pre/post screenshots. | Acts as an objective AI judge, scoring the outcome on rubrics (`intentMatch`, `visualDelta`, `absenceOfErrors`). Outputs a structured `VerificationResult` JSON. |
| **`SemanticDivergencePrompt`** | `SemanticDivergenceAnalysisStep` / replay healing | `TEXT_ONLY` | Baseline page source, current page source. | Compares expected vs actual SUT page states during a replay cache divergence to generate a plain-English diff summary (e.g. `ID changed from checkout to pay-now`). |
| **`VisualRcaPrompt`** | `StateMachineRunner.runVisualRca` / final error debug | `VISION` | Failed instruction, error details, current page screenshot. | Diagnoses visual root causes on conclusive execution failures (e.g., overlapping elements, cookie popups). Publishes a `DiagnosticErrorEvent`. |

### B. Custom Prompt Add-ons (`promptAddon`)

To tune the LLM's behavioral instructions for specific environments, applications, or testing scenarios, custom prompt add-ons can be declared dynamically in YAML playbooks, datasets, or model override files:

1. **Canonical Keyword (`promptAddon`)**:
   Playbooks and datasets standardize on the single canonical keyword `promptAddon` (supporting both scalar strings and nested capability maps):
   - **Scalar string (General)**:
     ```yaml
     promptAddon: "Always look for button text first and wait for spinners."
     ```
   - **Nested Map (Capability-Targeted)**:
     ```yaml
     promptAddon:
       general: "Always look for button text first"
       pesap: "Predict shorter execution timeouts"
       verification: "Be extremely strict about price format changes"
       rca: "Check if modal dialogs obscured the click target"
     ```
   - **Flat Dot Notation (Datasets & Playbooks)**:
     ```yaml
     promptAddon: "General rule for this dataset row"
     promptAddon.pesap: "Specialized pre-step check for this dataset row"
     promptAddon.verification: "Specialized outcome check for this dataset row"
     ```

2. **Per-Capability Customization Keys**:
   Add-on keys can target a specific type of LLM prompt or apply generally to all prompts:
   - `promptAddon` (or `promptAddon.general` / `promptAddon.default`): Applies to all prompts.
   - `promptAddon.pesap`: Appends specifically to the `PesapPrompt`.
   - `promptAddon.general`: Appends specifically to the `ActionExtractionPrompt`.
   - `promptAddon.verification`: Appends specifically to the `VerificationPrompt`.
   - `promptAddon.rca`: Appends specifically to the `VisualRcaPrompt`.
   - `promptAddon.divergence`: Appends specifically to the `SemanticDivergencePrompt`.

3. **Multi-Layer Accumulation (Stacking)**:
   Instead of an exclusive fallback, prompt add-ons **accumulate across all active layers** in order of specificity:
   0. **Multilingual Guidance Layer**: Dynamic language universality and localized DOM guidance (injected when `neodymium.ai.multilingual=true`).
   1. **Model / Disk Layer**: Global model-specific rules (`addon.md`, `addon-<type>.md`).
   2. **YAML Playbook Layer**: Playbook general add-on + capability-targeted add-on.
   3. **Test Dataset Layer**: Dataset general add-on + capability-targeted add-on (closest to data).
   
   All active layers are combined seamlessly, ensuring general playbook guidelines are preserved while dataset rows supply specific context.

4. **Dynamic Variable Interpolation (`${variableName}`)**:
   Prompt add-on strings support dynamic `${variable}` placeholders that are automatically resolved against runtime session data, dataset parameters, and configuration properties:
   ```yaml
   promptAddon: "The application is running in ${locale}. Ensure currency displays as ${currency}."
   ```

5. **Safety Limits & Adherence Enforcement**:
   To prevent custom prompt add-ons from diluting or overriding essential prompt instructions (such as JSON output schemas and capability rules), the following safety controls are enforced:
   * **Length Limit**: Any custom prompt add-on combined value must not exceed **2000 characters**. If it does, a validation check throws an `IllegalArgumentException` early.
   * **Adherence Enforcement Suffix**: When appending the custom add-on prompt, the compiler automatically appends a strict reminder suffix:
     `"CRITICAL REMINDER: The above rules are custom extensions for this test step. You MUST still strictly follow all JSON schema formatting rules, action capabilities, and output guidelines specified in the main system prompt above."`
     This prevents the LLM from generating invalid text/HTML outputs when guided by custom user rules.

### C. Disk-Based Model-Specific System Prompt Add-ons

To handle model-specific quirks (such as `gemini-3-5-flash-lite` requiring explicit warnings against synthetic HTML element tag names in DOM dumps) without hardcoding any text strings or model names in Java code, Neodymium AI automatically resolves model prompt add-ons dynamically from disk and classpath:

1. **Resolution Directory Layout**:
   Model-specific system prompt add-ons are placed in the `ai-prompts/models/<cleanModel>/` directory on the classpath (or filesystem `config/ai-prompts/models/<cleanModel>/`):
   - `config/ai-prompts/models/<cleanModel>/addon-<type>.md` (filesystem override for capability type, e.g. `addon-general.md`)
   - `config/ai-prompts/models/<cleanModel>/addon.md` (filesystem override default)
   - `ai-prompts/models/<cleanModel>/addon-<type>.md` (classpath resource for capability type)
   - `ai-prompts/models/<cleanModel>/addon.md` (classpath resource default)

   Where `<cleanModel>` is the active model name sanitized to lowercase alphanumeric kebab-case (e.g., `gemini-3.5-flash-lite` $\rightarrow$ `gemini-3-5-flash-lite`).

2. **Accumulation Flow**:
   Model add-ons serve as the foundational base layer (Layer 1), onto which playbook-level (Layer 2) and dataset-level (Layer 3) add-ons are appended.

3. **Example (`gemini-3-5-flash-lite`)**:
   File: `src/main/resources/ai-prompts/models/gemini-3-5-flash-lite/addon-general.md`
   ```markdown
   CRITICAL FOR LITE MODEL LOCATORS: DOM dump element tags represent real HTML tags 
   (<p>, <div>, <span>, <h1>, <button>, <input>, <link>). Never invent synthetic 
   tag names or pseudotags (such as 'text' or 'text:nth-of-type(N)') in locators. 
   If a target element lacks a direct class or id attribute, select its parent 
   element (e.g., 'div:has(...)') or set 'status' to 'ESCALATE' to request visual context.
   ```

### D. Multilingual Testing & Language Universality (`neodymium.ai.multilingual`)

When automated tests target localized applications (e.g. French, German, Japanese, Polish, Swedish, Spanish) or playbooks written in non-English natural languages, Neodymium AI provides a dedicated **Multilingual Testing Mode**.

1. **Token Cost Optimization**:
   Standard English test suites keep system prompts ultra-compact (saving ~50–80 tokens per step across CI runs). When multilingual mode is enabled, the prompt compiler dynamically injects **Language Universality** guidance into the relevant prompt pipelines without requiring separate translated system prompt files.

2. **Configuration (`neodymium.ai.multilingual`)**:
   - **Default**: `false`
   - **Configuration Methods**:
     - **Global Property**: In `neodymium.properties` or `ai.properties`:
       ```properties
       neodymium.ai.multilingual=true
       ```
     - **JVM Argument**: Passed dynamically at execution time:
       ```bash
       mvn test -Dneodymium.ai.multilingual=true
       ```
     - **Per-Test Setup**: Programmatically enabled in test setup:
       ```java
       Neodymium.getData().put("neodymium.ai.multilingual", "true");
       ```
     - **Per-Dataset Row**: Declared in JSON/CSV dataset files for specific localized rows:
       ```json
       {
         "locale": "ca_fr",
         "neodymium.ai.multilingual": "true"
       }
       ```

3. **Dynamic Prompt Injections by Capability**:
   - **PESAP (`pesap`)**: Instructs the pre-step analyzer that English splitting examples are illustrative only, applying identical splitting, context escalation, and non-splitting rules to equivalent phrasing in the target language while preserving the original natural language in generated sub-steps.
   - **Action Extraction (`general`)**: Directs the LLM to target localized button text, forms, labels, and links in the SUT DOM according to the active locale.
   - **Outcome Verification (`verification`)**: Verifies post-action outcomes against localized page content and currency/date formatting.

---

## 13. `@AiPlaybook` Path Resolution & Scoping Rules

The `@AiPlaybook` annotation configures the target playbook file for AI test execution and companion replay cache storage. Path resolution follows standard, deterministic Java resource rules:

### Path Resolution Rules

| Annotation Syntax | Resolution Strategy | Resolved Resource Path Example (Class: `org.neodymium.ai.live.AssertTest`, Method: `testOne`) |
| :--- | :--- | :--- |
| **No `@AiPlaybook`** / `@AiPlaybook` (empty) | **Package-Relative Default** | `org/neodymium/ai/live/AssertTest_testOne.yaml` |
| **`@AiPlaybook("custom.yaml")`** | **Package-Relative Custom Name** | `org/neodymium/ai/live/custom.yaml` |
| **`@AiPlaybook("sub/custom.yaml")`** | **Package-Relative Subdirectory** | `org/neodymium/ai/live/sub/custom.yaml` |
| **`@AiPlaybook("/playbooks/foo.yaml")`** | **Absolute Classpath Root** (leading `/`) | `playbooks/foo.yaml` |

### Scoping Rules

* **Class Level**: Declaring `@AiPlaybook` at the class level sets the default playbook for all test methods in that test class.
* **Method Level**: Declaring `@AiPlaybook` on a test method overrides any class-level annotation.
* **Programmatic Java Tests**: Test classes marked with `@NeodymiumAiTest` that execute steps programmatically via `runPlaybook(session, "...")` do not require `@AiPlaybook`. Replay recordings automatically use standard package-relative path resolution or absolute paths if specified.

### Multi-Dimensional Companion Recording Filenames

To prevent recording collisions across different test methods, datasets, or browser profiles referencing the same playbook YAML template, companion JSON recording files are constructed automatically using all active execution dimensions:

$$\text{RecordingPath} = \text{\{DirectoryPath\}} / \text{\{ClassName\}} \_\, \text{\{MethodName\}} [\_ \, \text{\{DataSet\}} ] [\_ \, \text{\{Browser\}}] \, . \text{json}$$

* **Class Name**: Identifies the test suite class (e.g. `VerlaGuestCheckoutIntegrationTest`).
* **Method Name**: Guarantees uniqueness for each test method (e.g. `testCheckoutLive`).
* **Dataset ID**: Appended when parameterized dataset execution is active (e.g. `_perfect`).
* **Browser Profile**: Appended when `@Browser("...")` annotation is present (e.g. `_Chrome_1500x1000`).

**Example Recording Path:**
`playbooks/integration/VerlaGuestCheckoutIntegrationTest_testCheckoutLive_perfect_Chrome_1500x1000.json`

---

## 14. Centralized Selenide Locator Resolver (`LocatorResolver`)

To support vendor-neutral prompt outputs and handle custom browser pseudo-selectors without failing W3C CSS parsing engines, Neodymium AI provides a centralized **`LocatorResolver`**:

### Concept & Scope
* **Selenium/Selenide Exclusive**: `LocatorResolver` is strictly used by the Selenide/Selenium execution layer (`ActionExecutor` and `SelenideElementFinder`) to translate target strings into W3C-compliant `org.openqa.selenium.By` locators.
* **Native Playwright Bypass**: Native Playwright execution drivers execute raw Playwright selector strings directly in Playwright's native JavaScript engine without routing through `LocatorResolver`.

### Supported Selector Translations

| Target Format | Translation Strategy | Resulting Selenium / Selenide Locator |
| :--- | :--- | :--- |
| **`text=Total Paid: $27.58`** | Playwright `text=` prefix | `Selectors.withText("Total Paid: $27.58")` |
| **`button:has-text('Total Paid')`** | Playwright `:has-text(...)` pseudo-selector | `By.xpath("//button[contains(normalize-space(.), 'Total Paid')]")` |
| **`span:contains('Shopping Cart')`** | jQuery/Playwright `:contains(...)` | `By.xpath("//span[contains(normalize-space(.), 'Shopping Cart')]")` |
| **`:text('Checkout')`** | Standalone `:text(...)` | `Selectors.withText("Checkout")` |
| **`neo-ref=c42`** | Neodymium automation reference ID | `By.cssSelector("[data-neo-ref='c42']")` |
| **`custom-card ::shadow .btn`** | Explicit Shadow DOM target | `Selectors.shadowCss(".btn", "custom-card")` |
| **`//button[@id='pay']`** | Standard XPath | `By.xpath("//button[@id='pay']")` |
| **`button#pay.primary`** | Standard W3C CSS | `By.cssSelector("button#pay.primary")` |

---

## 15. Multi-Dimensional Prompt Resolution Architecture

To optimize prompt engineering across different LLM providers (e.g. Gemini, GPT-4o, Claude 3.5) and target execution engines (e.g. Selenide/Selenium vs. Playwright), Neodymium AI implements **Multi-Dimensional Prompt Resolution** in `AiAgentPrompts`.

### Resolution Hierarchy

When a prompt template (e.g., `system-prompt-rules.md`) is requested for a given `ExecutionEngine` and `Model`, the framework evaluates classpath candidates in the following order (first match wins):

```
                       Prompt Request: "system-prompt-rules.md"
                                       │
                                       ▼
  1. Engine + Model Specific : ai-prompts/engines/{engine}/models/{model}/system-prompt-rules.md
                                       │
                                       ▼
  2. Engine Specific         : ai-prompts/engines/{engine}/system-prompt-rules.md
                                       │
                                       ▼
  3. Model Specific          : ai-prompts/models/{model}/system-prompt-rules.md
                                       │
                                       ▼
  4. Default Fallback        : ai-prompts/default/system-prompt-rules.md
                                       │
                                       ▼
  5. Direct Root Fallback    : ai-prompts/system-prompt-rules.md
```

### Plug-and-Play Snippet Substitution vs Full Copies

The framework supports two prompt tuning workflows:

1. **Plug-and-Play Snippet Injection (DRY Default)**:
   Base prompt templates remain shared across engines and include dynamic placeholders such as `{{ENGINE_LOCATOR_RULES}}`.
   * **Selenide Target** (`ai-prompts/engines/selenide/locator-rules.md`):
     > *"Target browser engine is W3C Selenium / Selenide. Generated CSS selectors MUST be valid W3C CSS level 3/4 selectors."*
   * **Playwright Target** (`ai-prompts/engines/playwright/locator-rules.md`):
     > *"Target browser engine is native Playwright. You MAY generate native Playwright pseudo-selectors such as `:has-text(...)`, `text=...`, or `:visible`."*

2. **Full Prompt Copy Overrides**:
   If a specific model or engine requires a fundamentally different prompt structure, a full prompt copy can be placed in `ai-prompts/engines/{engine}/system-prompt-rules.md`, overriding the default prompt entirely.

---

## 16. SSIM 64×64 Visual Verification & Candidate Playbook Persistence

To ensure fast visual verification, immunity against subpixel rendering noise, and persistent self-healing cache convergence, Neodymium AI implements **SSIM 64×64 Visual Matrix Verification** and **Universal Candidate Playbook Capture**.

### A. SSIM 64×64 Visual Matrix Verification
Rather than using lossy 17×16 perceptual bit-hashes, visual steps capture structural luminance matrices:
1. **Bilinear Downscaling**: Screenshots are downscaled to a $64 \times 64$ grid using `RenderingHints.VALUE_INTERPOLATION_BILINEAR`.
2. **8-bit Luminance Matrix**: Calculates a 4,096-byte luminance matrix (0..255 brightness per grid cell), serialized as a Base64 string in `step.setScreenshotHash()`.
3. **In-Memory SSIM Comparison**: During replay, Neodymium computes Mean SSIM ($0.0 \rightarrow 1.0$) across $8 \times 8$ local blocks in $< 0.05\text{ ms}$.
4. **Visual Match Gate**: Checks `ssimScore >= neodymium.ai.ssim.minScore` (default: `0.99`). If visual score passes, execution bypasses unnecessary LLM verification calls while staying immune to font anti-aliasing and subpixel noise.
5. **Temporal Inter-Frame Visual Stability Detection**:
   Rather than relying on arbitrary blind sleeps or premature frame comparisons against the baseline, Neodymium dynamically evaluates whether the live SUT has finished animating and reflowing by comparing consecutive frames against each other ($\text{SSIM}(\text{Frame}_t, \text{Frame}_{t-1})$):
   - **1-Second Frame Spacing**: Consecutive frame captures are spaced by at least $1000\text{ms}$ (`neodymium.ai.visual.stabilityIntervalMs`).
   - **Stability Quiescence Threshold**: When $\text{SSIM}(\text{Frame}_t, \text{Frame}_{t-1}) \ge 0.999$ (`neodymium.ai.visual.stabilityThreshold`), the DOM is considered visually quiescent and settled.
   - **5-Attempt Safety Cutoff**: Polling is capped at a maximum of 5 attempts (`neodymium.ai.visual.stabilityMaxAttempts`) to prevent infinite blocking on perpetual animations (such as looping spinners or video hero banners).
   - **Baseline & Replay Symmetry**: Used in both live recording (to record baselines from quiescent layouts) and replay playback (to settle before baseline evaluation).

```properties
# Minimum SSIM score (0.0 to 1.0) required for visual match gate approval against recorded baseline
neodymium.ai.ssim.minScore=0.99

# Polling interval in milliseconds between consecutive frame captures during visual stability detection (minimum: 1000ms)
neodymium.ai.visual.stabilityIntervalMs=1000

# Maximum number of attempts allowed for temporal inter-frame visual stability settling before proceeding
neodymium.ai.visual.stabilityMaxAttempts=5

# Minimum inter-frame SSIM threshold required to consider the SUT visually quiescent/settled
neodymium.ai.visual.stabilityThreshold=0.999
```


### B. Universal Candidate Playbook Capture & Staleness Detection
1. **All Execution Modes**: `PlaybookRecorder` collects candidate execution steps in memory across all modes (`LLM_RECORDING`, `FORCE_RECORDING`, `REPLAY_WITH_HEALING`, `REPLAY_STRICT`).
2. **Failure Protection**: If a test fails, the candidate playbook is discarded. The disk file remains 100% untouched.
3. **Success Write-Back**: If a test succeeds and steps were healed or updated, the candidate playbook replaces the disk file atomically. If 0 changes occurred on replay, disk writes are skipped.
4. **YAML Hash Invalidation**: Companion `.json` files store `sourceYamlHash` (SHA-256 of original `.yaml` playbook). On replay, if the source `.yaml` file has been modified, a staleness warning is logged.

---

## 17. Visual Root Cause Analysis (RCA) & Failure Diagnostics

When a test step fails during execution (in `LIVE`, `REPLAY_WITH_HEALING`, or `REPLAY_STRICT` mode), Neodymium AI automatically captures the final SUT page state and invokes the Vision LLM (`LlmCapability.VISION`) to generate a plain-English **Visual Root Cause Analysis (RCA)**.

### Concept & Logging
* **Post-Mortem Failure Diagnostic**: Visual RCA is strictly a diagnostic feature and does not perform self-healing or alter test execution flow. It attaches root cause explanations to Allure reports, telemetry sinks, and log files.
* **Detailed Logging**: System/User prompts and raw responses are output at `TRACE` log level (`Compiling prompt: VisualRcaPrompt`), while the final root cause diagnosis is emitted at `INFO` level:
  ```text
  INFO - 🚨 [Visual RCA Diagnosis]: The 'Submit Order' button is missing because the checkout page failed to populate payment methods due to an upstream API timeout.
  ```

### Configuration
Visual RCA can be optionally disabled (e.g., for token conservation or offline CI test runs):

```properties
# Enables or disables automatic Visual Root Cause Analysis (RCA) on step execution failures.
# Default is true. Set to false to bypass Visual RCA calls on step failure.
neodymium.ai.visualRca.enabled=true
```

---

## 18. Automatic Validated Locator Improver & Locator Quality Scoring

Neodymium AI automatically inspects target DOM elements during LLM step execution, generates candidate standard CSS locators (`#id`, `[data-testid='...']`, `[name='...']`, `[aria-label='...']`, `[placeholder='...']`), and validates them against live browser DOM invariants before upgrading saved playbook action locators.

### Quality Scoring Scale (0 to 10)

| Score | Locator Type | Examples |
| :---: | :--- | :--- |
| **10/10** | Unique ID or Test ID | `#purchase-btn`, `[data-testid='submit-order']`, `[data-test='checkout']` |
| **8/10** | Standard Attribute | `input[name='email']`, `[aria-label='Search']`, `[placeholder='Enter address']` |
| **6/10** | Single Clean Class | `.product-quick-add`, `.btn-primary` |
| **4/10** | Neodymium Fingerprint Tag | `[data-ai='xccaql7f']` |
| **2/10** | Complex Combinator / Deep Path | `header > div > form > input:nth-child(2)`, `//html/body/div[1]/input` |
| **0/10** | Volatile Dynamic ID / Invalid | `#v-btn-129481`, `#react-node-9941` |

### Validation & Safety Invariants
1. **Quality Score Upgrade Gate**: Candidate locators are upgraded ONLY if candidate quality score strictly exceeds the original locator score ($\text{CandidateScore} > \text{OriginalScore}$).
2. **Uniqueness Check**: The candidate locator must match **exactly 1 element** in the live DOM (`findElements().size() == 1`).
3. **Identity Check**: The element returned by the candidate locator must be the **exact same `WebElement` instance** (`matchedElement.equals(targetElement)`).
4. **Volatile ID Protection**: Ignores dynamic/framework auto-generated IDs using `VolatileIdDetector`.

### Configuration
The Locator Improver is enabled by default and can be configured via `neodymium.properties` or system properties:

```properties
# Enables or disables automatic live DOM locator upgrading and quality scoring for recorded playbooks.
# Default is true. Set to false for testing or disabling locator upgrades.
neodymium.ai.locatorImprover.enabled=true
```

---

## 19. Target Safeguarding & Full Escalation Flow

Neodymium AI enforces a multi-tier **Target Safeguarding & Escalation Pipeline** to prevent invalid, volatile, or hallucinated selectors (such as fake `#xc...` ID selectors or framework dynamic hashes) from executing or polluting recorded playbook files.

### Escalation & Safeguard Architecture

```mermaid
flowchart TD
    A["1. LLM Returns Action"] --> B{"ActionExtractionPrompt Safeguard"}
    B -->|"Illegal #xc... Selector / Volatile ID"| C["Throw ToLevelEscalationException"]
    B -->|"Valid Selector"| D["Execute Action against SUT"]
    
    C --> E["Escalate Context Level (MINIMAL -> LEAN -> STANDARD -> RICH -> VISUAL_RICH)"]
    E --> F["Capture Higher Context State"]
    F --> G["Re-prompt LLM with Higher Context"]
    
    D -->|"Action Execution Fails (Element Not Found)"| H["HealingRequiredException Handler"]
    H --> E
```

### Safeguard Rules & Pipeline Invariants

1. **Early Volatile ID Rejection (`ActionExtractionPrompt`):**
   - When the LLM parses an action, target locators are evaluated against `VolatileIdDetector` rules (including configured `neodymium.ai.dom.volatileIdPatterns` and framework invariant `^xc[a-z0-9_]+$`).
   - If an action returns an illegal `#xc...` ID selector or a volatile ID, `ActionExtractionPrompt` throws `ToLevelEscalationException` directly during response parsing.
   - **Result:** Context immediately escalates along the monotonic ladder (`MINIMAL` $\to$ `LEAN` $\to$ `STANDARD` $\to$ `RICH` $\to$ `VISUAL_RICH`), capturing higher context and re-prompting the LLM with richer state before an execution attempt is made.

2. **No Magic Selection Fallbacks (`SelenideElementFinder`):**
   - `SelenideElementFinder` restricts `data-ai` attribute matching strictly to explicit `[data-ai=...]` or `data-ai=` selectors.
   - If the LLM returns an invalid ID selector like `#xck520w4`, `SelenideElementFinder` queries `id="xck520w4"` directly on the HTML DOM, failing cleanly instead of silently rewriting the selector under the hood.

3. **Action Retry Context Escalation (`ExecuteActionsStep`):**
   - When an action execution fails on SUT (e.g. `Element not found`), `ExecuteActionsStep` catches the failure and automatically escalates `KEY_CURRENT_CONTEXT_LEVEL` to the next level (`MINIMAL` $\to$ `LEAN` $\to$ `STANDARD` $\to$ `RICH` $\to$ `VISUAL_RICH`), capturing state before re-querying the LLM.
   - **Result:** Monotonically escalates context with state re-capture at each level, ensuring the LLM receives full multimodal context to fix broken locators.

4. **Extensible TargetExecutor Capability Abstraction:**
   - Decoupled from specific driver implementations via `TargetExecutor.supportsLocatorImprovement()`.
   - Ensures DOM-specific locator improvement only runs for web browser executors (`SelenideTargetExecutor`, `PlaywrightTargetExecutor`), preserving clean architectural boundaries for non-DOM executors (`RestTargetExecutor`).

---

## 20. Multi-Candidate Locators & LLM Quality Judge

Neodymium AI features a **Selector Quality & Verification Architecture** to guarantee maximum locator stability, eliminate dynamic framework hashes, and provide detailed call-type token telemetry.

### A. Ranked Candidate Locators

For every extracted action, the primary LLM generates 2–3 candidate locators ranked by stability in `action.getCandidateLocators()`:
* **Candidate 1 (Primary)**: Unique standard `#id`, `name`, `data-test`, `data-testid`, or `aria-label`. `data-ai` attributes are **strictly forbidden** in Candidate 1.
* **Candidate 2 (Semantic Fallback)**: Clean semantic CSS class or standard attribute combination (e.g. `.btn-secondary[type='submit']`). `data-ai` attributes and dynamic CSS module hashes are **strictly forbidden**.
* **Candidate 3 (Stability Fallback)**: `[data-ai='...']` selector attribute provided in the DOM dump (strategy `DATA_AI`).

### B. External Quality Judge (`QualityJudgeStep` - Optional / Second Opinion)

When an independent "second opinion" model is desired:
* **Execution**: Executes `QualityJudgePrompt` passing the proposed primary action, candidate locators, and full DOM tree context.
* **Output**: Returns structured `QualityJudgeResult` JSON containing `judgment` (`APPROVED`, `REFINED`, `REJECTED`), `chosenLocator`, `chosenValue`, `isRegex`, `confidence`, and `reasoning`.
* **Configuration**:
  ```properties
  # Enables or disables the external LLM Quality Judge ("second opinion") step.
  neodymium.ai.judge.enabled=false

  # Execution mode for Quality Judge. Valid options: ON_AMBIGUITY (default), ALWAYS, ON_FAIL.
  neodymium.ai.judge.mode=ON_AMBIGUITY

  # Optional separate LLM provider and model for Quality Judge (e.g. Ollama / Llama 3)
  neodymium.ai.judge.provider=ollama
  neodymium.ai.judge.model=llama3
  ```

### C. Dynamic Candidate Prompt Injection & Detailed Judge Logging

1. **Dynamic Schema Injection**:
   To conserve tokens during standard execution, `ActionExtractionPrompt` appends the candidate locators generation rule (`CANDIDATE_LOCATORS_RULE`) **dynamically only when `neodymium.ai.judge.enabled=true`**. When the Quality Judge is disabled, the LLM prompt requests only the primary locator, keeping prompt and response tokens minimal.

2. **Ambiguity Triggering (`ON_AMBIGUITY`)**:
   Under `ON_AMBIGUITY` mode, the Quality Judge triggers when top candidate confidence scores are within $0.15$ of each other ($\text{Score}_1 - \text{Score}_2 < 0.15$), resolving locator ambiguity before executing on the browser.

3. **Transparent Debug Logging**:
   When `DEBUG` logging is enabled, `QualityJudgeStep` outputs:
   - Input candidate locators with their strategies and confidence scores.
   - The query instruction and target being evaluated.
   - The formatted, pretty-printed raw JSON response received from the Quality Judge model.
   - Trace-level logging for full compiled system and user prompts.

### D. Detailed Per-Call-Type Telemetry Breakdown

Test completion stats and log summaries report an exact breakdown of LLM calls and token usage across all four pipeline call types:

```text
🤖 LLM Calls & Tokens: 45 calls | 100,784 tokens (In: 95,584, Out: 5,200, Cached: 0)
  ├─ PESAP:             16 calls | 6,457 tokens (In: 6,269, Out: 188, Cached: 0)
  ├─ Action:            15 calls | 56,911 tokens (In: 53,194, Out: 3,717, Cached: 0)
  ├─ Judge:             14 calls | 37,416 tokens (In: 36,121, Out: 1,295, Cached: 0)
  └─ Verification:      0 calls | 0 tokens (In: 0, Out: 0, Cached: 0)
```

---

## 21. Playbook Annotations & Execution Patterns

Neodymium AI provides 8 distinct execution patterns for prompt execution, annotation-driven test methods, and hybrid Selenide debugging:

### A. Annotation-Driven Execution

#### 1. External File Playbooks (`@AiPlaybook`)
* **Purpose**: Specifies external playbook resource files located on the classpath.
* **Strict Boundary**: Only resolves external file resource paths. Does **not** accept inline text content.
* **Explicit Path**: `@AiPlaybook("/playbooks/integration/verla-search-demo.yaml")` resolves starting from the classpath root.
* **Relative Path**: `@AiPlaybook("verla-search-demo.yaml")` resolves relative to the test class package.
* **Convention Path**: `@AiPlaybook` without value resolves to `<package>/<TestClass>_<methodName>.yaml`.

#### 2. Inline Playbooks & Text Blocks (`@AiInlinePlaybook`)
* **Purpose**: Defines multi-line YAML playbooks (with `steps:` and `data:` sections) or step prompt text blocks directly on JUnit test methods in Java code.
* **Syntax Example**:
  ```java
  @AiMode(ExecutionMode.FORCE_RECORDING)
  @AiInlinePlaybook("""
      steps: |
        Open ${verla.url}/verla-perfect/index.html in the browser
        Locate the search input field and type '${searchTerm}' into it
        Press enter to submit search
      data:
        - testId: "default"
          searchTerm: "Minimalist"
      """)
  public void test3_AnnotationDrivenInlinePlaybookTextBlocks(final AiSession session)
  {
  }
  ```

---

### B. Programmatic & Debugging APIs

#### 3. Fully Programmatic Java Builder (`Playbook.builder()`)
Construct steps programmatically using `PlaybookStep` and `Playbook.builder()`:
```java
final Playbook playbook = Playbook.builder()
    .step("Open ${verla.url}/verla-perfect/index.html in the browser")
    .step("Locate the search input field and type '${searchTerm}' into it")
    .step("Press enter to submit search")
    .build();

try (final AiSession session = AiSession.selenide(ExecutionMode.FORCE_RECORDING))
{
    session.execute(playbook);
}
```

#### 4. Multiline Text Block String with Embedded YAML Data
Execute raw multiline text blocks containing embedded YAML `steps:` and `data:` sections via `session.execute(...)`:
```java
try (final AiSession session = AiSession.selenide(ExecutionMode.FORCE_RECORDING))
{
    session.execute("""
        steps: |
          Open ${verla.url}/verla-perfect/index.html in the browser
          Locate the search input field and type '${searchTerm}' into it
          Press enter to submit search

        data:
          - testId: "default"
            searchTerm: "Minimalist"
        """);
}
```

#### 5. Multiline Text Block String with `SessionData`
Execute text block prompt strings seeded with a programmatic `SessionData` container:
```java
final SessionData sessionData = new SessionData();
sessionData.set("searchTerm", "Minimalist");

try (final AiSession session = AiSession.selenide(ExecutionMode.FORCE_RECORDING))
{
    session.execute("""
        Open ${verla.url}/verla-perfect/index.html in the browser
        Locate the search input field and type '${searchTerm}' into it
        Press enter to submit search
        """, sessionData);
}
```

#### 6. Step-by-Step Java Statement Debugging
Execute single-statement prompts allowing standard IDE breakpoints on individual Java lines:
```java
try (final AiSession session = AiSession.selenide(ExecutionMode.FORCE_RECORDING))
{
    session.execute("Open ${verla.url}/verla-perfect/index.html in the browser");
    session.execute("Locate the search input field and type '${searchTerm}' into it");
    session.execute("Press enter to submit search");
}
```

#### 7. Hybrid Execution (Mixing Direct Selenide Commands & AI Steps)
Mix direct Java Selenide browser calls (`open`, `shouldBe`, `pressEnter`) seamlessly with AI prompt steps:
```java
open(Neodymium.getData().get("verla.url") + "/verla-perfect/index.html");
$("#search-input").shouldBe(visible);

try (final AiSession session = AiSession.selenide(ExecutionMode.FORCE_RECORDING))
{
    session.execute("Locate the search input field and type '${searchTerm}' into it");
}

$("#search-input").pressEnter();
```

---

## 22. In-Memory LLM Request Caching (`@AiLlmCache`)

Neodymium AI provides an in-memory key-value prompt response caching mechanism (`@AiLlmCache`) specifically designed to speed up test suites, eliminate LLM API costs during replay verification, and ensure fast, deterministic integration test execution.

```java
@Browser("Chrome_1500x1000")
@Tag("integration")
@AiLlmCache
@NeodymiumAiTest
public final class VerlaProgrammaticDemoTest
{
    @Test
    @AiLlmCache
    public void test1_FullyProgrammaticObjects() throws Exception
    {
        // First execution populates in-memory cache on MISS
    }

    @Test
    @AiLlmCache
    public void test2a_ProgrammaticTextBlockWithEmbeddedYamlData() throws Exception
    {
        // Subsequent execution with identical prompt hits cache instantly
    }
}
```

### Scoping & Lifecycle Rules

1. **Class Execution Scope (`@AiLlmCache` on test class)**:
   The cache persists for the entire test class run. Any test method annotated with `@AiLlmCache` inside the class shares and reuses identical prompt responses recorded during that test class run. The cache is automatically cleared when the class completes.
2. **Method-Only Scope (`@AiLlmCache` on `@Test` method without class annotation)**:
   The in-memory cache lives strictly for that single method execution. It is initialized before `beforeEach` and cleared immediately upon method completion in `afterEach`.
3. **No Cache (`@Test` method without `@AiLlmCache`)**:
   Caching is completely bypassed and live LLM execution is performed, even if the surrounding test class is annotated with `@AiLlmCache`.
4. **Programmatic & Annotation Support**:
   Both annotation-driven tests (`@AiPlaybook`, `@AiInlinePlaybook`) and programmatic `AiSession.selenide(...)` calls automatically inherit caching when `@AiLlmCache` is present on the executing thread stack via `LlmCacheHelper`.

### Human-Readable Logging & Telemetry

Cache keys are constructed directly from human-readable prompt instruction strings (not opaque hashes), making execution logs transparent and readable:

```text
21:00:47 [LLM Cache MISS] Prompt: "Open http://localhost:8542/verla-perfect/index.html in the browser" -> Executing live LLM provider
21:00:47 [LLM Cache HIT]  Prompt: "Open http://localhost:8542/verla-perfect/index.html in the browser" -> Returning cached response (gemini-3.5-flash-lite)
⚡ CallLlmStep replayed response from internal LLM cache for instruction: Open http://localhost:8542/verla-perfect/index.html in the browser
```

Test statistics summary reports report exact internal cache hits and cached token metrics:

```text
======== 📊 AI Step Execution Statistics ========
  Step 1: Open ${verla.url}/verla-perfect/index.html in the browser
      Mode:           LLM
      Duration:       267 ms
      Escalations:    0
      Context Levels: MINIMAL
      Actions:        1 (NAVIGATE)
      Standard Calls: 1 (Tokens: 0 in (1,459 cached) → 0 out)
=================================================
╔════════════════════════════════════════════════════════════════════════════════════
║ 🏁 TEST CASE COMPLETED: SUCCESS
║ ⏱️ Duration:            269 ms
║ 🎟️ Replays:             0
║ ⚡ Internal Cache Hits: 1 hits
║ 🤖 LLM Calls & Tokens:  1 calls | 1,459 tokens (In: 0, Out: 0, Cached: 1,459)
║   ├─ PESAP:             0 calls | 0 tokens (In: 0, Out: 0, Cached: 0)
║   ├─ Action:            1 calls | 1,459 tokens (In: 0, Out: 0, Cached: 1,459)
║   ├─ Judge:             0 calls | 0 tokens (In: 0, Out: 0, Cached: 0)
║   └─ Verification:      0 calls | 0 tokens (In: 0, Out: 0, Cached: 0)
╚════════════════════════════════════════════════════════════════════════════════════
```

---

## 23. Execution Data Access, Telemetry Metrics & Mode-Conditional Asserters

Neodymium AI provides programmatic access to session dataset variables, execution mode contexts, and telemetry metrics (such as LLM call counts, replayed steps, self-healing status, and token usage) directly from `AiSession` and `PlaybookRecording`.

### A. Session Data & Variable Management

Session data variables are stored in the thread-isolated `SessionData` container attached to `AiSession`:

```java
// Set dynamic variables during test case execution
session.setData("userEmail", "test@example.com");

// Retrieve variables (resolves dynamic layer, static dataset, System props, and Neodymium data)
Object user = session.getData("userEmail");
SessionData sessionData = session.getSessionData(); // or session.data()
```

### B. Execution Mode Querying

You can inspect the governing `ExecutionMode` (`LLM_ONLY`, `LLM_RECORDING`, `FORCE_RECORDING`, `REPLAY_WITH_HEALING`, `REPLAY_STRICT`) directly from `AiSession`, `PlaybookRecording`, or `ExecutionMetrics`:

```java
// Query execution mode directly
ExecutionMode mode = session.getExecutionMode();       // or recording.getExecutionMode()

// Mode query helper booleans available on Session, Recording, & Metrics:
boolean live      = session.isLive();           // FORCE_RECORDING, LLM_ONLY, LLM_RECORDING
boolean replay    = session.isReplay();         // REPLAY_STRICT, REPLAY_WITH_HEALING, LLM_RECORDING
boolean strict    = session.isStrictReplay();   // REPLAY_STRICT
boolean recording = session.isRecording();      // FORCE_RECORDING, LLM_RECORDING
boolean healing   = session.supportsHealing();  // REPLAY_WITH_HEALING
```

### C. Telemetry Metrics & Mode-Conditional Asserters (`verifyMetrics()`)

To validate execution invariants across different `@AiMode` parameterized test runs, `PlaybookRecording` provides a fluent `MetricsAsserter` with overloaded breakdown and range assertions:

```java
// Execute playbook and perform mode-conditional lambda validations:
session.execute(playbook)
    .verifyMetrics()
    .hasStepCount(12)
    .hasNoSoftFailures()
    .onLive(m -> m.hasStandardCalls(12).hasLlmCalls(12, 24))
    .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed())
    .onMode(ExecutionMode.REPLAY_WITH_HEALING, m -> {
        if (m.isHealed()) {
            m.hasLlmCalls();
        } else {
            m.hasNoLlmCalls().hasAllStepsReplayed();
        }
    });
```

#### Overloaded Range & Breakdown Assertions

`MetricsAsserter` supports exact counts (`hasLlmCalls(12)`) and inclusive ranges (`hasLlmCalls(12, 24)`):

```java
asserter
    .hasStepCount(12)                // exact step count
    .hasStepCount(10, 15)            // range [10, 15]
    .hasLlmCalls(12, 24)             // total LLM calls between 12 and 24
    .hasStandardCalls(12)            // exact standard action extraction calls
    .hasPesapCalls(0, 12)            // PESAP pre-step analysis calls
    .hasVerificationCalls(0)         // post-action verification calls
    .hasJudgeCalls(0)                // quality judge calls
    .hasInputTokens(1000, 5000)      // input tokens between 1000 and 5000
    .hasOutputTokens(200, 800)       // output tokens between 200 and 800
    .hasTotalTokens(1200, 5800)      // total tokens between 1200 and 5800
    .hasNoEscalations()              // asserts 0 context level escalations occurred
    .hasContextLevelCount(ContextLevel.MINIMAL, 12); // asserts ContextLevel.MINIMAL was used 12 times
```

#### Escalation & Context Level Usage Assertions

`MetricsAsserter` tracks step context level escalations and context level distribution (`ContextLevel.MINIMAL`, `LEAN`, `STANDARD`, `FULL`, `VISUAL`, `HINT`):

```java
asserter
    .hasNoEscalations()                              // asserts 0 escalations
    .hasEscalationCount(0)                           // exact escalation count
    .hasEscalationCount(0, 2)                        // range [0, 2]
    .hasContextLevelCount(ContextLevel.MINIMAL, 12)  // MINIMAL used 12 times
    .hasContextLevelCount(ContextLevel.LEAN, 0, 5)   // LEAN used between 0 and 5 times
    .hasContextLevelCount("MINIMAL", 12);            // String level overload
```

#### Automated Mode Invariants (`matchesModeExpectations()`)

For parameterized test methods running under multiple `@AiMode` configurations, `matchesModeExpectations()` automatically validates the correct telemetry invariants:

```java
session.execute(playbook)
    .verifyMetrics()
    .matchesModeExpectations();
```

* **In `REPLAY_STRICT`:** Asserts `llmCalls == 0`, `healedSteps == 0`, `replayedSteps == stepCount`, and `softFailedSteps == 0`.
* **In `FORCE_RECORDING` / `LLM_ONLY`:** Asserts `llmCalls > 0`, `replayedSteps == 0`, and `softFailedSteps == 0`.
* **In `REPLAY_WITH_HEALING`:** Asserts that if any step was healed, `healedStepCount > 0` and `llmCalls > 0` (for healed steps only), otherwise `llmCalls == 0`.

---

## 24. Token Budget Guard & Real-Time Limits

Neodymium AI supports real-time input (prompt) and output (completion) token budget limits per test run to prevent runaway LLM costs or infinite self-healing retry loops.

### A. Configuration Properties

Token budgets can be configured globally in `neodymium.properties`:

```properties
# Maximum input (prompt) token budget per test run (-1 = unlimited, default: -1)
neodymium.ai.tokenBudget.input=50000

# Maximum output (completion) token budget per test run (-1 = unlimited, default: -1)
neodymium.ai.tokenBudget.output=10000
```

### B. Annotation-Driven Token Budgets (`@AiContext`)

Token budgets can also be declared directly on test methods or test classes using the `@AiContext` annotation:

```java
@Test
@AiMode(ExecutionMode.LLM_ONLY)
@AiContext(tokenBudgetInput = 10000, tokenBudgetOutput = 2000)
public void testWithStrictTokenLimits()
{
    // Execution aborts immediately with TokenBudgetExceededException if token usage exceeds limits
}
```

### C. Real-Time Enforcement & Abort Behavior

- **`TokenBudgetGuard`**: An `ExecutionListener` registered automatically on every `AiSession`.
- **Event Monitoring**: Listens to `LlmResponseReceivedEvent` dispatches after each LLM provider call and tracks cumulative input and output tokens consumed during the test run.
- **Immediate Abort**: When cumulative input tokens exceed `neodymium.ai.tokenBudget.input` (or output tokens exceed `neodymium.ai.tokenBudget.output`), `TokenBudgetGuard` throws a `TokenBudgetExceededException`.
- **Bypasses Healing Loops**: `TokenBudgetExceededException` is treated as an unrecoverable failure by `StateMachineRunner`, immediately aborting the test without triggering retry loops or soft healing attempts.

### D. Token Verification Asserters (`verifyMetrics()`)

`MetricsAsserter` provides fluent assertion methods to validate input, output, and total token consumption during test runs:

```java
session.execute(playbook)
    .verifyMetrics()
    .hasInputTokens(1000, 5000)   // asserts input tokens fall within [1000, 5000]
    .hasOutputTokens(200, 800)    // asserts output tokens fall within [200, 800]
    .hasTotalTokens(1200, 5800);  // asserts total tokens fall within [1200, 5800]
```

---

## 25. Selector Syntax Classification & Element Finder Pipeline

To prevent structured locator strategies from accidentally searching literal DOM text or code blocks when elements are absent, the framework integrates `SelectorSyntaxChecker` and a streamlined 7-step resolution pipeline:

### Fast Offline Syntax Classifier (`SelectorSyntaxChecker`)
* **XPath Validation:** Uses JDK native `javax.xml.xpath.XPathFactory` to validate expression syntax for XPath expressions (`//...`, `xpath=...`).
* **CSS Validation:** Uses jsoup `QueryParser.parse` with UI state pseudo-class normalization (`:hover`, `:focus`, `:focus-visible`, `:active`) and combinator validation to reliably identify valid W3C CSS selectors.
* **Element Finder Protection:** In `SelenideElementFinder`, Strategy 6 (Text Content Searching) is strictly guarded by `SelectorSyntaxChecker.isCssSelector(clean)` and `SelectorSyntaxChecker.isXpathExpression(clean)`. Structured CSS or XPath locators will **never** fall through to literal DOM text or code block searches.

### Streamlined 7-Step Runtime Resolution Pipeline (`SelenideElementFinder`)
`SelenideElementFinder` executes a streamlined, deterministic 7-step dispatch without brittle Java-level regex pre-flight gates:
1. **Automation Reference ID:** Direct check for unique `[data-ai="..."]` or `#xc...` identifiers.
2. **Playwright Pseudo Translation:** Translates `:has-text(...)` and `text=...` via `LocatorResolver` into Selenide-compatible locators.
3. **XPath Expression:** Direct execution of XPath expressions (`//...`).
4. **W3C CSS Selector:** Resolution of standard CSS selectors via `LocatorResolver.resolve`.
5. **Link Text Matching:** Exact anchor tag link text matching (`By.linkText`).
6. **Semantic Text & ARIA Search:** Case-insensitive text and accessible name searching across interactive candidates.
7. **Bare Tag Name Fallback:** Direct fallback to HTML tag names (`button`, `input`).

### Mode-Scoped Selenide W3C Locator Constraints
When `ExecutionContext.KEY_TARGET_EXECUTOR` is operating in Selenide/WebDriver mode (`SelenideTargetExecutor`), `ActionExtractionPrompt` and `QualityJudgePrompt` append `SELENIDE_LOCATOR_RULE`:
* **W3C Standard CSS Compliance:** Forces the LLM to output standard W3C CSS selectors compatible with Selenium and Selenide.
* **Playwright Pseudo-Selector Ban:** Strictly forbids Playwright-specific pseudo-selectors (e.g., `:has-text(...)`, `:text(...)`, `:text-is(...)`, `:has(...)`) that cause Selenium driver runtime syntax exceptions.

---

## 26. Language-Agnostic `CONTINUE` Step Status & Multi-Stage Prelude Protocol

To support interactive instructions (such as clicking a search toggle button or expanding a dropdown menu to reveal hidden form inputs) **without relying on any hardcoded human language string matching in Java code**, the framework supports the `CONTINUE` step status protocol:

### AI Signal Protocol (`status: "CONTINUE"`)
* **Response Status Spectrum:** `SUCCESS` | `FAILED` | `ESCALATE` | `CONTINUE`
* **Prelude Action Execution:** When the LLM outputs `status: "CONTINUE"`, the pipeline executes the prelude actions (e.g. `CLICK .search-toggle`), captures the updated post-click SUT DOM state (where hidden inputs like `<input id="search-field">` are now visible), and triggers a continuation LLM call for the same active step.
* **100% Language Neutrality:** Because the LLM natively decodes instructions across all natural languages (English, German, French, Spanish, Japanese, etc.), Java code contains **zero** hardcoded human language string checks.
* **Unified Replay Cache Storage:** All sequential actions (`CLICK` $\rightarrow$ `TYPE` $\rightarrow$ `KEY_PRESS`) extracted across continuation calls are appended into the single `step.getActions()` list in the companion JSON file. During offline replay (`REPLAY_STRICT`), all recorded actions execute sequentially in a single pass without making any LLM calls, with Selenide automatically handling element visibility wait transitions.

---

## 27. Execution Timing Recording & Paced Replay Playback

To ensure faithful replay execution for asynchronous Single Page Applications (SPAs), HTMX/AJAX partial page updates, CSS micro-animations, and visual SSIM assertions, Neodymium AI records execution durations and delays into companion JSON recordings.

### A. Recorded Timing Fields in Companion JSON
* **`durationMs` (Action & Step Level):** Wall-clock duration in milliseconds spent executing the specific action or playbook step in the browser.
* **`delayMs` (Action & Step Level):** Inter-action or inter-step elapsed pause in milliseconds prior to execution during the recording phase.

### B. Configuration Options

| Property | Default | Description |
| :--- | :--- | :--- |
| `neodymium.ai.replay.useRecordedDelays` | `false` | When `true`, replay pauses proportionally to recorded delays between actions/steps. |
| `neodymium.ai.replay.delayScale` | `1.0` | Multiplier for recorded delays (e.g. `0.5` for 2x replay speed, `1.0` for real-time pacing). |
| `neodymium.ai.visual.postActionSettleMs` | `1000` | Minimum settling pause in milliseconds before capturing visual screenshots for SSIM baseline checks. |

### C. Adaptive Visual Settle in Replay
When executing visual verification steps (`(visual: full)` / SSIM comparison), replay honors `neodymium.ai.visual.postActionSettleMs` (and any recorded step delay) prior to capturing the screenshot. If transient repaint or dynamic animation occurs, a settle retry is automatically performed to guarantee stable visual comparisons.
