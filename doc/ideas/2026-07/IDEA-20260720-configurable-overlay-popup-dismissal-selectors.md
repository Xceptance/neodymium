# [IDEA-20260720] Configurable Overlay & Popup Dismissal Selectors

- **Status:** `Rejected` (Superseded)
- **Proposed:** 2026-07-20
- **Resolved:** 2026-10-03
- **Component:** `neodymium-core (PrepareRetryStep)`
- **Category:** `Architecture & Core`
- **Author:** Neodymium Core Team

> [!NOTE]
> **Superseded by Native Playbook Flow and Modular Includes:**
> Rather than maintaining a global hardcoded CSS selector regex or hidden dismisser in `PrepareRetryStep`, popup and overlay dismissal is modeled explicitly and declaratively via conditional playbook steps (e.g. `If cookie banner is visible, click Accept`) and reusable modular includes (e.g. `includes: - common/accept_cookies.yaml`).


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
