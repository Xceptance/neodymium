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
package org.neodymium.ai.integration.verla;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.ai.util.EmbeddedHtmlServer;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Integration demo demonstrating 5 distinct programmatic prompt execution and debugging
 * approaches without using external YAML playbook files.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("integration")
@Tag("verla")
@Tag("AuraIntegration")
@NeodymiumAiTest
public final class VerlaProgrammaticDemoTest extends BaseAiTest
{
    /**
     * Constructs a default VerlaProgrammaticDemoTest.
     */
    public VerlaProgrammaticDemoTest()
    {
    }

    /**
     * Set up dynamic test URL environment variables before each test method execution.
     */
    @BeforeEach
    public void setup()
    {
        EmbeddedHtmlServer.resetInventory();
        Neodymium.getData().put("verla.url", String.format("http://localhost:%d", server.getPort()));
    }

    /**
     * Concept 1: Fully Programmatic Java Object API using {@link PlaybookStep} and {@link Playbook}.
     *
     * @param session the thread-isolated AiSession injected by JUnit 5 runner
     */
    @org.junit.jupiter.api.Test
    public void test1_FullyProgrammaticObjects(final AiSession session) throws Exception
    {
        final String targetUrl = String.format("http://localhost:%d/verla-perfect/index.html", server.getPort());

        final Playbook playbook = Playbook.builder()
            .step("Open " + targetUrl + " in the browser")
            .step("Locate the search input field and type 'Minimalist' into it")
            .step("Press enter to submit search")
            .build();

        session.execute(playbook);
    }

    /**
     * Concept 2: Programmatic Text Block string execution via {@link AiSession#execute(String)}
     * directly without inheriting or using {@link BaseAiTest#runPlaybook(AiSession, String)}.
     *
     * @param session the thread-isolated AiSession injected by JUnit 5 runner
     */
    @org.junit.jupiter.api.Test
    public void test2_ProgrammaticTextBlockWithoutBaseAiTest(final AiSession session) throws Exception
    {
        final String targetUrl = String.format("http://localhost:%d/verla-perfect/index.html", server.getPort());

        session.execute("""
            Open %s in the browser
            Locate the search input field and type 'Minimalist' into it
            Press enter to submit search
            """.formatted(targetUrl));
    }

    /**
     * Concept 3: Annotation-driven inline playbook using Java text blocks with {@code @AiPlaybook}.
     *
     * @param session the thread-isolated AiSession injected by JUnit 5 runner
     */
    @AiPlaybook("""
        inline:steps: |
          Open ${verla.url}/verla-perfect/index.html in the browser
          Locate the search input field and type 'Minimalist' into it
          Press enter to submit search
        """)
    public void test3_AnnotationDrivenInlinePlaybookTextBlocks(final AiSession session)
    {
        // Executed automatically by NeodymiumAiRunner using the inline text block annotation above
    }

    /**
     * Concept 4: Pure Step-by-Step Java Debugging without Selenide calls.
     * Allows setting breakpoints on individual Java statements to step through prompt execution.
     *
     * @param session the thread-isolated AiSession injected by JUnit 5 runner
     */
    @org.junit.jupiter.api.Test
    public void test4_StepByStepJavaDebugging(final AiSession session) throws Exception
    {
        final String targetUrl = String.format("http://localhost:%d/verla-perfect/index.html", server.getPort());

        // Set breakpoint on step 1 to debug browser launch
        session.execute("Open " + targetUrl + " in the browser");

        // Set breakpoint on step 2 to inspect DOM state before search input
        session.execute("Locate the search input field and type 'Minimalist' into it");

        // Set breakpoint on step 3 to verify results update
        session.execute("Press enter to submit search");
    }

    /**
     * Concept 5: Mixing direct Java Selenide commands with AI prompt step executions.
     *
     * @param session the thread-isolated AiSession injected by JUnit 5 runner
     */
    @org.junit.jupiter.api.Test
    public void test5_MixStepsAndSelenideCommands(final AiSession session) throws Exception
    {
        final String targetUrl = String.format("http://localhost:%d/verla-perfect/index.html", server.getPort());

        // 1. Direct Selenide navigation
        open(targetUrl);

        // 2. Direct Selenide explicit wait / check
        $("#search-input").shouldBe(visible);

        // 3. AI prompt execution for form action
        session.execute("Locate the search input field and type 'Minimalist' into it and press enter");
    }
}
