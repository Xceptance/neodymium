# [DEF-20261104-1350] Modals Create Page th:onclick String Expression Template Processing Exception

- **Status:** `Resolved`
- **Opened:** 2026-11-04 13:50
- **Closed:** 2026-11-04 13:55
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Opening or rendering `index.html` throws `TemplateProcessingException: Only variable expressions returning numbers or booleans are allowed in this context, any other datatypes are not trusted in the context of this expression, including Strings or any other object that could be rendered as a text literal. (template: "fragments/modals" - line 258, col 37)`.
- **Root Cause:** Thymeleaf 3.1 `StandardDOMEventAttributeTagProcessor` strictly forbids String expressions in `th:onclick` event attributes to prevent script injection. Line 258 in `modals.html` used `th:onclick="'setCreatePageGroupBy(\'' + ${tg} + '\', this)'"`.
- **Detection Gap ("What did we miss?"):** Previous controller unit tests rendered isolated fragments without populating `availableIdentityTags`, leaving the dynamic tag loop in `modals.html` unrendered during automated test passes.
- **Resolution:** Replaced `th:onclick` with standard HTML `onclick="setCreatePageGroupBy(this.getAttribute('data-group-by'), this)"`, reading from the safe `data-group-by` attribute.
- **Safety Net Added:** Added `testModalsFragmentRendersWithDynamicIdentityTagsWithoutSecurityException` in `AuraReportViewControllerSpringContextTest`.
