## Context

Neodymium's AI Playbooks allow functional testing using natural language descriptions, but do not verify the usability or user experience (UX) quality of the pages. This design introduces a new AI Action plugin, `UsabilityDiagnosticsAction`, which runs highly optimized, deterministic usability audits directly inside the browser using clean JavaScript heuristics.

This avoids slow WebDriver network overhead and eliminates the need for heavyweight external UX analysis tools.

## Goals / Non-Goals

**Goals:**
- Provide a natural language usability assertion mechanism inside AI Playbooks via a new `(usability)` tag.
- Implement highly accurate browser-side heuristics to detect dead-end interactive elements (clickable styles with no actions).
- Calculate copy complexity and readability grade levels offline in JS without external Maven/JS libraries.
- Standardize empty state verification to prevent blank page dead-ends on empty dynamic elements.
- Support full page visual usability health audits (horizontal scroll overflow, visually clipped text, and offscreen controls).
- Report clear usability violations with exact element details and coordinates.

## Non-Goals:
- Creating a general-purpose natural language translation or proofreading engine.
- Performing visual rendering layout checks (which are handled by the layout engine).
- Fixing or rewriting bad page copy or empty states automatically.

## Decisions

### 1. Route `(usability)` Tag to `ContextLevel.VISUAL`
- **Decision**: Update `AiAgent.java`'s `getInitialContextLevel` method to recognize case-insensitive `(usability)` and `[usability]` tags anywhere in the step string and route them directly to `ContextLevel.VISUAL`.
- **Rationale**: Usability checks require access to full text coordinates, sibling elements, classes, and computed styling (such as `cursor: pointer`). Starting directly at `ContextLevel.VISUAL` ensures the LLM receives the complete DOM structure immediately on the first turn, avoiding expensive context escalations.

### 2. Single-Injection JavaScript Audits
- **Decision**: Execute all heuristic scans (dead-end detections, readability grade calculations, empty states checking, and viewport health checks) inside a single, highly optimized browser-side JavaScript payload.
- **Rationale**: Querying multiple individual elements or computing computed styles element-by-element from Java via WebDriver would trigger hundreds of network roundtrips, slowing down the test suite drastically. Implementing these audits directly in a single injected JS loop ensures execution times under 20ms.

### 3. Syllable and Readability Heuristics in JS
- **Decision**: Implement a lightweight syllable-counting and Flesch-Kincaid readability index algorithm directly in JavaScript, rather than importing massive linguistic libraries into Java.
- **Rationale**: A basic regex-based syllable count for English vowel groups is highly accurate (90%+) and completely sufficient for automated QA copy complexity checks, keeping Neodymium's footprint compact and zero-dependency.

### 4. Dead-End Clickability Detection Heuristics
- **Decision**: Identify interactive elements via computed style: `window.getComputedStyle(el).cursor === 'pointer'` or frameworks-specific classes (e.g. `btn`, `btn-primary`, `nav-link`). An element is flagged as a dead-end *only* if it lacks all of the following:
  1. An active, non-empty `href` attribute (excluding `#` or `javascript:void(0)`).
  2. An inline `onclick` handler.
  3. Active event listener markers or input/submit roles.
  4. Parent elements that are already valid anchors or buttons.
- **Rationale**: This heuristic precisely targets decorative icons or static text divs that have had `cursor: pointer` added by mistake without actually binding click behavior, which is a very common front-end UX bug.

### 5. Noise-Free Viewport Health Auditing
- **Decision**: When executing a full viewport usability health audit, do NOT run generalized, noise-prone $O(N^2)$ element-to-element overlap collision detection. Instead, execute three highly targeted, deterministic, $O(N)$ audits inside the browser:
  - **Horizontal Scrollbar Culprit Check**: Detect if `document.documentElement.scrollWidth > window.innerWidth`. If so, identify the visible element(s) whose bounding box `right > window.innerWidth` and do not have an ancestor with `overflow-x: hidden`.
  - **Clipped Text Content Check**: Scan visible text leaf elements (paragraphs, headings, labels) and verify that their scroll width/height does not exceed their client width/height. Ignore elements using standard CSS truncation (`text-overflow: ellipsis`) or visible overflow.
  - **Off-Screen Interactivity Check**: Ensure all visible, active interactive controls (buttons, links, inputs) have bounding box coordinates fully contained within the active viewport bounds.
- **Rationale**: An unguided visual collision audit is slow and generates massive numbers of false positives on wrapping layers, nested containers, and background graphics. Restructuring the audit around horizontal overflow, dynamic text truncation, and off-screen push guarantees zero-false-positive layout validations that catch real usability bugs.

## Risks / Trade-offs

- **[Risk] Multilingual Readability Scoring** → The Flesch-Kincaid formula is strictly designed for English text. Evaluating German or other language copy using English syllable rules will return inaccurate readability grades.
  - *Mitigation*: Fallback to basic sentence-length and word-count threshold warnings when non-English locales are active, and support disabling readability audits for unsupported locales.
- **[Risk] Framework Event Listeners (React/Vue/Angular)** → Modern single-page applications often attach event listeners dynamically in JS, meaning standard attributes like `onclick` are empty, potentially creating false positive dead-end flags.
  - *Mitigation*: Check if the element has active framework attributes, or if click events bubble up. Also, allow configuring exclusions or selective selector bypasses directly in the natural language step (e.g. `except '.tab-trigger'`).
