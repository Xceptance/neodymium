## Context

See [proposal.md](proposal.md) for motivation. Neodymium supports runtime per-step classification and JIT analysis (PESAP), as well as a local regex-based syntax checker (`StepLinter`). However, semantic and linguistic quality issues—such as compound instructions (e.g. `Open the country selector and click "${country}".`) where runtime execution risks dropping subsequent actions, passive affordance ambiguities, subjective test oracles, dangling relative pronouns, and temporal sequence inversions—benefit from an upfront, cross-lingual batch analysis before execution starts.

## Goals / Non-Goals

**Goals:**
- Provide a single-batch upfront LLM inspection of all scenario steps before the test execution loop begins.
- Detect 8 specific linguistic and semantic quality categories (`STEP_SPLITTING_CANDIDATE`, `MISSING_VISUAL_TAG`, `AMBIGUOUS_AFFORDANCE`, `VAGUE_TARGET`, `VAGUE_VERIFICATION`, `DANGLING_ANAPHORA`, `TEMPORAL_FLOW_ANOMALY`, `HARDCODED_VOLATILE_DATA`).
- Ground semantic evaluation using optional explicit scenario descriptions (playbook YAML `description:` or test `@Description("...")`).
- Track source line numbers and maintain dual representations (raw template instruction with `${...}` placeholders alongside resolved data-substituted instructions).
- Ensure strict natural language universality: analyze instructions in any language and generate rewrite suggestions in the exact original language of the step.
- Offer non-blocking advisory findings and suggested rewrites without failing tests.
- Support dedicated LLM provider and model configuration to allow cheap and fast models for linting.
- Integrate findings into execution context and test report generators.

**Non-Goals:**
- Reimplement deterministic syntax parsing for `${...}` placeholders or tag regexes in the LLM (handled deterministically by offline regex parsers).
- Automatically rewrite source YAML playbook files on disk during test execution.
- Infer synthetic context from class or method names.
- Replace runtime per-step perception, multimodal vision escalation, or the offline `StepLinter`.

## Decisions

### 1. Single Batch Scenario Evaluation vs. Per-Step Calls
- **Decision:** The linter formats all numbered instructions in the scenario into a single prompt and executes exactly **one** LLM call during session initialization.
- **Rationale:** Minimizes latency (1 network roundtrip vs N steps) and token overhead, while allowing the LLM to inspect the full scenario sequence for temporal consistency and antecedent references.

### 2. Advisory Findings vs. Test Blocking
- **Decision:** Pre-flight findings are logged at `INFO`/`WARN` level and rendered in the test execution report, but do not fail or abort the test run.
- **Rationale:** Authors should be advised of ambiguities and compound action risks without breaking existing suites or causing false-positive CI blockers.

### 3. Dedicated `LlmCapability.LINTER` & Configuration
- **Decision:** Introduce `LlmCapability.LINTER` and config keys `neodymium.ai.linter.enabled` (default `false`), `neodymium.ai.llm.linter.provider`, and `neodymium.ai.llm.linter.model`.
- **Rationale:** Allows users to route linting to lightweight, highly cost-effective models (e.g. `gemini-2.5-flash-lite`) without affecting primary agent executor models.

### 4. Language-Universal LLM Prompt Design
- **Decision:** The prompt instructions are in English, but explicitly instruct the LLM to analyze instructions written in any natural language (German, French, Japanese, etc.) and generate suggestions in the exact same language as the original instruction.
- **Rationale:** Keeps Neodymium strictly universal and domain/language-neutral.

### 5. Explicit Context Grounding (`description` only)
- **Decision:** Context is supplied to the linter prompt strictly from playbook YAML `description:` or test `@Description("...")` annotations.
- **Rationale:** Avoids noisy or misleading synthetic heuristics from test method names while allowing authors to ground complex flows.

### 6. Dual Step Views & Source Line Traceability
- **Decision:** `PlaybookLinterFinding` captures `lineNumber`, `sourceFile`, `rawInstruction`, `resolvedInstruction`, and suggested rewrites preserving `${...}` syntax.
- **Rationale:** Authors can pinpoint the exact line in their playbook file and understand how parameterized data affects execution.

### 7. Separation of Deterministic Syntax vs. Semantic Reasoning
- **Decision:** Deterministic syntax checks (e.g. `${...}` placeholder bracket matching, `(visual)` tag formatting) remain strictly in offline regex components (`StepLinter`). The LLM linter is exclusively used for linguistic, semantic, and assertion clarity analysis.

### 8. Visual Scope Differentiation (`(visual)` vs. `(visual: full)`)
- **Decision:** The linter evaluates whether visual descriptions describe viewport-local elements or whole-page/below-the-fold content (such as footers and page-spanning sections) and recommends the appropriate tag: `(visual)` vs `(visual: full)`.

## Risks / Trade-offs

- **[Risk: LLM Latency during Session Init]** → Mitigation: Feature is disabled by default; when enabled, single batch call adds only ~500ms–1s upfront; dedicated fast models can be specified.
- **[Risk: LLM Downtime or Malformed JSON]** → Mitigation: All linter calls are wrapped in `try-catch` with graceful degradation, logging warnings and allowing test execution to proceed unaffected.
- **[Risk: False-Positive Suggestions on Complex Steps]** → Mitigation: Findings are non-blocking advisory suggestions only; categories have strict detection definitions in the prompt.
