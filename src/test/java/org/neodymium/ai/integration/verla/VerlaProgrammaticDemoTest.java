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

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.junit.AiInlinePlaybook;
import org.neodymium.ai.junit.AiLlmCache;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.util.EmbeddedHtmlServer;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Integration demo demonstrating 8 distinct programmatic prompt execution, annotation-driven,
 * and debugging approaches without extending {@code BaseAiTest}.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@Tag("integration")
@Tag("verla")
@AiLlmCache
@NeodymiumAiTest
public final class VerlaProgrammaticDemoTest
{
    private static EmbeddedHtmlServer server;

    /**
     * Starts the embedded Verla server before tests run and configures lightweight execution.
     *
     * @throws IOException if the server fails to start
     */
    @BeforeAll
    public static void startServer() throws IOException
    {
        System.setProperty("neodymium.ai.pesap.enabled", "false");
        System.setProperty("neodymium.ai.judge.enabled", "false");
        System.setProperty("neodymium.ai.action.embeddedJudging.enabled", "false");
        System.setProperty("neodymium.ai.semanticVerification.enabled", "false");
        System.setProperty("neodymium.ai.visualRca.enabled", "false");
        AiConfiguration.resetInstance();

        server = new EmbeddedHtmlServer();
        server.start();
    }

    /**
     * Stops the embedded Verla server after all tests complete.
     */
    @AfterAll
    public static void stopServer()
    {
        server.stop();
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
     */
    @Test
    @AiLlmCache
    public void test1_FullyProgrammaticObjects() throws Exception
    {
        Neodymium.getData().put("searchTerm", "Minimalist");

        final Playbook playbook = Playbook.builder()
            .step("Open ${verla.url}/verla-perfect/index.html in the browser")
            .step("Locate the search input field and type '${searchTerm}' into it")
            .step("Press enter to submit search")
            .build();

        try (final AiSession session = AiSession.selenide(ExecutionMode.FORCE_RECORDING))
        {
            session.execute(playbook);
        }
    }

    /**
     * Concept 2a: Programmatic Text Block string execution with embedded YAML steps and data sections.
     */
    @Test
    @AiLlmCache
    public void test2a_ProgrammaticTextBlockWithEmbeddedYamlData() throws Exception
    {
        try (final AiSession session = AiSession.selenide(ExecutionMode.FORCE_RECORDING))
        {
            session.execute("""
                steps: |
                  Open ${verla.url}/verla-perfect/index.html in the browser
                  Locate the search input field and type '${searchTerm}' into it
                  Press enter to submit search

                data:
                  - testId: "default"
                    searchTerm: "Minimalist"
                """);
        }
    }

    /**
     * Concept 2b: Programmatic Text Block string execution seeded with SessionData container.
     */
    @Test
    @AiLlmCache
    public void test2b_ProgrammaticTextBlockWithSessionData() throws Exception
    {
        final SessionData sessionData = new SessionData();
        sessionData.set("searchTerm", "Minimalist");

        try (final AiSession session = AiSession.selenide(ExecutionMode.FORCE_RECORDING))
        {
            session.execute("""
                Open ${verla.url}/verla-perfect/index.html in the browser
                Locate the search input field and type '${searchTerm}' into it
                Press enter to submit search
                """, sessionData);
        }
    }

    /**
     * Concept 3: Annotation-driven inline playbook using Java text blocks with {@link AiInlinePlaybook}.
     *
     * @param session the thread-isolated AiSession injected by NeodymiumAiRunner
     */
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiLlmCache
    @AiInlinePlaybook("""
        steps: |
          Open ${verla.url}/verla-perfect/index.html in the browser
          Locate the search input field and type '${searchTerm}' into it
          Press enter to submit search
        data:
          - testId: "default"
            searchTerm: "Minimalist"
        """)
    public void test3_AnnotationDrivenInlinePlaybookTextBlocks(final AiSession session)
    {
        // Executed automatically by NeodymiumAiRunner using the inline text block annotation above
    }

    /**
     * Concept 4: Pure Step-by-Step Java Debugging without Selenide calls.
     * Allows setting breakpoints on individual Java statements to step through prompt execution.
     */
    @Test
    @AiLlmCache
    public void test4_StepByStepJavaDebugging() throws Exception
    {
        Neodymium.getData().put("searchTerm", "Minimalist");

        try (final AiSession session = AiSession.selenide(ExecutionMode.FORCE_RECORDING))
        {
            session.execute("Open ${verla.url}/verla-perfect/index.html in the browser");
            session.execute("Locate the search input field and type '${searchTerm}' into it");
            session.execute("Press enter to submit search");
        }
    }

    /**
     * Concept 5: Mixing direct Java Selenide commands with AI prompt step executions.
     */
    @Test
    @AiLlmCache
    public void test5_MixStepsAndSelenideCommands() throws Exception
    {
        Neodymium.getData().put("searchTerm", "Minimalist");

        open(Neodymium.getData().get("verla.url") + "/verla-perfect/index.html");
        $("#search-input").shouldBe(visible);

        try (final AiSession session = AiSession.selenide(ExecutionMode.FORCE_RECORDING))
        {
            session.execute("Locate the search input field and type '${searchTerm}' into it");
        }

        $("#search-input").pressEnter();
    }

    /**
     * Concept 6: Annotation-driven external YAML playbook with explicit path via {@code @AiPlaybook}.
     *
     * @param session the thread-isolated AiSession injected by NeodymiumAiRunner
     */
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiLlmCache
    @AiPlaybook("/playbooks/integration/verla-search-demo.yaml")
    public void test6_AnnotationDrivenExternalPlaybookExplicit(final AiSession session)
    {
        // Executed automatically by NeodymiumAiRunner using the explicit external YAML playbook above
    }

    /**
     * Concept 7: Annotation-driven external YAML playbook by naming convention via {@code @AiPlaybook}.
     *
     * @param session the thread-isolated AiSession injected by NeodymiumAiRunner
     */
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiLlmCache
    @AiPlaybook
    public void test7_AnnotationDrivenExternalPlaybookConvention(final AiSession session)
    {
        // Executed automatically by NeodymiumAiRunner using the convention-mapped YAML playbook above
    }
}
