# [DEF-20261022-1410] Workspace Test Filter Cookie URL-Decoding and Folder Filter Match Propagation

- **Status:** `Resolved`
- **Opened:** 2026-10-22 14:10
- **Closed:** 2026-10-22 14:12
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** 
  1. Test filters containing spaces or special characters (e.g., "login test") restored from cookie fallback on page reload resulted in zero matching tests and displayed literal `%20` in the search input.
  2. In multi-tab navigation, tab-scoped `sessionStorage` conflicting with origin-scoped cookies caused reloaded tabs to filter server-rendered HTML to a blank list.
  3. When searching for a folder name, child test cases and dataset items inside the matching folder were not universally displayed.
- **Root Cause:** 
  1. `AuraTestDashboardController` and `AuraTestFileController` read `@CookieValue` without URL decoding. Servlet containers do not automatically URL-decode cookie values.
  2. Client-side initialization prioritized stale `sessionStorage` over the server-rendered input value.
  3. `applyInstantTestFilter` only checked individual file containers and dataset items, omitting folder-level match propagation down to child tests.
- **Detection Gap ("What did we miss?"):** Prior unit tests only tested single-word alphanumeric queries ("checkout"), missing percent-encoded cookie scenarios and folder-level filter interactions.
- **Resolution:** 
  1. Implemented safe UTF-8 URL decoding for cookie fallback in `AuraTestDashboardController` and `AuraTestFileController`.
  2. Synchronized client initialization with the server-rendered search value.
  3. Updated `applyInstantTestFilter` and inline script to show all child tests and datasets when an ancestor folder matches the filter.
- **Safety Net Added:**
  - `AuraTestDashboardControllerTest.testRenderDashboardWithEncodedCookieFallback`
  - `AuraTestFileControllerTest.testFilesListFragmentWithEncodedCookieFallback`
  - `StaticResourceSyntaxTest.testWorkspaceTestSearchInstantFilterAndCookiePersistence`
