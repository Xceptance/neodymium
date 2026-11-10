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
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "passed_healed_count")
    private Integer passedHealedCount = 0;

    @Column(name = "succeeded_fixed_healed_count")
    private Integer succeededFixedHealedCount = 0;

    @Column(name = "failed_known_healed_count")
    private Integer failedKnownHealedCount = 0;

    @Column(name = "failed_unknown_healed_count")
    private Integer failedUnknownHealedCount = 0;

    @Column(name = "passed_ai_count")
    private Integer passedAiCount = 0;

    @Column(name = "succeeded_fixed_ai_count")
    private Integer succeededFixedAiCount = 0;

    @Column(name = "failed_known_ai_count")
    private Integer failedKnownAiCount = 0;

    @Column(name = "failed_unknown_ai_count")
    private Integer failedUnknownAiCount = 0;

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
        if (timestampLabel != null && !timestampLabel.trim().isEmpty() && !"Recently".equalsIgnoreCase(timestampLabel.trim()))
        {
            final String trimmed = timestampLabel.trim();
            try
            {
                final java.time.Instant instant = java.time.Instant.parse(trimmed);
                final java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
                return ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            catch (final Exception e1)
            {
                try
                {
                    final java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(trimmed.replace(" ", "T"));
                    return ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
                catch (final Exception e2)
                {
                    return trimmed;
                }
            }
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
        final int healed = getPassedHealedCountSafe();
        final int ai = getPassedAiCountSafe();
        final int clean = getPassedCleanCount();
        if (healed > 0 || ai > 0)
        {
            final List<String> parts = new ArrayList<>();
            parts.add(clean + " clean");
            if (healed > 0)
            {
                parts.add(healed + " healed");
            }
            if (ai > 0)
            {
                parts.add(ai + " AI-driven");
            }
            return "Passed: " + count + " (" + String.join(", ", parts) + ", " + Math.round(getPassedPct()) + "%)";
        }
        return "Passed Clean: " + count + " (" + Math.round(getPassedPct()) + "%)";
    }

    public String getFixedTooltip()
    {
        final int count = (succeededFixedCount != null) ? succeededFixedCount : 0;
        final int healed = getFixedHealedCountSafe();
        final int ai = getFixedAiCountSafe();
        final int clean = getFixedCleanCount();
        if (healed > 0 || ai > 0)
        {
            final List<String> parts = new ArrayList<>();
            parts.add(clean + " clean");
            if (healed > 0)
            {
                parts.add(healed + " healed");
            }
            if (ai > 0)
            {
                parts.add(ai + " AI-driven");
            }
            return "Succeeded Fixed: " + count + " (" + String.join(", ", parts) + ", " + Math.round(getFixedPct()) + "%)";
        }
        return "Succeeded Fixed: " + count + " (" + Math.round(getFixedPct()) + "%)";
    }

    public String getKnownTooltip()
    {
        final int count = (failedKnownCount != null) ? failedKnownCount : 0;
        final int healed = getKnownHealedCountSafe();
        final int ai = getKnownAiCountSafe();
        final int clean = getKnownCleanCount();
        if (healed > 0 || ai > 0)
        {
            final List<String> parts = new ArrayList<>();
            parts.add(clean + " clean");
            if (healed > 0)
            {
                parts.add(healed + " healed");
            }
            if (ai > 0)
            {
                parts.add(ai + " AI-driven");
            }
            return "Failed Known: " + count + " (" + String.join(", ", parts) + ", " + Math.round(getKnownPct()) + "%)";
        }
        return "Failed Known: " + count + " (" + Math.round(getKnownPct()) + "%)";
    }

    public String getUnknownTooltip()
    {
        final int count = (failedUnknownCount != null) ? failedUnknownCount : 0;
        final int healed = getUnknownHealedCountSafe();
        final int ai = getUnknownAiCountSafe();
        final int clean = getUnknownCleanCount();
        if (healed > 0 || ai > 0)
        {
            final List<String> parts = new ArrayList<>();
            parts.add(clean + " clean");
            if (healed > 0)
            {
                parts.add(healed + " healed");
            }
            if (ai > 0)
            {
                parts.add(ai + " AI-driven");
            }
            return "Failed Unknown: " + count + " (" + String.join(", ", parts) + ", " + Math.round(getUnknownPct()) + "%)";
        }
        return "Failed Unknown: " + count + " (" + Math.round(getUnknownPct()) + "%)";
    }

    public String getIgnoredTooltip()
    {
        final int count = (ignoredCount != null) ? ignoredCount : 0;
        return "Ignored: " + count + " (" + Math.round(getIgnoredPct()) + "%)";
    }

    public Integer getPassedHealedCount()
    {
        return passedHealedCount;
    }

    public void setPassedHealedCount(final Integer passedHealedCount)
    {
        this.passedHealedCount = passedHealedCount;
    }

    public Integer getSucceededFixedHealedCount()
    {
        return succeededFixedHealedCount;
    }

    public void setSucceededFixedHealedCount(final Integer succeededFixedHealedCount)
    {
        this.succeededFixedHealedCount = succeededFixedHealedCount;
    }

    public Integer getFailedKnownHealedCount()
    {
        return failedKnownHealedCount;
    }

    public void setFailedKnownHealedCount(final Integer failedKnownHealedCount)
    {
        this.failedKnownHealedCount = failedKnownHealedCount;
    }

    public Integer getFailedUnknownHealedCount()
    {
        return failedUnknownHealedCount;
    }

    public void setFailedUnknownHealedCount(final Integer failedUnknownHealedCount)
    {
        this.failedUnknownHealedCount = failedUnknownHealedCount;
    }

    public int getPassedHealedCountSafe()
    {
        return passedHealedCount != null ? passedHealedCount : 0;
    }

    public int getPassedCleanCount()
    {
        final int pass = (passedCount != null) ? passedCount : 0;
        return Math.max(0, pass - getPassedHealedCountSafe() - getPassedAiCountSafe());
    }

    public double getPassedCleanPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? (getPassedCleanCount() * 100.0 / total) : 0.0;
    }

    public double getPassedHealedPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? (getPassedHealedCountSafe() * 100.0 / total) : 0.0;
    }

    public double getPassedAiPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? (getPassedAiCountSafe() * 100.0 / total) : 0.0;
    }

    public int getFixedHealedCountSafe()
    {
        return succeededFixedHealedCount != null ? succeededFixedHealedCount : 0;
    }

    public int getFixedCleanCount()
    {
        final int fixed = (succeededFixedCount != null) ? succeededFixedCount : 0;
        return Math.max(0, fixed - getFixedHealedCountSafe() - getFixedAiCountSafe());
    }

    public double getFixedCleanPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? (getFixedCleanCount() * 100.0 / total) : 0.0;
    }

    public double getFixedHealedPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? (getFixedHealedCountSafe() * 100.0 / total) : 0.0;
    }

    public double getFixedAiPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? (getFixedAiCountSafe() * 100.0 / total) : 0.0;
    }

    public int getKnownHealedCountSafe()
    {
        return failedKnownHealedCount != null ? failedKnownHealedCount : 0;
    }

    public int getKnownCleanCount()
    {
        final int known = (failedKnownCount != null) ? failedKnownCount : 0;
        return Math.max(0, known - getKnownHealedCountSafe() - getKnownAiCountSafe());
    }

    public double getKnownCleanPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? (getKnownCleanCount() * 100.0 / total) : 0.0;
    }

    public double getKnownHealedPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? (getKnownHealedCountSafe() * 100.0 / total) : 0.0;
    }

    public double getKnownAiPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? (getKnownAiCountSafe() * 100.0 / total) : 0.0;
    }

    public int getUnknownHealedCountSafe()
    {
        return failedUnknownHealedCount != null ? failedUnknownHealedCount : 0;
    }

    public int getUnknownCleanCount()
    {
        final int unknown = (failedUnknownCount != null) ? failedUnknownCount : 0;
        return Math.max(0, unknown - getUnknownHealedCountSafe() - getUnknownAiCountSafe());
    }

    public double getUnknownCleanPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? (getUnknownCleanCount() * 100.0 / total) : 0.0;
    }

    public double getUnknownHealedPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? (getUnknownHealedCountSafe() * 100.0 / total) : 0.0;
    }

    public double getUnknownAiPct()
    {
        final int total = (totalTests != null && totalTests > 0) ? totalTests : 0;
        return total > 0 ? (getUnknownAiCountSafe() * 100.0 / total) : 0.0;
    }

    public Integer getPassedAiCount()
    {
        return passedAiCount;
    }

    public void setPassedAiCount(final Integer passedAiCount)
    {
        this.passedAiCount = passedAiCount;
    }

    public Integer getSucceededFixedAiCount()
    {
        return succeededFixedAiCount;
    }

    public void setSucceededFixedAiCount(final Integer succeededFixedAiCount)
    {
        this.succeededFixedAiCount = succeededFixedAiCount;
    }

    public Integer getFailedKnownAiCount()
    {
        return failedKnownAiCount;
    }

    public void setFailedKnownAiCount(final Integer failedKnownAiCount)
    {
        this.failedKnownAiCount = failedKnownAiCount;
    }

    public Integer getFailedUnknownAiCount()
    {
        return failedUnknownAiCount;
    }

    public void setFailedUnknownAiCount(final Integer failedUnknownAiCount)
    {
        this.failedUnknownAiCount = failedUnknownAiCount;
    }

    public int getPassedAiCountSafe()
    {
        return passedAiCount != null ? passedAiCount : 0;
    }

    public int getFixedAiCountSafe()
    {
        return succeededFixedAiCount != null ? succeededFixedAiCount : 0;
    }

    public int getKnownAiCountSafe()
    {
        return failedKnownAiCount != null ? failedKnownAiCount : 0;
    }

    public int getUnknownAiCountSafe()
    {
        return failedUnknownAiCount != null ? failedUnknownAiCount : 0;
    }

    public int getPassAiCountSafe()
    {
        return getPassedAiCountSafe();
    }

    public int getSucceededFixedAiCountSafe()
    {
        return getFixedAiCountSafe();
    }

    public int getFailedKnownAiCountSafe()
    {
        return getKnownAiCountSafe();
    }

    public int getFailedUnknownAiCountSafe()
    {
        return getUnknownAiCountSafe();
    }

    public String getPassedCleanLabel()
    {
        final int count = getPassedCleanCount();
        return getPassedCleanPct() >= 7.0 ? String.valueOf(count) : "";
    }

    public String getPassedHealedLabel()
    {
        final int count = getPassedHealedCountSafe();
        return getPassedHealedPct() >= 7.0 ? ("✨ " + count) : "";
    }

    public String getFixedCleanLabel()
    {
        final int count = getFixedCleanCount();
        return getFixedCleanPct() >= 7.0 ? String.valueOf(count) : "";
    }

    public String getFixedHealedLabel()
    {
        final int count = getFixedHealedCountSafe();
        return getFixedHealedPct() >= 7.0 ? ("✨ " + count) : "";
    }

    public String getKnownCleanLabel()
    {
        final int count = getKnownCleanCount();
        return getKnownCleanPct() >= 7.0 ? String.valueOf(count) : "";
    }

    public String getKnownHealedLabel()
    {
        final int count = getKnownHealedCountSafe();
        return getKnownHealedPct() >= 7.0 ? ("✨ " + count) : "";
    }

    public String getUnknownCleanLabel()
    {
        final int count = getUnknownCleanCount();
        return getUnknownCleanPct() >= 7.0 ? String.valueOf(count) : "";
    }

    public String getUnknownHealedLabel()
    {
        final int count = getUnknownHealedCountSafe();
        return getUnknownHealedPct() >= 7.0 ? ("✨ " + count) : "";
    }

    public String getPassedCleanTooltip()
    {
        final int count = getPassedCleanCount();
        return "Passed Clean: " + count + " (" + Math.round(getPassedCleanPct()) + "%)";
    }

    public String getPassedHealedTooltip()
    {
        final int count = getPassedHealedCountSafe();
        return "Passed (Healed): " + count + " (" + Math.round(getPassedHealedPct()) + "%)";
    }

    public String getFixedCleanTooltip()
    {
        final int count = getFixedCleanCount();
        return "Succeeded Fixed: " + count + " (" + Math.round(getFixedCleanPct()) + "%)";
    }

    public String getFixedHealedTooltip()
    {
        final int count = getFixedHealedCountSafe();
        return "Succeeded Fixed (Healed): " + count + " (" + Math.round(getFixedHealedPct()) + "%)";
    }

    public String getKnownCleanTooltip()
    {
        final int count = getKnownCleanCount();
        return "Failed Known: " + count + " (" + Math.round(getKnownCleanPct()) + "%)";
    }

    public String getKnownHealedTooltip()
    {
        final int count = getKnownHealedCountSafe();
        return "Failed Known (Healed): " + count + " (" + Math.round(getKnownHealedPct()) + "%)";
    }

    public String getUnknownCleanTooltip()
    {
        final int count = getUnknownCleanCount();
        return "Failed Unknown: " + count + " (" + Math.round(getUnknownCleanPct()) + "%)";
    }

    public String getUnknownHealedTooltip()
    {
        final int count = getUnknownHealedCountSafe();
        return "Failed Unknown (Healed): " + count + " (" + Math.round(getUnknownHealedPct()) + "%)";
    }

    public String getPassedAiLabel()
    {
        final int count = getPassedAiCountSafe();
        return getPassedAiPct() >= 7.0 ? String.valueOf(count) : "";
    }

    public String getFixedAiLabel()
    {
        final int count = getFixedAiCountSafe();
        return getFixedAiPct() >= 7.0 ? String.valueOf(count) : "";
    }

    public String getKnownAiLabel()
    {
        final int count = getKnownAiCountSafe();
        return getKnownAiPct() >= 7.0 ? String.valueOf(count) : "";
    }

    public String getUnknownAiLabel()
    {
        final int count = getUnknownAiCountSafe();
        return getUnknownAiPct() >= 7.0 ? String.valueOf(count) : "";
    }

    public String getPassedAiTooltip()
    {
        final int count = getPassedAiCountSafe();
        return "Passed (AI-Driven): " + count + " (" + Math.round(getPassedAiPct()) + "%)";
    }

    public String getFixedAiTooltip()
    {
        final int count = getFixedAiCountSafe();
        return "Succeeded Fixed (AI-Driven): " + count + " (" + Math.round(getFixedAiPct()) + "%)";
    }

    public String getKnownAiTooltip()
    {
        final int count = getKnownAiCountSafe();
        return "Failed Known (AI-Driven): " + count + " (" + Math.round(getKnownAiPct()) + "%)";
    }

    public String getUnknownAiTooltip()
    {
        final int count = getUnknownAiCountSafe();
        return "Failed Unknown (AI-Driven): " + count + " (" + Math.round(getUnknownAiPct()) + "%)";
    }
}
