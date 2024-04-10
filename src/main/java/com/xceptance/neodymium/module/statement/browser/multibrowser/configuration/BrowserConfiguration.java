package com.xceptance.neodymium.module.statement.browser.multibrowser.configuration;

import java.util.HashMap;
import java.util.List;

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

    private String downloadDirectory;

    private boolean useTestContainers;

    private int testContainerTimeout;

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

    public String getDownloadDirectory()
    {
        return downloadDirectory;
    }

    public void setDownloadDirectory(String downloadDirectory)
    {
        this.downloadDirectory = downloadDirectory;
    }

    public void setUseTestContainers(boolean useContainer)
    {
        this.useTestContainers = useContainer;
    }

    public boolean getUseTestContainers()
    {
        return this.useTestContainers;
    }

    public void setTestContainerTimeout(int timeout)
    {
        this.testContainerTimeout = timeout;
    }

    public int getTestContainerTimeout() {
        return this.testContainerTimeout;
    }
}
