package com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata;

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
            Document doc = dBuilder.parse(xmlFile);

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