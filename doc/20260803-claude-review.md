# Neodymium AI (v2) — Code Review

**Reviewer:** Claude (Opus 4.8)
**Date:** 2026-08-03
**Scope:** `org.neodymium.ai.*` (v2 redesign, branch `feat/neo-aura-ai-v2-redesign-cont`)
**Base intel:** Web test automation driven by natural language, with automatic browser
communication (Selenide), an LLM-less replay after a first live LLM run, and optional
auto-healing.

All findings below were verified against the source, not the README. File/line references
are relative to the repository root.

---

## 1. Overall opinion

This is an ambitious and, structurally, a **well-architected** framework.

The layering is genuinely good:

- `TargetExecutor` / `PlaybookResourceManager` / `PlaybookParser` interfaces cleanly
  decouple the pipeline from Selenide / REST / mock backends.
- The LLM layer is a capability-routed registry (`LlmRegistry` + `LlmProvider` +
  `LlmCapability`), so vision vs. text-only routing is data-driven.
- The execution engine is a continuation-style state machine (`StateMachineRunner` +
  `ExecutionContext` LIFO stack) with try/catch scopes for escalation and healing — a
  legitimately sophisticated design.
- Thread isolation is real: `ExecutionContext.activeContext` is a `ThreadLocal`, and the
  transient/recording maps are `ConcurrentHashMap`.
- `LocatorResolver.escapeXpath` even handles the tricky both-quotes case with a `concat()`
  fallback.
- Test breadth is real: ~1,290 `@Test` methods, ~5,000 assertions across 138 AI test
  files, plus per-action mock integration tests.

However, there is a recurring failure mode that the `@author AI-generated: Gemini` tags help
explain: **features that look complete end-to-end — config knobs, records, docs, dedicated
prompt classes — but are not actually wired through.** Several headline capabilities are
dead or inert. That is where remediation effort should concentrate.

The single most valuable process change: point focused QA at the question **"does this
config/feature actually change runtime behavior?"** across the board, because the dominant
bug class here is *plumbing that terminates before it reaches the thing it configures.*

---

## 2. Real bugs (behavior diverges from intent/docs)

### 2.1 Semantic-divergence self-healing is inert — its baseline is never captured

`SemanticDivergenceAnalysisStep` compares `currentStep.getBaselineState()` against the live
DOM (`src/main/java/org/neodymium/ai/pipeline/steps/SemanticDivergenceAnalysisStep.java:76`).
But `PlaybookStep.setBaselineState()`
(`src/main/java/org/neodymium/ai/model/PlaybookStep.java:590`) is **never called anywhere in
the codebase** (grep-confirmed) and is not populated during recording.

Consequence: `baselineState` is always `null`, the `!baselineState.equals(currentStateText)`
check at line 87 always falls through to the else branch, and the healing LLM is always told
*"No layout changes detected (states match or baseline unavailable)."* The documented
"compare expected vs. actual page state to guide healing" feature (README §12,
`SemanticDivergencePrompt`) does nothing.

**Fix:** capture the page source/DOM text into `baselineState` during the recording pass
(persist it on the step, or store a compact hash + text), or remove the feature and its
prompt class so the docs match reality.

### 2.2 Per-request `temperature` and `timeoutSeconds` are ignored by every provider

The pipeline computes `config.getTemperature(role)` / `getTimeoutSeconds(role)` and passes
them into `LlmRequest` (`src/main/java/org/neodymium/ai/client/LlmRequest.java:42`;
computed in `src/main/java/org/neodymium/ai/pipeline/steps/CallLlmStep.java:117`).

But every provider builds its model **once in the constructor** with hardcoded values and
never applies the request values:

- `src/main/java/org/neodymium/ai/client/GeminiLlmProvider.java:74` → `temperature(0.0)`,
  `timeout(180s)`
- `src/main/java/org/neodymium/ai/client/MistralLlmProvider.java:77` → `temperature(0.0)`
- `src/main/java/org/neodymium/ai/client/VertexAiLlamaProvider.java:97` → `temperature(0.0)`,
  `timeout(180s)`

Consequence: the entire per-role temperature/timeout config surface is dead
(`neodymium.ai.verification.temperature`, the 60s RCA/divergence timeouts, etc.). A single
model instance also means all capabilities (PESAP, VERIFICATION, VISION, …) share one
temperature.

**Fix:** either apply `request.temperature()` / `request.timeoutSeconds()` per call (build a
request-scoped model or use a provider API that accepts per-call params), or delete the
`LlmRequest` fields and the `getTemperature/getTimeoutSeconds` config so nothing pretends to
be configurable.

### 2.3 Recordings are written even when the test fails

`PlaybookRecorder.onEvent` serializes on `SessionFinishedEvent` **without checking
`isSuccess()`** (`src/main/java/org/neodymium/ai/recorder/PlaybookRecorder.java:80`), and that
event is dispatched from the `finally` block in `StateMachineRunner.run()` regardless of
outcome (`src/main/java/org/neodymium/ai/runner/StateMachineRunner.java:265`).

Because `FORCE_RECORDING` first *deletes* the existing companion
(`src/main/java/org/neodymium/ai/junit/NeodymiumAiRunner.java:745`), a run that fails halfway
leaves a **partial/broken companion JSON** on disk, which the next replay will consume.

**Fix:** gate the write on `SessionFinishedEvent.isSuccess()`, and write atomically
(temp file + rename) so a crash mid-write can't corrupt a good recording.

### 2.4 Environment-variable overrides silently fail for camelCase keys

`AiConfiguration.envToPropKey` does `key.toLowerCase().replace('_','.')`
(`src/main/java/org/neodymium/ai/config/AiConfiguration.java:99`). So
`NEODYMIUM_AI_EXECUTION_MODE` → `neodymium.ai.execution.mode`, which never matches the real
key `neodymium.ai.executionMode`. Same for `gemini.apiKey`, `timeoutSeconds`, etc.

Consequence: only all-lowercase single-segment keys (`neodymium.ai.model`, `.provider`,
`.temperature`) are settable via environment variables — a real papercut for CI/secrets.
Only the special-cased `GEMINI_API_KEY` fallback in `GeminiLlmProvider` saves the API key
case.

**Fix:** maintain an explicit env→property mapping table, or a case-insensitive property
lookup, so documented keys can actually be set from the environment.

---

## 3. Design / approach gaps

### 3.1 "Self-healing" never learns (no write-back)

`REPLAY_WITH_HEALING` is **not** a recording mode
(`ExecutionMode.isRecording()` = `LLM_RECORDING || FORCE_RECORDING`,
`src/main/java/org/neodymium/ai/config/ExecutionMode.java:73`), and `PlaybookRecorder` is only
registered when `isRecording()` (`src/main/java/org/neodymium/ai/junit/NeodymiumAiRunner.java:737`).

So when healing fixes a drifted step live, the healed actions are mutated in memory but
**never persisted**. Every subsequent replay re-diverges and re-pays the full healing LLM
cost; the cache never converges.

**Fix:** behind a flag (e.g. `neodymium.ai.healing.persist=true`), write the healed step
actions back into the companion JSON on success so healing amortizes.

### 3.2 Replay has no semantic safety net (state the tradeoff explicitly)

On replay, `VerifyOutcomeStep` returns early
(`src/main/java/org/neodymium/ai/pipeline/steps/VerifyOutcomeStep.java:90`), and even in live
mode it only ever appends *soft warnings* — semantic verification **can never fail a test**.

So replay correctness rests entirely on (a) selectors still resolving, and (b) for visual
steps only, a crude whole-page dHash. A behavior regression that keeps selectors valid and
the page visually similar **passes silently**.

This is defensible for a fast/cheap replay, but it must be made explicit to users: replay is
a *structural* regression check, not a *semantic* one. There is currently no opt-in "strict
verification that fails the test" mode.

**Fix:** add an opt-in strict mode where verification (or a subset of steps tagged e.g.
`(assert)`) can fail the run. Keep soft-by-default for speed.

### 3.3 The dHash visual gate is coarse

`ScreenshotHasher.computeDHash` downsamples a full-page screenshot to 17×16 with
`Graphics.drawImage` and **no smoothing hint** (effectively nearest-neighbor), then compares
with a **hardcoded** Hamming threshold `≤ 10`
(`src/main/java/org/neodymium/ai/util/ScreenshotHasher.java:80`;
threshold at `src/main/java/org/neodymium/ai/pipeline/steps/ExecuteActionsStep.java:670`).

For the one place visual state is the actual assertion, that's a blunt instrument, and the
threshold isn't configurable.

**Fix:** use bilinear/area-averaged downscaling
(`RenderingHints.VALUE_INTERPOLATION_BILINEAR`), and make the threshold a config property.

### 3.4 No LLM retry / backoff

Grep for `retry` / `maxRetries` / `backoff` in `org.neodymium.ai.client` returns nothing. A
single transient 429/503 throws `ConclusiveFailureException` and fails the test. For a
framework that fans out many LLM calls, that's a resilience hole.

**Fix:** configure provider `maxRetries` and/or wrap `provider.chat()` with bounded
exponential backoff on transient/HTTP 429/5xx errors.

### 3.5 `AiConfiguration` is re-parsed from scratch on every use

Its constructor reads up to five files and iterates *all* env vars and *all* system
properties (`src/main/java/org/neodymium/ai/config/AiConfiguration.java:56`), and it's
instantiated via `new AiConfiguration()` in **21 places**, including hot paths (every
`CallLlmStep`, every `VerifyOutcomeStep`, multiple times per step in `ExecuteActionsStep`).
No caching.

For a "milliseconds replay" goal, this is needless disk I/O and allocation on every step.

**Fix:** load once and cache (respecting thread-local Neodymium overrides at lookup time, not
load time), or make `AiConfiguration` a cached/shared instance.

### 3.6 `PrepareRetryStep` dismisses overlays with a hardcoded CSS guess

It hides `.modal, .overlay, .popup, [role="dialog"], .cookie-banner, #cookie-consent` via JS
(`src/main/java/org/neodymium/ai/pipeline/steps/PrepareRetryStep.java:69`). That both misses
modern obfuscated/hashed class names and can permanently `display:none` an element a later
step legitimately needs.

**Fix:** make the overlay-dismissal selector set configurable, and prefer scoped/temporary
handling over globally hiding elements for the rest of the test.

### 3.7 No coherence between the YAML and its JSON companion

In replay the runner resolves to the **JSON** companion and never re-reads the YAML
(`src/main/java/org/neodymium/ai/junit/NeodymiumAiRunner.java:626`). The companion carries no
schema version, no source-YAML hash, and no model/provider stamp (`PlaybookStep` has no such
fields).

So if someone edits the YAML (adds/reorders/removes steps) and runs replay, **the edits are
silently ignored** until they re-record.

**Fix:** stamp the companion with a schema version + a hash of the source YAML (+ model/
provider). On replay, if the YAML hash no longer matches, warn loudly (or fail) with
"recording out of date, re-record."

---

## 4. Maintainability smells

- **God-method.** `ExecuteActionsStep.mapPlaybookStepToPipelineStep`
  (`src/main/java/org/neodymium/ai/pipeline/steps/ExecuteActionsStep.java:386`) is ~480 lines
  returning a giant lambda that mixes PESAP LLM calls, dHash logic, stats bookkeeping, and
  pipeline assembly. `PageAnalyzer` is 1,822 lines. Both are hard to test in isolation.
  Extract the PESAP-split, dHash-gate, and try/catch-assembly concerns into named steps.

- **Stringly-typed context keys.** Half the transient keys are `ExecutionContext.KEY_*`
  constants; the other half are inline string literals that *look* like constants —
  `"KEY_CURRENT_STEP_STATS"`, `"playbook.flatSteps"`, `"pesap.alreadySplitSteps"`,
  `"verificationWarnings"`, `"KEY_CURRENT_STEP_RAW_INSTRUCTION"`. One typo silently returns
  `null`. Promote them all to constants.

- **Muddled `ExecutionMode` semantics.** `LLM_RECORDING` reports `isLive() && isRecording()
  && isReplay()` all true (`src/main/java/org/neodymium/ai/config/ExecutionMode.java:82`), but
  its `isReplay()` branch is unreachable because live mode always reloads the YAML (which has
  no recorded actions). Confusing dead state.

- **Lazy-default vs. raw reads of the mode key.** `KEY_EXECUTION_MODE` is read raw (nullable)
  at `ExecuteActionsStep:433` and `:493`, but lazily defaulted to `REPLAY_WITH_HEALING` at
  `:638`. If that key is ever unset (non-JUnit entry points), a step would run PESAP as
  "live" and then flip to replay — inconsistent. It's always set on the JUnit path today, so
  this is latent, not active.

- **Flag inheritance is inconsistent across (de)serialization.**
  `PlaybookStep.isOptional()/isBug()/…` walk the `parent` chain, but `parent` is
  `@JsonIgnore`/`transient`. PESAP split copies most flags to children but **omits
  `noReplay`** (`src/main/java/org/neodymium/ai/pipeline/steps/ExecuteActionsStep.java:585`).
  So inherited-flag behavior can differ between a live run (parent set) and a replay (parent
  null after load).

---

## 5. Missing features / approaches worth considering

- **Healing write-back / cache convergence** (see 3.1) — the natural completion of
  auto-healing.
- **Resilient recorded locators.** Each action records a single `target`. Consider recording
  a ranked set of fallback locators (id → data-testid → role+name → text → xpath) at record
  time, so replay can fail over locally before paying for LLM healing.
- **Opt-in strict semantic verification on replay** (see 3.2).
- **LLM retry / rate-limit handling** (see 3.4).
- **Recording provenance & invalidation** (see 3.7): schema version, source-YAML hash,
  model/provider stamp.
- **Config caching** (see 3.5).
- **Record-twice determinism check** — an optional mode that records a step twice and diffs
  the extracted actions to flag non-deterministic/brittle selectors before they're trusted.

---

## 6. Strengths (worth preserving)

- Clean interface-based decoupling (`TargetExecutor`, `PlaybookResourceManager`,
  `PlaybookParser`) with real alternative implementations (Selenide / REST / mock).
- Capability-routed LLM registry — vision vs. text routing is data-driven and pluggable.
- Genuine thread isolation (`ThreadLocal` active context, `ConcurrentHashMap` state).
- Correct, non-trivial XPath escaping (`LocatorResolver.escapeXpath`, including the
  both-quotes `concat()` fallback).
- The continuation/state-machine pipeline with try/catch scopes is a solid basis for
  escalation and healing.
- Thoughtful control-tag DSL (`(bug)`, `(optional)`, `(no-replay)`, `(no-healing)`,
  `(continue-on-error)`, `(timeout:)`) that maps to real-world flaky/known-bug scenarios.
- Token accounting and per-step stats are tracked throughout.
- Large, fast, deterministic mock-based test suite plus live integration tests.

---

## 7. Prioritized recommendations

| # | Priority | Item | Why |
|---|----------|------|-----|
| 1 | **High** | Gate recording on success + atomic write (2.3) | Silent cache corruption is the most dangerous failure. |
| 2 | **High** | Capture `baselineState` during recording, or delete the divergence feature (2.1) | Dead marquee features erode trust in the whole system. |
| 3 | **High** | Apply per-request `temperature`/`timeout`, or delete the knobs (2.2) | Config that silently does nothing is a debugging trap. |
| 4 | **High** | Define replay's contract: strict verification mode + persist healed actions (3.1, 3.2) | Today replay can't catch regressions and can't converge. |
| 5 | Medium | Cache `AiConfiguration` (3.5) + add LLM retry (3.4) | Cheap wins for speed and resilience. |
| 6 | Medium | Fix env-var key mapping (2.4) | Unblocks CI/secrets configuration. |
| 7 | Medium | Companion versioning + YAML hash invalidation (3.7) | Prevents a nasty "edited YAML ignored on replay" footgun. |
| 8 | Low | Improve dHash downscale + configurable threshold (3.3) | Better visual assertion fidelity. |
| 9 | Low | Refactor the `ExecuteActionsStep` god-method; promote string keys to constants (4) | Testability and typo-safety. |

**Net:** the architecture is sound and the ideas (control-tag DSL, PESAP splitting,
capability routing, record/replay) are good. The gap is **end-to-end wiring and honest
failure semantics** — exactly the seams where AI-generated scaffolding tends to look finished
without being finished.
