# [IDEA-20260807] Perceptual Screen Element-Level Masking for dHash Replay

- **Status:** `Proposed`
- **Proposed:** 2026-08-07
- **Resolved:** Pending
- **Component:** `neodymium-core (VisualAssertionEngine, DHashCalculator)`
- **Category:** `AI & VLM`
- **Author:** Neodymium Core Team

---

Perceptual visual hashing (dHash) is highly effective, but dynamic UI components (such as live clocks, active usernames, changing banners, or personalized recommendations) will cause the Hamming distance comparison to fail (exceeding the threshold of 15).
* **Concept:** Introduce region or locator-based visual masking using inline NLP or standard configurations.
* **Example Instruction:**
  ```yaml
  steps: |
    Verify that the checkout receipt is visually correct (visual) (mask: #live-time, .user-name).
  ```
* **Execution:** Prior to generating the 256-bit dHash, the framework uses Selenium to locate the masked selectors (`#live-time`, `.user-name`), retrieves their coordinates, and paints those exact bounding boxes solid black on the captured screenshot buffer. This ensures robust visual validation on semi-dynamic pages.
