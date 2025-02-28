/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.function.Function;

import com.google.common.base.Preconditions;

public class Observables
{
    public static <T> IObservable<T> create(Function<? super IObserver<T>, ? extends AutoCloseable> onSubscribe)
    {
        Preconditions.checkNotNull(onSubscribe);
        return new IObservable<>()
        {
            @Override
            public AutoCloseable subscribe(IObserver<T> observer)
            {
                return onSubscribe.apply(observer);
            }
        };
    }
}
