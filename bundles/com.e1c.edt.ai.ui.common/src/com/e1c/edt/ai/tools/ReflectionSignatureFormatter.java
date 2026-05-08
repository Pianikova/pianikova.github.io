/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.google.inject.Singleton;

@Singleton
public class ReflectionSignatureFormatter
    implements IReflectionSignatureFormatter
{
    @Override
    public String format(Method method)
    {
        return method.getName() + "(" + formatParameters(method) + "): " + formatType(method.getReturnType()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public String format(Constructor<?> constructor)
    {
        return constructor.getDeclaringClass().getSimpleName() + "(" + formatParameters(constructor) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public String format(Field field)
    {
        return field.getName() + ": " + formatType(field.getType()); //$NON-NLS-1$
    }

    private String formatParameters(Executable executable)
    {
        return Arrays.stream(executable.getParameters()).map(this::formatParameter).collect(Collectors.joining(", ")); //$NON-NLS-1$
    }

    private String formatParameter(Parameter parameter)
    {
        var name = parameter.isNamePresent() ? " " + parameter.getName() : ""; //$NON-NLS-1$ //$NON-NLS-2$
        return formatType(parameter.getType()) + name;
    }

    private String formatType(Class<?> type)
    {
        if (type.isArray())
        {
            return formatType(type.getComponentType()) + "[]"; //$NON-NLS-1$
        }
        return type.getSimpleName();
    }
}
