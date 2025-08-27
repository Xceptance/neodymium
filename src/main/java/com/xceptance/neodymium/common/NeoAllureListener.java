package com.xceptance.neodymium.common;

import java.io.IOException;

import com.codeborne.selenide.ex.UIAssertionError;
import com.codeborne.selenide.logevents.LogEvent;
import com.xceptance.neodymium.util.Neodymium;

import io.qameta.allure.selenide.AllureSelenide;

public class NeoAllureListener extends AllureSelenide
{
    @Override
    public void afterEvent(final LogEvent event)
    {
        // check config, we don't want to have two viewport only screenshots
        if ((Neodymium.configuration().enableViewportScreenshot() == false)
            ||
            (Neodymium.configuration().enableFullPageCapture() == false && Neodymium.configuration().enableAdvancedScreenShots() == true))
        {
            this.screenshots(false);
        }

        super.afterEvent(event);

        if (Neodymium.configuration().enableAdvancedScreenShots() && event.getStatus().equals(LogEvent.EventStatus.FAIL))
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
}
