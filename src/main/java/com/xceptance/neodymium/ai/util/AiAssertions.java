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
 *
 * // AI-generated: Claude Opus 4.6
 */
package com.xceptance.neodymium.ai.util;

/**
 * Built-in utility class providing common assertion methods for AI-driven test execution.
 * <p>
 * Methods in this class are automatically discoverable by the {@code JAVA_METHOD} action plugin
 * via the {@code neodymium.ai.agent.javaMethod.utilityClasses} configuration property.
 * By default, this class is registered and its methods can be invoked by name from any AI test
 * without requiring the test class to define them locally.
 * <p>
 * All methods must be {@code public static} with a single {@code String} parameter (or no
 * parameters) to be callable by the framework's reflection-based invocation.
 *
 * @see com.xceptance.neodymium.ai.action.plugins.JavaMethodAction
 */
public final class AiAssertions
{
    /**
     * Private constructor to prevent instantiation.
     */
    private AiAssertions()
    {
        // utility class
    }

    /**
     * Asserts that the provided price string represents a value strictly greater than zero.
     * <p>
     * This method is locale-agnostic: it strips all non-digit characters (currency symbols,
     * thousands separators, decimal separators) and checks whether the remaining digits form
     * a positive integer. This approach works regardless of locale-specific formatting
     * (e.g. {@code $17.99}, {@code 1.234,56 €}, {@code ¥1500}).
     *
     * @param price the price string to validate (e.g. "$17.99", "€ 12,50")
     * @throws AssertionError if the price is null, empty, contains no digits, or is zero
     */
    public static void assertPriceGreaterThanZero(final String price)
    {
        if (price == null || price.trim().isEmpty())
        {
            throw new AssertionError("Price string is null or empty");
        }

        // Extract all digits to form an integer representation.
        // This makes it immune to localized decimal/thousands separators and currency symbols.
        final String digits = price.replaceAll("[^\\d]", "");

        if (digits.isEmpty())
        {
            throw new AssertionError("No digits found in price: " + price);
        }

        try
        {
            final long value = Long.parseLong(digits);
            if (value <= 0)
            {
                throw new AssertionError("Expected price > 0, but was: " + price);
            }
        }
        catch (final NumberFormatException e)
        {
            throw new AssertionError("Failed to parse price: " + price, e);
        }
    }

    /**
     * Verifies that the first extracted number is less than or equal to the second extracted number.
     * Expects a JSON array containing exactly two string values.
     * 
     * @param jsonArrayStr a JSON array string containing two values (e.g. "[\"Results (10)\", \"Results (15)\"]")
     */
    public static void verifyLessOrEqual(final String jsonArrayStr)
    {
        try
        {
            final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            final java.util.List<String> values = mapper.readValue(jsonArrayStr, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>(){});
            
            if (values.size() != 2)
            {
                throw new AssertionError("verifyLessOrEqual expects exactly 2 arguments in the JSON array, got: " + values.size());
            }

            final long num1 = extractNumber(values.get(0));
            final long num2 = extractNumber(values.get(1));

            if (num1 > num2)
            {
                throw new AssertionError(String.format("Expected %d <= %d (extracted from '%s' and '%s')", num1, num2, values.get(0), values.get(1)));
            }
        }
        catch (final Exception e)
        {
            throw new AssertionError("Failed to parse arguments for verifyLessOrEqual: " + jsonArrayStr, e);
        }
    }

    private static long extractNumber(final String text)
    {
        if (text == null)
        {
            return 0;
        }
        
        final java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(text);
        if (m.find())
        {
            return Long.parseLong(m.group());
        }
        throw new AssertionError("No number found in text: " + text);
    }
}
