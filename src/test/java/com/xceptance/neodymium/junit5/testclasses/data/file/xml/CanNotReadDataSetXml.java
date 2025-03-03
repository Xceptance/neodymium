package com.xceptance.neodymium.junit5.testclasses.data.file.xml;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.util.Neodymium;

@DataFile("can/not/read/data/set/xml/DoesNotExist.xml")
public class CanNotReadDataSetXml
{
    @NeodymiumTest
    public void test()
    {
        Neodymium.getData();
    }
}
