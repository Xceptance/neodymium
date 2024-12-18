package com.xceptance.neodymium.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import org.junit.Assert;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_headless")
public class LighthouseUtilsTest
{
    @NeodymiumTest
    public void testLighthouseUtilsHappyPath() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.performance", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.accessiblity", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.bestPractices", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.seo", "0.5");

        Class<LighthouseUtils> clazz = LighthouseUtils.class;
        Method runProcess = clazz.getDeclaredMethod("runProcess", String[].class);
        runProcess.setAccessible(true);
        Process p = null;

        if (System.getProperty("os.name").toLowerCase().contains("win"))
        {
            Neodymium.configuration()
                     .setProperty("neodymium.lighthouse.binaryPath",
                                  "echo {\"categories\": {\"performance\": {\"score\": 0.5}, \"accessibility\": {\"score\": 0.5}, \"best-practices\": {\"score\": 0.5}, \"seo\": {\"score\": 0.5}}} > target/lighthouseUtilsReport.report.json | echo makeCommentWork #");
            p = (Process) runProcess.invoke(null, new Object[]
            {
              new String[]
              {
                "cmd.exe", "/c", "echo fabricatedHtml > target/lighthouseUtilsReport.report.html"
              }
            });
        }
        else if (System.getProperty("os.name").toLowerCase().contains("linux") || System.getProperty("os.name").toLowerCase().contains("mac"))
        {
            try
            {
                Neodymium.configuration()
                         .setProperty("neodymium.lighthouse.binaryPath",
                                      "echo {\"categories\": {\"performance\": {\"score\": 0.5}, \"accessibility\": {\"score\": 0.5}, \"best-practices\": {\"score\": 0.5}, \"seo\": {\"score\": 0.5}}} > target/lighthouseUtilsReport.report.json");
                p = (Process) runProcess.invoke(null, new Object[]
                {
                  new String[]
                  {
                    "sh", "-c", "echo fabricatedHtml > target/lighthouseUtilsReport.report.html"
                  }
                });
            }
            catch (Exception e)
            {
                p = (Process) runProcess.invoke(null, new Object[]
                {
                  new String[]
                  {
                    "echo fabricatedHtml > target/lighthouseUtilsReport.report.html"
                  }
                });
            }
        }
        else
        {
            throw new Exception("your current operation system is not supported, please use Windows, Linux or MacOS");
        }

        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while (r.readLine() != null)
        {
            continue;
        }

        Selenide.open("https://blog.xceptance.com/");

        LighthouseUtils.createLightHouseReport("lighthouseUtilsReport");
    }

    @NeodymiumTest
    public void testLighthouseUtilsPerformanceException() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.performance", "0.51");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.accessibility", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.bestPractices", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.seo", "0.5");

        if (System.getProperty("os.name").toLowerCase().contains("win"))
        {
            Neodymium.configuration()
                     .setProperty("neodymium.lighthouse.binaryPath",
                                  "echo {\"categories\": {\"performance\": {\"score\": 0.5}, \"accessibility\": {\"score\": 0.5}, \"best-practices\": {\"score\": 0.5}, \"seo\": {\"score\": 0.5}}} > target/lighthouseUtilsReport.report.json | echo makeCommentWork #");
        }
        else if (System.getProperty("os.name").toLowerCase().contains("linux") || System.getProperty("os.name").toLowerCase().contains("mac"))
        {
            Neodymium.configuration()
                     .setProperty("neodymium.lighthouse.binaryPath",
                                  "echo {\"categories\": {\"performance\": {\"score\": 0.5}, \"accessibility\": {\"score\": 0.5}, \"best-practices\": {\"score\": 0.5}, \"seo\": {\"score\": 0.5}}} > target/lighthouseUtilsReport.report.json");
        }
        else
        {
            throw new Exception("your current operation system is not supported, please use Windows, Linux or MacOS");
        }

        Selenide.open("https://blog.xceptance.com/");

        try
        {
            LighthouseUtils.createLightHouseReport("lighthouseUtilsReport");
        }
        catch (AssertionError e)
        {
            Assert.assertTrue("the compared error messages doesn't match",
                              e.getMessage()
                               .contains("The Lighthouse performance score 0.5 doesn't exceed nor match the required threshold of 0.51, please improve the score to match expectations"));
        }
    }

    @NeodymiumTest
    public void testLighthouseUtilsAccessibilityException() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.performance", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.accessibility", "0.51");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.bestPractices", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.seo", "0.5");

        if (System.getProperty("os.name").toLowerCase().contains("win"))
        {
            Neodymium.configuration()
                     .setProperty("neodymium.lighthouse.binaryPath",
                                  "echo {\"categories\": {\"performance\": {\"score\": 0.5}, \"accessibility\": {\"score\": 0.5}, \"best-practices\": {\"score\": 0.5}, \"seo\": {\"score\": 0.5}}} > target/lighthouseUtilsReport.report.json | echo makeCommentWork #");
        }
        else if (System.getProperty("os.name").toLowerCase().contains("linux") || System.getProperty("os.name").toLowerCase().contains("mac"))
        {
            Neodymium.configuration()
                     .setProperty("neodymium.lighthouse.binaryPath",
                                  "echo {\"categories\": {\"performance\": {\"score\": 0.5}, \"accessibility\": {\"score\": 0.5}, \"best-practices\": {\"score\": 0.5}, \"seo\": {\"score\": 0.5}}} > target/lighthouseUtilsReport.report.json");
        }
        else
        {
            throw new Exception("your current operation system is not supported, please use Windows, Linux or MacOS");
        }

        Selenide.open("https://blog.xceptance.com/");

        try
        {
            LighthouseUtils.createLightHouseReport("lighthouseUtilsReport");
        }
        catch (AssertionError e)
        {
            Assert.assertTrue("the compared error messages doesn't match",
                              e.getMessage()
                               .contains("The Lighthouse accessibility score 0.5 doesn't exceed nor match the required threshold of 0.51, please improve the score to match expectations"));
        }
    }

    @NeodymiumTest
    public void testLighthouseUtilsBestPracticesException() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.performance", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.accessibility", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.bestPractices", "0.51");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.seo", "0.5");

        if (System.getProperty("os.name").toLowerCase().contains("win"))
        {
            Neodymium.configuration()
                     .setProperty("neodymium.lighthouse.binaryPath",
                                  "echo {\"categories\": {\"performance\": {\"score\": 0.5}, \"accessibility\": {\"score\": 0.5}, \"best-practices\": {\"score\": 0.5}, \"seo\": {\"score\": 0.5}}} > target/lighthouseUtilsReport.report.json | echo makeCommentWork #");
        }
        else if (System.getProperty("os.name").toLowerCase().contains("linux") || System.getProperty("os.name").toLowerCase().contains("mac"))
        {
            Neodymium.configuration()
                     .setProperty("neodymium.lighthouse.binaryPath",
                                  "echo {\"categories\": {\"performance\": {\"score\": 0.5}, \"accessibility\": {\"score\": 0.5}, \"best-practices\": {\"score\": 0.5}, \"seo\": {\"score\": 0.5}}} > target/lighthouseUtilsReport.report.json");
        }
        else
        {
            throw new Exception("your current operation system is not supported, please use Windows, Linux or MacOS");
        }

        Selenide.open("https://blog.xceptance.com/");

        try
        {
            LighthouseUtils.createLightHouseReport("lighthouseUtilsReport");
        }
        catch (AssertionError e)
        {
            Assert.assertTrue("the compared error messages doesn't match",
                              e.getMessage()
                               .contains("The Lighthouse best practices score 0.5 doesn't exceed nor match the required threshold of 0.51, please improve the score to match expectations"));
        }
    }

    @NeodymiumTest
    public void testLighthouseUtilsSeoException() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.performance", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.accessibility", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.bestPractices", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.seo", "0.51");

        if (System.getProperty("os.name").toLowerCase().contains("win"))
        {
            Neodymium.configuration()
                     .setProperty("neodymium.lighthouse.binaryPath",
                                  "echo {\"categories\": {\"performance\": {\"score\": 0.5}, \"accessibility\": {\"score\": 0.5}, \"best-practices\": {\"score\": 0.5}, \"seo\": {\"score\": 0.5}}} > target/lighthouseUtilsReport.report.json | echo makeCommentWork #");
        }
        else if (System.getProperty("os.name").toLowerCase().contains("linux") || System.getProperty("os.name").toLowerCase().contains("mac"))
        {
            Neodymium.configuration()
                     .setProperty("neodymium.lighthouse.binaryPath",
                                  "echo {\"categories\": {\"performance\": {\"score\": 0.5}, \"accessibility\": {\"score\": 0.5}, \"best-practices\": {\"score\": 0.5}, \"seo\": {\"score\": 0.5}}} > target/lighthouseUtilsReport.report.json");
        }
        else
        {
            throw new Exception("your current operation system is not supported, please use Windows, Linux or MacOS");
        }

        Selenide.open("https://blog.xceptance.com/");

        try
        {
            LighthouseUtils.createLightHouseReport("lighthouseUtilsReport");
        }
        catch (AssertionError e)
        {
            Assert.assertTrue("the compared error messages doesn't match",
                              e.getMessage()
                               .contains("The Lighthouse seo score 0.5 doesn't exceed nor match the required threshold of 0.51, please improve the score to match expectations"));
        }
    }

    @NeodymiumTest
    public void testLighthouseUtilsBinNotFound() throws Exception
    {
        if (System.getProperty("os.name").toLowerCase().contains("win"))
        {
            Neodymium.configuration().setProperty("neodymium.lighthouse.binaryPath", "programmWhichIsDefinitelyNotInstalled");
        }
        else if (System.getProperty("os.name").toLowerCase().contains("linux") || System.getProperty("os.name").toLowerCase().contains("mac"))
        {
            Neodymium.configuration().setProperty("neodymium.lighthouse.binaryPath", "programmWhichIsDefinitelyNotInstalled");
        }
        else
        {
            throw new Exception("your current operation system is not supported, please use Windows, Linux or MacOS");
        }

        Selenide.open("https://blog.xceptance.com/");

        try
        {
            LighthouseUtils.createLightHouseReport("lighthouseUtilsReport");
        }
        catch (Exception e)
        {
            Assert.assertTrue("the compared error messages doesn't match",
                              e.getMessage().contains("please install lighthouse and add it to the PATH or enter the correct lighthouse binary location"));
        }
    }
}