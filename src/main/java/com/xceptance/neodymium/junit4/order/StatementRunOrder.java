package com.xceptance.neodymium.junit4.order;

import java.util.LinkedList;
import java.util.List;

import com.xceptance.neodymium.junit4.StatementBuilder;

public class StatementRunOrder
{
    protected List<Class<? extends StatementBuilder<?>>> runOrder = new LinkedList<>();

    public List<Class<? extends StatementBuilder<?>>> getRunOrder()
    {
        return runOrder;
    }
}
