# [DEF-20261004-0042] Default e2e run included live-LLM tests and the documented `-PLiveAPI` profile did not exist

- **Status:** `Resolved`
- **Opened:** 2026-10-04 00:12
- **Closed:** 2026-10-04 00:42
- **Component:** `neodymium-e2e-tests`
- **Scope:** `Infra/Build` & `Doc/Spec`
- **Symptom:** `mvn test -pl neodymium-e2e-tests -am` ran every test, including those that call a real LLM and external sites. `doc/AI_TESTING.md` told users to add `-PLiveAPI`; Maven answered "The requested profile "LiveAPI" could not be activated because it does not exist" and ran the same set anyway.
- **Root Cause:** The tags `LiveAPI` (88 uses), `LiveLlm` (26) and `ExternalSite` (1) were never connected to the build: the surefire block in `neodymium-e2e-tests/pom.xml` had no `excludedGroups` and no profile existed. Six live-mode classes were also untagged: `live/AssertIntegrationTest`, `live/VisualAndLayoutIntegrationTest`, `data/AiDataFileProgrammaticIntegrationTest`, `verla/demo/ProgrammaticDemoTest`, `verla/demo/ProgrammaticIncludesDemoTest` and `external/BlogTest`.
- **Detection Gap ("What did we miss?"):** The documented command was never run, and nothing checks that a class using `FORCE_RECORDING`, `LLM_RECORDING` or `LLM_ONLY` without a mock carries a live tag.
- **Resolution:** `neodymium-e2e-tests/pom.xml` now sets `e2e.excludedGroups=LiveAPI,LiveLlm,ExternalSite` as the surefire `excludedGroups`, and a `LiveAPI` profile clears it. The six classes above are tagged `LiveAPI` (`BlogTest` also `ExternalSite`). `doc/AI_TESTING.md` and `README.md` describe the default run, `-PLiveAPI`, and `-PLiveAPI -Dgroups=LiveAPI,LiveLlm` for live-only.
- **Safety Net Added:** No automated test. Checked by hand: `help:evaluate` shows the default exclusion and the empty value under `-PLiveAPI`, and a run of `VisualAndLayoutIntegrationTest` and `BlogTest` with the default build executed 0 tests (API key unset). Enforcing the tag convention with a check is still open.
- **Pending Items:** a test or build check that fails when a class uses a live execution mode without a live tag; the mock-tier classes were classified by a "no mock references" heuristic and not individually inspected.
