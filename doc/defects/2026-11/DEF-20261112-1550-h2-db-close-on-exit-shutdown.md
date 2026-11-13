# [DEF-20261112-1550] H2 Database Closed Concurrently on VM Shutdown Causing JDBCConnectionException

- **Status:** `Resolved`
- **Opened:** 2026-11-12 15:50
- **Closed:** 2026-11-12 15:56
- **Component:** `aura-manager`
- **Scope:** `Config/Environment`
- **Symptom:** On server teardown (SIGINT, Ctrl-C, or graceful container termination), Hibernate and Spring log WARN and ERROR messages: `Database is already closed (to disable automatic closing at VM shutdown, add ";DB_CLOSE_ON_EXIT=FALSE" to the db URL) [90121-240]` and `Failed to release JPA EntityManager: org.hibernate.exception.JDBCConnectionException: Unable to release JDBC Connection [Database is already closed]`.
- **Root Cause:** In `aura-manager/src/main/resources/application.properties`, the H2 datasource URL was defined as `jdbc:h2:file:./data/auradb;AUTO_SERVER=TRUE`. By default, H2 registers a JVM shutdown hook (`DB_CLOSE_ON_EXIT=TRUE`). During server teardown, H2's JVM shutdown hook raced with Spring Boot's shutdown hook, closing the database engine before Spring's `OpenEntityManagerInViewInterceptor` and HikariCP connection pool finished releasing open connections and JPA EntityManagers. Furthermore, H2 2.x explicitly forbids combining `AUTO_SERVER=TRUE` and `DB_CLOSE_ON_EXIT=FALSE` (`Feature not supported: "AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE"`). Because `aura-manager` is a standalone embedded server without external processes accessing `auradb`, `AUTO_SERVER=TRUE` was unnecessary.
- **Detection Gap ("What did we miss?"):** Integration tests run within test runner JVMs that execute specific test methods but do not simulate interactive server shutdown or verify DataSource URL configuration flags against concurrent shutdown races between H2 and the Spring ApplicationContext.
- **Resolution:**
  1. In `aura-manager/src/main/resources/application.properties`, updated `spring.datasource.url` to `jdbc:h2:file:./data/auradb;DB_CLOSE_ON_EXIT=FALSE`, removing the unneeded `AUTO_SERVER=TRUE` and disabling H2's premature JVM exit hook so Spring Boot and HikariCP cleanly manage connection pool and database closing on server teardown.
  2. In `AuraReportViewControllerSpringContextTest.java`, updated the area assertions in `testRunReportRendersDefaultInsteadOfBrowsingDefaultForUntaggedTestsWhenRealAreasExist` to align with the clean area name formatting without `@`.
- **Safety Net Added:** Added `@Test` `testDatasourceUrlDisablesDbCloseOnExit` in `AuraReportViewControllerSpringContextTest` asserting that the configured `spring.datasource.url` contains `;DB_CLOSE_ON_EXIT=FALSE`.
