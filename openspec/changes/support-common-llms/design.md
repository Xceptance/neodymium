## Context

To maximize reliability and integration simplicity, Neodymium will bundle the dependencies for our premium and local multimodal providers and configure them via a direct, compile-time verified factory (`ChatModelFactory`). This completely avoids dynamic reflection, keeping the code highly maintainable, type-safe, and fully navigable in modern IDEs.

## Goals / Non-Goals

**Goals:**
- Provide direct, pre-packaged support for `GoogleAiGeminiChatModel`, `VertexAiChatModel`, `MistralAiChatModel`, and `OllamaChatModel`.
- Package dependencies in the main `pom.xml` so they are immediately available downstream.
- Support multimodal vision inputs and strict JSON outputs for all supported models.

**Non-Goals:**
- Using reflection or dynamic classloading at runtime to construct models.
- Supporting general providers outside this core target group (e.g. OpenAI, Anthropic).

## Decisions

- **Type-Safe Factory Implementation (`ChatModelFactory`)**:
  We will introduce a class `ChatModelFactory` in `com.xceptance.neodymium.ai.core`:
  ```java
  public class ChatModelFactory {
      public static ChatModel createModel(String provider, Map<String, String> properties) {
          switch (provider.toLowerCase()) {
              case "gemini":
                  return GoogleAiGeminiChatModel.builder()
                          .apiKey(properties.get("apiKey"))
                          .modelName(properties.getOrDefault("modelName", "gemini-1.5-pro"))
                          .build();
              case "vertex":
                  return VertexAiChatModel.builder()
                          .project(properties.get("projectId"))
                          .location(properties.get("location"))
                          .modelName(properties.getOrDefault("modelName", "gemini-1.5-pro"))
                          .build();
              case "mistral":
                  return MistralAiChatModel.builder()
                          .apiKey(properties.get("apiKey"))
                          .modelName(properties.getOrDefault("modelName", "mistral-large-latest"))
                          .build();
              case "ollama":
                  return OllamaChatModel.builder()
                          .baseUrl(properties.getOrDefault("baseUrl", "http://localhost:11434"))
                          .modelName(properties.getOrDefault("modelName", "llava"))
                          .build();
              default:
                  throw new IllegalArgumentException("Unknown LLM provider: " + provider);
          }
      }
  }
  ```

- **Configuration Properties Structure**:
  - `neodymium.ai.provider` (`gemini`, `vertex`, `mistral`, `ollama`)
  - **Gemini**: `neodymium.ai.gemini.apiKey`, `neodymium.ai.gemini.modelName`
  - **Vertex**: `neodymium.ai.vertex.projectId`, `neodymium.ai.vertex.location`, `neodymium.ai.vertex.modelName`
  - **Mistral**: `neodymium.ai.mistral.apiKey`, `neodymium.ai.mistral.modelName`
  - **Ollama**: `neodymium.ai.ollama.baseUrl`, `neodymium.ai.ollama.modelName`

- **Dependencies**:
  Explicitly add standard model modules to `pom.xml`:
  - `dev.langchain4j:langchain4j-google-ai-gemini` (already present)
  - `dev.langchain4j:langchain4j-vertex-ai`
  - `dev.langchain4j:langchain4j-mistral-ai`
  - `dev.langchain4j:langchain4j-ollama`

## Risks / Trade-offs

- **Risk: Dependency Size Bloat**
  - *Trade-off*: Packaging multiple provider SDKs increases the Neodymium JAR dependency graph size.
  - *Mitigation*: We prioritize downstream developer experience ("batteries-included") and compilation robustness over a minimalist JAR footprint.
