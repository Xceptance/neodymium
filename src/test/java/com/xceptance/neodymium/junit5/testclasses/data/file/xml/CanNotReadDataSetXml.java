package com.xceptance.neodymium.junit5.testclasses.data.file.xml;

import org.neodymium.common.testdata.DataFile;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

@DataFile("can/not/read/data/set/xml/DoesNotExist.xml")
public class CanNotReadDataSetXml
{
    @NeodymiumTest
    public void test()
    {
        Neodymium.getData();
    }
}
