# [DEF-20261029-1515] Playbooks & Queue Quick Tag Filter Buttons Fail with Thymeleaf String Event Restriction

- **Status:** `Resolved`
- **Opened:** 2026-10-29 15:15
- **Closed:** 2026-10-29 15:21
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Opening the Playbooks & Queue page (`/playbooks` or `/`) throws an HTTP 500 error: `org.thymeleaf.exceptions.TemplateProcessingException: Only variable expressions returning numbers or booleans are allowed in this context, any other datatypes are not trusted in the context of this expression, including Strings or any other object that could be rendered as a text literal... (template: "fragments/test-selection" - line 42, col 103)`.
- **Root Cause:** `#quickTagFiltersBar` in `aura-manager/src/main/resources/templates/fragments/test-selection.html` used `th:onclick="|filterTestsByTag('${t}')|"` with a String variable `t`. Thymeleaf 3.1+ strictly disallows non-numeric/non-boolean variable interpolation in `th:on*` event handlers for security reasons. When test tags were present, template evaluation failed on line 42.
- **Detection Gap ("What did we miss?"):** Prior controller unit tests used standalone `MockMvc` asserting view names and model attributes rather than executing the full Thymeleaf `SpringTemplateEngine` rendering pipeline with populated `availableTags`.
- **Resolution:** Replaced `th:onclick="|filterTestsByTag('${t}')|"` with standard HTML `onclick="if(typeof filterTestsByTag==='function') filterTestsByTag(this.getAttribute('data-tag'));"` and preserved `th:data-tag="${t}"`.
- **Safety Net Added:** Added automated regression test `AuraTestFileControllerTest.testRenderTestSelectionTemplateWithAvailableTags` that renders `fragments/test-selection` with non-empty `availableTags` through `SpringTemplateEngine`.
