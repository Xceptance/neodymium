package com.xceptance.neodymium.common.testdata;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.xceptance.neodymium.util.Neodymium;

import io.qameta.allure.Allure;

public class TestData extends HashMap<String, String>
{
    // GsonBuilder().serializeNulls needed to keep explicit null values within Json objects
    private final static Gson GSON = new GsonBuilder().serializeNulls().create();

    private final static Configuration JSONPATH_CONFIGURATION = Configuration.builder().jsonProvider(new GsonJsonProvider(GSON))
                                                                             .mappingProvider(new GsonMappingProvider(GSON)).build();

    private boolean testDataUsed = false;

    /**
     * Returns the available test data as JsonObject.
     *
     * @return a JsonObject representing the available test data
     */
    public JsonObject getDataAsJsonObject()
    {
        final JsonObject jsonObject = new JsonObject();

        // iterate over every data entry and parse the entries to prepare complex structures for object mapping
        for (final String key : this.keySet())
        {
            final String value = this.get(key);
            final String trimmedValue = StringUtils.defaultString(value).trim();

            if (value == null)
            {
                jsonObject.add(key, null);
            }
            else if (trimmedValue.startsWith("{") || trimmedValue.startsWith("["))
            {
                jsonObject.add(key, JsonParser.parseString(value));
            }
            else
            {
                jsonObject.add(key, new JsonPrimitive(value));
            }
        }

        testDataUsed = true;

        return jsonObject;
    }

    /**
     * Returns data for the data type requested.
     *
     * @param <T>
     *            the inferred type
     * @param clazz
     *            A reference to a class that should be instantiated and filled from test data
     * @return an instance of the class provided
     * @throws JsonSyntaxException
     */
    public <T> T get(final Class<T> clazz)
    {
        String dataObjectJson = getDataAsJsonObject().toString();
        testDataUsed = true;

        return GSON.fromJson(dataObjectJson, clazz);
    }

    /**
     * <p>
     * Retrieves an element from the JSON representation of current test data using the given JsonPath expression and in
     * case such an element was found, it will be returned as instance of the given class, filled with appropriate values.
     * </p>
     * <b>Example:</b>
     *
     * <pre>
     * {@code
     * TestCreditCard creditCard = DataUtils.get("$.creditCard", TestCreditCard.class);
     * Assert.assertEquals("4111111111111111", creditCard.getCardNumber());
     * }
     * </pre>
     *
     * @param <T>
     *            The inferred type
     * @param jsonPath
     *            The JsonPath leading to the requested object
     * @param clazz
     *            A reference to an class that should be instantiated and filled from test data
     * @return an instance of the class provided or null
     */
    public <T> T get(final String jsonPath, final Class<T> clazz)
    {
        try
        {
            T dataObject = JsonPath.using(JSONPATH_CONFIGURATION).parse(getDataAsJsonObject()).read(jsonPath, clazz);
            testDataUsed = true;
            return dataObject;
        }
        catch (PathNotFoundException e)
        {
            return null;
        }
    }

    /**
     * @param json
     *            as a string
     * @return the string of the to html converted json
     */
    public String convertJsonToHtml(String json)
    {
        return ""
               + "<div id=\"json-viewer\"></div>"
               + "<script src=\"https://cdn.jsdelivr.net/npm/@textea/json-viewer@3\"></script>"
               + "<script>new JsonViewer({value:" + json + "}).render('#json-viewer')</script>";
    }

    /**
     * Check if a certain key exist within the data set.
     *
     * @param key
     *            Name of the test data key
     * @return true if the key was found and false otherwise
     */
    public boolean exists(String key)
    {
        return this.containsKey(key);
    }

    /**
     * Get a test data value as {@link String}.
     *
     * @param key
     *            Name of the test data key
     * @return mapped value as {@link String} if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    public String asString(String key)
    {
        final String value = this.get(key);
        if (value == null)
        {
            throw new IllegalArgumentException("Test data could not be found for key: " + key);
        }

        testDataUsed = true;

        return value;
    }

    /**
     * Get a test data value as string or default value if it couldn't be found.
     *
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as {@link String} if the key was found else defaultValue
     */
    public String asString(String key, String defaultValue)
    {
        try
        {
            return asString(key);
        }
        catch (IllegalArgumentException e)
        {
            return defaultValue;
        }
    }

    /**
     * Get a test data value as int.
     *
     * @param key
     *            Name of the test data key
     * @return mapped value as int if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    public int asInt(String key)
    {
        return Integer.parseInt(asString(key));
    }

    /**
     * Get a test data value as int or default value if it couldn't be found.
     *
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as int if the key was found else defaultValue
     */
    public int asInt(String key, int defaultValue)
    {
        try
        {
            return asInt(key);
        }
        catch (IllegalArgumentException e)
        {
            return defaultValue;
        }
    }

    /**
     * Get a test data value as long.
     *
     * @param key
     *            Name of the test data key
     * @return mapped value as long if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    public long asLong(String key)
    {
        return Long.parseLong(asString(key));
    }

    /**
     * Get a test data value as long or default value if it couldn't be found.
     *
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as long if the key was found else defaultValue
     */
    public long asLong(String key, long defaultValue)
    {
        try
        {
            return asLong(key);
        }
        catch (IllegalArgumentException e)
        {
            return defaultValue;
        }
    }

    /**
     * Get a test data value as double.
     *
     * @param key
     *            Name of the test data key
     * @return mapped value as double if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    public double asDouble(String key)
    {
        return Double.parseDouble(asString(key));
    }

    /**
     * Get a test data value as double or default value if it couldn't be found.
     *
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as double if the key was found else defaultValue
     */
    public double asDouble(String key, double defaultValue)
    {
        try
        {
            return asDouble(key);
        }
        catch (IllegalArgumentException e)
        {
            return defaultValue;
        }
    }

    /**
     * Get a test data value as float.
     *
     * @param key
     *            Name of the test data key
     * @return mapped value as float if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    public float asFloat(String key)
    {
        return Float.parseFloat(asString(key));
    }

    /**
     * Get a test data value as float or default value if it couldn't be found.
     *
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as float if the key was found else defaultValue
     */
    public float asFloat(String key, float defaultValue)
    {
        try
        {
            return asFloat(key);
        }
        catch (IllegalArgumentException e)
        {
            return defaultValue;
        }
    }

    /**
     * Get a test data value as boolean.
     *
     * @param key
     *            Name of the test data key
     * @return mapped value as boolean if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    public boolean asBoolean(String key)
    {
        return Boolean.parseBoolean(asString(key));
    }

    /**
     * Get a test data value as boolean or default value if it couldn't be found.
     *
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as boolean if the key was found else defaultValue
     */
    public boolean asBoolean(String key, boolean defaultValue)
    {
        try
        {
            return asBoolean(key);
        }
        catch (IllegalArgumentException e)
        {
            return defaultValue;
        }
    }

    /**
     * Attach the test data stored to the Allure report with the name "Testdata". The data will only be added if it is
     * configured to add the test data and the data wasn't added already as complete set.
     */
    public void addAttachmentJson()
    {
        // if the data doesn't need to be added return
        if (!Neodymium.configuration().addTestDataToReport() || !testDataUsed)
        {
            return;
        }

        Allure.addAttachment("Testdata", "text/html", convertJsonToHtml(getDataAsJsonObject().toString()), "html");
    }

    /**
     * Set the test data used flag to true. When the test finishes and the flag is true, the test data will be appended to
     * the Allure report. A call of this function should not be necessary since all methods accessing the test data are also
     * updating the flag.
     */
    public void setTestDataUsed()
    {
        this.testDataUsed = true;
    }
}
