# [IDEA-20260717] Pluggable Verification Modules

- **Status:** `Proposed`
- **Proposed:** 2026-07-17
- **Resolved:** Pending
- **Component:** `neodymium-core (VerificationRegistry, VerifyOutcomeStep)`
- **Category:** `Architecture & Core`
- **Author:** Neodymium Core Team

---

Currently, page state verification focuses primarily on functional outcomes (e.g., did the page transition, or did an action complete successfully). However, real-browser test suites are also an excellent opportunity to audit content and UI standards. Introducing a pluggable, modular verification system would allow teams to opt-in to non-functional quality gates without overloading the core functional execution logic.

---

### Proposal
Introduce a pluggable verification framework where developers can register optional validation modules to run during or after test execution:
* **Locale Consistency Modules:** Verify that dynamic content (e.g., dates, times, currencies, and numbers) is formatted correctly according to the target locale configurations.
* **Language & Editorial Quality Modules:** Run automated checks using specialized LLM prompts to verify spelling, grammar, and adherence to specific brand guidelines (voice, tone, terminology).
* **Image-Text Consistency Modules:** Use multimodal LLMs to analyze page content and verify that visual assets (images, banners, product pictures) match their accompanying text descriptions (e.g., flagging placeholder images, or detecting if a "Red Jacket" product page displays an image of a blue shirt).
* **Visual Layout & Template Consistency Modules:** Match the current page layout against baseline template images or visual layout structures defined for specific page types (e.g., PLP, PDP, Checkout) to ensure structural and design consistency. This checks that layout blocks, headers, and footers align with the reference template, which may require capturing and evaluating "long" (full-page scroll) screenshots.
* **Local Screenshot Reference Comparison Module:** Validate the current page's visual state against a reference screenshot stored on the local filesystem. The system instructs the multimodal LLM to compare the live screen capture with the reference file using a user-specified comparison standard. The evaluation supports various comparison modes and criteria:
  * **Identical / Pixel-Approximate:** Confirming that the layout, structure, and spacing are identical to the baseline, allowing only minor background noise or anti-aliasing differences.
  * **Similar Design / Structural Equivalence:** Checking if the page retains a similar visual design, alignment, grid structure, and branding, even if the actual content or images have been updated.
  * **Different Locale Validation:** Evaluating the layout of a translated or localized page against a master reference to ensure text expansions, currency symbols, and multi-byte characters do not break the design or alignment.
  * **Missing or Excess Elements Detection:** Prompting the LLM to inspect both images side-by-side to call out specific missing elements (e.g., a header button or promo banner that is gone) or unexpected extra components (e.g., a stray modal or double footer).
* **Accessibility (a11y) Auditing Modules:** Run accessibility checks (e.g., validating ARIA labels, color contrast ratios, semantic HTML structure, and keyboard navigability) either as a main step action within the test flow or as an optional post-step audit check.
