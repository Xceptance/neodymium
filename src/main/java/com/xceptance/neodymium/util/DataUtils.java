package com.xceptance.neodymium.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.commons.text.TextRandomProvider;

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

import io.qameta.allure.Allure;

/**
 * Class with util methods for test data
 * 
 * @author olha
 */
public class DataUtils
{
    // GsonBuilder().serializeNulls needed to keep explicit null values within Json objects
    private final static Gson GSON = new GsonBuilder().serializeNulls().create();

    private static final Map<Thread, Boolean> ALLURE_ALL_DATA_USED_FLAG = Collections.synchronizedMap(new WeakHashMap<>());

    public final static Configuration JSONPATH_CONFIGURATION = Configuration.builder().jsonProvider(new GsonJsonProvider(GSON))
                                                                             .mappingProvider(new GsonMappingProvider(GSON)).build();

    static Boolean getAllureAllDataUsedFlag()
    {
        Boolean isUsed = ALLURE_ALL_DATA_USED_FLAG.get(Thread.currentThread());
        return isUsed == null ? false : isUsed;
    }

    static Boolean setAllureAllDataUsedFlag(Boolean allureAllDataUsedFlag)
    {
        return ALLURE_ALL_DATA_USED_FLAG.put(Thread.currentThread(), allureAllDataUsedFlag);
    }

    /**
     * Clears the context instance for the current Thread. <br>
     */
    public static void clearThreadContext()
    {
        ALLURE_ALL_DATA_USED_FLAG.remove(Thread.currentThread());
    }

    /**
     * Returns a random email address. <br>
     * The random part contains characters that would match the following regular expression: \[a-z0-9]*\<br>
     * The length of the random part, a prefix and the domain can be configured within neodymium.properties: <br>
     * neodymium.dataUtils.email.randomCharsAmount = 12<br>
     * neodymium.dataUtils.email.local.prefix = test<br>
     * neodymium.dataUtils.email.domain = varmail.de
     * 
     * @return random email
     */
    public static String randomEmail()
    {
        final String randomPart = new RandomStringGenerator.Builder().usingRandom((TextRandomProvider) Neodymium.getRandom())
                                                                     .selectFrom("abcdefghijklmnopqrstuvwxyz0123456789".toCharArray()).build()
                                                                     .generate(Neodymium.configuration().dataUtilsEmailRandomCharsAmount());

        final StringBuilder sb = new StringBuilder(42);
        sb.append(Neodymium.configuration().dataUtilsEmailLocalPrefix());
        sb.append(randomPart);
        sb.append("@");
        sb.append(Neodymium.configuration().dataUtilsEmailDomain());

        return sb.toString().toLowerCase();
    }

    /**
     * A random password that is strong enough for most services <br>
     * The following parts can be configured within neodymium.properties: <br>
     * neodymium.dataUtils.password.uppercaseCharAmount = 2 <br>
     * neodymium.dataUtils.password.lowercaseCharAmount = 5 <br>
     * neodymium.dataUtils.password.digitAmount = 2 <br>
     * neodymium.dataUtils.password.specialCharAmount = 2 <br>
     * neodymium.dataUtils.password.specialChars = +-#$%&amp;.;,_
     * 
     * @return a password
     */
    public static String randomPassword()
    {
        TextRandomProvider textRandomProvider = (TextRandomProvider) Neodymium.getRandom();

        final String upper = new RandomStringGenerator.Builder().usingRandom(textRandomProvider)
                                                                .selectFrom("abcdefghijklmnopqrstuvwxyz".toUpperCase().toCharArray()).build()
                                                                .generate(Neodymium.configuration().dataUtilsPasswordUppercaseCharAmount());

        final String lower = new RandomStringGenerator.Builder().usingRandom(textRandomProvider)
                                                                .selectFrom("abcdefghijklmnopqrstuvwxyz".toCharArray()).build()
                                                                .generate(Neodymium.configuration().dataUtilsPasswordLowercaseCharAmount());

        final String number = new RandomStringGenerator.Builder().usingRandom(textRandomProvider)
                                                                 .selectFrom("0123456789".toCharArray()).build()
                                                                 .generate(Neodymium.configuration().dataUtilsPasswordDigitAmount());

        final String special = new RandomStringGenerator.Builder().usingRandom(textRandomProvider)
                                                                  .selectFrom(Neodymium.configuration().dataUtilsPasswordSpecialChars().toCharArray()).build()
                                                                  .generate(Neodymium.configuration().dataUtilsPasswordSpecialCharAmount());

        final char[] all = (upper + lower + number + special).toCharArray();
        ArrayUtils.shuffle(all, Neodymium.getRandom());

        return new String(all);
    }

    /**
     * Returns the available test data as JsonObject
     * 
     * @return a JsonObject representing the available test data
     */
    public static JsonObject getDataAsJsonObject()
    {
        final Map<String, String> data = Neodymium.getData();
        final JsonObject jsonObject = new JsonObject();

        // iterate over every data entry and parse the entries to prepare complex structures for object mapping
        for (Iterator<String> iterator = data.keySet().iterator(); iterator.hasNext();)
        {
            final String key = iterator.next();
            final String value = data.get(key);
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
        return jsonObject;
    }

    /**
     * Returns data for the data type requested
     * 
     * @param <T>
     *            the inferred type
     * @param clazz
     *            A reference to an class that should be instantiated and filled from test data
     * @return an instance of the class provided
     * @throws JsonSyntaxException
     */
    public static <T> T get(final Class<T> clazz)
    {
        String dataObjectJson = getDataAsJsonObject().toString();

        if (Neodymium.configuration().addTestDataToReport())
        {
            Allure.addAttachment("Testdata", "text/html", convertJsonToHtml(dataObjectJson), "html");

            // to check if whole test data object is used
            setAllureAllDataUsedFlag(true);
        }

        return GSON.fromJson(dataObjectJson, clazz);
    }

    /**
     * <p>
     * Retrieves an element from the JSON representation of current test data using the given JsonPath expression and in
     * case such an element was found, it will be returned as instance of the given class, filled with appropriate
     * values.
     * </p>
     * <b>Example:</b>
     * 
     * <pre>
     * TestCreditCard creditCard = DataUtils.get("$.creditCard", TestCreditCard.class);
     * Assert.assertEquals("4111111111111111", creditCard.getCardNumber());
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
    public static <T> T get(final String jsonPath, final Class<T> clazz)
    {
        try
        {
            T dataObject = JsonPath.using(JSONPATH_CONFIGURATION).parse(getDataAsJsonObject()).read(jsonPath, clazz);

            if (Neodymium.configuration().addTestDataToReport())
            {
                if (!getAllureAllDataUsedFlag())
                {
                    AllureAddons.addDataAsJsonToReport("Testdata (" + jsonPath + ")", dataObject);
                }
            }

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
    public static String convertJsonToHtml(String json)
    {
        return ""
               + "<div id=\"json-viewer\"></div>"
               + "<script src=\"https://cdn.jsdelivr.net/npm/@textea/json-viewer@3\"></script>"
               + "<script>new JsonViewer({value:" + json + "}).render('#json-viewer')</script>";
    }

    /**
     * Check if a certain key exist within the data set
     * 
     * @param key
     *            Name of the test data key
     * @return true if the key was found and false otherwise
     */
    public static boolean exists(String key)
    {
        return Neodymium.dataValue(key) != null;
    }

    /**
     * Get a test data value as {@link String}
     * 
     * @param key
     *            Name of the test data key
     * @return mapped value as {@link String} if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    public static String asString(String key)
    {
        final String value = Neodymium.dataValue(key);
        if (value == null)
        {
            throw new IllegalArgumentException("Test data could not be found for key: " + key);
        }

        return value;
    }

    /**
     * Get a test data value as string or default value if it couldn't be found
     * 
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as {@link String} if the key was found else defaultValue
     */
    public static String asString(String key, String defaultValue)
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
     * Get a test data value as int
     * 
     * @param key
     *            Name of the test data key
     * @return mapped value as int if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    public static int asInt(String key)
    {
        return Integer.parseInt(asString(key));
    }

    /**
     * Get a test data value as int or default value if it couldn't be found
     * 
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as int if the key was found else defaultValue
     */
    public static int asInt(String key, int defaultValue)
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
     * Get a test data value as long
     * 
     * @param key
     *            Name of the test data key
     * @return mapped value as long if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    public static long asLong(String key)
    {
        return Long.parseLong(asString(key));
    }

    /**
     * Get a test data value as long or default value if it couldn't be found
     * 
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as long if the key was found else defaultValue
     */
    public static long asLong(String key, long defaultValue)
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
     * Get a test data value as double
     * 
     * @param key
     *            Name of the test data key
     * @return mapped value as double if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    public static double asDouble(String key)
    {
        return Double.parseDouble(asString(key));
    }

    /**
     * Get a test data value as double or default value if it couldn't be found
     * 
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as double if the key was found else defaultValue
     */
    public static double asDouble(String key, double defaultValue)
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
     * Get a test data value as float
     * 
     * @param key
     *            Name of the test data key
     * @return mapped value as float if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    public static float asFloat(String key)
    {
        return Float.parseFloat(asString(key));
    }

    /**
     * Get a test data value as float or default value if it couldn't be found
     * 
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as float if the key was found else defaultValue
     */
    public static float asFloat(String key, float defaultValue)
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
     * Get a test data value as boolean
     * 
     * @param key
     *            Name of the test data key
     * @return mapped value as boolean if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    public static boolean asBool(String key)
    {
        return Boolean.parseBoolean(asString(key));
    }

    /**
     * Get a test data value as boolean or default value if it couldn't be found
     * 
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as boolean if the key was found else defaultValue
     */
    public static boolean asBool(String key, boolean defaultValue)
    {
        try
        {
            return asBool(key);
        }
        catch (IllegalArgumentException e)
        {
            return defaultValue;
        }
    }
}
