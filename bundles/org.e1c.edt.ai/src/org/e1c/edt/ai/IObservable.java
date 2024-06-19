/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface IObservable<T>
{
    AutoCloseable subscribe(IObserver<T> observer);
}
