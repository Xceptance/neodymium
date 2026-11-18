# [DEF-20261118-1301] Spring WebFlux 7.0.8 Transitive CVE Edge via spring-ai-mistral-ai in Aura Manager

- **Status:** `Resolved`
- **Opened:** 2026-11-18 12:40
- **Closed:** 2026-11-18 13:04
- **Component:** `aura-manager`
- **Scope:** `Infra/Build`
- **Symptom:** Socket Security scan on GitHub PR #564 flagged two Critical CVE alerts for `org.springframework:spring-webflux@7.0.8` along the path `aura-manager/pom.xml -> org.springframework.ai:spring-ai-mistral-ai@2.0.0 -> org.springframework:spring-webflux@7.0.8`:
  1. `GHSA-9qf2-26p9-2q2q`: Spring Framework Header Predicate Bypass in WebFlux Functional Endpoints (Critical, patched in `7.0.9`).
  2. `GHSA-j9f9-w8pj-32f8`: Spring Framework Server Sent Event stream corruption while rendering fragments (Critical, patched in `7.0.9`).
- **Root Cause:** Although `aura-manager/pom.xml` sets `<spring-framework.version>7.0.9</spring-framework.version>` for `spring-boot-starter-parent:4.1.0`, `org.springframework.ai:spring-ai-mistral-ai:2.0.0` explicitly declares `<artifactId>spring-webflux</artifactId><version>7.0.8</version>` in its own POM, and `aura-manager/pom.xml` neither excluded that `7.0.8` transitive edge nor imported `spring-framework-bom` / pinned `spring-webflux:7.0.9` directly inside its own `<dependencyManagement>` and `<dependencies>`. Bytecode inspection (`javap`) also confirmed `MistralAiApi` references `org.springframework.web.reactive.function.client.WebClient` (provided by `spring-webflux`), so excluding `spring-webflux` without keeping `7.0.9` would risk runtime `NoClassDefFoundError`.
- **Detection Gap ("What did we miss?"):** Local `mvn dependency:tree` already resolved `spring-webflux:7.0.9` through parent property inheritance, masking the unexcluded `7.0.8` edge declared inside `spring-ai-mistral-ai:2.0.0` until Socket's GitHub PR dependency analyzer reported it.
- **Resolution:**
  - Imported `org.springframework:spring-framework-bom:${spring-framework.version}` before `spring-ai-bom` in `aura-manager/pom.xml` `<dependencyManagement>`.
  - Added an `<exclusion>` for `org.springframework:spring-webflux` under `org.springframework.ai:spring-ai-mistral-ai` and explicitly declared `org.springframework:spring-webflux:${spring-framework.version}` (`7.0.9`).
- **Safety Net Added:**
  - Verified `mvn dependency:tree -pl aura-manager -Dincludes=org.springframework:spring-webflux` resolves `org.springframework:spring-webflux:jar:7.0.9:compile` directly with no `7.0.8` transitive edge.
  - Verified `AuraChatModelFactoryTest` (7/7 passing) alongside `AuraTestChatControllerTest` (19/19), `DashboardChatJsTest` (4/4), and `StaticResourceSyntaxTest` (83/83).
