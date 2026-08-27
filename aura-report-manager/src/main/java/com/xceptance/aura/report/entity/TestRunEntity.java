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
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * JPA entity representing a single Test Execution Run instance.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Entity
@Table(name = "test_run", indexes = {
    @Index(name = "idx_tr_batch_deleted", columnList = "batch_name, is_deleted"),
    @Index(name = "idx_tr_start_time", columnList = "start_time_ms")
})
public class TestRunEntity
{
    @Id
    @Column(name = "id", nullable = false, length = 80)
    private String id;

    @Column(name = "batch_name", nullable = false, length = 255)
    private String batchName;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "trigger_source", length = 255)
    private String triggerSource;

    @Column(name = "environment", length = 255)
    private String environment;

    @Column(name = "locales_csv", length = 500)
    private String localesCsv;

    @Column(name = "browsers_csv", length = 500)
    private String browsersCsv;

    @Column(name = "timestamp_label", length = 255)
    private String timestampLabel;

    @Column(name = "start_time_ms")
    private Long startTimeMs;

    @Column(name = "end_time_ms")
    private Long endTimeMs;

    @Column(name = "total_tests")
    private Integer totalTests = 0;

    @Column(name = "passed_count")
    private Integer passedCount = 0;

    @Column(name = "succeeded_fixed_count")
    private Integer succeededFixedCount = 0;

    @Column(name = "failed_known_count")
    private Integer failedKnownCount = 0;

    @Column(name = "failed_unknown_count")
    private Integer failedUnknownCount = 0;

    @Column(name = "ignored_count")
    private Integer ignoredCount = 0;

    @Column(name = "pass_rate")
    private Double passRate = 0.0;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "run_comment", length = 1000)
    private String runComment;

    @Column(name = "run_json_path", length = 500)
    private String runJsonPath;

    @Column(name = "attachments_synced_to_s3")
    private Boolean attachmentsSyncedToS3 = false;

    @Column(name = "duration_ms")
    private Long durationMs = 0L;

    @Column(name = "formatted_duration", length = 100)
    private String formattedDuration = "0 ms";

    @Column(name = "total_llm_calls")
    private Integer totalLlmCalls = 0;

    @Column(name = "total_llm_tokens")
    private Long totalLlmTokens = 0L;

    @Column(name = "total_llm_cost")
    private Double totalLlmCost = 0.0;

    public TestRunEntity()
    {
    }

    public TestRunEntity(final String id, final String batchName, final String status, final String triggerSource, final String environment, final String localesCsv, final String browsersCsv, final String timestampLabel, final Long startTimeMs)
    {
        this.id = id;
        this.batchName = batchName;
        this.status = status;
        this.triggerSource = triggerSource;
        this.environment = environment;
        this.localesCsv = localesCsv;
        this.browsersCsv = browsersCsv;
        this.timestampLabel = timestampLabel;
        this.startTimeMs = startTimeMs;
    }

    public String getId()
    {
        return id;
    }

    public void setId(final String id)
    {
        this.id = id;
    }

    public String getBatchName()
    {
        return batchName;
    }

    public void setBatchName(final String batchName)
    {
        this.batchName = batchName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(final String status)
    {
        this.status = status;
    }

    public String getTriggerSource()
    {
        return triggerSource;
    }

    public void setTriggerSource(final String triggerSource)
    {
        this.triggerSource = triggerSource;
    }

    public String getEnvironment()
    {
        return environment;
    }

    public void setEnvironment(final String environment)
    {
        this.environment = environment;
    }

    public String getLocalesCsv()
    {
        return localesCsv;
    }

    public void setLocalesCsv(final String localesCsv)
    {
        this.localesCsv = localesCsv;
    }

    public String getBrowsersCsv()
    {
        return browsersCsv;
    }

    public void setBrowsersCsv(final String browsersCsv)
    {
        this.browsersCsv = browsersCsv;
    }

    public String getTimestampLabel()
    {
        if (startTimeMs != null && startTimeMs > 0L)
        {
            final java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(startTimeMs),
                java.time.ZoneId.systemDefault()
            );
            return ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return timestampLabel != null ? timestampLabel : "Recently";
    }

    public void setTimestampLabel(final String timestampLabel)
    {
        this.timestampLabel = timestampLabel;
    }

    public Long getStartTimeMs()
    {
        return startTimeMs;
    }

    public void setStartTimeMs(final Long startTimeMs)
    {
        this.startTimeMs = startTimeMs;
    }

    public Long getEndTimeMs()
    {
        return endTimeMs;
    }

    public void setEndTimeMs(final Long endTimeMs)
    {
        this.endTimeMs = endTimeMs;
    }

    public Integer getTotalTests()
    {
        return totalTests;
    }

    public void setTotalTests(final Integer totalTests)
    {
        this.totalTests = totalTests;
    }

    public Integer getPassedCount()
    {
        return passedCount;
    }

    public void setPassedCount(final Integer passedCount)
    {
        this.passedCount = passedCount;
    }

    public Integer getSucceededFixedCount()
    {
        return succeededFixedCount;
    }

    public void setSucceededFixedCount(final Integer succeededFixedCount)
    {
        this.succeededFixedCount = succeededFixedCount;
    }

    public Integer getFailedKnownCount()
    {
        return failedKnownCount;
    }

    public void setFailedKnownCount(final Integer failedKnownCount)
    {
        this.failedKnownCount = failedKnownCount;
    }

    public Integer getFailedUnknownCount()
    {
        return failedUnknownCount;
    }

    public void setFailedUnknownCount(final Integer failedUnknownCount)
    {
        this.failedUnknownCount = failedUnknownCount;
    }

    public Integer getIgnoredCount()
    {
        return ignoredCount;
    }

    public void setIgnoredCount(final Integer ignoredCount)
    {
        this.ignoredCount = ignoredCount;
    }

    public Double getPassRate()
    {
        return passRate;
    }

    public void setPassRate(final Double passRate)
    {
        this.passRate = passRate;
    }

    public Boolean getIsDeleted()
    {
        return isDeleted;
    }

    public void setIsDeleted(final Boolean isDeleted)
    {
        this.isDeleted = isDeleted;
    }

    public String getRunComment()
    {
        return runComment;
    }

    public void setRunComment(final String runComment)
    {
        this.runComment = runComment;
    }

    public String getRunJsonPath()
    {
        return runJsonPath;
    }

    public void setRunJsonPath(final String runJsonPath)
    {
        this.runJsonPath = runJsonPath;
    }

    public Boolean getAttachmentsSyncedToS3()
    {
        return attachmentsSyncedToS3;
    }

    public void setAttachmentsSyncedToS3(final Boolean attachmentsSyncedToS3)
    {
        this.attachmentsSyncedToS3 = attachmentsSyncedToS3;
    }

    public Long getDurationMs()
    {
        return durationMs;
    }

    public void setDurationMs(final Long durationMs)
    {
        this.durationMs = durationMs;
    }

    public String getFormattedDuration()
    {
        return formattedDuration;
    }

    public void setFormattedDuration(final String formattedDuration)
    {
        this.formattedDuration = formattedDuration;
    }

    public Integer getTotalLlmCalls()
    {
        return totalLlmCalls != null ? totalLlmCalls : 0;
    }

    public void setTotalLlmCalls(final Integer totalLlmCalls)
    {
        this.totalLlmCalls = totalLlmCalls;
    }

    public Long getTotalLlmTokens()
    {
        return totalLlmTokens != null ? totalLlmTokens : 0L;
    }

    public void setTotalLlmTokens(final Long totalLlmTokens)
    {
        this.totalLlmTokens = totalLlmTokens;
    }

    public Double getTotalLlmCost()
    {
        return totalLlmCost != null ? totalLlmCost : 0.0;
    }

    public void setTotalLlmCost(final Double totalLlmCost)
    {
        this.totalLlmCost = totalLlmCost;
    }

    public void recalculatePassRate()
    {
        final int denominator = (totalTests != null && totalTests > 0) ? totalTests : 1;
        final int numPassed = (passedCount != null ? passedCount : 0) + (succeededFixedCount != null ? succeededFixedCount : 0);
        this.passRate = Math.round((numPassed * 100.0 / denominator) * 10.0) / 10.0;
    }

    public double getPassedPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? ((passedCount != null ? passedCount : 0) * 100.0 / total) : 0.0;
    }

    public double getFixedPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? ((succeededFixedCount != null ? succeededFixedCount : 0) * 100.0 / total) : 0.0;
    }

    public double getKnownPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? ((failedKnownCount != null ? failedKnownCount : 0) * 100.0 / total) : 0.0;
    }

    public double getUnknownPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? ((failedUnknownCount != null ? failedUnknownCount : 0) * 100.0 / total) : 0.0;
    }

    public double getIgnoredPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? ((ignoredCount != null ? ignoredCount : 0) * 100.0 / total) : 0.0;
    }

    public String getPassedLabel()
    {
        final int count = (passedCount != null) ? passedCount : 0;
        return getPassedPct() >= 7.0 ? String.valueOf(count) : "";
    }

    public String getFixedLabel()
    {
        final int count = (succeededFixedCount != null) ? succeededFixedCount : 0;
        return getFixedPct() >= 7.0 ? String.valueOf(count) : "";
    }

    public String getKnownLabel()
    {
        final int count = (failedKnownCount != null) ? failedKnownCount : 0;
        return getKnownPct() >= 7.0 ? String.valueOf(count) : "";
    }

    public String getUnknownLabel()
    {
        final int count = (failedUnknownCount != null) ? failedUnknownCount : 0;
        return getUnknownPct() >= 7.0 ? String.valueOf(count) : "";
    }

    public String getIgnoredLabel()
    {
        final int count = (ignoredCount != null) ? ignoredCount : 0;
        return getIgnoredPct() >= 7.0 ? String.valueOf(count) : "";
    }

    public String getPassedTooltip()
    {
        final int count = (passedCount != null) ? passedCount : 0;
        return "Passed Clean: " + count + " (" + Math.round(getPassedPct()) + "%)";
    }

    public String getFixedTooltip()
    {
        final int count = (succeededFixedCount != null) ? succeededFixedCount : 0;
        return "Succeeded Fixed: " + count + " (" + Math.round(getFixedPct()) + "%)";
    }

    public String getKnownTooltip()
    {
        final int count = (failedKnownCount != null) ? failedKnownCount : 0;
        return "Failed Known: " + count + " (" + Math.round(getKnownPct()) + "%)";
    }

    public String getUnknownTooltip()
    {
        final int count = (failedUnknownCount != null) ? failedUnknownCount : 0;
        return "Failed Unknown: " + count + " (" + Math.round(getUnknownPct()) + "%)";
    }

    public String getIgnoredTooltip()
    {
        final int count = (ignoredCount != null) ? ignoredCount : 0;
        return "Ignored: " + count + " (" + Math.round(getIgnoredPct()) + "%)";
    }
}
