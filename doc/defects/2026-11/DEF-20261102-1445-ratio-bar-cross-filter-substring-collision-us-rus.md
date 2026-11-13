# [DEF-20261102-1445] United Container Ratio Bar Fuzzy Substring Collision Between Locale Codes (US and RUS)

- **Status:** `Resolved`
- **Opened:** 2026-11-02 14:45
- **Closed:** 2026-11-02 14:48
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When filtering a united container by a dimension (e.g. `Browser: Chrome_1920x1080`) containing 2 tests with locales `RUS` and `ENG`:
  - The ratio bar section header correctly reported `2 Items`.
  - The stacked bar rendered three 50% segments side-by-side (`50% + 50% + 50% = 150%` width), extending past the container bounds.
  - The legend chips showed `US 1 (50%)`, `RUS 1 (50%)`, and `ENG 1 (50%)`, falsely reporting 3 items in total.
- **Root Cause:**
  1. In `resolveDimensionCount`, fallback matching used bidirectional substring inclusion (`kl.includes(rl) || rl.includes(kl)`).
  2. Because the two-letter country/locale code `"US"` is a substring of `"RUS"` (`R-U-S`), the `US` chip matched `RUS` (count 1) and claimed 50% width and count 1, in addition to the genuine `RUS` segment.
  3. In `matchesVariationFilter`, dimension matching on `browser`, `locale`, and `environment` also used `.includes(val)`, creating false positive cross-filter activations when clicking short dimension codes.
- **Detection Gap ("What did we miss?"):** Previous cross-filtering tests verified status dimension isolation between known and unknown failures, but did not assert discrete tag code isolation (e.g. 2-letter vs 3-letter country codes like `US` and `RUS`).
- **Resolution:**
  1. Replaced fuzzy substring matching in `resolveDimensionCount` with strict equality matching (`k.toLowerCase().trim() === rl`), completely disallowing any substring expansion across discrete tag values.
  2. Enforced strict equality comparisons in `matchesVariationFilter` for `browser`, `locale`, and `environment` dimensions.
- **Safety Net Added:** Added `testUnitedContainerRatioBarDiscreteTagIsolation` in `StaticResourceSyntaxTest.java` verifying that discrete tag dimensions use strict equality matching and disallow bidirectional substring inclusion.
