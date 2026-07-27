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

---

## 3. Runtime Instruction Preparation
Before compiling prompts or sending request payloads to the LLM, the framework runs a dedicated instruction preparation helper. It dynamically strips all explicit control tags case-insensitively, including:
* `(no-replay)`
* `(bug)` / `(bug: ...)`
* `(continue-on-error)`
* `(no-healing)`
* `(optional)` / `(soft)`
* `(timeout: ...)`

This prevents internal execution instructions from polluting the natural language prompts sent to the LLM.

---

## 4. Pre-Step Split Analysis (PESAP)
To handle complex, compound, or ambiguous instructions, the pipeline executes a **Pre-Step Split Analysis (PESAP)** using the `LlmCapability.STEP_SPLITTING` capability:
* **Contextual Inputs**: The analysis receives the current step, the previously executed step's instruction (for flow context), and up to two subsequent steps' instructions.
* **JIT Upfront Step Splitting**: If a compound step (e.g. `"Search for shirt, select size L, and click Checkout"`) is identified, the LLM splits the instruction into distinct leaf sub-steps. These are instantiated dynamically as child `PlaybookStep` instances and pushed onto the execution stack.
* **JIT Context-Level Detection**: Rather than relying on static defaults, PESAP dynamically determines the optimal interaction mode (Context Level) required for the step:
  - `LEAN`: Basic DOM-only execution.
  - `VISUAL_LEAN`: DOM with screenshot captures.
  - `VISUAL`: Full visual screenshot representation.
  - `HINT`: Targeted visual hints.
* **Bypassing on Replay**: PESAP runs during live recording mode; replay runs skip this analysis and execute the already-split steps directly from the JSON companion.

---

## 5. Post-Action AI Outcome Verification
After executing the SUT actions for a step, the framework performs a **Post-Action Outcome Verification**:
* **Always Visual**: Regardless of the initial execution context level, the outcome verification always captures the SUT state at the `VISUAL` level to record baseline images and compute screenshot dHash baselines.
* **Semantic Verification Prompt**: Evaluates the natural language instruction against the final page DOM and screenshot using the `VerificationPrompt` template via the `LlmCapability.VERIFICATION` capability.
* **Soft Failures**: Verification failures do not immediately break the test. Instead, they are collected and reported as warnings at the end of the test case, allowing developers to inspect semantic discrepancies without crashing the automation flow.

---

## 6. Dynamic Variable Parameterization
Ensures recorded playbooks remain reusable across environment and data changes:
* **Resolution**: Resolves variables (e.g. `${username}`) at runtime before executing actions.
* **Masking & Sanitization**: Compares SUT actions against sensitive dataset keys (e.g., passwords or tokens) and dynamically masks/sanitizes them before recording.
* **Parameterization**: Automatically matches generated dynamic values (such as order numbers or generated URLs) back to their variable definitions, writing parameterized entries like `"${order.number}"` into the companion JSON instead of hardcoded session values.

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
* **`LlmProviderRegistry`**: Hosts registered providers for LLM capabilities (e.g., `TEXT_ONLY`, `VISION`, `VERIFICATION`, `STEP_SPLITTING`).
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
| **`PesapPrompt`** | `BeforeStep` / pre-step analysis | `STEP_SPLITTING` | Current instruction, previous instruction, next instructions. | Analyzes instruction flow to predict interaction `ContextLevel`, split compound instructions into sub-steps, and check if custom Java reflection methods are required. Outputs a structured JSON. |
| **`ActionExtractionPrompt`** | `CallLlmStep` / live action generation | `ACTIONS` | Current SUT DOM state, natural language instruction, step history. | Identifies the correct sequence of web automation actions (`CLICK`, `TYPE`, etc.) and CSS selectors to implement the instruction. Outputs structured JSON actions. |
| **`VerificationPrompt`** | `VerifyOutcomeStep` / post-action validation | `VERIFICATION` | Natural language instruction, executed actions, pre/post screenshots. | Acts as an objective AI judge, scoring the outcome on rubrics (`intentMatch`, `visualDelta`, `absenceOfErrors`). Outputs a structured `VerificationResult` JSON. |
| **`SemanticDivergencePrompt`** | `SemanticDivergenceAnalysisStep` / replay healing | `TEXT_ONLY` | Baseline page source, current page source. | Compares expected vs actual SUT page states during a replay cache divergence to generate a plain-English diff summary (e.g. `ID changed from checkout to pay-now`). |
| **`VisualRcaPrompt`** | `StateMachineRunner.runVisualRca` / final error debug | `VISION` | Failed instruction, error details, current page screenshot. | Diagnoses visual root causes on conclusive execution failures (e.g., overlapping elements, cookie popups). Publishes a `DiagnosticErrorEvent`. |

### B. Custom System Add-on Prompts

To tune the LLM's system instructions for specific environments, applications, or testing scenarios, custom system prompt add-ons can be declared dynamically:

1. **Resolution Precedence**:
   System prompt add-ons are resolved with the following priority (first match wins):
   1. **Test Dataset Layer**: Defined inside a dataset entry (e.g. `systemPromptAddon.general: "Prefer CSS selectors"`).
   2. **YAML Playbook Layer**: Defined at the top level of the YAML playbook file.
   3. **System/Environment Properties**: Defined in system properties or configuration files (e.g., `neodymium.properties`).

2. **Per-Capability Customization Keys**:
   Add-on keys can target a specific type of LLM prompt or apply generally to all prompts:
   - `systemPromptAddon` (or `systemPromptAddon.default`): Appends to all system prompts.
   - `systemPromptAddon.pesap`: Appends specifically to the `PesapPrompt`.
   - `systemPromptAddon.general`: Appends specifically to the `ActionExtractionPrompt`.
   - `systemPromptAddon.verification`: Appends specifically to the `VerificationPrompt`.
   - `systemPromptAddon.rca`: Appends specifically to the `VisualRcaPrompt`.
   - `systemPromptAddon.divergence`: Appends specifically to the `SemanticDivergencePrompt`.

3. **YAML Playbook Declaration Examples**:
   Add-ons can be specified at the top level of a YAML playbook in flat key format, nested map format, or plural map format:
   ```yaml
   # Flat key format
   systemPromptAddon.general: "Always look for button text first"

   # Nested map format
   systemPromptAddon:
     pesap: "Predict shorter execution timeouts"
     verification: "Be extremely strict about price format changes"
   ```

4. **Safety Limits & Adherence Enforcement**:
   To prevent custom prompt add-ons from diluting or overriding essential prompt instructions (such as JSON output schemas and capability rules), the following safety controls are enforced:
   * **Length Limit**: Any custom prompt add-on value must not exceed **2000 characters**. If it does, a validation check throws an `IllegalArgumentException` early.
   * **Adherence Enforcement Suffix**: When appending the custom add-on prompt, the compiler automatically appends a strict reminder suffix:
     `"CRITICAL REMINDER: The above rules are custom extensions for this test step. You MUST still strictly follow all JSON schema formatting rules, action capabilities, and output guidelines specified in the main system prompt above."`
     This prevents the LLM from generating invalid text/HTML outputs when guided by custom user rules.

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

### Global Resource Root Redirection & CI/CD Write Safety

By default, companion recording JSON files generated during `FORCE_RECORDING` runs are written to the compiled output directory (`target/test-classes/`) and synced to the local source resources directory (`src/test/resources/`).

In environments where writing to `src/` is prohibited or undesirable (e.g., CI/CD build pipelines, read-only containers, or ephemeral build agents), you can redirect the source recording sync output directory globally using the configuration property:

```properties
# Redirect source recording output to target/ (or another directory) instead of src/test/resources/
neodymium.ai.playbook.directory.global=target/ai-recordings/
```

This ensures that recording artifacts are saved safely within build output directories without attempting to mutate read-only source trees.

---

## 14. Accessibility Tree (AXTree) Density & Coverage Floor

To maximize speed and token efficiency, Neodymium AI defaults to using the browser's Accessibility Tree (`AXTREE` context level) during initial step execution. However, on custom web applications, non-WCAG storefronts, or dynamic frameworks that lack standard ARIA accessibility markup, the browser's accessibility tree can be sparse or incomplete.

To prevent selector hallucination or missed elements on inaccessible pages, Neodymium AI calculates an in-memory **Accessibility Coverage Ratio**:

$$\text{Accessibility Coverage Ratio} = \frac{\text{Interactive Nodes in AXTree}}{\text{Interactive HTML Elements in LEAN DOM}}$$

### Concept & Mechanics
1. **Calculation**: During page context inspection, the framework counts interactive nodes in the Chrome DevTools Protocol (`Accessibility.getFullAXTree`) payload and compares them against interactive HTML elements (`a`, `button`, `input`, `select`, `textarea`, `[onclick]`, `[role]`) in the DOM.
2. **Performance Impact**: The calculation completes in **$< 0.1\text{ ms}$** via lightweight in-browser JS evaluation and in-memory line counting (zero additional HTTP or CDP roundtrips).
3. **Dynamic Context Floor**:
   - If PESAP predicts `AXTREE` for a step, but the page's calculated ratio falls below the configured threshold (`neodymium.ai.pesap.axtreeCoverageThreshold`, default: `0.30`), Neodymium AI automatically elevates the step's context level from `AXTREE` to **`LEAN`**.
   - This guarantees that the LLM receives the complete HTML DOM structure on inaccessible or non-standard pages without relying on a sparse accessibility tree.
   - If PESAP predicts a higher context level (`STANDARD`, `VISUAL_LEAN`, `VISUAL`), Neodymium AI respects PESAP's higher prediction.

### Configuration
```properties
# Threshold ratio (0.0 to 1.0) of AXTree interactive nodes to LEAN DOM elements.
# Default is 0.30 (30% coverage). Below this, context is elevated to LEAN.
neodymium.ai.pesap.axtreeCoverageThreshold=0.30
```



