package com.xceptance.neodymium.common.browser;

/**
 * @deprecated Use {@link org.neodymium.common.browser.BrowserMethodData} instead.
 */
@Deprecated
public class BrowserMethodData extends org.neodymium.common.browser.BrowserMethodData
{
    public BrowserMethodData(java.lang.String browserTag, boolean keepBrowserOpen, boolean keepBrowserOpenOnFailure, boolean startBrowserOnSetUp, boolean startBrowserOnCleanUp, java.util.List<java.lang.reflect.Method> afterMethodsWithTestBrowser)
    {
        super(browserTag, keepBrowserOpen, keepBrowserOpenOnFailure, startBrowserOnSetUp, startBrowserOnCleanUp, afterMethodsWithTestBrowser);
    }
}
