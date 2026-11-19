# [DEF-20261118-1215] License Compliance, In-Tree Provenance, and AI Copyleft Boundary Isolation

- **Status:** `Resolved`
- **Opened:** 2026-11-18 12:15
- **Closed:** 2026-11-18 12:45
- **Component:** `neodymium-core` / `neodymium-classic` / `aura-manager` / `neodymium-test-server` / `neodymium-e2e-tests` / `neodymium-archetype` / `pom.xml`
- **Scope:** `Framework & Infra/Build`
- **Symptom:** Monolithic `neodymium-core` contained GNU AGPLv3 AI engine code, Langchain4j dependencies, bundled minified HTMX without BSD notice, bundled modified Selenium HTTP client without Apache 2.0 § 4(b) modification notice, Google Material Symbols and OFL fonts without notices, and 800+ Java source files without license headers, creating severe copyleft contamination risk for permissive MIT consumers.
- **Root Cause:**
  1. AI features (playbook runner, LLM clients, VLM grounding, interactive console, Langchain4j) were built directly inside `neodymium-core` without multi-licensing declarations in POM metadata and file headers.
  2. Forked Selenium class `org.openqa.selenium.remote.http.jdk.JdkHttpClient.java` and derived OkHttpClient factory code in `NeodymiumProxyHttpClientFactory.java` lacked the prominent modification notices required by Apache 2.0 § 4(b).
  3. Bundled third-party web assets (`htmx.js`, `material-symbols.css`, fonts) lacked license banners and upstream attributions.
  4. Build packaging lacked automated third-party license aggregation and a root `NOTICE` file required by Apache 2.0 § 4(d).
- **Detection Gap ("What did we miss?"):**
  Existing CI and unit test suites only validated runtime functional correctness and API contracts. No automated license compliance test, copyleft isolation test, or AST import check existed to enforce MIT purity in `neodymium-classic` or dual-license declarations across all AI sub-projects.
- **Resolution:**
  1. Standardized Neodymium licensing architecture:
     - `neodymium-classic`: 100% pure permissive MIT License, completely free of AI and Langchain4j dependencies.
     - All other sub-projects (`neodymium-core`, `aura-manager`, `neodymium-test-server`, `neodymium-e2e-tests`, `neodymium-archetype`): Dual-licensed under GNU AGPLv3 + Commercial License.
  2. Maintained clean direct Java architecture inside `neodymium-core`: consolidated AI capabilities within `neodymium-core`, avoiding unnecessary reflection or artificial decoupling.
  3. Restored HTMX BSD 2-Clause license banner, prepended Apache 2.0 attribution to Material Symbols CSS, added OFL 1.1 notice for bundled fonts, and documented § 4(b) modifications in `JdkHttpClient.java` and `NeodymiumProxyHttpClientFactory.java`.
  4. Normalized all AI action plugins (`BranchAction`, `CheckAction`, `IncludeAction`, `StoreAction`, `AiMethod`, `JavaMethodAction`) to GNU AGPLv3 + Commercial dual-license.
  5. Applied standard MIT license header across all Java files in `neodymium-classic`, and canonical Dual-License header (`GNU AGPLv3 + Commercial`) across all other sub-projects.
  6. Configured root `pom.xml` with `license-maven-plugin:add-third-party` in `prepare-package` phase and created comprehensive root `NOTICE` and updated `LICENSE` files.
- **Safety Net Added:**
  1. `org.neodymium.license.ClassicLicenseComplianceTest` (in `neodymium-classic`):
     - `testLangchain4jNotOnClasspath`: Asserts Langchain4j cannot be resolved in `neodymium-classic`.
     - `testZeroAiImportsInClassicMainSources`: AST scan ensuring no Java file in classic imports AI packages (`org.neodymium.ai.*`, `com.xceptance.neodymium.ai.*`, `com.xceptance.neodymium.aura.*`, `dev.langchain4j.*`).
     - `testZeroCopyleftHeadersAndFullMitCoverage`: Verifies 100% of classic Java files have MIT headers and zero copyleft notices (`AGPL`/`GPL`).
     - `testPomDeclaresMitLicense`: Asserts POM explicitly declares MIT License.
  2. `org.neodymium.license.LicenseComplianceTest` (in `neodymium-core`):
     - `testCoreJavaFilesHaveDualLicenseHeader`: Asserts all Java files in `neodymium-core` carry dual-license header.
     - `testCorePomDeclaresDualLicense`: Asserts `neodymium-core/pom.xml` declares both AGPLv3 and Commercial licenses.
     - `testAuraManagerDeclaresDualLicenseAndCanonicalHeaders`: Verifies `aura-manager` POM declarations and header compliance.
     - `testNeodymiumTestServerDeclaresDualLicenseAndCanonicalHeaders`: Verifies `neodymium-test-server` POM declarations and header compliance.
     - `testNeodymiumE2eTestsDeclaresDualLicenseAndCanonicalHeaders`: Verifies `neodymium-e2e-tests` POM declarations and header compliance.
     - `testNeodymiumArchetypeDeclaresDualLicenseAndCanonicalHeaders`: Verifies `neodymium-archetype` POM declarations and template header compliance.
     - `testForkedSeleniumHttpClientPreservesApacheNotice`: Verifies Apache 2.0 and Section 4(b) notice on `JdkHttpClient.java`.
     - `testRootLicenseCoversAllThreeLicenseTiers`: Verifies root `LICENSE` defines Section A (MIT), Section B (AGPLv3), and Section C (Commercial).
     - `testRootNoticeDocumentsAllThirdPartyLicenses`: Verifies root `NOTICE` attributes Selenium Apache 2.0, HTMX BSD 2-Clause, Material Symbols Apache 2.0, SIL OFL 1.1, and first-party licenses.
     - `testThirdPartyStaticAssetsPreserveLicenses`: Verifies bundled static assets (`htmx.js`, `OFL.txt`) retain their respective upstream license headers.
