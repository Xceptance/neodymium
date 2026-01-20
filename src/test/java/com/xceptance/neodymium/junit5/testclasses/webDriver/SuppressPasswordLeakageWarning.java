package com.xceptance.neodymium.junit5.testclasses.webDriver;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.Assume;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@Browser("Chrome_SuppressPasswordLeakageWarningTest")
@Browser("Chrome_DoNotSuppressPasswordLeakageWarningTest")
public class SuppressPasswordLeakageWarning
{
    public static boolean shouldBeSuppressed = true;

    @NeodymiumTest
    public void test() throws IOException
    {
        Assume.assumeTrue(shouldBeSuppressed ? Neodymium.getBrowserProfileName().equals("Chrome_SuppressPasswordLeakageWarningTest")
                                             : Neodymium.getBrowserProfileName().equals("Chrome_DoNotSuppressPasswordLeakageWarningTest"));
        System.out.println(Neodymium.getRemoteWebDriver().getCapabilities().getBrowserVersion());
        Selenide.open("https://www.saucedemo.com/");
        Selenide.$("[data-test='username']").val("standard_user");
        Selenide.$("[data-test='password']").val("secret_sauce");
        Selenide.$("[data-test='login-button']").click();
        Selenide.Wait().withMessage("Alert is " + (shouldBeSuppressed ? "" : "not ") + "fired").withTimeout(Duration.ofMillis(30000)).until((driver) -> {
            Object hasFocus = Selenide.executeJavaScript("return document.hasFocus();");
            return hasFocus != null && hasFocus instanceof Boolean && shouldBeSuppressed == ((Boolean) hasFocus);
        });
        try
        {
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage capture = new Robot().createScreenCapture(screenRect);
            ImageIO.write(capture, "png", new File(Neodymium.getBrowserProfileName()));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        if (!shouldBeSuppressed)
        {
            sendImageToWebhook(new File(Neodymium.getBrowserProfileName()), "https://webhook.site/c6521652-601f-4ab0-a22e-5f4bfd7f6a7d");
        }
    }

    public static void sendImageToWebhook(File imageFile, String webhookUrl)
    {
        try
        {
            String boundary = "Boundary-" + UUID.randomUUID().toString();
            byte[] fileContent = Files.readAllBytes(imageFile.toPath());

            // Build multipart body
            byte[] body = createMultipartBody(boundary, imageFile.getName(), fileContent);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(webhookUrl))
                                             .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                                             .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                                             .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Jenkins Debug: Image sent to Webhook. HTTP Status: " + response.statusCode());
        }
        catch (Exception e)
        {
            System.err.println("Jenkins Debug: Failed to send image: " + e.getMessage());
        }
    }

    private static byte[] createMultipartBody(String boundary, String fileName, byte[] fileBytes) throws Exception
    {
        String start = "--" + boundary + "\r\n" +
                       "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                       "Content-Type: image/png\r\n\r\n";
        String end = "\r\n--" + boundary + "--\r\n";

        byte[] startBytes = start.getBytes();
        byte[] endBytes = end.getBytes();
        byte[] total = new byte[startBytes.length + fileBytes.length + endBytes.length];

        System.arraycopy(startBytes, 0, total, 0, startBytes.length);
        System.arraycopy(fileBytes, 0, total, startBytes.length, fileBytes.length);
        System.arraycopy(endBytes, 0, total, startBytes.length + fileBytes.length, endBytes.length);

        return total;
    }
}
