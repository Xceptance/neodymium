package com.xceptance.neodymium.util;

import java.util.List;

public class BrowserDataUtil
{
    public static String convertBrowserListToString(List<String> browserTags)
    {
        String resultString = "";
        for (String s : browserTags)
        {
            resultString += s + ",\n";
        }
        return resultString.substring(0, resultString.length() - 2);
    }
}
