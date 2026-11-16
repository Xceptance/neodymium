# [DEF-20261116-1247] Remove Extraneous Unstyled Info Bubble on Chrome Accordion Header

- **Status:** `Resolved`
- **Opened:** 2026-11-16 12:45
- **Closed:** 2026-11-16 12:48
- **Component:** `aura-manager`, `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In the Aura Manager Run Queue sidebar, the Google Chrome browser accordion header displayed an inconsistent bluish bubble containing raw text "info" next to the expand caret, which other browser accordions lacked.
- **Root Cause:** A hardcoded `<span>` with class `browser-info-icon` was conditionally rendered only for `grp.key == 'chrome'`. It omitted the `material-symbols-outlined` font class, causing the browser to render literal text "info" inside the badge circle, cluttering the UI and creating visual disparity between Chrome and other browser accordions.
- **Detection Gap ("What did we miss?"):** UI template tests checked overall layout and selection counts, but did not assert against unstyled ligature elements or enforce header consistency across browser group accordions.
- **Resolution:** Removed the redundant `browser-info-icon` span element from `queue.html` in both `aura-manager` and `neodymium-core`.
- **Safety Net Added:** Verified clean template structure across both modules. (Additional automated tests omitted per user instruction).
