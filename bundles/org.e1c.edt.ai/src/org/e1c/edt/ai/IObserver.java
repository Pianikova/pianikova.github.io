/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface IObserver<T>
{
    boolean onNext(T value);

    void onError(Throwable error);

    void onCompleted();
}