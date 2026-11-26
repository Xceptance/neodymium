/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.neodymium.common.browser.Browser;
import org.neodymium.common.retry.Retry;
import org.neodymium.junit4.NeodymiumRunner;

import com.codeborne.selenide.Selenide;
@Retry(exceptions = "Could not start a new session. Response code 500")
@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class LighthouseUtilsTest
{
    private static LocalTestHttpServer server;

    @BeforeClass
    public static void startServer() throws IOException
    {
        server = LocalTestHttpServer.startWithHtml("<html><body><h1>Lighthouse Test</h1></body></html>");
    }

    @AfterClass
    public static void stopServer()
    {
        if (server != null)
        {
            server.close();
        }
    }

    @Test
    public void testLighthouseUtilsHappyPath() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.performance", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.accessiblity", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.bestPractices", "0.5");
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.thresholdScore.seo", "0.5");

        mockLighthouseBinary();

        Selenide.open(server.url("/"));

        LighthouseUtils.createLightHouseReport("lighthouseUtilsReport");
    }

    @Test
    public void testLighthouseUtilsAuditNotTriggered() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.audits", "doctype");

        mockLighthouseBinary();

        Selenide.open(server.url("/"));

        LighthouseUtils.createLightHouseReport("lighthouseUtilsReport");

        // No exception appeared so everything is fine.
    }

    @Test
    public void testLighthouseUtilsAuditTriggered() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.lighthouse.assert.audits", "duplicated-javascript");

        mockLighthouseBinary();

        Selenide.open(server.url("/"));

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

        Selenide.open(server.url("/"));

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
        Selenide.open(server.url("/"));

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
        Selenide.open(server.url("/"));

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

        Selenide.open(server.url("/"));

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
        Selenide.open(server.url("/"));

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
