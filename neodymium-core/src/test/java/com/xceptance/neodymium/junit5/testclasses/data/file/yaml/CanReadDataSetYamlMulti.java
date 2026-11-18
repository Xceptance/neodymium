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
package com.xceptance.neodymium.junit5.testclasses.data.file.yaml;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.neodymium.common.testdata.DataFile;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

@DataFile("com/xceptance/neodymium/junit5/testclasses/data/set/yaml/CanReadDataSetYamlMulti.yaml")
public class CanReadDataSetYamlMulti
{
    @NeodymiumTest
    public void test()
    {
        final Map<String, String> data = Neodymium.getData();
        Assertions.assertTrue(data.size() >= 3);
        
        final String testId = data.get("testId");
        if ("Iteration1".equals(testId))
        {
            Assertions.assertEquals("john@example.com", data.get("user"));
            Assertions.assertEquals("Verify login logic", data.get("steps"));
        }
        else if ("Iteration2".equals(testId))
        {
            Assertions.assertEquals("jane@example.com", data.get("user"));
            Assertions.assertEquals("Local override prompt logic", data.get("steps"));
        }
        else
        {
            Assertions.fail("Unexpected testId: " + testId);
        }
    }
}
