package com.xceptance.neodymium.common.testdata.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class JsonFileReader
{
    public static List<Map<String, String>> readFile(InputStream inputStream)
    {
        List<Map<String, String>> data = new LinkedList<>();
        try
        {
            BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
            InputStreamReader streamReader = new InputStreamReader(new BufferedInputStream(bufferedStream), Charset.forName("UTF-8"));
            JsonReader jsonReader = new JsonReader(streamReader);

            JsonArray asJsonArray = JsonParser.parseReader(jsonReader).getAsJsonArray();

            for (int i = 0; i < asJsonArray.size(); i++)
            {
                JsonObject dataSet = asJsonArray.get(i).getAsJsonObject();
                Map<String, String> newDataSet = new HashMap<>();
                for (Entry<String, JsonElement> entry : dataSet.entrySet())
                {
                    JsonElement element = entry.getValue();
                    if (element.isJsonNull())
                    {
                        newDataSet.put(entry.getKey(), null);
                    }
                    else if (element.isJsonArray() || element.isJsonObject())
                    {
                        newDataSet.put(entry.getKey(), element.toString());
                    }
                    else
                    {
                        newDataSet.put(entry.getKey(), element.getAsString());
                    }
                }
                data.add(newDataSet);
            }

            jsonReader.close();
            streamReader.close();
            bufferedStream.close();

            return data;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static List<Map<String, String>> readFile(File file)
    {
        try
        {
            return readFile(new FileInputStream(file));
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }
}
