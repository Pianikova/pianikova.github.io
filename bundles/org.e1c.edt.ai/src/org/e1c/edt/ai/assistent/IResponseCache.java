/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface IResponseCache<T>
{
    CompletableFuture<Optional<T>> get(String key, Supplier<CompletableFuture<Optional<T>>> responseSupplier,
        boolean reset);
}
