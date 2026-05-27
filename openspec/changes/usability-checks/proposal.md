## Why

Automated functional testing verifies if a web application functions according to technical requirements, but completely overlooks user experience (UX) and usability health. By integrating automated, deterministic usability heuristics directly into Neodymium's AI Playbooks, QA engineers can easily audit and guarantee critical UX guidelines—such as identifying dead-end elements, poor readability, and missing empty states—without relying on manual reviews or heavy external scanning tools.

## What Changes

- **Usability Diagnostics Tag**: Support a case-insensitive `(usability)` or `[usability]` tag anywhere within natural language script steps to route instructions directly to the Usability Diagnostics Action.
- **Usability Diagnostics Action**: Register `UsabilityDiagnosticsAction` in the AI Action Registry to parse and execute usability validation steps.
- **Dead-End Interactivity Scan**: Implement an automated heuristic check in the browser to identify elements styled as interactive (e.g., `cursor: pointer` or interactive class styling) that have no associated click handlers, `href` attributes, or keyboard focusability.
- **Dynamic Text Readability Audit**: Implement text readability grade score evaluation (such as Flesch-Kincaid or automated length metrics) to flag overly complex sentences and dense page paragraphs.
- **Empty State Validation**: Implement dynamic diagnostics to ensure lists, search results, or data tables that render empty display an explicit user-friendly helper message or placeholder text, preventing dead-ends.
- **Viewport Usability Health Auditing**: Support a full page usability audit via the `(usability)` tag to scan for:
  - Unintended horizontal scrollbars (identifying overflowing elements).
  - Visually clipped or cut-off text content that is hidden without ellipsis.
  - Active interactive elements (buttons, links, inputs) positioned completely off-screen.

## Capabilities

### New Capabilities
- `usability-diagnostics`: Introduces automated browser heuristics to detect broken clickability patterns (dead-end targets), check dynamic copy readability, verify user-friendly empty state handling, and execute full viewport usability health checks.

### Modified Capabilities

## Impact

- **AI Core Routing**: Update Neodymium's routing system to parse `(usability)` and `[usability]` tags.
- **AI Action Plugins**: Register `UsabilityDiagnosticsAction` as a pluggable AI action.
- **Heuristic Scanning Engine**: browser-side JavaScript utilities executed on-demand to perform readability, empty states, and viewport visual usability audits.
- **Test Reporting**: Integration with Neodymium's reporting/logging system to highlight UX violations.
