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
package org.neodymium.license;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Architectural safety net enforcing compliance across all license types in Neodymium:
 * <ul>
 *   <li>Dual Licensing (GNU AGPLv3 + Commercial) for AI modules ({@code neodymium-core}, {@code aura-manager}, {@code neodymium-test-server})</li>
 *   <li>Apache 2.0 with Section 4(b) modification notices ({@code JdkHttpClient.java})</li>
 *   <li>BSD 2-Clause (HTMX) and SIL Open Font License 1.1 third-party assets</li>
 *   <li>Root multi-license governance ({@code LICENSE} and {@code NOTICE})</li>
 * </ul>
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class LicenseComplianceTest
{
    private static final String EXPECTED_DUAL_LICENSE_PREFIX = "/*\n * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial";
    private static final String EXPECTED_COMMERCIAL_NOTICE = "Alternatively, this program may be used under the terms of a commercial";
    private static final String EXPECTED_AGPL_NOTICE = "GNU Affero General Public License";

    @Test
    @DisplayName("Assert that all Java source and test files in neodymium-core carry canonical dual-license header")
    public void testCoreJavaFilesHaveDualLicenseHeader()
    {
        assertDualLicenseHeadersInDirectory(findRepoRoot().resolve("neodymium-core"), "neodymium-core");
    }

    @Test
    @DisplayName("Assert that neodymium-core pom.xml explicitly declares both AGPLv3 and Commercial licenses")
    public void testCorePomDeclaresDualLicense() throws IOException
    {
        assertPomDeclaresDualLicense(findRepoRoot().resolve("neodymium-core/pom.xml"), "neodymium-core");
    }

    @Test
    @DisplayName("Assert that JdkHttpClient preserves Apache 2.0 Section 4(b) modification notice")
    public void testForkedSeleniumHttpClientPreservesApacheNotice() throws IOException
    {
        final Path jdkHttpClient = findRepoRoot().resolve("neodymium-core/src/main/java/org/openqa/selenium/remote/http/jdk/JdkHttpClient.java");
        Assertions.assertTrue(Files.exists(jdkHttpClient), "JdkHttpClient.java must exist at: " + jdkHttpClient);

        final String content = Files.readString(jdkHttpClient, StandardCharsets.UTF_8);
        Assertions.assertTrue(
            content.contains("Apache License, Version 2.0"),
            "JdkHttpClient.java must retain Apache 2.0 notice"
        );
        Assertions.assertTrue(
            content.contains("Section 4(b)") || content.contains("MODIFICATIONS NOTICE") || content.contains("prominent notices stating that You changed the files"),
            "JdkHttpClient.java must contain Section 4(b) modification notice"
        );
    }

    @Test
    @DisplayName("Assert that aura-manager declares dual-license in POM and all Java files")
    public void testAuraManagerDeclaresDualLicenseAndCanonicalHeaders() throws IOException
    {
        assertPomDeclaresDualLicense(findRepoRoot().resolve("aura-manager/pom.xml"), "aura-manager");
        assertDualLicenseHeadersInDirectory(findRepoRoot().resolve("aura-manager"), "aura-manager");
    }

    @Test
    @DisplayName("Assert that neodymium-test-server declares dual-license in POM and all Java files")
    public void testNeodymiumTestServerDeclaresDualLicenseAndCanonicalHeaders() throws IOException
    {
        assertPomDeclaresDualLicense(findRepoRoot().resolve("neodymium-test-server/pom.xml"), "neodymium-test-server");
        assertDualLicenseHeadersInDirectory(findRepoRoot().resolve("neodymium-test-server"), "neodymium-test-server");
    }

    @Test
    @DisplayName("Assert that root LICENSE covers all three licensing tiers: MIT, AGPLv3, and Commercial")
    public void testRootLicenseCoversAllThreeLicenseTiers() throws IOException
    {
        final Path licenseFile = findRepoRoot().resolve("LICENSE");
        Assertions.assertTrue(Files.exists(licenseFile), "Root LICENSE file must exist");

        final String content = Files.readString(licenseFile, StandardCharsets.UTF_8);
        Assertions.assertTrue(
            content.contains("SECTION A: MIT LICENSE (Neodymium Classic)"),
            "Root LICENSE must contain Section A for Neodymium Classic (MIT)"
        );
        Assertions.assertTrue(
            content.contains("SECTION B: GNU AFFERO GENERAL PUBLIC LICENSE (Neodymium AI)"),
            "Root LICENSE must contain Section B for Neodymium AI (AGPLv3)"
        );
        Assertions.assertTrue(
            content.contains("SECTION C: COMMERCIAL LICENSE (Neodymium AI)"),
            "Root LICENSE must contain Section C for Neodymium AI (Commercial)"
        );
    }

    @Test
    @DisplayName("Assert that root NOTICE documents all third-party licenses and code provenance")
    public void testRootNoticeDocumentsAllThirdPartyLicenses() throws IOException
    {
        final Path noticeFile = findRepoRoot().resolve("NOTICE");
        Assertions.assertTrue(Files.exists(noticeFile), "Root NOTICE file must exist");

        final String content = Files.readString(noticeFile, StandardCharsets.UTF_8);
        Assertions.assertTrue(
            content.contains("Selenium / Software Freedom Conservancy") && content.contains("Apache License, Version 2.0"),
            "NOTICE must document Selenium Apache 2.0 attribution and Section 4(b) compliance"
        );
        Assertions.assertTrue(
            content.contains("HTMX") && content.contains("BSD 2-Clause License"),
            "NOTICE must document HTMX BSD 2-Clause attribution"
        );
        Assertions.assertTrue(
            content.contains("Google Material Symbols") && content.contains("Apache License, Version 2.0"),
            "NOTICE must document Google Material Symbols Apache 2.0 attribution"
        );
        Assertions.assertTrue(
            content.contains("SIL Open Font License"),
            "NOTICE must document SIL Open Font License attribution"
        );
        Assertions.assertTrue(
            content.contains("Neodymium Classic") && content.contains("MIT License"),
            "NOTICE must document Neodymium Classic MIT license"
        );
    }

    @Test
    @DisplayName("Assert that bundled third-party static assets preserve their upstream license notices")
    public void testThirdPartyStaticAssetsPreserveLicenses() throws IOException
    {
        final Path testServerRoot = findRepoRoot().resolve("neodymium-test-server");

        final Path htmxFile = testServerRoot.resolve("src/main/resources/ai-test-pages/shared/htmx.js");
        Assertions.assertTrue(Files.exists(htmxFile), "htmx.js must exist at: " + htmxFile);
        final String htmxContent = Files.readString(htmxFile, StandardCharsets.UTF_8);
        Assertions.assertTrue(
            htmxContent.contains("BSD 2-Clause License") && htmxContent.contains("Big Sky Software"),
            "htmx.js must preserve BSD 2-Clause copyright and license header"
        );

        final Path oflFile = testServerRoot.resolve("src/main/resources/ai-test-pages/shared/fonts/OFL.txt");
        Assertions.assertTrue(Files.exists(oflFile), "OFL.txt must exist at: " + oflFile);
        final String oflContent = Files.readString(oflFile, StandardCharsets.UTF_8);
        Assertions.assertTrue(
            oflContent.contains("SIL OPEN FONT LICENSE (OFL-1.1)"),
            "OFL.txt must specify SIL Open Font License (OFL-1.1)"
        );
    }

    private static Path findRepoRoot()
    {
        final Path cwd = Path.of(".").toAbsolutePath().normalize();
        if (Files.exists(cwd.resolve("LICENSE")) && Files.exists(cwd.resolve("NOTICE")))
        {
            return cwd;
        }
        final Path parent = cwd.getParent();
        if (parent != null && Files.exists(parent.resolve("LICENSE")) && Files.exists(parent.resolve("NOTICE")))
        {
            return parent;
        }
        throw new IllegalStateException("Unable to locate Neodymium repository root from " + cwd);
    }

    private static void assertDualLicenseHeadersInDirectory(final Path rootDir, final String moduleName)
    {
        final Path mainJava = rootDir.resolve("src/main/java");
        final Path testJava = rootDir.resolve("src/test/java");

        final List<String> missingDualLicenseHeader = new ArrayList<>();
        final List<String> missingCommercialClause = new ArrayList<>();
        final List<String> missingAgplClause = new ArrayList<>();

        for (final Path srcDir : List.of(mainJava, testJava))
        {
            if (!Files.isDirectory(srcDir))
            {
                continue;
            }
            try (final Stream<Path> paths = Files.walk(srcDir))
            {
                paths.filter(p -> p.toString().endsWith(".java")).forEach(javaFile ->
                {
                    final String fileName = javaFile.getFileName().toString();
                    if ("JdkHttpClient.java".equals(fileName))
                    {
                        // Forked Selenium HTTP client under Apache 2.0 with Section 4(b) modification notice
                        return;
                    }

                    try
                    {
                        final String content = Files.readString(javaFile, StandardCharsets.UTF_8);
                        final String normalized = content.replace("\r\n", "\n").stripLeading();

                        if (!normalized.startsWith(EXPECTED_DUAL_LICENSE_PREFIX))
                        {
                            missingDualLicenseHeader.add(javaFile.toString());
                        }

                        final int headerEnd = normalized.indexOf("*/");
                        final String header = (headerEnd > 0) ? normalized.substring(0, headerEnd) : "";

                        if (!header.contains(EXPECTED_COMMERCIAL_NOTICE))
                        {
                            missingCommercialClause.add(javaFile.toString());
                        }

                        if (!header.contains(EXPECTED_AGPL_NOTICE))
                        {
                            missingAgplClause.add(javaFile.toString());
                        }
                    }
                    catch (final IOException e)
                    {
                        throw new RuntimeException("Failed reading " + javaFile, e);
                    }
                });
            }
            catch (final IOException e)
            {
                throw new RuntimeException("Failed walking directory " + srcDir, e);
            }
        }

        Assertions.assertTrue(
            missingDualLicenseHeader.isEmpty(),
            "Found Java files in " + moduleName + " missing canonical dual-license header:\n" + String.join("\n", missingDualLicenseHeader)
        );

        Assertions.assertTrue(
            missingCommercialClause.isEmpty(),
            "Found Java files in " + moduleName + " missing commercial license clause:\n" + String.join("\n", missingCommercialClause)
        );

        Assertions.assertTrue(
            missingAgplClause.isEmpty(),
            "Found Java files in " + moduleName + " missing AGPLv3 clause:\n" + String.join("\n", missingAgplClause)
        );
    }

    private static void assertPomDeclaresDualLicense(final Path pomPath, final String moduleName) throws IOException
    {
        Assertions.assertTrue(Files.exists(pomPath), moduleName + " pom.xml must exist at: " + pomPath);

        final String pomContent = Files.readString(pomPath, StandardCharsets.UTF_8);
        Assertions.assertTrue(
            pomContent.contains("GNU Affero General Public License, Version 3"),
            moduleName + " pom.xml must declare GNU Affero General Public License, Version 3"
        );
        Assertions.assertTrue(
            pomContent.contains("Xceptance Commercial License"),
            moduleName + " pom.xml must declare Xceptance Commercial License"
        );
    }
}
