## Context

Test execution performance in Neodymium is heavily optimized through local fallbacks, playbooks, and caching. However, there are scenarios (such as testing the LLM integration itself, verifying non-deterministic logic, or debugging the AI pipeline) where we must guarantee that a specific step is processed live by the LLM, bypassing all local optimizations.

## Goals / Non-Goals

**Goals:**
- Provide a clear syntax in YAML playbooks to mark a step for enforced LLM execution.
- Update the execution engine to bypass local execution strategies and playbook caching when this marker is encountered.
- Ensure proper logging and telemetry to indicate a forced LLM step.

**Non-Goals:**
- Global enforcement of LLM (this is a per-step capability).
- Modifying how the LLM actually processes the step (just forcing the route).

## Decisions

- **Step Syntax:** We will use an explicit marker prefix `!(llm)` in the step string (e.g., `!(llm) Explain the image on the screen`).
  - *Rationale:* It's compact, integrates directly into the existing list-based string structure of steps without requiring a complex object notation, and is easy to parse during test loading.
  - *Alternatives Considered:* An object structure (e.g., `{ step: "Explain...", useLlm: true }`). This was rejected as it breaks the simple string-list nature of the playbook steps and complicates parsing unnecessarily.
- **Execution Bypass:** In `AiBrowser` or `PlaybookManager`, if a step is flagged with `!(llm)`, the engine will skip the playbook matching cache and local heuristic execution, routing the request directly to `LlmClient`.
  - *Rationale:* Ensures we never hit a cached path for steps flagged for live evaluation.

## Risks / Trade-offs

- [Risk] Performance degradation if users overuse the `!(llm)` flag.
  → Mitigation: Clear documentation outlining that this should only be used for debugging or specifically evaluating non-deterministic tasks.
