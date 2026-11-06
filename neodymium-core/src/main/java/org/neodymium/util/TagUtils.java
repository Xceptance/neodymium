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
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.neodymium.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Universal utility for parsing, merging, and resolving Neodymium execution and variation tags.
 * <p>
 * Supports scalar strings, numbers, booleans, and list sequences from YAML files,
 * as well as runtime JVM system property overrides via {@code -Dneodymium.tag.<tagName>=<value>}.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class TagUtils
{
    private static final String CLI_TAG_PREFIX = "neodymium.tag.";

    private TagUtils()
    {
    }

    /**
     * Parses a raw YAML object (typically a {@link Map}) into a canonical tag map.
     * Scalar values (strings, numbers, booleans) are normalized to single-element lists.
     * Sequences are parsed into lists of trimmed strings.
     *
     * @param raw the raw YAML object to parse
     * @return a map of tag key to list of tag values, never null
     */
    public static Map<String, List<String>> parseTags(final Object raw)
    {
        if (!(raw instanceof Map<?, ?> rawMap))
        {
            return Collections.emptyMap();
        }

        final Map<String, List<String>> result = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : rawMap.entrySet())
        {
            if (entry.getKey() == null)
            {
                continue;
            }
            final String key = String.valueOf(entry.getKey()).trim();
            if (key.isEmpty())
            {
                continue;
            }

            final Object val = entry.getValue();
            if (val == null)
            {
                continue;
            }

            final List<String> values = new ArrayList<>();
            if (val instanceof Iterable<?> iterable)
            {
                for (final Object item : iterable)
                {
                    if (item != null)
                    {
                        final String itemStr = String.valueOf(item).trim();
                        if (!itemStr.isEmpty() && !values.contains(itemStr))
                        {
                            values.add(itemStr);
                        }
                    }
                }
            }
            else
            {
                final String valStr = String.valueOf(val).trim();
                if (!valStr.isEmpty())
                {
                    values.add(valStr);
                }
            }

            if (val instanceof Iterable<?> || !values.isEmpty())
            {
                result.put(key, Collections.unmodifiableList(values));
            }
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * Discovers tag overrides defined on the JVM runtime via system properties matching
     * {@code -Dneodymium.tag.<key>=<value>}. Comma-separated values are split into lists.
     *
     * @return a map of system property tag overrides
     */
    public static Map<String, List<String>> getSystemPropertyOverrides()
    {
        return getSystemPropertyOverrides(System.getProperties());
    }

    /**
     * Discovers tag overrides from the given properties object matching
     * {@code neodymium.tag.<key>=<value>}.
     *
     * @param properties the properties source to inspect
     * @return a map of property tag overrides
     */
    public static Map<String, List<String>> getSystemPropertyOverrides(final Properties properties)
    {
        if (properties == null)
        {
            return Collections.emptyMap();
        }

        final Map<String, List<String>> overrides = new LinkedHashMap<>();
        for (final String propName : properties.stringPropertyNames())
        {
            final String lowerName = propName.toLowerCase(Locale.ROOT);
            if (lowerName.startsWith(CLI_TAG_PREFIX) && lowerName.length() > CLI_TAG_PREFIX.length())
            {
                final String tagKey = propName.substring(CLI_TAG_PREFIX.length()).trim();
                final String val = properties.getProperty(propName);
                if (val != null && !tagKey.isEmpty())
                {
                    final List<String> valList = new ArrayList<>();
                    if (val.contains(","))
                    {
                        for (final String part : val.split(","))
                        {
                            final String trimmedPart = part.trim();
                            if (!trimmedPart.isEmpty() && !valList.contains(trimmedPart))
                            {
                                valList.add(trimmedPart);
                            }
                        }
                    }
                    else
                    {
                        final String trimmedVal = val.trim();
                        if (!trimmedVal.isEmpty())
                        {
                            valList.add(trimmedVal);
                        }
                    }
                    if (!valList.isEmpty())
                    {
                        overrides.put(tagKey, Collections.unmodifiableList(valList));
                    }
                }
            }
        }

        return Collections.unmodifiableMap(overrides);
    }

    /**
     * Merges root file tags, dataset-level tags, and CLI system property overrides following
     * the strict precedence hierarchy: CLI overrides &gt; Dataset tags &gt; Root file tags.
     * Key matching across layers is case-insensitive, preserving the most specific key casing.
     *
     * @param rootTags file root-level tags
     * @param datasetTags dataset-level tags
     * @param systemOverrides CLI runtime overrides
     * @return an unmodifiable merged map of resolved tags
     */
    public static Map<String, List<String>> mergeTags(
        final Map<String, List<String>> rootTags,
        final Map<String, List<String>> datasetTags,
        final Map<String, List<String>> systemOverrides)
    {
        final Map<String, List<String>> resolved = new LinkedHashMap<>();

        if (rootTags != null)
        {
            for (final Map.Entry<String, List<String>> e : rootTags.entrySet())
            {
                putCaseInsensitive(resolved, e.getKey(), e.getValue());
            }
        }

        if (datasetTags != null)
        {
            for (final Map.Entry<String, List<String>> e : datasetTags.entrySet())
            {
                putCaseInsensitive(resolved, e.getKey(), e.getValue());
            }
        }

        if (systemOverrides != null)
        {
            for (final Map.Entry<String, List<String>> e : systemOverrides.entrySet())
            {
                putCaseInsensitive(resolved, e.getKey(), e.getValue());
            }
        }

        return Collections.unmodifiableMap(resolved);
    }

    /**
     * Resolves the final tags for a test variation from raw root tags and raw dataset tags,
     * applying any active JVM system property overrides.
     *
     * @param rawRootTags raw file root tags object
     * @param rawDatasetTags raw dataset tags object
     * @return unmodifiable merged map of resolved tags
     */
    public static Map<String, List<String>> resolveTags(final Object rawRootTags, final Object rawDatasetTags)
    {
        final Map<String, List<String>> parsedRoot = parseTags(rawRootTags);
        final Map<String, List<String>> parsedDataset = parseTags(rawDatasetTags);
        final Map<String, List<String>> cliOverrides = getSystemPropertyOverrides();
        return mergeTags(parsedRoot, parsedDataset, cliOverrides);
    }

    /**
     * Finds the first value for a tag key (case-insensitively).
     *
     * @param tags the tag map
     * @param key the tag key to find
     * @return the first value or null if not found
     */
    public static String findTagValue(final Map<String, List<String>> tags, final String key)
    {
        if (tags == null || key == null)
        {
            return null;
        }

        final List<String> values = findTagValues(tags, key);
        return values.isEmpty() ? null : values.get(0);
    }

    /**
     * Finds all values for a tag key (case-insensitively).
     *
     * @param tags the tag map
     * @param key the tag key to find
     * @return the list of values or empty list if not found
     */
    public static List<String> findTagValues(final Map<String, List<String>> tags, final String key)
    {
        if (tags == null || key == null)
        {
            return Collections.emptyList();
        }

        if (tags.containsKey(key))
        {
            return tags.get(key);
        }

        for (final Map.Entry<String, List<String>> e : tags.entrySet())
        {
            if (key.equalsIgnoreCase(e.getKey()))
            {
                return e.getValue();
            }
        }

        return Collections.emptyList();
    }

    private static void putCaseInsensitive(
        final Map<String, List<String>> target,
        final String key,
        final List<String> values)
    {
        String existingKey = null;
        for (final String k : target.keySet())
        {
            if (k.equalsIgnoreCase(key))
            {
                existingKey = k;
                break;
            }
        }

        if (existingKey != null)
        {
            target.remove(existingKey);
        }
        target.put(key, values);
    }

    /**
     * Parses a tag criteria string into individual tag requirement pairs.
     * Supports formats like:
     * - "browser: Safari, os: iOS"
     * - "env: staging; locale: de_DE"
     * - "mobile"
     * - "browser=Safari, device=tablet"
     *
     * @param criteriaString the comma- or semicolon-separated criteria string
     * @return a map of required tag keys to their expected value, or empty map if none
     */
    public static Map<String, String> parseTagCriteria(final String criteriaString)
    {
        if (criteriaString == null || criteriaString.trim().isEmpty() || "ALL".equalsIgnoreCase(criteriaString.trim()))
        {
            return Collections.emptyMap();
        }

        final Map<String, String> criteria = new LinkedHashMap<>();
        final String[] tokens = criteriaString.split("[,;]+");
        for (final String token : tokens)
        {
            final String trimmed = token.trim();
            if (trimmed.isEmpty())
            {
                continue;
            }

            int sepIndex = trimmed.indexOf(':');
            if (sepIndex < 0)
            {
                sepIndex = trimmed.indexOf('=');
            }

            if (sepIndex > 0)
            {
                final String key = trimmed.substring(0, sepIndex).trim();
                final String val = trimmed.substring(sepIndex + 1).trim();
                if (!key.isEmpty())
                {
                    criteria.put(key, val);
                }
            }
            else
            {
                criteria.put(trimmed, "");
            }
        }
        return Collections.unmodifiableMap(criteria);
    }

    /**
     * Evaluates whether a set of tags (and optional JUnit tags / built-in dimension fallbacks)
     * matches the specified tag criteria.
     * All criteria in the criteria string must be satisfied (conjunction / AND).
     * If the criteria string is null or empty, it matches unconditionally.
     *
     * @param tags the active dynamic tags map
     * @param junitTags optional JUnit tags list, may be null
     * @param browser optional browser name fallback, may be null
     * @param location optional location / locale name fallback, may be null
     * @param criteriaString the criteria string to evaluate
     * @return true if all criteria match, false otherwise
     */
    public static boolean matchesTagCriteria(
        final Map<String, List<String>> tags,
        final List<String> junitTags,
        final String browser,
        final String location,
        final String environment,
        final String batchName,
        final String criteriaString)
    {
        if (criteriaString == null || criteriaString.trim().isEmpty() || "ALL".equalsIgnoreCase(criteriaString.trim()))
        {
            return true;
        }

        final Map<String, String> criteria = parseTagCriteria(criteriaString);
        if (criteria.isEmpty())
        {
            return true;
        }

        for (final Map.Entry<String, String> entry : criteria.entrySet())
        {
            final String reqKey = entry.getKey();
            final String reqVal = entry.getValue();

            if (!reqVal.isEmpty())
            {
                boolean matched = false;

                if (tags != null)
                {
                    final List<String> actualVals = findTagValues(tags, reqKey);
                    if (actualVals != null)
                    {
                        for (final String v : actualVals)
                        {
                            if (reqVal.equalsIgnoreCase(v))
                            {
                                matched = true;
                                break;
                            }
                        }
                    }
                }

                if (!matched && browser != null && !browser.isBlank())
                {
                    if ("browser".equalsIgnoreCase(reqKey) || "browsers".equalsIgnoreCase(reqKey))
                    {
                        if (reqVal.equalsIgnoreCase(browser.trim())
                            || reqVal.equalsIgnoreCase(browser.trim().replace("_headless", ""))
                            || browser.trim().toLowerCase(Locale.ROOT).contains(reqVal.toLowerCase(Locale.ROOT)))
                        {
                            matched = true;
                        }
                    }
                }

                if (!matched && location != null && !location.isBlank())
                {
                    if ("locale".equalsIgnoreCase(reqKey) || "locales".equalsIgnoreCase(reqKey) || "location".equalsIgnoreCase(reqKey))
                    {
                        if (reqVal.equalsIgnoreCase(location.trim()))
                        {
                            matched = true;
                        }
                    }
                }

                if (!matched && environment != null && !environment.isBlank())
                {
                    if ("environment".equalsIgnoreCase(reqKey) || "env".equalsIgnoreCase(reqKey) || "instance".equalsIgnoreCase(reqKey))
                    {
                        if (reqVal.equalsIgnoreCase(environment.trim()))
                        {
                            matched = true;
                        }
                    }
                }

                if (!matched && batchName != null && !batchName.isBlank())
                {
                    if ("batch".equalsIgnoreCase(reqKey) || "batchname".equalsIgnoreCase(reqKey) || "suite".equalsIgnoreCase(reqKey))
                    {
                        if (reqVal.equalsIgnoreCase(batchName.trim()))
                        {
                            matched = true;
                        }
                    }
                }

                if (!matched)
                {
                    return false;
                }
            }
            else
            {
                boolean matched = false;

                if (tags != null)
                {
                    for (final String k : tags.keySet())
                    {
                        if (reqKey.equalsIgnoreCase(k))
                        {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched)
                    {
                        for (final List<String> valList : tags.values())
                        {
                            if (valList != null)
                            {
                                for (final String v : valList)
                                {
                                    if (reqKey.equalsIgnoreCase(v))
                                    {
                                        matched = true;
                                        break;
                                    }
                                }
                            }
                            if (matched)
                            {
                                break;
                            }
                        }
                    }
                }

                if (!matched && junitTags != null)
                {
                    for (final String jt : junitTags)
                    {
                        if (reqKey.equalsIgnoreCase(jt) || jt.equalsIgnoreCase("@" + reqKey) || reqKey.equalsIgnoreCase("@" + jt))
                        {
                            matched = true;
                            break;
                        }
                    }
                }

                if (!matched && browser != null && !browser.isBlank())
                {
                    if (reqKey.equalsIgnoreCase(browser.trim()) || browser.trim().toLowerCase(Locale.ROOT).contains(reqKey.toLowerCase(Locale.ROOT)))
                    {
                        matched = true;
                    }
                }
                if (!matched && location != null && !location.isBlank())
                {
                    if (reqKey.equalsIgnoreCase(location.trim()))
                    {
                        matched = true;
                    }
                }
                if (!matched && environment != null && !environment.isBlank())
                {
                    if (reqKey.equalsIgnoreCase(environment.trim()))
                    {
                        matched = true;
                    }
                }
                if (!matched && batchName != null && !batchName.isBlank())
                {
                    if (reqKey.equalsIgnoreCase(batchName.trim()))
                    {
                        matched = true;
                    }
                }

                if (!matched)
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Evaluates whether a set of tags matches the specified tag criteria.
     *
     * @param tags the active dynamic tags map
     * @param junitTags the list of JUnit tags
     * @param browser the current browser string
     * @param location the current location or locale string
     * @param criteriaString the criteria string to evaluate
     * @return true if all criteria match, false otherwise
     */
    public static boolean matchesTagCriteria(
        final Map<String, List<String>> tags,
        final List<String> junitTags,
        final String browser,
        final String location,
        final String criteriaString)
    {
        return matchesTagCriteria(tags, junitTags, browser, location, null, null, criteriaString);
    }

    /**
     * Evaluates whether a set of tags matches the specified tag criteria.
     *
     * @param tags the active dynamic tags map
     * @param criteriaString the criteria string to evaluate
     * @return true if all criteria match, false otherwise
     */
    public static boolean matchesTagCriteria(
        final Map<String, List<String>> tags,
        final String criteriaString)
    {
        return matchesTagCriteria(tags, Collections.emptyList(), null, null, criteriaString);
    }

    /**
     * Evaluates whether a set of tags matches the specified tag criteria.
     *
     * @param criteriaString the criteria string to evaluate
     * @param tags the active dynamic tags map
     * @param browser the current browser string
     * @param location the current location or locale string
     * @param junitTags the list of JUnit tags
     * @return true if all criteria match, false otherwise
     */
    public static boolean matchesTagCriteria(
        final String criteriaString,
        final Map<String, List<String>> tags,
        final String browser,
        final String location,
        final List<String> junitTags)
    {
        return matchesTagCriteria(tags, junitTags, browser, location, criteriaString);
    }
}
