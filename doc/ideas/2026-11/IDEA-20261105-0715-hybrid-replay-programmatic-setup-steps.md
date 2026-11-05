# [IDEA-20261105-0715] Make hybrid tests replayable: programmatic setup steps in the recording

- **Status:** `Proposed`
- **Proposed:** 2026-11-05 07:15
- **Resolved:** Pending
- **Component:** `neodymium-core`
- **Category:** `Architecture & Core`
- **Author:** AI-generated: Claude Sonnet 5.5 / Xceptance GmbH 2026

## 1. Problem Statement & Motivation
A hybrid test combines programmatic steps (`session.execute(...)` in `@BeforeEach`) with a YAML test method. Since the recorder writes the executed-step history, the recording of such a test now contains the setup step followed by the YAML steps, all tagged with block `steps`. Verified with a real browser (`RecordReplayLifecycleIntegrationTest.hybridRecord`): the recording is correct, each step once, in order, with actions and tool calls.

Replay fails (`RecordReplayLifecycleIntegrationTest.hybridReplay`, currently `@Disabled`): the replay parses the whole recording as the test's playbook, so the setup step is executed from the recording as a main step, and then `@BeforeEach` calls `session.execute(...)` for it again, where no recorded data is available and `REPLAY_STRICT` rejects it with `No recorded tool calls found`. Before this branch the programmatic step was missing from the recording altogether, so hybrid replay never worked either; it is a gap, not a regression.

## 2. Proposed Architecture & Design
The recording needs to say which steps came from programmatic `execute()` calls and which from the YAML playbook. Options:
1. Mark steps with their origin when they are recorded (for example a `programmatic: true` flag or the lifecycle phase), and on replay hand the programmatic ones to the `execute()` replay cursor instead of running them as main steps.
2. Record the programmatic steps in the `before`/`after` block by the lifecycle phase in which they ran. That needs the phase information that was removed together with the unused `KEY_ACTIVE_PHASE` marker, and the steps would also have to be registered in `playbook.beforeSteps` so the HUD still shows them.

## 3. Expected Impact & Trade-offs
- **Benefits:** Hybrid tests become replayable without LLM; the recording format stays one file per test.
- **Risks & Complexity:** Changes the recording format (additive field) and the split of recorded steps into cursor-consumed and runner-executed steps. Old recordings without the marker must keep working. Interacts with the step-order work in `IDEA-20261105-0700-single-authority-executed-step-order`.

## 4. Open Questions & Alternatives Considered
- Alternative: record only the YAML steps for hybrid tests and let programmatic steps always run live. Rejected: programmatic steps would then call the LLM on every replay.
- Is a test with several programmatic phases (`@BeforeEach`, test body, `@AfterEach`) around a YAML body common enough to justify the format change? Check usage before deciding.
