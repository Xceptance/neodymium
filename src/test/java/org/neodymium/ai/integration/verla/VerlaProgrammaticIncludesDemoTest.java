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
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiInlinePlaybook;
import org.neodymium.ai.junit.AiLlmCache;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.util.EmbeddedHtmlServer;
import com.xceptance.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Integration demo demonstrating modular playbook includes ({@code _include: ...}) combined with normal natural language
 * prompt steps across 6 distinct programmatic execution, annotation-driven, and debugging approaches without extending {@code BaseAiTest}.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@Tag("integration")
@Tag("verla")
@AiLlmCache
@NeodymiumAiTest
public final class VerlaProgrammaticIncludesDemoTest
{
    private static EmbeddedHtmlServer server;

    /**
     * Constructs a default VerlaProgrammaticIncludesDemoTest.
     */
    public VerlaProgrammaticIncludesDemoTest()
    {
    }

    /**
     * Starts the embedded Verla server before tests run and configures lightweight execution.
     *
     * @throws IOException if the server fails to start
     */
    @BeforeAll
    public static void startServer() throws IOException
    {
        System.setProperty("neodymium.ai.pesap.enabled", "false");
        Neodymium.getData().put("neodymium.ai.judge.enabled", "false");
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
        Neodymium.getData().put("verla.url", String.format("https://localhost:%d", server.getHttpsPort()));
    }

    /**
     * Concept 1: Fully Programmatic Java Object API using {@link Playbook.Builder#include(String)} combining an included sub-playbook step with normal prompt steps.
     *
     * @throws Exception if execution fails
     */
    @Test
    @AiLlmCache
    public void test1_FullyProgrammaticObjectsWithIncludes() throws Exception
    {
        Neodymium.getData().put("searchTerm", "Minimalist");

        final Playbook playbook = Playbook.builder()
            .include("playbooks/integration/includes/verla_open_homepage.yaml")
            .step("Locate the search input field and type '${searchTerm}' into it")
            .step("Press enter to submit search")
            .build();

        try (final AiSession session = AiSession.selenide(ExecutionMode.FORCE_RECORDING))
        {
            session.execute(playbook);
        }
    }

    /**
     * Concept 2a: Programmatic Text Block string execution combining an {@code _include:} step with normal YAML prompt steps and data sections.
     *
     * @throws Exception if execution fails
     */
    @Test
    @AiLlmCache
    public void test2a_ProgrammaticTextBlockWithIncludesAndEmbeddedYamlData() throws Exception
    {
        try (final AiSession session = AiSession.selenide(ExecutionMode.FORCE_RECORDING))
        {
            session.execute("""
                steps: |
                  _include: playbooks/integration/includes/verla_open_homepage.yaml
                  Locate the search input field and type '${searchTerm}' into it
                  Press enter to submit search

                data:
                  - testId: "default"
                    searchTerm: "Minimalist"
                """);
        }
    }

    /**
     * Concept 2b: Programmatic Text Block string execution combining an {@code _include:} step with normal prompt steps seeded with SessionData container.
     *
     * @throws Exception if execution fails
     */
    @Test
    @AiLlmCache
    public void test2b_ProgrammaticTextBlockWithIncludesAndSessionData() throws Exception
    {
        final SessionData sessionData = new SessionData();
        sessionData.set("searchTerm", "Minimalist");

        try (final AiSession session = AiSession.selenide(ExecutionMode.FORCE_RECORDING))
        {
            session.execute("""
                steps: |
                  _include: playbooks/integration/includes/verla_open_homepage.yaml
                  Locate the search input field and type '${searchTerm}' into it
                  Press enter to submit search
                """, sessionData);
        }
    }

    /**
     * Concept 3: Annotation-driven inline playbook combining an {@code _include:} step with normal YAML prompt steps via {@link AiInlinePlaybook}.
     *
     * @param session the thread-isolated AiSession injected by NeodymiumAiRunner
     */
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiLlmCache
    @AiInlinePlaybook("""
        steps: |
          _include: playbooks/integration/includes/verla_open_homepage.yaml
          Locate the search input field and type '${searchTerm}' into it
          Press enter to submit search
        data:
          - testId: "default"
            searchTerm: "Minimalist"
        """)
    public void test3_AnnotationDrivenInlinePlaybookWithIncludes(final AiSession session)
    {
        // Executed automatically by NeodymiumAiRunner using the inline text block annotation above
    }

    /**
     * Concept 4: Pure Step-by-Step Java Debugging combining an {@code _include:} sub-playbook step with individual normal step executions.
     * Allows setting breakpoints on individual Java statements to step through prompt executions.
     *
     * @throws Exception if execution fails
     */
    @Test
    @AiLlmCache
    public void test4_StepByStepJavaDebuggingWithIncludes() throws Exception
    {
        Neodymium.getData().put("searchTerm", "Minimalist");

        try (final AiSession session = AiSession.selenide(ExecutionMode.FORCE_RECORDING))
        {
            session.execute("_include: playbooks/integration/includes/verla_open_homepage.yaml");
            session.execute("Locate the search input field and type '${searchTerm}' into it");
            session.execute("Press enter to submit search");
        }
    }

    /**
     * Concept 5: Mixing direct Java Selenide commands with an {@code _include:} sub-playbook step execution.
     *
     * @throws Exception if execution fails
     */
    @Test
    @AiLlmCache
    public void test5_MixStepsAndSelenideCommandsWithIncludes() throws Exception
    {
        Neodymium.getData().put("searchTerm", "Minimalist");

        open(Neodymium.getData().get("verla.url") + "/verla-perfect/index.html");
        $("#search-input").shouldBe(visible);

        try (final AiSession session = AiSession.selenide(ExecutionMode.FORCE_RECORDING))
        {
            session.execute("_include: playbooks/integration/includes/verla_search_product.yaml");
        }

        $("#search-input").pressEnter();
    }

    /**
     * Concept 6: Annotation-driven external master YAML playbook composing an {@code _include:} step with normal prompt steps via {@link AiPlaybook}.
     *
     * @param session the thread-isolated AiSession injected by NeodymiumAiRunner
     */
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiLlmCache
    @AiPlaybook("/playbooks/integration/verla-search-includes-demo.yaml")
    public void test6_AnnotationDrivenExternalMasterPlaybookWithIncludes(final AiSession session)
    {
        // Executed automatically by NeodymiumAiRunner using the explicit external master YAML playbook above
    }
}
