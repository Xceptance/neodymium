/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
