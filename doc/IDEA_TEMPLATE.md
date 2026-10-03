# Idea Proposal Specification & Template

This document defines the architectural idea tracking workflow, lifecycle states, category taxonomy, and proposal template for all Neodymium future development.

To eliminate Git merge conflicts across concurrent branches, **never record architectural ideas in a single shared file**. Every idea must be authored in its own dedicated Markdown file under `doc/ideas/YYYY-MM/`.

---

## 1. File Location & Naming Convention

- **Directory:** `doc/ideas/YYYY-MM/` (partitioned by creation year and month)
- **File Format:** `IDEA-YYYYMMDD-HHmm-<slug>.md`
  - `YYYYMMDD`: 8-digit date (e.g. `20261003`)
  - `HHmm`: 4-digit local timestamp for natural intraday chronological sorting (e.g. `2015`)
  - `<slug>`: 2 to 6 kebab-case words summarizing the concept (e.g. `condensed-json-payloads`, `cdp-protocol-monitoring`)
- **Example:** `doc/ideas/2026-10/IDEA-20261003-2015-condensed-json-payloads.md`

---

## 2. Idea Lifecycle States

| Status | Definition | When to Use |
| :--- | :--- | :--- |
| **`Proposed`** | Concept drafted and submitted for team exploration and architectural feedback. | Initial proposal creation. |
| **`Under Review`** | Active technical review, feasibility prototyping, prompt experimentation, or benchmarking in progress. | When actively evaluating design or trade-offs. |
| **`Accepted`** | Concept approved by maintainers; scheduled for roadmap or OpenSpec change (`openspec/changes/`). | Pre-implementation agreement reached. |
| **`In Progress`** | Actively being implemented on a feature branch. | Development started. |
| **`Partially Implemented`**| Core subset or initial milestone delivered; remaining capabilities pending. | Work in flight across multiple PRs. |
| **`Implemented`** | Fully implemented, tested, verified by regression tests, and merged. | Feature shipped. Requires `- **Resolved:** YYYY-MM-DD`. |
| **`Deferred`** | Valuable concept deprioritized or postponed for a future milestone or engine iteration. | Parked without discarding. |
| **`Rejected`** | Evaluated and determined infeasible, unneeded, or superseded by an alternative design. | Closed with explanation in Alternatives. |

---

## 3. Category Taxonomy

Classify the primary technical domain in the `Category:` field:

| Category | Domain & Coverage | Typical Neodymium Scenarios |
| :--- | :--- | :--- |
| **`Architecture & Core`** | Core engine, pipeline steps, state machine, provider registry, runners, execution modes. | Dynamic LLM provider bindings, pluggable verification registry, `@AiPlaybook` annotation scoping. |
| **`AI & VLM`** | Vision-Language Models, visual grounding, visual markers, Visual RCA, multimodal judge, prompt tuning. | Set-of-Marks visual badges, dHash region masking, multi-crop disambiguation, premise bias elimination. |
| **`Protocol & Monitoring`**| Browser protocol interception (CDP, WebDriver BiDi), console logs, and network traffic. | Real-time JS exception capture, failed HTTP request flagging, network assertion tags. |
| **`Tooling & DX`** | Developer experience, IDE plugins (IntelliJ/VSCode), visual playbook editors, linters, sync services. | Language Server Protocol (LSP) diagnostics, step tracer extension, one-click self-healing sync. |
| **`Performance & Cost`** | Token reduction, response minification, latency tuning, and execution caching. | Condensed JSON payload schemas, empirical wait tuning, context escalation caching. |
| **`Autonomous QA`** | Self-directed exploratory testing, Large Action Models (LAMs), automated test generation. | SBTM charter execution, automated playbook synthesis, dynamic AI data mutation & fuzzing. |

---

## 4. Idea Template (Copy & Paste)

```markdown
# [IDEA-YYYYMMDD-HHmm] Concise Idea Title

- **Status:** `Proposed` | `Under Review` | `Accepted` | `In Progress` | `Partially Implemented` | `Implemented` | `Deferred` | `Rejected`
- **Proposed:** YYYY-MM-DD HH:mm
- **Resolved:** YYYY-MM-DD HH:mm (or `Pending`)
- **Component:** `neodymium-core` / `aura-manager` / etc.
- **Category:** `Architecture & Core` | `AI & VLM` | `Protocol & Monitoring` | `Tooling & DX` | `Performance & Cost` | `Autonomous QA`
- **Author:** Author Name / Team

## 1. Problem Statement & Motivation
What limitation, inefficiency, or testing challenge does this proposal address? Why is current behavior insufficient?

## 2. Proposed Architecture & Design
Detailed technical design, architecture diagrams (Mermaid), proposed API interfaces, configurations, or workflow stages.

## 3. Expected Impact & Trade-offs
- **Benefits:** Token savings, latency improvements, test reliability, or UX gains.
- **Risks & Complexity:** Added dependencies, maintenance burden, failure modes, or breaking changes.

## 4. Open Questions & Alternatives Considered
What alternate architectures were evaluated? What questions remain open before implementation?
```
