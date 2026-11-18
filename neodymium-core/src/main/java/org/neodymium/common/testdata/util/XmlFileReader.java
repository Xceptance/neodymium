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
package org.neodymium.common.testdata.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlFileReader
{
    public static List<Map<String, String>> readFile(InputStream inputStream)
    {

        List<Map<String, String>> testData = new LinkedList<>();
        try
        {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            Document doc = documentBuilder.parse(inputStream);

            doc.getDocumentElement().normalize();

            Element rootElement = doc.getDocumentElement();
            if (rootElement.getNodeName().equals("datafile"))
            {
                NodeList dataSets = rootElement.getElementsByTagName("dataset");
                for (int i = 0; i < dataSets.getLength(); i++)
                {
                    Node dataSet = dataSets.item(i);
                    NodeList datas = dataSet.getChildNodes();
                    Map<String, String> newTestData = new HashMap<>();

                    for (int j = 0; j < datas.getLength(); j++)
                    {
                        Node dataItem = datas.item(j);
                        if (dataItem.getNodeName().equals("data"))
                        {
                            Node key = dataItem.getAttributes().getNamedItem("key");
                            if (key != null)
                            {
                                newTestData.put(key.getNodeValue(), dataItem.getTextContent());
                            }
                        }
                    }
                    testData.add(newTestData);
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return testData;
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
