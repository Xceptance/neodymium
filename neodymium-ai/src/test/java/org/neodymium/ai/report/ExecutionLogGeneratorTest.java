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
package org.neodymium.ai.report;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ExecutionLogGenerator}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class ExecutionLogGeneratorTest
{
    @Test
    public void testGenerateWithNullReport()
    {
        final ExecutionLogGenerator generator = new ExecutionLogGenerator();
        final String log = generator.generate(null);
        assertNotNull(log);
        assertTrue(log.contains("No execution report data available"));
    }

    @Test
    public void testGenerateSuccessfulReport()
    {
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestName("Checkout Verification");
        report.setTestClass("org.example.CheckoutTest");
        report.setTestMethod("testCheckout");
        report.setSuccess(true);
        report.setDurationMs(4500);

        final TestExecutionReport.ReportStepEntry step = new TestExecutionReport.ReportStepEntry();
        step.setStepIndex(1);
        step.setInstruction("Navigate to checkout");
        step.setStatus("SUCCESS");
        step.setDurationMs(1200);

        final TestExecutionReport.ReportActionEntry action = new TestExecutionReport.ReportActionEntry();
        action.setType("NAVIGATION");
        action.setDescription("Open checkout page");
        action.setSuccess(true);
        step.addAction(action);

        report.addStep(step);

        final ExecutionLogGenerator generator = new ExecutionLogGenerator();
        final String log = generator.generate(report);

        assertNotNull(log);
        assertTrue(log.contains("Neodymium AI Test Execution Log: Checkout Verification"));
        assertTrue(log.contains("Test Class:       org.example.CheckoutTest"));
        assertTrue(log.contains("Test Method:      testCheckout"));
        assertTrue(log.contains("Status:           PASSED"));
        assertTrue(log.contains("[Step 01] Navigate to checkout"));
        assertTrue(log.contains("NAVIGATION: Open checkout page -> SUCCESS"));
    }

    @Test
    public void testGenerateFailedReportWithFailureDetails()
    {
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestName("Payment Flow");
        report.setTestClass("org.example.PaymentTest");
        report.setTestMethod("testCreditCardPayment");
        report.setSuccess(false);
        report.setFailureReason("Element not found: #submit-order");
        report.setFailureStackTrace("org.openqa.selenium.NoSuchElementException: Cannot locate #submit-order\n\tat org.example.PaymentTest.testCreditCardPayment(PaymentTest.java:42)");

        final ExecutionLogGenerator generator = new ExecutionLogGenerator();
        final String log = generator.generate(report);

        assertNotNull(log);
        assertTrue(log.contains("Status:           FAILED"));
        assertTrue(log.contains("--- FAILURE DETAILS ---"));
        assertTrue(log.contains("Element not found: #submit-order"));
        assertTrue(log.contains("NoSuchElementException"));
    }
}
