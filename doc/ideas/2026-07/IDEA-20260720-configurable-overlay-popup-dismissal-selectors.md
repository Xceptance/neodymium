# [IDEA-20260720] Configurable Overlay & Popup Dismissal Selectors

- **Status:** `Proposed`
- **Proposed:** 2026-07-20
- **Resolved:** Pending
- **Component:** `neodymium-core (PrepareRetryStep)`
- **Category:** `Architecture & Core`
- **Author:** Neodymium Core Team

---

When an interactive action fails, `PrepareRetryStep` attempts to dismiss blocking UI overlays (such as cookie banners or popups) before re-attempting execution. Currently, the CSS selectors for overlay dismissal are hardcoded inside a JavaScript snippet in `PrepareRetryStep`:
```javascript
".modal, .overlay, .popup, [role=\"dialog\"], .cookie-banner, #cookie-consent"
```
This hardcoded list misses custom popups in modern web applications (e.g., OneTrust `#onetrust-consent-sdk`, Material UI `.MuiDialog-root`, or Tailwind overlays) and provides no mechanism for test authors to customize or disable overlay hiding.

### Proposal
Expose overlay dismissal selectors as a dynamic configuration property resolved via `AiConfiguration.getInstance()`:
```properties
# Configurable CSS selectors for automated overlay dismissal during retry
neodymium.ai.retry.overlaySelectors=.modal, .overlay, .popup, [role="dialog"], .cookie-banner, #cookie-consent, #onetrust-consent-sdk
```
* **Customization**: Allows test engineers to configure application-specific popup selectors per test suite or environment.
* **Disabling**: Setting `neodymium.ai.retry.overlaySelectors=""` disables automatic overlay hiding entirely for tests that explicitly test modal dialogs.
