/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import com.google.common.base.Supplier;

public interface IDispatcher
{
    <T> Optional<T> dispatch(Supplier<T> supplier);

    Boolean dispatch(Runnable runnable);
}
