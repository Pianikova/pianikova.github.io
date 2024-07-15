/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface IResponseCache<T>
{
    CompletableFuture<Optional<T>> get(Supplier<CompletableFuture<Optional<T>>> responseSupplier, boolean reset);
}
