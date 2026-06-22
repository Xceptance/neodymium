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
package com.xceptance.neodymium.junit4.testclasses.ai;

import org.junit.Test;
import org.junit.Assert;
import org.junit.runner.RunWith;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookStep;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class AiBrowserPlaybookTest {
    
    private List<String> executionOrder = new ArrayList<>();
    
    @Test
    public void testPlaybookReplay() {
        Neodymium.initializePlaybook();
        Playbook pb = Neodymium.getAiPlaybook();
        pb.getSteps().clear();
        Action a = new Action("JAVA_METHOD", "executeDummy1", "", "Call executeDummy1");
        pb.addStep(new PlaybookStep("Click on the dummy button", "Reasoning", Arrays.asList(a)));
        pb.setRecording(false);
        pb.setChanged(false);

        Neodymium.ai().execute("Click on the dummy button");
        Assert.assertEquals(Arrays.asList("dummy1"), executionOrder);
    }

    @Test
    public void testPlaybookSkipReplay() {
        final Playbook mockPb = new Playbook(Neodymium.getTestName());
        mockPb.setRecording(false);
        Neodymium.setAiPlaybook(mockPb);

        Neodymium.initializePlaybook();
        Assert.assertFalse(Neodymium.getAiPlaybook().isRecording());

        Neodymium.setAiPlaybook(null);
        Neodymium.getData().put("skipReplay", "true");

        Neodymium.initializePlaybook();
        Assert.assertTrue(Neodymium.getAiPlaybook().isRecording());
    }


    @Test
    public void testPlaybookHealing() {
        Neodymium.initializePlaybook();
        Playbook pb = Neodymium.getAiPlaybook();
        pb.getSteps().clear();
        Action a = new Action("CLICK", "#does-not-exist-element-id-12345", "", "Click non-existent button");
        pb.addStep(new PlaybookStep("Click on the dummy button", "Reasoning", Arrays.asList(a)));
        pb.setRecording(false);
        pb.setChanged(false);

        try {
            Neodymium.ai().execute("Click on the dummy button");
            Assert.fail("Should have failed due to element not found and invalid API key");
        } catch (Throwable error) {
            boolean found = false;
            Throwable current = error;
            while (current != null) {
                if (current.getMessage() != null && (current.getMessage().contains("API key not valid") || current.getMessage().contains("API key not configured"))) {
                    found = true;
                    break;
                }
                current = current.getCause();
            }
            Assert.assertTrue("Should mention API key invalid or not configured", found);
        }
    }

    @Test
    public void testPlaybookPromptChange() {
        Neodymium.initializePlaybook();
        Playbook pb = Neodymium.getAiPlaybook();
        pb.getSteps().clear();
        Action a = new Action("JAVA_METHOD", "executeDummy1", "", "Call executeDummy1");
        pb.addStep(new PlaybookStep("Click on the dummy button", "Reasoning", Arrays.asList(a)));
        pb.setRecording(false);
        pb.setChanged(false);

        try {
            Neodymium.ai().execute("Click differently");
            Assert.fail("Should have failed due to prompt change and invalid API key");
        } catch (Throwable error) {
            boolean found = false;
            Throwable current = error;
            while (current != null) {
                if (current.getMessage() != null && (current.getMessage().contains("API key not valid") || current.getMessage().contains("API key not configured"))) {
                    found = true;
                    break;
                }
                current = current.getCause();
            }
            Assert.assertTrue("Should mention API key invalid or not configured", found);
        }
    }
    @Test
    public void testPlaybookHealingWithDirectParsing() {
        Neodymium.initializePlaybook();
        Playbook pb = Neodymium.getAiPlaybook();
        pb.getSteps().clear();
        Action a = new Action("JAVA_METHOD", "executeDummy1", "", "Call executeDummy1");
        pb.addStep(new PlaybookStep("java:executeDummy1()", "directly parsed or local validation succeeded", Arrays.asList(a)));
        pb.setRecording(false);
        pb.setChanged(false);

        Neodymium.ai().execute("java:executeDummy1()");
        Assert.assertEquals(Arrays.asList("dummy1"), executionOrder);
    }

    @Test
    public void testPlaybookPromptChangeFirstLine() {
        injectTestPlaybook("executeDummy4", "executeDummy2", "executeDummy3");

        Neodymium.ai().execute("java:executeDummy1()");
        Neodymium.ai().execute("java:executeDummy2()");
        Neodymium.ai().execute("java:executeDummy3()");

        Assert.assertEquals(Arrays.asList("dummy1", "dummy2", "dummy3"), executionOrder);
        Playbook playbook = Neodymium.getAiPlaybook();
        Assert.assertEquals(3, playbook.getSteps().size());
        Assert.assertEquals("java:executeDummy1()", playbook.getSteps().get(0).getPromptLine());
        Assert.assertEquals("java:executeDummy2()", playbook.getSteps().get(1).getPromptLine());
        Assert.assertEquals("java:executeDummy3()", playbook.getSteps().get(2).getPromptLine());
        Assert.assertTrue(playbook.isRecording());
    }

    @Test
    public void testPlaybookPromptChangeMiddle() {
        injectTestPlaybook("executeDummy1", "executeDummy4", "executeDummy3");

        Neodymium.ai().execute("java:executeDummy1()");
        Neodymium.ai().execute("java:executeDummy2()");
        Neodymium.ai().execute("java:executeDummy3()");

        Assert.assertEquals(Arrays.asList("dummy1", "dummy2", "dummy3"), executionOrder);
        Playbook playbook = Neodymium.getAiPlaybook();
        Assert.assertEquals(3, playbook.getSteps().size());
        Assert.assertEquals("java:executeDummy1()", playbook.getSteps().get(0).getPromptLine());
        Assert.assertEquals("java:executeDummy2()", playbook.getSteps().get(1).getPromptLine());
        Assert.assertEquals("java:executeDummy3()", playbook.getSteps().get(2).getPromptLine());
        Assert.assertTrue(playbook.isRecording());
    }

    @Test
    public void testPlaybookPromptChangeLastLine() {
        injectTestPlaybook("executeDummy1", "executeDummy2", "executeDummy4");

        Neodymium.ai().execute("java:executeDummy1()");
        Neodymium.ai().execute("java:executeDummy2()");
        Neodymium.ai().execute("java:executeDummy3()");

        Assert.assertEquals(Arrays.asList("dummy1", "dummy2", "dummy3"), executionOrder);
        Playbook playbook = Neodymium.getAiPlaybook();
        Assert.assertEquals(3, playbook.getSteps().size());
        Assert.assertEquals("java:executeDummy1()", playbook.getSteps().get(0).getPromptLine());
        Assert.assertEquals("java:executeDummy2()", playbook.getSteps().get(1).getPromptLine());
        Assert.assertEquals("java:executeDummy3()", playbook.getSteps().get(2).getPromptLine());
        Assert.assertTrue(playbook.isRecording());
    }

    private int flakyAttempts = 0;

    @Test
    public void testPlaybookReplaySkipsLLMOnRetryWhenHealingDisabled()
    {
        flakyAttempts = 0;
        injectTestPlaybook("executeFlaky", null, null);

        Neodymium.ai().execute("java:executeFlaky()");

        Assert.assertEquals(Arrays.asList("flakySuccess"), executionOrder);
        Assert.assertEquals(2, flakyAttempts);

        final Playbook playbook = Neodymium.getAiPlaybook();
        Assert.assertEquals(1, playbook.getSteps().size());
        Assert.assertEquals("java:executeFlaky()", playbook.getSteps().get(0).getPromptLine());
        Assert.assertFalse(playbook.isRecording());
        Assert.assertFalse(playbook.getSteps().get(0).failed());
    }

    public void executeFlaky()
    {
        flakyAttempts++;
        if (flakyAttempts < 2)
        {
            throw new AssertionError("Transient failure simulation");
        }
        executionOrder.add("flakySuccess");
    }

    public void executeDummy1() { executionOrder.add("dummy1"); }
    public void executeDummy2() { executionOrder.add("dummy2"); }
    public void executeDummy3() { executionOrder.add("dummy3"); }
    public void executeDummy4() { executionOrder.add("dummy4"); }

    private void injectTestPlaybook(String target1, String target2, String target3) {
        Neodymium.initializePlaybook();
        Playbook pb = Neodymium.getAiPlaybook();
        pb.getSteps().clear();
        if (target1 != null) pb.addStep(createStep("java:" + target1 + "()", target1));
        if (target2 != null) pb.addStep(createStep("java:" + target2 + "()", target2));
        if (target3 != null) pb.addStep(createStep("java:" + target3 + "()", target3));
        pb.setRecording(false);
        pb.setChanged(false);
    }

    private PlaybookStep createStep(String promptLine, String method) {
        Action a = new Action("JAVA_METHOD", method, "", "Mock action");
        return new PlaybookStep(promptLine, "mock reasoning", Arrays.asList(a));
    }
}
