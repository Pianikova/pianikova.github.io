/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.time.Duration;
import java.util.Optional;

import com.google.common.base.Supplier;

public interface IDispatcher
{
    <T> Optional<T> dispatch(Supplier<? extends T> supplier, Duration timeout);
}
