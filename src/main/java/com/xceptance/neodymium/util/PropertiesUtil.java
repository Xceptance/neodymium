package com.xceptance.neodymium.util;

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

import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.StringUtils;

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

    public static <T extends Object> Map<T, T> putAllIfAbsent(Map<T, T> map, Map<T, T> changeSet)
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

        // filter
        Map<String, String> propertiesMap = properties.entrySet().stream().filter(entry -> ((String) entry.getKey()).startsWith(customIdentifier))
                                                      .collect(Collectors.toMap(entry -> (String) entry.getKey(), entry -> (String) entry.getValue()));

        // substitute
        for (Entry<String, String> entry : propertiesMap.entrySet())
        {
            if (entry.getValue().startsWith("${"))
            {
                entry.setValue((String) properties.getOrDefault(entry.getValue().replace("${", "").replace("}", ""), entry.getValue()));
            }
        }

        return propertiesMap;
    }
}
