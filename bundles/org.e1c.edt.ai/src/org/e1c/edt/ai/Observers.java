/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.function.Consumer;

import com.google.common.base.Preconditions;

public class Observers
{
    public static <T> IObserver<T> create(Consumer<T> onNextHandler, Consumer<? super Throwable> onErrorHandler,
        Runnable onCompletedHandler)
    {
        Preconditions.checkNotNull(onNextHandler);
        Preconditions.checkNotNull(onErrorHandler);
        Preconditions.checkNotNull(onCompletedHandler);
        return new IObserver<>()
        {
            @Override
            public void onNext(T value)
            {
                onNextHandler.accept(value);
            }

            @Override
            public void onError(Throwable error)
            {
                onErrorHandler.accept(error);
            }

            @Override
            public void onCompleted()
            {
                onCompletedHandler.run();
            }
        };
    }
}
