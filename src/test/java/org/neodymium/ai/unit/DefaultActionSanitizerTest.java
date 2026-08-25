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
package org.neodymium.ai.unit;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.prompt.DefaultActionSanitizer;

/**
 * Unit tests for {@link DefaultActionSanitizer} verifying environment variable and
 * system property parameterization of action targets/values.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public class DefaultActionSanitizerTest
{
    @Test
    public void testSanitizeActionWithSystemProperty()
    {
        final String sysKey = "verla.test.url";
        final String sysValue = "http://localhost:8542";
        System.setProperty(sysKey, sysValue);

        try
        {
            final SessionData sessionData = new SessionData();
            final DefaultActionSanitizer sanitizer = new DefaultActionSanitizer();

            final Action rawAction = new Action(
                "NAVIGATE",
                sysValue + "/verla-perfect/index.html",
                List.of(sysValue + "/verla-perfect/index.html"),
                "Navigate to " + sysValue,
                "Reasoning"
            );

            final Action sanitized = sanitizer.sanitize(rawAction, sessionData);

            Assertions.assertEquals("${verla.test.url}/verla-perfect/index.html", sanitized.getTarget());
            Assertions.assertEquals("${verla.test.url}/verla-perfect/index.html", sanitized.getValues().get(0));
            Assertions.assertEquals("Navigate to ${verla.test.url}", sanitized.getDescription());
        }
        finally
        {
            System.clearProperty(sysKey);
        }
    }

    @Test
    public void testSanitizeActionWithStaticDataset()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("quality", "perfect", false);

        final DefaultActionSanitizer sanitizer = new DefaultActionSanitizer();

        final Action rawAction = new Action(
            "NAVIGATE",
            "http://localhost/verla-perfect/index.html",
            Collections.emptyList(),
            "Open perfect page",
            "Reasoning"
        );

        final Action sanitized = sanitizer.sanitize(rawAction, sessionData);

        Assertions.assertEquals("http://localhost/verla-${quality}/index.html", sanitized.getTarget());
    }

    @Test
    public void testSanitizeActionWithStoreActionVariable()
    {
        final SessionData sessionData = new SessionData();
        // Simulate STORE action capturing dynamic order number
        sessionData.putDynamic("orderId", "ORD-12345", false);

        final DefaultActionSanitizer sanitizer = new DefaultActionSanitizer();

        final Action rawAction = new Action(
            "ASSERT_TEXT",
            "#confirmation-msg",
            List.of("Thank you for your order ORD-12345!"),
            "Verify order ORD-12345",
            "Reasoning"
        );

        final Action sanitized = sanitizer.sanitize(rawAction, sessionData);

        Assertions.assertEquals("Thank you for your order ${orderId}!", sanitized.getValues().get(0));
        Assertions.assertEquals("Verify order ${orderId}", sanitized.getDescription());
    }

    @Test
    public void testSanitizeActionDoesNotCorruptShortSelectorWithGeneralVariable()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("qty", "1", false);
        sessionData.putDynamic("password", "MySecretPassword123!", true);

        final DefaultActionSanitizer sanitizer = new DefaultActionSanitizer();

        final Action rawAction = new Action(
            "TYPE",
            "#item1-input",
            List.of("MySecretPassword123!"),
            "Type password into #item1-input",
            "Reasoning"
        );

        final Action sanitized = sanitizer.sanitize(rawAction, sessionData);

        Assertions.assertEquals("#item1-input", sanitized.getTarget(), "Target selector #item1-input should NOT be corrupted into #item${qty}-input.");
        Assertions.assertEquals("${password}", sanitized.getValues().get(0), "Sensitive value should be parameterized into ${password}.");
    }

    @Test
    public void testSanitizeTextWithCompoundVariablesAndDynamicSeed()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("random", "95089506", false);
        sessionData.putDynamic("email", "john.doe.us.${random}@example.com", false);
        sessionData.putDynamic("password", "Password123!", true);

        final DefaultActionSanitizer sanitizer = new DefaultActionSanitizer();

        final String rawInstruction = "Type \"john.doe.us.95089506@example.com\" into the email address field";
        final String sanitized = sanitizer.sanitizeText(rawInstruction, sessionData);

        Assertions.assertEquals("Type \"${email}\" into the email address field", sanitized);

        final String rawCompound = "Type \"john.doe.us.95089506@example.com\" into the email address field, \"Password123!\" into the password field and \"Password123!\" into the confirm password field.";
        final String sanitizedCompound = sanitizer.sanitizeText(rawCompound, sessionData);

        Assertions.assertEquals("Type \"${email}\" into the email address field, \"${password}\" into the password field and \"${password}\" into the confirm password field.", sanitizedCompound);
    }

    @Test
    public void testSanitizeActionStepInstruction()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("random", "95089506", false);
        sessionData.putDynamic("email", "john.doe.us.${random}@example.com", false);

        final DefaultActionSanitizer sanitizer = new DefaultActionSanitizer();

        final Action rawAction = new Action(
            "TYPE",
            "#email",
            List.of("john.doe.us.95089506@example.com"),
            "Type email into email input",
            "Reasoning"
        );
        rawAction.setStepInstruction("Type \"john.doe.us.95089506@example.com\" into the email address field");

        final Action sanitized = sanitizer.sanitize(rawAction, sessionData);

        Assertions.assertEquals("${email}", sanitized.getValues().get(0));
        Assertions.assertEquals("Type \"${email}\" into the email address field", sanitized.getStepInstruction());
    }

    @Test
    public void testSanitizeTextIgnoresInternalFrameworkProperties()
    {
        final String sysPropKey = "neodymium.junit.viewmode";
        final String sysPropVal = "headless";
        System.setProperty(sysPropKey, sysPropVal);

        try
        {
            final SessionData sessionData = new SessionData();
            final DefaultActionSanitizer sanitizer = new DefaultActionSanitizer();

            final String text = "Running browser in headless mode";
            final String sanitized = sanitizer.sanitizeText(text, sessionData);

            Assertions.assertEquals("Running browser in headless mode", sanitized);
        }
        finally
        {
            System.clearProperty(sysPropKey);
        }
    }
}
