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
package org.neodymium.common.xtc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SurefireResultParser
{
    // Pattern to match test results summary
    private static final Pattern SUMMARY_PATTERN = Pattern.compile("Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)");

    private int totalTests;

    private int failedTests;

    private int skippedTests;

    private int brokenTests;

    private int passedTests;

    private static final Logger LOGGER = LoggerFactory.getLogger(RunInitializer.class);

    /**
     * Parses the results of the Surefire test reports located in the specified directory.
     *
     * @param surefireReportPath
     *     the path to the directory containing the Surefire test reports
     * @return a TestRunStatistics object containing the parsed and combined statistics
     */
    public TestRunStatistics parseResults(String surefireReportPath)
    {
        Path reportsPath = Paths.get(surefireReportPath);

        try (Stream<Path> files = Files.walk(reportsPath))
        {
            // Filter for regular files with .txt extension and not starting with "TEST-" and parse them
            files.filter(Files::isRegularFile)
                 .filter(path -> path.getFileName().toString().endsWith(".txt"))
                 .filter(path -> !path.getFileName().toString().startsWith("TEST-"))
                 .forEach(this::parseTestResultFile);
        }
        catch (IOException e)
        {
            LOGGER.error("Error reading reports directory: {}", e.getMessage(), e);
        }

        return new TestRunStatistics(this.totalTests, this.failedTests, this.skippedTests, this.brokenTests, this.passedTests);
    }

    /**
     * Parses a single test result file and updates the statistics.
     *
     * @param filePath
     *     the path to the test result file
     */
    private void parseTestResultFile(Path filePath)
    {
        try
        {
            List<String> lines = Files.readAllLines(filePath);

            // process each line of the result file
            for (String line : lines)
            {
                Matcher summaryMatcher = SUMMARY_PATTERN.matcher(line);
                if (summaryMatcher.find())
                {
                    int testsRun = Integer.parseInt(summaryMatcher.group(1));
                    int failures = Integer.parseInt(summaryMatcher.group(2));
                    int errors = Integer.parseInt(summaryMatcher.group(3));
                    int skipped = Integer.parseInt(summaryMatcher.group(4));

                    int passed = testsRun - failures - errors - skipped;

                    this.totalTests += testsRun;
                    this.failedTests += failures;
                    this.skippedTests += skipped;
                    this.brokenTests += errors;
                    this.passedTests += passed;
                }
            }
        }
        catch (IOException e)
        {
            LOGGER.error("Error reading file {}: {}", filePath, e.getMessage(), e);
        }
    }
}
