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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
@DataFile("com/xceptance/neodymium/junit4/testclasses/ai/AiBrowserBeforeAfterTest.yaml")
public class AiBrowserBeforeAfterTest {
    
    public static List<String> executionLog = new ArrayList<>();
    
    @Before
    public void setup() {
        Neodymium.aiConfiguration().setProperty("neodymium.ai.apiKey", "dummy_token");
        Neodymium.aiConfiguration().setProperty("neodymium.ai.agent.maxRetries", "0");
        Neodymium.aiConfiguration().setProperty("neodymium.ai.playbook.record", "false");
        executionLog.clear();
    }
    
    private Throwable runTestLifecycle() {
        Throwable error = null;
        try {
            Neodymium.ai().execute();
        } catch (Throwable t) {
            error = t;
        }
        return error;
    }

    // Executes:
    // Before: before1
    // Prompt: prompt1
    // After: after1, after2
    @Test
    @DataSet(id = "happyPath")
    public void testHappyPath() {
        Throwable error = runTestLifecycle();
        Assert.assertNull("Happy path should not throw any exceptions.", error);
        Assert.assertEquals(Arrays.asList("before1", "prompt1", "after1", "after2"), executionLog);
    }
    
    // Executes:
    // Before: before1, beforeFail, before2
    // Prompt: prompt1
    // After: after1
    @Test
    @DataSet(id = "failBefore")
    public void testFailBefore() {
        Throwable error = runTestLifecycle();
        Assert.assertNotNull("Error should propagate.", error);
        Throwable unwrap = error instanceof AssertionError && error.getCause() != null ? error.getCause() : error;
        Assert.assertTrue("Should see before fail message.", unwrap.getMessage().contains("Before failed"));
        Assert.assertEquals(Arrays.asList("before1", "beforeFail", "after1"), executionLog);
    }
    
    // Executes:
    // Before: before1
    // Prompt: promptFail
    // After: after1, after2
    @Test
    @DataSet(id = "failPrompt")
    public void testFailPrompt() {
        Throwable error = runTestLifecycle();
        Assert.assertNotNull(error);
        Throwable unwrap = error instanceof AssertionError && error.getCause() != null ? error.getCause() : error;
        Assert.assertTrue("Should see prompt failure", unwrap.getMessage().contains("Prompt failed"));
        Assert.assertEquals(Arrays.asList("before1", "promptFail", "after1", "after2"), executionLog);
    }
    
    // Executes:
    // Before: before1
    // Prompt: promptFail
    // After: afterFail1, after1, afterFail2
    @Test
    @DataSet(id = "failAfters")
    public void testFailAfters() {
        Throwable error = runTestLifecycle();
        Assert.assertNotNull(error);
        Throwable unwrap = error instanceof AssertionError && error.getCause() != null ? error.getCause() : error;
        Assert.assertTrue("The primary error should be the prompt failure throwing.", unwrap.getMessage().contains("Prompt failed"));
        Assert.assertEquals(Arrays.asList("before1", "promptFail", "afterFail1", "after1", "afterFail2"), executionLog);
        
        Throwable[] suppressed = error.getSuppressed();
        Assert.assertEquals("One composite after error should be suppressed under main prompt error", 1, suppressed.length);
        
        Throwable firstSuppressed = suppressed[0];
        Throwable unwrapFirst = firstSuppressed instanceof AssertionError && firstSuppressed.getCause() != null ? firstSuppressed.getCause() : firstSuppressed;
        Assert.assertTrue("Should contain first failure message", unwrapFirst.getMessage().contains("After1 failed"));
        
        Throwable[] subSuppressed = firstSuppressed.getSuppressed();
        if (subSuppressed.length == 0) {
             subSuppressed = unwrapFirst.getSuppressed();
        }
        
        Assert.assertEquals("Second After failure should be suppressed under the first Context", 1, subSuppressed.length);
        Throwable unwrapSecond = subSuppressed[0] instanceof AssertionError && subSuppressed[0].getCause() != null ? subSuppressed[0].getCause() : subSuppressed[0];
        Assert.assertTrue("Should contain second failure message", unwrapSecond.getMessage().contains("After2 failed"));
    }

    public void before1() { executionLog.add("before1"); }
    public void before2() { executionLog.add("before2"); }
    public void beforeFail() { executionLog.add("beforeFail"); throw new RuntimeException("Before failed"); }
    
    public void prompt1() { executionLog.add("prompt1"); }
    public void promptFail() { executionLog.add("promptFail"); throw new RuntimeException("Prompt failed"); }
    
    public void after1() { executionLog.add("after1"); }
    public void after2() { executionLog.add("after2"); }
    public void afterFail1() { executionLog.add("afterFail1"); throw new RuntimeException("After1 failed"); }
    public void afterFail2() { executionLog.add("afterFail2"); throw new RuntimeException("After2 failed"); }
}
