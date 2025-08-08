/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

public interface IReflection
{
    <T, R> Optional<R> getField(Class<T> classOfT, Object target, String fieldName, Class<R> classOfR);

    <T, R> Optional<R> callMethod(Class<T> classOfT, Object target, String methodName, Class<R> classOfR,
        Object... args);
}
