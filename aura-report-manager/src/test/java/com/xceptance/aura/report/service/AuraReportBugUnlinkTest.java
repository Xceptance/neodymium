/*
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.xceptance.aura.report.service;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestRunRepository;

/**
 * Tests linking and unlinking bug tickets from test executions and verifying status updates and persistence.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class AuraReportBugUnlinkTest
{
    @Autowired
    private AuraReportDataService dataService;

    @Autowired
    private TestRunRepository runRepository;

    @Autowired
    private TestBaseBugRepository bugRepository;

    private static final String TEST_RUN_ID = "run-unlink-test-101";

    @BeforeEach
    public void setUp()
    {
        bugRepository.deleteAll();

        final TestRunEntity runEntity = new TestRunEntity(
            TEST_RUN_ID,
            "Unlink Test Batch",
            "COMPLETED",
            "Regression batch for bug unlinking",
            "US-West",
            "en_US",
            "Chrome",
            "2026-08-17 12:00:00",
            System.currentTimeMillis()
        );
        runEntity.setTotalTests(1);
        runEntity.setFailedUnknownCount(1);
        runEntity.setFailedKnownCount(0);
        runEntity.setPassedCount(0);
        runEntity.setSucceededFixedCount(0);
        runEntity.setIgnoredCount(0);
        runEntity.setPassRate(0.0);
        runRepository.save(runEntity);

        final TestExecutionDto exec = new TestExecutionDto(
            "row-unlink-1",
            TEST_RUN_ID,
            "com.xceptance.neodymium.test.CheckoutTest",
            "testCheckoutProcess",
            "com.xceptance.neodymium.test.CheckoutTest testCheckoutProcess",
            "1.0s",
            "2026-08-17 12:00:00",
            "failed",
            "JUnit5",
            "US-West",
            "Chrome",
            "AssertionError: Element not found",
            List.of(),
            null,
            "Checkout",
            List.of(),
            null,
            null,
            null,
            null
        );

        final RunReportDto report = new RunReportDto(
            TEST_RUN_ID,
            "Unlink Test Batch",
            "2026-08-17 12:00:00",
            "1.0s",
            1,
            0,
            0,
            0,
            1,
            0,
            List.of(exec)
        );

        dataService.saveRunReportToDisk(TEST_RUN_ID, report);
    }

    @Test
    public void testLinkAndUnlinkBugTicket()
    {
        // 1. Initial State: 1 failed-unknown execution
        final RunReportDto initialReport = dataService.getRunReport(TEST_RUN_ID);
        Assertions.assertEquals(1, initialReport.getUnknownCount());
        Assertions.assertEquals(0, initialReport.getKnownCount());
        Assertions.assertEquals("failed-unknown", initialReport.getExecutions().get(0).getStatus());

        // 2. Link Bug Ticket: BUG-9999
        final TestExecutionDto linkedExec = dataService.addBugToExecution(TEST_RUN_ID, "row-unlink-1", "BUG-9999");
        Assertions.assertTrue(linkedExec.getBugs().contains("BUG-9999"));
        Assertions.assertEquals("failed-known", linkedExec.getStatus());

        final RunReportDto reportAfterLink = dataService.getRunReport(TEST_RUN_ID);
        Assertions.assertEquals(0, reportAfterLink.getUnknownCount());
        Assertions.assertEquals(1, reportAfterLink.getKnownCount());

        // 3. Unlink Bug Ticket: BUG-9999
        final TestExecutionDto unlinkedExec = dataService.removeBugFromExecution(TEST_RUN_ID, "row-unlink-1", "BUG-9999");
        Assertions.assertFalse(unlinkedExec.getBugs().contains("BUG-9999"));
        Assertions.assertEquals("failed-unknown", unlinkedExec.getStatus());

        final RunReportDto reportAfterUnlink = dataService.getRunReport(TEST_RUN_ID);
        Assertions.assertEquals(1, reportAfterUnlink.getUnknownCount());
        Assertions.assertEquals(0, reportAfterUnlink.getKnownCount());
    }
}
