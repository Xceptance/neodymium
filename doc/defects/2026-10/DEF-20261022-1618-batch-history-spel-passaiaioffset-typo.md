# [DEF-20261022-1618] SpEL property passAiAiOffset typo in batch-history.html donut SVG

- **Status:** `Resolved`
- **Opened:** 2026-10-22 16:18
- **Closed:** 2026-10-22 16:19
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Thymeleaf TemplateInputException with SpelEvaluationException: `EL1008E: Property or field 'passAiAiOffset' cannot be found on object of type 'com.xceptance.aura.report.dto.BatchSummaryDto'` when viewing batch history for batches containing AI-driven passed tests.
- **Root Cause:** Accidental duplicated "Ai" identifier `passAiAiOffset` in `stroke-dashoffset=${batchSummary.passAiAiOffset ?: batchSummary.passAiDashOffset}` in `batch-history.html:120`, whereas `BatchSummaryDto` provides `passAiDashOffset`.
- **Detection Gap ("What did we miss?"):** `AuraReportViewControllerTest` used MockMvc standalone setup without full Thymeleaf view resolution, asserting view names and model attributes without parsing the underlying HTML templates or evaluating Thymeleaf SpEL expressions.
- **Resolution:** Correct `batch-history.html:120` to `stroke-dashoffset=${batchSummary.passAiDashOffset}` matching line 119 and sibling templates.
- **Safety Net Added:** Added automated template rendering test in `AuraReportViewControllerTest` using `SpringTemplateEngine` with a `ClassLoaderTemplateResolver` that processes `fragments/batch-history` with a `BatchSummaryDto` having `latestPassAiCount > 0` to verify all SpEL property expressions evaluate without errors.
