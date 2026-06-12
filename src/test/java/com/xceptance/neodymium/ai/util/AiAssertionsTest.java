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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link AiAssertions} utility class.
 * Ensures robust localized numeric parsing and comparison operations.
 */
public final class AiAssertionsTest
{
    private void assertBigDecimalEquals(final String expected, final BigDecimal actual)
    {
        Assertions.assertNotNull(actual, "Actual BigDecimal is null");
        Assertions.assertTrue(new BigDecimal(expected).compareTo(actual) == 0,
            String.format("Expected %s but was %s", expected, actual.toPlainString()));
    }

    /**
     * Verifies correct parsing of localized numbers across various locales.
     */
    @Test
    public void testParseLocalizedBigDecimalWithExplicitLocales()
    {
        // de-DE locale (comma as decimal separator, dot as thousands separator)
        assertBigDecimalEquals("14.96", AiAssertions.parseLocalizedBigDecimal("14,96 €", "de-DE"));
        assertBigDecimalEquals("1234.56", AiAssertions.parseLocalizedBigDecimal("1.234,56", "de-DE"));
        assertBigDecimalEquals("-1234.56", AiAssertions.parseLocalizedBigDecimal("-1.234,56", "de-DE"));

        // en-US locale (dot as decimal separator, comma as thousands separator)
        assertBigDecimalEquals("14.96", AiAssertions.parseLocalizedBigDecimal("14.96", "en-US"));
        assertBigDecimalEquals("1234.56", AiAssertions.parseLocalizedBigDecimal("1,234.56 USD", "en-US"));
        assertBigDecimalEquals("-0.5", AiAssertions.parseLocalizedBigDecimal("$-0.50", "en-US"));

        // sv-SE locale (space as thousands separator, comma as decimal separator)
        assertBigDecimalEquals("1234.56", AiAssertions.parseLocalizedBigDecimal("1 234,56 kr", "sv-SE"));

        // ja-JP locale (no decimal separator, comma as thousands separator)
        assertBigDecimalEquals("1500.0", AiAssertions.parseLocalizedBigDecimal("¥1,500", "ja-JP"));
    }

    /**
     * Verifies the regex fallback parser when locale-based parsing is unavailable or fails.
     */
    @Test
    public void testParseLocalizedBigDecimalFallback()
    {
        // Both dot and comma (dot before comma)
        assertBigDecimalEquals("1234.56", AiAssertions.parseLocalizedBigDecimal("1.234,56", null));
        assertBigDecimalEquals("1234.56", AiAssertions.parseLocalizedBigDecimal("1.234,56", ""));

        // Both dot and comma (comma before dot)
        assertBigDecimalEquals("1234.56", AiAssertions.parseLocalizedBigDecimal("1,234.56", null));

        // Only dot
        assertBigDecimalEquals("12.34", AiAssertions.parseLocalizedBigDecimal("12.34", null));
        assertBigDecimalEquals("1234567.0", AiAssertions.parseLocalizedBigDecimal("1.234.567", null));

        // Only comma
        assertBigDecimalEquals("12.34", AiAssertions.parseLocalizedBigDecimal("12,34", null));
        assertBigDecimalEquals("1234567.0", AiAssertions.parseLocalizedBigDecimal("1,234,567", null));

        // No separators
        assertBigDecimalEquals("1234.0", AiAssertions.parseLocalizedBigDecimal("1234", null));
        assertBigDecimalEquals("-99.0", AiAssertions.parseLocalizedBigDecimal("-99", null));
        assertBigDecimalEquals("-99.0", AiAssertions.parseLocalizedBigDecimal("99-", null));
    }

    /**
     * Verifies that parseLocalizedBigDecimal throws a NumberFormatException for completely non-numeric strings.
     */
    @Test
    public void testParseLocalizedBigDecimalEmptyDigitsFailure()
    {
        Assertions.assertThrows(NumberFormatException.class, () -> {
            AiAssertions.parseLocalizedBigDecimal("abc");
        });
    }

    /**
     * Verifies that parseLocalizedBigDecimal throws an IllegalArgumentException for null inputs.
     */
    @Test
    public void testParseLocalizedBigDecimalNullFailure()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AiAssertions.parseLocalizedBigDecimal(null);
        });
    }

    /**
     * Verifies assertNumberGreaterThan.
     */
    @Test
    public void testAssertNumberGreaterThanSuccess()
    {
        AiAssertions.assertNumberGreaterThan("15.00, 10.00");
        AiAssertions.assertNumberGreaterThan("14,96 €, 0.00");
        AiAssertions.assertNumberGreaterThan("[\"15.00\", \"10.00\"]");
    }

    /**
     * Verifies assertNumberGreaterThan failures.
     */
    @Test
    public void testAssertNumberGreaterThanFailure()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertNumberGreaterThan("10.00, 15.00");
        });
    }

    /**
     * Verifies assertNumberGreaterThanOrEqual.
     */
    @Test
    public void testAssertNumberGreaterThanOrEqualSuccess()
    {
        AiAssertions.assertNumberGreaterThanOrEqual("15.00, 15.00");
        AiAssertions.assertNumberGreaterThanOrEqual("14,96 €, 14.00");
        AiAssertions.assertNumberGreaterThanOrEqual("[\"15.00\", \"15.00\"]");
    }

    /**
     * Verifies assertNumberGreaterThanOrEqual failures.
     */
    @Test
    public void testAssertNumberGreaterThanOrEqualFailure()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertNumberGreaterThanOrEqual("10.00, 15.00");
        });
    }

    /**
     * Verifies assertNumberLessThan.
     */
    @Test
    public void testAssertNumberLessThanSuccess()
    {
        AiAssertions.assertNumberLessThan("10.00, 15.00");
        AiAssertions.assertNumberLessThan("14,96 €, 20.00");
        AiAssertions.assertNumberLessThan("[\"5.00\", \"10.00\"]");
    }

    /**
     * Verifies assertNumberLessThan failures.
     */
    @Test
    public void testAssertNumberLessThanFailure()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertNumberLessThan("15.00, 10.00");
        });
    }

    /**
     * Verifies assertNumberLessThanOrEqual.
     */
    @Test
    public void testAssertNumberLessThanOrEqualSuccess()
    {
        AiAssertions.assertNumberLessThanOrEqual("10.00, 10.00");
        AiAssertions.assertNumberLessThanOrEqual("14,96 €, 15.00");
        AiAssertions.assertNumberLessThanOrEqual("[\"10.00\", \"10.00\"]");
    }

    /**
     * Verifies assertNumberLessThanOrEqual failures.
     */
    @Test
    public void testAssertNumberLessThanOrEqualFailure()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertNumberLessThanOrEqual("15.00, 10.00");
        });
    }

    /**
     * Verifies assertNumbersEqual.
     */
    @Test
    public void testAssertNumbersEqualSuccess()
    {
        AiAssertions.assertNumbersEqual("14,96, 14.96");
        AiAssertions.assertNumbersEqual("[\"10.00\", \"10.00\"]");
    }

    /**
     * Verifies assertNumbersEqual failures.
     */
    @Test
    public void testAssertNumbersEqualFailure()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertNumbersEqual("14.96, 15.00");
        });
    }

    /**
     * Verifies that the legacy assertPriceGreaterThanZero method remains fully functional.
     */
    @Test
    public void testAssertPriceGreaterThanZero()
    {
        AiAssertions.assertPriceGreaterThanZero("$17.99");
        AiAssertions.assertPriceGreaterThanZero("1.234,56 €");
        AiAssertions.assertPriceGreaterThanZero("¥1500");
    }

    /**
     * Verifies assertPriceGreaterThanZero failures.
     */
    @Test
    public void testAssertPriceGreaterThanZeroFailure()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertPriceGreaterThanZero("$0.00");
        });
    }

    /**
     * Verifies that the assertGreaterThanZero method is fully functional.
     */
    @Test
    public void testAssertGreaterThanZeroSuccess()
    {
        AiAssertions.assertGreaterThanZero("$17.99");
        AiAssertions.assertGreaterThanZero("1.234,56 €");
        AiAssertions.assertGreaterThanZero("¥1500");
    }

    /**
     * Verifies assertGreaterThanZero failures.
     */
    @Test
    public void testAssertGreaterThanZeroFailure()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertGreaterThanZero("$0.00");
        });
    }

    /**
     * Verifies that the legacy verifyLessOrEqual method remains fully functional.
     */
    @Test
    public void testVerifyLessOrEqual()
    {
        AiAssertions.verifyLessOrEqual("[\"Results (10)\", \"Results (15)\"]");
        AiAssertions.verifyLessOrEqual("[\"10\", \"10\"]");
    }

    /**
     * Verifies verifyLessOrEqual failures.
     */
    @Test
    public void testVerifyLessOrEqualFailure()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.verifyLessOrEqual("[\"Results (15)\", \"Results (10)\"]");
        });
    }

    /**
     * Asserts assertCalculation with correct mathematics across different formats, spaces, and locales.
     */
    @Test
    public void testAssertCalculationSuccess()
    {
        // German locale style equation with currency and percentage
        AiAssertions.assertCalculation("0,90 € = (14,96 € + 0,00 €) * 6,00%");

        // US locale style equation with currency
        AiAssertions.assertCalculation("15.00 USD = 10.00 USD + 5.00 USD");

        // Plain numbers with percentage
        AiAssertions.assertCalculation("0.90 = (14.96 + 0) * 6%");

        // Order of operations and parentheses
        AiAssertions.assertCalculation("2 + 3 * 4 = 14");
        AiAssertions.assertCalculation("(2 + 3) * 4 = 20");

        // Basic arithmetic division and negative results
        AiAssertions.assertCalculation("100 = 200 / 2");
        AiAssertions.assertCalculation("-1.50 = 1.50 * -1");

        // Zero value test
        AiAssertions.assertCalculation("0.00 = 0");

        // Tolerance/rounding test (difference within 0.02)
        // (14.96 + 0.0) * 0.06 = 0.8976, which is within 0.02 of 0.90
        AiAssertions.assertCalculation("0.90 = 0.8976");
        AiAssertions.assertCalculation("0.90 = 0.8801"); // diff is 0.0199 <= 0.02
        AiAssertions.assertCalculation("0.90 = 0.9199"); // diff is 0.0199 <= 0.02
    }

    /**
     * Verifies assertCalculation fails when the mathematical equation is incorrect.
     */
    @Test
    public void testAssertCalculationMathFailure()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertCalculation("1.00 = 1.05");
        });
    }

    /**
     * Verifies assertCalculation fails when difference is slightly larger than 0.02.
     */
    @Test
    public void testAssertCalculationMathToleranceFailure()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertCalculation("0.90 = 0.8749"); // diff is 0.03 > 0.02 after rounding to 0.87
        });
    }

    /**
     * Verifies assertCalculation fails when a malicious expression or code is injected (alphabetic characters rejected).
     */
    @Test
    public void testAssertCalculationSecuritySystemExit()
    {
        Assertions.assertThrows(SecurityException.class, () -> {
            AiAssertions.assertCalculation("System.exit(0) = 0");
        });
    }

    /**
     * Verifies assertCalculation fails when a malicious expression or code is injected (semicolon/commands rejected).
     */
    @Test
    public void testAssertCalculationSecurityCommand()
    {
        Assertions.assertThrows(SecurityException.class, () -> {
            AiAssertions.assertCalculation("1 + 1 = 2; Runtime.getRuntime().exec(\"echo\")");
        });
    }

    /**
     * Verifies assertCalculation fails when comments are added.
     */
    @Test
    public void testAssertCalculationSecurityComments()
    {
        Assertions.assertThrows(SecurityException.class, () -> {
            AiAssertions.assertCalculation("1 + 1 = 2 // test");
        });
    }

    /**
     * Verifies assertCalculation fails when calling valid Java Math libraries programmatically (since alphabet is blocked).
     */
    @Test
    public void testAssertCalculationSecurityJavaMath()
    {
        Assertions.assertThrows(SecurityException.class, () -> {
            AiAssertions.assertCalculation("1 = Math.min(1, 2)");
        });
    }

    /**
     * Verifies assertCalculation fails on an empty equation.
     */
    @Test
    public void testAssertCalculationEmptyFailure()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertCalculation("");
        });
    }

    /**
     * Verifies assertCalculation fails when there is no equals sign.
     */
    @Test
    public void testAssertCalculationNoEqualsFailure()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertCalculation("1 + 1");
        });
    }

    /**
     * Verifies assertCalculation fails when there are multiple equals signs.
     */
    @Test
    public void testAssertCalculationMultipleEqualsFailure()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertCalculation("1 = 1 = 1");
        });
    }

    /**
     * Verifies assertCalculation fails when operands cannot be parsed.
     */
    @Test
    public void testAssertCalculationMalformedOperandsFailure()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertCalculation("abc = def");
        });
    }

    /**
     * Verifies detectDisplayPrecision across various formatted numbers.
     */
    @Test
    public void testDetectDisplayPrecision()
    {
        Assertions.assertEquals(0, AiAssertions.detectDisplayPrecision("￥1"));
        Assertions.assertEquals(0, AiAssertions.detectDisplayPrecision("¥1"));
        Assertions.assertEquals(0, AiAssertions.detectDisplayPrecision("1 JPY"));
        Assertions.assertEquals(2, AiAssertions.detectDisplayPrecision("$0.90"));
        Assertions.assertEquals(2, AiAssertions.detectDisplayPrecision("14,96 €"));
        Assertions.assertEquals(2, AiAssertions.detectDisplayPrecision("1.234,56 €"));
        Assertions.assertEquals(2, AiAssertions.detectDisplayPrecision("1,234.56"));
        Assertions.assertEquals(0, AiAssertions.detectDisplayPrecision("1.234.567"));
        Assertions.assertEquals(0, AiAssertions.detectDisplayPrecision("1,234,567"));
        Assertions.assertEquals(0, AiAssertions.detectDisplayPrecision("1234"));
    }

    /**
     * Verifies assertCalculation with precision-aware rounding on JPY and integer-only formatting.
     */
    @Test
    public void testAssertCalculationIntegerPrecision()
    {
        // JPY equations that mathematically produce decimal tax, but are rounded to integer on UI
        AiAssertions.assertCalculation("￥1 = (￥17 + ￥7) * 6.00 / 100");
        AiAssertions.assertCalculation("¥1 = (¥17 + ¥7) * 6.00 / 100");
        AiAssertions.assertCalculation("1 JPY = (17 JPY + 7 JPY) * 6.00 / 100");

        // Normal integer matching
        AiAssertions.assertCalculation("3 = 10 * 30 / 100");
    }

    /**
     * Verifies assertCalculation failures on JPY equations when the math is fundamentally incorrect.
     */
    @Test
    public void testAssertCalculationIntegerPrecisionFailure()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertCalculation("￥5 = (￥17 + ￥7) * 6.00 / 100");
        });
    }

    /**
     * Verifies the isNumericOrPrice method against various valid and invalid strings.
     */
    @Test
    public void testIsNumericOrPrice()
    {
        // Valid numeric and price values
        Assertions.assertTrue(AiAssertions.isNumericOrPrice("14,96 €"));
        Assertions.assertTrue(AiAssertions.isNumericOrPrice("￥1,500"));
        Assertions.assertTrue(AiAssertions.isNumericOrPrice("6,00%"));
        Assertions.assertTrue(AiAssertions.isNumericOrPrice("$-0.50"));
        Assertions.assertTrue(AiAssertions.isNumericOrPrice("120 USD"));
        Assertions.assertTrue(AiAssertions.isNumericOrPrice("120kr"));
        Assertions.assertTrue(AiAssertions.isNumericOrPrice("zł 150"));
        Assertions.assertTrue(AiAssertions.isNumericOrPrice("-14.96"));
        Assertions.assertTrue(AiAssertions.isNumericOrPrice("123"));

        // Invalid numeric/price strings (alphanumeric codes, page text, labels)
        Assertions.assertFalse(AiAssertions.isNumericOrPrice("ORD-12345678"));
        Assertions.assertFalse(AiAssertions.isNumericOrPrice("TC-001"));
        Assertions.assertFalse(AiAssertions.isNumericOrPrice("Results (10)"));
        Assertions.assertFalse(AiAssertions.isNumericOrPrice("Page 1 of 5"));
        Assertions.assertFalse(AiAssertions.isNumericOrPrice("Total"));
        Assertions.assertFalse(AiAssertions.isNumericOrPrice("Order #123"));
        Assertions.assertFalse(AiAssertions.isNumericOrPrice(""));
        Assertions.assertFalse(AiAssertions.isNumericOrPrice("   "));
        Assertions.assertFalse(AiAssertions.isNumericOrPrice(null));
    }

    /**
     * Verifies the normalizeNumericOrPrice method.
     */
    @Test
    public void testNormalizeNumericOrPrice()
    {
        // Valid numeric/price normalizations
        Assertions.assertEquals("14.96", AiAssertions.normalizeNumericOrPrice("14,96 €"));
        Assertions.assertEquals("1500", AiAssertions.normalizeNumericOrPrice("￥1,500"));
        Assertions.assertEquals("6", AiAssertions.normalizeNumericOrPrice("6,00%"));
        Assertions.assertEquals("-0.5", AiAssertions.normalizeNumericOrPrice("$-0.50"));
        Assertions.assertEquals("120", AiAssertions.normalizeNumericOrPrice("120 USD"));
        Assertions.assertEquals("120", AiAssertions.normalizeNumericOrPrice("120kr"));
        Assertions.assertEquals("150", AiAssertions.normalizeNumericOrPrice("zł 150"));

        // Alphanumeric and other descriptive strings must remain unchanged
        Assertions.assertEquals("ORD-12345678", AiAssertions.normalizeNumericOrPrice("ORD-12345678"));
        Assertions.assertEquals("Results (10)", AiAssertions.normalizeNumericOrPrice("Results (10)"));
        Assertions.assertEquals("Page 1 of 5", AiAssertions.normalizeNumericOrPrice("Page 1 of 5"));
    }

    /**
     * Verifies that assertMatchesRegex successfully matches valid values against regular expressions.
     */
    @Test
    public void testAssertMatchesRegexSuccess()
    {
        // JSON array format
        AiAssertions.assertMatchesRegex("[\"ORD-1779265279589\", \"^ORD-[0-9]{10,15}$\"]");
        AiAssertions.assertMatchesRegex("[\"123\", \"^[0-9]+$\"]");

        // Comma-separated format (even with comma in regex pattern like {10,15})
        AiAssertions.assertMatchesRegex("ORD-1779265279589, ^ORD-[0-9]{10,15}$");
        AiAssertions.assertMatchesRegex("123, ^[0-9]+$");
    }

    /**
     * Verifies that assertMatchesRegex correctly throws AssertionError on mismatches.
     */
    @Test
    public void testAssertMatchesRegexFailure()
    {
        // Pattern mismatch
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertMatchesRegex("[\"ORD-12\", \"^ORD-[0-9]{10,15}$\"]");
        });

        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertMatchesRegex("ORD-12, ^ORD-[0-9]{10,15}$");
        });
    }

    /**
     * Verifies that assertMatchesRegex throws an AssertionError on syntax errors in regex pattern.
     */
    @Test
    public void testAssertMatchesRegexSyntaxError()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertMatchesRegex("[\"ORD-12\", \"^ORD-[0-9{10,15}$\"]");
        });
    }

    /**
     * Verifies that assertMatchesRegex throws an AssertionError on malformed/invalid inputs.
     */
    @Test
    public void testAssertMatchesRegexInvalidInput()
    {
        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertMatchesRegex("");
        });

        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertMatchesRegex(null);
        });

        Assertions.assertThrows(AssertionError.class, () -> {
            AiAssertions.assertMatchesRegex("ORD-12345");
        });
    }
}
