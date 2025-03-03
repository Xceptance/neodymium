package com.xceptance.neodymium.junit4.testclasses.cucumber;

import org.junit.runner.RunWith;

import com.xceptance.neodymium.junit4.NeodymiumCucumberRunner;

import io.cucumber.junit.CucumberOptions;

@RunWith(NeodymiumCucumberRunner.class)
@CucumberOptions(features = "src/test/resources/com/xceptance/neodymium/junit4/testclasses/cucumber/CucumberValidateAllureSelenideListenerIsActive.feature", glue = "com/xceptance/neodymium/junit4/testclasses/cucumber", plugin =
{
  // Plugins for generating additional JSON and XML reports.
  // Is equivalent to cucumber.plugin in cucumber.properties.
  // The plugin for generating an html report is included in the cucumber.properties file for all test cases.
  "json:target/cucumber-report/cucumber.json",
  "junit:target/cucumber-report/cucumber.xml"
})
public class CucumberValidateAllureSelenideListenerIsActive
{
}
