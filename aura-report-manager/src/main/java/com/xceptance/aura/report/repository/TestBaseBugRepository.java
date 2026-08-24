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

import com.xceptance.aura.report.entity.TestBaseBugEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Spring Data JPA Repository for {@link TestBaseBugEntity}.
 *
 * @author Xceptance GmbH 2026
 */
@Repository
public interface TestBaseBugRepository extends JpaRepository<TestBaseBugEntity, Long>
{
    List<TestBaseBugEntity> findByVariationId(String variationId);

    List<TestBaseBugEntity> findByVariationIdAndEnvironmentIn(String variationId, Collection<String> environments);

    List<TestBaseBugEntity> findByVariationIdAndBatchNameInAndEnvironmentIn(String variationId, Collection<String> batchNames, Collection<String> environments);

    List<TestBaseBugEntity> findByEnvironmentIn(Collection<String> environments);

    List<TestBaseBugEntity> findByBatchNameInAndEnvironmentIn(Collection<String> batchNames, Collection<String> environments);

    @Transactional
    void deleteByVariationIdAndBugTicket(String variationId, String bugTicket);

    @Transactional
    void deleteByVariationIdAndBugTicketAndEnvironment(String variationId, String bugTicket, String environment);
}
