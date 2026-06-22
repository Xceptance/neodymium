## ADDED Requirements

### Requirement: Pre-Packaged Provider Instantiation
The system SHALL support configuring and instantiating one of four type-safe, pre-packaged LLM providers: Google AI Gemini, GCP Vertex AI, Mistral AI, or Ollama.

#### Scenario: Instantiating Google AI Gemini
- **WHEN** the user configures `neodymium.ai.provider=gemini`
- **THEN** the system instantiates a `GoogleAiGeminiChatModel` with the configured API key and model properties.

#### Scenario: Instantiating GCP Vertex AI
- **WHEN** the user configures `neodymium.ai.provider=vertex`
- **THEN** the system instantiates a `VertexAiChatModel` with the configured project ID, location, and model properties.

#### Scenario: Instantiating Mistral AI
- **WHEN** the user configures `neodymium.ai.provider=mistral`
- **THEN** the system instantiates a `MistralAiChatModel` with the configured API key and model properties.

#### Scenario: Instantiating Ollama
- **WHEN** the user configures `neodymium.ai.provider=ollama`
- **THEN** the system instantiates an `OllamaChatModel` with the configured base URL and model properties.

### Requirement: Unified Configuration Validation
The system SHALL validate the presence of required configurations for the active provider and fail fast with detailed errors.

#### Scenario: Missing Vertex AI properties
- **WHEN** the user configures `neodymium.ai.provider=vertex` but omits `projectId` or `location`
- **THEN** the system throws an explicit configuration exception listing the missing properties.
