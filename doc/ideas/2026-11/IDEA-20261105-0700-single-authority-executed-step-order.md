# [IDEA-20261105-0700] Make executed-step history the single authority for step order

- **Status:** `Proposed`
- **Proposed:** 2026-11-05 07:00
- **Resolved:** Pending
- **Component:** `neodymium-core`
- **Category:** `Architecture & Core`
- **Author:** AI-generated: Claude Sonnet 5.5 / Xceptance GmbH 2026

## 1. Problem Statement & Motivation
Three places order the steps of a running AI test independently:
- `InteractiveStateBuilder` builds the HUD order: before steps, then executed and pending main steps (minus before/after-block steps), then after steps.
- `InteractiveConsoleListener.updateFlatSteps()` rebuilds `playbook.flatSteps` as before, main, executed, after.
- The rewind logic computes its range as `before.size() + core.size()`, which assumes the layout of the second rule.

During a normal run `playbook.flatSteps` is built append-only in execution order, which differs from both structural orders (programmatic `@BeforeEach` steps execute before the playbook's own `before:` steps). After an interactive add or reorder in a hybrid test (YAML test method plus programmatic setup) the rebuild switches to the structural order, so step indices can shift mid-run, the HUD numbering can disagree with `flatSteps`, and rewind can reschedule the wrong range.

## 2. Proposed Architecture & Design
Do not introduce a helper that recomputes the order from the structural lists: it cannot reproduce execution order without a per-step sequence marker.

Instead make `playbook.flatSteps` (append-only, execution order) the single authority:
1. Interactive add and move operations edit `flatSteps` in place, positioning the step by identity next to its predecessor or successor (the anchor logic of `InteractiveConsoleListener.mirrorIntoExecutedSteps`).
2. `InteractiveStateBuilder` derives the `before`/`steps`/`after` blocks and the global index from `flatSteps` plus each step's `block`.
3. Rewind and skip-remaining logic locate their range by the identity of the first and last affected step, not by list sizes.
4. Retire `updateFlatSteps()` as a full rebuild.

## 3. Expected Impact & Trade-offs
- **Benefits:** One order for the HUD, reports, recordings and rewind; no index drift after interactive edits.
- **Risks & Complexity:** Every consumer of `playbook.flatSteps` is affected (`InteractiveStateBuilder` ×3, `InteractiveConsoleListener` ×6, `ExecuteActionsStep`, `StateMachineRunner`, `PlaybookRecorder`, `AiSession`, `NeodymiumAiRunner`). The recorder writes this order and strict replay matches it, so a wrong order breaks replays. The HUD step numbers users see and the `targetIndex` values their actions send would change. `updateFlatSteps` clears and refills the shared list in place while the runner thread may read it; a redesign should swap in a new list instead. Many tests build partial contexts by hand and two existing tests encode different orders. The legacy `com.xceptance.neodymium.ai.console` and `org.neodymium.ai` package roots both take part.

## 4. Open Questions & Alternatives Considered
- Should programmatic steps run in a `@BeforeEach` method be shown in the HUD's `before` block? That needs them registered in `playbook.beforeSteps`; the HUD filters `before`/`after` block steps out of `blocks.steps`.
- Alternative: a shared ordering helper plus a per-step execution sequence number. Rejected for now because it adds a field to every step and still needs the rewind range fix.
- Trigger frequency is low (interactive edit in a hybrid test), so write a failing reproduction first and size the work from it.
