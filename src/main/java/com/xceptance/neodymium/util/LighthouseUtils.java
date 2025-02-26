package com.xceptance.neodymium.util;

import static com.xceptance.neodymium.common.testdata.TestData.JSONPATH_CONFIGURATION;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import io.qameta.allure.Allure;

/**
 * Powered by <a href="https://developer.chrome.com/docs/lighthouse/overview?hl=de">Lighthouse</a> (Copyright Google)
 * <p>
 * This class is used to create <a href="https://developer.chrome.com/docs/lighthouse/overview?hl=de">Lighthouse</a> (Copyright Google) 
 * reports and validate them using defined values ​​in the Neodymium configuration
 * </p>
 */
public class LighthouseUtils
{
    /**
     * Creates a <a href="https://developer.chrome.com/docs/lighthouse/overview?hl=de">Lighthouse</a> report
     * (Copyright Google) of the current URL and adds it to the Allure report.
     * 
     * @param reportName
     *            The name of the Lighthouse report attachment in the Allure report
     * @throws Exception
     */
    public static void createLightHouseReport(String reportName) throws Exception 
    {
        // validate that lighthouse is installed
        String readerOutput = "";
        try 
        {
            if (System.getProperty("os.name").toLowerCase().contains("win")) 
            {
                Process p = runProcess("cmd.exe", "/c", Neodymium.configuration().lighthouseBinaryPath(), "--version");
                checkErrorCodeProcess(p);
            }
            else if (System.getProperty("os.name").toLowerCase().contains("linux") || System.getProperty("os.name").toLowerCase().contains("mac"))
            {
                try 
                {
                    Process p = runProcess("sh", "-c", Neodymium.configuration().lighthouseBinaryPath(), "--version");
                    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String readerLine;
                    while ((readerLine = r.readLine()) != null)
                    {
                        readerOutput = readerOutput + readerLine;
                        continue;
                    }

                    checkErrorCodeProcess(p);
                }
                catch (Exception e) 
                {
                    Process p = runProcess(Neodymium.configuration().lighthouseBinaryPath(), "--version");
                    checkErrorCodeProcess(p);
                }
            }
            else 
            {
                throw new Exception("your current operation system is not supported, please use Windows, Linux or MacOS");
            }
        }
        catch (Exception e)
        {
            throw new Exception("lighthouse binary can't be run (" + Neodymium.configuration().lighthouseBinaryPath()
                                + "), please install lighthouse and add it to the PATH or enter the correct lighthouse binary location.\n\nProcess Log: "
                                + readerOutput, e);
        }
        
        // validate chrome browser (lighthouse only works for chrome)
        SelenideAddons.wrapAssertionError(() -> {
            Assert.assertTrue("the current browser is " + Neodymium.getBrowserName() + ", but lighthouse only works in combination with chrome", Neodymium.getBrowserName().contains("chrome"));
        });
        
        // get the current webdriver
        WebDriver driver = Neodymium.getDriver();
        
        // get the current URL
        String URL = driver.getCurrentUrl();
        
        // close window to avoid conflict with lighthouse
        String newWindow = windowOperations(driver);

        // start lighthouse report
        lighthouseAudit(URL, reportName);
        
        // add report html to allure
        Allure.addAttachment(reportName + " - Lighthouse Report", "text/html", FileUtils.openInputStream(new File("target/" + reportName + ".report.html")),
                             "html");
        
        // get report json
        File jsonFile = new File("target/" + reportName + ".report.json");
        FileReader reader = new FileReader("target/" + reportName + ".report.json");
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
        
        // get report json scores
        JsonObject categories = json.getAsJsonObject("categories");
        double performanceScore = categories.getAsJsonObject("performance").get("score").getAsDouble();
        double accessibilityScore = categories.getAsJsonObject("accessibility").get("score").getAsDouble();
        double bestPracticesScore = categories.getAsJsonObject("best-practices").get("score").getAsDouble();
        double seoScore = categories.getAsJsonObject("seo").get("score").getAsDouble();
        
        // validate if values in report json are greater than defined threshold in config
        SelenideAddons.wrapAssertionError(() -> {
            Assert.assertTrue("The Lighthouse performance score " + performanceScore + " doesn't exceed nor match the required threshold of " + Neodymium.configuration().lighthouseAssertPerformance() + ", please improve the score to match expectations and look into the corresponding Lighthouse report named \"" + reportName + "\"", Neodymium.configuration().lighthouseAssertPerformance() <= performanceScore);
            Assert.assertTrue("The Lighthouse accessibility score " + accessibilityScore + " doesn't exceed nor match the required threshold of " + Neodymium.configuration().lighthouseAssertAccessibility() + ", please improve the score to match expectations and look into the corresponding Lighthouse report named \"" + reportName + "\"", Neodymium.configuration().lighthouseAssertAccessibility() <= accessibilityScore);
            Assert.assertTrue("The Lighthouse best practices score " + bestPracticesScore + " doesn't exceed nor match the required threshold of " + Neodymium.configuration().lighthouseAssertBestPractices() + ", please improve the score to match expectations and look into the corresponding Lighthouse report named \"" + reportName + "\"", Neodymium.configuration().lighthouseAssertBestPractices() <= bestPracticesScore);
            Assert.assertTrue("The Lighthouse seo score " + seoScore + " doesn't exceed nor match the required threshold of " + Neodymium.configuration().lighthouseAssertSeo() + ", please improve the score to match expectations and look into the corresponding Lighthouse report named \"" + reportName + "\"", Neodymium.configuration().lighthouseAssertSeo() <= seoScore);
        });
        
        // validate jsonpaths in neodymium properties
        validateAudits(jsonFile, reportName);
        
        
        // switch back to saved URL
        driver.switchTo().window(newWindow);
        driver.get(URL);
    }
    
    /**
     * <p>
     * Opens a new tab apart from the first tab with the test automation, adds a handle and closes the first tab.
     * If the first tab with the test automation is not closed, the Lighthouse report will not have proper values,
     * because it will interfere with the Lighthouse report generation.
     * </p>
     * 
     * @param driver
     *            The current webdriver
     * @return A new empty tab with a window handle
     */
    private static String windowOperations(WebDriver driver)
    {
        String originalWindow = driver.getWindowHandle();
        driver.switchTo().newWindow(WindowType.TAB);
        String newWindow = driver.getWindowHandle();
        driver.switchTo().window(originalWindow);
        driver.close();
        return newWindow;
    }

    /**
     * <p>
     * Uses <a href="https://developer.chrome.com/docs/lighthouse/overview?hl=de">Lighthouse</a> (Copyright Google) 
     * to create a Lighthouse report of the current URL.
     * </p>
     * 
     * @param URL
     *            The current URL the Lighthouse report should be generated on
     * @param reportName
     *            The name of the Lighthouse report attachment in the Allure report 
     * @throws Exception 
     */
    private static void lighthouseAudit(String URL, String reportName) throws Exception
    {
        Process p = null;
        String readerOutput = "";
        String readerLine= "";
        
        try 
        {
            if (System.getProperty("os.name").toLowerCase().contains("win")) 
            {
                p = runProcess("cmd.exe", "/c", Neodymium.configuration().lighthouseBinaryPath(), "--chrome-flags=\"--ignore-certificate-errors\"", URL, "--port=" + Neodymium.getRemoteDebuggingPort(), "--preset=desktop", "--output=json", "--output=html", "--output-path=target/" + reportName + ".json");
                
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while ((readerLine = r.readLine()) != null)
                {
                    readerOutput = readerOutput + readerLine;
                    continue;
                }
                
                checkErrorCodeProcess(p);
            }
            else if (System.getProperty("os.name").toLowerCase().contains("linux") || System.getProperty("os.name").toLowerCase().contains("mac"))
            {
                try 
                {
                    p = runProcess("sh", "-c", Neodymium.configuration().lighthouseBinaryPath(), "--chrome-flags=\"--ignore-certificate-errors\"", URL, "--port=" + Neodymium.getRemoteDebuggingPort(), "--preset=desktop", "--output=json", "--output=html", "--output-path=target/" + reportName + ".json");
                    
                    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    while (r.readLine() != null)
                    {
                        readerOutput = readerOutput + readerLine;
                        continue;
                    }
                    
                    checkErrorCodeProcess(p);
                }
                catch (Exception e) 
                {
                    p = runProcess(Neodymium.configuration().lighthouseBinaryPath(), "--chrome-flags=\"--ignore-certificate-errors\"", URL, "--port=" + Neodymium.getRemoteDebuggingPort(), "--preset=desktop", "--output=json", "--output=html", "--output-path=target/" + reportName + ".json");
                    
                    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    while (r.readLine() != null)
                    {
                        readerOutput = readerOutput + readerLine;
                        continue;
                    }
                    
                    checkErrorCodeProcess(p);
                }
            }
            else 
            {
                throw new Exception("your current operation system is not supported, please use Windows, Linux or MacOS");
            }
        }
        catch (IOException e) 
        {
            throw new Exception("failed to run the lighthouse process. \n\nProcess Log: " + readerOutput, e);
        }
        
    }
    
    /**
     * <p>
     * Validates <a href="https://developer.chrome.com/docs/lighthouse/overview?hl=de">Lighthouse</a> (Copyright Google) 
     * Audits specified in the Neodymium configuration.
     * </p>
     * 
     * @param json 
     *            The json file of the <a href="https://developer.chrome.com/docs/lighthouse/overview?hl=de">Lighthouse</a> 
     *            (Copyright Google) report
     * @param reportName
     *             The name of the Lighthouse report attachment in the Allure report
     *            
     * @throws Exception
     */
    private static void validateAudits(File json, String reportName) throws Exception 
    {
        String assertAuditsString = Neodymium.configuration().lighthouseAssertAudits();
        List<String> errorAudits = new ArrayList<>();
        
        if (StringUtils.isNotBlank(assertAuditsString)) 
        {
            for (String audit : assertAuditsString.split(" ")) 
            {
                String jsonPath = ("$.audits." + audit.trim() + ".details.items.length()");
                
                try 
                {
                    long value =  JsonPath.using(JSONPATH_CONFIGURATION).parse(json).read(jsonPath);
                    if (value > 0) 
                    {
                        errorAudits.add(audit.trim());
                    }
                }
                catch (PathNotFoundException e)
                {
                    continue;
                }
                
            }
            
            SelenideAddons.wrapAssertionError(() -> {
                Assert.assertTrue("the following Lighthouse audits " + errorAudits
                                   + " contain errors that need to be fixed, please look into the Lighthouse report named \"" + reportName
                                  + "\" for further information. ", errorAudits.size() == 0);
            });
        }
    }
    
    /**
     * Runs a process with the in params specified command
     * 
     * @param
     *            params the command with all required parameters
     * @return
     *            the process 
     * @throws IOException
     */
    private static Process runProcess(String... params) throws IOException
    {
        ProcessBuilder builder = new ProcessBuilder();
        builder = new ProcessBuilder(params);
        builder.redirectErrorStream(true);
        
        return builder.start();
    }
    
    /**
     * Checks the error code for the specified process
     * 
     * @param p 
     *            the process of which the error code needs to be checked
     * @throws InterruptedException
     * @throws IOException
     */
    private static void checkErrorCodeProcess(Process p) throws InterruptedException, IOException 
    {
        int errorCode = p.waitFor();
        
        if (errorCode != 0) 
        {
            throw new IOException("process error code differs from 0");
        }
    }
}
