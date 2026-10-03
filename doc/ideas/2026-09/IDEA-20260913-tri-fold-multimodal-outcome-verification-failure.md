# [IDEA-20260913] Tri-Fold Multimodal Outcome Verification & Failure RCA

- **Status:** `Proposed`
- **Proposed:** 2026-09-13
- **Resolved:** Pending
- **Component:** `neodymium-core (VerificationPrompt, VisualRcaPrompt)`
- **Category:** `AI & VLM`
- **Author:** Neodymium Core Team

---

Outcome verification (`VerificationPrompt`) evaluates pre- and post-action visual screenshots and executed actions. When an API endpoint fails silently or a JavaScript runtime error crashes an event handler, the visual screenshot may appear normal, causing the LLM Judge to award an erroneous "PASS".

---

### Proposal: Tri-Fold Telemetry Injection

Inject protocol telemetry directly into `VerificationPrompt` and `VisualRcaPrompt`:

#### 1. Upgraded `absenceOfErrors` Rubric
Include the step's captured console errors and failed network requests in the LLM payload:
```json
{
  "protocolTelemetry": {
    "consoleErrors": [
      "Uncaught TypeError: Cannot read properties of undefined (reading 'items') at checkout.js:142"
    ],
    "failedNetworkRequests": [
      {
        "url": "https://example.com/api/v2/cart/checkout",
        "method": "POST",
        "status": 500,
        "statusText": "Internal Server Error"
      }
    ]
  }
}
```
If any unhandled exceptions or HTTP $\ge 400$ errors occurred during the step, the LLM Judge scores `absenceOfErrors` as **FAIL** with the exact backend/frontend failure cited in the rubric analysis.

#### 2. Enriched Visual Root Cause Analysis (Visual RCA)
When a test fails, include the latest network and console error stack traces in the Visual RCA payload. This enables the RCA model to determine whether a missing UI element was caused by a CSS rendering defect, a failed API response, or a crashed JavaScript event listener.
