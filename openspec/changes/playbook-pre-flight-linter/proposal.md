## Why

Playbook instructions can suffer from semantic ambiguity, lack of visual modality tagging, under-specified target anchors, weak or subjective verification criteria (e.g. *"Make sure the page looks good"*), dangling pronouns across steps, temporal sequence inversions, or compound multi-action steps (e.g. `Open the country selector and click "${country}".`). In runtime automated execution, compound instructions exhibit action drop-off risks: if runtime JIT analysis (PESAP) does not split a multi-action step, action execution frequently halts after executing only the first action (such as opening the dropdown/modal), leaving subsequent actions unperformed and causing downstream failures.

An upfront, single-batch linguistic pre-flight linter evaluates the entire scenario before step execution begins, producing non-blocking advisory findings, dual raw/resolved step representations, source line numbers, and actionable rewrite suggestions in test reports without interrupting or breaking execution.

## What Changes

- Introduce the **Upfront Playbook Pre-Flight Linter** (`PlaybookLinter`) that runs once during session initialization before step execution and per-step PESAP pre-flight.
- Implement 8 universal, domain-neutral semantic pre-flight checks:
  1. `STEP_SPLITTING_CANDIDATE`: Detect compound multi-action steps and action+verification combinations to prevent runtime action drop-offs.
  2. `MISSING_VISUAL_TAG`: Detect visual/layout descriptions lacking `(visual)` (viewport) or `(visual: full)` (full-page/scrolling) tags.
  3. `AMBIGUOUS_AFFORDANCE`: Detect element capability phrasing (*"allows to..."*, *"ermöglicht..."*) lacking explicit imperative action or verification verbs.
  4. `VAGUE_TARGET`: Detect under-specified element target references (*"Click the button"*) lacking container/section/label context.
  5. `VAGUE_VERIFICATION`: Detect subjective or non-verifiable test oracles (*"Make sure everything works"*, *"Ensure page is fine"*).
  6. `DANGLING_ANAPHORA`: Detect ambiguous pronouns or lost antecedents across steps (*"it"*, *"that one"*).
  7. `TEMPORAL_FLOW_ANOMALY`: Detect logical sequence inversions or operating on entities prior to opening/creating them.
  8. `HARDCODED_VOLATILE_DATA`: Detect hardcoded execution-time dynamic timestamps or IDs instead of parameterized variables (`${...}`).
- Support explicit scenario context grounding strictly via playbook YAML `description:` header or test `@Description("...")` annotation.
- Maintain dual instruction representations (raw template with `${...}` placeholders and resolved values with substituted test data) alongside source line numbers (`lineNumber`, `sourceFile`).
- Enforce strict natural language universality: evaluate instructions in any natural language (English, German, French, Japanese, Spanish, etc.) and generate rewrite suggestions in the exact same language as the original instruction.
- Enforce strict separation of concerns: Deterministic syntax parsing (e.g. `${...}` placeholder format, tag regex) remains in fast offline parsers (`StepLinter`), while `PlaybookLinter` handles semantic reasoning.
- Add `LlmCapability.LINTER` capability enum and routing configuration (`neodymium.ai.linter.enabled`, `neodymium.ai.llm.linter.provider`, `neodymium.ai.llm.linter.model`).
- Integrate advisory findings into `ExecutionContext`, `TestExecutionReport`, and the human-readable Markdown/HTML test reports.

## Capabilities

### New Capabilities
- `playbook-pre-flight-linter`: Covers upfront linguistic static analysis, compound step detection, test oracle precision, advisory finding models, dedicated LLM linter routing, and report generation for playbook quality.

### Modified Capabilities
<!-- None: purely additive feature with no existing capability spec requirement modifications -->

## Impact

- **Affected Components:** `org.neodymium.ai.client` (LLM capabilities), `org.neodymium.ai.config` (AI configuration), `org.neodymium.ai.playbook.linter` (new linter package), `org.neodymium.ai.prompt` (linter prompt template & parser), `org.neodymium.ai.runner` (`StateMachineRunner` pre-flight hook), and `org.neodymium.ai.report` (`TestExecutionReport`, `PreliminaryReportListener`, `MarkdownReportGenerator`).
- **Dependencies:** Uses existing LLM provider infrastructure (Gemini, Mistral, Vertex AI, etc.).
- **Compatibility:** Fully backward-compatible; disabled by default (`neodymium.ai.linter.enabled=false`), non-blocking/advisory only.
