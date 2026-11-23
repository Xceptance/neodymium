[![Maven Central](https://img.shields.io/maven-central/v/com.xceptance/neodymium-library)](https://mvnrepository.com/artifact/com.xceptance/neodymium-library)

# Neodymium Classic

**Neodymium Classic** (`neodymium-classic`) is the mature, code-first test automation framework within the Neodymium ecosystem. It solves modern UI automation challenges by combining **JUnit 4 / JUnit 5**, **Selenium WebDriver**, **Selenide**, **BDD/Cucumber**, and comprehensive **Allure** reporting into a cohesive, structured architecture.

While **Neodymium AI** (`neodymium-core`) offers natural language playbook execution, **Neodymium Classic** provides full programmatic control in Java using the `com.xceptance.neodymium.*` namespace.

---

## Included Projects & Integrations

Neodymium Classic combines and enhances battle-tested open-source libraries:

* [**JUnit 5 / JUnit 4**](https://github.com/junit-team): Robust test engine support with seamless IDE and CI/CD integration.
* [**Selenium WebDriver**](https://github.com/SeleniumHQ/selenium): The industry standard for browser communication.
* [**Selenide**](https://github.com/codeborne/selenide): Fluent, compact web element abstractions with built-in smart waits.
* [**Allure Report**](https://github.com/allure-framework/allure2): Clean, multi-language test report generation with step hierarchies and screenshots.
* [**BDD / Cucumber**](https://github.com/cucumber/cucumber-jvm): Native support for behavior-driven specifications.
* [**Owner**](https://github.com/lviggiano/owner): Declarative, multi-environment configuration management.

---

## Core Features

* **Multi-Browser Support**: Declarative browser matrices per test via annotations (`@Browser`), supporting desktop, mobile emulation, and remote grids.
* **Page & Component Concept**: Structured design patterns for modular, maintainable, and reusable page and component objects.
* **Data-Driven Testing**: Externalized dataset management (`@DataSet`) with automated test iteration per data row.
* **Localization**: Seamless execution of localized test cases combined with dataset management.
* **Concurrent Execution**: Thread-safe parallel test execution via Maven Surefire and dynamic browser instances.

---

## Getting Started

### 1. Maven Dependency

Add `neodymium-classic` to your `pom.xml`:

```xml
<dependency>
    <groupId>com.xceptance</groupId>
    <artifactId>neodymium-classic</artifactId>
    <version>6.0.0-BETA</version>
    <scope>test</scope>
</dependency>
```

### 2. Writing a Classic Test

Annotate your test method with `@NeodymiumTest` to activate Neodymium execution, automatic browser management, and reporting:

```java
package com.xceptance.neodymium.tests;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_1024x768")
public class StorefrontClassicTest
{
    @NeodymiumTest
    public void testHomepage()
    {
        open("https://www.xceptance.com");
        $("#navigation").shouldBe(visible);
    }
}
```

---

## Quicklinks & Resources

* [**Documentation & Guides**](https://docs.xceptance.com/neodymium)
* [**Neodymium Showcase Repository**](https://github.com/Xceptance/neodymium-showcase): Code snippets demonstrating individual features.
* [**Neodymium Template**](https://github.com/Xceptance/neodymium-template): Quickstart project template for Java and Cucumber.
* [**Neodymium Pure Java Example**](https://github.com/Xceptance/neodymium-example): Reference demo suite against the Posters demo store.
* [**Neodymium Cucumber Example**](https://github.com/Xceptance/neodymium-cucumber-example): BDD-driven test suite.

---

## Neodymium in the Media

* [Neodymium – An Open Source Framework for Web Testing](https://blog.xceptance.com/2019/02/26/neodymium-an-open-source-framework-for-web-testing/)
* [Neodymium 5.0.0 – A New Magnetic Force](https://blog.xceptance.com/2024/06/26/a-new-magnetic-force-neodymium-5-0-0-release/)
* [Neodymium 5.1 – Get More Done Faster](https://blog.xceptance.com/2025/02/11/neodymium-5-1-get-more-done-faster/)
* [Automated Accessibility Testing with Google Lighthouse and Neodymium](https://blog.xceptance.com/2025/03/06/automated-accessibility-testing-with-google-lighthouse-and-neodymium/)
* [Web Testing mit Neodymium (Video - German)](https://www.youtube.com/watch?v=hn-juzcXrZg)

---

## License & Attribution

Neodymium Classic is licensed under the [MIT License](https://opensource.org/licenses/MIT). Developed and maintained by [Xceptance Software Technologies GmbH](https://www.xceptance.com).

---

*This document was generated with the assistance of AI and reviewed by Xceptance experts.*
