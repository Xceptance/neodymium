package com.xceptance.neodymium.ai.generator;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PromptGenerationHudHelper {

    private static final Logger LOG = LoggerFactory.getLogger(PromptGenerationHudHelper.class);

    private static final String HUD_JS;
    private static final String HUD_HTML;

    static {
        HUD_JS = loadResource("hud.js");
        HUD_HTML = loadResource("hud.html");
    }

    private static String loadResource(String filename) {
        try (java.io.InputStream is = PromptGenerationHudHelper.class.getResourceAsStream("/com/xceptance/neodymium/ai/generator/" + filename)) {
            if (is != null) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOG.error("Failed to load HUD resource: " + filename, e);
        }
        return "";
    }

    public static void injectOrUpdateHud(List<String> planned, List<String> performed, boolean autoSkip,
            boolean hudPromptChanged, boolean isFinished) {
        try {
            com.codeborne.selenide.Selenide.executeJavaScript(HUD_JS, HUD_HTML, planned, performed, autoSkip,
                    hudPromptChanged, isFinished);
        } catch (Exception e) {
            LOG.warn("Failed to inject AI Generation HUD: {}", e.getMessage());
        }
    }

    public static String checkHudAction() {
        try {
            Object status = com.codeborne.selenide.Selenide.executeJavaScript("return window.neoHudAction;");
            if (status != null) {
                return String.valueOf(status);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Boolean checkAutoSkipStatus() {
        try {
            Object status = com.codeborne.selenide.Selenide.executeJavaScript("return window.neoHudAutoSkip;");
            if (status == null)
                return null;
            return (Boolean) status;
        } catch (Exception e) {
            return null;
        }
    }

    public static void resetHudAction() {
        try {
            com.codeborne.selenide.Selenide.executeJavaScript("window.neoHudAction = null;");
        } catch (Exception e) {
            // ignore
        }
    }
}
