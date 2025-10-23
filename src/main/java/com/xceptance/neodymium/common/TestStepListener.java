package com.xceptance.neodymium.common;

import com.codeborne.selenide.logevents.LogEvent;
import com.codeborne.selenide.logevents.LogEventListener;
import com.xceptance.neodymium.util.AllureAddons;
import com.xceptance.neodymium.util.JavaScriptUtils;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.PropertiesUtil;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import static org.openqa.selenium.support.ui.ExpectedConditions.alertIsPresent;

public class TestStepListener implements LogEventListener
{
    public static final String LISTENER_NAME = "end-teststep-listener";

    public static final String URL_CHANGED_STEP_MESSAGE = "URL changed";

    private static final Map<Thread, String> LAST_URL = Collections.synchronizedMap(new WeakHashMap<>());

    private List<String> includeList = new LinkedList<>();

    private List<String> excludeList = new LinkedList<>();

    private Map<String, String> popupMap = null;

    public TestStepListener()
    {
        if (!Neodymium.configuration().getIncludeList().isEmpty())
        {
            this.includeList = Arrays.asList(Neodymium.configuration().getIncludeList().split("\\s+"));
        }
        if (!Neodymium.configuration().getExcludeList().isEmpty())
        {
            this.excludeList = new ArrayList<>(Arrays.asList(Neodymium.configuration().getExcludeList().split("\\s+")));

            // remove all explicitly allowed URLs from the exclude list
            this.excludeList.removeAll(includeList);
        }
        this.popupMap = PropertiesUtil.getPropertiesMapForCustomIdentifier("neodymium.popup");
    }

    private static String getLastUrl()
    {
        return LAST_URL.get(Thread.currentThread());
    }

    private static void setLastUrl(String lastUrl)
    {
        LAST_URL.put(Thread.currentThread(), lastUrl);
    }

    public static void clearLastUrl()
    {
        LAST_URL.remove(Thread.currentThread());
    }

    @Override
    public void afterEvent(LogEvent currentLog)
    {
        if (!Neodymium.hasDriver())
        {
            return;
        }

        // getting the current URL while an alert is open will throw an exception and closes the alert, so it is checked here
        if (alertIsPresent().apply(Neodymium.getDriver()) != null)
        {
            return;
        }

        String currentUrl = Neodymium.getDriver().getCurrentUrl();
        String lastUrl = getLastUrl() != null ? getLastUrl() : "";

        // if URL hasn't changed or the browser just started nothing needs to be done
        if (lastUrl.equals(currentUrl) || "data:,".equals(currentUrl))
        {
            return;
        }

        // report URL change
        if (Neodymium.configuration().enableStepLinks())
        {
            AllureAddons.addLinkToReport(URL_CHANGED_STEP_MESSAGE, Neodymium.getDriver().getCurrentUrl());
        }

        if (!this.popupMap.isEmpty())
        {
            for (String popup : popupMap.values())
            {
                JavaScriptUtils.injectJavascriptPopupBlocker(popup);
            }
        }

        // check URL is included
        if (this.includeList != null && !this.includeList.isEmpty())
        {
            Assertions.assertTrue(this.includeList.stream().anyMatch(s -> Pattern.compile(s).matcher(currentUrl).find()),
                                  "Opened Link was outside permitted URLs: " + currentUrl);
        }

        // check URL is not excluded
        if (this.excludeList != null)
        {
            Assertions.assertTrue(this.excludeList.stream().noneMatch(s -> Pattern.compile(s).matcher(currentUrl).find()),
                                  "Opened Link was to forbidden site: " + currentUrl);
        }

        // inject pop up blocker
        if (!this.popupMap.isEmpty())
        {
            for (String popup : popupMap.values())
            {
                JavaScriptUtils.injectJavascriptPopupBlocker(popup);
            }
        }

        setLastUrl(currentUrl);
    }

    @Override
    public void beforeEvent(LogEvent currentLog)
    {
        // Do nothing as we only need the afterEvent method but both need to be implemented
    }
}
