/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.neodymium.ai.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Log comparator utility to compare token usage, costs, execution times, PESAP context levels,
 * and locator quality between two Neodymium AI recording log files.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class LogComparator
{
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

    // Pricing for Gemini 3.5 Flash Lite (per 1,000,000 tokens)
    private static final double INPUT_PRICE_PER_MILLION = 0.075;
    private static final double OUTPUT_PRICE_PER_MILLION = 0.30;

    /**
     * Record representing aggregate token statistics.
     */
    public record TokenStats(
        long promptTokens,
        long outputTokens,
        long totalTokens,
        int callCount
    )
    {
        public double calculateCost()
        {
            final double inputCost = (promptTokens / 1_000_000.0) * INPUT_PRICE_PER_MILLION;
            final double outputCost = (outputTokens / 1_000_000.0) * OUTPUT_PRICE_PER_MILLION;
            return inputCost + outputCost;
        }
    }

    /**
     * Record holding information about an action step execution.
     */
    public record StepInfo(
        int stepIndex,
        String instruction,
        String target,
        String locator,
        String actionType,
        String contextLevel
    )
    {
    }

    /**
     * Record holding dataset metrics.
     */
    public record DatasetMetrics(
        String name,
        String startTimeStr,
        String endTimeStr,
        long durationMs,
        TokenStats pesapStats,
        TokenStats actionStats,
        TokenStats totalStats,
        Map<String, Integer> contextLevels,
        List<StepInfo> steps,
        List<String> locators
    )
    {
    }

    /**
     * Record holding full analysis of a single log file.
     */
    public record LogAnalysis(
        String fileName,
        Map<String, DatasetMetrics> datasetMetricsMap,
        TokenStats overallPesapStats,
        TokenStats overallActionStats,
        TokenStats overallTotalStats,
        long totalDurationMs,
        Map<String, Integer> overallContextLevels,
        List<String> allLocators
    )
    {
    }

    public static void main(final String[] args)
    {
        final String oldLogPath = args.length > 0 ? args[0] : "neodymium-ai.testCheckoutLiveAllDataSets.recording.log";
        final String newLogPath = args.length > 1 ? args[1] : "neodymium-ai.testCheckoutLiveAllDataSets.recording.new-dom.log";

        try
        {
            final LogAnalysis oldAnalysis = analyzeLog(oldLogPath, "OLD Log (AXTree DOM)");
            final LogAnalysis newAnalysis = analyzeLog(newLogPath, "NEW Log (New DOM Repr)");

            printComparisonReport(oldAnalysis, newAnalysis);
        }
        catch (final Exception e)
        {
            System.err.println("Error analyzing log files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Analyzes a single log file and extracts dataset metrics, tokens, timings, context levels, and locators.
     *
     * @param logPath the file path to analyze
     * @param label descriptive label for the log
     * @return the complete LogAnalysis
     * @throws IOException if reading log file fails
     */
    public static LogAnalysis analyzeLog(final String logPath, final String label) throws IOException
    {
        final Path path = Paths.get(logPath);
        if (!Files.exists(path))
        {
            throw new IllegalArgumentException("Log file not found: " + logPath);
        }

        final List<String> lines = Files.readAllLines(path);

        final Pattern datasetStartPat = Pattern.compile("Active Dataset:\\s*([\\w-]+)");
        final Pattern pesapTokenPat = Pattern.compile("\\[Pre-Step PESAP\\] Tokens:\\s*(\\d+)\\s*in.*?→\\s*(\\d+)\\s*out.*?total:\\s*(\\d+)");
        final Pattern actionTokenPat = Pattern.compile("Call Tokens:\\s*(\\d+)\\s*in.*?→\\s*(\\d+)\\s*out.*?total:\\s*(\\d+)");
        final Pattern targetPat = Pattern.compile("Target:\\s*(.+)");
        final Pattern stepHeaderPat = Pattern.compile("▶ \\[Step (\\d+)/(\\d+)\\] Instruction:\\s*\"([^\"]+)\"");
        final Pattern contextLevelPat = Pattern.compile("\"targetContextLevel\"\\s*:\\s*\"([^\"]+)\"");
        final Pattern pesapPredictedPat = Pattern.compile("Predicted Context Level:\\s*([A-Z_]+)");
        final Pattern locatorPat = Pattern.compile("🎯 Locator:\\s*(.+)|\"locator\"\\s*:\\s*\"([^\"]+)\"");

        final Map<String, DatasetMetrics> datasetMetricsMap = new LinkedHashMap<>();

        String currentDataset = null;
        String currentDatasetStartTime = null;
        String lastTimestampStr = null;

        long pesapPrompt = 0;
        long pesapOutput = 0;
        long pesapTotal = 0;
        int pesapCount = 0;

        long actionPrompt = 0;
        long actionOutput = 0;
        long actionTotal = 0;
        int actionCount = 0;

        long overallPesapPrompt = 0;
        long overallPesapOutput = 0;
        long overallPesapTotal = 0;
        int overallPesapCount = 0;

        long overallActionPrompt = 0;
        long overallActionOutput = 0;
        long overallActionTotal = 0;
        int overallActionCount = 0;

        Map<String, Integer> dsContextLevels = new LinkedHashMap<>();
        final Map<String, Integer> overallContextLevels = new LinkedHashMap<>();

        List<StepInfo> dsSteps = new ArrayList<>();
        List<String> dsLocators = new ArrayList<>();
        final List<String> allLocators = new ArrayList<>();

        int currentStepIdx = 0;
        String currentInstruction = "";
        String currentTarget = "";
        String currentLocator = "";
        String currentContextLevel = "";

        for (final String line : lines)
        {
            if (line.length() >= 23 && line.charAt(4) == '-' && line.charAt(7) == '-')
            {
                lastTimestampStr = line.substring(0, 23);
            }

            final Matcher dsMatcher = datasetStartPat.matcher(line);
            if (dsMatcher.find())
            {
                if (currentDataset != null)
                {
                    final long duration = calculateDuration(currentDatasetStartTime, lastTimestampStr);
                    final TokenStats pesap = new TokenStats(pesapPrompt, pesapOutput, pesapTotal, pesapCount);
                    final TokenStats action = new TokenStats(actionPrompt, actionOutput, actionTotal, actionCount);
                    final TokenStats total = new TokenStats(
                        pesapPrompt + actionPrompt,
                        pesapOutput + actionOutput,
                        pesapTotal + actionTotal,
                        pesapCount + actionCount
                    );

                    datasetMetricsMap.put(currentDataset, new DatasetMetrics(
                        currentDataset,
                        currentDatasetStartTime,
                        lastTimestampStr,
                        duration,
                        pesap,
                        action,
                        total,
                        new LinkedHashMap<>(dsContextLevels),
                        new ArrayList<>(dsSteps),
                        new ArrayList<>(dsLocators)
                    ));
                }

                currentDataset = dsMatcher.group(1);
                currentDatasetStartTime = lastTimestampStr;

                pesapPrompt = 0;
                pesapOutput = 0;
                pesapTotal = 0;
                pesapCount = 0;

                actionPrompt = 0;
                actionOutput = 0;
                actionTotal = 0;
                actionCount = 0;

                dsContextLevels = new LinkedHashMap<>();
                dsSteps = new ArrayList<>();
                dsLocators = new ArrayList<>();
            }

            final Matcher stepHeaderMatcher = stepHeaderPat.matcher(line);
            if (stepHeaderMatcher.find())
            {
                currentStepIdx = Integer.parseInt(stepHeaderMatcher.group(1));
                currentInstruction = stepHeaderMatcher.group(3);
                currentTarget = "";
                currentLocator = "";
                currentContextLevel = "";
            }

            final Matcher targetMatcher = targetPat.matcher(line);
            if (targetMatcher.find())
            {
                currentTarget = targetMatcher.group(1).trim();
            }

            final Matcher pesapTokenMatcher = pesapTokenPat.matcher(line);
            if (pesapTokenMatcher.find())
            {
                final long pIn = Long.parseLong(pesapTokenMatcher.group(1));
                final long pOut = Long.parseLong(pesapTokenMatcher.group(2));
                final long pTot = Long.parseLong(pesapTokenMatcher.group(3));

                pesapPrompt += pIn;
                pesapOutput += pOut;
                pesapTotal += pTot;
                pesapCount++;

                overallPesapPrompt += pIn;
                overallPesapOutput += pOut;
                overallPesapTotal += pTot;
                overallPesapCount++;
            }

            final Matcher actionTokenMatcher = actionTokenPat.matcher(line);
            if (actionTokenMatcher.find())
            {
                final long aIn = Long.parseLong(actionTokenMatcher.group(1));
                final long aOut = Long.parseLong(actionTokenMatcher.group(2));
                final long aTot = Long.parseLong(actionTokenMatcher.group(3));

                actionPrompt += aIn;
                actionOutput += aOut;
                actionTotal += aTot;
                actionCount++;

                overallActionPrompt += aIn;
                overallActionOutput += aOut;
                overallActionTotal += aTot;
                overallActionCount++;
            }

            final Matcher pesapPredMatcher = pesapPredictedPat.matcher(line);
            if (pesapPredMatcher.find())
            {
                final String level = pesapPredMatcher.group(1);
                dsContextLevels.put(level, dsContextLevels.getOrDefault(level, 0) + 1);
                overallContextLevels.put(level, overallContextLevels.getOrDefault(level, 0) + 1);
                currentContextLevel = level;
            }

            final Matcher ctxLevelMatcher = contextLevelPat.matcher(line);
            if (ctxLevelMatcher.find() && currentContextLevel.isEmpty())
            {
                final String level = ctxLevelMatcher.group(1);
                dsContextLevels.put(level, dsContextLevels.getOrDefault(level, 0) + 1);
                overallContextLevels.put(level, overallContextLevels.getOrDefault(level, 0) + 1);
                currentContextLevel = level;
            }

            final Matcher locMatcher = locatorPat.matcher(line);
            if (locMatcher.find())
            {
                final String loc = locMatcher.group(1) != null ? locMatcher.group(1).trim() : locMatcher.group(2).trim();
                if (!loc.isEmpty() && !loc.equals("CSS selector or URL"))
                {
                    currentLocator = loc;
                    dsLocators.add(loc);
                    allLocators.add(loc);

                    dsSteps.add(new StepInfo(
                        currentStepIdx,
                        currentInstruction,
                        currentTarget,
                        currentLocator,
                        "ACTION",
                        currentContextLevel
                    ));
                }
            }
        }

        // Close last dataset
        if (currentDataset != null && !datasetMetricsMap.containsKey(currentDataset))
        {
            final long duration = calculateDuration(currentDatasetStartTime, lastTimestampStr);
            final TokenStats pesap = new TokenStats(pesapPrompt, pesapOutput, pesapTotal, pesapCount);
            final TokenStats action = new TokenStats(actionPrompt, actionOutput, actionTotal, actionCount);
            final TokenStats total = new TokenStats(
                pesapPrompt + actionPrompt,
                pesapOutput + actionOutput,
                pesapTotal + actionTotal,
                pesapCount + actionCount
            );

            datasetMetricsMap.put(currentDataset, new DatasetMetrics(
                currentDataset,
                currentDatasetStartTime,
                lastTimestampStr,
                duration,
                pesap,
                action,
                total,
                new LinkedHashMap<>(dsContextLevels),
                new ArrayList<>(dsSteps),
                new ArrayList<>(dsLocators)
            ));
        }

        long totalDurationMs = 0;
        for (final DatasetMetrics dm : datasetMetricsMap.values())
        {
            totalDurationMs += dm.durationMs();
        }

        final TokenStats overallPesap = new TokenStats(overallPesapPrompt, overallPesapOutput, overallPesapTotal, overallPesapCount);
        final TokenStats overallAction = new TokenStats(overallActionPrompt, overallActionOutput, overallActionTotal, overallActionCount);
        final TokenStats overallTotal = new TokenStats(
            overallPesapPrompt + overallActionPrompt,
            overallPesapOutput + overallActionOutput,
            overallPesapTotal + overallActionTotal,
            overallPesapCount + overallActionCount
        );

        return new LogAnalysis(
            label,
            datasetMetricsMap,
            overallPesap,
            overallAction,
            overallTotal,
            totalDurationMs,
            overallContextLevels,
            allLocators
        );
    }

    private static long calculateDuration(final String startStr, final String endStr)
    {
        if (startStr == null || endStr == null)
        {
            return 0;
        }
        try
        {
            final LocalDateTime start = LocalDateTime.parse(startStr, TIMESTAMP_FORMATTER);
            final LocalDateTime end = LocalDateTime.parse(endStr, TIMESTAMP_FORMATTER);
            return Duration.between(start, end).toMillis();
        }
        catch (final Exception e)
        {
            return 0;
        }
    }

    private static void printComparisonReport(final LogAnalysis oldLog, final LogAnalysis newLog)
    {
        System.out.println("====================================================================================================");
        System.out.println("                         NEODYMIUM AI LOG COMPARISON REPORT");
        System.out.println("====================================================================================================");
        System.out.printf("%-25s | %-32s | %-32s%n", "Metric", oldLog.fileName(), newLog.fileName());
        System.out.println("----------------------------------------------------------------------------------------------------");

        // 1. Overall Token Summary
        printHeader("1. OVERALL TOKEN USAGE & COST (Gemini 3.5 Flash Lite)");
        printRow("PESAP Calls", String.valueOf(oldLog.overallPesapStats().callCount()), String.valueOf(newLog.overallPesapStats().callCount()));
        printRow("PESAP Prompt Tokens", String.format("%,d", oldLog.overallPesapStats().promptTokens()), String.format("%,d", newLog.overallPesapStats().promptTokens()));
        printRow("PESAP Output Tokens", String.format("%,d", oldLog.overallPesapStats().outputTokens()), String.format("%,d", newLog.overallPesapStats().outputTokens()));
        printRow("PESAP Total Tokens", String.format("%,d", oldLog.overallPesapStats().totalTokens()), String.format("%,d", newLog.overallPesapStats().totalTokens()));
        
        printRow("Action LLM Calls", String.valueOf(oldLog.overallActionStats().callCount()), String.valueOf(newLog.overallActionStats().callCount()));
        printRow("Action Prompt Tokens", String.format("%,d", oldLog.overallActionStats().promptTokens()), String.format("%,d", newLog.overallActionStats().promptTokens()));
        printRow("Action Output Tokens", String.format("%,d", oldLog.overallActionStats().outputTokens()), String.format("%,d", newLog.overallActionStats().outputTokens()));
        printRow("Action Total Tokens", String.format("%,d", oldLog.overallActionStats().totalTokens()), String.format("%,d", newLog.overallActionStats().totalTokens()));

        printRow("TOTAL Prompt Tokens", String.format("%,d", oldLog.overallTotalStats().promptTokens()), String.format("%,d", newLog.overallTotalStats().promptTokens()));
        printRow("TOTAL Output Tokens", String.format("%,d", oldLog.overallTotalStats().outputTokens()), String.format("%,d", newLog.overallTotalStats().outputTokens()));
        printRow("TOTAL Tokens", String.format("%,d", oldLog.overallTotalStats().totalTokens()), String.format("%,d", newLog.overallTotalStats().totalTokens()));
        
        final double oldCost = oldLog.overallTotalStats().calculateCost();
        final double newCost = newLog.overallTotalStats().calculateCost();
        final double costDiffPct = oldCost > 0 ? ((newCost - oldCost) / oldCost) * 100.0 : 0;
        printRow("ESTIMATED LLM COST ($)", String.format("$%.5f", oldCost), String.format("$%.5f (%+.2f%%)", newCost, costDiffPct));

        // 2. Execution Time
        printHeader("2. EXECUTION TIME & DURATION");
        printRow("Total Wall-Clock Time", formatDuration(oldLog.totalDurationMs()), formatDuration(newLog.totalDurationMs()));

        // 3. Dataset Breakdown
        printHeader("3. DATASET BREAKDOWN");
        for (final String ds : oldLog.datasetMetricsMap().keySet())
        {
            final DatasetMetrics oldDs = oldLog.datasetMetricsMap().get(ds);
            final DatasetMetrics newDs = newLog.datasetMetricsMap().get(ds);
            if (oldDs != null && newDs != null)
            {
                System.out.println("  ▸ Dataset: " + ds);
                printRow("    - Total Tokens", String.format("%,d", oldDs.totalStats().totalTokens()), String.format("%,d", newDs.totalStats().totalTokens()));
                printRow("    - Action Prompt Tokens", String.format("%,d", oldDs.actionStats().promptTokens()), String.format("%,d", newDs.actionStats().promptTokens()));
                printRow("    - Duration", formatDuration(oldDs.durationMs()), formatDuration(newDs.durationMs()));
            }
        }

        // 4. Context Level Distribution
        printHeader("4. CONTEXT LEVEL SELECTIONS");
        final Map<String, Integer> allContextKeys = new LinkedHashMap<>(oldLog.overallContextLevels());
        allContextKeys.putAll(newLog.overallContextLevels());
        for (final String key : allContextKeys.keySet())
        {
            printRow("Context: " + key, String.valueOf(oldLog.overallContextLevels().getOrDefault(key, 0)), String.valueOf(newLog.overallContextLevels().getOrDefault(key, 0)));
        }

        // 5. Locator Analysis
        printHeader("5. LOCATOR QUALITY ANALYSIS");
        printLocatorAnalysis("OLD Log Locators", oldLog.allLocators());
        System.out.println("----------------------------------------------------------------------------------------------------");
        printLocatorAnalysis("NEW Log Locators", newLog.allLocators());

        // 6. Direct Locator Comparison per Step
        printHeader("6. DIRECT STEP LOCATOR COMPARISON");
        compareLocatorsPerStep(oldLog, newLog);

        System.out.println("====================================================================================================");
    }

    private static void printHeader(final String title)
    {
        System.out.println("----------------------------------------------------------------------------------------------------");
        System.out.println(" " + title);
        System.out.println("----------------------------------------------------------------------------------------------------");
    }

    private static void printRow(final String label, final String oldVal, final String newVal)
    {
        System.out.printf("%-25s | %-32s | %-32s%n", label, oldVal, newVal);
    }

    private static String formatDuration(final long ms)
    {
        final long seconds = ms / 1000;
        final long minutes = seconds / 60;
        final long remSec = seconds % 60;
        return String.format("%dm %02ds (%d ms)", minutes, remSec, ms);
    }

    private static void printLocatorAnalysis(final String label, final List<String> locators)
    {
        int dataAiCount = 0;
        int dataTestCount = 0;
        int idCount = 0;
        int nameCount = 0;
        int ariaCount = 0;
        int classCount = 0;
        int textCount = 0;
        int urlCount = 0;

        for (final String loc : locators)
        {
            if (loc.contains("data-ai")) dataAiCount++;
            else if (loc.contains("data-test") || loc.contains("data-testid")) dataTestCount++;
            else if (loc.contains("#") || loc.contains("id=")) idCount++;
            else if (loc.contains("name=")) nameCount++;
            else if (loc.contains("aria-label")) ariaCount++;
            else if (loc.startsWith(".") || loc.contains(" .") || loc.contains("class")) classCount++;
            else if (loc.contains("text=") || loc.contains(":contains")) textCount++;
            else if (loc.startsWith("http")) urlCount++;
        }

        System.out.printf("%s (Total Captured: %d)%n", label, locators.size());
        System.out.printf("  - Data-AI Fallback Locators:  %2d (%4.1f%%)%n", dataAiCount, locators.isEmpty() ? 0 : (dataAiCount * 100.0 / locators.size()));
        System.out.printf("  - Semantic Data-Test/ID:     %2d (%4.1f%%)%n", dataTestCount, locators.isEmpty() ? 0 : (dataTestCount * 100.0 / locators.size()));
        System.out.printf("  - ID Locators (#id):          %2d (%4.1f%%)%n", idCount, locators.isEmpty() ? 0 : (idCount * 100.0 / locators.size()));
        System.out.printf("  - Name / Aria Locators:       %2d (%4.1f%%)%n", nameCount + ariaCount, locators.isEmpty() ? 0 : ((nameCount + ariaCount) * 100.0 / locators.size()));
        System.out.printf("  - Class Selectors:            %2d (%4.1f%%)%n", classCount, locators.isEmpty() ? 0 : (classCount * 100.0 / locators.size()));
    }

    private static void compareLocatorsPerStep(final LogAnalysis oldLog, final LogAnalysis newLog)
    {
        for (final String ds : oldLog.datasetMetricsMap().keySet())
        {
            final DatasetMetrics oldDs = oldLog.datasetMetricsMap().get(ds);
            final DatasetMetrics newDs = newLog.datasetMetricsMap().get(ds);
            if (oldDs == null || newDs == null) continue;

            System.out.println("\n  [Dataset: " + ds + "]");
            final int maxSteps = Math.min(oldDs.steps().size(), newDs.steps().size());
            for (int i = 0; i < maxSteps; i++)
            {
                final StepInfo oldStep = oldDs.steps().get(i);
                final StepInfo newStep = newDs.steps().get(i);

                if (!oldStep.locator().equalsIgnoreCase(newStep.locator()))
                {
                    System.out.printf("    Step %d: \"%s\"%n", oldStep.stepIndex(), truncate(oldStep.instruction(), 60));
                    System.out.printf("      OLD: %s%n", oldStep.locator());
                    System.out.printf("      NEW: %s%n", newStep.locator());
                }
            }
        }
    }

    private static String truncate(final String str, final int maxLen)
    {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen - 3) + "..." : str;
    }
}
