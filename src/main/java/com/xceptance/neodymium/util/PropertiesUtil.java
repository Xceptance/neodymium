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

    public static void loadPropertiesFromFile(String path, Properties properties)
    {
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
        if (!changeSet.isEmpty())
        {
            for (Entry<String, String> entry : changeSet.entrySet())
            {
                map.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    public static Map<String, String> addMissingPropertiesFromFile(String fileLocation, String identifier, Map<String, String> dataMap)
    {
        Properties properties = new Properties();
        PropertiesUtil.loadPropertiesFromFile(fileLocation, properties);
        return PropertiesUtil.mapPutAllIfAbsent(dataMap,
                                                PropertiesUtil.getDataMapForIdentifier(identifier,
                                                                                       properties));
    }

    public static Map<String, String> getPropertiesMapForCustomIdentifier(String customIdentifier)
    {
        Map<String, String> dataMap = new HashMap<String, String>();
        dataMap = PropertiesUtil.addMissingPropertiesFromFile("./config/dev-neodymium.properties", customIdentifier, dataMap);

        Map<String, String> systemEnvMap = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : System.getenv().entrySet())
        {
            String key = entry.getKey();
            if (key.contains(customIdentifier))
            {
                String cleanedKey = key.replace(customIdentifier, "");
                cleanedKey = cleanedKey.replaceAll("\\.", "");
                systemEnvMap.put(cleanedKey, entry.getValue());
            }
        }
        dataMap = PropertiesUtil.mapPutAllIfAbsent(dataMap, systemEnvMap);
        dataMap = PropertiesUtil.mapPutAllIfAbsent(dataMap,
                                                   PropertiesUtil.getDataMapForIdentifier(customIdentifier,
                                                                                          System.getProperties()));
        dataMap = PropertiesUtil.addMissingPropertiesFromFile("./config/credentials.properties", customIdentifier, dataMap);
        dataMap = PropertiesUtil.addMissingPropertiesFromFile("./config/neodymium.properties", customIdentifier, dataMap);
        dataMap = PropertiesUtil.addMissingPropertiesFromFile(Optional.ofNullable(ConfigFactory.getProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME))
                                                                      .orElse("")
                                                                      .replaceAll("file:", "./"),
                                                              customIdentifier,
                                                              dataMap);
        return dataMap;
    }
}