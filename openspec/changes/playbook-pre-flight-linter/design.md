## Context

See [proposal.md](proposal.md) for motivation. Neodymium currently supports runtime per-step classification and JIT analysis (PESAP), as well as a local regex-based syntax checker (`StepLinter`). However, semantic and linguistic quality issues—such as compound instructions (e.g. `Open the country selector and click "${country}".`) where runtime execution (`ExecuteActionsStep`) risks dropping subsequent actions if JIT splitting does not trigger—require an upfront, cross-lingual batch analysis before execution starts.

## Goals / Non-Goals

**Goals:**
- Provide a single-batch upfront LLM inspection of all scenario steps before the test execution loop begins.
- Detect 4 specific linguistic and semantic quality categories (`STEP_SPLITTING_CANDIDATE`, `MISSING_VISUAL_TAG`, `AMBIGUOUS_AFFORDANCE`, `VAGUE_TARGET`).
- Offer non-blocking advisory findings and suggested rewrites without failing tests.
- Support dedicated LLM provider and model configuration to allow cheap and fast models for linting.
- Integrate findings into execution context and test report generators.

**Non-Goals:**
- Replace runtime per-step perception or multimodal vision escalation.
- Automatically rewrite source YAML playbook files on disk during test execution.
- Replace the offline regex-based `StepLinter` used for zero-token syntax validation.

## Decisions

### 1. Single Batch Scenario Evaluation vs. Per-Step Calls
- **Decision:** The linter formats all numbered instructions in the scenario into a single prompt and executes exactly **one** LLM call during session initialization.
- **Rationale:** Minimizes latency (1 network roundtrip vs N steps) and token overhead, while allowing the LLM to inspect the full scenario context.
- **Alternative Considered:** Calling the linter on each step during execution. Rejected due to runtime latency multiplication.

### 2. Advisory Findings vs. Test Blocking
- **Decision:** Pre-flight findings are logged at `INFO`/`WARN` level and rendered in the test execution report, but do not fail or abort the test run.
- **Rationale:** Authors should be advised of ambiguities and compound action risks without breaking existing suites or causing false-positive CI blockers.

### 3. Dedicated `LlmCapability.LINTER` & Configuration
- **Decision:** Introduce `LlmCapability.LINTER` and config keys `neodymium.ai.linter.enabled` (default `false`), `neodymium.ai.llm.linter.provider`, and `neodymium.ai.llm.linter.model`.
- **Rationale:** Allows users to route linting to lightweight, highly cost-effective models (e.g. `gemini-2.5-flash`) without affecting primary agent executor models.

### 4. Language-Agnostic LLM Prompt Design
- **Decision:** The prompt instructions are in English, but explicitly instruct the LLM to analyze instructions written in any natural language (German, French, Japanese, etc.) and generate suggestions in the exact same language as the original instruction.
- **Rationale:** Keeps Neodymium strictly universal and domain/language-neutral.

## Risks / Trade-offs

- **[Risk: LLM Latency during Session Init]** → Mitigation: Feature is disabled by default; when enabled, single batch call adds only ~500ms–1s upfront; dedicated fast models can be specified.
- **[Risk: LLM Downtime or Malformed JSON]** → Mitigation: All linter calls are wrapped in `try-catch` with graceful degradation, logging warnings and allowing test execution to proceed unaffected.
- **[Risk: False-Positive Suggestions on Complex Steps]** → Mitigation: Findings are non-blocking advisory suggestions only; categories have strict detection definitions in the prompt.
