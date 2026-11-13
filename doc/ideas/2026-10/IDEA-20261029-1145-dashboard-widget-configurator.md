# [IDEA-20261029-1145] Aura Dashboard Dynamic Widget Configurator & Multi-Scale Trend Engine

- **Status:** `In Progress`
- **Proposed:** 2026-10-29 11:45
- **Resolved:** Pending
- **Component:** `aura-manager`
- **Category:** `Tooling & DX`
- **Author:** AI-generated: Gemini 3.8 Flash (High) / Xceptance GmbH 2026

## 1. Problem Statement & Motivation
Aura Manager historically provided a fixed dashboard layout for test run history (composed of a static latest test status donut chart, token cost sparkline card, browser/tag ratio bars, and cumulative suite evolution area chart). Different testing stakeholders have fundamentally diverse reporting needs:
- Release engineers require **Perlenkette (pearl necklace / bead chain)** diagrams to evaluate pass rates and stability markers across pipeline stages and runs.
- QA leads require dedicated **Bug-centric** views (severity breakdowns and defect discovery trends).
- Performance & FinOps engineers require **LLM Token & Cost** tracking aggregated across hourly, daily, monthly, or per-run temporal buckets.
- Feature teams require **Tag-based distributions** displayed as pie charts, ratio bars, or trends.

Users need the ability to add, configure, customize, reorder, and persist their own dashboard widgets directly from the Run History page (`/` and `/batch-history`).

## 2. Proposed Architecture & Design
Introduce a modular widget engine supporting a 5-dimensional configuration matrix:
1. **Information (Metric Dimension):**
   - `STATUS`: Test outcome breakdown (Passed, Succeeded-Fixed, Failed-Known, Failed-Unknown, Ignored, with Clean, Healed, AI dimensions).
   - `COSTS`: LLM spending ($ amount, token volume, prompt vs completion).
   - `BUGS`: Known & active bugs, broken down by severity (Critical, Major, Minor, Trivial).
   - `TAG`: Any dynamic tag discovered across runs and variations (e.g. `browser`, `locale`, `area`, `release`, `smoke`).
2. **Way of Presentation:**
   - `PIE_CHART`: Circular donut breakdown with center KPI badge and breakdown list.
   - `RATIO_BAR`: Horizontal stacked proportion bar with legend chips.
   - `PERLENKETTE`: Sequential pearl necklace / bead chain connected by a midline track with stepper controls and interactive popovers.
   - `TREND_GRAPH`: Chronological stacked area or multi-line chart with axes, gridlines, and hover inspection.
3. **Target Point / Time Scope (for Static Point Charts: Pie Chart & Ratio Bar):**
   - `LATEST_TEST_STATUS`: Latest unique variation execution status across the test suite (preserving last known states).
   - `LAST_RUN`: The single most recent test run in history.
   - `TODAY`: Aggregated test runs executed today (since 00:00:00).
   - `LAST_HOUR`: Aggregated test runs executed in the preceding 60 minutes.
   - `LAST_MONTH`: Aggregated test runs executed in the preceding 30 days.
4. **Format Options (for Perlenkette & Value Displays):**
   - `PERCENT`: Displays percentage metric (e.g. `100%`, `85%`).
   - `ABSOLUTE`: Displays absolute count (e.g. `42 tests`, `3 bugs`, `$0.42`).
5. **Trend Aggregation Mode (for Perlenkette & Trend Graph):**
   - `BY_RUNS`: Discrete historical run points in chronological order.
   - `LATEST_APPROACH`: Cumulative known variation state progression (milestones across runs carrying forward un-run test variations in their last known state, identical to Suite Evolution Trend).
   - `DAILY`: Aggregated per calendar day.
   - `HOURLY`: Aggregated per hour.
   - `MONTHLY`: Aggregated per month.

The configuration is persisted via `ReportDashboardSettingService` (database + local JSON file) with full fallback to canonical default widgets when unconfigured. An **"+ Add Widget"** button is surfaced prominently on the Run History page toolbar and widgets section header.

## 3. Expected Impact & Trade-offs
- **Benefits:** Complete analytics customizability, support for bead-chain pipeline visualization, bug tracking metrics, and flexible cost monitoring.
- **Risks & Complexity:** Additional aggregation computation over time buckets. Mitigated by bounding data to active run limits and caching intermediate time-bucketed aggregations during the request lifecycle.

## 4. Open Questions & Alternatives Considered
- **Alternatives Considered:** Client-side charting libraries (Chart.js / D3). Rejected in favor of server-side SVG/HTML to preserve zero external CDN dependencies, instant HTMX swapping, and existing Allure/Aura theme consistency.
