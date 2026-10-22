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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
        final Path jsDirPath = Paths.get("src/main/resources/static/js");
        Assertions.assertTrue(Files.exists(jsDirPath), "Static JS directory must exist: " + jsDirPath.toAbsolutePath());

        final List<File> jsFiles;
        try (final Stream<Path> stream = Files.list(jsDirPath))
        {
            jsFiles = stream
                .filter(p -> p.toString().endsWith(".js"))
                .map(Path::toFile)
                .sorted()
                .collect(Collectors.toList());
        }

        Assertions.assertFalse(jsFiles.isEmpty(), "At least one JavaScript file should be present in " + jsDirPath);

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

    private boolean isNodeAvailable()
    {
        try
        {
            final Process process = new ProcessBuilder("node", "-v").start();
            final int exitCode = process.waitFor();
            return exitCode == 0;
        }
        catch (final Exception e)
        {
            return false;
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

        final int exitCode = process.waitFor();
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
        final Path historyPath = Paths.get("src/main/resources/templates/fragments/batch-history.html");
        Assertions.assertTrue(Files.exists(historyPath), "batch-history.html must exist: " + historyPath.toAbsolutePath());
        final String historyContent = Files.readString(historyPath, StandardCharsets.UTF_8);

        final String[] expectedLegendEntries = {
            "Passed Clean", "Passed Healed", "Passed AI-Driven",
            "Succeeded with Known Bug", "Succeeded Fixed Healed", "Succeeded Fixed AI-Driven",
            "Failed due to Known Bug", "Failed Known Healed", "Failed Known AI-Driven",
            "Failed without Issue Info", "Failed Unknown Healed", "Failed Unknown AI-Driven",
            "Ignored / Skipped"
        };
        for (final String entry : expectedLegendEntries)
        {
            Assertions.assertTrue(
                historyContent.contains(entry),
                "batch-history.html legend must contain '" + entry + "'"
            );
        }

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
        final Path historyPath = Paths.get("src/main/resources/templates/fragments/batch-history.html");
        Assertions.assertTrue(Files.exists(historyPath), "batch-history.html must exist: " + historyPath.toAbsolutePath());
        final String historyContent = Files.readString(historyPath, StandardCharsets.UTF_8);

        final String[] expectedLayerClasses = {
            "area-layer-known-ai",
            "area-layer-known-ai-pat",
            "area-layer-fixed-ai",
            "area-layer-fixed-ai-pat"
        };
        for (final String layerClass : expectedLayerClasses)
        {
            Assertions.assertTrue(
                historyContent.contains(layerClass),
                "batch-history.html area-trend-svg must contain layer class '" + layerClass + "'"
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
            indexContent.contains("report-manager.js?v=1.1.2"),
            "index.html must reference report-manager.js with cache buster ?v=1.1.2"
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
        Assertions.assertEquals(0, exitCode, "Filter state transition test failed:\n" + output);
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
        Assertions.assertEquals(0, exitCode, "AI log filtering test failed:\n" + output);
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

        final String[] expectedIcons = {"play_circle", "format_list_bulleted", "database"};
        for (final String icon : expectedIcons)
        {
            Assertions.assertTrue(
                sidebarContent.contains("<span class=\"material-symbols-outlined\">" + icon + "</span>"),
                "sidebar.html must contain icon: " + icon
            );
        }

        final String[] expectedLabels = {"Playbooks & Queue", "Run History", "Test Base"};
        for (final String label : expectedLabels)
        {
            Assertions.assertTrue(
                sidebarContent.contains("<span class=\"nav-label\">" + label + "</span>"),
                "sidebar.html must contain nav-label: " + label
            );
        }
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
        Assertions.assertEquals(0, exitCode, "Node script execution failed:\n" + output);
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
        Assertions.assertEquals(0, exitCode, "Node script execution failed:\n" + output);
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
        Assertions.assertEquals(0, exitCode, "Node script execution failed:\n" + output);
    }
}

