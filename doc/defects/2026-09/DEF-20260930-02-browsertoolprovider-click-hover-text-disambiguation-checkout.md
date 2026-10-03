# [DEF-20260930-02] BrowserToolProvider Click/Hover Text Disambiguation & Checkout/Search Fixture Sync Issues

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `neodymium-core` (`tool/browser/BrowserToolProvider` / `verla-playbooks` / `PrelinterRuleMatrixLiveTest`)
- **Scope:** `Framework` & `Test/Harness`
- **Symptom:** 1) `CartTest` (`basic`, `full`, `judge`) failed when the agent called `click(selector="article[...] button", text="S")`: the primary button was clicked instead of the size button, leaving cart count at 0. 2) `SearchGermanTest` failed dropdown assertions when the search form was submitted prematurely on Enter. 3) `CheckoutTest` on `tailwind_by_claude` failed asserting updated order summary subtotal ($31.98) against initial captured subtotal ($15.99). 4) `PrelinterRuleMatrixLiveTest.testVagueVerification_Spanish_Aviation` failed asserting `VAGUE_VERIFICATION` because Spanish phrasing without explicit verification keywords triggered `MISSING_VISUAL_TAG`.
- **Root Cause:** 1) `BrowserToolProvider.executeElementClick` and `executeHover` favored non-blank `selector` exclusively without filtering candidates by `text` when both parameters were present. 2) The instruction `Gib "${searchQuery}" in das Suchfeld ein.` led the LLM to submit the form immediately via `pressEnter: true`. 3) The playbook lacked a variable re-capture step after incrementing quantity. 4) Phrasing "se vea correcto y ordenado" triggered visual appearance rules rather than subjective verification.
- **Detection Gap ("What did we miss?"):** Tool parameter interaction tests did not cover compound selector-plus-text resolution, and multi-language linter matrix phrases were not evaluated against the full pre-flight taxonomy.
- **Resolution:** Implemented `resolveElementBySelectorAndText` and `safeGetText` in `BrowserToolProvider` to search matching visible candidates by text; updated search instruction to `Tippe...`; re-captured `${subtotal}` in the checkout playbook; and refined the Spanish linter prompt to `Compruebe que el plan de vuelo funcione correctamente.`.
- **Safety Net Added:** Verified resolution across all four affected test suites (`CartTest`, `SearchGermanTest`, `CheckoutTest`, `PrelinterRuleMatrixLiveTest`).
