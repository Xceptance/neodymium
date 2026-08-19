## Purpose

Introduces a 5-Tier fallback chain for element resolution and a local Feature Proximity Search engine to enable resilient, sub-millisecond local self-healing during replay without incurring remote LLM token costs.

## ADDED Requirements

### Requirement: 5-Tier Cascading Resolution
The system SHALL evaluate element locators during replay in the following strict priority order: Tier 1 (Engineering Test-IDs) -> Tier 2 (Semantic / AOM) -> Tier 3 (Text & CSS) -> Tier 4 (Feature Proximity Search) -> Tier 5 (Visual Anchor & Coordinates).

#### Scenario: Native Tier 1 match on unchanged UI
- **WHEN** the replay runner evaluates an action with a matching data-testid
- **THEN** it executes the action against the Tier 1 locator in $<1\text{ ms}$ without evaluating subsequent tiers

#### Scenario: Cascading down to Tier 4 on frontend refactor
- **WHEN** the primary Test-ID, Semantic, and Text locators all fail to find the element
- **THEN** the system activates Tier 4 Feature Proximity Search to score candidate elements against the recorded DOM Feature Vector

### Requirement: Dual Locator Storage in Companion Recordings
The system SHALL serialize both concrete selectors (for Tiers 1–3) and the complete DOM Feature Vector (for Tier 4) into companion `.json` recordings.

#### Scenario: Storing multi-candidate action definitions
- **WHEN** a test step is recorded in FORCE_RECORDING mode
- **THEN** the output JSON contains concrete locator strings for Tiers 1–3 alongside the structured Feature Vector object

### Requirement: Feature Proximity Similarity Scoring
The system SHALL calculate candidate element match scores using a composite weighted formula: Jaccard set similarity on attributes (30%) and classes (10%), Levenshtein string distance on text/labels (25%), and Tag match with semantic equivalence bucketing (35%).

#### Scenario: Resolving Tailwind CSS class migration locally
- **WHEN** an element's CSS classes change from Bootstrap to Tailwind but attributes and text match
- **THEN** the composite similarity score exceeds 80% and the runner clicks the element without calling the remote LLM

### Requirement: Semantic Tag Equivalence Bucketing
The system SHALL assign partial tag compatibility credit ($0.70$) when an element transitions across equivalent interactive HTML tags (such as `<button>`, `<a>`, `<div role="button">`, or `<input type="submit">`) with identical text and accessible intent.

#### Scenario: Element refactored from button to anchor tag
- **WHEN** a `<button>Save</button>` is refactored into `<a href="#" class="btn">Save</a>`
- **THEN** the tag score receives partial credit and the composite score exceeds 80%, successfully resolving the element
