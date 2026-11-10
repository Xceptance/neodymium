package com.xceptance.neodymium.junit4.testclasses.cucumber;

import org.junit.runner.RunWith;

import com.xceptance.neodymium.junit4.NeodymiumCucumberRunner;

import io.cucumber.junit.CucumberOptions;

@RunWith(NeodymiumCucumberRunner.class)
@CucumberOptions(features = "src/test/resources/com/xceptance/neodymium/junit4/testclasses/cucumber/CucumberSetBrowserViaTestData.feature", glue = "com/xceptance/neodymium/junit4/testclasses/cucumber")
public class CucumberSetBrowserViaTestData
{
}