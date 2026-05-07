/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface IReflectionSignatureFormatter
{
    String format(Method method);

    String format(Constructor<?> constructor);

    String format(Field field);
}
