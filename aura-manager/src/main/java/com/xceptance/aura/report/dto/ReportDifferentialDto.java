/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data transfer object encapsulating multi-report differential analysis, highlighting
 * tests that turned green, unticketed failures (ticket gaps), known bug failures, and regressions.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ReportDifferentialDto
{
    private final List<String> runIds;
    private final int totalVariations;
    private final int turnedGreenCount;
    private final int unticketedFailCount;
    private final int knownFailCount;
    private final int regressionCount;
    private final int stablePassCount;
    private final List<TestTransitionDto> turnedGreen;
    private final List<TestTransitionDto> unticketedFailures;
    private final List<TestTransitionDto> knownFailures;
    private final List<TestTransitionDto> regressions;
    private final List<TestTransitionDto> allTransitions;

    public ReportDifferentialDto(
        final List<String> runIds,
        final int totalVariations,
        final int turnedGreenCount,
        final int unticketedFailCount,
        final int knownFailCount,
        final int regressionCount,
        final int stablePassCount,
        final List<TestTransitionDto> turnedGreen,
        final List<TestTransitionDto> unticketedFailures,
        final List<TestTransitionDto> knownFailures,
        final List<TestTransitionDto> regressions,
        final List<TestTransitionDto> allTransitions)
    {
        this.runIds = (runIds != null && !runIds.isEmpty())
            ? Collections.unmodifiableList(new ArrayList<>(runIds))
            : Collections.emptyList();
        this.totalVariations = totalVariations;
        this.turnedGreenCount = turnedGreenCount;
        this.unticketedFailCount = unticketedFailCount;
        this.knownFailCount = knownFailCount;
        this.regressionCount = regressionCount;
        this.stablePassCount = stablePassCount;
        this.turnedGreen = (turnedGreen != null && !turnedGreen.isEmpty())
            ? Collections.unmodifiableList(new ArrayList<>(turnedGreen))
            : Collections.emptyList();
        this.unticketedFailures = (unticketedFailures != null && !unticketedFailures.isEmpty())
            ? Collections.unmodifiableList(new ArrayList<>(unticketedFailures))
            : Collections.emptyList();
        this.knownFailures = (knownFailures != null && !knownFailures.isEmpty())
            ? Collections.unmodifiableList(new ArrayList<>(knownFailures))
            : Collections.emptyList();
        this.regressions = (regressions != null && !regressions.isEmpty())
            ? Collections.unmodifiableList(new ArrayList<>(regressions))
            : Collections.emptyList();
        this.allTransitions = (allTransitions != null && !allTransitions.isEmpty())
            ? Collections.unmodifiableList(new ArrayList<>(allTransitions))
            : Collections.emptyList();
    }

    public List<String> getRunIds()
    {
        return runIds;
    }

    public List<String> getAnalyzedRunIds()
    {
        return runIds;
    }

    public int getTotalVariations()
    {
        return totalVariations;
    }

    public int getTotalEvaluatedTests()
    {
        return totalVariations;
    }

    public int getTotalTransitions()
    {
        return totalVariations;
    }

    public int getTurnedGreenCount()
    {
        return turnedGreenCount;
    }

    public int getUnticketedFailCount()
    {
        return unticketedFailCount;
    }

    public int getUnticketedFailureCount()
    {
        return unticketedFailCount;
    }

    public int getKnownFailCount()
    {
        return knownFailCount;
    }

    public int getKnownFailureCount()
    {
        return knownFailCount;
    }

    public int getRegressionCount()
    {
        return regressionCount;
    }

    public int getStablePassCount()
    {
        return stablePassCount;
    }

    public List<TestTransitionDto> getTurnedGreen()
    {
        return turnedGreen;
    }

    public List<TestTransitionDto> getUnticketedFailures()
    {
        return unticketedFailures;
    }

    public List<TestTransitionDto> getKnownFailures()
    {
        return knownFailures;
    }

    public List<TestTransitionDto> getRegressions()
    {
        return regressions;
    }

    public List<TestTransitionDto> getAllTransitions()
    {
        return allTransitions;
    }
}
