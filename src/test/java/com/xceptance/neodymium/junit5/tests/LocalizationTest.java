package com.xceptance.neodymium.junit5.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

public class LocalizationTest extends AbstractNeodymiumTest
{
    private static File tempConfigFile;

    @BeforeAll
    public static void createLocalizationFile() throws IOException
    {
        tempConfigFile = File.createTempFile("localization", ".yaml", new File("./config/"));
        tempFiles.add(tempConfigFile);

        // set system property to change default localization file to the new created
        System.setProperty("neodymium.localization.file", tempConfigFile.getPath());

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempConfigFile)));
        bw.write("default:");
        bw.newLine();
        bw.write(" key1: default");
        bw.newLine();
        bw.write(" \"Yes\": \"Yes\"");
        bw.newLine();
        bw.write(" \"911\": 911");
        bw.newLine();
        bw.write(" key2:");
        bw.newLine();
        bw.write("en_US:");
        bw.newLine();
        bw.write(" key1: en_US");
        bw.newLine();
        bw.write("en:");
        bw.newLine();
        bw.write(" key1: en");
        bw.newLine();
        bw.write("fr:");
        bw.newLine();
        bw.write(" key1: fr");
        bw.newLine();
        bw.write(" \"Yes\": Oui");
        bw.newLine();
        bw.write(" \"911\": 112");
        bw.newLine();
        bw.write("fr_FR:");
        bw.newLine();
        bw.write(" key1: fr_FR");
        bw.newLine();
        bw.write("de_DE:");
        bw.newLine();
        bw.write(" key1: de_DE");
        bw.newLine();
        bw.close();
    }

    @NeodymiumTest
    public void testDefault()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "default");
        String key = "key1";
        Assertions.assertEquals("default", Neodymium.localizedText(key));
    }

    @NeodymiumTest
    public void testAutoConversionInValue()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "default");
        String key1 = "Yes";
        Assertions.assertEquals("Yes", Neodymium.localizedText(key1));
        String key2 = "911";
        Assertions.assertEquals("911", Neodymium.localizedText(key2));
    }

    @NeodymiumTest
    public void testUnsetLocale()
    {
        // set locale null and check locale fallback to "default"
        Neodymium.configuration().setProperty("neodymium.locale", null);
        String key = "key1";
        Assertions.assertEquals("default", Neodymium.localizedText(key));
    }

    @NeodymiumTest
    public void testEnUS()
    {
        // en_US is the default locale which is defined in interface Configuration
        String key = "key1";
        Assertions.assertEquals("en_US", Neodymium.localizedText(key));
    }

    @NeodymiumTest
    public void testFrFR()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "fr_FR");
        Assertions.assertEquals("fr_FR", Neodymium.localizedText("key1"));
    }

    @NeodymiumTest
    public void testAutoConversionInValueFrench()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "fr");
        String key1 = "Yes";
        Assertions.assertEquals("Oui", Neodymium.localizedText(key1));
        String key2 = "911";
        Assertions.assertEquals("112", Neodymium.localizedText(key2));
    }

    @NeodymiumTest
    public void testLanguageFallback()
    {
        // we do not have a locale en_CA and expect to fallback to "en"
        Neodymium.configuration().setProperty("neodymium.locale", "en_CA");
        String key = "key1";
        Assertions.assertEquals("en", Neodymium.localizedText(key));
    }

    @NeodymiumTest
    public void testFallbackToDefault()
    {
        // we do not have a locale en_CA and expect to fallback to "en"
        Neodymium.configuration().setProperty("neodymium.locale", "de_AU");
        String key = "key1";
        Assertions.assertEquals("default", Neodymium.localizedText(key));
    }

    @NeodymiumTest
    public void testSpecificLanguageFallback()
    {
        // we do not have a locale fr_CA and expect to fallback to "fr"
        Neodymium.configuration().setProperty("neodymium.locale", "fr_CA");
        String key = "key1";
        Assertions.assertEquals("fr", Neodymium.localizedText(key));
    }

    @NeodymiumTest
    public void testEmptyKeyIsConvertedToEmptyString()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "default");
        Assertions.assertEquals("", Neodymium.localizedText("key2"));
    }

    @NeodymiumTest
    public void testAssertionsionErrorWhenKeyIsUnknown()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "default");
        Assertions.assertThrowsExactly(AssertionError.class, () -> {
            Neodymium.localizedText("key3");
        });
    }

    @NeodymiumTest
    public void testSpecificLocaleNullFallbackToDefault()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "default");
        String key = "key1";
        Assertions.assertEquals("default", Neodymium.localizedText(key, null));
    }

    @NeodymiumTest
    public void testSpecificLocaleEmptyFallbackToDefault()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "default");
        String key = "key1";
        Assertions.assertEquals("default", Neodymium.localizedText(key, ""));
    }

    @NeodymiumTest
    public void testSpecificLocale()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "default");
        String key = "key1";
        Assertions.assertEquals("en_US", Neodymium.localizedText(key, "en_US"));
    }

    @NeodymiumTest
    public void testSpecificLocaleFallback()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "en_US");
        String key = "key1";
        Assertions.assertEquals("fr", Neodymium.localizedText(key, "fr_CA"));
    }
}
