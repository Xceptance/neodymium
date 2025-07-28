[![Maven Central](https://img.shields.io/maven-central/v/com.xceptance/neodymium.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.xceptance%22%20AND%20a:%22neodymium%22) [![Join the chat at https://gitter.im/neodymium/community](https://badges.gitter.im/neodymium/community.svg)](https://gitter.im/neodymium/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

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
* [Neodymium Pure Java Example](https://github.com/Xceptance/neodymium-example): A demo test suite against the Xceptance
  reference demo store [Posters](https://github.com/Xceptance/neodymium/wiki/Posters-demo-store) as pure Java version.
* [Neodymium Cucumber Example](https://github.com/Xceptance/neodymium-cucumber-example): For the fans of BDD/Cucumber, this is almost the same test suite as before but driven by BDD syntax.
* [Neodymium Showcase](https://github.com/Xceptance/neodymium-showcase): This repository contains different showcases. Each of them demonstrates and comments on the usage of a single feature of Neodymium. Feel free to request more examples by creating an issue.

## Neodymium in the Media
* [Neodymium – An Open Source Framework for Web Testing](https://blog.xceptance.com/2019/02/26/neodymium-an-open-source-framework-for-web-testing/): An article about our motivation to start Neodymium and its most important features
* [Neodymium 5.0.0](https://blog.xceptance.com/2024/06/26/a-new-magnetic-force-neodymium-5-0-0-release/): An article about new things in version 5.0
* [Web Testing mit Neodymium](https://www.youtube.com/watch?v=hn-juzcXrZg): A recorded talk (in German) about testing with Neodymium. It also gives recommendations and shows best practises. Recorded during a J́UG Thüringen Meetup in 2019.
* [Neodymium 5.1 – Get more done faster](https://blog.xceptance.com/2025/02/11/neodymium-5-1-get-more-done-faster/): Neodymium 5.1 has been released with a host of impressive new features. These include full-page screenshots in reports, enhanced control over JSON assertions, accessibility testing via Google Lighthouse, and simplified session handling and URL validation.
* [Automated Accessibility Testing with Google Lighthouse and Neodymium](https://blog.xceptance.com/2025/03/06/automated-accessibility-testing-with-google-lighthouse-and-neodymium/): One valuable tool for checking WCAG compliance is Google Lighthouse. It can help identify accessibility issues during development. And when integrated into your test automation project with tools like Neodymium, it can significantly streamline your accessibility testing and monitoring process.

## Additional Features
These are our **additions** to make test automation nicer, quicker, and less painful. 

* **Multi Browser Support**: Simple setup and use of different browsers including remote browsers. Annotation per test case defines what browsers are supported by this very test case. Automatic execution of the test case per browser.
* **Page and Component Concept**: Our example test suites demonstrate a page and component concept for easier test design. (Structural guidance)
* **Test Data**: Externalized test data for Java test cases including automatic execution per test data set.
* **Localization**: Simple concept to run localized test cases in conjunction with test data management.
* **Concurrent Execution**: Concurrent execution of tests with Maven.

## Getting Started
We recommend starting with our template instead of writing a suite from scratch. If you need more guidance, you can also start from one of our full demo test suites.

Please hop over to the [Wiki](https://github.com/Xceptance/neodymium/wiki/) for a full tour of Neodymium and a Getting
Started guide.

If you are still impatient, here is the quickest way to get Neodymium added to your project. Don't forget to update the version number if you prefer a particular version.

```xml
<dependency>
    <groupId>com.xceptance</groupId>
    <artifactId>neodymium-library</artifactId>
    <version>INSERT_LATEST_VERSION_HERE</version>
</dependency>
```
To write a simple unit test:

Add the `@NeodymiumTest` annotation to your test method. This enables test execution with Neodymium.

```java
public class MyTests
{
    @NeodymiumTest 
    public void testMethod()
    {
        // test code
    }
}

```

Please be aware that Neodymium (starting at version 5.0.0) is using JUnit5. If you want to run your tests with JUnit4
Neodymium still supports this and you can have a look [here](https://github.com/Xceptance/neodymium/wiki/).

If your project is already driven by Neodymium but you want to update to the latest version, please, check
the [migration notes](https://github.com/Xceptance/neodymium/wiki/Migrate-to-Neodymium-5)

And now is the time to dive into the features we added to make the most out of
it. [Please head over to our Wiki](https://github.com/Xceptance/neodymium/wiki/).

## Client and Customers

If you are a customer and want to be mentioned here, please let us know. We will be happy to extend your web reach by placing a link and short description here. You can also just send us a pull request with a change, if you like.

## License
Neodymium is licensed under the MIT License.

## Who Are We
We are [Xceptance](https://www.xceptance.com/en/). A software testing company with strong commerce knowledge and projects with customers from all around the world. Besides Neodymium, we have developed [Xceptance Load Test (XLT)](https://github.com/Xceptance/XLT), a load and performance test tool that is open source too (APL 2.0). It provides an extensive range of awesome features to make the tester's and developer's life easier.

If you are looking for test automation that also covers the performance side of life, take a look at XLT. You can write and run load tests with real browsers including access to data from the [Web Performance Timing API](http://w3c.github.io/perf-timing-primer/). In case browsers are too heavy, XLT has other modes of load testing to offer as well. 

We offer professional support for Neodymium and XLT as well as implementation and training services.
