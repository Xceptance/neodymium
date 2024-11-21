package com.xceptance.neodymium.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.ImmutableMap;

public class AllureAddonsTest
{
    @Test
    public void TestAllureAddEnvironmentInformation() throws Exception
    {
        File envFile = getEnvFile();
        if (envFile.exists())
        {
            envFile.delete();
        }

        List<Entry<String, String>> expectedFileContentList = new ArrayList<>();

        // add single value
        ImmutableMap<String, String> map = ImmutableMap.<String, String> builder()
                                                       .put("a",
                                                            "a")
                                                       .build();
        AllureAddons.addEnvironmentInformation(map, false);

        expectedFileContentList.addAll(map.entrySet());
        this.validateEnvironmentFile(expectedFileContentList);

        // overwrite single value
        ImmutableMap<String, String> map2 = ImmutableMap.<String, String> builder()
                                                        .put("a",
                                                             "b")
                                                        .build();
        expectedFileContentList = new ArrayList<>();
        expectedFileContentList.addAll(map2.entrySet());

        AllureAddons.addEnvironmentInformation(map2, true);
        this.validateEnvironmentFile(expectedFileContentList);

        // single value is not added twice
        AllureAddons.addEnvironmentInformation(map2, true);
        this.validateEnvironmentFile(expectedFileContentList);
        // in any mode
        AllureAddons.addEnvironmentInformation(map2, false);
        this.validateEnvironmentFile(expectedFileContentList);

        // Add second single value
        ImmutableMap<String, String> map3 = ImmutableMap.<String, String> builder()
                                                        .put("b",
                                                             "b")
                                                        .build();
        expectedFileContentList.addAll(map3.entrySet());

        AllureAddons.addEnvironmentInformation(map3, true);
        this.validateEnvironmentFile(expectedFileContentList);

        // combined add and update
        ImmutableMap<String, String> map4 = ImmutableMap.<String, String> builder()
                                                        .put("c",
                                                             "c")
                                                        .put("b",
                                                             "c")
                                                        .build();
        expectedFileContentList = new ArrayList<>();
        expectedFileContentList.addAll(map2.entrySet());
        expectedFileContentList.addAll(map4.entrySet());

        AllureAddons.addEnvironmentInformation(map4, true);
        this.validateEnvironmentFile(expectedFileContentList);

        // add with same value
        ImmutableMap<String, String> map5 = ImmutableMap.<String, String> builder()
                                                        .put("c",
                                                             "d")
                                                        .build();
        expectedFileContentList.addAll(map5.entrySet());

        AllureAddons.addEnvironmentInformation(map5, false);
        this.validateEnvironmentFile(expectedFileContentList);

        // add multiple new values
        ImmutableMap<String, String> map6 = ImmutableMap.<String, String> builder()
                                                        .put("d",
                                                             "d")
                                                        .put("e",
                                                             "e")
                                                        .build();
        expectedFileContentList.addAll(map6.entrySet());

        AllureAddons.addEnvironmentInformation(map6, false);
        this.validateEnvironmentFile(expectedFileContentList);
    }

    private File getEnvFile()
    {
        File allureResultsDir = AllureAddons.getAllureResultsFolder();
        return new File(allureResultsDir.getAbsoluteFile() + File.separator + "environment.xml");
    }

    private void validateEnvironmentFile(List<Entry<String, String>> list) throws Exception
    {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(getEnvFile());

        Node environment = doc.getDocumentElement();
        Assert.assertEquals("Wrong root node name in environments.xml", "environment", environment.getNodeName());

        NodeList childNodes = environment.getChildNodes();
        Assert.assertEquals("Wrong number of params in environments.xml", list.size(), childNodes.getLength());

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node child = childNodes.item(i);
            NodeList subNodes = child.getChildNodes();
            String key = "";
            String value = "";
            for (int j = 0; j < subNodes.getLength(); j++)
            {
                Node subNode = subNodes.item(j);
                if ("key".equals(subNode.getNodeName()))
                {
                    key = subNode.getTextContent();
                }
                else if ("value".equals(subNode.getNodeName()))
                {
                    value = subNode.getTextContent();
                }
            }
            params.add(new NameValuePair(key, value));
        }
        for (Entry<String, String> testDataPoint : list)
        {
            int found = 0;
            for (NameValuePair xmlParam : params)
            {
                if (xmlParam.name.equals(testDataPoint.getKey()) && xmlParam.value.equals(testDataPoint.getValue()))
                {
                    found++;
                }
            }
            Assert.assertEquals("Wrong number of parameter for test data (" + testDataPoint.getKey() + "=" + testDataPoint.getValue() + ") found", 1, found);
        }
    }

    private static class NameValuePair
    {
        String name;

        String value;

        public NameValuePair(String name, String value)
        {
            this.name = name;
            this.value = value;
        }
    }
}