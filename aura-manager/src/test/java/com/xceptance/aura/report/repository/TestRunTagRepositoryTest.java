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
package com.xceptance.aura.report.repository;

import com.xceptance.aura.report.entity.TestRunTagEntity;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests verifying tag persistence, key-value lookup, and multi-tag intersection queries in {@link TestRunTagRepository}.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class TestRunTagRepositoryTest
{
    @Autowired
    private TestRunTagRepository tagRepository;

    @BeforeEach
    public void setUp()
    {
        tagRepository.deleteAll();
    }

    @Test
    public void testSaveAndFindByRunId()
    {
        final String runId = "run_test_01";
        tagRepository.save(new TestRunTagEntity(runId, "Instance", "Staging"));
        tagRepository.save(new TestRunTagEntity(runId, "Locale", "NA"));
        tagRepository.save(new TestRunTagEntity(runId, "Suite", "Smoke"));

        final List<TestRunTagEntity> tags = tagRepository.findByRunId(runId);
        Assertions.assertEquals(3, tags.size());
    }

    @Test
    public void testFindDistinctKeysAndValues()
    {
        tagRepository.save(new TestRunTagEntity("r1", "Instance", "Staging"));
        tagRepository.save(new TestRunTagEntity("r2", "Instance", "Prod"));
        tagRepository.save(new TestRunTagEntity("r3", "Locale", "NA"));

        final List<String> distinctKeys = tagRepository.findDistinctTagKeys();
        Assertions.assertEquals(2, distinctKeys.size());
        Assertions.assertTrue(distinctKeys.contains("Instance"));
        Assertions.assertTrue(distinctKeys.contains("Locale"));

        final List<String> instanceValues = tagRepository.findDistinctTagValuesByTagKey("Instance");
        Assertions.assertEquals(2, instanceValues.size());
        Assertions.assertTrue(instanceValues.contains("Staging"));
        Assertions.assertTrue(instanceValues.contains("Prod"));
    }

    @Test
    public void testMultiTagIntersectionQuery()
    {
        // Run 1: Staging + NA + Smoke
        tagRepository.save(new TestRunTagEntity("run_1", "Instance", "Staging"));
        tagRepository.save(new TestRunTagEntity("run_1", "Locale", "NA"));
        tagRepository.save(new TestRunTagEntity("run_1", "Suite", "Smoke"));

        // Run 2: Staging + EU + Smoke
        tagRepository.save(new TestRunTagEntity("run_2", "Instance", "Staging"));
        tagRepository.save(new TestRunTagEntity("run_2", "Locale", "EU"));
        tagRepository.save(new TestRunTagEntity("run_2", "Suite", "Smoke"));

        // Run 3: Prod + NA + Regression
        tagRepository.save(new TestRunTagEntity("run_3", "Instance", "Prod"));
        tagRepository.save(new TestRunTagEntity("run_3", "Locale", "NA"));
        tagRepository.save(new TestRunTagEntity("run_3", "Suite", "Regression"));

        // Query 1: Instance=Staging AND Locale=NA -> exactly run_1
        final List<String> stagingNa = tagRepository.findRunIdsMatchingAllTags(
            List.of("Instance:::Staging", "Locale:::NA"), 2L
        );
        Assertions.assertEquals(1, stagingNa.size());
        Assertions.assertEquals("run_1", stagingNa.get(0));

        // Query 2: Suite=Smoke -> run_1 and run_2
        final List<String> smokeRuns = tagRepository.findRunIdsMatchingAllTags(
            List.of("Suite:::Smoke"), 1L
        );
        Assertions.assertEquals(2, smokeRuns.size());
        Assertions.assertTrue(smokeRuns.contains("run_1"));
        Assertions.assertTrue(smokeRuns.contains("run_2"));

        // Query 3: Instance=Staging AND Suite=Smoke -> run_1 and run_2
        final List<String> stagingSmoke = tagRepository.findRunIdsMatchingAllTags(
            List.of("Instance:::Staging", "Suite:::Smoke"), 2L
        );
        Assertions.assertEquals(2, stagingSmoke.size());
        Assertions.assertTrue(stagingSmoke.contains("run_1"));
        Assertions.assertTrue(stagingSmoke.contains("run_2"));

        // Query 4: Instance=Prod AND Suite=Smoke -> empty
        final List<String> prodSmoke = tagRepository.findRunIdsMatchingAllTags(
            List.of("Instance:::Prod", "Suite:::Smoke"), 2L
        );
        Assertions.assertTrue(prodSmoke.isEmpty());
    }

    @Test
    public void testDeleteByRunId()
    {
        final String runId = "run_to_delete";
        tagRepository.save(new TestRunTagEntity(runId, "Branch", "develop"));
        Assertions.assertEquals(1, tagRepository.findByRunId(runId).size());

        tagRepository.deleteByRunId(runId);
        Assertions.assertTrue(tagRepository.findByRunId(runId).isEmpty());
    }
}
