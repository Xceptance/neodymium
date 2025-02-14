package com.xceptance.neodymium.util;

import static com.xceptance.neodymium.util.AllureAddons.addDataAsJsonToReport;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.commons.text.TextRandomProvider;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Class with util methods for test data
 * 
 * @author olha
 */
public class DataUtils
{
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

        String generatedEmail = sb.toString().toLowerCase();
        addDataAsJsonToReport("Testdata: random email", generatedEmail);

        return generatedEmail;
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

        String generatedPassword = new String(all);
        addDataAsJsonToReport("Testdata: random password", generatedPassword);

        return generatedPassword;
    }

    /**
     * Returns the available test data as JsonObject.<br>
     * Will be deprecated in the next version. Use {@code Neodymium.getData().getDataAsJsonObject();} instead
     * 
     * @return a JsonObject representing the available test data
     */
    @Deprecated
    public static JsonObject getDataAsJsonObject()
    {
        return Neodymium.getData().getDataAsJsonObject();
    }

    /**
     * Returns data for the data type requested.<br>
     * Will be deprecated in the next version. Use {@code Neodymium.getData().get(clazz);} instead
     * 
     * @param <T>
     *            the inferred type
     * @param clazz
     *            A reference to an class that should be instantiated and filled from test data
     * @return an instance of the class provided
     * @throws JsonSyntaxException
     */
    @Deprecated
    public static <T> T get(final Class<T> clazz)
    {
        return Neodymium.getData().get(clazz);
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
     * Will be deprecated in the next version. Use {@code Neodymium.getData().get(jsonPath, clazz);} instead
     * 
     * @param <T>
     *            The inferred type
     * @param jsonPath
     *            The JsonPath leading to the requested object
     * @param clazz
     *            A reference to an class that should be instantiated and filled from test data
     * @return an instance of the class provided or null
     */
    @Deprecated
    public static <T> T get(final String jsonPath, final Class<T> clazz)
    {
        return Neodymium.getData().get(jsonPath, clazz);
    }

    /**
     * Converts the json String to html.<br>
     * Will be deprecated in the next version. Use {@code Neodymium.getData().convertJsonToHtml(json);} instead
     *
     * @param json
     *            as a string
     * @return the string of the to html converted json
     */
    @Deprecated
    public static String convertJsonToHtml(String json)
    {
        return Neodymium.getData().convertJsonToHtml(json);
    }

    /**
     * Check if a certain key exist within the data set.<br>
     * Will be deprecated in the next version. Use {@code Neodymium.getData().exists(key);} instead
     * 
     * @param key
     *            Name of the test data key
     * @return true if the key was found and false otherwise
     */
    @Deprecated
    public static boolean exists(String key)
    {
        return Neodymium.getData().exists(key);
    }

    /**
     * Get a test data value as {@link String}.<br>
     * Will be deprecated in the next version. Use {@code Neodymium.getData().asString(key);} instead
     * 
     * @param key
     *            Name of the test data key
     * @return mapped value as {@link String} if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    @Deprecated
    public static String asString(String key)
    {
        return Neodymium.getData().asString(key);
    }

    /**
     * Get a test data value as string or default value if it couldn't be found.<br>
     * Will be deprecated in the next version. Use {@code Neodymium.getData().asString(key, defaultValue);} instead
     * 
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as {@link String} if the key was found else defaultValue
     */
    @Deprecated
    public static String asString(String key, String defaultValue)
    {
        return Neodymium.getData().asString(key, defaultValue);
    }

    /**
     * Get a test data value as int.<br>
     * Will be deprecated in the next version. Use {@code Neodymium.getData().asInt(key);} instead
     *
     * @param key
     *            Name of the test data key
     * @return mapped value as int if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    @Deprecated
    public static int asInt(String key)
    {
        return Neodymium.getData().asInt(key);
    }

    /**
     * Get a test data value as int or default value if it couldn't be found.<br>
     * Will be deprecated in the next version. Use {@code Neodymium.getData().asInt(key, defaultValue);} instead
     *
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as int if the key was found else defaultValue
     */
    @Deprecated
    public static int asInt(String key, int defaultValue)
    {
        return Neodymium.getData().asInt(key, defaultValue);
    }

    /**
     * Get a test data value as long.<br>
     * Will be deprecated in the next version. Use {@code Neodymium.getData().asLong(key);} instead
     *
     * @param key
     *            Name of the test data key
     * @return mapped value as long if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    @Deprecated
    public static long asLong(String key)
    {
        return Neodymium.getData().asLong(key);
    }

    /**
     * Get a test data value as long or default value if it couldn't be found.<br>
     * Will be deprecated in the next version. Use {@code Neodymium.getData().asLong(key, defaultValue);} instead
     *
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as long if the key was found else defaultValue
     */
    @Deprecated
    public static long asLong(String key, long defaultValue)
    {
        return Neodymium.getData().asLong(key, defaultValue);
    }

    /**
     * Get a test data value as double.<br>
     * Will be deprecated in the next version. Use {@code Neodymium.getData().asDouble(key);} instead
     *
     * @param key
     *            Name of the test data key
     * @return mapped value as double if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    @Deprecated
    public static double asDouble(String key)
    {
        return Neodymium.getData().asDouble(key);
    }

    /**
     * Get a test data value as double or default value if it couldn't be found.<br>
     * Will be deprecated in the next version. Use {@code Neodymium.getData().asDouble(key, defaultValue);} instead
     *
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as double if the key was found else defaultValue
     */
    @Deprecated
    public static double asDouble(String key, double defaultValue)
    {
        return Neodymium.getData().asDouble(key, defaultValue);
    }

    /**
     * Get a test data value as float.<br>
     * Will be deprecated in the next version. Use {@code Neodymium.getData().asFloat(key);} instead
     *
     * @param key
     *            Name of the test data key
     * @return mapped value as float if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    @Deprecated
    public static float asFloat(String key)
    {
        return Neodymium.getData().asFloat(key);
    }

    /**
     * Get a test data value as float or default value if it couldn't be found.<br>
     * Will be deprecated in the next version. Use {@code Neodymium.getData().asFloat(key, defaultValue);} instead
     *
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as float if the key was found else defaultValue
     */
    @Deprecated
    public static float asFloat(String key, float defaultValue)
    {
        return Neodymium.getData().asFloat(key, defaultValue);
    }

    /**
     * Get a test data value as boolean.<br>
     * Will be deprecated in the next version. Use {@code Neodymium.getData().asBoolean(key);} instead
     *
     * @param key
     *            Name of the test data key
     * @return mapped value as boolean if the key was found
     * @throws IllegalArgumentException
     *             if the key was NOT found
     */
    @Deprecated
    public static boolean asBool(String key)
    {
        return Neodymium.getData().asBoolean(key);
    }

    /**
     * Get a test data value as boolean or default value if it couldn't be found.<br>
     * Will be deprecated in the next version. Use {@code Neodymium.getData().asBoolean(key, defaultValue);} instead
     *
     * @param key
     *            Name of test data key
     * @param defaultValue
     *            a value that will be returned if the key was not found
     * @return mapped value as boolean if the key was found else defaultValue
     */
    @Deprecated
    public static boolean asBool(String key, boolean defaultValue)
    {
        return Neodymium.getData().asBoolean(key, defaultValue);
    }
}
