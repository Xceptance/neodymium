/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.util;

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

    protected NeodymiumLocalization(final Properties properties)
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

    public String tryGetText(final String key)
    {
        return tryGetText(key, null);
    }

    public String tryGetText(final String key, final String localeParam)
    {
        final String result = findText(key, localeParam);
        return result != null ? result : key;
    }

    public String getText(final String key, final String localeParam)
    {
        // if properties is not set then the localization yaml file was not found or is empty / invalid
        if (properties == null)
        {
            throw new RuntimeException("Localization file was not found or is invalid");
        }

        final String result = findText(key, localeParam);

        if (result == null)
        {
            Assert.fail(MessageFormat.format("Cannot find localization for ''{0}'' and locale {1}", key, localeParam));
        }

        return result;
    }

    private String findText(final String key, final String localeParam)
    {
        if (properties == null)
        {
            return null;
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
        catch (final IllegalArgumentException e)
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
