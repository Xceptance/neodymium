package com.xceptance.neodymium.junit4.testclasses.context.cucumbercontextclear;

import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumCucumberRunner;

import io.cucumber.junit.CucumberOptions;

@RunWith(NeodymiumCucumberRunner.class)
@CucumberOptions(features = "src/test/resources/com/xceptance/neodymium/junit4/testclasses/context/cucumbercontextclear", glue = "com/xceptance/neodymium/junit4/testclasses/context/cucumbercontextclear")
@SuppressBrowsers
public class CucumberContextGetsCleared
{
}
