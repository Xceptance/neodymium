package com.xceptance.neodymium.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class LighthouseUtilsTest
{
    @Test
    public void testLighthouseUtilsHappyPath() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.performance", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.accessiblity", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.bestPractices", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.seo", "0.5");

        mockLighthouseBinary();

        Selenide.open("https://blog.xceptance.com/");

        LighthouseUtils.createLightHouseReport("lighthouseUtilsReport");
    }

    @Test
    public void testLighthouseUtilsAuditNotTriggered() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.audits", "doctype");

        mockLighthouseBinary();

        Selenide.open("https://blog.xceptance.com/");

        LighthouseUtils.createLightHouseReport("lighthouseUtilsReport");

        // No exception appeared so everything is fine.
    }

    @Test
    public void testLighthouseUtilsAuditTriggered() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.audits", "duplicated-javascript");

        mockLighthouseBinary();

        Selenide.open("https://blog.xceptance.com/");

        AssertionError auditAssertion = assertThrows(AssertionError.class, () -> {
            LighthouseUtils.createLightHouseReport("lighthouseUtilsReport");
        });
        Assertions.assertTrue(auditAssertion.getMessage().contains(
                                                                   "he following Lighthouse audits [duplicated-javascript] contain errors that need to be fixed, please look into the Lighthouse report named \"lighthouseUtilsReport\" for further information."));
    }

    @Test
    public void testLighthouseUtilsPerformanceException() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.performance", "0.51");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.accessibility", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.bestPractices", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.seo", "0.5");

        mockLighthouseBinary();

        Selenide.open("https://blog.xceptance.com/");

        AssertionError auditAssertion = Assert.assertThrows(AssertionError.class, () -> {
            LighthouseUtils.createLightHouseReport("lighthouseUtilsReport");
        });

        Assert.assertTrue("the compared error messages doesn't match",
                          auditAssertion.getMessage()
                           .contains("The Lighthouse performance score 0.5 doesn't exceed nor match the required threshold of 0.51, please improve the score to match expectations"));
    }

    @Test
    public void testLighthouseUtilsAccessibilityException() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.performance", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.accessibility", "0.51");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.bestPractices", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.seo", "0.5");
        mockLighthouseBinary();
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

    @Test
    public void testLighthouseUtilsBestPracticesException() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.performance", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.accessibility", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.bestPractices", "0.51");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.seo", "0.5");
        mockLighthouseBinary();
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

    @Test
    public void testLighthouseUtilsSeoException() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.performance", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.accessibility", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.bestPractices", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.seo", "0.51");

        mockLighthouseBinary();

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

    @Test
    public void testLighthouseUtilsBinNotFound() throws Exception
    {
        mockLighthouseBinary();
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

    private void mockLighthouseBinary() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, Exception, IOException
    {
        Class<LighthouseUtils> clazz = LighthouseUtils.class;
        Method runProcess = clazz.getDeclaredMethod("runProcess", String[].class);
        runProcess.setAccessible(true);
        Process p = null;

        String lighthouseReportJson = getMockedLighthouseJson();
        if (System.getProperty("os.name").toLowerCase().contains("win"))
        {
            Neodymium.configuration()
                     .setProperty("neodymium.lighthouse.binaryPath",
                                  "echo " + lighthouseReportJson
                                                                     + " > target/lighthouseUtilsReport.report.json | echo makeCommentWork #");
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
                                      "echo " + lighthouseReportJson
                                                                         + " > target/lighthouseUtilsReport.report.json");
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
    }

    private String getMockedLighthouseJson()
    {
        return "{"
               + "  'categories': {"
               + "    'performance': {"
               + "      'score': 0.5"
               + "    },"
               + "    'accessibility': {"
               + "      'score': 0.5"
               + "    },"
               + "    'best-practices': {"
               + "      'score': 0.5"
               + "    },"
               + "    'seo': {"
               + "      'score': 0.5"
               + "    }"
               + "  },"
               + "  'audits': {"
               + "    'duplicated-javascript': {"
               + "      'score': 0.4,"
               + "      'details': {"
               + "        'items': ["
               + "          {}"
               + "        ]"
               + "      }"
               + "    }"
               + "  }"
               + "}";
    }

}
