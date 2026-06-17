/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.neodymium.ai.integration;

import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.util.AiAssertions;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

/**
 * Integration test verifying AI execution of non-assertion helper methods from AiAssertions.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("freeform")
public class JavaNonAssertionMethodTest extends BaseAiTest
{
    private String url;

    /**
     * Set up storefront url parameter before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        this.url = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Neodymium.getData().put("javaMethod.test.url", this.url);
    }

    /**
     * Split comma-separated arguments by the last comma to avoid splitting localized numbers.
     *
     * @param args the raw arguments string
     * @return an array containing the input and expected values
     */
    private static String[] parseArgs(final String args)
    {
        final int lastComma = args.lastIndexOf(',');
        if (lastComma < 0)
        {
            throw new IllegalArgumentException("Arguments must be comma-separated: " + args);
        }
        return new String[]
        {
            args.substring(0, lastComma).trim(),
            args.substring(lastComma + 1).trim()
        };
    }

    /**
     * Local test helper to verify parseLocalizedBigDecimal.
     *
     * @param args the input and expected values
     */
    public static void assertParsedBigDecimal(final String args)
    {
        final String[] parsed = parseArgs(args);
        final String input = parsed[0];
        final String expected = parsed[1];
        final BigDecimal result = AiAssertions.parseLocalizedBigDecimal(input);
        assertEquals(new BigDecimal(expected), result);
    }

    /**
     * Local test helper to verify detectDisplayPrecision.
     *
     * @param args the input and expected values
     */
    public static void assertDisplayPrecision(final String args)
    {
        final String[] parsed = parseArgs(args);
        final String input = parsed[0];
        final int expected = Integer.parseInt(parsed[1]);
        final int result = AiAssertions.detectDisplayPrecision(input);
        assertEquals(expected, result);
    }

    /**
     * Local test helper to verify isNumericOrPrice.
     *
     * @param args the input and expected values
     */
    public static void assertIsNumericOrPrice(final String args)
    {
        final String[] parsed = parseArgs(args);
        final String input = parsed[0];
        final boolean expected = Boolean.parseBoolean(parsed[1]);
        final boolean result = AiAssertions.isNumericOrPrice(input);
        assertEquals(expected, result);
    }

    /**
     * Local test helper to verify normalizeNumericOrPrice.
     *
     * @param args the input and expected values
     */
    public static void assertNormalizeNumericOrPrice(final String args)
    {
        final String[] parsed = parseArgs(args);
        final String input = parsed[0];
        final String expected = parsed[1];
        final String result = AiAssertions.normalizeNumericOrPrice(input);
        assertEquals(expected, result);
    }

    /**
     * Helper to assert execution throws a specific cause.
     */
    private void assertExecutionThrows(final String steps, final Class<? extends Throwable> expectedCause)
    {
        final Throwable thrown = Assertions.assertThrows(
            Throwable.class,
            () -> runAi(steps, VerificationMode.LIVE_LLM)
        );

        Throwable cause = thrown;
        boolean found = false;
        while (cause != null)
        {
            if (expectedCause.isInstance(cause))
            {
                found = true;
                break;
            }
            cause = cause.getCause();
        }
        assertTrue(found, "Expected " + expectedCause.getSimpleName() + " in cause chain of: " + thrown);
    }

    /**
     * Verify direct parseLocalizedBigDecimal invocation via java: prefix.
     */
    @NeodymiumTest
    public final void testDirectParseLocalizedBigDecimal()
    {
        // de-DE locale by default for this test
        Neodymium.getData().put("locale", "de-DE");

        final String successSteps = """
                OPEN ${javaMethod.test.url}
                java: parseLocalizedBigDecimal("14,96 €")
            """;

        final AiExecutionResult r1 = runAi(successSteps, VerificationMode.LIVE_LLM);
        assertThat(r1)
            .hasDirectParses(2)
            .hasActionsCount(2);

        final String failingSteps = """
                OPEN ${javaMethod.test.url}
                java: parseLocalizedBigDecimal("abc")
            """;

        assertExecutionThrows(failingSteps, NumberFormatException.class);
    }

    /**
     * Verify correct results of all non-assertion methods under integration execution.
     */
    @NeodymiumTest
    public final void testJavaNonAssertionMethodsCorrectness()
    {
        final String steps = """
                OPEN ${javaMethod.test.url}
                java: assertParsedBigDecimal("14,96 €, 14.96")
                java: assertDisplayPrecision("$0.90, 2")
                java: assertIsNumericOrPrice("zł 150, true")
                java: assertIsNumericOrPrice("Total, false")
                java: assertNormalizeNumericOrPrice("14,96 €, 14.96")
                java: assertNormalizeNumericOrPrice("ORD-12345, ORD-12345")
            """;

        final AiExecutionResult r = runAi(steps, VerificationMode.LIVE_LLM);
        assertThat(r)
            .hasDirectParses(7)
            .hasActionsCount(7);
    }

    /**
     * Verify locale-aware parsing when Neodymium locale changes.
     */
    @NeodymiumTest
    public final void testLocaleAwareBigDecimalParsing()
    {
        // Test German locale (comma decimal separator)
        Neodymium.getData().put("locale", "de-DE");
        final String germanSteps = """
                OPEN ${javaMethod.test.url}
                java: assertParsedBigDecimal("1.234,56, 1234.56")
            """;
        runAi(germanSteps, VerificationMode.LIVE_LLM);

        this.resetBrowser();

        // Test US locale (dot decimal separator)
        Neodymium.getData().put("locale", "en-US");
        final String usSteps = """
                OPEN ${javaMethod.test.url}
                java: assertParsedBigDecimal("1,234.56, 1234.56")
            """;
        runAi(usSteps, VerificationMode.LIVE_LLM);
    }
}
