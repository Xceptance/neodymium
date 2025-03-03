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
}
