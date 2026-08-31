## Why

Playbook instructions can suffer from semantic ambiguity, lack of visual tagging, under-specified target anchors, weak or subjective verification criteria (e.g. *"Make sure the page looks good"*), or compound multi-action steps (e.g. `Open the country selector and click "${country}".`). In runtime environments, compound instructions exhibit non-deterministic splitting behavior: if runtime analysis (PESAP) decides not to split a multi-action step, action execution (`ExecuteActionsStep`) frequently halts after executing only the first action (such as opening the dropdown/modal), leaving the subsequent action unperformed and causing downstream failures.

An upfront, single-batch linguistic pre-flight linter evaluates the entire scenario before step execution begins, producing non-blocking advisory findings and actionable rewrite suggestions in test reports without interrupting or breaking execution.

## What Changes

- Introduce the **Upfront Playbook Pre-Flight Linter** (`PlaybookLinter`) that runs once during session initialization before step execution and per-step PESAP pre-flight.
- Implement 5 semantic and linguistic pre-flight checks:
  1. `STEP_SPLITTING_CANDIDATE`: Detect compound multi-action steps and action+verification combinations to prevent runtime action drop-offs.
  2. `MISSING_VISUAL_TAG`: Detect visual/layout descriptions lacking `(visual)` or `(visual: full)` tags, distinguishing between viewport and full-page layout scopes.
  3. `AMBIGUOUS_AFFORDANCE`: Detect element capability phrasing (*"allows to..."*, *"ermöglicht..."*) lacking explicit action or verification verbs.
  4. `VAGUE_TARGET`: Detect under-specified element target references (*"Click the button"*) lacking container/section context.
  5. `VAGUE_VERIFICATION`: Detect subjective or non-verifiable test oracles (*"Make sure everything works"*, *"Ensure page is fine"*).
- Enforce strict separation of concerns: Deterministic syntax parsing (e.g. `${...}` placeholder format, tag regex) remains in fast offline parsers (`StepLinter`), while `PlaybookLinter` handles semantic reasoning.
- Add `LlmCapability.LINTER` capability enum and routing configuration (`neodymium.ai.linter.enabled`, `neodymium.ai.llm.linter.provider`, `neodymium.ai.llm.linter.model`).
- Integrate advisory findings into `ExecutionContext`, `TestExecutionReport`, and the human-readable Markdown test report.

## Capabilities

### New Capabilities
- `playbook-pre-flight-linter`: Covers upfront linguistic static analysis, compound step detection, test oracle precision, advisory finding models, dedicated LLM linter routing, and report generation for playbook quality.

### Modified Capabilities
<!-- None: purely additive feature with no existing capability spec requirement modifications -->

## Impact

- **Affected Components:** `org.neodymium.ai.client` (LLM capabilities), `org.neodymium.ai.config` (AI configuration), `org.neodymium.ai.playbook.linter` (new linter package), `org.neodymium.ai.prompt` (linter prompt template & parser), `org.neodymium.ai.runner` (`StateMachineRunner` pre-flight hook), and `org.neodymium.ai.report` (`TestExecutionReport`, `MarkdownReportGenerator`).
- **Dependencies:** Uses existing LLM provider infrastructure (Gemini, Mistral, Vertex AI, etc.).
- **Compatibility:** Fully backward-compatible; disabled by default (`neodymium.ai.linter.enabled=false`), non-blocking/advisory only.
