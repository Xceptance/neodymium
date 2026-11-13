# [DEF-20261021-1650] Unrounded Floating-Point Decimals in Pass Rate Badges

- **Status:** `Resolved`
- **Opened:** 2026-10-21 16:50
- **Closed:** 2026-10-21 16:55
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the Batch Overview run directory table, test run badges, and batch summaries, the pass rate was displaying unrounded raw IEEE-754 double floats with up to 14 decimal digits (e.g. `66.66666666666667% PASS`) instead of clean integer percentages (`67% PASS`).
- **Root Cause:** `RunStorageSyncService.java` (line 913) and `LocalRunJsonStorageService.java` computed `finalPassRate = (double)(finalPass + finalFixed) / finalTotal * 100.0` without integer rounding. Furthermore, `batch-overview.html` (line 336) and `batch-history.html` (line 433) rendered `${r.passRate + '% Pass'}` via direct string concatenation instead of formatting as an integer.
- **Detection Gap ("What did we miss?"):** Controller tests only asserted fragment view names and model attributes without verifying that the rendered pass rate string matches integer percentage formatting without decimal fractions.
- **Resolution:** Formatted pass rate to integer (`Math.round(...)`) across `RunStorageSyncService`, `LocalRunJsonStorageService`, `BatchSummaryDto` (`getLatestRunPassRateInt()`), and Thymeleaf formatting templates (`#numbers.formatDecimal(..., 1, 0)`).
- **Safety Net Added:** Added unit assertions in `AuraReportViewControllerTest` and `BatchSummaryDtoTest` verifying pass rate formatting never outputs fractional decimal places.
