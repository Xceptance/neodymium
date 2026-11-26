# [DEF-20261105-0705] Programmatic replay lost the recorded steps its replay cursor depends on

- **Status:** `Resolved`
- **Opened:** 2026-11-05 07:00
- **Closed:** 2026-11-05 07:05
- **Component:** `neodymium-core` (`NeodymiumAiRunner`)
- **Scope:** `Framework`
- **Symptom:** A fully programmatic test (`@AiPlaybook(AiPlaybook.PROGRAMMATIC)`, steps executed through `session.execute(...)`) could not be replayed in `REPLAY_STRICT` mode: the first `execute()` failed with `No recorded tool calls found for step '...'`, although the recording contained the step with its actions and tool calls. Found by `RecordReplayLifecycleIntegrationTest` against a real browser. Recordings of programmatic tests that were created before this branch would have been affected as well.
- **Root Cause:** `AiSession.execute()` builds its replay cursor from `playbook.steps` and applies the recorded status, actions and tool calls to each live `execute()` call. On `develop` the runner seeded `playbook.steps` with the steps parsed from the recording. This branch changed `executeBeforeEach` to start `playbook.steps` as an empty list, so that executed steps accumulate chronologically (needed for correct report order). For programmatic replay that removed the only source of recorded data, the cursor was never created, and strict replay rejected every step.
- **Detection Gap ("What did we miss?"):** The unit tests ran the lifecycle with a mock LLM that produces no recordable tool calls, and strict replay cannot be verified with such steps. No test recorded a programmatic test and replayed it in a real browser.
- **Resolution:** In `NeodymiumAiRunner.executeBeforeEach` the executed-step list is seeded with the recorded steps when the test is programmatic and the effective mode is a replay mode. In all other cases it still starts empty.
- **Safety Net Added:** `RecordReplayLifecycleIntegrationTest.programmaticRecord` / `programmaticReplay` (real headless browser and test server, scripted mock LLM that is empty during replay: a live LLM call fails the test; asserts the click took effect and that exactly two steps ran). Hybrid replay is not covered yet, see `IDEA-20261105-0715-hybrid-replay-programmatic-setup-steps`.
