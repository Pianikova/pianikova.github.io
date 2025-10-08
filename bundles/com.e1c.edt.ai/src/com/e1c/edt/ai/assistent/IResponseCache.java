/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.e1c.edt.ai.assistent.model.Session;

public interface IResponseCache
{
    CompletableFuture<Optional<Session>> get(String key, Supplier<CompletableFuture<Optional<Session>>> taskSupplier,
        boolean reset, boolean cacheErrors);
}

