# [DEF-20261004-0145] E2E Suite JVM Isolation Loss and Working Directory Divergence

- **Status:** Resolved
- **Opened:** 2026-10-04 01:45
- **Closed:** 2026-10-04 01:46
- **Component:** `neodymium-e2e-tests` / `neodymium-core` / `neodymium-test-server`
- **Scope:** `Infra/Build & Config/Environment`
- **Symptom:** The e2e test suite executed all ~180 classes sequentially in a single shared JVM (`reuseForks=true`), causing static state mutations (timeouts, thread context, mock provider queues, embedded server state) to leak across tests; test outputs (Allure and Aura run storage) landed in `neodymium-e2e-tests/` instead of the project root; recording config files contained misspelled `neodyium.properties` and lacked parent-directory fallbacks.
- **Root Cause:** During the module split, `neodymium-e2e-tests/pom.xml` omitted `<workingDirectory>${session.executionRootDirectory}</workingDirectory>` and set `<reuseForks>true</reuseForks>`. Config loaders in `GifRecordingConfigurations`, `VideoRecordingConfigurations`, and `VerlaConfiguration` lacked `../config/` fallbacks and contained filename typos.
- **Detection Gap ("What did we miss?"):** Tests were executed individually (`-Dtest=...`) or from submodules without verifying whole-suite isolation and output directory resolution against the multi-module project root.
- **Resolution:**
  1. Configured `<reuseForks>false</reuseForks>` and `<workingDirectory>${session.executionRootDirectory}</workingDirectory>` in `neodymium-e2e-tests/pom.xml`.
  2. Injected `allure.results.directory` and `selenide.headless` surefire system properties in `neodymium-e2e-tests`.
  3. Corrected `"file:config/neodyium.properties"` to `"file:config/neodymium.properties"` and added `"file:../config/..."` fallbacks in `GifRecordingConfigurations` and `VideoRecordingConfigurations`.
  4. Added `../config/` and `neodymium.configDir` fallbacks in `VerlaConfiguration.java`.
  5. Added `neodymium-e2e-tests/src/test/resources` to `AuraFileService.java`.
- **Safety Net Added:** Verified clean multi-module surefire execution of e2e sandbox mock tests (`VisualMarkersSandboxMockTest`, `VerlaConfigurationTest`) running in isolated forks with root-level report output and configuration loading.
