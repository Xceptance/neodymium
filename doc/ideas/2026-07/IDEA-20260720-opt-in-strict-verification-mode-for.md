# [IDEA-20260720] Opt-In Strict Verification Mode for Replay

- **Status:** `Implemented`
- **Proposed:** 2026-07-20
- **Resolved:** 2026-10-01
- **Component:** `neodymium-core (ExecutionMode.REPLAY_STRICT, VerifyOutcomeStep)`
- **Category:** `Architecture & Core`
- **Author:** Neodymium Core Team

---

Currently, replay mode skips `VerifyOutcomeStep` execution for maximum execution speed (`if (mode.isReplay()) return;`). Even during live runs, failed outcome checks produce soft log warnings rather than failing the test run. While this maximizes execution speed, visual SSIM and selector resolution alone cannot catch subtle text/behavioral regressions (e.g., text changing from "Order Confirmed" to "Order Failed" while maintaining identical element structure).

### Proposal
Introduce an opt-in strict verification configuration flag:
```properties
# Enable strict semantic outcome verification (fails test on outcome assertion failure, including on replay)
neodymium.ai.verification.strictMode=true
```
* **Default (`false`)**: Fast execution — soft warnings logged in live mode; verification skipped on replay.
* **Strict (`true`)**: Verification steps run on replay and throw a `ConclusiveFailureException` if outcome checks or semantic assertions fail, enforcing strict semantic regression testing.
