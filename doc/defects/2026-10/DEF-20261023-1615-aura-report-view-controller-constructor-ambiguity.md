# [DEF-20261023-1615] AuraReportViewController Spring Instantiation Failure Due to Constructor Ambiguity

- **Status:** `Resolved`
- **Opened:** 2026-10-23 16:15
- **Closed:** 2026-10-23 16:21
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** During standalone application startup (`com.xceptance.aura.AuraLauncher` / `AuraManagerApplication`), Spring Boot application context refresh fails with `BeanCreationException: Error creating bean with name 'auraReportViewController' ... Failed to instantiate [com.xceptance.aura.report.controller.AuraReportViewController]: No default constructor found` (`NoSuchMethodException: com.xceptance.aura.report.controller.AuraReportViewController.<init>()`).
- **Root Cause:** When `ReportDashboardSettingService` was introduced and injected into `AuraReportViewController`, an overloaded constructor was retained alongside the new 7-parameter constructor. Because neither constructor was marked with Spring's `@Autowired` annotation, Spring Framework's constructor resolution mechanism (`AbstractAutowireCapableBeanFactory.createBeanInstance`) determined that multiple non-annotated constructors existed. In the absence of an explicit `@Autowired` constructor or a no-argument default constructor (`<init>()`), Spring fell back to attempting default instantiation, which threw `NoSuchMethodException`. Similarly, `ReportDashboardSettingService` had both a 2-arg constructor and a mock 1-arg constructor without `@Autowired`.
- **Detection Gap ("What did we miss?"):** Existing controller unit tests (`AuraReportViewControllerTest`) instantiated `AuraReportViewController` directly in Java via `new AuraReportViewController(...)` without bootstrapping a real Spring Boot `ApplicationContext`. Because the Spring dependency injection container was never initialized in the test suite, container-level autowiring resolution bugs could not be caught prior to runtime.
- **Resolution:**
  1. Annotated `AuraReportViewController`'s canonical 7-parameter constructor with `@Autowired` and removed the redundant, un-annotated 6-parameter constructor to eliminate any constructor ambiguity.
  2. Annotated `ReportDashboardSettingService`'s primary constructor with `@Autowired`.
  3. Added an automated Spring Boot container integration test (`AuraReportViewControllerSpringContextTest`) that boots the full application context with `@SpringBootTest` to verify that `AuraReportViewController` is cleanly resolved and injected.
- **Safety Net Added:** Added permanent automated integration test `AuraReportViewControllerSpringContextTest.testAuraReportViewControllerBeanInstantiatesInContext` verifying Spring `ApplicationContext` initializes and wires `AuraReportViewController` without errors.
