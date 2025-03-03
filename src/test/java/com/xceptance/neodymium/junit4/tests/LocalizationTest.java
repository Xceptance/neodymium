package com.xceptance.neodymium.junit4.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
public class LocalizationTest extends NeodymiumTest
{
    private static File tempConfigFile;

    @BeforeClass
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

    @Test
    public void testDefault()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "default");
        String key = "key1";
        Assert.assertEquals("default", Neodymium.localizedText(key));
    }

    @Test
    public void testAutoConversionInValue()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "default");
        String key1 = "Yes";
        Assert.assertEquals("Yes", Neodymium.localizedText(key1));
        String key2 = "911";
        Assert.assertEquals("911", Neodymium.localizedText(key2));
    }

    @Test
    public void testUnsetLocale()
    {
        // set locale null and check locale fallback to "default"
        Neodymium.configuration().setProperty("neodymium.locale", null);
        String key = "key1";
        Assert.assertEquals("default", Neodymium.localizedText(key));
    }

    @Test
    public void testEnUS()
    {
        // en_US is the default locale which is defined in interface Configuration
        String key = "key1";
        Assert.assertEquals("en_US", Neodymium.localizedText(key));
    }

    @Test
    public void testFrFR()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "fr_FR");
        Assert.assertEquals("fr_FR", Neodymium.localizedText("key1"));
    }

    @Test
    public void testAutoConversionInValueFrench()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "fr");
        String key1 = "Yes";
        Assert.assertEquals("Oui", Neodymium.localizedText(key1));
        String key2 = "911";
        Assert.assertEquals("112", Neodymium.localizedText(key2));
    }

    @Test
    public void testLanguageFallback()
    {
        // we do not have a locale en_CA and expect to fallback to "en"
        Neodymium.configuration().setProperty("neodymium.locale", "en_CA");
        String key = "key1";
        Assert.assertEquals("en", Neodymium.localizedText(key));
    }

    @Test
    public void testFallbackToDefault()
    {
        // we do not have a locale en_CA and expect to fallback to "en"
        Neodymium.configuration().setProperty("neodymium.locale", "de_AU");
        String key = "key1";
        Assert.assertEquals("default", Neodymium.localizedText(key));
    }

    @Test
    public void testSpecificLanguageFallback()
    {
        // we do not have a locale fr_CA and expect to fallback to "fr"
        Neodymium.configuration().setProperty("neodymium.locale", "fr_CA");
        String key = "key1";
        Assert.assertEquals("fr", Neodymium.localizedText(key));
    }

    @Test
    public void testEmptyKeyIsConvertedToEmptyString()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "default");
        Assert.assertEquals("", Neodymium.localizedText("key2"));
    }

    @Test
    public void testAssertionErrorWhenKeyIsUnknown()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "default");
        Assert.assertThrows(AssertionError.class, () -> {
            Neodymium.localizedText("key3");
        });
    }

    @Test
    public void testSpecificLocaleNullFallbackToDefault()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "default");
        String key = "key1";
        Assert.assertEquals("default", Neodymium.localizedText(key, null));
    }

    @Test
    public void testSpecificLocaleEmptyFallbackToDefault()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "default");
        String key = "key1";
        Assert.assertEquals("default", Neodymium.localizedText(key, ""));
    }

    @Test
    public void testSpecificLocale()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "default");
        String key = "key1";
        Assert.assertEquals("en_US", Neodymium.localizedText(key, "en_US"));
    }

    @Test
    public void testSpecificLocaleFallback()
    {
        Neodymium.configuration().setProperty("neodymium.locale", "en_US");
        String key = "key1";
        Assert.assertEquals("fr", Neodymium.localizedText(key, "fr_CA"));
    }
}
