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
package com.xceptance.aura.report.dto.widget;

/**
 * Data transfer object encapsulating a single chronological point in a trend graph,
 * enriched with granular test execution state information for interactive hover inspection.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class WidgetTrendPointDto
{
    private final int index;
    private final double x;
    private final double y;
    private final String label;
    private final String timestampLabel;
    private final double value;
    private final String valueFormatted;
    private final String runId;

    // Granular test outcome state for hover inspection
    private final int totalTests;
    private final int passTotal;
    private final int passClean;
    private final int passHealed;
    private final int passAi;
    private final int fixedTotal;
    private final int fixedClean;
    private final int fixedHealed;
    private final int fixedAi;
    private final int knownTotal;
    private final int knownClean;
    private final int knownHealed;
    private final int knownAi;
    private final int unknownTotal;
    private final int unknownClean;
    private final int unknownHealed;
    private final int unknownAi;
    private final int ignoredCount;
    private final String stateBadgeText;
    private final String stateBadgeClass;

    public WidgetTrendPointDto(
        final int index,
        final double x,
        final double y,
        final String label,
        final String timestampLabel,
        final double value,
        final String valueFormatted,
        final String runId)
    {
        this(
            index, x, y, label, timestampLabel, value, valueFormatted, runId,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            valueFormatted, "badge-pass"
        );
    }

    public WidgetTrendPointDto(
        final int index,
        final double x,
        final double y,
        final String label,
        final String timestampLabel,
        final double value,
        final String valueFormatted,
        final String runId,
        final int totalTests,
        final int passTotal,
        final int passClean,
        final int passHealed,
        final int passAi,
        final int fixedTotal,
        final int fixedClean,
        final int fixedHealed,
        final int fixedAi,
        final int knownTotal,
        final int knownClean,
        final int knownHealed,
        final int knownAi,
        final int unknownTotal,
        final int unknownClean,
        final int unknownHealed,
        final int unknownAi,
        final int ignoredCount,
        final String stateBadgeText,
        final String stateBadgeClass)
    {
        this.index = index;
        this.x = x;
        this.y = y;
        this.label = (label != null) ? label : "";
        this.timestampLabel = (timestampLabel != null) ? timestampLabel : "";
        this.value = value;
        this.valueFormatted = (valueFormatted != null) ? valueFormatted : "";
        this.runId = (runId != null) ? runId : "";
        this.totalTests = totalTests;
        this.passTotal = passTotal;
        this.passClean = passClean;
        this.passHealed = passHealed;
        this.passAi = passAi;
        this.fixedTotal = fixedTotal;
        this.fixedClean = fixedClean;
        this.fixedHealed = fixedHealed;
        this.fixedAi = fixedAi;
        this.knownTotal = knownTotal;
        this.knownClean = knownClean;
        this.knownHealed = knownHealed;
        this.knownAi = knownAi;
        this.unknownTotal = unknownTotal;
        this.unknownClean = unknownClean;
        this.unknownHealed = unknownHealed;
        this.unknownAi = unknownAi;
        this.ignoredCount = ignoredCount;
        this.stateBadgeText = (stateBadgeText != null && !stateBadgeText.isBlank()) ? stateBadgeText : valueFormatted;
        this.stateBadgeClass = (stateBadgeClass != null && !stateBadgeClass.isBlank()) ? stateBadgeClass : "badge-pass";
    }

    public int getIndex()
    {
        return index;
    }

    public double getX()
    {
        return x;
    }

    public double getY()
    {
        return y;
    }

    public String getLabel()
    {
        return label;
    }

    public String getTimestampLabel()
    {
        return timestampLabel;
    }

    public double getValue()
    {
        return value;
    }

    public String getValueFormatted()
    {
        return valueFormatted;
    }

    public String getRunId()
    {
        return runId;
    }

    public int getTotalTests()
    {
        return totalTests;
    }

    public int getPassTotal()
    {
        return passTotal;
    }

    public int getPassClean()
    {
        return passClean;
    }

    public int getPassHealed()
    {
        return passHealed;
    }

    public int getPassAi()
    {
        return passAi;
    }

    public int getFixedTotal()
    {
        return fixedTotal;
    }

    public int getFixedClean()
    {
        return fixedClean;
    }

    public int getFixedHealed()
    {
        return fixedHealed;
    }

    public int getFixedAi()
    {
        return fixedAi;
    }

    public int getKnownTotal()
    {
        return knownTotal;
    }

    public int getKnownClean()
    {
        return knownClean;
    }

    public int getKnownHealed()
    {
        return knownHealed;
    }

    public int getKnownAi()
    {
        return knownAi;
    }

    public int getUnknownTotal()
    {
        return unknownTotal;
    }

    public int getUnknownClean()
    {
        return unknownClean;
    }

    public int getUnknownHealed()
    {
        return unknownHealed;
    }

    public int getUnknownAi()
    {
        return unknownAi;
    }

    public int getIgnoredCount()
    {
        return ignoredCount;
    }

    public String getStateBadgeText()
    {
        return stateBadgeText;
    }

    public String getStateBadgeClass()
    {
        return stateBadgeClass;
    }

    public boolean hasDetailedStatus()
    {
        return totalTests > 0;
    }
}
