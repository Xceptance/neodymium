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
