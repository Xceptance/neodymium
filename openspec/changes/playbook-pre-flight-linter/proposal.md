## Why

Playbook instructions can suffer from language ambiguity, lack of visual tagging, under-specified target anchors, or compound multi-action steps (e.g. `Open the country selector and click "${country}".`). In runtime environments, compound instructions exhibit non-deterministic splitting behavior: if runtime analysis (PESAP) decides not to split a multi-action step, action execution (`ExecuteActionsStep`) frequently halts after executing only the first action (such as opening the dropdown/modal), leaving the subsequent action unperformed and causing downstream failures. 

An upfront, single-batch linguistic pre-flight linter evaluates the entire scenario before step execution begins, producing non-blocking advisory findings and actionable rewrite suggestions in test reports without interrupting or breaking execution.

## What Changes

- Introduce the **Upfront Playbook Pre-Flight Linter** (`PlaybookLinter`) that runs once during session initialization before step execution and per-step PESAP pre-flight.
- Implement 4 linguistic pre-flight checks:
  1. `STEP_SPLITTING_CANDIDATE`: Detect compound multi-action steps and action+verification combinations to prevent runtime drop-offs.
  2. `MISSING_VISUAL_TAG`: Detect visual/layout descriptions lacking `(visual)` tags.
  3. `AMBIGUOUS_AFFORDANCE`: Detect element capability phrasing without explicit action or verification verbs.
  4. `VAGUE_TARGET`: Detect under-specified element target references.
- Add `LlmCapability.LINTER` capability enum and routing configuration (`neodymium.ai.linter.enabled`, `neodymium.ai.llm.linter.provider`, `neodymium.ai.llm.linter.model`).
- Integrate advisory findings into `ExecutionContext`, `TestExecutionReport`, and the human-readable Markdown test report.

## Capabilities

### New Capabilities
- `playbook-pre-flight-linter`: Covers upfront linguistic static analysis, compound step detection, advisory finding models, dedicated LLM linter routing, and report generation for playbook quality.

### Modified Capabilities
<!-- None: purely additive feature with no existing capability spec requirement modifications -->

## Impact

- **Affected Components:** `org.neodymium.ai.client` (LLM capabilities), `org.neodymium.ai.config` (AI configuration), `org.neodymium.ai.playbook.linter` (new linter package), `org.neodymium.ai.prompt` (linter prompt template & parser), `org.neodymium.ai.runner` (`StateMachineRunner` pre-flight hook), and `org.neodymium.ai.report` (`TestExecutionReport`, `MarkdownReportGenerator`).
- **Dependencies:** Uses existing LLM provider infrastructure (Gemini, Mistral, Vertex AI, etc.).
- **Compatibility:** Fully backward-compatible; disabled by default (`neodymium.ai.linter.enabled=false`), non-blocking/advisory only.
