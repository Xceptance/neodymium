package com.xceptance.neodymium.junit5.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.junit5.testclasses.popupblocker.PopupBlockerTestclass;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class PopupBlockerTest extends AbstractNeodymiumTest
{
    @Test
    public void testPopupBlocker()
    {
        Map<String, String> properties = new HashMap<>();

        properties.put("neodymium.popup.custom", "#myPopUp1");
        properties.put("neodymium.popup.second", "#myPopUp2");
        properties.put("neodymium.popup.third", "#myPopUp3");
        properties.put("neodymium.popup.customWithQuotes", "[data-testid='closeIcon']");

        addPropertiesForTest("temp-PopupBlockerTest-neodymium.properties", properties);

        NeodymiumTestExecutionSummary summary = run(PopupBlockerTestclass.class);
        checkPass(summary, 7, 0);
    }
}
