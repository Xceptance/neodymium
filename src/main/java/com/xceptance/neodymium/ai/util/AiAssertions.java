/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.ai.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xceptance.neodymium.util.Neodymium;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;


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
     * Asserts that the provided numeric/price string represents a value strictly greater than zero.
     * <p>
     * This is a convenience method that delegates to {@link #assertPriceGreaterThanZero(String)}.
     *
     * @param value the numeric/price string to validate (e.g. "$17.99", "12.50")
     * @throws AssertionError if the value is null, empty, contains no digits, or is zero
     */
    public static void assertGreaterThanZero(final String value)
    {
        assertPriceGreaterThanZero(value);
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
            final ObjectMapper mapper = new ObjectMapper();
            final List<String> values = mapper.readValue(jsonArrayStr, new TypeReference<List<String>>(){});
            
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
        
        final Matcher m = Pattern.compile("\\d+").matcher(text);
        if (m.find())
        {
            return Long.parseLong(m.group());
        }
        throw new AssertionError("No number found in text: " + text);
    }

    /**
     * Parses a localized number string using the active Neodymium locale configuration.
     *
     * @param text the localized string to parse (e.g., "14,96 €" or "1,234.56")
     * @return the parsed BigDecimal value
     * @throws IllegalArgumentException if the input text is null or empty
     * @throws NumberFormatException if the number cannot be resolved
     */
    public static BigDecimal parseLocalizedBigDecimal(final String text)
    {
        String localeStr = null;
        try
        {
            if (Neodymium.getData() != null && Neodymium.getData().containsKey("locale"))
            {
                localeStr = Neodymium.dataValue("locale");
            }
            if (localeStr == null || localeStr.trim().isEmpty())
            {
                localeStr = Neodymium.configuration().locale();
            }
        }
        catch (final Exception e)
        {
            // Fallback for tests/environments where Neodymium configuration is not fully initialized
        }
        return parseLocalizedBigDecimal(text, localeStr);
    }

    /**
     * Parses a localized number string using an explicitly specified locale string.
     *
     * @param text the localized string to parse (e.g., "14,96 €" or "1,234.56")
     * @param localeStr the locale string (e.g., "de-DE", "en_US")
     * @return the parsed BigDecimal value
     * @throws IllegalArgumentException if the input text is null or empty
     * @throws NumberFormatException if the number cannot be resolved
     */
    public static BigDecimal parseLocalizedBigDecimal(final String text, final String localeStr)
    {
        if (text == null || text.trim().isEmpty())
        {
            throw new IllegalArgumentException("Input text is null or empty");
        }

        // Clean the input: keep digits, commas, dots, and minus sign
        String cleaned = text.replaceAll("[^\\d.,-]", "");

        // Determine sign
        final boolean isNegative = cleaned.startsWith("-") || cleaned.endsWith("-");
        
        // Remove minus signs to parse magnitude
        cleaned = cleaned.replace("-", "");

        if (cleaned.isEmpty())
        {
            throw new NumberFormatException("No digits found in: " + text);
        }

        BigDecimal magnitude = null;
        boolean parsed = false;

        // Try standard JDK NumberFormat if locale is present
        if (localeStr != null && !localeStr.trim().isEmpty())
        {
            try
            {
                final String normalizedLocale = localeStr.replace('_', '-');
                final Locale locale = Locale.forLanguageTag(normalizedLocale);
                final DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
                final char groupChar = symbols.getGroupingSeparator();
                final char decimalChar = symbols.getDecimalSeparator();

                // If the cleaned string has grouping separators, verify that they are in valid groupings (followed by exactly 3 digits).
                // If not, we ignore standard parsing and fall back to the dynamic fallback parser.
                if (!hasInvalidGrouping(cleaned, groupChar, decimalChar))
                {
                    final NumberFormat nf = NumberFormat.getInstance(locale);
                    if (nf instanceof DecimalFormat)
                    {
                        final DecimalFormat df = (DecimalFormat) nf;
                        df.setParseBigDecimal(true);
                        final ParsePosition pos = new ParsePosition(0);
                        final Number number = df.parse(cleaned, pos);

                        if (number != null && pos.getIndex() == cleaned.length())
                        {
                            magnitude = (BigDecimal) number;
                            parsed = true;
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                // Fallback
            }
        }

        // Fallback to pure-regex parser if JDK parser failed or locale was not provided
        if (!parsed)
        {
            magnitude = parseLocalizedBigDecimalFallback(cleaned);
        }

        return isNegative ? magnitude.negate() : magnitude;
    }

    /**
     * Pure-regex fallback parser to resolve numeric separators in a locale-agnostic way.
     */
    private static BigDecimal parseLocalizedBigDecimalFallback(final String clean)
    {
        final int firstDot = clean.indexOf('.');
        final int lastDot = clean.lastIndexOf('.');
        final int firstComma = clean.indexOf(',');
        final int lastComma = clean.lastIndexOf(',');

        if (firstDot != -1 && firstComma != -1)
        {
            // Both dot and comma exist
            if (lastDot < lastComma)
            {
                // Dot comes before comma (e.g., 1.234,56) -> comma is decimal separator
                final String normalized = clean.replace(".", "").replace(',', '.');
                return new BigDecimal(normalized);
            }
            else
            {
                // Comma comes before dot (e.g., 1,234.56) -> dot is decimal separator
                final String normalized = clean.replace(",", "");
                return new BigDecimal(normalized);
            }
        }
        else if (firstDot != -1)
        {
            // Only dots exist
            if (firstDot != lastDot)
            {
                // Multiple dots -> thousands separators (e.g., 1.234.567)
                final String normalized = clean.replace(".", "");
                return new BigDecimal(normalized);
            }
            else
            {
                // Exactly one dot
                return new BigDecimal(clean);
            }
        }
        else if (firstComma != -1)
        {
            // Only commas exist
            if (firstComma != lastComma)
            {
                // Multiple commas -> thousands separators (e.g., 1,234,567)
                final String normalized = clean.replace(",", "");
                return new BigDecimal(normalized);
            }
            else
            {
                // Exactly one comma -> replace with dot and parse as decimal separator
                final String normalized = clean.replace(',', '.');
                return new BigDecimal(normalized);
            }
        }
        else
        {
            // No dots or commas
            return new BigDecimal(clean);
        }
    }

    /**
     * Splits a double-argument string robustly.
     * First attempts to parse as a JSON array. If that fails, it scans comma indices to find a
     * split position where both resulting parts are valid numbers.
     */
    private static String[] parseTwoArguments(final String args, final String methodName)
    {
        if (args == null || args.trim().isEmpty())
        {
            throw new AssertionError(methodName + " expects two arguments, but got null or empty string");
        }

        final String trimmed = args.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]"))
        {
            try
            {
                final ObjectMapper mapper = new ObjectMapper();
                final List<String> values = mapper.readValue(trimmed, new TypeReference<List<String>>(){});
                if (values.size() == 2)
                {
                    return new String[]{values.get(0), values.get(1)};
                }
            }
            catch (final Exception e)
            {
                // Ignore and fall back to parsing as comma-separated string
            }
        }

        // Find all comma indices
        final List<Integer> commaIndices = new ArrayList<>();
        for (int i = 0; i < trimmed.length(); i++)
        {
            if (trimmed.charAt(i) == ',')
            {
                commaIndices.add(i);
            }
        }

        if (commaIndices.isEmpty())
        {
            throw new AssertionError(methodName + " expects two comma-separated arguments, but no comma was found in: " + args);
        }

        // Prioritize splits with trailing whitespace after the comma
        final List<Integer> candidateIndices = new ArrayList<>();
        for (final int idx : commaIndices)
        {
            if (idx + 1 < trimmed.length() && Character.isWhitespace(trimmed.charAt(idx + 1)))
            {
                candidateIndices.add(idx);
            }
        }
        for (final int idx : commaIndices)
        {
            if (!candidateIndices.contains(idx))
            {
                candidateIndices.add(idx);
            }
        }

        for (final int idx : candidateIndices)
        {
            final String left = trimmed.substring(0, idx).trim();
            final String right = trimmed.substring(idx + 1).trim();

            if (left.isEmpty() || right.isEmpty())
            {
                continue;
            }

            try
            {
                parseLocalizedBigDecimal(left);
                parseLocalizedBigDecimal(right);
                return new String[]{left, right};
            }
            catch (final Exception e)
            {
                // Try next split index
            }
        }

        // Last resort fallback: split by the last comma index
        final int lastComma = trimmed.lastIndexOf(',');
        final String left = trimmed.substring(0, lastComma).trim();
        final String right = trimmed.substring(lastComma + 1).trim();
        return new String[]{left, right};
    }

    /**
     * Asserts that the first number is strictly greater than the second number.
     * Expects either a JSON array or a comma-separated string containing exactly two values.
     *
     * @param args arguments string (e.g. "14,96 €, 0.00" or "[\"15.00\", \"10.00\"]")
     */
    public static void assertNumberGreaterThan(final String args)
    {
        final String[] parsedArgs = parseTwoArguments(args, "assertNumberGreaterThan");
        final BigDecimal val1 = parseLocalizedBigDecimal(parsedArgs[0]);
        final BigDecimal val2 = parseLocalizedBigDecimal(parsedArgs[1]);

        if (val1.compareTo(val2) <= 0)
        {
            throw new AssertionError(String.format("Expected '%s' > '%s' (parsed as %s > %s)", parsedArgs[0], parsedArgs[1], val1.toPlainString(), val2.toPlainString()));
        }
    }

    /**
     * Asserts that the first number is greater than or equal to the second number.
     * Expects either a JSON array or a comma-separated string containing exactly two values.
     *
     * @param args arguments string (e.g. "14,96 €, 14.00" or "[\"15.00\", \"15.00\"]")
     */
    public static void assertNumberGreaterThanOrEqual(final String args)
    {
        final String[] parsedArgs = parseTwoArguments(args, "assertNumberGreaterThanOrEqual");
        final BigDecimal val1 = parseLocalizedBigDecimal(parsedArgs[0]);
        final BigDecimal val2 = parseLocalizedBigDecimal(parsedArgs[1]);

        if (val1.compareTo(val2) < 0)
        {
            throw new AssertionError(String.format("Expected '%s' >= '%s' (parsed as %s >= %s)", parsedArgs[0], parsedArgs[1], val1.toPlainString(), val2.toPlainString()));
        }
    }

    /**
     * Asserts that the first number is strictly less than the second number.
     * Expects either a JSON array or a comma-separated string containing exactly two values.
     *
     * @param args arguments string (e.g. "14,96 €, 20.00" or "[\"5.00\", \"10.00\"]")
     */
    public static void assertNumberLessThan(final String args)
    {
        final String[] parsedArgs = parseTwoArguments(args, "assertNumberLessThan");
        final BigDecimal val1 = parseLocalizedBigDecimal(parsedArgs[0]);
        final BigDecimal val2 = parseLocalizedBigDecimal(parsedArgs[1]);

        if (val1.compareTo(val2) >= 0)
        {
            throw new AssertionError(String.format("Expected '%s' < '%s' (parsed as %s < %s)", parsedArgs[0], parsedArgs[1], val1.toPlainString(), val2.toPlainString()));
        }
    }

    /**
     * Asserts that the first number is less than or equal to the second number.
     * Expects either a JSON array or a comma-separated string containing exactly two values.
     *
     * @param args arguments string (e.g. "14,96 €, 15.00" or "[\"10.00\", \"10.00\"]")
     */
    public static void assertNumberLessThanOrEqual(final String args)
    {
        final String[] parsedArgs = parseTwoArguments(args, "assertNumberLessThanOrEqual");
        final BigDecimal val1 = parseLocalizedBigDecimal(parsedArgs[0]);
        final BigDecimal val2 = parseLocalizedBigDecimal(parsedArgs[1]);

        if (val1.compareTo(val2) > 0)
        {
            throw new AssertionError(String.format("Expected '%s' <= '%s' (parsed as %s <= %s)", parsedArgs[0], parsedArgs[1], val1.toPlainString(), val2.toPlainString()));
        }
    }

    /**
     * Asserts that the first number is equal to the second number.
     * Expects either a JSON array or a comma-separated string containing exactly two values.
     *
     * @param args arguments string (e.g. "14,96 €, 14.96" or "[\"10.00\", \"10.00\"]")
     */
    public static void assertNumbersEqual(final String args)
    {
        final String[] parsedArgs = parseTwoArguments(args, "assertNumbersEqual");
        final BigDecimal val1 = parseLocalizedBigDecimal(parsedArgs[0]);
        final BigDecimal val2 = parseLocalizedBigDecimal(parsedArgs[1]);

        if (val1.compareTo(val2) != 0)
        {
            throw new AssertionError(String.format("Expected '%s' to equal '%s' (parsed as %s == %s)", parsedArgs[0], parsedArgs[1], val1.toPlainString(), val2.toPlainString()));
        }
    }

    /**
     * Splits a regex-argument string robustly.
     * First attempts to parse as a JSON array. If that fails, it splits by the first comma character
     * (since the first argument—the value to check—is usually a single identifier/token without commas,
     * whereas the regex pattern can contain commas like {10,15}).
     *
     * @param args the raw arguments string
     * @param methodName the name of the method calling this helper
     * @return the two parsed string arguments
     * @throws AssertionError if args is null/empty or parsing fails
     */
    private static String[] parseRegexArguments(final String args, final String methodName)
    {
        if (args == null || args.trim().isEmpty())
        {
            throw new AssertionError(methodName + " expects two arguments, but got null or empty string");
        }

        final String trimmed = args.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]"))
        {
            try
            {
                final ObjectMapper mapper = new ObjectMapper();
                final List<String> values = mapper.readValue(trimmed, new TypeReference<List<String>>(){});
                if (values.size() == 2)
                {
                    return new String[]{values.get(0), values.get(1)};
                }
            }
            catch (final Exception e)
            {
                // Ignore and fall back to split by first comma
            }
        }

        final int firstComma = trimmed.indexOf(',');
        if (firstComma == -1)
        {
            throw new AssertionError(methodName + " expects two comma-separated arguments or a JSON array of two elements, but no comma was found in: " + args);
        }

        final String left = trimmed.substring(0, firstComma).trim();
        final String right = trimmed.substring(firstComma + 1).trim();

        return new String[]{left, right};
    }

    /**
     * Asserts that the provided value matches the given regular expression pattern.
     * Expects either a JSON array or a comma-separated string containing exactly two values:
     * the value to check and the regex pattern.
     *
     * @param args arguments string (e.g. "ORD-12345, ^ORD-[0-9]{5}$" or "[\"ORD-12345\", \"^ORD-[0-9]{5}$\"]")
     * @throws AssertionError if the arguments are invalid, the regex pattern is malformed, or the value does not match the pattern
     */
    public static void assertMatchesRegex(final String args)
    {
        final String[] parsedArgs = parseRegexArguments(args, "assertMatchesRegex");
        final String value = parsedArgs[0];
        final String regex = parsedArgs[1];

        try
        {
            final Pattern pattern = Pattern.compile(regex);
            final Matcher matcher = pattern.matcher(value);
            if (!matcher.matches())
            {
                throw new AssertionError(String.format("Expected '%s' to match regular expression '%s', but it did not", value, regex));
            }
        }
        catch (final PatternSyntaxException e)
        {
            throw new AssertionError("Invalid regular expression pattern: " + regex, e);
        }
    }

    /**
     * Asserts that the provided mathematical equation is correct within an allowed tolerance of 0.02.
     * <p>
     * The method is locale-agnostic and supports parsing formatted currencies and percentage values.
     * Programmatic evaluation is performed securely via the JDK {@code JShell} engine.
     *
     * @param equation the mathematical equation to assert (e.g. "0,90 € = (14,96 € + 0,00 €) * 6,00%")
     * @throws AssertionError if the equation is null, empty, does not contain exactly one '=' operator,
     *                        fails to parse, or is mathematically inconsistent
     * @throws SecurityException if the equation fails the safety guardrail check
     */
    public static void assertCalculation(final String equation)
    {
        if (equation == null || equation.trim().isEmpty())
        {
            throw new AssertionError("Equation string is null or empty");
        }

        // Reject common injection characters and Java code signatures to ensure safety
        if (equation.contains(";") || equation.contains("\"") || equation.contains("'") || 
            equation.contains("//") || equation.contains("/*") || equation.contains("{") || equation.contains("}"))
        {
            throw new SecurityException("Security check failed: Equation contains injection characters");
        }

        final String lower = equation.toLowerCase(Locale.ROOT);
        if (lower.contains("system") || lower.contains("runtime") || lower.contains("exec") || 
            lower.contains("process") || lower.contains("class") || lower.contains("new") || 
            lower.contains("math") || lower.contains("java") || lower.contains("object"))
        {
            throw new SecurityException("Security check failed: Equation contains potentially unsafe code words");
        }

        final String[] sides = equation.split("=");
        if (sides.length != 2)
        {
            throw new AssertionError("Equation must contain exactly one '=' operator, but got: " + equation);
        }


        try
        {
            final String normalizedLhs = normalizeExpression(sides[0]);
            final String normalizedRhs = normalizeExpression(sides[1]);

            final double lhsDoubleVal = evaluateExpression(normalizedLhs);
            final double rhsDoubleVal = evaluateExpression(normalizedRhs);

            final BigDecimal lhsVal = new BigDecimal(Double.toString(lhsDoubleVal));
            final BigDecimal rhsVal = new BigDecimal(Double.toString(rhsDoubleVal));

            final int precision = detectDisplayPrecision(sides[0]);
            final BigDecimal roundedRhsVal = rhsVal.setScale(precision, RoundingMode.HALF_UP);

            final BigDecimal difference = lhsVal.subtract(roundedRhsVal).abs();
            final BigDecimal tolerance = new BigDecimal("0.02");

            if (difference.compareTo(tolerance) > 0)
            {
                throw new AssertionError(String.format("Equation is mathematically inconsistent: '%s' != '%s' (parsed as %s != %s, after precision-aware rounding to %d decimal places: %s != %s, difference is %s which exceeds allowed tolerance of 0.02)",
                    sides[0].trim(), sides[1].trim(), lhsVal.toPlainString(), rhsVal.toPlainString(), precision, lhsVal.toPlainString(), roundedRhsVal.toPlainString(), difference.toPlainString()));
            }
        }
        catch (final SecurityException e)
        {
            throw e;
        }
        catch (final Exception e)
        {
            throw new AssertionError("Failed to parse and assert equation: " + equation, e);
        }
    }

    /**
     * Detects the precision (number of decimal places) from the displayed numeric value on the LHS.
     * Reuses the separator heuristics from the fallback parser.
     *
     * @param text the side of the equation to inspect
     * @return the number of decimal places detected (0 for integers)
     */
    public static int detectDisplayPrecision(final String text)
    {
        if (text == null)
        {
            return 0;
        }

        final String cleaned = text.replaceAll("[^\\d.,-]", "").trim();
        if (cleaned.isEmpty())
        {
            return 0;
        }

        final int firstDot = cleaned.indexOf('.');
        final int lastDot = cleaned.lastIndexOf('.');
        final int firstComma = cleaned.indexOf(',');
        final int lastComma = cleaned.lastIndexOf(',');

        if (firstDot != -1 && firstComma != -1)
        {
            if (lastDot < lastComma)
            {
                return cleaned.length() - 1 - lastComma;
            }
            else
            {
                return cleaned.length() - 1 - lastDot;
            }
        }
        else if (firstDot != -1)
        {
            if (firstDot != lastDot)
            {
                return 0;
            }
            else
            {
                return cleaned.length() - 1 - firstDot;
            }
        }
        else if (firstComma != -1)
        {
            if (firstComma != lastComma)
            {
                return 0;
            }
            else
            {
                return cleaned.length() - 1 - firstComma;
            }
        }
        else
        {
            return 0;
        }
    }

    /**
     * Normalizes a localized mathematical expression side.
     */
    private static String normalizeExpression(final String expr)
    {
        final StringBuilder normalized = new StringBuilder();
        final StringBuilder currentOperand = new StringBuilder();

        for (int i = 0; i < expr.length(); i++)
        {
            final char c = expr.charAt(i);
            if (c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')')
            {
                if (currentOperand.length() > 0)
                {
                    final String opStr = currentOperand.toString().trim();
                    if (!opStr.isEmpty())
                    {
                        normalized.append(parseOperand(opStr)).append(" ");
                    }
                    currentOperand.setLength(0);
                }
                normalized.append(c).append(" ");
            }
            else
            {
                currentOperand.append(c);
            }
        }

        if (currentOperand.length() > 0)
        {
            final String opStr = currentOperand.toString().trim();
            if (!opStr.isEmpty())
            {
                normalized.append(parseOperand(opStr)).append(" ");
            }
        }

        return normalized.toString().trim();
    }

    private static double parseOperand(final String operand)
    {
        final String trimmed = operand.trim();
        if (trimmed.isEmpty())
        {
            throw new IllegalArgumentException("Operand is empty");
        }
        final boolean isPercent = trimmed.endsWith("%");
        final String clean = isPercent ? trimmed.substring(0, trimmed.length() - 1).trim() : trimmed;
        final BigDecimal value = parseLocalizedBigDecimal(clean);
        final double doubleValue = value.doubleValue();
        return isPercent ? doubleValue / 100.0 : doubleValue;
    }

    /**
     * Evaluates a mathematical expression securely using JDK JShell.
     */
    private static double evaluateExpression(final String expression)
    {
        // Safety guardrail: only allow digits, dots, plus, minus, star, slash, parentheses, and spaces.
        // This completely eliminates risk of executing arbitrary Java code or injections.
        if (!expression.matches("^[0-9.+\\-*/()\\s]+$"))
        {
            throw new SecurityException("Expression contains invalid or unsafe characters: " + expression);
        }

        try (final JShell jshell = JShell.create())
        {
            final List<SnippetEvent> events = jshell.eval(expression);
            if (events.isEmpty())
            {
                throw new AssertionError("No evaluation events returned for expression: " + expression);
            }

            final SnippetEvent event = events.get(events.size() - 1);
            if (event.status() == Snippet.Status.REJECTED)
            {
                throw new AssertionError("Expression evaluation was rejected: " + expression);
            }

            if (event.exception() != null)
            {
                throw new AssertionError("Exception thrown during expression evaluation: " + expression, event.exception());
            }

            final String valStr = event.value();
            if (valStr == null || valStr.isEmpty())
            {
                throw new AssertionError("Expression evaluated to null or empty: " + expression);
            }

            return Double.parseDouble(valStr);
        }
        catch (final Exception e)
        {
            throw new AssertionError("Failed to evaluate expression: " + expression, e);
        }
    }

    /**
     * Checks if the grouping separator character is used in an invalid position in the cleaned string.
     * In standard formatting, any grouping separator (comma or dot) must be followed by exactly 3 digits
     * before another grouping separator, a decimal separator, or the end of the string.
     */
    private static boolean hasInvalidGrouping(final String cleaned, final char groupChar, final char decimalChar)
    {
        if (groupChar != '.' && groupChar != ',')
        {
            return false;
        }

        final int decIdx = cleaned.indexOf(decimalChar);
        final int endLimit = (decIdx != -1) ? decIdx : cleaned.length();

        int lastGroupIdx = -1;
        for (int i = 0; i < endLimit; i++)
        {
            if (cleaned.charAt(i) == groupChar)
            {
                if (lastGroupIdx != -1)
                {
                    // Distance between group separators must be exactly 4 (e.g. 1,234,567 -> indices 1 and 5 -> 5-1 = 4)
                    if (i - lastGroupIdx != 4)
                    {
                        return true;
                    }
                }
                lastGroupIdx = i;
            }
        }

        if (lastGroupIdx != -1)
        {
            // Distance from last group separator to endLimit must be exactly 4 (3 digits)
            if (endLimit - lastGroupIdx != 4)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Generically determines whether the provided text represents a numeric or price value.
     * <p>
     * To be generic and locale/currency-agnostic, a string is considered numeric or price if:
     * 1. It contains at least one digit character.
     * 2. It contains only digits, standard mathematical signs (+, -), standard separators/whitespaces,
     *    percent sign (%), Unicode currency symbols, or at most one contiguous sequence of alphabetic letters.
     * 3. Any mathematical sign (+, -) present is not immediately preceded by an alphabetic letter
     *    (to differentiate signs from hyphenated alphanumeric codes like "ORD-1234").
     * 4. If a contiguous sequence of alphabetic letters is present, its length does not exceed 3 characters
     *    (covering short currency codes/abbreviations like USD, EUR, JPY, kr, zł, but excluding descriptive words
     *    like "Page", "Item", "Total", "Results").
     *
     * @param text the input string to evaluate
     * @return {@code true} if the text represents a numeric or price value; {@code false} otherwise
     */
    public static boolean isNumericOrPrice(final String text)
    {
        if (text == null)
        {
            return false;
        }

        final String trimmed = text.trim();
        if (trimmed.isEmpty())
        {
            return false;
        }

        boolean hasDigit = false;
        int letterSeqStart = -1;
        int letterSeqCount = 0;
        int maxLetterSeqLen = 0;

        for (int i = 0; i < trimmed.length(); i++)
        {
            final char c = trimmed.charAt(i);

            if (Character.isDigit(c))
            {
                hasDigit = true;
                if (letterSeqStart != -1)
                {
                    final int len = i - letterSeqStart;
                    if (len > maxLetterSeqLen)
                    {
                        maxLetterSeqLen = len;
                    }
                    letterSeqStart = -1;
                }
            }
            else if (Character.isLetter(c))
            {
                if (letterSeqStart == -1)
                {
                    letterSeqStart = i;
                    letterSeqCount++;
                }
            }
            else
            {
                if (letterSeqStart != -1)
                {
                    final int len = i - letterSeqStart;
                    if (len > maxLetterSeqLen)
                    {
                        maxLetterSeqLen = len;
                    }
                    letterSeqStart = -1;
                }

                // Check allowed non-digit, non-letter characters
                final boolean isSign = (c == '+' || c == '-');
                final boolean isSeparator = (c == '.' || c == ',' || c == '\'' || c == '`' || Character.isWhitespace(c));
                final boolean isCurrencySymbol = (Character.getType(c) == Character.CURRENCY_SYMBOL);
                final boolean isPercent = (c == '%');

                if (!isSign && !isSeparator && !isCurrencySymbol && !isPercent)
                {
                    // Any other character (e.g., #, @, !, (, ), [, ], _, /, \) makes it non-numeric
                    return false;
                }

                // Reject if a sign is immediately preceded by an alphabetic letter (e.g., "ORD-1234")
                if (isSign && i > 0 && Character.isLetter(trimmed.charAt(i - 1)))
                {
                    return false;
                }
            }
        }

        // Handle letter sequence ending at the end of the string
        if (letterSeqStart != -1)
        {
            final int len = trimmed.length() - letterSeqStart;
            if (len > maxLetterSeqLen)
            {
                maxLetterSeqLen = len;
            }
        }

        if (!hasDigit)
        {
            return false;
        }

        // Must have at most one contiguous sequence of letters
        if (letterSeqCount > 1)
        {
            return false;
        }

        // If letters exist, the sequence length must not exceed 3 characters (e.g., "USD", "kr", "zł")
        if (letterSeqCount == 1 && maxLetterSeqLen > 3)
        {
            return false;
        }

        return true;
    }

    /**
     * Generically normalizes a localized numeric or price string to standard US decimal format.
     * If the string does not represent a numeric or price value, it returns the original text.
     *
     * @param text the input string to normalize
     * @return the normalized US decimal string, or the original text if not a number/price
     */
    public static String normalizeNumericOrPrice(final String text)
    {
        if (!isNumericOrPrice(text))
        {
            return text;
        }
        try
        {
            final BigDecimal value = parseLocalizedBigDecimal(text);
            final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            final DecimalFormat format = new DecimalFormat("0.######", symbols);
            return format.format(value);
        }
        catch (final Exception e)
        {
            return text;
        }
    }
}

