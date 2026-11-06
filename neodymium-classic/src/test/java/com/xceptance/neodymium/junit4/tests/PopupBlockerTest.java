package com.xceptance.neodymium.junit4.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.popupblocker.PopupBlockerTestclass;

public class PopupBlockerTest extends NeodymiumTest
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

        Result result = run(PopupBlockerTestclass.class);
        checkPass(result, 7, 0);
    }
}
