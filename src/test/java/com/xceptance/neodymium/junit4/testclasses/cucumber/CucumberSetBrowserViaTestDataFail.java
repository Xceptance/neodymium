package com.xceptance.neodymium.junit4.testclasses.cucumber;

import org.junit.runner.RunWith;

import com.xceptance.neodymium.junit4.NeodymiumCucumberRunner;

import io.cucumber.junit.CucumberOptions;

@RunWith(NeodymiumCucumberRunner.class)
@CucumberOptions(features = "src/test/resources/com/xceptance/neodymium/testclasses/cucumber/CucumberSetBrowserViaTestDataFail.feature", glue = "com/xceptance/neodymium/testclasses/cucumber", plugin = "null_summary")
public class CucumberSetBrowserViaTestDataFail
{
}
