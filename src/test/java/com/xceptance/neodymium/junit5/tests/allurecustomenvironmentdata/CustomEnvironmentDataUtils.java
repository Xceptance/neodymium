package com.xceptance.neodymium.junit5.tests.allurecustomenvironmentdata;

import com.xceptance.neodymium.util.AllureAddons;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class CustomEnvironmentDataUtils
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

    public static void forceAllureAddonsCustomDataAddedFalse()
    {
        try
        {
            // Get the class object for the internal handler
            Class<?> targetClass = AllureAddons.class;

            // Get the 'customDataAdded' field
            Field field = targetClass.getDeclaredField("customDataAdded");

            // Make it accessible
            field.setAccessible(true);

            // Set its value back to false. Use 'null' for the first argument because it's a static field.
            field.set(null, false);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
