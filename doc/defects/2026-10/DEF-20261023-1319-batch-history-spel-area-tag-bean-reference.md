# [DEF-20261023-1319] SpEL BeanReference trigger from @ in area default in batch-history.html

- **Status:** `Resolved`
- **Opened:** 2026-10-23 13:19
- **Closed:** 2026-10-23 13:24
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** `TemplateProcessingException: Instantiation of new objects and access to static classes or parameters is forbidden in this context (template: "fragments/batch-history" - line 555, col 37)` when loading the Batch History overview for batches containing test variations.
- **Root Cause:** In `batch-history.html:555`, the fallback expression `th:data-area="${var.areaTag ?: '@Browsing (default)'}"` embedded `@Browsing` directly inside a SpEL expression. Thymeleaf Spring 6's `SpringStandardExpressionUtils.containsExternalAccess` performs a raw token scan for ASCII 64 (`@`) followed by an identifier character (`isSafeIdentifierChar`) to block unauthorized Spring Bean references (`@beanName`). The raw lexical scan does not discount single-quoted literals, mistakenly classifying `'@Browsing (default)'` as bean access and triggering an SSTI restriction error.
- **Detection Gap ("What did we miss?"):** `AuraReportViewControllerTest.testBatchHistoryRenderingFragment` initialized `context.setVariable("batchTestClasses", List.of())` with an empty list, so the inner table row loop `th:each="var : ${cls.variations}"` containing line 555 was never evaluated during template rendering tests.
- **Resolution:**
  1. Changed line 555 in `fragments/batch-history.html` to `th:data-area="${var.areaTag}"` without inline SpEL `@` string literals.
  2. Updated `TestBaseVariationEntity.getAreaTag()` and `setAreaTag()` to default to `"@Browsing (default)"` when null or blank, ensuring Java-level normalization.
  3. Ensured `AuraReportDataService.getBatchTestBaseClasses` constructs `TestBaseVariationEntity` with `normalizeArea(area)`.
- **Safety Net Added:** Updated `AuraReportViewControllerTest.testBatchHistoryRenderingFragment` to supply populated `batchTestClasses` with a variation entity and asserted `data-area="@Checkout"` in the rendered output, guaranteeing the variations loop and its attributes are executed and asserted during template rendering tests.
