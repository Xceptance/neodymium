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
package org.neodymium.util;

import io.qameta.allure.Allure;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Utility class for JSON comparison and assertion operations, that provides methods to compare two JSON strings.
 */
public class JsonAssert
{
    /**
     * Generates an HTML script that creates a visual comparison of two JSON strings.
     *
     * @param expectedJson
     *            The expected JSON string
     * @param actualJson
     *            The actual JSON string
     * @param jsonCompareMode
     *            The comparison mode that determines how strict the comparison should be
     * @return A string containing the HTML document with the visual comparison of both JSON strings
     */
    private static String htmlScript(String expectedJson, String actualJson, JSONCompareMode jsonCompareMode) 
    {
        boolean arrayOrderDoesntMatters = false;
        boolean arrayIsExtensible = false;
        
        if (jsonCompareMode == JSONCompareMode.LENIENT) 
        {
            arrayOrderDoesntMatters = true;
            arrayIsExtensible = true;
        }
        else if (jsonCompareMode == JSONCompareMode.NON_EXTENSIBLE) 
        {
            arrayOrderDoesntMatters = true;
        }
        
        String html = ""
            + "<html>"
              + "<head>"
                + "<link"
                  + " rel=\"stylesheet\""
                  + " href=\"https://esm.sh/jsondiffpatch@0.6.0/lib/formatters/styles/html.css\""
                  + " type=\"text/css\""
                + "/>"
              + "</head>"
              + "<body>"
                + "<div id=\"visual\"></div>"
                + "<script type=\"module\">"
                  + "import * as jsondiffpatch from 'https://esm.sh/jsondiffpatch@0.6.0';"
                  + "import * as htmlFormatter from 'https://esm.sh/jsondiffpatch@0.6.0/formatters/html';"
                  
                  + "const detectMoveFalseInstance = jsondiffpatch.create({"
                    + "arrays: {"
                      + "detectMove: false"
                    + "}"
                  + "});"
                  + "const detectMoveTrueInstance = jsondiffpatch.create({"
                    + "arrays: {"
                      + "detectMove: true"
                    + "}"
                  + "});"

                  + "const left = " + expectedJson + ";"
                  + "const right = " + actualJson + ";"
                  
                  + "function sortArraysInJson(json) {"
                    + "for (const key in json) {"
                      + "if (Array.isArray(json[key]) && json[key].length > 0) {"
                        + "if (json[key].every(item => typeof item === \"object\" && !Array.isArray(item))) {"
                          + "const firstKey = Object.keys(json[key][0])[0];"
                          + "json[key].sort((a, b) => a[firstKey].localeCompare(b[firstKey]));"
                        + "}"
                        + "else {"
                          + "json[key].sort();"
                        + "}"
                      + "}"
                      + "else if (typeof json[key] === \"object\") {"
                        + "sortArraysInJson(json[key]);"
                      + "}"
                    + "}"
                  + "}"
                    
                  + "function deleteArraysWithLengthOne(delta, deltaMove) {"
                    + "for (const key in delta) {"
                      + "if (Array.isArray(delta[key])) {"
                        + "if (delta[key].length === 1 && key in deltaMove) {"
                          + "delete delta[key];"
                        + "}"
                        + "else {"
                          + "deleteArraysWithLengthOne(delta[key], deltaMove[key]);"
                        + "}"
                      + "}"
                      + "else if (typeof delta[key] === \"object\") {"
                        + "deleteArraysWithLengthOne(delta[key], deltaMove[key]);"
                      + "}"
                    + "}"
                  + "}"
                    
                  + "if (" + arrayOrderDoesntMatters + ") {"
                    + "sortArraysInJson(left);"
                    + "sortArraysInJson(right);"
                  + "}"
                  
                  + "const deltaDetectMoveTrue = detectMoveTrueInstance.diff(left, right);"
                  + "const deltaDetectMoveFalse = detectMoveFalseInstance.diff(left, right);"
                  
                  + "if (" + arrayIsExtensible + ") {"
                    + "deleteArraysWithLengthOne(deltaDetectMoveFalse, deltaDetectMoveTrue);"
                  + "}"
                  
                  + "document.getElementById('visual').innerHTML = htmlFormatter.format(deltaDetectMoveFalse, left);"
                + "</script>"
                + "<script>"
                    + "window.onerror = function (e) {"
                        + "document.getElementById('visual').innerHTML = \"An error occured: please check all json data for correct syntax.\";"
                    + "};"
                + "</script>"
              + "</body>"
            + "</html>";
        
        return html;
    }

    /**
     * Asserts that two JSON strings are equal according to the specified comparison mode.
     * If the assertion fails, an HTML containing the differences between both JSON files
     * is generated and attached to the Allure report.
     * 
     * @param expectedJson
     *            The expected JSON string
     * @param actualJson
     *            The actual JSON string
     * @param jsonCompareMode
     *            The comparison mode that determines how strict the JSON comparison should be
     * @throws AssertionError
     *            if the JSONs are not equal according to the specified comparison mode
     * @throws JSONException
     *            if either JSON string is invalid
     */
    public static void assertEquals(String expectedJson, String actualJson, JSONCompareMode jsonCompareMode)
    {
        try
        {
            SelenideAddons.wrapAssertionError(() -> {
                JSONAssert.assertEquals("both json's are not equal (see attachment 'Json Compare')", expectedJson, actualJson, jsonCompareMode);
            });
        }
        catch (AssertionError | JSONException e)
        {
            Allure.addAttachment("Json Compare (" + jsonCompareMode.name() + ")" , "text/html", htmlScript(expectedJson, actualJson, jsonCompareMode), "html");
            
            throw e;
        }
    }

    /**
     * Asserts that two JSON strings are not equal according to the specified comparison mode.
     * If the assertion fails, an HTML containing the JSON content of both files once
     * (because the content of both files is the same) is generated and attached to the
     * Allure report.
     *
     * @param expectedJson
     *            The expected JSON string
     * @param actualJson
     *            The actual JSON string
     * @param jsonCompareMode
     *            The comparison mode that determines how strict the comparison should be
     * @throws AssertionError
     *            if the JSONs are equal according to the specified comparison mode
     * @throws JSONException
     *            if either JSON string is invalid
     */
    public static void assertNotEquals(String expectedJson, String actualJson, JSONCompareMode jsonCompareMode)
    {
        try
        {
            SelenideAddons.wrapAssertionError(() -> {
                JSONAssert.assertNotEquals("both json's are the exact same (see attachment 'Json View')", expectedJson, actualJson, jsonCompareMode);
            });
        }
        catch (AssertionError | JSONException e)
        {

            Allure.addAttachment("Json View", "text/html", Neodymium.getData().convertJsonToHtml(expectedJson), "html");

            throw e;
        }
    }
}
