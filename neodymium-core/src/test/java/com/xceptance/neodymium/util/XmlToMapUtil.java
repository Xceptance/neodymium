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
package com.xceptance.neodymium.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class XmlToMapUtil
{
    /**
     * Get the key value pairs of all parameter nodes as map.<br>
     * Usage example to map the parameters of the Allure environment.xml:<br>
     * {@code getXmlParameterMap(AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment.xml")}
     * 
     * @param filePath
     *            the path to the xml file to parse
     * @return Map<String, String> of the parameter node key value pairs
     */
    public static Map<String, String> getXmlParameterMap(String filePath)
    {
        Map<String, String> map = new HashMap<>();

        try
        {
            File xmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            AllureAddons.lockEnvironmentInformationFile();
            Document doc = dBuilder.parse(xmlFile);
            AllureAddons.unlockEnvironmentFile();
            doc.getDocumentElement().normalize();

            NodeList parameterList = doc.getElementsByTagName("parameter");

            for (int i = 0; i < parameterList.getLength(); i++)
            {
                Element parameterElement = (Element) parameterList.item(i);

                String key = parameterElement.getElementsByTagName("key").item(0).getTextContent();
                String value = parameterElement.getElementsByTagName("value").item(0).getTextContent();

                map.put(key, value);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return map;
    }
}
