package com.xceptance.neodymium.junit4.statement.parameter;

import java.util.List;

import org.junit.runners.model.FrameworkField;

public class ParameterStatementData
{
    private int parameterIndex;

    private Object[] parameter;

    private List<FrameworkField> parameterFrameworkFields;

    public ParameterStatementData(int parameterIndex, Object[] parameter, List<FrameworkField> parameterFrameworkFields)
    {
        this.parameterIndex = parameterIndex;
        this.parameter = parameter;
        this.parameterFrameworkFields = parameterFrameworkFields;
    }

    public int getParameterIndex()
    {
        return parameterIndex;
    }

    public Object[] getParameter()
    {
        return parameter;
    }

    public List<FrameworkField> getParameterFrameworkFields()
    {
        return parameterFrameworkFields;
    }

    public void setParameterFrameworkFields(List<FrameworkField> parameterFrameworkFields)
    {
        this.parameterFrameworkFields = parameterFrameworkFields;
    }
}
