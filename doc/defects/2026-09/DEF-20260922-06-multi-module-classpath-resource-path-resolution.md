# [DEF-20260922-06] Multi-Module Classpath Resource Path Resolution Divergence

- **Status:** Resolved
- **Opened:** 2026-09-22
- **Closed:** 2026-09-22
- **Component:** `neodymium-core` (`ClasspathResourceManager`, `ClasspathResourceManagerTest`)
- **Scope:** `Framework`
- **Symptom:** `FORCE_RECORDING` mode in multi-module builds wrote newly generated JSON playbooks into `./src/test/resources/` in the top-level aggregator root directory instead of the submodule's directory (`neodymium-core/src/test/resources/`). Consequently, subsequent test runs executing `REPLAY_STRICT` or `REPLAY_WITH_HEALING` loaded obsolete cached recordings from `neodymium-core/target/test-classes/` that were never updated, leading to schema mismatches (`3.0` vs `4.0`) or missing recordings.
- **Root Cause:** `ClasspathResourceManager.getSourceResourcesRoot()` fell back to `Path.of("src/test/resources")` relative to `System.getProperty("user.dir")` instead of inspecting the active classloader root (`target/test-classes`, `target/classes`, or `bin`) to derive the actual Maven/Gradle submodule source resources directory.
- **Detection Gap ("What did we miss?"):** Unit tests ran in single-module contexts where `user.dir` coincided with the module directory, masking path divergence in multi-module reactor builds.
- **Resolution:** Updated `getSourceResourcesRoot()` to dynamically derive the source resource folder from the active classloader resource URL (`this.classLoader.getResource("")`) by substituting `target/test-classes` with `src/test/resources` before falling back to `user.dir`. Cleaned up 36 orphan playbooks from aggregator root.
- **Safety Net Added:** Added `testSourceResourcesRootResolutionInMultiModule()` in `ClasspathResourceManagerTest.java` and verified live recording in `CanvasClickSandboxMockTest`.
