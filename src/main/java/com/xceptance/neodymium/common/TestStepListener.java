package com.xceptance.neodymium.common;

import com.codeborne.selenide.logevents.LogEvent;
import com.codeborne.selenide.logevents.LogEventListener;
import com.xceptance.neodymium.util.AllureAddons;
import com.xceptance.neodymium.util.JavaScriptUtils;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.PropertiesUtil;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

public class TestStepListener implements LogEventListener
{
    public static final String LISTENER_NAME = "end-teststep-listener";

    private static final Map<Thread, String> LAST_URL = Collections.synchronizedMap(new WeakHashMap<>());

    private List<String> includeList = null;

    private List<String> excludeList = null;

    private Map<String, String> popupMap = null;

    public TestStepListener()
    {
        if(!Neodymium.configuration().getExcludeList().isEmpty()) {
            this.excludeList = Arrays.asList(Neodymium.configuration().getExcludeList().split("\\s+"));
        }
        if(!Neodymium.configuration().getIncludeList().isEmpty()) {
            this.includeList = Arrays.asList(Neodymium.configuration().getIncludeList().split("\\s+"));
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

        String currentUrl = Neodymium.getDriver().getCurrentUrl();
        String lastUrl = getLastUrl();
        if (lastUrl == null)
        {
            lastUrl = "";
        }
        if (!lastUrl.equals(currentUrl) && !currentUrl.equals("data:,"))
        {
            if (Neodymium.configuration().enableStepLinks())
            {
                AllureAddons.addLinkToReport("URL changed", Neodymium.getDriver().getCurrentUrl());
            }
            if (!this.popupMap.isEmpty())
            {
                for (String popup : popupMap.values())
                {
                    JavaScriptUtils.injectJavascriptPopupBlocker(popup);
                }
            }

        }
        setLastUrl(currentUrl);
        if (this.includeList != null)
        {
            boolean result = false;
            for (String s : this.includeList)
            {
                result = Pattern.compile(s).matcher(currentUrl).find();
                if (result)
                {
                    break;
                }
            }
            Assertions.assertTrue(result, "Opened Link was outside permitted URLs: " + currentUrl);
        }
        else if (this.excludeList != null)
        {
            for (String s : this.excludeList)
            {
                Assertions.assertTrue(!Pattern.compile(s).matcher(currentUrl).find(), "Opened Link was to forbidden site: " + currentUrl);
            }
        }
    }

    @Override
    public void beforeEvent(LogEvent currentLog)
    {
        // Do nothing as we only need the afterEvent method but both need to be implemented
    }
}
