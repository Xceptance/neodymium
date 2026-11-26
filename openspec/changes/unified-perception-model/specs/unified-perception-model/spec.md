## Purpose

Establishes a Unified Perception Model (UPM) that extracts semantic (AOM), structural (DOM), and geometric metadata for interactive elements, replacing the brittle data-ai hash with a comprehensive DOM Feature Vector.

## ADDED Requirements

### Requirement: Multi-Layer Perception Extraction
The system SHALL extract semantic accessibility roles, computed accessible names, test identifiers (`data-testid`, `data-qa`), clean CSS classes, text content, and normalized bounding box geometry in a single in-browser JavaScript evaluation pass.

#### Scenario: Full extraction of hybrid interactive element
- **WHEN** the browser analyzer scans an interactive element (e.g. `<button data-testid="cart-btn" class="btn-primary">Add to Bag</button>`)
- **THEN** it outputs an enriched perception node containing computedRole="button", computedName="Add to Bag", testId="cart-btn", classes=["btn-primary"], and spatial bounding box [x, y, w, h]

### Requirement: DOM Feature Vector Serialization
The system SHALL construct a persistent DOM Feature Vector capturing the target element's tag, classes, attributes, text, and relative structural hierarchy to support downstream local similarity matching.

#### Scenario: Capturing feature vector on action generation
- **WHEN** an action is generated during recording
- **THEN** the analyzer captures a structured Feature Vector containing tag name, attribute map, class set, text label, and sibling index
