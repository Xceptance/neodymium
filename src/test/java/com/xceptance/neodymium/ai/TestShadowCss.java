package com.xceptance.neodymium.ai;

import com.codeborne.selenide.Selectors;
import java.lang.reflect.Method;

public class TestShadowCss {
    public static void main(String[] args) {
        for (Method m : Selectors.class.getMethods()) {
            if (m.getName().toLowerCase().contains("shadow")) {
                System.out.println(m);
            }
        }
    }
}
