## Why

Currently, the Neodymium AI Quality Judge acts as a single-shot, static reviewer ("second opinion") evaluating proposed web automation actions and candidate locators solely against static/truncated DOM text snapshots. 

This model has several critical limitations:
1. **Blind / Hallucinated Selectors**: If an LLM proposes a brittle selector (or one that happens to match an unintended element), the static judge cannot verify whether the selector actually resolves uniquely or targets the visible element in the live browser.
2. **Accidental One-Off Matches**: Some poor selectors (e.g. bare `.btn-primary` or volatile `#v-btn-123`) may accidentally succeed once during recording but fail on subsequent runs due to DOM changes or ambiguity.
3. **Lack of Feedback Loop**: If candidate locators are ambiguous or non-existent, the agent either guesses or immediately fails/retries during execution rather than deliberating with real SUT feedback prior to execution.

Transforming the Quality Judge into an **Interactive Deliberation Round with Live SUT Probing** bridges the gap between static LLM reasoning and live browser reality. By probing candidate selectors in real-time on the browser (checking match count, visibility, interactability, text matching, and bounding rectangles) and discussing the concrete findings with the Judge, Neodymium verifies locator quality *before* committing any state-changing action.

---

## What Changes

- **Driver-Agnostic Live SUT Probing Abstraction**:
  - Extend `TargetExecutor` with read-only probing methods (`supportsLocatorProbing()`, `probeLocators(...)`).
  - Introduce generic, immutable probing DTOs (`LocatorProbeResult`, `ProbeElementSummary`, `ProbeBoundingRect`) in `org.neodymium.ai.executor.probe`.
  - Extract purely standard W3C attributes and geometric dimensions (`isDisplayed`, `isEnabled`, `isSelected`, `rect.width > 0`, `rect.height > 0`, raw attributes map).
  - **Zero Hardcoded CSS/HTML Magic Classes**: Strictly avoid hardcoding framework-specific classes (like `.loading`, `.skeleton`, or `#spinner`). Telemetry is generic, allowing the LLM Judge's semantic reasoning to interpret element states.
  - Provide concrete driver implementations (`SelenideLocatorProber` for Selenide/WebDriver, `MockTargetExecutor` for testing), keeping future support open for Playwright, Appium, and REST.
  - Keep `QualityJudgeStep` and prompts 100% decoupled from browser driver dependencies (no Selenide/Selenium imports in pipeline steps).

- **Provider-Independent Cumulative Deliberation Protocol**:
  - Implement a multi-turn discussion loop in `QualityJudgeStep`.
  - Use a **Cumulative Structured Prompt** format where deliberation history (turn proposals, SUT probe telemetry, and previous critiques) is maintained in a single structured prompt payload, guaranteeing 100% compatibility across all LLM providers (Gemini, Llama, Mistral, Mock) without reliance on provider-specific stateful chat session APIs.
  - Support judgment states: `APPROVED` (consensus on winning locator), `REFINED` (consensus with adjusted selector/value), and `NEED_REFINEMENT` (requesting next turn with `refinedProposal`).

- **Fast-Path & Guardrails**:
  - If a candidate selector scores $10/10$ (gold standard ID/data-testid) and live SUT probing confirms `matchCount == 1`, `visible == true`, and matching inner text, the deliberation loop fast-paths in **Turn 1 (0 extra LLM turns)**.
  - Configurable hard turn limits (`neodymium.ai.judge.discussion.maxTurns=3`) prevent runaway loops.

- **Clean Recording Integrity**:
  - Only the single, verified winning primary selector is committed to the recorded playbook/companion JSON upon consensus. Self-healing handles runtime recovery on replay.

- **Optional with Recommended Status**:
  - Remains strictly opt-in (`neodymium.ai.judge.enabled=false` by default) so existing zero-config suites remain untouched.
  - Officially recommended in `ai.properties` template for high-accuracy recording/healing runs.
  - When enabled, `neodymium.ai.judge.mode=DISCUSSION` is the default active mode.

- **Full Observability & Token Metrics**:
  - Structured console logging for each discussion turn, displaying probed selectors, match counts, element summaries, Judge critique, and consensus status.
  - EventBus integration (`LlmRequestSentEvent` and `LlmResponseReceivedEvent` tagged with `"JUDGE_DISCUSSION"`).
  - Accurate accumulation of per-turn and total token metrics (`inputTokenCount`, `outputTokenCount`, `cachedTokenCount`) and timing in `ExecutionContext.KEY_JUDGE_TOKEN_USAGE` and `StateMachineRunner` summary boxes.

---

## Capabilities

### New Capabilities
- `interactive-quality-judge`: Multi-turn pre-flight deliberation between the LLM Judge and live SUT prober to evaluate, probe, and refine locators against real-time browser feedback before action execution.
- `driver-agnostic-sut-probing`: Generic, read-only locator inspection contract on `TargetExecutor` returning element uniqueness, visibility, text, attributes, and bounding rectangles without hardcoded CSS/HTML assumptions.

### Modified Capabilities
- `quality-judge-step`: Enhanced from single-shot evaluation to support interactive discussion mode (`neodymium.ai.judge.mode=DISCUSSION`).

---

## Impact

- **Affected Code**:
  - `org.neodymium.ai.executor.TargetExecutor`: Add `supportsLocatorProbing()` and `probeLocators(...)`.
  - `org.neodymium.ai.executor.probe.*`: New DTOs (`LocatorProbeResult`, `ProbeElementSummary`, `ProbeBoundingRect`).
  - `org.neodymium.ai.executor.selenide.SelenideLocatorProber`: WebDriver probing implementation.
  - `org.neodymium.ai.executor.selenide.SelenideTargetExecutor`: Implement locator probing.
  - `org.neodymium.ai.executor.MockTargetExecutor`: Add canned probe support for unit tests.
  - `org.neodymium.ai.prompt.QualityJudgePrompt`: Add cumulative discussion prompt builder and response parser.
  - `src/main/resources/ai-prompts/quality-judge-discussion-prompt.md`: Discussion prompt template.
  - `org.neodymium.ai.pipeline.steps.QualityJudgeStep`: Implement multi-turn deliberation loop, fast-path, and metrics tracking.
  - `org.neodymium.ai.config.AiConfiguration`: Add discussion configuration properties (`neodymium.ai.judge.mode`, `neodymium.ai.judge.discussion.maxTurns`, `neodymium.ai.judge.discussion.probeDepth`, `neodymium.ai.judge.discussion.fastPath`).
  - `org.neodymium.ai.runner.StateMachineRunner`: Verify discussion token metrics and timing formatting.
- **Non-Goals**:
  - Storing backup candidate lists in playbooks (delegated to self-healing).
  - Hardcoding magic CSS classes or framework-specific tags in Java code.
  - State-changing interactions during probing (probing is strictly read-only element inspection).
