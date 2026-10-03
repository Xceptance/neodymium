# [DEF-20261002-04] Video Recording Attribute Missing in Execution JSON Files Named After Test ID

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`InteractiveConsoleEngine.java`)
- **Scope:** `Framework`
- **Symptom:** The execution report UI rendered the video attachment section without playing or referencing the video file ("video is not referenced").
- **Root Cause:** `InteractiveConsoleEngine.attachVideoToLatestExecutionLog` filtered JSON log files strictly by prefix `console-execution-*.json`, missing execution JSON files named after execution IDs (e.g. `Aura_my_test_yaml_Test#executeYamlTest#en#Chrome_1920x1080.json`).
- **Detection Gap ("What did we miss?"):** Previous unit tests only generated log files starting with `console-execution-`.
- **Resolution:** Updated `attachVideoToLatestExecutionLog` to discover all execution JSON files (`*.json` excluding index metadata) in the target test directory and append `videoUrl` / `videoPath` to them.
- **Safety Net Added:** Added unit test coverage verifying video property injection into custom-named execution log JSON files.
