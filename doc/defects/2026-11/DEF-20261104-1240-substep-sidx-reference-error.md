# [DEF-20261104-1240] Sub-Step LLM Rendering References Undeclared sIdx Causing Infinite Loading Spinner

- **Status:** `Resolved`
- **Opened:** 2026-11-04 12:40
- **Closed:** 2026-11-04 12:42
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In Aura Manager run reports, selecting a test execution whose sub-steps contain LLM invocations stays permanently on "Loading steps and screenshots..." without ever rendering steps or displaying an error message.
- **Root Cause:** In `report-manager.js`, the loop `subSteps.forEach((sub, subIdx) => ...)` referred to `sIdx` instead of `subIdx` when constructing `subStepKey`, `stepNum`, and `title` for sub-steps with LLM calls. This threw `ReferenceError: sIdx is not defined`. Because `data-blocks` had already been populated on `activeRow` before calling `renderStepsForExecution`, the `.catch(err)` block evaluated `hasExistingSteps` to true and left the loading indicator DOM element intact, resulting in a permanent loading spinner.
- **Detection Gap ("What did we miss?"):** `StaticResourceSyntaxTest` verified JavaScript parse syntax, but undeclared variable references are syntactically valid parse tokens that only trigger runtime exceptions during execution. Previous tests lacked sub-steps with nested LLM calls.
- **Resolution:**
  1. In `report-manager.js`, replaced `sIdx` with `subIdx` at lines 4255, 4259, and 4260.
  2. In `report-manager.js` `.catch()`, updated logic to replace `stepListEl.innerHTML` with an error message if the loading spinner is still active (`isStillLoading || !hasRenderedSteps`), avoiding perpetual spinner states upon render exceptions.
- **Safety Net Added:** Added `@Test` `StaticResourceSyntaxTest.testSubStepLlmInvocationRenderingDoesNotReferenceUndeclaredSidx` asserting that `report-manager.js` contains no `sIdx` references and successfully executing `renderStepsForExecution` against mock sub-steps with LLM invocations in a Node.js VM context.
