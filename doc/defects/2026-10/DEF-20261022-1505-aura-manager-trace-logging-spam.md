# [DEF-20261022-1505] Log4j2 and Spring Boot Trace Level Console Flooding in Aura Manager Startup

- **Status:** `Resolved`
- **Opened:** 2026-10-22 14:55
- **Closed:** 2026-10-22 15:05
- **Component:** `aura-manager` / `neodymium-core` / `neodymium-ai-testcase-hub`
- **Scope:** `Config/Environment`
- **Symptom:** Running `run-aura.sh` emits an excessive torrent of internal Log4j2 status lines and TRACE log entries from Spring Framework, Hibernate, Tomcat, and Neodymium onto standard output.
- **Root Cause:** In `neodymium-ai-testcase-hub`, `log4j2.properties` had `status = ALL` and `rootLogger.level = ALL` configured alongside `appender.console.filter.threshold.level = trace`. When `run-aura.sh` launched `AuraManagerApplication` within the test classpath scope, SLF4J bound to Log4j2, causing all internal subsystem logs across the entire JVM to print at TRACE level to console. In `neodymium-core`, the console appender filter threshold was also configured to `trace`, and `aura-manager` lacked explicit default `logging.level` settings in `application.properties`.
- **Detection Gap ("What did we miss?"):** Test suites in CI run with log redirect or headless assertions that do not check console log volume or threshold levels of standard console appenders.
- **Resolution:** Reverted `status` to `WARN`, reset `rootLogger.level` and console filter threshold to `info` across `neodymium-ai-testcase-hub` and `neodymium-core`, and added explicit Spring Boot `logging.level` configurations (`logging.level.root=INFO`, `logging.level.com.xceptance.aura=INFO`, `logging.level.org.springframework=INFO`, `logging.level.org.hibernate=INFO`) to `aura-manager/src/main/resources/application.properties`.
- **Safety Net Added:** Added automated regression test `StaticResourceSyntaxTest.testAuraManagerLoggingConfigurationContracts` verifying that `application.properties` specifies `logging.level.root=INFO` and `logging.level.com.xceptance.aura=INFO`, and that `log4j2.properties` does not use `status = ALL` or console threshold `trace`.
