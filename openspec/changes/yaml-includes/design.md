## Context

To modularize natural language playbooks and avoid copy-pasting of identical steps across different test suites, we need support for static and dynamic includes.

## Goals / Non-Goals

**Goals:**
- Modularize playbooks using static `_include:` directives.
- Support dynamic inclusions in conditional steps (`If ... then _include: ... else _include: ...`).
- Prevent circular inclusion loops (infinite recursion).
- Preserve line number/location tracing when steps are executed from includes.

**Non-Goals:**
- Regex-based evaluation of conditional steps in Java (delegated entirely to the LLM).

## Decisions

- **Interleaved Resolution:** Perform variable interpolation and inclusion expansion iteratively (up to 10 iterations) to support parametrized includes.
- **Trace Format:** Use string-based stack trace formatted strings like `included.steps:4 -> main.yaml:2` instead of plain line number integers.
- **Merge Logic:** Data-level inclusions unwrap nested `_data` or `data` keys and either splice or merge them.

## Risks / Trade-offs

- *Circular loops:* Prevented by keeping an active inclusion stack and checking paths on every inclusion.
