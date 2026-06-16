/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package com.xceptance.neodymium.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.testing.AiMockResponse;
import com.xceptance.neodymium.ai.testing.MockActionExecutor;
import com.xceptance.neodymium.ai.testing.MockLlmClient;
import com.xceptance.neodymium.ai.testing.MockPageAnalyzer;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Verifies that the condition in conditional includes is evaluated exactly once
 * and the sub-steps in the target include file are executed as separate,
 * debuggable steps.
 */
public final class AiAgentConditionalIncludesTest
{
    @TempDir
    File tempDir;

    private Playbook originalPlaybook;
    private String originalApiKey;

    @BeforeEach
    public final void setUp()
    {
        Neodymium.clearThreadContext();
        System.setProperty("neodymium.ai.apiKey", "mock-api-key");
        System.setProperty("neodymium.ai.pesap.enabled", "false");

        originalPlaybook = Neodymium.getAiPlaybook();
        originalApiKey = Neodymium.aiConfiguration().aiApiKey();
    }

    @AfterEach
    public final void tearDown()
    {
        Neodymium.setAiPlaybook(originalPlaybook);
        if (originalApiKey != null)
        {
            System.setProperty("neodymium.ai.apiKey", originalApiKey);
        }
        else
        {
            System.clearProperty("neodymium.ai.apiKey");
        }
        System.clearProperty("neodymium.ai.pesap.enabled");
        Neodymium.clearThreadContext();
    }

    @Test
    public final void testConditionalIncludeThenBranch() throws Exception
    {
        // 1. Setup a fresh Playbook in recording mode
        final Playbook playbook = new Playbook("test-conditional-include-then");
        playbook.setRecording(true);
        Neodymium.setAiPlaybook(playbook);

        // 2. Setup mock source file path so relative includes work
        final File mainFile = new File(tempDir, "main.yaml");
        Files.writeString(mainFile.toPath(), "steps:\n  - dummy\n", StandardCharsets.UTF_8);
        Neodymium.getData().put("neodymium.sourceFile", mainFile.getAbsolutePath());

        // Create the include folder and step files
        final File fragmentsFolder = new File(tempDir, "fragments");
        fragmentsFolder.mkdirs();

        final File subStepsFile = new File(fragmentsFolder, "sub.steps");
        Files.writeString(subStepsFile.toPath(), "- Click button A\n- Click button B\n", StandardCharsets.UTF_8);

        final File elseStepsFile = new File(fragmentsFolder, "else.steps");
        Files.writeString(elseStepsFile.toPath(), "- Click button C\n- Click button D\n", StandardCharsets.UTF_8);

        // 3. Setup mock LLM responses:
        final MockLlmClient mockLlm = new MockLlmClient();

        // Turn 1: Main conditional include step -> returns BRANCH
        mockLlm.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Evaluating condition and selecting branch.",
                      "a": [
                        {
                          "t": "BRANCH",
                          "c": [
                            {
                              "t": "ASSERT",
                              "tg": "#element",
                              "desc": "Check element is visible"
                            }
                          ],
                          "th": [
                            {
                              "t": "INCLUDE",
                              "tg": "fragments/sub.steps",
                              "desc": "Include sub steps"
                            }
                          ],
                          "el": [
                            {
                              "t": "INCLUDE",
                              "tg": "fragments/else.steps",
                              "desc": "Include else steps"
                            }
                          ]
                        }
                      ]
                    }
                    """)
                .build());

        // Turn 2: LLM response for include step 1 "Click button A"
        mockLlm.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Clicking A.",
                      "a": [
                        {
                          "t": "CLICK",
                          "tg": "#btnA",
                          "desc": "Click button A"
                        }
                      ]
                    }
                    """)
                .build());

        // Turn 3: LLM response for include step 2 "Click button B"
        mockLlm.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Clicking B.",
                      "a": [
                        {
                          "t": "CLICK",
                          "tg": "#btnB",
                          "desc": "Click button B"
                        }
                      ]
                    }
                    """)
                .build());

        final MockPageAnalyzer mockAnalyzer = new MockPageAnalyzer("<html><body>Mock DOM Context</body></html>");
        final MockActionExecutor mockExecutor = new MockActionExecutor();

        final AiAgent agent = new AiAgent(mockLlm, mockAnalyzer, mockExecutor, Neodymium.aiConfiguration());

        final List<String> instructions = List.of(
            "If visible, then _include: fragments/sub.steps else _include: fragments/else.steps"
        );

        final AiExecutionResult result = new AiExecutionResult(Neodymium.getData());
        agent.execute(String.join("\n", instructions), result);

        // Verify steps executed (1 main step + 2 included steps = 3 steps)
        assertEquals(3, result.getSteps().size());
        assertEquals("If visible, then _include: fragments/sub.steps else _include: fragments/else.steps", result.getSteps().get(0).getExpandedInstruction());
        assertEquals("Click button A", result.getSteps().get(1).getExpandedInstruction());
        assertEquals("Click button B", result.getSteps().get(2).getExpandedInstruction());

        // Verify executed actions:
        // 1. BRANCH action container
        // 2. ASSERT action (condition check)
        // 3. INCLUDE action
        // 4. CLICK #btnA
        // 5. CLICK #btnB
        final List<Action> executed = mockExecutor.getExecutedActions();
        assertEquals(5, executed.size());
        assertEquals("BRANCH", executed.get(0).getType());
        assertEquals("ASSERT", executed.get(1).getType());
        assertEquals("#element", executed.get(1).getTarget());
        assertEquals("INCLUDE", executed.get(2).getType());
        assertEquals("fragments/sub.steps", executed.get(2).getTarget());
        assertEquals("CLICK", executed.get(3).getType());
        assertEquals("#btnA", executed.get(3).getTarget());
        assertEquals("CLICK", executed.get(4).getType());
        assertEquals("#btnB", executed.get(4).getTarget());
    }

    @Test
    public final void testConditionalIncludeElseBranch() throws Exception
    {
        // 1. Setup a fresh Playbook in recording mode
        final Playbook playbook = new Playbook("test-conditional-include-else");
        playbook.setRecording(true);
        Neodymium.setAiPlaybook(playbook);

        // 2. Setup mock source file path so relative includes work
        final File mainFile = new File(tempDir, "main.yaml");
        Files.writeString(mainFile.toPath(), "steps:\n  - dummy\n", StandardCharsets.UTF_8);
        Neodymium.getData().put("neodymium.sourceFile", mainFile.getAbsolutePath());

        // Create the include folder and step files
        final File fragmentsFolder = new File(tempDir, "fragments");
        fragmentsFolder.mkdirs();

        final File subStepsFile = new File(fragmentsFolder, "sub.steps");
        Files.writeString(subStepsFile.toPath(), "- Click button A\n- Click button B\n", StandardCharsets.UTF_8);

        final File elseStepsFile = new File(fragmentsFolder, "else.steps");
        Files.writeString(elseStepsFile.toPath(), "- Click button C\n- Click button D\n", StandardCharsets.UTF_8);

        // 3. Setup mock LLM responses:
        final MockLlmClient mockLlm = new MockLlmClient();

        // Turn 1: Main conditional include step -> returns BRANCH
        mockLlm.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Evaluating condition and selecting branch.",
                      "a": [
                        {
                          "t": "BRANCH",
                          "c": [
                            {
                              "t": "ASSERT",
                              "tg": "#element",
                              "desc": "Check element is visible"
                            }
                          ],
                          "th": [
                            {
                              "t": "INCLUDE",
                              "tg": "fragments/sub.steps",
                              "desc": "Include sub steps"
                            }
                          ],
                          "el": [
                            {
                              "t": "INCLUDE",
                              "tg": "fragments/else.steps",
                              "desc": "Include else steps"
                            }
                          ]
                        }
                      ]
                    }
                    """)
                .build());

        // Turn 2: LLM response for include step 1 "Click button C"
        mockLlm.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Clicking C.",
                      "a": [
                        {
                          "t": "CLICK",
                          "tg": "#btnC",
                          "desc": "Click button C"
                        }
                      ]
                    }
                    """)
                .build());

        // Turn 3: LLM response for include step 2 "Click button D"
        mockLlm.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Clicking D.",
                      "a": [
                        {
                          "t": "CLICK",
                          "tg": "#btnD",
                          "desc": "Click button D"
                        }
                      ]
                    }
                    """)
                .build());

        final MockPageAnalyzer mockAnalyzer = new MockPageAnalyzer("<html><body>Mock DOM Context</body></html>");
        final MockActionExecutor mockExecutor = new MockActionExecutor();

        // Force condition check (ASSERT) to fail so else branch is chosen
        mockExecutor.setBeforeExecuteHook((final Action action) -> {
            if ("ASSERT".equals(action.getType()))
            {
                throw new AssertionError("Element not visible!");
            }
        });

        final AiAgent agent = new AiAgent(mockLlm, mockAnalyzer, mockExecutor, Neodymium.aiConfiguration());

        final List<String> instructions = List.of(
            "If visible, then _include: fragments/sub.steps else _include: fragments/else.steps"
        );

        final AiExecutionResult result = new AiExecutionResult(Neodymium.getData());
        agent.execute(String.join("\n", instructions), result);

        // Verify steps executed (1 main step + 2 included steps = 3 steps)
        assertEquals(3, result.getSteps().size());
        assertEquals("If visible, then _include: fragments/sub.steps else _include: fragments/else.steps", result.getSteps().get(0).getExpandedInstruction());
        assertEquals("Click button C", result.getSteps().get(1).getExpandedInstruction());
        assertEquals("Click button D", result.getSteps().get(2).getExpandedInstruction());

        // Verify executed actions:
        // 1. BRANCH action container
        // 2. INCLUDE action
        // 3. CLICK #btnC
        // 4. CLICK #btnD
        final List<Action> executed = mockExecutor.getExecutedActions();
        assertEquals(4, executed.size());
        assertEquals("BRANCH", executed.get(0).getType());
        assertEquals("INCLUDE", executed.get(1).getType());
        assertEquals("fragments/else.steps", executed.get(1).getTarget());
        assertEquals("CLICK", executed.get(2).getType());
        assertEquals("#btnC", executed.get(2).getTarget());
        assertEquals("CLICK", executed.get(3).getType());
        assertEquals("#btnD", executed.get(3).getTarget());
    }
}
