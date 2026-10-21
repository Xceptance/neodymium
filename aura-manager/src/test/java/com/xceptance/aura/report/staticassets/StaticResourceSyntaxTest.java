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

        final Path coreJsPath = Paths.get("src/main/resources/static/dashboard-core.js");
        Assertions.assertTrue(Files.exists(coreJsPath), "dashboard-core.js must exist");
        final String coreJsContent = Files.readString(coreJsPath, StandardCharsets.UTF_8);

        Assertions.assertTrue(
            coreJsContent.contains("initTestSearchFilter();"),
            "dashboard-core.js must initialize search filter on load"
        );
        Assertions.assertTrue(
            coreJsContent.contains("addEventListener('pageshow'"),
            "dashboard-core.js must re-verify search filter on pageshow (BFcache support)"
        );
    }
}

