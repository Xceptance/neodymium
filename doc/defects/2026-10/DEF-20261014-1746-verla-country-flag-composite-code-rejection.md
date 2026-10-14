# [DEF-20261014-1746] VÉRLA Country Flag Fallback on Composite Region Codes (CA_FR / CA_EN)

- **Status:** `Resolved`
- **Opened:** 2026-10-14 17:46
- **Closed:** 2026-10-14 18:28
- **Component:** `neodymium-test-server`
- **Scope:** `SUT` & `Test/Harness`
- **Symptom:** When selecting Canadian locales (`Canada (FR)` / `CA_FR` or `Canada (EN)` / `CA_EN`) in VÉRLA storefronts, the `#country-trigger-btn` in the header renders the generic fallback flag `🏳️` instead of the Canadian flag emoji `🇨🇦`. Consequently, playbook assertions such as `Verify country flag '🇨🇦' is displayed.` fail with `AssertionError: Expected text/pattern "🇨🇦" was not found on selector "#country-trigger-btn" within 3000ms. (Actual element text: "🏳️")`.
- **Root Cause:** Commit `c1a65a92c` introduced `VerlaCatalog.getCountryFlag(final String countryCode)` with an upfront strict length guard (`if (countryCode == null || countryCode.length() != 2) return "🏳️";`). Unlike the previous `EmbeddedHtmlServer.java` logic, it failed to normalize composite locale identifiers (e.g. `CA_FR`, `CA_EN`, or any hyphenated/underscored code starting with a valid 2-letter ISO prefix). Because VÉRLA catalog entries for Canada use 5-character keys (`CA_FR` and `CA_EN`), length validation failed before Unicode Regional Indicator emoji generation could occur.
- **Detection Gap ("What did we miss?"):** `VerlaCatalogTest` tested product lookups (SKU, slug, composite size keys) but lacked tests for `VerlaCatalog.getCountryFlag(...)`. Additionally, `JavalinVerlaServerTest.testCountrySelectionAndSearch` only verified country selection for Germany (`code=DE`), masking the failure for multi-locale composite country codes.
- **Resolution:**
  1. Updated `VerlaCatalog.getCountryFlag(final String countryCode)` to normalize composite codes (`CA_FR`, `CA_EN`, `ca-fr`, etc.) by extracting the 2-letter ISO country prefix before evaluating length and generating Regional Indicator symbols, preserving `UK` -> `GB` mapping and properly resolving `CA_FR` / `CA_EN` to `🇨🇦`.
  2. Updated `VerlaCatalog.getCountry(final String code)` to support `"CA"` as an alias for `"CA_EN"`.
- **Safety Net Added:**
  - Added `VerlaCatalogTest.testCountryFlagResolutionForAllCatalogCountries()` verifying all catalog countries resolve to valid emoji flags.
  - Added `VerlaCatalogTest.testCountryFlagFallbacksAndEdgeCases()` verifying null, empty, invalid, and lowercase composite code handling.
  - Added `VerlaCatalogTest.testCountryLookupWithCaAliasAndDefaults()` verifying `CA` alias resolution.
  - Added integration assertion in `JavalinVerlaServerTest.testCountrySelectionAndSearch` verifying that selecting `CA_FR` sets the cookie and renders `🇨🇦` on the home page.
