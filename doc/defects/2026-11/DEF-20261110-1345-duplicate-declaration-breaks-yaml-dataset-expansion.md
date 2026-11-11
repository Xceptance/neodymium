# [DEF-20261110-1345] Duplicate rawTextarea Declaration Breaks YAML Dataset Expansion

- **Status:** `Resolved`
- **Opened:** 2026-11-10 13:36
- **Closed:** 2026-11-10 13:46
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the Neodymium Aura Manager "Playbooks & Queue" tab (`/playbooks`), clicking on any YAML test case row fails to expand its dataset list. The list items do not open, leaving dataset entries hidden.
- **Root Cause:**
  1. In `aura-manager/src/main/resources/static/dashboard-editor.js`, inside `initVisualPlaybookEditor()`, `const rawTextarea` was declared at line 2175 and redeclared with `const rawTextarea` at line 2264 (introduced during the `_properties` preservation update).
  2. In modern ECMAScript, redeclaring a lexical `const` binding in the same function scope throws an immediate parse-time `SyntaxError: Identifier 'rawTextarea' has already been declared`.
  3. Consequently, the browser's JavaScript engine rejected the entire `dashboard-editor.js` script during initial evaluation, leaving all functions (including `window.toggleExpandFile` and `window.initVisualPlaybookEditor`) undefined.
  4. In `test-selection.html`, the row click handler specifies `onclick="if(typeof toggleExpandFile==='function') toggleExpandFile(this.getAttribute('data-file'), this);"`. Because `toggleExpandFile` was undefined, clicking silently no-oped.
  5. Furthermore, `.dataset-list` in `test-selection.html` lacked `th:data-file="${item.file}"`, preventing fallback DOM queries by `data-file`.
- **Detection Gap ("What did we miss?"):** `StaticResourceSyntaxTest.java` was configured to check only `src/main/resources/static/js/`, omitting all root scripts under `src/main/resources/static/*.js` (`dashboard-editor.js`, `dashboard-core.js`, `dashboard-chat.js`, etc.). As a result, the test suite reported 71 passing tests and zero syntax errors despite the fatal syntax error in `dashboard-editor.js`.
- **Resolution:**
  1. Removed redundant `const rawTextarea = ...` declaration at line 2264 in `dashboard-editor.js`, reusing the existing variable in scope.
  2. Added `th:data-file="${item.file}"` to `.dataset-list` in `test-selection.html` to guarantee resilient fallback querying across nested subdirectories.
  3. Expanded `StaticResourceSyntaxTest.testAllStaticJavaScriptAssetsHaveValidSyntax()` to recursively scan `src/main/resources/static` using `Files.walk()`, ensuring all root and sub-folder static JavaScript assets are validated with Node.js in CI.
- **Safety Net Added:**
  - `StaticResourceSyntaxTest.testAllStaticJavaScriptAssetsHaveValidSyntax`: Recursively walks `src/main/resources/static/`, validating all JavaScript assets (`dashboard-editor.js`, `dashboard-core.js`, `report-manager.js`, etc.) using `node -c` (with fallback to bracket balancing).
