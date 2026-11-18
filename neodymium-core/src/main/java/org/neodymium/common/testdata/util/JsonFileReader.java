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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.neodymium.common.testdata.util;

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
