# [DEF-20261118-1215] License Compliance, In-Tree Provenance, and AI Copyleft Boundary Isolation

- **Status:** `Resolved`
- **Opened:** 2026-11-18 12:15
- **Closed:** 2026-11-18 12:45
- **Component:** `neodymium-core` / `neodymium-ai` / `pom.xml`
- **Scope:** `Framework & Infra/Build`
- **Symptom:** Monolithic `neodymium-core` contained GNU AGPLv3 AI engine code, Langchain4j dependencies, bundled minified HTMX without BSD notice, bundled modified Selenium HTTP client without Apache 2.0 § 4(b) modification notice, Google Material Symbols and OFL fonts without notices, and 800+ Java source files without license headers, creating severe copyleft contamination risk for permissive MIT consumers.
- **Root Cause:**
  1. AI features (playbook runner, LLM clients, VLM grounding, interactive console, Langchain4j) were built directly inside `neodymium-core` without module boundary separation, coupling core MIT runtime classes (`Neodymium.java`, `NeodymiumRunner.java`, `BrowserData.java`, `YamlFileReader.java`) directly to AGPLv3 AI classes.
  2. Forked Selenium class `org.openqa.selenium.remote.http.jdk.JdkHttpClient.java` contained significant functional modifications (native streaming and direct client exposure) but lacked the prominent modification notices required by Apache 2.0 § 4(b).
  3. Bundled third-party web assets (`htmx.js`, `material-symbols.css`, fonts) lacked license banners and upstream attributions.
  4. Build packaging lacked automated third-party license aggregation and a root `NOTICE` file required by Apache 2.0 § 4(d).
- **Detection Gap ("What did we miss?"):**
  Existing CI and unit test suites only validated runtime functional correctness and API contracts. No automated license compliance plugin, copyleft isolation test, or AST import check existed to prevent `neodymium-core` from importing AI classes or transitive copyleft dependencies.
- **Resolution:**
  1. Decoupled AI from `neodymium-core` by moving AI runtime, runners, models, and interactive console into a dedicated standalone Maven module `neodymium-ai` licensed under GNU AGPLv3.
  2. Converted compile-time couplings in `NeodymiumRunner`, `BrowserData`, and `TakeScreenshotsThread` to reflection/string-based checks, extracted universal `YamlStepPreprocessor` into `neodymium-core` (MIT), and moved `ExecutionAttachment` to `org.neodymium.common.model.ExecutionAttachment` (MIT).
  3. Restored HTMX BSD 2-Clause license banner, prepended Apache 2.0 attribution to Material Symbols CSS, added OFL 1.1 notice for bundled fonts, and documented § 4(b) modifications in `JdkHttpClient.java`.
  4. Normalized all AI action plugins (`BranchAction`, `CheckAction`, `IncludeAction`, `StoreAction`, `AiMethod`, `JavaMethodAction`) to GNU AGPLv3.
  5. Applied standard MIT license header across all first-party `.java` files in `neodymium-core` and `neodymium-classic`, and GNU AGPLv3 across `neodymium-ai`.
  6. Configured root `pom.xml` with `license-maven-plugin:add-third-party` in `prepare-package` phase and created comprehensive root `NOTICE` file.
- **Safety Net Added:**
  `org.neodymium.license.LicenseComplianceTest`:
  - `testLangchain4jNotOnClasspath`: Asserts Langchain4j cannot be resolved in `neodymium-core`.
  - `testAiClassesNotOnClasspath`: Asserts AI runners and facades cannot be resolved in `neodymium-core`.
  - `testZeroAiImportsInCoreMainSources`: Scans all `.java` sources in `neodymium-core` and fails if any import references `org.neodymium.ai.*`, `com.xceptance.neodymium.ai.*`, `com.xceptance.neodymium.aura.*`, or `dev.langchain4j.*`.
  - `testZeroAiClassesInTargetClasses`: Scans `target/classes` of `neodymium-core` to ensure zero compiled classes belong to forbidden AI packages.
