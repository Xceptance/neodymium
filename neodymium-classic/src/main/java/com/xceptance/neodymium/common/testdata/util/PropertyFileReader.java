package com.xceptance.neodymium.common.testdata.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class PropertyFileReader
{
    public static List<Map<String, String>> readFile(InputStream inputStream)
    {
        // since property files are always flat structured we only need on entry in the result list
        List<Map<String, String>> dataList = new ArrayList<>(1);
        Map<String, String> data = new HashMap<>();
        Properties prop = new Properties();
        try
        {
            prop.load(inputStream);

            for (Entry<Object, Object> entry : prop.entrySet())
            {
                data.put(entry.getKey().toString(), entry.getValue().toString());
            }

            dataList.add(data);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                inputStream.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return dataList;
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
