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
 * Data transfer object encapsulating the latest LLM token cost aggregate and historical trend sparkline data.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class TokenCostTrendDto
{
    private final double currentCost;
    private final double previousCost;
    private final double deltaCost;
    private final double deltaPercentage;
    private final boolean isPositive;
    private final long totalTokens;
    private final List<Double> trendPoints;
    private final String sparklineSvgPath;
    private final List<TokenCostPointGroupDto> pointGroups;

    public TokenCostTrendDto(
        final double currentCost,
        final double previousCost,
        final long totalTokens,
        final List<Double> trendPoints)
    {
        this(currentCost, previousCost, totalTokens, trendPoints, Collections.emptyList());
    }

    public TokenCostTrendDto(
        final double currentCost,
        final double previousCost,
        final long totalTokens,
        final List<Double> trendPoints,
        final List<TokenCostPointGroupDto> pointGroups)
    {
        this.currentCost = Math.max(0.0, currentCost);
        this.previousCost = previousCost;
        this.totalTokens = Math.max(0L, totalTokens);
        this.trendPoints = (trendPoints != null) ? List.copyOf(trendPoints) : Collections.emptyList();
        this.pointGroups = (pointGroups != null) ? List.copyOf(pointGroups) : Collections.emptyList();

        if (previousCost >= 0.0)
        {
            this.deltaCost = this.currentCost - previousCost;
            if (previousCost > 0.0)
            {
                this.deltaPercentage = (this.deltaCost / previousCost) * 100.0;
            }
            else
            {
                this.deltaPercentage = (this.deltaCost > 0.0) ? 100.0 : 0.0;
            }
        }
        else
        {
            this.deltaCost = 0.0;
            this.deltaPercentage = 0.0;
        }

        this.isPositive = this.deltaCost >= 0.0;
        this.sparklineSvgPath = generateSparklineSvgPath(this.trendPoints);
    }

    public double getCurrentCost()
    {
        return currentCost;
    }

    public double getPreviousCost()
    {
        return previousCost;
    }

    public double getDeltaCost()
    {
        return deltaCost;
    }

    public double getDeltaPercentage()
    {
        return deltaPercentage;
    }

    public boolean isPositive()
    {
        return isPositive;
    }

    public long getTotalTokens()
    {
        return totalTokens;
    }

    public List<TokenCostPointGroupDto> getPointGroups()
    {
        return pointGroups;
    }

    public String getPointGroupsJson()
    {
        if (pointGroups == null || pointGroups.isEmpty())
        {
            return "[]";
        }
        final StringBuilder sb = new StringBuilder("[");
        boolean firstGroup = true;
        for (final TokenCostPointGroupDto g : pointGroups)
        {
            if (!firstGroup)
            {
                sb.append(",");
            }
            firstGroup = false;
            sb.append("{")
              .append("\"pointIndex\":").append(g.getPointIndex()).append(",")
              .append("\"pointLabel\":\"").append(escapeJson(g.getPointLabel())).append("\",")
              .append("\"runId\":\"").append(escapeJson(g.getRunId())).append("\",")
              .append("\"timestampLabel\":\"").append(escapeJson(g.getTimestampLabel())).append("\",")
              .append("\"totalCost\":").append(String.format(Locale.ROOT, "%.6f", g.getTotalCost())).append(",")
              .append("\"totalTokens\":").append(g.getTotalTokens()).append(",")
              .append("\"totalCostFormatted\":\"").append(escapeJson(g.getTotalCostFormatted())).append("\",")
              .append("\"totalTokensFormatted\":\"").append(escapeJson(g.getTotalTokensFormatted())).append("\",")
              .append("\"deltaCost\":").append(String.format(Locale.ROOT, "%.6f", g.getDeltaCost())).append(",")
              .append("\"deltaPercentage\":").append(String.format(Locale.ROOT, "%.2f", g.getDeltaPercentage())).append(",")
              .append("\"deltaLabel\":\"").append(escapeJson(g.getDeltaLabel())).append("\",")
              .append("\"contributingRuns\":[");

            boolean firstRun = true;
            for (final ContributingRunCostDto r : g.getContributingRuns())
            {
                if (!firstRun)
                {
                    sb.append(",");
                }
                firstRun = false;
                sb.append("{")
                  .append("\"runId\":\"").append(escapeJson(r.getRunId())).append("\",")
                  .append("\"timestampLabel\":\"").append(escapeJson(r.getTimestampLabel())).append("\",")
                  .append("\"environment\":\"").append(escapeJson(r.getEnvironment())).append("\",")
                  .append("\"browser\":\"").append(escapeJson(r.getBrowser())).append("\",")
                  .append("\"batchName\":\"").append(escapeJson(r.getBatchName())).append("\",")
                  .append("\"contributedTestCount\":").append(r.getContributedTestCount()).append(",")
                  .append("\"contributedCost\":").append(String.format(Locale.ROOT, "%.6f", r.getContributedCost())).append(",")
                  .append("\"contributedTokens\":").append(r.getContributedTokens()).append(",")
                  .append("\"costPercentage\":").append(String.format(Locale.ROOT, "%.2f", r.getCostPercentage())).append(",")
                  .append("\"contributedCostFormatted\":\"").append(escapeJson(r.getContributedCostFormatted())).append("\",")
                  .append("\"contributedTokensFormatted\":\"").append(escapeJson(r.getContributedTokensFormatted())).append("\",")
                  .append("\"costPercentageFormatted\":\"").append(escapeJson(r.getCostPercentageFormatted())).append("\"")
                  .append("}");
            }
            sb.append("]}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escapeJson(final String s)
    {
        if (s == null)
        {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    public List<Double> getTrendPoints()
    {
        return trendPoints;
    }

    public String getSparklineSvgPath()
    {
        return sparklineSvgPath;
    }

    public String getCurrentCostFormatted()
    {
        return formatCurrency(currentCost);
    }

    public String getDeltaFormatted()
    {
        final String sign = deltaCost > 0.0 ? "+" : (deltaCost < 0.0 ? "-" : "");
        return sign + formatCurrency(Math.abs(deltaCost));
    }

    public String getDeltaLabel()
    {
        if (previousCost < 0.0 || trendPoints.size() <= 1)
        {
            return "+$0.0000 vs prev day";
        }

        final String sign = deltaCost > 0.0 ? "+" : (deltaCost < 0.0 ? "-" : "");
        final String formattedAbs = formatCurrency(Math.abs(deltaCost));
        if (Math.abs(deltaCost) < 0.00005)
        {
            return "$0.0000 vs prev day";
        }
        return String.format(Locale.ROOT, "%s%s (%.0f%%) vs prev day", sign, formattedAbs, deltaPercentage);
    }

    public String getTotalTokensFormatted()
    {
        return String.format(Locale.ROOT, "%,d tokens", totalTokens);
    }

    private static String formatCurrency(final double amount)
    {
        if (amount >= 1_000_000.0)
        {
            return String.format(Locale.ROOT, "$%.2fM", amount / 1_000_000.0);
        }
        if (amount >= 10_000.0)
        {
            return String.format(Locale.ROOT, "$%.1fK", amount / 1_000.0);
        }
        if (amount >= 1.0)
        {
            return String.format(Locale.ROOT, "$%.2f", amount);
        }
        if (amount > 0.0)
        {
            return String.format(Locale.ROOT, "$%.4f", amount);
        }
        return "$0.0000";
    }

    private static String generateSparklineSvgPath(final List<Double> points)
    {
        final double width = 120.0;
        final double height = 32.0;
        final double padX = 4.0;
        final double padY = 4.0;
        final double plotW = width - (2.0 * padX);
        final double plotH = height - (2.0 * padY);

        if (points == null || points.isEmpty() || points.size() == 1)
        {
            return String.format(Locale.ROOT, "M %.1f %.1f L %.1f %.1f", padX, height / 2.0, width - padX, height / 2.0);
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (final Double val : points)
        {
            final double v = (val != null) ? val : 0.0;
            if (v < min)
            {
                min = v;
            }
            if (v > max)
            {
                max = v;
            }
        }

        final int n = points.size();
        final double[] xs = new double[n];
        final double[] ys = new double[n];

        for (int i = 0; i < n; i++)
        {
            xs[i] = padX + (i * (plotW / (n - 1.0)));
            final double v = (points.get(i) != null) ? points.get(i) : 0.0;
            if (max > min)
            {
                final double norm = (v - min) / (max - min);
                ys[i] = padY + ((1.0 - norm) * plotH);
            }
            else
            {
                ys[i] = height / 2.0;
            }
        }

        if (n == 2)
        {
            return String.format(Locale.ROOT, "M %.1f %.1f L %.1f %.1f", xs[0], ys[0], xs[1], ys[1]);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "M %.1f %.1f", xs[0], ys[0]));

        for (int i = 0; i < n - 1; i++)
        {
            final double p0x = (i > 0) ? xs[i - 1] : xs[i];
            final double p0y = (i > 0) ? ys[i - 1] : ys[i];
            final double p1x = xs[i];
            final double p1y = ys[i];
            final double p2x = xs[i + 1];
            final double p2y = ys[i + 1];
            final double p3x = (i + 2 < n) ? xs[i + 2] : p2x;
            final double p3y = (i + 2 < n) ? ys[i + 2] : p2y;

            final double cp1x = p1x + ((p2x - p0x) / 6.0);
            final double cp1y = p1y + ((p2y - p0y) / 6.0);
            final double cp2x = p2x - ((p3x - p1x) / 6.0);
            final double cp2y = p2y - ((p3y - p1y) / 6.0);

            sb.append(String.format(Locale.ROOT, " C %.1f %.1f, %.1f %.1f, %.1f %.1f", cp1x, cp1y, cp2x, cp2y, p2x, p2y));
        }

        return sb.toString();
    }
}
