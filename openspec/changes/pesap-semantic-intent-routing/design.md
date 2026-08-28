## Context

See [proposal.md](proposal.md) for motivation. PESAP currently analyzes instructions upfront for context level prediction (`c`), Java reflection requirement (`jm`), and upfront JIT step-splitting (`sp`). However, without semantic intent typing, downstream Action Extraction prompts treat all instructions identically, allowing the LLM to occasionally emit speculative mutating actions (like clicking purchase/submit buttons) on assertion steps, or selecting irrelevant element types when filling forms.

## Goals / Non-Goals

**Goals:**
- Extend PESAP output schema with an explicit balanced semantic intent enum (`i`).
- Enforce deterministic Java-side constraints in `ActionExtractionPrompt` and `ExecuteActionsStep` preventing mutating actions (`CLICK`, `TYPE`, `CLEAR`, `SELECT`) when `intent == ASSERT` or `ASSERT_METADATA`.
- Inject the classified semantic intent into the Action Extractor user prompt to narrow the LLM element search space (`TYPE` $\rightarrow$ inputs, `CLICK` $\rightarrow$ buttons/links, `SELECT` $\rightarrow$ dropdowns/radios).
- Enable fast-path native assertions for `ASSERT_METADATA` (URL and page title).
- Maintain 100% universal, language-agnostic behavior across all natural languages without hardcoded language dictionaries.

**Non-Goals:**
- Replace the multimodal Action Extractor LLM for complex DOM resolution.
- Hardcode language-specific keyword lists in Java code.

## Decisions

### 1. Balanced Intent Taxonomy in PESAP Output Schema
- **Decision:** PESAP returns a concise intent property:
  `"i": "ASSERT|ASSERT_METADATA|CLICK|TYPE|SELECT|HOVER_SCROLL|NAVIGATE|WAIT|STORE|BRANCH"`
- **Rationale:** 
  - Simplifies assertion types into two categories (`ASSERT` for elements/text, `ASSERT_METADATA` for browser state), avoiding fragmentation where downstream execution rules are identical.
  - Granularizes interactive operations into distinct UI actions (`CLICK`, `TYPE`, `SELECT`, `HOVER_SCROLL`), which directly instructs the Action Extractor on what HTML tags to target.
  - Flash Lite generates this with minimal token overhead (~2-3 tokens) and high multilingual accuracy (>98%).
- **Alternative Considered:** Single generic `INTERACT` bucket with 4 `ASSERT_*` sub-types. Rejected because it failed to provide actionable element targeting guidance to the Action Extractor and over-partitioned identical assertion mechanics.

### 2. Java-Side Hard Invariant Enforcement
- **Decision:** When `ExecutionContext.KEY_PESAP_INTENT` is `ASSERT` or `ASSERT_METADATA`, `ActionExtractionPrompt` and `ExecuteActionsStep` discard any mutating actions (`CLICK`, `TYPE`, `CLEAR`, `SELECT`). The step is strictly treated as an assertion or escalated to visual verification.
- **Rationale:** Prevents catastrophic category errors (e.g. clicking a submit button on an order confirmation wait step, altering SUT state).

### 3. Action Extraction Scope Guidance
- **Decision:** Inject `[SEMANTIC_INTENT] <INTENT>` into the execution context header of `ActionExtractionPrompt`.
  - `TYPE`: Focuses on `input`, `textarea`, `contenteditable`.
  - `CLICK`: Focuses on `<button>`, `<a>`, clickable elements.
  - `SELECT`: Focuses on `<select>`, dropdown options, radio buttons.
  - `ASSERT`: Focuses on verification checks without state modification.
  - `STORE`: Focuses on reading text/values into session variables.
- **Rationale:** Drastically reduces LLM search space and eliminates hallucinated clicks during data entry or verification.

### 4. Fast-Path for Browser Metadata Assertions (`ASSERT_METADATA`)
- **Decision:** For `ASSERT_METADATA`, clamp ContextLevel to `MINIMAL` (or evaluate directly against `WebDriver.getCurrentUrl()` / `WebDriver.getTitle()`).
- **Rationale:** Completely eliminates expensive DOM capture and vision escalation on simple page title and URL checks.

### 5. Graceful Fallback and Step Splitting Synergy
- **Decision:** If PESAP returns an invalid or unrecognized intent, intent defaults to `null` and standard execution continues safely. For compound instructions, PESAP step-splitting runs first so each atomic sub-step receives its own distinct intent.
- **Rationale:** Ensures resilience, backward compatibility, and proper intent assignment per atomic action.

## Risks / Trade-offs

- **[Risk: PESAP Misclassifies Intent on Ambiguous Instructions]** → Mitigation: If Action Extractor receives an assertion intent but determines that no assertion is viable, it escalates to the next context level with clear reasoning rather than failing silently.
- **[Risk: Compound Multi-Action Instructions]** → Mitigation: PESAP step splitting decomposes compound multi-action steps into distinct sub-steps prior to intent assignment, ensuring each sub-step has a single unambiguous intent.
