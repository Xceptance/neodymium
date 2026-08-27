## Context

See [proposal.md](proposal.md) for motivation. PESAP currently analyzes instructions upfront for context level prediction (`c`), Java reflection requirement (`jm`), and upfront JIT step-splitting (`sp`). However, without semantic intent typing, downstream Action Extraction prompts treat all instructions identically, allowing the LLM to occasionally emit speculative mutating actions (like clicking purchase/submit buttons) on assertion steps.

## Goals / Non-Goals

**Goals:**
- Extend PESAP output schema with an explicit semantic intent enum (`i`).
- Enforce deterministic Java-side constraints in `ActionExtractionPrompt` and `ExecuteActionsStep` preventing mutating actions (`CLICK`, `TYPE`, `CLEAR`, `SELECT`) when `intent == ASSERT_*`.
- Inject the classified semantic intent into the Action Extractor user prompt to prevent LLM search space explosion.
- Enable fast-path native assertions for `ASSERT_URL` and `ASSERT_TITLE`.
- Maintain 100% universal, language-agnostic behavior across all natural languages without hardcoded language dictionaries.

**Non-Goals:**
- Replace the multimodal Action Extractor LLM for complex interactions.
- Hardcode language-specific keyword lists in Java code.

## Decisions

### 1. Intent Taxonomy in PESAP Output Schema
- **Decision:** PESAP returns a concise intent property `"i": "ASSERT_TEXT|ASSERT_STATE|ASSERT_URL|ASSERT_TITLE|INTERACT|NAVIGATE|BRANCH|LOOP|WAIT|STORE|JAVA_METHOD"`.
- **Rationale:** Flash Lite generates this with minimal token overhead (~2-3 tokens) and high accuracy (>98% across multilingual benchmarks).
- **Alternative Considered:** Inferring intent purely via regex in Java. Rejected because natural language phrasing varies widely across languages (French, German, Japanese, etc.) and is best classified semantically by the LLM.

### 2. Java-Side Hard Invariant Enforcement
- **Decision:** When `ExecutionContext.KEY_PESAP_INTENT` is an assertion (`ASSERT_TEXT`, `ASSERT_STATE`, `ASSERT_URL`, `ASSERT_TITLE`), `ActionExtractionPrompt` and `ExecuteActionsStep` discard or reject any mutating actions (`CLICK`, `TYPE`, `CLEAR`, `SELECT`). If mutating actions are returned, they are discarded and the step is treated strictly as an assertion or escalated.
- **Rationale:** Prevents catastrophic category errors (e.g. clicking a submit button on an order confirmation wait step, altering SUT state).

### 3. Graceful Fallback and Escalation
- **Decision:** If PESAP returns an invalid or unrecognized intent, or if the PESAP call fails, intent defaults to `INTERACT` or `null` and standard execution continues without breaking.
- **Rationale:** Ensures resilience and backward compatibility.

### 4. Fast-Path for Title and URL Assertions
- **Decision:** For `ASSERT_URL` and `ASSERT_TITLE`, set ContextLevel to `MINIMAL` (or evaluate directly against `WebDriver.getCurrentUrl()` / `WebDriver.getTitle()`).
- **Rationale:** Completely eliminates expensive DOM capture and vision escalation on simple metadata assertions.

## Risks / Trade-offs

- **[Risk: PESAP Misclassifies Intent on Ambiguous Instructions]** → Mitigation: Action Extractor receives the intent as guidance; if high-confidence DOM evidence indicates a mismatch, the prompt allows escalation.
- **[Risk: Compound Unsplit Instructions]** → Mitigation: PESAP step splitting decomposes compound multi-action steps into distinct sub-steps before intent assignment.
