## Purpose

Standardizes the Quality Judge under the unified `AiPrompt<T>` interface and elevates its evaluation rubric to grade the entire 5-Tier candidate bundle during live recording.

## ADDED Requirements

### Requirement: Candidate Bundle Evaluation
The system SHALL pass the entire multi-tier locator bundle (Tiers 1–4) to the Quality Judge during live recording to verify that selectors are non-volatile, WCAG-compliant, and accompanied by a sufficiently rich DOM Feature Vector.

#### Scenario: Quality Judge evaluates 5-tier candidate set
- **WHEN** the primary extraction LLM generates candidate locators and feature vectors for an action
- **THEN** the Quality Judge evaluates the entire bundle and assigns a score from 1 to 10 with actionable feedback

### Requirement: Standardized Prompt Hierarchy
The system SHALL implement the `QualityJudgePrompt` using the generic `AiPrompt<QualityJudgeResult>` interface, loading its system instructions from external markdown templates under classpath.

#### Scenario: Compiling Quality Judge prompt through standard pipeline
- **WHEN** CallLlmStep invokes the Quality Judge
- **THEN** it executes via the polymorphic AiPrompt contract without requiring ad-hoc compileRequest() branching
