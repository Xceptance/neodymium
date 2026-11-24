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
