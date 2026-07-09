package com.xceptance.neodymium.common;

import static org.openqa.selenium.support.ui.ExpectedConditions.alertIsPresent;

import com.codeborne.selenide.logevents.LogEvent;
import com.codeborne.selenide.logevents.LogEventListener;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Selenide-specific log event listener that forwards navigation events
 * to {@link BrowserNavigationHook}.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class TestStepListener implements LogEventListener
{
    public static final String LISTENER_NAME = "end-teststep-listener";

    private final BrowserNavigationHook navigationHook = new BrowserNavigationHook();

    public static void clearLastUrl()
    {
        BrowserNavigationHook.clearLastUrl();
    }

    @Override
    public void afterEvent(final LogEvent currentLog)
    {
        if (!Neodymium.hasActiveBrowser() || Neodymium.getDriver() == null)
        {
            return;
        }

        // getting the current URL while an alert is open will throw an exception and closes the alert
        if (alertIsPresent().apply(Neodymium.getDriver()) != null)
        {
            return;
        }

        navigationHook.onNavigation(Neodymium.getDriver().getCurrentUrl());
    }

    @Override
    public void beforeEvent(final LogEvent currentLog)
    {
        // Do nothing as we only need the afterEvent method
    }
}
