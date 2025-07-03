package com.xceptance.neodymium.util;

import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class PropertiesUtil
{
    public static Set<String> getSubkeysForPrefix(Properties properties, String prefix)
    {
        Set<String> keys = new HashSet<String>();

        for (Object key : properties.keySet())
        {
            String keyString = (String) key;
            if (keyString.toLowerCase().startsWith(prefix.toLowerCase()))
            {
                // cut off prefix
                keyString = keyString.substring(prefix.length());

                // split on the next dots
                String[] split = keyString.split("\\.");
                if (split != null && split.length > 0)
                {
                    // the first entry in the resulting array will be the key we are searching for
                    String newKey = split[0];
                    if (StringUtils.isNotBlank(newKey))
                    {
                        keys.add(newKey);
                    }
                }
            }
        }

        return keys;
    }

    public static Properties loadPropertiesFromFile(String path)
    {
        Properties properties = new Properties();

        try
        {
            File source = new File(path);
            if (source.exists())
            {
                FileInputStream fileInputStream = new FileInputStream(source);
                properties.load(fileInputStream);
                fileInputStream.close();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return properties;
    }

    public static Map<String, String> getDataMapForIdentifier(String identifier, Properties properties)
    {
        Map<String, String> resultMap = new HashMap<String, String>();
        for (Entry<Object, Object> entry : properties.entrySet())
        {
            String key = (String) entry.getKey();
            if (key.contains(identifier))
            {
                String cleanedKey = key.replace(identifier, "");
                cleanedKey = cleanedKey.replaceAll("\\.", "");
                resultMap.put(cleanedKey, (String) entry.getValue());
            }
        }
        return resultMap;
    }

    public static Map<String, String> mapPutAllIfAbsent(Map<String, String> map, Map<String, String> changeSet)
    {
        if (changeSet.isEmpty())
        {
            return map;
        }

        for (Entry<String, String> entry : changeSet.entrySet())
        {
            map.putIfAbsent(entry.getKey(), entry.getValue());
        }

        return map;
    }

    public static <T> Map<T, T> putAllIfAbsent(Map<T, T> map, Map<T, T> changeSet)
    {
        if (changeSet.isEmpty())
        {
            return map;
        }

        for (Entry<T, T> entry : changeSet.entrySet())
        {
            map.putIfAbsent(entry.getKey(), entry.getValue());
        }

        return map;
    }

    public static Map<String, String> addMissingPropertiesFromFile(String fileLocation, String identifier, Map<String, String> dataMap)
    {
        Properties properties = loadPropertiesFromFile(fileLocation);
        return PropertiesUtil.mapPutAllIfAbsent(dataMap,
                                                PropertiesUtil.getDataMapForIdentifier(identifier,
                                                                                       properties));
    }

    public static Map<String, String> getPropertiesMapForCustomIdentifier(String customIdentifier)
    {
        // System properties
        Properties properties = System.getProperties();

        // temporary config file
        putAllIfAbsent(properties, loadPropertiesFromFile(Optional.ofNullable(ConfigFactory.getProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME))
                                                                  .orElse("")
                                                                  .replaceAll("file:", "./")));

        // config/dev-neodymium.properties
        putAllIfAbsent(properties, loadPropertiesFromFile("." + File.separator + "config" + File.separator + "dev-neodymium.properties"));

        // System environment variables
        Properties systemProperties = new Properties();
        systemProperties.putAll(System.getenv());

        putAllIfAbsent(properties, systemProperties);

        // config/credentials.properties
        putAllIfAbsent(properties, loadPropertiesFromFile("." + File.separator + "config" + File.separator + "credentials.properties"));

        // config/neodymium.properties
        putAllIfAbsent(properties, loadPropertiesFromFile("." + File.separator + "config" + File.separator + "neodymium.properties"));

        return substituteProperties(properties, customIdentifier);
    }

    private static <T> Map<String, String> substituteProperties(Map<T, T> propertiesMap, String customIdentifier)
    {
        // filter properties for the custom identifier
        Map<String, String> customDataPropertiesMap = propertiesMap.entrySet().stream()
                                                                   .filter(entry -> ((String) entry.getKey()).startsWith(customIdentifier))
                                                                   .collect(
                                                                       Collectors.toMap(entry -> (String) entry.getKey(), entry -> (String) entry.getValue()));

        Map<String, String> substitutedMap = new HashMap<>();

        for (Entry<String, String> entry : customDataPropertiesMap.entrySet())
        {
            substitutedMap.put(entry.getKey(), substitutePropertyValue(entry.getValue(), propertiesMap, new HashSet<>()));
        }

        return substitutedMap;
    }

    private static <T> String substitutePropertyValue(String value, Map<T, T> propertiesMap, Set<String> visitedPlaceholders)
    {
        // If the value does not contain any placeholders, return it as is
        String result = value;
        boolean changed;
        int iterationCount = 0;
        final int MAX_ITERATIONS = 10; // Prevent infinite loops

        do
        {
            // Reset the changed flag for this iteration
            changed = false;
            // Store the previous result to check if any changes were made
            String previousResult = result;

            // Find all ${key} patterns in the value
            int startIndex = result.indexOf("${");

            while (startIndex != -1 && startIndex < result.length() - 1)
            {
                int endIndex = result.indexOf("}", startIndex);
                if (endIndex != -1)
                {
                    // Extract the key from ${key}
                    String placeholder = result.substring(startIndex, endIndex + 1);
                    String key = result.substring(startIndex + 2, endIndex);

                    // Check for circular dependencies
                    if (visitedPlaceholders.contains(key))
                    {
                        // Circular reference detected, keep the placeholder as is
                        startIndex = result.indexOf("${", endIndex + 1);
                        continue;
                    }

                    // Add this key to the visited set for this substitution chain
                    visitedPlaceholders.add(key);

                    // Get the replacement value
                    String replacement = (String) propertiesMap.get(key);
                    // If no value found, keep the placeholder as is
                    if (replacement == null)
                    {
                        replacement = placeholder;
                    }

                    result = result.replace(placeholder, replacement);
                    changed = !result.equals(previousResult);

                    // Continue searching for more placeholders from the beginning since the replacement could have introduced new placeholders
                    // By resetting startIndex to the beginning of the string, the possible new placeholders will be found
                    startIndex = result.indexOf("${");
                }
                else
                {
                    // If we found a starting ${ but no closing }, break
                    break;
                }
            }

            iterationCount++;
        }
        while (changed && iterationCount < MAX_ITERATIONS);

        // If we hit the max iterations, it's likely a circular reference that wasn't caught by our visited set (e.g., complex nested substitutions)
        // In that case the partially substituted result will be returned
        return result;
    }
}
