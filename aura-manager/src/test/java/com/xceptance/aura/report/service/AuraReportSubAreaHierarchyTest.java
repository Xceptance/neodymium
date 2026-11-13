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
package com.xceptance.aura.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xceptance.aura.report.dto.AreaSummaryDto;
import com.xceptance.aura.report.dto.SubAreaSummaryDto;
import com.xceptance.aura.report.dto.TestBaseAreaDto;
import com.xceptance.aura.report.dto.TestBaseClassDto;
import com.xceptance.aura.report.dto.TestBaseSubAreaDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.entity.TestBaseVariationEntity;

/**
 * Integration tests verifying sub-area hierarchy rollup in {@link AuraReportDataService}.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class AuraReportSubAreaHierarchyTest
{
    @Autowired
    private AuraReportDataService dataService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testBuildAreaSummariesWithSubAreas() throws Exception
    {
        final String json1 = """
            {
                "id": "exec-search-browsing",
                "testClass": "com.xceptance.SearchTest",
                "testMethod": "testKeywordSearch",
                "areaName": "@Search",
                "subArea": "@Browsing",
                "status": "passed"
            }
            """;
        final String json2 = """
            {
                "id": "exec-search-filtering",
                "testClass": "com.xceptance.SearchTest",
                "testMethod": "testFacetFilter",
                "areaName": "@Search",
                "subArea": "@Filtering",
                "status": "succeeded-fixed"
            }
            """;
        final String json3 = """
            {
                "id": "exec-account-profile",
                "testClass": "com.xceptance.AccountTest",
                "testMethod": "testViewProfile",
                "areaName": "@Account",
                "status": "passed"
            }
            """;

        final TestExecutionDto exec1 = objectMapper.readValue(json1, TestExecutionDto.class);
        final TestExecutionDto exec2 = objectMapper.readValue(json2, TestExecutionDto.class);
        final TestExecutionDto exec3 = objectMapper.readValue(json3, TestExecutionDto.class);

        final List<AreaSummaryDto> areaSummaries = dataService.buildAreaSummaries(List.of(exec1, exec2, exec3));
        assertNotNull(areaSummaries);
        assertEquals(2, areaSummaries.size());

        final AreaSummaryDto searchArea = areaSummaries.stream()
            .filter(a -> "@Search".equals(a.getAreaName()))
            .findFirst()
            .orElseThrow();

        assertTrue(searchArea.hasSubAreas(), "Search area with multiple subAreas must report hasSubAreas() == true");
        assertEquals(2, searchArea.getSubAreas().size());

        final SubAreaSummaryDto browsingSub = searchArea.getSubAreas().stream()
            .filter(s -> "@Browsing".equals(s.getSubAreaName()))
            .findFirst()
            .orElseThrow();
        assertEquals(1, browsingSub.getTotalCount());
        assertEquals(1, browsingSub.getPassCount());
        assertEquals(0, browsingSub.getFixedCount());

        final SubAreaSummaryDto filteringSub = searchArea.getSubAreas().stream()
            .filter(s -> "@Filtering".equals(s.getSubAreaName()))
            .findFirst()
            .orElseThrow();
        assertEquals(1, filteringSub.getTotalCount());
        assertEquals(0, filteringSub.getPassCount());
        assertEquals(1, filteringSub.getFixedCount());

        // Backward compatibility: all test classes must be available at area level
        assertEquals(1, searchArea.getTestClasses().size());
        assertEquals("com.xceptance.SearchTest", searchArea.getTestClasses().get(0).getClassName());

        final AreaSummaryDto accountArea = areaSummaries.stream()
            .filter(a -> "@Account".equals(a.getAreaName()))
            .findFirst()
            .orElseThrow();
        assertFalse(accountArea.hasSubAreas(), "Area without sub-areas must report hasSubAreas() == false");
        assertTrue(accountArea.getSubAreas().isEmpty());
    }

    @Test
    public void testGroupBatchTestClassesByAreaWithSubAreas()
    {
        final TestBaseVariationEntity var1 = new TestBaseVariationEntity(
            "var-1",
            "com.xceptance.SearchTest",
            "testKeywordSearch",
            "Default",
            "@Search",
            "US",
            "Chrome"
        );
        var1.setSubAreaTag("@Browsing");
        var1.setLastStatus("passed");

        final TestBaseVariationEntity var2 = new TestBaseVariationEntity(
            "var-2",
            "com.xceptance.SearchTest",
            "testFacetFilter",
            "Default",
            "@Search",
            "US",
            "Chrome"
        );
        var2.setSubAreaTag("@Filtering");
        var2.setLastStatus("passed");

        final TestBaseVariationEntity var3 = new TestBaseVariationEntity(
            "var-3",
            "com.xceptance.AccountTest",
            "testViewProfile",
            "Default",
            "@Account",
            "US",
            "Chrome"
        );
        var3.setLastStatus("passed");

        final TestBaseClassDto searchClassDto = new TestBaseClassDto(
            "com.xceptance.SearchTest",
            "container-search",
            2,
            List.of(var1, var2)
        );

        final TestBaseClassDto accountClassDto = new TestBaseClassDto(
            "com.xceptance.AccountTest",
            "container-account",
            1,
            List.of(var3)
        );

        final List<TestBaseAreaDto> batchAreas = dataService.groupBatchTestClassesByArea(List.of(searchClassDto, accountClassDto));
        assertNotNull(batchAreas);
        assertEquals(2, batchAreas.size());

        final TestBaseAreaDto searchArea = batchAreas.stream()
            .filter(a -> "@Search".equals(a.getAreaName()))
            .findFirst()
            .orElseThrow();

        assertTrue(searchArea.hasSubAreas());
        assertEquals(2, searchArea.getSubAreas().size());

        final TestBaseSubAreaDto browsingSub = searchArea.getSubAreas().stream()
            .filter(s -> "@Browsing".equals(s.getSubAreaName()))
            .findFirst()
            .orElseThrow();
        assertEquals(1, browsingSub.getClassCount());
        assertEquals(1, browsingSub.getTotalVariationsCount());

        final TestBaseSubAreaDto filteringSub = searchArea.getSubAreas().stream()
            .filter(s -> "@Filtering".equals(s.getSubAreaName()))
            .findFirst()
            .orElseThrow();
        assertEquals(1, filteringSub.getClassCount());
        assertEquals(1, filteringSub.getTotalVariationsCount());

        final TestBaseAreaDto accountArea = batchAreas.stream()
            .filter(a -> "@Account".equals(a.getAreaName()))
            .findFirst()
            .orElseThrow();
        assertFalse(accountArea.hasSubAreas());
        assertTrue(accountArea.getSubAreas().isEmpty());
    }
}
