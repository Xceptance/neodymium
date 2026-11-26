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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.yaml.snakeyaml.Yaml;

public class YamlProperties
{
    /**
     * Our properties that will hit the outside world later
     */
    private final Properties properties = new Properties();

    /**
     * Don't publish the constructor
     */
    protected YamlProperties()
    {
    }

    /**
     * Build us a property key from the current state on the stack
     *
     * @param state
     *            the stack
     * @return a property key
     */
    private String buildKey(final Deque<String> state)
    {
        final Iterator<String> i = state.descendingIterator();

        final StringBuilder sb = new StringBuilder(64);
        while (i.hasNext())
        {
            if (sb.length() > 0)
            {
                sb.append('.');
            }
            sb.append(i.next());
        }

        return sb.toString();
    }

    /**
     * Handle all remaining, hopefully primitive types
     *
     * @param entry
     *            the entry
     * @param state
     *            the current state of the stack
     */
    private void handleTypes(final Object entry, final Deque<String> state)
    {
        if (entry != null)
        {
            properties.setProperty(buildKey(state), entry.toString());
        }
        else
        {
            properties.setProperty(buildKey(state), "");
        }
    }

    /**
     * Handle lists
     *
     * @param list
     *            the list to deal with
     * @param state
     *            the current state
     */
    @SuppressWarnings("unchecked")
    private void list(final List<Object> list, final Deque<String> state)
    {
        for (int i = 0; i < list.size(); i++)
        {
            final Object entry = list.get(i);
            state.push(String.valueOf(i));

            if (entry instanceof Map)
            {
                map((Map<String, Object>) entry, state);
            }
            else if (entry instanceof List)
            {
                list((List<Object>) entry, state);
            }
            else
            {
                handleTypes(entry, state);
            }

            state.pop();
        }
    }

    /**
     * Deal with maps
     *
     * @param map
     *            the map of data
     * @param state
     *            the current state
     */
    @SuppressWarnings("unchecked")
    private void map(final Map<String, Object> map, final Deque<String> state)
    {
        for (final String key : map.keySet())
        {
            state.push(key);

            final Object value = map.get(key);

            if (value instanceof Map)
            {
                map((Map<String, Object>) value, state);
            }
            else if (value instanceof List)
            {
                list((List<Object>) value, state);
            }
            else
            {
                handleTypes(value, state);
            }

            state.pop();
        }
    }

    /**
     * Just do the magic
     *
     * @param map
     *            the map to convert
     */
    private void yamlToJavaProperties(final Map<String, Object> map)
    {
        map(map, new ArrayDeque<String>());
    }

    /**
     * Parses yaml properties and turns them into regular Java properties. If the file does not exists, it returns null.
     *
     * @param file
     *            a {@link File} for a YAML file
     * @return a Java properties file reader
     */
    public static Properties build(final File file)
    {
        try (final Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8"))))
        {
            final Yaml yaml = new Yaml();

            final Map<String, Object> map = yaml.load(reader);

            if (map == null)
            {
                // the localization file is empty
                return null;
            }

            final YamlProperties yamlProperties = new YamlProperties();
            yamlProperties.yamlToJavaProperties(map);

            return yamlProperties.properties;
        }
        catch (final FileNotFoundException e)
        {
            return null;
        }
        catch (final IOException e)
        {
            return null;
        }
    }
}
