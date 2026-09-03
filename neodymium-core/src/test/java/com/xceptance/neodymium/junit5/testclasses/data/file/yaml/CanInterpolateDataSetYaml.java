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
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.xceptance.neodymium.junit5.testclasses.data.file.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.neodymium.common.testdata.DataFile;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

/**
 * Verifies that Neodymium runner loads and interpolates test data variables from YAML data files.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@DataFile("com/xceptance/neodymium/junit5/testclasses/data/set/yaml/CanInterpolateDataSetYaml.yaml")
public class CanInterpolateDataSetYaml
{
    @NeodymiumTest
    @DisplayName("Verify test data variable interpolation through Neodymium test lifecycle")
    public void test()
    {
        final Map<String, String> data = Neodymium.getData();
        assertEquals("bar", data.get("foo"));
        assertEquals("bar", data.get("foobar"));
        assertEquals("Deutsch", data.get("language"));
        assertEquals("Neodymium", data.get("searchPhrase"));
    }
}
