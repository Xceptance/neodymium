package com.xceptance.neodymium.junit4;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.junit.runners.model.FrameworkMethod;

public class EnhancedMethod extends FrameworkMethod
{
    private List<Object> data = new LinkedList<>();

    private List<StatementBuilder<?>> builder = new LinkedList<>();

    public EnhancedMethod(Method method)
    {
        super(method);
    }

    public List<Object> getData()
    {
        return data;
    }

    public void setData(List<Object> data)
    {
        this.data = data;
    }

    public List<StatementBuilder<?>> getBuilder()
    {
        return builder;
    }

    public void setBuilder(List<StatementBuilder<?>> builder)
    {
        this.builder = builder;
    }

    @Override
    public String getName()
    {
        StringBuilder nameBuilder = new StringBuilder(250);
        nameBuilder.append(super.getName());
        for (int i = builder.size() - 1; i >= 0; i--)
        {
            StatementBuilder<?> statementBuilder = builder.get(i);
            nameBuilder.append(" :: ");
            nameBuilder.append(statementBuilder.getTestName(data.get(i)));
        }

        return nameBuilder.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((builder == null) ? 0 : builder.hashCode());
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!super.equals(obj))
        {
            return false;
        }
        if (!(obj instanceof EnhancedMethod))
        {
            return false;
        }
        EnhancedMethod other = (EnhancedMethod) obj;
        if (builder == null)
        {
            if (other.builder != null)
            {
                return false;
            }
        }
        else if (!builder.equals(other.builder))
        {
            return false;
        }
        if (data == null)
        {
            if (other.data != null)
            {
                return false;
            }
        }
        else if (!data.equals(other.data))
        {
            return false;
        }
        return true;
    }
}
