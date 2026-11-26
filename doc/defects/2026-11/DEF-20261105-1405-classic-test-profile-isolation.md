# [DEF-20261105-1405] Neo Classic Test Profile Aggregator Bleed and Missing Module Isolation

- **Status:** `Resolved`
- **Opened:** 2026-11-05 14:05
- **Closed:** 2026-11-05 14:08
- **Component:** `neodymium-classic & neodymium-core`
- **Scope:** `Infra/Build & Config/Environment`
- **Symptom:** Executing `mvn test -Pnon-llm-tests` or `mvn test -Punit-tests` from the root aggregator inadvertently executed legacy browser tests in `neodymium-classic`. Additionally, `neo-classic-junit4` and `neo-classic-junit5` profiles executed within `neodymium-core` rather than being strictly confined to `neodymium-classic`, and running targeted `-Dtest` filters failed `neodymium-classic` with `No tests matching pattern were executed`.
- **Root Cause:**
  1. `neodymium-core/pom.xml` defined test execution suites for `neo-classic-junit4` and `neo-classic-junit5` instead of skipping them in favor of the dedicated `neodymium-classic` subproject.
  2. `neodymium-classic/pom.xml` lacked `<skipTests>true</skipTests>` bindings for modern profiles (`unit-tests`, `non-llm-tests`, `verla-tests`, `ai-mock-tests`, `ai-live-tests`, `aura-sandbox`, `aura-manager`, `smoke`).
  3. `neodymium-classic/pom.xml` did not define `surefire.failIfNoSpecifiedTests=false`.
- **Detection Gap ("What did we miss?"):** The subproject was added before PR #510's aggregator profile system was merged, leaving `neodymium-classic` unaligned with the new test isolation architecture.
- **Resolution:**
  1. Updated `neodymium-core/pom.xml` so `neo-classic-junit4`, `neo-classic-junit5`, `junit-4`, and `junit-5` set `<skipTests>true</skipTests>`.
  2. Configured `neodymium-classic/pom.xml` with `neo-classic-junit4` (and alias `junit-4`) and `neo-classic-junit5` (and alias `junit-5`) to execute classic tests exclusively in `neodymium-classic`.
  3. Added `<skipTests>true</skipTests>` to `neodymium-classic/pom.xml` for all modern/non-classic profiles (`unit-tests`, `non-llm-tests`, `verla-tests`, `ai-mock-tests`, `ai-live-tests`, `aura-sandbox`, `aura-manager`, `smoke`).
  4. Added `<surefire.failIfNoSpecifiedTests>false</surefire.failIfNoSpecifiedTests>` and configured Surefire in `neodymium-classic/pom.xml`.
- **Safety Net Added:** Automated verification verifying `mvn test -Punit-tests -Dtest=VerlaCatalogTest` skips `neodymium-classic`, and `mvn test -Pneo-classic-junit4 -Dtest=DataUtilsTest` and `mvn test -Pneo-classic-junit5 -Dtest=DataUtilsTest` skip `neodymium-core` while executing in `neodymium-classic`.
