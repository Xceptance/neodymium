package com.xceptance.neodymium.junit4.testclasses.webDriver;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.ClickOptions;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Class with tests verifying that download folder configuration works for any download type
 */

@RunWith(NeodymiumRunner.class)
@Browser("chrome_download")
@Browser("firefox_download")
public class DownloadFilesInDifferentWays extends NeodymiumTest
{
    private File fileName;

    /**
     * Verify file saved to the correct directory when downloaded via link
     */
    @Test
    public void downloadViaLink()
    {
        fileName = new File("target/02_2020-Java_aktuell-Autor-Rene_Schwietzke-High-Performance-Java-Hinter-den-Kulissen-von-Java.pdf");
        Selenide.open("https://blog.xceptance.com/2020/02/28/ijug-magazin-java-aktuell-high-performance-java/");
        $(".alignright.is-resized").scrollIntoView(true).click();
        waitForFileDownloading();
        validateFilePresentInDownloadHistory();
    }

    /**
     * Verify file saved to the correct directory when downloaded on form submission
     */
    @Test
    public void downloadOnFormSubmission()
    {
        fileName = new File("target/png2pdf.pdf");
        Selenide.open("https://png2pdf.com/");
        $(".fc-cta-consent").click();
        $("#fileSelector").uploadFile(new File("src/test/resources/2020-in-one-picture.png"));
        $("button[aria-label='COMBINED']").shouldBe(enabled, Duration.ofMillis(9000));
        $("button[aria-label='COMBINED']").click(ClickOptions.usingJavaScript());
        waitForFileDownloading();
        validateFilePresentInDownloadHistory();
    }

    /**
     * Verify file saved to the correct directory when downloaded via link
     */
    @Test
    public void downloadPerLinkWithSelenide() throws FileNotFoundException
    {
        Selenide.open("https://blog.xceptance.com/2020/02/28/ijug-magazin-java-aktuell-high-performance-java/");
        fileName = $(".alignright.is-resized>a").scrollIntoView(true).download();
        waitForFileDownloading();
    }

    @SuppressBrowsers
    @After
    public void deleteFile()
    {
        fileName.delete();
    }

    private void waitForFileDownloading()
    {
        Selenide.Wait().withMessage("File was not downloaded").withTimeout(Duration.ofMillis(9000)).until((driver) -> {
            return fileName.exists() && fileName.canRead();
        });
    }

    private void validateFilePresentInDownloadHistory()
    {
        if (Neodymium.getBrowserName().contains("chrome"))
        {
            Selenide.open("chrome://downloads/");
            $$(Selectors.shadowCss("#title-area", "downloads-manager", "#downloadsList downloads-item")).findBy(exactText(fileName.getName()))
                                                                                                        .should(exist, Duration.ofMillis(9000)).parent()
                                                                                                        .find(".description[role='gridcell']")
                                                                                                        .shouldHave(attribute("hidden"));
        }
        else
        {
            Selenide.open("about:downloads");
            $("description[tooltiptext='" + fileName.getName() + "']").closest(".download-state").shouldHave(attribute("state", "1"));
        }
    }
}
