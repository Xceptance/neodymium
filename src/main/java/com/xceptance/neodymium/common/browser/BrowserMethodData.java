package com.xceptance.neodymium.common.browser;

import java.lang.reflect.Method;
import java.util.List;

public class BrowserMethodData
{
    private String browserTag;

    private boolean keepBrowserOpen;

    private boolean keepBrowserOpenOnFailure;

    private boolean startBrowserOnSetUp;

    private boolean startBrowserOnCleanUp;

    private List<Method> afterMethodsWithTestBrowser;

    public BrowserMethodData(String browserTag, boolean keepBrowserOpen, boolean keepBrowserOpenOnFailure, boolean startBrowserOnSetUp,
        boolean startBrowserOnCleanUp, List<Method> afterMethodsWithTestBrowser)
    {
        super();
        this.browserTag = browserTag;
        this.keepBrowserOpen = keepBrowserOpen;
        this.keepBrowserOpenOnFailure = keepBrowserOpenOnFailure;
        this.startBrowserOnSetUp = startBrowserOnSetUp;
        this.startBrowserOnCleanUp = startBrowserOnCleanUp;
        this.afterMethodsWithTestBrowser = afterMethodsWithTestBrowser;
    }

    public String getBrowserTag()
    {
        return browserTag;
    }

    public boolean isKeepBrowserOpen()
    {
        return keepBrowserOpen;
    }

    public boolean isKeepBrowserOpenOnFailure()
    {
        return keepBrowserOpenOnFailure;
    }

    public boolean isStartBrowserOnSetUp()
    {
        return startBrowserOnSetUp;
    }

    public boolean isStartBrowserOnCleanUp()
    {
        return startBrowserOnCleanUp;
    }

    public List<Method> getAfterMethodsWithTestBrowser()
    {
        return afterMethodsWithTestBrowser;
    }
}
