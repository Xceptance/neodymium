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
package org.neodymium.ai.report;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.event.structural.StateCapturedEvent;
import org.neodymium.ai.executor.MockSutState;
import org.neodymium.ai.model.PlaybookStep;

/**
 * Unit tests verifying screenshot dimension extraction, serialization, and reporting.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class ReportScreenshotDimensionsTest
{
    private String createBase64Image(final int width, final int height, final String format) throws Exception
    {
        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, format, baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    @Test
    @DisplayName("Verify ReportScreenshotEntry resolves dimensions automatically from Base64")
    public void testReportScreenshotEntryResolvesDimensions() throws Exception
    {
        final String base64Png = createBase64Image(1500, 857, "png");
        final TestExecutionReport.ReportScreenshotEntry entry = new TestExecutionReport.ReportScreenshotEntry(
            "Test Screenshot",
            0,
            "image/png",
            base64Png,
            System.currentTimeMillis()
        );

        Assertions.assertEquals(Integer.valueOf(1500), entry.getWidth());
        Assertions.assertEquals(Integer.valueOf(857), entry.getHeight());
        Assertions.assertEquals("1500x857 px", entry.getDimensions());
    }

    @Test
    @DisplayName("Verify ReportScreenshotEntry with explicit dimensions constructor")
    public void testReportScreenshotEntryExplicitDimensions()
    {
        final TestExecutionReport.ReportScreenshotEntry entry = new TestExecutionReport.ReportScreenshotEntry(
            "Explicit Screenshot",
            1,
            "image/png",
            null,
            System.currentTimeMillis(),
            1920,
            1080
        );

        Assertions.assertEquals(Integer.valueOf(1920), entry.getWidth());
        Assertions.assertEquals(Integer.valueOf(1080), entry.getHeight());
        Assertions.assertEquals("1920x1080 px", entry.getDimensions());
    }

    @Test
    @DisplayName("Verify Markdown report generator includes Width, Height, and Dimensions columns")
    public void testMarkdownReportIncludesDimensionsColumn() throws Exception
    {
        final String base64Png = createBase64Image(1500, 881, "png");
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestName("VisualDimensionsTest");
        report.setStatus("SUCCESS");

        final TestExecutionReport.ReportScreenshotEntry sc = new TestExecutionReport.ReportScreenshotEntry(
            "Step #1 Capture",
            0,
            "image/png",
            base64Png,
            123456789L,
            1500,
            881
        );
        report.addScreenshot(sc);

        final TestExecutionReport.ReportStepEntry step = new TestExecutionReport.ReportStepEntry();
        step.setStepIndex(0);
        step.setInstruction("Check dashboard layout");
        step.setStatus("SUCCESS");
        step.addScreenshot(sc);
        report.addStep(step);

        final MarkdownReportGenerator generator = new MarkdownReportGenerator();
        final String md = generator.generate(report);

        Assertions.assertTrue(md.contains("| # | Step | Name | Format | Width | Height | Dimensions | Timestamp |"), "Markdown must contain explicit Width and Height column headers");
        Assertions.assertTrue(md.contains("| `1500 px` | `881 px` | `1500x881 px` |"), "Markdown table row must contain explicit width and height values");
        Assertions.assertTrue(md.contains("Width: 1500px, Height: 881px"), "Markdown step detail must show explicit Width and Height");
    }

    @Test
    @DisplayName("Verify HTML report generator includes width and height metadata in gallery and inspector")
    public void testHtmlReportIncludesDimensions() throws Exception
    {
        final String base64Png = createBase64Image(1500, 857, "png");
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestName("VisualDimensionsTest");
        report.setStatus("SUCCESS");

        final TestExecutionReport.ReportScreenshotEntry sc = new TestExecutionReport.ReportScreenshotEntry(
            "Step #1 Capture",
            0,
            "image/png",
            base64Png,
            123456789L,
            1500,
            857
        );
        report.addScreenshot(sc);

        final TestExecutionReport.ReportStepEntry step = new TestExecutionReport.ReportStepEntry();
        step.setStepIndex(0);
        step.setInstruction("Check dashboard layout");
        step.setStatus("SUCCESS");
        step.addScreenshot(sc);
        report.addStep(step);

        final HtmlReportGenerator generator = new HtmlReportGenerator();
        final String html = generator.generate(report);

        Assertions.assertTrue(html.contains("1500x857 px"), "HTML report must contain image dimensions");
        Assertions.assertTrue(html.contains("Width: <strong>1500px</strong>") || html.contains("1500px"), "HTML report must display explicit width");
        Assertions.assertTrue(html.contains("Height: <strong>857px</strong>") || html.contains("857px"), "HTML report must display explicit height");
    }

    @Test
    @DisplayName("Verify JSON report generator includes width, height, and dimensions fields")
    public void testJsonReportIncludesWidthAndHeight() throws Exception
    {
        final String base64Png = createBase64Image(1500, 857, "png");
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestName("VisualDimensionsTest");
        report.setStatus("SUCCESS");

        final TestExecutionReport.ReportScreenshotEntry sc = new TestExecutionReport.ReportScreenshotEntry(
            "Step #1 Capture",
            0,
            "image/png",
            base64Png,
            123456789L,
            1500,
            857
        );
        report.addScreenshot(sc);

        final JsonReportGenerator generator = new JsonReportGenerator();
        final String json = generator.generate(report);

        Assertions.assertTrue(json.contains("\"width\" : 1500") || json.contains("\"width\":1500"), "JSON report must contain width field");
        Assertions.assertTrue(json.contains("\"height\" : 857") || json.contains("\"height\":857"), "JSON report must contain height field");
        Assertions.assertTrue(json.contains("\"dimensions\" : \"1500x857 px\"") || json.contains("\"dimensions\":\"1500x857 px\""), "JSON report must contain dimensions field");
    }

    @Test
    @DisplayName("Verify PreliminaryReportListener populates dimensions on StateCapturedEvent")
    public void testPreliminaryReportListenerExtractsDimensions() throws Exception
    {
        final String base64Png = createBase64Image(1500, 857, "png");
        final SutAttachment attachment = new SutAttachment("image/png", null, base64Png);
        final MockSutState state = new MockSutState("<html></html>", List.of(attachment), "hash123");

        final PlaybookStep pbStep = new PlaybookStep();
        pbStep.setInstruction("Visual check");

        final PreliminaryReportListener listener = new PreliminaryReportListener(Path.of("target/test-reports"), Set.of(DiskReportFormat.HTML, DiskReportFormat.MARKDOWN, DiskReportFormat.JSON));
        listener.onEvent(new org.neodymium.ai.event.structural.StepStartedEvent(pbStep, 0));
        listener.onEvent(new StateCapturedEvent(state));

        final TestExecutionReport report = listener.getReport();
        Assertions.assertEquals(1, report.getScreenshots().size());
        final TestExecutionReport.ReportScreenshotEntry sc = report.getScreenshots().get(0);
        Assertions.assertEquals(Integer.valueOf(1500), sc.getWidth());
        Assertions.assertEquals(Integer.valueOf(857), sc.getHeight());
        Assertions.assertEquals("1500x857 px", sc.getDimensions());
    }
}
