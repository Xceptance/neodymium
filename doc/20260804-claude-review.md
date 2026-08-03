# Neodymium AI (v2) — Full Feature, Code & Quality Review (Round 6)

**Reviewer:** Claude (Opus 5)
**Date:** 2026-08-04
**Scope:** `org.neodymium.ai.*` — 147 main files / 23,829 LOC, 141 test files / 22,858 LOC,
232 `@Test` methods. Branch `feat/neo-aura-ai-v2-redesign-cont`; the entire package is new
on this branch.
**Method:** Round-5 remediation verified against current source, then an independent review
of feature quality, architecture, code quality, and test quality. The hermetic unit suite was
executed to establish ground truth rather than relying on stored reports. All findings are
verified against source, not the README. File/line references are relative to the repository
root.

---

## Verdict

**The core idea is right and the execution is roughly 70% of the way there. It is not
shippable yet — not because of the architecture, but because the build is red, there is no
CI, and the YAML parser silently discards valid input.**

The central bet — *treat the LLM as a compiler, not an interpreter*; extract actions once,
cache them to a JSON companion, replay offline in milliseconds — is the correct architecture
for this problem and is genuinely well realized. Most "AI testing" tools call the model on
every run and are therefore slow, expensive, and non-deterministic. This design avoids all
three. It should be kept.

What undermines it is a consistent pattern: **features are wired at one site, documented as
universal, and never verified end to end.** That pattern is visible in the code, in the docs,
and across the five prior review rounds.

### Round-5 remediation: verified as genuinely fixed

`StateMachineRunner:132` now binds the execution context around the whole run loop,
`ExecuteActionsStep:291` restores the previous context instead of hard-nulling it, and all
four providers (Gemini, Mistral, Vertex, Mock) route through `LlmSanitizerHelper`. The B.1
masking gap is closed on the propagation axis. That was the right fix.

However, the same commit introduced a new ThreadLocal leak (finding 3), and two long-standing
residuals remain open (findings 7 and P3 items).

---

## P0 — Blocking

### 1. The YAML parser silently drops structured steps, producing a green test that executed nothing

`src/main/java/org/neodymium/ai/playbook/YamlPlaybookParser.java:356` — when a `steps:` list
item is a `Map`, the parser handles **only** `include:`. Any other map is discarded with no
error, no warning, no log.

```yaml
steps:
  - instruction: "Open demo store homepage"     # <- silently dropped
    actions:
      - type: "NAVIGATE"
        target: "http://localhost:8080"
```

This parses to **zero steps**. There is no empty-playbook validation anywhere in the package.
`StateMachineRunner.run()` then finds an empty stack, skips the loop, and sets
`success = true`.

**A user who writes a playbook in this shape gets a passing test that ran nothing.** For a
test framework this is the worst possible failure mode — worse than crashing.

This is not hypothetical. `YamlPlaybookParserTest.testParseValidYamlPlaybook` asserts exactly
this format and **fails right now**:

```
YamlPlaybookParserTest.testParseValidYamlPlaybook:60
  Should parse 2 top-level playbook steps. ==> expected: <2> but was: <0>
```

Either the structured format is intended and unimplemented, or it was dropped without updating
the test.

**Fix:** reject unknown step-map shapes loudly, and treat an empty playbook as an error.

### 2. No CI, and the default build runs live LLM tests

`.github/workflows/` does not exist. There is no `junit-platform.properties`, and `pom.xml`
has **no tag filtering at all** (checked for `excludedGroups`, `<groups>`, `excludeTags`,
`includeTags`).

The tags are therefore decorative:

| Tag | Test classes |
|---|---:|
| `@Tag("AuraIntegration")` | 69 |
| `@Tag("LiveAPI")` | 23 |
| `@Tag("LiveLlm")` | 12 |
| `@Tag("verla")` | 7 |
| `@Tag("integration")` | 7 |

Surefire includes `org/neodymium/**/*Test.java`, which matches `ClickIntegrationTest.java`.
A plain `mvn test` therefore attempts real LLM API calls over the network and fails with
`ConclusiveFailureException: LLM provider communication failed` — 108 of the errors in the
stored surefire reports are exactly this.

**Fix:** exclude `LiveAPI`/`LiveLlm` by default, provide an opt-in profile, and add a CI job.
Until that exists, nothing prevents round 7 from repeating rounds 1–6.

---

## P1 — High

### 3. `StateMachineRunner.run()` leaks the `ExecutionContext` ThreadLocal

`src/main/java/org/neodymium/ai/runner/StateMachineRunner.java:129-132` captures
`previousContext` and binds the new context — and the `finally` block at `:264` **never
restores it**. `previousContext` is dead on every path.

Introduced by the round-5 remediation. Consequences:

- The context (holding the session, DOM states, screenshots, attachments) stays pinned to the
  thread after the test ends — a retention leak across a reused thread pool.
- Any LLM dispatch outside a `run()` masks against a *stale* session's secrets, while the
  security warning added to `LlmSanitizerHelper` stays silent because the ThreadLocal is
  non-null.

Every other site added in that commit restores correctly; this one was missed.

**Fix:** one line in the existing `finally`.

### 4. `AiConfiguration` snapshots System properties at construction — runtime changes are ignored

`src/main/java/org/neodymium/ai/config/AiConfiguration.java:123-129` copies system properties
into a private `Properties` map at construction time. The singleton then caches that map
forever. `System.setProperty` / `System.clearProperty` after first touch has **no effect**.

Proven by a real, reproducible failure:

```
VertexAiLlamaProviderTest.testMissingApiKeyThrowsException:81
  Missing API key should throw IllegalArgumentException.
  ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
```

`testCapabilitiesDeclaration` sets `neodymium.ai.vertex.apiKey`, the singleton caches it, the
next test clears the system property, and the cached copy still returns the stale key — so no
exception is thrown. `VertexAiLlamaProviderTest` is the one provider test with **zero**
`resetInstance()` calls; `GeminiLlmProviderTest`, `MistralLlmProviderTest`, and
`LlmClientAndRegistryTest` all have them. The test passes in isolation and fails in a batch.

The broader risk is larger than the test. Mock integration tests set
`neodymium.ai.global.provider=mock` in `@BeforeAll`. That only works because
`reuseForks=false` gives a fresh JVM per test class. **The correctness of the mock test suite
currently depends on the surefire fork strategy.** Anyone setting `reuseForks=true` for speed
silently routes mock tests to live providers.

**Fix:** read system properties live rather than snapshotting them.

### 5. Unsanitized LLM `reasoning` is written to disk

`src/main/java/org/neodymium/ai/prompt/DefaultActionSanitizer.java:143-149` sanitizes
`target`, `values`, and `description` — then passes `rawAction.getReasoning()` through
**untouched** into the constructed `Action`.

`reasoning` is `@JsonProperty`-serialized into the companion JSON (confirmed present in 3 of
15 on-disk recordings under `src/test/resources/playbooks/integration/`), and those files are
committed to the repository. It is LLM-generated free text that routinely echoes the value it
just handled — e.g. *"Enter the password Hunter2 into the login field"*.

Five rounds hardened the outbound network path. The persistence path has the same exposure,
has never been reviewed, and unlike the network path it lands in git history.

**Fix:** sanitize `reasoning`, or drop it from serialization.

### 6. Global Selenide timeout mutation contradicts the documented thread isolation

`src/main/java/org/neodymium/ai/pipeline/steps/ExecuteActionsStep.java:228-245` mutates
`com.codeborne.selenide.Configuration.timeout` — a JVM-global static — to implement the
`(timeout: ...)` control tag, then restores it in a `finally`.

`AI-README_V2.md` §8 sells "Session-Centric Architecture & Thread Isolation … preventing
thread cross-talk" as a headline feature. This breaks it: thread A's `(timeout: 500)` applies
to thread B's concurrent actions, and `origTimeout` is read from the same racing global, so
the restore can write back another thread's value.

**Fix:** per-driver `SelenideConfig`, or drop the parallel-isolation claim from the README.

### 7. DOM masking is tested but not wired to production

`SanitizedPayload.sanitizedStateText()` is computed by `DefaultContextSanitizer` and asserted
in **three tests** (`DefaultContextSanitizerTest:53`, `SanitizersTest:103`, `:107`) — and has
**zero production consumers**. `LlmSanitizerHelper.toSanitizedRequest:80-87` uses only
`sanitizedPrompt()`.

So the test suite proves DOM secrets are masked, while in production the captured DOM is
masked only incidentally, where it happens to be inlined into the prompt string.

This is round 3's residual, still open, and it is the clearest instance of the pattern:
**the tests prove the component works, not that anything uses it.**

**Fix:** wire it, or delete it together with its tests.

---

## P2 — Medium

### 8. `DefaultActionSanitizer` blind substring replacement

`DefaultActionSanitizer.java:94` (and `:115`, `:134`) replaces any variable value of length
>= 4 across values, **targets**, and descriptions. A variable `qty=1000` rewrites every
occurrence of "1000" in the recording (prices, IDs, timestamps); a variable whose value is
`button` or `input` corrupts CSS selectors. The `length >= 4` gate added in round 3 is not a
meaningful bound.

**Fix:** restrict to `sensitive()` entries, or match on token boundaries.

### 9. Test coverage is inverted against risk

| Class | LOC | Dedicated test |
|---|---:|---|
| `PageAnalyzer` | 1822 | none (referenced in 1 test file) |
| `ExecuteActionsStep` | 1038 | none (referenced in 3) |
| `StateMachineRunner` | 599 | none (referenced in 2) |
| `LlmSanitizerHelper` | 112 | none (**referenced in 0**) |

Every *other* pipeline step has a dedicated test — `CallLlmStepTest`, `CaptureStateStepTest`,
`PostExecutionAuditStepTest`, `PrepareRetryStepTest`, `SemanticDivergenceAnalysisStepTest`,
`VerifyOutcomeStepTest`, `VisualRcaStepTest`. The step that actually executes actions, the
class that runs the loop, and the class that does DOM analysis do not. The security choke
point has no direct test, and nothing asserts its fail-closed behaviour.

This distribution is precisely inverted against risk, and it explains why findings 1, 3, and 7
survived five review rounds.

### 10. Two parallel warning channels

Two independent buckets exist:

- `"verificationWarnings"` — raw string key, 5 write sites (`VerifyOutcomeStep:226`, `:274`,
  `:332`, `ExecuteActionsStep:912`, `StateMachineRunner:225`), read by the console summary
  (`StateMachineRunner:370`) and `ExecutionAuditor:99`.
- `ExecutionContext.KEY_EXECUTION_WARNINGS` (`"executionWarnings"`) — written only by
  `NeodymiumAiRunner:727`/`:789`.

`SessionFinishedEvent` at `StateMachineRunner:268` dispatches the **latter**, so every
HUD/event listener receives an effectively empty list while all real warnings sit in the
former.

### 11. LLM call counters are wrong in the summary

`StateMachineRunner.java:361` reports verification calls as
`verificationUsage != null ? 1 : 0`. Ten verified steps are reported as 1 call. Cost
reporting is a selling point of this framework; it should be accurate.

### 12. `JavaMethodAction` reconfiguration race

`src/main/java/org/neodymium/ai/executor/selenide/plugins/JavaMethodAction.java:175` calls
`staticConfigurationClasses.clear()` inside the lock but never resets `configurationScanned`
to `false` before repopulating at `:213-227`. A concurrent thread reading the still-`true`
flag at `:163` sees an empty set mid-rebuild and fails with "no public method found".

The volatile piggyback on the read path is correct; the rebuild window is not.

### 13. Config load failures are silent

`AiConfiguration.java:172-175` swallows `IOException` with a bare comment. An unreadable or
malformed `credentials.properties` yields silent misconfiguration with no diagnostic.
`getProperty` (`:197`) additionally catches `Throwable`.

### 14. `LlmSanitizerHelper` fails open and is noisy

`LlmSanitizerHelper.java:53-59` warns and then dispatches the payload **unmasked**. For a
security control the default should be to fail closed. As written, the warning also fires on
every run with no sensitive session entries at all, so it will be tuned out.

---

## P3 — Low / cleanup

- **The `ExecutionContext` blackboard.** 166 `getTransientData()` accesses, 32
  `@SuppressWarnings("unchecked")`, and **18 raw string keys** coexisting with 30 typed
  `KEY_*` constants. Four of the literals are named `"KEY_CURRENT_STEP_FIRST_ACTION"`,
  `"KEY_CURRENT_STEP_RAW_INSTRUCTION"`, `"KEY_CURRENT_STEP_STATS"`,
  `"KEY_CURRENT_STEP_NO_REPLAY"` — they look like constants but are string literals. This is
  the single largest maintainability drag in the package, and the reason the context
  propagation bugs were hard to see.
- **Giant methods.** `mapPlaybookStepToPipelineStep` 401 lines, `serializeAXNode` 358,
  `NeodymiumAiRunner.beforeEach` 351, `provideTestTemplateInvocationContexts` 270,
  `VerifyOutcomeStep.executeInternal` 268, `StateMachineRunner.run` 202,
  `SelenideElementFinder.tryResolveCandidate` 208.
- **`schemaVersion` is dead, and so is its validator.** `PlaybookStep:130` defaults the field
  to `"2.0"` and it is never populated from YAML, so the `!version.startsWith("2.")` check at
  `NeodymiumAiRunner:717` can never fire. Round 3 flagged the field; round 4 added dead
  validation around it.
- **`new AiConfiguration()` residual** at `StateMachineRunner:94` — the one site the singleton
  migration missed.
- **Attachments and the system message are still unmasked** (`LlmSanitizerHelper:83`, `:81`).
- **`Pattern.compile` per action execution** at `ExecuteActionsStep:219`.
- **`ThreadDeath` in `instanceof` chains** (`StateMachineRunner:142`,
  `ExecuteActionsStep:254`) — deprecated for removal in modern Java.
- **Inconsistent RCA triggering.** `runVisualRca` runs only for `PipelineException`
  (`StateMachineRunner:253`); a raw `RuntimeException` rethrown at `:242` bypasses it, and the
  `ConclusiveFailureException` wrapper built at `:154` is discarded on that path.

---

## Feature & idea quality

### Genuinely good — keep as is

- **Playbook + companion recording** is the right primitive. The multi-dimensional recording
  filename (class × method × dataset × browser) is a well-judged detail that avoids a whole
  class of collisions.
- **Control tags are the best thing in the package.** `(bug)` with unexpected-success
  detection — failing the test when an expected bug *stops* reproducing — is a sophisticated
  idea not commonly seen elsewhere. `(optional)`, `(no-replay)`, `(no-healing)` compose
  cleanly. Stripping them before prompt assembly is exactly right.
- **AXTree-first with a coverage-ratio floor** falling back to LEAN DOM is a smart, cheap
  token optimization with a real justification for non-WCAG storefronts.
- **Capability-based routing** (PESAP / EXECUTION / VERIFICATION / VISION) with disk-resolved
  per-model prompt add-ons keeps model quirks out of Java. Good separation.
- **SSIM gate to skip LLM verification** is a sound cost lever.
- The `TargetExecutor` / `PlaybookResourceManager` / `PlaybookParser` interfaces are honestly
  decoupled, not decoupled-in-name-only.

### Where the feature story overreaches

- **Verification is advisory-only, by design.** Every semantic verification failure becomes a
  warning; the AI judge can never fail a test. Round 5 closed this as a deliberate product
  decision and the reasoning is sound — but `AI-README_V2.md` §5 sells it as "Post-Action AI
  Outcome Verification" without stating that it cannot fail anything. Its actual value is
  diagnostics, and the docs should say so.
- **Self-healing has no measured success rate.** It was inert for three rounds; it is wired
  now, but nothing tracks how often healing actually recovers a step versus burning a vision
  call before failing anyway. Without that number the cost cannot be justified, and a
  regression that makes it inert again would be invisible.
- **Replay keying is incomplete.** Recordings key on class/method/dataset/browser but not
  viewport, locale, or SUT version — so cross-environment replay silently leans on healing.
- **Doc-vs-reality drift is the most expensive debt here.** `AI-README_V2.md` is 358 lines of
  confident present-tense description covering DOM masking (not wired), structured YAML steps
  (silently dropped), and schema versioning (dead). For a library other teams adopt, that gap
  costs more trust than any single bug.

---

## Test quality summary

| Metric | Value |
|---|---:|
| Test files | 141 |
| `@Test` methods | 232 |
| Test LOC | 22,858 |
| `@Disabled` / `@Ignore` | 2 |
| Mockito usage | 0 (hand-rolled `MockLlmProvider` / `MockTargetExecutor`) |
| Hermetic unit run | **168 tests, 2 failures** |
| Stored full-suite report | 476 tests, 22 failures, 108 errors (mostly credential-gated live tests) |

Observations:

- The hand-rolled mock providers instead of Mockito are a reasonable, deliberate choice for
  this domain — the queued-response model matches how the pipeline consumes the LLM.
- The mock integration tests are decent quality: `ShadowDomSandboxMockTest` runs three
  execution modes (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`) and closes with
  a real Selenide assertion. The 51 test files with no Java-level assertions are AI playbook
  tests whose assertions live in YAML `ASSERT` steps — that is by design, not a gap.
- The two real gaps are coverage distribution (finding 9) and the absence of environment
  isolation (finding 4), not assertion density.

---

## Prioritized recommendations

| # | Recommendation | Prio | Effort |
|---|---|---|---|
| 1 | Make the parser strict — reject unknown step-map shapes, error on empty playbooks; then fix or delete `YamlPlaybookParserTest` | P0 | S |
| 2 | Add CI + `excludedGroups` for `LiveAPI`/`LiveLlm` so `mvn test` is hermetic and green | P0 | S |
| 3 | Restore the ThreadLocal in `StateMachineRunner.run()`'s `finally` | P1 | XS |
| 4 | Stop snapshotting system properties in `AiConfiguration` — read live | P1 | S |
| 5 | Sanitize `reasoning`, or drop it from serialization | P1 | S |
| 6 | Wire `sanitizedStateText`, or delete it and its tests | P1 | S |
| 7 | Per-driver Selenide timeout, or drop the parallel-isolation claim from the README | P1 | M |
| 8 | Dedicated tests for `ExecuteActionsStep`, `StateMachineRunner`, `LlmSanitizerHelper`, including a fail-closed masking assertion | P2 | M |
| 9 | Restrict `DefaultActionSanitizer` to sensitive entries or token boundaries | P2 | S |
| 10 | Collapse the two warning channels onto `KEY_EXECUTION_WARNINGS`; migrate the 18 raw string keys to constants | P2–P3 | mechanical |
| 11 | Reconcile `AI-README_V2.md` with what actually ships | P2 | doc-only |

---

## Process note

Five consecutive rounds each landed a fix at one site and reported it as universal — and
round 5's fix, which was architecturally the right one, introduced finding 3.

The common thread is that nothing in the build verifies these claims: no CI, no hermetic
default, and no tests on the three classes where the bugs live.

**Recommendations 1, 2, and 8 are worth more than the rest combined, because they are what
make round 7 unnecessary.**

---

## Team Verification, Decisions & Responses (2026-08-04)

### 1. Architectural & Testing Strategy Decisions

- **Definition of "Live" vs "Mock"**:
  In Neodymium AI, "Live" (e.g., `@Tag("LiveAPI")`) does **NOT** mean testing against a live production web application/system. It refers specifically to calling a real remote LLM provider (over the network, using real credentials) vs. calling an offline `MockLlmProvider`.
- **Tag Consolidation (`@Tag("LLM")`)**:
  All `@Tag("LiveAPI")` and `@Tag("LiveLlm")` annotations across the test suite are consolidated into a single unified tag: **`@Tag("LLM")`**.
- **Inclusion in Default Build (`mvn test`)**:
  Real LLM tests (`@Tag("LLM")`) **MUST remain enabled in the default `mvn test` execution**. They provide ~75% of feedback for concept correctness, prompt construction, and model integration. They will not be excluded from default builds.

### 2. Status of Findings & Action Plan

| # | Finding / Area | Decision & Resolution Plan | Status |
|---|---|---|---|
| **1** | `YamlPlaybookParser` drops step maps | **Accepted**: Update `YamlPlaybookParser.java` to support structured step maps (`instruction:`, `actions:`) and fail on unexpected step formats. Fix `YamlPlaybookParserTest`. | Planned |
| **2** | Test Tagging & CI | **Clarified**: Replaced `@Tag("LiveAPI")` / `@Tag("LiveLlm")` with `@Tag("LLM")`. Retained in default `mvn test`. | In Progress |
| **3** | ThreadLocal Leak in `StateMachineRunner` | **Verified Resolved**: `ExecutionContext.setActiveContext(previousContext)` is already present in `StateMachineRunner.java:277` inside a `finally` block in current branch commits. | Resolved |
| **4** | `AiConfiguration` System Prop Snapshot | **Accepted**: Modify `AiConfiguration.getProperty()` to check live `System.getProperty(key)` before cached map fallback. Resolves `VertexAiLlamaProviderTest`. | Planned |
| **5** | Unsanitized LLM `reasoning` written to JSON | **Accepted**: Update `DefaultActionSanitizer.java` to sanitize `rawAction.getReasoning()` before building `Action`. | Planned |
| **6** | Global `Selenide.Configuration.timeout` mutation | **Accepted**: Refactor custom timeout execution in `ExecuteActionsStep.java` to eliminate static global mutation race conditions. | Planned |
| **7** | DOM Masking (`sanitizedStateText`) unwired | **Accepted**: Wire `sanitizedStateText` from `SanitizedPayload` into outbound request payload construction in `LlmSanitizerHelper.java`. | Planned |
| **8** | Substring replacement in `DefaultActionSanitizer` | **Accepted**: Restrict replacement to sensitive entries or token boundary matching. | Planned |
| **10**| Warning channel duplication | **Accepted**: Consolidate warning channels onto `KEY_EXECUTION_WARNINGS`. | Planned |
| **11**| Verification call summary counter | **Accepted**: Correct counter logic in `StateMachineRunner` log summary. | Planned |

