package com.xceptance.neodymium.util;

import com.google.common.collect.ImmutableMap;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.AllureAddons.EnvironmentInfoMode;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

@RunWith(NeodymiumRunner.class)
@SuppressBrowsers
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

        final List<Entry<String, String>> expectedFileContentList = new ArrayList<>();

        // add single value
        ImmutableMap<String, String> map = ImmutableMap.<String, String> builder()
                                                       .put("a",
                                                            "a")
                                                       .build();
        AllureAddons.addEnvironmentInformation(map);

        expectedFileContentList.addAll(map.entrySet());
        this.validateEnvironmentFile(expectedFileContentList);

        // overwrite single value
        ImmutableMap<String, String> map2 = ImmutableMap.<String, String> builder()
                                                        .put("a",
                                                             "b")
                                                        .build();
        expectedFileContentList.clear();
        expectedFileContentList.addAll(map2.entrySet());

        // Replace the old value
        AllureAddons.addEnvironmentInformation(map2, EnvironmentInfoMode.REPLACE);
        this.validateEnvironmentFile(expectedFileContentList);

        // single value is not added twice
        AllureAddons.addEnvironmentInformation(map2, EnvironmentInfoMode.ADD);
        this.validateEnvironmentFile(expectedFileContentList);
        // in any mode
        AllureAddons.addEnvironmentInformation(map2, EnvironmentInfoMode.REPLACE);
        this.validateEnvironmentFile(expectedFileContentList);
        AllureAddons.addEnvironmentInformation(map2, EnvironmentInfoMode.APPEND_VALUE);
        this.validateEnvironmentFile(expectedFileContentList);
        AllureAddons.addEnvironmentInformation(map2, EnvironmentInfoMode.IGNORE);
        this.validateEnvironmentFile(expectedFileContentList);

        // Add second single value
        ImmutableMap<String, String> map3 = ImmutableMap.<String, String> builder()
                                                        .put("b",
                                                             "b")
                                                        .build();
        expectedFileContentList.addAll(map3.entrySet());

        AllureAddons.addEnvironmentInformation(map3, EnvironmentInfoMode.REPLACE);
        this.validateEnvironmentFile(expectedFileContentList);

        // combined add and update
        ImmutableMap<String, String> map4 = ImmutableMap.<String, String> builder()
                                                        .put("c",
                                                             "c")
                                                        .put("b",
                                                             "c")
                                                        .build();
        expectedFileContentList.clear();
        expectedFileContentList.addAll(map2.entrySet());
        expectedFileContentList.addAll(map4.entrySet());

        AllureAddons.addEnvironmentInformation(map4, EnvironmentInfoMode.REPLACE);
        this.validateEnvironmentFile(expectedFileContentList);

        // add with same value
        ImmutableMap<String, String> map5 = ImmutableMap.<String, String> builder()
                                                        .put("c",
                                                             "d")
                                                        .build();
        expectedFileContentList.addAll(map5.entrySet());

        AllureAddons.addEnvironmentInformation(map5, EnvironmentInfoMode.ADD);
        this.validateEnvironmentFile(expectedFileContentList);

        // add multiple new values
        ImmutableMap<String, String> map6 = ImmutableMap.<String, String> builder()
                                                        .put("d",
                                                             "d")
                                                        .put("e",
                                                             "e")
                                                        .build();
        expectedFileContentList.addAll(map6.entrySet());

        AllureAddons.addEnvironmentInformation(map6, EnvironmentInfoMode.ADD);
        this.validateEnvironmentFile(expectedFileContentList);

        // ignore value
        ImmutableMap<String, String> map7 = ImmutableMap.<String, String> builder()
                                                        .put("d",
                                                             "12")
                                                        .put("e",
                                                             "12")
                                                        .build();

        AllureAddons.addEnvironmentInformation(map7, EnvironmentInfoMode.IGNORE);
        this.validateEnvironmentFile(expectedFileContentList);

        // append values
        ImmutableMap<String, String> map8 = ImmutableMap.<String, String> builder()
                                                        .put("a",
                                                             "x")
                                                        .build();
        ImmutableMap<String, String> map8_result = ImmutableMap.<String, String> builder()
                                                               .put("a",
                                                                    "b, x")
                                                               .build();
        // remove a,b
        map2.entrySet().forEach((entry) -> expectedFileContentList.remove(entry));
        expectedFileContentList.addAll(map8_result.entrySet());

        AllureAddons.addEnvironmentInformation(map8, EnvironmentInfoMode.APPEND_VALUE);
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
        Assert.assertEquals("Wrong root node name in environments-test.xml", "environment", environment.getNodeName());

        NodeList childNodes = environment.getChildNodes();

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
                    // skip browser configuration coming from other tests
                    if ("Used Browserconfigurations".equals(subNode.getTextContent()))
                    {
                        j++; // skip the value for they key
                        continue;
                    }
                    key = subNode.getTextContent();
                }
                else if ("value".equals(subNode.getNodeName()))
                {
                    value = subNode.getTextContent();
                }
            }

            // skip empty pairs from browser configuration entries
            if (!"".equals(key))
            {
                params.add(new NameValuePair(key, value));
            }
        }

        // assert provided list with parsed params
        // in case environment contained browser configuration childNodes.getLength contains more entries than expected
        Assert.assertEquals("Wrong number of params in environments.xml. It contains: " + params, list.size(), params.size());

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

        @Override
        public String toString()
        {
            return "[ " + name + " ; " + value + " ]";
        }
    }
}
