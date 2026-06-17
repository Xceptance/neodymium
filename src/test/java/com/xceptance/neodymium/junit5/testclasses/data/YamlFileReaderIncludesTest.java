/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.xceptance.neodymium.junit5.testclasses.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.xceptance.neodymium.common.testdata.util.YamlFileReader;
import com.xceptance.neodymium.common.testdata.util.MalformedPlaybookException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xceptance.neodymium.util.Neodymium;

public final class YamlFileReaderIncludesTest
{
    @TempDir
    File tempDir;

    private File mainPlaybookFile;
    private File fragmentFolder;
    private File loginStepsFile;
    private File defaultUserFile;

    @BeforeEach
    public final void setUp() throws IOException
    {
        this.fragmentFolder = new File(this.tempDir, "fragments");
        this.fragmentFolder.mkdirs();
        this.mainPlaybookFile = new File(this.tempDir, "main.yaml");
        this.loginStepsFile = new File(this.fragmentFolder, "login.steps");
        this.defaultUserFile = new File(this.fragmentFolder, "default_user.yaml");
    }

    @Test
    @DisplayName("Verify list-based steps and simple step inclusions resolution")
    public final void testSimpleStepInclusion() throws IOException
    {
        final String loginSteps = """
            - Enter username "${username}"
            - Enter password "${password}"
            - Click submit""";
        Files.writeString(this.loginStepsFile.toPath(), loginSteps, StandardCharsets.UTF_8);

        final String mainPlaybook = """
            _meta:
              _author: TestAuthor
            steps:
              - Open home page
              - _include: fragments/login.steps
              - Verify dashboard page
            data:
              - _testId: TC01
                username: admin
                password: secretPassword""";
        Files.writeString(this.mainPlaybookFile.toPath(), mainPlaybook, StandardCharsets.UTF_8);

        final List<Map<String, String>> dataSets = YamlFileReader.readFile(this.mainPlaybookFile);
        assertEquals(1, dataSets.size());
        final Map<String, String> row = dataSets.get(0);

        final String steps = row.get("steps");
        assertNotNull(steps);
        assertTrue(steps.contains("Open home page"));
        assertTrue(steps.contains("Enter username \"admin\""));
        assertTrue(steps.contains("Enter password \"secretPassword\""));
        assertTrue(steps.contains("Verify dashboard page"));
    }

    @Test
    @DisplayName("Verify interleaved variable loop and circular inclusion guard")
    public final void testCircularInclusionGuard() throws IOException
    {
        final File fileA = new File(this.tempDir, "A.yaml");
        final File fileB = new File(this.tempDir, "B.yaml");

        Files.writeString(fileA.toPath(), """
            steps:
              - _include: B.yaml""", StandardCharsets.UTF_8);
        Files.writeString(fileB.toPath(), """
            steps:
              - _include: A.yaml""", StandardCharsets.UTF_8);

        assertThrows(RuntimeException.class, () -> {
            YamlFileReader.readFile(fileA);
        });
    }

    @Test
    @DisplayName("Verify map-level data inclusion with local overrides")
    public final void testMapLevelDataInclusion() throws IOException
    {
        final String defaultUser = """
            username: guest_user
            role: guest
            country: US""";
        Files.writeString(this.defaultUserFile.toPath(), defaultUser, StandardCharsets.UTF_8);

        final String mainPlaybook = """
            steps:
              - Do nothing
            data:
              - _testId: GuestCase
                _include: fragments/default_user.yaml
              - _testId: AdminCase
                _include: fragments/default_user.yaml
                username: admin_user
                role: admin""";
        Files.writeString(this.mainPlaybookFile.toPath(), mainPlaybook, StandardCharsets.UTF_8);

        final List<Map<String, String>> dataSets = YamlFileReader.readFile(this.mainPlaybookFile);
        assertEquals(2, dataSets.size());

        final Map<String, String> guestRow = dataSets.get(0);
        assertEquals("GuestCase", guestRow.get("_testId"));
        assertEquals("guest_user", guestRow.get("username"));
        assertEquals("guest", guestRow.get("role"));
        assertEquals("US", guestRow.get("country"));

        final Map<String, String> adminRow = dataSets.get(1);
        assertEquals("AdminCase", adminRow.get("_testId"));
        assertEquals("admin_user", adminRow.get("username")); // Overridden
        assertEquals("admin", adminRow.get("role")); // Overridden
        assertEquals("US", adminRow.get("country")); // Inherited
    }

    @Test
    @DisplayName("Verify step location tracing and JSON array serialization")
    public final void testStepLocationTracing() throws IOException
    {
        final String loginSteps = """
            - Enter credentials
            - Submit login""";
        Files.writeString(this.loginStepsFile.toPath(), loginSteps, StandardCharsets.UTF_8);

        final String mainPlaybook = """
            steps:
              - Open home page
              - _include: fragments/login.steps
              - Verify home page
            data:
              - _testId: TC01""";
        Files.writeString(this.mainPlaybookFile.toPath(), mainPlaybook, StandardCharsets.UTF_8);

        final List<Map<String, String>> dataSets = YamlFileReader.readFile(this.mainPlaybookFile);
        assertEquals(1, dataSets.size());
        final Map<String, String> row = dataSets.get(0);

        final String stepsTraceJson = row.get("neodymium.stepLineNumbers");
        assertNotNull(stepsTraceJson);

        final List<String> traces = new Gson().fromJson(
            stepsTraceJson,
            new TypeToken<List<String>>() {}.getType()
        );
        assertEquals(4, traces.size());
        assertTrue(traces.get(0).endsWith("main.yaml:2"));
        assertTrue(traces.get(1).contains("login.steps:1 -> "));
        assertTrue(traces.get(1).endsWith("main.yaml:3"));
        assertTrue(traces.get(2).contains("login.steps:2 -> "));
        assertTrue(traces.get(2).endsWith("main.yaml:3"));
        assertTrue(traces.get(3).endsWith("main.yaml:4"));
    }

    @Test
    @DisplayName("Verify Framework Key Guard throws exception on invalid underscore key")
    public final void testFrameworkKeyGuard() throws IOException
    {
        final String mainPlaybook = """
            _meta:
              _author: TestAuthor
            _invalidKey:
              - Some value
            steps:
              - Do nothing""";
        Files.writeString(this.mainPlaybookFile.toPath(), mainPlaybook, StandardCharsets.UTF_8);

        assertThrows(MalformedPlaybookException.class, () -> {
            YamlFileReader.readFile(this.mainPlaybookFile);
        });
    }

    @Test
    @DisplayName("Verify dynamic include path parameterization from test data")
    public final void testDynamicIncludePathParameterization() throws IOException
    {
        final String extraSteps = """
            - Special step 1
            - Special step 2""";
        final File extraStepsFile = new File(this.fragmentFolder, "extra.steps");
        Files.writeString(extraStepsFile.toPath(), extraSteps, StandardCharsets.UTF_8);

        final String mainPlaybook = """
            steps:
              - _include: ${custom_path}
            data:
              - _testId: TC01
                custom_path: fragments/extra.steps""";
        Files.writeString(this.mainPlaybookFile.toPath(), mainPlaybook, StandardCharsets.UTF_8);

        final List<Map<String, String>> dataSets = YamlFileReader.readFile(this.mainPlaybookFile);
        assertEquals(1, dataSets.size());
        final String steps = dataSets.get(0).get("steps");
        assertNotNull(steps);
        assertTrue(steps.contains("Special step 1"));
        assertTrue(steps.contains("Special step 2"));
    }

    @Test
    @DisplayName("Verify inline conditional inclusion replacement")
    public final void testInlineConditionalInclusion() throws IOException
    {
        final String subSteps = """
            - Click A
            - Click B""";
        final File subStepsFile = new File(this.fragmentFolder, "sub.steps");
        Files.writeString(subStepsFile.toPath(), subSteps, StandardCharsets.UTF_8);

        final String mainPlaybook = """
            steps:
              - If visible, then _include: fragments/sub.steps, else Click C
            data:
              - _testId: TC01""";
        Files.writeString(this.mainPlaybookFile.toPath(), mainPlaybook, StandardCharsets.UTF_8);

        final List<Map<String, String>> dataSets = YamlFileReader.readFile(this.mainPlaybookFile);
        assertEquals(1, dataSets.size());
        final String steps = dataSets.get(0).get("steps");
        assertNotNull(steps);

        final String expected = "If visible, then _include: fragments/sub.steps, else Click C";
        assertEquals(expected, steps);
    }

    @Test
    @DisplayName("Verify inline conditional inclusion with else inclusion replacement")
    public final void testInlineConditionalElseInclusion() throws IOException
    {
        final String subSteps = """
            - Click A
            - Click B""";
        final File subStepsFile = new File(this.fragmentFolder, "sub.steps");
        Files.writeString(subStepsFile.toPath(), subSteps, StandardCharsets.UTF_8);

        final String elseSteps = """
            - Click C
            - Click D""";
        final File elseStepsFile = new File(this.fragmentFolder, "else.steps");
        Files.writeString(elseStepsFile.toPath(), elseSteps, StandardCharsets.UTF_8);

        final String mainPlaybook = """
            steps:
              - If visible, then _include: fragments/sub.steps, else _include: fragments/else.steps
            data:
              - _testId: TC01""";
        Files.writeString(this.mainPlaybookFile.toPath(), mainPlaybook, StandardCharsets.UTF_8);

        final List<Map<String, String>> dataSets = YamlFileReader.readFile(this.mainPlaybookFile);
        assertEquals(1, dataSets.size());
        final String steps = dataSets.get(0).get("steps");
        assertNotNull(steps);

        final String expected = "If visible, then _include: fragments/sub.steps, else _include: fragments/else.steps";
        assertEquals(expected, steps);
    }

    @Test
    @DisplayName("Verify loadInclude resolves correctly from file and classpath")
    public final void testLoadIncludeResolution() throws IOException
    {
        final String extraSteps = """
            - Click some button
            - Click other button""";
        final File extraStepsFile = new File(this.fragmentFolder, "extra.steps");
        Files.writeString(extraStepsFile.toPath(), extraSteps, StandardCharsets.UTF_8);

        // 1. File system resolution
        Neodymium.getData().put("neodymium.sourceFile", this.mainPlaybookFile.getAbsolutePath());
        final List<YamlFileReader.Step> steps1 = YamlFileReader.loadInclude("fragments/extra.steps");
        assertEquals(2, steps1.size());
        assertEquals("Click some button", steps1.get(0).text);
        assertEquals("Click other button", steps1.get(1).text);

        // 2. Classpath resolution
        Neodymium.getData().put("neodymium.sourceFile", "another-dummy.yaml");
        final List<YamlFileReader.Step> steps2 = YamlFileReader.loadInclude("dummy-test.yml");
        assertEquals(1, steps2.size());
        assertEquals("Click button 2", steps2.get(0).text);

        // 3. Fallback classpath extraction from filesystem path containing target/test-classes/
        Neodymium.getData().put("neodymium.sourceFile", "/some/path/target/test-classes/dummy-test.yml");
        Neodymium.getData().remove("neodymium.classpathResourcePath");
        final List<YamlFileReader.Step> steps3 = YamlFileReader.loadInclude("dummy-test.yml");
        assertEquals(1, steps3.size());
        assertEquals("Click button 2", steps3.get(0).text);
    }

    @Test
    @DisplayName("Verify nested static includes and variable propagation")
    public final void testNestedStaticIncludes() throws IOException
    {
        final File level2File = new File(this.fragmentFolder, "level2.steps");
        final String level2Content = "- Inner step with ${username}";
        Files.writeString(level2File.toPath(), level2Content, StandardCharsets.UTF_8);

        final File level1File = new File(this.fragmentFolder, "level1.steps");
        final String level1Content = """
            - Mid step 1
            - _include: fragments/level2.steps
            - Mid step 2""";
        Files.writeString(level1File.toPath(), level1Content, StandardCharsets.UTF_8);

        final String mainPlaybook = """
            steps:
              - Top step
              - _include: fragments/level1.steps
            data:
              - _testId: TC01
                username: nested_user""";
        Files.writeString(this.mainPlaybookFile.toPath(), mainPlaybook, StandardCharsets.UTF_8);

        final List<Map<String, String>> dataSets = YamlFileReader.readFile(this.mainPlaybookFile);
        assertEquals(1, dataSets.size());
        final String steps = dataSets.get(0).get("steps");
        assertNotNull(steps);
        assertTrue(steps.contains("Top step"));
        assertTrue(steps.contains("Mid step 1"));
        assertTrue(steps.contains("Inner step with nested_user"));
        assertTrue(steps.contains("Mid step 2"));
    }

    @Test
    @DisplayName("Verify classpath circular inclusion detection throws MalformedPlaybookException")
    public final void testClasspathCircularInclusionGuard()
    {
        Neodymium.getData().put("neodymium.sourceFile", "another-dummy.yaml");
        // circular-a.steps includes circular-b.steps which includes circular-a.steps in test resources
        final MalformedPlaybookException exception = assertThrows(MalformedPlaybookException.class, () -> {
            YamlFileReader.loadInclude("circular-a.steps");
        });
        assertTrue(exception.getMessage().contains("Circular inclusion detected"));
    }

    @Test
    @DisplayName("Verify resolving non-existent include path throws RuntimeException")
    public final void testFileNotFound()
    {
        Neodymium.getData().put("neodymium.sourceFile", this.mainPlaybookFile.getAbsolutePath());
        final RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            YamlFileReader.loadInclude("fragments/non-existent.steps");
        });
        assertTrue(exception.getMessage().contains("Could not resolve include path"));
    }

    @Test
    @DisplayName("Verify circular inclusion detection works with differing relative paths pointing to the same file")
    public final void testCircularInclusionDifferentRelativePaths() throws IOException
    {
        final File fragmentsDir = new File(this.tempDir, "fragments");
        fragmentsDir.mkdirs();

        final File fileA = new File(this.tempDir, "A.yaml");
        final File fileB = new File(fragmentsDir, "B.steps");
        final File fileC = new File(fragmentsDir, "C.steps");

        Files.writeString(fileA.toPath(), """
            steps:
              - _include: fragments/B.steps""", StandardCharsets.UTF_8);
        // B includes C using absolute path fragments/C.steps to succeed load
        Files.writeString(fileB.toPath(), "- _include: fragments/C.steps", StandardCharsets.UTF_8);
        // C includes B (using redundant relative path fragments/./B.steps to bypass loop check)
        Files.writeString(fileC.toPath(), "- _include: fragments/./B.steps", StandardCharsets.UTF_8);

        final RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            YamlFileReader.readFile(fileA);
        });
        assertEquals("Circular inclusion detected: A.yaml -> B.steps -> C.steps -> B.steps", ex.getMessage());
    }
}

