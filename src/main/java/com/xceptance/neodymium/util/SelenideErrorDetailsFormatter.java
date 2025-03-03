package com.xceptance.neodymium.util;

import com.codeborne.selenide.Driver;
import com.codeborne.selenide.ex.SelenideErrorFormatter;
import com.codeborne.selenide.impl.Screenshot;

public class SelenideErrorDetailsFormatter extends SelenideErrorFormatter
{
    @Override
    public String generateErrorDetails(AssertionError error, Driver driver, Screenshot screenshot, long timeoutMs)
    {

        if (Neodymium.configuration().showSelenideErrorDetails())
        {
            return super.generateErrorDetails(error, driver, screenshot, timeoutMs);
        }

        // The screenshot path is always unique. Also the caused by message is dependent on the browser, so just ignore
        // those.
        // Timeout might be interesting, but to keep the message clean we add it as a step parameter to the current step
        StringBuilder sb = new StringBuilder();
        return sb.append("(")
                 .append(timeout(timeoutMs))
                 .append(")")
                 .toString();
    }
}
