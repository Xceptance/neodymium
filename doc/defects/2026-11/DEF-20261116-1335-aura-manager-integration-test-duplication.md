# [DEF-20261116-1335] Aura Manager Integration Test Duplication Across Unit-Tests Profile

- **Status:** `Resolved`
- **Opened:** 2026-11-16 13:35
- **Closed:** 2026-11-16 13:40
- **Component:** `aura-manager`
- **Scope:** `Infra/Build & Test/Harness`
- **Symptom:** Spring context integration tests such as `CustomDashboardViewControllerIntegrationTest` and `AuraReportViewControllerSpringContextTest` were executed twice during test execution pipelines: once under profile `-Punit-tests` and once under profile `-Paura-manager`.
- **Root Cause:** In `aura-manager/pom.xml`, the `<id>unit-tests</id>` profile only excluded UI tests (`**/*UiTest.java` and `**/ui/**`). It lacked exclusions for Spring context integration suites (`**/*IntegrationTest.java` and `**/*SpringContextTest.java`). Consequently, heavy `@SpringBootTest` and `@WebMvcTest` suites intended for the dedicated `aura-manager` profile ran redundantly inside the fast in-memory `unit-tests` profile.
- **Detection Gap ("What did we miss?"):** Profile exclusions between the root `pom.xml` contract (lightweight in-memory tests without browsers, servers, or external lifecycles) and submodule `aura-manager/pom.xml` were not synchronized. Submodule profile definition only guarded against Selenium UI tests, allowing Spring Boot container tests to leak into unit test execution.
- **Resolution:** Added `<exclude>**/*IntegrationTest.java</exclude>` and `<exclude>**/*SpringContextTest.java</exclude>` to maven-surefire-plugin configuration under profile `unit-tests` in `aura-manager/pom.xml`.
- **Safety Net Added:** Verified `mvn test -Punit-tests -pl aura-manager` executes all 871 pure unit tests with zero integration test duplication. Verified `mvn test -Paura-manager -pl aura-manager -Dtest=CustomDashboardViewControllerIntegrationTest` executes and passes in the dedicated `aura-manager` profile.
