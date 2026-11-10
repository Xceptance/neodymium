package com.xceptance.neodymium.junit4.statement.parameter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.junit4.StatementBuilder;

public class ParameterStatement extends StatementBuilder<ParameterStatementData>
{
    public static Logger LOGGER = LoggerFactory.getLogger(ParameterStatement.class);

    private Statement next;

    private ParameterStatementData statementData;

    private Object testClassInstance;

    public ParameterStatement(Statement next, ParameterStatementData parameter, Object testClassInstance)
    {
        this.next = next;
        this.statementData = parameter;
        this.testClassInstance = testClassInstance;
    }

    public ParameterStatement()
    {
    }

    @Override
    public void evaluate() throws Throwable
    {
        injectTestParameter();
        next.evaluate();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ParameterStatementData> createIterationData(TestClass testClass, FrameworkMethod method) throws Exception
    {
        List<FrameworkMethod> parametersMethods = testClass.getAnnotatedMethods(Parameters.class);
        Iterable<Object> parameter = null;

        for (FrameworkMethod parametersMethod : parametersMethods)
        {
            if (parametersMethod.isPublic() && parametersMethod.isStatic())
            {
                // take the first public static method. invoke it and use the result as parameter
                try
                {
                    Object parametersResult = parametersMethod.invokeExplosively(null);
                    if (parametersResult instanceof Iterable)
                    {
                        parameter = (Iterable<Object>) parametersResult;
                    }
                    else if (parametersResult instanceof Object[])
                    {
                        parameter = Arrays.asList((Object[]) parametersResult);
                    }
                    else
                    {
                        String msg = MessageFormat.format("{0}.{1}() must return an Iterable of arrays.",
                                                          testClass.getJavaClass().getName(), parametersMethod.getName());
                        throw new Exception(msg);
                    }
                    break;
                }
                catch (Throwable e)
                {
                    throw new RuntimeException(e);
                }
            }
        }

        if (!parametersMethods.isEmpty() && parameter == null)
        {
            throw new Exception("No public static parameters method on class " + testClass.getJavaClass().getCanonicalName());
        }

        List<FrameworkField> parameterFrameworkFields = testClass.getAnnotatedFields(Parameter.class);
        LOGGER.debug("Found " + parameterFrameworkFields.size() + " parameter fields");

        List<ParameterStatementData> iterations = new LinkedList<>();

        if (parameter != null)
        {
            int parameterSetCounter = 0;
            for (Object para : parameter)
            {
                Object[] p;
                if (para instanceof Object[])
                {
                    p = (Object[]) para;
                }
                else
                {
                    p = new Object[]
                    {
                      para
                    };
                }

                iterations.add(new ParameterStatementData(parameterSetCounter, p, parameterFrameworkFields));
                parameterSetCounter++;
            }
        }

        return iterations;
    }

    private void injectTestParameter() throws Exception
    {
        int parameterFieldCount = statementData.getParameterFrameworkFields().size();
        Object[] parameter = statementData.getParameter();

        if (parameterFieldCount != parameter.length)
        {
            throw new Exception("Number of parameters (" + parameter.length + ") and " + //
                                "fields (" + parameterFieldCount + ") " + //
                                "annotated with @Parameter must match!");
        }

        for (FrameworkField parameterFrameworkField : statementData.getParameterFrameworkFields())
        {
            Field field = parameterFrameworkField.getField();
            int parameterIndex = field.getAnnotation(Parameter.class).value();

            LOGGER.debug("Set parameter \"" + parameterFrameworkField.getName() + "\" to \"" + parameter[parameterIndex] + "\"");
            setField(field, parameter[parameterIndex]);
        }
    }

    private void setField(Field field, Object value)
    {
        Class<?> fieldType = field.getType();
        Class<?> valueType;
        if (value == null)
        {
            valueType = Void.class;
        }
        else
        {
            valueType = value.getClass();
        }

        // try to convert String values to appropriate target types
        if (valueType == String.class)
        {
            try
            {
                if (fieldType == int.class || fieldType == Integer.class)
                {
                    value = Integer.valueOf((String) value);
                }
                else if (fieldType == long.class || fieldType == Long.class)
                {
                    value = Long.valueOf((String) value);
                }
                else if (fieldType == double.class || fieldType == Double.class)
                {
                    value = Double.valueOf((String) value);
                }
                else if (fieldType == float.class || fieldType == Float.class)
                {
                    value = Float.valueOf((String) value);
                }
                else if (fieldType == boolean.class || fieldType == Boolean.class)
                {
                    value = Boolean.valueOf((String) value);
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException("An error occurred during conversion of input string \"" + (String) value + "\" to type " +
                                           fieldType.getName() + " for field \"" + field.getName() + "\"", e);
            }
        }

        try
        {
            if (Modifier.isFinal(field.getModifiers()))
            {
                throw new IllegalAccessException();
            }
            field.set(testClassInstance, value);
        }
        catch (IllegalArgumentException e)
        {
            throw new RuntimeException("Could not set parameter of type " + valueType + " to field \"" + field.getName() + "\" of type " +
                                       fieldType + ". Value: " + value);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Could not set parameter due to it is not public or it is final");
        }
    }

    @Override
    public ParameterStatement createStatement(Object testClassInstance, Statement next, Object parameter)
    {
        return new ParameterStatement(next, (ParameterStatementData) parameter, testClassInstance);
    }

    @Override
    public String getTestName(Object data)
    {
        ParameterStatementData p = (ParameterStatementData) data;
        return "[" + p.getParameterIndex() + "]";
    }

    @Override
    public String getCategoryName(Object data)
    {
        return getTestName(data);
    }
}
