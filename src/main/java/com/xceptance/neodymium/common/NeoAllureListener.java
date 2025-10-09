package com.xceptance.neodymium.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.ex.UIAssertionError;
import com.codeborne.selenide.logevents.LogEvent;
import com.xceptance.neodymium.util.Neodymium;

import io.qameta.allure.Allure;
import io.qameta.allure.selenide.AllureSelenide;

public class NeoAllureListener extends AllureSelenide
{
    @Override
    public void afterEvent(final LogEvent event)
    {
        if (Neodymium.configuration().enableAdvancedScreenShots() == true)
        {
            // if advanced screenshots are enabled, Selenide screenshots should be disabled
            this.screenshots(false);

            // if view port screenshot is desired along with full page capture, do Selenide screenshot here 
            // (if done in super.afterEvent(event), step gets completed and advanced screenshot lands outside of step scope and
            // if super.afterEvent(event) is called before advanced screenshot may impact the view port)
            if (Neodymium.configuration().enableFullPageCapture() && Neodymium.configuration().enableViewportScreenshot())
            {
                Allure.addAttachment("Viewport Screenshot",
                                     new ByteArrayInputStream(((TakesScreenshot) WebDriverRunner.getWebDriver()).getScreenshotAs(OutputType.BYTES)));
            }
            if (event.getStatus().equals(LogEvent.EventStatus.FAIL))
            {
                try
                {
                    if (event.getError() != null)
                    {
                        Throwable error = event.getError();
                        if (error instanceof UIAssertionError)
                        {
                            ScreenshotWriter.doScreenshot("Advanced Screenshot");
                        }
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException("Could not take screenshot", e);
                }
            }
        }

        super.afterEvent(event);
    }
}
