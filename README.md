[![Maven Central](https://img.shields.io/maven-central/v/com.xceptance/neodymium-library.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.xceptance%22%20AND%20a:%22neodymium-library%22) [![Join the chat at https://gitter.im/neodymium-library/community](https://badges.gitter.im/neodymium-library/community.svg)](https://gitter.im/neodymium-library/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# Neodymium
Neodymium tries to solve your typical and most pressing UI test automation problems by combining JUnit, WebDriver, BDD/Cucumber, and proper reporting. It gives you ready to use templates, assembles well-known open source projects, and enhances this all with additional functionality that is often missing.

Neodymium is basically the combination of state of the art open source test libraries with additional glue to make it stick reliably together.

## Included Projects
We combine and use several open source frameworks. Here are some honorable mentions:

* [**JUnit**](https://github.com/junit-team/junit4): It is the base test framework because JUnit support can be found everywhere, so it is the perfect base for easy integration and comprehensive IDE support. Right now it is on version 4, but as soon as version 5 features are high in demand, version 5 support will be added.
* [**WebDriver**](https://github.com/SeleniumHQ/selenium): Of course WebDriver is the core, because it is the only way to communicate with browsers consistently without using proprietary technologies.
* [**Selenide**](https://github.com/codeborne/selenide): There are plenty of UI automation libraries available that are built on top of WebDriver. Selenide is one of the most popular approaches that is compact, fluent, and solves a lot of the basic challenges, hence we included it.
* [**Allure**](https://github.com/allure-framework/allure2): The Allure Framework is a flexible lightweight multi-language test report tool that not only shows a very concise representation of what has been tested in a neat web report form, but allows everyone participating in the development process to extract the maximum of useful information from daily test execution.
* [**BDD/Cucumber**](https://github.com/cucumber/cucumber-jvm): Neodymium supports BDD. If you prefer this testing style, you can organize your tests using Cucumber. See our example projects for more information.
* [**Owner**](https://github.com/lviggiano/owner): Organize and implement configurations throughout the different stages of test projects.

## Quicklinks
If you already know what you want to do, you can pick one of the demo test suites or our test suite template. If you are new to Neodymium, please read on. 

* [Neodymium Template](https://github.com/Xceptance/neodymium-template): A template to start a new test automation suite quickly either as Cucumber or as pure Java. 
* [Neodymium Pure Java Example](https://github.com/Xceptance/neodymium-example): A demo test suite against the Xceptance reference demo store [Posters](https://github.com/Xceptance/neodymium-library/wiki/Posters-demo-store) as pure Java version.  
* [Neodymium Cucumber Example](https://github.com/Xceptance/neodymium-cucumber-example): For the fans of BDD/Cucumber, this is almost the same test suite as before but driven by BDD syntax.

## Introductions in other media
We have an article on [DZone](https://dzone.com/articles/neodymium-an-open-source-framework-for-web-testing) describing the main features of Neodymium.

We have a talk (in German) about the features and some best practices for Neodymium available on [YouTube](https://www.youtube.com/watch?v=hn-juzcXrZg).

## Additional Features
These are our **additions** to make test automation nicer, quicker, and less painful. 

* **Multi Browser Support**: Simple setup and use of different browsers including remote browsers. Annotation per test case defines what browsers are supported by this very test case. Automatic execution of the test case per browser.
* **Page and Component Concept**: Our example test suites demonstrate a page and component concept for easier test design. (Structural guidance)
* **Test Data**: Externalized test data for Java test cases including automatic execution per test data set.
* **Localization**: Simple concept to run localized test cases in conjunction with test data management.
* **Concurrent Execution**: Concurrent execution of tests with Maven.

## Getting Started
We recommend starting with our template instead of writing a suite from scratch. If you need more guidance, you can also start from one of our full demo test suites.

Please hop over to the [Wiki](https://github.com/Xceptance/neodymium-library/wiki/) for a full tour of Neodymium and a Getting Started guide. 

If you are still impatient, here is the quickest way to get Neodymium added to your project. Don't forget to update the version number if you prefer a particular version.

```xml
<dependency>
    <groupId>com.xceptance</groupId>
    <artifactId>neodymium-library</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
```
Add the `@RunWith` annotation to your test class or its superclass. This enables test execution with Neodymium.
```java
@RunWith(NeodymiumRunner.class)
public class MyTests
{
    @Test 
    public void testMethod()
    {
        // test code
    }
}
```
And now is the time to dive into the features we added to make the most out of it. [Please head over to our Wiki](https://github.com/Xceptance/neodymium-library/wiki/).

## License
Neodymium is licensed under the MIT License.

## Who Are We
We are [Xceptance](https://www.xceptance.com/en/). A software testing company with strong commerce knowledge and projects with customers from all around the world. Besides Neodymium, we have developed [Xceptance Load Test (XLT)](https://github.com/Xceptance/XLT), a load and performance test tool that is open source too (APL 2.0). It provides an extensive range of awesome features to make the tester's and developer's life easier.

If you are looking for test automation that also covers the performance side of life, take a look at XLT. You can write and run load tests with real browsers including access to data from the [Web Performance Timing API](http://w3c.github.io/perf-timing-primer/). In case browsers are too heavy, XLT has other modes of load testing to offer as well. 

We offer professional support for Neodymium and XLT as well as implementation and training services.
