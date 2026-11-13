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
package com.xceptance.aura.report.staticassets;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Automated regression safety net verifying that all static JavaScript assets
 * in {@code src/main/resources/static/js/} are syntactically valid.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class StaticResourceSyntaxTest
{
    @Test
    public void testAllStaticJavaScriptAssetsHaveValidSyntax() throws Exception
    {
        final Path staticDirPath = Paths.get("src/main/resources/static");
        Assertions.assertTrue(Files.exists(staticDirPath), "Static directory must exist: " + staticDirPath.toAbsolutePath());

        final List<File> jsFiles;
        try (final Stream<Path> stream = Files.walk(staticDirPath))
        {
            jsFiles = stream
                .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".js"))
                .map(Path::toFile)
                .sorted()
                .collect(Collectors.toList());
        }

        Assertions.assertFalse(jsFiles.isEmpty(), "At least one JavaScript file should be present in " + staticDirPath);

        final boolean nodeAvailable = isNodeAvailable();

        for (final File jsFile : jsFiles)
        {
            Assertions.assertTrue(jsFile.length() > 0, "JavaScript file must not be empty: " + jsFile.getName());

            if (nodeAvailable)
            {
                verifySyntaxWithNode(jsFile);
            }
            else
            {
                verifyBraceBalance(jsFile);
            }
        }
    }

    private static final Object NODE_CHECK_LOCK = new Object();
    private static volatile Boolean nodeAvailableCached = null;

    private boolean isNodeAvailable()
    {
        if (nodeAvailableCached != null)
        {
            return nodeAvailableCached.booleanValue();
        }

        synchronized (NODE_CHECK_LOCK)
        {
            if (nodeAvailableCached != null)
            {
                return nodeAvailableCached.booleanValue();
            }

            try
            {
                final Process process = new ProcessBuilder("node", "-v")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
                final boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                if (!finished)
                {
                    process.destroyForcibly();
                    return false;
                }
                nodeAvailableCached = (process.exitValue() == 0);
            }
            catch (final Exception e)
            {
                nodeAvailableCached = Boolean.FALSE;
            }
            return nodeAvailableCached != null && nodeAvailableCached.booleanValue();
        }
    }

    private static Path resolveTempDirectory(final Path baseDir)
    {
        try
        {
            final Path candidate = baseDir.resolve("target").resolve("temp-node-tests");
            Files.createDirectories(candidate);
            return candidate;
        }
        catch (final Exception ignored)
        {
            return null;
        }
    }

    private static void deleteSilently(final Path path)
    {
        if (path != null)
        {
            try
            {
                Files.deleteIfExists(path);
            }
            catch (final IOException ignored)
            {
                // Prevent masking primary test assertion failures on Windows file locks
            }
        }
    }

    private void runNodeScript(final String nodeTestScript, final String failureMessagePrefix) throws Exception
    {
        if (!isNodeAvailable())
        {
            return;
        }

        final Path baseDir = Files.exists(Paths.get("src/main/resources"))
            ? Paths.get(".").toAbsolutePath().normalize()
            : Paths.get("aura-manager").toAbsolutePath().normalize();

        final Path tempDir = resolveTempDirectory(baseDir);
        final Path tempScript = (tempDir != null)
            ? Files.createTempFile(tempDir, "node-test-", ".js")
            : Files.createTempFile("node-test-", ".js");
        final Path tempOutput = (tempDir != null)
            ? Files.createTempFile(tempDir, "node-out-", ".log")
            : Files.createTempFile("node-out-", ".log");

        try
        {
            Files.writeString(tempScript, nodeTestScript, StandardCharsets.UTF_8);

            final ProcessBuilder pb = new ProcessBuilder("node", tempScript.toAbsolutePath().toString());
            pb.directory(baseDir.toFile());
            pb.redirectErrorStream(true);
            pb.redirectOutput(tempOutput.toFile());

            final Process process = pb.start();
            try
            {
                boolean finished = false;
                try
                {
                    finished = process.waitFor(30, TimeUnit.SECONDS);
                }
                catch (final InterruptedException e)
                {
                    process.destroyForcibly();
                    Thread.currentThread().interrupt();
                    Assertions.fail(failureMessagePrefix + " was interrupted while executing Node script", e);
                }

                if (!finished)
                {
                    process.destroyForcibly();
                    try
                    {
                        process.waitFor(5, TimeUnit.SECONDS);
                    }
                    catch (final InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }
                    Assertions.fail(failureMessagePrefix + " timed out after 30 seconds");
                }

                final int exitCode = process.exitValue();
                final String rawOutput = Files.readString(tempOutput, StandardCharsets.UTF_8);
                final String trimmedOutput = rawOutput.strip();
                final String diagnosticDetails = trimmedOutput.isEmpty()
                    ? "(No output produced, exit code: " + exitCode + ")"
                    : trimmedOutput;

                Assertions.assertEquals(0, exitCode, failureMessagePrefix + " (exit code " + exitCode + "):\n" + diagnosticDetails);
            }
            finally
            {
                if (process.isAlive())
                {
                    process.destroyForcibly();
                }
            }
        }
        finally
        {
            deleteSilently(tempScript);
            deleteSilently(tempOutput);
        }
    }

    private void verifySyntaxWithNode(final File jsFile) throws Exception
    {
        final ProcessBuilder pb = new ProcessBuilder("node", "-c", jsFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        final Process process = pb.start();

        final StringBuilder output = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                output.append(line).append("\n");
            }
        }

        final boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        if (!finished)
        {
            process.destroyForcibly();
            Assertions.fail("Syntax check timed out for " + jsFile.getName());
        }

        final int exitCode = process.exitValue();
        Assertions.assertEquals(0, exitCode, "Syntax error detected in " + jsFile.getName() + ":\n" + output);
    }

    private void verifyBraceBalance(final File jsFile) throws Exception
    {
        final String content = Files.readString(jsFile.toPath(), StandardCharsets.UTF_8);
        int braceCount = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean escaped = false;

        for (int i = 0; i < content.length(); i++)
        {
            final char c = content.charAt(i);

            if (escaped)
            {
                escaped = false;
                continue;
            }

            if (c == '\\')
            {
                escaped = true;
                continue;
            }

            if (inLineComment)
            {
                if (c == '\n')
                {
                    inLineComment = false;
                }
                continue;
            }

            if (inBlockComment)
            {
                if (c == '*' && i + 1 < content.length() && content.charAt(i + 1) == '/')
                {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && !inBacktick)
            {
                if (c == '/' && i + 1 < content.length())
                {
                    final char next = content.charAt(i + 1);
                    if (next == '/')
                    {
                        inLineComment = true;
                        i++;
                        continue;
                    }
                    else if (next == '*')
                    {
                        inBlockComment = true;
                        i++;
                        continue;
                    }
                }
            }

            if (c == '\'' && !inDoubleQuote && !inBacktick)
            {
                inSingleQuote = !inSingleQuote;
            }
            else if (c == '"' && !inSingleQuote && !inBacktick)
            {
                inDoubleQuote = !inDoubleQuote;
            }
            else if (c == '`' && !inSingleQuote && !inDoubleQuote)
            {
                inBacktick = !inBacktick;
            }
            else if (!inSingleQuote && !inDoubleQuote && !inBacktick)
            {
                if (c == '{')
                {
                    braceCount++;
                }
                else if (c == '}')
                {
                    braceCount--;
                }
            }
        }

        Assertions.assertEquals(0, braceCount, "Brace mismatch detected in " + jsFile.getName());
    }

    @Test
    public void testLegendBoxPatternStylesNotOverriddenByBackgroundShorthand() throws Exception
    {
        final Path templatePath = Paths.get("src/main/resources/templates/fragments/batch-history.html");
        Assertions.assertTrue(Files.exists(templatePath), "Template must exist: " + templatePath.toAbsolutePath());

        final String templateContent = Files.readString(templatePath, StandardCharsets.UTF_8);
        Assertions.assertFalse(
            templateContent.contains("class=\"legend-box seg-ai-print\" style=\"background:")
            || templateContent.contains("class=\"legend-box seg-healed-print\" style=\"background:"),
            "Patterned legend boxes must not use shorthand 'background:' which overrides background-image. Use 'background-color:'."
        );

        final Path cssPath = Paths.get("src/main/resources/static/css/report-manager.css");
        Assertions.assertTrue(Files.exists(cssPath), "CSS file must exist: " + cssPath.toAbsolutePath());

        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);
        Assertions.assertTrue(
            cssContent.contains(".legend-box.seg-ai-print") && cssContent.contains("repeating-linear-gradient"),
            "CSS must define .legend-box.seg-ai-print repeating linear gradient pattern."
        );
        Assertions.assertTrue(
            cssContent.contains(".legend-box.seg-healed-print") && cssContent.contains("radial-gradient"),
            "CSS must define .legend-box.seg-healed-print radial gradient sparkle pattern."
        );
    }

    @Test
    public void testSymmetricPatternLegendAndStatusBreakdownTemplates() throws Exception
    {
        final Path detailsPath = Paths.get("src/main/resources/templates/fragments/batch-details.html");
        Assertions.assertTrue(Files.exists(detailsPath), "batch-details.html must exist: " + detailsPath.toAbsolutePath());
        final String detailsContent = Files.readString(detailsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            detailsContent.contains("run.fixedCleanCount")
            && detailsContent.contains("run.fixedHealedCountSafe")
            && detailsContent.contains("run.fixedAiCountSafe")
            && detailsContent.contains("run.knownCleanCount")
            && detailsContent.contains("run.knownHealedCountSafe")
            && detailsContent.contains("run.knownAiCountSafe"),
            "batch-details.html must render granular clean, healed, and AI-driven segments for known tests."
        );
    }

    @Test
    public void testAreaTrendSvgLayersIncludeKnownAndFixedAiPaths() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        final String[] expectedLayerClasses = {
            "area-layer-known-ai",
            "area-layer-known-ai-pat",
            "area-layer-fixed-ai",
            "area-layer-fixed-ai-pat"
        };
        for (final String layerClass : expectedLayerClasses)
        {
            Assertions.assertTrue(
                jsContent.contains(layerClass),
                "report-manager.js must contain layer class '" + layerClass + "'"
            );
        }
    }

    @Test
    public void testRunReportStatusFilterStateTransitions() throws Exception
    {
        final Path indexPath = Paths.get("src/main/resources/templates/index.html");
        Assertions.assertTrue(Files.exists(indexPath), "index.html must exist: " + indexPath.toAbsolutePath());
        final String indexContent = Files.readString(indexPath, StandardCharsets.UTF_8);
        Assertions.assertTrue(
            indexContent.contains("report-manager.js?v=1.1.7"),
            "index.html must reference report-manager.js with cache buster ?v=1.1.7"
        );
        Assertions.assertTrue(
            indexContent.contains("dashboard-styles.css?v=1.2.7"),
            "index.html must reference dashboard-styles.css with cache buster ?v=1.2.7"
        );
        Assertions.assertTrue(
            indexContent.contains("report-manager.css?v=1.1.15"),
            "index.html must reference report-manager.css with cache buster ?v=1.1.15"
        );

        if (!isNodeAvailable())
        {
            return;
        }

        final Path jsFilePath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsFilePath), "report-manager.js must exist: " + jsFilePath.toAbsolutePath());

        final String nodeTestScript =
            "const fs = require('fs');\n" +
            "const vm = require('vm');\n" +
            "const code = fs.readFileSync('" + jsFilePath.toAbsolutePath().toString().replace("\\", "/") + "', 'utf8');\n" +
            "const testScript = code + '\\n' + `\n" +
            "function makeEl(tag) {\n" +
            "    return {\n" +
            "        tagName: tag,\n" +
            "        style: {},\n" +
            "        classList: { add: () => {}, remove: () => {}, contains: () => false },\n" +
            "        getAttribute: () => '',\n" +
            "        setAttribute: () => {},\n" +
            "        removeAttribute: () => {},\n" +
            "        querySelectorAll: () => [],\n" +
            "        querySelector: () => null,\n" +
            "        closest: () => null\n" +
            "    };\n" +
            "}\n" +
            "function makeRow(status) {\n" +
            "    const el = makeEl('TR');\n" +
            "    el.getAttribute = (attr) => attr === 'data-status' ? status : '';\n" +
            "    el.innerText = status;\n" +
            "    return el;\n" +
            "}\n" +
            "const row1 = makeRow('PASSED');\n" +
            "const row2 = makeRow('FAILED_KNOWN');\n" +
            "const row3 = makeRow('FAILED_UNKNOWN');\n" +
            "const allRows = [row1, row2, row3];\n" +
            "document.getElementById = (id) => makeEl('DIV');\n" +
            "document.querySelectorAll = (sel) => sel.includes('.test-row') ? allRows : [];\n" +
            "applyRunReportFilters();\n" +
            "if (row1.style.display !== '' || row2.style.display !== '' || row3.style.display !== '') {\n" +
            "    throw new Error('Initial rows display mismatch');\n" +
            "}\n" +
            "filterAllTestsByStatus('PASSED');\n" +
            "if (currentGlobalStatusBadgeFilter !== 'PASSED' || activeWholeExecutionFilter !== 'PASSED') {\n" +
            "    throw new Error('Step 1 state mismatch: ' + currentGlobalStatusBadgeFilter + ', ' + activeWholeExecutionFilter);\n" +
            "}\n" +
            "if (row1.style.display !== '' || row2.style.display !== 'none' || row3.style.display !== 'none') {\n" +
            "    throw new Error('Step 1 row display mismatch');\n" +
            "}\n" +
            "filterByGlobalStatusBadge('PASSED');\n" +
            "if (currentGlobalStatusBadgeFilter !== 'ALL' || activeWholeExecutionFilter !== null) {\n" +
            "    throw new Error('Step 2 (deselect) state mismatch: ' + currentGlobalStatusBadgeFilter + ', ' + activeWholeExecutionFilter);\n" +
            "}\n" +
            "if (row1.style.display !== '' || row2.style.display !== '' || row3.style.display !== '') {\n" +
            "    throw new Error('Step 2 (deselect) row display mismatch');\n" +
            "}\n" +
            "filterAllTestsByStatus('PASSED');\n" +
            "filterByGlobalStatusBadge('FAILED_KNOWN');\n" +
            "if (currentGlobalStatusBadgeFilter !== 'FAILED_KNOWN' || activeWholeExecutionFilter !== 'FAILED_KNOWN') {\n" +
            "    throw new Error('Step 3 (switch) state mismatch: ' + currentGlobalStatusBadgeFilter + ', ' + activeWholeExecutionFilter);\n" +
            "}\n" +
            "if (row1.style.display !== 'none' || row2.style.display !== '' || row3.style.display !== 'none') {\n" +
            "    throw new Error('Step 3 (switch) row display mismatch');\n" +
            "}\n" +
            "filterAllTestsByStatus('ALL');\n" +
            "if (currentGlobalStatusBadgeFilter !== 'ALL' || activeWholeExecutionFilter !== null) {\n" +
            "    throw new Error('Step 4 (ALL) state mismatch: ' + currentGlobalStatusBadgeFilter + ', ' + activeWholeExecutionFilter);\n" +
            "}\n" +
            "if (row1.style.display !== '' || row2.style.display !== '' || row3.style.display !== '') {\n" +
            "    throw new Error('Step 4 (ALL) row display mismatch');\n" +
            "}\n" +
            "`;\n" +
            "const sandbox = {\n" +
            "    console,\n" +
            "    window: {},\n" +
            "    document: {\n" +
            "        getElementById: () => null,\n" +
            "        querySelectorAll: () => [],\n" +
            "        querySelector: () => null,\n" +
            "        addEventListener: () => {},\n" +
            "        body: { addEventListener: () => {} }\n" +
            "    }\n" +
            "};\n" +
            "sandbox.window = sandbox;\n" +
            "vm.runInNewContext(testScript, sandbox);\n";

        runNodeScript(nodeTestScript, "Filter state transition test failed");
    }

    /**
     * Automated regression safety net verifying that dashboard-runner.js correctly filters
     * terminal log lines: retaining modern AI events (thoughts, tool calls, results, turns) and
     * continuation lines while suppressing Maven noise, JVM warnings, and tool schema dumps.
     *
     * @throws Exception
     *             if Node execution or verification fails
     * @author AI-generated: Gemini 3.8 Flash
     * @author Xceptance GmbH 2026
     */
    @Test
    public void testDashboardRunnerAiLogFilter() throws Exception
    {
        if (!isNodeAvailable())
        {
            return;
        }

        final Path runnerPath = Paths.get("src/main/resources/static/dashboard-runner.js");
        Assertions.assertTrue(Files.exists(runnerPath), "dashboard-runner.js must exist: " + runnerPath.toAbsolutePath());

        final String nodeTestScript =
            "const fs = require('fs');\n" +
            "const vm = require('vm');\n" +
            "const runnerCode = fs.readFileSync('" + runnerPath.toAbsolutePath().toString().replace("\\", "/") + "', 'utf8');\n" +
            "const sandbox = {\n" +
            "    console,\n" +
            "    window: { addEventListener: () => {} },\n" +
            "    document: {\n" +
            "        getElementById: () => ({ innerHTML: '', querySelectorAll: () => [], insertAdjacentHTML: () => {} }),\n" +
            "        querySelectorAll: () => [],\n" +
            "        querySelector: () => null,\n" +
            "        addEventListener: () => {}\n" +
            "    },\n" +
            "    localStorage: { getItem: () => null, setItem: () => {} },\n" +
            "    setTimeout: () => {}\n" +
            "};\n" +
            "sandbox.window.document = sandbox.document;\n" +
            "sandbox.window.localStorage = sandbox.localStorage;\n" +
            "vm.runInNewContext(runnerCode, sandbox);\n" +
            "const isAiLogLine = sandbox.window.isAiLogLine;\n" +
            "const aiLines = [\n" +
            "    '13:30:00 ╔════════════════════════════════════════════════════════════════════════════════════',\n" +
            "    '13:30:00 ║ 🚀 STARTING TEST CASE: cart/add_item.yaml',\n" +
            "    '13:30:00 ╚════════════════════════════════════════════════════════════════════════════════════',\n" +
            "    '13:30:00 ────────────────────────────────────────────────────────────────────────────────',\n" +
            "    '13:30:01 🤖 Agent Turn #1 (max: 25) | Step: \"Search for jeans\"',\n" +
            "    '13:30:03 💭 Thought:',\n" +
            "    '13:30:03 🔧 Tool Call: type',\n" +
            "    '13:30:04 📥 Result [type]: 🏁 SUCCESS - typed into \\'#search\\'',\n" +
            "    '13:30:10 🎯 Goal Accomplished: Found jeans (Turns: 1, Executed Calls: 1)',\n" +
            "    '13:30:10 📊 Tokens: 1,200 in (800 cached) → 45 out (total: 1,245) | Turn 1',\n" +
            "    '13:30:10 ║ 🏁 TEST CASE COMPLETED: SUCCESS',\n" +
            "    '13:30:10 🚨 [Visual RCA Diagnosis]: Button was covered by banner',\n" +
            "    '13:30:10 ⚖️ Quality Judge advised retry'\n" +
            "];\n" +
            "for (const line of aiLines) {\n" +
            "    if (!isAiLogLine(line, false)) {\n" +
            "        throw new Error('Expected AI line to match: ' + line);\n" +
            "    }\n" +
            "}\n" +
            "if (!isAiLogLine('   I will search for jeans and submit the query.', true)) {\n" +
            "    throw new Error('Expected thought continuation to match');\n" +
            "}\n" +
            "if (!isAiLogLine('   • target: #search-input', true)) {\n" +
            "    throw new Error('Expected tool call argument continuation to match');\n" +
            "}\n" +
            "if (isAiLogLine('   I will search for jeans and submit the query.', false)) {\n" +
            "    throw new Error('Expected unanchored indented line to NOT match when previous was not AI');\n" +
            "}\n" +
            "const allowsAiContinuation = sandbox.window.allowsAiContinuation;\n" +
            "if (typeof allowsAiContinuation !== 'function') {\n" +
            "    throw new Error('Expected window.allowsAiContinuation to be defined');\n" +
            "}\n" +
            "if (allowsAiContinuation('16:17:11 🤖 Agent Turn #1 (max: 15) | Step: \"Search jeans\"')) {\n" +
            "    throw new Error('Agent Turn header should NOT allow continuation lines');\n" +
            "}\n" +
            "if (!allowsAiContinuation('16:17:12 💭 Thought:')) {\n" +
            "    throw new Error('Thought header should allow continuation lines');\n" +
            "}\n" +
            "if (!allowsAiContinuation('16:17:13 🔧 Tool Call: fill')) {\n" +
            "    throw new Error('Tool Call header should allow continuation lines');\n" +
            "}\n" +
            "const wireLines = [\n" +
            "    '      \"text\" : \"You are an autonomous web testing agent. Execute the test instruction...\",',\n" +
            "    '      \"role\" : \"user\",',\n" +
            "    '      \"parts\" : [',\n" +
            "    '  {',\n" +
            "    '  }',\n" +
            "    'Request:',\n" +
            "    'Response:',\n" +
            "    'Host: generativelanguage.googleapis.com',\n" +
            "    'Content-Type: application/json',\n" +
            "    '- method: POST',\n" +
            "    '16:17:11 [DEBUG] dev.langchain4j.http.client.log.LoggingHttpClient - Request:'\n" +
            "];\n" +
            "for (const wireLine of wireLines) {\n" +
            "    if (isAiLogLine(wireLine, true)) {\n" +
            "        throw new Error('Expected wire log line to be suppressed even with previousWasAi=true: ' + wireLine);\n" +
            "    }\n" +
            "}\n" +
            "const noiseLines = [\n" +
            "    '[INFO] Scanning for projects...',\n" +
            "    '[INFO] Compiling 758 source files with javac [debug target 17] to target/test-classes',\n" +
            "    'WARNING: A terminally deprecated method in sun.misc.Unsafe has been called',\n" +
            "    'WARNING: sun.misc.Unsafe::objectFieldOffset has been called by ...',\n" +
            "    'SLF4J(W): Class path contains multiple SLF4J providers.',\n" +
            "    '[INFO] Running YAML Test: cart/add_item.yaml',\n" +
            "    '[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0',\n" +
            "    '[INFO] BUILD SUCCESS',\n" +
            "    '[INFO] Total time: 12.5 s',\n" +
            "    '13:30:00 [DEBUG] 🛠️ Available Native Tools (25):',\n" +
            "    '    ActionTool(name=\"click\", description=\"Click an element\", params=...)',\n" +
            "    'Conversation context for Turn #1:',\n" +
            "    'This test uses Neodymium Library (version: ?.?.?)'\n" +
            "];\n" +
            "for (const line of noiseLines) {\n" +
            "    if (isAiLogLine(line, false)) {\n" +
            "        throw new Error('Expected noise line to be suppressed: ' + line);\n" +
            "    }\n" +
            "}\n";

        runNodeScript(nodeTestScript, "AI log filtering test failed");
    }

    @Test
    public void testCollapsedSidebarPreservesMaterialIcons() throws Exception
    {
        final Path cssPath = Paths.get("src/main/resources/static/css/report-manager.css");
        Assertions.assertTrue(Files.exists(cssPath), "CSS file must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            cssContent.contains("aside.collapsed .nav-item-btn span:not(.material-symbols-outlined)"),
            "report-manager.css must preserve .material-symbols-outlined inside collapsed .nav-item-btn"
        );
        Assertions.assertTrue(
            cssContent.contains("aside.collapsed .nav-label"),
            "report-manager.css must hide .nav-label in collapsed sidebar"
        );
        Assertions.assertFalse(
            cssContent.contains("aside.collapsed .nav-item-btn span {"),
            "report-manager.css must not unconditionally hide all span elements in collapsed nav buttons"
        );
    }

    @Test
    public void testSidebarNavItemsHaveExplicitLabelsAndMaterialIcons() throws Exception
    {
        final Path sidebarPath = Paths.get("src/main/resources/templates/fragments/sidebar.html");
        Assertions.assertTrue(Files.exists(sidebarPath), "sidebar.html must exist: " + sidebarPath.toAbsolutePath());
        final String sidebarContent = Files.readString(sidebarPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            sidebarContent.contains("class=\"logo-brand\">Neodymium</span>"),
            "sidebar.html must render brand label 'Neodymium'"
        );
        Assertions.assertTrue(
            sidebarContent.contains("class=\"logo-title\">Aura Manager</span>"),
            "sidebar.html must render title label 'Aura Manager'"
        );

        final String[] expectedIcons = {"play_circle"};
        for (final String icon : expectedIcons)
        {
            Assertions.assertTrue(
                sidebarContent.contains("<span class=\"material-symbols-outlined\">" + icon + "</span>"),
                "sidebar.html must contain icon: " + icon
            );
        }

        final String[] expectedLabels = {"Playbooks & Queue"};
        for (final String label : expectedLabels)
        {
            Assertions.assertTrue(
                sidebarContent.contains("<span class=\"nav-label\">" + label + "</span>"),
                "sidebar.html must contain nav-label: " + label
            );
        }

        Assertions.assertTrue(
            sidebarContent.contains("<span>Dashboards</span>"),
            "sidebar.html must render 'Dashboards' section header"
        );
        Assertions.assertFalse(
            sidebarContent.contains("<span>Custom Dashboards</span>"),
            "sidebar.html must not contain old 'Custom Dashboards' section header"
        );
        Assertions.assertFalse(
            sidebarContent.contains("Run History"),
            "sidebar.html must not contain Run History tab"
        );
        Assertions.assertFalse(
            sidebarContent.contains("Test Base"),
            "sidebar.html must not contain Test Base tab"
        );
    }

    @Test
    public void testCustomDashboardsSidebarAddButtonIsUnique() throws Exception
    {
        final Path sidebarPath = Paths.get("src/main/resources/templates/fragments/sidebar.html");
        Assertions.assertTrue(Files.exists(sidebarPath), "sidebar.html must exist: " + sidebarPath.toAbsolutePath());
        final String sidebarContent = Files.readString(sidebarPath, StandardCharsets.UTF_8);

        // Verify the single '+' button next to the section label exists
        Assertions.assertTrue(
            sidebarContent.contains("btn-sidebar-add-page"),
            "sidebar.html must contain the header '+' button (.btn-sidebar-add-page)"
        );

        // Verify the duplicate '+ New Page' card button has been removed
        Assertions.assertFalse(
            sidebarContent.contains("btn-new-page-link"),
            "sidebar.html must NOT contain duplicate button (.btn-new-page-link)"
        );

        final Path modalsPath = Paths.get("src/main/resources/templates/fragments/modals.html");
        Assertions.assertTrue(Files.exists(modalsPath), "modals.html must exist: " + modalsPath.toAbsolutePath());
        final String modalsContent = Files.readString(modalsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            modalsContent.contains("createPageIdentityDimCheckbox"),
            "createPageModal in modals.html must contain execution identity dimension controls"
        );
        Assertions.assertTrue(
            modalsContent.contains("createPageFilterLimitInput"),
            "createPageModal in modals.html must contain run filter limit control"
        );
    }

    @Test
    public void testCustomDashboardWidgetsSingleAddButtonAndNoManageButtons() throws Exception
    {
        final Path customPagePath = Paths.get("src/main/resources/templates/fragments/custom-dashboard-page.html");
        Assertions.assertTrue(Files.exists(customPagePath), "custom-dashboard-page.html must exist");
        final String customPageContent = Files.readString(customPagePath, StandardCharsets.UTF_8);

        final Path dashboardWidgetsPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(dashboardWidgetsPath), "dashboard-widgets.html must exist");
        final String widgetsContent = Files.readString(dashboardWidgetsPath, StandardCharsets.UTF_8);

        final Path modalPath = Paths.get("src/main/resources/templates/fragments/widget-configurator-modal.html");
        Assertions.assertTrue(Files.exists(modalPath), "widget-configurator-modal.html must exist");
        final String modalContent = Files.readString(modalPath, StandardCharsets.UTF_8);

        // Verify no broken openWidgetManagerModal or Manage buttons
        Assertions.assertFalse(customPageContent.contains("openWidgetManagerModal()"), "custom-dashboard-page.html must not contain openWidgetManagerModal");
        Assertions.assertFalse(widgetsContent.contains("openWidgetManagerModal()"), "dashboard-widgets.html must not contain openWidgetManagerModal");
        Assertions.assertFalse(customPageContent.contains("btnManageWidgetsCustomPage"), "custom-dashboard-page.html must not contain btnManageWidgetsCustomPage");
        Assertions.assertFalse(widgetsContent.contains("btnManageWidgetsDashboard"), "dashboard-widgets.html must not contain btnManageWidgetsDashboard");

        // Verify single Add Widget button on custom page
        Assertions.assertTrue(customPageContent.contains("btnAddWidgetCustomPage"), "custom-dashboard-page.html must have btnAddWidgetCustomPage in toolbar");
        Assertions.assertTrue(widgetsContent.contains("th:if=\"${customPage == null}\""), "dashboard-widgets.html must hide btnAddWidgetDashboard when customPage is present");

        // Verify pageId in widget configurator form
        Assertions.assertTrue(modalContent.contains("name=\"pageId\""), "widget-configurator-modal.html must include hidden pageId input");
    }

    @Test
    public void testCustomDashboardGroupCardsDoNotContainBottomActionButtons() throws Exception
    {
        final Path customPagePath = Paths.get("src/main/resources/templates/fragments/custom-dashboard-page.html");
        Assertions.assertTrue(Files.exists(customPagePath), "custom-dashboard-page.html must exist");
        final String customPageContent = Files.readString(customPagePath, StandardCharsets.UTF_8);

        Assertions.assertFalse(customPageContent.contains("<span>Open Dashboard</span>"),
            "custom-dashboard-page.html must not contain bottom 'Open Dashboard' button");
        Assertions.assertFalse(customPageContent.contains("<span>History</span>"),
            "custom-dashboard-page.html must not contain bottom 'History' button");
        Assertions.assertFalse(customPageContent.contains("batch-card-actions"),
            "custom-dashboard-page.html must not contain batch-card-actions container");
        Assertions.assertTrue(customPageContent.contains("onclick=\"if (!event.target.closest('a, button, input, select'))"),
            "custom-dashboard-page.html must have click delegation on batch-card to open group dashboard");
    }

    @Test
    public void testCustomDashboardNoBackToOverviewOnMainDashboardAndPreservesSubDashboardBackLink() throws Exception
    {
        final Path customPagePath = Paths.get("src/main/resources/templates/fragments/custom-dashboard-page.html");
        Assertions.assertTrue(Files.exists(customPagePath), "custom-dashboard-page.html must exist");
        final String customPageContent = Files.readString(customPagePath, StandardCharsets.UTF_8);

        Assertions.assertFalse(
            customPageContent.contains("Back to Overview"),
            "custom-dashboard-page.html must not contain 'Back to Overview' button on main dashboards"
        );
        Assertions.assertTrue(
            customPageContent.contains("th:if=\"${isSubPage == true and parentDashboard != null}\""),
            "custom-dashboard-page.html must preserve sub-dashboard back button leading to parent group dashboard"
        );
    }

    @Test
    public void testBatchHistoryAreaRatioFiltering() throws Exception
    {
        if (!isNodeAvailable())
        {
            return;
        }

        final Path jsFilePath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsFilePath), "report-manager.js must exist: " + jsFilePath.toAbsolutePath());

        final String nodeTestScript =
            "const fs = require('fs');\n" +
            "const vm = require('vm');\n" +
            "const code = fs.readFileSync('" + jsFilePath.toAbsolutePath().toString().replace("\\", "/") + "', 'utf8');\n" +
            "function makeEl(tag, attrs = {}) {\n" +
            "    const attributes = { ...attrs };\n" +
            "    const classes = new Set();\n" +
            "    return {\n" +
            "        tagName: tag,\n" +
            "        style: {},\n" +
            "        classList: {\n" +
            "            add: (...cs) => cs.forEach(c => classes.add(c)),\n" +
            "            remove: (...cs) => cs.forEach(c => classes.delete(c)),\n" +
            "            contains: (c) => classes.has(c)\n" +
            "        },\n" +
            "        getAttribute: (attr) => attributes[attr] || '',\n" +
            "        setAttribute: (attr, val) => { attributes[attr] = String(val); },\n" +
            "        removeAttribute: (attr) => { delete attributes[attr]; },\n" +
            "        querySelectorAll: () => [],\n" +
            "        querySelector: () => null,\n" +
            "        closest: () => null,\n" +
            "        innerText: '',\n" +
            "        title: ''\n" +
            "    };\n" +
            "}\n" +
            "const row1 = makeEl('TR', { 'data-areas': 'checkout', 'data-env': 'DEFAULT', 'data-locales': 'US', 'data-browsers': 'CHROME', 'data-batch': 'DEFAULT' });\n" +
            "const row2 = makeEl('TR', { 'data-areas': 'browsing', 'data-env': 'DEFAULT', 'data-locales': 'US', 'data-browsers': 'CHROME', 'data-batch': 'DEFAULT' });\n" +
            "const row3 = makeEl('TR', { 'data-areas': '@Browsing (default)', 'data-env': 'DEFAULT', 'data-locales': 'US', 'data-browsers': 'CHROME', 'data-batch': 'DEFAULT' });\n" +
            "const row4 = makeEl('TR', { 'data-areas': '', 'data-env': 'DEFAULT', 'data-locales': 'US', 'data-browsers': 'CHROME', 'data-batch': 'DEFAULT' });\n" +
            "const card1 = makeEl('DIV', { 'data-area': 'Checkout' });\n" +
            "const card2 = makeEl('DIV', { 'data-area': 'Browsing' });\n" +
            "const card3 = makeEl('DIV', { 'data-area': 'Browsing (default)' });\n" +
            "const var1 = makeEl('DIV', { 'data-area': 'Checkout', 'data-env': 'DEFAULT', 'data-location': 'US', 'data-browser': 'CHROME', 'data-batch': 'DEFAULT' });\n" +
            "const var2 = makeEl('DIV', { 'data-area': 'Browsing', 'data-env': 'DEFAULT', 'data-location': 'US', 'data-browser': 'CHROME', 'data-batch': 'DEFAULT' });\n" +
            "const var3 = makeEl('DIV', { 'data-area': 'Browsing (default)', 'data-env': 'DEFAULT', 'data-location': 'US', 'data-browser': 'CHROME', 'data-batch': 'DEFAULT' });\n" +
            "const seg1 = makeEl('DIV', { 'data-ratio-type': 'area', 'data-filter-val': '@Checkout' });\n" +
            "seg1.classList.add('ratio-bar-seg');\n" +
            "const seg2 = makeEl('DIV', { 'data-ratio-type': 'area', 'data-filter-val': '@Browsing' });\n" +
            "seg2.classList.add('ratio-bar-seg');\n" +
            "const seg3 = makeEl('DIV', { 'data-ratio-type': 'area', 'data-filter-val': '@Browsing (default)' });\n" +
            "seg3.classList.add('ratio-bar-seg');\n" +
            "const chip1 = makeEl('DIV', { 'data-ratio-type': 'area', 'data-filter-val': '@Checkout' });\n" +
            "chip1.classList.add('ratio-legend-chip');\n" +
            "chip1.querySelector = (sel) => sel === '.val' ? makeEl('SPAN') : null;\n" +
            "const chip2 = makeEl('DIV', { 'data-ratio-type': 'area', 'data-filter-val': '@Browsing' });\n" +
            "chip2.classList.add('ratio-legend-chip');\n" +
            "chip2.querySelector = (sel) => sel === '.val' ? makeEl('SPAN') : null;\n" +
            "const chip3 = makeEl('DIV', { 'data-ratio-type': 'area', 'data-filter-val': '@Browsing (default)' });\n" +
            "chip3.classList.add('ratio-legend-chip');\n" +
            "chip3.querySelector = (sel) => sel === '.val' ? makeEl('SPAN') : null;\n" +
            "const clearBtn = makeEl('BUTTON', { id: 'batchClearAreaFilterBtn' });\n" +
            "const idMap = {\n" +
            "    'batchClearAreaFilterBtn': clearBtn,\n" +
            "    'batchClearBrowserFilterBtn': makeEl('BUTTON'),\n" +
            "    'batchClearLocaleFilterBtn': makeEl('BUTTON'),\n" +
            "    'batchClearEnvFilterBtn': makeEl('BUTTON'),\n" +
            "    'batchClearTagFilterBtn': makeEl('BUTTON')\n" +
            "};\n" +
            "const queryMap = {\n" +
            "    '#batchRunsHistoryTableBody .run-history-row': [row1, row2, row3, row4],\n" +
            "    '.area-trend-grid .area-trend-card': [card1, card2, card3],\n" +
            "    '#batchTestBaseVariationsPanel .tb-entry-row': [var1, var2, var3],\n" +
            "    '#batchLatestVariationsData .batch-latest-var-entry': [var1, var2, var3],\n" +
            "    '.ratio-bar-seg[data-ratio-type=\"area\"], .ratio-legend-chip[data-ratio-type=\"area\"]': [seg1, seg2, seg3, chip1, chip2, chip3],\n" +
            "    '.ratio-legend-chip[data-ratio-type=\"area\"], .ratio-bar-seg[data-ratio-type=\"area\"]': [seg1, seg2, seg3, chip1, chip2, chip3]\n" +
            "};\n" +
            "const sandbox = {\n" +
            "    console,\n" +
            "    window: {},\n" +
            "    document: {\n" +
            "        getElementById: (id) => idMap[id] || null,\n" +
            "        querySelectorAll: (sel) => queryMap[sel] || [],\n" +
            "        querySelector: () => null,\n" +
            "        addEventListener: () => {},\n" +
            "        body: { addEventListener: () => {} }\n" +
            "    }\n" +
            "};\n" +
            "sandbox.window = sandbox;\n" +
            "vm.runInNewContext(code, sandbox);\n" +
            "sandbox.filterFromRatioBar('area', '@Browsing', false);\n" +
            "if (row1.style.display === 'none' || row2.style.display === 'none' || row3.style.display === 'none' || row4.style.display === 'none') {\n" +
            "    throw new Error('Step 1 (@Browsing isolation) runs history must remain unaffected: row1=' + row1.style.display + ', row2=' + row2.style.display);\n" +
            "}\n" +
            "if (card1.style.display === 'none' || card2.style.display === 'none' || card3.style.display === 'none') {\n" +
            "    throw new Error('Step 1 (@Browsing isolation) area trend cards must remain unaffected');\n" +
            "}\n" +
            "if (var2.style.display !== '' || var1.style.display !== 'none' || var3.style.display !== 'none') {\n" +
            "    throw new Error('Step 1 (@Browsing isolation) tb-entry-row filtering failed');\n" +
            "}\n" +
            "if (!seg2.classList.contains('is-active') || seg3.classList.contains('is-active') || !seg3.classList.contains('is-dimmed')) {\n" +
            "    throw new Error('Step 1 (@Browsing isolation) ratio segment active/dimmed class failed');\n" +
            "}\n" +
            "if (clearBtn.style.display !== 'inline-flex') {\n" +
            "    throw new Error('Step 1 clear button display failed');\n" +
            "}\n" +
            "sandbox.filterFromRatioBar('area', '@Browsing (default)', false);\n" +
            "if (row1.style.display === 'none' || row2.style.display === 'none' || row3.style.display === 'none' || row4.style.display === 'none') {\n" +
            "    throw new Error('Step 2 (@Browsing (default) isolation) runs history must remain unaffected: row2=' + row2.style.display + ', row3=' + row3.style.display);\n" +
            "}\n" +
            "if (card1.style.display === 'none' || card2.style.display === 'none' || card3.style.display === 'none') {\n" +
            "    throw new Error('Step 2 (@Browsing (default) isolation) area trend cards must remain unaffected');\n" +
            "}\n" +
            "if (var3.style.display !== '' || var1.style.display !== 'none' || var2.style.display !== 'none') {\n" +
            "    throw new Error('Step 2 (@Browsing (default) isolation) tb-entry-row filtering failed');\n" +
            "}\n" +
            "if (!seg3.classList.contains('is-active') || seg2.classList.contains('is-active') || !seg2.classList.contains('is-dimmed')) {\n" +
            "    throw new Error('Step 2 (@Browsing (default) isolation) ratio segment active/dimmed class failed');\n" +
            "}\n" +
            "sandbox.filterFromRatioBar('area', '@Browsing (default)', false);\n" +
            "if (row1.style.display === 'none' || row2.style.display === 'none' || row3.style.display === 'none' || row4.style.display === 'none') {\n" +
            "    throw new Error('Step 3 (toggle reset) runs history must remain unaffected');\n" +
            "}\n" +
            "if (var1.style.display !== '' || var2.style.display !== '' || var3.style.display !== '') {\n" +
            "    throw new Error('Step 3 (toggle reset) tb-entry-row display failed');\n" +
            "}\n" +
            "if (seg2.classList.contains('is-active') || seg3.classList.contains('is-active')) {\n" +
            "    throw new Error('Step 3 (toggle reset) class cleanup failed');\n" +
            "}\n" +
            "if (clearBtn.style.display !== 'none') {\n" +
            "    throw new Error('Step 3 (toggle reset) clear button hide failed');\n" +
            "}\n" +
            "sandbox.resetBatchRatioFilter('area');\n" +
            "sandbox.filterFromRatioBar('area', '@Checkout', true);\n" +
            "if (row1.style.display === 'none' || row2.style.display === 'none' || row3.style.display === 'none') {\n" +
            "    throw new Error('Step 4 (Ctrl+click exclude) runs history must remain unaffected');\n" +
            "}\n" +
            "if (var1.style.display !== 'none' || var2.style.display !== '' || var3.style.display !== '') {\n" +
            "    throw new Error('Step 4 (Ctrl+click exclude) tb-entry-row display failed');\n" +
            "}\n" +
            "sandbox.filterFromRatioBar('area', '@Checkout', true);\n" +
            "if (row1.style.display === 'none' || row2.style.display === 'none' || row3.style.display === 'none') {\n" +
            "    throw new Error('Step 5 (Ctrl+click re-include) runs history must remain unaffected');\n" +
            "}\n" +
            "if (var1.style.display !== '' || var2.style.display !== '' || var3.style.display !== '') {\n" +
            "    throw new Error('Step 5 (Ctrl+click re-include) tb-entry-row display failed');\n" +
            "}\n";

        final ProcessBuilder pb = new ProcessBuilder("node", "-e", nodeTestScript);
        pb.redirectErrorStream(true);
        final Process process = pb.start();

        final StringBuilder output = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                output.append(line).append("\n");
            }
        }

        final int exitCode = process.waitFor();
        Assertions.assertEquals(0, exitCode, "Area ratio filter test failed:\n" + output);
    }

    @Test
    public void testBatchLatestStatusCardStickyPinnedAndFillsRunsList() throws Exception
    {
        final Path cssPath = Paths.get("src/main/resources/static/css/report-manager.css");
        Assertions.assertTrue(Files.exists(cssPath), "report-manager.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            cssContent.contains("--batch-sticky-filters-height: 135px;"),
            "report-manager.css must define fallback --batch-sticky-filters-height in :root"
        );

        Assertions.assertTrue(
            cssContent.contains(".batch-top-overview-panel {")
            && cssContent.contains("align-items: start;"),
            "report-manager.css must configure .batch-top-overview-panel with align-items: start to enable sticky scrolling of status card."
        );

        Assertions.assertTrue(
            cssContent.contains(".batch-latest-status-card {")
            && cssContent.contains("position: sticky;")
            && cssContent.contains("top: calc(var(--batch-sticky-filters-height, 135px) + 0.5rem);")
            && cssContent.contains("align-self: start;")
            && cssContent.contains("max-height: calc(100vh - var(--batch-sticky-filters-height, 135px) - 1.5rem);"),
            "report-manager.css must configure .batch-latest-status-card as sticky pinned below global filters bar while scrolling ratio filters."
        );

        Assertions.assertTrue(
            cssContent.contains(".contributing-runs-list {")
            && cssContent.contains("flex: 1;")
            && cssContent.contains("min-height: 0;")
            && cssContent.contains("overflow-y: auto;"),
            "report-manager.css must configure .contributing-runs-list with flex: 1 and min-height: 0 to fill remaining space."
        );

        Assertions.assertTrue(
            cssContent.contains("@media (max-width: 1100px)")
            && cssContent.contains(".batch-latest-status-card {")
            && cssContent.contains("position: static;")
            && cssContent.contains("height: auto;"),
            "report-manager.css must provide single-column responsive overrides for .batch-latest-status-card."
        );

        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            jsContent.contains("updateBatchStickyFiltersHeight")
            && jsContent.contains("initBatchStickyFiltersObserver")
            && jsContent.contains("--batch-sticky-filters-height"),
            "report-manager.js must dynamically observe and update --batch-sticky-filters-height on ResizeObserver"
        );
    }

    @Test
    public void testBatchVariationDrawerResponsiveLayoutSafetyNet() throws Exception
    {
        final Path cssPath = Paths.get("src/main/resources/static/css/report-manager.css");
        Assertions.assertTrue(Files.exists(cssPath), "report-manager.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            cssContent.contains("container-type: inline-size;"),
            "report-manager.css must declare container-type: inline-size on .content-body for component-level responsiveness."
        );

        Assertions.assertTrue(
            cssContent.contains("body:has(#testSidePagePanel.active) .batch-top-overview-panel")
            || cssContent.contains("@container (max-width: 1100px)"),
            "report-manager.css must contain responsive rules when side page panel is active or container shrinks below 1100px."
        );

        Assertions.assertTrue(
            cssContent.contains("body:has(#testSidePagePanel.active) .contributing-runs-list")
            && cssContent.contains("overflow-x: hidden;"),
            "report-manager.css must prevent horizontal overflow on contributing-runs-list when side panel is open."
        );

        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            jsContent.contains("window.innerWidth - 420"),
            "report-manager.js must clamp side panel width to preserve at least 420px for the main content area."
        );

        final Path templatePath = Paths.get("src/main/resources/templates/fragments/test-base.html");
        Assertions.assertTrue(Files.exists(templatePath), "test-base.html must exist: " + templatePath.toAbsolutePath());
        final String templateContent = Files.readString(templatePath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            templateContent.contains("<col style=\"width: 38%;\">")
            && templateContent.contains("<col style=\"width: 18%;\">"),
            "test-base.html variations table must use adjusted column widths for optimal legibility."
        );
    }

    @Test
    public void testBatchRunScopeFilterDeselectAllAndExclusionSyntax() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            jsContent.contains("function selectAllBatchRunScopeFilter(paramName, isChecked, menuId)"),
            "report-manager.js must define selectAllBatchRunScopeFilter"
        );
        Assertions.assertTrue(
            jsContent.contains("menuEl.dataset.pendingSelection = 'true';"),
            "selectAllBatchRunScopeFilter must mark menu as pendingSelection when unchecking Select All without reloading"
        );
        Assertions.assertTrue(
            jsContent.contains("urlParams.delete(excludeParamName);"),
            "selectAllBatchRunScopeFilter must clear exclusion parameters when Select All is re-checked"
        );
        Assertions.assertTrue(
            jsContent.contains("delete menuEl.dataset.pendingSelection;"),
            "toggleBatchRunScopeFilter must clear pendingSelection when a value is selected"
        );
        Assertions.assertTrue(
            jsContent.contains("excludeParamName"),
            "report-manager.js must support excludeParamName for exclusion filter semantics"
        );
        Assertions.assertTrue(
            jsContent.contains("removeBatchRunScopeFilter(paramName, value)"),
            "report-manager.js must define removeBatchRunScopeFilter"
        );
        Assertions.assertTrue(
            jsContent.contains("resetAllBatchRunScopeFilters()"),
            "report-manager.js must define resetAllBatchRunScopeFilters"
        );
        Assertions.assertTrue(
            jsContent.contains("function applyBatchRunScopeFilter(paramName, menuId)"),
            "report-manager.js must define applyBatchRunScopeFilter"
        );
        Assertions.assertTrue(
            jsContent.contains("window.applyBatchRunScopeFilter = applyBatchRunScopeFilter;"),
            "report-manager.js must export applyBatchRunScopeFilter on window"
        );
        Assertions.assertTrue(
            jsContent.contains("revertScopeMenuCheckboxes"),
            "report-manager.js must define revertScopeMenuCheckboxes to discard unapplied changes on menu close"
        );

        final Path cssPath = Paths.get("src/main/resources/static/css/report-manager.css");
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);
        Assertions.assertTrue(
            cssContent.contains(".btn-apply-scope-filter"),
            "report-manager.css must define .btn-apply-scope-filter style"
        );
        Assertions.assertTrue(
            cssContent.contains(".run-scope-menu-footer"),
            "report-manager.css must define .run-scope-menu-footer style"
        );
        Assertions.assertFalse(
            cssContent.matches("(?s).*\\.run-scope-menu\\s*\\{[^}]*display\\s*:\\s*flex;.*"),
            ".run-scope-menu must not declare display: flex unconditionally so dropdowns remain closed by default"
        );
    }

    @Test
    public void testRunsTableResponsiveAndTagsEllipsisStyles() throws Exception
    {
        final Path cssPath = Paths.get("src/main/resources/static/css/report-manager.css");
        Assertions.assertTrue(Files.exists(cssPath), "report-manager.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            cssContent.contains(".cell-locales") && cssContent.contains("min-width: 130px;"),
            "report-manager.css must declare min-width: 130px on .cell-locales to avoid vertical squishing"
        );
        Assertions.assertTrue(
            cssContent.contains(".cell-tags") && cssContent.contains("min-width: 160px;"),
            "report-manager.css must declare min-width on .cell-tags"
        );
        Assertions.assertTrue(
            cssContent.contains("text-overflow: ellipsis;") && cssContent.contains(".table-cell-chips .tag-chip"),
            "report-manager.css must declare text-overflow: ellipsis on tag chips to prevent column overlap"
        );
        Assertions.assertTrue(
            cssContent.contains("table.aura-table th.col-action") && cssContent.contains("position: sticky;"),
            "report-manager.css must pin action column with position: sticky to keep action buttons visible"
        );
        Assertions.assertTrue(
            cssContent.contains("body:has(#testSidePagePanel.active) table.aura-table th"),
            "report-manager.css must compact table cell padding when test side panel is active"
        );
        Assertions.assertTrue(
            cssContent.contains(".col-pass-rate") && cssContent.contains("white-space: nowrap;"),
            "report-manager.css must declare white-space: nowrap on .col-pass-rate to prevent pass rate squishing"
        );
    }

    @Test
    public void testDynamicTagRatioBarFilteringSyntax() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            jsContent.contains("document.querySelectorAll('.ratio-bar-seg[data-ratio-type^=\"tag_\"], .ratio-legend-chip[data-ratio-type^=\"tag_\"]')"),
            "report-manager.js must dynamically discover and synchronize tag ratio cards matching data-ratio-type^='tag_'"
        );
        Assertions.assertTrue(
            jsContent.contains("function getElementTags(el)"),
            "report-manager.js must provide getElementTags helper"
        );
        Assertions.assertTrue(
            jsContent.contains("function matchTagFilters(el)"),
            "report-manager.js must provide matchTagFilters helper"
        );

        final String nodeTestScript =
            "const vm = require('vm');\n" +
            "const fs = require('fs');\n" +
            "const code = fs.readFileSync('src/main/resources/static/js/report-manager.js', 'utf8');\n" +
            "function makeEl(tag, attrs = {}) {\n" +
            "    const attributes = { ...attrs };\n" +
            "    const classes = new Set();\n" +
            "    return {\n" +
            "        tagName: tag,\n" +
            "        style: {},\n" +
            "        classList: {\n" +
            "            add: (...cs) => cs.forEach(c => classes.add(c)),\n" +
            "            remove: (...cs) => cs.forEach(c => classes.delete(c)),\n" +
            "            contains: (c) => classes.has(c)\n" +
            "        },\n" +
            "        getAttribute: (attr) => attributes[attr] || '',\n" +
            "        setAttribute: (attr, val) => { attributes[attr] = String(val); },\n" +
            "        removeAttribute: (attr) => { delete attributes[attr]; },\n" +
            "        querySelectorAll: () => [],\n" +
            "        querySelector: () => null,\n" +
            "        closest: () => null,\n" +
            "        innerText: '',\n" +
            "        title: ''\n" +
            "    };\n" +
            "}\n" +
            "const row1 = makeEl('TR', { 'data-tags': JSON.stringify({ release: '1.0', team: 'Core' }), 'data-env': 'DEFAULT', 'data-locales': 'US', 'data-browsers': 'CHROME', 'data-batch': 'DEFAULT' });\n" +
            "const row2 = makeEl('TR', { 'data-tags': JSON.stringify({ release: '2.0', team: 'Checkout' }), 'data-env': 'DEFAULT', 'data-locales': 'US', 'data-browsers': 'CHROME', 'data-batch': 'DEFAULT' });\n" +
            "const var1 = makeEl('TR', { 'data-tags': JSON.stringify({ release: '1.0', team: 'Core' }), 'data-env': 'DEFAULT', 'data-location': 'US', 'data-browser': 'CHROME', 'data-batch': 'DEFAULT' });\n" +
            "const var2 = makeEl('TR', { 'data-tags': JSON.stringify({ release: '2.0', team: 'Checkout' }), 'data-env': 'DEFAULT', 'data-location': 'US', 'data-browser': 'CHROME', 'data-batch': 'DEFAULT' });\n" +
            "const seg1 = makeEl('DIV', { 'data-ratio-type': 'tag_Release', 'data-filter-val': '1.0' });\n" +
            "seg1.classList.add('ratio-bar-seg');\n" +
            "const seg2 = makeEl('DIV', { 'data-ratio-type': 'tag_Release', 'data-filter-val': '2.0' });\n" +
            "seg2.classList.add('ratio-bar-seg');\n" +
            "const chip1 = makeEl('DIV', { 'data-ratio-type': 'tag_Release', 'data-filter-val': '1.0' });\n" +
            "chip1.classList.add('ratio-legend-chip');\n" +
            "chip1.querySelector = (sel) => sel === '.val' ? makeEl('SPAN') : null;\n" +
            "const chip2 = makeEl('DIV', { 'data-ratio-type': 'tag_Release', 'data-filter-val': '2.0' });\n" +
            "chip2.classList.add('ratio-legend-chip');\n" +
            "chip2.querySelector = (sel) => sel === '.val' ? makeEl('SPAN') : null;\n" +
            "const clearBtn = makeEl('BUTTON', { id: 'batchClearTag_ReleaseFilterBtn' });\n" +
            "const idMap = {\n" +
            "    'batchClearTag_ReleaseFilterBtn': clearBtn,\n" +
            "    'batchClearAreaFilterBtn': makeEl('BUTTON'),\n" +
            "    'batchClearBrowserFilterBtn': makeEl('BUTTON'),\n" +
            "    'batchClearLocaleFilterBtn': makeEl('BUTTON'),\n" +
            "    'batchClearEnvFilterBtn': makeEl('BUTTON'),\n" +
            "    'batchClearTagFilterBtn': makeEl('BUTTON')\n" +
            "};\n" +
            "const queryMap = {\n" +
            "    '#batchRunsHistoryTableBody .run-history-row': [row1, row2],\n" +
            "    '#batchTestBaseVariationsPanel .tb-entry-row': [var1, var2],\n" +
            "    '#batchLatestVariationsData .batch-latest-var-entry': [var1, var2],\n" +
            "    '.ratio-bar-seg[data-ratio-type=\"tag_Release\"], .ratio-legend-chip[data-ratio-type=\"tag_Release\"]': [seg1, seg2, chip1, chip2],\n" +
            "    '.ratio-legend-chip[data-ratio-type=\"tag_Release\"], .ratio-bar-seg[data-ratio-type=\"tag_Release\"]': [seg1, seg2, chip1, chip2],\n" +
            "    '.ratio-bar-seg[data-ratio-type^=\"tag_\"], .ratio-legend-chip[data-ratio-type^=\"tag_\"]': [seg1, seg2, chip1, chip2]\n" +
            "};\n" +
            "const sandbox = {\n" +
            "    console,\n" +
            "    window: {},\n" +
            "    document: {\n" +
            "        getElementById: (id) => idMap[id] || null,\n" +
            "        querySelectorAll: (sel) => queryMap[sel] || [],\n" +
            "        querySelector: () => null,\n" +
            "        addEventListener: () => {},\n" +
            "        body: { addEventListener: () => {} }\n" +
            "    }\n" +
            "};\n" +
            "sandbox.window = sandbox;\n" +
            "vm.runInNewContext(code, sandbox);\n" +
            "sandbox.filterFromRatioBar('tag_Release', '1.0', false);\n" +
            "if (row1.style.display === 'none' || row2.style.display === 'none') {\n" +
            "    throw new Error('Step 1 (dynamic tag_Release 1.0 isolation) run rows must not be filtered');\n" +
            "}\n" +
            "if (var1.style.display !== '' || var2.style.display !== 'none') {\n" +
            "    throw new Error('Step 1 (dynamic tag_Release 1.0 isolation) tb-entry-row filtering failed');\n" +
            "}\n" +
            "if (clearBtn.style.display !== 'inline-flex') {\n" +
            "    throw new Error('Step 1 clear button display failed');\n" +
            "}\n" +
            "sandbox.resetBatchRatioFilter('tag_Release');\n" +
            "if (row1.style.display === 'none' || row2.style.display === 'none' || var1.style.display !== '' || var2.style.display !== '') {\n" +
            "    throw new Error('Step 2 (reset tag filter) row display failed');\n" +
            "}\n";

        final ProcessBuilder pb = new ProcessBuilder("node", "-e", nodeTestScript);
        pb.redirectErrorStream(true);
        final Process process = pb.start();

        final StringBuilder output = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                output.append(line).append("\n");
            }
        }

        final int exitCode = process.waitFor();
        Assertions.assertEquals(0, exitCode, "Dynamic tag ratio filter test failed:\n" + output);
    }

    @Test
    public void testBatchHistoryMultiValueTagRatioFiltering() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist");

        final String nodeTestScript =
            "const fs = require('fs');\n" +
            "const vm = require('vm');\n" +
            "const code = fs.readFileSync('" + jsPath.toAbsolutePath().toString().replace("\\", "/") + "', 'utf8');\n" +
            "function makeEl(tag, attrs = {}) {\n" +
            "    const attributes = Object.assign({}, attrs);\n" +
            "    return {\n" +
            "        tagName: tag,\n" +
            "        style: { display: '' },\n" +
            "        classList: {\n" +
            "            classes: new Set(),\n" +
            "            add: function(...c) { c.forEach(x => this.classes.add(x)); },\n" +
            "            remove: function(...c) { c.forEach(x => this.classes.delete(x)); },\n" +
            "            contains: function(c) { return this.classes.has(c); }\n" +
            "        },\n" +
            "        getAttribute: (attr) => attributes[attr] || '',\n" +
            "        setAttribute: (attr, val) => { attributes[attr] = String(val); },\n" +
            "        removeAttribute: (attr) => { delete attributes[attr]; },\n" +
            "        querySelectorAll: () => [],\n" +
            "        querySelector: () => null,\n" +
            "        closest: () => null,\n" +
            "        innerText: '',\n" +
            "        title: ''\n" +
            "    };\n" +
            "}\n" +
            "const row1 = makeEl('TR', { 'data-tags': JSON.stringify({ locale: ['UK', 'US', 'DE'], release: '1.0' }), 'data-env': 'DEFAULT', 'data-locales': 'UK,US,DE', 'data-browsers': 'CHROME', 'data-batch': 'DEFAULT' });\n" +
            "const row2 = makeEl('TR', { 'data-tags': JSON.stringify({ locale: 'FR', release: '1.0' }), 'data-env': 'DEFAULT', 'data-locales': 'FR', 'data-browsers': 'CHROME', 'data-batch': 'DEFAULT' });\n" +
            "const var1 = makeEl('TR', { 'data-tags': JSON.stringify({ locale: 'UK' }), 'data-env': 'DEFAULT', 'data-location': 'UK', 'data-browser': 'CHROME', 'data-batch': 'DEFAULT' });\n" +
            "const var2 = makeEl('TR', { 'data-tags': JSON.stringify({ locale: 'FR' }), 'data-env': 'DEFAULT', 'data-location': 'FR', 'data-browser': 'CHROME', 'data-batch': 'DEFAULT' });\n" +
            "const seg1 = makeEl('DIV', { 'data-ratio-type': 'tag_locale', 'data-filter-val': 'UK' });\n" +
            "seg1.classList.add('ratio-bar-seg');\n" +
            "const seg2 = makeEl('DIV', { 'data-ratio-type': 'tag_locale', 'data-filter-val': 'FR' });\n" +
            "seg2.classList.add('ratio-bar-seg');\n" +
            "const chip1 = makeEl('DIV', { 'data-ratio-type': 'tag_locale', 'data-filter-val': 'UK' });\n" +
            "chip1.classList.add('ratio-legend-chip');\n" +
            "chip1.querySelector = (sel) => sel === '.val' ? makeEl('SPAN') : null;\n" +
            "const chip2 = makeEl('DIV', { 'data-ratio-type': 'tag_locale', 'data-filter-val': 'FR' });\n" +
            "chip2.classList.add('ratio-legend-chip');\n" +
            "chip2.querySelector = (sel) => sel === '.val' ? makeEl('SPAN') : null;\n" +
            "const clearBtn = makeEl('BUTTON', { id: 'batchClearTag_localeFilterBtn' });\n" +
            "const idMap = {\n" +
            "    'batchClearTag_localeFilterBtn': clearBtn,\n" +
            "    'batchClearAreaFilterBtn': makeEl('BUTTON'),\n" +
            "    'batchClearBrowserFilterBtn': makeEl('BUTTON'),\n" +
            "    'batchClearLocaleFilterBtn': makeEl('BUTTON'),\n" +
            "    'batchClearEnvFilterBtn': makeEl('BUTTON'),\n" +
            "    'batchClearTagFilterBtn': makeEl('BUTTON')\n" +
            "};\n" +
            "const queryMap = {\n" +
            "    '#batchRunsHistoryTableBody .run-history-row': [row1, row2],\n" +
            "    '#batchTestBaseVariationsPanel .tb-entry-row': [var1, var2],\n" +
            "    '#batchLatestVariationsData .batch-latest-var-entry': [var1, var2],\n" +
            "    '.ratio-bar-seg[data-ratio-type=\"tag_locale\"], .ratio-legend-chip[data-ratio-type=\"tag_locale\"]': [seg1, seg2, chip1, chip2],\n" +
            "    '.ratio-legend-chip[data-ratio-type=\"tag_locale\"], .ratio-bar-seg[data-ratio-type=\"tag_locale\"]': [seg1, seg2, chip1, chip2],\n" +
            "    '.ratio-bar-seg[data-ratio-type^=\"tag_\"], .ratio-legend-chip[data-ratio-type^=\"tag_\"]': [seg1, seg2, chip1, chip2]\n" +
            "};\n" +
            "const sandbox = {\n" +
            "    console,\n" +
            "    window: {},\n" +
            "    document: {\n" +
            "        getElementById: (id) => idMap[id] || null,\n" +
            "        querySelectorAll: (sel) => queryMap[sel] || [],\n" +
            "        querySelector: () => null,\n" +
            "        addEventListener: () => {},\n" +
            "        body: { addEventListener: () => {} }\n" +
            "    }\n" +
            "};\n" +
            "sandbox.window = sandbox;\n" +
            "vm.runInNewContext(code, sandbox);\n" +
            "// Step 1: Filter by UK\n" +
            "sandbox.filterFromRatioBar('tag_locale', 'UK', false);\n" +
            "if (row1.style.display === 'none' || row2.style.display === 'none') {\n" +
            "    throw new Error('Step 1 (tag_locale UK isolation) run rows must not be filtered');\n" +
            "}\n" +
            "if (var1.style.display !== '' || var2.style.display !== 'none') {\n" +
            "    throw new Error('Step 1 (tag_locale UK isolation) tb-entry-row filtering failed');\n" +
            "}\n" +
            "// Step 2: Reset filter\n" +
            "sandbox.resetBatchRatioFilter('tag_locale');\n" +
            "if (row1.style.display === 'none' || row2.style.display === 'none' || var1.style.display !== '' || var2.style.display !== '') {\n" +
            "    throw new Error('Step 2 (reset tag filter) run row display reset failed');\n" +
            "}\n" +
            "// Step 3: Test duplicate key raw string parsing resilience\n" +
            "const rowDup = makeEl('TR', { 'data-tags': '{\"locale\":\"DE\",\"locale\":\"UK\",\"locale\":\"US\"}' });\n" +
            "const rawVal = sandbox.getElementTagValue(rowDup, 'locale');\n" +
            "const candidates = sandbox.extractCandidateTagValues(rawVal);\n" +
            "if (!candidates.includes('UK') || !candidates.includes('DE') || !candidates.includes('US')) {\n" +
            "    throw new Error('Step 3 duplicate key raw string parsing failed: ' + JSON.stringify(candidates));\n" +
            "}\n";

        final ProcessBuilder pb = new ProcessBuilder("node", "-e", nodeTestScript);
        pb.redirectErrorStream(true);
        final Process process = pb.start();

        final StringBuilder output = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                output.append(line).append("\n");
            }
        }

        final int exitCode = process.waitFor();
        Assertions.assertEquals(0, exitCode, "Multi-value tag ratio filter test failed:\n" + output);
    }

    @Test
    public void testBatchHistoryAndOverviewPieBreakdownMultiLineStructure() throws Exception
    {
        final Path cssPath = Paths.get("src/main/resources/static/css/report-manager.css");
        Assertions.assertTrue(Files.exists(cssPath), "report-manager.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            cssContent.contains(".pie-breakdown-main-row"),
            "report-manager.css must define .pie-breakdown-main-row for primary status line"
        );
        Assertions.assertTrue(
            cssContent.contains(".pie-breakdown-sub-row"),
            "report-manager.css must define .pie-breakdown-sub-row for indented healed and AI badges"
        );
        Assertions.assertTrue(
            cssContent.contains(".pie-breakdown-total-tile"),
            "report-manager.css must define .pie-breakdown-total-tile to utilize vertical space"
        );
        Assertions.assertTrue(
            cssContent.contains(".latest-status-chart-group") && cssContent.contains("width: 185px;"),
            "report-manager.css must allocate at least 185px width for .latest-status-chart-group"
        );

        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist");
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            jsContent.contains("batchLatestTotalVal") && jsContent.contains("batchLatestTotalTile"),
            "report-manager.js must dynamically update total test tile count and visibility"
        );
    }

    @Test
    public void testBatchLatestStatusStackedAndCollapsedLayout() throws Exception
    {
        final Path cssPath = Paths.get("src/main/resources/static/css/report-manager.css");
        Assertions.assertTrue(Files.exists(cssPath), "report-manager.css must exist");
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            cssContent.contains(".pie-breakdown-items-wrapper"),
            "report-manager.css must define .pie-breakdown-items-wrapper"
        );
        Assertions.assertTrue(
            cssContent.contains(".latest-status-body.layout-stacked"),
            "report-manager.css must define .latest-status-body.layout-stacked for few runs layout"
        );
        Assertions.assertTrue(
            cssContent.contains(".latest-status-body.contrib-collapsed"),
            "report-manager.css must define .latest-status-body.contrib-collapsed for collapsed runs layout"
        );
        Assertions.assertTrue(
            cssContent.contains(".btn-toggle-contrib") && cssContent.contains(".contributing-runs-collapsed-strip"),
            "report-manager.css must define collapse button and collapsed strip styles"
        );

        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist");
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            jsContent.contains("toggleContributingRunsCollapse") && jsContent.contains("initContributingRunsCollapseState"),
            "report-manager.js must define collapse toggle and state restoration functions"
        );
        Assertions.assertTrue(
            jsContent.contains("layout-stacked"),
            "report-manager.js must dynamically toggle layout-stacked based on visible runs count"
        );
    }

    @Test
    public void testBatchStickyFiltersContainerAndUniquenessPlacement() throws Exception
    {
        final Path cssPath = Paths.get("src/main/resources/static/css/report-manager.css");
        Assertions.assertTrue(Files.exists(cssPath), "report-manager.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            cssContent.contains(".batch-sticky-filters-container {")
            && cssContent.contains("position: sticky;")
            && cssContent.contains("top: 0;")
            && cssContent.contains("z-index: 1020;"),
            "report-manager.css must configure .batch-sticky-filters-container as sticky pinned to top with z-index: 1020."
        );
    }

    @Test
    public void testExecutionIdentityInfoIconsAndTooltips() throws Exception
    {
        final Path modalsPath = Paths.get("src/main/resources/templates/fragments/modals.html");
        Assertions.assertTrue(Files.exists(modalsPath), "modals.html must exist");
        final String modalsHtml = Files.readString(modalsPath, StandardCharsets.UTF_8);

        final Path widgetConfigModalPath = Paths.get("src/main/resources/templates/fragments/widget-configurator-modal.html");
        Assertions.assertTrue(Files.exists(widgetConfigModalPath), "widget-configurator-modal.html must exist");
        final String widgetConfigModalHtml = Files.readString(widgetConfigModalPath, StandardCharsets.UTF_8);

        final Path customDashboardPath = Paths.get("src/main/resources/templates/fragments/custom-dashboard-page.html");
        Assertions.assertTrue(Files.exists(customDashboardPath), "custom-dashboard-page.html must exist");
        final String customDashboardHtml = Files.readString(customDashboardPath, StandardCharsets.UTF_8);

        final Path dashboardWidgetsPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(dashboardWidgetsPath), "dashboard-widgets.html must exist");
        final String widgetsHtml = Files.readString(dashboardWidgetsPath, StandardCharsets.UTF_8);

        final String expectedTooltip = "Execution Identity defines which dimensions (such as Test, Browser, Environment, or Tags) are used to uniquely distinguish and track test execution instances across runs and variations.";

        Assertions.assertTrue(
            modalsHtml.contains("execution-identity-info-icon") && modalsHtml.contains(expectedTooltip),
            "modals.html must render execution-identity-info-icon with explanation tooltip in modals"
        );
        Assertions.assertTrue(
            widgetConfigModalHtml.contains("execution-identity-info-icon") && widgetConfigModalHtml.contains(expectedTooltip),
            "widget-configurator-modal.html must render execution-identity-info-icon with explanation tooltip"
        );
        Assertions.assertTrue(
            customDashboardHtml.contains("execution-identity-info-icon") && customDashboardHtml.contains(expectedTooltip),
            "custom-dashboard-page.html must render execution-identity-info-icon with explanation tooltip"
        );
        Assertions.assertTrue(
            widgetsHtml.contains("execution-identity-info-icon") && widgetsHtml.contains(expectedTooltip),
            "dashboard-widgets.html must render execution-identity-info-icon with explanation tooltip on widget cards"
        );
    }

    @Test
    public void testAllDashboardNavigationLinksHaveAnchorHrefSemantics() throws Exception
    {
        final Path batchHistoryPath = Paths.get("src/main/resources/templates/fragments/batch-history.html");
        Assertions.assertTrue(Files.exists(batchHistoryPath), "batch-history.html must exist");
        final String historyHtml = Files.readString(batchHistoryPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            historyHtml.contains("run-id-link") && historyHtml.contains("th:href=\"'/run-report?runId=' + ${run.id}\""),
            "batch-history.html runs table must wrap Run ID in an anchor link with th:href"
        );
        Assertions.assertTrue(
            historyHtml.contains("tag-chip tag-batch"),
            "batch-history.html batch suite chip must be an anchor element"
        );

        final Path batchDetailsPath = Paths.get("src/main/resources/templates/fragments/batch-details.html");
        Assertions.assertTrue(Files.exists(batchDetailsPath), "batch-details.html must exist");
        final String detailsHtml = Files.readString(batchDetailsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            detailsHtml.contains("btn btn-primary")
            && detailsHtml.contains("th:href=\"'/run-report?runId=' + ${run.id}\""),
            "batch-details.html 'Open Report' button must be an anchor with valid th:href"
        );

        final Path runReportPath = Paths.get("src/main/resources/templates/fragments/run-report.html");
        Assertions.assertTrue(Files.exists(runReportPath), "run-report.html must exist");
        final String runReportHtml = Files.readString(runReportPath, StandardCharsets.UTF_8);

        Assertions.assertFalse(
            runReportHtml.contains("href=\"javascript:void(0)\""),
            "run-report.html must not contain javascript:void(0) pseudo links"
        );
        Assertions.assertTrue(
            runReportHtml.contains("test-title-link")
            && runReportHtml.contains("th:href=\"@{/run-report(runId=${report?.runId ?: runId}, executionId=${exec.id})}\""),
            "run-report.html test titles must be anchors pointing to permalink with runId and executionId"
        );

        final Path testBasePath = Paths.get("src/main/resources/templates/fragments/test-base.html");
        Assertions.assertTrue(Files.exists(testBasePath), "test-base.html must exist");
        final String testBaseHtml = Files.readString(testBasePath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            testBaseHtml.contains("test-title-link")
            && testBaseHtml.contains("th:href=\"@{/test-base(testName="),
            "test-base.html variation test title must be an anchor with th:href to test-base permalink"
        );

        final Path sidePanelPath = Paths.get("src/main/resources/templates/fragments/side-panel-step-list.html");
        Assertions.assertTrue(Files.exists(sidePanelPath), "side-panel-step-list.html must exist");
        final String sidePanelHtml = Files.readString(sidePanelPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            sidePanelHtml.contains("<a id=\"btnViewHistoryInTestBase\"")
            && sidePanelHtml.contains("href=\"/test-base\""),
            "side-panel-step-list.html 'View History' action must be an anchor element with href='/test-base'"
        );
    }

    @Test
    public void testRatioFilterDecoupledFromRunsAndTrends() throws Exception
    {
        if (!isNodeAvailable())
        {
            return;
        }

        final Path jsFilePath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsFilePath), "report-manager.js must exist: " + jsFilePath.toAbsolutePath());

        final String nodeTestScript =
            "const fs = require('fs');\n" +
            "const vm = require('vm');\n" +
            "const code = fs.readFileSync('" + jsFilePath.toAbsolutePath().toString().replace("\\", "/") + "', 'utf8');\n" +
            "function makeEl(tag, attrs = {}) {\n" +
            "    const attributes = { ...attrs };\n" +
            "    const classes = new Set();\n" +
            "    return {\n" +
            "        tagName: tag,\n" +
            "        style: {},\n" +
            "        classList: {\n" +
            "            add: (...cs) => cs.forEach(c => classes.add(c)),\n" +
            "            remove: (...cs) => cs.forEach(c => classes.delete(c)),\n" +
            "            contains: (c) => classes.has(c)\n" +
            "        },\n" +
            "        getAttribute: (attr) => attributes[attr] || '',\n" +
            "        setAttribute: (attr, val) => { attributes[attr] = String(val); },\n" +
            "        removeAttribute: (attr) => { delete attributes[attr]; },\n" +
            "        querySelectorAll: () => [],\n" +
            "        querySelector: () => null,\n" +
            "        closest: () => null,\n" +
            "        innerText: '',\n" +
            "        title: ''\n" +
            "    };\n" +
            "}\n" +
            "const row1 = makeEl('TR', { 'data-browsers': 'CHROME', 'data-env': 'DEFAULT', 'data-locales': 'US', 'data-batch': 'DEFAULT' });\n" +
            "const row2 = makeEl('TR', { 'data-browsers': 'FIREFOX', 'data-env': 'DEFAULT', 'data-locales': 'US', 'data-batch': 'DEFAULT' });\n" +
            "const var1 = makeEl('DIV', { 'data-browser': 'CHROME', 'data-env': 'DEFAULT', 'data-location': 'US', 'data-batch': 'DEFAULT' });\n" +
            "const var2 = makeEl('DIV', { 'data-browser': 'FIREFOX', 'data-env': 'DEFAULT', 'data-location': 'US', 'data-batch': 'DEFAULT' });\n" +
            "const trendPath = makeEl('PATH', { d: 'M0,10 L10,20' });\n" +
            "const mainTrendSvg = makeEl('SVG', { id: 'mainTrendSvg' });\n" +
            "mainTrendSvg.querySelectorAll = (sel) => (sel === 'path' || sel.includes('path')) ? [trendPath] : [];\n" +
            "const clearBrowserBtn = makeEl('BUTTON', { id: 'batchClearBrowserFilterBtn' });\n" +
            "const segChrome = makeEl('DIV', { 'data-ratio-type': 'browser', 'data-filter-val': 'CHROME' });\n" +
            "segChrome.classList.add('ratio-bar-seg');\n" +
            "const segFirefox = makeEl('DIV', { 'data-ratio-type': 'browser', 'data-filter-val': 'FIREFOX' });\n" +
            "segFirefox.classList.add('ratio-bar-seg');\n" +
            "const chipChrome = makeEl('DIV', { 'data-ratio-type': 'browser', 'data-filter-val': 'CHROME' });\n" +
            "chipChrome.classList.add('ratio-legend-chip');\n" +
            "chipChrome.querySelector = () => makeEl('SPAN');\n" +
            "const chipFirefox = makeEl('DIV', { 'data-ratio-type': 'browser', 'data-filter-val': 'FIREFOX' });\n" +
            "chipFirefox.classList.add('ratio-legend-chip');\n" +
            "chipFirefox.querySelector = () => makeEl('SPAN');\n" +
            "const idMap = {\n" +
            "    'mainTrendSvg': mainTrendSvg,\n" +
            "    'batchClearBrowserFilterBtn': clearBrowserBtn,\n" +
            "    'batchClearLocaleFilterBtn': makeEl('BUTTON'),\n" +
            "    'batchClearEnvFilterBtn': makeEl('BUTTON'),\n" +
            "    'batchClearTagFilterBtn': makeEl('BUTTON'),\n" +
            "    'batchClearAreaFilterBtn': makeEl('BUTTON')\n" +
            "};\n" +
            "const queryMap = {\n" +
            "    '#batchRunsHistoryTableBody .run-history-row': [row1, row2],\n" +
            "    '#batchTestBaseVariationsPanel .tb-entry-row': [var1, var2],\n" +
            "    '#batchLatestVariationsData .batch-latest-var-entry': [var1, var2],\n" +
            "    '.ratio-bar-seg[data-ratio-type=\"browser\"], .ratio-legend-chip[data-ratio-type=\"browser\"]': [segChrome, segFirefox, chipChrome, chipFirefox],\n" +
            "    '.ratio-legend-chip[data-ratio-type=\"browser\"], .ratio-bar-seg[data-ratio-type=\"browser\"]': [segChrome, segFirefox, chipChrome, chipFirefox]\n" +
            "};\n" +
            "const sandbox = {\n" +
            "    console,\n" +
            "    window: {},\n" +
            "    document: {\n" +
            "        getElementById: (id) => idMap[id] || null,\n" +
            "        querySelectorAll: (sel) => queryMap[sel] || [],\n" +
            "        querySelector: () => null,\n" +
            "        addEventListener: () => {},\n" +
            "        body: { addEventListener: () => {} }\n" +
            "    }\n" +
            "};\n" +
            "sandbox.window = sandbox;\n" +
            "vm.runInNewContext(code, sandbox);\n" +
            "// Step 1: Filter by FIREFOX\n" +
            "sandbox.filterFromRatioBar('browser', 'FIREFOX', false);\n" +
            "if (row1.style.display === 'none' || row2.style.display === 'none') {\n" +
            "    throw new Error('Ratio filter must not hide runs in #batchRunsHistoryTableBody: row1=' + row1.style.display + ', row2=' + row2.style.display);\n" +
            "}\n" +
            "if (trendPath.getAttribute('d') !== 'M0,10 L10,20') {\n" +
            "    throw new Error('Ratio filter must not wipe or re-render #mainTrendSvg: path d was modified to ' + trendPath.getAttribute('d'));\n" +
            "}\n" +
            "if (var1.style.display !== 'none' || var2.style.display !== '') {\n" +
            "    throw new Error('Ratio filter must filter variations in #batchTestBaseVariationsPanel');\n" +
            "}\n" +
            "// Step 2: Reset browser ratio filter\n" +
            "sandbox.resetBatchRatioFilter('browser');\n" +
            "if (row1.style.display === 'none' || row2.style.display === 'none') {\n" +
            "    throw new Error('Reset must leave runs visible');\n" +
            "}\n" +
            "if (var1.style.display !== '' || var2.style.display !== '') {\n" +
            "    throw new Error('Reset must restore all test variations');\n" +
            "}\n";

        final ProcessBuilder pb = new ProcessBuilder("node", "-e", nodeTestScript);
        pb.redirectErrorStream(true);
        final Process process = pb.start();

        final StringBuilder output = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                output.append(line).append("\n");
            }
        }

        final int exitCode = process.waitFor();
        Assertions.assertEquals(0, exitCode, "Ratio filter decoupling test failed:\n" + output);
    }

    @Test
    public void testSuiteEvolutionFrontendArtifactsIntegrity() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist");
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(jsContent.contains("function renderDynamicSuiteEvolutionChart()"),
            "report-manager.js must define renderDynamicSuiteEvolutionChart");
        Assertions.assertTrue(jsContent.contains("renderDynamicSuiteEvolutionChart();"),
            "report-manager.js must invoke renderDynamicSuiteEvolutionChart");
        Assertions.assertTrue(jsContent.contains("function setBatchHistoryAsOfRun("),
            "report-manager.js must define setBatchHistoryAsOfRun for milestone rewind");
        Assertions.assertTrue(jsContent.contains("function stepSuiteEvolutionMilestone("),
            "report-manager.js must define stepSuiteEvolutionMilestone");
        Assertions.assertTrue(jsContent.contains("function updateEvoStepperControls("),
            "report-manager.js must define updateEvoStepperControls");
        Assertions.assertTrue(jsContent.contains("evo-node-active"),
            "report-manager.js must apply evo-node-active to selected milestone");

        final Path cssPath = Paths.get("src/main/resources/static/css/report-manager.css");
        Assertions.assertTrue(Files.exists(cssPath), "report-manager.css must exist");
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(cssContent.contains(".suite-evolution-card"),
            "report-manager.css must declare .suite-evolution-card");
        Assertions.assertTrue(cssContent.contains(".batch-suite-evolution-section"),
            "report-manager.css must declare .batch-suite-evolution-section");
        Assertions.assertTrue(cssContent.contains(".suite-evolution-svg"),
            "report-manager.css must declare .suite-evolution-svg");
        Assertions.assertTrue(cssContent.contains(".evo-tooltip"),
            "report-manager.css must declare .evo-tooltip");
        Assertions.assertTrue(cssContent.contains(".historical-snapshot-banner"),
            "report-manager.css must declare .historical-snapshot-banner");
        Assertions.assertTrue(cssContent.contains(".btn-return-latest"),
            "report-manager.css must declare .btn-return-latest");
        Assertions.assertTrue(cssContent.contains(".evo-node-circle.evo-node-active"),
            "report-manager.css must declare .evo-node-circle.evo-node-active");
        Assertions.assertTrue(cssContent.contains(".evo-node-circle.evo-node-growth"),
            "report-manager.css must declare .evo-node-circle.evo-node-growth");
        Assertions.assertTrue(cssContent.contains("#evoHoverDot"),
            "report-manager.css must declare #evoHoverDot");
        Assertions.assertTrue(cssContent.contains(".btn-chart-arrow"),
            "report-manager.css must declare .btn-chart-arrow");
        Assertions.assertTrue(cssContent.contains(".evo-axis-text"),
            "report-manager.css must declare .evo-axis-text");
        Assertions.assertTrue(cssContent.contains(".evo-tt-run-link"),
            "report-manager.css must declare .evo-tt-run-link");

        if (isNodeAvailable())
        {
            verifySyntaxWithNode(jsPath.toFile());
        }
    }

    @Test
    public void testDynamicSuiteEvolutionChartExecutionWithoutRuntimeErrors() throws Exception
    {
        if (!isNodeAvailable())
        {
            return;
        }

        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());

        final String nodeScript = """
            const fs = require('fs');
            const js = fs.readFileSync('%s', 'utf8');

            class MockElement {
              constructor(name) {
                this.name = name;
                this.attrs = {};
                this.dataset = {};
                this.style = {};
                this.classList = { add: () => {}, remove: () => {}, contains: () => false, toggle: () => {} };
                this.children = [];
                this.textContent = '';
                this.innerText = '';
              }
              setAttribute(k, v) { this.attrs[k] = v; }
              getAttribute(k) { return this.attrs[k] || null; }
              appendChild(c) { this.children.push(c); }
              addEventListener() {}
              closest() { return this; }
              getBoundingClientRect() { return { width: 800, height: 180, left: 0, top: 0 }; }
              querySelector(sel) {
                if (sel.startsWith('#')) {
                  const id = sel.slice(1);
                  if (this.attrs['id'] === id) return this;
                  for (const ch of this.children) {
                    if (ch.attrs && ch.attrs['id'] === id) return ch;
                    if (ch.querySelector) {
                      const res = ch.querySelector(sel);
                      if (res) return res;
                    }
                  }
                }
                return null;
              }
              querySelectorAll() { return []; }
            }

            const mockSvg = new MockElement('svg');
            mockSvg.setAttribute('id', 'batchSuiteEvolutionSvg');
            mockSvg.setAttribute('data-evolution-points', JSON.stringify([
              {runId: 1, totalTestsKnown: 10, passCleanCount: 10, newTestsCount: 0},
              {runId: 2, totalTestsKnown: 15, passCleanCount: 15, newTestsCount: 5},
              {runId: 3, totalTestsKnown: 18, passCleanCount: 18, newTestsCount: 3}
            ]));

            ['evoSvgYAxis', 'evoSvgXAxis', 'evoLayerIgnored', 'evoLayerUnknownClean', 'evoLayerUnknownHealed', 'evoLayerUnknownHealedPat', 'evoLayerUnknownAi', 'evoLayerUnknownAiPat', 'evoLayerKnownClean', 'evoLayerKnownHealed', 'evoLayerKnownHealedPat', 'evoLayerKnownAi', 'evoLayerKnownAiPat', 'evoLayerFixedClean', 'evoLayerFixedHealed', 'evoLayerFixedHealedPat', 'evoLayerFixedAi', 'evoLayerFixedAiPat', 'evoLayerPassClean', 'evoLayerPassHealed', 'evoLayerPassHealedPat', 'evoLayerPassAi', 'evoLayerPassAiPat', 'evoTopStroke', 'evoSvgDots', 'evoHoverGuide', 'evoHoverDot'].forEach(id => {
              const el = new MockElement('g');
              el.setAttribute('id', id);
              mockSvg.appendChild(el);
            });

            const mockDoc = {
              createElementNS: (ns, tag) => new MockElement(tag),
              getElementById: (id) => {
                if (id === 'batchSuiteEvolutionSvg') return mockSvg;
                return new MockElement(id);
              },
              querySelector: () => new MockElement('div'),
              querySelectorAll: () => [],
              addEventListener: () => {},
              body: new MockElement('body')
            };

            const vm = require('vm');
            const sandbox = {
              document: mockDoc,
              console,
              window: { location: { search: '', pathname: '' }, addEventListener: () => {} },
              URLSearchParams: function() { return { get: () => null }; },
              localStorage: { getItem: () => null, setItem: () => {} }
            };
            vm.createContext(sandbox);

            vm.runInContext(js + '; this.fn = renderDynamicSuiteEvolutionChart;', sandbox);
            sandbox.fn();

            const passClean = mockSvg.querySelector('#evoLayerPassClean');
            if (!passClean || !passClean.attrs['d'] || !passClean.attrs['d'].startsWith('M ')) {
              throw new Error('PassClean layer path d attribute was not rendered');
            }
            const xAxis = mockSvg.querySelector('#evoSvgXAxis');
            if (!xAxis || xAxis.children.length === 0) {
              throw new Error('X-axis ticks and labels were not rendered');
            }
            const dots = mockSvg.querySelector('#evoSvgDots');
            if (!dots || dots.children.length === 0) {
              throw new Error('Evolution milestone dots were not rendered');
            }
            console.log('OK');
            """.formatted(jsPath.toAbsolutePath().toString().replace("\\", "/"));

        final ProcessBuilder pb = new ProcessBuilder("node", "-e", nodeScript);
        pb.redirectErrorStream(true);
        final Process process = pb.start();

        final StringBuilder output = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                output.append(line).append("\n");
            }
        }

        final int exitCode = process.waitFor();
        Assertions.assertEquals(0, exitCode, "renderDynamicSuiteEvolutionChart() must execute without runtime errors:\n" + output);
        Assertions.assertTrue(output.toString().contains("OK"), "Execution must succeed with output 'OK'");
    }

    /**
     * Regression safety net ensuring that clicking test areas and applying batch filters
     * does not trigger unwanted automatic scrolling or jumping to the runs list.
     */
    @Test
    public void testPreventAutomaticScrollToRunsListOnAreaClickAndFilter() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // 1. openAllTestsAndExpandArea must not trigger scrollIntoView
        final int openAreaIdx = jsContent.indexOf("function openAllTestsAndExpandArea(targetAreaGroupId)");
        Assertions.assertTrue(openAreaIdx > 0, "openAllTestsAndExpandArea function should exist in report-manager.js");
        final int openAreaEnd = jsContent.indexOf("window.openAllTestsAndExpandArea =", openAreaIdx);
        Assertions.assertTrue(openAreaEnd > openAreaIdx, "End of openAllTestsAndExpandArea should be found");
        final String openAreaBody = jsContent.substring(openAreaIdx, openAreaEnd);
        Assertions.assertFalse(openAreaBody.contains(".scrollIntoView("),
            "openAllTestsAndExpandArea must not call scrollIntoView");

        // 2. focusAndInspectExecution and pageTestBase must not call scrollIntoView to jump down
        Assertions.assertFalse(jsContent.contains("rowEl.scrollIntoView("),
            "focusAndInspectExecution must not call scrollIntoView on rowEl");
        Assertions.assertFalse(jsContent.contains("targetRow.scrollIntoView("),
            "pageTestBase initialization must not call scrollIntoView on targetRow");

        // 3. Scroll position preservation logic in directory filters and HTMX swaps
        Assertions.assertTrue(jsContent.contains("savedScrollTop = scrollContainer ? scrollContainer.scrollTop : 0;"),
            "applyBatchDirectoryFilters must capture scroll position before filtering");
        Assertions.assertTrue(jsContent.contains("preservedMainViewScrollTop"),
            "report-manager.js must preserve scroll position across HTMX partial updates");

        // 4. CSS overflow-anchor: none on container elements to prevent browser-native scroll snapping
        final Path cssPath = Paths.get("src/main/resources/static/css/report-manager.css");
        Assertions.assertTrue(Files.exists(cssPath), "report-manager.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(cssContent.contains("#batchRunsHistoryTableBody") && cssContent.contains("overflow-anchor: none;"),
            "CSS must set overflow-anchor: none on #batchRunsHistoryTableBody to prevent browser scroll anchoring");
        Assertions.assertTrue(cssContent.contains(".area-trend-card") && cssContent.contains("cursor: default;"),
            "CSS must style .area-trend-card with cursor: default");
    }

    /**
     * Regression safety net for DEF-20261030-1605: Verifies that trend graph cards allow hover popovers
     * to render over other elements with overflow: visible, elevated z-index, and adaptive vertical orientation.
     */
    @Test
    public void testTrendGraphDotsPopoverElevationAndOverflowStyling() throws Exception
    {
        final Path cssPath = Paths.get("src/main/resources/static/dashboard-styles.css");
        Assertions.assertTrue(Files.exists(cssPath), "dashboard-styles.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        // 1. .widget-trend-card must have overflow: visible
        Assertions.assertTrue(cssContent.contains(".widget-trend-card {\n    overflow: visible;\n}"),
            ".widget-trend-card must have overflow: visible to allow hover popover to escape card boundaries");

        // 2. Active hovered trend point card must have elevated z-index
        Assertions.assertTrue(cssContent.contains(".widget-card:has(.widget-trend-point-wrapper:hover)")
            && cssContent.contains("z-index: 1000 !important;"),
            "Hovered trend card must have z-index: 1000 !important to display above other cards in grid");

        // 3. Popover must have elevated z-index and popover-below placement
        Assertions.assertTrue(cssContent.contains(".widget-trend-dot-popover") && cssContent.contains("z-index: 1050;"),
            ".widget-trend-dot-popover must have high z-index (1050) to render on top of surrounding elements");
        Assertions.assertTrue(cssContent.contains(".widget-trend-point-wrapper.popover-below .widget-trend-dot-popover"),
            "CSS must define .widget-trend-point-wrapper.popover-below rule for upper boundary points");

        // 4. HTML template must include popover-below dynamic class logic
        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(htmlPath), "dashboard-widgets.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(htmlContent.contains("popover-below"),
            "dashboard-widgets.html must include popover-below class dynamically for points near upper edge");
    }

    /**
     * Regression safety net for DEF-20261030-1615: Verifies that widget-executions-modal.html
     * renders highlighted badges (.run-meta-chip.chip-env, .chip-browser, .js-run-status-badge),
     * contains the exclude button (.btn-exclude-run) with excludeWidgetRun onclick, and provides
     * client-side exclusion management in report-manager.js.
     */
    @Test
    public void testWidgetExecutionsModalBadgeHighlightAndExclusionSyntax() throws Exception
    {
        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/widget-executions-modal.html");
        Assertions.assertTrue(Files.exists(htmlPath), "widget-executions-modal.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        // 1. Exclude button and click handler
        Assertions.assertTrue(htmlContent.contains("btn-exclude-run"),
            "widget-executions-modal.html must include .btn-exclude-run on run headers");
        Assertions.assertTrue(htmlContent.contains("excludeWidgetRun("),
            "widget-executions-modal.html must bind excludeWidgetRun on exclude button");
        Assertions.assertTrue(htmlContent.contains("clearExcludedRunsInModal("),
            "widget-executions-modal.html must support clearExcludedRunsInModal");

        // 2. Highlighted badges
        Assertions.assertTrue(htmlContent.contains("run-meta-chip chip-env"),
            "widget-executions-modal.html must style environment as .run-meta-chip.chip-env");
        Assertions.assertTrue(htmlContent.contains("js-run-status-badge"),
            "widget-executions-modal.html must mark status breakdown badges with .js-run-status-badge");
        Assertions.assertTrue(htmlContent.contains("badge-status badge-pass clickable-badge js-run-status-badge"),
            "widget-executions-modal.html must make pass badge clickable and highlighted");

        // 3. JavaScript functions in report-manager.js
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(jsContent.contains("function excludeWidgetRun("),
            "report-manager.js must define excludeWidgetRun");
        Assertions.assertTrue(jsContent.contains("window.excludeWidgetRun = excludeWidgetRun;"),
            "report-manager.js must expose excludeWidgetRun globally");
        Assertions.assertTrue(jsContent.contains("function clearExcludedRunsInModal("),
            "report-manager.js must define clearExcludedRunsInModal");
        Assertions.assertTrue(jsContent.contains("window.clearExcludedRunsInModal = clearExcludedRunsInModal;"),
            "report-manager.js must expose clearExcludedRunsInModal globally");
        Assertions.assertTrue(jsContent.contains("active-filter-badge"),
            "report-manager.js must highlight active-filter-badge on status filter toggle");
    }

    /**
     * Verifies that trend chart legend selection functions and DOM bindings exist across
     * report-manager.js, report-manager.css, batch-history.html, and dashboard-widgets.html.
     */
    @Test
    public void testTrendChartLegendSelectionSyntax() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(jsContent.contains("function toggleMainTrendLegendSelect("),
            "report-manager.js must define toggleMainTrendLegendSelect");
        Assertions.assertTrue(jsContent.contains("window.toggleMainTrendLegendSelect = toggleMainTrendLegendSelect;"),
            "report-manager.js must expose toggleMainTrendLegendSelect globally");
        Assertions.assertTrue(jsContent.contains("function toggleSuiteEvolutionLegendSelect("),
            "report-manager.js must define toggleSuiteEvolutionLegendSelect");
        Assertions.assertTrue(jsContent.contains("window.toggleSuiteEvolutionLegendSelect = toggleSuiteEvolutionLegendSelect;"),
            "report-manager.js must expose toggleSuiteEvolutionLegendSelect globally");
        Assertions.assertTrue(jsContent.contains("function toggleWidgetTrendLayerSelect("),
            "report-manager.js must define toggleWidgetTrendLayerSelect");
        Assertions.assertTrue(jsContent.contains("window.toggleWidgetTrendLayerSelect = toggleWidgetTrendLayerSelect;"),
            "report-manager.js must expose toggleWidgetTrendLayerSelect globally");
        Assertions.assertTrue(jsContent.contains("function extractLinePathPoints("),
            "report-manager.js must define extractLinePathPoints");

        final Path cssPath = Paths.get("src/main/resources/static/css/report-manager.css");
        Assertions.assertTrue(Files.exists(cssPath), "report-manager.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(cssContent.contains(".legend-item.is-clickable"),
            "report-manager.css must define .legend-item.is-clickable styling");
        Assertions.assertTrue(cssContent.contains(".legend-item.is-active"),
            "report-manager.css must define .legend-item.is-active styling");
        Assertions.assertTrue(cssContent.contains(".legend-item.is-dimmed"),
            "report-manager.css must define .legend-item.is-dimmed styling");
        Assertions.assertTrue(cssContent.contains(".ratio-legend-chip.is-clickable"),
            "report-manager.css must define .ratio-legend-chip.is-clickable styling");

        final Path dashboardWidgetsHtmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(dashboardWidgetsHtmlPath), "dashboard-widgets.html must exist: " + dashboardWidgetsHtmlPath.toAbsolutePath());
        final String dashboardWidgetsHtmlContent = Files.readString(dashboardWidgetsHtmlPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(dashboardWidgetsHtmlContent.contains("widget-trend-layer-line"),
            "dashboard-widgets.html must render .widget-trend-layer-line paths");
        Assertions.assertTrue(dashboardWidgetsHtmlContent.contains("onclick=\"event.stopPropagation(); toggleWidgetTrendLayerSelect(this)\""),
            "dashboard-widgets.html must bind toggleWidgetTrendLayerSelect(this) with event.stopPropagation via standard HTML onclick");
        Assertions.assertFalse(dashboardWidgetsHtmlContent.contains("th:onclick="),
            "dashboard-widgets.html must not use th:onclick string expressions on trend legend chips");
        Assertions.assertFalse(dashboardWidgetsHtmlContent.contains("clickable-widget-card"),
            "dashboard-widgets.html must not make entire widget cards clickable");
    }

    @Test
    public void testDashboardWidgetDragAutoScrollEnginePresent() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(jsContent.contains("function getDashboardScrollContainer("),
            "report-manager.js must define getDashboardScrollContainer");
        Assertions.assertTrue(jsContent.contains("function updateDragAutoScrollOnPointer("),
            "report-manager.js must define updateDragAutoScrollOnPointer");
        Assertions.assertTrue(jsContent.contains("function startDragAutoScrollLoop("),
            "report-manager.js must define startDragAutoScrollLoop");
        Assertions.assertTrue(jsContent.contains("function stopDragAutoScroll("),
            "report-manager.js must define stopDragAutoScroll");
        Assertions.assertTrue(jsContent.contains("requestAnimationFrame(step)"),
            "report-manager.js must use requestAnimationFrame for drag auto-scrolling");
        Assertions.assertTrue(jsContent.contains("cancelAnimationFrame(dragAutoScrollRafId)"),
            "report-manager.js must cancel animation frame on drag stop");
        Assertions.assertTrue(jsContent.contains("window.addEventListener('wheel'"),
            "report-manager.js must listen to wheel events during drag to allow scrolling");
        Assertions.assertTrue(jsContent.contains("grid.ondragover = (e) =>"),
            "report-manager.js must handle dragover on grid container");
        Assertions.assertTrue(jsContent.contains("grid.ondrop = (e) =>"),
            "report-manager.js must handle drop on grid container");
    }

    @Test
    public void testUnitedWidgetContainerSeamlessLayoutAndThymeleafPrecedenceSafety() throws Exception
    {
        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(htmlPath), "dashboard-widgets.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(htmlContent.contains("widget-container-unified-row"),
            "dashboard-widgets.html must render unified row for container widgets");
        Assertions.assertTrue(htmlContent.contains("widget-container-section"),
            "dashboard-widgets.html must render sections for container children");
        Assertions.assertTrue(htmlContent.contains("widget-container-pie-section"),
            "dashboard-widgets.html must style pie chart section in container");
        Assertions.assertTrue(htmlContent.contains("widget-container-ratios-section"),
            "dashboard-widgets.html must style ratio bar section in container");
        Assertions.assertTrue(htmlContent.contains("widget-container-divider"),
            "dashboard-widgets.html must render divider between container sections");

        // Safety assertion against Thymeleaf 3 precedence bug: th:replace must NOT be on the same th:block as th:if
        final boolean hasThBlockPrecedenceRisk = htmlContent.lines()
            .anyMatch(line -> line.contains("<th:block") && line.contains("th:if=") && line.contains("th:replace="));
        Assertions.assertFalse(hasThBlockPrecedenceRisk,
            "dashboard-widgets.html must never combine th:if and th:replace on the same th:block tag due to Thymeleaf 3 attribute precedence");

        final Path cssPath = Paths.get("src/main/resources/static/dashboard-styles.css");
        Assertions.assertTrue(Files.exists(cssPath), "dashboard-styles.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(cssContent.contains(".widget-container-unified-row"),
            "dashboard-styles.css must define .widget-container-unified-row");
        Assertions.assertTrue(cssContent.contains(".widget-container-pie-section"),
            "dashboard-styles.css must define .widget-container-pie-section");
        Assertions.assertTrue(cssContent.contains(".widget-container-ratios-section"),
            "dashboard-styles.css must define .widget-container-ratios-section");
        Assertions.assertTrue(cssContent.contains(".widget-container-divider"),
            "dashboard-styles.css must define .widget-container-divider");

        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(jsContent.contains(".widget-container-section"),
            "report-manager.js must query .widget-container-section during cross-filtering");
        Assertions.assertTrue(jsContent.contains("function parseContainerDataset("),
            "report-manager.js must define parseContainerDataset");
        Assertions.assertTrue(jsContent.contains("function matchesVariationFilter("),
            "report-manager.js must define matchesVariationFilter");
        Assertions.assertTrue(jsContent.contains("function recalculateContainerPieChart("),
            "report-manager.js must define recalculateContainerPieChart");
        Assertions.assertTrue(jsContent.contains("function recalculateContainerRatioBar("),
            "report-manager.js must define recalculateContainerRatioBar");
        Assertions.assertTrue(htmlContent.contains("widget-container-dataset"),
            "dashboard-widgets.html must render widget-container-dataset for cross-filtering");
        Assertions.assertTrue(htmlContent.contains("w.containerVariationsJson"),
            "dashboard-widgets.html must bind w.containerVariationsJson");
        Assertions.assertTrue(htmlContent.contains("data-tag-key="),
            "dashboard-widgets.html must bind data-tag-key on container sections");
    }

    @Test
    public void testUnitedContainerCrossFilterStatusIsolationAndSafety() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(jsContent.contains("function normalizeStatusCategory("),
            "report-manager.js must define normalizeStatusCategory to prevent substring collisions");
        Assertions.assertTrue(jsContent.contains("function resolveSliceCount("),
            "report-manager.js must define resolveSliceCount for exact status count lookup");
        Assertions.assertTrue(jsContent.contains("function resolveDimensionCount("),
            "report-manager.js must define resolveDimensionCount for collision-free ratio bar count resolution");

        // Verify that unknown is checked before known in status evaluation
        final int unknownIdx = jsContent.indexOf("s.includes('unknown')");
        final int knownIdx = jsContent.indexOf("s.includes('known')");
        Assertions.assertTrue(unknownIdx != -1 && knownIdx != -1,
            "report-manager.js must inspect unknown and known statuses");
        Assertions.assertTrue(unknownIdx < knownIdx,
            "report-manager.js must check unknown status prior to known status to prevent substring hijacking");
    }

    @Test
    public void testUnitedContainerRatioBarDiscreteTagIsolation() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // Strict discrete dimension comparison in matchesVariationFilter
        Assertions.assertTrue(jsContent.contains("return b === val;"),
            "report-manager.js must enforce strict equality for browser filter matching");
        Assertions.assertTrue(jsContent.contains("return l === val;"),
            "report-manager.js must enforce strict equality for locale filter matching");
        Assertions.assertTrue(jsContent.contains("return e === val;"),
            "report-manager.js must enforce strict equality for environment filter matching");

        // Strict dimension count resolution in resolveDimensionCount
        Assertions.assertTrue(jsContent.contains("if (k.toLowerCase().trim() === rl) return counts[k];"),
            "resolveDimensionCount must use strict equality matching instead of substring inclusion");
        Assertions.assertFalse(jsContent.contains("kl.includes(rl) || rl.includes(kl)"),
            "resolveDimensionCount must not perform fuzzy bidirectional substring matching");
    }

    @Test
    public void testUnitedContainerMetricAwarePieChartDispatchingAndCostsPreservation() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // Metric-aware pie chart handlers must exist
        Assertions.assertTrue(jsContent.contains("function recalculateCostsPieChart("),
            "report-manager.js must define recalculateCostsPieChart for COSTS pie widgets");
        Assertions.assertTrue(jsContent.contains("function recalculateTagPieChart("),
            "report-manager.js must define recalculateTagPieChart for TAG pie widgets");
        Assertions.assertTrue(jsContent.contains("function recalculateAiUsagePieChart("),
            "report-manager.js must define recalculateAiUsagePieChart for AI_USAGE pie widgets");
        Assertions.assertTrue(jsContent.contains("function recalculateStatusPieChart("),
            "report-manager.js must define recalculateStatusPieChart for STATUS pie widgets");
        Assertions.assertTrue(jsContent.contains("function recalculateBugsPieChart("),
            "report-manager.js must define recalculateBugsPieChart for BUGS pie widgets");

        // Verify dispatching logic in recalculateContainerPieChart
        Assertions.assertTrue(jsContent.contains("if (metric === 'COSTS')"),
            "recalculateContainerPieChart must dispatch to recalculateCostsPieChart when metric is COSTS");
        Assertions.assertTrue(jsContent.contains("else if (metric === 'BUGS')"),
            "recalculateContainerPieChart must dispatch to recalculateBugsPieChart when metric is BUGS");
        Assertions.assertTrue(jsContent.contains("subEl.textContent = 'COST';"),
            "recalculateCostsPieChart must preserve 'COST' center label rather than clobbering with 'PASS'");
        Assertions.assertTrue(jsContent.contains("subEl.textContent = 'BUGS';"),
            "recalculateBugsPieChart must preserve 'BUGS' center label rather than clobbering with 'PASS'");
    }

    @Test
    public void testUnitedContainerCostsPieCrossFilteringSupport() throws Exception
    {
        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(htmlPath), "dashboard-widgets.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        // Verify data-filter-dim binding for COSTS metric
        Assertions.assertTrue(htmlContent.contains("w.metric.name() == 'COSTS' ? 'costs'"),
            "dashboard-widgets.html must map COSTS metric to 'costs' filter dimension");

        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // Verify matchesVariationFilter handles costs and token breakdown filters
        Assertions.assertTrue(jsContent.contains("dim === 'costs' || dim === 'tokens' || val.includes('token') || val.includes('cost')"),
            "matchesVariationFilter must recognize costs, tokens, and token label filters");
        Assertions.assertTrue(jsContent.contains("(v.tok && Number(v.tok) > 0) || (v.c && Number(v.c) > 0) || v.m === 'AI'"),
            "matchesVariationFilter must evaluate token usage or AI execution mode for costs filters");

        // Verify resolveVariationDimensionValue handles costs dimension
        Assertions.assertTrue(jsContent.contains("if (d === 'costs' || d === 'tokens')"),
            "resolveVariationDimensionValue must support costs and tokens dimensions");
    }

    @Test
    public void testUnitedContainerBugsWidgetsCrossFilteringSupport() throws Exception
    {
        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(htmlPath), "dashboard-widgets.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        // Verify data-filter-dim binding for BUGS metric
        Assertions.assertTrue(htmlContent.contains("w.metric.name() == 'BUGS' ? 'severity'"),
            "dashboard-widgets.html must map BUGS metric to 'severity' filter dimension");

        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // Verify matchesVariationFilter handles bug severities (v.bsev)
        Assertions.assertTrue(jsContent.contains("if (Array.isArray(v.bsev))"),
            "matchesVariationFilter must recognize and evaluate v.bsev array");

        // Verify ratio bar BUGS support
        Assertions.assertTrue(jsContent.contains("if (metric === 'BUGS')"),
            "recalculateContainerRatioBar must handle BUGS metric");

        // Verify text widget BUGS support
        Assertions.assertTrue(jsContent.contains("} else if (metric === 'BUGS')"),
            "recalculateContainerText must handle BUGS metric");

        // Verify recalculateBugsPieChart export
        Assertions.assertTrue(jsContent.contains("window.recalculateBugsPieChart = recalculateBugsPieChart;"),
            "report-manager.js must export recalculateBugsPieChart to global window scope");
    }

    @Test
    public void testUnitedContainerSeparateChildWidgetTemplateBinding() throws Exception
    {
        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(htmlPath), "dashboard-widgets.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        // Verify separate-child endpoint binding in child widget section header
        Assertions.assertTrue(htmlContent.contains("/widgets/separate-child"),
            "dashboard-widgets.html must include /widgets/separate-child endpoint binding");
        Assertions.assertTrue(htmlContent.contains("title=\"Separate from group\""),
            "dashboard-widgets.html must provide 'Separate from group' action title for child widgets");
    }

    @Test
    public void testWidgetContainerContributingRunsGeneralButtonAndNoSingleChildButton() throws Exception
    {
        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(htmlPath), "dashboard-widgets.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        // Verify container header has general openWidgetExecutionsModal button
        Assertions.assertTrue(
            htmlContent.contains("openWidgetExecutionsModal(\\'' + ${w.id} + '\\')"),
            "dashboard-widgets.html container header must trigger openWidgetExecutionsModal for ${w.id}"
        );

        // Verify child sections do NOT have openWidgetExecutionsModal for child widgets
        Assertions.assertFalse(
            htmlContent.contains("openWidgetExecutionsModal(\\'' + ${child.id} + '\\')"),
            "dashboard-widgets.html child sections must not render openWidgetExecutionsModal for ${child.id}"
        );
    }

    @Test
    public void testUnitedContainerMultiRowGridLayoutAndFullSpanSupport() throws Exception
    {
        final Path cssPath = Paths.get("src/main/resources/static/dashboard-styles.css");
        Assertions.assertTrue(Files.exists(cssPath), "dashboard-styles.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        // Verify responsive multi-row grid configuration to prevent single-row squishing
        Assertions.assertTrue(cssContent.contains("display: grid;"),
            "dashboard-styles.css must configure display: grid for .widget-container-unified-row");
        Assertions.assertTrue(cssContent.contains("repeat(auto-fit, minmax(min(100%, 440px), 1fr))"),
            "dashboard-styles.css must define responsive multi-row auto-fit minmax grid columns");

        // Verify sub-panel framing and full-span styling
        Assertions.assertTrue(cssContent.contains(".widget-container-section.widget-container-full-span"),
            "dashboard-styles.css must define .widget-container-section.widget-container-full-span");
        Assertions.assertTrue(cssContent.contains("grid-column: 1 / -1;"),
            "dashboard-styles.css must span full width for expansive widget presentations");

        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(htmlPath), "dashboard-widgets.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(htmlContent.contains("widget-container-full-span"),
            "dashboard-widgets.html must bind widget-container-full-span class dynamically");
    }

    @Test
    public void testUnitedContainerTextWidgetCrossFilterDispatching() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // Verify recalculateContainerText function definition
        Assertions.assertTrue(jsContent.contains("function recalculateContainerText("),
            "report-manager.js must define recalculateContainerText for TEXT widget presentations");
        Assertions.assertTrue(jsContent.contains("window.recalculateContainerText = recalculateContainerText;"),
            "report-manager.js must export recalculateContainerText to global window scope");

        // Verify metric support inside recalculateContainerText
        Assertions.assertTrue(jsContent.contains("metric === 'AI_USAGE'"),
            "recalculateContainerText must support AI_USAGE metric");
        Assertions.assertTrue(jsContent.contains("metric === 'STATUS'"),
            "recalculateContainerText must support STATUS metric");
        Assertions.assertTrue(jsContent.contains("metric === 'COSTS'"),
            "recalculateContainerText must support COSTS metric");

        // Verify wiring in onContainerWidgetFilterClick
        Assertions.assertTrue(jsContent.contains("else if (presentation === 'TEXT' || presentation === 'TEXT_WITH_SMALL_TREND') {\n                recalculateContainerText(childCard, matchedVariations, true);\n            }"),
            "onContainerWidgetFilterClick must dispatch to recalculateContainerText on filter activation");

        // Verify wiring in clearContainerCrossFilter
        Assertions.assertTrue(jsContent.contains("else if (presentation === 'TEXT' || presentation === 'TEXT_WITH_SMALL_TREND') {\n            recalculateContainerText(childCard, allVariations, false);\n        }"),
            "clearContainerCrossFilter must dispatch to recalculateContainerText on filter reset");
    }

    @Test
    public void testUnitedContainerListWidgetCrossFilterDispatchingAndRecalculation() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // Verify recalculateContainerList function definition and export
        Assertions.assertTrue(jsContent.contains("function recalculateContainerList(sec, matchedVariations, isFiltered)"),
            "report-manager.js must define recalculateContainerList with isFiltered parameter");
        Assertions.assertTrue(jsContent.contains("window.recalculateContainerList = recalculateContainerList;"),
            "report-manager.js must export recalculateContainerList to global window scope");

        // Verify vid, key, and bug isolation matching
        Assertions.assertTrue(jsContent.contains("matchedVids.has(subVarId)"),
            "recalculateContainerList must match sub-item canonical variation ID against matchedVids");
        Assertions.assertTrue(jsContent.contains("subVarId && occ.vid && subVarId === occ.vid"),
            "recalculateContainerList must strictly match subitem variation ID to occurrence variation ID");
        Assertions.assertTrue(jsContent.contains("subVarKey && occ.k && subVarKey === occ.k"),
            "recalculateContainerList must strictly match subitem variation key to occurrence variation key");
        Assertions.assertTrue(jsContent.contains("envTags.length > 0 && !envTags.includes(occ.env)"),
            "recalculateContainerList must use set containment for multi-valued fallback environment tags");
        Assertions.assertTrue(jsContent.contains("st.startsWith('environment:')") && jsContent.contains("st.startsWith('location:')"),
            "recalculateContainerList must normalize environment and location tag prefixes");
        Assertions.assertFalse(jsContent.contains("matchedBugTickets.has(subLabel)"),
            "recalculateContainerList must NOT match bug tickets by label to prevent cross-variation ticket leakage");
        Assertions.assertTrue(jsContent.contains("const matchedBugOccurrences = new Map();"),
            "recalculateContainerList must build matchedBugOccurrences map for BUGS metric");
        Assertions.assertTrue(jsContent.contains("matchedBugOccurrences.get(normSubTicket)"),
            "recalculateContainerList must lookup bug subitem label in matchedBugOccurrences");

        // Verify group count and section summary updates
        Assertions.assertTrue(jsContent.contains("countSpan.setAttribute('data-original-val'"),
            "recalculateContainerList must cache original list group count in data-original-val");
        Assertions.assertTrue(jsContent.contains("sumEl.setAttribute('data-original-summary'"),
            "recalculateContainerList must cache original section summary in data-original-summary");

        // Verify dispatch in onContainerWidgetFilterClick and clearContainerCrossFilter
        Assertions.assertTrue(jsContent.contains("recalculateContainerList(childCard, matchedVariations, true);"),
            "onContainerWidgetFilterClick must pass isFiltered=true to recalculateContainerList");
        Assertions.assertTrue(jsContent.contains("recalculateContainerList(childCard, allVariations, false);"),
            "clearContainerCrossFilter must pass isFiltered=false to recalculateContainerList");

        // Verify template bindings in dashboard-widgets.html
        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(htmlPath), "dashboard-widgets.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(htmlContent.contains("data-variation-key=${sub.variationKey}"),
            "dashboard-widgets.html must bind data-variation-key on list subitems");
        Assertions.assertTrue(htmlContent.contains("data-variation-id=${sub.variationId}"),
            "dashboard-widgets.html must bind data-variation-id on list subitems");
        Assertions.assertTrue(htmlContent.contains("data-subitem-tags=${sub.hasTags() ? #strings.listJoin(sub.tags, ',') : ''}"),
            "dashboard-widgets.html must bind data-subitem-tags on list subitems");
    }

    @Test
    public void testUnitedContainerMultiValueCrossFilteringSupport() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // Verify multi-value filtering functions and exports
        Assertions.assertTrue(jsContent.contains("function matchesContainerFilters(v, activeFilters, excludeDim)"),
            "report-manager.js must define matchesContainerFilters supporting multi-value dimension filtering");
        Assertions.assertTrue(jsContent.contains("window.matchesContainerFilters = matchesContainerFilters;"),
            "report-manager.js must export matchesContainerFilters to global window scope");

        Assertions.assertTrue(jsContent.contains("function getContainerActiveFilters(container)"),
            "report-manager.js must define getContainerActiveFilters for container-scoped filter state");
        Assertions.assertTrue(jsContent.contains("window.getContainerActiveFilters = getContainerActiveFilters;"),
            "report-manager.js must export getContainerActiveFilters to global window scope");

        Assertions.assertTrue(jsContent.contains("function removeContainerSingleFilter("),
            "report-manager.js must define removeContainerSingleFilter for individual chip removal");
        Assertions.assertTrue(jsContent.contains("window.removeContainerSingleFilter = removeContainerSingleFilter;"),
            "report-manager.js must export removeContainerSingleFilter to global window scope");

        Assertions.assertTrue(jsContent.contains("function renderContainerFilterStrip(container, activeFilters)"),
            "report-manager.js must define renderContainerFilterStrip for dynamic multi-chip strip rendering");
        Assertions.assertTrue(jsContent.contains("window.renderContainerFilterStrip = renderContainerFilterStrip;"),
            "report-manager.js must export renderContainerFilterStrip to global window scope");

        // Verify disjunctive same-dimension matching
        Assertions.assertTrue(jsContent.contains("matchesAnyInDim = vals.some(val => matchesVariationFilter(v, dim, val))"),
            "matchesContainerFilters must evaluate disjunctive matching across multiple values in the same dimension");

        // Verify template bindings in dashboard-widgets.html
        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(htmlPath), "dashboard-widgets.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(htmlContent.contains("container-filter-chips"),
            "dashboard-widgets.html must provide container-filter-chips wrapper for dynamic filter chips");
        Assertions.assertTrue(htmlContent.contains("btn-clear-all-container-filters"),
            "dashboard-widgets.html must provide btn-clear-all-container-filters button");
    }

    @Test
    public void testUnitedContainerAreaAndLocaleRatioBarNormalizationSupport() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // Verify Area normalization in resolveVariationDimensionValue
        Assertions.assertTrue(jsContent.contains("const clean = raw.replace(/^@/, '').replace(/\\s*\\(default\\)$/i, '').trim();"),
            "resolveVariationDimensionValue must strip leading @ and trailing (default) from area dimension values");
        Assertions.assertTrue(jsContent.contains("return clean.length > 0 ? clean : 'Default';"),
            "resolveVariationDimensionValue must return clean area fallback without @ prefix");

        // Verify Area normalization in resolveDimensionCount
        Assertions.assertTrue(jsContent.contains("const cleanRl = rl.replace(/^@/, '').replace(/\\s*\\(default\\)$/i, '').trim();"),
            "resolveDimensionCount must strip leading @ and trailing (default) from comparison target");
        Assertions.assertTrue(jsContent.contains("if (cleanK === cleanRl) return counts[k];"),
            "resolveDimensionCount must perform exact equality on normalized area values");

        // Verify Area normalization in matchesVariationFilter
        Assertions.assertTrue(jsContent.contains("const normA = normalizeAreaFilterVal(v.a || '');"),
            "matchesVariationFilter must use normalizeAreaFilterVal for area filter matching");

        // Verify Locale/Location and Environment/Env synonym canonicalization in matchesContainerFilters
        Assertions.assertTrue(jsContent.contains("const normExclude = (cleanExclude === 'location' ? 'locale' : (cleanExclude === 'environment' ? 'env' : cleanExclude));"),
            "matchesContainerFilters must canonicalize location/locale and environment/env for excludeDim facet exclusion");
        Assertions.assertTrue(jsContent.contains("const normDim = (cleanDim === 'location' ? 'locale' : (cleanDim === 'environment' ? 'env' : cleanDim));"),
            "matchesContainerFilters must canonicalize location/locale and environment/env for active filter dimensions");
    }

    @Test
    public void testUnitedContainerListWidgetFilteringHidesNonMatchingItems() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // Verify recalculateContainerList hides non-matching subitems and empty groups
        Assertions.assertTrue(jsContent.contains("sub.style.display = 'none';"),
            "recalculateContainerList must hide subitems that do not match active filter");
        Assertions.assertTrue(jsContent.contains("sub.style.display = '';"),
            "recalculateContainerList must display subitems that match active filter");
        Assertions.assertTrue(jsContent.contains("grp.style.display = 'none';"),
            "recalculateContainerList must hide list groups that contain zero visible subitems");

        // Verify robust display: none !important and is-filtered-hidden class toggling to override .d-flex
        Assertions.assertTrue(jsContent.contains("sub.style.setProperty('display', 'none', 'important');"),
            "recalculateContainerList must set display: none !important on non-matching subitems to override .d-flex");
        Assertions.assertTrue(jsContent.contains("sub.classList.add('is-filtered-hidden');"),
            "recalculateContainerList must add is-filtered-hidden class to non-matching subitems");
        Assertions.assertTrue(jsContent.contains("grp.style.setProperty('display', 'none', 'important');"),
            "recalculateContainerList must set display: none !important on empty groups to override .d-flex");
        Assertions.assertTrue(jsContent.contains("grp.classList.add('is-filtered-hidden');"),
            "recalculateContainerList must add is-filtered-hidden class to empty groups");

        // Verify CSS defines high-specificity override for .is-filtered-hidden with display: none !important
        final Path cssPath = Paths.get("src/main/resources/static/dashboard-styles.css");
        Assertions.assertTrue(Files.exists(cssPath), "dashboard-styles.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);
        Assertions.assertTrue(cssContent.contains(".widget-list-group.is-filtered-hidden,") &&
                              cssContent.contains(".widget-list-subitem-row.is-filtered-hidden,") &&
                              cssContent.contains("display: none !important;"),
            "dashboard-styles.css must define display: none !important for .is-filtered-hidden list items and groups");

        // Verify empty state message handling when all list items are filtered out
        Assertions.assertTrue(jsContent.contains("widget-list-empty-filter"),
            "recalculateContainerList must manage widget-list-empty-filter element for zero matches");

        // Verify LIST presentation bypasses facet exclusion to strictly filter list items across all dimensions
        Assertions.assertTrue(jsContent.contains("const excludeDimForCard = (presentation === 'LIST') ? null : cardDim;"),
            "updateContainerCardsVisualState must pass excludeDim=null for LIST presentation to strictly filter items");
    }

    @Test
    public void testUnitedContainerHealedAndAiInteractiveFilterBadges() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // Verify composite qualifier parsing in matchesVariationFilter
        Assertions.assertTrue(jsContent.contains("const isHealedFilter = /\\(healed\\)/i.test(rawVal) || val === 'healed';"),
            "matchesVariationFilter must recognize (healed) sub-mode filter qualifier");
        Assertions.assertTrue(jsContent.contains("const isAiFilter = /\\(ai\\)/i.test(rawVal) || val === 'ai';"),
            "matchesVariationFilter must recognize (ai) sub-mode filter qualifier");
        Assertions.assertTrue(jsContent.contains("val = val.replace(/\\s*\\((healed|ai|clean)\\)/i, '').trim();"),
            "matchesVariationFilter must strip sub-mode qualifier to compare against base dimension value");

        // Verify superset/subset disambiguation in onContainerWidgetFilterClick
        Assertions.assertTrue(jsContent.contains("if (lowerVal.endsWith('(healed)') || lowerVal.endsWith('(ai)') || lowerVal.endsWith('(clean)'))"),
            "onContainerWidgetFilterClick must detect qualified filter activation");

        // Verify dynamic badge setup in recalculateStatusPieChart and recalculateContainerText
        Assertions.assertTrue(jsContent.contains("healedBadge.setAttribute('data-filter-dim', 'status');"),
            "Status pie chart and text KPI recalculators must set data-filter-dim on healed badge");
        Assertions.assertTrue(jsContent.contains("healedBadge.setAttribute('data-filter-val', `${normKey} (Healed)`);")
            || jsContent.contains("healedBadge.setAttribute('data-filter-val', 'Passed (Healed)');"),
            "Status recalculators must set data-filter-val with (Healed) qualifier");

        // Verify template bindings in dashboard-widgets.html
        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(htmlPath), "dashboard-widgets.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(htmlContent.contains("data-filter-val=${s.label + ' (Healed)'}"),
            "dashboard-widgets.html must bind data-filter-val with (Healed) qualifier in pie slices");
        Assertions.assertTrue(htmlContent.contains("data-filter-val=${s.label + ' (AI)'}"),
            "dashboard-widgets.html must bind data-filter-val with (AI) qualifier in pie slices");
        Assertions.assertTrue(htmlContent.contains("data-filter-val=${seg.name + ' (Healed)'}"),
            "dashboard-widgets.html must bind data-filter-val with (Healed) qualifier in ratio bar chips");
        Assertions.assertTrue(htmlContent.contains("data-filter-val=${item.label + ' (Healed)'}"),
            "dashboard-widgets.html must bind data-filter-val with (Healed) qualifier in list groups and subitems");

        // Verify custom active filter styling in dashboard-styles.css
        final Path cssPath = Paths.get("src/main/resources/static/dashboard-styles.css");
        Assertions.assertTrue(Files.exists(cssPath), "dashboard-styles.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(cssContent.contains(".badge-healed-inline.widget-interactive-filter.is-filter-active"),
            "dashboard-styles.css must style active healed filter badge with emerald glow");
        Assertions.assertTrue(cssContent.contains(".badge-ai-inline.widget-interactive-filter.is-filter-active"),
            "dashboard-styles.css must style active AI filter badge with violet glow");
        Assertions.assertTrue(cssContent.contains(".widget-interactive-filter:has(.is-filter-active)"),
            "dashboard-styles.css must maintain full opacity for parent filter elements containing active badges");

        // Verify let binding for val to allow safe reassignment without TypeError
        Assertions.assertTrue(jsContent.contains("let val = rawVal.toLowerCase();"),
            "matchesVariationFilter must declare val with let to prevent TypeError: Assignment to constant variable");
    }

    @Test
    public void testMatchesVariationFilterRuntimeExecutionWithoutConstTypeError() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());

        // Check if node is available to execute runtime validation
        final ProcessBuilder pbCheck = new ProcessBuilder("node", "-v");
        try
        {
            final Process pCheck = pbCheck.start();
            final int exitCode = pCheck.waitFor();
            if (exitCode != 0)
            {
                return;
            }
        }
        catch (final Exception e)
        {
            // Node not installed on this test environment, skip execution
            return;
        }

        final String testScript = String.join("\n",
            "const fs = require('fs');",
            "const code = fs.readFileSync('" + jsPath.toAbsolutePath().toString().replace("\\", "/") + "', 'utf8');",
            "const vm = require('vm');",
            "const mockDoc = { querySelector: () => null, querySelectorAll: () => [], getElementById: () => null, addEventListener: () => {}, body: { addEventListener: () => {} } };",
            "const mockWin = { addEventListener: () => {}, document: mockDoc, ResizeObserver: class { observe() {} unobserve() {} disconnect() {} } };",
            "mockDoc.defaultView = mockWin;",
            "const context = { window: mockWin, document: mockDoc, console: console, ResizeObserver: mockWin.ResizeObserver, setTimeout: setTimeout, clearTimeout: clearTimeout };",
            "vm.createContext(context);",
            "vm.runInContext(code, context);",
            "context.v = { s: 'PASSED', b: 'Chrome_1920x1080', h: false };",
            "const matchBrowser = vm.runInContext('matchesVariationFilter(v, \"browser\", \"Chrome_1920x1080\")', context);",
            "if (matchBrowser !== true) { throw new Error('Expected matchBrowser === true'); }",
            "context.vHealed = { s: 'PASSED', b: 'Chrome_1920x1080', h: true };",
            "const matchHealed = vm.runInContext('matchesVariationFilter(vHealed, \"status\", \"Passed (Healed)\")', context);",
            "if (matchHealed !== true) { throw new Error('Expected matchHealed === true'); }",
            "const matchClean = vm.runInContext('matchesVariationFilter(v, \"status\", \"Passed (Healed)\")', context);",
            "if (matchClean !== false) { throw new Error('Expected matchClean === false'); }",
            "console.log('SUCCESS');"
        );

        final ProcessBuilder pb = new ProcessBuilder("node", "-e", testScript);
        final Process p = pb.start();
        final StringBuilder out = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                out.append(line).append("\n");
            }
        }
        final StringBuilder err = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                err.append(line).append("\n");
            }
        }

        final int exitCode = p.waitFor();
        Assertions.assertEquals(0, exitCode, "Node execution failed with error: " + err + "\nOutput: " + out);
        Assertions.assertTrue(out.toString().contains("SUCCESS"), "Expected SUCCESS output from runtime Node test");
    }

    @Test
    public void testUnitedContainerListWidgetCollapseDecoupledFromFilter() throws Exception
    {
        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(htmlPath), "dashboard-widgets.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        // Verify that widget-list-row is no longer an interactive filter element
        Assertions.assertFalse(htmlContent.contains("widget-list-row widget-interactive-filter"),
            "widget-list-row must not contain widget-interactive-filter class to decouple row from container filter");

        // Verify that the collapse chevron invokes toggleWidgetListItemCollapse without filtering
        Assertions.assertTrue(htmlContent.contains("toggleWidgetListItemCollapse(this, event)"),
            "widget-list-chevron must bind toggleWidgetListItemCollapse");
        Assertions.assertFalse(htmlContent.contains("toggleWidgetListItemCollapse(this, event); onContainerWidgetFilterClick"),
            "widget-list-chevron must not trigger onContainerWidgetFilterClick");

        // Verify that the filter area is scoped to the item label
        Assertions.assertTrue(htmlContent.contains("class=\"widget-list-label widget-interactive-filter"),
            "dashboard-widgets.html must designate .widget-list-label as the interactive filter element");
        Assertions.assertTrue(htmlContent.contains("data-filter-val=${item.label}"),
            "dashboard-widgets.html must bind data-filter-val to item.label on the label filter chip");

        // Verify report-manager.js handles chevron resolution directly when called from the collapse button
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);
        Assertions.assertTrue(jsContent.contains("rowEl.classList.contains('widget-list-chevron')"),
            "toggleWidgetListItemCollapse must resolve chevron directly if invoked from the collapse button element");

        // Verify dashboard-styles.css provides pointer cursors and styling for the chevron and label filter
        final Path cssPath = Paths.get("src/main/resources/static/dashboard-styles.css");
        Assertions.assertTrue(Files.exists(cssPath), "dashboard-styles.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);
        Assertions.assertTrue(cssContent.contains(".widget-list-chevron {") && cssContent.contains("cursor: pointer;"),
            "dashboard-styles.css must style .widget-list-chevron with cursor: pointer");
        Assertions.assertTrue(cssContent.contains(".widget-list-label.widget-interactive-filter {"),
            "dashboard-styles.css must style .widget-list-label.widget-interactive-filter");
    }

    @Test
    public void testUnitedContainerListWidgetAdaptiveFullSpanAndPanoramicRow() throws Exception
    {
        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(htmlPath), "dashboard-widgets.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        // Verify that dashboard-widgets.html renders the section full-span toggle button
        Assertions.assertTrue(htmlContent.contains("btn-widget-section-span"),
            "dashboard-widgets.html must define btn-widget-section-span button");
        Assertions.assertTrue(htmlContent.contains("toggleWidgetSectionSpan(this, event)"),
            "dashboard-widgets.html must bind toggleWidgetSectionSpan to button");

        // Verify dashboard-widgets.html renders list items in collapsed state by default
        Assertions.assertTrue(htmlContent.contains("widget-list-group collapsed"),
            "dashboard-widgets.html must render widget-list-group with collapsed class by default");
        Assertions.assertTrue(htmlContent.contains("widget-list-subitems is-collapsed-hidden") && htmlContent.contains("style=\"display: none;\""),
            "dashboard-widgets.html must render widget-list-subitems with is-collapsed-hidden and display: none by default");
        Assertions.assertTrue(htmlContent.contains("transform: rotate(-90deg);"),
            "dashboard-widgets.html must initialize widget-list-chevron with rotate(-90deg)");

        // Verify report-manager.js implements adaptive parent section full-span management
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(jsContent.contains("function updateWidgetParentSectionSpan(rowEl)"),
            "report-manager.js must define updateWidgetParentSectionSpan");
        Assertions.assertTrue(jsContent.contains("section.classList.add('widget-container-full-span')"),
            "updateWidgetParentSectionSpan must apply widget-container-full-span when items are expanded");
        Assertions.assertTrue(jsContent.contains("function toggleWidgetSectionSpan(btn, event)"),
            "report-manager.js must define toggleWidgetSectionSpan");

        // Verify dashboard-styles.css provides scroll cap and responsive panoramic single-row layout
        final Path cssPath = Paths.get("src/main/resources/static/dashboard-styles.css");
        Assertions.assertTrue(Files.exists(cssPath), "dashboard-styles.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(cssContent.contains(".widget-list-subitems {") && cssContent.contains("max-height: 520px;"),
            "dashboard-styles.css must configure max-height: 520px on .widget-list-subitems");
        Assertions.assertTrue(cssContent.contains(".widget-container-full-span .widget-list-subitem-row:not(.is-filtered-hidden)"),
            "dashboard-styles.css must scope panoramic flex layout with :not(.is-filtered-hidden) to prevent resurrecting filtered subitems");
        Assertions.assertTrue(cssContent.contains(".widget-container-full-span .widget-list-subitem-row.is-filtered-hidden"),
            "dashboard-styles.css must define elevated hiding rule for .widget-container-full-span .widget-list-subitem-row.is-filtered-hidden");
        Assertions.assertTrue(cssContent.contains(".btn-widget-section-span"),
            "dashboard-styles.css must style .btn-widget-section-span");
    }

    @Test
    public void testUniqueBugsPointWidgetsDeduplicationInJavaScript() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // Verify report-manager.js defines getBugSeverityRank
        Assertions.assertTrue(jsContent.contains("function getBugSeverityRank(sev)"),
            "report-manager.js must define getBugSeverityRank helper");

        // Verify extractVariationBugSeverities de-duplicates unique bugs by ticketKey
        Assertions.assertTrue(jsContent.contains("const uniqueBugs = new Map();"),
            "extractVariationBugSeverities must use uniqueBugs map to track unique tickets");
        Assertions.assertTrue(jsContent.contains("const ticketKey = cleanTicket.toLowerCase();"),
            "extractVariationBugSeverities must normalize ticketKey");
        Assertions.assertTrue(jsContent.contains("const totalBugs = uniqueBugs.size;"),
            "extractVariationBugSeverities must set totalBugs from uniqueBugs.size");

        // Verify recalculateContainerList counts unique bug tickets among visible subitems
        Assertions.assertTrue(jsContent.contains("if (metric === 'BUGS') {") && jsContent.contains("const uniqueTickets = new Set();"),
            "recalculateContainerList must count uniqueTickets when metric is BUGS");
        Assertions.assertTrue(jsContent.contains("const uniqueVisibleTickets = new Set();"),
            "recalculateContainerList must count uniqueVisibleTickets for section summary when metric is BUGS");
    }

    @Test
    public void testContainerCrossFilterDecouplesTagKeyFromNonTagMetrics() throws Exception
    {
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // Verify updateContainerCardsVisualState only uses data-tag-key when metric === 'TAG'
        Assertions.assertTrue(
            jsContent.contains("const cardDim = (metric === 'TAG')") &&
            jsContent.contains("(childCard.getAttribute('data-tag-key') || 'browser').toLowerCase().trim()"),
            "updateContainerCardsVisualState must only inspect data-tag-key for TAG metric to avoid pre-empting native dimensions"
        );

        // Verify recalculateContainerRatioBar only uses data-tag-key when metric === 'TAG'
        Assertions.assertTrue(
            jsContent.contains("const dim = (metric === 'TAG')") &&
            jsContent.contains("(sec.getAttribute('data-tag-key') || 'browser')"),
            "recalculateContainerRatioBar must only inspect data-tag-key when metric === 'TAG'"
        );

        // Verify modal disables widgetTagKeySelect when metric is not TAG
        final Path modalPath = Paths.get("src/main/resources/templates/fragments/widget-configurator-modal.html");
        Assertions.assertTrue(Files.exists(modalPath), "widget-configurator-modal.html must exist: " + modalPath.toAbsolutePath());
        final String modalContent = Files.readString(modalPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            modalContent.contains("th:disabled=\"${widget == null || widget.metric == null || widget.metric.name() != 'TAG'}\""),
            "widget-configurator-modal.html must disable widgetTagKeySelect for non-TAG metrics"
        );
        Assertions.assertTrue(
            modalContent.contains("tagSelect.disabled = (metric !== 'TAG');"),
            "widget-configurator-modal.html onWidgetMetricChanged must toggle disabled state of widgetTagKeySelect"
        );
        Assertions.assertFalse(
            modalContent.contains("widget.metric.name() == 'TAG')}\" th:checked=\"${widget != null && widget.presentation != null && widget.presentation.name() == 'PERLENKETTE'"),
            "widget-configurator-modal.html must not disable PERLENKETTE for TAG metric"
        );
        Assertions.assertFalse(
            modalContent.contains("perlenketteRadio.disabled = (metric === 'TAG');"),
            "widget-configurator-modal.html onWidgetMetricChanged must not disable perlenketteRadio for TAG"
        );
        Assertions.assertFalse(
            jsContent.contains("perlenketteRadio.disabled = (metric === 'TAG');"),
            "report-manager.js onWidgetMetricChanged must not disable perlenketteRadio for TAG"
        );

        // Node runtime validation: Status card with legacy data-tag-key="Area" must still be filtered when filtering by area
        final ProcessBuilder pbCheck = new ProcessBuilder("node", "-v");
        try
        {
            final Process pCheck = pbCheck.start();
            final int exitCode = pCheck.waitFor();
            if (exitCode != 0)
            {
                return;
            }
        }
        catch (final Exception e)
        {
            return;
        }

        final String testScript = String.join("\n",
            "const fs = require('fs');",
            "const code = fs.readFileSync('" + jsPath.toAbsolutePath().toString().replace("\\", "/") + "', 'utf8');",
            "const vm = require('vm');",
            "const mockDoc = { querySelector: () => null, querySelectorAll: () => [], getElementById: () => null, addEventListener: () => {}, body: { addEventListener: () => {} } };",
            "const mockWin = { addEventListener: () => {}, document: mockDoc, ResizeObserver: class { observe() {} unobserve() {} disconnect() {} } };",
            "mockDoc.defaultView = mockWin;",
            "const context = { window: mockWin, document: mockDoc, console: console, ResizeObserver: mockWin.ResizeObserver, setTimeout: setTimeout, clearTimeout: clearTimeout };",
            "vm.createContext(context);",
            "vm.runInContext(code, context);",
            "const vInter = { s: 'PASSED', b: 'Chrome_1920x1080', a: '@International' };",
            "const vCheckout = { s: 'PASSED', b: 'Chrome_1920x1080', a: '@Checkout' };",
            "const activeFilters = { area: new Set(['International']) };",
            "// Status card with polluted tagKey='Area': metric is STATUS, presentation is PIE_CHART",
            "const metric = 'STATUS';",
            "const tagKey = 'Area';",
            "const presentation = 'PIE_CHART';",
            "const cardDim = (metric === 'TAG') ? (tagKey || 'browser').toLowerCase().trim() : 'status';",
            "const excludeDimForCard = (presentation === 'LIST') ? null : cardDim;",
            "context.vInter = vInter;",
            "context.vCheckout = vCheckout;",
            "context.activeFilters = activeFilters;",
            "context.excludeDim = excludeDimForCard;",
            "const matchInter = vm.runInContext('matchesContainerFilters(vInter, activeFilters, excludeDim)', context);",
            "const matchCheckout = vm.runInContext('matchesContainerFilters(vCheckout, activeFilters, excludeDim)', context);",
            "if (matchInter !== true) { throw new Error('Expected vInter to match area filter'); }",
            "if (matchCheckout !== false) { throw new Error('Expected vCheckout to be filtered out by area filter, but was not!'); }",
            "console.log('SUCCESS');"
        );

        final ProcessBuilder pb = new ProcessBuilder("node", "-e", testScript);
        final Process p = pb.start();
        final StringBuilder out = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                out.append(line).append("\n");
            }
        }
        final StringBuilder err = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                err.append(line).append("\n");
            }
        }
        final int exitCode = p.waitFor();
        Assertions.assertEquals(0, exitCode, "Node execution failed with error: " + err + "\nOutput: " + out);
        Assertions.assertTrue(out.toString().contains("SUCCESS"), "Expected node test script output to contain SUCCESS");
    }

    @Test
    public void testWidgetConfiguratorModalNoDuplicateTitleInputAndHasNameSuggestion() throws IOException
    {
        final Path modalPath = Paths.get("src/main/resources/templates/fragments/widget-configurator-modal.html");
        Assertions.assertTrue(Files.exists(modalPath), "widget-configurator-modal.html must exist: " + modalPath.toAbsolutePath());
        final String modalContent = Files.readString(modalPath, StandardCharsets.UTF_8);

        // Verify exactly ONE widgetTitleInput exists in widget-configurator-modal.html
        final int firstIdx = modalContent.indexOf("id=\"widgetTitleInput\"");
        Assertions.assertTrue(firstIdx >= 0, "widget-configurator-modal.html must contain widgetTitleInput");
        final int secondIdx = modalContent.indexOf("id=\"widgetTitleInput\"", firstIdx + 1);
        Assertions.assertEquals(-1, secondIdx, "widget-configurator-modal.html must NOT contain duplicate widgetTitleInput elements");

        // Verify section 8 does NOT contain title input
        final int sizingSectionIdx = modalContent.indexOf("8. WIDGET SIZING");
        Assertions.assertTrue(sizingSectionIdx >= 0, "widget-configurator-modal.html must include 8. WIDGET SIZING section");
        final String afterSizing = modalContent.substring(sizingSectionIdx);
        final int nextSectionIdx = afterSizing.indexOf("9. ACTIVE WIDGETS MANAGEMENT ACCORDION");
        final String sizingBlock = nextSectionIdx >= 0 ? afterSizing.substring(0, nextSectionIdx) : afterSizing;
        Assertions.assertFalse(sizingBlock.contains("name=\"title\""), "Section 8 must not have a duplicate title input");

        // Verify dynamic name suggestion functions and bindings exist
        Assertions.assertTrue(modalContent.contains("computeSuggestedWidgetTitle"), "widget-configurator-modal.html must define computeSuggestedWidgetTitle");
        Assertions.assertTrue(modalContent.contains("updateSuggestedWidgetTitle"), "widget-configurator-modal.html must define updateSuggestedWidgetTitle");
        Assertions.assertTrue(modalContent.contains("data-user-custom"), "widgetTitleInput must track userCustom attribute");

        // Verify report-manager.js hooks
        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);
        Assertions.assertTrue(jsContent.contains("updateSuggestedWidgetTitle"), "report-manager.js must reference updateSuggestedWidgetTitle");
    }

    @Test
    public void testTextWithSmallTrendWidget_CompactStylesAndMarkup() throws IOException
    {
        final Path cssPath = Paths.get("src/main/resources/static/dashboard-styles.css");
        Assertions.assertTrue(Files.exists(cssPath), "dashboard-styles.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        // 1. Verify CSS enforces side-by-side on wide cards and full-width column on quarter cards
        Assertions.assertTrue(cssContent.contains(".widget-text-trend-card .widget-text-trend-card-body {\n    min-height: 125px;\n    display: flex;\n    flex-direction: row;"),
            "dashboard-styles.css must specify flex-direction: row for wide widget-text-trend-card body");
        Assertions.assertTrue(cssContent.contains(".widget-card-quarter.widget-text-trend-card .widget-card-body {\n    min-height: 120px;\n    display: flex;\n    flex-direction: column;"),
            "dashboard-styles.css must specify flex-direction: column for quarter-width widget-text-trend-card body");
        Assertions.assertTrue(cssContent.contains(".widget-card-quarter .widget-card-header {\n    padding: 0.65rem 0.85rem;\n    gap: 0.4rem;\n    flex-wrap: nowrap;\n}"),
            "dashboard-styles.css must prevent wrapping on quarter-card-header");
        Assertions.assertTrue(cssContent.contains("text-overflow: ellipsis;"),
            "dashboard-styles.css must specify text-overflow: ellipsis for quarter-card-title text");
        Assertions.assertTrue(cssContent.contains(".token-cost-delta-group.trend-down .token-cost-delta {\n    color: var(--status-unknown-fail, #ef4444) !important;\n}"),
            "dashboard-styles.css must enforce red color for trend-down delta");

        final Path reportCssPath = Paths.get("src/main/resources/static/css/report-manager.css");
        Assertions.assertTrue(Files.exists(reportCssPath));
        final String reportCssContent = Files.readString(reportCssPath, StandardCharsets.UTF_8);
        Assertions.assertTrue(reportCssContent.contains(".token-cost-delta-group.trend-down .token-cost-delta {\n    color: var(--status-unknown-fail, #ef4444);"),
            "report-manager.css must specify red color for trend-down delta");

        // 2. Verify dashboard-widgets.html renders the balanced layout and normalized sparkline
        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(htmlPath), "dashboard-widgets.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(htmlContent.contains("th:fragment=\"textSmallTrendBody(w)\""),
            "dashboard-widgets.html must define textSmallTrendBody fragment");
        Assertions.assertTrue(htmlContent.contains("class=\"widget-text-trend-metrics\""),
            "textSmallTrendBody must contain widget-text-trend-metrics container");
        Assertions.assertTrue(htmlContent.contains("viewBox=\"0 0 300 70\""),
            "textSmallTrendBody must use normalized 300x70 viewBox");
        Assertions.assertTrue(htmlContent.contains("vector-effect=\"non-scaling-stroke\""),
            "textSmallTrendBody must use non-scaling stroke on sparkline path");
        Assertions.assertTrue(htmlContent.contains("th:attr=\"d=${w.sparklineAreaSvgPath}\""),
            "textSmallTrendBody must render sparklineAreaSvgPath path");
        Assertions.assertTrue(htmlContent.contains("th:if=\"${w.sparklineHasEndpoint}\""),
            "textSmallTrendBody must conditionally render endpoint circle");
        Assertions.assertTrue(htmlContent.contains("th:attr=\"cx=${w.sparklineLastX},cy=${w.sparklineLastY}\""),
            "textSmallTrendBody must bind sparklineLastX and sparklineLastY on endpoint circle");
    }

    @Test
    public void testConvertToGroupDashboardUiArtifactsAndTemplates() throws IOException
    {
        final Path customPagePath = Paths.get("src/main/resources/templates/fragments/custom-dashboard-page.html");
        Assertions.assertTrue(Files.exists(customPagePath));
        final String customPageContent = Files.readString(customPagePath, StandardCharsets.UTF_8);
        Assertions.assertTrue(customPageContent.contains("id=\"btnConvertToGroupDashboard\""),
            "custom-dashboard-page.html must include btnConvertToGroupDashboard");
        Assertions.assertTrue(customPageContent.contains("th:if=\"${customPage != null and !customPage.isGroupParent}\""),
            "btnConvertToGroupDashboard must be conditionally rendered when !customPage.isGroupParent");

        final Path modalsPath = Paths.get("src/main/resources/templates/fragments/modals.html");
        Assertions.assertTrue(Files.exists(modalsPath));
        final String modalsContent = Files.readString(modalsPath, StandardCharsets.UTF_8);
        Assertions.assertTrue(modalsContent.contains("id=\"convertToGroupModal\""),
            "modals.html must include convertToGroupModal");
        Assertions.assertTrue(modalsContent.contains("action=\"/dashboard-pages/convert-to-group\""),
            "convertToGroupModal form must post to /dashboard-pages/convert-to-group");

        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath));
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);
        Assertions.assertTrue(jsContent.contains("openConvertToGroupModal"),
            "report-manager.js must define openConvertToGroupModal");
        Assertions.assertTrue(jsContent.contains("submitConvertToGroup"),
            "report-manager.js must define submitConvertToGroup");

        final Path sidebarPath = Paths.get("src/main/resources/templates/fragments/sidebar.html");
        Assertions.assertTrue(Files.exists(sidebarPath));
        final String sidebarContent = Files.readString(sidebarPath, StandardCharsets.UTF_8);
        Assertions.assertTrue(sidebarContent.contains("th:if=\"${page.isGroupParent}\""),
            "sidebar.html must support intermediate sub main groups recursively");
        Assertions.assertTrue(sidebarContent.contains("sidebar-scrollable-content"),
            "sidebar.html must wrap navigation in sidebar-scrollable-content container");
        Assertions.assertTrue(sidebarContent.contains("btn-sidebar-group-toggle"),
            "sidebar.html must render collapsible toggle chevron button for groups");
        Assertions.assertTrue(sidebarContent.contains("sidebar-page-group-container collapsed"),
            "sidebar.html must collapse groupings by default");

        Assertions.assertTrue(modalsContent.contains("id=\"createPageGroupByInput\" value=\"\""),
            "modals.html createPageGroupByInput must not preselect any dimension");
        Assertions.assertTrue(modalsContent.contains("id=\"convertToGroupGroupByInput\" value=\"\""),
            "modals.html convertToGroupGroupByInput must not preselect any dimension");

        Assertions.assertTrue(customPageContent.contains("data-prohibited-group-by"),
            "custom-dashboard-page.html must include data-prohibited-group-by attribute");

        final Path cssPath = Paths.get("src/main/resources/static/css/report-manager.css");
        Assertions.assertTrue(Files.exists(cssPath));
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);
        Assertions.assertTrue(cssContent.contains(".pill-btn.pill-disabled"),
            "report-manager.css must define .pill-btn.pill-disabled");
        Assertions.assertTrue(cssContent.contains(".sidebar-scrollable-content"),
            "report-manager.css must define .sidebar-scrollable-content");
        Assertions.assertTrue(cssContent.contains(".btn-sidebar-group-toggle"),
            "report-manager.css must define .btn-sidebar-group-toggle");
        Assertions.assertTrue(cssContent.contains(".sidebar-page-group-container.collapsed > .sidebar-sub-pages-list"),
            "report-manager.css must hide sub-pages of collapsed groups");

        Assertions.assertTrue(jsContent.contains("toggleSidebarGroup"),
            "report-manager.js must define toggleSidebarGroup");
        Assertions.assertTrue(jsContent.contains("initSidebarGroupExpansion"),
            "report-manager.js must define initSidebarGroupExpansion");
    }

    @Test
    public void testPerlenketteTimelineRailStylesAndMarkup() throws Exception
    {
        final Path cssPath = Paths.get("src/main/resources/static/dashboard-styles.css");
        Assertions.assertTrue(Files.exists(cssPath));
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(cssContent.contains(".perlenkette-pearl-anchor"),
            "dashboard-styles.css must style .perlenkette-pearl-anchor");
        Assertions.assertTrue(cssContent.contains("text-decoration: none !important"),
            "dashboard-styles.css must prevent anchor text underline on pearls");
        Assertions.assertTrue(cssContent.contains(".perlenkette-timeline-rail"),
            "dashboard-styles.css must style .perlenkette-timeline-rail");
        Assertions.assertTrue(cssContent.contains(".perlenkette-top-pill-bg"),
            "dashboard-styles.css must style .perlenkette-top-pill-bg");
        Assertions.assertTrue(cssContent.contains(".perlenkette-top-pill-text"),
            "dashboard-styles.css must style .perlenkette-top-pill-text");
        Assertions.assertTrue(cssContent.contains(".perlenkette-bottom-label"),
            "dashboard-styles.css must style .perlenkette-bottom-label");
        Assertions.assertTrue(cssContent.contains(".perlenkette-latest-pill-bg"),
            "dashboard-styles.css must style .perlenkette-latest-pill-bg");

        final Path templatePath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(templatePath));
        final String templateContent = Files.readString(templatePath, StandardCharsets.UTF_8);

        Assertions.assertTrue(templateContent.contains("class=\"perlenkette-top-pill-group\""),
            "dashboard-widgets.html must render top milestone pill group");
        Assertions.assertTrue(templateContent.contains("class=\"perlenkette-top-pill-bg\""),
            "dashboard-widgets.html must render top pill background");
        Assertions.assertTrue(templateContent.contains("class=\"perlenkette-top-pill-text\""),
            "dashboard-widgets.html must render top pill milestone text");
        Assertions.assertTrue(templateContent.contains("class=\"perlenkette-bottom-label\""),
            "dashboard-widgets.html must render bottom timestamp label");
        Assertions.assertTrue(templateContent.contains("perlenkette-timeline-rail"),
            "dashboard-widgets.html must render perlenkette-timeline-rail class");
    }

    @Test
    public void testManagerWorkspaceFilterPersistenceContracts() throws Exception
    {
        final Path testSelectionPath = Paths.get("src/main/resources/templates/fragments/test-selection.html");
        Assertions.assertTrue(Files.exists(testSelectionPath), "test-selection.html must exist");
        final String testSelectionContent = Files.readString(testSelectionPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            testSelectionContent.contains("id=\"clearTestSearchBtn\""),
            "test-selection.html must render clearTestSearchBtn"
        );
        Assertions.assertTrue(
            testSelectionContent.contains("oninput=\"if(typeof onTestSearchInput==='function') onTestSearchInput(this.value);\""),
            "test-selection.html must handle oninput for search persistence"
        );
        Assertions.assertTrue(
            testSelectionContent.contains("id=\"refreshFilesBtn\"") && testSelectionContent.contains("hx-include=\"#testSearchInput\""),
            "refreshFilesBtn must include #testSearchInput to avoid wiping search query on refresh"
        );
        Assertions.assertTrue(
            testSelectionContent.contains("id=\"yamlFileList\"") && testSelectionContent.contains("hx-include=\"#testSearchInput\""),
            "yamlFileList must include #testSearchInput on background file refreshes"
        );

        final Path editorJsPath = Paths.get("src/main/resources/static/dashboard-editor.js");
        Assertions.assertTrue(Files.exists(editorJsPath), "dashboard-editor.js must exist");
        final String editorJsContent = Files.readString(editorJsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            editorJsContent.contains("window.onTestSearchInput = onTestSearchInput;"),
            "dashboard-editor.js must export onTestSearchInput"
        );
        Assertions.assertTrue(
            editorJsContent.contains("window.clearTestSearch = clearTestSearch;"),
            "dashboard-editor.js must export clearTestSearch"
        );
        Assertions.assertTrue(
            editorJsContent.contains("window.updateClearTestSearchBtn = updateClearTestSearchBtn;"),
            "dashboard-editor.js must export updateClearTestSearchBtn"
        );
        Assertions.assertTrue(
            editorJsContent.contains("aura_test_search_query"),
            "dashboard-editor.js must use aura_test_search_query for session persistence"
        );

        Assertions.assertTrue(
            editorJsContent.contains("window.applyInstantTestFilter = applyInstantTestFilter;"),
            "dashboard-editor.js must export applyInstantTestFilter"
        );
        Assertions.assertTrue(
            editorJsContent.contains("window.setFilterCookie = setFilterCookie;"),
            "dashboard-editor.js must export setFilterCookie"
        );
        Assertions.assertTrue(
            editorJsContent.contains("window.getFilterCookie = getFilterCookie;"),
            "dashboard-editor.js must export getFilterCookie"
        );
        Assertions.assertTrue(
            editorJsContent.contains("inMatchingFolder"),
            "dashboard-editor.js must propagate folder match status to child tests and datasets"
        );
        Assertions.assertTrue(
            editorJsContent.contains("window.toggleFolderCheckboxesInstant = toggleFolderCheckboxesInstant;"),
            "dashboard-editor.js must export toggleFolderCheckboxesInstant"
        );
        Assertions.assertTrue(
            editorJsContent.contains("window.updateParentFolderCheckboxes = updateParentFolderCheckboxes;"),
            "dashboard-editor.js must export updateParentFolderCheckboxes"
        );
        Assertions.assertTrue(
            editorJsContent.contains("window.updateAllFolderCheckboxes = updateAllFolderCheckboxes;"),
            "dashboard-editor.js must export updateAllFolderCheckboxes"
        );

        final Path coreJsPath = Paths.get("src/main/resources/static/dashboard-core.js");
        Assertions.assertTrue(Files.exists(coreJsPath), "dashboard-core.js must exist");
        final String coreJsContent = Files.readString(coreJsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            coreJsContent.contains("serverQuery"),
            "dashboard-core.js must prioritize serverQuery over stale sessionStorage"
        );
        Assertions.assertTrue(
            coreJsContent.contains("initTestSearchFilter();"),
            "dashboard-core.js must initialize search filter on load"
        );
        Assertions.assertTrue(
            coreJsContent.contains("addEventListener('pageshow'"),
            "dashboard-core.js must re-verify search filter on pageshow (BFcache support)"
        );
        Assertions.assertTrue(
            coreJsContent.contains("applyInstantTestFilter(query);"),
            "dashboard-core.js must apply instant test filter on init"
        );
        Assertions.assertTrue(
            coreJsContent.contains("folder-select-cb"),
            "dashboard-core.js must construct folder-select-cb for hierarchical tree folders"
        );
        Assertions.assertTrue(
            coreJsContent.contains("updateAllFolderCheckboxes(container);"),
            "dashboard-core.js must initialize folder checkboxes when building tree view"
        );
    }

    @Test
    public void testAuraManagerLoggingConfigurationContracts() throws Exception
    {
        final Path appPropertiesPath = Paths.get("src/main/resources/application.properties");
        Assertions.assertTrue(Files.exists(appPropertiesPath), "application.properties must exist");
        final String appPropsContent = Files.readString(appPropertiesPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            appPropsContent.contains("logging.level.root=INFO"),
            "application.properties must set logging.level.root=INFO to prevent verbose framework tracing"
        );
        Assertions.assertTrue(
            appPropsContent.contains("logging.level.com.xceptance.aura=INFO"),
            "application.properties must set logging.level.com.xceptance.aura=INFO"
        );

        final Path log4jPath = Paths.get("../neodymium-core/src/main/resources/log4j2.properties");
        if (Files.exists(log4jPath))
        {
            final String log4jContent = Files.readString(log4jPath, StandardCharsets.UTF_8);
            Assertions.assertFalse(
                log4jContent.contains("appender.console.filter.threshold.level = trace"),
                "log4j2.properties console filter threshold must not be set to trace"
            );
            Assertions.assertFalse(
                log4jContent.contains("status = ALL"),
                "log4j2.properties status logger must not be set to ALL"
            );
        }
    }

    @Test
    public void testStreamlinedLlmDetailsRenderingAndPromptParsing() throws Exception
    {
        final Path cssPath = Paths.get("src/main/resources/static/css/report-manager.css");
        Assertions.assertTrue(Files.exists(cssPath), "report-manager.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            cssContent.contains(".llm-timeline-bar") && cssContent.contains(".llm-flow-chip"),
            "report-manager.css must define .llm-timeline-bar and .llm-flow-chip for sequence overview"
        );
        Assertions.assertTrue(
            cssContent.contains(".llm-turn-card") && cssContent.contains(".llm-header-details-btn"),
            "report-manager.css must define .llm-turn-card and .llm-header-details-btn"
        );
        Assertions.assertTrue(
            cssContent.contains(".llm-exchange-row") && cssContent.contains(".llm-details-drawer"),
            "report-manager.css must define .llm-exchange-row and .llm-details-drawer"
        );

        if (!isNodeAvailable())
        {
            return;
        }

        final Path jsFilePath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsFilePath), "report-manager.js must exist: " + jsFilePath.toAbsolutePath());

        final String nodeTestScript =
            "const fs = require('fs');\n" +
            "const vm = require('vm');\n" +
            "const code = fs.readFileSync('" + jsFilePath.toAbsolutePath().toString().replace("\\", "/") + "', 'utf8');\n" +
            "const testScript = code + '\\n' + `\n" +
            "const promptSample = '### Active Session Variables:\\\\n- editorUrl: \\\"https://example.com/mug\\\"\\\\n- name: \\\"Fototasse\\\"\\\\n\\\\n### Test Instruction:\\\\nVerify photo is displayed in placeholder.\\\\n\\\\n### Current Page State & Interactive Elements:\\\\nPage URL: https://example.com\\\\n<div>Placeholder</div>';\n" +
            "const parsed = parseLlmPromptContent(promptSample);\n" +
            "if (!parsed.instruction || !parsed.instruction.includes('Verify photo is displayed')) throw new Error('Instruction parse failed: ' + parsed.instruction);\n" +
            "if (parsed.sessionVariables.length !== 2) throw new Error('Session variables parse failed: ' + parsed.sessionVariables.length);\n" +
            "if (parsed.sessionVariables[0].key !== 'editorUrl' || parsed.sessionVariables[1].key !== 'name') throw new Error('Variable keys mismatch');\n" +
            "if (!parsed.domSnapshot || !parsed.domSnapshot.includes('Placeholder')) throw new Error('DOM snapshot parse failed');\n" +
            "\n" +
            "const respSample = 'Tool Calls: [{\\\"name\\\":\\\"query_dom\\\",\\\"arguments\\\":{\\\"selector\\\":\\\"app-image-placeholder\\\"}}]';\n" +
            "const parsedResp = parseToolCallResponse(respSample);\n" +
            "if (parsedResp.primaryTool !== 'query_dom') throw new Error('Tool call parse failed: ' + parsedResp.primaryTool);\n" +
            "if (!parsedResp.formattedToolCall.includes('app-image-placeholder')) throw new Error('Tool call args mismatch');\n" +
            "\n" +
            "const calls = [\n" +
            "    { model: 'gemini-3.5-flash-lite', inputTokens: 500, outputTokens: 50, durationMs: 1200, userPrompt: promptSample, responseContent: respSample },\n" +
            "    { model: 'gemini-3.5-flash-lite', inputTokens: 600, outputTokens: 60, durationMs: 900, userPrompt: 'Tool Result: 0 elements', responseContent: 'Tool Calls: [{\\\"name\\\":\\\"wait\\\",\\\"arguments\\\":{\\\"milliseconds\\\":2000}}]' }\n" +
            "];\n" +
            "const timelineHtml = renderExecutionTimelineBar(calls, 'step_1', true);\n" +
            "if (!timelineHtml.includes('Execution Sequence Overview') || !timelineHtml.includes('query_dom') || !timelineHtml.includes('wait')) throw new Error('Timeline generation failed');\n" +
            "if (!timelineHtml.includes('failed') || !timelineHtml.includes('❌')) throw new Error('Failed step indicator missing from last turn in timeline');\n" +
            "\n" +
            "const cardHtml = renderStreamlinedLlmCallCard(calls[0], 0, 'step_1', false, false);\n" +
            "if (!cardHtml.includes('llm-turn-card') || !cardHtml.includes('llm-header-details-btn')) throw new Error('Turn card structure failed');\n" +
            "if (!cardHtml.includes('Session Variables (2)') || !cardHtml.includes('editorUrl')) throw new Error('Variables table missing from drawer');\n" +
            "if (!cardHtml.includes('query_dom')) throw new Error('Response tool call missing from card');\n" +
            "`;\n" +
            "const sandbox = {\n" +
            "    console,\n" +
            "    window: {},\n" +
            "    document: {\n" +
            "        getElementById: () => null,\n" +
            "        querySelectorAll: () => [],\n" +
            "        querySelector: () => null,\n" +
            "        addEventListener: () => {},\n" +
            "        body: { addEventListener: () => {} }\n" +
            "    }\n" +
            "};\n" +
            "sandbox.window = sandbox;\n" +
            "vm.runInNewContext(testScript, sandbox);\n";

        runNodeScript(nodeTestScript, "Node script execution failed");
    }

    @Test
    public void testMultiTurnToolOutputParsingAndClipboardExport() throws Exception
    {
        final Path cssPath = Paths.get("src/main/resources/static/css/report-manager.css");
        Assertions.assertTrue(Files.exists(cssPath), "report-manager.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            cssContent.contains(".llm-copy-btn") && cssContent.contains(".llm-drawer-section"),
            "report-manager.css must define .llm-copy-btn and .llm-drawer-section"
        );
        Assertions.assertTrue(
            cssContent.contains(".llm-code-block") && cssContent.contains(".llm-context-box"),
            "report-manager.css must define .llm-code-block and .llm-context-box"
        );

        if (!isNodeAvailable())
        {
            return;
        }

        final Path jsFilePath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsFilePath), "report-manager.js must exist: " + jsFilePath.toAbsolutePath());

        final String nodeTestScript =
            "const fs = require('fs');\n" +
            "const vm = require('vm');\n" +
            "const code = fs.readFileSync('" + jsFilePath.toAbsolutePath().toString().replace("\\", "/") + "', 'utf8');\n" +
            "const testScript = code + '\\n' + `\n" +
            "const multiTurnSample = 'TOOL (tool=query_dom, id=query_dom_40a28575): {\"status\":\"SUCCESS\",\"action\":\"query_dom\",\"matches\":[{\"tag\":\"app-image-placeholder\",\"selector\":\"app-image-placeholder.hoveredPlaceholder\",\"text\":\"\",\"visible\":false,\"inViewport\":false,\"dataAi\":null,\"y\":297,\"outerHtml\":\"<app-image-placeholder class=hoveredPlaceholder>\"}],\"matchCount\":1} ChatMessage[USER]: ### Current Page: URL: https://order.example.com/editor/ Title: Editor Note: Page visual state is unchanged. If this was an action instruction and all actions have been executed, complete_step now.';\n" +
            "const parsed = parseLlmPromptContent(multiTurnSample);\n" +
            "if (!parsed.turnInputSummary || !parsed.turnInputSummary.includes('query_dom ➔ SUCCESS (1 match: app-image-placeholder.hoveredPlaceholder)')) {\n" +
            "    throw new Error('turnInputSummary failed: ' + parsed.turnInputSummary);\n" +
            "}\n" +
            "if (!parsed.toolResult || parsed.toolResult.toolName !== 'query_dom' || !parsed.toolResult.formattedJson.includes('\"action\": \"query_dom\"')) {\n" +
            "    throw new Error('toolResult structure failed');\n" +
            "}\n" +
            "if (!parsed.continuationContext || !parsed.continuationContext.pageUrl.includes('order.example.com') || parsed.continuationContext.pageTitle !== 'Editor') {\n" +
            "    throw new Error('continuationContext failed');\n" +
            "}\n" +
            "if (!parsed.continuationContext.guidanceText.includes('complete_step')) {\n" +
            "    throw new Error('guidanceText extraction failed');\n" +
            "}\n" +
            "\n" +
            "const call = {\n" +
            "    userPrompt: multiTurnSample,\n" +
            "    responseContent: 'Tool Calls: [{\\\"name\\\":\\\"complete_step\\\",\\\"arguments\\\":{}}]'\n" +
            "};\n" +
            "const cardHtml = renderStreamlinedLlmCallCard(call, 1, 'step_99', false, false);\n" +
            "if (!cardHtml.includes('Tool Result: query_dom ➔ SUCCESS (1 match: app-image-placeholder.hoveredPlaceholder)')) {\n" +
            "    throw new Error('Oneliner summary missing from card: ' + cardHtml);\n" +
            "}\n" +
            "if (!cardHtml.includes('Tool Result Payload') || !cardHtml.includes('Page State & Continuation Context')) {\n" +
            "    throw new Error('Drawer sections missing from card: ' + cardHtml);\n" +
            "}\n" +
            "if (cardHtml.includes('copyTurnConversation')) {\n" +
            "    throw new Error('Single-turn copy button should not be present on card header');\n" +
            "}\n" +
            "\n" +
            "const initialCall = {\n" +
            "    userPrompt: '### Test Instruction: Place image.',\n" +
            "    responseContent: 'Tool Calls: [{\\\"name\\\":\\\"query_dom\\\",\\\"arguments\\\":{\\\"selector\\\":\\\"app-image-placeholder\\\"}}]'\n" +
            "};\n" +
            "window._neodymiumStepLlmData = {\n" +
            "    step_99: {\n" +
            "        stepId: 'step_99',\n" +
            "        stepNum: 3,\n" +
            "        title: 'Image Placement',\n" +
            "        model: 'gemini-3.6-flash',\n" +
            "        totalTokens: 12000,\n" +
            "        totalCost: '$0.0042',\n" +
            "        calls: [initialCall, call],\n" +
            "        systemPrompt: 'You are an automated web agent.'\n" +
            "    }\n" +
            "};\n" +
            "const md = generateStepConversationMarkdown('step_99');\n" +
            "if (!md.includes('# LLM Conversation: Step #3 - Image Placement') || !md.includes('You are an automated web agent.') || !md.includes('Tool Execution Payload')) {\n" +
            "    throw new Error('Markdown generation failed: ' + md.substring(0, 100));\n" +
            "}\n" +
            "`;\n" +
            "const sandbox = {\n" +
            "    console,\n" +
            "    window: {},\n" +
            "    document: {\n" +
            "        getElementById: () => null,\n" +
            "        querySelectorAll: () => [],\n" +
            "        querySelector: () => null,\n" +
            "        addEventListener: () => {},\n" +
            "        body: { addEventListener: () => {} }\n" +
            "    }\n" +
            "};\n" +
            "sandbox.window = sandbox;\n" +
            "vm.runInNewContext(testScript, sandbox);\n";

        runNodeScript(nodeTestScript, "Node script execution failed");
    }

    @Test
    public void testAccumulatedChatMessageHistoryParsingAndDomCollapsing() throws Exception
    {
        final Path jsFilePath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsFilePath), "report-manager.js must exist");

        final String nodeTestScript =
            "const fs = require('fs');\n" +
            "const vm = require('vm');\n" +
            "const code = fs.readFileSync('" + jsFilePath.toAbsolutePath().toString().replace("\\", "/") + "', 'utf8');\n" +
            "const testScript = code + '\\n' + `\n" +
            "const fullHistorySample = 'ChatMessage[USER]: ### Active Session Variables:\\\\n- BROWSER: chrome\\\\n### Test Instruction:\\\\nGenerate a random image and upload it.\\\\n### Current Page State & Interactive Elements:\\\\nPage URL: https://order.example.com\\\\nChatMessage[ASSISTANT]: Tool Calls: [{\\\\\"name\\\\\":\\\\\"generateAndUploadImage\\\\\",\\\\\"arguments\\\\\":{\\\\\"target\\\\\":\\\\\"gallery\\\\\"}}]\\\\nChatMessage[TOOL (tool=generateAndUploadImage, id=call_4)]: Tool error: Error executing method generateAndUploadImage: Connection refused\\\\nChatMessage[USER]: ### Current Page:\\\\nURL: https://order.example.com/editor/\\\\nTitle: Editor\\\\nNote: Page visual state unchanged.\\\\n=== Structural DOM Tree ===\\\\n<app-root><div class=\"editor\">Canvas</div></app-root>';\n" +
            "const messages = splitChatMessages(fullHistorySample);\n" +
            "if (messages.length !== 4) {\n" +
            "    throw new Error('Expected 4 discrete messages but got: ' + messages.length);\n" +
            "}\n" +
            "if (!messages[2].role.includes('TOOL') || !messages[2].content.includes('Connection refused')) {\n" +
            "    throw new Error('Tool message extraction mismatch: ' + JSON.stringify(messages[2]));\n" +
            "}\n" +
            "const parsed = parseLlmPromptContent(fullHistorySample);\n" +
            "if (!parsed.turnInputSummary.includes('generateAndUploadImage ➔ ERROR: Error executing method generateAndUploadImage: Connection refused')) {\n" +
            "    throw new Error('Plain text tool error summary mismatch: ' + parsed.turnInputSummary);\n" +
            "}\n" +
            "if (!parsed.domSnapshot || !parsed.domSnapshot.includes('<app-root><div class=\"editor\">Canvas</div></app-root>')) {\n" +
            "    throw new Error('Structural DOM tree not properly isolated into domSnapshot: ' + parsed.domSnapshot);\n" +
            "}\n" +
            "if (parsed.continuationContext.rawText.includes('<app-root>')) {\n" +
            "    throw new Error('Structural DOM tree leaked into continuationContext.rawText');\n" +
            "}\n" +
            "if (parsed.continuationContext.pageUrl !== 'https://order.example.com/editor/' || parsed.continuationContext.pageTitle !== 'Editor') {\n" +
            "    throw new Error('Continuation context URL or Title mismatch');\n" +
            "}\n" +
            "\n" +
            "const call = {\n" +
            "    userPrompt: fullHistorySample,\n" +
            "    responseContent: 'Tool Calls: [{\\\"name\\\":\\\"query_dom\\\",\\\"arguments\\\":{\\\"selector\\\":\\\"app-root\\\"}}]'\n" +
            "};\n" +
            "const cardHtml = renderStreamlinedLlmCallCard(call, 1, 'step_101', false, false);\n" +
            "if (!cardHtml.includes('Tool Result: generateAndUploadImage ➔ ERROR:')) {\n" +
            "    throw new Error('Card missing concise oneliner: ' + cardHtml);\n" +
            "}\n" +
            "if (!cardHtml.includes('Page State & DOM Snapshot') || !cardHtml.includes('llm-dom-toggle')) {\n" +
            "    throw new Error('Card missing collapsible DOM snapshot toggle: ' + cardHtml);\n" +
            "}\n" +
            "if (cardHtml.includes('copyTurnConversation')) {\n" +
            "    throw new Error('Card must not have per-turn copy button');\n" +
            "}\n" +
            "\n" +
            "window._neodymiumStepLlmData = {\n" +
            "    step_101: {\n" +
            "        stepId: 'step_101',\n" +
            "        stepNum: 4,\n" +
            "        title: 'Generate and upload image',\n" +
            "        model: 'gemini-3.5-flash-lite',\n" +
            "        totalTokens: 116730,\n" +
            "        totalCost: '$0.0406',\n" +
            "        calls: [\n" +
            "            {\n" +
            "                userPrompt: '### Test Instruction: Generate image and upload.',\n" +
            "                responseContent: 'Tool Calls: [{\\\"name\\\":\\\"generateAndUploadImage\\\",\\\"arguments\\\":{}}]'\n" +
            "            },\n" +
            "            call\n" +
            "        ],\n" +
            "        systemPrompt: 'Autonomous web agent.'\n" +
            "    }\n" +
            "};\n" +
            "const md = generateStepConversationMarkdown('step_101');\n" +
            "if (!md.includes('<details>') || !md.includes('<summary>Page DOM Tree Snapshot') || !md.includes('</details>')) {\n" +
            "    throw new Error('Markdown must collapse DOM tree inside <details> tags: ' + md.substring(0, 300));\n" +
            "}\n" +
            "`;\n" +
            "const sandbox = {\n" +
            "    console,\n" +
            "    window: {},\n" +
            "    document: {\n" +
            "        getElementById: () => null,\n" +
            "        querySelectorAll: () => [],\n" +
            "        querySelector: () => null,\n" +
            "        addEventListener: () => {},\n" +
            "        body: { addEventListener: () => {} }\n" +
            "    }\n" +
            "};\n" +
            "sandbox.window = sandbox;\n" +
            "vm.runInNewContext(testScript, sandbox);\n";

        runNodeScript(nodeTestScript, "Node script execution failed");
    }

    @Test
    public void testSubStepLlmInvocationRenderingDoesNotReferenceUndeclaredSidx() throws Exception
    {
        final Path jsFilePath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsFilePath), "report-manager.js must exist");

        final String content = Files.readString(jsFilePath, StandardCharsets.UTF_8);
        Assertions.assertFalse(content.contains("sIdx"), "report-manager.js must not reference undeclared sIdx variable");

        if (!isNodeAvailable())
        {
            return;
        }

        final String nodeTestScript =
            "const fs = require('fs');\n" +
            "const vm = require('vm');\n" +
            "const code = fs.readFileSync('" + jsFilePath.toAbsolutePath().toString().replace("\\", "/") + "', 'utf8');\n" +
            "const sampleRow = {\n" +
            "    id: 'row_1',\n" +
            "    querySelector: () => null,\n" +
            "    getAttribute: (name) => {\n" +
            "        if (name === 'data-blocks') {\n" +
            "            return JSON.stringify({\n" +
            "                steps: [{\n" +
            "                    instruction: 'Parent step',\n" +
            "                    subSteps: [{\n" +
            "                        instruction: 'Child sub-step',\n" +
            "                        llmCalls: [{ model: 'gemini-2.5', userPrompt: 'prompt', responseContent: 'response' }]\n" +
            "                    }]\n" +
            "                }]\n" +
            "            });\n" +
            "        }\n" +
            "        if (name === 'data-status') return 'FAILED';\n" +
            "        return '';\n" +
            "    },\n" +
            "    hasAttribute: (name) => name === 'data-steps-fetched',\n" +
            "    setAttribute: () => {},\n" +
            "    removeAttribute: () => {}\n" +
            "};\n" +
            "const sideList = { innerHTML: '', querySelector: () => null };\n" +
            "const sandbox = {\n" +
            "    console,\n" +
            "    window: {},\n" +
            "    document: {\n" +
            "        readyState: 'complete',\n" +
            "        getElementById: (id) => (id === 'sidePageStepList' ? sideList : null),\n" +
            "        querySelectorAll: () => [],\n" +
            "        querySelector: () => null,\n" +
            "        addEventListener: () => {},\n" +
            "        body: { addEventListener: () => {}, contains: () => true }\n" +
            "    }\n" +
            "};\n" +
            "sandbox.window = sandbox;\n" +
            "vm.runInNewContext(code, sandbox);\n" +
            "sandbox.renderStepsForExecution(sampleRow);\n" +
            "if (!sideList.innerHTML.includes('Child sub-step') || !sideList.innerHTML.includes('LLM Invocations')) {\n" +
            "    throw new Error('Expected rendered step HTML with sub-step LLM invocations but got: ' + sideList.innerHTML);\n" +
            "}\n";

        runNodeScript(nodeTestScript, "Node script execution failed");
    }

    @Test
    public void testIsTerminalStatusRecognizesNormalizedStatusesAndHandlesAbort() throws Exception
    {
        final Path jsFilePath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsFilePath), "report-manager.js must exist");

        if (!isNodeAvailable())
        {
            return;
        }

        final String nodeTestScript =
            "const fs = require('fs');\n" +
            "const vm = require('vm');\n" +
            "const code = fs.readFileSync('" + jsFilePath.toAbsolutePath().toString().replace("\\", "/") + "', 'utf8');\n" +
            "const sandbox = {\n" +
            "    console,\n" +
            "    window: {},\n" +
            "    document: {\n" +
            "        readyState: 'complete',\n" +
            "        getElementById: () => null,\n" +
            "        querySelectorAll: () => [],\n" +
            "        querySelector: () => null,\n" +
            "        addEventListener: () => {},\n" +
            "        body: { addEventListener: () => {}, contains: () => true }\n" +
            "    }\n" +
            "};\n" +
            "sandbox.window = sandbox;\n" +
            "vm.runInNewContext(code, sandbox);\n" +
            "const terminalChecks = ['PASSED', 'passed-clean', 'failed-assertion', 'succeeded-fixed', 'SUCCESS', 'successful', 'cancelled', 'canceled', 'stopped', 'aborted', 'skipped', 'ignored', 'completed'];\n" +
            "for (const st of terminalChecks) {\n" +
            "    if (!sandbox.isTerminalStatus(st)) {\n" +
            "        throw new Error('Status should be recognized as terminal: ' + st);\n" +
            "    }\n" +
            "}\n" +
            "const nonTerminalChecks = ['running', 'in_progress', 'pending', 'queued', 'paused', 'waiting', '', null, undefined];\n" +
            "for (const st of nonTerminalChecks) {\n" +
            "    if (sandbox.isTerminalStatus(st)) {\n" +
            "        throw new Error('Status should NOT be recognized as terminal: ' + st);\n" +
            "    }\n" +
            "}\n" +
            "sandbox.stopLiveStepRefreshTimer();\n";

        runNodeScript(nodeTestScript, "Node script execution failed");
    }

    @Test
    public void testOpenTestSidePagePanelInvokesStopLiveStepRefreshTimer() throws Exception
    {
        final Path jsFilePath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsFilePath), "report-manager.js must exist");

        final String content = Files.readString(jsFilePath, StandardCharsets.UTF_8);
        final int fnIndex = content.indexOf("function openTestSidePagePanel(");
        Assertions.assertTrue(fnIndex >= 0, "openTestSidePagePanel must exist in report-manager.js");

        final String fnBody = content.substring(fnIndex, Math.min(content.length(), fnIndex + 400));
        Assertions.assertTrue(
            fnBody.contains("stopLiveStepRefreshTimer();"),
            "openTestSidePagePanel must invoke stopLiveStepRefreshTimer() on entry to clean up active timers"
        );
    }

    @Test
    public void testLiveStepFetchInFlightGuardsArePresent() throws Exception
    {
        final Path jsFilePath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsFilePath), "report-manager.js must exist");

        final String content = Files.readString(jsFilePath, StandardCharsets.UTF_8);
        Assertions.assertTrue(
            content.contains("if (isLiveStepFetchInFlight && currentActiveRowId === rowId)"),
            "renderStepsForExecution must guard against duplicate in-flight fetches for the active row"
        );
        Assertions.assertTrue(
            content.contains("if (isLiveStepFetchInFlight)") && content.contains("liveStepRefreshTimer = setTimeout(poll, 1000)"),
            "poll must reschedule when fetch is in-flight"
        );
    }

    @Test
    public void testResponsibilityTableOmitsPesapAndParsesModernBuckets() throws Exception
    {
        final Path jsFilePath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsFilePath), "report-manager.js must exist");

        if (!isNodeAvailable())
        {
            return;
        }

        final String nodeTestScript =
            "const fs = require('fs');\n" +
            "const vm = require('vm');\n" +
            "const code = fs.readFileSync('" + jsFilePath.toAbsolutePath().toString().replace("\\", "/") + "', 'utf8');\n" +
            "const sandbox = {\n" +
            "    console,\n" +
            "    window: {},\n" +
            "    document: {\n" +
            "        readyState: 'complete',\n" +
            "        getElementById: () => null,\n" +
            "        querySelectorAll: () => [],\n" +
            "        querySelector: () => null,\n" +
            "        addEventListener: () => {},\n" +
            "        body: { addEventListener: () => {}, contains: () => true }\n" +
            "    }\n" +
            "};\n" +
            "sandbox.window = sandbox;\n" +
            "vm.runInNewContext(code, sandbox);\n" +
            "const respJson = JSON.stringify({\n" +
            "    action: { calls: 10, inputTokens: 50000, outputTokens: 500, cachedTokens: 1000, totalTokens: 50500, estimatedCostUsd: 0.015 },\n" +
            "    verification: { calls: 5, inputTokens: 20000, outputTokens: 200, cachedTokens: 500, totalTokens: 20200, estimatedCostUsd: 0.006 },\n" +
            "    judge: { calls: 2, inputTokens: 5000, outputTokens: 50, cachedTokens: 0, totalTokens: 5050, estimatedCostUsd: 0.0015 },\n" +
            "    visualRca: { calls: 0, inputTokens: 0, outputTokens: 0, cachedTokens: 0, totalTokens: 0, estimatedCostUsd: 0 }\n" +
            "});\n" +
            "const buckets = sandbox.parseResponsibilityBuckets(respJson);\n" +
            "if (!buckets) throw new Error('parseResponsibilityBuckets should successfully parse modern categories without pesap');\n" +
            "if (!buckets.total || buckets.total.calls !== 17) throw new Error('Calculated total calls should be 17, got: ' + JSON.stringify(buckets.total));\n" +
            "if (buckets.pesap) throw new Error('buckets should not contain pesap');\n" +
            "const tbody = { innerHTML: '' };\n" +
            "sandbox.renderLlmResponsibilityRows(tbody, respJson, null);\n" +
            "if (tbody.innerHTML.includes('PESAP')) throw new Error('renderLlmResponsibilityRows should not contain PESAP');\n" +
            "if (!tbody.innerHTML.includes('Action (Standard Generation)')) throw new Error('renderLlmResponsibilityRows should contain Action');\n" +
            "const heuristicHtml = sandbox.renderHeuristicLlmRows({ totalCalls: 10, totalTokens: 5000, inputTokens: 4000, outputTokens: 1000, cachedTokens: 0, cost: 0.01 });\n" +
            "if (heuristicHtml.includes('PESAP')) throw new Error('renderHeuristicLlmRows should not contain PESAP');\n" +
            "if (!heuristicHtml.includes('Action (Standard Generation)')) throw new Error('renderHeuristicLlmRows should contain Action');\n" +
            "sandbox.stopLiveStepRefreshTimer();\n";

        runNodeScript(nodeTestScript, "Responsibility table evaluation script failed");
    }

    @Test
    public void testSyncRowAiUsageRecursivelyTraversesSubStepsAndPreFlightCalls() throws Exception
    {
        final Path jsFilePath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsFilePath), "report-manager.js must exist");

        if (!isNodeAvailable())
        {
            return;
        }

        final String nodeTestScript =
            "const fs = require('fs');\n" +
            "const vm = require('vm');\n" +
            "const code = fs.readFileSync('" + jsFilePath.toAbsolutePath().toString().replace("\\", "/") + "', 'utf8');\n" +
            "const sandbox = {\n" +
            "    console,\n" +
            "    window: {},\n" +
            "    document: {\n" +
            "        readyState: 'complete',\n" +
            "        getElementById: () => null,\n" +
            "        querySelectorAll: () => [],\n" +
            "        querySelector: () => null,\n" +
            "        addEventListener: () => {},\n" +
            "        body: { addEventListener: () => {}, contains: () => true }\n" +
            "    }\n" +
            "};\n" +
            "sandbox.window = sandbox;\n" +
            "vm.runInNewContext(code, sandbox);\n" +
            "const blocksObj = {\n" +
            "    llmCalls: [\n" +
            "        { id: 'pre-1', stepIndex: -1, inputTokens: 3474, outputTokens: 0, cachedTokens: 0, durationMs: 120 }\n" +
            "    ],\n" +
            "    steps: [\n" +
            "        {\n" +
            "            instruction: 'Step 1',\n" +
            "            llmCalls: [\n" +
            "                { id: 'call-1', stepIndex: 0, inputTokens: 10000, outputTokens: 100, cachedTokens: 1000, durationMs: 500 }\n" +
            "            ]\n" +
            "        },\n" +
            "        {\n" +
            "            instruction: 'Step 2 - fragment',\n" +
            "            subSteps: [\n" +
            "                {\n" +
            "                    instruction: 'SubStep 2.1',\n" +
            "                    llmCalls: [\n" +
            "                        { id: 'sub-call-1', stepIndex: 1, subStepIndex: 0, inputTokens: 20000, outputTokens: 200, cachedTokens: 2000, durationMs: 600 }\n" +
            "                    ]\n" +
            "                },\n" +
            "                {\n" +
            "                    instruction: 'SubStep 2.2',\n" +
            "                    llmCalls: [\n" +
            "                        { id: 'sub-call-2', stepIndex: 1, subStepIndex: 1, inputTokens: 30000, outputTokens: 300, cachedTokens: 3000, durationMs: 700 }\n" +
            "                    ]\n" +
            "                }\n" +
            "            ]\n" +
            "        }\n" +
            "    ]\n" +
            "};\n" +
            "const attributes = {\n" +
            "    'data-blocks': JSON.stringify(blocksObj),\n" +
            "    'data-steps': '{}'\n" +
            "};\n" +
            "const mockRow = {\n" +
            "    getAttribute: (k) => attributes[k] || null,\n" +
            "    setAttribute: (k, v) => { attributes[k] = String(v); },\n" +
            "    querySelector: () => ({ innerHTML: '' })\n" +
            "};\n" +
            "const result = sandbox.syncRowAiUsage(mockRow);\n" +
            "if (!result) throw new Error('syncRowAiUsage returned null');\n" +
            "if (result.llmCalls !== 4) throw new Error('Expected 4 llmCalls, got: ' + result.llmCalls);\n" +
            "if (result.inputTokens !== 63474) throw new Error('Expected 63474 inputTokens, got: ' + result.inputTokens);\n" +
            "if (result.outputTokens !== 600) throw new Error('Expected 600 outputTokens, got: ' + result.outputTokens);\n" +
            "if (result.cachedTokens !== 6000) throw new Error('Expected 6000 cachedTokens, got: ' + result.cachedTokens);\n" +
            "sandbox.stopLiveStepRefreshTimer();\n";

        runNodeScript(nodeTestScript, "syncRowAiUsage recursion evaluation script failed");
    }

    @Test
    public void testUnitedContainerAiUsageKpiCrossFilteringSupport() throws Exception
    {
        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(htmlPath), "dashboard-widgets.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        // Verify dashboard-widgets.html binds data-filter-dim='mode' and eliminates legacy 'type'
        Assertions.assertFalse(htmlContent.contains("data-filter-dim='type'"),
            "dashboard-widgets.html must not use legacy data-filter-dim='type'");
        Assertions.assertTrue(htmlContent.contains("th:attr=\"data-filter-dim='mode',data-filter-val=${seg.name}\""),
            "dashboard-widgets.html must bind data-filter-dim='mode' for AI_USAGE segments in textBody and textSmallTrendBody");

        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // Verify matchesVariationFilter handles mode and type dimensions with AI-Driven, Healed, and Playbook segments
        Assertions.assertTrue(jsContent.contains("if (dim === 'mode' || dim === 'type')"),
            "matchesVariationFilter must recognize both 'mode' and 'type' filter dimensions");
        Assertions.assertTrue(jsContent.contains("val.includes('playbook')"),
            "matchesVariationFilter must evaluate playbook segments in mode dimension");

        // Verify recalculateContainerText selector handles data-filter-dim="mode"
        Assertions.assertTrue(jsContent.contains("data-filter-dim=\"mode\""),
            "recalculateContainerText must query elements with data-filter-dim=\"mode\"");

        // Verify resolveVariationDimensionValue classifies mode as AI-Driven
        Assertions.assertTrue(jsContent.contains("if (isAi) return 'AI-Driven';"),
            "resolveVariationDimensionValue must return 'AI-Driven' for AI executions");

        // Verify resolveDimensionCount maps AI-Driven
        Assertions.assertTrue(jsContent.contains("counts['AI-Driven']"),
            "resolveDimensionCount must support 'AI-Driven' segment count lookup");

        // Execute live runtime verification in Node VM
        final String testScript = String.join("\n",
            "const fs = require('fs');",
            "const code = fs.readFileSync('" + jsPath.toAbsolutePath().toString().replace("\\", "/") + "', 'utf8');",
            "const vm = require('vm');",
            "const mockDoc = { querySelector: () => null, querySelectorAll: () => [], getElementById: () => null, addEventListener: () => {}, body: { addEventListener: () => {} } };",
            "const mockWin = { addEventListener: () => {}, document: mockDoc, ResizeObserver: class { observe() {} unobserve() {} disconnect() {} } };",
            "mockDoc.defaultView = mockWin;",
            "const context = { window: mockWin, document: mockDoc, console: console, ResizeObserver: mockWin.ResizeObserver, setTimeout: setTimeout, clearTimeout: clearTimeout };",
            "vm.createContext(context);",
            "vm.runInContext(code, context);",
            "const vAi = { s: 'PASSED', b: 'Chrome', m: 'AI', h: false };",
            "const vHealed = { s: 'PASSED', b: 'Chrome', m: 'JAVA', h: true };",
            "const vPlaybook = { s: 'PASSED', b: 'Chrome', m: 'PLAYBOOK', h: false };",
            "const vRecording = { s: 'PASSED', b: 'Chrome', m: 'RECORDING', h: false };",
            "const activeFiltersMode = { mode: new Set(['AI-Driven']) };",
            "const activeFiltersType = { type: new Set(['AI-Driven']) };",
            "context.vAi = vAi;",
            "context.vHealed = vHealed;",
            "context.vPlaybook = vPlaybook;",
            "context.vRecording = vRecording;",
            "context.activeFiltersMode = activeFiltersMode;",
            "context.activeFiltersType = activeFiltersType;",
            "const matchAiMode = vm.runInContext('matchesContainerFilters(vAi, activeFiltersMode, null)', context);",
            "const matchAiType = vm.runInContext('matchesContainerFilters(vAi, activeFiltersType, null)', context);",
            "const matchHealedMode = vm.runInContext('matchesContainerFilters(vHealed, activeFiltersMode, null)', context);",
            "const matchPlaybookMode = vm.runInContext('matchesContainerFilters(vPlaybook, activeFiltersMode, null)', context);",
            "const matchRecordingMode = vm.runInContext('matchesContainerFilters(vRecording, activeFiltersMode, null)', context);",
            "const facetExcludeMode = vm.runInContext('matchesContainerFilters(vHealed, activeFiltersMode, \\'mode\\')', context);",
            "const facetExcludeType = vm.runInContext('matchesContainerFilters(vHealed, activeFiltersType, \\'mode\\')', context);",
            "if (matchAiMode !== true) throw new Error('vAi must match active filter mode=AI-Driven');",
            "if (matchAiType !== true) throw new Error('vAi must match active filter type=AI-Driven');",
            "if (matchHealedMode !== false) throw new Error('vHealed must be filtered out by mode=AI-Driven');",
            "if (matchPlaybookMode !== false) throw new Error('vPlaybook must be filtered out by mode=AI-Driven');",
            "if (matchRecordingMode !== false) throw new Error('vRecording must be filtered out by mode=AI-Driven');",
            "if (facetExcludeMode !== true) throw new Error('Facet exclusion for mode must allow vHealed on AI_USAGE card');",
            "if (facetExcludeType !== true) throw new Error('Facet exclusion with legacy type must allow vHealed on AI_USAGE card');",
            "const dimVal = vm.runInContext('resolveVariationDimensionValue(vAi, \\'mode\\')', context);",
            "if (dimVal !== 'AI-Driven') throw new Error('resolveVariationDimensionValue must return AI-Driven but got: ' + dimVal);",
            "const dimCount = vm.runInContext('resolveDimensionCount(\\'AI-Driven\\', { \\'AI\\': 16 }, \\'mode\\')', context);",
            "if (dimCount !== 16) throw new Error('resolveDimensionCount must map AI-Driven to AI count 16 but got: ' + dimCount);",
            "console.log('SUCCESS');"
        );

        final ProcessBuilder pb = new ProcessBuilder("node", "-e", testScript);
        pb.redirectErrorStream(true);
        final Process p = pb.start();
        final StringBuilder out = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                out.append(line).append("\n");
            }
        }

        final int exitCode = p.waitFor();
        Assertions.assertEquals(0, exitCode, "United container AI usage KPI filtering test failed:\n" + out);
        Assertions.assertTrue(out.toString().contains("SUCCESS"), "Test output must contain SUCCESS: " + out);
    }

    @Test
    public void testWidgetGroupChildDragAndDropOrderingAssets() throws Exception
    {
        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(htmlPath), "dashboard-widgets.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(htmlContent.contains("child-drag-handle"),
            "dashboard-widgets.html must render child-drag-handle on container widgets");
        Assertions.assertTrue(htmlContent.contains("title=\"Drag to reorder in widget group\""),
            "dashboard-widgets.html must set drag handle tooltip for child widgets");

        final Path cssPath = Paths.get("src/main/resources/static/dashboard-styles.css");
        Assertions.assertTrue(Files.exists(cssPath), "dashboard-styles.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(cssContent.contains(".widget-container-section.widget-is-dragging"),
            "dashboard-styles.css must define dragging state style for child widgets");
        Assertions.assertTrue(cssContent.contains(".widget-container-section .child-drag-handle"),
            "dashboard-styles.css must style child-drag-handle");
        Assertions.assertTrue(cssContent.contains(".widget-container-section.drop-reorder-left") &&
                              cssContent.contains(".widget-container-section.drop-reorder-right"),
            "dashboard-styles.css must define horizontal drop indicator styles for side-by-side child sections");
        Assertions.assertTrue(cssContent.contains(".widget-container-section.drop-reorder-top") &&
                              cssContent.contains(".widget-container-section.drop-reorder-bottom"),
            "dashboard-styles.css must define vertical drop indicator styles for stacked child sections");

        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(jsContent.contains("activeDraggedChildWidgetId"),
            "report-manager.js must define activeDraggedChildWidgetId");
        Assertions.assertTrue(jsContent.contains("activeDraggedChildContainerId"),
            "report-manager.js must define activeDraggedChildContainerId");
        Assertions.assertTrue(jsContent.contains("POST', '/widgets/reorder-children'"),
            "report-manager.js must invoke /widgets/reorder-children on drop reorder");
    }

    @Test
    public void testExecutionIdentityLockedByTestAndBrowserInAllDialogs() throws Exception
    {
        final Path modalsPath = Paths.get("src/main/resources/templates/fragments/modals.html");
        Assertions.assertTrue(Files.exists(modalsPath), "modals.html must exist: " + modalsPath.toAbsolutePath());
        final String modalsContent = Files.readString(modalsPath, StandardCharsets.UTF_8);

        final Path widgetConfigPath = Paths.get("src/main/resources/templates/fragments/widget-configurator-modal.html");
        Assertions.assertTrue(Files.exists(widgetConfigPath), "widget-configurator-modal.html must exist: " + widgetConfigPath.toAbsolutePath());
        final String widgetConfigContent = Files.readString(widgetConfigPath, StandardCharsets.UTF_8);

        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // Verify Browser is rendered as a locked pill in createPageModal and convertToGroupModal
        Assertions.assertTrue(
            modalsContent.contains("title=\"Browser (Mandatory Base Coordinate)\""),
            "modals.html must render locked Browser pill with mandatory coordinate title"
        );
        Assertions.assertFalse(
            modalsContent.contains("name=\"createPageIdentityDimCheckbox\" value=\"browser\""),
            "createPageModal must NOT render a toggleable checkbox for browser"
        );
        Assertions.assertFalse(
            modalsContent.contains("name=\"convertToGroupIdentityDimCheckbox\" value=\"browser\""),
            "convertToGroupModal must NOT render a toggleable checkbox for browser"
        );

        // Verify Browser is rendered as a locked pill in widget configurator modal
        Assertions.assertTrue(
            widgetConfigContent.contains("title=\"Browser (Mandatory Base Coordinate)\""),
            "widget-configurator-modal.html must render locked Browser pill with mandatory coordinate title"
        );
        Assertions.assertFalse(
            widgetConfigContent.contains("name=\"identityDimCheckbox\" value=\"browser\""),
            "widget-configurator-modal.html must NOT render a toggleable checkbox for browser"
        );

        // Verify JavaScript functions always include browser as base identity
        Assertions.assertTrue(
            jsContent.contains("var vals = ['browser'];"),
            "report-manager.js updateExecutionIdentityDimensionsInput must default to including browser"
        );
        Assertions.assertTrue(
            jsContent.contains("const vals = ['browser', ...checked.filter(d => d.toLowerCase() !== 'browser')];"),
            "report-manager.js must lock browser as base dimension in create page and convert to group identity inputs"
        );
    }

    @Test
    public void testUnitedPieChartExpandedAndDefaultDensityLayout() throws Exception
    {
        final Path cssPath = Paths.get("src/main/resources/static/dashboard-styles.css");
        Assertions.assertTrue(Files.exists(cssPath), "dashboard-styles.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        // Verify enhanced unexpanded donut density and dimensions in widget groups
        Assertions.assertTrue(cssContent.contains(".widget-container-pie-section .pie-chart-wrapper-batch"),
            "dashboard-styles.css must define .widget-container-pie-section .pie-chart-wrapper-batch");
        Assertions.assertTrue(cssContent.contains("width: 160px !important;") && cssContent.contains("height: 160px !important;"),
            "dashboard-styles.css must enlarge default unexpanded donut to 160px to eliminate excessive dead space");
        Assertions.assertTrue(cssContent.contains(".widget-container-pie-section .pie-center-rate")
            && cssContent.contains("font-size: 1.85rem !important;"),
            "dashboard-styles.css must configure prominent 1.85rem rate typography for unexpanded group pie chart");

        // Verify expanded full-span layout rules for pie chart in widget groups
        Assertions.assertTrue(cssContent.contains(".widget-container-pie-section.widget-container-full-span .widget-card-body"),
            "dashboard-styles.css must define expanded body rules for .widget-container-pie-section.widget-container-full-span");
        Assertions.assertTrue(cssContent.contains("min-height: 280px !important;"),
            "dashboard-styles.css must expand vertical length/height with min-height: 280px when full span is active");
        Assertions.assertTrue(cssContent.contains(".widget-container-pie-section.widget-container-full-span .pie-chart-wrapper-batch"),
            "dashboard-styles.css must define expanded pie-chart-wrapper-batch");
        Assertions.assertTrue(cssContent.contains("width: 220px !important;") && cssContent.contains("height: 220px !important;"),
            "dashboard-styles.css must scale expanded donut dimensions to 220px");
        Assertions.assertTrue(cssContent.contains("font-size: 2.6rem !important;"),
            "dashboard-styles.css must scale expanded rate typography to 2.6rem");

        // Verify multi-column responsive grid layout for breakdown slices when expanded
        Assertions.assertTrue(cssContent.contains(".widget-container-pie-section.widget-container-full-span .pie-breakdown-list-sm"),
            "dashboard-styles.css must define expanded breakdown list rules");
        Assertions.assertTrue(cssContent.contains("repeat(auto-fit, minmax(220px, 1fr)) !important;"),
            "dashboard-styles.css must distribute slices across responsive multi-column auto-fit grid to utilize horizontal space");
    }

    @Test
    public void testContainerPieChartSvgLayerGroupingAndOverlaySynchronization() throws Exception
    {
        final Path htmlPath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(htmlPath), "dashboard-widgets.html must exist: " + htmlPath.toAbsolutePath());
        final String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);

        final Path cssPath = Paths.get("src/main/resources/static/dashboard-styles.css");
        Assertions.assertTrue(Files.exists(cssPath), "dashboard-styles.css must exist: " + cssPath.toAbsolutePath());
        final String cssContent = Files.readString(cssPath, StandardCharsets.UTF_8);

        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist: " + jsPath.toAbsolutePath());
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // 1. Verify SVG slice layer grouping in dashboard-widgets.html
        Assertions.assertTrue(htmlContent.contains("<g class=\"pie-slice-group widget-interactive-filter\""),
            "dashboard-widgets.html must enclose pie slices in .pie-slice-group.widget-interactive-filter");
        Assertions.assertTrue(htmlContent.contains("class=\"pie-slice-base\""),
            "dashboard-widgets.html must mark base donut circle with .pie-slice-base");
        Assertions.assertTrue(htmlContent.contains("class=\"pie-slice-healed\""),
            "dashboard-widgets.html must mark healed circle with .pie-slice-healed");
        Assertions.assertTrue(htmlContent.contains("class=\"pie-slice-sparkle\""),
            "dashboard-widgets.html must mark sparkle overlay with .pie-slice-sparkle");
        Assertions.assertTrue(htmlContent.contains("class=\"pie-slice-ai\""),
            "dashboard-widgets.html must mark AI circle with .pie-slice-ai");
        Assertions.assertTrue(htmlContent.contains("class=\"pie-slice-stripes\""),
            "dashboard-widgets.html must mark AI stripe overlay with .pie-slice-stripes");
        Assertions.assertTrue(htmlContent.contains("viewBox=\"-2 -2 40 40\" style=\"overflow: visible;\""),
            "dashboard-widgets.html must configure expanded viewBox -2 -2 40 40 with visible overflow to eliminate donut clipping");

        // 2. Verify dedicated container active filter strip outside title badge row
        Assertions.assertTrue(htmlContent.contains("<!-- DEDICATED CONTAINER ACTIVE FILTER STRIP -->"),
            "dashboard-widgets.html must contain dedicated container active filter strip comment");
        Assertions.assertTrue(htmlContent.contains("class=\"container-active-filter-strip d-flex align-items-center gap-2 flex-wrap px-3 py-1-5 border-bottom\""),
            "dashboard-widgets.html must render dedicated container-active-filter-strip bar");

        // 3. Verify CSS styling for grouped slices and filter strip
        Assertions.assertTrue(cssContent.contains("g.pie-slice-group.widget-interactive-filter"),
            "dashboard-styles.css must style g.pie-slice-group.widget-interactive-filter");
        Assertions.assertTrue(cssContent.contains(".has-active-cross-filter g.pie-slice-group.widget-interactive-filter:not(.is-filter-active) circle:not(.pie-slice-base)"),
            "dashboard-styles.css must suppress decorative overlays on dimmed inactive slices to prevent pattern cracks");
        Assertions.assertTrue(cssContent.contains(".container-active-filter-strip"),
            "dashboard-styles.css must style .container-active-filter-strip");

        // 4. Verify JS support for .pie-slice-group in recalculateStatusPieChart
        Assertions.assertTrue(jsContent.contains("const sliceGroups = Array.from(sec.querySelectorAll('.pie-slice-group'));"),
            "report-manager.js must query .pie-slice-group in recalculateStatusPieChart");
        Assertions.assertTrue(jsContent.contains("Math.round(((kpiPassed + kpiFixed) / kpiTotal) * 100)"),
            "report-manager.js must calculate KPI pass rate from strictly filtered variations");

        // 5. Live Node.js runtime simulation of interactive filtering and clearing
        final String nodeTestScript = String.join("\n",
            "const fs = require('fs');",
            "const vm = require('vm');",
            "const code = fs.readFileSync('" + jsPath.toAbsolutePath().toString().replace("\\", "/") + "', 'utf8');",
            "",
            "function createEl(tag, cls = '') {",
            "    const el = {",
            "        tagName: tag.toUpperCase(),",
            "        className: cls,",
            "        classList: {",
            "            _c: new Set(cls.split(' ').filter(Boolean)),",
            "            add: (c) => el.classList._c.add(c),",
            "            remove: (c) => el.classList._c.delete(c),",
            "            contains: (c) => el.classList._c.has(c),",
            "            toggle: (c, force) => {",
            "                if (force !== undefined) {",
            "                    if (force) el.classList._c.add(c);",
            "                    else el.classList._c.delete(c);",
            "                } else {",
            "                    if (el.classList._c.has(c)) el.classList._c.delete(c);",
            "                    else el.classList._c.add(c);",
            "                }",
            "            }",
            "        },",
            "        attributes: {},",
            "        style: {},",
            "        children: [],",
            "        parentNode: null,",
            "        textContent: '',",
            "        setAttribute: (k, v) => { el.attributes[k] = String(v); },",
            "        getAttribute: (k) => el.attributes[k] !== undefined ? el.attributes[k] : null,",
            "        hasAttribute: (k) => el.attributes[k] !== undefined,",
            "        removeAttribute: (k) => { delete el.attributes[k]; },",
            "        appendChild: (ch) => { ch.parentNode = el; el.children.push(ch); return ch; },",
            "        querySelector: (sel) => {",
            "            const matches = el.querySelectorAll(sel);",
            "            return matches.length > 0 ? matches[0] : null;",
            "        },",
            "        querySelectorAll: (sel) => {",
            "            const res = [];",
            "            const check = (node) => {",
            "                if (sel === '.widget-container-dataset' && node.classList.contains('widget-container-dataset')) return true;",
            "                if (sel === '.pie-slice-group' && node.classList.contains('pie-slice-group')) return true;",
            "                if (sel === '.widget-interactive-filter' && node.classList.contains('widget-interactive-filter')) return true;",
            "                if (sel === '.pie-slice-base' && node.classList.contains('pie-slice-base')) return true;",
            "                if (sel === '.pie-slice-healed' && node.classList.contains('pie-slice-healed')) return true;",
            "                if (sel === '.pie-slice-sparkle' && node.classList.contains('pie-slice-sparkle')) return true;",
            "                if (sel === '.pie-slice-ai' && node.classList.contains('pie-slice-ai')) return true;",
            "                if (sel === '.pie-slice-stripes' && node.classList.contains('pie-slice-stripes')) return true;",
            "                if (sel === '.pie-slice-healed, .pie-slice-sparkle' && (node.classList.contains('pie-slice-healed') || node.classList.contains('pie-slice-sparkle'))) return true;",
            "                if (sel === '.pie-slice-ai, .pie-slice-stripes' && (node.classList.contains('pie-slice-ai') || node.classList.contains('pie-slice-stripes'))) return true;",
            "                if (sel === '.pie-center-rate' && node.classList.contains('pie-center-rate')) return true;",
            "                if (sel === '.pie-center-sub' && node.classList.contains('pie-center-sub')) return true;",
            "                if (sel === '.widget-section-header .font-mono' && node.classList.contains('font-mono')) return true;",
            "                if (sel === '.widget-child-card, .widget-container-section' && (node.classList.contains('widget-child-card') || node.classList.contains('widget-container-section'))) return true;",
            "                if (sel === '.container-active-filter-strip' && node.classList.contains('container-active-filter-strip')) return true;",
            "                if (sel === '.container-filter-chips' && node.classList.contains('container-filter-chips')) return true;",
            "                if (sel === '.btn-clear-all-container-filters' && node.classList.contains('btn-clear-all-container-filters')) return true;",
            "                if (sel === '.pie-breakdown-item' && node.classList.contains('pie-breakdown-item')) return true;",
            "                return false;",
            "            };",
            "            const walk = (node) => {",
            "                for (const ch of node.children) {",
            "                    if (check(ch)) res.push(ch);",
            "                    walk(ch);",
            "                }",
            "            };",
            "            walk(el);",
            "            return res;",
            "        },",
            "        closest: (sel) => {",
            "            let cur = el.parentNode;",
            "            while (cur) {",
            "                if (sel === '.widget-container-card' && cur.classList && cur.classList.contains('widget-container-card')) return cur;",
            "                if (sel === '.widget-child-card, .widget-container-section' && cur.classList && (cur.classList.contains('widget-child-card') || cur.classList.contains('widget-container-section'))) return cur;",
            "                cur = cur.parentNode;",
            "            }",
            "            return null;",
            "        }",
            "    };",
            "    return el;",
            "}",
            "",
            "const mockDoc = {",
            "    createElement: (tag) => createEl(tag),",
            "    querySelector: () => null,",
            "    querySelectorAll: () => [],",
            "    getElementById: () => null,",
            "    addEventListener: () => {},",
            "    body: { addEventListener: () => {} }",
            "};",
            "const mockWin = {",
            "    addEventListener: () => {},",
            "    document: mockDoc,",
            "    ResizeObserver: class { observe() {} unobserve() {} disconnect() {} }",
            "};",
            "mockDoc.defaultView = mockWin;",
            "const context = { window: mockWin, document: mockDoc, console: console, ResizeObserver: mockWin.ResizeObserver, setTimeout: setTimeout, clearTimeout: clearTimeout };",
            "vm.createContext(context);",
            "vm.runInContext(code, context);",
            "",
            "const container = createEl('div', 'widget-container-card');",
            "const variations = [",
            "    { s: 'PASSED', b: 'Chrome', m: 'JAVA', h: false },",
            "    { s: 'FIXED', b: 'Chrome', m: 'JAVA', h: true },",
            "    { s: 'FAILED', b: 'Chrome', m: 'JAVA', h: false }",
            "];",
            "const dataScript = createEl('script', 'widget-container-dataset');",
            "dataScript.textContent = JSON.stringify(variations);",
            "container.appendChild(dataScript);",
            "container.setAttribute('data-container-filters', '{}');",
            "",
            "const filterStrip = createEl('div', 'container-active-filter-strip');",
            "filterStrip.style.display = 'none';",
            "const chips = createEl('div', 'container-filter-chips');",
            "const clearBtn = createEl('button', 'btn-clear-all-container-filters');",
            "filterStrip.appendChild(chips);",
            "filterStrip.appendChild(clearBtn);",
            "container.appendChild(filterStrip);",
            "",
            "const sec = createEl('div', 'widget-child-card widget-container-section');",
            "sec.setAttribute('data-presentation', 'PIE_CHART');",
            "sec.setAttribute('data-metric', 'STATUS');",
            "container.appendChild(sec);",
            "",
            "const secHeader = createEl('div', 'widget-section-header');",
            "const fontMono = createEl('span', 'font-mono');",
            "fontMono.textContent = '3 Tests';",
            "secHeader.appendChild(fontMono);",
            "sec.appendChild(secHeader);",
            "",
            "const centerRate = createEl('div', 'pie-center-rate');",
            "centerRate.textContent = '67%';",
            "const centerSub = createEl('div', 'pie-center-sub');",
            "centerSub.textContent = 'PASS';",
            "sec.appendChild(centerRate);",
            "sec.appendChild(centerSub);",
            "",
            "const grpPass = createEl('g', 'pie-slice-group widget-interactive-filter');",
            "grpPass.setAttribute('data-filter-dim', 'status');",
            "grpPass.setAttribute('data-filter-val', 'Passed');",
            "const passBase = createEl('circle', 'pie-slice-base');",
            "grpPass.appendChild(passBase);",
            "sec.appendChild(grpPass);",
            "",
            "const grpFixed = createEl('g', 'pie-slice-group widget-interactive-filter');",
            "grpFixed.setAttribute('data-filter-dim', 'status');",
            "grpFixed.setAttribute('data-filter-val', 'Succeeded Fixed');",
            "const fixedBase = createEl('circle', 'pie-slice-base');",
            "const fixedHealed = createEl('circle', 'pie-slice-healed');",
            "const fixedSparkle = createEl('circle', 'pie-slice-sparkle');",
            "grpFixed.appendChild(fixedBase);",
            "grpFixed.appendChild(fixedHealed);",
            "grpFixed.appendChild(fixedSparkle);",
            "sec.appendChild(grpFixed);",
            "",
            "const grpFail = createEl('g', 'pie-slice-group widget-interactive-filter');",
            "grpFail.setAttribute('data-filter-dim', 'status');",
            "grpFail.setAttribute('data-filter-val', 'Failed');",
            "const failBase = createEl('circle', 'pie-slice-base');",
            "grpFail.appendChild(failBase);",
            "sec.appendChild(grpFail);",
            "",
            "context.grpFixed = grpFixed;",
            "context.clearBtn = clearBtn;",
            "context.container = container;",
            "context.sec = sec;",
            "vm.runInContext('onContainerWidgetFilterClick(grpFixed, { stopPropagation: () => {} })', context);",
            "",
            "if (grpFixed.style.opacity !== '1' || !grpFixed.classList._c.has('is-filter-active')) throw new Error('Fixed group should be active');",
            "if (grpPass.style.opacity !== '0.38') throw new Error('Pass group should be dimmed to 0.38');",
            "if (grpFail.style.opacity !== '0.38') throw new Error('Fail group should be dimmed to 0.38');",
            "if (fixedBase.getAttribute('stroke-dasharray') !== '100.00 0.00') throw new Error('Fixed base donut should expand to 100% (100.00 0.00) but was: ' + fixedBase.getAttribute('stroke-dasharray'));",
            "if (grpPass.style.display !== 'none') throw new Error('Pass slice should be hidden when filtered out');",
            "if (grpFail.style.display !== 'none') throw new Error('Fail slice should be hidden when filtered out');",
            "if (centerRate.textContent !== '100%') throw new Error('Center rate should be 100% but was: ' + centerRate.textContent);",
            "if (fontMono.textContent !== '1 of 3 Tests') throw new Error('Summary count should be 1 of 3 Tests but was: ' + fontMono.textContent);",
            "",
            "vm.runInContext('clearContainerCrossFilter(clearBtn, { stopPropagation: () => {} })', context);",
            "",
            "if (grpPass.style.opacity !== '1') throw new Error('Pass group should be restored to 1');",
            "if (grpFixed.style.opacity !== '1') throw new Error('Fixed group should be restored to 1');",
            "if (grpFail.style.opacity !== '1') throw new Error('Fail group should be restored to 1');",
            "if (grpPass.style.display !== '') throw new Error('Pass slice display should be restored');",
            "if (grpFail.style.display !== '') throw new Error('Fail slice display should be restored');",
            "if (centerRate.textContent !== '67%') throw new Error('Center rate should restore to 67% but was: ' + centerRate.textContent);",
            "if (fontMono.textContent !== '3 Tests') throw new Error('Summary text should restore to 3 Tests but was: ' + fontMono.textContent);",
            "console.log('SUCCESS');"
        );

        final ProcessBuilder pb = new ProcessBuilder("node", "-e", nodeTestScript);
        pb.redirectErrorStream(true);
        final Process p = pb.start();
        final StringBuilder out = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                out.append(line).append("\n");
            }
        }

        final int exitCode = p.waitFor();
        Assertions.assertEquals(0, exitCode, "Container pie chart SVG layer grouping and overlay synchronization test failed:\n" + out);
        Assertions.assertTrue(out.toString().contains("SUCCESS"), "Test output must contain SUCCESS: " + out);
    }

    /**
     * Automated regression safety net verifying that clicking a test variation row within
     * the Known Test Base Variations widget on the dashboard correctly resolves the row element,
     * recognizes it as a test variation, displays sidePageTestBaseVariationControls, updates
     * variation badges (locale, browser, batch), and invokes loadTestBaseVariationHistory.
     *
     * @throws Exception
     *             if template reading or node script execution fails
     * @author AI-generated: Gemini 3.8 Flash (High)
     * @author Xceptance GmbH 2026
     */
    @Test
    public void testKnownTestVariationsWidgetRowClickOpensVariationHistorySafetyNet() throws Exception
    {
        final Path templatePath = Paths.get("src/main/resources/templates/fragments/dashboard-widgets.html");
        Assertions.assertTrue(Files.exists(templatePath), "dashboard-widgets.html must exist");
        final String templateContent = Files.readString(templatePath, StandardCharsets.UTF_8);

        // 1. Verify template contract: th:data-row-id must match th:id with widget id prefix
        Assertions.assertFalse(
            templateContent.contains("th:data-row-id=\"'row-var-' + ${var.id}\""),
            "dashboard-widgets.html must not use unprefixed 'row-var-' for data-row-id on widget rows"
        );
        Assertions.assertTrue(
            templateContent.contains("th:data-row-id=\"'widget-' + ${w.id} + '-row-var-' + ${var.id}\""),
            "dashboard-widgets.html must prefix data-row-id with widget id matching th:id"
        );

        final Path jsPath = Paths.get("src/main/resources/static/js/report-manager.js");
        Assertions.assertTrue(Files.exists(jsPath), "report-manager.js must exist");
        final String jsContent = Files.readString(jsPath, StandardCharsets.UTF_8);

        // 2. Verify JavaScript contracts in report-manager.js
        Assertions.assertTrue(
            jsContent.contains("const rowId = row.id || row.getAttribute('data-row-id');"),
            "handleRowClick must prioritize row.id before getAttribute('data-row-id')"
        );
        Assertions.assertTrue(
            jsContent.contains("activeRow.closest('.widget-test-variations-body') !== null"),
            "openTestSidePagePanel must recognize .widget-test-variations-body for isTestBaseRow routing"
        );
        Assertions.assertTrue(
            jsContent.contains("const batchBadge = document.getElementById('tbSideBatchBadge');"),
            "openTestSidePagePanel must synchronize tbSideBatchBadge with the variation batch"
        );

        // 3. Node.js headless DOM behavioral assertion
        final String nodeTestScript = String.join(
            "\n",
            "const fs = require('fs');",
            "const vm = require('vm');",
            "",
            "const jsCode = fs.readFileSync('src/main/resources/static/js/report-manager.js', 'utf8');",
            "",
            "let historyLoaded = false;",
            "let loadedParams = null;",
            "const mockElements = {};",
            "",
            "function createMockEl(id, tag, classes) {",
            "    const el = {",
            "        id: id || '',",
            "        tagName: (tag || 'DIV').toUpperCase(),",
            "        classList: {",
            "            _classes: new Set(classes || []),",
            "            add: function(c) { this._classes.add(c); },",
            "            remove: function(c) { this._classes.delete(c); },",
            "            contains: function(c) { return this._classes.has(c); }",
            "        },",
            "        style: {},",
            "        attributes: {},",
            "        getAttribute: function(a) { return this.attributes[a] || null; },",
            "        setAttribute: function(a, v) { this.attributes[a] = String(v); },",
            "        hasAttribute: function(a) { return a in this.attributes; },",
            "        removeAttribute: function(a) { delete this.attributes[a]; },",
            "        parentElement: null,",
            "        children: [],",
            "        closest: function(sel) {",
            "            let cur = this;",
            "            while (cur) {",
            "                if (sel === '.widget-test-variations-body' && cur.classList.contains('widget-test-variations-body')) return cur;",
            "                if (sel === '#pageTestBase' && cur.id === 'pageTestBase') return cur;",
            "                if (sel === '#batchTestBaseVariationsPanel' && cur.id === 'batchTestBaseVariationsPanel') return cur;",
            "                cur = cur.parentElement;",
            "            }",
            "            return null;",
            "        },",
            "        querySelector: function() { return null; },",
            "        querySelectorAll: function() { return []; },",
            "        prepend: function() {},",
            "        appendChild: function(c) { this.children.push(c); c.parentElement = this; return c; }",
            "    };",
            "    if (id) mockElements[id] = el;",
            "    return el;",
            "}",
            "",
            "const widgetBody = createMockEl('widgetBody', 'div', ['widget-test-variations-body']);",
            "const rowEl = createMockEl('widget-1-row-var-42', 'tr', ['tb-entry-row', 'clickable-row', 'js-open-side-panel']);",
            "rowEl.setAttribute('data-row-id', 'widget-1-row-var-42');",
            "rowEl.setAttribute('data-test-name', 'com.example.SampleTest');",
            "rowEl.setAttribute('data-test-method', 'executeTest');",
            "rowEl.setAttribute('data-dataset', 'dataset_1');",
            "rowEl.setAttribute('data-location', 'DEU');",
            "rowEl.setAttribute('data-browser', 'Chrome_1920x1080');",
            "rowEl.setAttribute('data-batch', 'Nightly');",
            "rowEl.setAttribute('data-status', 'passed');",
            "widgetBody.appendChild(rowEl);",
            "",
            "createMockEl('testSidePagePanel', 'div', ['allure-side-page-panel']);",
            "createMockEl('sidePageSingleRunControls', 'div', []);",
            "createMockEl('sidePageTestBaseVariationControls', 'div', []);",
            "createMockEl('tbSideLocaleBadge', 'span', []);",
            "createMockEl('tbSideBrowserBadge', 'span', []);",
            "createMockEl('tbSideBatchBadge', 'span', []);",
            "createMockEl('sidePageTestName', 'div', []);",
            "createMockEl('sidePageTestDataSet', 'div', []);",
            "const mockBody = { addEventListener: () => {}, removeEventListener: () => {}, style: {} };",
            "const mockDoc = {",
            "    body: mockBody,",
            "    addEventListener: function() {},",
            "    removeEventListener: function() {},",
            "    getElementById: function(id) { return mockElements[id] || null; },",
            "    querySelector: function(sel) {",
            "        if (sel === '[data-row-id=\"widget-1-row-var-42\"]') return rowEl;",
            "        return null;",
            "    },",
            "    querySelectorAll: function(sel) {",
            "        if (sel === 'tr.clickable-row') return [rowEl];",
            "        return [];",
            "    }",
            "};",
            "const mockWin = {",
            "    document: mockDoc,",
            "    addEventListener: function() {},",
            "    removeEventListener: function() {},",
            "    location: { href: 'http://localhost:8080/dashboard' },",
            "    history: { replaceState: function() {} },",
            "    ResizeObserver: class { observe() {} unobserve() {} disconnect() {} }",
            "};",
            "mockDoc.defaultView = mockWin;",
            "",
            "const context = {",
            "    console: console,",
            "    window: mockWin,",
            "    document: mockDoc,",
            "    ResizeObserver: mockWin.ResizeObserver,",
            "    setTimeout: setTimeout,",
            "    clearTimeout: clearTimeout,",
            "    CSS: { escape: function(s) { return s; } },",
            "    localStorage: { getItem: function() { return null; }, setItem: function() {} }",
            "};",
            "",
            "vm.createContext(context);",
            "vm.runInContext(jsCode, context);",
            "",
            "historyLoaded = false;",
            "loadedParams = null;",
            "context.loadTestBaseVariationHistory = function(testClass, testMethod, dataSet, loc, browser) {",
            "    historyLoaded = true;",
            "    loadedParams = { testClass, testMethod, dataSet, loc, browser };",
            "};",
            "",
            "// Simulate row click event on the widget row",
            "context.handleRowClick({ currentTarget: rowEl, preventDefault: () => {} });",
            "",
            "const singleRun = mockElements['sidePageSingleRunControls'];",
            "const testBase = mockElements['sidePageTestBaseVariationControls'];",
            "const batchBadge = mockElements['tbSideBatchBadge'];",
            "",
            "if (singleRun.style.display !== 'none') throw new Error('singleRunControls must be hidden (display: none)');",
            "if (testBase.style.display !== 'flex') throw new Error('testBaseVariationControls must be displayed (display: flex)');",
            "if (!historyLoaded) throw new Error('loadTestBaseVariationHistory was not invoked on row click');",
            "if (!batchBadge.innerHTML.includes('Nightly')) throw new Error('tbSideBatchBadge should contain batch name Nightly but was: ' + batchBadge.innerHTML);",
            "if (loadedParams.testClass !== 'com.example.SampleTest' || loadedParams.testMethod !== 'executeTest' || loadedParams.dataSet !== 'dataset_1') {",
            "    throw new Error('Incorrect variation parameters passed to loadTestBaseVariationHistory: ' + JSON.stringify(loadedParams));",
            "}",
            "console.log('SUCCESS');"
        );

        final ProcessBuilder pb = new ProcessBuilder("node", "-e", nodeTestScript);
        pb.redirectErrorStream(true);
        final Process p = pb.start();
        final StringBuilder out = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                out.append(line).append("\n");
            }
        }

        final int exitCode = p.waitFor();
        Assertions.assertEquals(0, exitCode, "Known test variations widget row click regression test failed:\n" + out);
        Assertions.assertTrue(out.toString().contains("SUCCESS"), "Output must contain SUCCESS: " + out);
    }
}


