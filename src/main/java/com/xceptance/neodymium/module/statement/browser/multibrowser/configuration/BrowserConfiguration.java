package com.xceptance.neodymium.module.statement.browser.multibrowser.configuration;

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

    private String testEnvironment;

    private int browserWidth;

    private int browserHeight;

    private boolean headless;

    private List<String> arguments;

    private Map<String, Boolean> preferencesBoolean;

    private Map<String, Integer> preferencesInteger;

    private Map<String, String> preferencesString;

    public String getConfigTag()
    {
        return browserTag;
    }

    protected void setConfigTag(String configTag)
    {
        this.browserTag = configTag;
    }

    public String getName()
    {
        return name;
    }

    protected void setName(String name)
    {
        this.name = name;
    }

    public MutableCapabilities getCapabilities()
    {
        return capabilities;
    }

    protected void setCapabilities(MutableCapabilities capabilities)
    {
        this.capabilities = capabilities;
    }

    public String getTestEnvironment()
    {
        return testEnvironment;
    }

    protected void setTestEnvironment(String testEnvironment)
    {
        this.testEnvironment = testEnvironment;
    }

    public int getBrowserWidth()
    {
        return browserWidth;
    }

    protected void setBrowserWidth(int browserWidth)
    {
        this.browserWidth = browserWidth;
    }

    public int getBrowserHeight()
    {
        return browserHeight;
    }

    protected void setBrowserHeight(int browserHeight)
    {
        this.browserHeight = browserHeight;
    }

    public boolean isHeadless()
    {
        return headless;
    }

    public void setHeadless(boolean headless)
    {
        this.headless = headless;
    }

    public List<String> getArguments()
    {
        return arguments;
    }

    public void setArguments(List<String> arguments)
    {
        this.arguments = arguments;
    }

    public Map<String, Object> getPreferences()
    {
        Map<String, Object> allPreferences = new HashMap<>();
        if (preferencesBoolean != null)
        {
            allPreferences.putAll(preferencesBoolean);
        }
        if (preferencesInteger != null)
        {
            allPreferences.putAll(preferencesInteger);
        }
        if (preferencesString != null)
        {
            allPreferences.putAll(preferencesString);
        }
        return allPreferences;
    }

    public Map<String, Boolean> getPreferencesBoolean()
    {
        return preferencesBoolean;
    }

    public Map<String, Integer> getPreferencesInteger()
    {
        return preferencesInteger;
    }

    public Map<String, String> getPreferencesString()
    {
        return preferencesString;
    }

    public void addPreference(String key, boolean val)
    {
        if (this.preferencesBoolean == null)
        {
            this.preferencesBoolean = new HashMap<>();
        }
        this.preferencesBoolean.put(key, val);
    }

    public void addPreference(String key, int val)
    {
        if (this.preferencesInteger == null)
        {
            this.preferencesInteger = new HashMap<>();
        }
        this.preferencesInteger.put(key, val);
    }

    public void addPreference(String key, String val)
    {
        if (this.preferencesString == null)
        {
            this.preferencesString = new HashMap<>();
        }
        this.preferencesString.put(key, val);
    }
}
