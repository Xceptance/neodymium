# [DEF-20260930-08] Missing Model Pricing for Gemini 3.8 Flash in MetricsCollector

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `neodymium-core` (`ai-telemetry`)
- **Scope:** `Framework`
- **Symptom:** AI test runs configured with `neodymium.ai.model = gemini-3.8-flash` log a warning (`Unknown model 'gemini-3.8-flash' encountered for cost estimation. Cost calculation skipped (set to $0.00).`) and calculate $0.00 estimated USD cost in reports and session telemetry.
- **Root Cause:** `MetricsCollector.getModelRate` lacked a rate entry for `3.8-flash`.
- **Detection Gap ("What did we miss?"):** Unit tests in `MetricsCollectorTest` only asserted pricing for versions up through `3.7-flash`, allowing newly configured Flash models to silently evaluate to zero cost.
- **Resolution:** Added `3.8-flash` rate resolution ($0.75 input, $3.75 output, $0.1875 cached per 1M tokens) matching `3.7-flash` rates.
- **Safety Net Added:** Added test assertion for `gemini-3.8-flash` in `MetricsCollectorTest.testCostCalculationForSupportedGeminiModels`.
