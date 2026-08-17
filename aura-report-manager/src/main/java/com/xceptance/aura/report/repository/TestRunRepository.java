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

import com.xceptance.aura.report.entity.TestRunEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for {@link TestRunEntity}.
 *
 * @author Xceptance GmbH 2026
 */
@Repository
public interface TestRunRepository extends JpaRepository<TestRunEntity, String>
{
    List<TestRunEntity> findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc(String batchName);

    List<TestRunEntity> findByIsDeletedFalseOrderByStartTimeMsDesc();

    Optional<TestRunEntity> findByIdAndIsDeletedFalse(String id);
}
