package com.xceptance.neodymium.util;

import java.io.File;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

/**
 * Keeps track of localized resources
 *
 * @author rschwietzke
 */
public class NeodymiumLocalization
{
    private final Properties properties;

    private NeodymiumLocalization(final Properties properties)
    {
        this.properties = properties;
    }

    /**
     * Create our localization setup
     *
     * @param file
     *            the file to load including path
     * @return a new localization object
     */
    public static NeodymiumLocalization build(final String file)
    {
        try
        {
            return new NeodymiumLocalization(YamlProperties.build(new File(file)));
        }
        catch (ClassCastException e)
        {
            throw new RuntimeException("Localization keys must be of type String. (e.g. use \"Yes\" instead of Yes as key. This is due to YAML auto conversion.)", e);
        }
    }

    /**
     * Looks up the key in the localization setup starting full locale such as en_US first, fallback to language en,
     * fallback to default, and finally break with an assertion to be on the safe side.
     *
     * @param key
     *            the key to look for without the locale
     * @return the found localization or &lt;localization is undefined&gt;
     */
    public String getText(final String key)
    {
        return getText(key, null);
    }

    public String getText(final String key, final String localeParam)
    {
        if (properties == null)
        {
            // if properties is not set then the localization yaml file was not found or is empty / invalid
            throw new RuntimeException("Localization file was not found or is invalid");
        }

        final String localeString;
        if (StringUtils.isEmpty(localeParam))
        {
            localeString = Neodymium.configuration().locale();
        }
        else
        {
            localeString = localeParam;
        }

        Locale locale;
        try
        {
            locale = LocaleUtils.toLocale(localeString);
        }
        catch (IllegalArgumentException e)
        {
            // the locale was not found
            locale = null;
        }

        // en_US
        String result = properties.getProperty(localeString + "." + key);

        // en
        if (result == null && locale != null)
        {
            result = properties.getProperty(locale.getLanguage() + "." + key);
        }

        // default
        if (result == null)
        {
            result = properties.getProperty("default." + key);
        }

        if (result == null)
        {
            Assert.fail(MessageFormat.format("Cannot find localization for ''{0}'' and locale {1}", key, localeString));
        }

        return result;
    }

    /**
     * Looks up the key in the localization setup starting full locale such as en_US first, fallback to language en,
     * fallback to default. If no localization is found null String is returned.
     *
     * @param key
     *            the key to look for without the locale
     * @return the found localization or null String
     */
    public String getTextUnchecked(final String key)
    {
        final String localeString = Neodymium.dataValueOrDefault("locale", Neodymium.configuration().locale());

        Locale locale;
        try
        {
            locale = LocaleUtils.toLocale(localeString);
        }
        catch (IllegalArgumentException e)
        {
            // the locale was not found
            locale = null;
        }

        // en_US
        String result = properties.getProperty(localeString + "." + key);

        // en
        if (result == null && locale != null)
        {
            result = properties.getProperty(locale.getLanguage() + "." + key);
        }

        // default
        if (result == null)
        {
            result = properties.getProperty("default." + key);
        }

        return result;
    }
}
