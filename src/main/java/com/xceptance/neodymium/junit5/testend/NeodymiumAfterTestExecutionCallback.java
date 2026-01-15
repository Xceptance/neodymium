package com.xceptance.neodymium.junit5.testend;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.common.ScreenshotWriter;
import com.xceptance.neodymium.util.Neodymium;

import io.qameta.allure.Allure;

public class NeodymiumAfterTestExecutionCallback implements TestWatcher
{
    @Override
    public void testSuccessful(ExtensionContext context)
    {
        // covering only screenshots on success because
        // screenshots on failure are covered within NeoAllureListener
        if (Neodymium.configuration().enableOnSuccess())
        {
            if (Neodymium.configuration().enableFullPageCapture() && Neodymium.configuration().enableViewportScreenshot())
            {
                Allure.addAttachment("View Port Screenshot",
                                     new ByteArrayInputStream(((TakesScreenshot) WebDriverRunner.getWebDriver()).getScreenshotAs(OutputType.BYTES)));
            }
            try
            {
                ScreenshotWriter.doScreenshot("Advanced Screenshot");
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to take screenshot after successful run", e);
            }
        }
    }
}
