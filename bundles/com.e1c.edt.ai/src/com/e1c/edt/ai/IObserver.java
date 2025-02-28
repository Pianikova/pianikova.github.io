/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

public interface IObserver<T>
{
    void onNext(T value);

    void onError(Throwable error);

    void onCompleted();
}