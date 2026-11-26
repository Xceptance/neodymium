/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.common.browser.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.MutableCapabilities;

/**
 * POJO class to hold browser configurations
 * 
 * @author m.kaufmann
 */
public class BrowserConfiguration
{
    private String browserTag;

    private String name;

    private MutableCapabilities capabilities;

    private HashMap<String, Object> gridProperties;

    private String testEnvironment;

    private int browserWidth;

    private int browserHeight;

    private boolean headless;

    private List<String> arguments;

    private List<String> driverArguments;

    private Map<String, Object> preferences;

    private String downloadDirectory;
    
    private boolean suppressPasswordLeakageWarning;

    /**
     * get config tag
     * 
     * @return config tag
     */
    public String getConfigTag()
    {
        return browserTag;
    }

    /**
     * set config tag for browser
     * 
     * @param configTag
     */
    protected void setConfigTag(String configTag)
    {
        this.browserTag = configTag;
    }

    /**
     * get browser name
     * 
     * @return
     */
    public String getName()
    {
        return name;
    }

    /**
     * set browser name
     * 
     * @param name
     */
    protected void setName(String name)
    {
        this.name = name;
    }

    /**
     * get browser capabilities
     * 
     * @return browser capabilities
     */
    public MutableCapabilities getCapabilities()
    {
        return capabilities;
    }

    /**
     * set browser capabilities
     * 
     * @param capabilities
     */
    protected void setCapabilities(MutableCapabilities capabilities)
    {
        this.capabilities = capabilities;
    }

    /**
     * get browser tag
     * 
     * @return browser tag
     */
    public String getBrowserTag()
    {
        return browserTag;
    }

    /**
     * get grid properties
     * 
     * @return grid properties
     */
    public HashMap<String, Object> getGridProperties()
    {
        return gridProperties;
    }

    /**
     * set grid properties
     * 
     * @param gridProperties
     */
    public void setGridProperties(HashMap<String, Object> gridProperties)
    {
        this.gridProperties = gridProperties;
    }

    /**
     * get test environment (grid)
     * 
     * @return
     */
    public String getTestEnvironment()
    {
        return testEnvironment;
    }

    /**
     * set test environment (grid)
     * 
     * @param testEnvironment
     */
    protected void setTestEnvironment(String testEnvironment)
    {
        this.testEnvironment = testEnvironment;
    }

    /**
     * get browser width
     * 
     * @return browser width
     */
    public int getBrowserWidth()
    {
        return browserWidth;
    }

    /**
     * set browser width
     * 
     * @param browserWidth
     */
    protected void setBrowserWidth(int browserWidth)
    {
        this.browserWidth = browserWidth;
    }

    /**
     * get browser height
     * 
     * @return browser height
     */
    public int getBrowserHeight()
    {
        return browserHeight;
    }

    /**
     * set browser height
     * 
     * @param browserHeight
     */
    protected void setBrowserHeight(int browserHeight)
    {
        this.browserHeight = browserHeight;
    }

    /**
     * should browser be headless
     * 
     * @return
     */
    public boolean isHeadless()
    {
        return headless;
    }

    /**
     * make browser headless/non-headless
     * 
     * @param headless
     */
    public void setHeadless(boolean headless)
    {
        this.headless = headless;
    }

    /**
     * get browser arguments
     * 
     * @return list of browser arguments
     */
    public List<String> getArguments()
    {
        return arguments;
    }

    /**
     * set browser arguments
     * 
     * @param arguments
     */
    public void setArguments(List<String> arguments)
    {
        this.arguments = arguments;
    }

    public List<String> getDriverArguments()
    {
        return driverArguments;
    }

    public void setDriverArguments(List<String> driverArguments)
    {
        this.driverArguments = driverArguments;
    }

    public Map<String, Object> getPreferences()
    {
        return preferences;
    }

    public void addPreference(String key, Object val)
    {
        if (this.preferences == null)
        {
            this.preferences = new HashMap<String, Object>();
        }
        this.preferences.put(key, val);
    }

    public String getDownloadDirectory()
    {
        return downloadDirectory;
    }

    public void setDownloadDirectory(String downloadDirectory)
    {
        this.downloadDirectory = downloadDirectory;
    }

    public boolean isSuppressPasswordLeakageWarning()
    {
        return suppressPasswordLeakageWarning;
    }

    public void setSuppressPasswordLeakageWarning(boolean suppressPasswordLeakageWarning)
    {
        this.suppressPasswordLeakageWarning = suppressPasswordLeakageWarning;
    }
}
