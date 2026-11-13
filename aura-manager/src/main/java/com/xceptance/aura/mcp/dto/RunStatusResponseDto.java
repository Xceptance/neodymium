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
package com.xceptance.aura.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Universal run status DTO returned by get_status and summary resources.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RunStatusResponseDto
{
    private String runId;

    private String status;

    private MetricsDto metrics;

    private String summary;

    private List<FailureClusterDto> failureClusters;

    private List<DistilledFailureDto> inlineFailures;

    private HighEntropyNoticeDto highEntropyNotice;

    private String nextSteps;

    public String getRunId()
    {
        return runId;
    }

    public void setRunId(final String runId)
    {
        this.runId = runId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(final String status)
    {
        this.status = status;
    }

    public MetricsDto getMetrics()
    {
        return metrics;
    }

    public void setMetrics(final MetricsDto metrics)
    {
        this.metrics = metrics;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(final String summary)
    {
        this.summary = summary;
    }

    public List<FailureClusterDto> getFailureClusters()
    {
        return failureClusters;
    }

    public void setFailureClusters(final List<FailureClusterDto> failureClusters)
    {
        this.failureClusters = failureClusters;
    }

    public List<DistilledFailureDto> getInlineFailures()
    {
        return inlineFailures;
    }

    public void setInlineFailures(final List<DistilledFailureDto> inlineFailures)
    {
        this.inlineFailures = inlineFailures;
    }

    public HighEntropyNoticeDto getHighEntropyNotice()
    {
        return highEntropyNotice;
    }

    public void setHighEntropyNotice(final HighEntropyNoticeDto highEntropyNotice)
    {
        this.highEntropyNotice = highEntropyNotice;
    }

    public String getNextSteps()
    {
        return nextSteps;
    }

    public void setNextSteps(final String nextSteps)
    {
        this.nextSteps = nextSteps;
    }

    public static class MetricsDto
    {
        private int total;

        private int passed;

        private int failed;

        private int skipped;

        private DurationDto duration;

        public MetricsDto()
        {
        }

        public MetricsDto(final int total, final int passed, final int failed, final int skipped, final long durationMs)
        {
            this.total = total;
            this.passed = passed;
            this.failed = failed;
            this.skipped = skipped;
            this.duration = DurationDto.ofMillis(durationMs);
        }

        public int getTotal()
        {
            return total;
        }

        public void setTotal(final int total)
        {
            this.total = total;
        }

        public int getPassed()
        {
            return passed;
        }

        public void setPassed(final int passed)
        {
            this.passed = passed;
        }

        public int getFailed()
        {
            return failed;
        }

        public void setFailed(final int failed)
        {
            this.failed = failed;
        }

        public int getSkipped()
        {
            return skipped;
        }

        public void setSkipped(final int skipped)
        {
            this.skipped = skipped;
        }

        public DurationDto getDuration()
        {
            return duration;
        }

        public void setDuration(final DurationDto duration)
        {
            this.duration = duration;
        }

        public long getDurationMs()
        {
            return (duration != null) ? duration.getTotalMs() : 0L;
        }

        public void setDurationMs(final long durationMs)
        {
            this.duration = DurationDto.ofMillis(durationMs);
        }
    }

    public static class DurationDto
    {
        private Duration duration;

        public DurationDto()
        {
            this.duration = Duration.ZERO;
        }

        public DurationDto(final Duration duration)
        {
            this.duration = (duration != null) ? duration : Duration.ZERO;
        }

        public static DurationDto ofMillis(final long ms)
        {
            return new DurationDto(Duration.ofMillis(ms));
        }

        public static DurationDto ofSeconds(final long seconds)
        {
            return new DurationDto(Duration.ofSeconds(seconds));
        }

        public static DurationDto of(final Duration duration)
        {
            return new DurationDto(duration);
        }

        public String getFormatted()
        {
            if (duration == null || duration.isZero())
            {
                return "0s";
            }

            final long hours = duration.toHours();
            final int minutes = duration.toMinutesPart();
            final int seconds = duration.toSecondsPart();
            final int millis = duration.toMillisPart();

            if (hours > 0)
            {
                return String.format("%dh %02dm %02ds", hours, minutes, seconds);
            }
            if (minutes > 0)
            {
                return String.format("%dm %02ds", minutes, seconds);
            }
            if (seconds > 0)
            {
                return (millis > 0)
                    ? String.format("%d.%01ds", seconds, millis / 100)
                    : String.format("%ds", seconds);
            }
            return millis + "ms";
        }

        public String getIso8601()
        {
            return (duration != null) ? duration.toString() : "PT0S";
        }

        public long getTotalSeconds()
        {
            return (duration != null) ? duration.toSeconds() : 0L;
        }

        public long getTotalMinutes()
        {
            return (duration != null) ? duration.toMinutes() : 0L;
        }

        public long getTotalHours()
        {
            return (duration != null) ? duration.toHours() : 0L;
        }

        public long getTotalMs()
        {
            return (duration != null) ? duration.toMillis() : 0L;
        }

        @JsonIgnore
        public Duration asDuration()
        {
            return duration;
        }

        public void setDuration(final Duration duration)
        {
            this.duration = (duration != null) ? duration : Duration.ZERO;
        }
    }

    public static class HighEntropyNoticeDto
    {
        private int clusterCount;

        private String notice;

        public HighEntropyNoticeDto()
        {
        }

        public HighEntropyNoticeDto(final int clusterCount, final String notice)
        {
            this.clusterCount = clusterCount;
            this.notice = notice;
        }

        public int getClusterCount()
        {
            return clusterCount;
        }

        public void setClusterCount(final int clusterCount)
        {
            this.clusterCount = clusterCount;
        }

        public String getNotice()
        {
            return notice;
        }

        public void setNotice(final String notice)
        {
            this.notice = notice;
        }
    }
}
