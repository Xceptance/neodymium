package com.xceptance.neodymium.util;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.xceptance.neodymium.util.AllureAddons;

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