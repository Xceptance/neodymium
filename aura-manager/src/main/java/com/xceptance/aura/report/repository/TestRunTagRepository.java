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
import com.xceptance.aura.report.entity.TestRunTagId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JPA Repository for {@link TestRunTagEntity}.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Repository
public interface TestRunTagRepository extends JpaRepository<TestRunTagEntity, TestRunTagId>
{
    List<TestRunTagEntity> findByRunId(String runId);

    List<TestRunTagEntity> findByRunIdIn(Collection<String> runIds);

    List<TestRunTagEntity> findByTagKeyAndTagValue(String tagKey, String tagValue);

    @Query("SELECT DISTINCT t.tagKey FROM TestRunTagEntity t ORDER BY t.tagKey ASC")
    List<String> findDistinctTagKeys();

    @Query("SELECT DISTINCT t.tagValue FROM TestRunTagEntity t WHERE t.tagKey = :tagKey ORDER BY t.tagValue ASC")
    List<String> findDistinctTagValuesByTagKey(@Param("tagKey") String tagKey);

    @Query("SELECT t.runId FROM TestRunTagEntity t WHERE CONCAT(t.tagKey, ':::', t.tagValue) IN :compositeTags GROUP BY t.runId HAVING COUNT(DISTINCT t.tagKey) = :tagCount")
    List<String> findRunIdsMatchingAllTags(@Param("compositeTags") Collection<String> compositeTags, @Param("tagCount") long tagCount);

    @Transactional
    void deleteByRunId(String runId);
}
