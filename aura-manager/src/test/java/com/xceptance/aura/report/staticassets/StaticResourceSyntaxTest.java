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
        Assertions.assertTrue(jsContent.contains("return clean.length > 0 ? clean : 'Browsing';"),
            "resolveVariationDimensionValue must return clean area fallback without @ prefix");

        // Verify Area normalization in resolveDimensionCount
        Assertions.assertTrue(jsContent.contains("const cleanRl = rl.replace(/^@/, '').replace(/\\s*\\(default\\)$/i, '').trim();"),
            "resolveDimensionCount must strip leading @ and trailing (default) from comparison target");
        Assertions.assertTrue(jsContent.contains("if (cleanK === cleanRl) return counts[k];"),
            "resolveDimensionCount must perform exact equality on normalized area values");

        // Verify Area normalization in matchesVariationFilter
        Assertions.assertTrue(jsContent.contains("const a = (v.a || '').toLowerCase().trim().replace(/^@/, '').replace(/\\s*\\(default\\)$/i, '').trim();"),
            "matchesVariationFilter must strip leading @ and trailing (default) for area filter matching");

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
        Assertions.assertTrue(
            modalContent.contains("widget.metric.name() == 'TAG'"),
            "widget-configurator-modal.html must include TAG metric check in th:disabled for PERLENKETTE"
        );
        Assertions.assertTrue(
            modalContent.contains("perlenketteRadio.disabled = (metric === 'TAG');"),
            "widget-configurator-modal.html onWidgetMetricChanged must toggle disabled state of perlenketteRadio"
        );
        Assertions.assertTrue(
            jsContent.contains("perlenketteRadio.disabled = (metric === 'TAG');"),
            "report-manager.js onWidgetMetricChanged must toggle disabled state of perlenketteRadio"
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
}


