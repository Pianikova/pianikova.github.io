/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.time.Duration;
import java.util.Optional;

import com.google.common.base.Supplier;

public interface IDispatcher
{
    <T> Optional<T> dispatch(Supplier<? extends T> supplier, Duration timeout);
}
