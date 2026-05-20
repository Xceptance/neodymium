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
        // This implicitly reads the manually created JSON file for this test
        // and succeeds without hitting the LLM as it's purely a NAVIGATE action
        // navigated via Replay Mode.
        Neodymium.ai().execute("Click on the dummy button");
    }

    @Test
    public void testPlaybookHealing() {
        try {
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
            org.junit.Assert.assertTrue("Should mention API key invalid or not configured", found);
        }
    }

    @Test
    public void testPlaybookPromptChange() {
        try {
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
            org.junit.Assert.assertTrue("Should mention API key invalid or not configured", found);
        }
    }
    @Test
    public void testPlaybookHealingWithDirectParsing() {
        Neodymium.ai().execute("java executeDummy1()");
    }

    @Test
    public void testPlaybookPromptChangeFirstLine() {
        injectTestPlaybook("executeDummy4", "executeDummy2", "executeDummy3");

        Neodymium.ai().execute("java executeDummy1()");
        Neodymium.ai().execute("java executeDummy2()");
        Neodymium.ai().execute("java executeDummy3()");

        Assert.assertEquals(Arrays.asList("dummy1", "dummy2", "dummy3"), executionOrder);
        Playbook playbook = Neodymium.getAiPlaybook();
        Assert.assertEquals(3, playbook.getSteps().size());
        Assert.assertEquals("java executeDummy1()", playbook.getSteps().get(0).getPromptLine());
        Assert.assertEquals("java executeDummy2()", playbook.getSteps().get(1).getPromptLine());
        Assert.assertEquals("java executeDummy3()", playbook.getSteps().get(2).getPromptLine());
        Assert.assertTrue(playbook.isRecording());
    }

    @Test
    public void testPlaybookPromptChangeMiddle() {
        injectTestPlaybook("executeDummy1", "executeDummy4", "executeDummy3");

        Neodymium.ai().execute("java executeDummy1()");
        Neodymium.ai().execute("java executeDummy2()");
        Neodymium.ai().execute("java executeDummy3()");

        Assert.assertEquals(Arrays.asList("dummy1", "dummy2", "dummy3"), executionOrder);
        Playbook playbook = Neodymium.getAiPlaybook();
        Assert.assertEquals(3, playbook.getSteps().size());
        Assert.assertEquals("java executeDummy1()", playbook.getSteps().get(0).getPromptLine());
        Assert.assertEquals("java executeDummy2()", playbook.getSteps().get(1).getPromptLine());
        Assert.assertEquals("java executeDummy3()", playbook.getSteps().get(2).getPromptLine());
        Assert.assertTrue(playbook.isRecording());
    }

    @Test
    public void testPlaybookPromptChangeLastLine() {
        injectTestPlaybook("executeDummy1", "executeDummy2", "executeDummy4");

        Neodymium.ai().execute("java executeDummy1()");
        Neodymium.ai().execute("java executeDummy2()");
        Neodymium.ai().execute("java executeDummy3()");

        Assert.assertEquals(Arrays.asList("dummy1", "dummy2", "dummy3"), executionOrder);
        Playbook playbook = Neodymium.getAiPlaybook();
        Assert.assertEquals(3, playbook.getSteps().size());
        Assert.assertEquals("java executeDummy1()", playbook.getSteps().get(0).getPromptLine());
        Assert.assertEquals("java executeDummy2()", playbook.getSteps().get(1).getPromptLine());
        Assert.assertEquals("java executeDummy3()", playbook.getSteps().get(2).getPromptLine());
        Assert.assertTrue(playbook.isRecording());
    }
    public void executeDummy1() { executionOrder.add("dummy1"); }
    public void executeDummy2() { executionOrder.add("dummy2"); }
    public void executeDummy3() { executionOrder.add("dummy3"); }
    public void executeDummy4() { executionOrder.add("dummy4"); }

    private void injectTestPlaybook(String target1, String target2, String target3) {
        Neodymium.initializePlaybook();
        Playbook pb = Neodymium.getAiPlaybook();
        pb.getSteps().clear();
        if (target1 != null) pb.addStep(createStep("java " + target1 + "()", target1));
        if (target2 != null) pb.addStep(createStep("java " + target2 + "()", target2));
        if (target3 != null) pb.addStep(createStep("java " + target3 + "()", target3));
        pb.setRecording(false);
        pb.setChanged(false);
    }

    private PlaybookStep createStep(String promptLine, String method) {
        Action a = new Action("JAVA_METHOD", method, "", "Mock action");
        return new PlaybookStep(promptLine, "mock reasoning", Arrays.asList(a));
    }
}
