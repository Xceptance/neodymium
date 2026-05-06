package com.xceptance.neodymium.junit5.testclasses.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookStep;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@Browser("Chrome_headless")
public class AiBrowserPlaybookTest {
    
    private List<String> executionOrder = new ArrayList<>();
    
    @NeodymiumTest
    public void testPlaybookReplay() {
        // This implicitly reads the manually created JSON file for this test
        // and succeeds without hitting the LLM as it's purely a NAVIGATE action
        // navigated via Replay Mode.
        Neodymium.ai().execute("Click on the dummy button");
    }

    @NeodymiumTest
    public void testPlaybookHealing() {
        // Will try to click #does-not-exist found in playbook, fail, retry with LLM, and fail due to invalid key
        Throwable error = org.junit.jupiter.api.Assertions.assertThrows(Throwable.class, () -> {
            Neodymium.ai().execute("Click on the dummy button");
        });
        boolean found = false;
        Throwable current = error;
        while (current != null) {
            if (current.getMessage() != null && (current.getMessage().contains("API key not valid") || current.getMessage().contains("API key not configured"))) {
                found = true;
                break;
            }
            current = current.getCause();
        }
        org.junit.jupiter.api.Assertions.assertTrue(found, "Should mention API key invalid or not configured");
    }

    @NeodymiumTest
    public void testPlaybookPromptChange() {
        // Will expect "Click on the dummy button" but get "Click differently"
        // This causes removal of future playbook steps, setting playbook to recording,
        // and calling LLM, which fails due to invalid key.
        Throwable error = org.junit.jupiter.api.Assertions.assertThrows(Throwable.class, () -> {
            Neodymium.ai().execute("Click differently");
        });
        boolean found = false;
        Throwable current = error;
        while (current != null) {
            if (current.getMessage() != null && (current.getMessage().contains("API key not valid") || current.getMessage().contains("API key not configured"))) {
                found = true;
                break;
            }
            current = current.getCause();
        }
        org.junit.jupiter.api.Assertions.assertTrue(found, "Should mention API key invalid or not configured");
    }

    @NeodymiumTest
    public void testPlaybookHealingWithDirectParsing() {
        Neodymium.ai().execute("java executeDummy1()");
    }

    @NeodymiumTest
    public void testPlaybookPromptChangeFirstLine() {
        injectTestPlaybook("executeDummy4", "executeDummy2", "executeDummy3");

        Neodymium.ai().execute("java executeDummy1()");
        Neodymium.ai().execute("java executeDummy2()");
        Neodymium.ai().execute("java executeDummy3()");

        Assertions.assertIterableEquals(Arrays.asList("dummy1", "dummy2", "dummy3"), executionOrder);
        Playbook playbook = Neodymium.getAiPlaybook();
        Assertions.assertEquals(3, playbook.getSteps().size());
        Assertions.assertEquals("java executeDummy1()", playbook.getSteps().get(0).getPromptLine());
        Assertions.assertEquals("java executeDummy2()", playbook.getSteps().get(1).getPromptLine());
        Assertions.assertEquals("java executeDummy3()", playbook.getSteps().get(2).getPromptLine());
        Assertions.assertTrue(playbook.isRecording());
    }

    @NeodymiumTest
    public void testPlaybookPromptChangeMiddle() {
        injectTestPlaybook("executeDummy1", "executeDummy4", "executeDummy3");

        Neodymium.ai().execute("java executeDummy1()");
        Neodymium.ai().execute("java executeDummy2()");
        Neodymium.ai().execute("java executeDummy3()");

        Assertions.assertIterableEquals(Arrays.asList("dummy1", "dummy2", "dummy3"), executionOrder);
        Playbook playbook = Neodymium.getAiPlaybook();
        Assertions.assertEquals(3, playbook.getSteps().size());
        Assertions.assertEquals("java executeDummy1()", playbook.getSteps().get(0).getPromptLine());
        Assertions.assertEquals("java executeDummy2()", playbook.getSteps().get(1).getPromptLine());
        Assertions.assertEquals("java executeDummy3()", playbook.getSteps().get(2).getPromptLine());
        Assertions.assertTrue(playbook.isRecording());
    }

    @NeodymiumTest
    public void testPlaybookPromptChangeLastLine() {
        injectTestPlaybook("executeDummy1", "executeDummy2", "executeDummy4");

        Neodymium.ai().execute("java executeDummy1()");
        Neodymium.ai().execute("java executeDummy2()");
        Neodymium.ai().execute("java executeDummy3()");

        Assertions.assertIterableEquals(Arrays.asList("dummy1", "dummy2", "dummy3"), executionOrder);
        Playbook playbook = Neodymium.getAiPlaybook();
        Assertions.assertEquals(3, playbook.getSteps().size());
        Assertions.assertEquals("java executeDummy1()", playbook.getSteps().get(0).getPromptLine());
        Assertions.assertEquals("java executeDummy2()", playbook.getSteps().get(1).getPromptLine());
        Assertions.assertEquals("java executeDummy3()", playbook.getSteps().get(2).getPromptLine());
        Assertions.assertTrue(playbook.isRecording());
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
