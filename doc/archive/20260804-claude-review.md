# Neodymium AI (v2) — Full Feature, Code & Quality Review

**Reviewer:** Claude (Opus 5)
**Date:** 2026-08-04
**Reviewed commit:** `79cd37a1` (branch `feat/neo-aura-ai-v2-redesign-cont`)
**Scope:** `org.neodymium.ai.*` — 147 main files / 23,928 LOC, 145 test files / 23,340 LOC,
249 `@Test` methods. The entire package is new on this branch.
**Method:** Independent review of feature quality, architecture, code quality, and test
quality, plus verification of every finding from the prior round against current source. The
hermetic unit suite was executed to establish ground truth rather than relying on stored
reports. All findings are verified against source, not the README. File/line references are
relative to the repository root.

---

## Verdict

**The core idea is right, and this revision is materially better than the last one. It is
still not shippable — for one structural reason and one security reason.**

The central bet — *treat the LLM as a compiler, not an interpreter*; extract actions once,
cache them to a JSON companion, replay offline in milliseconds — is the correct architecture
for this problem and is genuinely well realized. Most "AI testing" tools call the model on
every run and are therefore slow, expensive, and non-deterministic. This design avoids all
three. It should be kept.

The last round fixed the two most serious defects properly: the YAML parser now rejects
malformed input instead of silently producing empty playbooks, and `AiConfiguration` now reads
system properties live instead of snapshotting them. Both fixes are correct, and the parser
fix in particular is exactly right — it parses the structured format, throws on unknown step
shapes, and throws on empty playbooks.

Two things block a release:

1. **The build has no CI and is red.** Two hermetic tests fail on `HEAD`, one of them red
   since `9f25ab34` — many commits ago, unnoticed, because nothing runs the suite.
2. **The new attachment secret masking does not mask anything**, and the test written to prove
   it works asserts against a data shape that production never produces.

That second point matters more than the bug itself. This is the seventh consecutive round in
which a security fix landed with the right architecture, a passing test, and no real effect.
The failure mode is no longer "we forgot a call site" — it is **"the test was built to match
the implementation instead of the producer."** Until the suite runs automatically and asserts
end to end, this will keep recurring.

---

## Status of prior findings

| # | Finding | Status |
|---|---|---|
| 1 | YAML parser silently drops structured steps | **Fixed** (`YamlPlaybookParser:385-424`) — partial residual, see N4 |
| 2 | No CI; default build runs live LLM tests | **Open** |
| 3 | `StateMachineRunner.run()` leaks the ThreadLocal | **Fixed** in `e9de4ca1` / `8c6b7b0b` |
| 4 | `AiConfiguration` snapshots system properties | **Fixed** (`AiConfiguration:194-208`) |
| 5 | Unsanitized `reasoning` written to disk | **Open** |
| 6 | Global Selenide timeout mutation | **Open** |
| 7 | `sanitizedStateText` dead in production | **Open** |
| 8 | `DefaultActionSanitizer` blind substring replacement | **Open** |
| 9 | Inverted test coverage on the largest classes | **Partially addressed** — dedicated tests now exist, but thin |
| 10 | Two parallel warning channels | **Open** |
| 11 | Verification call counter is wrong | **Open** |
| 12 | `JavaMethodAction` reconfiguration race | **Open** |
| 13 | Config load failures silently swallowed | **Open** |
| 14 | `LlmSanitizerHelper` fails open | **Open** (by design; see N1) |
| — | Attachment masking (was P3) | **Attempted, inert** — see N1 |

---

## P0 — Blocking

### N1. The new attachment secret masking is inert, and its test proves the wrong thing

`LlmSanitizerHelper.java:92-95` masks non-image attachments like this:

```java
for (final Map.Entry<String, String> entry : payload.maskToVariableMap().entrySet())
{
    decoded = decoded.replace(entry.getValue(), entry.getKey());
}
```

`maskToVariableMap` is produced by `DefaultContextSanitizer:70-71`:

```java
final String maskPlaceholder = "[MASKED_VAR_" + varKey + "]";
maskToVariableMap.put(maskPlaceholder, "${" + varKey + "}");   // key -> "${password}"
```

So `entry.getValue()` is **`"${password}"`**, not the raw secret. The attachment code therefore
executes `decoded.replace("${password}", "[MASKED_VAR_password]")` against a text attachment
that contains the **raw secret value**. Nothing matches. **The raw secret is Base64-re-encoded
unchanged and shipped to the provider.**

The map's real semantics are "placeholder → variable reference", which is correct for
`unmaskResponse:105-107` (`replace(key, value)`). The attachment loop is the one place that
reads it backwards.

**The test written to validate this asserts a map that production never produces.**
`LlmSanitizerHelperTest:118-122` hand-builds:

```java
Map.of("[MASKED_VAR_password]", "SecretValue123")   // value = raw secret
```

while **the same test class**, 20 lines earlier, asserts the production shape correctly:

```java
LlmSanitizerHelperTest:100
assertEquals("${password}", payload.maskToVariableMap().get("[MASKED_VAR_password]"));
```

The file contradicts itself, and the attachment path passes only under the fabricated version.

**Fix:** mask attachments against the raw sensitive values (`SessionData.getRawSensitiveData()`),
not against `maskToVariableMap`. Then assert end to end — build the payload with
`DefaultContextSanitizer`, never by hand.

### 2. No CI, and the default build runs live LLM tests

`.github/workflows/` does not exist. There is no `junit-platform.properties`, and `pom.xml`
contains **no tag filtering at all** (checked for `excludedGroups`, `<groups>`, `excludeTags`,
`includeTags`).

The tags are therefore decorative:

| Tag | Test classes |
|---|---:|
| `@Tag("AuraIntegration")` | 69 |
| `@Tag("LiveAPI")` | 23 |
| `@Tag("LiveLlm")` | 12 |
| `@Tag("verla")` | 7 |
| `@Tag("integration")` | 7 |

Surefire includes `org/neodymium/**/*Test.java`, which matches `ClickIntegrationTest.java`. A
plain `mvn test` therefore attempts real LLM API calls over the network and fails with
`ConclusiveFailureException: LLM provider communication failed`.

The direct consequence is finding N3 below: a test has been red for many commits and nobody
noticed. This is the single highest-leverage fix in this document — it is what makes round 8
unnecessary.

**Fix:** exclude `LiveAPI`/`LiveLlm` by default, add an opt-in profile, add a CI job.

---

## P1 — High

### N2. Assertion steps are never self-healed, even when the locator is the problem

`ExecuteActionsStep.java:280-284` routes **every** `ASSERT` failure to
`ConclusiveFailureException`:

```java
if ("ASSERT".equalsIgnoreCase(action.getType()))
{
    final Throwable finalCause = isAssertionFailure ? t : new AssertionError(failureMsg, t);
    throw new ConclusiveFailureException("Assertion failed: " + failureMsg, finalCause);
}
throw new HealingRequiredException(...);
```

This conflates two different failures:

- the assertion was **evaluated and failed** — a real SUT defect, correctly conclusive;
- the **element was not found** — a stale or hallucinated locator, which is exactly what
  self-healing exists for.

Because both take the conclusive branch, a broken selector on an assertion step aborts the
test instead of being healed — while the identical broken selector on a `CLICK` step is
healed. Assertions are a large fraction of any playbook, so this removes self-healing from a
large fraction of steps.

The code already computes the signal it needs. `isAssertionFailure` (`:268-278`) walks the
cause chain for `AssertionError`, and is then used only to pick the wrapped cause — never to
route.

This is a live test failure on `HEAD`, and the test encodes the correct intent:

```
RunnerIntegrationTest.testExecuteActionsStepAssertionErrorHandling:434
  Unexpected exception type thrown,
  expected: <HealingRequiredException> but was: <ConclusiveFailureException>
```

The fixture uses locator `text:nth-of-type(4)` — precisely the synthetic pseudo-tag that
`ai-prompts/models/gemini-3-5-flash-lite/addon-general.md` warns the model against
generating. This is the highest-value healing case, and it is the one that cannot heal.

**Fix:** route on `isAssertionFailure` — `AssertionError` conclusive, everything else healable.

### N3. A test of the largest class has been red for many commits, unnoticed

```
PageAnalyzerFrameAndAXTreeTest.captureSimplifiedDom_withCdpSupport_returnsSerializedAXTree:171
  expected: <true> but was: <false>
```

Line 171 asserts the AXTree serializer emits
`<button data-ai="xc_ax_ref_1001">Click Me</button>`. Neither `PageAnalyzer.java` (1822 LOC —
the largest file in the package) nor the test has been touched since `9f25ab34`, and the
failure is present in stored surefire reports from before the last two remediation rounds.

So the AXTree serialization path — the default context level, the one every step uses first —
has a broken contract test that has been red across at least eight commits. Nothing surfaced
it, because of finding 2.

**Fix:** fix the serializer or the fixture, then keep it green with CI.

### N4. The empty-playbook guard does not cover the JSON replay path

The new guard is correct but sits on only one of four return paths:

| Line | Return | Guarded |
|---|---|---|
| 180 | JSON companion, typed `List<PlaybookStep>` | no |
| 230 | JSON companion, action-list form | no |
| 241 | YAML parse | **yes** (`:236`) |
| 535 | JSON steps merged onto YAML playbook | no |

A companion JSON that deserializes to zero steps therefore still yields a silent empty
playbook, a skipped run loop, and `success = true` — the original P0 failure mode, on the path
taken by **every replay run**.

**Fix:** move the check to a single exit point, or apply it to all four.

### 5. Unsanitized LLM `reasoning` is written to disk

`DefaultActionSanitizer.java:143-149` sanitizes `target`, `values`, and `description` — then
passes `rawAction.getReasoning()` through **untouched** into the constructed `Action`.

`reasoning` is `@JsonProperty`-serialized into the companion JSON (confirmed present in on-disk
recordings under `src/test/resources/playbooks/integration/`), and those files are committed to
the repository. It is LLM-generated free text that routinely echoes the value it just handled —
e.g. *"Enter the password Hunter2 into the login field"*.

Seven rounds have hardened the outbound network path. The persistence path has the same
exposure, has never been reviewed, and unlike the network path it lands in git history.

**Fix:** sanitize `reasoning`, or drop it from serialization.

### 6. Global Selenide timeout mutation contradicts the documented thread isolation

`ExecuteActionsStep.java:228-245` mutates `com.codeborne.selenide.Configuration.timeout` — a
JVM-global static — to implement the `(timeout: ...)` control tag, then restores it in a
`finally`.

`AI-README_V2.md` §8 sells "Session-Centric Architecture & Thread Isolation … preventing thread
cross-talk" as a headline feature. This breaks it: thread A's `(timeout: 500)` applies to
thread B's concurrent actions, and `origTimeout` is read from the same racing global, so the
restore can write back another thread's value.

**Fix:** per-driver `SelenideConfig`, or drop the parallel-isolation claim from the README.

### 7. DOM masking is tested but still not wired to production

`SanitizedPayload.sanitizedStateText()` is computed by `DefaultContextSanitizer` and asserted in
three tests — and still has **zero production consumers** (grep-confirmed).
`LlmSanitizerHelper.toSanitizedRequest` uses only `sanitizedPrompt()`.

The test suite proves DOM secrets are masked; in production the captured DOM is masked only
incidentally, where it happens to be inlined into the prompt string. Open since round 3.

**Fix:** wire it, or delete it together with its tests. Same root cause as N1.

---

## P2 — Medium

### 8. `DefaultActionSanitizer` blind substring replacement

`DefaultActionSanitizer.java:94`, `:115`, `:134` replace any variable value of length >= 4
across values, **targets**, and descriptions. A variable `qty=1000` rewrites every occurrence
of "1000" in the recording (prices, IDs, timestamps); a variable whose value is `button` or
`input` corrupts CSS selectors. The `length >= 4` gate is not a meaningful bound.

**Fix:** restrict to `sensitive()` entries, or match on token boundaries.

### 9. Test coverage: the gap is closed on paper, thin in practice

The previously untested classes now have dedicated tests — real progress:

| Class | LOC | Test | Tests / LOC |
|---|---:|---|---|
| `PageAnalyzer` | 1822 | `PageAnalyzerTest` + `PageAnalyzerFrameAndAXTreeTest` | 2 + 2 / 48 + … |
| `ExecuteActionsStep` | 1038 | `ExecuteActionsStepTest` | 3 / 109 |
| `StateMachineRunner` | 599 | `StateMachineRunnerTest` | 2 / 106 |
| `LlmSanitizerHelper` | 112 | `LlmSanitizerHelperTest` | 7 / 155 |

`LlmSanitizerHelperTest` is genuinely good coverage for its size. The other three are thin
relative to the surface they guard — three tests do not cover a 1038-line step, and the
`PageAnalyzer` pair does not cover the 358-line `serializeAXNode` (see N3). "Risk-proportional"
is generous; the direction is right.

Note also that `LlmSanitizerHelperTest` is the class where N1 slipped through — coverage
counted, correctness did not.

### 10. Two parallel warning channels

- `"verificationWarnings"` — raw string key, 5 write sites (`VerifyOutcomeStep:226`, `:274`,
  `:332`, `ExecuteActionsStep:912`, `StateMachineRunner:225`), read by the console summary
  (`StateMachineRunner:370`) and `ExecutionAuditor:99`.
- `ExecutionContext.KEY_EXECUTION_WARNINGS` — written only by `NeodymiumAiRunner:727`/`:789`.

`SessionFinishedEvent` dispatches the **latter**, so every HUD/event listener receives an
effectively empty list while all real warnings sit in the former.

### 11. Verification call counter is wrong

`StateMachineRunner.java:368` reports verification calls as `verificationUsage != null ? 1 : 0`.
Ten verified steps are reported as 1 call. Cost reporting is a selling point of this framework;
it should be accurate.

### 12. `JavaMethodAction` reconfiguration race

`JavaMethodAction.java:175` calls `staticConfigurationClasses.clear()` inside the lock but never
resets `configurationScanned` to `false` before repopulating at `:213-227`. A concurrent thread
reading the still-`true` flag at `:163` sees an empty set mid-rebuild and fails with "no public
method found". The volatile piggyback on the read path is correct; the rebuild window is not.

### 13. Config load failures are silent

`AiConfiguration.java:163-177` swallows `IOException` with a bare comment. An unreadable or
malformed `credentials.properties` yields silent misconfiguration with no diagnostic.
`getProperty` additionally catches `Throwable`.

### 14. `LlmSanitizerHelper` fails open, and the test codifies it

`LlmSanitizerHelper.java:54-60` warns and then dispatches the payload **unmasked**. The test
`testSanitizeRequestWithoutActiveContextEmitsWarningAndPreservesPrompt` asserts the prompt is
preserved verbatim and never asserts that a warning was emitted — so the class javadoc's claim
of "fail-closed context state handling" is contradicted by its own test.

Fail-open may be the deliberate choice (a masking failure should not break a test run), but it
should be stated as such rather than described as fail-closed. The warning also fires on every
run with no sensitive entries at all, so it will be tuned out.

### 15. `getProperty` now performs two linear scans per miss

The round-7 fix (correct in substance) added a scan of `System.getProperties()` at
`AiConfiguration:199-207` **in addition to** the existing scan of `this.properties` at `:216-223`.
Every lookup that falls through to its default now walks both property sets. `getProperty` is
called on hot paths.

**Fix:** cache the normalized-key mapping instead of rescanning.

---

## P3 — Low / cleanup

- **The `ExecutionContext` blackboard.** 166 `getTransientData()` accesses, 32
  `@SuppressWarnings("unchecked")`, and **18 raw string keys** coexisting with 30 typed `KEY_*`
  constants. Four literals are named `"KEY_CURRENT_STEP_FIRST_ACTION"`,
  `"KEY_CURRENT_STEP_RAW_INSTRUCTION"`, `"KEY_CURRENT_STEP_STATS"`,
  `"KEY_CURRENT_STEP_NO_REPLAY"` — they look like constants but are string literals. This
  remains the largest maintainability drag in the package.
- **Giant methods.** `mapPlaybookStepToPipelineStep` 401 lines, `serializeAXNode` 358,
  `NeodymiumAiRunner.beforeEach` 351, `provideTestTemplateInvocationContexts` 270,
  `VerifyOutcomeStep.executeInternal` 268, `SelenideElementFinder.tryResolveCandidate` 208,
  `StateMachineRunner.run` 202.
- **`schemaVersion` is dead, and so is its validator.** `PlaybookStep:130` defaults the field to
  `"2.0"` and it is never populated from YAML, so the `!version.startsWith("2.")` check at
  `NeodymiumAiRunner:717` can never fire.
- **`new AiConfiguration()` residual** at `StateMachineRunner:94` — the one site the singleton
  migration missed.
- **`ObjectMapper` allocated per action** in the new parser branch
  (`YamlPlaybookParser:394`); hoist it.
- **`Pattern.compile` per action execution** at `ExecuteActionsStep:219`.
- **The system message is still unmasked** (`LlmSanitizerHelper:113`). Low risk — system prompts
  are static templates — but worth stating deliberately.
- **`ThreadDeath` in `instanceof` chains** (`StateMachineRunner:142`, `ExecuteActionsStep:254`) —
  deprecated for removal in modern Java.
- **Inconsistent RCA triggering.** `runVisualRca` runs only for `PipelineException`
  (`StateMachineRunner:253`); a raw `RuntimeException` rethrown at `:242` bypasses it, and the
  `ConclusiveFailureException` wrapper built at `:154` is discarded on that path.

---

## Feature & idea quality

### Genuinely good — keep as is

- **Playbook + companion recording** is the right primitive. The multi-dimensional recording
  filename (class × method × dataset × browser) is a well-judged detail that avoids a whole
  class of collisions.
- **Control tags are the best thing in the package.** `(bug)` with unexpected-success detection
  — failing the test when an expected bug *stops* reproducing — is a sophisticated idea not
  commonly seen elsewhere. `(optional)`, `(no-replay)`, `(no-healing)` compose cleanly.
  Stripping them before prompt assembly is exactly right.
- **AXTree-first with a coverage-ratio floor** falling back to LEAN DOM is a smart, cheap token
  optimization with a real justification for non-WCAG storefronts.
- **Capability-based routing** (PESAP / EXECUTION / VERIFICATION / VISION) with disk-resolved
  per-model prompt add-ons keeps model quirks out of Java. Good separation.
- **SSIM gate to skip LLM verification** is a sound cost lever.
- The `TargetExecutor` / `PlaybookResourceManager` / `PlaybookParser` interfaces are honestly
  decoupled, not decoupled-in-name-only.
- **New:** the parser now documents its four supported step formats in the class javadoc and
  enforces them at parse time. That is the right pattern — make the contract explicit, then
  fail loudly on violation. It should be the template for the rest of the package.

### Where the feature story overreaches

- **Verification is advisory-only, by design.** Every semantic verification failure becomes a
  warning; the AI judge can never fail a test. This was closed as a deliberate product decision
  and the reasoning is sound — but `AI-README_V2.md` §5 sells it as "Post-Action AI Outcome
  Verification" without stating that it cannot fail anything. Its actual value is diagnostics,
  and the docs should say so.
- **Self-healing has no measured success rate** — and per N2 it is switched off entirely for
  assertion steps. Nothing tracks how often healing recovers a step versus burning a vision call
  before failing anyway. Without that number the cost cannot be justified, and a regression that
  makes it inert again would be invisible.
- **Replay keying is incomplete.** Recordings key on class/method/dataset/browser but not
  viewport, locale, or SUT version — so cross-environment replay silently leans on healing.
- **Doc-vs-reality drift remains the most expensive debt.** `AI-README_V2.md` describes DOM
  masking (not wired), attachment masking (inert), and schema versioning (dead) in confident
  present tense. For a library other teams adopt, that gap costs more trust than any single bug.

---

## Test quality summary

| Metric | Value |
|---|---:|
| Test files | 145 |
| `@Test` methods | 249 |
| Test LOC | 23,340 |
| `@Disabled` / `@Ignore` | 2 |
| Mockito usage | 0 (hand-rolled `MockLlmProvider` / `MockTargetExecutor`) |
| **Hermetic run on `HEAD`** | **240 tests, 2 failures** (N2, N3) |

Observations:

- Test count rose from 232 to 249 and the two failures from the prior review
  (`YamlPlaybookParserTest`, `VertexAiLlamaProviderTest`) are both genuinely fixed. Real
  progress.
- The hand-rolled mock providers instead of Mockito remain a reasonable, deliberate choice —
  the queued-response model matches how the pipeline consumes the LLM.
- The mock integration tests are decent: `ShadowDomSandboxMockTest` runs three execution modes
  and closes with a real Selenide assertion. The test files with no Java-level assertions are AI
  playbook tests whose assertions live in YAML `ASSERT` steps — by design, not a gap.
- **The remaining weakness is not coverage, it is fixture fidelity.** N1 is the clearest case: a
  new, well-structured, seven-test suite passes while the feature it covers does nothing,
  because one fixture was hand-built to match the implementation rather than the producer.
  Tests that construct their own inputs for a data structure another class owns should build
  them through that class.

---

## Prioritized recommendations

| # | Recommendation | Prio | Effort |
|---|---|---|---|
| 1 | Mask attachments against raw sensitive values, not `maskToVariableMap`; rebuild the fixture through `DefaultContextSanitizer` | P0 | S |
| 2 | Add CI + `excludedGroups` for `LiveAPI`/`LiveLlm` so `mvn test` is hermetic and green | P0 | S |
| 3 | Route `ASSERT` failures on `isAssertionFailure` so broken locators stay healable | P1 | XS |
| 4 | Fix `PageAnalyzerFrameAndAXTreeTest` / the AXTree serializer | P1 | S |
| 5 | Apply the empty-playbook guard to all four parser return paths | P1 | XS |
| 6 | Sanitize `reasoning`, or drop it from serialization | P1 | S |
| 7 | Wire `sanitizedStateText`, or delete it and its tests | P1 | S |
| 8 | Per-driver Selenide timeout, or drop the parallel-isolation claim from the README | P1 | M |
| 9 | Restrict `DefaultActionSanitizer` to sensitive entries or token boundaries | P2 | S |
| 10 | Deepen `ExecuteActionsStepTest` / `PageAnalyzerTest` toward their actual surface | P2 | M |
| 11 | Collapse the two warning channels; migrate the 18 raw string keys to constants | P2–P3 | mechanical |
| 12 | Reconcile `AI-README_V2.md` with what actually ships | P2 | doc-only |

---

## Process note

Seven rounds in, the pattern has shifted but not broken. Earlier rounds landed a fix at one call
site and reported it as universal. This round landed two fixes correctly and completely — the
parser and the config — and then reproduced the old failure mode in a new form: a security
feature that is architecturally right, covered by a new test, and inert in production, because
the test fixture was written to match the implementation rather than the class that actually
produces the data.

Coverage is no longer the gap. **Verification is.** Recommendations 1 and 2 are worth more than
the rest combined: one closes the live security hole, and the other is the only thing that will
catch the next one automatically.
