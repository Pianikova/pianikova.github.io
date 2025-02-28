/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

public interface IObservable<T>
{
    AutoCloseable subscribe(IObserver<T> observer);
}
