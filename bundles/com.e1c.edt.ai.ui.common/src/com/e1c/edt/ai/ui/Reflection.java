/**
 *
 */
package com.e1c.edt.ai.ui;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class Reflection implements IReflection
{
    private final ILog log;
    private final ConcurrentMap<MemberKey, Optional<Field>> fields = new ConcurrentHashMap<>();
    private final ConcurrentMap<MemberKey, Optional<Method>> methods = new ConcurrentHashMap<>();

    @Inject
    public Reflection(ILog log)
    {
        Preconditions.checkNotNull(log);
        this.log = log;
    }

    @Override
    public <T, R> Optional<R> getField(Class<T> classOfT, Object target, String fieldName, Class<R> classOfR)
    {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(fieldName);
        return fields
            .computeIfAbsent(new MemberKey(classOfT, fieldName), k -> getField(classOfT, fieldName))
            .map(field -> {
                try
                {
                    var result = field.get(target);
                    if (!checkAssignable(result, classOfR))
                    {
                        return null;
                    }

                    return classOfR.cast(result);
                }
                catch (IllegalArgumentException | IllegalAccessException error)
                {
                    log.logError(error);
                    return null;
                }
            });
    }

    @Override
    public <T, R> Optional<R> callMethod(Class<T> classOfT, Object target, String methodName, Class<R> classOfR,
        Object... args)
    {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(methodName);
        return methods
            .computeIfAbsent(new MemberKey(classOfT, methodName), k -> getMethod(classOfT, methodName))
            .map(method -> {
                try
                {
                    var result = method.invoke(target, args);
                    if (!checkAssignable(result, classOfR))
                    {
                        return null;
                    }

                    return classOfR.cast(result);
                }
                catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException error)
                {
                    log.logError(error);
                    return null;
                }
            });
    }

    private <R> boolean checkAssignable(Object result, Class<R> classOfR)
    {
        var resultClazz = result.getClass();
        if (classOfR.isAssignableFrom(resultClazz))
        {
            return true;
        }

        log.logError(classOfR.getSimpleName() + " is not assignable from " + resultClazz.getSimpleName()); //$NON-NLS-1$
        return false;
    }

    private <T> Optional<Field> getField(Class<T> classOfT, String fieldName)
    {
        Field field;
        try
        {
            field = classOfT.getDeclaredField(fieldName);
            if (field != null)
            {
                field.setAccessible(true);
            }
        }
        catch (Exception error)
        {
            log.logError(error);
            return Optional.empty();
        }

        return Optional.ofNullable(field);
    }

    private <T> Optional<Method> getMethod(Class<T> classOfT, String methodName)
    {
        Method method;
        try
        {
            method = classOfT.getDeclaredMethod(methodName);
            if (method != null)
            {
                method.setAccessible(true);
            }
        }
        catch (Exception error)
        {
            log.logError(error);
            return Optional.empty();
        }

        return Optional.ofNullable(method);
    }

    private class MemberKey
    {
        public final Class<?> classOfT;
        public final String fieldName;

        public MemberKey(Class<?> classOfT, String fieldName)
        {
            this.classOfT = classOfT;
            this.fieldName = fieldName;
        }

        @Override
        public int hashCode()
        {
            return classOfT.hashCode() ^ fieldName.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
            {
                return false;
            }

            if (obj == this)
            {
                return true;
            }

            if (obj.getClass() != MemberKey.class)
            {
                return false;
            }

            var other = (MemberKey)obj;
            return classOfT.equals(other.classOfT) && fieldName.equals(other.fieldName);
        }

    }
}
