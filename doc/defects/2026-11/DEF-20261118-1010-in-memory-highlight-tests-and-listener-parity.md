# [DEF-20261118-1010] In-Memory Test Fixture Migration and Listener Reflection Parity in DebugUtilsTest

- **Status:** `Resolved`
- **Opened:** 2026-11-18 10:10
- **Closed:** 2026-11-18 10:20
- **Component:** `neodymium-core`
- **Scope:** `Test/Harness & Framework`
- **Symptom:** Running `mvn test -pl neodymium-core -Dtest=DebugUtilsTest,ElementHighlightUtilsTest` failed with 5 errors: `DebugUtilsTest` (methods `testHighlighting:41`, `testHighlightingWithBlinkCount:179`, `testHighlightingWithoutImplicitWaitTime:85`) and `ElementHighlightUtilsTest.testHighlightingWithSelenide:71` failed with `Element not found {.site-title} Expected: visible (Timeout: 3s)`, and `DebugUtilsTest.testWaiting:99` failed with `Test classes do not contain the same number of listeners expected:<8> but was:<4>`.
- **Root Cause:**
  1. `DebugUtilsTest` and `ElementHighlightUtilsTest` hardcoded external live website URLs (`https://blog.xceptance.com/` and `https://www.w3schools.com/...`). The external blog migrated to a Hugo static site generator, completely eliminating WordPress DOM structures and classes (`.site-title`, `#masthead`, etc.). Consequently, Selenide timed out waiting for `.site-title`.
  2. `org.neodymium.NeodymiumWebDriverListener` declared 8 public/protected methods after adding `afterFindElement`/`afterFindElements`, but test spy `NeodymiumWebDriverTestListener` only implemented the 4 `beforeFind*` methods. Its constructor reflection parity check (`Assert.assertEquals("Test classes do not contain the same number of listeners", parentMethods.size(), childMethods.size())`) failed.
- **Detection Gap ("What did we miss?"):** Unit and integration tests for debug highlighting and listener interception depended on the public internet and external third-party sites instead of self-contained in-memory `data:` URI fixtures, masking local framework integrity behind external site volatility and network timeouts.
- **Resolution:**
  1. Updated `NeodymiumWebDriverTestListener` to implement all 8 `WebDriverListener` methods, delegating `afterFind*` calls to `super`.
  2. Replaced external URL navigations in `DebugUtilsTest` and `ElementHighlightUtilsTest` with deterministic, self-contained in-memory `data:text/html;charset=utf-8,...` pages containing all required DOM elements and an iframe fixture.
  3. Cleaned up obsolete diagnostic `System.out.println` and w3schools cookie-banner handling, as well as an unused `ArrayList` import.
- **Safety Net Added:**
  - Automated targeted regression tests in `DebugUtilsTest` and `ElementHighlightUtilsTest` (`mvn test -pl neodymium-core -Dtest=DebugUtilsTest,ElementHighlightUtilsTest`), now passing 7/7 tests completely in-memory without external network dependencies.
