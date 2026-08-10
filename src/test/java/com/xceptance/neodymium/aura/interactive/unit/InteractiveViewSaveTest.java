package com.xceptance.neodymium.aura.interactive.unit;

import static com.codeborne.selenide.Selenide.$;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.yaml.snakeyaml.Yaml;

import com.codeborne.selenide.ClickOptions;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.testing.BaseAiTest;
import com.xceptance.neodymium.aura.interactive.util.InteractiveHudTestUtils;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.testdata.util.YamlFileReader;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * E2E tests for the interactive view saving functionality.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("unit")
@Tag("interactive-view")
@Browser("Chrome_headless")
public class InteractiveViewSaveTest extends BaseAiTest
{
    private File tempDatasetYaml;
    private Thread bgThread;

    @Override
    @BeforeEach
    public void setupPageUrl(final org.junit.jupiter.api.TestInfo testInfo)
    {
        System.clearProperty("neodymium.ai.interactive");
        org.neodymium.ai.config.AiConfiguration.resetInstance();
        super.setupPageUrl(testInfo);
    }

    @BeforeEach
    public void setup() throws Exception
    {
        System.setProperty("neodymium.ai.interactive", "true");
        System.setProperty("neodymium.ai.interactive.allowHeadlessHUD", "true");
        // Decrease think delay to make test fast
        System.setProperty("neodymium.ai.console.simulation.thinkMs", "20");
        org.neodymium.ai.config.AiConfiguration.resetInstance();
        Configuration.timeout = 10000;
        Configuration.headless = true;
    }

    @AfterEach
    public void teardown()
    {
        System.clearProperty("neodymium.ai.interactive");
        System.clearProperty("neodymium.ai.interactive.allowHeadlessHUD");
        System.clearProperty("neodymium.ai.console.simulation.thinkMs");
        org.neodymium.ai.config.AiConfiguration.resetInstance();
        if (tempDatasetYaml != null && tempDatasetYaml.exists())
        {
            // tempDatasetYaml.delete();
        }
        if (bgThread != null && bgThread.isAlive())
        {
            bgThread.interrupt();
        }
    }

    private void uiEditStep(int globalStepIndex, String newText) {
        final SelenideElement step = $(".step-card[data-step-idx='" + globalStepIndex + "']");
        com.codeborne.selenide.Selenide.executeJavaScript("arguments[0].dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, view: window}));", step.$(".step-edit-btn"));
        step.$(".inline-edit-textarea").should(Condition.exist);
        step.$(".inline-edit-textarea").setValue(newText);
        com.codeborne.selenide.Selenide.executeJavaScript("window.saveEdit(arguments[0]);", step.$(".step-save-btn"));
        step.$(".inline-edit-textarea").shouldNotBe(Condition.visible);
        step.shouldNotHave(Condition.cssClass("editing"));
    }

    private void uiSkipStep(int globalStepIndex) {
        final SelenideElement step = $(".step-card[data-step-idx='" + globalStepIndex + "']");
        com.codeborne.selenide.Selenide.executeJavaScript("arguments[0].dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, view: window}));", $("#btnSkip"));
        step.shouldHave(Condition.cssClass("skipped"));
    }

    private void uiAddStep(String blockName, String newText) {
        com.codeborne.selenide.Selenide.executeJavaScript("openAddStepOverlay('" + blockName + "');");
        
        // Wait for the new temp step to appear
        final SelenideElement newStep = $(".step-card.editing");
        newStep.$(".inline-edit-textarea").should(Condition.exist);
        newStep.$(".inline-edit-textarea").setValue(newText);
        com.codeborne.selenide.Selenide.executeJavaScript("window.saveEdit(arguments[0]);", newStep.$(".step-save-btn"));
        $(".step-card.editing").shouldNot(Condition.exist);
    }

    private void uiChangeVariable(String key, String value) {
        //enter edit mode
        $(".step-card.active").hover();
        $(".step-card.active .step-edit-btn").click(ClickOptions.usingJavaScript());
        //change value
        $(".active.editing .inline-edit-bindings tbody tr input[oninput*=" + key + "]").setValue(value);
        //save
        $(".active.editing .step-save-btn").click();
    }
    
    private void uiAdvanceToStep(String blockName, int targetGlobalIndex) {
        final java.util.Set<String> processedPauseIds = new java.util.HashSet<>();
        com.codeborne.selenide.Selenide.Wait().until(driver -> {
            String pauseId = (String) com.codeborne.selenide.Selenide.executeJavaScript("return window.currentPauseId;");
            if (pauseId != null && !pauseId.isEmpty()) {
                String js = "if (!window.currentState || !window.currentState.blocks) return null;" +
                            "for (const b of ['before', 'steps', 'after']) {" +
                            "  const steps = window.currentState.blocks[b] || [];" +
                            "  for (const s of steps) {" +
                            "    if (s.status === 'running') return s.index;" +
                            "  }" +
                            "}" +
                            "return null;";
                Object activeTargetObj = com.codeborne.selenide.Selenide.executeJavaScript(js);
                if (activeTargetObj != null) {
                    int activeGlobalIdx = ((Number) activeTargetObj).intValue();
                    if (activeGlobalIdx >= targetGlobalIndex) {
                        return true;
                    }
                    if (processedPauseIds.add(pauseId)) {
                        com.codeborne.selenide.Selenide.executeJavaScript("sendAction('RUN');");
                    }
                }
            }
            return false;
        });
    }

    private void waitPauseId() {
        com.codeborne.selenide.Selenide.Wait().until(driver -> {
            Boolean hasPauseId = (Boolean) com.codeborne.selenide.Selenide.executeJavaScript("return !!window.currentPauseId;");
            return hasPauseId != null && hasPauseId;
        });
    }

    private void runInteractiveConsoleTest(String sourceYamlName, Runnable uiActions, Consumer<String> resultAssertions, boolean local) throws Exception {
        File sourceDataset = new File("src/test/resources/com/xceptance/neodymium/ai/console/save_tests/" + sourceYamlName);
        tempDatasetYaml = File.createTempFile(sourceYamlName.replace(".yaml", ""), ".yaml");
        Files.copy(sourceDataset.toPath(), tempDatasetYaml.toPath(), StandardCopyOption.REPLACE_EXISTING);

        final MockLlmProvider mockLlmProvider = new MockLlmProvider();

        Neodymium.setTestdataSourceFile(tempDatasetYaml.getAbsolutePath());

        java.util.List<java.util.Map<String, String>> datasets = YamlFileReader.readFile(tempDatasetYaml);
        Neodymium.getData().putAll(datasets.get(0));

        final AtomicReference<String> consoleUrl = new AtomicReference<>();
        final CountDownLatch urlLatch = new CountDownLatch(1);

        bgThread = InteractiveHudTestUtils.runInteractiveInBgSeparateBrowser(() -> {
            try {
                final com.xceptance.neodymium.ai.console.InteractiveConsoleEngine engine = new com.xceptance.neodymium.ai.console.InteractiveConsoleEngine("test-run-" + System.currentTimeMillis());
                final com.xceptance.neodymium.ai.console.InteractiveConsoleServer server = new com.xceptance.neodymium.ai.console.InteractiveConsoleServer(engine);
                consoleUrl.set(server.getLocalUrl());
                urlLatch.countDown();
                try {
                    com.xceptance.neodymium.ai.console.InteractiveConsoleServer.runSimulation(engine, tempDatasetYaml, null);
                } finally {
                    server.stop();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, e -> {
            e.printStackTrace();
            return null;
        });
        urlLatch.await(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(consoleUrl.get(), "Interactive console URL should not be null");

        Selenide.open(consoleUrl.get());

        final SelenideElement step0 = $(".step-card");
        step0.should(Condition.exist);
        
        waitPauseId();

        // Do the test
        uiActions.run();
        // auto run till the End
        if (!$("#finalSaveOverlay").is(Condition.visible)) {
            Selenide.executeJavaScript("if (!window.isAutoMode) { toggleAuto(); }");
        }

        $("#finalSaveOverlay").shouldBe(Condition.visible, java.time.Duration.ofSeconds(45));

        if ($("#saveScopeSelect").is(Condition.visible)) {
            $("#saveScopeSelect").selectOptionByValue(local ? "local" : "global");
        }
        // Save changes
        $("#finalSaveButtons .btn-primary").shouldBe(Condition.visible, java.time.Duration.ofSeconds(10)).click();

        bgThread.join(10000);

        String finalContent = Files.readString(tempDatasetYaml.toPath());
        System.out.println("====== GENERATED YAML ======");
        System.out.println(finalContent);
        System.out.println("============================");
        resultAssertions.accept(finalContent);
    }

    @NeodymiumTest
    public void testSkipBeforeStep_Global() throws Exception {
        System.out.println(">>> RUNNING TEST: " + "testSkipBeforeStep_Global");
        runInteractiveConsoleTest("save_dataset_global.yaml", () -> {
            uiAdvanceToStep("before", 0);
            uiSkipStep(0);
        }, (final String content) -> {
            assertGlobalBlock(content, "before", "// [SKIPPED] Before step 1");
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, false);
    }

    @NeodymiumTest
    public void testEditBeforeStep_Global() throws Exception {
        System.out.println(">>> RUNNING TEST: " + "testEditBeforeStep_Global");
        runInteractiveConsoleTest("save_dataset_global.yaml", () -> {
            uiAdvanceToStep("before", 0);
            uiEditStep(0, "Before step modified");
        }, (final String content) -> {
            assertGlobalBlock(content, "before", "Before step modified");
            assertNoRandomKey(content);
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, false);
    }

    @NeodymiumTest
    public void testAddBeforeStep_Global() throws Exception
    {
        System.out.println(">>> RUNNING TEST: " + "testAddAfterBeforeStep_Global");
        runInteractiveConsoleTest("save_dataset_global.yaml", () -> {
            uiAdvanceToStep("before", 0);
            uiAddStep("before", "New after Before");
        }, (final String content) -> {
            assertGlobalBlock(content, "before", "Before step 1\nNew after Before");
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, false);
    }

    @NeodymiumTest
    public void testSkipstepsStep_Global() throws Exception {
        System.out.println(">>> RUNNING TEST: " + "testSkipstepsStep_Global");
        runInteractiveConsoleTest("save_dataset_global.yaml", () -> {
            uiAdvanceToStep("steps", 1);
            uiSkipStep(1);
        }, (final String content) -> {
            assertGlobalBlock(content, "steps", "// [SKIPPED] Step 1 \"${var}\" (optional)");
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, false);
    }

    @NeodymiumTest
    public void testEditstepsStep_Global() throws Exception {
        System.out.println(">>> RUNNING TEST: " + "testEditstepsStep_Global");
        runInteractiveConsoleTest("save_dataset_global.yaml", () -> {
            uiAdvanceToStep("steps", 1);
            uiEditStep(1, "steps step modified");
        }, (final String content) -> {
            assertGlobalBlock(content, "steps", "steps step modified");
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, false);
    }

    @NeodymiumTest
    public void testAddStep_Global() throws Exception
    {
        System.out.println(">>> RUNNING TEST: " + "testAddAfterstepsStep_Global");
        runInteractiveConsoleTest("save_dataset_global.yaml", () -> {
            uiAdvanceToStep("steps", 1);
            uiAddStep("steps", "New after steps");
        }, (final String content) -> {
            assertGlobalBlock(content, "steps", "Step 1 \"${var}\" (optional)\nNew after steps");
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, false);
    }

    @NeodymiumTest
    public void testSkipAfterStep_Global() throws Exception {
        System.out.println(">>> RUNNING TEST: " + "testSkipAfterStep_Global");
        runInteractiveConsoleTest("save_dataset_global.yaml", () -> {
            uiAdvanceToStep("after", 2);
            uiSkipStep(2);
        }, (final String content) -> {
            assertGlobalBlock(content, "after", "// [SKIPPED] After step 1");
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, false);
    }

    @NeodymiumTest
    public void testEditAfterStep_Global() throws Exception {
        System.out.println(">>> RUNNING TEST: " + "testEditAfterStep_Global");
        runInteractiveConsoleTest("save_dataset_global.yaml", () -> {
            uiAdvanceToStep("after", 2);
            uiEditStep(2, "After step modified");
        }, (final String content) -> {
            assertGlobalBlock(content, "after", "After step modified");
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, false);
    }

    @NeodymiumTest
    public void testAddAfterStep_Global() throws Exception
    {
        System.out.println(">>> RUNNING TEST: " + "testAddAfterAfterStep_Global");
        runInteractiveConsoleTest("save_dataset_global.yaml", () -> {
            uiAdvanceToStep("after", 2);
            uiAddStep("after", "New after After");
        }, (final String content) -> {
            assertGlobalBlock(content, "after", "After step 1\nNew after After");
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, false);
    }

    @NeodymiumTest
    public void testSkipBeforeStep_Local() throws Exception {
        System.out.println(">>> RUNNING TEST: " + "testSkipBeforeStep_Local");
        runInteractiveConsoleTest("save_dataset_local.yaml", () -> {
            uiAdvanceToStep("before", 0);
            uiSkipStep(0);
        }, (final String content) -> {
            assertLocalBlock(content, "before", "// [SKIPPED] Before step 1");
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, true);
    }

    @NeodymiumTest
    public void testEditBeforeStep_Local() throws Exception {
        System.out.println(">>> RUNNING TEST: " + "testEditBeforeStep_Local");
        runInteractiveConsoleTest("save_dataset_local.yaml", () -> {
            uiAdvanceToStep("before", 0);
            uiEditStep(0, "Before step modified");
        }, (final String content) -> {
            assertLocalBlock(content, "before", "Before step modified");
            assertNoRandomKey(content);
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, true);
    }

    @NeodymiumTest
    public void testAddBeforeStep_Local() throws Exception
    {
        System.out.println(">>> RUNNING TEST: " + "testAddAfterBeforeStep_Local");
        runInteractiveConsoleTest("save_dataset_local.yaml", () -> {
            uiAdvanceToStep("before", 0);
            uiAddStep("before", "New after Before");
        }, (final String content) -> {
            assertLocalBlock(content, "before", "Before step 1\nNew after Before");
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, true);
    }

    @NeodymiumTest
    public void testSkipstepsStep_Local() throws Exception {
        System.out.println(">>> RUNNING TEST: " + "testSkipstepsStep_Local");
        runInteractiveConsoleTest("save_dataset_local.yaml", () -> {
            uiAdvanceToStep("steps", 1);
            uiSkipStep(1);
        }, (final String content) -> {
            assertLocalBlock(content, "steps", "// [SKIPPED] Step 1 \"${var}\" (optional)");
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, true);
    }

    @NeodymiumTest
    public void testEditstepsStep_Local() throws Exception {
        System.out.println(">>> RUNNING TEST: " + "testEditstepsStep_Local");
        runInteractiveConsoleTest("save_dataset_local.yaml", () -> {
            uiAdvanceToStep("steps", 1);
            uiEditStep(1, "steps step modified");
        }, (final String content) -> {
            assertLocalBlock(content, "steps", "steps step modified");
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, true);
    }

    @NeodymiumTest
    public void testAddAfterstepsStep_Local() throws Exception {
        System.out.println(">>> RUNNING TEST: " + "testAddAfterstepsStep_Local");
        runInteractiveConsoleTest("save_dataset_local.yaml", () -> {
            uiAdvanceToStep("steps", 1);
            uiAddStep("steps", "New after steps");
        }, (final String content) -> {
            assertLocalBlock(content, "steps", "Step 1 \"${var}\" (optional)\nNew after steps");
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, true);
    }

    @NeodymiumTest
    public void testSkipAfterStep_Local() throws Exception {
        System.out.println(">>> RUNNING TEST: " + "testSkipAfterStep_Local");
        runInteractiveConsoleTest("save_dataset_local.yaml", () -> {
            uiAdvanceToStep("after", 2);
            uiSkipStep(2);
        }, (final String content) -> {
            assertLocalBlock(content, "after", "// [SKIPPED] After step 1");
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, true);
    }

    @NeodymiumTest
    public void testEditAfterStep_Local() throws Exception {
        System.out.println(">>> RUNNING TEST: " + "testEditAfterStep_Local");
        runInteractiveConsoleTest("save_dataset_local.yaml", () -> {
            uiAdvanceToStep("after", 2);
            uiEditStep(2, "After step modified");
        }, (final String content) -> {
            assertLocalBlock(content, "after", "After step modified");
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, true);
    }

    @NeodymiumTest
    public void testAddAfterStep_Local() throws Exception
    {
        System.out.println(">>> RUNNING TEST: " + "testAddAfterAfterStep_Local");
        runInteractiveConsoleTest("save_dataset_local.yaml", () -> {
            uiAdvanceToStep("after", 2);
            uiAddStep("after", "New after After");
        }, (final String content) -> {
            assertLocalBlock(content, "after", "After step 1\nNew after After");
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, true);
    }

    @NeodymiumTest
    public void testChangeVariable() throws Exception
    {
        System.out.println(">>> RUNNING TEST: " + "testChangeVariable_Global");
        runInteractiveConsoleTest("save_dataset_global.yaml", () -> {
            uiAdvanceToStep("before", 0);
            uiChangeVariable("var", "new_val");
        }, (final String content) -> {
            assertLocalBlock(content, "var", "new_val");
            assertNoRandomKey(content);
            Assertions.assertFalse(content.contains("raw_steps:"), "No raw steps");
        }, true);
    }

    @SuppressWarnings("unchecked")
    private void assertGlobalBlock(final String content, final String blockKey, final String expectedSteps)
    {
        final Yaml yaml = new Yaml();
        final Map<String, Object> map = yaml.load(content);
        final String actualSteps = (String) map.get(blockKey);
        Assertions.assertNotNull(actualSteps, "Global block " + blockKey + " should exist");
        Assertions.assertEquals(expectedSteps.trim(), actualSteps.trim());

        // Assert it does NOT exist inside the local dataset
        final List<Object> dataList = (List<Object>) map.get("data");
        if (dataList != null)
        {
            for (final Object datasetObj : dataList)
            {
                if (datasetObj instanceof Map)
                {
                    final Map<String, Object> dataset = (Map<String, Object>) datasetObj;
                    Assertions.assertFalse(dataset.containsKey(blockKey), "Local dataset should NOT contain block " + blockKey);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void assertLocalBlock(final String content, final String blockKey, final String expectedSteps)
    {
        final Yaml yaml = new Yaml();
        final Map<String, Object> map = yaml.load(content);

        // Assert it does NOT exist inside the global level
        Assertions.assertFalse(map.containsKey(blockKey), "Global level should NOT contain block " + blockKey);

        // Assert it exists inside the first local dataset
        final List<Object> dataList = (List<Object>) map.get("data");
        Assertions.assertNotNull(dataList, "Data array must exist");
        Assertions.assertFalse(dataList.isEmpty(), "Data array must not be empty");

        final Map<String, Object> firstDataset = (Map<String, Object>) dataList.get(0);
        final String actualSteps = (String) firstDataset.get(blockKey);
        Assertions.assertNotNull(actualSteps, "Local dataset block " + blockKey + " should exist");
        Assertions.assertEquals(expectedSteps.trim(), actualSteps.trim());
    }

    @SuppressWarnings("unchecked")
    private void assertNoRandomKey(final String content)
    {
        final Yaml yaml = new Yaml();
        final Map<String, Object> map = yaml.load(content);
        Assertions.assertFalse(map.containsKey("random"), "Global level should not contain random");
        final List<Object> dataList = (List<Object>) map.get("data");
        if (dataList != null)
        {
            for (final Object datasetObj : dataList)
            {
                if (datasetObj instanceof Map)
                {
                    final Map<String, Object> dataset = (Map<String, Object>) datasetObj;
                    Assertions.assertFalse(dataset.containsKey("random"), "Local dataset should not contain random");
                }
            }
        }
    }
}
