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
package com.xceptance.aura.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * JPA entity representing a persistent bug assignment linked to a Test Base variation and environment.
 *
 * @author Xceptance GmbH 2026
 */
@Entity
@Table(name = "test_base_bug", indexes = {
    @Index(name = "idx_tbb_variation_id", columnList = "variation_id"),
    @Index(name = "idx_tbb_bug_ticket", columnList = "bug_ticket"),
    @Index(name = "idx_tbb_var_env", columnList = "variation_id, environment")
})
public class TestBaseBugEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "variation_id", nullable = false, length = 120)
    private String variationId;

    @Column(name = "bug_ticket", nullable = false, length = 80)
    private String bugTicket;

    @Column(name = "environment", nullable = false, length = 80)
    private String environment = "ALL";

    @Column(name = "created_at_ms")
    private Long createdAtMs;

    @Column(name = "removed_at_ms")
    private Long removedAtMs;

    @Column(name = "linked_run_id", length = 80)
    private String linkedRunId;

    @Column(name = "removed_run_id", length = 80)
    private String removedRunId;

    public TestBaseBugEntity()
    {
    }

    public TestBaseBugEntity(final String variationId, final String bugTicket, final Long createdAtMs)
    {
        this(variationId, bugTicket, "ALL", createdAtMs, null);
    }

    public TestBaseBugEntity(final String variationId, final String bugTicket, final String environment, final Long createdAtMs)
    {
        this(variationId, bugTicket, environment, createdAtMs, null);
    }

    public TestBaseBugEntity(final String variationId, final String bugTicket, final String environment, final Long createdAtMs, final String linkedRunId)
    {
        this.variationId = variationId;
        this.bugTicket = bugTicket;
        this.environment = (environment != null && !environment.trim().isEmpty()) ? environment.trim() : "ALL";
        this.createdAtMs = createdAtMs;
        this.linkedRunId = linkedRunId;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(final Long id)
    {
        this.id = id;
    }

    public String getVariationId()
    {
        return variationId;
    }

    public void setVariationId(final String variationId)
    {
        this.variationId = variationId;
    }

    public String getBugTicket()
    {
        return bugTicket;
    }

    public void setBugTicket(final String bugTicket)
    {
        this.bugTicket = bugTicket;
    }

    public String getEnvironment()
    {
        return environment;
    }

    public void setEnvironment(final String environment)
    {
        this.environment = (environment != null && !environment.trim().isEmpty()) ? environment.trim() : "ALL";
    }

    public Long getCreatedAtMs()
    {
        return createdAtMs;
    }

    public void setCreatedAtMs(final Long createdAtMs)
    {
        this.createdAtMs = createdAtMs;
    }

    public Long getRemovedAtMs()
    {
        return removedAtMs;
    }

    public void setRemovedAtMs(final Long removedAtMs)
    {
        this.removedAtMs = removedAtMs;
    }

    public String getLinkedRunId()
    {
        return linkedRunId;
    }

    public void setLinkedRunId(final String linkedRunId)
    {
        this.linkedRunId = linkedRunId;
    }

    public String getRemovedRunId()
    {
        return removedRunId;
    }

    public void setRemovedRunId(final String removedRunId)
    {
        this.removedRunId = removedRunId;
    }
}
