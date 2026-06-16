package com.xceptance.neodymium.junit5.testclasses.webDriver;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.util.EmbeddedHtmlServer;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Class with tests verifying that download folder configuration works for any download type.
 * Uses a locally embedded HTTP server to avoid external dependencies and flakiness.
 */
@Browser("chrome_download")
@Browser("firefox_download")
public class DownloadFilesInDifferentWays extends AbstractNeodymiumTest
{
    private static EmbeddedHtmlServer server;

    private static String baseUrl;

    private File fileName;

    @BeforeAll
    public static void startServer() throws IOException
    {
        server = new EmbeddedHtmlServer();
        server.start();
        baseUrl = "http://localhost:" + server.getPort() + "/DownloadTest";
    }

    @AfterAll
    public static void stopServer()
    {
        if (server != null)
        {
            server.stop();
        }
    }

    /**
     * Verify file saved to the correct directory when downloaded via link
     */
    @NeodymiumTest
    public void downloadViaLink()
    {
        fileName = new File("target/sample.pdf");
        Selenide.open(baseUrl + "/index.html");
        $("#downloadLink").scrollIntoView(true).click();
        waitForFileDownloading();
        validateFilePresentInDownloadHistory();
    }

    /**
     * Verify file saved to the correct directory when downloaded via link using Selenide's download()
     */
    @NeodymiumTest
    public void downloadPerLinkWithSelenide() throws FileNotFoundException
    {
        Selenide.open(baseUrl + "/index.html");
        fileName = $("#downloadLink").scrollIntoView(true).download();
        waitForFileDownloading();
    }

    /**
     * Verify file saved to the correct directory when downloaded on form submission
     */
    @NeodymiumTest
    public void downloadOnFormSubmission()
    {
        fileName = new File("target/test.pdf");
        Selenide.open(baseUrl + "/upload.html");
        $("#fileInput").should(exist, Duration.ofMillis(10000))
                       .uploadFile(new File("src/test/resources/xceptance_bugs.png"));
        $("#uploadBtn").shouldBe(visible, Duration.ofMillis(10000)).click();
        $("#downloadBtn").shouldBe(visible, Duration.ofMillis(10000)).click();
        waitForFileDownloading();
    }

    @SuppressBrowsers
    @AfterEach
    public void deleteFile()
    {
        if (fileName != null)
        {
            fileName.delete();
        }
    }

    private void waitForFileDownloading()
    {
        Selenide.Wait().withMessage("File was not downloaded: " + fileName)
                       .withTimeout(Duration.ofMillis(30000))
                       .until((driver) -> fileName.exists() && fileName.canRead());
    }

    private void validateFilePresentInDownloadHistory()
    {
        if (Neodymium.getBrowserName().contains("chrome"))
        {
            Selenide.open("chrome://downloads/");
            $$(Selectors.shadowCss("#title-area", "downloads-manager", "#downloadsList downloads-item"))
                .findBy(exactText(fileName.getName())).parent()
                .find(".description[role='gridcell']")
                .shouldHave(attribute("hidden"), Duration.ofMillis(30000));
        }
        else if (Neodymium.getBrowserName().contains("firefox"))
        {
            // For Firefox, opening and automating about:downloads is restricted,
            // so we just verify the file is available on disk
            Assertions.assertTrue(fileName.exists() && fileName.canRead(), "Downloaded file is not available");
        }
        else
        {
            Selenide.open("about:downloads");
            $("description[tooltiptext='" + fileName.getName() + "']").closest(".download-state")
                                                                      .shouldHave(attribute("state", "1"),
                                                                                  Duration.ofMillis(30000));
        }
    }
}
