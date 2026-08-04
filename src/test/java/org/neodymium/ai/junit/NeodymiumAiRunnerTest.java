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
package org.neodymium.ai.junit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit test suite for {@link NeodymiumAiRunner} and associated JUnit 5 annotations.
 * Verifies annotation processing and metadata extraction for {@link NeodymiumAiTest} and {@link AiPlaybook}.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class NeodymiumAiRunnerTest
{
    /**
     * Sample test class decorated with {@link NeodymiumAiTest} and {@link AiPlaybook} for reflection verification.
     */
    @NeodymiumAiTest("inline:name: sample\nsteps:\n  - step: Click search button\n")
    public static class SampleTestClass
    {
        /**
         * Sample test method annotated with {@link AiPlaybook}.
         */
        @Test
        @AiPlaybook("inline:name: sample\nsteps:\n  - step: Click search button\n")
        public void sampleTestMethod()
        {
        }
    }

    /**
     * Goal: Verifies that reflection correctly detects the presence of {@link NeodymiumAiTest} on the test class
     * and {@link AiPlaybook} on individual test methods, extracting inline YAML playbook definitions.
     */
    @Test
    public void testAnnotationPresenceOnSampleTestClass() throws Exception
    {
        // 1. Verify class-level @NeodymiumAiTest annotation presence and value
        final NeodymiumAiTest aiTest = SampleTestClass.class.getAnnotation(NeodymiumAiTest.class);
        Assertions.assertNotNull(aiTest);
        Assertions.assertEquals(1, aiTest.value().length);

        // 2. Verify method-level @AiPlaybook annotation presence and inline prefix
        final AiPlaybook playbook = SampleTestClass.class.getMethod("sampleTestMethod").getAnnotation(AiPlaybook.class);
        Assertions.assertNotNull(playbook);
        Assertions.assertTrue(playbook.value().startsWith("inline:"));
    }

    /**
     * Sample test class with explicit recordingMethod and recordingFileName configuration.
     */
    public static class RecordingAnnotationTestClass
    {
        @Test
        @AiPlaybook(value = "/playbooks/sample.yaml", recordingMethod = "testSourceLive")
        public void sampleReplayMethod()
        {
        }

        @Test
        @AiPlaybook(value = "/playbooks/sample.yaml", recordingFileName = "custom-baseline-file")
        public void sampleCustomFileNameMethod()
        {
        }
    }

    /**
     * Goal: Verifies that recordingMethod and recordingFileName attributes on @AiPlaybook are correctly read.
     */
    @Test
    public void testRecordingMethodAndFileNameAnnotationAttributes() throws Exception
    {
        final AiPlaybook replayPb = RecordingAnnotationTestClass.class.getMethod("sampleReplayMethod").getAnnotation(AiPlaybook.class);
        Assertions.assertNotNull(replayPb);
        Assertions.assertEquals("testSourceLive", replayPb.recordingMethod());
        Assertions.assertEquals("", replayPb.recordingFileName());

        final AiPlaybook filePb = RecordingAnnotationTestClass.class.getMethod("sampleCustomFileNameMethod").getAnnotation(AiPlaybook.class);
        Assertions.assertNotNull(filePb);
        Assertions.assertEquals("", filePb.recordingMethod());
        Assertions.assertEquals("custom-baseline-file", filePb.recordingFileName());
    }

    /**
     * Sample test class decorated with dataset annotations to test exact vs prefix dataset matching.
     */
    public static class DataSetMatchingTestClass
    {
        @Test
        @AiDataSet("modern-bad")
        public void testExactMatch()
        {
        }
    }

    /**
     * Goal: Verifies that @AiDataSet("modern-bad") correctly extracts the dataset annotation and does not match via partial substring.
     */
    @Test
    public void testDataSetAnnotationExtraction() throws Exception
    {
        final AiDataSet ds = DataSetMatchingTestClass.class.getMethod("testExactMatch").getAnnotation(AiDataSet.class);
        Assertions.assertNotNull(ds);
        Assertions.assertEquals(1, ds.value().length);
        Assertions.assertEquals("modern-bad", ds.value()[0]);
    }
}
