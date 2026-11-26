# [DEF-20261124-1007] Integration Test Failures in CustomDashboardViewControllerIntegrationTest and NoReplayIntegrationTest

- **Status:** `Resolved`
- **Opened:** 2026-11-24 10:04
- **Closed:** 2026-11-24 10:08
- **Component:** `aura-manager`, `neodymium-e2e-tests`
- **Scope:** `Test Harness & Fixtures`
- **Symptom:**
  1. `CustomDashboardViewControllerIntegrationTest.testCombineWidgetsEndpointEquivalenceRejection` failed with `Model attribute 'widgetWarningMessage' Expected: a string containing "Cannot unite widgets" but: was null`.
  2. `NoReplayIntegrationTest.testNoReplayMock` (`REPLAY_STRICT` invocation) failed with `Replay should make exactly 1 LLM call because of the (no-replay) tag on step 2 ==> expected: <1> but was: <2>`.
- **Root Cause:**
  1. `testCombineWidgetsEndpointEquivalenceRejection` did not seed two distinct runs with non-overlapping executions, so `LATEST_TEST_STATUS` and `LAST_RUN` did not resolve to differing contributing run IDs.
  2. `NoReplayIntegrationTest.setupPropertiesAndMock` (`@BeforeEach`) ran before every `@AiMode` invocation and unconditionally queued `[NAVIGATE, CLICK, CLICK]`. On the `REPLAY_STRICT` invocation, Step 1 replayed offline and Step 2 (`Click`) popped the `NAVIGATE` response, causing the navigation provenance guard to trigger a retry (2 LLM calls).
- **Detection Gap ("What did we miss?"):**
  Both test setups relied on implicit state assumptions rather than explicitly seeding two runs or scoping `@BeforeEach` mock responses by `ExecutionMode`.
- **Resolution:**
  1. Seeded two distinct `TestRunEntity` runs (`CombineTestA` in `run-1` and `CombineTestB` in `run-2`, tracked in `createdRunIds` for `@AfterEach` cleanup) in `testCombineWidgetsEndpointEquivalenceRejection`.
  2. Updated `NoReplayIntegrationTest.setupPropertiesAndMock` to call `mock.clearResponses()` and queue `[CLICK]` for replay mode or `[NAVIGATE, CLICK]` for recording mode.
- **Safety Net Added:**
  - Verified with targeted Maven runs of `CustomDashboardViewControllerIntegrationTest#testCombineWidgetsEndpointEquivalenceRejection` and `org.neodymium.ai.integration.mock.NoReplayIntegrationTest`.
