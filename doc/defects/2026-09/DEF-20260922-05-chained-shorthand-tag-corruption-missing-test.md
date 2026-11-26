# [DEF-20260922-05] Chained Shorthand Tag Corruption, Missing Test-ID Variants, and Silent Blind Fallthrough in LocatorResolver

- **Status:** Resolved
- **Opened:** 2026-09-22
- **Closed:** 2026-09-22
- **Component:** `neodymium-core` (`LocatorResolver`, `LocatorResolverTest`, `LocatorResolverBrowserTest`)
- **Scope:** `Framework`
- **Symptom:**
  1. Chaining attribute shorthands (e.g. `#modal >> data-testid=save` or `form >> id=submit`) generated invalid XPath queries searching for literal `<data-testid>` and `<id>` HTML tags (`//*[@id='modal']//data-testid` and `//id`).
  2. The `data-test-id=` attribute shorthand was unmapped, falling through to invalid raw CSS and failing at runtime.
  3. Unsupported vendor pseudo-classes and layout selectors (`:visible`, `:hidden`, `:right-of()`, `:left-of()`, `:above()`, `:below()`, `:near()`, `:nth-match()`, `:text-matches()`) silently fell through to `By.cssSelector`, causing deferred browser crashes with cryptic syntax errors and LLM thrashing.
  4. Playwright codegen internal prefixes (`internal:role=...`, `internal:text=...`, etc.) failed to resolve.
- **Root Cause:**
  1. `resolveChainedLocator` only handled `text=` and `role=`; all other segments were passed to `toXPathSegment()` which mistook shorthand keys (`data-testid`, `id`, `placeholder`) for HTML element tags.
  2. `LocatorResolver.resolveLocator` lacked pre-validation for unsupported Playwright/jQuery pseudo-classes before falling through to `By.cssSelector`, and did not recognize `data-test-id=` or `internal:` prefixes.
- **Detection Gap ("What did we miss?"):**
  Unit tests in `LocatorResolverTest` only verified isolated selectors and basic CSS/text chains. No unit or integration tests exercised attribute shorthands inside `>>` chains or checked behavior when unsupported pseudo-classes were submitted.
- **Resolution:**
  1. Expanded `resolveChainedLocator` to recursively map attribute shorthands (`id=`, `data-testid=`, `data-test=`, `data-test-id=`, `placeholder=`, `alt=`, `title=`, `label=`).
  2. Added `data-test-id=` attribute shorthand support.
  3. Added pre-validation in `resolveLocator` to detect unsupported pseudo-classes and spatial layout selectors, throwing an explicit `InvalidSelectorException` with actionable remedies.
  4. Normalized Playwright codegen `internal:*` prefixes and case flags (`[name="Save"i]`).
  5. Expanded pure unit tests in `LocatorResolverTest` and live headless Chrome browser tests in `LocatorResolverBrowserTest`.
- **Safety Net Added:**
  - Unit tests in `LocatorResolverTest`: `testChainedAttributeShorthands`, `testDataTestIdAttribute`, `testPlaywrightCodegenInternalPrefixes`, and `testUnsupportedSelectorsFailFastWithDiagnostics`.
  - Real-browser integration tests in `LocatorResolverBrowserTest`: `testNamedRoleInLiveBrowser`, `testLabelInLiveBrowser`, `testChainedShorthandsInLiveBrowser`, `testDataTestIdInLiveBrowser`, and `testUnsupportedSelectorThrowsInBrowser`.
