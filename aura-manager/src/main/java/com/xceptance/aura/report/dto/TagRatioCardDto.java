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
package com.xceptance.aura.report.dto;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Data transfer object encapsulating a dynamic tag type ratio card and its distribution segments.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class TagRatioCardDto
{
    private final String tagKey;
    private final String displayName;
    private final String ratioType;
    private final String cleanKey;
    private final String clearBtnId;
    private final List<DistributionRatioDto> ratios;
    private final String emptyMessage;

    public TagRatioCardDto(
        final String tagKey,
        final List<DistributionRatioDto> ratios)
    {
        final String safeKey = (tagKey != null && !tagKey.isBlank()) ? tagKey.trim() : "tag";
        this.tagKey = safeKey;
        this.cleanKey = safeKey.replaceAll("[^a-zA-Z0-9_]", "_");
        final String capKey = safeKey.substring(0, 1).toUpperCase(Locale.ROOT) + safeKey.substring(1);
        this.displayName = "Tag: " + capKey + " Ratio";
        this.ratioType = "tag_" + this.cleanKey;
        this.clearBtnId = "batchClearTag_" + this.cleanKey + "FilterBtn";
        this.ratios = (ratios != null) ? List.copyOf(ratios) : Collections.emptyList();
        this.emptyMessage = "No " + safeKey + " data available.";
    }

    public TagRatioCardDto(
        final String tagKey,
        final String displayName,
        final String ratioType,
        final String cleanKey,
        final String clearBtnId,
        final List<DistributionRatioDto> ratios,
        final String emptyMessage)
    {
        this.tagKey = (tagKey != null && !tagKey.isBlank()) ? tagKey.trim() : "tag";
        this.displayName = displayName;
        this.ratioType = ratioType;
        this.cleanKey = cleanKey;
        this.clearBtnId = clearBtnId;
        this.ratios = (ratios != null) ? List.copyOf(ratios) : Collections.emptyList();
        this.emptyMessage = emptyMessage;
    }

    public String getTagKey()
    {
        return tagKey;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getRatioType()
    {
        return ratioType;
    }

    public String getCleanKey()
    {
        return cleanKey;
    }

    public String getClearBtnId()
    {
        return clearBtnId;
    }

    public List<DistributionRatioDto> getRatios()
    {
        return ratios;
    }

    public String getEmptyMessage()
    {
        return emptyMessage;
    }
}
